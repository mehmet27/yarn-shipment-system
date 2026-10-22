-- Fasoncu ve şoför yönetimi: uygulama rolü ekler ve günceller (yetki uç noktada MASTER_DATA_MANAGE ile verilir).
-- Kayıt silinmez; id, oluşturan ve oluşma zamanı değişmez, yalnızca ad ve etkinlik durumu değişir. Her değişiklik
-- audit_local'e app.actor_id ile birlikte düşer.

GRANT INSERT, UPDATE ON md_subcontractor, md_driver TO ${app_role};

CREATE TRIGGER md_subcontractor_guard
    AFTER UPDATE OR DELETE ON md_subcontractor
    FOR EACH ROW EXECUTE FUNCTION guard_immutable_columns('title', 'active');

CREATE TRIGGER md_subcontractor_audit_local
    AFTER INSERT OR UPDATE OR DELETE ON md_subcontractor
    FOR EACH ROW EXECUTE FUNCTION audit_local_capture();

CREATE TRIGGER md_driver_guard
    AFTER UPDATE OR DELETE ON md_driver
    FOR EACH ROW EXECUTE FUNCTION guard_immutable_columns('full_name', 'subcontractor_id', 'active');

CREATE TRIGGER md_driver_audit_local
    AFTER INSERT OR UPDATE OR DELETE ON md_driver
    FOR EACH ROW EXECUTE FUNCTION audit_local_capture();
