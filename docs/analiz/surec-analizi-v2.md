# İplik Sevkiyat ve Fason Bakiye Sistemi: Süreç Analizi v2

| | |
|---|---|
| Sürüm | 2.0 |
| Tarih | 10.11.2026 (son güncelleme 18.11.2026) |
| Durum | Onaylanan saha cevaplarıyla yazıldı; anlatılan akışın tamamı kodda çalışıyor. Bölüm 15'teki sorular karara bağlandı |
| Önceki sürüm | Ön analiz v1.1 (güven ve kanıt odaklı akış: şoför, mühür, kör teslim, otomatik uyuşmazlık) |
| Kaynaklar | Fabrika ziyareti el notları, Ambar Çıkış Fişi No 0239, 09.11.2026 "iplik cinsi × fasoncu" stok tablosu, `FASON SİPRİŞ FORMU.xlsx`, Kırıkhan giden iplik / gelen kumaş Excel'i, saha analizi raporu v1.0 ve S1–S10 cevapları |

Bu doküman, sistemin bugün neyi nasıl yaptığını anlatır; anlatılan özelliklerin tamamı kodda çalışmaktadır. Bölüm 12
bu sürümle koddan neyin kaldırıldığını, Bölüm 14 yapılan işlerin sırasını verir. Doküman ile ekrandaki davranış
ayrışırsa ekrandaki davranış doğrudur; ekran adları ve düğme adları uygulamadaki yazılışla verilmiştir.

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
15. Karara bağlanan sorular
16. Kabul senaryoları

---

## 1. Amaç ve kapsam

### 1.1 Sorun

Fabrika ipliği fasonculara (dış üreticilere) gönderir; fasoncu bu iplikle kumaş üretip geri yollar. Sistem öncesinde bu takip
Excel'de yapılıyordu:

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
| **Reçete (sevkiyattaki iplikler)** | Müdürün sevkiyatı açarken elle yazdığı "hangi iplik cinsinden ne kadar" listesi; ekranda "Gönderilecek iplikler" (sevkiyat dosyasında "Sevkiyattaki iplikler") diye geçer (S3). |
| **Beklenen net kg** | Reçete satırlarının toplamı; kantar kontrolünün hedefi. |
| **Brüt / dara / net** | Kantar fişindeki toplam ağırlık / araç (ve ambalaj) ağırlığı / yükün kendisi. Net = brüt − dara. |
| **Kantar uyuşmazlığı** | Çıkış tartısının neti beklenen net kg'yi tutmadığında oluşan durum; sevkiyat durur (S4). |
| **İrsaliye** | Sevkiyatın fasoncuya ulaştığını gösteren belge; müdür yükler ve numarasını yazar (S10). |
| **Fire oranı** | İplikten kumaşa geçişte kabul edilen kayıp. Varsayılan %4, sevkiyata özel değiştirilebilir (S1, S2). |
| **Beklenen kumaş** | (Gönderilen net − dönen iplik net) × (1 − fire oranı). |
| **Hatalı kg** | Beklenen kumaştan eksik gelen kilo; fasoncuya fatura edilir. Ekranda "Faturalanacak kilo" diye yazar. |
| **Bakiye** | Bir fasoncuda, iplik cinsi bazında, henüz kumaşa dönmemiş net iplik kilosu. |
| **Bakiye hareketi** | Bakiyeyi değiştiren her kayıt: çıkış, kumaş girişi, iplik dönüşü, başka fasoncuya sevk edilen iplik, fatura, açılış (eski kayıtlarda itiraz düzeltmesi). |
| **Fatura edildi** | Müdürün, ERP'de kesilen faturayı Faturalama ekranının Faturalanacaklar sekmesinden girmesi (fatura no, tarih, iplik başına kilo); parça parça girilebilir. Faturalanan kilo fasoncunun bakiyesinden düşer; faturalanacak kilo bitince sevkiyatın fire dosyası kapanır. |
| **Merkez iplik stoku** | Merkez depoda duran iplik (fasoncudaki iplik değil). "Merkez iplik stoku" ekranında iplik cinsi başına kilo olarak görünür. Fasoncu bakiyesinden ayrıdır. |
| **Kapıdan aktarma** | İpliğin depoya girmeden tedarikçinin kamyonundan fasoncunun kamyonuna aktarılması; sevkiyat açılırken "Kapıdan aktarma" seçilir ve tedarikçi listeden seçilir. |

---

## 3. Roller ve yetkiler

### 3.1 Roller

| Rol | Kim | Sistemde ne yapar |
|---|---|---|
| **Müdür** (`MANAGER`) | Fason işlerinden sorumlu müdür | Sevkiyat açar (gönderilecek iplikler, fiyat, fire oranı, sevkiyat tarihi), sevkiyat tarihini ve şoför/aracı günceller; kantar uyuşmazlığında karar verir (Çuval ekle / çıkar, İplik güncelle, Sevkiyatı iptal et); irsaliyeyi yükleyip sevkiyatı tamamlar, sevkiyatı iptal eder; fire oranını değiştirir; Faturalanacaklar'dan "Fatura edildi" girer ve faturalanacak kilosu kalmamış fire dosyasını kapatır; kalite itirazını yalnızca "Haklı" ya da "Haksız" diye karara bağlar; Tanımlar ve Partiler ekranlarını yönetir, kanıtı geçersiz kılar; raporları, tutarları (TL), fasoncu bakiyesini ve merkez iplik stokunu görür. **Kumaş girişi ve iplik dönüşü kaydedemez**, kalite itirazı açamaz |
| **Depo** (`WAREHOUSE`) | Merkez depo sorumlusu | Çuvalları sevkiyata ekler (çuval kilosu yazarak), çıkış tartısını (boş araç, dolu araç, çuval sayısı) ve kantar fişi fotoğrafını girer, tartı beklenenin altındaysa "Eksik kilo nedeni" yazar; **fasoncudan dönen ipliği kaydeder**; merkeze gelen ipliği stoğa yazar (tedarikçi seçmek zorunlu); merkez iplik stokunu görür. Sevkiyat açamaz, bakiye ve raporları görmez |
| **Kalite** (`QUALITY`) | Fasoncu kalite ve merkez kalite (kullanıcı açılırken "Çalışma alanı" seçilir: Merkez fabrika ya da Fasoncular) | Kalite itirazı açar, uyuşmazlığı üstlenir ("Bana ata"); **karar veremez** (laboratuvar ya da numune kontrolü adımı yoktur). Fasoncudan gelen kumaşı yalnızca **fasoncu kalite** girer; merkez fabrikanın kumaşını ve merkez iplik stokunu yalnızca **merkez kalite** görür ve girer. Partilerin kalitesini kilitler; raporları ve fasoncu bakiyesini görür |
| **Genel müdür** (`GENERAL_MANAGER`) | Üst yönetim | Raporları (tutarlar dahil), fasoncu bakiyesini, Faturalama ekranını (Faturalanacaklar ve Faturalananlar) ve merkez iplik stokunu salt okunur görür; işlem yapmaz |
| **Admin** (`ADMIN`) | Sistem yöneticisi | Yalnızca sistemi yönetir: Kullanıcılar (hesap açma, rol, şifre verme), Görünüm (firma adı, renk, logo), Yetkiler ve Denetim kaydı. Sevkiyat, uyuşmazlık, rapor, bakiye ve merkez stok ekranlarını görmez; Tanımlar ve Partiler müdürdedir, admin açamaz |
| ~~Şoför~~ | — | **Kaldırıldı.** Şoför ve araç yalnızca sevkiyatın üzerinde bilgi olarak yazılır |
| ~~Fasoncu~~ | — | **Kaldırıldı.** S5 gereği fasoncu sisteme girmez; rolü ve saha ekranı yoktur, yalnızca Tanımlar'daki "Fasoncular" kaydıdır |

Yukarıdaki yetkiler varsayılandır; yönetici bunları **Yetkiler** ekranından rol bazında değiştirebilir (admin'in kendi yetkileri sabittir).

### 3.2 İzin matrisi

Endpoint'ler rol değil izin ister; matrisin tek kaynağı `libs/common-security/.../RolePermissionMatrix.java`'dır. Her rolün
varsayılan yetkileri asistana doğrudan koddan verilir ("rol-yetkileri" bölümü); bu yüzden tablo burada tekrarlanmaz,
doküman ile kod ayrışırsa o liste doğrudur. Matristeki temel ilkeler:

- Sevkiyatı yalnız müdür açar; depo sevkiyat açamaz, yalnız çuvalları ve çıkış tartısını girer (karar Bölüm 15, A3).
- Sevkiyatı müdür tamamlar ve iptal eder; kantar uyuşmazlığı kararını da müdür verir.
- Kumaş girişini yalnız kalite, fasoncudan iplik dönüşünü yalnız depo kaydeder; müdür ikisini de kaydedemez.
- Kalite itirazını yalnız kalite açar; itirazı yalnız müdür "Haklı" / "Haksız" diye karara bağlar. Kalite karar veremez.
- Fire oranını değiştirmek ve "Fatura edildi" girmek yalnız müdürdedir.
- Tutarları (TL) yalnız müdür ve genel müdür görür.
- Genel müdür yalnız okur: rapor, bakiye, merkez iplik stoku.
- Admin yalnız sistemi yönetir (kullanıcılar, ayarlar, yetkiler, denetim kaydı); iş ekranlarını görmez.
- Yetkiler ekranından yapılan değişiklik, o roldeki herkese yeniden giriş gerekmeden hemen yansır.

### 3.3 Sonradan eklenen izinler

| İzin | Kim (varsayılan) | Ne için |
|---|---|---|
| `FABRIC_RECEIPT_RECORD` | Kalite | Kumaş girişi (müdürden alındı, 15.11.2026) |
| `YARN_RETURN_RECORD` | Depo | Fasoncudan iplik dönüşü (müdürden alındı, 15.11.2026) |
| `FIRE_RATE_EDIT` | Müdür | Sevkiyatın fire oranını değiştirmek |
| `INVOICE_MARK` | Müdür | "Fatura edildi" girmek, fire dosyasını kapatmak |
| `BALANCE_VIEW` | Müdür, Genel müdür, Kalite | Fasoncu bakiyesi, Faturalama (Faturalanacaklar, Faturalananlar) ve fire dosyası |
| `WEIGHT_MISMATCH_RESOLVE` | Müdür | Durmuş sevkiyatı yeniden tartıya açmak |
| `DISCREPANCY_DECIDE` | Müdür | Kalite itirazını "Haklı" / "Haksız" diye kapatmak |
| `AMOUNT_VIEW` | Müdür, Genel müdür | Raporlarda tutar (TL) bilgisi |
| `CENTRAL_STOCK_VIEW` / `CENTRAL_STOCK_RECORD` / `CENTRAL_PRODUCTION_RECORD` | Müdür, Genel müdür, Depo, Kalite görür; depo iplik girişini, merkez kalite kumaş girişini yazar | Merkez iplik stoku |
| `PERMISSION_MANAGE` | Yalnız admin | Yetkiler ekranı |

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
    X -->|Müdür: iplik güncelle, kilolar tartıya eşit| D
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

Şemadaki tüm adımlar (A–K, iptal ve kantar uyuşmazlığı kararı) kodda çalışır. Merkezden çıkan sevkiyatta iplik merkez
iplik stokundan düşer; sevkiyat başka bir fasoncunun elindeki iplikten ya da kapıdan aktarmayla da açılabilir (5.2).

---

## 5. Süreç adımlarının ayrıntısı

### 5.1 Tanımlar (ana veri)

| Tanım | Durum | Not |
|---|---|---|
| Fasoncu | ✓ (ad tekil, pasife alınır, silinmez) | Giriş hesabı açma kaldırıldı (S5) |
| Lokasyon (merkez depo) | ✓ | — |
| Araç (plaka) | ✓ (TR plaka deseni, tekil) | Bilgi amaçlı kalır |
| Şoför | ✓ | Bilgi amaçlı kalır; giriş hesabı yok |
| İplik cinsi / ürün (Tanımlar > İplikler) | ✓ (aynı ad ikinci kez eklenemez) | Sevkiyatta listeden seçilir, listede yoksa yazılır (S3); merkeze iplik girişinde ve merkez kumaş girişinde yalnızca listeden seçilir |
| Parti (parti no), kalite sınıfı | ✓ | **İplik tedarikçisi zorunludur**; tedarikçisiz parti açılmaz. Parti no büyük harfe çevrilir; başka iplik ya da başka tedarikçiye ait parti no reddedilir |
| İplik tedarikçisi (Tanımlar > İplik tedarikçileri) | ✓ | Şireci, Karesi, Teksmar, Migiteks, Creora… Merkeze iplik girişinde, parti açarken ve kapıdan aktarmada seçilir |
| Fire oranı varsayılanı | ✓ | Ayar: `%4`; yalnız yönetici değiştirebilir. Arayüzde bu ayar için ayrı bir ekran yoktur; yeni sevkiyat formu varsayılanı bu ayardan alır, müdür sevkiyat başına değiştirir |
| Kantar toleransı | ✓ | Ayar: varsayılan 0 kg (Bölüm 15, A1); yalnız yönetici değiştirebilir, arayüzde ayar ekranı yoktur |

### 5.2 Sevkiyat oluşturma (Müdür)

Müdür Sevkiyatlar ekranında "Yeni sevkiyat" der.

**Giriş alanları**

| Alan | Zorunlu | Kural |
|---|:-:|---|
| Alıcı fasoncu | ✓ | Etkin fasoncu |
| İplik nereden çıkıyor? | ✓ | "Merkez stok" (varsayılan), "Başka fasoncu" ya da "Kapıdan aktarma" (aşağıya bakın) |
| Şoför, araç | | İsteğe bağlı, sonradan da atanır; şoför listesinde "Merkez şoförleri" ve "Fasoncunun şoförleri" ayrı başlıklarla gelir |
| Gönderilecek iplikler (reçete) | ✓ (en az 1 satır) | Her satır: iplik cinsi (listeden seçilir, listede yoksa yazılır), isteğe bağlı parti no, net kg (> 0, 3 ondalık), **birim fiyat (TL/kg; zorunlu, boşsa "Fiyat yazılmalı")**. "Sipariş bilgileri" sekmesinde isteğe bağlı: kumaş cinsi, LYC, tüp / may / açık en, iplik uzunluğu / LYC, puss-fine |
| Fire oranı | ✓ | Varsayılan ayardan gelir (%4); 0–%20 arası, 2 ondalık |
| Sevkiyat tarihi | ✓ | Bugünden önce olamaz. Depo bu tarihten önce çuval atayamaz, çıkış tartısı ve çıkış fotoğrafı giremez. Taslakta müdür "Tarihi değiştir" ile güncelleyebilir |
| İlgili kişi, sipariş no | | Yalnızca "Sipariş formu PDF" çıktısında yer alır |

**İplik nereden çıkıyor?**

| Seçenek | Ne olur |
|---|---|
| Merkez stok | İplik merkez iplik stokundan düşer (5.6) |
| Başka fasoncu | "Gönderen fasoncu" seçilir (alıcıyla aynı olamaz). Gönderen fasoncunun elinde istenen iplik yoksa taslak açılmaz ("Gönderen fasoncunun elinde yeterli iplik yok"). Sevkiyat tamamlanınca gönderenin bakiyesi düşer, alıcının bakiyesi artar |
| Kapıdan aktarma | "Tedarikçi" listeden seçilir (zorunlu); geliş irsaliye no isteğe bağlıdır. İplik depoya girmeden kamyondan kamyona geçer, merkez stok eksiye düşmez; merkez iplik stoku ekranında "Kapıdan aktarılan" olarak görünür |

**Sonuç:** Sevkiyat `DRAFT` (Taslak) olarak açılır, numarası `SV-YYYYAAGG-XXXXXXXX` biçiminde verilir (tarih ve 8 karakter).
Beklenen net kg, gönderilecek iplik satırlarının toplamıdır. Merkez stoktan çıkan sevkiyatta stok yetmeyecekse form ve
sevkiyat dosyası "Merkez depoda iplik yetmiyor" uyarısını gösterir (kapatılabilir; 5.6). Sevkiyat dosyasında ve yeni
sevkiyat ekranında **"Sipariş formu PDF"** düğmesi sipariş formunu (firma, kumaş cinsi, LYC, en, iplik uzunluğu, puss-fine, kg, fiyat) PDF olarak indirir.

**Neden reçete elle?** S3: Otomatik oran yok; müdür Excel'de yaptığı gibi hangi iplik cinsinden ne kadar gideceğini
kendisi yazar. Örnek: "36/1 VİSKON / 75 BEYAZ POLY LYC" kumaşı için 36/1 viskon 19.488 kg, 70 denye poly 13.492 kg,
20 denye likra 2.036 kg.

### 5.3 Çuval atama (Depo)

- Çuvalda kod ya da QR yoktur ve okutulmaz; depo sevkiyat dosyasının "Çuvallar" sekmesinde her çuvalın kilosunu yazıp
  "Listeye ekle" der, sonra "N çuvalı ekle" ile kaydeder. Her kilo bir çuvaldır ve sevkiyattaki son sıradan devam eden
  sıra numarasını alır (Çuval 1, Çuval 2…).
- Çuval yalnızca **Taslak** sevkiyatta eklenip çıkarılır; yanlış eklenen çuval çıkış tartısından önce "Sevkiyattan çıkar"
  ile çıkarılır. Sevkiyat tarihi gelmeden çuval eklenemez.
- Sevkiyat dosyasındaki "Ambar çıkış fişi" düğmesi sıra no + kg listesini yazdırılabilir fişle açar.

### 5.4 Kantar ve çıkış tartısı (Depo)

1. Taslak sevkiyatta çuvallar eklendikten sonra depo "Çıkış tartısını gir" der (çuval yokken düğme görünmez, "önce
   çuvalları ekleyin" yazar; sevkiyat tarihi gelmeden de girilemez). Önce **boş araç tartısı (dara)**, sonra yükleme
   sonrası **dolu araç tartısı (brüt)** ve çuval sayısı yazılır; net = brüt − dara ekranda anında hesaplanır.
2. **Kantar fişinin fotoğrafı zorunludur** ("Devam etmek için kantar fişinin fotoğrafı gerekli."). Fiş sistem tarafından
   okunmaz ve "yazıcı bozuk" gibi gerekçeyle fotoğrafsız devam edilemez. Fotoğraf, tartı kaydedilince yüklenir; sevkiyat
   yola çıkmadan önce depo, kendi yüklediği fişi "Kantar fişini yenile" ile değiştirebilir (yeni fiş öncekini geçersiz kılar).
3. Kayıttan önce iki denetim vardır; biri tutmazsa hiçbir şey kaydedilmez ve depo çuvalları ya da tartıyı düzeltir:
   - Tartıdaki çuval sayısı sevkiyata eklenen çuval sayısına eşit olmalı ("Çuval sayısı tutmuyor").
   - Eklenen çuvalların kilo toplamı araç net kilosunu tutmalı, kantar toleransı kadar fark olabilir ("Çuvallar araç kilosunu tutmuyor").
4. Net kilo beklenenin altındaysa depo **"Eksik kilo nedeni"** yazmak zorundadır; neden müdürün kararına girdi olur ve
   sevkiyat dosyasında "Eksik gönderim nedeni" olarak görünür.
5. Depo "İleri" der ve onay penceresinde beklenen ile tartılan neti görür. Net, beklenen net ile tutuyorsa
   "Onayla, yola çıkar" der; sevkiyat `AWAITING_DELIVERY` ("Fasoncuya yolda") olur. Tutmuyorsa pencere "Onayla, müdüre
   bildir" der ve sevkiyat kantar uyuşmazlığına düşer (5.4.1).
6. Standart dara tanımı ve "dara sapması" uyarısı yoktur: dara boş araç tartısıdır, araçtan araca değişir.

**5.4.1 Kantar uyuşmazlığı kuralı (S4)**

1. Kamyon boş gelir, kantara çıkar → **boş araç tartısı** (dara).
2. Yüklenir, tekrar kantara çıkar → **dolu araç tartısı** (brüt).
3. Net = brüt − dara. |net − beklenen net| > kantar toleransı (varsayılan 0 kg) ise:
   - Sevkiyat **`WEIGHT_MISMATCH`** ("Kantar uyuşmazlığı") durumuna geçer; yola çıkamaz.
   - Müdür, sağ üstteki zilde "Kantar uyuşmazlığı: karar bekleyen sevkiyatlar" listesini görür; sevkiyat dosyasında
     beklenen, tartılan ve fark kg yazar.
4. Müdürün seçenekleri ("Müdür kararı: bu sevkiyat için ne yapılsın?"; her kararda gerekçe yazılır):

| Seçenek | Ne olur |
|---|---|
| **Sevkiyatı iptal et** | Sevkiyat gerekçeyle `CANCELLED` olur, çuvallar serbest kalır. Müdür yeni sevkiyat açar. |
| **Çuval ekle / çıkar** | Sevkiyat `DRAFT`'a döner; depo dosyada "Müdürün isteği" notunu görür, çuval ekler (eksik yükte) ya da çıkarır (fazla yükte), sonra çıkış tartısını yeniden girer. Önceki tartım silinmez, "yerine geçen" olarak saklanır. |
| **İplik güncelle** (Bölüm 15, A2) | "İplik listesini düzelt" sayfası açılır; müdür iplik kilolarını tartıya eşitler (en çok kantar toleransı kadar fark kalabilir), gerekçe yazar ve "Kaydet ve yola çıkar" der; kilolar tartıyı tutmuyorsa kaydedilmez. Sevkiyat yola çıkar. |

### 5.5 Fasoncuya yolda

- Sevkiyat yoldadır; fasoncunun bakiyesine **henüz yazılmaz** (Bölüm 7, KR-12); bakiye ekranında ve raporda "Yolda" sütununda ayrı görünür.
- Müdür bu durumda iptal edebilir (ör. araç geri döndü). İptal gerekçe ister.

### 5.6 İrsaliyeyle tamamlama (Müdür)

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

Tamamlama anında sevkiyatın net kilosu, reçete satırlarına göre fasoncu bakiyesine "çıkış" hareketi olarak yazılır ve
sevkiyatın **fire dosyası** açılır ("Kumaş bekleniyor"). Ayrıca:

- **Merkez stoktan çıkan sevkiyatta** iplik merkez iplik stokundan düşer. Merkez stok eksiye düşmez: stok yetmiyorsa
  müdür sevkiyat açarken ve dosyada "Merkez depoda iplik yetmiyor" uyarısını görür; tamamlarken soru çıkmaz, eksik kilo
  kadar merkeze "Sistem girişi" yazılır ve stok sıfırda kalır.
- **Başka fasoncudan** açılan sevkiyatta gönderen fasoncunun bakiyesi düşer, alıcının bakiyesi artar.
- **Kapıdan aktarmada** iplik merkez stoka girip aynı anda çıkar; stok değişmez, "Kapıdan aktarılan" toplamı artar.

### 5.7 Fasoncu bakiyesi

Bakiye, değiştirilemez bir **hareket defteri**dir (`balance_movement`). Bakiye her zaman hareketlerin toplamıdır;
hareket silinmez ve elle "bakiye düzeltme" yoktur.

| Hareket türü (ekranda) | Yön | Kaynak | Not |
|---|:-:|---|---|
| `DISPATCH` (Çıkış) | + | İrsaliyeyle tamamlanan sevkiyat | Reçete satırı başına bir hareket |
| `FABRIC_RECEIPT` (Kumaş girişi) | − | Kumaş girişi | Tüketilen iplik kilosu kadar |
| `YARN_RETURN` (İplik dönüşü) | − | Fasoncudan depoya dönen iplik (S6, S9); kaydı yalnızca depo girer | Fire tabanından da düşer |
| `TRANSFER_OUT` | − | Başka fasoncuya sevk edilen iplik | Gönderen fasoncunun bakiyesinden düşer; alıcıda "Çıkış" olarak artar |
| `INVOICE` (Fatura) | − | Faturalama ekranının Faturalanacaklar sekmesinden girilen fatura | Faturalanan iplik kilosu kadar; hatalı gelen kumaşın itirazı karşılayan kısmı ikinci kez düşmez (5.12) |
| `YARN_FROM_SUBCONTRACTOR` (Gelen iplik) | − | Fasoncudan gelen iplik (S6) | 15.11.2026'dan beri yeni kayıt açılmaz (depo fasoncu stoğundan iplik girmez); yalnızca eski kayıtlar geçmişte kalır ("Eski kayıt: fasoncudan gelen iplik") |
| `CLAIM_ADJUSTMENT` (İtiraz düzeltmesi) | ± | Eski kayıtlar (yeni itirazlar bakiye hareketi yazmaz) | Bölüm 5.12 |
| `OPENING` (Açılış) | + | Excel'den açılış bakiyesi | Bir kez, geçişte (Raporlar > Bakiye matrisi) |
| `CORRECTION` (Düzeltme) | ± | Eski kayıtlar | Yeni düzeltme kaydı açan bir ekran yoktur (Düzeltmeler bölümü kaldırıldı) |

Bakiye ekranı **Fasoncu bakiyesi** menüsündedir: iplik cinsi × fasoncu matrisi, "Yolda" sütunu ve hareket dökümü.
Merkez depodaki iplik burada görünmez, o **Merkez iplik stoku** ekranındadır (5.14).

Her hareket: fasoncu, iplik cinsi, (varsa) lot, sevkiyat, net kg (3 ondalık), tarih, giren kişi, gerekçe/not.
Birim her yerde **net kg**'dir (S7).

### 5.8 Kumaş girişi

1. **Fasoncu kalite** (çalışma alanı "Fasoncular" olan kalite kullanıcısı), fasoncudan gelen kumaşı tamamlanmış sevkiyatın
   dosyasında **"Kumaş girişi"** sekmesinden kaydeder. Müdür kumaş girişi yapamaz; merkez kalite de sevkiyata kumaş
   giremez. Alanlar: kumaş cinsi, kumaş net kg, top sayısı (isteğe bağlı), irsaliye no. Kumaş, fire sevkiyat üzerinden
   izlendiği için hep bir sevkiyata girilir (S2).
2. Formun üstünde tek cümle durur: "Fasoncuya … kg iplik gönderildi, şimdiye kadar … kg kumaş geldi; en çok … kg daha
   girebilirsiniz." Kumaş kilosu bu kalanı aşamaz; faturası kesilen kilo da gönderilen iplikten tüketilmiş sayılır.
3. Kumaşın hangi iplik cinslerinden ne kadar tükettiği yazılır; iplik cinsi **sevkiyattaki ipliklerden seçilir** (sevkiyatta
   olmayan iplik seçilemez, bir satırda seçilen iplik diğer satırda sunulmaz). Dağılımın toplamı kumaş kilosuna eşit
   olmalıdır. Boş bırakılırsa kilo sevkiyattaki iplik oranına göre dağıtılır (S3).
4. "Kumaş girişini kaydet" önce özet ve "Kayıt sonradan değiştirilemez." uyarısını gösterir; yalnızca **"Evet, kaydet"**
   kaydeder, "Vazgeç" hiçbir şey yazmaz. Kayıt "Kumaş girişi kaydedildi." der.
5. Kayıt fasoncu bakiyesinden `FABRIC_RECEIPT` hareketi düşer ve sevkiyatın fire hesabını günceller; ilk kumaş girişinden
   sonra fire dosyası "Faturalanacak" durumuna geçer.
6. Bir sevkiyata birden çok kumaş girişi yapılabilir (kumaş parça parça gelir). Kapanmış fire dosyasına kumaş girişi yapılamaz.

### 5.9 İplik dönüşü

- **Dönen iplik (S9):** Fasoncunun kullanmadığı iplik merkeze döner. **Depo** kaydeder (sevkiyat dosyası > "İplik dönüşü"
  sekmesi); müdür ve admin kaydedemez. Kayıt fasoncunun bakiyesinden ve ilgili sevkiyatın fire tabanından düşer (dönen
  iplikten kumaş beklenmez).
- Alanlar: **iplik cinsi ve parti no, o sevkiyatta gönderilenlerden seçilir** (sevkiyatta olmayan iplik girilemez; bir ipliğin
  dönen toplamı o sevkiyatta gönderilen kilosunu aşamaz), net kg, irsaliye no (isteğe bağlı). Yalnızca tamamlanmış
  sevkiyata ve kapanmamış fire dosyasına girilir.
- "Fasoncudan gelen iplik" (S6) seçeneği kaldırılmıştır; formda yön alanı yoktur. Eski kayıtlar yalnızca geçmişte "Eski
  kayıt: fasoncudan gelen iplik" olarak görünür.

### 5.10 Fire hesabı

Bkz. Bölüm 8. Fire **sevkiyat** üzerinden hesaplanır (S2); oran sevkiyatta saklanır (S1). Müdür, açık fire dosyasında
"Fire oranını değiştir" ile oranı (gerekçe yazarak) değiştirebilir; değişiklik denetim kaydına düşer. Fire dosyasında
sayılar düz cümleyle verilir ("Fasoncuya … iplik gönderildi, … kumaş geldi; gelmesi gereken …"); ayrıntılar "Hesap
ayrıntıları" altındadır.

### 5.11 "Fatura edildi"

Fire dosyasının durumları: "Kumaş bekleniyor" → "Faturalanacak" → "Faturalandı" ya da "Kapandı".

1. Fire dosyasında faturalanacak kilo (hatalı kg ve haklı itirazdan gelen kilo) varsa dosya **Faturalama** ekranının **Faturalanacaklar** sekmesinde
   listelenir (menü: Faturalama; Raporlar'da da Faturalanacaklar sekmesi vardır). Faturası girilen kayıtlar aynı ekranın
   **Faturalananlar** sekmesine düşer: fatura no, tarih, fasoncu, sevkiyat ve kilo; tarih ve fasoncuya göre süzülür, Excel'e
   alınabilir. Müdür, kalite ve genel müdür ekranı görür.
2. Müdür ERP'de faturayı keser, sistemde satırdaki **"Fatura bilgisini gir"** der: fatura no, fatura tarihi, **iplik başına kilo**
   ve isteğe bağlı not yazar, **"Fatura edildi"** der (S8). Aynı form sevkiyat dosyasındaki "Fire dosyası" sekmesinde de
   bulunur.
3. Kayıttan önce onay penceresi çıkar: "Faturası kesilen kilonun kumaşı artık gelmeyecek mi?" Yalnızca **"Evet, fatura
   edildi"** kaydeder; "Vazgeç" hiçbir şey yazmaz. Kayıttan sonra o kilo için kumaş girilemez ve kayıt geri alınamaz.
4. Fatura **parça parça** girilebilir: kalandan az kilo yazılırsa dosya açık kalır, "Faturası kesilen" görünür ve
   "Faturalanacak kilo" (kalan) azalır; kalanın tamamı faturalanınca dosya kapanır. Kalandan fazla kilo yazılamaz.
5. Faturalanan kilo fasoncunun iplik bakiyesinden düşer (hareketlerde "Fatura"), gerçekleşen fireden de düşer (fire %
   azalır) ve kumaş girişi sınırından da düşer.
6. Faturalanacak kilosu kalmamış dosya müdürün **"Kapat"** onayıyla kapanır; faturalanacak kilosu olan dosya kapatılamaz.
7. Kapanmış dosyaya kumaş girişi yapılamaz. Sonradan haklı bir kalite itirazı gelirse dosya yeniden "Faturalanacak" olur (5.12).

### 5.12 Kalite itirazı

**Kim açar, nasıl:** Yalnızca **kalite** itiraz açar (müdür itirazları görür ama "Kalite itirazı aç" düğmesi yoktur; fasoncu sisteme girmez, S5).
İtiraz, **tamamlanmış** sevkiyatın dosyasında "Kalite itirazı" sekmesinden açılır; "İtiraz edilen kilo", "Sorunu anlatın"
açıklaması ve en az bir fotoğraf zorunludur. **İtiraz için süre sınırı yoktur** (S18); ekranda yalnızca 3 iş günü
sonuçlandırma hedef tarihi gösterilir.

**Kumaş girişi şartı:** İtiraz yalnızca girilmiş kumaş için açılır. İtiraz edilebilecek kilo, sevkiyata girilen kumaştan
haksız bulunmamış itirazların kilosu çıkarılarak bulunur ("Girilen kumaşa göre en çok … kg yazabilirsiniz."); kumaş girişi
yoksa form açılmaz ve "Kalite itirazı için önce kumaş girişi yapılmalı" yazar (`QUALITY_CLAIM_FABRIC_NOT_RECEIVED`). Girilen
kumaşın tamamı için itiraz açıldıysa yeni itiraz açılamaz.

**Uyuşmazlık ve karar:** Her itiraz Kırmızı bir kalite uyuşmazlığı açar (hedef kapanış 5 iş günü). Kalite uyuşmazlığı
"Bana ata" ile üstlenir; laboratuvar ya da numune kontrolü adımı yoktur ("Kontrol iste" ve sonuç girme kaldırıldı).
Kalite **karar veremez**. Kararı yalnızca **müdür** verir ve yalnızca iki seçenek vardır: **"Haklı"**
ya da **"Haksız"**; karar notu boş olamaz, "Karara bağla" → "Karar geri alınamaz." → **"Evet, kararı kaydet"** ile kaydedilir.
"Haklı, faturalandı" ya da "Sorumluluk atandı" diye
bir karar yoktur; fatura bu ekranda girilmez, Faturalama ekranının Faturalanacaklar sekmesinden girilir (5.11).

**Bakiye etkisi:** İtiraz "Haklı" bulunursa itiraz edilen kilo **hem kumaştan hem gönderilen iplikten**
sayılır, böylece fire hesabı sapmaz (fire tabanı T = G − D − I). Kilo bakiyeden **hemen düşülmez**; dosyada
"İtirazdan faturalanacak" olarak faturalanacak kiloya eklenir (`fire_claim`) ve dosya **Faturalama** ekranındaki Faturalanacaklar sekmesine düşer.
"Haksız" kararda fasoncunun ipliğinde değişiklik olmaz, uyuşmazlık yalnızca kapanır. Dosya kapanmış ya da faturalanmışsa haklı
itiraz dosyayı yeniden faturalanacak yapar (süre sınırı yoktur). Hatalı gelen kumaş, girilen kumaş K'nın içindedir ve
bakiyeden K olarak zaten düşülmüştür; bu yüzden müdür fatura girince faturanın itirazı karşılayan kısmı bakiyeyi **ikinci kez
düşürmez** (`fire_invoice.claim_kg`), yalnızca eksik kilo `INVOICE` ile düşer. Fatura önce itirazdan kalan kiloyu
karşılar, fazlası eksik kumaşa gider; itirazı karşılayan kısım fire yüzdesine ve kumaş girişi sınırına girmez.
Nihai bakiye = G − D − K − faturalanan eksik kilo. Örnek: 1.000 kg iplik, 100 kg itirazlı → fire tabanı 900 kg,
beklenen kumaş 900 × 0,96 = 864 kg; faturalanacak 100 kg, bakiye etkisi 0.

### 5.13 İptal ve kilitleme

- **İptal:** Müdür, `DRAFT`, `WEIGHT_MISMATCH` veya `AWAITING_DELIVERY` durumundaki sevkiyatı gerekçeyle iptal eder
  (sevkiyat dosyasında "İptal et"; kantar uyuşmazlığında "Sevkiyatı iptal et" kararı). Çuvallar serbest kalır.
  Tamamlanmış sevkiyat iptal edilemez ("Tamamlanmış sevkiyat iptal edilemez.").
- **Kilitleme:** Tamamlanan sevkiyat kilitlenir. Sevkiyat dosyasındaki "Düzeltmeler" bölümü ve gerekçeli düzeltme kaydı
  **kaldırılmıştır**; kilitli kaydı değiştiren bir ekran ya da izin yoktur. Yanlış kantar fişi için tek yol, sevkiyat yola
  çıkmadan "Kantar fişini yenile"dir; yanlış kanıt müdür tarafından "Geçersiz kıl" ile geçersiz sayılır (dosya silinmez).

### 5.14 Merkez iplik stoku

**Merkez iplik stoku** menüsü, merkez depodaki (fasoncuda olmayan) ipliği gösterir; "Fasoncu bakiyesi"nden ayrıdır. Müdür,
genel müdür, depo ve kalite (merkez kalite) görür. Sekmeler:

- **Eldeki iplik:** iplik cinsi başına eldeki kg, toplam, arama ve sıralama, Excel/PDF indirme. Üstteki kartlar:
  "Toplam iplik stoku", "Üretilen kumaş", "Gelen ipliğin dağılımı" (merkez depoda, fasoncularda, kapıdan aktarılan, merkez
  üretime verilen). Eksiye düşen iplik "Eksi stok" rozetiyle işaretlenir. Merkezden fasoncuya sevk edilen iplik
  sevkiyat tamamlanınca stoktan düşer.
- **İplik girişi** (depo yazar): "Merkeze iplik girişi" formu. İplik cinsi, **tedarikçi** ve gelen net kg **zorunludur**; parti no,
  tedarikçi irsaliye no ve not isteğe bağlıdır. İplik cinsi ve tedarikçi Tanımlar'daki aktif kayıtlardan seçilir. Parti no yazılırsa
  Partiler'deki partiye bağlanır, yoksa açılır; ilk kez yazılan parti için kalite sınıfı seçilir.
- **Kumaş girişi** (merkez kalite yazar): merkez fabrikada üretilen kumaş; kullanılan iplikler fire eklenerek merkez
  stoktan düşer.
- **Tedarikçi raporu:** Stok ekranında ve ana sayfada "Tedarikçiden gelen iplik" tablosu, tedarikçi başına toplam gelen kg'yi
  ve altında iplik cinslerini gösterir; Excel'de ayrı sayfadır.

---

## 6. Sevkiyat durum makinesi

### 6.1 Durumlar

| Kod | Ekranda | Anlamı |
|---|---|---|
| `DRAFT` | Taslak | Açıldı; çuvallar atanıyor |
| `WEIGHT_MISMATCH` | Kantar uyuşmazlığı | Tartı beklenen yükü tutmadı; müdür kararı bekleniyor |
| `AWAITING_DELIVERY` | Fasoncuya yolda | Çıkış tartısı girildi, yolda |
| `COMPLETED` | Tamamlandı | İrsaliyeyle tamamlandı, kilitli |
| `CANCELLED` | İptal | Gerekçeyle iptal edildi |

Fire dosyasının kendi durumu vardır: `AWAITING_FABRIC` (Kumaş bekleniyor) → `TO_INVOICE`
(Faturalanacak) → `INVOICED` (Faturalandı) / `CLOSED` (Kapandı). Kapanmış dosyaya haklı kalite itirazı gelirse dosya
yeniden `TO_INVOICE` olur.

### 6.2 Geçişler

| Kimden | Kime | Kim | Ön koşul |
|---|---|---|---|
| — | `DRAFT` | Müdür | Fasoncu etkin, gönderilecek iplikler, sevkiyat tarihi |
| `DRAFT` | `AWAITING_DELIVERY` | Depo | Çıkış tartısı + kantar fişi fotoğrafı; çuvallar ve çuval sayısı tutuyor; net beklenen ile tutuyor |
| `DRAFT` | `WEIGHT_MISMATCH` | Sistem | Net ≠ beklenen (tolerans dışı) |
| `WEIGHT_MISMATCH` | `DRAFT` | Müdür | "Çuval ekle / çıkar" kararı + gerekçe |
| `WEIGHT_MISMATCH` | `AWAITING_DELIVERY` | Müdür | "İplik güncelle" + gerekçe, kilolar tartıya eşit (A2) |
| `DRAFT` / `WEIGHT_MISMATCH` / `AWAITING_DELIVERY` | `CANCELLED` | Müdür | Gerekçe |
| `AWAITING_DELIVERY` | `COMPLETED` | Müdür | İrsaliye belgesi + numarası |

Geçiş kuralı veritabanında da (`guard_shipment_update` tetikleyicisi) uygulanır; uygulama hatası bile geçersiz
geçiş yazamaz. Her geçiş `shipment_status_history` tablosuna değiştirilemez satır olarak düşer.

---

## 7. İş kuralları

| No | Kural |
|---|---|
| KR-01 | Sevkiyatı müdür açar; gönderilecek iplikleri (reçete) elle yazar (S3). |
| KR-02 | Fire oranı varsayılan %4'tür; her sevkiyatta ayrı değiştirilebilir (S1). Değişiklik denetim kaydına düşer. |
| KR-03 | Fire sevkiyat üzerinden hesaplanır (S2). |
| KR-04 | Bakiye ve fire **net kg** üzerinden tutulur (S7). Kilolar 3 ondalıklı ondalık sayıdır, kayan nokta kullanılmaz. |
| KR-05 | Net = brüt − dara; dara brütten büyük olamaz. |
| KR-06 | Çıkış tartısında kantar fişi fotoğrafı zorunludur; fotoğrafsız, gerekçeyle devam edilemez. |
| KR-07 | Tartı beklenen net'i (tolerans dahil) tutmazsa sevkiyat durur; yalnız müdür karar verir (S4). Net beklenenin altındaysa depo eksik kilo nedenini yazmak zorundadır. |
| KR-08 | Çuval ekleme/çıkarma yalnız Taslak sevkiyatta, çıkış tartısından önce yapılır. Tartıdaki çuval sayısı ve çuvalların kilo toplamı tartıyla tutmalıdır. |
| KR-09 | Sevkiyat yalnız irsaliye belgesi ve numarasıyla tamamlanır (S10). |
| KR-10 | Tamamlanan sevkiyat kilitlenir; kilitli kaydı değiştiren bir düzeltme mekanizması yoktur. |
| KR-11 | İptal gerekçe ister; tamamlanmış sevkiyat iptal edilemez. |
| KR-12 | Bakiye, sevkiyat tamamlanınca artar (yolda olan bakiyeye girmez; ekranda ve raporda ayrı "Yolda" sütunu). |
| KR-13 | Bakiye yalnız hareketlerle değişir; hareket silinmez. |
| KR-14 | Dönen iplik fire tabanından düşer. |
| KR-15 | Haklı itiraz kilosu hem kumaştan hem iplikten sayılır (fire tabanı değişmez); bakiyeden hemen düşmez, faturalanacak kiloya eklenir. |
| KR-16 | Faturalanacak kilosu olan fire dosyası, kilo faturalanmadan kapanmaz (S8). Fatura Faturalama ekranının Faturalanacaklar sekmesinden, parça parça girilir; kayıt geri alınamaz. |
| KR-17 | Fasoncu sisteme giriş yapmaz (S5); fasoncu verisi fabrika personelince girilir. |
| KR-18 | Fasoncu pasife alınabilir, silinmez; bakiyesi olan fasoncu pasife alınırken uyarı verilir ("Yine de pasife al"). |
| KR-19 | Her kayıt değişikliği denetim zincirine (hash zinciri) yazılır. |
| KR-20 | Kullanıcı yalnız kapsamındaki lokasyonların verisini görür. |
| KR-21 | Kalite itirazı yalnızca girilmiş kumaş için açılır; itiraz süresi sınırı yoktur (S18). Kararı yalnız müdür verir ("Haklı" / "Haksız"). |
| KR-22 | Merkez iplik stoku eksiye düşmez; stok yetmeyen sevkiyat tamamlanırken eksik kadar "Sistem girişi" yazılır. İplik girişinde tedarikçi zorunludur. |

---

## 8. Hesaplar

### 8.1 Tanımlar

Bir sevkiyat `s` için:

```
G  = gönderilen net kg            (çıkış tartısı neti; reçete satırları toplamı buna eşittir)
D  = dönen iplik net kg           (YARN_RETURN toplamı, bu sevkiyata bağlı)
I  = haklı itiraz kg              (CLAIM_ADJUSTMENT toplamı + fire_claim toplamı)
f  = sevkiyatın fire oranı        (varsayılan 0,04)
K  = gelen kumaş net kg           (FABRIC_RECEIPT toplamı)

Taban            T = G − D − I
Beklenen kumaş   B = T × (1 − f)
Eksik            E = B − (K − I)          → itiraz edilen kumaş kilosu K'dan da düşer
Hatalı kg        H = max(0, E)            (yalnız fire dosyası kapatılırken kesinleşir)
Gerçekleşen fire = (T − (K − I)) / T
Kalan bakiye     R = T − (K − I) − H_fatura_edilen
```

Faturalanan kilo da kumaş gibi hesaba katılır: gerçekleşen fire azalır ve girilebilecek kumaş sınırından düşer.
Ekranda G "Gönderilen iplik", D "Dönen iplik", K "Gelen kumaş", T "Fire hesabına esas iplik", B "Gelmesi gereken kumaş",
H "Faturalanacak kilo" olarak yazar.

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

### 9.1 Sevkiyat şeması (sadeleştirilmiş)

| Tablo | Amaç | Önemli alanlar |
|---|---|---|
| `shipment` | Sevkiyat | `shipment_no`, `site_id`, `subcontractor_id/title`, `driver_name`, `plate_no`, `status`, `planned_net_kg`, `fire_rate`, `e_waybill_no`, `waybill_evidence_id`, `locked_at`, sevkiyat tarihi, iplik kaynağı (merkez / başka fasoncu / kapıdan aktarma) |
| `shipment_line` | Reçete satırı | `shipment_id`, `line_no`, `yarn_type_id` ya da `yarn_type_text`, `lot_id?`, `net_kg`, sipariş ayrıntıları ve birim fiyat |
| `shipment_bag` | Sevkiyattaki çuvallar | `seq_no` (sıra no), `weight_kg`, `removed_at` |
| `weighing` | Çıkış tartısı | `gross_kg`, `tare_kg`, `net_kg`, `bag_count`, `superseded_by` |
| `weight_check` | Kantar uyuşmazlığı kaydı ve müdür kararı | `empty_kg`, `loaded_kg`, `expected_kg`, `diff_kg`, `shortfall_reason`, `decision`, `decided_by`, `reason` |
| `shipment_status_history` | Durum geçmişi (değiştirilemez) | `from_status`, `to_status`, `changed_by`, `reason` |
| `md_*` | Ana veri | fasoncu, şoför, araç, lokasyon, iplik (ürün), kalite sınıfı, parti, iplik tedarikçisi |
| `app_user`, `role_permission`, `staff_work_area` | Giriş hesapları, rol izinleri (Yetkiler ekranı), kalite çalışma alanı | |
| `app_setting` | Varsayılan fire oranı, kantar toleransı, görünüm ayarları | |

Diğer servisler: `evidence` (kanıt, `WAYBILL` türü dahil; kantar fişi okuma kaldırıldı), `discrepancy` (uyuşmazlık, kalite
itirazı), `reporting` (okuma modeli), `audit` (hash zinciri). Çuval servisi (`bag`) kaldırıldı;
çuvallar sevkiyat şemasında `shipment_bag` olarak tutulur.

### 9.2 Bakiye, kumaş, fire ve stok tabloları

| Tablo | Amaç | Alanlar |
|---|---|---|
| `balance_movement` | Bakiye defteri (yalnız ekleme) | `subcontractor_id`, `yarn_type`, `lot?`, `shipment_id?`, `kind`, `net_kg` (işaretli), `occurred_at`, `created_by`, `reason?` |
| `fabric_receipt` | Kumaş girişi (iplik dağılımı `yarns` jsonb alanındadır) | `shipment_id`, `fabric_type`, `net_kg`, `roll_count?`, `waybill_no`, `received_at`, `yarns` |
| `yarn_return` | Dönen iplik (eski kayıtlarda gelen iplik) | `shipment_id?`, `subcontractor_id`, `yarn_type`, `lot?`, `net_kg`, `direction`, `waybill_no?` |
| `fire_settlement` | Sevkiyatın fire dosyası | `shipment_id`, `status`, `fire_rate`, `sent_kg`, son fatura bilgisi |
| `fire_invoice` | Parça parça girilen fatura kayıtları (yalnız ekleme) | `shipment_id`, `invoice_no`, `invoiced_on`, `kg`, `claim_kg`, `note?` |
| `fire_claim` | Haklı bulunan kalite itirazının faturalanacağa eklenen kilosu | `discrepancy_id`, `shipment_id`, `claimed_kg` |
| `fire_rate_change` | Fire oranı değişiklik geçmişi | |
| `central_stock_movement` | Merkez iplik stoku hareketleri | `yarn_type`, `kind` (iplik girişi, fasoncuya sevk, merkez kumaş üretimi), `net_kg`, `note?` |
| `md_yarn_supplier` | İplik tedarikçisi | `name`, `active` |

Bakiye, `balance_movement` hareketlerinin toplamıdır; rapor servisi olaylarla kendi okuma modelini besler. Ters
hareket kolonu (`reverses_id`) kaldırıldı. Gerekçeli düzeltme kayıtları (`correction_record`) ve "Düzeltmeler" özelliği
kaldırıldı.

---

## 10. Ekranlar

Yan menü ("Menü") müdürde "Günlük işler" ve "Tanımlar ve ayarlar" diye iki gruba ayrılır. Menüde görünen ekranlar rolün
yetkilerine göre değişir.

| Ekran | Rol | İçerik |
|---|---|---|
| Sevkiyatlar (liste) | Sevkiyat görenler | Durum süzgeci (5 durum: Taslak, Kantar uyuşmazlığı, Fasoncuya yolda, Tamamlandı, İptal), sayfalama |
| Yeni sevkiyat | Müdür | Alıcı fasoncu, iplik nereden çıkıyor (merkez stok / başka fasoncu / kapıdan aktarma), şoför/araç, gönderilecek iplikler ve fiyatlar, fire oranı, sevkiyat tarihi, "Sipariş formu PDF" |
| Sevkiyat dosyası | Sevkiyat görenler | Başlık (durum, irsaliye no, ilerleme: Taslak → Yolda → Tamam). Sekmeler: **Kilolar** (çıkış tartısı, sevkiyattaki iplikler, Ambar çıkış fişi, Sipariş formu PDF), **Çuvallar**, **Kanıtlar** ("Fotoğraf ve belgeler"), **Geçmiş**, **Kalite itirazı**, **Kumaş girişi**, **İplik dönüşü**, **Fire dosyası**. "Düzeltmeler" sekmesi yoktur |
| Çuvallar sekmesi | Depo | Çuval kilosu yaz, "Listeye ekle", "N çuvalı ekle"; "Sevkiyattan çıkar" |
| Çıkış tartısı | Depo | Boş araç (dara), dolu araç (brüt), çuval sayısı, eksik kilo nedeni, kantar fişi fotoğrafı; "İleri" → "Onayla, yola çıkar" / "Onayla, müdüre bildir" |
| İrsaliyeyle tamamla | Müdür | İrsaliye yükleme, irsaliye no, "Sevkiyatı tamamla" |
| İptal | Müdür | Gerekçe |
| Kantar uyuşmazlığı kararı | Müdür | Beklenen/tartılan/fark ve eksik gönderim nedeni; "Çuval ekle / çıkar", "İplik güncelle" ("İplik listesini düzelt" sayfası), "Sevkiyatı iptal et" |
| Kumaş girişi (sekme) | Fasoncu kalite kaydeder; bakiye görenler okur | Kumaş cinsi, kg, top sayısı, irsaliye no, iplik dağılımı; "Evet, kaydet" onayı |
| İplik dönüşü (sekme) | Depo kaydeder | Sevkiyatta gönderilen iplik/parti seç, net kg, irsaliye no |
| Fire dosyası (sekme) | Bakiye görenler; müdür işlem yapar | Gönderilen, dönen, gelen kumaş, gelmesi gereken kumaş, faturalanacak kilo, "Hesap ayrıntıları"; müdür: "Fire oranını değiştir", "Fatura bilgisini gir" / "Kapat" |
| Faturalama | Müdür, Kalite, Genel müdür görür; müdür fatura girer | İki sekme. Faturalanacaklar: faturalanacak kilosu kalan dosyalar; "Fatura bilgisini gir" → "Fatura edildi" → "Evet, fatura edildi". Faturalananlar: girilmiş faturalar (fatura no, tarih, fasoncu, sevkiyat, kilo); tarih ve fasoncu süzgeci; Excel |
| Fasoncu bakiyesi | Müdür, Genel müdür, Kalite | İplik cinsi × fasoncu matrisi, "Yolda" sütunu, hareket dökümü |
| Merkez iplik stoku | Müdür, Genel müdür, Depo, Merkez kalite | Eldeki iplik, İplik girişi (depo), Kumaş girişi (merkez kalite), tedarikçiden gelen iplik tablosu (5.14) |
| Uyuşmazlıklar | Kalite, Müdür | Kalite itirazı uyuşmazlıkları; kalite "Bana ata", müdür "Haklı" / "Haksız" kararı; müdür ana sayfasında "Karar bekleyen uyuşmazlıklar" listesi |
| Raporlar | Kalite, Müdür, Genel müdür | Bölüm 11 |
| Partiler | Müdür, Kalite | Parti ekle/düzenle (iplik tedarikçisi zorunlu), "Kaliteyi kilitle" |
| Tanımlar | Müdür | Fasoncular, Şoförler, Araçlar, İplikler, Kalite sınıfları, İplik tedarikçileri |
| Kullanıcılar, Görünüm, Yetkiler, Denetim kaydı | Admin (Denetim kaydını müdür ve kalite de görür) | Hesap açma ve şifre verme, firma adı/renk/logo, rol izinleri |
| Yardım | Herkes | Sağ altta "Yardım" balonu; yalnızca bilgi verir, kayıt değiştirmez |
| Saha (fasoncu) | — | Kaldırıldı; fasoncu sisteme girmez |

Ekranlar telefon (360 px), tablet (768 px) ve masaüstünde (1280 px) yatay taşmasız açılır; e2e testleri her ekranı
bu üç genişlikte denetler.

---

## 11. Raporlar

| Rapor | Durum | İçerik |
|---|:-:|---|
| Özet | ✓ | Stok durumu (merkez depoda iplik, fasoncularda iplik, fasoncudan gelen kumaş, merkezde üretilen kumaş), dönemdeki sevkiyat sayıları, açık ve süresi geçen uyuşmazlıklar; müdür ve genel müdür için "Tutar bilgisi" (fasoncudaki ipliğin değeri, faturalanacak tutar, yolda olan ipliğin tutarı) |
| Fasoncu raporu | ✓ | Fasoncu bazında sevkiyat sayısı, uyuşmazlıklı sevkiyat oranı |
| Sevkiyat dökümü | ✓ | Sevkiyat no, fasoncu, durum, çıkış neti, tarih |
| Fasoncu bakiye (basit) | ✓ | Gönderilen / yolda / tamamlanan kg |
| **Bakiye matrisi** | ✓ | İplik cinsi × fasoncu (09.11.2026 tablosunun ekranı), "Yolda" sütunu, toplamlar; açılış bakiyesi buradan yüklenir |
| **Fire raporu** | ✓ | Sevkiyat bazında G, D, I, K, beklenen, gerçekleşen fire %, hatalı kg, fatura durumu |
| **Giden iplik ↔ gelen kumaş** | ✓ | Kırıkhan Excel'inin ekran hali, fasoncu bazında |
| **Faturalama** | ✓ | Faturalanacaklar sekmesi: faturalanacak kilosu kalmış dosyalar; Faturalananlar sekmesi: girilmiş faturalar |

Bütün raporlar Excel (.xlsx) ve PDF dosyası olarak indirilir; dönem (7/30/90 gün ya da tarih aralığı) süzgeci vardır.
**Tedarikçi raporu** Merkez iplik stoku ekranındadır: "Tedarikçiden gelen iplik" tablosu (5.14).

> Not: Çıkış–teslim karşılaştırmasına dayanan "Yeşil/Sarı/Kırmızı bant" ve "en büyük farklar" göstergeleri kaldırıldı;
> yerlerini fire göstergeleri aldı. Uyuşmazlık servisi yalnızca kalite itirazı (`QUALITY`) üretir.

---

## 12. Bu sürümde koddan kaldırılanlar

Sadeleştirme sürümüyle aşağıdakiler koddan kaldırıldı (sonraki sürümlerde kaldırılanlar 12.4'tedir).

### 12.1 Davranış

| Kaldırılan | Yerine |
|---|---|
| Şoför rolü (`DRIVER`), şoför saha ekranı, "Yükü teslim aldım", "Vardım" beyanı | — (şoför yalnız sevkiyatta ad/plaka) |
| Mühür numarası (çıkışta ve teslimde), tek kullanımlık mühür kuralı, mühür fotoğrafı | — |
| Fasoncunun 5 adımlı kör teslim tutanağı, teslim tartısı, teslimde çuval okutma | Müdürün irsaliyeyle tamamlaması |
| Rastgele kontrol (gizli sayım) | — |
| Otomatik çıkış–teslim karşılaştırması ve varış takibi (discrepancy-service) | Kantar kuralı + fire hesabı |
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

### 12.4 Sonraki sürümlerde kaldırılanlar

| Kaldırılan | Yerine / durum |
|---|---|
| Fasoncu rolü ve fasoncu saha ekranı, fasoncu giriş hesabı | Fasoncu yalnızca ana veridir; kalite itirazını kalite açar |
| Çuval servisi, çuval anahtarı (kod/QR), çuval rezervasyonu | Çuval sıra no + kg olarak yazılır (5.3) |
| Kantar fişi okuma (fiş fotoğrafından kilo okuma) ve "yazıcı bozuk" gerekçesiyle devam | Fiş fotoğrafı zorunlu, okuma yok (5.4) |
| Sevkiyat dosyasındaki "Düzeltmeler" bölümü, gerekçeli düzeltme kaydı, `CORRECTION_CREATE` izni | Yok; tamamlanan sevkiyat kilitlidir (5.13) |
| Uyuşmazlık kararlarından "Haklı, faturalandı" ve "Sorumluluk atandı", sorumlu taraf alanı | Yalnızca müdürün "Haklı" / "Haksız" kararı (5.12) |
| Kalite itirazı için 3 iş günü süre sınırı | Süre sınırı yok (S18) |
| Numune / laboratuvar kontrolü | Yoktur; kalite yalnızca "Bana ata" ile üstlenir, karar müdürdedir |
| "Fasoncudan gelen iplik" kaydı | Yalnızca "İplik dönüşü"; eski kayıtlar geçmişte durur |
| Admin'in sevkiyat, uyuşmazlık, rapor, bakiye, tanım ve parti yetkileri | Admin yalnız sistemi yönetir; tanımlar ve partiler müdürde |

---

## 13. Korunanlar ve geçiş dönemi

### 13.1 Korunanlar

Giriş (uygulamanın kendi giriş sistemi), roller ve izin matrisi, tanımlar, sevkiyat açma, çuval atama, çıkış tartısı,
iptal, kanıt yükleme altyapısı, kalite itirazı, uyuşmazlık yönetimi, denetim zinciri, rapor
servisi, Excel/PDF dışa aktarma, ön yüz kabuğu ve kurulum.

### 13.2 Geçiş dönemi tamamlandı

Eskiden "sonraki PR'larda kaldırılacak" denen konuların hepsi kaldırıldı: fasoncu rolü ve saha ekranı, kalite itirazının
`comparison_input` okuması, kantar fişi okuma kontrolleri, panel "bant" göstergeleri (yerini fire göstergeleri aldı).
Şoför ve araç ana verisi bilgi amaçlı kalıcıdır.

### 13.3 Çuval kodu

Sahada "çuvala kod gerekmez" denildi (S3). Çuval atama artık çuval anahtarıyla değil, **sıra no + kg** ile çalışır
(5.3); sistem sıra numarasını kendisi verir, kimse kod yazmaz ya da okutmaz. Çıktıda (Ambar çıkış fişi) sıra no ve kg
gösterilir. QR etiket yoktur. Karar Bölüm 15, A4.

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
| ✓ | Fire dosyası ve "fatura edildi" | Hesap, Faturalama ekranı (faturalanacaklar ve faturalananlar), işaret | O |
| ✓ | Kalite itirazının taşınması | Kalite açar, müdür karara bağlar; haklı itiraz faturalanacak kiloya eklenir; fasoncu rolü ve saha ekranı kaldırıldı | O |
| ✓ | Raporlar | Bakiye matrisi, fire raporu, giden iplik ↔ gelen kumaş, faturalanacaklar; Excel/PDF | O–B |
| ✓ | Açılış bakiyesi | 09.11.2026 tablosu ve fasoncu sayfalarından içe aktarma, Kırıkhan kabul testi | K–O |
| ✓ | Genel müdür rolü, iplik tedarikçisi | Salt okuma rolü, tedarikçi tanımı ve lota bağlanması; tedarikçi artık zorunlu | K |
| ✓ | Merkez iplik stoku | Merkeze iplik girişi (depo), merkez kumaş girişi (merkez kalite), tedarikçiden gelen iplik tablosu, stok eksiye düşmez | O |
| ✓ | İplik kaynağı | Başka fasoncudan sevkiyat, kapıdan aktarma (tedarikçi listeden seçilir) | O |
| ✓ | Parça parça fatura | Faturalama ekranından iplik başına kilo, onay adımı; haklı itiraz faturalanacağa eklenir; kalite kararı yalnız müdürde | O |
| ✓ | Sipariş formu ve sevkiyat tarihi | Fiyat zorunlu, sipariş formu PDF, tarih gelmeden depo işlem yapamaz, tutar bilgisi | K–O |
| ✓ | Yetkiler ekranı, yardım asistanı | Rol izinleri yönetici tarafından düzenlenir; "Yardım" balonu | O |

Boyut: K ≈ 1–2 gün, O ≈ 3–5 gün, B ≈ 1–2 hafta.

---

## 15. Karara bağlanan sorular

| No | Soru | Karar (kodda uygulanan) |
|---|---|---|
| A1 | Kantar farkında tolerans olsun mu (ör. ±10 kg), yoksa her fark mı durdursun? | Tolerans ayar olarak vardır, varsayılan 0 kg (S4'e uygun: her fark durdurur) |
| A2 | Müdür küçük farkı reçeteyi güncelleyerek kabul edebilsin mi? | Evet: "İplik güncelle" kararı; iplik kiloları tartıya eşitlenir, gerekçe yazılır |
| A3 | Depo sevkiyat açabilmeye devam etsin mi? | Hayır; yalnız müdür sevkiyat açar |
| A4 | Çuval anahtarı tamamen kalksın mı, yoksa sistem üretsin mi? | Anahtar kalktı; çuval sıra no + kg ile yazılır, sıra numarasını sistem verir |
| A5 | Kalite itirazı çuval mı, kumaş partisi mi üzerinden açılsın? | Girilmiş kumaş üzerinden, kg ile; kumaş girişi yoksa itiraz açılamaz |
| A6 | Hatalı kg için kg başı ceza fiyatı sistemde tutulsun mu? | Ceza fiyatı tutulmuyor (S14). Sevkiyattaki her iplik satırının birim fiyatı (TL/kg) zorunludur; "Tutar bilgisi" bu fiyatlardan hesaplanır |
| A7 | Bakiye sevkiyat tamamlanınca mı, çıkış tartısında mı artmalı? | Tamamlanınca; "Yolda" ayrı sütun |
| A8 | Merkez fabrikanın kendi stoku ve kumaş üretimi izlensin mi? | Evet (S17): "Merkez iplik stoku" ekranında, fasoncu bakiyesinden ayrı |
| A9 | Fasoncular arası iplik transferi var mı? | Evet: sevkiyatta "Başka fasoncu" seçilir; gönderenin bakiyesi düşer, alıcının artar |

---

## 16. Kabul senaryoları

Aşağıdaki senaryoların hepsi kodda çalışır (otomatik testler ve robot senaryolarıyla doğrulanır).

1. *Depo çıkış tartısı:* Taslak sevkiyatta depo boş araç (dara) 17,5, dolu araç (brüt) 2.000, çuval 40 girer ve kantar
   fişini çeker, "İleri" ve "Onayla, yola çıkar" der → net 1.982,5 kg görünür, durum "Fasoncuya yolda" olur.
2. *Fiş zorunlu:* Fiş çekilmeden kaydet → "Devam etmek için kantar fişinin fotoğrafı gerekli."
3. *İrsaliyeyle tamamlama:* Yoldaki sevkiyatta müdür irsaliye PDF'ini yükler, "IRS-2026-77" yazar, tamamlar →
   durum "Tamamlandı", form kaybolur, irsaliye no başlıkta görünür.
4. *Tamamlama düğmesi:* İrsaliye yüklenmeden numara yazılırsa düğme kapalı kalır.
5. *Yetki:* Depo tamamlama formunu görmez; kalite yeni sevkiyat açamaz; müdür taslakta tartı girişi görmez ama
   iptali görür.
6. *İptal:* Müdür taslak sevkiyatı gerekçeyle iptal eder → durum "İptal".
7. *Çuvallar tutmuyor:* Çuvalların kilo toplamı araç net kilosunu tutmazsa depo "İleri"de "Çuvallar araç kilosunu
   tutmuyor" penceresini görür; hiçbir şey kaydedilmez. Çuval sayısı tutmazsa "Çuval sayısı tutmuyor" çıkar.
8. *Kantar uyuşmazlığı:* Reçete 1.000 kg, tartı neti 980 kg, tolerans 0 → depo eksik kilo nedenini yazar,
   "Onayla, müdüre bildir" der; durum "Kantar uyuşmazlığı", müdürün zilinde görünür; sevkiyat yola çıkamaz.
9. *Çuval ekleme:* Müdür "Çuval ekle / çıkar" seçer → durum "Taslak"; depo 1 çuval ekleyip yeniden tartar, net
   1.000 → "Fasoncuya yolda"; ilk tartım "yerine geçen" olarak geçmişte görünür.
10. *Bakiye:* 1.000 kg (viskon 600, poly 400) sevkiyat tamamlanır → fasoncu bakiyesi viskon +600, poly +400.
11. *Fire içinde:* Aynı sevkiyata 960 kg kumaş girilir ("Evet, kaydet" onayıyla) → faturalanacak kilo 0, gerçekleşen fire
    %4,00; dosya müdürün "Kapat" onayıyla kapanır.
12. *Fire aşımı:* 900 kg kumaş → faturalanacak kilo 60, dosya "Faturalanacak"; müdür Faturalama ekranında "Fatura bilgisini
    gir" der, fatura no, tarih ve iplik başına kilo yazıp "Fatura edildi" der, "Evet, fatura edildi" onayını verir →
    kalanın tamamı faturalandığı için dosya kapanır, faturalanan kilo fasoncu bakiyesinden düşer, kayıt geri alınamaz.
    Kalandan az kilo yazılırsa dosya açık kalır ve kalan azalır.
13. *Özel fire:* Fire %5 girilmiş sevkiyatta 940 kg kumaş → faturalanacak kilo 10.
14. *Dönen iplik:* Depo, sevkiyatta gönderilen ipliklerden birini seçip 100 kg dönüş kaydeder → taban 900, beklenen 864 kg.
15. *Haklı itiraz:* Kalite 100 kg itiraz açar, müdür "Haklı" der → 100 kg fire hesabına esas iplikten ve gelen kumaştan
    sayılır (fire hesabı değişmez), faturalanacak kiloya eklenir ve dosya Faturalama ekranındaki Faturalanacaklar sekmesine düşer; bakiyeden hemen düşmez.
16. *Kırıkhan:* Excel'deki giden 34.032 kg ve gelen 32.660 kg girildiğinde gerçekleşen fire %4,03, hatalı kg 10,72.
17. *Bakiye matrisi:* Açılış bakiyesi aktarıldıktan sonra matris 09.11.2026 tablosundaki fasoncu bakiyeleriyle birebir tutar.
18. *Kumaş yokken itiraz:* Kumaş girişi yapılmamış sevkiyatta kalite itirazı formu açılmaz; "Kalite itirazı için önce
    kumaş girişi yapılmalı" yazar.
19. *Kalite karar veremez:* Kalitede "Karar ver" bölümü yoktur; kararı yalnızca müdür "Haklı" ya da "Haksız" diye verir.
20. *Tedarikçi zorunlu:* Merkeze iplik girişinde tedarikçi seçilmeden kayıt yapılamaz; tedarikçisiz parti açılamaz.
