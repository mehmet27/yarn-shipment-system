-- Saha analizi (10.11.2026): şoför, mühür ve fasoncunun kör teslim girişi kalktı. Müdür sevkiyatı tamamlarken
-- irsaliye belgesini (WAYBILL) yükler.
--
-- DİKKAT: Geri alınamaz. Kaldırılan türlerin (VEHICLE_PLATE, SEAL, DELIVERY_SCALE, DELIVERY_BAGS,
-- SIGNED_DELIVERY_DOC) kayıtları silinir; nesne deposundaki dosyalar elle temizlenir.

DELETE FROM ticket_unverified WHERE evidence_type = 'DELIVERY_SCALE';
DELETE FROM ticket_reading WHERE evidence_type = 'DELIVERY_SCALE';
ALTER TABLE ticket_reading DROP CONSTRAINT ticket_reading_evidence_type_check;
ALTER TABLE ticket_reading ADD CONSTRAINT ticket_reading_evidence_type_check CHECK (evidence_type = 'DISPATCH_SCALE');
ALTER TABLE ticket_unverified DROP CONSTRAINT ticket_unverified_evidence_type_check;
ALTER TABLE ticket_unverified ADD CONSTRAINT ticket_unverified_evidence_type_check
    CHECK (evidence_type = 'DISPATCH_SCALE');

DELETE FROM upload_intent
WHERE evidence_type IN ('VEHICLE_PLATE', 'SEAL', 'DELIVERY_SCALE', 'DELIVERY_BAGS', 'SIGNED_DELIVERY_DOC');
ALTER TABLE upload_intent DROP CONSTRAINT upload_intent_evidence_type_check;
ALTER TABLE upload_intent ADD CONSTRAINT upload_intent_evidence_type_check CHECK (evidence_type IN (
    'DISPATCH_SCALE', 'BAG_OVERVIEW', 'BAG_LABEL', 'WAYBILL', 'QUALITY_CLAIM', 'LAB_RESULT'));

ALTER TABLE evidence_duplicate DISABLE TRIGGER evidence_duplicate_append_only;
DELETE FROM evidence_duplicate d
USING evidence_file f
WHERE f.id IN (d.evidence_id, d.duplicate_of_id)
  AND f.evidence_type IN ('VEHICLE_PLATE', 'SEAL', 'DELIVERY_SCALE', 'DELIVERY_BAGS', 'SIGNED_DELIVERY_DOC');
ALTER TABLE evidence_duplicate ENABLE TRIGGER evidence_duplicate_append_only;

ALTER TABLE evidence_file DISABLE TRIGGER evidence_file_guard;
DELETE FROM evidence_file
WHERE evidence_type IN ('VEHICLE_PLATE', 'SEAL', 'DELIVERY_SCALE', 'DELIVERY_BAGS', 'SIGNED_DELIVERY_DOC');
ALTER TABLE evidence_file ENABLE TRIGGER evidence_file_guard;
ALTER TABLE evidence_file DROP CONSTRAINT evidence_file_evidence_type_check;
ALTER TABLE evidence_file ADD CONSTRAINT evidence_file_evidence_type_check CHECK (evidence_type IN (
    'DISPATCH_SCALE', 'BAG_OVERVIEW', 'BAG_LABEL', 'WAYBILL', 'QUALITY_CLAIM', 'LAB_RESULT'));
