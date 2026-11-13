# Sunucuya kurulum rehberi (Ubuntu, tek sunucu)

Bu rehber, kiralanmış bir Ubuntu 22.04/24.04 sunucuya sistemi Docker ile kurar. Dışarıya yalnızca 80 ve 443 açılır;
veritabanı ve uygulama iç ağda kalır. HTTPS sertifikasını Caddy kendiliğinden alır.

## 0. Hazırlık

- **Sunucu:** en az 4 vCPU, 8 GB RAM, 60 GB disk (Java uygulaması bellek yer). Docker'ı betik kurar.
- **Bir adres (DNS A kaydı, sunucunun IP'sine):** `APP_HOST` uygulama, örn. `iplik.firma.com.tr`. Fotoğraf ve belgeler
  de bu adresten yüklenir/indirilir; ayrı bir dosya adresi yoktur.
- **Disk:** kanıt dosyaları `evidence-data` Docker biriminde tutulur (tahminen yılda 6-22 GB; 10 yılda en çok yaklaşık
  220 GB). Yedek için ayrı bir disk ya da bağlı NAS önerilir.
- Güvenlik duvarında 22, 80 ve 443 portlarını açın (başka port gerekmez).

## 1. Kurulum

```bash
git clone https://github.com/mehmet27/yarn-shipment-system.git
cd yarn-shipment-system
./deploy/prod/install.sh          # ilk çalışmada .env oluşturur ve durur
nano deploy/prod/.env             # APP_HOST ve ACME_EMAIL alanlarını doldurun
./deploy/prod/install.sh          # sırları üretir, imajları derler, her şeyi başlatır
```

İlk derleme 10-20 dakika sürer. Bitince `docker compose -f deploy/prod/docker-compose.yml --env-file deploy/prod/.env ps`
ile tüm servislerin `running`/`healthy` olduğunu görün. Betik sonunda yönetici bilgilerini yazar; `deploy/prod/.env`
dosyasını güvenli bir yere yedekleyin (içindeki sırlar kaybolursa veriye erişilemez).

## 2. İlk giriş

1. `https://APP_HOST` adresini açın, `admin` ve `INITIAL_ADMIN_PASSWORD` ile girin. İlk girişte uygulama yeni şifre ister.
2. İkinci faktör (OTP) şimdilik yoktur; ileride eklenebilir. Bu yüzden `admin` şifresini uzun ve tahmin edilmesi zor seçin.
3. Uygulamada lokasyonları ve temel tanımları girin, ardından Kullanıcılar ekranından depo, kalite, şoför, fasoncu hesaplarını açın.
   Üretimde test kullanıcısı yoktur; `local` profili hiçbir yerde açılmaz.

## 3. Günlük işler

| İş | Komut (depo kökünde) |
|---|---|
| Güncelleme | `git pull && ./deploy/prod/install.sh` |
| Durum / günlük | `docker compose -f deploy/prod/docker-compose.yml --env-file deploy/prod/.env ps` · `... logs -f yarnshipment-app` |
| Durdurma | `... down` (veriler korunur; `down -v` **tüm veriyi siler**) |
| Yedek | `./deploy/prod/yedekle.sh` (veritabanı dökümü + kanıt dosyaları; aşağıya bakın) ve `deploy/prod/.env` |

## 4. Yedekleme ve geri yükleme

Kanıt dosyaları (kantar fişi, irsaliye, itiraz belgeleri) sunucu diskindeki `evidence-data` biriminde durur; dosyaların
SHA-256 değeri veritabanındadır, bu yüzden bozulma sonradan doğrulanabilir. Disk kaybına karşı **gece yedeği şarttır**.

```bash
# Elle bir kez: yedek diski /mnt/yedek, şehir dışı kopya (isteğe bağlı) başka bir sunucuda
sudo YEDEK_DIZINI=/mnt/yedek YEDEK_UZAK=yedek@baska-sunucu:/yedekler/iplik ./deploy/prod/yedekle.sh

# Her gece 02:30 (root crontab -e):
30 2 * * * cd /opt/yarn-shipment-system && YEDEK_DIZINI=/mnt/yedek ./deploy/prod/yedekle.sh >> /var/log/yedekle.log 2>&1
```

Betik veritabanını `pg_dump` ile alır (son 30 gün saklanır) ve kanıt klasörünü `rsync` ile **silmeden** kopyalar
(uygulama dosya silmez; yedekte de silinmez). `rsync` kurulu olmalıdır (`apt install rsync`).

Geri yükleme: boş kurulumda servisleri durdurun, `docker compose ... exec -T postgres pg_restore -U postgres -d yarnshipment --clean --if-exists < yarnshipment-<tarih>.dump`
ile veritabanını, `rsync -a /mnt/yedek/evidence/ $(docker volume inspect -f '{{ .Mountpoint }}' yarn-shipment-prod_evidence-data)/`
ile dosyaları yerine koyup `chown -R 10001 <aynı klasör>` çalıştırın ve servisleri başlatın. Ayda bir geri yükleme denemesi yapın.

## Bilinen sınırlar

- Derleme ve imajlar bu ortamda Docker olmadığı için çalıştırılarak denenmedi; ilk kurulumda bir adım hata verirse
  `docker compose ... logs <servis>` çıktısını paylaşın.
- **MinIO'dan geçiş:** eski kurulumda dosyalar MinIO'daydı (`evidence` kovası; MinIO'nun disk düzeni düz dosya değildir,
  `minio-data` klasörü kopyalanarak taşınamaz). Güncellemeden **önce**, eski yığın çalışırken dosyaları düz klasöre alın
  (anahtarlar aynı kalır, veritabanına dokunulmaz):
  `docker run --rm --network yarn-shipment-prod_default -v "$PWD/minio-yedek":/out --entrypoint sh chainguard/minio-client:latest-dev -c 'mc alias set l http://minio:9000 <EVIDENCE_STORAGE_ACCESS_KEY> <EVIDENCE_STORAGE_SECRET_KEY> && mc mirror l/evidence /out'`
  (anahtar değerleri eski `deploy/prod/.env` içindedir). Yeni sürümü kurduktan sonra `minio-yedek/` içeriğini
  `$(docker volume inspect -f '{{ .Mountpoint }}' yarn-shipment-prod_evidence-data)/` altına kopyalayın
  (sonuç: `.../evidence/2026/11/<sevkiyat>/<kanıt>`), `chown -R 10001` yapın ve bir kanıtı arayüzden açarak doğrulayın.
  Doğrulanana kadar `minio-data` birimini silmeyin.
- Dosyalar uygulamanın kendi imzaladığı kısa ömürlü adreslerle yüklenir/indirilir; `EVIDENCE_STORAGE_SIGNING_KEY` değiştirilirse
  açık adresler (en çok 10 dakika) geçersiz olur, kayıtlı dosyalar etkilenmez.
- Veritabanı şifresi yalnızca ilk açılışta yazılır; sonradan `.env` içinde değiştirmek veritabanına yansımaz.
- SMS ve e-posta doğrulaması yoktur: yönetici kullanıcı açarken geçici şifreyi kendisi yazar ve unutulan şifreyi
  Kullanıcılar ekranından yeni geçici şifre vererek sıfırlar; kişi ilk girişte şifresini değiştirir.
- Tek uygulama süreci ve tek veritabanı: pilot ölçeği için uygundur, yüksek erişilebilirlik sağlamaz.
- Oturumlar uygulama belleğindedir: uygulama yeniden başlayınca (güncelleme dahil) herkes yeniden giriş yapar.
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
- **Keycloak'tan geçiş.** Eski kurulumda Keycloak, Redis ve gateway konteynerleri artık yoktur; `up -d --remove-orphans` ile
  temizlenir. Kullanıcılar Keycloak'ta kalmıştır ve yeni `shipment.app_user` tablosuna **otomatik taşınmaz**: açılışta yalnızca
  `admin` hesabı (`INITIAL_ADMIN_PASSWORD` ile) oluşur; diğer kullanıcıları yönetici Kullanıcılar ekranından yeniden açar.
  Eski `keycloak` şeması gerekmiyorsa yedek aldıktan sonra `DROP SCHEMA keycloak CASCADE;` ile silinebilir.
