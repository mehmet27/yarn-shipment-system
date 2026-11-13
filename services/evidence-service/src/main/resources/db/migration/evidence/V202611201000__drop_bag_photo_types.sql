-- Çuval fotoğrafı türleri (BAG_OVERVIEW, BAG_LABEL) kaldırıldı; hiçbir ekrandan yüklenmiyordu.
--
-- DİKKAT: Geri alınamaz. Bu türlerin varsa kayıtları silinir; nesne deposundaki dosyalar elle temizlenir.

DELETE FROM upload_intent WHERE evidence_type IN ('BAG_OVERVIEW', 'BAG_LABEL');
ALTER TABLE upload_intent DROP CONSTRAINT upload_intent_evidence_type_check;
ALTER TABLE upload_intent ADD CONSTRAINT upload_intent_evidence_type_check CHECK (evidence_type IN (
    'DISPATCH_SCALE', 'WAYBILL', 'QUALITY_CLAIM', 'LAB_RESULT'));

ALTER TABLE evidence_duplicate DISABLE TRIGGER evidence_duplicate_append_only;
DELETE FROM evidence_duplicate d
USING evidence_file f
WHERE f.id IN (d.evidence_id, d.duplicate_of_id)
  AND f.evidence_type IN ('BAG_OVERVIEW', 'BAG_LABEL');
ALTER TABLE evidence_duplicate ENABLE TRIGGER evidence_duplicate_append_only;

ALTER TABLE evidence_file DISABLE TRIGGER evidence_file_guard;
DELETE FROM evidence_file WHERE evidence_type IN ('BAG_OVERVIEW', 'BAG_LABEL');
ALTER TABLE evidence_file ENABLE TRIGGER evidence_file_guard;
ALTER TABLE evidence_file DROP CONSTRAINT evidence_file_evidence_type_check;
ALTER TABLE evidence_file ADD CONSTRAINT evidence_file_evidence_type_check CHECK (evidence_type IN (
    'DISPATCH_SCALE', 'WAYBILL', 'QUALITY_CLAIM', 'LAB_RESULT'));
