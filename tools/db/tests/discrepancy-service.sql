-- discrepancy_db kısıt testleri; uygulama rolüyle çalışır ve geri alınır.
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

INSERT INTO discrepancy (id, discrepancy_no, site_id, shipment_id, discrepancy_type, severity, status, rule_code,
                         created_at, created_by)
VALUES ('00000000-0000-7000-8000-000000000601', 'U-1', gen_random_uuid(), gen_random_uuid(), 'QUANTITY', 'RED',
        'OPEN', 'NET_KG_DIFF', now(), gen_random_uuid());
-- Karar ve not olmadan RESOLVED olamaz.
SELECT pg_temp.expect_error($$UPDATE discrepancy SET status = 'RESOLVED'
    WHERE id = '00000000-0000-7000-8000-000000000601'$$, '23514');
UPDATE discrepancy SET status = 'RESOLVED', resolution = 'TOLERANCE', resolution_note = 'Kantar farkı',
                       resolved_by = gen_random_uuid(), resolved_at = now()
WHERE id = '00000000-0000-7000-8000-000000000601';
INSERT INTO discrepancy_activity (id, created_at, discrepancy_id, action_type, actor_id, occurred_at)
VALUES (gen_random_uuid(), now(), '00000000-0000-7000-8000-000000000601', 'RESOLVED', gen_random_uuid(), now());
ROLLBACK;
