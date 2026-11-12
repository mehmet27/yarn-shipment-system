-- Yükleme niyeti: istemci kısa ömürlü imzalı adrese dosyayı yükler, sonra onaylar. Onayda sunucu nesneyi
-- depodan okuyup hash'i hesaplar (SEC-09); bu tablo onaya kadar neyin beklendiğini tutar.
CREATE TABLE upload_intent (
    id           uuid        PRIMARY KEY,
    shipment_id  uuid        NOT NULL,
    site_id      uuid        NOT NULL,
    evidence_type text       NOT NULL CHECK (evidence_type IN (
        'DISPATCH_SCALE', 'BAG_OVERVIEW', 'BAG_LABEL', 'VEHICLE_PLATE', 'SEAL', 'DELIVERY_SCALE',
        'DELIVERY_BAGS', 'QUALITY_CLAIM', 'SIGNED_DELIVERY_DOC', 'LAB_RESULT')),
    mime_type    text        NOT NULL,
    size_bytes   bigint      NOT NULL CHECK (size_bytes > 0),
    source       text        NOT NULL CHECK (source IN ('CAMERA', 'GALLERY')),
    captured_at  timestamptz,
    storage_key  text        NOT NULL,
    requested_by uuid        NOT NULL,
    requested_at timestamptz NOT NULL,
    expires_at   timestamptz NOT NULL,
    created_at   timestamptz NOT NULL,
    created_by   uuid        NOT NULL,
    CONSTRAINT upload_intent_expiry CHECK (expires_at > requested_at)
);

COMMENT ON TABLE upload_intent IS 'Onay bekleyen yüklemeler; onaylanan niyet evidence_file kaydına dönüşür ve değişmez';

CREATE INDEX upload_intent_shipment_idx ON upload_intent (shipment_id);

GRANT SELECT, INSERT ON upload_intent TO ${app_role};
