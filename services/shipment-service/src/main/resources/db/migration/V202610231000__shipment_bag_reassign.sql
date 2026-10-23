-- Taslaktan çıkarılan çuval (removed_at dolu) aynı sevkiyata yeniden atanabilmeli. removed_at bir kez yazılır ve
-- değiştirilemez; bu yüzden benzersizlik yalnızca aktif satırlar için geçerlidir.
ALTER TABLE shipment_bag DROP CONSTRAINT shipment_bag_shipment_id_bag_id_key;

CREATE UNIQUE INDEX shipment_bag_active_uq ON shipment_bag (shipment_id, bag_id) WHERE removed_at IS NULL;
