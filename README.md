# yarn-shipment-system

İplik sevkiyat, teslimat ve kalite kontrol sistemi.

## Dokümanlar

- [Java 25 kod standartları](docs/standartlar/java-kod-standartlari.md): projedeki tüm Java kodu için bağlayıcı kurallar.
- [React ve TypeScript kod standartları](docs/standartlar/react-typescript-kod-standartlari.md): frontend kodu için bağlayıcı kurallar.
- [Veritabanı tasarımı](docs/veritabani-tasarimi.md): servis başına PostgreSQL 17 veritabanları ve tablolar.

## Derleme

Java 25 ve Docker gerekir; Maven, `./mvnw` ile kendiliğinden iner.

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
| `sofor` | driver | A lokasyonu, kendi şoför kimliği |
| `fasoncu` | subcontractor | A lokasyonu, kendi fasoncu kimliği |

Token'ların servislerin beklediği claim'leri taşıdığını doğrulamak için (Node 22):

```bash
KEYCLOAK_TEST_USER_PASSWORD=<deploy/.env değeri> node tools/keycloak/verify-realm.mjs
```

Yerel Keycloak'ta ikinci faktör kurulu değildir; `admin` ve `kalite` rolleri MFA'sız oturumda düşürüldüğü için
shipment-service'i `--spring.profiles.active=local` ile başlatın (yalnızca yerelde MFA şartını kapatır).

Compose, tek bir PostgreSQL 17 konteynerinde 8 servis veritabanını ve Keycloak veritabanını, her servis için
`<servis>_migrator`, `<servis>_app` ve `<servis>_readonly` rolleriyle açar. Tabloları servisler açılışta Flyway
ile kurar; migration'lar `services/<servis>/src/main/resources/db/migration` altındadır.

Migration'ları ve veritabanı kısıt testlerini servisler olmadan doğrulamak için:

```bash
tools/db/verify-migrations.sh
```

## shipment-service'i çalıştırma

PostgreSQL'i ayrıca kurmanız gerekmez; yukarıdaki Compose yeterlidir. Servis açılışta Flyway ile tabloları
`shipment_migrator` rolüyle kurar, sonra yalnızca DML yetkili `shipment_app` rolüyle çalışır.

```bash
export SHIPMENT_DB_APP_PASSWORD=<LOCAL_DB_PASSWORD>
export SHIPMENT_DB_MIGRATOR_PASSWORD=<LOCAL_DB_PASSWORD>
./mvnw -pl services/shipment-service spring-boot:run -Dspring-boot.run.profiles=local
```

`local` profilinde ayrıca yerel örnek veri yüklenir (`db/seed-local/R__local_sample_shipments.sql`): iki lokasyonda, taslaktan kontrole kadar her durumda yedi sevkiyat; kimlikler yerel Keycloak kullanıcılarıyla eşleşir (fasoncu ve şoför kendi sevkiyatlarını görür). Dosya tekrarlanabilir migration'dır, kayıtlar iki kez eklenmez. Üretim profilinde bu konum yoktur.

Sevkiyat iptali (`POST /api/v1/shipments/{id}/cancel`, `Idempotency-Key` ve `reason` ister) yalnızca yöneticidedir (`SHIPMENT_CANCEL`) ve yalnızca taslak ya da çıkış tartımı yapılmış sevkiyatta çalışır. İptal, sevkiyattaki çuvalları bag-service'te serbest bırakır; çuval servisi yanıt vermezse iptal de yapılmaz (502 `BAG_SERVICE_UNAVAILABLE`) ve istek güvenle tekrarlanır.

Şoför fasoncuya varınca `POST /api/v1/shipments/{id}/arrival-declaration` (`Idempotency-Key` ve `sealIntact` ister) ile varışını beyan eder (`DRIVER_DECLARATION_RECORD`; yalnızca yoldaki sevkiyatta). Sevkiyat teslim beklemeye geçer, fasoncunun kör teslim girişi açılır. Mühür hasarlı beyanı akışı durdurmaz, `shipment-arrived.v1` olayıyla karşılaştırmaya gider.

Taslak sevkiyata yanlış okutulan çuval `DELETE /api/v1/shipments/{id}/bags/{uniqueKey}` ile çıkarılır (`DISPATCH_RECORD`; yalnızca çıkış tartımından önce). Satır silinmez, `removed_at` ile işaretlenir; bag-service'te rezervasyon bırakılır (`POST /api/v1/bags/reservations/release` içinde `uniqueKeys`) ve çuval başka sevkiyata atanabilir olur. Çuval listede yoksa da 204 döner. Sevkiyatın tamamını bırakmak (`uniqueKeys` olmadan) yine yalnızca `SHIPMENT_CANCEL` ile yapılır.

Ana veri listeleri (`GET /api/v1/master-data/{sites,subcontractors,drivers,vehicles}`, `q` ile arama, sayfalı) `md_*` tablolarından okunur; yerel örnek veri iki lokasyon, üç fasoncu, üç şoför ve üç araç içerir. Lokasyon listesi isteyenin kapsamıyla süzülür. 

Fasoncu ve şoför yönetimi yalnızca yöneticidedir (`MASTER_DATA_MANAGE`): `POST /api/v1/master-data/{subcontractors,drivers}` (`Idempotency-Key` ister), `PUT .../{id}`, `POST .../{id}/deactivate` ve `.../activate`. Kayıt silinmez, pasife alınır; pasif kayıt listelerde görünmez, eski sevkiyatlar etkilenmez. Pasife alınmış kayıtları yönetici `?status=ALL` ile listeler (diğer roller 403 alır); yanıttaki `active` alanı durumu gösterir. Her değişiklik `audit_local`'e değiştiren kullanıcıyla düşer. Şoförün fasoncusu verilirse etkin olmalıdır. Araç ve lokasyon için aynı uçlar vardır (`/api/v1/master-data/{vehicles,sites}`); plaka boşluksuz büyük harfe çevrilir, kullanılmış plaka 409 `DUPLICATE_PLATE` verir. Şoför ve fasoncunun giriş hesabı Keycloak'ta ayrıca açılır: [adımlar](docs/yonetim/giris-hesabi-acma.md). Otomatik hesap açma sonraki iştir.

| Değişken | Varsayılan |
|---|---|
| `SHIPMENT_DB_URL` | `jdbc:postgresql://localhost:5432/shipment_db` |
| `SHIPMENT_DB_APP_USER` / `SHIPMENT_DB_MIGRATOR_USER` | `shipment_app` / `shipment_migrator` |
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8180/realms/yarnshipment` |
| `SERVER_PORT` | `8083` |
| `YARNSHIPMENT_BAG_SERVICE_URL` | `http://localhost:8084` (çuval atama için bag-service; istek sahibinin token'ı iletilir, bu yüzden bag-service de çalışıyor olmalı) |
| `YARNSHIPMENT_KEYCLOAK_ADMIN_URL` / `YARNSHIPMENT_KEYCLOAK_ADMIN_REALM` / `YARNSHIPMENT_KEYCLOAK_ADMIN_CLIENT_ID` | `http://localhost:8180` / `yarnshipment` / `yarnshipment-admin` (şoför ve fasoncuya giriş hesabı açan servis hesabı) |
| `YARNSHIPMENT_KEYCLOAK_ADMIN_CLIENT_SECRET` | boş (boşsa hesap açma uçları 502 `LOGIN_ACCOUNT_UNAVAILABLE` döner); yerel değer `deploy/.env` içindeki `KEYCLOAK_ADMIN_CLIENT_SECRET` (varsayılan `local-admin-client-secret`) |

Persistence testleri Testcontainers ile gerçek PostgreSQL'e karşı çalışır; `./mvnw verify` için Docker açık olmalıdır.

## bag-service'i çalıştırma

Çuval kaydı, tek kullanım garantisi ve sevkiyat rezervasyonu `bag_db` veritabanındadır. Compose'un `bag_app` ve
`bag_migrator` rolleriyle çalışır; `local` profilinde yerel örnek çuvallar (`BAG-0001`…`BAG-0005`) yüklenir.

```bash
export BAG_DB_APP_PASSWORD=<LOCAL_DB_PASSWORD>
export BAG_DB_MIGRATOR_PASSWORD=<LOCAL_DB_PASSWORD>
./mvnw -pl services/bag-service spring-boot:run -Dspring-boot.run.profiles=local
```

Uçlar (port `8084`, gateway üzerinden `/api/v1/bags/**`): `GET /api/v1/bags/{uniqueKey}`, `POST /api/v1/bags/reservations`
(çuvalları sevkiyata ayırır; hepsi ya da hiçbiri) ve `POST /api/v1/bags/reservations/release` (sevkiyat iptalinde bırakır).
Çuval oluşturma ve etiket baskısı sonraki adımlardır.

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
| `BAG_SERVICE_URL` | `http://localhost:8084` |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | `localhost` / `6379` / boş (oturumlar Redis'te tutulur; gateway yeniden başlasa da oturum düşmez) |

## common-outbox (olay yayını)

Servis iş verisiyle aynı transaction'da `outbox_event` tablosuna yazdığı olayları (EVT-03) bu kütüphanenin yoklamalı yayıncısı Kafka'ya iletir. Mesajlar CloudEvents ikili kipindedir (`ce_id`, `ce_type`, `ce_source`, `ce_subject`, `ce_time` başlıkları, gövde JSON), anahtar sevkiyat kimliğidir. Kafka onayından sonra satıra `published_at` yazılır; gönderim başarısızsa satır bir sonraki turda yeniden gönderilir (en az bir kez teslim). Birden çok servis örneği aynı satırı göndermez (`FOR UPDATE SKIP LOCKED`).

Servis `yarnshipment.outbox.topic` (ör. `shipment.events.v1`) ve `yarnshipment.outbox.source` verirse yayıncı açılır; `yarnshipment.outbox.enabled=false` kapatır. Tüketen servisler `ProcessedEvents.markProcessed(eventId, type)` ile olayı işlemle aynı transaction'da kaydeder; ikinci kez gelen olay `false` döner (EVT-05). Kafka Compose ile `localhost:9092` adresinde açılır; shipment-service `KAFKA_BOOTSTRAP_SERVERS` ile başka adrese yönlendirilir.

## discrepancy-service (otomatik karşılaştırma)

Servis `shipment.events.v1` konusundaki teslim girişi olayını (`delivery-received.v1`) dinler ve çıkışla teslimi karşılaştırır. Olay sözleşmeleri `libs/event-contracts` altındadır; shipment-service'teki sözleşme testi yayınlanan olayın bu kayda kayıpsız okunduğunu denetler.

| Kural | Tolerans | Öncelik |
|---|---|---|
| Net kilo farkı | `tolerance_rule` genel kuralı; yoksa Yeşil ≤ %0,5, Sarı ≤ %1,5 (mutlak fark) | Sarı / Kırmızı |
| Çıkış tartısı yok | yok | Kırmızı |
| Çuval sayısı, Unique Key listesi | sıfır | Kırmızı |
| Mühür hasarlı/yok, mühür no farklı | sıfır | Kırmızı |
| İmzalı irsaliye yok, irsaliye alanları teslimle tutmuyor | sıfır | Sarı |

Her bulgu bir `discrepancy` satırı (`UY-<sevkiyat no>-<sıra>`, durum `OPEN`, son tarih 5 iş günü) ve `OPENED` geçmiş kaydı açar. Girdi, uyuşmazlıklar ve `comparison-completed.v1` olayı (`discrepancy.events.v1`) aynı transaction'da yazılır. Aynı olay ya da aynı sevkiyatın ikinci teslim olayı tekrar karşılaştırılmaz. Geçici hata üç kez yeniden denenir; bozuk ya da eksik olay doğrudan `shipment.events.v1.DLT` konusuna gider.

shipment-service `discrepancy.events.v1` konusunu dinler: temiz sonuç sevkiyatı `CHECKING` → `COMPLETED` yapar ve `shipment-completed.v1` yayınlar, uyuşmazlık `IN_REVIEW`'a alır. Geçiş geçmişte sistem hesabıyla (`00000000-0000-0000-0000-000000000000`) görünür. Kanıt (imzalı irsaliye fotoğrafı) yükleme henüz olmadığı için şimdilik her karşılaştırma `DOCUMENT_MISSING` (Sarı) açar ve sevkiyatlar incelemeye düşer.

Servisin HTTP ucu yoktur. Çalıştırmak için `DISCREPANCY_DB_APP_PASSWORD`, `DISCREPANCY_DB_MIGRATOR_PASSWORD` ve gerekirse `KAFKA_BOOTSTRAP_SERVERS` verilir:

```bash
DISCREPANCY_DB_APP_PASSWORD=... DISCREPANCY_DB_MIGRATOR_PASSWORD=... ./mvnw -pl services/discrepancy-service spring-boot:run
```

## evidence-service (fotoğraf ve belge yükleme)

Dosya servisten geçmez; iki adımda doğrudan nesne deposuna (S3 uyumlu, yerelde MinIO) yüklenir. Uçlar `/api/v1/evidence/**` altındadır, sözleşme `services/evidence-service/src/main/resources/openapi/evidence-service-api.yaml`.

1. `POST /api/v1/evidence/uploads` (`Idempotency-Key` zorunlu): sevkiyat, kanıt türü, dosya türü, boyut ve kaynak (kamera/galeri) bildirilir. Servis izni, 20 MB sınırını ve izinli türleri (`image/jpeg`, `image/png`, `image/heic`, `application/pdf`) denetler, sevkiyatı shipment-service'e isteyenin token'ıyla sorar (görünmeyen sevkiyat `404 SHIPMENT_NOT_FOUND`; tamamlanmış/iptal `409 SHIPMENT_LOCKED`) ve yaklaşık 10 dakika geçerli imzalı bir `PUT` adresi döner.
2. İstemci dosyayı o adrese, cevaptaki başlıklarla aynen `PUT` eder.
3. `POST /api/v1/evidence/uploads/{evidenceId}/confirm`: sunucu dosyayı depodan okuyup gerçek boyut, tür ve SHA-256'yı kendisi hesaplar (SEC-09); bildirilenle uyuşmazsa `422 UPLOAD_MISMATCH`, dosya henüz yoksa `409 UPLOAD_NOT_COMPLETED`. Onay tekrarlanabilir.

Yükleme izni kanıt türünün aşamasına bağlıdır: sevk (`EVIDENCE_UPLOAD_DISPATCH`), şoför, teslim, şikâyet. Aynı dosya (aynı hash) başka sevkiyatta daha önce yüklenmişse kayıt yine alınır ama `evidence_duplicate` satırı ve `evidence-reuse-detected.v1` olayı yazılır. Kanıt kaydı ile `evidence-uploaded.v1` olayı (`evidence.events.v1`) aynı transaction'da yazılır.

Yerelde MinIO `docker compose` ile açılır (`deploy/.env` içinde `EVIDENCE_STORAGE_ACCESS_KEY` ve `EVIDENCE_STORAGE_SECRET_KEY`; `evidence` kovası kendiliğinden oluşur). Servis aynı anahtarlarla, `EVIDENCE_DB_APP_PASSWORD` ve `EVIDENCE_DB_MIGRATOR_PASSWORD` ile çalıştırılır:

```bash
EVIDENCE_DB_APP_PASSWORD=... EVIDENCE_DB_MIGRATOR_PASSWORD=... \
EVIDENCE_STORAGE_ACCESS_KEY=... EVIDENCE_STORAGE_SECRET_KEY=... ./mvnw -pl services/evidence-service spring-boot:run
```

İmzalı adres, `EVIDENCE_STORAGE_ENDPOINT` adresiyle üretilir; telefon ya da tarayıcının eriştiği adres olmalıdır. Kanıt görüntüleme, geçersiz kılma ve imzalı irsaliyenin teslim girişine bağlanması sonraki adımlardır.

## common-web (ortak web altyapısı)

Servis ana sınıfına `@EnableYarnShipmentWeb` eklenir; şunları verir:

- **Hata yanıtı (ERR-06/07):** `ProblemFactory` ve ortak advice (`MISSING_HEADER`, `VALIDATION_FAILED`, `MALFORMED_REQUEST`). Servise özgü hatalar kendi advice'ında aynı fabrikayı kullanır.
- **İstek kimliği:** her isteğe `X-Request-Id` (istemcinin güvenli değeri korunur, yoksa üretilir); log bağlamında `requestId`, hata gövdesinde `requestId` alanı.
- **Idempotency-Key (API-04):** servis `JdbcClient` kullanıyorsa kendiliğinden devreye girer. `POST/PUT/PATCH` isteğinde `Idempotency-Key` varsa: aynı anahtar ve aynı gövde ilk başarılı (2xx) yanıtı tekrar döner (`Idempotency-Replayed: true`), aynı anahtar farklı gövde `422 IDEMPOTENCY_KEY_REUSED`, hâlâ işlenen istek `409 IDEMPOTENCY_IN_PROGRESS`. Başarısız istek anahtarı bırakır. Anahtar kullanıcıya özeldir; kayıtlar 24 saat tutulur (`yarnshipment.idempotency.*`). Servisin migration'ında `idempotency_record` tablosu gerekir (referans şema: `libs/common-web/src/main/resources/idempotency/idempotency_record.sql`).
