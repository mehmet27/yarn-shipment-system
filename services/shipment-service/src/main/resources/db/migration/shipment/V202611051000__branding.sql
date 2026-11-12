-- Kurulumun marka ayarı: firma adı, ana renk ve logo. Her firma ayrı kurulum olduğu için tek satır vardır; satır yoksa
-- arayüz varsayılan temayı kullanır. Diğer ana veri gibi master-data servisi gelene kadar shipment_db içinde `md_`
-- önekiyle durur. Giriş sayfası da okur, yazma yalnızca SETTINGS_MANAGE yetkisiyle yapılır.

CREATE TABLE md_branding (
    id            uuid        PRIMARY KEY CHECK (id = '00000000-0000-0000-0000-000000000001'),
    company_name  text        CHECK (char_length(company_name) <= 80),
    primary_color text        NOT NULL CHECK (primary_color ~ '^#[0-9a-f]{6}$'),
    logo_data_url text        CHECK (char_length(logo_data_url) <= 200000
                                     AND logo_data_url ~ '^data:image/(png|jpeg|webp|svg\+xml);base64,[A-Za-z0-9+/=]+$'),
    updated_at    timestamptz NOT NULL,
    updated_by    uuid        NOT NULL
);

COMMENT ON TABLE md_branding IS 'Kurulumun marka ayarı; tek satır (id sabit)';
COMMENT ON COLUMN md_branding.primary_color IS 'Ana renk, küçük harfli #rrggbb; arayüz paleti bundan üretilir';
COMMENT ON COLUMN md_branding.logo_data_url IS 'Logo; base64 data adresi (png, jpeg, webp, svg)';

GRANT SELECT, INSERT, UPDATE ON md_branding TO ${app_role};

CREATE TRIGGER md_branding_audit_local
    AFTER INSERT OR UPDATE OR DELETE ON md_branding
    FOR EACH ROW EXECUTE FUNCTION audit_local_capture();
