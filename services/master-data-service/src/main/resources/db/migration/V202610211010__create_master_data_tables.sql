-- masterdata_db: tanımlar, parti kalite kilidi, kullanıcı profili (veritabanı tasarımı, "masterdata_db").
-- Kayıtlar silinmez, status = PASSIVE yapılır (GEN-04); uygulama rolüne DELETE yetkisi verilmez.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE site (
    id         uuid        PRIMARY KEY,
    code       text        NOT NULL UNIQUE,
    name       text        NOT NULL,
    site_type  text        NOT NULL CHECK (site_type IN ('FACTORY', 'WAREHOUSE')),
    address    text,
    timezone   text        NOT NULL DEFAULT 'Europe/Istanbul',
    status     text        NOT NULL CHECK (status IN ('ACTIVE', 'PASSIVE')),
    created_at timestamptz NOT NULL,
    created_by uuid        NOT NULL,
    updated_at timestamptz,
    updated_by uuid,
    version    integer     NOT NULL DEFAULT 0
);

COMMENT ON TABLE site IS 'Lokasyon (fabrika/depo); Faz 1''de tek kayıt (GEN-07)';

CREATE TABLE subcontractor (
    id           uuid        PRIMARY KEY,
    code         text        NOT NULL UNIQUE,
    title        text        NOT NULL,
    tax_no       text        NOT NULL UNIQUE,
    address      text,
    contact_name text,
    phone        text,
    email        text,
    status       text        NOT NULL CHECK (status IN ('ACTIVE', 'PASSIVE')),
    created_at   timestamptz NOT NULL,
    created_by   uuid        NOT NULL,
    updated_at   timestamptz,
    updated_by   uuid,
    version      integer     NOT NULL DEFAULT 0
);

COMMENT ON TABLE subcontractor IS 'Fasoncu';

CREATE TABLE driver (
    id              uuid        PRIMARY KEY,
    full_name       text        NOT NULL,
    national_id_enc bytea,
    phone_enc       bytea,
    license_no      text,
    employer_type   text        NOT NULL CHECK (employer_type IN ('OWN', 'CONTRACTED')),
    status          text        NOT NULL CHECK (status IN ('ACTIVE', 'PASSIVE')),
    created_at      timestamptz NOT NULL,
    created_by      uuid        NOT NULL,
    updated_at      timestamptz,
    updated_by      uuid,
    version         integer     NOT NULL DEFAULT 0
);

COMMENT ON TABLE driver IS 'Şoför';
COMMENT ON COLUMN driver.national_id_enc IS 'TCKN, pgp_sym_encrypt ile şifreli (KVKK); anahtar uygulama gizlisidir';
COMMENT ON COLUMN driver.phone_enc IS 'Telefon, pgp_sym_encrypt ile şifreli (KVKK)';

CREATE TABLE vehicle (
    id           uuid        PRIMARY KEY,
    plate_no     text        NOT NULL UNIQUE,
    vehicle_type text        NOT NULL,
    status       text        NOT NULL CHECK (status IN ('ACTIVE', 'PASSIVE')),
    created_at   timestamptz NOT NULL,
    created_by   uuid        NOT NULL,
    updated_at   timestamptz,
    updated_by   uuid,
    version      integer     NOT NULL DEFAULT 0
);

CREATE TABLE product (
    id             uuid        PRIMARY KEY,
    code           text        NOT NULL UNIQUE,
    name           text        NOT NULL,
    yarn_count     text,
    composition    text,
    packaging_type text        NOT NULL CHECK (packaging_type IN ('BAG', 'BOX', 'PALLET', 'BOBBIN')),
    status         text        NOT NULL CHECK (status IN ('ACTIVE', 'PASSIVE')),
    created_at     timestamptz NOT NULL,
    created_by     uuid        NOT NULL,
    updated_at     timestamptz,
    updated_by     uuid,
    version        integer     NOT NULL DEFAULT 0
);

COMMENT ON TABLE product IS 'İplik / ürün';

CREATE TABLE quality_grade (
    id          uuid        PRIMARY KEY,
    code        text        NOT NULL UNIQUE,
    name        text        NOT NULL,
    label_color text,
    status      text        NOT NULL CHECK (status IN ('ACTIVE', 'PASSIVE')),
    created_at  timestamptz NOT NULL,
    created_by  uuid        NOT NULL,
    updated_at  timestamptz,
    updated_by  uuid,
    version     integer     NOT NULL DEFAULT 0
);

CREATE TABLE production_lot (
    id                uuid        PRIMARY KEY,
    site_id           uuid        NOT NULL REFERENCES site (id),
    lot_no            text        NOT NULL,
    product_id        uuid        NOT NULL REFERENCES product (id),
    quality_grade_id  uuid        NOT NULL REFERENCES quality_grade (id),
    production_date   date        NOT NULL,
    quality_locked_at timestamptz,
    quality_locked_by uuid,
    status            text        NOT NULL CHECK (status IN ('ACTIVE', 'PASSIVE')),
    created_at        timestamptz NOT NULL,
    created_by        uuid        NOT NULL,
    updated_at        timestamptz,
    updated_by        uuid,
    version           integer     NOT NULL DEFAULT 0,
    UNIQUE (site_id, lot_no),
    CONSTRAINT production_lot_quality_lock_complete
        CHECK ((quality_locked_at IS NULL) = (quality_locked_by IS NULL))
);

COMMENT ON TABLE production_lot IS 'Parti; kalite kilitlendikten sonra kalite değişemez (rapor Bölüm 10)';

CREATE INDEX production_lot_product_quality_idx ON production_lot (product_id, quality_grade_id);

-- Rapor Bölüm 10: kilitli partinin kalitesi ve kilidi geri alınamaz.
CREATE FUNCTION guard_lot_quality_lock()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.quality_locked_at IS NULL THEN
        RETURN NEW;
    END IF;
    IF NEW.quality_grade_id IS DISTINCT FROM OLD.quality_grade_id
            OR NEW.quality_locked_at IS DISTINCT FROM OLD.quality_locked_at
            OR NEW.quality_locked_by IS DISTINCT FROM OLD.quality_locked_by
            OR NEW.product_id IS DISTINCT FROM OLD.product_id THEN
        RAISE EXCEPTION 'Lot quality is locked lotId=%', OLD.id USING ERRCODE = 'YS004';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER production_lot_quality_lock
    BEFORE UPDATE ON production_lot
    FOR EACH ROW EXECUTE FUNCTION guard_lot_quality_lock();

CREATE TRIGGER production_lot_audit_local
    AFTER INSERT OR UPDATE OR DELETE ON production_lot
    FOR EACH ROW EXECUTE FUNCTION audit_local_capture();

CREATE TABLE witness_sample (
    id               uuid        PRIMARY KEY,
    lot_id           uuid        NOT NULL REFERENCES production_lot (id),
    sample_code      text        NOT NULL UNIQUE,
    storage_location text        NOT NULL,
    taken_at         timestamptz NOT NULL,
    disposed_at      timestamptz,
    created_at       timestamptz NOT NULL,
    created_by       uuid        NOT NULL,
    updated_at       timestamptz,
    updated_by       uuid,
    version          integer     NOT NULL DEFAULT 0,
    CONSTRAINT witness_sample_disposed_after_taken CHECK (disposed_at IS NULL OR disposed_at >= taken_at)
);

COMMENT ON TABLE witness_sample IS 'Şahit numune';

CREATE TABLE tare_standard (
    id                uuid          PRIMARY KEY,
    site_id           uuid          NOT NULL REFERENCES site (id),
    packaging_type    text          NOT NULL CHECK (packaging_type IN ('BAG', 'BOX', 'PALLET', 'BOBBIN')),
    standard_tare_kg  numeric(12, 3) NOT NULL CHECK (standard_tare_kg >= 0),
    max_deviation_pct numeric(5, 2)  NOT NULL CHECK (max_deviation_pct BETWEEN 0 AND 100),
    created_at        timestamptz   NOT NULL,
    created_by        uuid          NOT NULL,
    updated_at        timestamptz,
    updated_by        uuid,
    version           integer       NOT NULL DEFAULT 0,
    UNIQUE (site_id, packaging_type)
);

COMMENT ON TABLE tare_standard IS 'Standart dara';

CREATE TABLE user_profile (
    id                uuid        PRIMARY KEY,
    keycloak_user_id  uuid        NOT NULL UNIQUE,
    full_name         text        NOT NULL,
    user_type         text        NOT NULL CHECK (user_type IN ('INTERNAL', 'DRIVER', 'SUBCONTRACTOR')),
    subcontractor_id  uuid        REFERENCES subcontractor (id),
    driver_id         uuid        REFERENCES driver (id),
    status            text        NOT NULL CHECK (status IN ('ACTIVE', 'PASSIVE')),
    created_at        timestamptz NOT NULL,
    created_by        uuid        NOT NULL,
    updated_at        timestamptz,
    updated_by        uuid,
    version           integer     NOT NULL DEFAULT 0,
    CONSTRAINT user_profile_scope_matches_type CHECK (
        CASE user_type
            WHEN 'DRIVER' THEN driver_id IS NOT NULL AND subcontractor_id IS NULL
            WHEN 'SUBCONTRACTOR' THEN subcontractor_id IS NOT NULL AND driver_id IS NULL
            ELSE driver_id IS NULL AND subcontractor_id IS NULL
        END)
);

COMMENT ON TABLE user_profile IS 'Keycloak kullanıcısının fasoncu/şoför bağı (yetkilendirme analizi, kapsam claim''leri)';

CREATE TABLE user_site (
    id         uuid        PRIMARY KEY,
    user_id    uuid        NOT NULL REFERENCES user_profile (id),
    site_id    uuid        NOT NULL REFERENCES site (id),
    revoked_at timestamptz,
    revoked_by uuid,
    created_at timestamptz NOT NULL,
    created_by uuid        NOT NULL,
    updated_at timestamptz,
    updated_by uuid,
    version    integer     NOT NULL DEFAULT 0,
    CONSTRAINT user_site_revocation_complete CHECK ((revoked_at IS NULL) = (revoked_by IS NULL))
);

-- Yetki satırı silinmez, revoked_at ile kapatılır (GEN-04); aynı anda tek etkin yetki.
CREATE UNIQUE INDEX user_site_active_uq ON user_site (user_id, site_id) WHERE revoked_at IS NULL;

COMMENT ON TABLE user_site IS 'Depo ve kalite kullanıcısının yetkili olduğu lokasyonlar';

CREATE TABLE evidence_requirement (
    id            uuid        PRIMARY KEY,
    stage         text        NOT NULL CHECK (stage IN ('DISPATCH', 'DELIVERY', 'CLAIM')),
    evidence_type text        NOT NULL CHECK (evidence_type IN (
        'DISPATCH_SCALE', 'BAG_OVERVIEW', 'BAG_LABEL', 'VEHICLE_PLATE', 'SEAL', 'DELIVERY_SCALE',
        'DELIVERY_BAGS', 'QUALITY_CLAIM', 'SIGNED_DELIVERY_DOC', 'LAB_RESULT')),
    min_count     integer     NOT NULL CHECK (min_count >= 0),
    mandatory     boolean     NOT NULL,
    created_at    timestamptz NOT NULL,
    created_by    uuid        NOT NULL,
    updated_at    timestamptz,
    updated_by    uuid,
    version       integer     NOT NULL DEFAULT 0,
    UNIQUE (stage, evidence_type)
);

COMMENT ON TABLE evidence_requirement IS 'Aşama başına zorunlu kanıtlar (rapor Bölüm 11.1)';

CREATE TABLE system_setting (
    id            uuid        PRIMARY KEY,
    setting_key   text        NOT NULL UNIQUE CHECK (setting_key ~ '^[a-z][a-z0-9]*(\.[a-z][a-z0-9-]*)*$'),
    setting_value jsonb       NOT NULL,
    created_at    timestamptz NOT NULL,
    created_by    uuid        NOT NULL,
    updated_at    timestamptz,
    updated_by    uuid,
    version       integer     NOT NULL DEFAULT 0
);

COMMENT ON TABLE system_setting IS 'İş ayarları (itiraz süresi, rastgele kontrol oranı, saklama süresi); konfigürasyon değildir';

CREATE TRIGGER system_setting_audit_local
    AFTER INSERT OR UPDATE OR DELETE ON system_setting
    FOR EACH ROW EXECUTE FUNCTION audit_local_capture();

GRANT SELECT, INSERT, UPDATE ON
    site, subcontractor, driver, vehicle, product, quality_grade, production_lot, witness_sample,
    tare_standard, user_profile, user_site, evidence_requirement, system_setting
    TO ${app_role};
