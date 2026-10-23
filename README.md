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

Ana veri listeleri (`GET /api/v1/master-data/{sites,subcontractors,drivers,vehicles}`, `q` ile arama, sayfalı) `md_*` tablolarından okunur; yerel örnek veri iki lokasyon, üç fasoncu, üç şoför ve üç araç içerir. Lokasyon listesi isteyenin kapsamıyla süzülür. 

Fasoncu ve şoför yönetimi yalnızca yöneticidedir (`MASTER_DATA_MANAGE`): `POST /api/v1/master-data/{subcontractors,drivers}` (`Idempotency-Key` ister), `PUT .../{id}`, `POST .../{id}/deactivate` ve `.../activate`. Kayıt silinmez, pasife alınır; pasif kayıt listelerde görünmez, eski sevkiyatlar etkilenmez. Pasife alınmış kayıtları yönetici `?status=ALL` ile listeler (diğer roller 403 alır); yanıttaki `active` alanı durumu gösterir. Her değişiklik `audit_local`'e değiştiren kullanıcıyla düşer. Şoförün fasoncusu verilirse etkin olmalıdır. Araç ve lokasyon için aynı uçlar vardır (`/api/v1/master-data/{vehicles,sites}`); plaka boşluksuz büyük harfe çevrilir, kullanılmış plaka 409 `DUPLICATE_PLATE` verir. Şoför ve fasoncunun giriş hesabı Keycloak'ta ayrıca açılır: [adımlar](docs/yonetim/giris-hesabi-acma.md). Otomatik hesap açma sonraki iştir.

| Değişken | Varsayılan |
|---|---|
| `SHIPMENT_DB_URL` | `jdbc:postgresql://localhost:5432/shipment_db` |
| `SHIPMENT_DB_APP_USER` / `SHIPMENT_DB_MIGRATOR_USER` | `shipment_app` / `shipment_migrator` |
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8180/realms/yarnshipment` |
| `SERVER_PORT` | `8083` |
| `YARNSHIPMENT_BAG_SERVICE_URL` | `http://localhost:8084` (çuval atama için bag-service; istek sahibinin token'ı iletilir, bu yüzden bag-service de çalışıyor olmalı) |

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

## common-web (ortak web altyapısı)

Servis ana sınıfına `@EnableYarnShipmentWeb` eklenir; şunları verir:

- **Hata yanıtı (ERR-06/07):** `ProblemFactory` ve ortak advice (`MISSING_HEADER`, `VALIDATION_FAILED`, `MALFORMED_REQUEST`). Servise özgü hatalar kendi advice'ında aynı fabrikayı kullanır.
- **İstek kimliği:** her isteğe `X-Request-Id` (istemcinin güvenli değeri korunur, yoksa üretilir); log bağlamında `requestId`, hata gövdesinde `requestId` alanı.
- **Idempotency-Key (API-04):** servis `JdbcClient` kullanıyorsa kendiliğinden devreye girer. `POST/PUT/PATCH` isteğinde `Idempotency-Key` varsa: aynı anahtar ve aynı gövde ilk başarılı (2xx) yanıtı tekrar döner (`Idempotency-Replayed: true`), aynı anahtar farklı gövde `422 IDEMPOTENCY_KEY_REUSED`, hâlâ işlenen istek `409 IDEMPOTENCY_IN_PROGRESS`. Başarısız istek anahtarı bırakır. Anahtar kullanıcıya özeldir; kayıtlar 24 saat tutulur (`yarnshipment.idempotency.*`). Servisin migration'ında `idempotency_record` tablosu gerekir (referans şema: `libs/common-web/src/main/resources/idempotency/idempotency_record.sql`).
