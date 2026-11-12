-- Süreç analizi v2, Bölüm 5.2 ve 9.2: reçete satırları (müdür elle yazar) ve sevkiyatın fire oranı.
-- Eski shipment_line (ürün/parti planı) hiç kullanılmadı; yeni reçete şekliyle yeniden kurulur.
-- planned_net_kg artık satır toplamından hesaplanır.

DROP TABLE shipment_line;

CREATE TABLE shipment_line (
    id               uuid           PRIMARY KEY,
    shipment_id      uuid           NOT NULL REFERENCES shipment (id),
    line_no          integer        NOT NULL CHECK (line_no > 0),
    yarn_type_id     uuid,
    yarn_type_text   text           CHECK (yarn_type_text IS NULL OR length(btrim(yarn_type_text)) > 0),
    lot_id           uuid,
    net_kg           numeric(12, 3) NOT NULL CHECK (net_kg > 0),
    created_at       timestamptz    NOT NULL,
    created_by       uuid           NOT NULL,
    updated_at       timestamptz,
    updated_by       uuid,
    version          integer        NOT NULL DEFAULT 0,
    UNIQUE (shipment_id, line_no),
    CONSTRAINT shipment_line_yarn_type_given CHECK (yarn_type_id IS NOT NULL OR yarn_type_text IS NOT NULL)
);

COMMENT ON TABLE shipment_line IS 'Reçete satırı: iplik cinsi (listeden ya da serbest metin), isteğe bağlı lot, net kg';

CREATE INDEX shipment_line_shipment_idx ON shipment_line (shipment_id);

CREATE TRIGGER shipment_line_lock
    BEFORE INSERT OR UPDATE ON shipment_line
    FOR EACH ROW EXECUTE FUNCTION guard_locked_shipment_child();

CREATE TRIGGER shipment_line_no_delete
    BEFORE DELETE ON shipment_line
    FOR EACH ROW EXECUTE FUNCTION reject_modification();

CREATE TRIGGER shipment_line_audit_local
    AFTER INSERT OR UPDATE OR DELETE ON shipment_line
    FOR EACH ROW EXECUTE FUNCTION audit_local_capture();

GRANT SELECT, INSERT, UPDATE ON shipment_line TO ${app_role};

ALTER TABLE shipment ADD COLUMN fire_rate numeric(5, 4) NOT NULL DEFAULT 0.04
    CONSTRAINT shipment_fire_rate_range CHECK (fire_rate >= 0 AND fire_rate <= 0.20);

COMMENT ON COLUMN shipment.fire_rate IS 'Sevkiyatın fire oranı (0.04 = %4); varsayılan app_setting.default_fire_rate';

-- Kurulum ayarları (varsayılan fire oranı, kantar toleransı). Satır yoksa uygulama kod varsayılanını kullanır.
CREATE TABLE app_setting (
    key        text        PRIMARY KEY CHECK (key IN ('default_fire_rate', 'weight_tolerance_kg')),
    value      text        NOT NULL,
    updated_at timestamptz NOT NULL,
    updated_by uuid        NOT NULL
);

COMMENT ON TABLE app_setting IS 'Kurulum ayarları; değişiklik audit_local trigger''ıyla izlenir';

CREATE TRIGGER app_setting_audit_local
    AFTER INSERT OR UPDATE OR DELETE ON app_setting
    FOR EACH ROW EXECUTE FUNCTION audit_local_capture();

GRANT SELECT, INSERT, UPDATE ON app_setting TO ${app_role};
