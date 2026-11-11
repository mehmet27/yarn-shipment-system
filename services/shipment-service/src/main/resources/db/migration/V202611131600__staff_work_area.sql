-- Kalite rolündeki kullanıcının çalışma alanı: merkez fabrikanın kumaşı mı, fasoncuların kumaşı mı. Başka roller
-- için satır yazılmaz; satırı olmayan kullanıcı iki alanı da görür. Kimlik Keycloak'ta, bu bilgi yerelde tutulur.
CREATE TABLE staff_work_area (
    user_id    uuid        PRIMARY KEY,
    work_area  text        NOT NULL CHECK (work_area IN ('CENTRAL', 'SUBCONTRACTOR')),
    created_at timestamptz NOT NULL DEFAULT now()
);

COMMENT ON TABLE staff_work_area IS 'Kalite kullanıcısının çalışma alanı (merkez / fasoncu)';

GRANT SELECT, INSERT, UPDATE ON staff_work_area TO ${app_role};
