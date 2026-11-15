-- MEVCUT veritabanında "admin" kullanıcısı ve Yetkiler ekranı (role_permission) dışındaki TÜM veriyi siler.
-- Şema, migration geçmişi (flyway_schema_history), fonksiyonlar, görünümler ve roller olduğu gibi kalır; uygulama
-- yeniden kurulum gerektirmeden açılmaya devam eder. GERİ ALINAMAZ: önce yedek alın (DEPLOY.md, "Yedekleme").
--
-- Kullanım (depo kökünden, Docker Compose yığını çalışırken; ayrıntı: README.md, "Yalnızca admin bırakma"):
--   docker compose -f deploy/docker-compose.yml exec -T postgres \
--     psql -U postgres -d yarnshipment -v ON_ERROR_STOP=1 -v onay=SIL < deploy/postgres/yalnizca-admin-birak.sql
--
--   -v onay=SIL         zorunlu; verilmezse hiçbir şey silinmez
--   -v ayarlari_koru=1  isteğe bağlı; app_setting (fire oranı, kantar toleransı, firma adı/renk/logo) de kalsın
--
-- Kalanlar: app_user (yalnızca admin; kilit/başarısız deneme/şifre talebi temizlenir, lokasyon listesi boşaltılır),
-- role_permission (olduğu gibi), flyway_schema_history tabloları, audit_chain_head (denetim zinciri başlangıca alınır).
-- Silinenler: sevkiyatlar, kantar/tartı, stok ve bakiye hareketleri, fireler, ana veriler (lokasyon, ürün, alt yüklenici,
-- araç, şoför, tedarikçi...), uyuşmazlık/kalite itirazları, kanıt kayıtları, denetim kayıtları, idempotency kayıtları,
-- admin dışındaki kullanıcılar. Kanıt DOSYALARI diskte durur (EVIDENCE_STORAGE_ROOT); onları ayrıca silmelisiniz.
--
-- Tek transaction'dır: bir hata olursa hiçbir şey silinmez. Çalıştırırken uygulamayı durdurmanız önerilir.

\if :{?onay}
\else
  \echo 'DURDU: Onay yok. Çalıştırmak için -v onay=SIL ekleyin. Hiçbir şey silinmedi.'
  \quit
\endif
\if :{?ayarlari_koru}
\else
  \set ayarlari_koru 0
\endif

BEGIN;

-- psql değişkenleri $$ bloğunun içine işlemez; değer oturum ayarı olarak geçirilir.
SELECT set_config('yas.onay', :'onay', true), set_config('yas.ayarlari_koru', :'ayarlari_koru', true);

DO $$
DECLARE
    tablolar text;
    korunacak text[] := ARRAY['flyway_schema_history', 'app_user', 'role_permission', 'audit_chain_head'];
BEGIN
    IF current_setting('yas.onay') <> 'SIL' THEN
        RAISE EXCEPTION 'Onay değeri SIL olmalı';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM shipment.app_user WHERE lower(username) = 'admin' AND role = 'ADMIN') THEN
        RAISE EXCEPTION 'admin kullanıcısı bulunamadı; hiçbir şey silinmedi';
    END IF;
    IF current_setting('yas.ayarlari_koru') = '1' THEN
        korunacak := array_append(korunacak, 'app_setting');
    END IF;

    -- Uygulama şemalarındaki tüm tablolar (bölüm tabloları kendi ana tablosuyla birlikte boşalır). Liste dinamik olduğu
    -- için ileride eklenen tablolar da otomatik temizlenir; CASCADE kullanılmaz, korunan bir tabloya FK varsa hata verir.
    SELECT string_agg(format('%I.%I', n.nspname, c.relname), ', ' ORDER BY n.nspname, c.relname)
      INTO tablolar
      FROM pg_class c
      JOIN pg_namespace n ON n.oid = c.relnamespace
     WHERE c.relkind IN ('r', 'p')
       AND NOT c.relispartition
       AND n.nspname IN ('platform', 'shipment', 'evidence', 'discrepancy', 'reporting', 'audit')
       AND c.relname <> ALL (korunacak);

    EXECUTE 'TRUNCATE TABLE ' || tablolar || ' RESTART IDENTITY';
END
$$;

-- Admin dışındaki hesaplar.
DELETE FROM shipment.app_user WHERE lower(username) <> 'admin';
-- Silinen lokasyonlara işaret eden listeyi temizle; kilit ve şifre talebi kalmasın.
UPDATE shipment.app_user
   SET site_ids = '{}', failed_attempts = 0, locked_until = NULL, password_requested_at = NULL
 WHERE lower(username) = 'admin';

-- Denetim zincirinin son halkası boşalan audit_log ile uyumlu olsun (ilk kayıt sıfır özetten başlar).
UPDATE audit.audit_chain_head SET last_id = 0, last_hash = repeat('0', 64);

-- Sonuç özeti (yalnızca bilgi): kalan kullanıcı ve izin sayısı.
SELECT username, role FROM shipment.app_user;
SELECT role, count(*) AS izin_sayisi FROM shipment.role_permission GROUP BY role ORDER BY role;

COMMIT;
