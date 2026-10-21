-- bag_db: çuval, etiket baskısı, rezervasyon (veritabanı tasarımı, "bag_db").
-- Unique Key'in tek kullanımı bu veritabanında, tek işlemle garanti edilir.

-- master-data-service'in LotQualityLocked olayından doldurulur. Çuval yalnızca burada kaydı olan
-- (kalitesi kilitli) partiye bağlanabilir ve kalitesini buradan kopyalar (rapor Bölüm 10).
CREATE TABLE lot_quality_lock (
    lot_id             uuid        PRIMARY KEY,
    site_id            uuid        NOT NULL,
    lot_no             text        NOT NULL,
    product_id         uuid        NOT NULL,
    quality_grade_code text        NOT NULL,
    locked_at          timestamptz NOT NULL,
    created_at         timestamptz NOT NULL,
    UNIQUE (lot_id, quality_grade_code)
);

COMMENT ON TABLE lot_quality_lock IS 'LotQualityLocked olayının yerel kopyası; kilit geri alınamaz';

CREATE TRIGGER lot_quality_lock_append_only
    BEFORE UPDATE OR DELETE ON lot_quality_lock
    FOR EACH ROW EXECUTE FUNCTION reject_modification();

CREATE TABLE bag (
    id                 uuid        PRIMARY KEY,
    site_id            uuid        NOT NULL,
    unique_key         text        NOT NULL UNIQUE,
    package_type       text        NOT NULL CHECK (package_type IN ('BAG', 'BOX', 'PALLET', 'BOBBIN')),
    parent_bag_id      uuid        REFERENCES bag (id),
    lot_id             uuid        NOT NULL,
    lot_no             text        NOT NULL,
    product_id         uuid        NOT NULL,
    quality_grade_code text        NOT NULL,
    status             text        NOT NULL CHECK (status IN ('CREATED', 'RESERVED', 'SHIPPED', 'DELIVERED', 'VOID')),
    created_at         timestamptz NOT NULL,
    created_by         uuid        NOT NULL,
    updated_at         timestamptz,
    updated_by         uuid,
    version            integer     NOT NULL DEFAULT 0,
    CONSTRAINT bag_quality_from_locked_lot
        FOREIGN KEY (lot_id, quality_grade_code) REFERENCES lot_quality_lock (lot_id, quality_grade_code),
    CONSTRAINT bag_not_own_parent CHECK (parent_bag_id IS NULL OR parent_bag_id <> id)
);

COMMENT ON TABLE bag IS 'Çuval / ambalaj birimi; kalite partiden kopyalanır, elle girilmez';
COMMENT ON COLUMN bag.parent_bag_id IS 'Palet içindeki çuvallar için palet kaydı';

CREATE INDEX bag_lot_status_idx ON bag (lot_id, status);
CREATE INDEX bag_site_status_idx ON bag (site_id, status);

-- Çuvalın kimliği, partisi ve kalitesi oluşturulduktan sonra değişmez; yalnızca durum ve palet bağı değişir.
CREATE TRIGGER bag_guard
    AFTER UPDATE OR DELETE ON bag
    FOR EACH ROW EXECUTE FUNCTION guard_immutable_columns('status', 'parent_bag_id');

CREATE TRIGGER bag_audit_local
    AFTER INSERT OR UPDATE OR DELETE ON bag
    FOR EACH ROW EXECUTE FUNCTION audit_local_capture();

CREATE TABLE label_print (
    id             uuid        PRIMARY KEY,
    bag_id         uuid        NOT NULL REFERENCES bag (id),
    print_no       integer     NOT NULL CHECK (print_no >= 1),
    printed_at     timestamptz NOT NULL,
    printed_by     uuid        NOT NULL,
    reprint_reason text,
    created_at     timestamptz NOT NULL,
    created_by     uuid        NOT NULL,
    updated_at     timestamptz,
    updated_by     uuid,
    version        integer     NOT NULL DEFAULT 0,
    UNIQUE (bag_id, print_no),
    CONSTRAINT label_print_reprint_reason_required
        CHECK (print_no = 1 OR length(btrim(coalesce(reprint_reason, ''))) > 0)
);

COMMENT ON TABLE label_print IS 'Etiket baskısı; ilk baskı print_no = 1, yeniden baskıda gerekçe zorunlu';

CREATE TRIGGER label_print_append_only
    BEFORE UPDATE OR DELETE ON label_print
    FOR EACH ROW EXECUTE FUNCTION reject_modification();

CREATE TABLE bag_reservation (
    id             uuid        PRIMARY KEY,
    bag_id         uuid        NOT NULL REFERENCES bag (id),
    shipment_id    uuid        NOT NULL,
    reserved_at    timestamptz NOT NULL,
    reserved_by    uuid        NOT NULL,
    released_at    timestamptz,
    released_by    uuid,
    release_reason text,
    created_at     timestamptz NOT NULL,
    created_by     uuid        NOT NULL,
    updated_at     timestamptz,
    updated_by     uuid,
    version        integer     NOT NULL DEFAULT 0,
    CONSTRAINT bag_reservation_release_complete CHECK (
        (released_at IS NULL AND released_by IS NULL AND release_reason IS NULL)
        OR (released_at IS NOT NULL AND released_by IS NOT NULL AND length(btrim(coalesce(release_reason, ''))) > 0))
);

COMMENT ON TABLE bag_reservation IS 'Çuvalın sevkiyata bağlanması; ShipmentCancelled gelince bırakılır';

-- Bir çuval aynı anda yalnızca bir sevkiyatta olabilir (rapor Bölüm 8).
CREATE UNIQUE INDEX bag_reservation_active_uq ON bag_reservation (bag_id) WHERE released_at IS NULL;
CREATE INDEX bag_reservation_shipment_idx ON bag_reservation (shipment_id);

-- Rezervasyon yalnızca bir kez bırakılabilir; bırakma dışında hiçbir alan değişmez.
CREATE TRIGGER bag_reservation_guard
    AFTER UPDATE OR DELETE ON bag_reservation
    FOR EACH ROW EXECUTE FUNCTION guard_immutable_columns('released_at', 'released_by', 'release_reason');

CREATE FUNCTION guard_reservation_release_once()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.released_at IS NOT NULL THEN
        RAISE EXCEPTION 'Reservation already released reservationId=%', OLD.id USING ERRCODE = 'YS002';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER bag_reservation_release_once
    BEFORE UPDATE ON bag_reservation
    FOR EACH ROW EXECUTE FUNCTION guard_reservation_release_once();

CREATE TRIGGER bag_reservation_audit_local
    AFTER INSERT OR UPDATE OR DELETE ON bag_reservation
    FOR EACH ROW EXECUTE FUNCTION audit_local_capture();

GRANT SELECT, INSERT ON lot_quality_lock, label_print TO ${app_role};
GRANT SELECT, INSERT, UPDATE ON bag, bag_reservation TO ${app_role};
