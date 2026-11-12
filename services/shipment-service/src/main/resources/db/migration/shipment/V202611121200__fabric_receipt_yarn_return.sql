-- Kumaş girişi (analiz 5.8) ve iplik dönüşü / fasoncudan gelen iplik (5.9). Kayıtlar yalnız eklenir.
CREATE TABLE fabric_receipt (
    id          uuid           PRIMARY KEY,
    shipment_id uuid           NOT NULL REFERENCES shipment (id),
    fabric_type text           NOT NULL CHECK (length(btrim(fabric_type)) > 0),
    net_kg      numeric(12, 3) NOT NULL CHECK (net_kg > 0),
    roll_count  integer        CHECK (roll_count > 0),
    waybill_no  text           NOT NULL CHECK (length(btrim(waybill_no)) > 0),
    received_at timestamptz    NOT NULL,
    created_at  timestamptz    NOT NULL DEFAULT now(),
    created_by  uuid           NOT NULL
);

CREATE INDEX fabric_receipt_shipment_idx ON fabric_receipt (shipment_id, received_at);

CREATE TABLE fabric_receipt_yarn (
    fabric_receipt_id uuid           NOT NULL REFERENCES fabric_receipt (id),
    yarn_type         text           NOT NULL CHECK (length(btrim(yarn_type)) > 0),
    net_kg            numeric(12, 3) NOT NULL CHECK (net_kg > 0),
    PRIMARY KEY (fabric_receipt_id, yarn_type)
);

CREATE TABLE yarn_return (
    id               uuid           PRIMARY KEY,
    shipment_id      uuid           NOT NULL REFERENCES shipment (id),
    subcontractor_id uuid           NOT NULL,
    direction        text           NOT NULL CHECK (direction IN ('RETURN', 'FROM_SUBCONTRACTOR')),
    yarn_type        text           NOT NULL CHECK (length(btrim(yarn_type)) > 0),
    lot_id           uuid,
    net_kg           numeric(12, 3) NOT NULL CHECK (net_kg > 0),
    waybill_no       text,
    created_at       timestamptz    NOT NULL,
    created_by       uuid           NOT NULL
);

CREATE INDEX yarn_return_shipment_idx ON yarn_return (shipment_id, created_at);

CREATE TRIGGER fabric_receipt_append_only
    BEFORE UPDATE OR DELETE ON fabric_receipt
    FOR EACH ROW EXECUTE FUNCTION reject_modification();
CREATE TRIGGER fabric_receipt_yarn_append_only
    BEFORE UPDATE OR DELETE ON fabric_receipt_yarn
    FOR EACH ROW EXECUTE FUNCTION reject_modification();
CREATE TRIGGER yarn_return_append_only
    BEFORE UPDATE OR DELETE ON yarn_return
    FOR EACH ROW EXECUTE FUNCTION reject_modification();

GRANT SELECT, INSERT ON fabric_receipt, fabric_receipt_yarn, yarn_return TO ${app_role};
