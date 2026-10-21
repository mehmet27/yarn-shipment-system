-- discrepancy_db: tolerans, karşılaştırma, uyuşmazlık, kalite itirazı, fiziki kontrol
-- (veritabanı tasarımı, "discrepancy_db"). Fabrika ve fasoncu değerlerini yan yana gören tek yer
-- bu veritabanıdır (SEC-06).

CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE tolerance_rule (
    id             uuid          PRIMARY KEY,
    product_id     uuid,
    product_scope  uuid          GENERATED ALWAYS AS
        (coalesce(product_id, '00000000-0000-0000-0000-000000000000'::uuid)) STORED,
    green_max_pct  numeric(5, 2) NOT NULL CHECK (green_max_pct >= 0),
    yellow_max_pct numeric(5, 2) NOT NULL,
    valid_from     timestamptz   NOT NULL,
    valid_to       timestamptz,
    created_at     timestamptz   NOT NULL,
    created_by     uuid          NOT NULL,
    updated_at     timestamptz,
    updated_by     uuid,
    version        integer       NOT NULL DEFAULT 0,
    CONSTRAINT tolerance_rule_band_order CHECK (yellow_max_pct > green_max_pct),
    CONSTRAINT tolerance_rule_validity CHECK (valid_to IS NULL OR valid_to > valid_from),
    -- Aynı kapsam (ürün veya genel) için geçerlilik aralıkları çakışamaz.
    CONSTRAINT tolerance_rule_no_overlap EXCLUDE USING gist (
        product_scope WITH =,
        tstzrange(valid_from, valid_to, '[)') WITH &&)
);

COMMENT ON TABLE tolerance_rule IS 'Tolerans bantları; product_id boş ise genel kural (DOM-08)';
COMMENT ON COLUMN tolerance_rule.product_scope IS 'Genel kuralı çakışma kısıtına dahil etmek için türetilmiş kolon';

CREATE TRIGGER tolerance_rule_audit_local
    AFTER INSERT OR UPDATE OR DELETE ON tolerance_rule
    FOR EACH ROW EXECUTE FUNCTION audit_local_capture();

CREATE TABLE comparison_input (
    id                uuid           PRIMARY KEY,
    shipment_id       uuid           NOT NULL UNIQUE,
    dispatch_net_kg   numeric(12, 3) NOT NULL CHECK (dispatch_net_kg >= 0),
    delivery_net_kg   numeric(12, 3) NOT NULL CHECK (delivery_net_kg >= 0),
    dispatch_bag_keys jsonb          NOT NULL CHECK (jsonb_typeof(dispatch_bag_keys) = 'array'),
    delivery_bag_keys jsonb          NOT NULL CHECK (jsonb_typeof(delivery_bag_keys) = 'array'),
    seal_nos          jsonb          NOT NULL,
    received_at       timestamptz    NOT NULL,
    created_at        timestamptz    NOT NULL,
    created_by        uuid           NOT NULL
);

COMMENT ON TABLE comparison_input IS 'DeliveryReceived olayından doldurulur; değiştirilemez';

CREATE TRIGGER comparison_input_append_only
    BEFORE UPDATE OR DELETE ON comparison_input
    FOR EACH ROW EXECUTE FUNCTION reject_modification();

CREATE TABLE discrepancy (
    id              uuid           PRIMARY KEY,
    discrepancy_no  text           NOT NULL UNIQUE,
    site_id         uuid           NOT NULL,
    shipment_id     uuid           NOT NULL,
    discrepancy_type text          NOT NULL CHECK (discrepancy_type IN (
        'QUANTITY', 'BAG', 'SEAL', 'QUALITY', 'DOCUMENT', 'PHOTO', 'TIME')),
    severity        text           NOT NULL CHECK (severity IN ('YELLOW', 'RED')),
    status          text           NOT NULL CHECK (status IN ('OPEN', 'IN_REVIEW', 'RESOLVED', 'CLOSED')),
    rule_code       text           NOT NULL,
    expected_value  text,
    actual_value    text,
    diff_kg         numeric(12, 3),
    diff_pct        numeric(7, 2),
    assignee_id     uuid,
    due_at          timestamptz,
    resolution      text           CHECK (resolution IN (
        'TOLERANCE', 'DATA_ERROR', 'CLAIM_JUSTIFIED', 'CLAIM_UNJUSTIFIED', 'LIABILITY_ASSIGNED')),
    resolution_note text,
    liable_party    text,
    resolved_by     uuid,
    resolved_at     timestamptz,
    closed_at       timestamptz,
    created_at      timestamptz    NOT NULL,
    created_by      uuid           NOT NULL,
    updated_at      timestamptz,
    updated_by      uuid,
    version         integer        NOT NULL DEFAULT 0,
    CONSTRAINT discrepancy_resolution_required CHECK (
        status IN ('OPEN', 'IN_REVIEW')
        OR (resolution IS NOT NULL AND length(btrim(coalesce(resolution_note, ''))) > 0
            AND resolved_by IS NOT NULL AND resolved_at IS NOT NULL)),
    CONSTRAINT discrepancy_closed_at CHECK ((status = 'CLOSED') = (closed_at IS NOT NULL))
);

COMMENT ON TABLE discrepancy IS 'Uyuşmazlık; RESOLVED için karar ve not zorunlu';
COMMENT ON COLUMN discrepancy.discrepancy_type IS 'Tasarımdaki type kolonu (QUANTITY, BAG, ...)';

CREATE INDEX discrepancy_site_status_idx ON discrepancy (site_id, status, severity, due_at);
CREATE INDEX discrepancy_shipment_idx ON discrepancy (shipment_id);
CREATE INDEX discrepancy_assignee_status_idx ON discrepancy (assignee_id, status);

CREATE TRIGGER discrepancy_no_delete
    BEFORE DELETE ON discrepancy
    FOR EACH ROW EXECUTE FUNCTION reject_modification();

CREATE TRIGGER discrepancy_audit_local
    AFTER INSERT OR UPDATE OR DELETE ON discrepancy
    FOR EACH ROW EXECUTE FUNCTION audit_local_capture();

CREATE TABLE discrepancy_activity (
    id             uuid        NOT NULL,
    created_at     timestamptz NOT NULL,
    discrepancy_id uuid        NOT NULL,
    action_type    text        NOT NULL,
    note           text,
    evidence_id    uuid,
    actor_id       uuid        NOT NULL,
    occurred_at    timestamptz NOT NULL,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

COMMENT ON TABLE discrepancy_activity IS 'Uyuşmazlık yaşam döngüsü geçmişi; yalnızca ekleme';

CREATE INDEX discrepancy_activity_discrepancy_idx ON discrepancy_activity (discrepancy_id, occurred_at);

CREATE TRIGGER discrepancy_activity_append_only
    BEFORE UPDATE OR DELETE ON discrepancy_activity
    FOR EACH ROW EXECUTE FUNCTION reject_modification();

CREATE TABLE quality_claim (
    id               uuid        PRIMARY KEY,
    discrepancy_id   uuid        NOT NULL UNIQUE REFERENCES discrepancy (id),
    shipment_id      uuid        NOT NULL,
    subcontractor_id uuid        NOT NULL,
    opened_by        uuid        NOT NULL,
    opened_at        timestamptz NOT NULL,
    deadline_at      timestamptz NOT NULL,
    description      text        NOT NULL CHECK (length(btrim(description)) > 0),
    created_at       timestamptz NOT NULL,
    created_by       uuid        NOT NULL,
    updated_at       timestamptz,
    updated_by       uuid,
    version          integer     NOT NULL DEFAULT 0,
    CONSTRAINT quality_claim_opened_before_deadline CHECK (opened_at <= deadline_at)
);

COMMENT ON TABLE quality_claim IS 'Kalite itirazı; deadline = teslim + 3 iş günü (rapor karar #7, BusinessCalendar)';

CREATE INDEX quality_claim_shipment_idx ON quality_claim (shipment_id);
CREATE INDEX quality_claim_subcontractor_idx ON quality_claim (subcontractor_id);

CREATE TRIGGER quality_claim_guard
    AFTER UPDATE OR DELETE ON quality_claim
    FOR EACH ROW EXECUTE FUNCTION guard_immutable_columns();

CREATE TABLE quality_claim_bag (
    claim_id   uuid        NOT NULL REFERENCES quality_claim (id),
    bag_id     uuid        NOT NULL,
    unique_key text        NOT NULL,
    created_at timestamptz NOT NULL,
    created_by uuid        NOT NULL,
    PRIMARY KEY (claim_id, bag_id)
);

CREATE TRIGGER quality_claim_bag_append_only
    BEFORE UPDATE OR DELETE ON quality_claim_bag
    FOR EACH ROW EXECUTE FUNCTION reject_modification();

CREATE TABLE inspection (
    id                 uuid        PRIMARY KEY,
    shipment_id        uuid        NOT NULL,
    discrepancy_id     uuid        REFERENCES discrepancy (id),
    reason             text        NOT NULL CHECK (reason IN ('RANDOM', 'RISK', 'CLAIM')),
    kind               text        NOT NULL CHECK (kind IN ('PHYSICAL_COUNT', 'SAMPLE', 'LAB')),
    selected_at        timestamptz NOT NULL,
    performed_at       timestamptz,
    performed_by       uuid,
    result             text,
    result_evidence_id uuid,
    created_at         timestamptz NOT NULL,
    created_by         uuid        NOT NULL,
    updated_at         timestamptz,
    updated_by         uuid,
    version            integer     NOT NULL DEFAULT 0,
    CONSTRAINT inspection_performed_complete CHECK (
        (performed_at IS NULL AND performed_by IS NULL AND result IS NULL)
        OR (performed_at IS NOT NULL AND performed_by IS NOT NULL AND result IS NOT NULL))
);

COMMENT ON TABLE inspection IS 'Fiziki kontrol, numune, lab; şoför/fasoncu API''sinde yola çıkana kadar gizli (SEC-08)';

CREATE INDEX inspection_shipment_idx ON inspection (shipment_id);

CREATE TRIGGER inspection_no_delete
    BEFORE DELETE ON inspection
    FOR EACH ROW EXECUTE FUNCTION reject_modification();

GRANT SELECT, INSERT, UPDATE ON tolerance_rule, discrepancy, quality_claim, inspection TO ${app_role};
GRANT SELECT, INSERT ON comparison_input, discrepancy_activity, quality_claim_bag TO ${app_role};

SELECT create_monthly_partitions('discrepancy_activity', date '2026-10-01', 15);
