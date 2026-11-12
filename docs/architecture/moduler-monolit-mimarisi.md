# İplik Sevkiyat Sistemi – Modüler Monolit Mimarisi

> Sürüm 2.0 · 12.11.2026 · Güncel mimari. Önceki sürüm (8 mikroservis, Kafka, transactional outbox, Kubernetes) tarihsel
> kayıt olarak [java-mikroservis-mimarisi.md](java-mikroservis-mimarisi.md) dosyasında durur; oradaki kural kimlikleri
> (STR, ARCH, EVT-xx …) kod standartlarında geçtiği için dosya silinmedi. İki belge çeliştiğinde bu belge geçerlidir.
> Karar ve adımların gerekçesi: `db/monolit-gecis-plani-v1.md` (proje dosyaları).

## Özet ve temel kararlar

Backend, tek Spring Boot uygulamasında (`services/yarnshipment-app`) çalışan beş modülden ve onun önündeki ayrı bir
gateway sürecinden oluşur. Pilot ölçeğinde (tek fabrika, birkaç fasoncu) asıl ihtiyaç güvenilirlik ve kanıt bütünlüğüdür;
sekiz servis, Kafka ve Kubernetes işletme yükü bu ihtiyaca göre fazlaydı. Modül sınırları korundu, ağ sınırı kalktı.

| Konu | Karar | Kısa gerekçe |
|---|---|---|
| Mimari | Modüler monolit: 5 modül tek süreçte, gateway ayrı süreç | Modül sınırı kodda ve şemada korunur; dağıtık sistem işletmesi yok |
| Dil ve çatı | Java 21 LTS, Spring Boot 4.x, Spring MVC | Java 22+ özelliği kullanılmaz |
| Modüller arası olay | Süreç içi: `DomainEventPublisher` portu + `InProcessDomainEventPublisher` | Kafka ve outbox kalktı; olay üreten işlemin içinde iletilir |
| Modüller arası çağrı | `libs/module-api` arayüzleri (süreç içi port) | REST istemcileri ve `TokenRelayInterceptor` kalktı |
| Veritabanı | Tek PostgreSQL 17 veritabanı, modül başına şema, ortak nesneler için `platform` şeması | Şema sınırı korunur; her şemanın kendi Flyway geçmişi vardır |
| Audit | `audit_append()` iş transaction'ının içinde; `audit_writer` / `audit_reader` rolleri | Olay kaybı yok; işlem geri alınırsa audit de yazılmaz |
| Gateway | Spring Cloud Gateway (MVC), ayrı süreç, BFF oturumu (Redis) | Token tarayıcıya inmez; oturum katmanı uygulamadan ayrı kalır |
| Kimlik | Keycloak (OIDC/JWT); Admin ve Kalite için OTP | Uygulama JWT'yi yine doğrular |
| Dosyalar | MinIO (S3 uyumlu), önceden imzalı adresle doğrudan yükleme | Fotoğraflar gateway'den ve veritabanından geçmez |
| Raporlama | Canlı SQL görünümleri (`reporting` şeması) | Okuma kopyası ve olayla besleme yok |
| Dağıtım | Docker Compose (tek sunucu): postgres, redis, minio, keycloak, yarnshipment-app, gateway, web, caddy | Bkz. `DEPLOY.md` |

## Süreç ve modül görünümü

```
tarayıcı ──► caddy ──► gateway (BFF, ayrı süreç, Redis oturumu)
                          │  Bearer token (token relay)
                          ▼
                 yarnshipment-app (tek Spring Boot, port 8083)
                 ├─ modül shipment      ─ şema shipment
                 ├─ modül discrepancy   ─ şema discrepancy
                 ├─ modül evidence      ─ şema evidence ──► MinIO
                 ├─ modül reporting     ─ şema reporting (görünümler)
                 ├─ modül audit         ─ şema audit
                 └─ ortak nesneler      ─ şema platform
                          │
                     PostgreSQL 17 (tek veritabanı: yarnshipment)      Keycloak (şema keycloak)
```

| Modül | Sorumluluk | Şema ve tablolar |
|---|---|---|
| shipment | Sevkiyat yaşam döngüsü (müdür açar, depo çuval atar ve çıkış tartar, müdür irsaliyeyle tamamlar), tanımlar (`md_*`), fasoncu bakiyesi, fire, kumaş ve iplik dönüşü, merkezi stok, ayarlar | `shipment`: `shipment`, `shipment_line`, `shipment_bag`, `shipment_status_history`, `weighing`, `weight_check`, `correction_record`, `md_site`, `md_subcontractor`, `md_driver`, `md_vehicle`, `md_product`, `md_quality_grade`, `md_production_lot`, `md_yarn_supplier`, `balance_movement`, `fire_settlement`, `fire_rate_change`, `fabric_lot`, `yarn_return`, `central_stock_movement`, `staff_work_area`, `app_setting` |
| discrepancy | Uyuşmazlık ve kalite itirazı yaşam döngüsü, tolerans ve zaman kuralları | `discrepancy`: `discrepancy`, `discrepancy_activity`, `inspection`, `quality_claim`, `shipment_watch` |
| evidence | Fotoğraf ve belge yükleme, SHA-256 hash, tekrar kullanım tespiti, geçersiz işaretleme (silme yok) | `evidence`: `evidence_file`, `evidence_duplicate`, `upload_intent`; dosya MinIO'da |
| reporting | Dashboard ve rapor okuma modelleri | `reporting`: `shipment_view`, `discrepancy_view`, `balance_movement_view`, `fire_file_view` (hepsi canlı SQL görünümü) |
| audit | Değiştirilemez denetim izi, hash zinciri, günlük zincir doğrulama | `audit`: `audit_log`, `audit_chain_head`, `audit_chain_check` |
| platform (ortak) | Modüllerin paylaştığı iki nesne | `platform`: `idempotency_record`, `audit_local` |

Toplam 36 tablo (Flyway geçmiş tabloları ve aylık bölümler hariç). Kaynak hesap: 56 tablodan başlandı; ölü tablolar,
outbox, `processed_event`, okuma kopyaları ve tekilleştirilen ortak tablolar çıkarıldı.

Her modül altıgen mimariyi (`adapter` / `application` / `domain`) korur; kurallar ArchUnit testleriyle sabitlenmiştir
(`libs/common-test`). Modül kodu başka modülün iç paketine erişmez; modüller yalnızca `libs/module-api` arayüzleri,
`libs/event-contracts` olay sözleşmeleri ve bu belgedeki olay akışıyla konuşur.

## Modüller arası iletişim

### Senkron çağrılar (süreç içi port)

Karşı modülün cevabı hemen gerektiğinde çağıran taraf `libs/module-api` içindeki arayüzü kullanır; uygulaması çağrılan
modüldedir ve görünürlük ve kapsam denetimi (SiteId, fasoncu kapsamı) yine çağrılan modülde yapılır. Kullanıcının
güvenlik bağlamı aynı thread'de kaldığı için JWT iletmeye gerek yoktur.

| Çağıran | Arayüz | Uygulayan | Amaç |
|---|---|---|---|
| evidence | `ShipmentAccessApi.findVisibleShipment` | shipment | Yüklemenin ait olduğu sevkiyatın var olduğunu, görünür ve kilitli olmadığını doğrulamak |
| shipment | `EvidenceQueryApi.findVisibleEvidence` | evidence | Tamamlama için irsaliye kanıtının varlığını, türünü (`WAYBILL`) ve durumunu (`VALID`) doğrulamak |
| discrepancy | `EvidenceQueryApi.findVisibleEvidence` | evidence | Uyuşmazlığa bağlanan kanıtın varlığını ve görünürlüğünü doğrulamak |

evidence ↔ shipment karşılıklı bağımlılığı bu arayüzlerle kırılır: iki modül birbirinin sınıfını değil `module-api`
arayüzünü tanır. Hedef modül yanıt veremezse `ModuleUnavailableException` fırlatılır (bugün aynı süreç olduğu için
yalnızca beklenmedik hatada).

### Olaylar (süreç içi)

Üreten modül iş satırını yazdığı transaction içinde `DomainEventPublisher.publish(DomainEvent)` çağırır. Olay türüne
göre konu ve kaynak `yarnshipment.events.routes` ayarından (`EventRoutes`) bulunur; denetim kaydı bunlardan varlık
türünü ve servis adını çıkarır. Olay zarfı CloudEvents alanlarını (`id` UUIDv7, `type`, `source`, `time`, `subject`)
taşımaya devam eder; böylece yük `libs/event-contracts` sözleşmeleriyle aynıdır ve gerekirse (Faz 2/3) olay altyapısı
yeniden eklenebilir.

Tüketici, hangi transaction semantiğini istediğini `DomainEventConsumer.delivery()` ile yazılı bildirir:

| Tüketici | Dinlediği olaylar | Teslim | Neden |
|---|---|---|---|
| discrepancy `ShipmentEventsConsumer` | oluşturma, çıkış, tamamlanma, iptal | `BEFORE_COMMIT` | Zaman ve fotoğraf kurallarının girdisi sevkiyat kaydıdır; hata sevkiyat işlemini geri alır |
| shipment `DiscrepancyEventsConsumer` | `DiscrepancyResolved` (talep düzeltmesi) | `BEFORE_COMMIT` | İki modül birbirinden ayrışmamalı |
| audit `AuditEventsConsumer` | tüm modüllerin olayları | `BEFORE_COMMIT` | Audit kaydı iş işlemiyle birlikte yazılır, işlem geri alınırsa o da yazılmaz |

- `BEFORE_COMMIT`: üreten işlemin commit'inden hemen önce, aynı transaction'da; tüketici hatası iş işlemini geri alır.
  Kaybı kabul edilemeyen tüketiciler içindir.
- `AFTER_COMMIT`: commit'ten sonra kendi yeni transaction'ında; hata iş işlemini etkilemez, loglanır. Yeniden
  üretilebilir okuma modelleri içindir. Bugün bu semantiği kullanan tüketici yoktur (reporting görünüme geçti).
- Aktif transaction yoksa ikisi de hemen çalışır; tüketici commit öncesi yeni olay yayınlarsa o da aynı transaction'da
  iletilir.
- Olay sözleşmesi sürümlenir (`.v1`); bir sürüme yalnızca isteğe bağlı alan eklenir (EVT-06). Tüketici bilinmeyen
  alanları yok sayar.

Kafka, `outbox_event`, `processed_event`, DLT ve `common-outbox` kaldırıldı. Süreç içi teslimde yeniden deneme yoktur:
hata ya iş işlemini geri alır (`BEFORE_COMMIT`) ya da loglanır (`AFTER_COMMIT`).

## Veri mimarisi

- **Tek veritabanı `yarnshipment`, tek uygulama kullanıcısı `yarnshipment`** (`DB_URL`, `DB_USER`, `DB_PASSWORD`).
  Tek bağlantı havuzu (`DB_POOL_SIZE`); havuz `search_path` değerini modül şemalarına ayarlar. Ortak tablolar şema adıyla
  yazılır (`platform.idempotency_record`).
- **Her şema kendi Flyway'iyle göç eder** (`DatabaseMigrationConfiguration`): önce `platform`, sonra shipment →
  discrepancy → evidence → audit → reporting. Reporting görünümleri shipment ve discrepancy tablolarını okuduğu için
  en sona kalır. Göç, JDBC ve JPA bean'leri başlamadan biter. Migration dosyaları modülün `db/migration/<şema>`
  klasöründedir; `platform` migration'ları `libs/common-web/src/main/resources/db/platform` altındadır (modül
  Flyway'leri bu klasörü görmez). Eski migration'lara dokunulmaz, her değişiklik yeni migration'dır.
- **Platform şeması.** Üç modülde ayrı kopyası olan `idempotency_record` ve `audit_local` tek yere taşındı.
  `audit_local` kritik tablolardaki değişikliği trigger ile (`platform.audit_local_capture()`) yazar; hangi modül
  şemasından geldiği `table_schema` kolonundadır, aktör `app.actor_id` oturum ayarından gelir. Uygulama rolü bu
  tabloyu okuyamaz ve yazamaz; yalnızca `SECURITY DEFINER` trigger fonksiyonu ekler, UPDATE ve DELETE reddedilir
  (`YS001`). Aylık bölümler `platform.create_monthly_partitions()` ile açılır (ilk 15 ay hazır).
- **Reporting canlı görünümdür.** Dashboard sorguları shipment ve discrepancy tablolarından okunur; olayla beslenen
  kopya tablo yoktur. Sorgu süresi sorun olursa görünüm materialized view'a çevrilir (ayrı karar).
- **Ayrı kalan tablolar bilinçli:** `yarn_return` iade belgesidir (irsaliye no, yön), `balance_movement` ortak
  defterdir; ikisi birleştirilmez. `fire_settlement` ve `fire_rate_change` birleştirmesi ertelendi.

### Audit

1. Her işlem, iş transaction'ının içinde `audit_append(jsonb)` ile `audit.audit_log`'a ekler. Fonksiyon `SECURITY
   DEFINER`dir, `audit_chain_head` satırını kilitleyerek eklemeleri sıraya koyar ve her satırı bir öncekinin hash'ine
   bağlar. Aynı `eventId` ikinci kez yazılmaz.
2. `audit_verify_chain()` günlük olarak (cron `AUDIT_CHAIN_VERIFICATION_CRON`) zinciri doğrular ve sonucu
   `audit_chain_check`'e yazar.
3. **Roller.** `audit_writer` tablolara hiçbir yetkisi olmayan, yalnızca `audit_append()` ve `audit_verify_chain()`
   çalıştıran roldür; `audit_reader` yalnızca `SELECT` yapar. İkisi de `NOLOGIN`'dir. Uygulama kullanıcısı ikisinin
   üyesidir ama yetkilerini miras almaz (`INHERIT FALSE`); audit adaptörleri (`AuditAppendAdapter`,
   `AuditChainAdapter`, `AuditQueryAdapter`) işlerini `SET LOCAL ROLE` ile ilgili rolün altında çalıştırır ve bitince
   `RESET ROLE` yapar. Böylece yazma yolu denetim kaydını okuyamaz, okuma yolu hiçbir şey ekleyemez.
4. **Sınır.** Tablo sahibi uygulama kullanıcısıdır (Flyway aynı kullanıcıyla çalışır); sahip DDL yapabilir. Kalan
   korumalar: `reject_modification` trigger'ı, rol disiplini ve hash zinciri (trigger kapatılıp satır değiştirilirse
   doğrulama bozulmayı yakalar; zincir baştan hesaplanırsa yakalanmaz). Daha güçlü güvence gerekirse Flyway için ayrı
   bir sahip rolü ve audit için ayrı veritabanı kullanıcısı geri getirilir.
5. **Kilit süresi.** `audit_chain_head` tek satır kilidi tüm audit yazmalarını sıraya koyar ve artık iş transaction'ının
   içindedir. Pilot hacminde sorun beklenmez; audit tüketicisi commit'e en yakın noktada çalışır ve yük testiyle
   doğrulanmalıdır.

## Gateway, kimlik ve güvenlik

- **Gateway ayrı süreçtir** (Spring Cloud Gateway, MVC). Kullanıcı girişi BFF oturumuyla yapılır: gateway Keycloak ile
  OIDC akışını yürütür, token'ları sunucu tarafında (Redis oturumu) tutar ve tarayıcıya yalnızca HttpOnly oturum
  çerezi verir. Uygulamaya giden isteğe çerez gitmez, yalnızca `Authorization: Bearer` (token relay). Tüm rotalar tek
  uygulamaya (`APP_URL`) gider; yönlendirme yalnızca yol ayrımıdır.
- **Uygulama token'ı yine doğrular** (Resource Server, `audiences: yarnshipment-api`); gateway ele geçirilse bile
  yetki kontrolü uygulamada kalır. İzinler `common-security` içindedir; Admin ve Kalite rolleri OTP kanıtı (`acr=2`)
  olmadan yetki almaz.
- **Dosyalar** gateway'den geçmez: evidence modülü kısa ömürlü imzalı yükleme adresi verir, tarayıcı MinIO'ya doğrudan
  yükler, modül hash'i yeniden hesaplayıp kaydı onaylar. Bucket'ta versiyonlama ve Object Lock açıktır.
- **Idempotency-Key.** `POST/PUT/PATCH` isteklerinde `Idempotency-Key` ile aynı istek ikinci kez işlenmez
  (`platform.idempotency_record`, kullanıcı başına).
- **Değiştirilemezlik.** Tamamlanan sevkiyat kilitlenir; kilitli kayda değişiklik trigger'la reddedilir (`YS003`).
  Fiziksel silme yoktur (pasife alma, geçersiz işaretleme, iptal).
- **Modül izolasyonu** süreç sınırıyla değil kodla sağlanır: ArchUnit kuralları modüller arası iç paket erişimini
  yasaklar. Kör giriş (fasoncunun çıkış kilosunu görmemesi) Kasım 2026 süreç değişikliğiyle (teslim tartısı ve kör
  sayım kalktı) kapsam dışıdır.

## Çalışma zamanı ve dağıtım

| Bileşen | Not |
|---|---|
| yarnshipment-app | Tek JAR/imaj, port 8083; ayar `services/yarnshipment-app/src/main/resources/application.yml`; ortam değişkenleri README'de |
| gateway | Ayrı imaj, port 8080; Redis oturumu; `APP_URL` ile uygulamaya yönlenir |
| PostgreSQL 17 | Tek veritabanı; `deploy/postgres/initdb` veritabanı, kullanıcı ve audit rollerini açar |
| Keycloak, MinIO, Redis | Aynı Compose dosyasında; Keycloak aynı veritabanında `keycloak` şemasını kullanır |
| Bellek | 2 JVM (uygulama + gateway); eski düzende 6 JVM ve Kafka vardı |

- Yerel: `docker compose -f deploy/docker-compose.yml up -d`; üretim: `deploy/prod` (bkz. `DEPLOY.md`).
- Sanal thread'ler açıktır; reaktif kod yalnızca gateway'dedir.
- CI: `./mvnw verify` (Error Prone + NullAway, Checkstyle, ArchUnit, PMD, JaCoCo) ve `db-migrations` iş akışı
  (`tools/db/verify-migrations.sh`: önce platform, sonra modül migration'ları, kısıt ve rol testleri).
- Yük: outbox yoklaması (3 yayıncı × 1 sn) ve 5 Kafka dinleyicisi kalktı; kritik işlemde yazma sayısı iş satırı,
  değişen tablonun `platform.audit_local` satırı, `audit_log` ve `audit_chain_head` ile sınırlıdır.

## Ertelenenler ve riskler

| Konu | Durum |
|---|---|
| Ortak olmayan birleştirmeler: `fire_settlement` ve `fire_rate_change` | Ertelendi; geri alınması kolay, ayrı karar |
| Reporting için materialized view | Gerekirse (dashboard sorgu süresi) |
| `audit_chain_head` kilidi | Yük testiyle doğrulanacak |
| Ayrı Flyway sahibi rolü ve audit veritabanı kullanıcısı | Daha güçlü audit güvencesi gerekirse |
| Faz 2/3 (e-İrsaliye, ERP, kantar, GPS) | Yüksek hacimli kaynaklarda olay altyapısı ilgili entegrasyon için ayrı süreç olarak yeniden eklenir; `DomainEventPublisher` portu bunu mümkün kılar |
| Row Level Security | İsteğe bağlı ek güvence |

## Tarihsel not

Önceki sürümün 8 servisi (master-data, bag, shipment, evidence, discrepancy, audit, notification, reporting) bugünkü
beş modüle şöyle eşlenir: master-data ve bag shipment'a (`md_*` tabloları; çuval etiketi ve `bag` şeması kaldırıldı),
notification kaldırıldı, diğerleri aynı adla modül oldu. Eski belgedeki Kafka, outbox, DLT, Kubernetes, Resilience4j,
servisler arası REST ve servis başına veritabanı anlatımları geçerli değildir.
