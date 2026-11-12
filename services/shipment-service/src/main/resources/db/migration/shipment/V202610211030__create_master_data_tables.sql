-- Ana veri okuma tabloları. Ayrı master-data servisi (masterdata_db) gelene kadar shipment_db içinde `md_` önekiyle
-- durur; taşınırken bu dosya ve masterdata paketi birlikte taşınır. Yönetim (ekle, güncelle, pasifleştir) sonraki
-- işlerdir; şimdilik uygulama rolü yalnızca okur. Kayıt silinmez, pasifleştirilir (active = false).

CREATE TABLE md_site (
    id         uuid        PRIMARY KEY,
    name       text        NOT NULL,
    active     boolean     NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL,
    created_by uuid        NOT NULL
);

CREATE TABLE md_subcontractor (
    id         uuid        PRIMARY KEY,
    title      text        NOT NULL,
    active     boolean     NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL,
    created_by uuid        NOT NULL
);

CREATE TABLE md_driver (
    id              uuid        PRIMARY KEY,
    full_name       text        NOT NULL,
    subcontractor_id uuid       REFERENCES md_subcontractor (id),
    active          boolean     NOT NULL DEFAULT true,
    created_at      timestamptz NOT NULL,
    created_by      uuid        NOT NULL
);

CREATE INDEX md_driver_subcontractor_idx ON md_driver (subcontractor_id);

CREATE TABLE md_vehicle (
    id         uuid        PRIMARY KEY,
    plate_no   text        NOT NULL UNIQUE,
    active     boolean     NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL,
    created_by uuid        NOT NULL
);

GRANT SELECT ON md_site, md_subcontractor, md_driver, md_vehicle TO ${app_role};
