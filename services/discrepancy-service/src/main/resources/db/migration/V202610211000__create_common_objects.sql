-- Her servis veritabanında bulunan ortak nesneler (veritabanı tasarımı, "Servisler arası kurallar",
-- "Ortak konular"). Servisler veritabanını paylaşmadığı için bu dosya her serviste ayrı bir kopyadır
-- (PERS-15); değişiklik gerekiyorsa her servise yeni bir migration olarak eklenir.
--
-- Flyway yer tutucuları:
--   ${app_role}       uygulamanın bağlandığı rol (DML)
--   ${readonly_role}  salt okuma rolü
--
-- Uygulamanın yakalayıp eşlediği SQLSTATE kodları:
--   YS001  yalnızca ekleme yapılan tabloda UPDATE/DELETE
--   YS002  değiştirilemez kolon değiştirildi
--   YS003  kilitli kayda değişiklik (REST 409)
--   YS004  kilitli parti kalitesi değiştirildi

ALTER DEFAULT PRIVILEGES REVOKE EXECUTE ON FUNCTIONS FROM PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO ${readonly_role};
GRANT USAGE ON SCHEMA public TO ${app_role}, ${readonly_role};

-- Aylık range partition'ları oluşturur. pg_partman olmayan ortamlarda (yerel Compose) servis
-- bu fonksiyonu zamanlanmış görevle her gün çağırarak gelecek ayları hazır tutar.
CREATE FUNCTION create_monthly_partitions(parent regclass, from_month date, month_count integer)
    RETURNS void
    LANGUAGE plpgsql
    SECURITY DEFINER
    SET search_path = public, pg_temp
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

-- Geçmiş tabloları yalnızca ekleme kabul eder (shipment_status_history, discrepancy_activity vb.).
CREATE FUNCTION reject_modification()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'Table is append-only table=% operation=%', TG_TABLE_NAME, TG_OP
        USING ERRCODE = 'YS001';
END;
$$;

-- Satırın yalnızca trigger argümanlarında sayılan kolonları değişebilir. Kritik ölçüm kayıtlarında
-- (tartım, mühür) düzeltme yeni satır ekler; eski satırda yalnızca superseded_by işaretlenir.
-- superseded_by bir kez dolduktan sonra değişmez. DELETE her zaman reddedilir. Üretilmiş kolonlar
-- (weighing.net_kg) BEFORE aşamasında henüz hesaplanmadığı için AFTER trigger olarak bağlanır.
CREATE FUNCTION guard_immutable_columns()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
DECLARE
    mutable_columns text[] := TG_ARGV || ARRAY['updated_at', 'updated_by', 'version'];
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'Rows cannot be deleted table=%', TG_TABLE_NAME USING ERRCODE = 'YS001';
    END IF;
    IF (to_jsonb(OLD) - mutable_columns) IS DISTINCT FROM (to_jsonb(NEW) - mutable_columns) THEN
        RAISE EXCEPTION 'Immutable column changed table=% id=%', TG_TABLE_NAME, OLD.id
            USING ERRCODE = 'YS002';
    END IF;
    IF to_jsonb(OLD) ? 'superseded_by'
            AND to_jsonb(OLD) ->> 'superseded_by' IS NOT NULL
            AND to_jsonb(OLD) -> 'superseded_by' IS DISTINCT FROM to_jsonb(NEW) -> 'superseded_by' THEN
        RAISE EXCEPTION 'Superseded row cannot change table=% id=%', TG_TABLE_NAME, OLD.id
            USING ERRCODE = 'YS002';
    END IF;
    RETURN NULL;
END;
$$;

-- Kuyruk çalışmasa bile kritik tablolardaki değişikliklerin izi kaybolmasın diye trigger ile
-- doldurulan yerel iz (veritabanı tasarımı, "Audit Log akışı"). Uygulama rolü bu tabloya doğrudan
-- yazamaz ve okuyamaz; yalnızca SECURITY DEFINER trigger fonksiyonu ekler.
CREATE TABLE audit_local (
    id             bigint GENERATED ALWAYS AS IDENTITY,
    recorded_at    timestamptz NOT NULL DEFAULT clock_timestamp(),
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

COMMENT ON TABLE audit_local IS 'Kritik tablolardaki değişikliklerin trigger ile tutulan yerel izi';
COMMENT ON COLUMN audit_local.actor_id IS 'Uygulamanın transaction başında ayarladığı app.actor_id değeri';

CREATE FUNCTION audit_local_capture()
    RETURNS trigger
    LANGUAGE plpgsql
    SECURITY DEFINER
    SET search_path = public, pg_temp
AS $$
DECLARE
    old_row jsonb := CASE WHEN TG_OP IN ('UPDATE', 'DELETE') THEN to_jsonb(OLD) END;
    new_row jsonb := CASE WHEN TG_OP IN ('INSERT', 'UPDATE') THEN to_jsonb(NEW) END;
BEGIN
    INSERT INTO audit_local (table_name, operation, row_id, old_row, new_row, actor_id, db_user, transaction_id)
    VALUES (TG_TABLE_NAME, TG_OP, (coalesce(new_row, old_row) ->> 'id')::uuid, old_row, new_row,
            nullif(current_setting('app.actor_id', true), ''), session_user, txid_current());
    RETURN NULL;
END;
$$;

CREATE TRIGGER audit_local_append_only
    BEFORE UPDATE OR DELETE ON audit_local
    FOR EACH ROW EXECUTE FUNCTION reject_modification();

-- Transactional outbox (EVT-03): iş verisi ve olay aynı transaction'da yazılır, common-outbox
-- yayıncısı Kafka'ya iletip published_at'i doldurur.
CREATE TABLE outbox_event (
    id             uuid        NOT NULL,
    created_at     timestamptz NOT NULL,
    aggregate_type text        NOT NULL,
    aggregate_id   uuid        NOT NULL,
    event_type     text        NOT NULL,
    payload        jsonb       NOT NULL,
    headers        jsonb       NOT NULL DEFAULT '{}'::jsonb,
    occurred_at    timestamptz NOT NULL,
    published_at   timestamptz,
    PRIMARY KEY (id, created_at),
    CONSTRAINT outbox_event_type_format
        CHECK (event_type ~ '^com\.yarnshipment\.[a-z]+(-[a-z]+)*\.[a-z]+(-[a-z]+)*\.v[0-9]+$')
) PARTITION BY RANGE (created_at);

COMMENT ON COLUMN outbox_event.id IS 'CloudEvents id (UUIDv7); tüketicilerde idempotency anahtarı';
COMMENT ON COLUMN outbox_event.aggregate_id IS 'CloudEvents subject ve Kafka mesaj anahtarı';
COMMENT ON COLUMN outbox_event.headers IS 'actorId, actorRoles, correlationId (mimari doküman, olay formatı)';

CREATE INDEX outbox_event_unpublished_idx ON outbox_event (created_at) WHERE published_at IS NULL;

CREATE TRIGGER outbox_event_guard
    AFTER UPDATE OR DELETE ON outbox_event
    FOR EACH ROW EXECUTE FUNCTION guard_immutable_columns('published_at');

GRANT SELECT, INSERT ON outbox_event TO ${app_role};
GRANT UPDATE (published_at) ON outbox_event TO ${app_role};

-- İdempotent tüketici (EVT-05): olay id'si işlemle aynı transaction'da yazılır.
CREATE TABLE processed_event (
    event_id     uuid        PRIMARY KEY,
    event_type   text        NOT NULL,
    processed_at timestamptz NOT NULL
);

GRANT SELECT, INSERT ON processed_event TO ${app_role};

SELECT create_monthly_partitions('audit_local', date '2026-10-01', 15);
SELECT create_monthly_partitions('outbox_event', date '2026-10-01', 15);
