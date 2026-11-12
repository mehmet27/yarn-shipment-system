-- Saha analizi (10.11.2026): şoför beyanı, mühür, fasoncunun kör teslim girişi ve otomatik karşılaştırma kalkar.
-- Yeni akış: DRAFT → (depo çıkış tartımı) → AWAITING_DELIVERY → (müdür irsaliyeyi yükler) → COMPLETED.
-- DRAFT ve AWAITING_DELIVERY iptal edilebilir.
--
-- DİKKAT: Geri alınamaz. seal, driver_declaration, delivery_receipt, delivery_scan tabloları ve teslim tarafı
-- tartımları silinir. Eski ara durumlar (LOADED, IN_TRANSIT, CHECKING, IN_REVIEW) AWAITING_DELIVERY olur.

-- 1. Yeni durum geçişleri (ShipmentStatus ile birebir aynı).
CREATE OR REPLACE FUNCTION guard_shipment_update()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.locked_at IS NOT NULL AND nullif(current_setting('app.correction_record_id', true), '') IS NULL THEN
        RAISE EXCEPTION 'Shipment is locked shipmentId=%', OLD.id USING ERRCODE = 'YS003';
    END IF;
    IF NEW.status IS DISTINCT FROM OLD.status AND NOT (OLD.status, NEW.status) IN (
            ('DRAFT', 'AWAITING_DELIVERY'), ('DRAFT', 'CANCELLED'),
            ('AWAITING_DELIVERY', 'COMPLETED'), ('AWAITING_DELIVERY', 'CANCELLED')) THEN
        RAISE EXCEPTION 'Invalid shipment status transition shipmentId=% from=% to=%', OLD.id, OLD.status, NEW.status
            USING ERRCODE = 'YS003';
    END IF;
    IF NEW.id <> OLD.id OR NEW.shipment_no <> OLD.shipment_no OR NEW.site_id <> OLD.site_id
            OR NEW.created_at <> OLD.created_at OR NEW.created_by <> OLD.created_by THEN
        RAISE EXCEPTION 'Immutable column changed table=shipment id=%', OLD.id USING ERRCODE = 'YS002';
    END IF;
    RETURN NEW;
END;
$$;

-- 2. Eski ara durumdaki sevkiyatlar ve geçmişleri yeni duruma taşınır. Koruma tetikleyicileri bu adımda kapalıdır.
ALTER TABLE shipment DISABLE TRIGGER shipment_guard;
UPDATE shipment SET status = 'AWAITING_DELIVERY'
WHERE status IN ('LOADED', 'IN_TRANSIT', 'CHECKING', 'IN_REVIEW');
ALTER TABLE shipment ENABLE TRIGGER shipment_guard;

ALTER TABLE shipment_status_history DISABLE TRIGGER shipment_status_history_append_only;
UPDATE shipment_status_history SET from_status = 'AWAITING_DELIVERY'
WHERE from_status IN ('LOADED', 'IN_TRANSIT', 'CHECKING', 'IN_REVIEW');
UPDATE shipment_status_history SET to_status = 'AWAITING_DELIVERY'
WHERE to_status IN ('LOADED', 'IN_TRANSIT', 'CHECKING', 'IN_REVIEW');
-- Taşımadan sonra aynı durumda kalan (AWAITING_DELIVERY → AWAITING_DELIVERY) satırlar anlamsızdır.
DELETE FROM shipment_status_history WHERE from_status = to_status;
ALTER TABLE shipment_status_history ENABLE TRIGGER shipment_status_history_append_only;

ALTER TABLE shipment DROP CONSTRAINT shipment_status_check;
ALTER TABLE shipment ADD CONSTRAINT shipment_status_check
    CHECK (status IN ('DRAFT', 'AWAITING_DELIVERY', 'COMPLETED', 'CANCELLED'));

-- 3. Kaldırılan kayıtlara ait düzeltmeler ve teslim tartımları silinir.
ALTER TABLE correction_record DISABLE TRIGGER correction_record_guard;
DELETE FROM correction_record
WHERE entity_type IN ('SEAL', 'DRIVER_DECLARATION', 'DELIVERY_RECEIPT')
   OR (entity_type = 'WEIGHING' AND field_name = 'DELIVERY_WEIGHING');
ALTER TABLE correction_record ENABLE TRIGGER correction_record_guard;
ALTER TABLE correction_record DROP CONSTRAINT correction_record_entity_type_check;
ALTER TABLE correction_record ADD CONSTRAINT correction_record_entity_type_check
    CHECK (entity_type IN ('SHIPMENT', 'SHIPMENT_BAG', 'WEIGHING'));

ALTER TABLE weighing DISABLE TRIGGER weighing_lock;
ALTER TABLE weighing DISABLE TRIGGER weighing_guard;
UPDATE weighing SET superseded_by = NULL WHERE side = 'DELIVERY';
DELETE FROM weighing WHERE side = 'DELIVERY';
ALTER TABLE weighing ENABLE TRIGGER weighing_lock;
ALTER TABLE weighing ENABLE TRIGGER weighing_guard;
ALTER TABLE weighing DROP CONSTRAINT weighing_side_check;
ALTER TABLE weighing ADD CONSTRAINT weighing_side_check CHECK (side = 'DISPATCH');

-- 4. Kaldırılan tablolar ve fonksiyonlar.
DROP TABLE delivery_scan;
DROP TABLE delivery_receipt;
DROP TABLE driver_declaration;
DROP TABLE seal;
DROP FUNCTION guard_delivery_scan_insert();
DROP FUNCTION guard_submitted_delivery_receipt();

-- 5. Rastgele kontrol kör teslimle birlikte kalkar; tamamlamada yüklenen irsaliye belgesi saklanır.
ALTER TABLE shipment DROP COLUMN random_check_selected;
ALTER TABLE shipment DROP COLUMN random_check_revealed_at;
ALTER TABLE shipment ADD COLUMN waybill_evidence_id uuid;

COMMENT ON COLUMN shipment.e_waybill_no IS 'Tamamlarken müdürün girdiği irsaliye numarası';
COMMENT ON COLUMN shipment.waybill_evidence_id IS 'Tamamlarken yüklenen irsaliye belgesi (evidence-service, FK yok)';
