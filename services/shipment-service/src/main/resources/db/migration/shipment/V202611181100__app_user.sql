-- Giriş yapan fabrika personeli (yönetici, depo, kalite, müdür, genel müdür). Kimlik artık Keycloak'ta değil burada
-- tutulur; kimlik (id) eski Keycloak kullanıcı kimliğiyle aynıdır, böylece created_by / actor_id sütunları ve denetim
-- kayıtları bozulmaz. Şifre yalnızca tuzlu BCrypt özeti olarak saklanır ("!" = henüz şifre belirlenmedi, giriş yapılamaz).
-- Gizli veri taşıdığı için denetim tetikleyicisi yoktur.

CREATE TABLE app_user (
    id                   uuid        PRIMARY KEY,
    username             text        NOT NULL CHECK (length(btrim(username)) BETWEEN 3 AND 100),
    full_name            text        NOT NULL CHECK (length(btrim(full_name)) BETWEEN 2 AND 200),
    role                 text        NOT NULL CHECK (role IN ('ADMIN', 'WAREHOUSE', 'QUALITY', 'MANAGER', 'GENERAL_MANAGER')),
    password_hash        text        NOT NULL,
    enabled              boolean     NOT NULL DEFAULT true,
    password_change_required boolean NOT NULL DEFAULT true,
    failed_attempts      integer     NOT NULL DEFAULT 0 CHECK (failed_attempts >= 0),
    locked_until         timestamptz,
    site_ids             uuid[]      NOT NULL DEFAULT '{}',
    password_changed_at  timestamptz,
    last_login_at        timestamptz,
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now()
);

-- Giriş adı büyük/küçük harf duyarsız tektir.
CREATE UNIQUE INDEX ux_app_user_username ON app_user (lower(username));

COMMENT ON TABLE app_user IS 'Giriş hesapları: kullanıcı adı, rol, şifre özeti (BCrypt), kilit ve lokasyon kapsamı';
COMMENT ON COLUMN app_user.password_hash IS 'Spring DelegatingPasswordEncoder biçimi, ör. {bcrypt}...; "!" giriş kapalı';
COMMENT ON COLUMN app_user.site_ids IS 'Kullanıcının göreceği lokasyonlar; hesap açılırken etkin tüm lokasyonlar yazılır';

GRANT SELECT, INSERT, UPDATE ON app_user TO ${app_role};
