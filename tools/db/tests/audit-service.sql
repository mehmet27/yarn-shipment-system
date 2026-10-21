-- audit_db testleri: yazan rol yalnızca audit_append() çalıştırır, zincir doğrulanır ve geri alınır.
BEGIN;
-- Beklenen hata kodu üretmeyen ifade testi düşürür.
CREATE FUNCTION pg_temp.expect_error(statement text, expected_state text) RETURNS void LANGUAGE plpgsql AS $$
DECLARE
    actual_state text;
BEGIN
    BEGIN
        EXECUTE statement;
    EXCEPTION WHEN OTHERS THEN
        actual_state := SQLSTATE;
    END;
    IF actual_state IS DISTINCT FROM expected_state THEN
        RAISE EXCEPTION 'Expected SQLSTATE % but got %: %', expected_state, coalesce(actual_state, 'success'), statement;
    END IF;
END;
$$;
GRANT EXECUTE ON FUNCTION pg_temp.expect_error(text, text) TO PUBLIC;
SET ROLE audit_writer;

SELECT audit_append(jsonb_build_object('eventId', '00000000-0000-7000-8000-000000000701',
    'occurredAt', '2026-10-21T08:00:00Z', 'sourceService', 'shipment-service', 'actorUserId', gen_random_uuid(),
    'actorRole', 'WAREHOUSE', 'action', 'com.yarnshipment.shipment.shipment-loaded.v1', 'entityType', 'Shipment',
    'entityId', gen_random_uuid(), 'after', jsonb_build_object('status', 'LOADED')));
SELECT audit_append(jsonb_build_object('eventId', '00000000-0000-7000-8000-000000000702',
    'occurredAt', '2026-10-21T08:05:00Z', 'sourceService', 'shipment-service', 'clientId', 'shipment-service',
    'action', 'AUTHORIZATION', 'decision', 'DENIED'));

DO $$
DECLARE
    first_id bigint;
BEGIN
    -- Aynı olay ikinci kez yazılmaz, ilk kaydın id'si döner.
    first_id := audit_append(jsonb_build_object('eventId', '00000000-0000-7000-8000-000000000701',
        'occurredAt', '2026-10-21T08:00:00Z', 'sourceService', 'shipment-service', 'clientId', 'x', 'action', 'x'));
    IF first_id <> 1 THEN
        RAISE EXCEPTION 'Duplicate event must return the first id, got %', first_id;
    END IF;
    IF audit_verify_chain(1, 2) IS NOT NULL THEN
        RAISE EXCEPTION 'Fresh chain must verify';
    END IF;
END
$$;

-- Yazan rol tabloyu okuyamaz, değiştiremez, doğrudan ekleyemez.
SELECT pg_temp.expect_error($$SELECT * FROM audit_log$$, '42501');
SELECT pg_temp.expect_error($$UPDATE audit_log SET action = 'x'$$, '42501');
SELECT pg_temp.expect_error($$DELETE FROM audit_log$$, '42501');

-- Okuyan rol yalnızca okur.
RESET ROLE;
SET ROLE audit_reader;
DO $$ BEGIN
    IF (SELECT count(*) FROM audit_log) <> 2 THEN
        RAISE EXCEPTION 'Reader must see two rows';
    END IF;
END $$;
SELECT pg_temp.expect_error($$SELECT audit_append('{}'::jsonb)$$, '42501');

-- Tablo sahibi bile güncelleyemez; trigger kapatılıp kayıt değiştirilirse zincir bozulmayı yakalar.
RESET ROLE;
SELECT pg_temp.expect_error($$UPDATE audit_log SET action = 'x' WHERE id = 1$$, 'YS001');
ALTER TABLE audit_log DISABLE TRIGGER audit_log_append_only;
UPDATE audit_log SET action = 'tampered' WHERE id = 1;
DO $$ BEGIN
    IF audit_verify_chain(1, 2) IS DISTINCT FROM 1 THEN
        RAISE EXCEPTION 'Tampered row must break the chain';
    END IF;
END $$;
ROLLBACK;
