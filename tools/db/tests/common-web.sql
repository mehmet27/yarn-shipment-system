-- platform şeması testleri: audit_local yalnızca ekleme kabul eder, Idempotency-Key kaydı durumu kısıtlıdır.
BEGIN;
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

INSERT INTO audit_local (table_schema, table_name, operation, row_id, db_user, transaction_id)
VALUES ('shipment', 'md_product', 'INSERT', gen_random_uuid(), 'test', 1);
SELECT pg_temp.expect_error($$UPDATE audit_local SET table_name = 'x'$$, 'YS001');
SELECT pg_temp.expect_error($$DELETE FROM audit_local$$, 'YS001');
SELECT pg_temp.expect_error($$INSERT INTO audit_local (table_schema, table_name, operation, db_user, transaction_id)
    VALUES ('shipment', 'md_product', 'MERGE', 'test', 1)$$, '23514');

INSERT INTO idempotency_record (actor, idempotency_key, fingerprint, state, created_at)
VALUES ('depo', 'k1', 'f', 'IN_PROGRESS', now());
SELECT pg_temp.expect_error($$INSERT INTO idempotency_record (actor, idempotency_key, fingerprint, state, created_at)
    VALUES ('depo', 'k1', 'f', 'IN_PROGRESS', now())$$, '23505');
SELECT pg_temp.expect_error($$INSERT INTO idempotency_record (actor, idempotency_key, fingerprint, state, created_at)
    VALUES ('depo', 'k2', 'f', 'DONE', now())$$, '23514');
ROLLBACK;
