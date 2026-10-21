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
`yarnshipment` realm'i, rolleri ve claim mapper'ları sonraki adımda içe aktarılacak.

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
./mvnw -pl services/shipment-service spring-boot:run
```

| Değişken | Varsayılan |
|---|---|
| `SHIPMENT_DB_URL` | `jdbc:postgresql://localhost:5432/shipment_db` |
| `SHIPMENT_DB_APP_USER` / `SHIPMENT_DB_MIGRATOR_USER` | `shipment_app` / `shipment_migrator` |
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8180/realms/yarnshipment` |
| `SERVER_PORT` | `8083` |

Persistence testleri Testcontainers ile gerçek PostgreSQL'e karşı çalışır; `./mvnw verify` için Docker açık olmalıdır.
