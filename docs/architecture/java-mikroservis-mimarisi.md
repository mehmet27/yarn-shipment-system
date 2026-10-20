# İplik Sevkiyat Sistemi – Java Mikroservis Mimarisi

> Sürüm 1.0 · 20.10.2026 · Kaynak: Ön analiz raporu v1.1 · Canlı doküman: [Claude Docs](https://claude.ai/code/artifact/b3dbda92-562a-4125-a8e8-5d4812fd2607)

## Özet ve temel kararlar

Backend, iş alanına göre ayrılmış 8 Spring Boot servisinden, önlerinde tek bir API Gateway'den ve servisler arası olayları taşıyan bir mesaj kuyruğundan oluşur. Ön analiz v1.1'deki modüller (Bölüm 4.1 ve 17.1) bu servislere birebir dağıtılmıştır; kör giriş, kayıt kilidi ve audit log gibi hile önleme kuralları servis sınırlarıyla korunur.

| Konu                | Karar                                                       | Kısa gerekçe                                                                         |
|---------------------|-------------------------------------------------------------|--------------------------------------------------------------------------------------|
| Mimari              | Alan bazlı mikroservis, 8 iş servisi + gateway              | Modüller birbirinden net ayrılıyor; kanıt ve audit gibi hassas alanlar izole kalıyor |
| Dil ve çatı         | Java 25 LTS, Spring Boot 4.x, Spring Cloud                  | Ekip Java yazacak; Spring en olgun ve en çok destek bulunan ekosistem                |
| Senkron iletişim    | REST + JSON (OpenAPI 3), Spring RestClient / HTTP Interface | Anında cevap gereken az sayıdaki çağrı için basit ve izlenebilir                     |
| Asenkron iletişim   | Apache Kafka (KRaft), transactional outbox                  | Olaylar kalıcı ve tekrar oynatılabilir; audit ve raporlama aynı akıştan beslenir     |
| Süreç koordinasyonu | Olay tabanlı koreografi (saga)                              | Sevkiyat akışı adım adım ilerliyor; merkezi orkestratöre gerek yok                   |
| API Gateway         | Spring Cloud Gateway                                        | Tek giriş noktası, JWT doğrulama, hız sınırı, CORS                                   |
| Service discovery   | Kubernetes Service DNS (Eureka yok)                         | Platform zaten sağlıyor; ek bileşen işletmeye gerek yok                              |
| Konfigürasyon       | Kubernetes ConfigMap/Secret + Spring profilleri             | Ortam bazlı ayar tek yerde; Config Server yalnızca K8s dışı kurulumda                |
| Kimlik              | Keycloak (OIDC/JWT)                                         | Detayı yetkilendirme çalışmasında netleşecek                                         |
| Dosyalar            | S3 uyumlu nesne deposu (MinIO)                              | Fotoğraf ve belgeler veritabanına değil nesne deposuna yazılır                       |

Veritabanı seçimi, yetki modeli ve React frontend ayrı çalışmalarda ele alınıyor; bu doküman onlarla kesiştiği yerleri yalnızca arayüz düzeyinde tanımlar.

## Mimari yaklaşım

Mikroservis tercih edildi, ancak servis sayısı sistemin ölçeğine göre bilinçli olarak küçük tutuldu. Faz 1'de tek fabrika ve 1–2 pilot fasoncu var; yük düşük, asıl ihtiyaç güvenilirlik ve kanıt bütünlüğü.

Mikroservisin bu projede sağladığı somut faydalar:

- **Kanıt ve audit izolasyonu:** Fotoğraf, hash ve audit kayıtları ayrı servislerde ve ayrı veritabanlarında durur. Sevkiyat servisinde yetki açığı olsa bile kanıt silinemez, audit değiştirilemez.

- **Kör girişin teknik garantisi:** Çıkış kilosu ile teslim kilosunu yan yana gören tek yer karşılaştırma servisidir. Fasoncuya dönen API bu veriyi hiç taşımaz.

- **Bağımsız ölçekleme:** Fotoğraf yükleme (sevkiyat başına 10–15 dosya) diğer işlemlerden çok daha fazla kaynak tüketir ve tek başına ölçeklenebilir.

- **Faz 2 ve 3'e hazırlık:** e-İrsaliye, ERP, kantar, GPS ve RFID entegrasyonları yeni servisler olarak eklenir, mevcut servislere dokunulmaz.

Bedeli: dağıtık sistem işletmesi (Kafka, Kubernetes, izleme) ve servisler arası tutarlılığın olaylarla sağlanması. Bu yükü azaltmak için tüm servisler tek repoda, ortak kütüphanelerle ve aynı şablonla yazılır. Ekip 3 kişiden fazla olduğu için Faz 1'e doğrudan mikroservislerle başlanır; servisler ekip üyeleri arasında paylaştırılabilir.

## Servis sınırları

Her servis kendi verisinin tek sahibidir; başka bir servisin tablosuna doğrudan erişmez, yalnızca API'si veya olayları üzerinden konuşur. Faz sütunu, ön analizin Bölüm 17 yol haritasına göre verilmiştir.

![Backend mimarisi · gateway, 8 servis, Kafka](img/backend-mimarisi.png)

Üst sıradaki servisler kullanıcı işlemlerini alır ve olay yayınlar; alt sıradakiler bu olaylarla çalışır. Gateway, uyuşmazlık kararı ve raporlar için alt sıradaki servislerin API'lerine de yönlendirir; şemada sadeleştirmek için bu oklar gösterilmedi.

| Servis                                            | Sorumluluk                                                                                                                                  | Sahip olduğu veri                                                       | Yayınladığı başlıca olaylar                                                                      | Faz             |
|---------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------|-----------------|
| master-data-service (Tanımlar)                    | Fasoncu, şoför, araç, iplik/ürün, parti ve kalite tanımları; partinin kalite kilidi; standart dara değerleri                                | Fasoncu, Şoför, Araç, Ürün, Parti, KaliteSınıfı, DaraStandardı          | PartiKalitesiKilitlendi, FasoncuGuncellendi, SoforGuncellendi                                    | 1               |
| bag-service (Çuval ve etiket)                     | Unique Key üretimi, etiket basımı (QR/barkod), çuval–parti bağı, anahtarın tek kullanım garantisi                                           | Çuval, EtiketBaskısı, ÇuvalRezervasyonu                                 | CuvalOlusturuldu, CuvalSevkiyataBaglandi, CuvalRezervasyonuBirakildi                             | 1               |
| shipment-service (Sevkiyat ve teslimat)           | Sevkiyat oluşturma, şoför/araç atama, çıkış tartımı, mühür, şoför beyanları, fasoncu kör teslim girişi, durum makinesi ve kilitleme         | Sevkiyat, ÇıkışTartımı, Mühür, ŞoförBeyanı, TeslimGirişi, DüzeltmeKaydı | SevkiyatYuklendi, SevkiyatYolaCikti, TeslimGirisiYapildi, SevkiyatTamamlandi, SevkiyatKilitlendi | 1               |
| evidence-service (Kanıt)                          | Fotoğraf ve imzalı belge yükleme, SHA-256 hash, sunucu zaman damgası, tekrar kullanım tespiti, filigran, "geçersiz" işaretleme (silme yok)  | KanıtDosyası (meta veri), dosyanın kendisi nesne deposunda              | KanitYuklendi, KanitGecersizSayildi, KanitTekrarKullanimiTespitEdildi                            | 1               |
| discrepancy-service (Karşılaştırma ve uyuşmazlık) | Otomatik karşılaştırma (net kilo, çuval listesi, mühür, belge, zaman), tolerans bantları, uyuşmazlık yaşam döngüsü, kalite itirazı ve karar | Uyuşmazlık, KaliteItirazı, Karar, ToleransKuralı                        | KarsilastirmaTamamlandi, UyusmazlikAcildi, UyusmazlikKarariVerildi                               | 1               |
| audit-service (Denetim izi)                       | Tüm servislerin olaylarını ve değişiklik kayıtlarını sadece-ekle (append-only) olarak saklar; kayıtlar hash zinciriyle birbirine bağlanır   | AuditKaydı                                                              | (tüketici, olay yayınlamaz)                                                                      | 1               |
| notification-service (Bildirim)                   | Kırmızı uyuşmazlık, süresi geçen inceleme ve eksik belge için e-posta ve uygulama içi bildirim                                              | Bildirim, BildirimTercihi                                               | BildirimGonderildi                                                                               | 1               |
| reporting-service (Rapor ve risk)                 | Dashboard, şoför/fasoncu/eşleşme bazlı göstergeler; Faz 2'de risk skoru ve rastgele kontrol seçimi                                          | Okuma modelleri (olaylardan türetilir)                                  | KontrolIcinSecildi (Faz 2)                                                                       | 1 temel, 2 risk |

Kimlik doğrulama Keycloak tarafından yapılır ve ayrı bir Java servisi yazılmaz; kullanıcı–fasoncu ilişkisi gibi iş kuralları yetkilendirme çalışmasının sonucuna göre master-data-service içinde ya da küçük bir user-service olarak tutulacaktır.

**Sonraki fazlarda eklenecek servisler:** integration-service (e-İrsaliye, e-Fatura, ERP; Faz 2) ve device-gateway-service (kantar, GPS, RFID, elektronik mühür; Faz 3). İkisi de mevcut olayları tüketir ve yeni olay üretir; çekirdek servislerin kodu değişmez.

## Servisler arası iletişim

Kural basit: kullanıcı bir cevap bekliyorsa ve sonuç hemen gerekiyorsa REST, bir şey olduğunu diğer servislere duyurmak gerekiyorsa Kafka olayı kullanılır. Servisler arası senkron çağrı sayısı bilinçli olarak üçle sınırlıdır.

### Senkron çağrılar (REST)

| Çağıran          | Çağrılan            | Amaç                                                                                  | Neden senkron                                                         |
|------------------|---------------------|---------------------------------------------------------------------------------------|-----------------------------------------------------------------------|
| shipment-service | bag-service         | Okutulan Unique Key'leri sevkiyata rezerve etmek, parti ve kaliteyi almak             | Anahtar başka sevkiyattaysa depo personeli bunu okuttuğu anda görmeli |
| shipment-service | master-data-service | Sevkiyat oluştururken fasoncu, şoför ve araç geçerliliğini doğrulamak                 | Geçersiz kayıtla sevkiyat açılmamalı                                  |
| evidence-service | shipment-service    | Yükleme isteğinin ait olduğu sevkiyatın var olduğunu ve kilitli olmadığını doğrulamak | Kilitli sevkiyata kanıt eklenmemeli                                   |

Tüm senkron çağrılarda Resilience4j ile zaman aşımı (2 sn), sınırlı tekrar ve devre kesici kullanılır. Hata modeli RFC 9457 ProblemDetail formatındadır.

### Asenkron olaylar (Kafka)

- **Transactional outbox:** Servis, iş verisini ve olayı aynı veritabanı işlemiyle kendi outbox tablosuna yazar; ayrı bir yayıncı Kafka'ya iletir. Böylece "veri kaydedildi ama olay kayboldu" durumu oluşmaz. Faz 1'de yoklamalı (polling) yayıncı, hacim artarsa Debezium kullanılır.

- **Idempotent tüketici:** Her olayda benzersiz eventId bulunur; tüketici işlediği kimlikleri saklar ve tekrar gelen olayı yok sayar.

- **Sıralama:** Olaylar sevkiyat kimliğiyle (shipmentId) bölümlenir, böylece aynı sevkiyatın olayları sırayla işlenir.

- **Olay formatı:** JSON + CloudEvents zarfı (id, type, source, time, subject). Şemalar ortak event-contracts modülünde versiyonlanır.

- **Hatalı olaylar:** Tekrar denemelerden sonra işlenemeyen olay \*.DLT konusuna gider ve izleme panelinde alarm üretir.

### Sevkiyat akışı (koreografi saga)

1.  Depo personeli çuvalları okutur; shipment-service, bag-service üzerinden anahtarları rezerve eder.

2.  Çıkış tartımı, mühür ve fotoğraflar girilir. evidence-service her dosya için KanitYuklendi yayınlar; shipment-service zorunlu kanıtların tamamlandığını bu olaylardan izler ve durumu *Yüklendi* yapar.

3.  Şoför beyanlarıyla durum *Yolda* ve *Teslim Bekliyor* olur.

4.  Fasoncu kör teslim girişini yapar; shipment-service TeslimGirisiYapildi olayını çıkış ve teslim verisiyle birlikte yayınlar, durum *Kontrolde* olur.

5.  discrepancy-service karşılaştırmayı yapar ve KarsilastirmaTamamlandi yayınlar: temizse sevkiyat *Tamamlandı* olup kilitlenir; uyuşmazlık varsa *İncelemede* olur ve notification-service Kırmızı bantta yetkiliye bildirim gönderir.

6.  Kalite yetkilisi karar verdikçe UyusmazlikKarariVerildi yayınlanır; son açık uyuşmazlık kapandığında sevkiyat *Tamamlandı* olur ve kilitlenir.

7.  audit-service ve reporting-service tüm bu olayları dinler; hiçbir servis onları beklemez.

Bir adımın geri alınması gerektiğinde (örneğin *İptal*) telafi olayı yayınlanır: SevkiyatIptalEdildi gelince bag-service rezervasyonları serbest bırakır.

**Kör giriş kuralı:** Çıkış kilosu ve çuval sayısı TeslimGirisiYapildi olayında yalnızca discrepancy-service'e gider. shipment-service'in fasoncuya ve şoföre dönen API yanıtları bu alanları karşılaştırma bitene kadar içermez; bu, ayrı DTO'larla ve sözleşme testleriyle güvence altına alınır.

## API Gateway, service discovery ve konfigürasyon

React uygulaması ve mobil tarayıcılar yalnızca gateway ile konuşur; servisler dışarıya kapalıdır.

### API Gateway: Spring Cloud Gateway

- **Yönlendirme:** /api/v1/shipments/\*\* → shipment-service, /api/v1/evidence/\*\* → evidence-service gibi yol bazlı kurallar.

- **Kimlik doğrulama:** Keycloak'tan gelen JWT gateway'de doğrulanır ve servislere iletilir (token relay). Servisler de token'ı tekrar doğrular (sıfır güven); rol ve kapsam kontrolü servis içinde yapılır.

- **Hız sınırı:** Redis tabanlı kullanıcı başına limit; özellikle giriş ve yükleme uçları için.

- **İzlenebilirlik:** Her isteğe traceparent ve X-Request-Id eklenir.

- **CORS ve güvenlik başlıkları** tek yerde yönetilir.

- **Büyük dosyalar gateway'den geçmez:** evidence-service kısa ömürlü (ör. 10 dk) imzalı yükleme adresi (pre-signed URL) verir, telefon dosyayı doğrudan nesne deposuna gönderir, ardından servis hash'i sunucuda yeniden hesaplayıp kaydı onaylar. Bu, zayıf bağlantıda yarım kalan yüklemelerin tekrar denenmesini de kolaylaştırır.

Frontend için ayrı bir BFF katmanı Faz 1'de gerekmiyor; gateway bu rolü yeterince karşılıyor. Token'ın tarayıcıda mı yoksa gateway oturumunda mı tutulacağı yetkilendirme çalışmasıyla birlikte kesinleşecek.

### Service discovery: Kubernetes DNS

Servisler Kubernetes üzerinde çalışır ve birbirini http://bag-service:8080 gibi Service adlarıyla bulur. Eureka veya Consul kurulmaz; yük dağıtımı ve sağlık kontrolü Kubernetes tarafından yapılır. İlk aşamada sistem yerel makinede Docker Compose ile çalışır; Compose ağı da aynı servis adlarını çözdüğü için Eureka yine gerekmez. Kendi sunucularınıza geçişte kod değişmez, yalnızca dağıtım dosyaları (Helm) devreye girer.

### Konfigürasyon

- Ortam ayarları (dev, test, prod) Spring profilleri ve Kubernetes ConfigMap ile verilir.

- Gizli bilgiler (veritabanı şifresi, Kafka ve MinIO anahtarları) Kubernetes Secret'ta, tercihen Vault veya Sealed Secrets ile tutulur; repoya girmez.

- **İş ayarları konfigürasyon değildir:** Tolerans bantları, zorunlu fotoğraf listesi, itiraz süresi ve rastgele kontrol oranı Admin ekranından değiştirilir. Bu yüzden ilgili servisin veritabanında, değişiklik geçmişiyle ve audit kaydıyla tutulur.

## Teknoloji yığını

Sürümler projenin başladığı günün güncel kararlı sürümleriyle kesinleştirilmelidir; aşağıdaki numaralar hafızadan verilmiş yaklaşık değerlerdir.

| Katman                 | Seçim                                                                               | Gerekçe                                                                                                       |
|------------------------|-------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| Dil                    | Java 25 LTS (Java 21 LTS yedek)                                                     | Uzun süreli destek; sanal thread'ler yüksek eşzamanlı G/Ç'yi basitleştirir                                    |
| Çatı                   | Spring Boot 4.x (Spring Framework 7)                                                | Olgun ekosistem, geniş geliştirici havuzu, Kafka/Keycloak/S3 entegrasyonları hazır                            |
| Bulut bileşenleri      | Spring Cloud (Gateway, Kubernetes, Stream veya Spring Kafka)                        | Gateway ve K8s entegrasyonu tek ekosistemden                                                                  |
| Web                    | Spring MVC + sanal thread'ler                                                       | Reaktif programlamanın karmaşıklığı olmadan yeterli ölçek                                                     |
| Veri erişimi           | Spring Data JPA (Hibernate), Flyway ile migration                                   | Standart, test edilmesi kolay; şema değişiklikleri versiyonlu                                                 |
| Veritabanı             | Servis başına ayrı şema veya veritabanı                                             | Kesin seçim veritabanı çalışmasında; bu mimari ilişkisel bir veritabanı varsayıyor                            |
| Mesajlaşma             | Apache Kafka (KRaft modu), Spring Kafka                                             | Kalıcı ve tekrar oynatılabilir olay akışı; audit ve raporlama yeniden inşa edilebilir                         |
| Nesne deposu           | MinIO (S3 uyumlu), AWS SDK v2                                                       | Kendi sunucunuzda çalışır; ileride buluta taşımak kod değiştirmez. Object Lock ile dosyalar silinemez yapılır |
| Önbellek ve hız sınırı | Redis                                                                               | Gateway rate limit ve kısa süreli önbellek                                                                    |
| Kimlik                 | Keycloak + Spring Security OAuth2 Resource Server                                   | Açık kaynak, iki adımlı doğrulama (Admin ve Kalite için) hazır                                                |
| API sözleşmesi         | OpenAPI 3 (springdoc), API-first                                                    | Frontend ekibi sözleşmeden TypeScript istemcisi üretebilir                                                    |
| Eşleme ve doğrulama    | MapStruct, Jakarta Bean Validation                                                  | Derleme zamanında güvenli DTO dönüşümü                                                                        |
| Dayanıklılık           | Resilience4j                                                                        | Zaman aşımı, tekrar, devre kesici                                                                             |
| Etiket ve belge        | ZXing (QR/barkod), OpenPDF veya JasperReports (etiket ve sevkiyat dosyası PDF)      | Açık kaynak, sunucu tarafında üretim                                                                          |
| Test                   | JUnit 5, Testcontainers (veritabanı, Kafka, MinIO), Spring Cloud Contract, ArchUnit | Gerçek altyapıyla entegrasyon testi; servisler arası sözleşme testi                                           |
| Derleme                | Maven (çok modüllü), Jib ile container imajı                                        | Dockerfile yazmadan tekrarlanabilir imaj                                                                      |

## Gözlemlenebilirlik, güvenlik ve dağıtım

Bir sevkiyatın tüm servislerdeki izi tek bir trace kimliğiyle takip edilebilmelidir; bir uyuşmazlık incelenirken "bu olay ne zaman, hangi servisten geçti" sorusu dakikalar içinde cevaplanmalıdır.

| Alan            | Yaklaşım                                                                                                                           |
|-----------------|------------------------------------------------------------------------------------------------------------------------------------|
| Metrikler       | Micrometer → Prometheus → Grafana; iş metrikleri de (açık uyuşmazlık, bekleyen teslim, outbox gecikmesi)                           |
| Loglar          | JSON log, traceId ve shipmentId alanlarıyla; Loki veya ELK'de toplanır. Kişisel veri loglanmaz (KVKK)                              |
| Dağıtık izleme  | OpenTelemetry → Tempo veya Jaeger; gateway'den Kafka tüketicisine kadar tek iz                                                     |
| Sağlık          | Spring Boot Actuator liveness ve readiness uçları, Kubernetes problarına bağlı                                                     |
| Alarmlar        | DLT'ye düşen olay, outbox birikmesi, 5xx oranı, disk doluluk (fotoğraf deposu)                                                     |
| Audit bütünlüğü | audit-service her kaydı bir öncekinin hash'iyle zincirler; günlük zincir doğrulama işi bozulma olursa alarm verir                  |
| Ağ güvenliği    | Yalnızca gateway dışarı açık; servisler arası trafik Kubernetes NetworkPolicy ile sınırlı; dış trafik HTTPS                        |
| Yedekleme       | Veritabanları günlük yedek ve noktasal geri dönüş; MinIO versiyonlama ve ikinci lokasyona kopya                                    |
| Dağıtım         | Şimdilik yerelde Docker Compose; sonra kendi sunucularınızda Kubernetes (k3s), Helm chart'ları, servis başına en az 2 kopya (prod) |
| CI/CD           | Her commit'te derleme, birim ve Testcontainers testleri, imaj, dev ortamına otomatik dağıtım; prod onaylı                          |
| Ortamlar        | dev, test (pilot öncesi saha testi), prod                                                                                          |

Faz 1 için kaba kaynak tahmini: 8 servis + gateway, servis başına 0,5 vCPU ve 512 MB–1 GB bellek; Kafka 3 düğüm (pilotta 1 düğüm yeterli). Bu, 3 orta boy sunucudan oluşan bir kümeye sığar; kesin boyutlandırma pilot verisiyle yapılmalıdır.

## Proje yapısı, standartlar ve yol haritası

### Repo yapısı (tek repo, Maven çok modüllü)

```
iplik-sevkiyat-backend/
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
    event-contracts/     # olay şemaları ve versiyonları
    common-web/          # ProblemDetail hata modeli, request id, sayfalama
    common-security/     # JWT, rol ve fasoncu kapsamı yardımcıları
    common-outbox/       # outbox yayıncısı ve idempotent tüketici
  deploy/
    helm/
    docker-compose.yml   # yerel geliştirme: DB, Kafka, MinIO, Keycloak, Redis
```

### Servis içi yapı

Her servis aynı altıgen (ports and adapters) düzeni izler: domain (iş kuralları, framework bağımsız), application (kullanım senaryoları), adapter.in.web, adapter.in.messaging, adapter.out.persistence, adapter.out.messaging. ArchUnit testleri, domain katmanının Spring veya JPA'ya bağımlı olmasını engeller.

### Ortak kurallar

- REST adresleri çoğul ve versiyonlu: /api/v1/shipments/{id}/delivery.

- Durum değiştiren uçlar Idempotency-Key başlığı kabul eder; zayıf bağlantıda tekrar gönderilen istek çift kayıt üretmez.

- Kilitli kayıtta güncelleme uçları 409 Conflict döner; düzeltme yalnızca ayrı /corrections uçuyla, gerekçeyle yapılır.

- Zaman alanları UTC ve ISO 8601; istemcinin çekim zamanı (capturedAt) ile sunucu zamanı (receivedAt) ayrı saklanır.

- İyimser kilitleme (version alanı) ile aynı sevkiyatın eşzamanlı güncellenmesi engellenir.

### Geliştirme sırası (Faz 1)

1.  Altyapı iskeleti: repo, ortak kütüphaneler, yerel docker-compose, CI, gateway ve Keycloak bağlantısı.

2.  master-data-service ve bag-service: tanımlar, parti kalite kilidi, Unique Key ve etiket basımı.

3.  shipment-service ve evidence-service: sevkiyat durum makinesi, çıkış, mühür, fotoğraf yükleme.

4.  Kör teslim girişi ve discrepancy-service: karşılaştırma, tolerans, uyuşmazlık ve karar.

5.  audit-service, notification-service, reporting-service temel dashboard.

6.  Saha testi (test ortamı), ardından 4–6 haftalık pilot.

Audit ve outbox altyapısı 1. adımda kurulur; böylece ilk servisten itibaren her değişiklik kayıt altındadır.

### Açık sorular

- [x] Sunucular nerede olacak? Yanıt: şimdilik yerel makinede (Docker Compose), sonra şirketin kendi sunucularında (Kubernetes/k3s, MinIO kendi sunucuda).

- [x] Geliştirme ekibi kaç kişi? Yanıt: 3'ten fazla; mikroservisle başlanır.

- [ ] Kafka'yı işletecek bir ekip var mı? Yoksa RabbitMQ daha az bakım ister; outbox katmanı sayesinde geçiş kodu etkilemez.

- [ ] Yasal saklama süresi kaç yıl? Nesne deposu kapasitesi ve Object Lock süresi buna bağlı.

- [ ] Veritabanı çalışması ilişkisel dışı bir seçim yaparsa veri erişim katmanı yeniden değerlendirilmeli.
