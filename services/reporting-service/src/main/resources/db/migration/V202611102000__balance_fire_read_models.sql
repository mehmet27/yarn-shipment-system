-- Bakiye defteri ve fire dosyası için rapor servisinin kendi okuma modelleri (analiz 5.7, 8, 9.2, 11).
-- Kaynak defter/fire tabloları ayrı PR'da gelir; bu tablolar olaylardan yeniden kurulabilir.
-- OPENING satırları açılış bakiyesi içe aktarmasıyla doğrudan buraya yazılır.

CREATE TABLE balance_movement_view (
    id                 uuid           PRIMARY KEY,
    subcontractor_id   uuid           NOT NULL,
    subcontractor_name text           NOT NULL,
    yarn_type          text           NOT NULL,
    shipment_id        uuid,
    site_id            uuid,
    kind               text           NOT NULL
        CHECK (kind IN ('DISPATCH', 'FABRIC_RECEIPT', 'YARN_RETURN', 'CLAIM_ADJUSTMENT', 'OPENING')),
    net_kg             numeric(14, 3) NOT NULL,
    occurred_at        timestamptz    NOT NULL,
    reverses_id        uuid
);

COMMENT ON TABLE balance_movement_view IS 'Fasoncu bakiye defterinin okuma modeli; net_kg işaretlidir, bakiye toplamdır';
COMMENT ON COLUMN balance_movement_view.site_id IS 'Boşsa (ör. OPENING) lokasyondan bağımsızdır';

CREATE INDEX balance_movement_view_party_idx ON balance_movement_view (subcontractor_id, yarn_type);
CREATE INDEX balance_movement_view_shipment_idx ON balance_movement_view (shipment_id) WHERE shipment_id IS NOT NULL;
CREATE UNIQUE INDEX balance_movement_view_opening_uq
    ON balance_movement_view (subcontractor_id, yarn_type) WHERE kind = 'OPENING';

CREATE TABLE fire_file_view (
    shipment_id        uuid           PRIMARY KEY,
    shipment_no        text           NOT NULL,
    subcontractor_id   uuid           NOT NULL,
    subcontractor_name text           NOT NULL,
    site_id            uuid           NOT NULL,
    sent_kg            numeric(14, 3) NOT NULL,
    returned_yarn_kg   numeric(14, 3) NOT NULL DEFAULT 0,
    fabric_kg          numeric(14, 3) NOT NULL DEFAULT 0,
    fire_rate          numeric(5, 4)  NOT NULL DEFAULT 0.04,
    status             text           NOT NULL CHECK (status IN ('AWAITING_FABRIC', 'TO_INVOICE', 'CLOSED')),
    invoice_no         text,
    invoiced_at        timestamptz,
    completed_at       timestamptz    NOT NULL
);

COMMENT ON TABLE fire_file_view IS 'Sevkiyat bazında fire dosyası okuma modeli (G, D, K, f); hesaplar kodda yapılır';

CREATE INDEX fire_file_view_site_completed_idx ON fire_file_view (site_id, completed_at);

GRANT SELECT, INSERT, UPDATE ON balance_movement_view, fire_file_view TO ${app_role};
