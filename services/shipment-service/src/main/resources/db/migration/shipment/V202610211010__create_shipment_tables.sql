-- shipment_db: sevkiyat, tartım, mühür, şoför beyanı, kör teslim girişi (veritabanı tasarımı, "shipment_db").
-- Başka servislerin kayıtları yalnızca kimlik ve anlık görüntü kolonlarıyla tutulur, FK yoktur.

CREATE TABLE shipment (
    id                        uuid           PRIMARY KEY,
    site_id                   uuid           NOT NULL,
    shipment_no               text           NOT NULL UNIQUE,
    subcontractor_id          uuid           NOT NULL,
    subcontractor_title       text           NOT NULL,
    driver_id                 uuid,
    driver_name               text,
    vehicle_id                uuid,
    plate_no                  text,
    status                    text           NOT NULL CHECK (status IN (
        'DRAFT', 'LOADED', 'IN_TRANSIT', 'AWAITING_DELIVERY', 'CHECKING', 'IN_REVIEW', 'COMPLETED', 'CANCELLED')),
    planned_net_kg            numeric(12, 3) CHECK (planned_net_kg >= 0),
    planned_bag_count         integer        CHECK (planned_bag_count >= 0),
    e_waybill_no              text,
    random_check_selected     boolean        NOT NULL DEFAULT false,
    random_check_revealed_at  timestamptz,
    locked_at                 timestamptz,
    cancelled_reason          text,
    created_at                timestamptz    NOT NULL,
    created_by                uuid           NOT NULL,
    updated_at                timestamptz,
    updated_by                uuid,
    version                   integer        NOT NULL DEFAULT 0,
    CONSTRAINT shipment_driver_snapshot CHECK ((driver_id IS NULL) = (driver_name IS NULL)),
    CONSTRAINT shipment_vehicle_snapshot CHECK ((vehicle_id IS NULL) = (plate_no IS NULL)),
    CONSTRAINT shipment_locked_only_when_completed CHECK (locked_at IS NULL OR status = 'COMPLETED'),
    CONSTRAINT shipment_cancel_reason_required
        CHECK (status <> 'CANCELLED' OR length(btrim(coalesce(cancelled_reason, ''))) > 0)
);

COMMENT ON TABLE shipment IS 'Sevkiyat; COMPLETED olunca locked_at dolar ve bağlı kayıtlar kilitlenir';
COMMENT ON COLUMN shipment.site_id IS 'Çıkış lokasyonu (GEN-07)';
COMMENT ON COLUMN shipment.subcontractor_title IS 'Oluşturma anındaki fasoncu unvanı (anlık görüntü)';

CREATE INDEX shipment_site_status_created_idx ON shipment (site_id, status, created_at DESC);
CREATE INDEX shipment_subcontractor_status_idx ON shipment (subcontractor_id, status);
CREATE INDEX shipment_driver_status_idx ON shipment (driver_id, status);

-- Rapor 7.2: izin verilen durum geçişleri, ShipmentStatus.canTransitionTo ile birebir aynıdır.
CREATE FUNCTION guard_shipment_update()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.locked_at IS NOT NULL AND nullif(current_setting('app.correction_record_id', true), '') IS NULL THEN
        RAISE EXCEPTION 'Shipment is locked shipmentId=%', OLD.id USING ERRCODE = 'YS003';
    END IF;
    IF NEW.status IS DISTINCT FROM OLD.status AND NOT (OLD.status, NEW.status) IN (
            ('DRAFT', 'LOADED'), ('DRAFT', 'CANCELLED'), ('LOADED', 'IN_TRANSIT'), ('LOADED', 'CANCELLED'),
            ('IN_TRANSIT', 'AWAITING_DELIVERY'), ('AWAITING_DELIVERY', 'CHECKING'),
            ('CHECKING', 'COMPLETED'), ('CHECKING', 'IN_REVIEW'), ('IN_REVIEW', 'COMPLETED')) THEN
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

CREATE TRIGGER shipment_guard
    BEFORE UPDATE ON shipment
    FOR EACH ROW EXECUTE FUNCTION guard_shipment_update();

CREATE TRIGGER shipment_no_delete
    BEFORE DELETE ON shipment
    FOR EACH ROW EXECUTE FUNCTION reject_modification();

CREATE TRIGGER shipment_audit_local
    AFTER INSERT OR UPDATE OR DELETE ON shipment
    FOR EACH ROW EXECUTE FUNCTION audit_local_capture();

-- Kilitli sevkiyata bağlı satırlar (tartım, mühür, çuval, beyan, teslim) eklenemez, değiştirilemez,
-- silinemez. Tek istisna Admin onaylı düzeltmedir: uygulama transaction başında
-- app.correction_record_id ayarlar ve yalnızca yeni satır ekleyip eskisini superseded_by ile işaretler.
CREATE FUNCTION guard_locked_shipment_child()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
DECLARE
    target_shipment_id uuid := CASE WHEN TG_OP = 'DELETE' THEN OLD.shipment_id ELSE NEW.shipment_id END;
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'Rows cannot be deleted table=%', TG_TABLE_NAME USING ERRCODE = 'YS001';
    END IF;
    IF nullif(current_setting('app.correction_record_id', true), '') IS NOT NULL THEN
        RETURN NEW;
    END IF;
    IF EXISTS (SELECT 1 FROM shipment s WHERE s.id = target_shipment_id AND s.locked_at IS NOT NULL) THEN
        RAISE EXCEPTION 'Shipment is locked shipmentId=% table=%', target_shipment_id, TG_TABLE_NAME
            USING ERRCODE = 'YS003';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TABLE shipment_line (
    id                 uuid           PRIMARY KEY,
    shipment_id        uuid           NOT NULL REFERENCES shipment (id),
    product_id         uuid           NOT NULL,
    lot_id             uuid,
    quality_grade_code text,
    planned_kg         numeric(12, 3) NOT NULL CHECK (planned_kg >= 0),
    planned_bag_count  integer        NOT NULL CHECK (planned_bag_count >= 0),
    created_at         timestamptz    NOT NULL,
    created_by         uuid           NOT NULL,
    updated_at         timestamptz,
    updated_by         uuid,
    version            integer        NOT NULL DEFAULT 0
);

COMMENT ON TABLE shipment_line IS 'Sevkiyat planı (ürün, parti, kalite bazında)';

CREATE INDEX shipment_line_shipment_idx ON shipment_line (shipment_id);

CREATE TRIGGER shipment_line_lock
    BEFORE INSERT OR UPDATE OR DELETE ON shipment_line
    FOR EACH ROW EXECUTE FUNCTION guard_locked_shipment_child();

CREATE TABLE shipment_bag (
    id                 uuid        PRIMARY KEY,
    shipment_id        uuid        NOT NULL REFERENCES shipment (id),
    bag_id             uuid        NOT NULL,
    unique_key         text        NOT NULL,
    lot_no             text        NOT NULL,
    quality_grade_code text        NOT NULL,
    scan_method        text        NOT NULL CHECK (scan_method IN ('SCAN', 'LIST', 'MANUAL')),
    assigned_at        timestamptz NOT NULL,
    assigned_by        uuid        NOT NULL,
    removed_at         timestamptz,
    removed_by         uuid,
    created_at         timestamptz NOT NULL,
    created_by         uuid        NOT NULL,
    updated_at         timestamptz,
    updated_by         uuid,
    version            integer     NOT NULL DEFAULT 0,
    UNIQUE (shipment_id, bag_id),
    CONSTRAINT shipment_bag_removal_complete CHECK ((removed_at IS NULL) = (removed_by IS NULL))
);

COMMENT ON TABLE shipment_bag IS 'Sevkiyattaki çuvallar; bag-service rezervasyonu başarılı olunca yazılır (kopya, FK yok)';

CREATE TRIGGER shipment_bag_lock
    BEFORE INSERT OR UPDATE OR DELETE ON shipment_bag
    FOR EACH ROW EXECUTE FUNCTION guard_locked_shipment_child();

CREATE TRIGGER shipment_bag_guard
    AFTER UPDATE ON shipment_bag
    FOR EACH ROW EXECUTE FUNCTION guard_immutable_columns('removed_at', 'removed_by');

CREATE TRIGGER shipment_bag_audit_local
    AFTER INSERT OR UPDATE OR DELETE ON shipment_bag
    FOR EACH ROW EXECUTE FUNCTION audit_local_capture();

CREATE TABLE weighing (
    id              uuid           PRIMARY KEY,
    shipment_id     uuid           NOT NULL REFERENCES shipment (id),
    side            text           NOT NULL CHECK (side IN ('DISPATCH', 'DELIVERY')),
    gross_kg        numeric(12, 3) NOT NULL,
    tare_kg         numeric(12, 3) NOT NULL,
    net_kg          numeric(12, 3) GENERATED ALWAYS AS (gross_kg - tare_kg) STORED,
    bag_count       integer        NOT NULL CHECK (bag_count > 0),
    scale_ticket_no text,
    weighed_at      timestamptz    NOT NULL,
    entered_by      uuid           NOT NULL,
    superseded_by   uuid           REFERENCES weighing (id),
    created_at      timestamptz    NOT NULL,
    created_by      uuid           NOT NULL,
    updated_at      timestamptz,
    updated_by      uuid,
    version         integer        NOT NULL DEFAULT 0,
    CONSTRAINT weighing_gross_tare CHECK (gross_kg >= tare_kg AND tare_kg >= 0)
);

COMMENT ON TABLE weighing IS 'Tartım; hiç güncellenmez, düzeltme yeni satır ekler (DOM-16). net = brüt − dara';

-- Her taraf için tek geçerli tartım. Düzeltmede yeni satır eklenip eskisi aynı transaction'da
-- superseded_by ile işaretlendiği için kısıt commit anında denetlenir (DEFERRABLE).
ALTER TABLE weighing ADD CONSTRAINT weighing_current_uq
    EXCLUDE USING btree (shipment_id WITH =, side WITH =) WHERE (superseded_by IS NULL)
    DEFERRABLE INITIALLY DEFERRED;

CREATE TRIGGER weighing_lock
    BEFORE INSERT OR UPDATE OR DELETE ON weighing
    FOR EACH ROW EXECUTE FUNCTION guard_locked_shipment_child();

CREATE TRIGGER weighing_guard
    AFTER UPDATE ON weighing
    FOR EACH ROW EXECUTE FUNCTION guard_immutable_columns('superseded_by');

CREATE TRIGGER weighing_audit_local
    AFTER INSERT OR UPDATE OR DELETE ON weighing
    FOR EACH ROW EXECUTE FUNCTION audit_local_capture();

CREATE TABLE seal (
    id             uuid        PRIMARY KEY,
    shipment_id    uuid        NOT NULL REFERENCES shipment (id),
    side           text        NOT NULL CHECK (side IN ('DISPATCH', 'DELIVERY')),
    seal_no        text        NOT NULL,
    seal_condition text        NOT NULL CHECK (seal_condition IN ('INTACT', 'DAMAGED', 'MISSING')),
    recorded_by    uuid        NOT NULL,
    recorded_at    timestamptz NOT NULL,
    superseded_by  uuid        REFERENCES seal (id),
    created_at     timestamptz NOT NULL,
    created_by     uuid        NOT NULL,
    updated_at     timestamptz,
    updated_by     uuid,
    version        integer     NOT NULL DEFAULT 0
);

COMMENT ON TABLE seal IS 'Mühür; hiç güncellenmez, düzeltme yeni satır ekler (DOM-16)';

ALTER TABLE seal ADD CONSTRAINT seal_current_uq
    EXCLUDE USING btree (shipment_id WITH =, side WITH =) WHERE (superseded_by IS NULL)
    DEFERRABLE INITIALLY DEFERRED;
-- Mühür tek kullanımlıktır: aynı numara başka bir sevkiyatın çıkışında kullanılamaz.
ALTER TABLE seal ADD CONSTRAINT seal_dispatch_no_uq
    EXCLUDE USING btree (seal_no WITH =) WHERE (side = 'DISPATCH' AND superseded_by IS NULL)
    DEFERRABLE INITIALLY DEFERRED;

CREATE TRIGGER seal_lock
    BEFORE INSERT OR UPDATE OR DELETE ON seal
    FOR EACH ROW EXECUTE FUNCTION guard_locked_shipment_child();

CREATE TRIGGER seal_guard
    AFTER UPDATE ON seal
    FOR EACH ROW EXECUTE FUNCTION guard_immutable_columns('superseded_by');

CREATE TRIGGER seal_audit_local
    AFTER INSERT OR UPDATE OR DELETE ON seal
    FOR EACH ROW EXECUTE FUNCTION audit_local_capture();

CREATE TABLE driver_declaration (
    id               uuid        PRIMARY KEY,
    shipment_id      uuid        NOT NULL REFERENCES shipment (id),
    declaration_type text        NOT NULL CHECK (declaration_type IN ('PICKUP', 'ARRIVAL')),
    driver_id        uuid        NOT NULL,
    bag_count        integer     NOT NULL CHECK (bag_count >= 0),
    seal_no          text,
    seal_intact      boolean,
    declared_at      timestamptz NOT NULL,
    device_info      jsonb,
    created_at       timestamptz NOT NULL,
    created_by       uuid        NOT NULL,
    updated_at       timestamptz,
    updated_by       uuid,
    version          integer     NOT NULL DEFAULT 0,
    UNIQUE (shipment_id, declaration_type)
);

COMMENT ON TABLE driver_declaration IS 'Şoför beyanı (teslim alma ve varış); değiştirilemez';

CREATE TRIGGER driver_declaration_lock
    BEFORE INSERT OR UPDATE OR DELETE ON driver_declaration
    FOR EACH ROW EXECUTE FUNCTION guard_locked_shipment_child();

CREATE TRIGGER driver_declaration_guard
    AFTER UPDATE ON driver_declaration
    FOR EACH ROW EXECUTE FUNCTION guard_immutable_columns();

CREATE TRIGGER driver_declaration_audit_local
    AFTER INSERT OR UPDATE OR DELETE ON driver_declaration
    FOR EACH ROW EXECUTE FUNCTION audit_local_capture();

CREATE TABLE delivery_receipt (
    id                     uuid           PRIMARY KEY,
    shipment_id            uuid           NOT NULL UNIQUE REFERENCES shipment (id),
    received_by            uuid           NOT NULL,
    received_at            timestamptz    NOT NULL,
    signed_doc_evidence_id uuid,
    doc_shipment_no        text,
    doc_date               date,
    doc_net_kg             numeric(12, 3) CHECK (doc_net_kg >= 0),
    doc_bag_count          integer        CHECK (doc_bag_count >= 0),
    submitted_at           timestamptz,
    created_at             timestamptz    NOT NULL,
    created_by             uuid           NOT NULL,
    updated_at             timestamptz,
    updated_by             uuid,
    version                integer        NOT NULL DEFAULT 0
);

COMMENT ON TABLE delivery_receipt IS 'Fasoncunun kör teslim girişi; submitted_at dolunca kilitlenir';

CREATE FUNCTION guard_submitted_delivery_receipt()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.submitted_at IS NOT NULL THEN
        RAISE EXCEPTION 'Delivery receipt is submitted deliveryReceiptId=%', OLD.id USING ERRCODE = 'YS003';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER delivery_receipt_lock
    BEFORE INSERT OR UPDATE OR DELETE ON delivery_receipt
    FOR EACH ROW EXECUTE FUNCTION guard_locked_shipment_child();

CREATE TRIGGER delivery_receipt_submitted
    BEFORE UPDATE ON delivery_receipt
    FOR EACH ROW EXECUTE FUNCTION guard_submitted_delivery_receipt();

CREATE TRIGGER delivery_receipt_audit_local
    AFTER INSERT OR UPDATE OR DELETE ON delivery_receipt
    FOR EACH ROW EXECUTE FUNCTION audit_local_capture();

-- Teslimde okutulan anahtarlar. Listede olmayan anahtar da saklanır (matched = false).
-- (delivery_receipt_id, scanned_key) tekilliği partition'lı tabloda global olarak zorlanamadığı için
-- bu tablo partition'lanmaz; hacmi shipment_bag ile aynı düzeydedir.
CREATE TABLE delivery_scan (
    id                  uuid        PRIMARY KEY,
    delivery_receipt_id uuid        NOT NULL REFERENCES delivery_receipt (id),
    scanned_key         text        NOT NULL,
    matched             boolean     NOT NULL,
    scan_method         text        NOT NULL CHECK (scan_method IN ('SCAN', 'LIST', 'MANUAL')),
    scanned_at          timestamptz NOT NULL,
    created_at          timestamptz NOT NULL,
    created_by          uuid        NOT NULL,
    updated_at          timestamptz,
    updated_by          uuid,
    version             integer     NOT NULL DEFAULT 0,
    UNIQUE (delivery_receipt_id, scanned_key)
);

CREATE FUNCTION guard_delivery_scan_insert()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM delivery_receipt r
               WHERE r.id = NEW.delivery_receipt_id AND r.submitted_at IS NOT NULL) THEN
        RAISE EXCEPTION 'Delivery receipt is submitted deliveryReceiptId=%', NEW.delivery_receipt_id
            USING ERRCODE = 'YS003';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER delivery_scan_submitted
    BEFORE INSERT ON delivery_scan
    FOR EACH ROW EXECUTE FUNCTION guard_delivery_scan_insert();

CREATE TRIGGER delivery_scan_append_only
    BEFORE UPDATE OR DELETE ON delivery_scan
    FOR EACH ROW EXECUTE FUNCTION reject_modification();

CREATE TABLE shipment_status_history (
    id          uuid        NOT NULL,
    created_at  timestamptz NOT NULL,
    shipment_id uuid        NOT NULL,
    from_status text,
    to_status   text        NOT NULL,
    changed_by  uuid        NOT NULL,
    changed_at  timestamptz NOT NULL,
    reason      text,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

COMMENT ON TABLE shipment_status_history IS 'Durum geçmişi; yalnızca ekleme (DOM-14)';

CREATE INDEX shipment_status_history_shipment_idx ON shipment_status_history (shipment_id, changed_at);

CREATE TRIGGER shipment_status_history_append_only
    BEFORE UPDATE OR DELETE ON shipment_status_history
    FOR EACH ROW EXECUTE FUNCTION reject_modification();

CREATE TABLE correction_record (
    id           uuid        PRIMARY KEY,
    shipment_id  uuid        NOT NULL REFERENCES shipment (id),
    entity_type  text        NOT NULL CHECK (entity_type IN (
        'SHIPMENT', 'SHIPMENT_BAG', 'WEIGHING', 'SEAL', 'DRIVER_DECLARATION', 'DELIVERY_RECEIPT')),
    entity_id    uuid        NOT NULL,
    field_name   text        NOT NULL,
    old_value    text,
    new_value    text,
    reason       text        NOT NULL CHECK (length(btrim(reason)) > 0),
    status       text        NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    requested_by uuid        NOT NULL,
    approved_by  uuid,
    approved_at  timestamptz,
    created_at   timestamptz NOT NULL,
    created_by   uuid        NOT NULL,
    updated_at   timestamptz,
    updated_by   uuid,
    version      integer     NOT NULL DEFAULT 0,
    CONSTRAINT correction_record_decision_complete CHECK (
        (status = 'PENDING' AND approved_by IS NULL AND approved_at IS NULL)
        OR (status <> 'PENDING' AND approved_by IS NOT NULL AND approved_at IS NOT NULL))
);

COMMENT ON TABLE correction_record IS 'Düzeltme kaydı; gerekçe zorunlu, yalnızca Admin onaylar (rapor karar #9)';
COMMENT ON COLUMN correction_record.approved_by IS 'Onaylayan veya reddeden Admin';

CREATE INDEX correction_record_shipment_idx ON correction_record (shipment_id);

CREATE TRIGGER correction_record_guard
    AFTER UPDATE OR DELETE ON correction_record
    FOR EACH ROW EXECUTE FUNCTION guard_immutable_columns('status', 'approved_by', 'approved_at');

CREATE FUNCTION guard_correction_decided_once()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.status <> 'PENDING' THEN
        RAISE EXCEPTION 'Correction already decided correctionRecordId=%', OLD.id USING ERRCODE = 'YS002';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER correction_record_decided_once
    BEFORE UPDATE ON correction_record
    FOR EACH ROW EXECUTE FUNCTION guard_correction_decided_once();

CREATE TRIGGER correction_record_audit_local
    AFTER INSERT OR UPDATE OR DELETE ON correction_record
    FOR EACH ROW EXECUTE FUNCTION audit_local_capture();

GRANT SELECT, INSERT, UPDATE ON
    shipment, shipment_line, shipment_bag, weighing, seal, delivery_receipt, correction_record
    TO ${app_role};
GRANT SELECT, INSERT ON driver_declaration, delivery_scan, shipment_status_history TO ${app_role};

SELECT create_monthly_partitions('shipment_status_history', date '2026-10-01', 15);
