-- İkinci dalga birleştirme (monolit geçiş planı, Bölüm 3): md_branding tek satırlık bir ayar tablosuydu; app_setting
-- anahtar-değer tablosuna üç anahtar olarak taşınır. Doğrulama kuralları anahtara bağlı CHECK olarak korunur.
-- Eski V202611051000 ve V202611101100 dosyalarına dokunulmaz (Flyway sağlama toplamı).

ALTER TABLE app_setting DROP CONSTRAINT app_setting_key_check;
ALTER TABLE app_setting ADD CONSTRAINT app_setting_key_check CHECK (key IN (
    'default_fire_rate', 'weight_tolerance_kg', 'branding_company_name', 'branding_primary_color', 'branding_logo'));

ALTER TABLE app_setting ADD CONSTRAINT app_setting_branding_value CHECK (
    (key <> 'branding_company_name' OR char_length(value) <= 80)
    AND (key <> 'branding_primary_color' OR value ~ '^#[0-9a-f]{6}$')
    AND (key <> 'branding_logo' OR value = '' OR (char_length(value) <= 200000
                                    AND value ~ '^data:image/(png|jpeg|webp|svg\+xml);base64,[A-Za-z0-9+/=]+$')));

-- Uygulama rolünde DELETE yok: kaldırılan firma adı ya da logo boş metin olarak yazılır, okuma boşu "yok" sayar.

COMMENT ON TABLE app_setting IS 'Kurulum ayarları (fire oranı, kantar toleransı, marka); değişiklik audit_local trigger''ıyla izlenir';

-- Veri taşıma: iz tekrarlanmasın diye taşıma sırasında trigger kapalıdır; eski izler audit_local''da (md_branding adıyla) kalır.
ALTER TABLE app_setting DISABLE TRIGGER app_setting_audit_local;
INSERT INTO app_setting (key, value, updated_at, updated_by)
SELECT 'branding_primary_color', primary_color, updated_at, updated_by FROM md_branding
UNION ALL
SELECT 'branding_company_name', company_name, updated_at, updated_by FROM md_branding WHERE company_name IS NOT NULL
UNION ALL
SELECT 'branding_logo', logo_data_url, updated_at, updated_by FROM md_branding WHERE logo_data_url IS NOT NULL;
ALTER TABLE app_setting ENABLE TRIGGER app_setting_audit_local;

DROP TABLE md_branding;
