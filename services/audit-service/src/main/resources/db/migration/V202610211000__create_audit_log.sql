-- audit_db: değiştirilemez Audit Log (veritabanı tasarımı, "audit_db"; yetkilendirme analizi).
--
-- Flyway yer tutucuları:
--   ${app_role}       audit_writer: tablolara hiçbir yetkisi yoktur, yalnızca audit_append() ve
--                     audit_verify_chain() fonksiyonlarını çalıştırır
--   ${readonly_role}  audit_reader: audit ekranı için yalnızca SELECT
--
-- Hash zinciri: row_hash = SHA-256(prev_hash || audit_canonical(satır)). İlk satırın prev_hash'i 64 sıfırdır.
-- Aynı olay iki kez yazılmaz: (event_id, occurred_at) tekildir; aynı olayın occurred_at'i her
-- tekrarda aynı olduğu için bu, partition'lı tabloda event_id tekilliğinin karşılığıdır.

ALTER DEFAULT PRIVILEGES REVOKE EXECUTE ON FUNCTIONS FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO ${app_role}, ${readonly_role};

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

CREATE FUNCTION reject_modification()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'Table is append-only table=% operation=%', TG_TABLE_NAME, TG_OP
        USING ERRCODE = 'YS001';
END;
$$;

CREATE TABLE audit_log (
    id             bigint      NOT NULL GENERATED ALWAYS AS IDENTITY,
    occurred_at    timestamptz NOT NULL,
    recorded_at    timestamptz NOT NULL,
    source_service text        NOT NULL,
    event_id       uuid        NOT NULL,
    actor_user_id  uuid,
    actor_role     text,
    client_id      text,
    action         text        NOT NULL,
    decision       text        CHECK (decision IN ('ALLOWED', 'DENIED')),
    entity_type    text,
    entity_id      uuid,
    before         jsonb,
    after          jsonb,
    ip_address     inet,
    device_info    jsonb,
    user_agent     text,
    trace_id       text,
    prev_hash      char(64)    NOT NULL CHECK (prev_hash ~ '^[0-9a-f]{64}$'),
    row_hash       char(64)    NOT NULL CHECK (row_hash ~ '^[0-9a-f]{64}$'),
    PRIMARY KEY (id, occurred_at),
    UNIQUE (event_id, occurred_at),
    CONSTRAINT audit_log_actor_present CHECK (actor_user_id IS NOT NULL OR client_id IS NOT NULL)
) PARTITION BY RANGE (occurred_at);

COMMENT ON TABLE audit_log IS 'Değiştirilemez denetim izi; yalnızca audit_append() ile eklenir';
COMMENT ON COLUMN audit_log.client_id IS 'Kullanıcısız (SYSTEM) işlerde çağıran servisin client kimliği';
COMMENT ON COLUMN audit_log.decision IS 'Yalnızca yetki kararı kayıtlarında dolu';

CREATE INDEX audit_log_entity_idx ON audit_log (entity_type, entity_id, occurred_at);
CREATE INDEX audit_log_actor_idx ON audit_log (actor_user_id, occurred_at);
CREATE INDEX audit_log_id_idx ON audit_log (id);

CREATE TRIGGER audit_log_append_only
    BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION reject_modification();

-- Zincirin son halkası. audit_append() bu satırı kilitleyerek eklemeleri sıraya koyar.
CREATE TABLE audit_chain_head (
    singleton boolean  PRIMARY KEY DEFAULT true CHECK (singleton),
    last_id   bigint   NOT NULL,
    last_hash char(64) NOT NULL
);

INSERT INTO audit_chain_head (last_id, last_hash) VALUES (0, repeat('0', 64));

CREATE TABLE audit_chain_check (
    id           bigint      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    checked_at   timestamptz NOT NULL,
    from_id      bigint      NOT NULL,
    to_id        bigint      NOT NULL,
    ok           boolean     NOT NULL,
    broken_at_id bigint,
    CONSTRAINT audit_chain_check_result CHECK (ok = (broken_at_id IS NULL))
);

COMMENT ON TABLE audit_chain_check IS 'Günlük zincir doğrulama sonucu; bozulmada alarm';

CREATE TRIGGER audit_chain_check_append_only
    BEFORE UPDATE OR DELETE ON audit_chain_check
    FOR EACH ROW EXECUTE FUNCTION reject_modification();

-- Hash'e giren kanonik biçim. jsonb'nin metin çıktısı anahtar sırası dahil deterministiktir;
-- zaman değerleri oturum ayarından bağımsız olsun diye fonksiyon UTC ile çalışır.
CREATE FUNCTION audit_canonical(entry audit_log)
    RETURNS text
    LANGUAGE sql
    STABLE
    SET timezone = 'UTC'
AS $$
    SELECT jsonb_build_object(
        'id', entry.id, 'occurredAt', entry.occurred_at, 'recordedAt', entry.recorded_at,
        'sourceService', entry.source_service, 'eventId', entry.event_id,
        'actorUserId', entry.actor_user_id, 'actorRole', entry.actor_role, 'clientId', entry.client_id,
        'action', entry.action, 'decision', entry.decision, 'entityType', entry.entity_type,
        'entityId', entry.entity_id, 'before', entry.before, 'after', entry.after,
        'ipAddress', entry.ip_address, 'deviceInfo', entry.device_info, 'userAgent', entry.user_agent,
        'traceId', entry.trace_id)::text;
$$;

CREATE FUNCTION audit_hash(prev_hash text, entry audit_log)
    RETURNS char(64)
    LANGUAGE sql
    STABLE
AS $$
    SELECT encode(sha256(convert_to(prev_hash || audit_canonical(entry), 'UTF8')), 'hex');
$$;

-- Tek ekleme yolu. Girdi, audit_log kolonlarının camelCase adlarını taşıyan bir JSON nesnesidir.
-- Aynı eventId daha önce yazıldıysa yeni satır eklenmez, mevcut satırın id'si döner.
CREATE FUNCTION audit_append(entry jsonb)
    RETURNS bigint
    LANGUAGE plpgsql
    SECURITY DEFINER
    SET search_path = public, pg_temp
AS $$
DECLARE
    head audit_chain_head;
    new_row audit_log;
    existing_id bigint;
BEGIN
    SELECT * INTO STRICT head FROM audit_chain_head FOR UPDATE;

    SELECT a.id INTO existing_id FROM audit_log a
    WHERE a.event_id = (entry ->> 'eventId')::uuid AND a.occurred_at = (entry ->> 'occurredAt')::timestamptz;
    IF existing_id IS NOT NULL THEN
        RETURN existing_id;
    END IF;

    new_row := jsonb_populate_record(NULL::audit_log, jsonb_build_object(
        'id', nextval(pg_get_serial_sequence('audit_log', 'id')),
        'occurred_at', entry -> 'occurredAt', 'recorded_at', to_jsonb(clock_timestamp()),
        'source_service', entry -> 'sourceService', 'event_id', entry -> 'eventId',
        'actor_user_id', entry -> 'actorUserId', 'actor_role', entry -> 'actorRole',
        'client_id', entry -> 'clientId', 'action', entry -> 'action', 'decision', entry -> 'decision',
        'entity_type', entry -> 'entityType', 'entity_id', entry -> 'entityId',
        'before', entry -> 'before', 'after', entry -> 'after', 'ip_address', entry -> 'ipAddress',
        'device_info', entry -> 'deviceInfo', 'user_agent', entry -> 'userAgent', 'trace_id', entry -> 'traceId',
        'prev_hash', head.last_hash));
    new_row.row_hash := audit_hash(head.last_hash, new_row);

    INSERT INTO audit_log OVERRIDING SYSTEM VALUE SELECT new_row.*;
    UPDATE audit_chain_head SET last_id = new_row.id, last_hash = new_row.row_hash;
    RETURN new_row.id;
END;
$$;

-- Zinciri from_id..to_id aralığında doğrular, sonucu audit_chain_check'e yazar ve ilk bozuk
-- satırın id'sini döner (bozulma yoksa NULL).
CREATE FUNCTION audit_verify_chain(from_id bigint, to_id bigint)
    RETURNS bigint
    LANGUAGE plpgsql
    SECURITY DEFINER
    SET search_path = public, pg_temp
AS $$
DECLARE
    entry audit_log;
    expected_prev text;
    broken_id bigint;
BEGIN
    SELECT a.row_hash INTO expected_prev FROM audit_log a WHERE a.id < from_id ORDER BY a.id DESC LIMIT 1;
    expected_prev := coalesce(expected_prev, repeat('0', 64));
    FOR entry IN SELECT * FROM audit_log a WHERE a.id BETWEEN from_id AND to_id ORDER BY a.id LOOP
        IF entry.prev_hash <> expected_prev OR entry.row_hash <> audit_hash(expected_prev, entry) THEN
            broken_id := entry.id;
            EXIT;
        END IF;
        expected_prev := entry.row_hash;
    END LOOP;
    INSERT INTO audit_chain_check (checked_at, from_id, to_id, ok, broken_at_id)
    VALUES (clock_timestamp(), from_id, to_id, broken_id IS NULL, broken_id);
    RETURN broken_id;
END;
$$;

GRANT EXECUTE ON FUNCTION audit_append(jsonb) TO ${app_role};
GRANT EXECUTE ON FUNCTION audit_verify_chain(bigint, bigint) TO ${app_role};
GRANT EXECUTE ON FUNCTION create_monthly_partitions(regclass, date, integer) TO ${app_role};
GRANT SELECT ON audit_log, audit_chain_check TO ${readonly_role};

SELECT create_monthly_partitions('audit_log', date '2026-10-01', 15);
