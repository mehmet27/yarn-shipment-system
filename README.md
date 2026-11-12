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
- [Veritabanı tasarımı](docs/veritabani-tasarimi.md): servis başına PostgreSQL 17 veritabanları ve tablolar.
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

Ayrıca Keycloak `http://localhost:8180` adresinde açılır (yönetici: `admin` / `KEYCLOAK_ADMIN_PASSWORD`).
`yarnshipment` realm'i (roller, claim mapper'ları, test kullanıcıları) ilk açılışta
`deploy/keycloak/yarnshipment-realm-local.json` dosyasından kendiliğinden içe aktarılır. Realm zaten varsa
yeniden aktarılmaz; sıfırlamak için `docker compose -f deploy/docker-compose.yml down -v` çalıştırın.

`deploy/.env` içinde üç değer zorunludur (örnekleri `deploy/.env.example` içinde): `KEYCLOAK_TEST_USER_PASSWORD` test kullanıcılarının ortak şifresidir, `GATEWAY_CLIENT_SECRET` gateway'in Keycloak'taki istemci sırrıdır, `REDIS_PASSWORD` gateway oturumlarını tutan Redis'in şifresidir (ikisi de gateway'i başlatırken aynı değerle verilir). Giriş sayfası Türkçe açılır.

Test kullanıcıları (hepsinin şifresi `KEYCLOAK_TEST_USER_PASSWORD`):

| Kullanıcı | Rol | Kapsam |
|---|---|---|
| `admin` | admin | iki lokasyon |
| `depo` | warehouse | A lokasyonu |
| `kalite` | quality | iki lokasyon |
| `mudur` | manager | iki lokasyon |

Token'ların servislerin beklediği claim'leri taşıdığını doğrulamak için (Node 22):

```bash
KEYCLOAK_TEST_USER_PASSWORD=<deploy/.env değeri> node tools/keycloak/verify-realm.mjs
```

Yerel Keycloak'ta ikinci faktör kurulu değildir; `admin` ve `kalite` rolleri MFA'sız oturumda düşürüldüğü için
shipment, bag, discrepancy ve evidence servislerini `--spring.profiles.active=local` ile başlatın (yalnızca yerelde
MFA şartını kapatır; üretim ayarı değişmez). Profilsiz açılan serviste bu iki rol 403 `ACCESS_DENIED` alır.

Compose, tek bir PostgreSQL 17 konteynerinde tek veritabanı (`yarnshipment`) ve tek kullanıcı (`yarnshipment`, şifre
`LOCAL_DB_PASSWORD`) açar. Her servis tablolarını kendi şemasında tutar (`masterdata`, `shipment`, `bag`, `evidence`,
`discrepancy`, `reporting`, `audit`; Keycloak için `keycloak`). Kullanıcının `search_path`'i hepsini
kapsar: `psql -h localhost -U yarnshipment yarnshipment` ile tek bağlantıdan her tabloya erişilir (`SELECT * FROM bag.bag`).
Tabloları servisler açılışta Flyway ile kurar; migration'lar `services/<servis>/src/main/resources/db/migration` altındadır.

Migration'ları ve veritabanı kısıt testlerini servisler olmadan doğrulamak için:

```bash
tools/db/verify-migrations.sh
```

## shipment-service'i çalıştırma

PostgreSQL'i ayrıca kurmanız gerekmez; yukarıdaki Compose yeterlidir. Servis açılışta Flyway ile tabloları
`yarnshipment` kullanıcısıyla `shipment` şemasına kurar.

```bash
export DB_PASSWORD=<LOCAL_DB_PASSWORD>
export YARNSHIPMENT_KEYCLOAK_ADMIN_CLIENT_SECRET=<deploy/.env içindeki KEYCLOAK_ADMIN_CLIENT_SECRET>
./mvnw -pl services/shipment-service spring-boot:run -Dspring-boot.run.profiles=local
```

`YARNSHIPMENT_KEYCLOAK_ADMIN_CLIENT_SECRET` verilmezse servis yine açılır, ama Tanımlar ekranındaki "Giriş hesabı aç"
502 `LOGIN_ACCOUNT_UNAVAILABLE` ("Giriş sistemine ulaşılamadı") döner.

`local` profilinde ayrıca yerel örnek veri yüklenir (`db/seed-local/R__local_sample_shipments.sql`): iki lokasyonda, taslak, fasoncuya yolda, tamamlandı ve iptal durumlarında örnek sevkiyatlar; kimlikler yerel Keycloak kullanıcılarıyla eşleşir (fasoncu kendi sevkiyatlarını görür). Dosya tekrarlanabilir migration'dır, kayıtlar iki kez eklenmez. Üretim profilinde bu konum yoktur.

Sevkiyat iptali (`POST /api/v1/shipments/{id}/cancel`, `Idempotency-Key` ve `reason` ister) yalnızca yöneticidedir (`SHIPMENT_CANCEL`) ve yalnızca taslak ya da fasoncuya yoldaki (`AWAITING_DELIVERY`) sevkiyatta çalışır. Kantar beklenen yükü tutmazsa müdür sevkiyatı iptal edip yenisini açar ya da tartıdan önce çuval ekleyip çıkarır. İptal, sevkiyattaki çuvalları bag-service'te serbest bırakır; çuval servisi yanıt vermezse iptal de yapılmaz (502 `BAG_SERVICE_UNAVAILABLE`) ve istek güvenle tekrarlanır.

Sevkiyat fasoncuya ulaşınca müdür irsaliyeyi `WAYBILL` türünde kanıt olarak yükler, sonra `POST /api/v1/shipments/{id}/completion` (`Idempotency-Key`, gövde `{waybillNo, waybillEvidenceId}`) ile sevkiyatı tamamlar (`SHIPMENT_COMPLETE`; yalnızca `AWAITING_DELIVERY` durumunda). Kanıt evidence-service'ten doğrulanır: yoksa, başka sevkiyata aitse ya da türü `WAYBILL` değilse `422 WAYBILL_EVIDENCE_INVALID`, servise ulaşılamazsa `502 EVIDENCE_SERVICE_UNAVAILABLE`. Tamamlanan sevkiyat kilitlenir, `shipment-completed` olayı yayınlanır; sonraki değişiklik yalnızca gerekçeli düzeltmeyle yapılır.

Taslak sevkiyata yanlış okutulan çuval `DELETE /api/v1/shipments/{id}/bags/{uniqueKey}` ile çıkarılır (`DISPATCH_RECORD`; yalnızca çıkış tartımından önce). Satır silinmez, `removed_at` ile işaretlenir; bag-service'te rezervasyon bırakılır (`POST /api/v1/bags/reservations/release` içinde `uniqueKeys`) ve çuval başka sevkiyata atanabilir olur. Çuval listede yoksa da 204 döner. Sevkiyatın tamamını bırakmak (`uniqueKeys` olmadan) yine yalnızca `SHIPMENT_CANCEL` ile yapılır.

Ana veri listeleri (`GET /api/v1/master-data/{sites,subcontractors,drivers,vehicles}`, `q` ile arama, sayfalı) `md_*` tablolarından okunur; yerel örnek veri iki lokasyon, üç fasoncu, üç şoför ve üç araç içerir (şoför ve araç yalnızca kayıt bilgisidir; şoför sisteme girmez). Lokasyon listesi isteyenin kapsamıyla süzülür. 

Fasoncu ve şoför yönetimi yalnızca yöneticidedir (`MASTER_DATA_MANAGE`): `POST /api/v1/master-data/{subcontractors,drivers}` (`Idempotency-Key` ister), `PUT .../{id}`, `POST .../{id}/deactivate` ve `.../activate`. Kayıt silinmez, pasife alınır; pasif kayıt listelerde görünmez, eski sevkiyatlar etkilenmez. Pasife alınmış kayıtları yönetici `?status=ALL` ile listeler (diğer roller 403 alır); yanıttaki `active` alanı durumu gösterir. Her değişiklik `audit_local`'e değiştiren kullanıcıyla düşer. Şoförün fasoncusu verilirse etkin olmalıdır. Araç ve lokasyon için aynı uçlar vardır (`/api/v1/master-data/{vehicles,sites}`); plaka boşluksuz büyük harfe çevrilir ve Türkiye plaka biçiminde olmalıdır (01-81 il kodu, 1-3 harf, 2-4 rakam; değilse 400 `INVALID_PLATE`), kullanılmış plaka 409 `DUPLICATE_PLATE` verir. Fasoncu ünvanı tektir (büyük/küçük harf ve baş/son boşluk yok sayılır); kullanılmış ünvan 409 `DUPLICATE_SUBCONTRACTOR` verir. Fasoncunun giriş hesabı (geçiş dönemi; yeni süreçte fasoncu sisteme girmeyecek) Keycloak'ta ayrıca açılır: [adımlar](docs/yonetim/giris-hesabi-acma.md). Otomatik hesap açma sonraki iştir.

| Değişken | Varsayılan |
|---|---|
| `DB_URL` (tüm servisler) | `jdbc:postgresql://localhost:5432/yarnshipment` |
| `DB_USER` / `DB_PASSWORD` (tüm servisler) | `yarnshipment` / (zorunlu) |
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8180/realms/yarnshipment` |
| `SERVER_PORT` | `8083` |
| `YARNSHIPMENT_KEYCLOAK_ADMIN_URL` / `YARNSHIPMENT_KEYCLOAK_ADMIN_REALM` / `YARNSHIPMENT_KEYCLOAK_ADMIN_CLIENT_ID` | `http://localhost:8180` / `yarnshipment` / `yarnshipment-admin` (fabrika içi giriş hesabı açan servis hesabı) |
| `YARNSHIPMENT_KEYCLOAK_ADMIN_CLIENT_SECRET` | boş (boşsa hesap açma uçları 502 `LOGIN_ACCOUNT_UNAVAILABLE` döner); yerel değer `deploy/.env` içindeki `KEYCLOAK_ADMIN_CLIENT_SECRET` (varsayılan `local-admin-client-secret`) |

Persistence testleri Testcontainers ile gerçek PostgreSQL'e karşı çalışır; `./mvnw verify` için Docker açık olmalıdır.

## Gateway (BFF)'i çalıştırma

Tarayıcı yalnızca gateway ile konuşur (`http://localhost:8080`). Giriş Keycloak'ta yapılır, token tarayıcıya inmez;
gateway oturum çerezini tutar ve `/api/v1/shipments/**` ile `/api/v1/me` isteklerini access token'ı `Authorization:
Bearer` olarak ekleyip shipment-service'e (`8083`) iletir. Keycloak açık olmalıdır; gateway açılışta Keycloak'tan
yapılandırmayı okur.

```bash
export GATEWAY_CLIENT_SECRET=<deploy/.env içindeki GATEWAY_CLIENT_SECRET>
export REDIS_PASSWORD=<deploy/.env içindeki REDIS_PASSWORD>
./mvnw -pl services/gateway spring-boot:run
```

| Uç | Davranış |
|---|---|
| `GET /oauth2/authorization/keycloak?returnTo=<adres>` | Keycloak girişini başlatır; giriş bitince tarayıcı `returnTo` adresine (yalnızca SPA ile aynı kökene ya da köke göreli yola izin verilir), yoksa `http://localhost:5173/` adresine döner |
| `GET /api/**` oturumsuz | `401` Problem Details, `code: AUTHENTICATION_REQUIRED` (yönlendirme yok) |
| `POST /logout` (`X-XSRF-TOKEN` ile) | Gateway oturumunu kapatır, `200 {"logoutUrl": "..."}` döner; tarayıcı bu adrese giderek Keycloak oturumunu da kapatır |
| `XSRF-TOKEN` çerezi | JS tarafından okunur; durum değiştiren her istekte `X-XSRF-TOKEN` başlığı olarak geri gönderilir |

| Değişken | Varsayılan |
|---|---|
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8180/realms/yarnshipment` |
| `GATEWAY_PUBLIC_URL` | `http://localhost:8080` (Keycloak'taki yönlendirme adresi; realm dosyasındakiyle aynı olmalı) |
| `GATEWAY_POST_LOGIN_URL` / `GATEWAY_POST_LOGOUT_URL` | `http://localhost:5173/` |
| `SHIPMENT_SERVICE_URL` | `http://localhost:8083` |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | `localhost` / `6379` / boş (oturumlar Redis'te tutulur; gateway yeniden başlasa da oturum düşmez) |

## common-outbox (olay yayını)

Servis iş verisiyle aynı transaction'da `outbox_event` tablosuna yazdığı olayları (EVT-03) bu kütüphanenin yoklamalı yayıncısı Kafka'ya iletir. Mesajlar CloudEvents ikili kipindedir (`ce_id`, `ce_type`, `ce_source`, `ce_subject`, `ce_time` başlıkları, gövde JSON), anahtar sevkiyat kimliğidir. Kafka onayından sonra satıra `published_at` yazılır; gönderim başarısızsa satır bir sonraki turda yeniden gönderilir (en az bir kez teslim). Birden çok servis örneği aynı satırı göndermez (`FOR UPDATE SKIP LOCKED`).

Servis `yarnshipment.outbox.topic` (ör. `shipment.events.v1`) ve `yarnshipment.outbox.source` verirse yayıncı açılır; `yarnshipment.outbox.enabled=false` kapatır. Tüketen servisler `ProcessedEvents.markProcessed(eventId, type)` ile olayı işlemle aynı transaction'da kaydeder; ikinci kez gelen olay `false` döner (EVT-05). Kafka Compose ile `localhost:9092` adresinde açılır; shipment-service `KAFKA_BOOTSTRAP_SERVERS` ile başka adrese yönlendirilir.

## discrepancy-service (uyuşmazlık ve kalite itirazı)

Teslim tartısı kaldırıldığı için çıkış–teslim otomatik karşılaştırması artık çalışmaz; `comparison_input` ve eski uyuşmazlık kayıtları geçmiş için salt okunur tutulur. Servisin bugünkü işi uyuşmazlık kayıtlarının yönetimi (atama, not, karar) ve kalite itirazıdır. Kantar uyuşmazlığı ve fasoncu bakiyesi kuralları yeni süreç analizinde tanımlıdır ve sonraki işlerde eklenecektir.

### Kalite itirazı ve numune/lab

Fabrika personeli (kalite, müdür, admin; fasoncu sisteme giriş yapmaz, KR-17) tamamlanmış sevkiyat için **kilogram bazlı** kalite itirazı açar (`POST /api/v1/quality-claims`, `QUALITY_CLAIM_OPEN`). Hedef sevkiyat `shipment_watch` izinden okunur (`comparison_input` artık okunmaz). Pencere tamamlanmadan itibaren 3 iş günüdür (Europe/Istanbul); itirazda `claimedKg` (sıfırdan büyük, en çok 3 ondalık, sevkiyatın çıkış net kilosunu aşamaz), açıklama ve en az bir `QUALITY_CLAIM` fotoğrafı zorunludur; `fabricReceiptId` isteğe bağlıdır (kumaş girişi henüz yok). Fotoğrafların bu sevkiyata ait olduğu evidence-service'e isteyenin token'ıyla sorulur. Her itiraz `QUALITY` türünde Kırmızı bir uyuşmazlık (`QUALITY_CLAIM` kuralı, son tarih 5 iş günü) açar. Kalite (`DISCREPANCY_RESOLVE`) itiraz üzerinden numune (`SAMPLE`) ya da laboratuvar (`LAB`) kontrolü ister ve sonucu bir kez girer; lab sonucunda `LAB_RESULT` belgesi zorunludur. Karar mevcut uyuşmazlık akışıyla (`CLAIM_JUSTIFIED` / `CLAIM_UNJUSTIFIED`) verilir; `CLAIM_JUSTIFIED` kararı kiloyu `DiscrepancyResolved` olayıyla taşır; shipment-service dinleyicisi itiraz kilosunu sevkiyatın iplik cinslerine gönderim oranıyla dağıtıp fasoncunun bakiyesinden `CLAIM_ADJUSTMENT` olarak düşer (KR-15, aynı uyuşmazlık bir kez) ve fire tabanından düşülür.

| Kod | HTTP | Anlamı |
|---|---|---|
| `QUALITY_CLAIM_NOT_AVAILABLE` | 409 | Sevkiyat tamamlanmamış ya da çıkış kilosu bilinmiyor |
| `QUALITY_CLAIM_WINDOW_CLOSED` | 409 | 3 iş günlük süre doldu |
| `QUALITY_CLAIM_BAG_NOT_DELIVERED` / `_BAG_ALREADY_CLAIMED` | 400 / 409 | Çuval sevkiyatta yok / zaten itiraz edilmiş |
| `QUALITY_CLAIM_EVIDENCE_REQUIRED` / `_EVIDENCE_INVALID` | 400 | Fotoğraf yok / başka sevkiyata ait ya da türü yanlış |
| `QUALITY_CLAIM_INSPECTION_RECORDED` | 409 | Kontrol sonucu zaten girilmiş |
| `QUALITY_CLAIM_RESULT_EVIDENCE_REQUIRED` | 400 | Lab sonucunda belge yok |
| `EVIDENCE_SERVICE_UNAVAILABLE` | 503 | Kanıtlar doğrulanamadı |

Gateway `/api/v1/discrepancies/**` ve `/api/v1/quality-claims/**` yollarını bu servise yönlendirir. Çalıştırmak için `DB_PASSWORD`, gerekirse `KAFKA_BOOTSTRAP_SERVERS` ve `EVIDENCE_SERVICE_URL` verilir:

```bash
DB_PASSWORD=... \
./mvnw -pl services/discrepancy-service spring-boot:run -Dspring-boot.run.profiles=local
```

## reporting-service (dashboard ve raporlar)

Rapor servisidir; kendi veri kopyası tutmaz. `shipment_view`, `discrepancy_view`, `balance_movement_view` ve `fire_file_view` `reporting` şemasında, `shipment` ve `discrepancy` şemalarındaki kayıt tablolarını okuyan canlı SQL görünümleridir (olay dinleyicisi, `processed_event` ve yeniden oynatma yoktur). Bu yüzden servisin migration'ı, shipment-service ve discrepancy-service migration'larından sonra çalışmalıdır. Açılış bakiyesi aktarımı tek yazma yoludur ve doğrudan `shipment.balance_movement` defterine `OPENING` satırı ekler. Sözleşme `services/reporting-service/src/main/resources/openapi/reporting-service-api.yaml`, gateway `/api/v1/reports/**` yolunu bu servise (`8087`) yönlendirir. Uçlar `REPORT_VIEW` izni ister (Admin, Kalite, Yönetici); fasoncu rapor görmez, diğerleri yalnızca `site_ids` lokasyonlarını görür.

| Uç | İçerik |
|---|---|
| `GET /api/v1/reports/dashboard` | Sevkiyat durum dağılımı, karşılaştırılan kilo toplamları, Yeşil/Sarı/Kırmızı sayıları, açık ve süresi geçen uyuşmazlıklar, kilo farkı en büyük 5 sevkiyat |
| `GET /api/v1/reports/subcontractors` | Fasoncu bazında sevkiyat sayısı, uyuşmazlıklı sevkiyat oranı, ortalama mutlak kilo farkı |
| `GET /api/v1/reports/shipments`, `/balance` | Sevkiyat dökümü ve fasoncu bazında gönderilen / yolda / tamamlanan kilo |

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

shipment-service sevkiyatı tamamlarken `waybillEvidenceId` kanıtını bu servisten doğrular: yoksa, başka sevkiyata aitse ya da türü `WAYBILL` değilse `422 WAYBILL_EVIDENCE_INVALID`, servise ulaşılamazsa `502 EVIDENCE_SERVICE_UNAVAILABLE` (adres: `yarnshipment.evidence-service.url`, varsayılan `http://localhost:8086`). Yalnız `EVIDENCE_INVALIDATE` yetkisi olan (admin) `POST /api/v1/evidence/{id}/invalidate` ile gerekçe vererek kanıtı geçersiz kılabilir: kayıt silinmez, `status=INVALIDATED` olur, `EvidenceInvalidated` olayı yayımlanır; geçersiz kanıt tamamlamada kabul edilmez. Tekrar geçersiz kılma `409 EVIDENCE_ALREADY_INVALIDATED`, gerekçesiz istek `400 INVALIDATION_REASON_REQUIRED` döner.

## common-web (ortak web altyapısı)

Servis ana sınıfına `@EnableYarnShipmentWeb` eklenir; şunları verir:

- **Hata yanıtı (ERR-06/07):** `ProblemFactory` ve ortak advice (`MISSING_HEADER`, `VALIDATION_FAILED`, `MALFORMED_REQUEST`). Servise özgü hatalar kendi advice'ında aynı fabrikayı kullanır.
- **İstek kimliği:** her isteğe `X-Request-Id` (istemcinin güvenli değeri korunur, yoksa üretilir); log bağlamında `requestId`, hata gövdesinde `requestId` alanı.
- **Idempotency-Key (API-04):** servis `JdbcClient` kullanıyorsa kendiliğinden devreye girer. `POST/PUT/PATCH` isteğinde `Idempotency-Key` varsa: aynı anahtar ve aynı gövde ilk başarılı (2xx) yanıtı tekrar döner (`Idempotency-Replayed: true`), aynı anahtar farklı gövde `422 IDEMPOTENCY_KEY_REUSED`, hâlâ işlenen istek `409 IDEMPOTENCY_IN_PROGRESS`. Başarısız istek anahtarı bırakır. Anahtar kullanıcıya özeldir; kayıtlar 24 saat tutulur (`yarnshipment.idempotency.*`). Servisin migration'ında `idempotency_record` tablosu gerekir (referans şema: `libs/common-web/src/main/resources/idempotency/idempotency_record.sql`).

## audit-service (denetim izi)

`shipment.events.v1`, `evidence.events.v1` ve `discrepancy.events.v1` konularındaki her olay, `audit` şemasındaki değiştirilemez hash zincirine `audit_append()` ile yazılır. Aktör (`actorId`, `actorRoles`) ve korelasyon (`correlationId`) CloudEvents başlıklarından gelir; aktörsüz sistem olayı, olayı üreten servisin adıyla `client_id` taşır. Aynı olayın tekrar teslimi ikinci satır üretmez (tekillik veritabanı fonksiyonundadır), bu yüzden işlenmiş olay tablosu yoktur. Zarfı bozuk olay kalıcı hatadır ve DLT'ye gider.

Tek veritabanı kullanıcısına geçildiği için okuma/yazma rol ayrımı kalktı: audit tablolarını sahibi olan kullanıcı doğrudan değiştirebilir. Değişmezlik artık yalnızca `reject_modification` tetikleyicisi ve hash zinciriyle (bozulmayı günlük doğrulama yakalar) sağlanır; ayrıntı için `docs/veritabani-tasarimi.md`. Her gün 03:00 UTC'de (`AUDIT_CHAIN_VERIFICATION_CRON`) son doğrulamadan sonraki kayıtlar `audit_verify_chain()` ile doğrulanır; bozulma `ERROR` olarak loglanır.

`AUDIT_VIEW` izni olanlar (Admin, Kalite, Depo, Yönetici) gateway üzerinden `GET /api/v1/audit-log` (süzgeçler: `entityType`, `entityId`, `actorUserId`, `action`, `sourceService`, `from`, `to`; sayfalı, en yeni önce) ve `GET /api/v1/audit-log/chain-checks` uçlarını okur. IP, cihaz ve user-agent yanıtta yer almaz. Çalıştırmak için `AUDIT_DB_WRITER_PASSWORD`, `AUDIT_DB_READER_PASSWORD`, `AUDIT_DB_MIGRATOR_PASSWORD` verilir; servis 8088 portunda çalışır:

```bash
./mvnw -pl services/audit-service spring-boot:run -Dspring-boot.run.profiles=local
```
