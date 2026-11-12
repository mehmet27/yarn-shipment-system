-- Modüler monolit geçişi: ortak nesneler `platform` şemasına taşındı (bkz. db/platform/V202611181000).
--  * Bu şemadaki audit_local trigger'ları platform.audit_local_capture() fonksiyonuna bağlanır; şemanın eski
--    audit_local satırları platform.audit_local'a (table_schema ile) kopyalanır, eski tablo ve fonksiyon düşer.
--  * idempotency_record: kayıtlar platform.idempotency_record'a taşınır (aynı anahtar varsa ilk kayıt kalır).
-- Platform şeması bu migration'dan önce göç etmiş olmalıdır (yarnshipment-app ve dilim testleri bunu sağlar).

DO $$
DECLARE
    capture regprocedure := to_regprocedure(format('%I.audit_local_capture()', current_schema()));
    bound record;
BEGIN
    IF to_regclass('platform.audit_local') IS NULL THEN
        RAISE EXCEPTION 'platform schema is not migrated; run PlatformSchemaMigration before the module migrations';
    END IF;
    -- Bölümlü tabloların trigger'ı bölümlere kendiliğinden kopyalanır (tgparentid <> 0); yalnız ana trigger yeniden kurulur.
    FOR bound IN
        SELECT t.tgname, t.tgrelid::regclass AS table_name, pg_get_triggerdef(t.oid) AS definition
          FROM pg_trigger t
         WHERE t.tgfoid = capture AND NOT t.tgisinternal AND t.tgparentid = 0
    LOOP
        EXECUTE format('DROP TRIGGER %I ON %s', bound.tgname, bound.table_name);
        EXECUTE regexp_replace(bound.definition,
                'EXECUTE (FUNCTION|PROCEDURE) [A-Za-z0-9_."]*audit_local_capture\(\)',
                'EXECUTE FUNCTION platform.audit_local_capture()');
    END LOOP;
END
$$;

INSERT INTO platform.audit_local (recorded_at, table_schema, table_name, operation, row_id, old_row, new_row, actor_id,
                                  db_user, transaction_id)
SELECT recorded_at, current_schema(), table_name, operation, row_id, old_row, new_row, actor_id, db_user,
       transaction_id
  FROM audit_local
 ORDER BY recorded_at, id;

DROP TABLE audit_local;
DROP FUNCTION audit_local_capture();

INSERT INTO platform.idempotency_record
SELECT * FROM idempotency_record
ON CONFLICT DO NOTHING;

DROP TABLE idempotency_record;
