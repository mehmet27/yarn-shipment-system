-- YALNIZ TEST: Üretimde bu tabloları shipment-service ve discrepancy-service migration'ları kurar. Rapor görünümlerini
-- (V202611161100) bu modülün testinde sınamak için, görünümlerin okuduğu kolonların birebir aynı adlı kopyasıdır.
-- Kolon adı ya da türü kaynak şemada değişirse bu dosya ve görünüm birlikte güncellenmelidir.
CREATE SCHEMA IF NOT EXISTS shipment;
CREATE SCHEMA IF NOT EXISTS discrepancy;

CREATE TABLE shipment.md_subcontractor (
    id    uuid PRIMARY KEY,
    title text NOT NULL
);

CREATE TABLE shipment.shipment (
    id                  uuid           PRIMARY KEY,
    site_id             uuid           NOT NULL,
    shipment_no         text           NOT NULL UNIQUE,
    subcontractor_id    uuid           NOT NULL,
    subcontractor_title text           NOT NULL,
    status              text           NOT NULL,
    locked_at           timestamptz,
    created_at          timestamptz    NOT NULL,
    updated_at          timestamptz,
    version             integer        NOT NULL DEFAULT 0
);

CREATE TABLE shipment.weighing (
    id            uuid           PRIMARY KEY,
    shipment_id   uuid           NOT NULL REFERENCES shipment.shipment (id),
    side          text           NOT NULL,
    net_kg        numeric(12, 3) NOT NULL,
    weighed_at    timestamptz    NOT NULL,
    superseded_by uuid
);

CREATE TABLE shipment.balance_movement (
    id               uuid           PRIMARY KEY,
    subcontractor_id uuid           NOT NULL,
    yarn_type        text           NOT NULL,
    shipment_id      uuid           REFERENCES shipment.shipment (id),
    kind             text           NOT NULL,
    net_kg           numeric(12, 3) NOT NULL CHECK (net_kg <> 0),
    occurred_at      timestamptz    NOT NULL,
    created_by       uuid           NOT NULL,
    source_ref       text           UNIQUE
);

CREATE TABLE shipment.fire_settlement (
    shipment_id      uuid           PRIMARY KEY REFERENCES shipment.shipment (id),
    subcontractor_id uuid           NOT NULL,
    status           text           NOT NULL,
    fire_rate        numeric(5, 4)  NOT NULL DEFAULT 0.0400,
    sent_kg          numeric(12, 3) NOT NULL,
    invoice_no       text,
    invoiced_on      date,
    created_at       timestamptz    NOT NULL
);

CREATE TABLE shipment.fabric_receipt (
    id          uuid           PRIMARY KEY,
    shipment_id uuid           NOT NULL REFERENCES shipment.shipment (id),
    net_kg      numeric(12, 3) NOT NULL
);

CREATE TABLE shipment.yarn_return (
    id          uuid           PRIMARY KEY,
    shipment_id uuid           NOT NULL REFERENCES shipment.shipment (id),
    direction   text           NOT NULL,
    net_kg      numeric(12, 3) NOT NULL
);

CREATE TABLE discrepancy.discrepancy (
    id               uuid        PRIMARY KEY,
    discrepancy_no   text        NOT NULL,
    site_id          uuid        NOT NULL,
    shipment_id      uuid        NOT NULL,
    discrepancy_type text        NOT NULL,
    severity         text        NOT NULL,
    status           text        NOT NULL,
    due_at           timestamptz,
    resolved_at      timestamptz,
    created_at       timestamptz NOT NULL
);

GRANT USAGE ON SCHEMA shipment, discrepancy TO ${app_role};
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA shipment, discrepancy TO ${app_role};
