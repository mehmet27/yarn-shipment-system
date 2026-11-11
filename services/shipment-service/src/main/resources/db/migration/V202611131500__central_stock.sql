-- Merkez iplik stoku: merkeze giren iplik (+), fasoncuya sevk edilen (-) ve merkez fabrikada kumaşa dönüşen (-)
-- hareketlerin toplamıdır. Yalnız ekleme; hata ters hareketle düzeltilir. net_kg işaretlidir.
CREATE TABLE central_production (
    id          uuid           PRIMARY KEY,
    fabric_type text           NOT NULL CHECK (length(btrim(fabric_type)) > 0),
    net_kg      numeric(12, 3) NOT NULL CHECK (net_kg > 0),
    fire_rate   numeric(5, 4)  NOT NULL CHECK (fire_rate >= 0 AND fire_rate <= 0.2),
    roll_count  integer        CHECK (roll_count IS NULL OR roll_count > 0),
    waybill_no  text,
    produced_at timestamptz    NOT NULL,
    created_by  uuid           NOT NULL
);

COMMENT ON TABLE central_production IS 'Merkez fabrikanın ürettiği kumaş; tükettiği iplik central_stock_movement içinde fire ile birlikte düşer';

CREATE TABLE central_stock_movement (
    id            uuid           PRIMARY KEY,
    yarn_type     text           NOT NULL CHECK (length(btrim(yarn_type)) > 0),
    kind          text           NOT NULL CHECK (kind IN ('YARN_RECEIPT', 'TO_SUBCONTRACTOR', 'PRODUCTION')),
    net_kg        numeric(12, 3) NOT NULL CHECK (net_kg <> 0),
    shipment_id   uuid           REFERENCES shipment (id),
    production_id uuid           REFERENCES central_production (id),
    fabric_kg     numeric(12, 3),
    note          text,
    occurred_at   timestamptz    NOT NULL,
    created_by    uuid           NOT NULL,
    source_ref    text           UNIQUE,
    CONSTRAINT central_stock_sign CHECK (
        (kind = 'YARN_RECEIPT' AND net_kg > 0) OR (kind IN ('TO_SUBCONTRACTOR', 'PRODUCTION') AND net_kg < 0))
);

COMMENT ON TABLE central_stock_movement IS 'Merkez iplik stoku defteri; yalnız ekleme';
COMMENT ON COLUMN central_stock_movement.fabric_kg IS 'PRODUCTION satırında bu iplikten çıkan kumaş kilosu (fire düşülmeden)';

CREATE INDEX central_stock_movement_yarn_idx ON central_stock_movement (yarn_type, occurred_at DESC);

CREATE TRIGGER central_stock_movement_append_only
    BEFORE UPDATE OR DELETE ON central_stock_movement
    FOR EACH ROW EXECUTE FUNCTION reject_modification();

CREATE TRIGGER central_production_append_only
    BEFORE UPDATE OR DELETE ON central_production
    FOR EACH ROW EXECUTE FUNCTION reject_modification();

GRANT SELECT, INSERT ON central_stock_movement, central_production TO ${app_role};
