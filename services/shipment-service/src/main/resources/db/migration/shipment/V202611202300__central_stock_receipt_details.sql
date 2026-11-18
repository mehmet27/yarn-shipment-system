-- Merkeze iplik girişinde tedarikçi, parti ve tedarikçi irsaliye no tutulur: "hangi tedarikçiden ne kadar iplik aldık"
-- raporu ve sevkiyat / iplik dönüşündeki parti seçimi buradaki partiden beslenir. Eski satırlarda bu alanlar boştur.
-- supplier_name hareket anındaki adın kopyasıdır; tanıma bağlı olmayan kapıdan aktarma tedarikçisi de burada saklanır.
ALTER TABLE central_stock_movement
    ADD COLUMN supplier_id   uuid REFERENCES md_yarn_supplier (id),
    ADD COLUMN supplier_name text,
    ADD COLUMN lot_id        uuid REFERENCES md_production_lot (id),
    ADD COLUMN waybill_no    text,
    ADD CONSTRAINT central_stock_supplier_name_text CHECK (supplier_name IS NULL OR length(btrim(supplier_name)) > 0),
    ADD CONSTRAINT central_stock_waybill_text CHECK (waybill_no IS NULL OR length(btrim(waybill_no)) > 0),
    ADD CONSTRAINT central_stock_receipt_only_details CHECK (
        kind = 'YARN_RECEIPT' OR (supplier_id IS NULL AND supplier_name IS NULL AND lot_id IS NULL AND waybill_no IS NULL));

COMMENT ON COLUMN central_stock_movement.supplier_id IS 'İplik tedarikçisi tanımı; kapıdan aktarmada tanımla eşleşmezse boştur';
COMMENT ON COLUMN central_stock_movement.supplier_name IS 'Tedarikçinin hareket anındaki adı; yalnızca iplik girişinde dolu';
COMMENT ON COLUMN central_stock_movement.lot_id IS 'Girişte yazılan parti; sevkiyat ve iplik dönüşündeki parti seçimi bu partidir';
COMMENT ON COLUMN central_stock_movement.waybill_no IS 'Tedarikçinin irsaliye numarası';

CREATE INDEX central_stock_movement_supplier_idx ON central_stock_movement (supplier_name) WHERE kind = 'YARN_RECEIPT';
CREATE INDEX central_stock_movement_lot_idx ON central_stock_movement (lot_id) WHERE lot_id IS NOT NULL;
