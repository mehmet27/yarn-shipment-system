-- SMS ile şifre belirleme/sıfırlama kodları. Kullanıcı Keycloak'tadır; burada yalnızca kodun tuzlu özeti, süresi ve
-- yanlış deneme sayısı tutulur. Kod düz metin saklanmaz. Kullanıcı başına tek satır vardır (yeni kod eskisini değiştirir).
-- Gizli veri taşıdığı için denetim tetikleyicisi yoktur.

CREATE TABLE md_password_reset_code (
    user_id    uuid        PRIMARY KEY,
    code_hash  text        NOT NULL,
    salt       text        NOT NULL,
    issued_at  timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    attempts   integer     NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    CHECK (expires_at > issued_at)
);

COMMENT ON TABLE md_password_reset_code IS 'Şifre belirleme SMS kodunun tuzlu özeti; kullanıcı başına tek satır';

GRANT SELECT, INSERT, UPDATE, DELETE ON md_password_reset_code TO ${app_role};
