-- Kumaş cinsi için varsayılan iplik karışımı (reçete): kumaş girişinde oranlar hazır dolu gelir, kaliteci düzeltebilir.
-- Reçete iplik cinsi düzeyindedir; aynı cinsin birden fazla partisi olduğunda oran, elde bulunan kiloya göre bölünür.
-- Kayıt silinmez, pasifleştirilir.
CREATE TABLE md_fabric_recipe (
    id          uuid        PRIMARY KEY,
    fabric_type text        NOT NULL CHECK (length(btrim(fabric_type)) > 0),
    lines       jsonb       NOT NULL CHECK (jsonb_typeof(lines) = 'array' AND jsonb_array_length(lines) > 0),
    active      boolean     NOT NULL DEFAULT true,
    created_at  timestamptz NOT NULL,
    created_by  uuid        NOT NULL,
    updated_at  timestamptz
);

COMMENT ON TABLE md_fabric_recipe IS 'Kumaş cinsi başına varsayılan iplik karışımı';
COMMENT ON COLUMN md_fabric_recipe.lines IS 'Karışım: [{"yarnType": text, "ratioPct": sayı}]; oranların toplamı 100';

CREATE UNIQUE INDEX md_fabric_recipe_type_key ON md_fabric_recipe (lower(btrim(fabric_type)));

GRANT SELECT, INSERT, UPDATE ON md_fabric_recipe TO ${app_role};
