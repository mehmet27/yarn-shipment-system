# Sunucuya kurulum rehberi (Ubuntu, tek sunucu)

Bu rehber, kiralanmış bir Ubuntu 22.04/24.04 sunucuya sistemi Docker ile kurar. Dışarıya yalnızca 80 ve 443 açılır;
veritabanı, Redis, MinIO ve uygulama iç ağda kalır. HTTPS sertifikasını Caddy kendiliğinden alır.

## 0. Hazırlık

- **Sunucu:** en az 4 vCPU, 8 GB RAM, 60 GB disk (Java servisleri ve Keycloak bellek yer). Docker'ı betik kurar.
- **Üç adres (DNS A kaydı, hepsi sunucunun IP'sine):**
  - `APP_HOST` uygulama, örn. `iplik.firma.com.tr`
  - `AUTH_HOST` giriş (Keycloak), örn. `giris.firma.com.tr`
  - `FILES_HOST` fotoğraf/belge yükleme, örn. `dosya.firma.com.tr`
- Güvenlik duvarında 22, 80 ve 443 portlarını açın (başka port gerekmez).

## 1. Kurulum

```bash
git clone https://github.com/mehmet27/yarn-shipment-system.git
cd yarn-shipment-system
./deploy/prod/install.sh          # ilk çalışmada .env oluşturur ve durur
nano deploy/prod/.env             # üç adresi, ACME_EMAIL ve INITIAL_ADMIN_EMAIL alanlarını doldurun
./deploy/prod/install.sh          # sırları üretir, imajları derler, her şeyi başlatır
```

İlk derleme 10-20 dakika sürer. Bitince `docker compose -f deploy/prod/docker-compose.yml --env-file deploy/prod/.env ps`
ile tüm servislerin `running`/`healthy` olduğunu görün. Betik sonunda yönetici bilgilerini yazar; `deploy/prod/.env`
dosyasını güvenli bir yere yedekleyin (içindeki sırlar kaybolursa veriye erişilemez).

## 2. İlk giriş ve OTP (zorunlu)

1. `https://APP_HOST` adresini açın, `admin` ve `INITIAL_ADMIN_PASSWORD` ile girin. Keycloak yeni şifre ve OTP
   (Google Authenticator vb.) kurulumu ister.
2. ADMIN ve KALİTE rolleri, token'da OTP kanıtı (`acr=2`) yoksa **hiçbir yetki almaz**. Bu yüzden
   `docs/yonetim/uretim-keycloak-otp.md` adımlarını Keycloak konsolunda (`https://AUTH_HOST/admin`, kullanıcı `admin`,
   şifre `.env` içindeki `KEYCLOAK_ADMIN_PASSWORD`) bir kez uygulayın. Bu adım bitmeden uygulamada yönetici ekranları açılmaz.
3. Uygulamada lokasyonları ve temel tanımları girin, ardından Kullanıcılar ekranından depo, kalite, şoför, fasoncu hesaplarını açın.
   Üretimde test kullanıcısı yoktur; `local` profili hiçbir yerde açılmaz.

## 3. Günlük işler

| İş | Komut (depo kökünde) |
|---|---|
| Güncelleme | `git pull && ./deploy/prod/install.sh` |
| Durum / günlük | `docker compose -f deploy/prod/docker-compose.yml --env-file deploy/prod/.env ps` · `... logs -f gateway` |
| Durdurma | `... down` (veriler korunur; `down -v` **tüm veriyi siler**) |
| Yedek | `... exec postgres pg_dump -U postgres yarnshipment > yedek.sql`; ayrıca `minio-data` ve `deploy/prod/.env` |

## Bilinen sınırlar

- Derleme ve imajlar bu ortamda Docker olmadığı için çalıştırılarak denenmedi; ilk kurulumda bir adım hata verirse
  `docker compose ... logs <servis>` çıktısını paylaşın.
- Veritabanı şifresi yalnızca ilk açılışta yazılır; sonradan `.env` içinde değiştirmek veritabanına yansımaz.
- SMS ve e-posta doğrulaması yoktur: yönetici kullanıcı açarken geçici şifreyi kendisi yazar ve unutulan şifreyi
  Kullanıcılar ekranından yeni geçici şifre vererek sıfırlar; kişi ilk girişte şifresini değiştirir.
- Tek uygulama süreci ve tek veritabanı: pilot ölçeği için uygundur, yüksek erişilebilirlik sağlamaz.
- Kafka kaldırıldı. Eski (çok servisli) kurulumdan geçerken: önce eski servisleri durdurmadan `outbox_event` tablolarında
  `published_at IS NULL` satır kalmadığını ve tüketici gecikmesinin sıfır olduğunu doğrulayın, veritabanı yedeği alın,
  sonra yeni imajla `docker compose up -d --build` çalıştırın. Açılışta şema başına Flyway migration'ları çalışır;
  `outbox_event` ve `reporting.processed_event` tabloları düşürülür. `kafka-data` birimi artık kullanılmaz
  (`docker volume rm yarn-shipment-prod_kafka-data` ile silinebilir).
- **Audit rolleri (mevcut veritabanını yükseltirken).** Yeni kurulumda `audit_writer` ve `audit_reader` rolleri
  `deploy/postgres/initdb` ile gelir. Bu sürümden önce kurulmuş bir veritabanında initdb tekrar çalışmaz; uygulama
  `yarnshipment` kullanıcısı rol oluşturamadığı için açılışta "audit_writer / audit_reader roles are missing" hatasıyla
  durur. Tek seferlik, yönetici olarak (veri korunur):
  ```bash
  docker compose -f deploy/prod/docker-compose.yml --env-file deploy/prod/.env exec postgres psql -U postgres -d yarnshipment -c \
    "CREATE ROLE audit_writer NOLOGIN; CREATE ROLE audit_reader NOLOGIN; GRANT audit_writer, audit_reader TO yarnshipment WITH INHERIT FALSE, SET TRUE;"
  ```
  Yerel Compose'ta hacmi sıfırlamak (`down -v`) da yeterlidir. Aynı açılışta `platform` şeması kurulur, her modülün `audit_local` ve
  `idempotency_record` verisi oraya taşınır ve eski tablolar düşer; yükseltmeden önce yedek alın.
- Keycloak yönetim konsolu internetten erişilebilir; güçlü şifre kullanın, gerekirse Caddyfile'da IP ile kısıtlayın.
