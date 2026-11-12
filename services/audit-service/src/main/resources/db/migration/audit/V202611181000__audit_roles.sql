-- Tek uygulamaya geçişten sonra denetim kaydı için rol ayrımı geri gelir (docs/veritabani-tasarimi.md, "Audit yetkileri").
--
--   audit_writer  tablolara hiçbir yetkisi yoktur; yalnızca audit_append() ve audit_verify_chain() çalıştırır
--   audit_reader  audit ekranı için yalnızca SELECT
--
-- İki rol de giriş yapamaz (NOLOGIN). Uygulamanın bağlandığı kullanıcı ikisinin üyesidir ama yetkilerini kendiliğinden
-- miras almaz (INHERIT FALSE); denetim adaptörleri ilgili işi SET LOCAL ROLE ile o rolün altında çalıştırır, böylece
-- yazma koduyla okuma kodu birbirinin yetkisini kullanamaz.
--
-- Roller yoksa bu migration oluşturur (kullanıcının CREATEROLE yetkisi gerekir). Yerel Compose ve üretim kurulumunda
-- roller deploy/postgres/initdb ile gelir; yetkisiz bir kullanıcıyla mevcut veritabanı yükseltiliyorsa DEPLOY.md'deki
-- tek seferlik komutu yönetici olarak çalıştırın.
--
-- Flyway yer tutucusu:
--   ${app_role}  uygulamanın bağlandığı rol

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'audit_writer') THEN
        CREATE ROLE audit_writer NOLOGIN;
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'audit_reader') THEN
        CREATE ROLE audit_reader NOLOGIN;
    END IF;
EXCEPTION
    WHEN insufficient_privilege THEN
        RAISE EXCEPTION 'audit_writer / audit_reader roles are missing and this user cannot create roles; see DEPLOY.md (audit roles)';
END
$$;

GRANT USAGE ON SCHEMA ${flyway:defaultSchema} TO audit_writer, audit_reader;
GRANT EXECUTE ON FUNCTION audit_append(jsonb), audit_verify_chain(bigint, bigint),
    create_monthly_partitions(regclass, date, integer) TO audit_writer;
GRANT SELECT ON audit_log, audit_chain_check TO audit_reader;

DO $$
BEGIN
    IF NOT pg_has_role('${app_role}', 'audit_writer', 'SET') THEN
        GRANT audit_writer TO ${app_role} WITH INHERIT FALSE, SET TRUE;
    END IF;
    IF NOT pg_has_role('${app_role}', 'audit_reader', 'SET') THEN
        GRANT audit_reader TO ${app_role} WITH INHERIT FALSE, SET TRUE;
    END IF;
END
$$;
