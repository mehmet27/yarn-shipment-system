-- Modüler monolit geçişi: modüllerin ortak kullandığı iki nesne tek yerde, `platform` şemasında toplanır.
--  * idempotency_record: Idempotency-Key kayıtları (shipment ve evidence kopyaları tekilleşir)
--  * audit_local: kritik tablolardaki değişikliklerin trigger ile tutulan yerel izi (shipment, discrepancy ve evidence
--    kopyaları tekilleşir). Hangi şemadaki tablodan geldiği table_schema kolonunda durur.
--
-- Bu şema modül şemalarından önce göç eder (kendi flyway_schema_history tablosu vardır); modüllerin trigger'ları
-- platform.audit_local_capture() fonksiyonuna bağlanır.
--
-- Flyway yer tutucusu:
--   ${app_role}  uygulamanın bağlandığı rol (DML)

ALTER DEFAULT PRIVILEGES REVOKE EXECUTE ON FUNCTIONS FROM PUBLIC;
GRANT USAGE ON SCHEMA ${flyway:defaultSchema} TO ${app_role};

-- audit_local yalnızca ekleme kabul eder; trigger fonksiyonu uygulama rolüne yazma yetkisi vermeden ekler.
CREATE FUNCTION reject_modification()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'Table is append-only table=% operation=%', TG_TABLE_NAME, TG_OP
        USING ERRCODE = 'YS001';
END;
$$;

-- Aylık range partition'ları oluşturur (bkz. audit şemasındaki aynı adlı fonksiyon).
CREATE FUNCTION create_monthly_partitions(parent regclass, from_month date, month_count integer)
    RETURNS void
    LANGUAGE plpgsql
    SECURITY DEFINER
    SET search_path = ${flyway:defaultSchema}, pg_temp
AS $$
DECLARE
    parent_name text;
    month_start date;
BEGIN
    SELECT c.relname INTO STRICT parent_name FROM pg_class c WHERE c.oid = parent;
    FOR i IN 0 .. month_count - 1 LOOP
        month_start := (date_trunc('month', from_month) + make_interval(months => i))::date;
        EXECUTE format(
            'CREATE TABLE IF NOT EXISTS %I PARTITION OF %I FOR VALUES FROM (%L) TO (%L)',
            parent_name || '_p' || to_char(month_start, 'YYYYMM'),
            parent_name,
            to_char(month_start, 'YYYY-MM-DD') || ' 00:00:00+00',
            to_char(month_start + interval '1 month', 'YYYY-MM-DD') || ' 00:00:00+00');
    END LOOP;
END;
$$;

GRANT EXECUTE ON FUNCTION create_monthly_partitions(regclass, date, integer) TO ${app_role};

-- API-04: Idempotency-Key kayıtları. Kayıtlar kullanıcıya özeldir (actor + idempotency_key).
CREATE TABLE idempotency_record (
    actor                 text        NOT NULL,
    idempotency_key       text        NOT NULL,
    fingerprint           text        NOT NULL,
    state                 text        NOT NULL CHECK (state IN ('IN_PROGRESS', 'COMPLETED')),
    response_status       integer,
    response_content_type text,
    response_location     text,
    response_body         bytea,
    created_at            timestamptz NOT NULL,
    PRIMARY KEY (actor, idempotency_key)
);

CREATE INDEX idempotency_record_created_idx ON idempotency_record (created_at);

GRANT SELECT, INSERT, UPDATE, DELETE ON idempotency_record TO ${app_role};

-- Kuyruk çalışmasa bile kritik tablolardaki değişikliklerin izi kaybolmasın diye trigger ile doldurulan yerel iz
-- (veritabanı tasarımı, "Audit Log akışı"). Uygulama rolü bu tabloya doğrudan yazamaz ve okuyamaz; yalnızca
-- SECURITY DEFINER trigger fonksiyonu ekler.
CREATE TABLE audit_local (
    id             bigint GENERATED ALWAYS AS IDENTITY,
    recorded_at    timestamptz NOT NULL DEFAULT clock_timestamp(),
    table_schema   text        NOT NULL,
    table_name     text        NOT NULL,
    operation      text        NOT NULL CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    row_id         uuid,
    old_row        jsonb,
    new_row        jsonb,
    actor_id       text,
    db_user        text        NOT NULL,
    transaction_id bigint      NOT NULL,
    PRIMARY KEY (id, recorded_at)
) PARTITION BY RANGE (recorded_at);

COMMENT ON TABLE audit_local IS 'Kritik tablolardaki değişikliklerin trigger ile tutulan yerel izi (tüm modüller)';
COMMENT ON COLUMN audit_local.table_schema IS 'Değişen tablonun modül şeması (shipment, discrepancy, evidence)';
COMMENT ON COLUMN audit_local.actor_id IS 'Uygulamanın transaction başında ayarladığı app.actor_id değeri';

CREATE INDEX audit_local_table_idx ON audit_local (table_schema, table_name, row_id);

CREATE FUNCTION audit_local_capture()
    RETURNS trigger
    LANGUAGE plpgsql
    SECURITY DEFINER
    SET search_path = ${flyway:defaultSchema}, pg_temp
AS $$
DECLARE
    old_row jsonb := CASE WHEN TG_OP IN ('UPDATE', 'DELETE') THEN to_jsonb(OLD) END;
    new_row jsonb := CASE WHEN TG_OP IN ('INSERT', 'UPDATE') THEN to_jsonb(NEW) END;
BEGIN
    INSERT INTO audit_local (table_schema, table_name, operation, row_id, old_row, new_row, actor_id, db_user,
                             transaction_id)
    VALUES (TG_TABLE_SCHEMA, TG_TABLE_NAME, TG_OP, (coalesce(new_row, old_row) ->> 'id')::uuid, old_row, new_row,
            nullif(current_setting('app.actor_id', true), ''), session_user, txid_current());
    RETURN NULL;
END;
$$;

CREATE TRIGGER audit_local_append_only
    BEFORE UPDATE OR DELETE ON audit_local
    FOR EACH ROW EXECUTE FUNCTION reject_modification();

SELECT create_monthly_partitions('audit_local', date '2026-10-01', 15);
