-- Araç ve lokasyon yönetimi: uygulama rolü ekler ve günceller (yetki uç noktada MASTER_DATA_MANAGE ile verilir).
-- Kayıt silinmez; yalnızca plaka/ad ve etkinlik durumu değişir. Her değişiklik audit_local'e app.actor_id ile düşer.

GRANT INSERT, UPDATE ON md_vehicle, md_site TO ${app_role};

CREATE TRIGGER md_vehicle_guard
    AFTER UPDATE OR DELETE ON md_vehicle
    FOR EACH ROW EXECUTE FUNCTION guard_immutable_columns('plate_no', 'active');

CREATE TRIGGER md_vehicle_audit_local
    AFTER INSERT OR UPDATE OR DELETE ON md_vehicle
    FOR EACH ROW EXECUTE FUNCTION audit_local_capture();

CREATE TRIGGER md_site_guard
    AFTER UPDATE OR DELETE ON md_site
    FOR EACH ROW EXECUTE FUNCTION guard_immutable_columns('name', 'active');

CREATE TRIGGER md_site_audit_local
    AFTER INSERT OR UPDATE OR DELETE ON md_site
    FOR EACH ROW EXECUTE FUNCTION audit_local_capture();
