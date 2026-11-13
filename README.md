# yarn-shipment-system

İplik sevkiyat, fasoncu bakiyesi ve kalite kontrol sistemi.

**Kasım 2026 süreç değişikliği.** Sahada şoför uygulaması, teslim tartısı, mühür ve kör sayım kullanılmadığı için bu
adımlar sistemden kaldırıldı. Akış artık şöyledir: müdür sevkiyatı açar → depo çuvalları atar ve kantarda çıkış
tartısını girer (sevkiyat `AWAITING_DELIVERY`, "Fasoncuya yolda") → sevkiyat fasoncuya ulaşınca müdür irsaliyeyi
yükleyip numarasını yazar ve sevkiyatı tamamlar (`COMPLETED`). Ayrıntılı süreç analizi ve sonraki adımlar
(fire, fasoncu bakiyesi, kumaş/iplik dönüşü, fatura işareti) için proje dokümanına bakın.

## Dokümanlar

- [Java 21 kod standartları](docs/standartlar/java-kod-standartlari.md): projedeki tüm Java kodu için bağlayıcı kurallar.
- [React ve TypeScript kod standartları](docs/standartlar/react-typescript-kod-standartlari.md): frontend kodu için bağlayıcı kurallar.
- [Modüler monolit mimarisi](docs/architecture/moduler-monolit-mimarisi.md): modüller, olaylar, veri, audit ve güvenlik (güncel mimari).
- [Veritabanı tasarımı](docs/veritabani-tasarimi.md): PostgreSQL 17 şemaları, tablolar, `platform` şeması ve audit rolleri.
- [Süreç analizi v2](docs/analiz/surec-analizi-v2.md): Kasım 2026 saha incelemesinden sonraki iş akışı, roller, kurallar ve yol haritası.

## Derleme

Java 21 ve Docker gerekir; Maven, `./mvnw` ile kendiliğinden iner.

```bash
./mvnw spotless:apply   # biçimi düzeltir (BUILD-05)
./mvnw verify           # Error Prone + NullAway, Checkstyle, ArchUnit, PMD ve JaCoCo eşikleri
```

Kod standartlarındaki kuralların araç ayarları `build-tools/` altındadır (Ek C). Kural ihlali derlemeyi durdurur;
uyarı seviyesi yoktur. Ortak ArchUnit kural seti `libs/common-test` içindedir ve her servis kendi
`ArchitectureTest` sınıfında şöyle bağlar:

```java
@AnalyzeClasses(packages = "com.yarnshipment.shipment", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
    @ArchTest static final ArchTests HEXAGONAL = ArchTests.in(HexagonalArchitectureRules.class);
    @ArchTest static final ArchTests NAMING = ArchTests.in(NamingRules.class);
    @ArchTest static final ArchTests STRUCTURE = ArchTests.in(StructureRules.class);
}
```

## Yerel veritabanı

```bash
cp deploy/.env.example deploy/.env      # şifreleri değiştirin; dosya git'e girmez
docker compose -f deploy/docker-compose.yml up -d
```

`deploy/.env` içinde `LOCAL_TEST_USER_PASSWORD` yerel kullanıcıların ortak şifresidir (en az 8 karakter; `12345678` gibi yaygın şifreler reddedilir).
Giriş uygulamanın kendi hesap tablosuyla (`app_user`) yapılır; ayrı bir kimlik sunucusu, gateway ya da Redis yoktur.

`local` profilinde `yarnshipment-app` açılışta şu kullanıcıları (yoksa) açar; hepsinin şifresi `LOCAL_TEST_USER_PASSWORD`'dür
(`LOCAL_SAMPLE_USERS=false` verilirse yalnızca `admin` açılır):

| Kullanıcı | Rol       | Kapsam       |
| --------- | --------- | ------------ |
| `admin`   | admin     | iki lokasyon |
| `depo`    | warehouse | A lokasyonu  |
| `kalite`  | quality   | iki lokasyon |
| `mudur`   | manager   | iki lokasyon |

İkinci faktör (OTP) şimdilik yoktur; ileride eklenebilir. Yetkiler yalnızca role bağlıdır.

Compose, tek bir PostgreSQL 17 konteynerinde tek veritabanı (`yarnshipment`) ve tek kullanıcı (`yarnshipment`, şifre
`LOCAL_DB_PASSWORD`) açar. Her modül tablolarını kendi şemasında tutar (`shipment`, `discrepancy`, `evidence`, `reporting`, `audit`); ortak iki tablo (`idempotency_record`, `audit_local`) `platform` şemasındadır. Compose ayrıca giriş yapamayan
`audit_writer` ve `audit_reader` rollerini açar. Kullanıcının `search_path`'i hepsini kapsar:
`psql -h localhost -U yarnshipment yarnshipment` ile tek bağlantıdan her tabloya erişilir (`SELECT * FROM shipment.shipment`).
Tabloları uygulama açılışta Flyway ile kurar (önce `platform`, sonra modül şemaları); migration'lar
`services/<servis>/src/main/resources/db/migration/<şema>` ve `libs/common-web/src/main/resources/db/platform` altındadır.

Migration'ları ve veritabanı kısıt testlerini servisler olmadan doğrulamak için:

```bash
tools/db/verify-migrations.sh
```

## Yerel ortamı sıfırlama (tek komut)

Yerel veriyi silip yalnızca `admin` kullanıcısıyla temiz bir ortam kurmak için (geri alınamaz; **yalnızca yerel geliştirme**):

```bash
cd frontend
pnpm yerel:sifirla              # "SIL" yazarak onaylarsınız; sıfırlar, backend'i derler, yığını yeniden başlatır
pnpm yerel:sifirla -- --evet    # onay sormadan
pnpm yerel:sifirla -- --sadece-veri      # yalnızca Docker verisini sıfırlar (uygulama ve arayüze dokunmaz)
pnpm yerel:sifirla -- --derleme-atla     # backend'i yeniden derlemeden (kod değişmediyse daha hızlı)
pnpm yerel:baslat               # veriye dokunmadan servisleri yeniden başlatır (onay sormaz)
pnpm yerel:durdur               # servisleri durdurur (Docker'a dokunmaz)
```

Betik (`tools/yerel-sifirla.mjs`): 8083 ve 5173 portlarını dinleyen yerel servisleri durdurur; `deploy/docker-compose.yml` ile Docker
hacimlerini siler ve yeniden kurar; backend'i derleyip uygulamayı (`LOCAL_SAMPLE_USERS=false`: yalnızca `admin`, şifresi
`LOCAL_TEST_USER_PASSWORD`) ve arayüzü **gizli, pencere açmadan** ve terminalden bağımsız başlatır (terminal kapansa da çalışır; günlükler ve süreç numaraları `.yerel/` altında, durdurmak için `pnpm yerel:durdur`). Betik robotu **kendiliğinden başlatmaz**. Korumalar: `SPRING_PROFILES_ACTIVE`/`YARNSHIPMENT_ENV` canlı ya da
CI gibiyse, `DOCKER_HOST` uzak sunucuysa ya da Docker bağlamı yerel değilse durur; `deploy/.env` yoksa çalışmaz.
Sıfırlamadan sonra diğer kullanıcıları, siz isterseniz robot açar: `pnpm robot -- --rol=Kurulum` (`frontend/apps/web/robot/README.md`).

## Uygulamayı çalıştırma (yarnshipment-app)

shipment, discrepancy, evidence, reporting ve audit modülleri `services/yarnshipment-app` içinde tek Spring Boot uygulaması olarak çalışır (port `8083`); önünde ayrı bir geçit yoktur, giriş de bu uygulamadadır. Tek `application.yml` ve tek bağlantı havuzu vardır; her şema kendi Flyway'iyle (kendi `flyway_schema_history` tablosu) açılışta kurulur, migration'lar `services/<modül>-service/src/main/resources/db/migration/<şema>/` altındadır. PostgreSQL'i ayrıca kurmanız gerekmez; yukarıdaki Compose yeterlidir.

```bash
export DB_PASSWORD=<LOCAL_DB_PASSWORD>
export LOCAL_TEST_USER_PASSWORD=<deploy/.env değeri>
export EVIDENCE_STORAGE_ACCESS_KEY=<deploy/.env değeri> EVIDENCE_STORAGE_SECRET_KEY=<deploy/.env değeri>
./mvnw -pl services/yarnshipment-app -am install -DskipTests
./mvnw -pl services/yarnshipment-app spring-boot:run -Dspring-boot.run.profiles=local
```

`local` profilinde ayrıca yerel örnek veri yüklenir (`db/seed-local/R__local_sample_shipments.sql`): iki lokasyonda, taslak, fasoncuya yolda, tamamlandı ve iptal durumlarında örnek sevkiyatlar; kimlikler yerel örnek kullanıcılarla eşleşir (fasoncu kendi sevkiyatlarını görür). Dosya tekrarlanabilir migration'dır, kayıtlar iki kez eklenmez. Üretim profilinde bu konum yoktur.

Sevkiyat iptali (`POST /api/v1/shipments/{id}/cancel`, `Idempotency-Key` ve `reason` ister) yalnızca yöneticidedir (`SHIPMENT_CANCEL`) ve yalnızca taslak ya da fasoncuya yoldaki (`AWAITING_DELIVERY`) sevkiyatta çalışır. Kantar beklenen yükü tutmazsa müdür sevkiyatı iptal edip yenisini açar ya da tartıdan önce çuval ekleyip çıkarır. İptal, sevkiyattaki çuvalları bag-service'te serbest bırakır; çuval servisi yanıt vermezse iptal de yapılmaz (502 `BAG_SERVICE_UNAVAILABLE`) ve istek güvenle tekrarlanır.

Sevkiyat fasoncuya ulaşınca müdür irsaliyeyi `WAYBILL` türünde kanıt olarak yükler, sonra `POST /api/v1/shipments/{id}/completion` (`Idempotency-Key`, gövde `{waybillNo, waybillEvidenceId}`) ile sevkiyatı tamamlar (`SHIPMENT_COMPLETE`; yalnızca `AWAITING_DELIVERY` durumunda). Kanıt evidence-service'ten doğrulanır: yoksa, başka sevkiyata aitse ya da türü `WAYBILL` değilse `422 WAYBILL_EVIDENCE_INVALID`, servise ulaşılamazsa `502 EVIDENCE_SERVICE_UNAVAILABLE`. Tamamlanan sevkiyat kilitlenir, `shipment-completed` olayı yayınlanır; sonraki değişiklik yalnızca gerekçeli düzeltmeyle yapılır.

Taslak sevkiyata yanlış okutulan çuval `DELETE /api/v1/shipments/{id}/bags/{uniqueKey}` ile çıkarılır (`DISPATCH_RECORD`; yalnızca çıkış tartımından önce). Satır silinmez, `removed_at` ile işaretlenir; bag-service'te rezervasyon bırakılır (`POST /api/v1/bags/reservations/release` içinde `uniqueKeys`) ve çuval başka sevkiyata atanabilir olur. Çuval listede yoksa da 204 döner. Sevkiyatın tamamını bırakmak (`uniqueKeys` olmadan) yine yalnızca `SHIPMENT_CANCEL` ile yapılır.

Ana veri listeleri (`GET /api/v1/master-data/{sites,subcontractors,drivers,vehicles}`, `q` ile arama, sayfalı) `md_*` tablolarından okunur; yerel örnek veri iki lokasyon, üç fasoncu, üç şoför ve üç araç içerir (şoför ve araç yalnızca kayıt bilgisidir; şoför sisteme girmez). Lokasyon listesi isteyenin kapsamıyla süzülür.

Fasoncu ve şoför yönetimi yalnızca yöneticidedir (`MASTER_DATA_MANAGE`): `POST /api/v1/master-data/{subcontractors,drivers}` (`Idempotency-Key` ister), `PUT .../{id}`, `POST .../{id}/deactivate` ve `.../activate`. Kayıt silinmez, pasife alınır; pasif kayıt listelerde görünmez, eski sevkiyatlar etkilenmez. Pasife alınmış kayıtları yönetici `?status=ALL` ile listeler (diğer roller 403 alır); yanıttaki `active` alanı durumu gösterir. Her değişiklik `audit_local`'e değiştiren kullanıcıyla düşer. Şoförün fasoncusu verilirse etkin olmalıdır. Araç ve lokasyon için aynı uçlar vardır (`/api/v1/master-data/{vehicles,sites}`); plaka boşluksuz büyük harfe çevrilir ve Türkiye plaka biçiminde olmalıdır (01-81 il kodu, 1-3 harf, 2-4 rakam; değilse 400 `INVALID_PLATE`), kullanılmış plaka 409 `DUPLICATE_PLATE` verir. Fasoncu ünvanı tektir (büyük/küçük harf ve baş/son boşluk yok sayılır); kullanılmış ünvan 409 `DUPLICATE_SUBCONTRACTOR` verir. Giriş hesapları Kullanıcılar ekranından açılır: [adımlar](docs/yonetim/giris-hesabi-acma.md).

| Değişken                                          | Varsayılan                                                                            |
| ------------------------------------------------- | ------------------------------------------------------------------------------------- |
| `DB_URL` (tüm servisler)                          | `jdbc:postgresql://localhost:5432/yarnshipment`                                       |
| `DB_USER` / `DB_PASSWORD` (tüm servisler)         | `yarnshipment` / (zorunlu)                                                            |
| `SERVER_PORT`                                     | `8083`                                                                                |
| `SESSION_COOKIE_SECURE`                           | `false` (HTTPS arkasında `true`; üretim compose'u verir)                              |
| `YARNSHIPMENT_AUTH_BOOTSTRAP_ADMIN_PASSWORD`      | boş; doluysa `admin` hesabı yoksa bu şifreyle açılır (ilk girişte değiştirilir)       |
| `LOCAL_TEST_USER_PASSWORD` / `LOCAL_SAMPLE_USERS` | yalnızca `local` profili: örnek kullanıcıların şifresi / açılıp açılmayacağı (`true`) |

Persistence testleri Testcontainers ile gerçek PostgreSQL'e karşı çalışır; `./mvnw verify` için Docker açık olmalıdır.

## Giriş ve oturum

Kullanıcılar `shipment.app_user` tablosundadır; şifreler BCrypt ile özetlenir. Giriş `POST /api/v1/auth/login` ile yapılır
(`{username, password}`); başarılıysa `YARNSHIPMENT_SESSION` çerezi (HttpOnly, SameSite=Lax; HTTPS'te Secure) verilir, token
tarayıcıya inmez. Oturum sunucu belleğindedir ve 30 dakika boşta kalınca düşer; uygulama yeniden başlarsa herkes yeniden giriş yapar.

| Uç                          | Davranış                                                                                                                                                                                                                                      |
| --------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `POST /api/v1/auth/login`   | Kullanıcı adı/şifre doğruysa oturum açar: `200 {passwordChangeRequired}`. Yanlışsa ya da hesap kapalıysa `401 AUTHENTICATION_FAILED` (hangisinin yanlış olduğu söylenmez). 5 hatalı denemede hesap 15 dakika kilitlenir: `423 ACCOUNT_LOCKED` |
| `POST /api/v1/auth/logout`  | Oturumu kapatır (`204`)                                                                                                                                                                                                                       |
| `PUT /api/v1/auth/password` | Kişi kendi şifresini değiştirir (`currentPassword`, `newPassword`); yanlış mevcut şifre `400 INVALID_CURRENT_PASSWORD`, kurala uymayan yeni şifre `400 WEAK_PASSWORD`. Diğer oturumları kapanır                                               |
| `GET /api/**` oturumsuz     | `401` Problem Details, `code: AUTHENTICATION_REQUIRED` (yönlendirme yok)                                                                                                                                                                      |
| `XSRF-TOKEN` çerezi         | JS tarafından okunur; durum değiştiren her istekte `X-XSRF-TOKEN` başlığı olarak geri gönderilir (giriş ucu hariç)                                                                                                                            |

Şifre kuralı: en az 8, en çok 128 karakter; tek karakterden oluşamaz, kullanıcı adı olamaz, yaygın şifre listesinde olamaz.
Yöneticinin verdiği geçici şifreyle giren kişi şifresini değiştirmeden hiçbir izin alamaz. Rol değişince, hesap kapanınca ya da
yönetici yeni şifre verince kişinin açık oturumları kapanır. Tarayıcı yalnızca uygulamayla (`8083`; geliştirmede `5173`
arayüz sunucusu `/api` isteklerini ona iletir) konuşur.

## common-events (süreç içi olay yolu)

Modüller arası olaylar tek süreçte taşınır; Kafka ve `outbox_event` yoktur. Bir modül `DomainEventPublisher.publish` ile olay yayınlar; `InProcessDomainEventPublisher` olayı kayıtlı `DomainEventConsumer`'lara iletir. `BEFORE_COMMIT` tüketiciler (audit, uyuşmazlık, sevkiyat defteri) üreten işlemle aynı transaction'da çalışır ve hata verirse iş işlemi geri alınır; `AFTER_COMMIT` tüketiciler commit'ten sonra kendi transaction'ında çalışır.

Konu (`shipment.events.v1` vb.) ve kaynak (`/yarn-shipment/shipment-service` vb.) olay türünün önekinden gelir: `yarnshipment.events.routes` listesi (`type-prefix`, `topic`, `source`) `yarnshipment-app/src/main/resources/application.yml` içindedir; eşleşmeyen olay türü yayın anında hata verir. Audit varlık türünü konudan türetir. Testlerde `RecordingDomainEventPublisher` yayınlanan olayları tutar.

## discrepancy-service (uyuşmazlık ve kalite itirazı)

Teslim tartısı kaldırıldığı için çıkış–teslim otomatik karşılaştırması artık çalışmaz; `comparison_input` ve eski uyuşmazlık kayıtları geçmiş için salt okunur tutulur. Servisin bugünkü işi uyuşmazlık kayıtlarının yönetimi (atama, not, karar) ve kalite itirazıdır. Kantar uyuşmazlığı ve fasoncu bakiyesi kuralları yeni süreç analizinde tanımlıdır ve sonraki işlerde eklenecektir.

### Kalite itirazı ve numune/lab

Fabrika personeli (kalite, müdür, admin; fasoncu sisteme giriş yapmaz, KR-17) tamamlanmış sevkiyat için **kilogram bazlı** kalite itirazı açar (`POST /api/v1/quality-claims`, `QUALITY_CLAIM_OPEN`). Hedef sevkiyat `shipment_watch` izinden okunur (`comparison_input` artık okunmaz). Pencere tamamlanmadan itibaren 3 iş günüdür (Europe/Istanbul); itirazda `claimedKg` (sıfırdan büyük, en çok 3 ondalık, sevkiyatın çıkış net kilosunu aşamaz), açıklama ve en az bir `QUALITY_CLAIM` fotoğrafı zorunludur; `fabricReceiptId` isteğe bağlıdır (kumaş girişi henüz yok). Fotoğrafların bu sevkiyata ait olduğu evidence-service'e isteyenin token'ıyla sorulur. Her itiraz `QUALITY` türünde Kırmızı bir uyuşmazlık (`QUALITY_CLAIM` kuralı, son tarih 5 iş günü) açar. Kalite (`DISCREPANCY_RESOLVE`) itiraz üzerinden numune (`SAMPLE`) ya da laboratuvar (`LAB`) kontrolü ister ve sonucu bir kez girer; lab sonucunda `LAB_RESULT` belgesi zorunludur. Karar mevcut uyuşmazlık akışıyla (`CLAIM_JUSTIFIED` / `CLAIM_UNJUSTIFIED`) verilir; `CLAIM_JUSTIFIED` kararı kiloyu `DiscrepancyResolved` olayıyla taşır; shipment-service dinleyicisi itiraz kilosunu sevkiyatın iplik cinslerine gönderim oranıyla dağıtıp fasoncunun bakiyesinden `CLAIM_ADJUSTMENT` olarak düşer (KR-15, aynı uyuşmazlık bir kez) ve fire tabanından düşülür.

| Kod                                                        | HTTP      | Anlamı                                               |
| ---------------------------------------------------------- | --------- | ---------------------------------------------------- |
| `QUALITY_CLAIM_NOT_AVAILABLE`                              | 409       | Sevkiyat tamamlanmamış ya da çıkış kilosu bilinmiyor |
| `QUALITY_CLAIM_WINDOW_CLOSED`                              | 409       | 3 iş günlük süre doldu                               |
| `QUALITY_CLAIM_BAG_NOT_DELIVERED` / `_BAG_ALREADY_CLAIMED` | 400 / 409 | Çuval sevkiyatta yok / zaten itiraz edilmiş          |
| `QUALITY_CLAIM_EVIDENCE_REQUIRED` / `_EVIDENCE_INVALID`    | 400       | Fotoğraf yok / başka sevkiyata ait ya da türü yanlış |
| `QUALITY_CLAIM_INSPECTION_RECORDED`                        | 409       | Kontrol sonucu zaten girilmiş                        |
| `QUALITY_CLAIM_RESULT_EVIDENCE_REQUIRED`                   | 400       | Lab sonucunda belge yok                              |
| `EVIDENCE_SERVICE_UNAVAILABLE`                             | 503       | Kanıtlar doğrulanamadı                               |

Çalıştırmak için `DB_PASSWORD`, verilir:

```bash
DB_PASSWORD=... \
./mvnw -pl services/discrepancy-service spring-boot:run -Dspring-boot.run.profiles=local
```

## reporting-service (dashboard ve raporlar)

Rapor servisidir; kendi veri kopyası tutmaz. `shipment_view`, `discrepancy_view`, `balance_movement_view` ve `fire_file_view` `reporting` şemasında, `shipment` ve `discrepancy` şemalarındaki kayıt tablolarını okuyan canlı SQL görünümleridir (olay dinleyicisi, `processed_event` ve yeniden oynatma yoktur). Bu yüzden servisin migration'ı, shipment-service ve discrepancy-service migration'larından sonra çalışmalıdır. Açılış bakiyesi aktarımı tek yazma yoludur ve doğrudan `shipment.balance_movement` defterine `OPENING` satırı ekler. Sözleşme `services/reporting-service/src/main/resources/openapi/reporting-service-api.yaml`. Uçlar `REPORT_VIEW` izni ister (Admin, Kalite, Yönetici); fasoncu rapor görmez, diğerleri yalnızca `site_ids` lokasyonlarını görür.

| Uç                                          | İçerik                                                                                                                                                    |
| ------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `GET /api/v1/reports/dashboard`             | Sevkiyat durum dağılımı, karşılaştırılan kilo toplamları, Yeşil/Sarı/Kırmızı sayıları, açık ve süresi geçen uyuşmazlıklar, kilo farkı en büyük 5 sevkiyat |
| `GET /api/v1/reports/subcontractors`        | Fasoncu bazında sevkiyat sayısı, uyuşmazlıklı sevkiyat oranı, ortalama mutlak kilo farkı                                                                  |
| `GET /api/v1/reports/shipments`, `/balance` | Sevkiyat dökümü ve fasoncu bazında gönderilen / yolda / tamamlanan kilo                                                                                   |

`from`/`to` (tarih, iki uç dahil) verilmezse son 30 gün alınır; en çok 366 gün (`REPORT_PERIOD_INVALID`). `siteId` kapsamı daraltır, yetkisiz lokasyon 403 döner. Henüz kapsam dışı: parti bazlı kalite itirazı raporu (olaylarda parti bilgisi yok), eksik belge/galeri oranı (8.5), fire ve kumaş/iplik dönüşlü fasoncu bakiyesi (süreç analizi v2'de tanımlı). Çalıştırmak için `REPORTING_DB_APP_PASSWORD`, `REPORTING_DB_MIGRATOR_PASSWORD` ve gerekirse `KAFKA_BOOTSTRAP_SERVERS` verilir:

```bash
DB_PASSWORD=... \
./mvnw -pl services/reporting-service spring-boot:run -Dspring-boot.run.profiles=local
```

Panelde ana sayfa (`/panel`) dönem seçimli dashboard'u, `/panel/raporlar` özet, fasoncu, sevkiyat dökümü ve fasoncu bakiye sekmelerini gösterir.

## evidence-service (fotoğraf ve belge yükleme)

Dosya servisten geçmez; iki adımda doğrudan nesne deposuna (S3 uyumlu, yerelde MinIO) yüklenir. Uçlar `/api/v1/evidence/**` altındadır, sözleşme `services/evidence-service/src/main/resources/openapi/evidence-service-api.yaml`.

1. `POST /api/v1/evidence/uploads` (`Idempotency-Key` zorunlu): sevkiyat, kanıt türü, dosya türü, boyut ve kaynak (kamera/galeri) bildirilir. Servis izni, 20 MB sınırını ve izinli türleri (`image/jpeg`, `image/png`, `image/heic`, `application/pdf`) denetler, sevkiyatı shipment-service'e isteyenin token'ıyla sorar (görünmeyen sevkiyat `404 SHIPMENT_NOT_FOUND`; tamamlanmış/iptal `409 SHIPMENT_LOCKED`; kalite itirazı fotoğrafı ve lab belgesi tamamlanmış sevkiyata da yüklenebilir) ve yaklaşık 10 dakika geçerli imzalı bir `PUT` adresi döner.
2. İstemci dosyayı o adrese, cevaptaki başlıklarla aynen `PUT` eder.
3. `POST /api/v1/evidence/uploads/{evidenceId}/confirm`: sunucu dosyayı depodan okuyup gerçek boyut, tür ve SHA-256'yı kendisi hesaplar (SEC-09); bildirilenle uyuşmazsa `422 UPLOAD_MISMATCH`, dosya henüz yoksa `409 UPLOAD_NOT_COMPLETED`. Onay tekrarlanabilir.

Yükleme izni kanıt türüne bağlıdır: sevk fotoğrafları ve kantar fişi (`EVIDENCE_UPLOAD_DISPATCH`), irsaliye (`WAYBILL`, `SHIPMENT_COMPLETE`), şikâyet ve lab belgesi (`EVIDENCE_UPLOAD_CLAIM`). Aynı dosya (aynı hash) başka sevkiyatta daha önce yüklenmişse kayıt yine alınır ama `evidence_duplicate` satırı ve `evidence-reuse-detected.v1` olayı yazılır. Kanıt kaydı ile `evidence-uploaded.v1` olayı (`evidence.events.v1`) aynı transaction'da yazılır.

**Kantar fişi.** Çıkış kantar fişi (`DISPATCH_SCALE`) yalnızca fotoğraf olarak yüklenir ve sevkiyat için zorunludur; sistem fişi okumaz, kilolar elle girilir.

Fiş okumasının elle girilen çıkış tartısıyla (±1 kg), plakayla ve başka sevkiyatların fişleriyle karşılaştırılması eskiden teslim karşılaştırmasına bağlıydı; yeni akışta kantar uyuşmazlığı kuralının parçası olarak yeniden kurulacaktır (süreç analizi v2).

Yerelde MinIO `docker compose` ile açılır (`deploy/.env` içinde `EVIDENCE_STORAGE_ACCESS_KEY` ve `EVIDENCE_STORAGE_SECRET_KEY`; `evidence` kovası kendiliğinden oluşur). Servis aynı anahtarlarla, `EVIDENCE_DB_APP_PASSWORD` ve `EVIDENCE_DB_MIGRATOR_PASSWORD` ile çalıştırılır:

```bash
DB_PASSWORD=... \
EVIDENCE_STORAGE_ACCESS_KEY=... EVIDENCE_STORAGE_SECRET_KEY=... \
./mvnw -pl services/evidence-service spring-boot:run -Dspring-boot.run.profiles=local
```

İmzalı adres, `EVIDENCE_STORAGE_ENDPOINT` adresiyle üretilir; telefon ya da tarayıcının eriştiği adres olmalıdır.

`GET /api/v1/evidence/{evidenceId}` kanıt üst verisini (dosyayı değil), `GET /api/v1/evidence?shipmentId=` sevkiyatın kanıtlarını, `GET /api/v1/evidence/{evidenceId}/download-url` dosya için yaklaşık 5 dakika geçerli imzalı indirme adresini verir. Üçü de sevkiyat kapsamına uyar: kullanıcı yalnızca görebildiği sevkiyatların kanıtlarını görür; görünmeyen kanıt listede yer almaz ve tek tek istenirse `404 EVIDENCE_NOT_FOUND` döner.

shipment-service sevkiyatı tamamlarken `waybillEvidenceId` kanıtını bu servisten doğrular: yoksa, başka sevkiyata aitse ya da türü `WAYBILL` değilse `422 WAYBILL_EVIDENCE_INVALID`, servise ulaşılamazsa `502 EVIDENCE_SERVICE_UNAVAILABLE` (modüller aynı süreçte olduğundan artık ağ çağrısı yoktur). Yalnız `EVIDENCE_INVALIDATE` yetkisi olan (admin) `POST /api/v1/evidence/{id}/invalidate` ile gerekçe vererek kanıtı geçersiz kılabilir: kayıt silinmez, `status=INVALIDATED` olur, `EvidenceInvalidated` olayı yayımlanır; geçersiz kanıt tamamlamada kabul edilmez. Tekrar geçersiz kılma `409 EVIDENCE_ALREADY_INVALIDATED`, gerekçesiz istek `400 INVALIDATION_REASON_REQUIRED` döner.

## common-web (ortak web altyapısı)

Servis ana sınıfına `@EnableYarnShipmentWeb` eklenir; şunları verir:

- **Hata yanıtı (ERR-06/07):** `ProblemFactory` ve ortak advice (`MISSING_HEADER`, `VALIDATION_FAILED`, `MALFORMED_REQUEST`). Servise özgü hatalar kendi advice'ında aynı fabrikayı kullanır.
- **İstek kimliği:** her isteğe `X-Request-Id` (istemcinin güvenli değeri korunur, yoksa üretilir); log bağlamında `requestId`, hata gövdesinde `requestId` alanı.
- **Idempotency-Key (API-04):** servis `JdbcClient` kullanıyorsa kendiliğinden devreye girer. `POST/PUT/PATCH` isteğinde `Idempotency-Key` varsa: aynı anahtar ve aynı gövde ilk başarılı (2xx) yanıtı tekrar döner (`Idempotency-Replayed: true`), aynı anahtar farklı gövde `422 IDEMPOTENCY_KEY_REUSED`, hâlâ işlenen istek `409 IDEMPOTENCY_IN_PROGRESS`. Başarısız istek anahtarı bırakır. Anahtar kullanıcıya özeldir; kayıtlar 24 saat tutulur (`yarnshipment.idempotency.*`). Kayıtlar tüm modüller için ortak `platform.idempotency_record` tablosundadır (`libs/common-web/src/main/resources/db/platform`); modül dilim testlerinde `PlatformSchemaMigration.migrate(flyway)` modül göçünden önce çalıştırılır (bkz. `TestApplication`).

## audit-service (denetim izi)

`shipment.events.v1`, `evidence.events.v1` ve `discrepancy.events.v1` konularına yayınlanan her olay, `audit` şemasındaki değiştirilemez hash zincirine `audit_append()` ile yazılır. Aktör (`actorId`, `actorRoles`) ve korelasyon (`correlationId`) CloudEvents başlıklarından gelir; aktörsüz sistem olayı, olayı üreten servisin adıyla `client_id` taşır. Aynı olayın tekrar teslimi ikinci satır üretmez (tekillik veritabanı fonksiyonundadır), bu yüzden işlenmiş olay tablosu yoktur.

Tek veritabanı kullanıcısına geçildiği için okuma/yazma rol ayrımı kalktı: audit tablolarını sahibi olan kullanıcı doğrudan değiştirebilir. Değişmezlik artık yalnızca `reject_modification` tetikleyicisi ve hash zinciriyle (bozulmayı günlük doğrulama yakalar) sağlanır; ayrıntı için `docs/veritabani-tasarimi.md`. Her gün 03:00 UTC'de (`AUDIT_CHAIN_VERIFICATION_CRON`) son doğrulamadan sonraki kayıtlar `audit_verify_chain()` ile doğrulanır; bozulma `ERROR` olarak loglanır.

`AUDIT_VIEW` izni olanlar (Admin, Kalite, Depo, Yönetici) `GET /api/v1/audit-log` (süzgeçler: `entityType`, `entityId`, `actorUserId`, `action`, `sourceService`, `from`, `to`; sayfalı, en yeni önce) ve `GET /api/v1/audit-log/chain-checks` uçlarını okur. IP, cihaz ve user-agent yanıtta yer almaz. Çalıştırmak için `AUDIT_DB_WRITER_PASSWORD`, `AUDIT_DB_READER_PASSWORD`, `AUDIT_DB_MIGRATOR_PASSWORD` verilir; servis 8088 portunda çalışır:

```bash
./mvnw -pl services/audit-service spring-boot:run -Dspring-boot.run.profiles=local
```
