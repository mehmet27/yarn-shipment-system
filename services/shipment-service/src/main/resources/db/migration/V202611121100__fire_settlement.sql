-- Sevkiyatın fire dosyası (analiz 5.10-5.11, 8). Kumaş ve iplik dönüşü toplamları kayıtlardan hesaplanır;
-- burada yalnız dosyanın durumu, oranı, gönderilen kg ve fatura işareti saklanır.
CREATE TABLE fire_settlement (
    shipment_id      uuid           PRIMARY KEY REFERENCES shipment (id),
    subcontractor_id uuid           NOT NULL,
    status           text           NOT NULL CHECK (status IN ('AWAITING_FABRIC', 'TO_INVOICE', 'INVOICED', 'CLOSED')),
    fire_rate        numeric(5, 4)  NOT NULL DEFAULT 0.0400 CHECK (fire_rate >= 0 AND fire_rate <= 0.2000),
    sent_kg          numeric(12, 3) NOT NULL CHECK (sent_kg >= 0),
    invoice_no       text,
    invoiced_on      date,
    invoiced_by      uuid,
    invoice_note     text,
    created_at       timestamptz    NOT NULL,
    updated_at       timestamptz,
    CONSTRAINT fire_settlement_invoice_fields CHECK (
        (status = 'INVOICED') = (invoice_no IS NOT NULL AND invoiced_on IS NOT NULL AND invoiced_by IS NOT NULL))
);

CREATE INDEX fire_settlement_status_idx ON fire_settlement (status, subcontractor_id);

-- Kapanmış (INVOICED, CLOSED) dosya geri açılmaz (analiz 5.11: işaret geri alınamaz).
CREATE FUNCTION guard_fire_settlement_update()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.status IN ('INVOICED', 'CLOSED') THEN
        RAISE EXCEPTION 'Fire settlement is closed shipmentId=%', OLD.shipment_id USING ERRCODE = 'YS003';
    END IF;
    IF NEW.shipment_id <> OLD.shipment_id OR NEW.subcontractor_id <> OLD.subcontractor_id
            OR NEW.sent_kg <> OLD.sent_kg THEN
        RAISE EXCEPTION 'Immutable column changed table=fire_settlement id=%', OLD.shipment_id USING ERRCODE = 'YS002';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER fire_settlement_guard
    BEFORE UPDATE ON fire_settlement
    FOR EACH ROW EXECUTE FUNCTION guard_fire_settlement_update();

CREATE TRIGGER fire_settlement_no_delete
    BEFORE DELETE ON fire_settlement
    FOR EACH ROW EXECUTE FUNCTION reject_modification();

-- Fire oranı değişikliklerinin denetim kaydı (KR-02); yalnız ekleme.
CREATE TABLE fire_rate_change (
    id          uuid          PRIMARY KEY,
    shipment_id uuid          NOT NULL REFERENCES shipment (id),
    old_rate    numeric(5, 4) NOT NULL,
    new_rate    numeric(5, 4) NOT NULL,
    reason      text          NOT NULL CHECK (length(btrim(reason)) > 0),
    changed_by  uuid          NOT NULL,
    changed_at  timestamptz   NOT NULL
);

CREATE INDEX fire_rate_change_shipment_idx ON fire_rate_change (shipment_id, changed_at);

CREATE TRIGGER fire_rate_change_append_only
    BEFORE UPDATE OR DELETE ON fire_rate_change
    FOR EACH ROW EXECUTE FUNCTION reject_modification();

GRANT SELECT, INSERT, UPDATE ON fire_settlement TO ${app_role};
GRANT SELECT, INSERT ON fire_rate_change TO ${app_role};

-- Bu migration'dan önce tamamlanmış sevkiyatlar: çıkış tartısı neti tek satır DISPATCH hareketi ve fire dosyası.
INSERT INTO balance_movement (id, subcontractor_id, yarn_type, shipment_id, kind, net_kg, occurred_at, created_by,
                              source_ref)
SELECT gen_random_uuid(), s.subcontractor_id, '(reçetesiz)', s.id, 'DISPATCH', w.net_kg,
       coalesce(s.locked_at, s.created_at), s.created_by, 'DISPATCH:' || s.id || ':1'
FROM shipment s
JOIN weighing w ON w.shipment_id = s.id AND w.side = 'DISPATCH' AND w.superseded_by IS NULL
WHERE s.status = 'COMPLETED' AND w.net_kg > 0
ON CONFLICT DO NOTHING;

INSERT INTO fire_settlement (shipment_id, subcontractor_id, status, sent_kg, created_at)
SELECT s.id, s.subcontractor_id, 'AWAITING_FABRIC', w.net_kg, coalesce(s.locked_at, s.created_at)
FROM shipment s
JOIN weighing w ON w.shipment_id = s.id AND w.side = 'DISPATCH' AND w.superseded_by IS NULL
WHERE s.status = 'COMPLETED'
ON CONFLICT DO NOTHING;
