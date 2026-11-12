-- Kapıdan aktarma: iplik depoya girmeden kamyondan kamyona yüklenir. Sevkiyat tedarikçi ve geliş irsaliyesi ile işaretlenir;
-- çıkış tartısında merkez stok defterine aynı sevkiyata bağlı giriş (+) ve çıkış (-) yazılır, stok eksiye düşmez.
ALTER TABLE shipment
    ADD COLUMN direct_transfer_supplier   text,
    ADD COLUMN direct_transfer_waybill_no text,
    ADD CONSTRAINT shipment_direct_transfer_central CHECK (
        direct_transfer_supplier IS NULL OR source_subcontractor_id IS NULL),
    ADD CONSTRAINT shipment_direct_transfer_supplier_text CHECK (
        direct_transfer_supplier IS NULL OR length(btrim(direct_transfer_supplier)) > 0),
    ADD CONSTRAINT shipment_direct_transfer_waybill_needs_supplier CHECK (
        direct_transfer_waybill_no IS NULL OR direct_transfer_supplier IS NOT NULL);

COMMENT ON COLUMN shipment.direct_transfer_supplier IS 'Doluysa iplik merkeze girmeden kapıdan aktarılır; gelen tedarikçi';
