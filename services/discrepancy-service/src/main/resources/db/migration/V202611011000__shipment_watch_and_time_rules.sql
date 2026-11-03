-- Karşılaştırma dışındaki uyuşmazlık kuralları (eksik listesi 3.14, 6.3, 6.9): zorunlu fotoğraf eksik, aynı fotoğraf
-- başka sevkiyatta kullanılmış, sevkiyat yolda çok kaldı. Bunlar için servis sevkiyatın yaşam döngüsünü ve yüklenen
-- kanıt türlerini olaylardan kendi tablolarında izler (başka servisin tablosu okunmaz).

CREATE TABLE shipment_watch (
    shipment_id         uuid        PRIMARY KEY,
    shipment_no         text        NOT NULL,
    site_id             uuid        NOT NULL,
    subcontractor_id    uuid,
    shipment_created_at timestamptz NOT NULL,
    dispatched_at       timestamptz,
    arrived_at          timestamptz,
    closed_at           timestamptz,
    transit_alerted_at  timestamptz,
    created_at          timestamptz NOT NULL,
    created_by          uuid        NOT NULL,
    updated_at          timestamptz,
    updated_by          uuid,
    version             integer     NOT NULL DEFAULT 0
);

COMMENT ON TABLE shipment_watch IS 'SevkiyatOlusturuldu/YolaCikti/Vardi/Tamamlandi/IptalEdildi olaylarından; zaman kuralı ve fotoğraf kuralları için';
COMMENT ON COLUMN shipment_watch.closed_at IS 'Tamamlanma ya da iptal zamanı; kapanan sevkiyat zaman kuralına girmez';
COMMENT ON COLUMN shipment_watch.transit_alerted_at IS 'Yolda süre aşımı uyuşmazlığının açıldığı an; sevkiyat başına bir kez';

-- Zamanlanmış kontrol yalnızca yolda olan, henüz uyarılmamış sevkiyatları tarar.
CREATE INDEX shipment_watch_in_transit_idx ON shipment_watch (dispatched_at)
    WHERE arrived_at IS NULL AND closed_at IS NULL AND transit_alerted_at IS NULL;

CREATE TABLE shipment_evidence (
    evidence_id   uuid        PRIMARY KEY,
    shipment_id   uuid        NOT NULL,
    evidence_type text        NOT NULL,
    uploaded_at   timestamptz NOT NULL,
    created_at    timestamptz NOT NULL,
    created_by    uuid        NOT NULL
);

COMMENT ON TABLE shipment_evidence IS 'KanitYuklendi olaylarından sevkiyata yüklenen kanıt türleri; zorunlu fotoğraf kontrolü için';

CREATE INDEX shipment_evidence_shipment_idx ON shipment_evidence (shipment_id, evidence_type);

CREATE TRIGGER shipment_evidence_append_only
    BEFORE UPDATE OR DELETE ON shipment_evidence
    FOR EACH ROW EXECUTE FUNCTION reject_modification();

-- Durumda kalma süresi sınırı (rapor 7.2: "Yolda > 24 saat"). Tolerans kuralı gibi geçerlilik aralığıyla saklanır:
-- yeni değer eski satırın valid_to'su kapatılıp yeni satırla girilir, geçmiş korunur.
CREATE TABLE time_limit_rule (
    id         uuid        PRIMARY KEY,
    stage      text        NOT NULL CHECK (stage IN ('IN_TRANSIT')),
    max_hours  integer     NOT NULL CHECK (max_hours BETWEEN 1 AND 720),
    valid_from timestamptz NOT NULL,
    valid_to   timestamptz,
    created_at timestamptz NOT NULL,
    created_by uuid        NOT NULL,
    updated_at timestamptz,
    updated_by uuid,
    version    integer     NOT NULL DEFAULT 0,
    CONSTRAINT time_limit_rule_validity CHECK (valid_to IS NULL OR valid_to > valid_from),
    CONSTRAINT time_limit_rule_no_overlap EXCLUDE USING gist (
        stage WITH =,
        tstzrange(valid_from, valid_to, '[)') WITH &&)
);

COMMENT ON TABLE time_limit_rule IS 'Durumda kalma süresi sınırı (saat); aşılınca TIME uyuşmazlığı açılır';

CREATE TRIGGER time_limit_rule_audit_local
    AFTER INSERT OR UPDATE OR DELETE ON time_limit_rule
    FOR EACH ROW EXECUTE FUNCTION audit_local_capture();

INSERT INTO time_limit_rule (id, stage, max_hours, valid_from, created_at, created_by)
VALUES ('0192f0a0-0000-7000-8000-00000000c101', 'IN_TRANSIT', 24, '2026-01-01 00:00:00+00', now(),
        '00000000-0000-0000-0000-000000000000');

GRANT SELECT, INSERT, UPDATE ON shipment_watch, time_limit_rule TO ${app_role};
GRANT SELECT, INSERT ON shipment_evidence TO ${app_role};
