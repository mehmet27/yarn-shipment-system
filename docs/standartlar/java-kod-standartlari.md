# İplik Sevkiyat Sistemi – Java 25 Kod Standartları

| Alan | Değer |
| --- | --- |
| Sürüm | 1.0 |
| Tarih | 20.10.2026 |
| Kapsam | Backend: 8 Spring Boot servisi, API Gateway, `libs/*` ortak kütüphaneleri |
| Dayandığı dokümanlar | Ön analiz raporu v1.1, Java mikroservis mimarisi v1, Veritabanı tasarımı |
| Bağlayıcılık | **Projede yazılan her Java satırı bu dokümana uyar.** Kurala aykırı kod birleştirilmez. |

Bu doküman projenin temel taşıdır. Kod yazmaya başlamadan önce okunur, code review'da kural kimlikleriyle (ör. `DOM-04`) atıf yapılır. Kurallar iki seviyededir:

- **ZORUNLU**: İstisnası yoktur ya da yalnızca bölüm 18'deki istisna süreciyle delinebilir. Çoğu derlemede otomatik denetlenir.
- **ÖNERİLEN**: Varsayılan davranıştır; uymamak için PR açıklamasında kısa gerekçe yeterlidir.

Kural belirtilmeden yazılan her madde ZORUNLU kabul edilir.

---

## İçindekiler

1. [Temel ilkeler](#1-temel-ilkeler)
2. [Java 25 dil kullanımı](#2-java-25-dil-kullanımı)
3. [Adlandırma ve dil](#3-adlandırma-ve-dil)
4. [Kod boyutu, karmaşıklık ve yapı sınırları](#4-kod-boyutu-karmaşıklık-ve-yapı-sınırları)
5. [Repo, modül ve paket yapısı](#5-repo-modül-ve-paket-yapısı)
6. [Altıgen mimari katman kuralları](#6-altıgen-mimari-katman-kuralları)
7. [Domain modelleme](#7-domain-modelleme)
8. [Null güvenliği, hata ve istisna yönetimi](#8-null-güvenliği-hata-ve-istisna-yönetimi)
9. [Kalıcılık: JPA, Flyway, transaction](#9-kalıcılık-jpa-flyway-transaction)
10. [REST API kuralları](#10-rest-api-kuralları)
11. [Olaylar, Kafka ve outbox](#11-olaylar-kafka-ve-outbox)
12. [Eşzamanlılık ve sanal thread'ler](#12-eşzamanlılık-ve-sanal-threadler)
13. [Güvenlik, kör giriş ve KVKK](#13-güvenlik-kör-giriş-ve-kvkk)
14. [Loglama ve gözlemlenebilirlik](#14-loglama-ve-gözlemlenebilirlik)
15. [Konfigürasyon](#15-konfigürasyon)
16. [Test kuralları](#16-test-kuralları)
17. [Derleme, bağımlılık ve araç zinciri](#17-derleme-bağımlılık-ve-araç-zinciri)
18. [Git, PR, code review ve istisna süreci](#18-git-pr-code-review-ve-istisna-süreci)
19. [Ek A: Domain sözlüğü (Türkçe → kod)](#ek-a-domain-sözlüğü-türkçe--kod)
20. [Ek B: Olay adları sözlüğü](#ek-b-olay-adları-sözlüğü)
21. [Ek C: Otomatik denetim özeti](#ek-c-otomatik-denetim-özeti)
22. [Ek D: Bu dokümanda alınan kararlar](#ek-d-bu-dokümanda-alınan-kararlar)
23. [Ek E: Araç ayarları (derlemede zorlanan kurallar)](#ek-e-araç-ayarları-derlemede-zorlanan-kurallar)

---

## 1. Temel ilkeler

Bu sistemin değeri **kanıt bütünlüğünden** gelir: kör giriş, değiştirilemezlik, tam iz. Kod kuralları bu yüzden "güzel kod" kadar "yanlış kodu yazılamaz hale getirmek" üzerine kuruludur.

- **GEN-01 Derleyici ve araç önce gelir.** Bir kural otomatik denetlenebiliyorsa (derleyici, Error Prone, ArchUnit, test) denetlenir; review'a bırakılmaz. Ek C hangi kuralın nerede denetlendiğini, Ek E araç ayarlarını listeler.
- **GEN-02 Geçersiz durum temsil edilemez.** Domain nesneleri oluşturulduğu anda geçerlidir. "Önce oluştur, sonra `validate()` çağır" deseni yasaktır.
- **GEN-03 Değişmezlik varsayılandır.** Alanlar `final`, koleksiyonlar değiştirilemez, değer nesneleri `record`. Değişebilir durum yalnızca aggregate kökünde ve kontrollü metotlarla değişir.
- **GEN-04 Silme yok, düzeltme kaydı var.** Hiçbir iş verisi fiziksel olarak silinmez ya da üzerine yazılmaz (rapor Bölüm 3, 11; veritabanı tasarımı "Silme yok"). Kod bu varsayımı bozan bir yol açmaz.
- **GEN-05 Açık olan örtük olandan iyidir.** Sihirli davranış (Lombok, çalışma zamanı proxy hileleri, `@Autowired` alan enjeksiyonu, reflection ile alan erişimi) kullanılmaz.
- **GEN-06 Basitlik, ama büyümeye kapalı değil.** Faz 1 düşük yüklü, güvenilirlik odaklıdır. Reaktif programlama, genel amaçlı "framework içinde framework" soyutlamaları ve ihtiyaç doğmadan önbellek yazılmaz. Buna karşılık sonradan değiştirmesi pahalı olan temel kararlar (servis sınırları, sürümlü sözleşmeler, tipli kimlikler, lokasyon kavramı) baştan büyümeye uygun alınır; bkz. Ek D.

---

## 2. Java 25 dil kullanımı

### 2.1 Sürüm ve önizleme özellikleri

- **LANG-01** Hedef **Java 25 LTS**'tir. Tüm modüllerde `maven.compiler.release=25`. Yedek plan Java 21 LTS'tir (mimari doküman); bu yüzden aşağıda "25'e özgü" işaretli özellikler yalnızca belirtilen yerlerde kullanılır ki gerekirse geri dönüş maliyeti düşük kalsın.
- **LANG-02 Önizleme (preview) ve incubator özellikleri üretim kodunda yasaktır.** `--enable-preview` hiçbir modülde açılmaz. Java 25'te hâlâ önizlemede olan özellikler (ör. Structured Concurrency, ilkel tiplerle desen eşleme, Stable Values, Vector API) kullanılmaz. Bu özellikler kalıcı hale geldiğinde bu doküman güncellenir.

### 2.2 Kullanılması beklenen özellikler

| Özellik | Kural |
| --- | --- |
| `record` | Değer nesneleri, DTO'lar, komutlar, sorgular, olay yükleri ve `@ConfigurationProperties` sınıfları **record olarak yazılır** (LANG-03). |
| `sealed` sınıf/arayüz | Kapalı tip hiyerarşileri (sonuç tipleri, uyuşmazlık bulguları, domain istisnaları) `sealed` yazılır (LANG-04). |
| `switch` ifadesi + desen eşleme | `sealed` tip ve `enum` üzerinde `switch` **ifade** olarak ve `default` dalı **olmadan** yazılır; böylece yeni bir alt tip eklendiğinde derleyici eksik dalı yakalar (LANG-05). |
| Record desenleri | Record'u açmak için kullanılabilir: `case Discrepant(var id, var findings) ->` (ÖNERİLEN). |
| Adsız değişken `_` | Kullanılmayan değişken, yakalanan ama kullanılmayan istisna ve desen bileşenleri için `_` kullanılır: `catch (DuplicateKeyException _)` (LANG-06). |
| Sıralı koleksiyonlar | `list.get(0)` yerine `getFirst()`, `list.get(list.size() - 1)` yerine `getLast()`, ters gezinti için `reversed()` (ÖNERİLEN). |
| Esnek yapıcı gövdeleri (25'e özgü) | `super(...)` çağrısından önce argüman doğrulaması yapılabilir. Yalnızca doğrulama ve argüman hazırlığı için kullanılır, `this` erişimi yoktur (ÖNERİLEN). |
| `ScopedValue` (25'e özgü) | Kendi yazdığımız istek bağlamı taşıma ihtiyacında `ThreadLocal` yerine kullanılır (bkz. CONC-05). |
| Stream Gatherers | Pencereleme, gruplu toplama gibi işlerde özel `Collector` yazmak yerine `Gatherers` kullanılabilir (ÖNERİLEN). |
| Javadoc | Javadoc klasik `/** ... */` biçimindedir (LANG-07). Java 23+ `///` Markdown yorumları, Checkstyle Javadoc kontrolleri (COMM-02) bunları tanımadığı için kullanılmaz. |
| `var` | Sağ taraf tipi açıkça gösteriyorsa kullanılır (`var shipment = new Shipment(...)`). Dönüş tipi belirsiz metot çağrılarında, alanlarda ve lambda parametrelerinde kullanılmaz (LANG-08). |
| Metin blokları `"""` | Çok satırlı SQL, JSON test verisi ve şablonlar için kullanılır (ÖNERİLEN). |

### 2.3 Kullanılmayacaklar

- **LANG-09** `import module ...;` (modül import bildirimi) üretim kodunda kullanılmaz; joker import gibi hangi tipin nereden geldiğini gizler. Yalnızca `tools/` altındaki tek dosyalık betiklerde serbesttir.
- **LANG-10** Kompakt kaynak dosyası / örnek `main` metodu (`void main()`) yalnızca `tools/` betiklerinde kullanılır.
- **LANG-11** Joker import (`import java.util.*;`) yasaktır. Statik import yalnızca test kodunda (AssertJ, Mockito) ve `java.util.Objects.requireNonNull` için serbesttir.
- **LANG-12** `Object.finalize()`, `Thread.stop()`, `sun.*` ve `jdk.internal.*` paketleri, `Unsafe` kullanılmaz.
- **LANG-13** Ham tip (`List` yerine `List<Bag>`) ve kontrolsüz dönüşüm uyarısı bırakılmaz; derleme `-Xlint:all -Werror` ile yapılır (bkz. BUILD-04).

### 2.4 Örnek: sealed sonuç tipi ve kapsamlı switch

```java
/** Karşılaştırma servisinin sevkiyat için ürettiği sonuç. */
public sealed interface ComparisonResult {

    ShipmentId shipmentId();

    record Clean(ShipmentId shipmentId) implements ComparisonResult {}

    record Discrepant(ShipmentId shipmentId, List<DiscrepancyFinding> findings)
            implements ComparisonResult {
        public Discrepant {
            findings = List.copyOf(findings);
            if (findings.isEmpty()) {
                throw new IllegalArgumentException("Discrepant sonuç en az bir bulgu içermeli");
            }
        }
    }
}

// Kullanım: default dalı yok, yeni alt tip eklenirse derleme kırılır.
ShipmentStatus next = switch (result) {
    case ComparisonResult.Clean _ -> ShipmentStatus.COMPLETED;
    case ComparisonResult.Discrepant _ -> ShipmentStatus.IN_REVIEW;
};
```

### 2.5 Örnek: super() öncesi doğrulama (Java 25)

```java
public final class ShipmentLockedException extends DomainException {

    public ShipmentLockedException(ShipmentId shipmentId) {
        Objects.requireNonNull(shipmentId, "shipmentId");
        super(ErrorCode.SHIPMENT_LOCKED, "Sevkiyat kilitli: " + shipmentId.value());
    }
}
```

---

## 3. Adlandırma ve dil

### 3.1 Dil kararı

- **NAME-01 Kod tanımlayıcıları İngilizcedir.** Sınıf, metot, alan, paket, tablo, kolon, olay tipi, Kafka konusu, REST yolu ve JSON alanları İngilizce yazılır. Veritabanı tasarımı da İngilizce `snake_case` kullandığı için uçtan uca tek dil olur.
- **NAME-02 Domain terimleri Ek A'daki sözlükten alınır.** Aynı kavrama iki farklı İngilizce ad verilmez (ör. fasoncu her yerde `Subcontractor`, asla `Contractor`/`Supplier`). Sözlükte olmayan yeni bir terim gerekiyorsa önce Ek A güncellenir.
- **NAME-03 Tanımlayıcılarda yalnızca ASCII karakter kullanılır.** `ş, ğ, ı, İ, ö, ü, ç` tanımlayıcılarda yasaktır.
- **NAME-04 Yorumlar, Javadoc, ADR ve commit açıklamaları Türkçedir.** İş kuralları ekibin konuştuğu dille daha doğru anlatılır. Rapor bölümüne atıf vermek teşviktir: `/** Rapor 9.1: fark yüzdesi çıkış net kilosuna göre hesaplanır. */`
- **NAME-05 Log mesajları ve istisna mesajları İngilizcedir;** kullanıcıya dönen `ProblemDetail.title/detail` alanları Türkçedir (bkz. API-08). Log arama ve alarm kuralları İngilizce sabit metinler üzerinde kurulur.

### 3.2 Biçim kuralları

| Öğe | Biçim | Örnek |
| --- | --- | --- |
| Paket | küçük harf, tek kelime segmentler | `com.yarnshipment.shipment.domain.model` |
| Sınıf, record, arayüz | `PascalCase`, isim | `DeliveryReceipt`, `ToleranceRule` |
| Kullanım senaryosu (port in) | fiil + `UseCase` | `SubmitDeliveryReceiptUseCase` |
| Kullanım senaryosu uygulaması | fiil + `Service` | `SubmitDeliveryReceiptService` |
| Giden port | ne yaptığını anlatan isim + `Port` | `ShipmentRepositoryPort`, `BagReservationPort` |
| Adapter | teknoloji + rol | `JpaShipmentRepositoryAdapter`, `KafkaComparisonCompletedListener`, `ShipmentController` |
| Komut / sorgu | fiil + `Command`, isim + `Query` | `RecordDispatchWeighingCommand`, `ShipmentFileQuery` |
| Olay yükü | geçmiş zaman, `Event` soneki **yok** | `DeliveryReceived`, `EvidenceUploaded` |
| DTO (web) | `Request` / `Response` soneki, hedef kitle öneki | `SubcontractorDeliveryView`, `ShipmentAdminResponse` |
| JPA entity | `JpaEntity` soneki | `ShipmentJpaEntity` |
| Metot | `camelCase`, fiil | `assignBag`, `lock`, `isLocked` |
| Sabit | `UPPER_SNAKE_CASE` | `MAX_BAG_COUNT` |
| Enum değeri | `UPPER_SNAKE_CASE`, veritabanındaki CHECK değeriyle **birebir aynı** | `AWAITING_DELIVERY` |
| Test sınıfı | test edilen sınıf + `Test` / `IT` | `ShipmentTest`, `ShipmentRepositoryIT` |

- **NAME-06** Kısaltma kullanılmaz (`shp`, `qty`, `mgr` yok). Kabul edilen kısaltmalar: `id`, `dto`, `url`, `uri`, `api`, `sha256`, `kg`, `pct`, `jwt`.
- **NAME-07** Kısaltmalar PascalCase'de tek büyük harfle yazılır: `HttpClient`, `JwtDecoder`, `Sha256Hash` (`HTTPClient` değil).
- **NAME-08** Boolean alan ve metotlar `is/has/can` ile başlar: `isLocked()`, `hasOpenDiscrepancy()`. Record bileşenlerinde de geçerlidir: `boolean randomCheckSelected` → erişim `randomCheckSelected()` olur; bu durumda bileşen adı soru gibi okunacak şekilde seçilir.

### 3.3 Türkçe yerel ayar tuzağı

- **NAME-09** `toUpperCase()`, `toLowerCase()`, `String.format()` ve `equalsIgnoreCase` dışındaki büyük/küçük harf işlemleri **her zaman `Locale` ile** çağrılır. Teknik metinlerde `Locale.ROOT` kullanılır. Türkçe yerel ayarda `"unique_key".toUpperCase()` sonucu `"UNİQUE_KEY"` olur ve enum, başlık ve anahtar karşılaştırmalarını sessizce bozar. Error Prone `StringCaseLocaleUsage` kontrolü hata seviyesinde açıktır.
- **NAME-10** Container imajlarında JVM `-Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8` ile başlatılır; yine de kod NAME-09'a güvenir, bu ayara değil.
- **NAME-11** Kullanıcıya gösterilen metinlerde Türkçe sıralama gerekiyorsa `Collator.getInstance(Locale.forLanguageTag("tr-TR"))` kullanılır.

---

### 3.4 Sınıf adlandırma

- **NAME-12** Sınıf adı bir **isimdir** ve tek bir sorumluluğu anlatır. Sınıf adı `^[A-Z][a-zA-Z0-9]*$` kalıbına uyar ve en az 3 karakterdir.
- **NAME-13 Yasaklı sınıf adı sonekleri:** `Manager`, `Helper`, `Util`, `Utils`, `Impl`, `Data`, `Info`, `Processor`, `Handler` (Spring ve Kafka hata işleyicileri hariç), `Base` öneki. Bu adlar "ne yaptığı belli olmayan sınıf" üretir. Bir arayüzün tek uygulaması varsa adı uygulamanın farkını anlatır (`ShipmentRepositoryPort` → `JpaShipmentRepositoryAdapter`), `Impl` ile bitmez.
- **NAME-14** Ortak yardımcı statik metotlar gerekiyorsa sınıf, ait olduğu kavramın çoğul adını alır ve yalnızca `libs/common-*` içinde bulunur (`Percentages`, `UniqueKeys`); `final` ve özel yapıcılıdır. Servis kodunda statik yardımcı sınıf açılmaz; davranış ilgili değer nesnesine konur.
- **NAME-15** İstisna sınıfları `Exception` ile biter ve neyin yanlış gittiğini söyler: `ShipmentLockedException`, `BagAlreadyAssignedException`. `GeneralException`, `ServiceException` gibi adlar yasaktır.
- **NAME-16** Arayüz adında `I` öneki kullanılmaz (`IShipmentService` yasak).
- **NAME-17** Soyut sınıflar `Abstract` önekiyle başlar ve yalnızca `libs/common-*` içinde bulunabilir. Servis kodunda soyut sınıf kalıtımı yerine `sealed interface` + record veya kompozisyon kullanılır.
- **NAME-18** Generic tip parametreleri tek büyük harftir (`T`, `E`, `K`, `V`, `R`) ya da tek büyük harf + rakamdır (`T2`).

### 3.5 Metot adlandırma

- **NAME-19** Metot adı `^[a-z][a-zA-Z0-9]*$` kalıbına uyar, en az 2 karakterdir ve **fiille başlar**. Fiil kuralının istisnaları: erişimciler (NAME-24) ve türetilmiş değer erişimcileri (`weighing.net()`), statik fabrikalar `of`/`from` ve değer nesnesi aritmetiği (`plus`, `minus`, `multiply`, `compareTo`). Bir metot adı en fazla 5 kelimedir (`assignBagToShipment` = 4 kelime).
- **NAME-20 Fiil sözlüğü.** Aynı anlam için her zaman aynı fiil kullanılır:

| Fiil / önek | Anlamı | Dönüş | Örnek |
| --- | --- | --- | --- |
| `find…` | Bulunamayabilir; aramak | `Optional<T>` veya `List<T>` | `findByUniqueKey(UniqueKey)` |
| `get…` | Mutlaka vardır; yoksa `ResourceNotFoundException` | `T` | `getShipment(ShipmentId)` |
| `exists…` | Var mı? | `boolean` | `existsByShipmentNo(...)` |
| `count…` | Sayı | `long` | `countOpenDiscrepancies(...)` |
| `is…` / `has…` / `can…` | Durum sorusu | `boolean` | `isLocked()`, `hasOpenDiscrepancy()`, `canTransitionTo(...)` |
| `create…` | Yeni kayıt oluşturmak (application katmanı) | yeni nesne/kimlik | `createShipment(...)` |
| `of` / `from` | Statik fabrika: `of` parçalardan, `from` başka bir tipten dönüştürerek | yeni nesne | `Kilogram.of("12.500")`, `ShipmentAdminResponse.from(shipment)` |
| `to…` | Başka tipe dönüştürmek | yeni nesne | `toEntity()`, `toCommand()` |
| `with…` | Değişmez nesnenin bir alanı değişmiş kopyası | aynı tip | `withStatus(...)` |
| `calculate…` | Yan etkisiz hesaplama | değer | `calculateDifferencePercentage(...)` |
| `validate…` | Doğrular, geçersizse istisna fırlatır | `void` | `validateTareDeviation(...)` |
| `record…` | Ölçüm/beyan kaydetmek | `void` veya kimlik | `recordDispatchWeighing(...)` |
| `assign…` / `release…` | Bağlamak / bağı çözmek | `void` | `assignBag(...)`, `releaseReservation(...)` |
| `submit…` / `approve…` / `reject…` | İş akışı adımı | `void` | `submitDeliveryReceipt(...)` |
| `lock` / `cancel` / `invalidate` / `resolve` | Durum geçişi | `void` | `shipment.lock(clock)` |
| `publish…` | Olay yayınlamak (yalnızca `adapter.out.messaging`) | `void` | `publishDeliveryReceived(...)` |
| `on<OlayAdı>` | Olay dinleyicisi (yalnızca `adapter.in.messaging`) | `void` | `onComparisonCompleted(...)` |

- **NAME-21 Yasaklı belirsiz metot adları:** `process`, `handle` (Spring `@ExceptionHandler` hariç), `doIt`, `do…`, `manage…`, `perform…`, `execute` (bkz. NAME-22), `run` (`Runnable` uygulaması hariç), `data`, `info`, `check` (yerine `validate…` veya `is…`), `update` tek başına (neyi güncellediği yazılır: `updateContactPhone`).
- **NAME-22 Use case arayüzü** tek metotludur ve metot adı arayüz adındaki fiildir: `SubmitDeliveryReceiptUseCase.submit(SubmitDeliveryReceiptCommand)`, `GetShipmentFileUseCase.get(ShipmentFileQuery)`.
- **NAME-23 Controller metotları** OpenAPI `operationId` ile aynı addadır; `operationId` fiil + kaynak biçimindedir (`submitDeliveryReceipt`, `listShipments`, `getShipment`).
- **NAME-24 Erişimciler.** Domain ve uygulama sınıflarında alan erişimcisi `get` öneki **almaz**, record gibi alan adını taşır: `shipment.status()`, `shipment.shipmentNo()`. `get…` öneki NAME-20'deki anlamına ("mutlaka bulunur") ayrılmıştır. JavaBean getter'ı yalnızca bir framework zorunlu kıldığında (JPA entity, `@ConfigurationProperties` olmayan Spring bean'i) yazılır.
- **NAME-25** Boolean dönen metot olumsuz adlandırılmaz: `isNotLocked()` değil `!isLocked()`; `isValid()` gibi neye göre geçerli olduğu belirsiz adlar yerine kural adı kullanılır (`isWithinTolerance()`).

### 3.6 Değişken, alan ve sabit adlandırma

- **NAME-26** Yerel değişken, parametre ve alan adları `^[a-z][a-zA-Z0-9]*$` kalıbına uyar ve en az 2 karakterdir. Tek harfli ad yalnızca klasik `for` sayaçlarında (`i`, `j`) ve tek satırlık lambda parametrelerinde serbesttir; çok satırlı lambda'da parametre adı açık yazılır.
- **NAME-27** Ad, tipi değil anlamı anlatır: `shipmentList` değil `shipments`, `bagMap` değil `bagsByUniqueKey`. Koleksiyonlar çoğul, `Map`'ler `<değer>By<anahtar>` biçimindedir.
- **NAME-28** Birim adın parçasıdır: `netKg`, `diffPct`, `uploadTtl` (`Duration` ise birim yazılmaz, tip zaten birimdir). `weight`, `amount`, `value` tek başına kullanılmaz.
- **NAME-29** Kimlik taşıyan değişkenler `…Id` ile biter ve tipli kimliktir (`ShipmentId shipmentId`). Okunabilir iş numaraları `…No` ile biter (`shipmentNo`, `sealNo`, `lotNo`).
- **NAME-30** Zaman alanları `…At` ile biter ve `Instant`'tır (`lockedAt`, `capturedAt`); tarih alanları `…Date` ile biter ve `LocalDate`'tir (`productionDate`).
- **NAME-31** `static final` sabitler `UPPER_SNAKE_CASE`'tir. Tek istisna logger'dır: `private static final Logger log`.
- **NAME-32** Enum tipi tekildir (`ShipmentStatus`, `EvidenceType`), değerleri `UPPER_SNAKE_CASE`'tir.
- **NAME-33** Paket adı segmentleri yalnızca küçük harf ve rakamdır (`^[a-z][a-z0-9]*$`); alt çizgi ve büyük harf yoktur.

### 3.7 Test adlandırma

- **NAME-34** Birim test sınıfı `<TestEdilenSınıf>Test`, entegrasyon testi `<TestEdilen>IT`, sözleşme testi `<Ad>ContractTest`, mimari test `ArchitectureTest` adını alır.
- **NAME-35** Test metotları `should` ile başlar ve beklenen davranışı anlatır; koşul `When`/`Given` ile eklenir: `shouldRejectBagWhenAlreadyAssignedToAnotherShipment`. Kalıp: `^should[A-Z][a-zA-Z0-9]*$`. `test1`, `testSubmit` gibi adlar yasaktır. Her test metodunda Türkçe `@DisplayName` zorunludur.

---

## 4. Kod boyutu, karmaşıklık ve yapı sınırları

Bu bölümdeki sayılar **pazarlık konusu değildir**. Hepsi derleme sırasında Checkstyle, PMD veya ArchUnit ile **hata** seviyesinde denetlenir; sınırı aşan kod derlenmez, dolayısıyla birleştirilemez. Uyarı seviyesi ve "baseline" (eski ihlalleri görmezden gelme) kullanılmaz: proje sıfırdan başladığı için ilk satırdan itibaren tüm kurallar geçerlidir. Araç ayarlarının tamamı Ek E'dedir.

Sınırı aşan kodun çözümü sınırı esnetmek değil, kodu bölmektir: uzun metot küçük metotlara, büyük sınıf ayrı sorumluluklara, çok parametreli metot bir parametre record'una ayrılır.

### 4.1 Boyut sınırları

| Kural | Ne | Sınır (üretim kodu) | Sınır (test kodu) | Denetleyen |
| --- | --- | --- | --- | --- |
| **SIZE-01** | Bir dosyanın (sınıf, record, enum, arayüz) toplam satır sayısı, import ve yorumlar dahil | **≤ 400 satır** | ≤ 600 satır | Checkstyle `FileLength` |
| **SIZE-02** | Bir metodun veya yapıcının uzunluğu, boş satırlar hariç | **≤ 30 satır** | ≤ 40 satır | Checkstyle `MethodLength` |
| **SIZE-03** | Bir metottaki çalıştırılabilir ifade sayısı | **≤ 20** | ≤ 30 | Checkstyle `ExecutableStatementCount` |
| **SIZE-04** | Lambda gövdesi uzunluğu | **≤ 8 satır** | ≤ 8 satır | Checkstyle `LambdaBodyLength` |
| **SIZE-05** | Anonim iç sınıf uzunluğu (mümkünse lambda kullanılır) | **≤ 20 satır** | ≤ 20 satır | Checkstyle `AnonInnerLength` |
| **SIZE-06** | Satır uzunluğu (import ve paket satırları hariç) | **≤ 120 karakter** | ≤ 120 karakter | Checkstyle `LineLength`, Spotless |
| **SIZE-07** | Metot parametre sayısı | **≤ 4** | ≤ 4 | Checkstyle `ParameterNumber` |
| **SIZE-08** | Yapıcı parametre sayısı (Spring bean'lerinde enjekte edilen bağımlılık sayısı dahil) | **≤ 7** | ≤ 7 | Checkstyle `ParameterNumber` (yapıcı) |
| **SIZE-09** | Record bileşen sayısı | **≤ 10** | ≤ 10 | Checkstyle `RecordComponentNumber` |
| **SIZE-10** | Bir sınıftaki örnek (instance) alanı sayısı | **≤ 10** | — | PMD `TooManyFields` |
| **SIZE-11** | Bir sınıftaki metot sayısı: toplam / `public` | **≤ 25 / ≤ 15** | ≤ 40 / — | Checkstyle `MethodCount` |
| **SIZE-12** | Bir paketteki üst düzey tip sayısı | **≤ 30** | — | ArchUnit |
| **SIZE-13** | Bir dosyadaki üst düzey tip sayısı | **1** | 1 | Checkstyle `OuterTypeNumber` |

- **SIZE-14** Yapıcı parametresi 7'yi aşan bir Spring bean'i çok fazla iş yapıyordur; bağımlılık sayısı artınca sınıf bölünür. Application servislerinde ÖNERİLEN sınır 5 port bağımlılığıdır.
- **SIZE-15** 4'ten fazla değer taşıması gereken metotlar bir `record` parametre alır (use case'ler zaten `Command`/`Query` record'u alır, bkz. NAME-22).

### 4.2 Karmaşıklık ve iç içe derinlik sınırları

| Kural | Ne | Sınır | Denetleyen |
| --- | --- | --- | --- |
| **CPLX-01** | Metot başına döngüsel (cyclomatic) karmaşıklık; bir `switch` tek karar noktası sayılır | **≤ 10** | Checkstyle `CyclomaticComplexity` |
| **CPLX-02** | Metot başına bilişsel (cognitive) karmaşıklık | **≤ 14** (15 ve üzeri hata) | PMD `CognitiveComplexity` |
| **CPLX-03** | Metot başına NPath karmaşıklığı (olası yürütme yolu sayısı) | **≤ 200** | Checkstyle `NPathComplexity` |
| **CPLX-04** | Tek bir boolean ifadedeki `&&`, `\|\|`, `&`, `\|`, `^` operatör sayısı | **≤ 3** | Checkstyle `BooleanExpressionComplexity` |
| **CPLX-05** | İç içe `if` derinliği | **≤ 2 seviye** | Checkstyle `NestedIfDepth` (max=1) |
| **CPLX-06** | İç içe döngü derinliği | **≤ 2 seviye** | Checkstyle `NestedForDepth` (max=1) |
| **CPLX-07** | İç içe `try` | **yasak** (1 seviye) | Checkstyle `NestedTryDepth` (max=0) |
| **CPLX-08** | Metot başına `return` sayısı (lambda hariç); `void` metotlarda erken `return` | **≤ 3**; `void` için ≤ 1 | Checkstyle `ReturnCount` |
| **CPLX-09** | Sınıfın kullandığı farklı sınıf sayısı (fan-out; `java.*` hariç) | **≤ 20** | Checkstyle `ClassFanOutComplexity` |
| **CPLX-10** | Sınıf içinde `new` ile örneklenen farklı sınıf sayısı (veri soyutlama bağlılığı) | **≤ 7** | Checkstyle `ClassDataAbstractionCoupling` |
| **CPLX-11** | Bir metodun `throws` bildirimindeki istisna sayısı | **≤ 1** | Checkstyle `ThrowsCount` |

- **CPLX-12** Derinliği azaltmak için **erken çıkış (guard clause)** kullanılır: önce geçersiz durumlar elenir, ana akış girintisiz yazılır. `else` bloğu, `if` bloğu `return`/`throw` ile bitiyorsa yazılmaz.
- **CPLX-13** Zincirleme `if-else if` ile tip veya enum ayrımı yapılmaz; `switch` ifadesi kullanılır (LANG-05).

```java
// YANLIŞ: 3 seviye iç içe, else zinciri
public void submit(DeliveryReceipt receipt) {
    if (receipt != null) {
        if (!shipment.isLocked()) {
            if (shipment.status() == AWAITING_DELIVERY) {
                // ...
            } else {
                throw new BusinessRuleViolationException(...);
            }
        }
    }
}

// DOĞRU: guard clause, tek seviye
public void submitDeliveryReceipt(DeliveryReceipt receipt) {
    requireNotLocked();
    requireStatus(ShipmentStatus.AWAITING_DELIVERY);
    this.deliveryReceipt = receipt;
    transitionTo(ShipmentStatus.CHECKING);
}
```

### 4.3 Yapı ve biçim kuralları

| Kural | Ne | Denetleyen |
| --- | --- | --- |
| **STRUC-01** | Sınıf içi sıralama: (1) `static final` sabitler, (2) diğer statik alanlar, (3) örnek alanları, (4) yapıcılar, (5) statik fabrika metotları, (6) `public` metotlar, (7) paket-özel/`protected` metotlar, (8) `private` metotlar, (9) iç içe tipler. Aşırı yüklenmiş (overload) metotlar yan yana durur. | Checkstyle `DeclarationOrder`, `OverloadMethodsDeclarationOrder` |
| **STRUC-02** | Kalıtım için tasarlanmamış her somut sınıf `final`'dır. İstisnalar: JPA entity'leri, `@Configuration` sınıfları ve `@Transactional` taşıyan application servisleri (Spring proxy'si alt sınıf üretir). | ArchUnit |
| **STRUC-03** | Her satırda tek ifade, her bildirimde tek değişken. | Checkstyle `OneStatementPerLine`, `MultipleVariableDeclarations` |
| **STRUC-04** | `if`, `else`, `for`, `while`, `do` blokları tek satır olsa da süslü parantezlidir. | Checkstyle `NeedBraces` |
| **STRUC-05** | Boş blok yasaktır (`catch` dahil). | Checkstyle `EmptyBlock`, `EmptyCatchBlock` |
| **STRUC-06** | Metot parametresine yeniden değer atanmaz; ifade içinde atama yapılmaz (`if ((x = ...) != null)` yasak). | Checkstyle `ParameterAssignment`, `InnerAssignment` |
| **STRUC-07** | Sihirli sayı yasaktır; `-1, 0, 1, 2, 100` dışındaki sayısal sabitler adlandırılmış `static final` alan olur. Testler ve `hashCode` hariçtir. | Checkstyle `MagicNumber` |
| **STRUC-08** | `public` metotlarda `boolean` parametre yasaktır ("bayrak argümanı"); iki ayrı metot veya enum kullanılır (`lock(force=true)` değil `forceLock()`). Record bileşenleri bu kuralın dışındadır. | ArchUnit |
| **STRUC-09** | `equals` ezen sınıf `hashCode`'u da ezer. Dize karşılaştırması `==` ile yapılmaz. | Checkstyle `EqualsHashCode`, `StringLiteralEquality` |
| **STRUC-10** | Yerel değişken ve parametre aynı addaki alanı gölgelemez (yapıcı parametreleri hariç). | Checkstyle `HiddenField` |
| **STRUC-11** | `catch (Exception)`, `catch (Throwable)`, `catch (RuntimeException)` yalnızca ERR-04'te sayılan dış sınır sınıflarında; `throws Exception` hiçbir yerde. | Checkstyle `IllegalCatch`, `IllegalThrows` (+ suppressions dosyası) |
| **STRUC-12** | Basitleştirilebilir boolean ifade ve dönüş yasaktır (`if (x) return true; else return false;`). | Checkstyle `SimplifyBooleanExpression`, `SimplifyBooleanReturn` |
| **STRUC-13** | Kullanılmayan import, değişken, parametre (arayüz uygulaması hariç) ve `private` metot bırakılmaz. | Checkstyle `UnusedImports`, `UnusedLocalVariable`; PMD `UnusedPrivateMethod`, `UnusedFormalParameter` |
| **STRUC-14** | Joker import yasak; statik import yalnızca izinli listede (AssertJ, Mockito, ArchUnit, `Objects.requireNonNull`). Import sırası Spotless'a aittir. | Checkstyle `AvoidStarImport`, `AvoidStaticImport` |

### 4.4 Yorum kuralları

- **COMM-01** Yorum **neden**i anlatır, **ne**yi değil. Kodu tekrar eden yorum (`// sayacı artır`) yazılmaz.
- **COMM-02** Javadoc zorunlu olan yerler: `port.in` arayüzleri, `libs/event-contracts` içindeki tüm public tipler, `libs/common-*` public API'si ve her aggregate kök sınıfı. Diğer yerlerde isteğe bağlıdır. (Checkstyle `MissingJavadocType`, `MissingJavadocMethod`, kapsam suppressions dosyasıyla daraltılır.)
- **COMM-03** Yorum satırına alınmış kod yasaktır; silinen kod git geçmişindedir. (Checkstyle `RegexpSinglelineJava`: `//` ile başlayıp `;`, `{` veya `}` ile biten satır.)
- **COMM-04** `TODO` yalnızca issue numarasıyla yazılır: `// TODO(#42): kantar entegrasyonunda kaldırılacak`. `FIXME`, `XXX` ve numarasız `TODO` yasaktır. (Checkstyle `TodoComment`.)
- **COMM-05** `@author`, `@version`, `@since` etiketleri ve dosya başı tarih/kişi blokları yazılmaz; bu bilgi git'tedir. (Checkstyle `RegexpSinglelineJava`.)

### 4.5 İstisna ve log mesajı biçimi

- **MSG-01** İstisna mesajı İngilizce, büyük harfle başlayan, noktasız tek cümledir; değişken değerleri `anahtar=değer` biçiminde sona eklenir: `"Shipment is locked shipmentId=0192..."`. Kişisel veri içermez (SEC-12).
- **MSG-02** Log mesajı aynı biçimdedir, geçmiş zaman veya durum bildirir ve parametrelidir: `log.info("Shipment locked shipmentId={} lockedBy={}", id, actorId)`. Mesaj metni sabittir; değişken değer metnin ortasına gömülmez ki log aramasında tek kalıpla bulunabilsin.
- **MSG-03** Bir metotta en fazla bir `try` bloğu bulunur; birden fazla hata kaynağı varsa her biri ayrı metoda alınır (CPLX-07 ile birlikte; review'da denetlenir).
- **MSG-04** MSG-01 ve MSG-02 biçimi review'da denetlenir; log mesajında dize birleştirme Error Prone ve Checkstyle `RegexpSinglelineJava` (`log\.\w+\("[^"]*"\s*\+`) ile derlemede yakalanır.

---

## 5. Repo, modül ve paket yapısı

### 5.1 Repo yapısı

Mimari dokümandaki çok modüllü Maven yapısı esas alınır:

```
yarn-shipment-system/
  pom.xml                     # kök: parent + BOM, sürümler tek yerde
  build-tools/                # Spotless, Checkstyle, Error Prone ayar dosyaları
  platform/
    api-gateway/
  services/
    master-data-service/
    bag-service/
    shipment-service/
    evidence-service/
    discrepancy-service/
    audit-service/
    notification-service/
    reporting-service/
  libs/
    event-contracts/          # olay record'ları, CloudEvents zarfı, şema sürümleri
    common-web/               # ProblemDetail, request id, sayfalama, Idempotency-Key
    common-security/          # JWT, rol, fasoncu/şoför kapsam kontrolü
    common-outbox/            # outbox yayıncısı, idempotent tüketici
    common-test/              # Testcontainers tabanlı test destekleri, ArchUnit kuralları
  deploy/
    docker-compose.yml
    helm/
  docs/
    standartlar/              # bu doküman
    adr/                      # mimari karar kayıtları
  tools/                      # tek dosyalık yardımcı betikler
```

- **STR-01** Servis modülleri birbirine Maven bağımlılığı **eklemez**. Bir servis başka servisi yalnızca REST sözleşmesi (OpenAPI'den üretilen istemci) veya `event-contracts` üzerinden tanır.
- **STR-02** `libs/*` modülleri iş kuralı içermez. "İki serviste aynı iş kodu var, lib'e taşıyalım" gerekçesi kabul edilmez; iş kuralı tek servise aittir.
- **STR-03** `common-*` kütüphaneleri `services/*` modüllerine bağımlı olamaz (döngü yasağı, Maven Enforcer ile).

### 5.2 Kök paket ve servis içi paket yapısı

- **STR-04** Kök paket `com.yarnshipment`'tir. Her servis kendi alt paketindedir: `com.yarnshipment.shipment`, `com.yarnshipment.bag`, `com.yarnshipment.masterdata`, `com.yarnshipment.evidence`, `com.yarnshipment.discrepancy`, `com.yarnshipment.audit`, `com.yarnshipment.notification`, `com.yarnshipment.reporting`, `com.yarnshipment.gateway`. Ortak kütüphaneler `com.yarnshipment.common.<ad>` ve `com.yarnshipment.events`.
- **STR-05** Servis içi yapı mimari dokümandaki altıgen düzenin birebir karşılığıdır:

```
com.yarnshipment.shipment
  ShipmentServiceApplication.java
  domain/
    model/          # aggregate, entity, value object, enum, domain olayları
    service/        # birden fazla aggregate'e yayılan saf domain mantığı (gerekirse)
    exception/      # sealed DomainException hiyerarşisi
  application/
    port/in/        # UseCase arayüzleri, Command/Query record'ları
    port/out/       # Repository ve dış servis portları
    service/        # UseCase uygulamaları (@Transactional burada)
  adapter/
    in/web/         # controller, web DTO, DTO eşleyici
    in/messaging/   # Kafka dinleyicileri
    out/persistence/# JPA entity, Spring Data repository, port uygulaması
    out/messaging/  # outbox'a yazan olay yayıncısı
    out/client/     # diğer servislerin HTTP istemcileri
  config/           # Spring @Configuration sınıfları
```

- **STR-06** Her pakette `package-info.java` bulunur ve en az `@NullMarked` taşır (bkz. NULL-01).
- **STR-07** Bir sınıf `public` olmak zorunda değilse paket-özel bırakılır. Adapter ve config sınıflarının çoğu paket-özel olabilir.

---

## 6. Altıgen mimari katman kuralları

Bağımlılık yönü dışarıdan içeriye doğrudur: `adapter → application → domain`. Bu kurallar `common-test` içindeki ortak ArchUnit test seti ile **her servisin derlemesinde** çalışır.

- **ARCH-01** `domain` paketi yalnızca JDK'ya, `org.jspecify` anotasyonlarına ve aynı servisin `domain` paketine bağımlı olabilir. Spring, Jakarta (JPA, Validation), Jackson, Hibernate, Kafka, SLF4J dahil hiçbir framework import edilemez.
- **ARCH-02** `application` paketi `domain`'e ve kendi `port` paketlerine bağımlıdır. Spring'den yalnızca `@Service` benzeri stereotype anotasyonlar ve `org.springframework.transaction.annotation.Transactional` kullanılabilir. `adapter` paketine bağımlılık yasaktır.
- **ARCH-03** Adapter'lar birbirine bağımlı olamaz: `adapter.in.web` sınıfları `adapter.out.persistence`'ı göremez. İletişim her zaman bir port üzerinden geçer.
- **ARCH-04** Controller'lar yalnızca `port.in` arayüzlerini çağırır; repository veya domain servisini doğrudan çağırmaz.
- **ARCH-05** JPA entity'leri (`*JpaEntity`) yalnızca `adapter.out.persistence` içinde yaşar ve bu paketin dışına sızmaz. Domain modeli ile kalıcılık modeli ayrıdır; aralarındaki dönüşüm paket içindeki bir eşleyiciyle yapılır.
- **ARCH-06** Web DTO'ları yalnızca `adapter.in.web` içinde yaşar. Bir DTO hem istek hem yanıt olarak paylaşılmaz; domain nesnesi asla doğrudan JSON'a serileştirilmez (kör giriş güvencesinin teknik temeli, bkz. SEC-05).
- **ARCH-07** `KafkaTemplate` yalnızca `common-outbox` yayıncısında kullanılır. Servis kodu olayı her zaman outbox'a yazar (bkz. EVT-03).
- **ARCH-08** Döngüsel paket bağımlılığı yasaktır (ArchUnit `slices().should().beFreeOfCycles()`).

```java
// common-test içindeki ortak kural setinden kesit
public final class HexagonalArchitectureRules {

    public static final ArchRule DOMAIN_IS_FRAMEWORK_FREE = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta..", "org.hibernate..",
                    "tools.jackson..", "com.fasterxml..", "org.apache.kafka..", "org.slf4j..")
            .because("ARCH-01: domain katmanı framework bağımsızdır");

    private HexagonalArchitectureRules() {}
}
```

---

## 7. Domain modelleme

### 7.1 Aggregate ve kimlik

- **DOM-01** Her aggregate kökü kendi tutarlılığından sorumludur. Durum değişikliği yalnızca aggregate'in iş metotlarıyla yapılır (`shipment.recordDispatchWeighing(...)`). Public setter yasaktır.
- **DOM-02** Kimlikler tipli record'dur: `ShipmentId`, `BagId`, `SubcontractorId`. Metot imzalarında çıplak `UUID` veya `String` kimlik dolaşmaz. Bu sayede `assign(BagId, ShipmentId)` çağrısında argümanların yeri karışamaz.
- **DOM-03** Kimlikler UUIDv7'dir (veritabanı tasarımı). Domain kimliği kendisi üretir; üretim bir `IdGenerator` portu arkasındadır, testlerde deterministik uygulama kullanılır. Veritabanına kimlik ürettirilmez.
- **DOM-04** Başka servise ait kayıtlar yalnızca tipli kimlikle ve gerekirse anlık görüntü (snapshot) değerleriyle tutulur (`SubcontractorId` + `subcontractorTitle`). Başka servisin modeline ait sınıf kopyalanmaz.

```java
public record ShipmentId(UUID value) {
    public ShipmentId {
        Objects.requireNonNull(value, "value");
    }
}
```

### 7.2 Değer nesneleri: kilo, yüzde, zaman

- **DOM-05 Kilo `double` veya `float` ile tutulmaz.** Ağırlıklar `Kilogram` değer nesnesidir: `BigDecimal`, ölçek 3 (veritabanı `numeric(12,3)`). Ham `BigDecimal` metot imzalarında dolaşmaz.
- **DOM-06** `BigDecimal` karşılaştırması `compareTo` ile yapılır; `equals` ölçeği de karşılaştırdığı için (`1.0` ≠ `1.00`) iş mantığında kullanılmaz. `new BigDecimal(double)` yasaktır; `new BigDecimal("12.5")` veya `BigDecimal.valueOf(long, scale)` kullanılır.
- **DOM-07** Her bölme işlemi açık ölçek ve `RoundingMode` ile yapılır. Fark yüzdesi ölçek 2, `RoundingMode.HALF_UP` ile hesaplanır (rapor 9.1). Yuvarlama kuralı değişirse yalnızca `Percentage` sınıfı değişir.
- **DOM-08** Tolerans bandı sınırları kod içine sabit yazılmaz; `ToleranceRule` olarak master-data'dan gelir (iş ayarları konfigürasyon değildir, mimari doküman). Kodda yalnızca sınıflandırma algoritması bulunur.

```java
/** Net, brüt veya dara ağırlığı. Ölçek her zaman 3 (gram hassasiyeti). */
public record Kilogram(BigDecimal value) implements Comparable<Kilogram> {

    public static final int SCALE = 3;
    public static final Kilogram ZERO = new Kilogram(BigDecimal.ZERO);

    public Kilogram {
        Objects.requireNonNull(value, "value");
        if (value.signum() < 0) {
            throw new IllegalArgumentException("Weight cannot be negative: " + value);
        }
        // Gramdan küçük hassasiyet gelirse sessizce yuvarlamak yerine hata ver.
        value = value.setScale(SCALE, RoundingMode.UNNECESSARY);
    }

    public static Kilogram of(String value) {
        return new Kilogram(new BigDecimal(value));
    }

    public Kilogram minus(Kilogram other) {
        return new Kilogram(value.subtract(other.value));
    }

    @Override
    public int compareTo(Kilogram other) {
        return value.compareTo(other.value);
    }
}

/** Rapor 9.1: brüt, dara ve net ayrı saklanır; net = brüt − dara. */
public record Weighing(WeighingSide side, Kilogram gross, Kilogram tare, int bagCount, Instant weighedAt) {

    public Weighing {
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(gross, "gross");
        Objects.requireNonNull(tare, "tare");
        Objects.requireNonNull(weighedAt, "weighedAt");
        if (tare.compareTo(gross) > 0) {
            throw new IllegalArgumentException("Tare cannot exceed gross weight");
        }
        if (bagCount <= 0) {
            throw new IllegalArgumentException("Bag count must be positive");
        }
    }

    public Kilogram net() {
        return gross.minus(tare);
    }
}

/**
 * Rapor 9.1: (çıkış net − teslim net) / çıkış net × 100. Teslim fazlası da uyuşmazlıktır,
 * bu yüzden bant sınıflandırması mutlak değer üzerinden yapılır.
 */
public static Percentage calculateDifferencePercentage(Kilogram dispatchNet, Kilogram deliveryNet) {
    if (dispatchNet.compareTo(Kilogram.ZERO) == 0) {
        throw new IllegalArgumentException("Dispatch net weight cannot be zero");
    }
    BigDecimal pct = dispatchNet.value()
            .subtract(deliveryNet.value())
            .multiply(BigDecimal.valueOf(100))
            .divide(dispatchNet.value(), Percentage.SCALE, RoundingMode.HALF_UP);
    return new Percentage(pct);
}
```

- **DOM-09 Zaman.** Anlık zaman `Instant` ile tutulur ve UTC'dir. `LocalDateTime` kalıcı veride ve API'de kullanılmaz. `Europe/Istanbul` dönüşümü yalnızca sunum (frontend, PDF etiket) katmanında ve "iş günü" hesabında yapılır.
- **DOM-10** `Instant.now()`, `LocalDate.now()`, `System.currentTimeMillis()` doğrudan çağrılmaz; zaman enjekte edilen `java.time.Clock`'tan alınır. Testler sabit `Clock` kullanır. (ArchUnit ile denetlenir.)
- **DOM-11** İstemcinin çekim zamanı (`capturedAt`) ile sunucunun alış zamanı (`receivedAt`/`uploadedAt`) ayrı alanlardır ve asla birbirinin yerine kullanılmaz. Karar veren kurallar (zaman anomalisi, itiraz süresi) yalnızca sunucu zamanına dayanır.
- **DOM-12** "3 iş günü" gibi takvim kuralları tek bir `BusinessCalendar` portu üzerinden hesaplanır; kod içinde `plusDays(3)` yazılmaz.

### 7.3 Durum makineleri

- **DOM-13** Sevkiyat, uyuşmazlık, kanıt ve çuval durumları `enum` olarak modellenir; izin verilen geçişler enum'un kendisinde, `default` dalı olmayan bir `switch` ifadesiyle tanımlanır. Durum alanı yalnızca bu tabloyu kullanan aggregate metodu ile değişir.
- **DOM-14** Her durum geçişi bir domain olayı üretir ve `shipment_status_history` tablosuna yazılır. Olay üretmeyen durum değişikliği hatadır.

```java
/** Rapor 7.2 ve veritabanı CHECK kısıtıyla birebir aynı değerler. */
public enum ShipmentStatus {
    DRAFT, LOADED, IN_TRANSIT, AWAITING_DELIVERY, CHECKING, IN_REVIEW, COMPLETED, CANCELLED;

    public boolean canTransitionTo(ShipmentStatus target) {
        return switch (this) {
            case DRAFT -> target == LOADED || target == CANCELLED;
            case LOADED -> target == IN_TRANSIT || target == CANCELLED; // iptal yalnızca çıkıştan önce
            case IN_TRANSIT -> target == AWAITING_DELIVERY;
            case AWAITING_DELIVERY -> target == CHECKING;
            case CHECKING -> target == COMPLETED || target == IN_REVIEW;
            case IN_REVIEW -> target == COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
    }

    public boolean isLocked() {
        return this == COMPLETED || this == CANCELLED;
    }
}
```

### 7.4 Değiştirilemezlik ve düzeltme kaydı

- **DOM-15** Kilitli (`COMPLETED`) bir aggregate üzerinde değişiklik metodu çağrıldığında `ShipmentLockedException` fırlatılır (REST'te 409). Bu kontrol aggregate içindedir, controller'da değil.
- **DOM-16** Kritik ölçüm kayıtları (tartım, mühür, şoför beyanı, teslim girişi) güncellenmez. Düzeltme `CorrectionRecord` ile yapılır: yeni değer yeni kayıt olarak eklenir, eski kayıt `supersededBy` ile işaretlenir, gerekçe zorunludur ve yalnızca Admin onaylar (rapor karar #9).
- **DOM-17** Kanıt dosyaları silinmez; `invalidate(reason)` ile `INVALIDATED` olur. Kodda `delete` adlı iş metodu bulunmaz.

### 7.5 Domain olayları

- **DOM-18** Aggregate, ürettiği domain olaylarını kendi içinde biriktirir (`List<DomainEvent>`), application servisi kaydettikten sonra `pullDomainEvents()` ile alır ve outbox'a yazar. Domain katmanı Kafka'yı veya outbox'ı bilmez.
- **DOM-19** Domain olayları ile entegrasyon olayları (`event-contracts`) ayrı tiplerdir. Dönüşüm `adapter.out.messaging` içinde yapılır; böylece iç model değişikliği dış sözleşmeyi kırmaz.

---

## 8. Null güvenliği, hata ve istisna yönetimi

### 8.1 Null

- **NULL-01** Tüm paketler `@org.jspecify.annotations.NullMarked` ile işaretlenir; varsayılan "null olamaz"dır. Null alabilecek her parametre, dönüş ve alan `@Nullable` ile açıkça işaretlenir. Spring Framework 7 de JSpecify kullandığı için framework sınırında da tutarlıdır.
- **NULL-02** NullAway (Error Prone eklentisi) derlemede **hata** seviyesinde çalışır. Null uyarısı ile birleştirme yapılmaz.
- **NULL-03** `Optional` yalnızca "bulunamayabilir" anlamındaki dönüş tiplerinde kullanılır (`Optional<Shipment> findById`). Alan, parametre ve koleksiyon elemanı olarak kullanılmaz. `Optional.get()` yasaktır; `orElseThrow()` kullanılır.
- **NULL-04** Koleksiyon dönen metotlar `null` değil boş koleksiyon döner.
- **NULL-05** Public yapıcı ve fabrika metotları zorunlu argümanları `Objects.requireNonNull(x, "x")` ile doğrular (record kompakt yapıcısında).

### 8.2 İstisna hiyerarşisi

- **ERR-01** İş kuralı ihlalleri domain istisnasıdır ve tek bir `sealed` hiyerarşiden türer. Her istisna bir `ErrorCode` taşır; REST eşlemesi bu koda göre yapılır.

```java
public abstract sealed class DomainException extends RuntimeException
        permits BusinessRuleViolationException, ResourceNotFoundException,
                ShipmentLockedException, ConcurrentModificationException,
                BlindEntryViolationException {

    private final ErrorCode code;

    protected DomainException(ErrorCode code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
    }

    public ErrorCode code() {
        return code;
    }
}
```

- **ERR-02** Kontrollü (checked) istisna yeni API'lerde tanımlanmaz. Altyapıdan gelen kontrollü istisnalar adapter'da yakalanır ve anlamlı bir runtime istisnasına sarılır; orijinal istisna `cause` olarak korunur.
- **ERR-03** İstisna yutulmaz. `catch` bloğu ya yeniden fırlatır, ya sarar, ya da bilinçli bir kurtarma yapıp gerekçesini yorumla yazar. Boş `catch` yasaktır.
- **ERR-04** `catch (Exception e)` ve `catch (Throwable t)` yalnızca en dış sınırlarda (global hata işleyici, Kafka hata işleyici, zamanlanmış görev kökü) kullanılabilir.
- **ERR-05** İstisna akış kontrolü için kullanılmaz. Beklenen alternatif sonuçlar (ör. "çuval başka sevkiyatta") `sealed` sonuç tipiyle döner; istisna beklenmeyen veya kural ihlali durumları içindir.

```java
/** bag-service yanıtı: depo personeli okuttuğu anda sonucu görmeli (mimari doküman, senkron çağrı). */
public sealed interface BagReservationResult {
    record Reserved(BagId bagId, LotNo lotNo, QualityGradeCode qualityGrade) implements BagReservationResult {}
    record AlreadyAssigned(BagId bagId, ShipmentId otherShipmentId) implements BagReservationResult {}
    record UnknownKey(UniqueKey uniqueKey) implements BagReservationResult {}
    record Voided(BagId bagId) implements BagReservationResult {}
}
```

### 8.3 Hata yanıtı

- **ERR-06** Tüm REST hataları RFC 9457 `ProblemDetail` formatındadır ve `common-web` içindeki tek `@RestControllerAdvice` tarafından üretilir. Servisler kendi hata gövdesini icat etmez.
- **ERR-07** `ProblemDetail` alanları: `type` (`https://errors.yarnshipment.com/<error-code>`; adres sabittir, erişilebilir olması gerekmez), `title`, `status`, `detail`, `instance`, ek olarak `code` (makine tarafından okunur, ör. `SHIPMENT_LOCKED`), `requestId`, doğrulama hatalarında `errors[]`.
- **ERR-08** 5xx yanıtlarında yığın izi, SQL, sınıf adı veya iç sistem bilgisi istemciye dönmez.

| Durum | HTTP |
| --- | --- |
| Bean Validation / istek biçimi hatası | 400 |
| Kimlik doğrulama yok / geçersiz token | 401 |
| Rol veya kapsam yetkisi yok (ör. başka fasoncunun sevkiyatı) | 403 |
| Kayıt bulunamadı | 404 |
| Kilitli kayıt, geçersiz durum geçişi, iyimser kilit çakışması | 409 |
| Aynı `Idempotency-Key` ile farklı gövde | 422 |
| İş kuralı ihlali (ör. dara bruttan büyük) | 422 |
| Bağımlı servis zaman aşımı / devre açık | 503 |

---

## 9. Kalıcılık: JPA, Flyway, transaction

### 9.1 JPA

- **PERS-01** `spring.jpa.open-in-view=false` tüm servislerde zorunludur.
- **PERS-02** `spring.jpa.hibernate.ddl-auto=validate`. Şemayı yalnızca Flyway yönetir.
- **PERS-03** Tüm ilişkiler `FetchType.LAZY`'dir. Toplu okuma ihtiyacı `JOIN FETCH`, `@EntityGraph` veya projeksiyon sorgusuyla, açıkça çözülür. N+1 sorgu PR'da kabul edilmez; entegrasyon testlerinde sorgu sayısı kontrol edilir (ÖNERİLEN: `datasource-proxy` ile).
- **PERS-04** `CascadeType.REMOVE`, `CascadeType.ALL` ve `orphanRemoval = true` yasaktır (GEN-04). Aggregate içi çocuk koleksiyonları için `PERSIST` ve `MERGE` açıkça yazılır.
- **PERS-05** `@ManyToMany` kullanılmaz; ara tablo (`shipment_bag` gibi) kendi entity'si olarak modellenir çünkü ek kolonları (`assigned_at`, `removed_at`) vardır.
- **PERS-06** Enum'lar `@Enumerated(EnumType.STRING)` ile saklanır; `ORDINAL` yasaktır.
- **PERS-07** Her aggregate kök entity'sinde `@Version` alanı bulunur (iyimser kilit). `OptimisticLockException` REST'te 409'a eşlenir.
- **PERS-08** Spring Data repository arayüzleri `adapter.out.persistence` içinde paket-özeldir; dışarıya yalnızca port uygulaması açılır.
- **PERS-09** Türetilmiş sorgu adları (`findByStatusAndSubcontractorIdAndCreatedAtAfter...`) üç koşulu geçerse `@Query` ile JPQL yazılır.
- **PERS-10** Dize birleştirmeyle SQL/JPQL oluşturulmaz. Dinamik sorgu gerekiyorsa parametreli Criteria API veya Spring Data `Specification` kullanılır.
- **PERS-11** Uygulamadan fiziksel `DELETE` çalıştırılmaz; repository'lerde `delete*` metotları açılmaz (veritabanında da `<servis>_app` rolünün DELETE yetkisi yoktur).

### 9.2 Flyway

- **PERS-12** Migration dosyaları `src/main/resources/db/migration` altındadır ve `V<yyyyMMddHHmm>__<ingilizce_aciklama>.sql` biçiminde adlandırılır (ör. `V202610211030__create_shipment_tables.sql`). Zaman damgası, paralel çalışan geliştiricilerin sürüm çakışmasını önler.
- **PERS-13** `main`'e birleştirilmiş bir migration **asla değiştirilmez**; düzeltme yeni migration ile yapılır.
- **PERS-14** Geriye uyumsuz şema değişikliği (kolon silme, yeniden adlandırma) "genişlet ve daralt" ile en az iki sürümde yapılır: önce yeni kolon eklenir ve iki yere yazılır, sonraki sürümde eski kolon kaldırılır.
- **PERS-15** Bir servis başka servisin veritabanına migration yazmaz.
- **PERS-16** Kısıtlar (FK, CHECK, kısmi unique indeks, trigger) veritabanı tasarımındaki gibi veritabanında da tanımlanır. Kod içi doğrulama veritabanı kısıtının yerine geçmez; ikisi birlikte bulunur.

### 9.3 Transaction

- **PERS-17** `@Transactional` yalnızca `application.service` sınıflarının public metotlarında kullanılır. Controller, adapter ve domain'de kullanılmaz.
- **PERS-18** Okuma senaryoları `@Transactional(readOnly = true)` ile işaretlenir.
- **PERS-19 Transaction içinde ağ çağrısı yapılmaz.** Başka servise REST çağrısı veya nesne deposuna (MinIO) yükleme, veritabanı transaction'ı açılmadan önce ya da kapandıktan sonra yapılır. Kafka'ya doğrudan yazılmaz; outbox kullanılır.
- **PERS-20** Aggregate değişikliği ve outbox kaydı **aynı transaction**'dadır (EVT-03).
- **PERS-21** Aynı sınıf içinden `@Transactional` metot çağırmak proxy'yi atladığı için etkisizdir; bu durumda iş ayrı bir bean'e taşınır.

---

## 10. REST API kuralları

- **API-01 API-first.** Her servisin sözleşmesi `src/main/resources/openapi/<servis>-api.yaml` dosyasıdır. Controller arayüzleri bu dosyadan `openapi-generator` (`spring` üreteci, `interfaceOnly=true`, `useJakartaEe=true`) ile üretilir ve controller bunları uygular. Sözleşme kodla birlikte aynı PR'da değişir; frontend TypeScript istemcisi aynı dosyadan üretilir. Springdoc yalnızca çalışan servisin sözleşmesini Swagger UI'da göstermek için kullanılır.
- **API-02** Yollar çoğul isim, küçük harf, kebab-case ve sürümlüdür: `/api/v1/shipments/{shipmentId}/delivery-receipt`. Fiil yoldaki son segmentte yalnızca durum geçişi eylemlerinde kullanılır: `POST /api/v1/shipments/{id}/dispatch`, `POST /api/v1/shipments/{id}/lock`.
- **API-03** JSON alanları `camelCase`'dir. Tarih-saat ISO 8601 UTC (`2026-10-20T08:15:30Z`), tarih `yyyy-MM-dd`. Kilo değerleri JSON'da **dize** olarak taşınır (`"netKg": "1982.500"`); JavaScript sayılarının ondalık hassasiyet kaybı önlenir.
- **API-04** Durum değiştiren her uç (`POST`, `PUT`, `PATCH`) `Idempotency-Key` başlığı kabul eder ve `common-web` içindeki ortak mekanizmayla işler: aynı anahtar + aynı gövde ilk yanıtı tekrar döner; aynı anahtar + farklı gövde 422 döner. Zayıf bağlantıda tekrar gönderilen istek çift kayıt üretmez (rapor Bölüm 14).
- **API-05** Kilitli kayıtta güncelleme uçları 409 döner. Düzeltme yalnızca `/corrections` alt kaynağıyla ve zorunlu `reason` alanıyla yapılır.
- **API-06** Liste uçları sayfalıdır: `page` (0'dan başlar), `size` (varsayılan 20, en fazla 100), `sort`. Yanıt `{ "items": [...], "page": {...} }` biçimindedir. Sayfasız liste dönen uç yazılmaz.
- **API-07** Girdi doğrulaması request record'larında Jakarta Bean Validation ile yapılır (`@NotNull`, `@Size`, `@Pattern`), controller parametresinde `@Valid`. Bu yalnızca biçim doğrulamasıdır; iş kuralları domain'dedir.
- **API-08** Kullanıcıya gösterilecek `title` ve `detail` metinleri Türkçedir ve `messages_tr.properties` içinden gelir; kod içine kullanıcı metni gömülmez. Frontend kararları `code` alanına göre verir, metne göre değil.
- **API-09** Yanıt DTO'su hedef kitleye göre ayrıdır: fasoncuya, şoföre ve iç kullanıcılara dönen sevkiyat görünümleri **farklı record'lardır** (bkz. SEC-05). Ortak alanlar için kalıtım kullanılmaz; her görünüm kendi alanlarını açıkça listeler.
- **API-10** Controller metotları ince tutulur: DTO → Command dönüşümü, use case çağrısı, sonuç → DTO dönüşümü. Controller metodu en fazla 15 satırdır (SIZE-02'deki genel 30 satır sınırından daha sıkı; review'da denetlenir); aşıyorsa iş mantığı sızmıştır.
- **API-11** DTO eşleme `adapter.in.web` içinde MapStruct ile veya elle yazılan statik fabrika metoduyla (`ShipmentAdminResponse.from(shipment)`) yapılır. MapStruct'ta `unmappedTargetPolicy = ReportingPolicy.ERROR` zorunludur; yeni alan eklendiğinde unutulan eşleme derlemeyi kırar.
- **API-12** Servisler arası senkron çağrılar yalnızca mimari dokümanda listelenen üç çağrıdır. Yeni bir senkron çağrı eklemek ADR gerektirir.
- **API-13** Servis istemcileri Spring HTTP Interface (`@HttpExchange` arayüzleri, `RestClient` tabanlı) ile yazılır; `RestTemplate` ve `WebClient` kullanılmaz. Her istemcide 2 sn zaman aşımı, Resilience4j `CircuitBreaker` ve sınırlı `Retry` (en fazla 2 tekrar, yalnızca idempotent çağrılar) yapılandırılır.
- **API-14** Tekrar deneme tek mekanizmayla yapılır: Resilience4j. Spring Framework 7'nin `@Retryable`'ı ve Spring Retry kullanılmaz; iki katmanlı tekrar yük çarpanı oluşturur.

---

## 11. Olaylar, Kafka ve outbox

- **EVT-01** Tüm entegrasyon olayları `libs/event-contracts` içinde record olarak tanımlanır. Bir servis başka servisin olay sınıfını kendisi yeniden tanımlamaz.
- **EVT-02** Zarf CloudEvents'tir (mimari doküman). Alanlar:

| Alan | Kural | Örnek |
| --- | --- | --- |
| `id` | UUIDv7, olay başına benzersiz; idempotency anahtarı | `0192b5d0-...` |
| `source` | `/yarn-shipment/<servis-adı>` | `/yarn-shipment/shipment-service` |
| `type` | `com.yarnshipment.<alan>.<olay-kebab>.v<n>` | `com.yarnshipment.shipment.delivery-received.v1` |
| `subject` | Bölümleme kimliği (çoğunlukla `shipmentId`) | `0192b5c1-...` |
| `time` | Olayın gerçekleştiği sunucu zamanı (UTC) | `2026-10-20T08:15:30Z` |
| `datacontenttype` | `application/json` | |
| `data` | Olay record'u | |

- **EVT-03 Transactional outbox zorunludur.** Olay, iş verisiyle aynı transaction'da `outbox_event` tablosuna yazılır; Kafka'ya `common-outbox` yayıncısı iletir. Servis kodu Kafka'ya doğrudan yazmaz (ARCH-07).
- **EVT-04 Konu adları** `<alan>.events.v<n>` biçimindedir: `shipment.events.v1`, `evidence.events.v1`, `discrepancy.events.v1`. Hatalı olay konusu `<konu>.DLT`. Mesaj anahtarı bölümleme kimliğidir (sevkiyat olaylarında `shipmentId`); aynı sevkiyatın olayları sıralı işlenir.
- **EVT-05 İdempotent tüketici zorunludur.** Her tüketici olayın `id`'sini `processed_event` tablosuna işlemle **aynı transaction**'da yazar. Unique ihlali "zaten işlendi" anlamına gelir ve olay sessizce onaylanır.
- **EVT-06 Şema evrimi.** Bir olay sürümüne yalnızca **isteğe bağlı yeni alan** eklenebilir. Alan silme, yeniden adlandırma, tip veya anlam değişikliği yeni sürüm (`.v2`) gerektirir; eski sürüm tüm tüketiciler geçene kadar paralel yayınlanır. Tüketiciler bilinmeyen alanları yok sayar (`FAIL_ON_UNKNOWN_PROPERTIES=false` yalnızca olay okuyucusunda).
- **EVT-07** Olay yükü "ne oldu"yu taşır, alıcıya komut vermez. Adı geçmiş zamandır (Ek B). `ProcessShipment` gibi komut adlı olay yazılmaz.
- **EVT-08** Olay yükünde kişisel veri (TCKN, telefon, e-posta, konum) taşınmaz; kimlik taşınır, gerekiyorsa sahibi servisten sorgulanır.
- **EVT-09** Tüketici hata politikası: geçici hatalarda üstel bekleme ile en fazla 3 tekrar, sonra `.DLT`. Kalıcı hatalar (seri kaldırma, doğrulama) tekrar edilmeden doğrudan `.DLT`'ye gider. DLT'ye düşen her olay alarm üretir.
- **EVT-10** Kafka dinleyicileri `adapter.in.messaging` içindedir, olayı Command'a çevirip use case çağırır; iş mantığı içermez.
- **EVT-11** Olay sözleşmeleri Spring Cloud Contract ile test edilir: üretici servis, olayın sözleşmeye uyduğunu; tüketici servis, sözleşmedeki örneği işleyebildiğini kanıtlar.

```java
/** Fasoncu kör teslim girişini tamamladı. Çıkış verisi yalnızca discrepancy-service içindir (SEC-06). */
public record DeliveryReceived(
        UUID shipmentId,
        UUID deliveryReceiptId,
        Instant submittedAt,
        WeighingData delivery,
        WeighingData dispatch,
        List<String> expectedUniqueKeys,
        List<String> scannedUniqueKeys,
        SealData dispatchSeal,
        SealData deliverySeal) {

    public static final String TYPE = "com.yarnshipment.shipment.delivery-received.v1";

    public DeliveryReceived {
        expectedUniqueKeys = List.copyOf(expectedUniqueKeys);
        scannedUniqueKeys = List.copyOf(scannedUniqueKeys);
    }
}
```

---

## 12. Eşzamanlılık ve sanal thread'ler

- **CONC-01** Tüm servislerde `spring.threads.virtual.enabled=true`. Web istekleri, Kafka dinleyicileri ve `@Async`/zamanlanmış işler sanal thread'lerde çalışır. Kod bloklayan, düz, okunabilir G/Ç yazar.
- **CONC-02 Reaktif programlama kullanılmaz** (WebFlux, Reactor, RxJava). Tek istisna API Gateway'dir (karar D-3); reaktif kod yalnızca `platform/api-gateway` içinde kalır.
- **CONC-03** Sanal thread havuzlanmaz. `Executors.newFixedThreadPool` ile sanal thread sınırlandırılmaz; G/Ç işleri için `Executors.newVirtualThreadPerTaskExecutor()` kullanılır (try-with-resources ile kapatılır).
- **CONC-04** Sınırlı bir kaynağa (başka servis, veritabanı bağlantısı) eşzamanlılık sınırı gerekiyorsa thread havuzu değil `Semaphore` veya Resilience4j `Bulkhead` kullanılır.
- **CONC-05** Kendi `ThreadLocal`'ımızı tanımlamayız. Uygulama kodunda istek bağlamı (kullanıcı, kapsam) metot parametresiyle taşınır; bu mümkün değilse `ScopedValue` kullanılır. Framework'ün yönettiği `ThreadLocal`'lar (Spring Security `SecurityContextHolder`, MDC, transaction senkronizasyonu) bu kuralın dışındadır.
- **CONC-06** Paylaşılan değişebilir durum yazılmaz. Gerekirse `java.util.concurrent` atomik tipleri ve eşzamanlı koleksiyonları kullanılır. Kilit gerekiyorsa `ReentrantLock` tercih edilir (zaman aşımlı `tryLock` için); Java 24'ten itibaren `synchronized` sanal thread'i taşıyıcıya sabitlemediği için yasak değildir ama uzun G/Ç'yi kilit altında yapmak her iki durumda da yasaktır.
- **CONC-07** Dağıtık tutarlılık için JVM kilidi kullanılmaz. Aynı sevkiyatın eşzamanlı güncellenmesi `@Version` iyimser kilidiyle, Unique Key'in tek kullanımı veritabanı kısmi unique indeksiyle engellenir.
- **CONC-08** Structured Concurrency Java 25'te önizlemede olduğu için kullanılmaz (LANG-02). Paralel alt görev ihtiyacı `newVirtualThreadPerTaskExecutor()` + `Future` ile çözülür.
- **CONC-09** `CompletableFuture` zincirleri iş mantığında kullanılmaz; sanal thread'lerle bloklayan kod aynı ölçeği daha okunur sağlar.

---

## 13. Güvenlik, kör giriş ve KVKK

### 13.1 Kimlik ve yetki

- **SEC-01** Her servis OAuth2 Resource Server olarak JWT'yi kendisi de doğrular (sıfır güven). Gateway'den geldi diye istek güvenilir sayılmaz.
- **SEC-02** Rol kontrolü method seviyesinde `@PreAuthorize` ile application servisinde veya controller'da yapılır; URL kalıbı tabanlı yetkilendirme tek başına yeterli değildir. Varsayılan "reddet"tir: yetki tanımı olmayan uç erişilemez.
- **SEC-03 Kapsam kontrolü.** Fasoncu yalnızca kendi `subcontractorId`'sine, şoför yalnızca kendisine atanmış sevkiyatlara erişir. Bu kontrol, kaydı yükledikten sonra `common-security` içindeki `ScopeGuard` ile yapılır ve atlanması ArchUnit + test ile engellenir. Kapsam dışı erişim 404 değil 403 döner ve audit'e yazılır.
- **SEC-04** İşlemi yapan kullanıcı (`actor`) her zaman JWT'den alınır; istek gövdesinde `createdBy`, `userId` gibi alanlar kabul edilmez.

### 13.2 Kör giriş

Kör giriş projenin en önemli kontrolüdür (rapor Bölüm 3). Kod seviyesinde üç katmanla korunur:

- **SEC-05 Tip seviyesi.** Fasoncu ve şoför görünümleri (`SubcontractorShipmentView`, `DriverShipmentView`) çıkış kilosu ve çuval sayısı **alanlarını hiç içermez**. Alanı `@JsonIgnore` ile gizlemek veya `null` dönmek yasaktır; alan record'da yoksa sızamaz.
- **SEC-06 Olay seviyesi.** Çıkış verisini taşıyan olaylar (`DeliveryReceived`) yalnızca discrepancy-service ve audit-service tarafından tüketilir. Bu iki servis dışında bu alanları bir API yanıtına koyan kod yazılmaz; Kafka ACL'leri buna göre tanımlanır.
- **SEC-07 Test seviyesi.** Her kör giriş görünümü için sözleşme testi bulunur: fasoncu ve şoför rolüyle yapılan isteklerin yanıtında yasaklı alan adlarının (`dispatchNetKg`, `dispatchGrossKg`, `dispatchBagCount`, ...) bulunmadığı, karşılaştırma tamamlanana kadar doğrulanır. Bu test listesi `common-test` içindeki `BlindEntryFields` sabitinden gelir.
- **SEC-08** Rastgele kontrol seçimi (`randomCheckSelected`) de aynı şekilde sevkiyat yola çıkana kadar şoför ve fasoncu görünümlerinde bulunmaz (rapor 13.1).

### 13.3 Kanıt ve bütünlük

- **SEC-09** Dosya hash'i (SHA-256) istemcinin bildirdiği değer değil, sunucunun nesne deposundan okuyarak yeniden hesapladığı değerdir. Hash hesaplama `MessageDigest` ile akış (stream) üzerinden yapılır; dosya belleğe tümüyle alınmaz.
- **SEC-10** Pre-signed URL süreleri koda gömülmez, konfigürasyondan gelir (varsayılan 10 dk).
- **SEC-11** Güvenlik amaçlı rastgele değerler (`UniqueKey`, token) `SecureRandom` ile üretilir; `Random`/`Math.random()` yasaktır.

### 13.4 KVKK ve gizli bilgiler

- **SEC-12** Kişisel veri (TCKN, telefon, e-posta, ad-soyad, konum, IP) loglanmaz, istisna mesajına yazılmaz, olay yüküne konmaz. Gerekli yerlerde kimlik (`driverId`) kullanılır.
- **SEC-13** Kişisel veri taşıyan record'lar `toString()`'i ezer ve hassas alanları maskeler. Record'un otomatik `toString()`'i tüm alanları yazdığı için bu kural record'lar için özellikle önemlidir.

```java
public record DriverContact(DriverId driverId, String fullName, String phone) {
    @Override
    public String toString() {
        return "DriverContact[driverId=" + driverId + ", fullName=***, phone=***]";
    }
}
```

- **SEC-14** Şifre, anahtar, token ve bağlantı dizeleri repoya girmez (`application*.yml` dahil). Yerel geliştirmede `.env` (git'e girmez) ve Docker Compose ortam değişkenleri; Kubernetes'te Secret kullanılır. CI'da gizli bilgi taraması (gitleaks) çalışır.
- **SEC-15** Bağımlılık güvenlik açıkları CI'da taranır (OWASP Dependency-Check veya eşdeğeri); CVSS ≥ 7 açığı olan bağımlılıkla birleştirme yapılmaz.

---

## 14. Loglama ve gözlemlenebilirlik

- **LOG-01** Loglama yalnızca SLF4J üzerinden yapılır (`private static final Logger log = LoggerFactory.getLogger(X.class);`). `System.out`, `System.err`, `printStackTrace()` yasaktır.
- **LOG-02** Log çıktısı JSON'dur (Spring Boot yapılandırılmış loglama, `logging.structured.format.console=ecs`). Her satırda `traceId`, `spanId`, `requestId` bulunur; sevkiyat bağlamında `shipmentId` MDC'ye eklenir.
- **LOG-03** Log mesajları parametreli yazılır: `log.info("Shipment locked shipmentId={}", id)`. Dize birleştirme ile log yazılmaz.
- **LOG-04** Seviyeler:

| Seviye | Ne zaman |
| --- | --- |
| `ERROR` | İnsan müdahalesi gereken durum: DLT'ye düşen olay, audit zinciri bozulması, beklenmeyen 5xx. Alarm üretir. |
| `WARN` | Sistemin kendini toparladığı ama izlenmesi gereken durum: devre kesici açıldı, tekrar denendi, kapsam dışı erişim girişimi. |
| `INFO` | İş açısından anlamlı olay: durum geçişi, kilitleme, uyuşmazlık açılması. Her istek için INFO yazılmaz. |
| `DEBUG` | Geliştirici ayrıntısı; prod'da kapalı. |

- **LOG-05** Bir istisna yalnızca bir kez loglanır: yakalayıp yeniden fırlatan kod loglamaz, sonucu ele alan en dış katman loglar.
- **LOG-06** Audit log, uygulama logu değildir. "Kim neyi değiştirdi" bilgisi log satırına değil outbox üzerinden audit-service'e gider.
- **LOG-07** İş metrikleri Micrometer ile yayınlanır; metrik adı `yarnshipment.<alan>.<ölçüm>` biçimindedir (`yarnshipment.discrepancy.open`, `yarnshipment.outbox.lag`). Etiketlerde yüksek kardinaliteli değer (`shipmentId`, `userId`) kullanılmaz.
- **LOG-08** OpenTelemetry ile izleme açıktır; HTTP, JDBC ve Kafka izleri otomatik taşınır. Elle span gerekiyorsa Micrometer Observation API kullanılır.
- **LOG-09** Her servis Actuator `liveness` ve `readiness` uçlarını açar; readiness, veritabanı ve Kafka bağlantısını kapsar. Actuator uçları yalnızca iç ağdan erişilebilir.

---

## 15. Konfigürasyon

- **CONF-01** Konfigürasyon `@ConfigurationProperties` record'larıyla, `@Validated` ve Bean Validation anotasyonlarıyla okunur. Dağınık `@Value` kullanılmaz.
- **CONF-02** Özellik önekleri `yarnshipment.<servis>.<alan>` biçimindedir (`yarnshipment.evidence.upload.url-ttl=10m`). Süreler `Duration`, boyutlar `DataSize` tipindedir.
- **CONF-03** Profiller: `local` (Docker Compose), `test`, `prod`. Profil adına göre kod içinde `if` yazılmaz; fark konfigürasyonla veya `@Profile` ile yönetilir.
- **CONF-04** İş ayarları (tolerans bantları, zorunlu fotoğraf listesi, itiraz süresi, rastgele kontrol oranı) konfigürasyon dosyasında değil, sahibi servisin veritabanında, değişiklik geçmişi ve audit kaydıyla tutulur (mimari doküman).
- **CONF-05** Bağımlılık enjeksiyonu yalnızca yapıcı (constructor) ile yapılır; tek yapıcı varsa `@Autowired` yazılmaz. Alan ve setter enjeksiyonu yasaktır.

```java
@ConfigurationProperties("yarnshipment.evidence.upload")
@Validated
public record UploadProperties(
        @NotNull Duration urlTtl,
        @NotNull DataSize maxFileSize,
        @NotEmpty List<String> allowedMimeTypes) {}
```

---

## 16. Test kuralları

### 16.1 Test türleri

| Tür | Kapsam | Araç | Sınıf soneki | Ne zaman çalışır |
| --- | --- | --- | --- | --- |
| Birim | `domain`, `application` (portlar sahte) | JUnit Jupiter, AssertJ | `*Test` | Her derleme (`mvn test`) |
| Mimari | Paket bağımlılıkları, adlandırma | ArchUnit | `ArchitectureTest` | Her derleme |
| Entegrasyon | Adapter'lar gerçek altyapıyla | Testcontainers (PostgreSQL, Kafka, MinIO, Keycloak) | `*IT` | Her PR (`mvn verify`) |
| Sözleşme | REST ve olay sözleşmeleri | Spring Cloud Contract | `*ContractTest` | Her PR |
| Uçtan uca | Sevkiyat akışının tamamı | Docker Compose + test istemcisi | `*E2E` | Gece ve sürüm öncesi |

- **TEST-01** Domain katmanı Spring bağlamı olmadan test edilir. Domain testinde `@SpringBootTest` yasaktır.
- **TEST-02** Veritabanı testlerinde H2 veya gömülü veritabanı kullanılmaz; gerçek PostgreSQL 17 Testcontainers ile çalışır (`@ServiceConnection`). Kısmi unique indeks, trigger ve CHECK kısıtları ancak böyle doğrulanır.
- **TEST-03** Mock yalnızca port sınırında kullanılır. Domain nesneleri ve değer nesneleri mock'lanmaz. Mockito `@Mock` ile somut sınıf değil arayüz mock'lanır.
- **TEST-04** Zaman ve kimlik deterministiktir: `Clock.fixed(...)` ve sabit `IdGenerator`. `Thread.sleep` ile bekleme yazılmaz; asenkron doğrulama Awaitility ile yapılır.
- **TEST-05** Test adları davranışı anlatır, İngilizce `snake_case` veya `camelCase`, `should` ile başlar; Türkçe açıklama `@DisplayName` ile verilir:

```java
@Test
@DisplayName("Çuval başka bir sevkiyata bağlıysa rezervasyon reddedilir")
void shouldRejectBagAlreadyAssignedToAnotherShipment() {
    // given
    var bag = BagFixtures.assignedTo(OTHER_SHIPMENT_ID);

    // when
    var result = reservationPolicy.reserve(bag, SHIPMENT_ID);

    // then
    assertThat(result).isInstanceOf(BagReservationResult.AlreadyAssigned.class);
}
```

- **TEST-06** Test yapısı `given / when / then` yorumlarıyla ayrılır. Bir test tek davranışı doğrular.
- **TEST-07** Kurallar için zorunlu test kümeleri: her durum geçişi (izinli ve izinsiz), tolerans bandı sınırları (tam %0,5 ve %1,5 değerleri dahil), kör giriş alan yokluğu (SEC-07), kilitli kayıtta 409, Idempotency-Key tekrarı, olay idempotensi (aynı olay iki kez).
- **TEST-08** Kapsam eşikleri (JaCoCo, satır): `domain` ≥ %90, servis geneli ≥ %80. Eşik altı derleme kırılır. Domain katmanında PIT mutasyon testi ÖNERİLEN'dir.
- **TEST-09** Kararsız (flaky) test `@Disabled` ile susturulmaz; sorumlu kişiyle issue açılır ve aynı sprintte düzeltilir.
- **TEST-10** Test verisi "fixture" sınıflarıyla (`ShipmentFixtures`) üretilir; testler arası paylaşılan değişebilir durum yoktur.

---

## 17. Derleme, bağımlılık ve araç zinciri

- **BUILD-01** Maven Wrapper (`./mvnw`) kullanılır. Tüm sürümler kök `pom.xml` içindeki `dependencyManagement` ve Spring Boot BOM'undan gelir; alt modüllerde sürüm numarası yazılmaz.
- **BUILD-02** Maven Enforcer: Java sürümü `[25,)`, Maven sürümü alt sınırı, `dependencyConvergence`, `banDuplicatePomDependencyVersions`, yasaklı bağımlılık listesi.
- **BUILD-03 Yasaklı bağımlılıklar:** Lombok, H2/HSQLDB (test dahil), `spring-boot-starter-webflux` (gateway hariç), `commons-lang` 2.x, Guava (JDK karşılığı olan işler için gereksiz; gerekçeyle istisna alınabilir), JUnit 4, log4j 1.x.
- **BUILD-04** Derleyici: `-parameters -Xlint:all -Werror`. Error Prone + NullAway hata seviyesinde (NULL-02). Bilinçli bastırma `@SuppressWarnings("Kontrol")` ile en dar kapsamda ve yanında yorumla yapılır.
- **BUILD-05 Biçimlendirme otomatiktir.** Spotless + Palantir Java Format. `./mvnw spotless:apply` biçimi düzeltir, CI'da `spotless:check` biçimsiz kodu reddeder. Biçim tartışması review konusu değildir. Satır uzunluğu 120, girinti 4 boşluk, dosya kodlaması UTF-8, satır sonu LF.
- **BUILD-06** Checkstyle ve PMD, Ek E'deki ayarlarla **hata** seviyesinde çalışır; Bölüm 3 ve 4'teki adlandırma, boyut, karmaşıklık ve yapı kurallarını derlemede zorlar. Uyarı seviyesi ve baseline kullanılmaz.
- **BUILD-07** Container imajı Jib ile üretilir; temel imaj Eclipse Temurin 25 JRE'dir. JVM parametreleri: `-XX:MaxRAMPercentage=75 -XX:+UseCompactObjectHeaders -XX:+ExitOnOutOfMemoryError` ve NAME-10'daki yerel ayarlar. Çöp toplayıcı varsayılan G1'dir; değiştirmek ölçüm ve ADR gerektirir.
- **BUILD-08** CI her PR'da sırasıyla şunları çalıştırır: `spotless:check` → derleme (Error Prone, NullAway) → birim + ArchUnit → `verify` (Testcontainers, sözleşme) → JaCoCo eşikleri → bağımlılık ve gizli bilgi taraması → imaj. Herhangi bir adım kırmızıysa birleştirme yapılmaz.
- **BUILD-09** Yeni bağımlılık eklemek PR açıklamasında gerekçe ister: ne için, alternatifi neden yetmedi, lisansı (yalnızca Apache 2.0, MIT, BSD, EPL ve benzeri izin verici lisanslar).

---

## 18. Git, PR, code review ve istisna süreci

### 18.1 Git

- **GIT-01** Ana dal `main`'dir ve korumalıdır. Doğrudan push yapılmaz.
- **GIT-02** Kısa ömürlü dallar (en fazla birkaç gün): `feature/<kısa-ad>`, `fix/<kısa-ad>`, `docs/<kısa-ad>`, `chore/<kısa-ad>`.
- **GIT-03** Commit mesajları Conventional Commits biçimindedir; tip ve kapsam İngilizce, açıklama Türkçe olabilir: `feat(shipment): kör teslim girişi uç noktası eklendi`. Kapsam servis veya kütüphane adıdır.
- **GIT-04** PR'lar "squash merge" ile birleştirilir. PR başlığı da Conventional Commits biçimindedir.

### 18.2 PR ve review

- **GIT-05** Bir PR tek bir amaca hizmet eder ve ÖNERİLEN boyutu 400 değişen satırın altıdır (üretilmiş kod ve migration hariç).
- **GIT-06** En az bir onay gerekir. `domain`, `common-security`, `event-contracts` ve migration değişikliklerinde servis sahibinin onayı zorunludur.
- **GIT-07** Review'da kural ihlali kural kimliğiyle belirtilir: "`DOM-05`: kilo `double` olamaz." Böylece tartışma kişisel değil kurala dayalı olur.
- **GIT-08 Bitti tanımı (Definition of Done):**
  - CI tamamen yeşil.
  - Yeni davranış için test var (TEST-07 kümeleri dahil).
  - OpenAPI ve olay sözleşmesi değiştiyse aynı PR'da güncellendi.
  - Migration varsa geri uyumlu (PERS-14).
  - Kör giriş, kilit veya audit'e dokunan değişiklikte ilgili testler güncellendi.
  - Yeni iş terimi varsa Ek A güncellendi.

### 18.3 Bu dokümanın değiştirilmesi ve istisnalar

- **GIT-09** Bu doküman `docs/standartlar/` altında sürümlenir. Kural ekleme, değiştirme veya kaldırma PR ile yapılır ve proje sahibinin onayını gerektirir. Önemli kararlar `docs/adr/` altında ADR olarak kayda geçer.
- **GIT-10** ZORUNLU bir kuraldan tek seferlik istisna gerekiyorsa: kodda en dar kapsamda bastırma + `// İstisna: <kural-id>, <gerekçe>, <ADR veya issue bağlantısı>` yorumu yazılır ve PR açıklamasında belirtilir. Gerekçesiz istisna birleştirilmez.

---

## Ek A: Domain sözlüğü (Türkçe → kod)

| Türkçe (rapor) | Kod adı | Not |
| --- | --- | --- |
| Fasoncu | `Subcontractor` | |
| Şoför | `Driver` | |
| Araç | `Vehicle` | |
| İplik / ürün | `Product` | |
| Parti | `ProductionLot` | Kısa biçim yalnızca alan adında: `lotId`, `lotNo` |
| Kalite (A/B) | `QualityGrade` | Kod değeri `QualityGradeCode` |
| Kalite kilidi | `qualityLocked` / `lockQuality()` | |
| Şahit numune | `WitnessSample` | |
| Çuval | `Bag` | Ambalaj birimi genelleşirse `Package` olacak (veritabanı açık sorusu) |
| Unique Key | `UniqueKey` | |
| Etiket basımı | `LabelPrint` | |
| Sevkiyat | `Shipment` | |
| Sevkiyat satırı (plan) | `ShipmentLine` | |
| Tartım | `Weighing` | Taraf: `WeighingSide.DISPATCH` / `DELIVERY` |
| Brüt / Dara / Net | `gross` / `tare` / `net` | Değer nesnesi `Kilogram` |
| Standart dara | `TareStandard` | |
| Mühür | `Seal` | Durum: `SealCondition.INTACT/DAMAGED/MISSING` |
| Şoför beyanı | `DriverDeclaration` | Tür: `PICKUP`, `ARRIVAL` |
| Kör teslim girişi | `DeliveryReceipt` | Fiil: `submitDeliveryReceipt` |
| Kör giriş (ilke) | `BlindEntry` | |
| Kanıt (fotoğraf/belge) | `Evidence` / `EvidenceFile` | |
| İmzalı teslim belgesi | `SignedDeliveryDocument` | Kanıt tipi `SIGNED_DELIVERY_DOC` |
| Karşılaştırma | `Comparison` | |
| Uyuşmazlık | `Discrepancy` | |
| Tolerans bandı | `ToleranceBand` (`GREEN`, `YELLOW`, `RED`) | Kural: `ToleranceRule` |
| Kalite itirazı | `QualityClaim` | |
| Karar | `Resolution` | |
| Fiziki kontrol / numune / lab | `Inspection` | Tür: `PHYSICAL_COUNT`, `SAMPLE`, `LAB` |
| Rastgele kontrol | `RandomCheck` | |
| Risk skoru | `RiskScore` | |
| Düzeltme kaydı | `CorrectionRecord` | |
| Denetim izi | `AuditLog` / `AuditRecord` | |
| Bildirim | `Notification` | |
| Kilitleme | `lock()` / `lockedAt` | |
| İptal | `cancel()` / `CANCELLED` | |

Servis adları mimari dokümandaki gibidir: `master-data-service`, `bag-service`, `shipment-service`, `evidence-service`, `discrepancy-service`, `audit-service`, `notification-service`, `reporting-service`. (Veritabanı dokümanındaki `masterdata-service` yazımı bu adla hizalanmalıdır.)

## Ek B: Olay adları sözlüğü

Mimari dokümanda olaylar Türkçe adlarla anılmıştır. NAME-01 gereği kodda ve Kafka'da aşağıdaki İngilizce adlar kullanılır; dokümanlar arası eşleme bu tablodur.

| Mimari dokümandaki ad | Kod adı (record) | CloudEvents `type` | Yayınlayan |
| --- | --- | --- | --- |
| PartiKalitesiKilitlendi | `LotQualityLocked` | `com.yarnshipment.masterdata.lot-quality-locked.v1` | master-data |
| FasoncuGuncellendi | `SubcontractorUpdated` | `com.yarnshipment.masterdata.subcontractor-updated.v1` | master-data |
| SoforGuncellendi | `DriverUpdated` | `com.yarnshipment.masterdata.driver-updated.v1` | master-data |
| CuvalOlusturuldu | `BagCreated` | `com.yarnshipment.bag.bag-created.v1` | bag |
| CuvalSevkiyataBaglandi | `BagAssignedToShipment` | `com.yarnshipment.bag.bag-assigned-to-shipment.v1` | bag |
| CuvalRezervasyonuBirakildi | `BagReservationReleased` | `com.yarnshipment.bag.bag-reservation-released.v1` | bag |
| SevkiyatYuklendi | `ShipmentLoaded` | `com.yarnshipment.shipment.shipment-loaded.v1` | shipment |
| SevkiyatYolaCikti | `ShipmentDispatched` | `com.yarnshipment.shipment.shipment-dispatched.v1` | shipment |
| TeslimGirisiYapildi | `DeliveryReceived` | `com.yarnshipment.shipment.delivery-received.v1` | shipment |
| SevkiyatTamamlandi | `ShipmentCompleted` | `com.yarnshipment.shipment.shipment-completed.v1` | shipment |
| SevkiyatKilitlendi | `ShipmentLocked` | `com.yarnshipment.shipment.shipment-locked.v1` | shipment |
| SevkiyatIptalEdildi | `ShipmentCancelled` | `com.yarnshipment.shipment.shipment-cancelled.v1` | shipment |
| KanitYuklendi | `EvidenceUploaded` | `com.yarnshipment.evidence.evidence-uploaded.v1` | evidence |
| KanitGecersizSayildi | `EvidenceInvalidated` | `com.yarnshipment.evidence.evidence-invalidated.v1` | evidence |
| KanitTekrarKullanimiTespitEdildi | `EvidenceReuseDetected` | `com.yarnshipment.evidence.evidence-reuse-detected.v1` | evidence |
| KarsilastirmaTamamlandi | `ComparisonCompleted` | `com.yarnshipment.discrepancy.comparison-completed.v1` | discrepancy |
| UyusmazlikAcildi | `DiscrepancyOpened` | `com.yarnshipment.discrepancy.discrepancy-opened.v1` | discrepancy |
| UyusmazlikKarariVerildi | `DiscrepancyResolved` | `com.yarnshipment.discrepancy.discrepancy-resolved.v1` | discrepancy |
| BildirimGonderildi | `NotificationSent` | `com.yarnshipment.notification.notification-sent.v1` | notification |
| KontrolIcinSecildi (Faz 2) | `ShipmentSelectedForInspection` | `com.yarnshipment.reporting.shipment-selected-for-inspection.v1` | reporting |

## Ek C: Otomatik denetim özeti

| Kural(lar) | Nerede denetlenir |
| --- | --- |
| LANG-02 | Maven Enforcer: `--enable-preview` bayrağı yasak |
| LANG-11, LOG-01, BUILD-06, NAME-12…33, SIZE-01…13, CPLX-01…11, STRUC-01, 03…07, 09…14, COMM-02…05 | Checkstyle (Ek E.1) |
| SIZE-10, CPLX-02, STRUC-13 | PMD (Ek E.2) |
| NAME-13…17, 21, 22, 35, SIZE-12, STRUC-02, STRUC-08 | ArchUnit (Ek E.3) |
| LANG-13, BUILD-04 | `javac -Xlint:all -Werror` |
| NAME-09, ERR-03, DOM-06 | Error Prone (`StringCaseLocaleUsage`, `EmptyCatch`, `BigDecimalEquals`, `BigDecimalLiteralDouble`) |
| NULL-01, NULL-02 | NullAway + `package-info.java` varlık kontrolü (ArchUnit) |
| ARCH-01…08, PERS-08, PERS-17, DOM-10, CONF-05, ARCH-07 | ArchUnit (`common-test` ortak kural seti) |
| PERS-02, PERS-12, PERS-13 | Flyway `validate` + CI'da migration checksum kontrolü |
| API-11 | MapStruct `unmappedTargetPolicy = ERROR` |
| SEC-07, SEC-08 | Kör giriş sözleşme testleri |
| EVT-06, EVT-11 | Spring Cloud Contract |
| SEC-14, SEC-15 | gitleaks, bağımlılık açık taraması |
| BUILD-03 | Maven Enforcer `bannedDependencies` |
| BUILD-05 | Spotless `check` |
| TEST-08 | JaCoCo `check` |

## Ek D: Bu dokümanda alınan kararlar

Proje sahibi, başlangıçtaki açık konularda büyümeye uygun ve sonradan geliştirilebilir tercihlerin yapılmasını istedi. Aşağıdaki konular bu yüzden açık bırakılmadı, karara bağlandı. Her biri gerekirse ADR ile değiştirilebilir; değişikliğin etkisi sınırlı tutulacak şekilde seçildi.

| # | Konu | Karar | Gerekçe ve büyüme etkisi |
| --- | --- | --- | --- |
| D-1 | Kök paket adı | `com.yarnshipment` | Ürün adına bağlı, şirket yapısı değişse de sabit kalır. Kod yazılmadan önce değiştirmek bedavadır, sonra pahalıdır; bu yüzden şimdi sabitlendi. |
| D-2 | Mesaj kuyruğu | **Apache Kafka (KRaft)** | Olayların kalıcı ve tekrar oynatılabilir olması audit, raporlama ve Faz 2–3 entegrasyonlarının (ERP, e-İrsaliye, kantar, GPS) aynı akıştan beslenmesini sağlar. Yerelde ve pilotta tek düğüm, prod'da 3 düğüm. Outbox katmanı sayesinde servis kodu kuyruk teknolojisine bağlı değildir. |
| D-3 | API Gateway | **Spring Cloud Gateway (reaktif sürüm)**, yalnızca yapılandırma ve filtre kodu | En olgun varyanttır, Redis tabanlı hız sınırlayıcısı ve token relay hazırdır. Reaktif kod yalnızca `platform/api-gateway` içinde kalır, iş kodu içermez (CONC-02). |
| D-4 | Kimlik ve kapsam taşıma | Keycloak realm rolleri JWT'de `roles`, fasoncu ve şoför kapsamı `subcontractor_id` / `driver_id` claim'leriyle gelir; `common-security` bunları tipli `Actor` record'una çevirir | Kapsam kontrolü (SEC-03) tek yerde ve token'dan beslenir. Yetkilendirme çalışması yeni rol eklerse yalnızca `Actor` ve rol enum'u genişler. |
| D-5 | Çok lokasyon hazırlığı | Kod "tek fabrika / tek depo" varsayımını sabit değer olarak gömmez. Lokasyon kavramı `SiteId` ile modellenir; sevkiyat, çuval ve parti kendi `SiteId`'sini taşır. Faz 1'de tek kayıt bulunur. | Projenin büyümesi büyük olasılıkla yeni fabrika ve depolar demektir. Sonradan bu kolonu tüm tablolara eklemek ve geçmiş veriyi doldurmak pahalıdır; şimdi eklemek neredeyse bedavadır. Veritabanı tasarımının bu kararla hizalanması gerekir. |
| D-6 | API ve olay sürümleme | Yol sürümlü REST (`/api/v1`) ve sürümlü olay tipi (`.v1`) korunur; geriye uyumsuz değişiklikte yeni sürüm açılır, eski sürüm en az bir sürüm döngüsü boyunca paralel yaşar | Çok sayıda istemci (web, mobil tarayıcı, ileride ERP ve fasoncu entegrasyonları) bağımsız güncellendiğinde kırılma olmaz. |
| D-7 | Sürüm yönetimi | Tüm kütüphane sürümleri kök `pom.xml`'de sabittir; iskelet kurulurken o günün güncel kararlı sürümleri seçilir. Renovate (veya Dependabot) haftalık güncelleme PR'ı açar; Java ve Spring Boot ana sürüm geçişleri ADR ile yapılır. | Servis sayısı artınca sürüm sapması en büyük bakım maliyetidir; tek kaynak bunu önler. |
| D-8 | Yeni servis ekleme | Yeni servis yalnızca `services/` altındaki şablon servisten kopyalanarak açılır; şablon bu dokümanın tüm araç ayarlarını (Error Prone, ArchUnit, Spotless, JaCoCo, Jib) hazır içerir | Ekip ve servis sayısı büyüdükçe kuralların her yeni serviste kendiliğinden geçerli olmasını sağlar. |
| D-9 | Kod sahipliği | Repo kökünde `CODEOWNERS` bulunur; her servis ve her `libs/*` modülünün sahibi tanımlanır (GIT-06 bunu kullanır) | Ekip büyüdüğünde review sorumluluğu belirsiz kalmaz. |

### D-5 için kod kuralı

- **GEN-07** Lokasyona bağlı her aggregate (`Shipment`, `Bag`, `ProductionLot`, `TareStandard` ve ileride eklenecekler) bir `SiteId` taşır. Sorgular, listeler ve yetki kontrolleri `SiteId`'yi göz ardı edemez; "tek lokasyon var" varsayımıyla `findAll()` yazılmaz. Faz 1'de tek lokasyon kaydı bulunur ve kullanıcı arayüzünde seçim gösterilmeyebilir, ama kod bu alanı her zaman taşır.

## Ek E: Araç ayarları (derlemede zorlanan kurallar)

Bu ek, Bölüm 3 ve 4'teki kuralların araç karşılığıdır. İskelet kurulurken dosyalar `build-tools/` altına **bu içerikle** konur; doküman ile ayar dosyası arasında fark olursa doküman esastır ve ayar dosyası düzeltilir. Her kuralın şiddeti `error`'dır; Maven `checkstyle:check` ve `pmd:check` hedefleri `validate`/`verify` aşamasına bağlıdır ve ihlalde derleme durur (`failOnViolation=true`, `violationSeverity=warning` ile uyarı dahi hata sayılır).

Kullanılan araç sürümleri Java 25 sözdizimini (adsız değişken `_`, esnek yapıcı gövdeleri) ayrıştırabilen sürümler olmalıdır. Bir kontrol Java 25 sözdizimini henüz desteklemiyorsa, ilgili kural ArchUnit veya PMD karşılığıyla denetlenir ve bu ek güncellenir; kural kaldırılmaz.

### E.1 Checkstyle (`build-tools/checkstyle/checkstyle.xml`)

Üretim ve test kodu aynı dosyayla, farklı sınır değerleriyle denetlenir. Değerler Maven `checkstyle` eklentisinin iki ayrı çalıştırmasında `propertyExpansion` ile verilir:

| Özellik | Üretim | Test |
| --- | --- | --- |
| `fileLength` | 400 | 600 |
| `methodLength` | 30 | 40 |
| `executableStatements` | 20 | 30 |
| `methodCountTotal` | 25 | 40 |
| `methodCountPublic` | 15 | 40 |

```xml
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
        "https://checkstyle.org/dtds/configuration_1_3.dtd">
<module name="Checker">
    <property name="charset" value="UTF-8"/>
    <property name="severity" value="error"/>

    <module name="SuppressionFilter">
        <property name="file" value="${config_loc}/suppressions.xml"/>
    </module>
    <module name="SuppressWarningsFilter"/>

    <!-- SIZE-01, SIZE-06 -->
    <module name="FileLength">
        <property name="max" value="${fileLength}"/>
    </module>
    <module name="LineLength">
        <property name="max" value="120"/>
        <property name="ignorePattern" value="^(package|import) .*"/>
    </module>

    <!-- COMM-05: @author ve benzeri etiketler -->
    <module name="RegexpSingleline">
        <property name="format" value="@(author|version|since)\b"/>
        <property name="message" value="COMM-05: @author/@version/@since yazılmaz"/>
    </module>

    <module name="TreeWalker">
        <module name="SuppressWarningsHolder"/>

        <!-- 4.1 Boyut -->
        <module name="MethodLength">
            <property name="max" value="${methodLength}"/>
            <property name="countEmpty" value="false"/>
        </module>
        <module name="ExecutableStatementCount">
            <property name="max" value="${executableStatements}"/>
        </module>
        <module name="LambdaBodyLength">
            <property name="max" value="8"/>
        </module>
        <module name="AnonInnerLength">
            <property name="max" value="20"/>
        </module>
        <module name="ParameterNumber">
            <property name="max" value="4"/>
            <property name="ignoreOverriddenMethods" value="true"/>
            <property name="tokens" value="METHOD_DEF"/>
        </module>
        <module name="ParameterNumber">
            <property name="max" value="7"/>
            <property name="tokens" value="CTOR_DEF"/>
        </module>
        <module name="RecordComponentNumber">
            <property name="max" value="10"/>
        </module>
        <module name="MethodCount">
            <property name="maxTotal" value="${methodCountTotal}"/>
            <property name="maxPublic" value="${methodCountPublic}"/>
        </module>
        <module name="OuterTypeNumber">
            <property name="max" value="1"/>
        </module>

        <!-- 4.2 Karmaşıklık -->
        <module name="CyclomaticComplexity">
            <property name="max" value="10"/>
            <property name="switchBlockAsSingleDecisionPoint" value="true"/>
        </module>
        <module name="NPathComplexity">
            <property name="max" value="200"/>
        </module>
        <module name="BooleanExpressionComplexity">
            <property name="max" value="3"/>
        </module>
        <module name="NestedIfDepth">
            <property name="max" value="1"/>
        </module>
        <module name="NestedForDepth">
            <property name="max" value="1"/>
        </module>
        <module name="NestedTryDepth">
            <property name="max" value="0"/>
        </module>
        <module name="ReturnCount">
            <property name="max" value="3"/>
            <property name="maxForVoid" value="1"/>
        </module>
        <module name="ClassFanOutComplexity">
            <property name="max" value="20"/>
        </module>
        <module name="ClassDataAbstractionCoupling">
            <property name="max" value="7"/>
        </module>
        <module name="ThrowsCount">
            <property name="max" value="1"/>
        </module>

        <!-- 4.3 Yapı -->
        <module name="DeclarationOrder"/>
        <module name="OverloadMethodsDeclarationOrder"/>
        <module name="OneStatementPerLine"/>
        <module name="MultipleVariableDeclarations"/>
        <module name="NeedBraces"/>
        <module name="EmptyBlock">
            <property name="option" value="statement"/>
        </module>
        <module name="EmptyCatchBlock"/>
        <module name="ParameterAssignment"/>
        <module name="InnerAssignment"/>
        <module name="MagicNumber">
            <property name="ignoreNumbers" value="-1, 0, 1, 2, 100"/>
            <property name="ignoreHashCodeMethod" value="true"/>
        </module>
        <module name="EqualsHashCode"/>
        <module name="StringLiteralEquality"/>
        <module name="HiddenField">
            <property name="ignoreConstructorParameter" value="true"/>
            <property name="ignoreSetter" value="true"/>
        </module>
        <module name="IllegalCatch"/>
        <module name="IllegalThrows"/>
        <module name="SimplifyBooleanExpression"/>
        <module name="SimplifyBooleanReturn"/>
        <module name="UnusedImports"/>
        <module name="UnusedLocalVariable"/>
        <module name="AvoidStarImport"/>
        <module name="AvoidStaticImport">
            <property name="excludes" value="org.assertj.core.api.Assertions.*,
                org.mockito.Mockito.*, org.mockito.ArgumentMatchers.*,
                com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*,
                java.util.Objects.requireNonNull"/>
        </module>

        <!-- 3. Adlandırma -->
        <module name="PackageName">
            <property name="format" value="^[a-z][a-z0-9]*(\.[a-z][a-z0-9]*)*$"/>
        </module>
        <module name="TypeName">
            <property name="format" value="^[A-Z][a-zA-Z0-9]{2,}$"/>
        </module>
        <module name="MethodName">
            <property name="format" value="^[a-z][a-zA-Z0-9]+$"/>
        </module>
        <module name="MemberName">
            <property name="format" value="^[a-z][a-zA-Z0-9]+$"/>
        </module>
        <module name="ParameterName">
            <property name="format" value="^[a-z][a-zA-Z0-9]+$"/>
        </module>
        <module name="LocalVariableName">
            <property name="format" value="^[a-z][a-zA-Z0-9]+$"/>
            <property name="allowOneCharVarInForLoop" value="true"/>
        </module>
        <module name="LambdaParameterName">
            <property name="format" value="^[a-z][a-zA-Z0-9]*$"/>
        </module>
        <module name="RecordComponentName">
            <property name="format" value="^[a-z][a-zA-Z0-9]+$"/>
        </module>
        <module name="ConstantName">
            <property name="format" value="^(log|[A-Z][A-Z0-9]*(_[A-Z0-9]+)*)$"/>
        </module>
        <module name="ClassTypeParameterName">
            <property name="format" value="^[A-Z][0-9]?$"/>
        </module>
        <module name="MethodTypeParameterName">
            <property name="format" value="^[A-Z][0-9]?$"/>
        </module>
        <module name="AbbreviationAsWordInName">
            <property name="allowedAbbreviationLength" value="0"/>
        </module>

        <!-- 4.4 Yorumlar -->
        <module name="TodoComment">
            <property name="format" value="(TODO(?!\(#\d+\))|FIXME|XXX)"/>
        </module>
        <module name="MissingJavadocType">
            <property name="scope" value="public"/>
        </module>
        <module name="RegexpSinglelineJava">
            <property name="format" value="^\s*//.*[;{}]\s*$"/>
            <property name="message" value="COMM-03: yorum satırına alınmış kod yasak"/>
        </module>

        <!-- LOG-03, MSG-04: log mesajında dize birleştirme -->
        <module name="RegexpSinglelineJava">
            <property name="format" value="log\.\w+\(&quot;[^&quot;]*&quot;\s*\+"/>
            <property name="message" value="LOG-03: parametreli log kullanın"/>
        </module>

        <!-- LOG-01 -->
        <module name="RegexpSinglelineJava">
            <property name="format" value="System\.(out|err)|\.printStackTrace\("/>
            <property name="ignoreComments" value="true"/>
            <property name="message" value="LOG-01: SLF4J kullanın"/>
        </module>
    </module>
</module>
```

`build-tools/checkstyle/suppressions.xml` yalnızca şu durumları içerir; başka satır eklemek GIT-10 istisna sürecine tabidir:

- `MissingJavadocType`: COMM-02'de sayılan paketler dışındaki tüm paketler.
- `IllegalCatch`: global hata işleyici, Kafka hata işleyici ve zamanlanmış görev kök sınıfları (ERR-04).
- Üretilmiş kod (`target/generated-sources/**`): tüm kontroller.

### E.2 PMD (`build-tools/pmd/ruleset.xml`)

```xml
<?xml version="1.0"?>
<ruleset name="yarnshipment"
         xmlns="http://pmd.sourceforge.net/ruleset/2.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://pmd.sourceforge.net/ruleset/2.0.0 https://pmd.sourceforge.io/ruleset_2_0_0.xsd">
    <description>Java 25 kod standartları, Bölüm 4</description>

    <!-- SIZE-10 -->
    <rule ref="category/java/design.xml/TooManyFields">
        <properties>
            <property name="maxfields" value="10"/>
        </properties>
    </rule>
    <!-- CPLX-02 -->
    <rule ref="category/java/design.xml/CognitiveComplexity">
        <properties>
            <property name="reportLevel" value="15"/>
        </properties>
    </rule>
    <!-- STRUC-13 -->
    <rule ref="category/java/bestpractices.xml/UnusedPrivateMethod"/>
    <rule ref="category/java/bestpractices.xml/UnusedFormalParameter"/>
</ruleset>
```

### E.3 ArchUnit (`libs/common-test`, her serviste çalışır)

Checkstyle ve PMD'nin göremediği kurallar ArchUnit ile her servisin `ArchitectureTest` sınıfında denetlenir. Ortak kural seti tüm servislerde aynıdır; servis yalnızca kök paketini verir.

| Kural | ArchUnit denetimi |
| --- | --- |
| ARCH-01…08 | Katman bağımlılıkları, döngü yasağı (Bölüm 6) |
| NAME-13 | Sınıf adı `Manager`, `Helper`, `Util`, `Utils`, `Impl`, `Data`, `Info`, `Processor` ile bitemez; `Base` ile başlayamaz |
| NAME-15 | `Throwable` alt sınıflarının adı `Exception` ile biter |
| NAME-16 | Arayüz adı `I[A-Z]` ile başlayamaz |
| NAME-17 | Soyut sınıflar `Abstract` ile başlar ve yalnızca `..common..` paketinde bulunur |
| NAME-21 | Metot adı yasaklı listede olamaz (`process`, `handle`, `manage…`, `perform…`, `execute`, `check`, `update`…) |
| NAME-22 | `port.in` arayüzleri `UseCase` ile biter ve tek metotludur |
| NAME-35 | `@Test` metotları `^should[A-Z]` ile başlar ve `@DisplayName` taşır |
| SIZE-12 | Bir pakette en fazla 30 üst düzey tip |
| STRUC-02 | Somut sınıflar `final` (sayılan istisnalar hariç) |
| STRUC-08 | `public` metotlarda `boolean`/`Boolean` parametre yok |
| DOM-10 | `Instant.now()`, `LocalDate.now()`, `LocalDateTime.now()`, `System.currentTimeMillis()` çağrısı yok |
| CONF-05 | `@Autowired` alan veya setter yok |

```java
public final class NamingRules {

    private static final String[] FORBIDDEN_SUFFIXES =
            {"Manager", "Helper", "Util", "Utils", "Impl", "Data", "Info", "Processor"};

    public static final ArchRule NO_VAGUE_CLASS_NAMES = noClasses()
            .should(haveSimpleNameEndingWithAnyOf(FORBIDDEN_SUFFIXES))
            .orShould().haveSimpleNameStartingWith("Base")
            .because("NAME-13: sınıf adı tek bir sorumluluğu anlatır");

    public static final ArchRule TEST_METHODS_START_WITH_SHOULD = methods()
            .that().areAnnotatedWith(Test.class)
            .should().haveNameMatching("^should[A-Z][a-zA-Z0-9]*$")
            .andShould().beAnnotatedWith(DisplayName.class)
            .because("NAME-35");

    public static final ArchRule NO_BOOLEAN_FLAG_PARAMETERS = methods()
            .that().arePublic()
            .and().areDeclaredInClassesThat().areNotRecords()
            .should(notHaveBooleanParameters())
            .because("STRUC-08: bayrak argümanı yerine ayrı metot veya enum");

    private NamingRules() {}
}
```

`haveSimpleNameEndingWithAnyOf` ve `notHaveBooleanParameters` `common-test` içinde yazılan küçük `ArchCondition` yardımcılarıdır.
