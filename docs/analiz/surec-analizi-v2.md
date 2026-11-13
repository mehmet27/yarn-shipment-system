# İplik Sevkiyat ve Fason Bakiye Sistemi: Süreç Analizi v2

| | |
|---|---|
| Sürüm | 2.0 |
| Tarih | 10.11.2026 |
| Durum | Onaylanan saha cevaplarıyla (S1–S10) yazıldı; açık sorular Bölüm 15'tedir |
| Önceki sürüm | Ön analiz v1.1 (güven ve kanıt odaklı akış: şoför, mühür, kör teslim, otomatik uyuşmazlık) |
| Kaynaklar | Fabrika ziyareti el notları, Ambar Çıkış Fişi No 0239, 09.11.2026 "iplik cinsi × fasoncu" stok tablosu, `FASON SİPRİŞ FORMU.xlsx`, Kırıkhan giden iplik / gelen kumaş Excel'i, saha analizi raporu v1.0 ve S1–S10 cevapları |

Bu doküman, sistemin **bundan sonra** neyi nasıl yapacağını anlatır. Bölüm 12 bu sürümle koddan neyin kaldırıldığını,
Bölüm 14 kalan işlerin sırasını verir. Kodda bugün çalışan kısımlar "**[Bugün]**", henüz yazılmamış kısımlar
"**[Yapılacak]**" etiketiyle ayrılmıştır.

---

## İçindekiler

1. Amaç ve kapsam
2. Sözlük
3. Roller ve yetkiler
4. Uçtan uca süreç
5. Süreç adımlarının ayrıntısı
6. Sevkiyat durum makinesi
7. İş kuralları
8. Hesaplar: net kg, fire, hatalı kg, bakiye
9. Veri modeli
10. Ekranlar
11. Raporlar
12. Bu sürümde koddan kaldırılanlar
13. Korunanlar ve geçiş dönemi
14. Yol haritası
15. Açık sorular
16. Kabul senaryoları

---

## 1. Amaç ve kapsam

### 1.1 Sorun

Fabrika ipliği fasonculara (dış üreticilere) gönderir; fasoncu bu iplikle kumaş üretip geri yollar. Bugün bu takip
Excel'de yapılıyor:

- Hangi fasoncuda, hangi iplik cinsinden ne kadar iplik kaldığı (09.11.2026 tablosu: ~20 iplik cinsi × ~20 fasoncu,
  toplam 262.153 kg, merkezde 173.505 kg).
- Bir fasoncuya giden iplikle ondan gelen kumaşın karşılaştırılması ve fire (Kırıkhan Excel'i: 34.032 kg iplik,
  32.660 kg kumaş, gerçekleşen fire **%4,03**).
- Fire sınırını aşan eksik kilonun fasoncuya fatura edilmesi.

Excel'de hareketler elle toplandığı için bakiye geç ve hatalı çıkıyor, kimin neyi ne zaman girdiği izlenemiyor.

### 1.2 Sistemin amacı

1. Müdürün açtığı her iplik sevkiyatını, depoda kantarla doğrulanmış **net kilo** ile kayda almak.
2. Kantar beklenen yükü tutmazsa sevkiyatı **durdurmak** ve müdürün kararına bırakmak.
3. Sevkiyat fasoncuya ulaşınca **irsaliyeyle** tamamlamak.
4. Her fasoncu için iplik cinsi bazında **net kg bakiyesi** tutmak (giden iplik, gelen kumaş, dönen iplik).
5. Sevkiyat bazında **fireyi** (varsayılan %4, sevkiyata özel değiştirilebilir) hesaplamak, sınırı aşan
   **hatalı kiloyu** göstermek ve müdürün "**fatura edildi**" işaretlemesiyle kapatmak.
6. Kalite itirazlarını kayda almak ve bakiyeye/fireye etkisini doğru yansıtmak.
7. Genel müdür ve müdür için bakiye, fire ve sevkiyat raporlarını Excel/PDF olarak vermek.

### 1.3 Kapsam dışı

| Konu | Neden |
|---|---|
| Şoför uygulaması, teslim alma, varış beyanı | Sahada şoför rolü yok (saha notları) |
| Mühür numarası, mühür fotoğrafı | S10: kontroller kaldırılır |
| Fasoncunun sisteme girmesi | S5: fasoncu yalnızca kayıt (ana veri) olarak tutulur |
| Teslim tartısı ve kör sayım | Teslim irsaliyeyle kabul edilir |
| Otomatik çıkış–teslim karşılaştırması | Yerini kantar kuralı ve fire hesabı alır |
| Fatura kesmek, muhasebe/ERP entegrasyonu | S8: sistem faturalanacak kg/tutarı çıkarır, faturayı ERP keser |
| Çuval QR kodu zorunluluğu | Sahada çuvala kod gerekmiyor (bkz. Bölüm 13.3) |

---

## 2. Sözlük

| Terim | Anlamı |
|---|---|
| **Fasoncu** | İpliği alıp kumaş üreten dış firma. Sisteme giriş yapmaz; ana veri kaydıdır. |
| **Sevkiyat** | Merkez depodan bir fasoncuya tek araçla giden iplik yükü. Fire ve bakiye sevkiyat üzerinden izlenir. |
| **Reçete (sevkiyat satırları)** | Müdürün sevkiyatı açarken elle yazdığı "hangi iplik cinsinden ne kadar" listesi (S3). |
| **Beklenen net kg** | Reçete satırlarının toplamı; kantar kontrolünün hedefi. |
| **Brüt / dara / net** | Kantar fişindeki toplam ağırlık / araç (ve ambalaj) ağırlığı / yükün kendisi. Net = brüt − dara. |
| **Kantar uyuşmazlığı** | Çıkış tartısının neti beklenen net kg'yi tutmadığında oluşan durum; sevkiyat durur (S4). |
| **İrsaliye** | Sevkiyatın fasoncuya ulaştığını gösteren belge; müdür yükler ve numarasını yazar (S10). |
| **Fire oranı** | İplikten kumaşa geçişte kabul edilen kayıp. Varsayılan %4, sevkiyata özel değiştirilebilir (S1, S2). |
| **Beklenen kumaş** | (Gönderilen net − dönen iplik net) × (1 − fire oranı). |
| **Hatalı kg** | Beklenen kumaştan eksik gelen kilo; fasoncuya fatura edilir. |
| **Bakiye** | Bir fasoncuda, iplik cinsi bazında, henüz kumaşa dönmemiş net iplik kilosu. |
| **Bakiye hareketi** | Bakiyeyi değiştiren her kayıt: çıkış, kumaş girişi, iplik dönüşü, itiraz düzeltmesi, açılış. |
| **Fatura edildi** | Müdürün, hatalı kg'nin ERP'de faturalandığını işaretlemesi; sevkiyatın fire dosyası kapanır. |
| **Düzeltme** | Kilitli kayıttaki hatanın gerekçeyle, eski değer saklanarak değiştirilmesi. |

---

## 3. Roller ve yetkiler

### 3.1 Roller

| Rol | Kim | Sistemde ne yapar |
|---|---|---|
| **Müdür** (`MANAGER`) | Fason işlerinden sorumlu müdür | Sevkiyat açar, reçeteyi ve fire oranını girer; kantar uyuşmazlığında karar verir (iptal / çuval ekle-çıkar); irsaliyeyi yükleyip tamamlar; kumaş girişini ve iplik dönüşünü onaylar **[Yapılacak]**; "fatura edildi" işaretler **[Yapılacak]**; raporları görür |
| **Depo** (`WAREHOUSE`) | Merkez depo sorumlusu | Çuvalları sevkiyata atar, kantar fişiyle çıkış tartısını girer; eksik gönderimde neden yazar **[Yapılacak]** |
| **Kalite** (`QUALITY`) | Depo-kalite ve dışarı-kalite | Kalite itirazı açar, numune/lab sonucunu girer, uyuşmazlığı karara bağlar; gelen kumaşı kaydeder **[Yapılacak]** |
| **Genel müdür** **[Yapılacak]** | Üst yönetim | Yalnızca rapor ve bakiye görür; işlem yapmaz |
| **Admin** (`ADMIN`) | Sistem yöneticisi | Tanımlar, kullanıcılar, ayarlar, gerekçeli düzeltme, kanıt geçersiz kılma |
| ~~Şoför~~ | — | **Kaldırıldı.** Şoför ve araç yalnızca sevkiyatın üzerinde bilgi olarak yazılır |
| Fasoncu (`SUBCONTRACTOR`) | — | **Geçiş dönemi.** S5 gereği sisteme girmeyecek; rol ve saha ekranı sonraki PR'da kaldırılacak (Bölüm 13.2) |

### 3.2 İzin matrisi (bugünkü kod)

Endpoint'ler rol değil izin ister; matrisin tek kaynağı `libs/common-security/.../RolePermissionMatrix.java`.

| İzin | Admin | Depo | Kalite | Müdür | Fasoncu (geçiş) |
|---|:-:|:-:|:-:|:-:|:-:|
| `SHIPMENT_VIEW` | ✓ | ✓ | ✓ | ✓ | ✓ (yalnız kendi) |
| `SHIPMENT_CREATE` | ✓ | ✓ | | ✓ | |
| `SHIPMENT_CANCEL` | ✓ | | | ✓ | |
| `SHIPMENT_COMPLETE` (irsaliyeyle tamamlama, irsaliye yükleme) | ✓ | | | ✓ | |
| `DISPATCH_RECORD` (çuval atama/çıkarma, çıkış tartısı) | | ✓ | | | |
| `DISPATCH_VIEW` | ✓ | ✓ | ✓ | ✓ | ✓ |
| `EVIDENCE_UPLOAD_DISPATCH` (kantar fişi) | | ✓ | | | |
| `EVIDENCE_UPLOAD_CLAIM` (itiraz fotoğrafı, lab belgesi) | | | ✓ | | ✓ |
| `EVIDENCE_VIEW` | ✓ | ✓ | ✓ | ✓ | ✓ |
| `EVIDENCE_INVALIDATE` | ✓ | | | | |
| `CORRECTION_CREATE` | ✓ | | | | |
| `QUALITY_CLAIM_OPEN` | ✓ | | ✓ | | ✓ |
| `DISCREPANCY_VIEW` | ✓ | | ✓ | ✓ | ✓ |
| `DISCREPANCY_RESOLVE` | ✓ | | ✓ | | |
| `AUDIT_VIEW` | ✓ | | ✓ | ✓ | |
| `REPORT_VIEW` | ✓ | | ✓ | ✓ | |
| `MASTER_DATA_VIEW` / `_MANAGE` | ✓ / ✓ | ✓ / | ✓ / | ✓ / | |
| `USER_MANAGE`, `SETTINGS_MANAGE` | ✓ | | | | |

> Not: Depo sevkiyat açamaz; `SHIPMENT_CREATE` yalnız Müdür ve Admin'dedir (karar Bölüm 15, A3). Depo yalnız
> `DISPATCH_RECORD` ile çıkış tartısı girer.

### 3.3 Eklenecek izinler [Yapılacak]

| İzin | Kim | Ne için |
|---|---|---|
| `FABRIC_RECEIPT_RECORD` | Kalite, Müdür | Kumaş girişi |
| `YARN_RETURN_RECORD` | Depo, Müdür | Fasoncudan iplik dönüşü / fasoncudan gelen iplik |
| `FIRE_RATE_EDIT` | Müdür | Sevkiyatın fire oranını değiştirmek |
| `INVOICE_MARK` | Müdür | "Fatura edildi" işareti |
| `BALANCE_VIEW` | Müdür, Genel müdür, Kalite, Admin | Bakiye ekranı ve raporu |
| `WEIGHT_MISMATCH_RESOLVE` | Müdür | Durmuş sevkiyatı yeniden tartıya açmak |

---

## 4. Uçtan uca süreç

```mermaid
flowchart TD
    A[Müdür sevkiyat açar<br/>fasoncu, reçete, fire %4] --> B[Depo çuvalları atar]
    B --> C[Kantar: boş araç + dolu araç<br/>depo çıkış tartısını girer]
    C -->|net = beklenen| D[Fasoncuya yolda]
    C -->|net ≠ beklenen| X[Kantar uyuşmazlığı: sevkiyat durur]
    X -->|Müdür: iptal| Z[İptal + yeni sevkiyat]
    X -->|Müdür: çuval ekle/çıkar| B
    D --> E[Fasoncuya ulaştı<br/>müdür irsaliyeyi yükler ve tamamlar]
    E --> F[Fasoncu bakiyesi artar<br/>kumaş beklenir]
    F --> G[Kumaş girişi]
    F --> H[İplik dönüşü]
    G --> I[Fire hesabı]
    H --> I
    I -->|eksik ≤ fire| K[Sevkiyatın fire dosyası kapanır]
    I -->|eksik > fire| J[Hatalı kg → müdür ERP'de faturalar<br/>"fatura edildi" işaretler]
    J --> K
```

**[Bugün]** A, B, C (tek çıkış tartısı), D, E ve iptal çalışıyor. **[Yapılacak]** reçete satırları, kantar
uyuşmazlığı kuralı, F–K.

---

## 5. Süreç adımlarının ayrıntısı

### 5.1 Tanımlar (ana veri)

| Tanım | Bugün | Değişiklik |
|---|---|---|
| Fasoncu | ✓ (ünvan tekil, pasife alınır, silinmez) | Giriş hesabı açma kaldırılacak (S5) |
| Lokasyon (merkez depo) | ✓ | — |
| Araç (plaka) | ✓ (TR plaka deseni, tekil) | Bilgi amaçlı kalır |
| Şoför | ✓ | Bilgi amaçlı kalır; giriş hesabı yok |
| İplik cinsi / ürün | ✓ | Reçete satırında seçilecek; serbest metin de kabul edilecek (S3) |
| Parti (lot), kalite sınıfı | ✓ | İplik tedarikçisi alanı eklenecek **[Yapılacak]** |
| İplik tedarikçisi | — | **[Yapılacak]** (Şireci, Karesi, Teksmar, Migiteks, Creora…) |
| Fire oranı varsayılanı | — | **[Yapılacak]** ayar: `%4`, yalnız Admin değiştirir |
| Kantar toleransı | — | **[Yapılacak]** ayar: varsayılan 0 kg (Bölüm 15, A1) |

### 5.2 Sevkiyat oluşturma (Müdür)

**Giriş alanları**

| Alan | Zorunlu | Kural |
|---|:-:|---|
| Lokasyon | ✓ | Kullanıcının kapsamındaki lokasyon |
| Fasoncu | ✓ | Etkin fasoncu |
| Şoför adı, plaka | | Bilgi amaçlı; şoför seçilen fasoncuya göre süzülür |
| Reçete satırları **[Yapılacak]** | ✓ (en az 1) | Her satır: iplik cinsi (listeden ya da serbest metin), isteğe bağlı lot, net kg (> 0, 3 ondalık) |
| Beklenen çuval sayısı | | Bilgi amaçlı |
| Fire oranı **[Yapılacak]** | ✓ | Varsayılan ayardan gelir (%4); 0–%20 arası, 2 ondalık |
| Not | | Serbest metin |

**Sonuç:** Sevkiyat `DRAFT` (Taslak) olarak açılır, numarası `SVK-YYYY-NNNN` biçiminde verilir.
**[Bugün]** Beklenen net kg tek alan (`planned_net_kg`) olarak tutuluyor; reçete satırları gelince bu alan satır
toplamından hesaplanacak.

**Neden reçete elle?** S3: Otomatik oran yok; müdür Excel'de yaptığı gibi hangi iplik cinsinden ne kadar gideceğini
kendisi yazar. Örnek: "36/1 VİSKON / 75 BEYAZ POLY LYC" kumaşı için 36/1 viskon 19.488 kg, 70 denye poly 13.492 kg,
20 denye likra 2.036 kg.

### 5.3 Çuval atama (Depo)

- Depo, sevkiyata giden çuvalları listeye ekler (okutarak ya da elle yazarak). **[Bugün]** Çuval anahtarı büyük
  harfe çevrilir, aynı anahtar iki kez eklenmez, tek istekte en fazla 2.000 çuval.
- Yanlış eklenen çuval, çıkış tartısından önce çıkarılabilir; satır silinmez, `removed_at` ile işaretlenir.
- Başka lokasyonun ya da başka sevkiyatın çuvalı reddedilir.
- **[Bugün]** Kodsuz çuval listesi: sıra no + çuval kg; sevkiyat dosyasındaki "Ambar çıkış fişi" düğmesi yazdırılabilir fişi açar.

### 5.4 Kantar ve çıkış tartısı (Depo)

**[Bugün]**

1. Depo brüt, dara ve çuval sayısını girer; net = brüt − dara ekranda anında hesaplanır.
2. Kantar fişinin fotoğrafı zorunludur. Fiş okuma açıksa fiş fotoğrafı okunur; okunamazsa üç denemeden sonra
   gerekçeyle devam edilir (yazıcı bozuk, fiş hasarlı…).
3. Kayıtla sevkiyat `AWAITING_DELIVERY` ("Fasoncuya yolda") olur; çuval rezervasyonları kesinleşir.
4. Standart dara tanımı ve "dara sapması" uyarısı yoktur: dara boş araç tartısıdır, araçtan araca değişir.

**[Yapılacak] Kantar uyuşmazlığı kuralı (S4)**

1. Kamyon boş gelir, kantara çıkar → **boş araç tartısı** (dara).
2. Yüklenir, tekrar kantara çıkar → **dolu araç tartısı** (brüt).
3. Net = brüt − dara. |net − beklenen net| > kantar toleransı ise:
   - Sevkiyat **`WEIGHT_MISMATCH`** ("Kantar uyuşmazlığı") durumuna geçer; yola çıkamaz.
   - Müdüre bildirim gider; ekranda beklenen, tartılan ve fark kg görünür.
4. Müdürün seçenekleri:

| Seçenek | Ne olur |
|---|---|
| **İptal et** | Sevkiyat gerekçeyle `CANCELLED` olur, çuvallar serbest kalır. Müdür yeni sevkiyat açar. |
| **Çuval ekle / çıkar** | Sevkiyat `DRAFT`'a döner; depo çuval ekler (eksik yükte) ya da çıkarır (fazla yükte), sonra yeniden tartar. Önceki tartım silinmez, "yerine geçen" olarak saklanır. |
| **Reçeteyi güncelle** (Bölüm 15, A2) | Fark kabul edilecekse müdür reçete kilosunu tartıya eşitler, gerekçe yazar; sevkiyat yola çıkar. |

5. Depo eksik gönderiyorsa (stok yetmedi gibi) nedenini yazar **[Yapılacak]**; neden müdürün kararına girdi olur.

### 5.5 Fasoncuya yolda

- Sevkiyat yoldadır; fasoncunun bakiyesine **henüz yazılmaz** (Bölüm 7, KR-12'deki açık soruya bakın).
- Müdür bu durumda iptal edebilir (ör. araç geri döndü). İptal gerekçe ister.

### 5.6 İrsaliyeyle tamamlama (Müdür) [Bugün]

1. Sevkiyat fasoncuya ulaşınca müdür sevkiyat dosyasında "İrsaliyeyle tamamla" bölümünü açar.
2. İrsaliyenin fotoğrafını ya da PDF'ini yükler (kanıt türü `WAYBILL`, en fazla 20 MB; JPEG, PNG, HEIC, PDF).
3. İrsaliye numarasını yazar (en fazla 64 karakter).
4. "Sevkiyatı tamamla" düğmesi ikisi de girilince açılır.
5. Sunucu irsaliyenin bu sevkiyata ait ve `WAYBILL` türünde olduğunu doğrular; sevkiyat `COMPLETED` olur ve
   kilitlenir. `shipment-completed` olayı yayınlanır.

| Hata | Kod | Kullanıcıya mesaj |
|---|---|---|
| Sevkiyat yolda değil | `INVALID_SHIPMENT_STATUS` | Sevkiyat tamamlanabilecek durumda değil; önce çıkış tartısı girilmeli. |
| Numara boş / uzun | `INVALID_COMPLETION` | İrsaliye numarası geçersiz. |
| Belge yok, başka sevkiyatın ya da türü yanlış | `WAYBILL_EVIDENCE_INVALID` | Yüklenen dosya bu sevkiyatın geçerli irsaliyesi değil. |
| Belge servisi kapalı | `EVIDENCE_SERVICE_UNAVAILABLE` | Fotoğraf servisi yanıt vermiyor; tamamlama yapılmadı. |

**[Yapılacak]** Tamamlama anında sevkiyatın net kilosu, reçete satırlarına göre fasoncu bakiyesine "çıkış" hareketi
olarak yazılır ve sevkiyatın **fire dosyası** açılır ("Kumaş bekleniyor").

### 5.7 Fasoncu bakiyesi [Yapılacak]

Bakiye, değiştirilemez bir **hareket defteri**dir (`balance_movement`). Bakiye her zaman hareketlerin toplamıdır;
elle "bakiye düzeltme" yoktur, hata düzeltme hareketiyle giderilir.

| Hareket türü | Yön | Kaynak | Not |
|---|:-:|---|---|
| `DISPATCH` | + | İrsaliyeyle tamamlanan sevkiyat | Reçete satırı başına bir hareket |
| `FABRIC_RECEIPT` | − | Kumaş girişi | Tüketilen iplik kilosu kadar |
| `YARN_RETURN` | − | Fasoncudan merkeze dönen iplik (S6, S9) | Fire tabanından da düşer |
| `YARN_FROM_SUBCONTRACTOR` | − | Fasoncudan gelen (onun stoğundan alınan) iplik (S6) | Excel'deki eksi satırlar |
| `CLAIM_ADJUSTMENT` | ± | Kalite itirazının karara bağlanması | Bölüm 5.12 |
| `OPENING` | + | Excel'den açılış bakiyesi | Bir kez, geçişte |
| `CORRECTION` | ± | Gerekçeli düzeltme | Eski hareket silinmez, ters hareket yazılır |

Her hareket: fasoncu, iplik cinsi, (varsa) lot, sevkiyat, net kg (3 ondalık), tarih, giren kişi, gerekçe/not.
Birim her yerde **net kg**'dir (S7).

### 5.8 Kumaş girişi [Yapılacak]

1. Kalite (dışarı-kalite) ya da müdür, gelen kumaşı kaydeder: fasoncu, **sevkiyat** (fire sevkiyat üzerinden
   izlendiği için zorunlu, S2), kumaş cinsi, kumaş net kg, top/rulo sayısı (isteğe bağlı), irsaliye no, tarih.
2. Kumaşın hangi iplik cinslerinden ne kadar tükettiği yazılır. Varsayılan: sevkiyatın reçete oranlarıyla dağıtılır,
   kullanıcı değiştirebilir (S3: reçeteyi müdür yazdığı için oran reçeteden gelir).
3. Kayıt fasoncu bakiyesinden `FABRIC_RECEIPT` hareketi düşer ve sevkiyatın fire hesabını günceller.
4. Bir sevkiyata birden çok kumaş girişi yapılabilir (kumaş parça parça gelir).

### 5.9 İplik dönüşü ve fasoncudan gelen iplik [Yapılacak]

- **Dönen iplik (S9):** Fasoncunun kullanmadığı iplik merkeze döner. Depo dönen ipliği tartar, müdür onaylar.
  Bakiyeden düşer, ilgili sevkiyatın fire tabanından da düşer (dönen iplikten kumaş beklenmez).
- **Fasoncudan gelen iplik (S6):** Fasoncu kendi stoğundan iplik gönderebilir; bu da bakiye hareketidir.
- Her iki kayıtta: fasoncu, iplik cinsi, lot (isteğe bağlı), net kg, ilgili sevkiyat (dönüşte zorunlu), irsaliye no,
  kantar fişi fotoğrafı (isteğe bağlı).

### 5.10 Fire hesabı [Yapılacak]

Bkz. Bölüm 8. Fire **sevkiyat** üzerinden hesaplanır (S2); oran sevkiyatta saklanır (S1).

### 5.11 "Fatura edildi" [Yapılacak]

1. Fire dosyasında hatalı kg > 0 ise sistem "faturalanacak kg" ve (kg başı ceza fiyatı tanımlıysa) tutarı gösterir.
2. Müdür ERP'de faturayı keser, sistemde **"Fatura edildi"** işaretler: fatura no, tarih, not (S8).
3. İşaret geri alınamaz; yanlışsa gerekçeli düzeltmeyle açılır.
4. Hatalı kg = 0 olan sevkiyatın fire dosyası müdürün "Kapat" onayıyla kapanır.
5. Kapanmış dosyaya kumaş girişi yapılamaz (yanlış sevkiyata giriş uyarısı verilir).

### 5.12 Kalite itirazı

**[Bugün]**

- Kalite (geçiş döneminde fasoncu hesabı da), **tamamlanmış** sevkiyatın çuvalları için itiraz açar; açıklama ve en
  az bir fotoğraf zorunludur. Pencere tamamlanmadan itibaren 3 iş günüdür.
- Her itiraz Kırmızı bir kalite uyuşmazlığı açar; kalite numune ya da lab kontrolü ister, sonucu girer
  (lab sonucunda belge zorunlu), uyuşmazlık `CLAIM_JUSTIFIED` / `CLAIM_UNJUSTIFIED` ile karara bağlanır.

**[Bugün] Bakiye etkisi:** İtiraz haklı bulunursa itiraz edilen kilo **hem kumaştan hem gönderilen iplikten**
düşülür (`CLAIM_ADJUSTMENT`), böylece fire hesabı sapmaz. Örnek: 1.000 kg iplik, 100 kg itirazlı → fire tabanı
900 kg, beklenen kumaş 900 × 0,96 = 864 kg.

> Not: S5 ile itirazı fasoncu değil fabrika personeli açar. İtirazın fasoncu-çuval yerine **kumaş partisi**
> üzerinden açılması gerekebilir; Bölüm 15, A5.

### 5.13 İptal ve düzeltme [Bugün]

- **İptal:** Müdür ya da admin, `DRAFT`, `WEIGHT_MISMATCH` **[Yapılacak]** veya `AWAITING_DELIVERY` durumundaki
  sevkiyatı gerekçeyle iptal eder. Çuvallar serbest kalır; çuval servisi yanıt vermezse iptal yapılmaz
  (`BAG_SERVICE_UNAVAILABLE`), istek güvenle tekrarlanır. Tamamlanmış sevkiyat iptal edilemez.
- **Düzeltme:** Admin, kilitli sevkiyatta çıkış tartısını gerekçeyle düzeltir; eski değer, yeni değer, kişi ve zaman
  saklanır. **[Yapılacak]** bakiye hareketleri ve kumaş girişleri için de aynı mekanizma.

---

## 6. Sevkiyat durum makinesi

### 6.1 Durumlar

| Kod | Ekranda | Anlamı |
|---|---|---|
| `DRAFT` | Taslak | Açıldı; çuvallar atanıyor |
| `WEIGHT_MISMATCH` **[Yapılacak]** | Kantar uyuşmazlığı | Tartı beklenen yükü tutmadı; müdür kararı bekleniyor |
| `AWAITING_DELIVERY` | Fasoncuya yolda | Çıkış tartısı girildi, yolda |
| `COMPLETED` | Tamamlandı | İrsaliyeyle tamamlandı, kilitli |
| `CANCELLED` | İptal | Gerekçeyle iptal edildi |

Fire dosyasının kendi durumu vardır **[Yapılacak]**: `AWAITING_FABRIC` (Kumaş bekleniyor) → `TO_INVOICE`
(Faturalanacak) → `INVOICED` (Fatura edildi) / `CLOSED` (Kapandı).

### 6.2 Geçişler

| Kimden | Kime | Kim | Ön koşul | Bugün |
|---|---|---|---|:-:|
| — | `DRAFT` | Müdür (Admin, Depo) | Fasoncu etkin | ✓ |
| `DRAFT` | `AWAITING_DELIVERY` | Depo | Çıkış tartısı + kantar fişi; [Yapılacak] net beklenen ile tutuyor | ✓ |
| `DRAFT` | `WEIGHT_MISMATCH` | Sistem | Net ≠ beklenen (tolerans dışı) | ✗ |
| `WEIGHT_MISMATCH` | `DRAFT` | Müdür | "Çuval ekle/çıkar" kararı | ✗ |
| `WEIGHT_MISMATCH` | `AWAITING_DELIVERY` | Müdür | "Reçeteyi güncelle" + gerekçe (A2) | ✗ |
| `DRAFT` / `WEIGHT_MISMATCH` / `AWAITING_DELIVERY` | `CANCELLED` | Müdür, Admin | Gerekçe | ✓ (`WEIGHT_MISMATCH` hariç) |
| `AWAITING_DELIVERY` | `COMPLETED` | Müdür, Admin | İrsaliye belgesi + numarası | ✓ |

Geçiş kuralı veritabanında da (`guard_shipment_update` tetikleyicisi) uygulanır; uygulama hatası bile geçersiz
geçiş yazamaz. Her geçiş `shipment_status_history` tablosuna değiştirilemez satır olarak düşer.

---

## 7. İş kuralları

| No | Kural | Bugün |
|---|---|:-:|
| KR-01 | Sevkiyatı müdür açar; reçete satırlarını elle yazar (S3). | Kısmen (tek kg alanı) |
| KR-02 | Fire oranı varsayılan %4'tür; her sevkiyatta ayrı değiştirilebilir (S1). Değişiklik denetim kaydına düşer. | ✗ |
| KR-03 | Fire sevkiyat üzerinden hesaplanır (S2). | ✗ |
| KR-04 | Bakiye ve fire **net kg** üzerinden tutulur (S7). Kilolar 3 ondalıklı ondalık sayıdır, kayan nokta kullanılmaz. | ✓ (kg tipi) |
| KR-05 | Net = brüt − dara; dara brütten büyük olamaz. | ✓ |
| KR-06 | Çıkış tartısında kantar fişi fotoğrafı zorunludur; okunamazsa gerekçe yazılır. | ✓ |
| KR-07 | Tartı beklenen net'i (tolerans dahil) tutmazsa sevkiyat durur; yalnız müdür karar verir (S4). | ✗ |
| KR-08 | Çuval ekleme/çıkarma yalnız çıkış tartısından önce yapılır. | ✓ |
| KR-09 | Sevkiyat yalnız irsaliye belgesi ve numarasıyla tamamlanır (S10). | ✓ |
| KR-10 | Tamamlanan sevkiyat kilitlenir; değişiklik yalnız gerekçeli düzeltmeyle yapılır. | ✓ |
| KR-11 | İptal gerekçe ister; tamamlanmış sevkiyat iptal edilemez. | ✓ |
| KR-12 | Bakiye, sevkiyat tamamlanınca artar (yolda olan bakiyeye girmez; raporda ayrı "yolda" sütunu). | ✗ |
| KR-13 | Bakiye yalnız hareketlerle değişir; hareket silinmez, ters hareketle düzeltilir. | ✗ |
| KR-14 | Dönen iplik fire tabanından düşer. | ✗ |
| KR-15 | Haklı itiraz kilosu hem kumaştan hem iplikten düşer. | ✓ |
| KR-16 | Hatalı kg > 0 ise sevkiyatın fire dosyası "fatura edildi" işaretlenmeden kapanmaz (S8). | ✗ |
| KR-17 | Fasoncu sisteme giriş yapmaz (S5); fasoncu verisi fabrika personelince girilir. | Geçiş dönemi |
| KR-18 | Fasoncu pasife alınabilir, silinmez; bakiyesi olan fasoncu pasife alınırken uyarı verilir. | Kısmen |
| KR-19 | Her kayıt değişikliği denetim zincirine (hash zinciri) yazılır. | ✓ |
| KR-20 | Kullanıcı yalnız kapsamındaki lokasyonların verisini görür. | ✓ |

---

## 8. Hesaplar

### 8.1 Tanımlar

Bir sevkiyat `s` için:

```
G  = gönderilen net kg            (çıkış tartısı neti; reçete satırları toplamı buna eşittir)
D  = dönen iplik net kg           (YARN_RETURN toplamı, bu sevkiyata bağlı)
I  = haklı itiraz kg              (CLAIM_ADJUSTMENT toplamı)
f  = sevkiyatın fire oranı        (varsayılan 0,04)
K  = gelen kumaş net kg           (FABRIC_RECEIPT toplamı)

Taban            T = G − D − I
Beklenen kumaş   B = T × (1 − f)
Eksik            E = B − (K − I)          → itiraz edilen kumaş kilosu K'dan da düşer
Hatalı kg        H = max(0, E)            (yalnız fire dosyası kapatılırken kesinleşir)
Gerçekleşen fire = (T − (K − I)) / T
Kalan bakiye     R = T − (K − I) − H_fatura_edilen
```

### 8.2 Örnekler

| Durum | G | D | I | f | K | B | Hatalı kg |
|---|---:|---:|---:|---:|---:|---:|---:|
| Fire içinde | 1.000 | 0 | 0 | %4 | 960 | 960 | 0 |
| Fire aşılmış (saha notu) | 1.000 | 0 | 0 | %4 | 900 | 960 | **60** |
| İplik dönmüş | 1.000 | 100 | 0 | %4 | 864 | 864 | 0 |
| Haklı itiraz | 1.000 | 0 | 100 | %4 | 964 (100'ü itirazlı) | 864 | 0 |
| Özel fire oranı | 1.000 | 0 | 0 | %5 | 940 | 950 | 10 |
| Kırıkhan (toplam) | 34.032 | 0 | 0 | %4 | 32.660 | 32.670,72 | 10,72 |

Kırıkhan satırı kabul testi olarak kullanılır: aynı veri girildiğinde gerçekleşen fire **%4,03** çıkmalıdır.

### 8.3 Yuvarlama

Kilolar 3 ondalıkla (gram) saklanır ve hesaplanır; ekranda Türkçe biçimde (1.982,500 kg) gösterilir. Oranlar
2 ondalıklı yüzdedir. Ara hesapta yuvarlama yapılmaz; yalnız sonuç `HALF_UP` ile 3 ondalığa yuvarlanır.

---

## 9. Veri modeli

### 9.1 Bugün (shipment şeması, sadeleştirilmiş)

| Tablo | Amaç | Önemli alanlar |
|---|---|---|
| `shipment` | Sevkiyat | `shipment_no`, `site_id`, `subcontractor_id/title`, `driver_name`, `plate_no`, `status`, `planned_net_kg`, `planned_bag_count`, `e_waybill_no`, `waybill_evidence_id`, `locked_at` |
| `shipment_bag` | Sevkiyattaki çuvallar | `unique_key`, `removed_at` |
| `weighing` | Çıkış tartısı (yalnız `side = 'DISPATCH'`) | `gross_kg`, `tare_kg`, `net_kg`, `bag_count`, `superseded_by` |
| `shipment_status_history` | Durum geçmişi (değiştirilemez) | `from_status`, `to_status`, `changed_by`, `reason` |
| `correction_record` | Gerekçeli düzeltmeler | `entity_type` ∈ {SHIPMENT, SHIPMENT_BAG, WEIGHING} |
| `md_*` | Ana veri | fasoncu, şoför, araç, lokasyon, ürün, kalite sınıfı |

Diğer servisler: `bag` (çuval, lot, rezervasyon), `evidence` (kanıt, `WAYBILL` türü dahil, kantar fişi okuma),
`discrepancy` (uyuşmazlık, kalite itirazı, numune/lab; `comparison_input` salt okunur geçmiş), `reporting`
(okuma modeli), `audit` (hash zinciri).

### 9.2 Eklenenler [Bugün]

| Tablo | Amaç | Alanlar |
|---|---|---|
| `shipment_line` | Reçete satırı | `shipment_id`, `line_no`, `yarn_type_id` ya da `yarn_type_text`, `lot_id?`, `net_kg` |
| `shipment.fire_rate` | Sevkiyatın fire oranı | `numeric(5,4)`, varsayılan ayardan |
| `weight_check` | Boş/dolu araç tartısı ve karar | `empty_kg`, `loaded_kg`, `expected_kg`, `diff_kg`, `decision`, `decided_by`, `reason` |
| `balance_movement` | Bakiye defteri (yalnız ekleme) | `subcontractor_id`, `yarn_type`, `lot?`, `shipment_id?`, `kind`, `net_kg` (işaretli), `occurred_at`, `created_by`, `reason?`, `reverses_id?` |
| `fabric_receipt` | Kumaş girişi | `shipment_id`, `fabric_type`, `net_kg`, `roll_count?`, `waybill_no`, `received_at` |
| `fabric_receipt_yarn` | Kumaşın tükettiği iplik | `fabric_receipt_id`, `yarn_type`, `net_kg` |
| `yarn_return` | Dönen / gelen iplik | `shipment_id?`, `subcontractor_id`, `yarn_type`, `net_kg`, `direction`, `waybill_no?` |
| `fire_settlement` | Sevkiyatın fire dosyası | `shipment_id`, `status`, `expected_fabric_kg`, `shortage_kg`, `invoice_no?`, `invoiced_at?`, `invoiced_by?` |
| `yarn_supplier` | İplik tedarikçisi | `name`, `active` |

Bakiye, `balance_movement` hareketlerinin toplamıdır; rapor servisi olaylarla kendi okuma modelini besler. Ters
hareket kolonu (`reverses_id`) kullanılmadığı için kaldırıldı; düzeltme `CORRECTION` türüyle yeni satır olarak yazılır.

---

## 10. Ekranlar

| Ekran | Rol | İçerik | Bugün |
|---|---|---|:-:|
| Sevkiyat listesi | Hepsi | Durum süzgeci (4 durum), arama, sayfalama | ✓ |
| Yeni sevkiyat | Müdür | Lokasyon, fasoncu, şoför/plaka, reçete satırları, fire oranı | ✓ |
| Sevkiyat dosyası | Hepsi | Başlık (durum, irsaliye no, ilerleme: Taslak → Yolda → Tamamlandı), çıkış tartısı paneli (net, çuval, brüt/dara/net), çuvallar, geçmiş, kanıtlar, düzeltmeler | ✓ |
| Çuval atama | Depo | Anahtar ekle/çıkar, toplu ata | ✓ |
| Çıkış tartısı | Depo | Brüt, dara, çuval sayısı, kantar fişi no, fiş fotoğrafı ve okuma | ✓ |
| İrsaliyeyle tamamla | Müdür | İrsaliye yükleme, irsaliye no, tamamla | ✓ |
| İptal | Müdür, Admin | Gerekçe | ✓ |
| Kantar uyuşmazlığı kararı | Müdür | Beklenen/tartılan/fark, iptal / çuval ekle-çıkar / reçeteyi güncelle | ✓ |
| Kumaş girişi | Kalite, Müdür | Sevkiyat seç, kumaş kg, iplik dağılımı | ✓ |
| İplik dönüşü | Depo, Müdür | Sevkiyat, iplik, kg | ✓ |
| Fire dosyası | Müdür | G, D, I, K, B, hatalı kg, "fatura edildi" | ✓ |
| Fasoncu bakiyesi | Müdür, Genel müdür | İplik cinsi × fasoncu matrisi, hareket dökümü | ✓ |
| Kalite itirazları, uyuşmazlıklar | Kalite, Admin, Müdür | Bugünkü akış | ✓ |
| Tanımlar, Kullanıcılar, Denetim kaydı | Admin | Bugünkü akış | ✓ |
| Saha (fasoncu) | — | Kaldırıldı; fasoncu sisteme girmez | — |

Ekranlar telefon (360 px), tablet (768 px) ve masaüstünde (1280 px) yatay taşmasız açılır; e2e testleri her ekranı
bu üç genişlikte denetler.

---

## 11. Raporlar

| Rapor | Bugün | İçerik |
|---|:-:|---|
| Panel özeti | ✓ | Dönemdeki sevkiyat sayıları (taslak+yolda, tamamlanan, iptal), açık ve süresi geçen uyuşmazlıklar |
| Fasoncu raporu | ✓ | Fasoncu bazında sevkiyat sayısı, uyuşmazlıklı sevkiyat oranı |
| Sevkiyat dökümü | ✓ | Sevkiyat no, fasoncu, durum, çıkış neti, tarih |
| Fasoncu bakiye (basit) | ✓ | Gönderilen / yolda / tamamlanan kg |
| **Bakiye matrisi** | ✓ | İplik cinsi × fasoncu (09.11.2026 tablosunun ekranı), merkez sütunu, toplamlar |
| **Fire raporu** | ✓ | Sevkiyat bazında G, D, I, K, beklenen, gerçekleşen fire %, hatalı kg, fatura durumu |
| **Giden iplik ↔ gelen kumaş** | ✓ | Kırıkhan Excel'inin ekran hali, fasoncu bazında |
| **Faturalanacaklar** | ✓ | Hatalı kg > 0 ve faturalanmamış dosyalar |

Bütün raporlar Excel (.xlsx) ve PDF dosyası olarak indirilir; dönem, lokasyon ve fasoncu süzgeci vardır.

> Not: Çıkış–teslim karşılaştırmasına dayanan "Yeşil/Sarı/Kırmızı bant" ve "en büyük farklar" göstergeleri kaldırıldı;
> yerlerini fire göstergeleri aldı. Uyuşmazlık servisi yalnızca kalite itirazı (`QUALITY`) üretir.

---

## 12. Bu sürümde koddan kaldırılanlar

Bu sürümle birlikte gelen değişiklik (`claude/project-thread-guqkg0` dalı) aşağıdakileri kaldırır.

### 12.1 Davranış

| Kaldırılan | Yerine |
|---|---|
| Şoför rolü (`DRIVER`), şoför saha ekranı, "Yükü teslim aldım", "Vardım" beyanı | — (şoför yalnız sevkiyatta ad/plaka) |
| Mühür numarası (çıkışta ve teslimde), tek kullanımlık mühür kuralı, mühür fotoğrafı | — |
| Fasoncunun 5 adımlı kör teslim tutanağı, teslim tartısı, teslimde çuval okutma | Müdürün irsaliyeyle tamamlaması |
| Rastgele kontrol (gizli sayım) | — |
| Otomatik çıkış–teslim karşılaştırması ve varış takibi (discrepancy-service) | Kantar kuralı + fire hesabı (yapılacak) |
| Şoför raporu, şoför–fasoncu eşleşme raporu | Fasoncu raporu |
| Şoför giriş hesabı açma | — |
| Durumlar `LOADED`, `IN_TRANSIT`, `CHECKING`, `IN_REVIEW` | `AWAITING_DELIVERY` |
| Kanıt türleri: şoför/teslim fotoğrafları, imzalı teslim belgesi, teslim kantar fişi | `WAYBILL` (irsaliye) |

### 12.2 Veritabanı (geri alınamaz migration'lar)

| Servis | Migration | Etki |
|---|---|---|
| shipment | `V202611101000__simplified_shipment_flow.sql` | `seal`, `driver_declaration`, `delivery_receipt`, `delivery_scan` tabloları ve teslim tartımları **silinir**; ara durumlar `AWAITING_DELIVERY` olur; ilgili düzeltme kayıtları silinir; `random_check_*` sütunları düşer; `waybill_evidence_id` eklenir |
| evidence | `V202611101000__waybill_evidence.sql` | `WAYBILL` türü eklenir; kaldırılan türlerin (araç plakası, mühür, teslim kantar fişi, teslim çuvalları, imzalı teslim belgesi) kayıtları ve teslim fişi okumaları **silinir**; nesne deposundaki dosyalar elle temizlenir |
| discrepancy | — (migration yok) | Teslim karşılaştırması ve varış takibi kodu kaldırıldı; tablolar ve `comparison_input` geçmiş için salt okunur kalır |
| reporting | `V202611101000__simplified_shipment_flow.sql` | Durumlar yeni akışa çevrilir, `driver_id` sütunu ve `pair_daily_stats` tablosu silinir |

> **Üretime almadan önce veritabanı yedeği alınmalıdır.** Silinen teslim verisi geri getirilemez.

### 12.3 API

- Kaldırıldı: `POST /api/v1/shipments/{id}/pickup-confirmation`, `/arrival-declaration`, `/delivery-receipt`,
  `POST /api/v1/master-data/drivers/{id}/login-account`, `GET /api/v1/reports/drivers`, `/reports/pairs`.
- Eklendi: `POST /api/v1/shipments/{id}/completion` (`Idempotency-Key`, `{waybillNo, waybillEvidenceId}`).
- Değişti: Çıkış tartısı isteğinde `sealNo` yok; sevkiyat görünümlerinde teslim ve mühür alanları yok, fasoncu
  görünümünde çıkış tartısı var.
- Olaylar: `shipment-dispatched.v2` (mühürsüz), `shipment-completed.v1` (irsaliyeyle); `shipment-arrived`,
  `delivery-received` ve `comparison-completed` sözleşmeleri kaldırıldı.

---

## 13. Korunanlar ve geçiş dönemi

### 13.1 Korunanlar

Giriş/Keycloak, roller ve izin matrisi, tanımlar, sevkiyat açma, çuval atama, çıkış tartısı ve kantar fişi okuma,
iptal, gerekçeli düzeltme, kanıt yükleme altyapısı, kalite itirazı ve numune/lab, uyuşmazlık yönetimi, denetim
zinciri, rapor servisi iskeleti, Excel/PDF dışa aktarma, ön yüz kabuğu ve kurulum.

### 13.2 Geçiş dönemi (sonraki PR'larda kaldırılacak)

| Konu | Neden hemen kaldırılmadı |
|---|---|
| Fasoncu rolü, fasoncu saha ekranı, fasoncu giriş hesabı | Kalite itirazı bugün fasoncu kapsamına bağlı; itiraz akışı fabrika personeline taşınınca kaldırılacak (S5) |
| Kalite itirazının `comparison_input` okuması | İtiraz motoru yeni modele (tamamlanan sevkiyat) taşınınca kalkacak |
| Kantar fişi okuma kontrolleri (K1–K5) | Kantar uyuşmazlığı kuralının parçası olarak yeniden kurulacak |
| Panel "bant" göstergeleri | Fire raporuyla değişecek |
| Şoför ve araç ana verisi | Bilgi amaçlı kalıyor; kalıcı |

### 13.3 Çuval kodu

Sahada "çuvala kod gerekmez" denildi. Bugün çuval atama hâlâ çuval anahtarıyla çalışıyor (lot ve stok takibi buna
bağlı). Öneri: anahtarı **sistemin üretmesi** (kullanıcı yazmasın), çıktıda sıra no + kg gösterilmesi; QR etiket
basımı isteğe bağlı kalsın. Karar Bölüm 15, A4.

---

## 14. Yol haritası

| Sıra | İş | İçerik | Boyut |
|---|---|---|---|
| ✓ | **Sadeleştirme (bu sürüm)** | Şoför, mühür, kör teslim, teslim tartısı, otomatik karşılaştırma kaldırıldı; irsaliyeyle tamamlama | — |
| ✓ | Reçete ve fire oranı | `shipment_line`, `fire_rate`, yeni sevkiyat ekranı, ayarlar | K–O |
| ✓ | Kantar uyuşmazlığı | Boş/dolu araç tartısı, `WEIGHT_MISMATCH`, müdür kararı, eksik gönderim nedeni, bildirim | O |
| ✓ | Bakiye defteri | `balance_movement`, tamamlamada `DISPATCH` hareketi, bakiye görünümü | O–B |
| ✓ | Kumaş girişi | Kayıt, iplik dağılımı, bakiyeden düşme | O |
| ✓ | İplik dönüşü / gelen iplik | Kayıt, bakiye ve fire tabanı | K–O |
| ✓ | Fire dosyası ve "fatura edildi" | Hesap, faturalanacaklar listesi, işaret | O |
| ✓ | Kalite itirazının taşınması | Fabrika personeli açar, haklı itiraz bakiye düzeltmesi; fasoncu rolü ve saha ekranı kaldırılır | O |
| ✓ | Raporlar | Bakiye matrisi, fire raporu, giden iplik ↔ gelen kumaş, faturalanacaklar; Excel/PDF | O–B |
| ✓ | Açılış bakiyesi | 09.11.2026 tablosu ve fasoncu sayfalarından içe aktarma, Kırıkhan kabul testi | K–O |
| ✓ | Genel müdür rolü, iplik tedarikçisi | Salt okuma rolü, tedarikçi tanımı ve lota bağlanması | K |

Boyut: K ≈ 1–2 gün, O ≈ 3–5 gün, B ≈ 1–2 hafta.

---

## 15. Açık sorular

| No | Soru | Önerilen varsayım |
|---|---|---|
| A1 | Kantar farkında tolerans olsun mu (ör. ±10 kg), yoksa her fark mı durdursun? | Ayar olarak eklensin, varsayılan 0 kg (S4'e uygun) |
| A2 | Müdür küçük farkı reçeteyi güncelleyerek kabul edebilsin mi, yoksa yalnız iptal / çuval ekle-çıkar mı? | Gerekçeyle kabul edilebilsin |
| A3 | Depo sevkiyat açabilmeye devam etsin mi? | Hayır; yalnız müdür ve admin |
| A4 | Çuval anahtarı tamamen kalksın mı, yoksa sistem üretsin mi? | Sistem üretsin, kullanıcı görmesin |
| A5 | Kalite itirazı çuval mı, kumaş partisi mi üzerinden açılsın? | Kumaş girişi üzerinden, kg ile |
| A6 | Hatalı kg için kg başı ceza fiyatı sistemde tutulsun mu? | Fasoncu ya da sevkiyat bazında isteğe bağlı alan |
| A7 | Bakiye sevkiyat tamamlanınca mı, çıkış tartısında mı artmalı? | Tamamlanınca; "yolda" ayrı sütun |
| A8 | Merkez fabrikanın kendi kumaş üretimi bakiyede "merkez fasoncu" gibi izlensin mi? | Evet, merkez bir lokasyon olarak |
| A9 | Fasoncular arası iplik transferi var mı (önceki notlarda geçiyor)? | Varsa iki bakiye hareketi (çıkan / giren) |

---

## 16. Kabul senaryoları

**Bugün çalışanlar (otomatik testlerle doğrulanıyor)**

1. *Depo çıkış tartısı:* Taslak sevkiyatta depo brüt 2.000, dara 17,5, çuval 40 girer ve kantar fişini çeker →
   net 1.982,5 kg görünür, kayıtla durum "Fasoncuya yolda" olur.
2. *Fiş zorunlu:* Fiş çekilmeden kaydet → "Devam etmek için kantar fişinin fotoğrafı gerekli."
3. *İrsaliyeyle tamamlama:* Yoldaki sevkiyatta müdür irsaliye PDF'ini yükler, "IRS-2026-77" yazar, tamamlar →
   durum "Tamamlandı", form kaybolur, irsaliye no başlıkta görünür.
4. *Tamamlama düğmesi:* İrsaliye yüklenmeden numara yazılırsa düğme kapalı kalır.
5. *Yetki:* Depo tamamlama formunu görmez; kalite yeni sevkiyat açamaz; müdür taslakta tartı girişi görmez ama
   iptali görür.
6. *İptal:* Admin taslak sevkiyatı gerekçeyle iptal eder → durum "İptal".
7. *Düzeltme:* Admin tamamlanan sevkiyatta brütü 2.010 yapar, gerekçe yazar → eski ve yeni değer düzeltmeler
   sekmesinde görünür; gerekçesiz düzeltme gönderilmez.

**Sonradan eklenen özellikler için kabul ölçütleri (artık çalışıyor)**

8. *Kantar uyuşmazlığı:* Reçete 1.000 kg, tartı neti 980 kg, tolerans 0 → durum "Kantar uyuşmazlığı", müdüre
   bildirim; depo yola çıkaramaz.
9. *Çuval ekleme:* Müdür "çuval ekle/çıkar" seçer → durum "Taslak"; depo 1 çuval ekleyip yeniden tartar, net
   1.000 → "Fasoncuya yolda"; ilk tartım "yerine geçen" olarak geçmişte görünür.
10. *Bakiye:* 1.000 kg (viskon 600, poly 400) sevkiyat tamamlanır → fasoncu bakiyesi viskon +600, poly +400.
11. *Fire içinde:* Aynı sevkiyata 960 kg kumaş girilir → hatalı kg 0, gerçekleşen fire %4,00; dosya müdür onayıyla
    kapanır.
12. *Fire aşımı:* 900 kg kumaş → hatalı kg 60, dosya "Faturalanacak"; müdür fatura no girip "Fatura edildi"
    işaretler → dosya kapanır, işaret geri alınamaz.
13. *Özel fire:* Fire %5 girilmiş sevkiyatta 940 kg kumaş → hatalı kg 10.
14. *Dönen iplik:* 100 kg iplik döner → taban 900, beklenen 864 kg.
15. *Haklı itiraz:* 100 kg itiraz haklı bulunur → iplik ve kumaştan 100'er kg düşer; fire hesabı değişmez.
16. *Kırıkhan:* Excel'deki giden 34.032 kg ve gelen 32.660 kg girildiğinde gerçekleşen fire %4,03, hatalı kg 10,72.
17. *Bakiye matrisi:* Açılış bakiyesi aktarıldıktan sonra matris 09.11.2026 tablosuyla (toplam 262.153 kg, merkez
    173.505 kg) birebir tutar.
