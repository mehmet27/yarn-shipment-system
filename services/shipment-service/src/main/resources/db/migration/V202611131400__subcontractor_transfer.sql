-- Fasoncudan fasoncuya sevkiyat: iplik merkezden değil, bir fasoncunun elindeki stoktan başka fasoncuya gider.
-- Gönderen fasoncu boşsa sevkiyat merkezdendir. Alıcı fasoncu bakiyesi her zamanki gibi DISPATCH ile artar; gönderenin
-- bakiyesi TRANSFER_OUT ile aynı kilo kadar düşer.
ALTER TABLE shipment
    ADD COLUMN source_subcontractor_id    uuid,
    ADD COLUMN source_subcontractor_title text,
    ADD CONSTRAINT shipment_source_pair CHECK (
        (source_subcontractor_id IS NULL) = (source_subcontractor_title IS NULL)),
    ADD CONSTRAINT shipment_source_differs CHECK (
        source_subcontractor_id IS NULL OR source_subcontractor_id <> subcontractor_id);

COMMENT ON COLUMN shipment.source_subcontractor_id IS 'Gönderen fasoncu; boşsa iplik merkezden çıkar';

ALTER TABLE balance_movement DROP CONSTRAINT balance_movement_kind_check;
ALTER TABLE balance_movement DROP CONSTRAINT balance_movement_sign;
ALTER TABLE balance_movement
    ADD CONSTRAINT balance_movement_kind_check CHECK (kind IN (
        'DISPATCH', 'FABRIC_RECEIPT', 'YARN_RETURN', 'YARN_FROM_SUBCONTRACTOR', 'TRANSFER_OUT', 'CLAIM_ADJUSTMENT',
        'OPENING', 'CORRECTION')),
    ADD CONSTRAINT balance_movement_sign CHECK (
        (kind = 'DISPATCH' AND net_kg > 0)
        OR (kind IN ('FABRIC_RECEIPT', 'YARN_RETURN', 'YARN_FROM_SUBCONTRACTOR', 'TRANSFER_OUT') AND net_kg < 0)
        OR kind IN ('CLAIM_ADJUSTMENT', 'OPENING', 'CORRECTION'));
