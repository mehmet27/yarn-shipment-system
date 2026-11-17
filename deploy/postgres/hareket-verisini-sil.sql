-- MEVCUT veritabanında yalnızca HAREKET VERİSİNİ siler; kullanıcılar, Yetkiler ekranı ve tanımlar kalır.
-- Şema, migration geçmişi (flyway_schema_history), fonksiyonlar, görünümler ve roller olduğu gibi kalır; uygulama
-- yeniden kurulum gerektirmeden açılmaya devam eder. GERİ ALINAMAZ: önce yedek alın (DEPLOY.md, "Yedekleme").
-- (Yalnızca admin kalsın istiyorsanız yalnizca-admin-birak.sql kullanılır.)
--
-- Kullanım (depo kökünden, Docker Compose yığını çalışırken; PowerShell'de tek satır, ayrıntı: README.md):
--   Get-Content deploy/postgres/hareket-verisini-sil.sql -Raw -Encoding UTF8 | docker compose -f deploy/docker-compose.yml exec -T postgres psql -U postgres -d yarnshipment -v ON_ERROR_STOP=1 -v onay=SIL
-- (bash:  ... exec -T postgres psql -U postgres -d yarnshipment -v ON_ERROR_STOP=1 -v onay=SIL < deploy/postgres/hareket-verisini-sil.sql)
--
--   -v onay=SIL         zorunlu; verilmezse hiçbir şey silinmez
--   -v ayarlari_sil=1   isteğe bağlı; app_setting (fire oranı, kantar toleransı, firma adı/renk/logo) de silinsin
--
-- Kalanlar: app_user (tüm hesaplar, şifreler, roller; yalnızca kilit/başarısız deneme/şifre talebi temizlenir),
-- staff_work_area, role_permission, tüm tanımlar (md_* tabloları: lokasyon, fasoncu, şoför, araç, iplik, kalite sınıfı,
-- tedarikçi ve partiler), app_setting, flyway_schema_history tabloları, audit_chain_head (denetim zinciri başlangıca
-- alınır).
-- Silinenler: sevkiyatlar, kantar/tartı, stok ve bakiye hareketleri, fireler, düzeltmeler, uyuşmazlık/kalite itirazları,
-- kanıt kayıtları, denetim kayıtları, idempotency kayıtları. Kanıt DOSYALARI diskte durur (EVIDENCE_STORAGE_ROOT);
-- onları ayrıca silmelisiniz.
--
-- Tek transaction'dır: bir hata olursa hiçbir şey silinmez. Çalıştırırken uygulamayı durdurmanız önerilir.

\if :{?onay}
\else
  \echo 'DURDU: Onay yok. Çalıştırmak için -v onay=SIL ekleyin. Hiçbir şey silinmedi.'
  \quit
\endif
\if :{?ayarlari_sil}
\else
  \set ayarlari_sil 0
\endif

BEGIN;

-- psql değişkenleri $$ bloğunun içine işlemez; değer oturum ayarı olarak geçirilir.
SELECT set_config('yas.onay', :'onay', true), set_config('yas.ayarlari_sil', :'ayarlari_sil', true);

DO $$
DECLARE
    tablolar text;
    korunacak text[] := ARRAY['flyway_schema_history', 'app_user', 'staff_work_area', 'role_permission',
                          'audit_chain_head', 'app_setting'];
BEGIN
    IF current_setting('yas.onay') <> 'SIL' THEN
        RAISE EXCEPTION 'Onay değeri SIL olmalı';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM shipment.app_user WHERE lower(username) = 'admin' AND role = 'ADMIN') THEN
        RAISE EXCEPTION 'admin kullanıcısı bulunamadı; yanlış veritabanı olabilir, hiçbir şey silinmedi';
    END IF;
    IF current_setting('yas.ayarlari_sil') = '1' THEN
        korunacak := array_remove(korunacak, 'app_setting');
    END IF;

    -- Uygulama şemalarındaki tüm tablolar (tanım tabloları md_* hariç) (bölüm tabloları kendi ana tablosuyla birlikte boşalır). Liste dinamik olduğu
    -- için ileride eklenen tablolar da otomatik temizlenir; CASCADE kullanılmaz, korunan bir tabloya FK varsa hata verir.
    SELECT string_agg(format('%I.%I', n.nspname, c.relname), ', ' ORDER BY n.nspname, c.relname)
      INTO tablolar
      FROM pg_class c
      JOIN pg_namespace n ON n.oid = c.relnamespace
     WHERE c.relkind IN ('r', 'p')
       AND NOT c.relispartition
       AND n.nspname IN ('platform', 'shipment', 'evidence', 'discrepancy', 'reporting', 'audit')
       AND c.relname NOT LIKE 'md\_%'
       AND c.relname <> ALL (korunacak);

    EXECUTE 'TRUNCATE TABLE ' || tablolar || ' RESTART IDENTITY';
END
$$;

-- Kilit ve şifre talebi kalmasın (hesaplar, roller ve şifreler olduğu gibi kalır).
UPDATE shipment.app_user SET failed_attempts = 0, locked_until = NULL, password_requested_at = NULL;

-- Denetim zincirinin son halkası boşalan audit_log ile uyumlu olsun (ilk kayıt sıfır özetten başlar).
UPDATE audit.audit_chain_head SET last_id = 0, last_hash = repeat('0', 64);

-- Sonuç özeti (yalnızca bilgi): kalan hesap ve izin sayısı.
SELECT role, count(*) AS hesap_sayisi FROM shipment.app_user GROUP BY role ORDER BY role;
SELECT role, count(*) AS izin_sayisi FROM shipment.role_permission GROUP BY role ORDER BY role;

COMMIT;
