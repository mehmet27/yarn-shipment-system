-- Sade sevkiyat akışı: şoför, varış ve teslim karşılaştırması kaldırıldı. Sevkiyat DRAFT → AWAITING_DELIVERY →
-- COMPLETED (ya da CANCELLED) ilerler. Okuma modeli olaylardan yeniden kurulabilir; eski durumlar yeni akışa
-- çevrilir, şoför ve eşleşme verisi silinir.

-- shipment-service ile aynı eşleme: çıkış tartımı yapılmış her ara durum fasoncuya yolda sayılır.
UPDATE shipment_view SET status = 'AWAITING_DELIVERY' WHERE status IN ('LOADED', 'IN_TRANSIT', 'CHECKING', 'IN_REVIEW');
UPDATE shipment_view SET delivery_net_kg = dispatch_net_kg WHERE status = 'COMPLETED' AND delivery_net_kg IS NULL;

DROP INDEX IF EXISTS shipment_view_driver_idx;
ALTER TABLE shipment_view DROP COLUMN driver_id;

DROP TABLE pair_daily_stats;

COMMENT ON COLUMN shipment_view.status IS 'Sevkiyat durumu: DRAFT, AWAITING_DELIVERY, COMPLETED, CANCELLED';
COMMENT ON COLUMN shipment_view.delivery_net_kg IS 'Fasoncuya ulaşan net kilo; sevkiyat tamamlanınca çıkış netine eşitlenir';
