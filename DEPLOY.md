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

İlk derleme 10-20 dakika sürer. Betik uygulama gerçekten yanıt verene kadar bekler (açılmazsa günlüğün sonunu gösterip
hatayla çıkar), ardından **gece yedeğini kendiliğinden kurar** (bkz. bölüm 4) ve yönetici bilgilerini yazar. Durumu sonradan
`./deploy/prod/durum.sh` ile görün. `deploy/prod/.env` dosyasını güvenli bir yere yedekleyin (yönetici ilk şifresi ve sırlar
içindedir).

## 2. İlk giriş

1. `https://APP_HOST` adresini açın, `admin` ve `INITIAL_ADMIN_PASSWORD` ile girin. İlk girişte uygulama yeni şifre ister.
2. İkinci faktör (OTP) şimdilik yoktur; ileride eklenebilir. Bu yüzden `admin` şifresini uzun ve tahmin edilmesi zor seçin.
3. Kullanıcılar ekranından önce müdür hesabını, ardından depo, kalite ve diğer hesapları açın; temel tanımları (fasoncu, iplik vb.) müdür girer.
   Lokasyon girmeniz gerekmez: hiç lokasyon yoksa uygulama açılışta "Merkez" lokasyonunu kendisi açar ve tüm hesaplara verir.
   Üretimde test kullanıcısı yoktur; `local` profili hiçbir yerde açılmaz.

## 3. Günlük işler

| İş | Komut (depo kökünde) |
|---|---|
| Durum (servisler, son yedek, disk) | `./deploy/prod/durum.sh` (sorun varsa uyarır ve sıfırdan farklı kodla çıkar) |
| Güncelleme | `git pull && ./deploy/prod/install.sh` (önce veritabanı dökümü alır, sonra yeni sürümü başlatır ve açılmasını bekler) |
| Günlük | `docker compose -f deploy/prod/docker-compose.yml --env-file deploy/prod/.env logs -f yarnshipment-app` |
| Durdurma | `docker compose -f deploy/prod/docker-compose.yml --env-file deploy/prod/.env down` (veriler korunur; `down -v` **tüm veriyi siler**) |
| Yedek (elle) | `sudo ./deploy/prod/yedekle.sh` |
| Geri yükleme | `sudo ./deploy/prod/geri-yukle.sh` (aşağıya bakın) |

Sunucuya özel ek Compose ayarı gerekiyorsa (farklı port, bellek sınırı) `deploy/prod/docker-compose.override.yml` dosyasına
yazın; betikler varsa kendiliğinden kullanır, dosya git'e girmez.

## 4. Yedekleme ve geri yükleme

Kanıt dosyaları (kantar fişi, irsaliye, itiraz belgeleri) sunucu diskindeki `evidence-data` biriminde durur; dosyaların
SHA-256 değeri veritabanındadır, bu yüzden bozulma sonradan doğrulanabilir.

**Gece yedeği kendiliğinden kurulur.** `install.sh` her çalıştığında `yedek-kur.sh` ile `/etc/cron.d/yarn-shipment-yedek`
dosyasını yazar: her gece 02:30'da `yedekle.sh` çalışır, çıktısı `/var/log/yarn-shipment-yedek.log` dosyasına eklenir.
Ayarlar `deploy/prod/.env` içindedir; değiştirince `sudo ./deploy/prod/yedek-kur.sh` çalıştırın:

| Ayar | Anlamı | Varsayılan |
|---|---|---|
| `YEDEK_DIZINI` | Yedeğin yazıldığı yer. **Ayrı bir disk ya da bağlı NAS verin**; boşsa yedek uygulamayla aynı diskte durur ve disk bozulursa birlikte kaybolur | `/var/backups/yarn-shipment` |
| `YEDEK_UZAK` | Şehir dışı kopya, `rsync` hedefi (ör. `yedek@baska-sunucu:/yedekler/iplik`; root'un SSH anahtarı tanımlı olmalı) | yok |
| `YEDEK_SAATI` | Gece yedeğinin saati (sunucu saati, `SS:DD`) | `02:30` |
| `YEDEK_GUN` | Veritabanı dökümlerinin saklandığı gün sayısı | `30` |

`yedekle.sh` veritabanını `pg_dump` ile alır, dökümün okunabildiğini doğrular (bozuk döküm yedek sayılmaz, eski dökümler
silinmez) ve kanıt klasörünü `rsync` ile **silmeden** kopyalar (uygulama dosya silmez; yedekte de silinmez). Başarılı her
yedekten sonra `<YEDEK_DIZINI>/son-basarili-yedek.txt` güncellenir; `durum.sh` son yedek 26 saatten eskiyse uyarır.
Gece yedeğini kaldırmak: `sudo ./deploy/prod/yedek-kur.sh --kaldir`.

**Geri yükleme** (aynı sunucuda ya da yeni kurulmuş boş bir sunucuda; önce `install.sh` çalışmış olmalı):

```bash
sudo ./deploy/prod/geri-yukle.sh                                   # YEDEK_DIZINI içindeki en son dökümü geri yükler
sudo ./deploy/prod/geri-yukle.sh /mnt/yedek/veritabani/yarnshipment-20261120-023000.dump   # belirli bir döküm
sudo ./deploy/prod/geri-yukle.sh --kanit-atla                      # yalnızca veritabanı
```

Betik onay ister (`GERI-YUKLE` yazılır; `--evet` ile sormaz), uygulamayı durdurur, **mevcut veritabanının dökümünü
`geri-yukleme-oncesi-<tarih>.dump` adıyla alır**, veritabanını dökümden yeniden kurar, kanıt dosyalarını yerine koyar
(sunucudaki dosyaları silmez), uygulamayı başlatır ve açılmasını bekler. Yeni sunucuya taşırken yedek klasörünü
(`veritabani/` ve `evidence/`) yeni sunucuya kopyalayıp `YEDEK_DIZINI` ile gösterin. Geri yüklemeden sonra herkes yeniden
giriş yapar. **Ayda bir**, yedeği ayrı bir deneme sunucusunda geri yükleyip bir sevkiyatı ve bir kanıt dosyasını açarak deneyin.

## Bilinen sınırlar

- **Ne denendi:** kurulum zinciri (`install.sh` → HTTPS üzerinden arayüz ve API → admin girişi ve şifre değişimi →
  `yedekle.sh` → `install.sh` ile güncelleme → tüm verinin silinmesi → yeniden kurulum → `geri-yukle.sh`) `tools/ci/deploy-smoke.sh`
  ile Docker üzerinde uçtan uca denenir; aynı test CI'da (`deploy` iş akışı) kurulum dosyaları değişince ve haftada bir koşar.
  **Denenmeyenler:** gerçek alan adıyla Let's Encrypt sertifikası (testte Caddy `localhost` için kendi iç sertifikasını üretir),
  gerçek bir Ubuntu sunucuda Docker'ın `get.docker.com` ile kurulması, cron'un gece gerçekten tetiklemesi ve `YEDEK_UZAK`
  kopyası. İlk kurulumda bir adım hata verirse `docker compose ... logs <servis>` çıktısını paylaşın.
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

## Admin şifresini sıfırlama

Giriş ekranından admin şifresi talep edilemez; unutulursa sunucu sahibi sıfırlar (`deploy/prod` klasöründe):

1. `.env` içinde `INITIAL_ADMIN_PASSWORD` değerini yeni geçici şifreyle (en az 8 karakter) değiştirin ve `RESET_ADMIN_PASSWORD=true` satırını ekleyin.
2. `docker compose up -d app` ile uygulamayı yeniden başlatın. `admin` şifresi bu değer olur, hesap kilidi kalkar.
3. **Hemen** `RESET_ADMIN_PASSWORD` satırını silin (ya da `false` yapın) ve uygulamayı bir kez daha başlatın; aksi halde her açılışta şifre sıfırlanır.
4. `admin` ile girin; uygulama yeni şifre seçtirir.
