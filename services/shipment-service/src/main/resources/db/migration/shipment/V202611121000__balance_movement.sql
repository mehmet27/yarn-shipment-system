-- Fasoncu bakiye defteri (analiz 5.7, KR-13): yalnız ekleme. Bakiye hareketlerin toplamıdır; hareket değiştirilmez,
-- silinmez, hata ters hareketle (CORRECTION) düzeltilir. net_kg işaretlidir (+ fasoncuya giden, - düşen).
CREATE TABLE balance_movement (
    id               uuid           PRIMARY KEY,
    subcontractor_id uuid           NOT NULL,
    yarn_type        text           NOT NULL CHECK (length(btrim(yarn_type)) > 0),
    lot_id           uuid,
    shipment_id      uuid           REFERENCES shipment (id),
    kind             text           NOT NULL CHECK (kind IN (
        'DISPATCH', 'FABRIC_RECEIPT', 'YARN_RETURN', 'YARN_FROM_SUBCONTRACTOR', 'CLAIM_ADJUSTMENT', 'OPENING',
        'CORRECTION')),
    net_kg           numeric(12, 3) NOT NULL CHECK (net_kg <> 0),
    occurred_at      timestamptz    NOT NULL,
    created_by       uuid           NOT NULL,
    reason           text,
    reverses_id      uuid           REFERENCES balance_movement (id),
    source_ref       text           UNIQUE,
    CONSTRAINT balance_movement_sign CHECK (
        (kind = 'DISPATCH' AND net_kg > 0) OR (kind IN ('FABRIC_RECEIPT', 'YARN_RETURN', 'YARN_FROM_SUBCONTRACTOR')
            AND net_kg < 0) OR kind IN ('CLAIM_ADJUSTMENT', 'OPENING', 'CORRECTION')),
    CONSTRAINT balance_movement_correction_reason CHECK (
        kind <> 'CORRECTION' OR length(btrim(coalesce(reason, ''))) > 0)
);

COMMENT ON TABLE balance_movement IS 'Fasoncu bakiye defteri; yalnız ekleme (KR-13). source_ref aynı kaynağın tekrar yazılmasını önler';
COMMENT ON COLUMN balance_movement.source_ref IS 'Kaynağa özgü tekrar anahtarı, ör. DISPATCH:{sevkiyat}:{satır}';

CREATE INDEX balance_movement_subcontractor_idx ON balance_movement (subcontractor_id, yarn_type, occurred_at DESC);
CREATE INDEX balance_movement_shipment_idx ON balance_movement (shipment_id, kind);

CREATE TRIGGER balance_movement_append_only
    BEFORE UPDATE OR DELETE ON balance_movement
    FOR EACH ROW EXECUTE FUNCTION reject_modification();

GRANT SELECT, INSERT ON balance_movement TO ${app_role};
