-- Süreç analizi v2, Bölüm 5.4 ve 6.2: kantar uyuşmazlığı. Çıkış tartısı beklenen net kg'yi (tolerans dahil)
-- tutmazsa sevkiyat WEIGHT_MISMATCH olur ve müdür karar verene kadar yola çıkamaz.

ALTER TABLE shipment DROP CONSTRAINT shipment_status_check;
ALTER TABLE shipment ADD CONSTRAINT shipment_status_check
    CHECK (status IN ('DRAFT', 'WEIGHT_MISMATCH', 'AWAITING_DELIVERY', 'COMPLETED', 'CANCELLED'));


-- ShipmentStatus.nextStatuses ile birebir aynı.
CREATE OR REPLACE FUNCTION guard_shipment_update()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.locked_at IS NOT NULL AND nullif(current_setting('app.correction_record_id', true), '') IS NULL THEN
        RAISE EXCEPTION 'Shipment is locked shipmentId=%', OLD.id USING ERRCODE = 'YS003';
    END IF;
    IF NEW.status IS DISTINCT FROM OLD.status AND NOT (OLD.status, NEW.status) IN (
            ('DRAFT', 'AWAITING_DELIVERY'), ('DRAFT', 'WEIGHT_MISMATCH'), ('DRAFT', 'CANCELLED'),
            ('WEIGHT_MISMATCH', 'DRAFT'), ('WEIGHT_MISMATCH', 'AWAITING_DELIVERY'),
            ('WEIGHT_MISMATCH', 'CANCELLED'),
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

CREATE TABLE weight_check (
    id              uuid           PRIMARY KEY,
    shipment_id     uuid           NOT NULL REFERENCES shipment (id),
    weighing_id     uuid           NOT NULL REFERENCES weighing (id),
    empty_kg        numeric(12, 3) NOT NULL CHECK (empty_kg >= 0),
    loaded_kg       numeric(12, 3) NOT NULL CHECK (loaded_kg >= empty_kg),
    expected_kg     numeric(12, 3) NOT NULL CHECK (expected_kg >= 0),
    diff_kg         numeric(12, 3) NOT NULL,
    tolerance_kg    numeric(12, 3) NOT NULL CHECK (tolerance_kg >= 0),
    shortfall_reason text          CHECK (shortfall_reason IS NULL OR length(btrim(shortfall_reason)) > 0),
    decision        text           CHECK (decision IN ('CANCEL', 'ADD_REMOVE_BAGS', 'UPDATE_RECIPE')),
    decided_by      uuid,
    decided_at      timestamptz,
    reason          text           CHECK (reason IS NULL OR length(btrim(reason)) > 0),
    created_at      timestamptz    NOT NULL,
    created_by      uuid           NOT NULL,
    CONSTRAINT weight_check_decision_complete CHECK (
        (decision IS NULL AND decided_by IS NULL AND decided_at IS NULL AND reason IS NULL)
        OR (decision IS NOT NULL AND decided_by IS NOT NULL AND decided_at IS NOT NULL AND reason IS NOT NULL))
);

COMMENT ON TABLE weight_check IS 'Kantar uyuşmazlığı kaydı: boş/dolu araç tartısı, beklenen, fark ve müdür kararı';
COMMENT ON COLUMN weight_check.diff_kg IS 'Tartılan net - beklenen net (işaretli)';

CREATE INDEX weight_check_shipment_idx ON weight_check (shipment_id, created_at DESC);

CREATE TRIGGER weight_check_lock
    BEFORE INSERT OR UPDATE ON weight_check
    FOR EACH ROW EXECUTE FUNCTION guard_locked_shipment_child();

CREATE TRIGGER weight_check_no_delete
    BEFORE DELETE ON weight_check
    FOR EACH ROW EXECUTE FUNCTION reject_modification();

CREATE TRIGGER weight_check_audit_local
    AFTER INSERT OR UPDATE OR DELETE ON weight_check
    FOR EACH ROW EXECUTE FUNCTION audit_local_capture();

GRANT SELECT, INSERT, UPDATE ON weight_check TO ${app_role};
