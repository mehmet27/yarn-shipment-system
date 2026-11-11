-- Çuval rezervasyonu kodu kaldırıldığından tablo hiçbir yerde okunmuyor; palet üst kaydı kolonu da hiç yazılmadı.
DROP TABLE IF EXISTS bag_reservation;

DROP TRIGGER IF EXISTS bag_guard ON bag;
ALTER TABLE bag DROP COLUMN IF EXISTS parent_bag_id;
CREATE TRIGGER bag_guard
    AFTER UPDATE OR DELETE ON bag
    FOR EACH ROW EXECUTE FUNCTION guard_immutable_columns('status');
