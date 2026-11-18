-- Parça parça fatura: müdür ERP'de kestiği faturayı iplik ve kilo vererek girer. Faturalanan kilo fasoncunun iplik
-- bakiyesinden düşer (INVOICE hareketi); kalan kilo bitene kadar fire dosyası açık kalır.
CREATE TABLE fire_invoice (
    id          uuid           PRIMARY KEY,
    shipment_id uuid           NOT NULL REFERENCES shipment (id),
    invoice_no  text           NOT NULL CHECK (length(btrim(invoice_no)) > 0),
    invoiced_on date           NOT NULL,
    note        text,
    kg          numeric(12, 3) NOT NULL CHECK (kg > 0),
    invoiced_by uuid           NOT NULL,
    created_at  timestamptz    NOT NULL,
    UNIQUE (shipment_id, invoice_no)
);

CREATE TRIGGER fire_invoice_append_only
    BEFORE UPDATE OR DELETE ON fire_invoice
    FOR EACH ROW EXECUTE FUNCTION reject_modification();

ALTER TABLE balance_movement DROP CONSTRAINT balance_movement_kind_check;
ALTER TABLE balance_movement DROP CONSTRAINT balance_movement_sign;
ALTER TABLE balance_movement
    ADD CONSTRAINT balance_movement_kind_check CHECK (kind IN (
        'DISPATCH', 'FABRIC_RECEIPT', 'YARN_RETURN', 'YARN_FROM_SUBCONTRACTOR', 'TRANSFER_OUT', 'CLAIM_ADJUSTMENT',
        'INVOICE', 'OPENING', 'CORRECTION')),
    ADD CONSTRAINT balance_movement_sign CHECK (
        (kind = 'DISPATCH' AND net_kg > 0)
        OR (kind IN ('FABRIC_RECEIPT', 'YARN_RETURN', 'YARN_FROM_SUBCONTRACTOR', 'TRANSFER_OUT', 'INVOICE')
            AND net_kg < 0)
        OR kind IN ('CLAIM_ADJUSTMENT', 'OPENING', 'CORRECTION'));

GRANT SELECT, INSERT ON fire_invoice TO ${app_role};
