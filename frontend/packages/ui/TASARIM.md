# Tasarım kılavuzu

Ekranların ortak görsel dili. Yeni ekran yazarken ya da var olanı düzenlerken buraya uyun. Kaynaklar: shadcn/ui'nin
sade yüzey ve çizgi dili, Anthropic `frontend-design` kılavuzu (kararlar konuya göre verilir, süs olmaz).
Bileşen kütüphanesi Mantine'dir ve yalnızca `@iplik/ui` üzerinden kullanılır; görünüm `theme.ts` ve `styles.css`
içinde tek yerden yönetilir.

## Kim kullanıyor

Depo, kalite, müdür ve genel müdür. Depo ve kalite ekranı çoğunlukla telefonda, ayakta ve acele kullanır. Ekranın
işi: **şimdi ne yapmam gerekiyor** sorusunu ilk bakışta cevaplamak ve kiloyu yanlış okutmamak.

## İlkeler

1. **Üç yüzey katmanı.** Zemin taş grisi (`--app-page-bg`), kart beyaz (`--app-surface`), kart içindeki başlık
   şeridi ve tablo başlığı girintili gri (`--app-inset`). Bölümler gölgeyle değil bu katmanlar ve ince çizgiyle
   (`--app-line`) ayrılır. Sayfa zeminine doğrudan form alanı ya da tablo konmaz; bir karta girer.
2. **Renk anlam taşır.** Marka rengi yalnızca ana eylem düğmesinde, etkin menüde, etkin sekmede ve sıradaki adımda
   kullanılır. Sarı/turuncu uyarı, kırmızı hata ve iptal, yeşil tamamlanan iştir. Süs için renk, geçişli zemin
   (gradient) ve renkli başlık kutusu kullanılmaz.
3. **Ekranda tek ana eylem.** Dolgulu (varsayılan) düğme ekranda bir tanedir. İkincil eylemler
   `variant="default"` (beyaz, kenarlı), önemsizler `variant="subtle"`, geri dönüşü olmayanlar `color="red"`.
   `variant="white"` ve `variant="light"` düğme kullanılmaz.
4. **Sıradaki adım görünür olur.** Bir kaydın durumu ve sıradaki adımı kartın içinde boydan boya bir şeritle
   söylenir (`NextStepBand` örneği). Kullanıcının yapacağı iş varsa düğmesi bu şeridin yakınındadır.
5. **Sıkı ama dokunulabilir.** Denetimler masaüstünde 36 px, telefonda 44 px (tema halleder; `size` vermeyin).
   Etiket 14 px orta kalınlıkta. Form alanları `SimpleGrid` ile hizalanır; ilgili alanlar aynı `SectionCard` içinde
   durur.
6. **Sayılar hizalı.** Kilo, tutar, adet sütunları sağa yaslanır (`ta="right"`); rakamlar eş genişliktedir (gövdede
   `tabular-nums` açık). Birim sayının yanında yazılır.
7. **Yazı.** Büyük harfli etiket, harf aralığı açılmış başlık yok. Başlıklar yarı kalın: sayfa `Title order={1}`
   (24 px), bölüm `order={2}`, alt bölüm `order={3}`. Açıklama metni `c="dimmed" size="sm"`.
8. **Telefon.** Geniş tablo yerine kart listesi (örnek: `ShipmentTable` → `ShipmentCard`). Yatay taşma olmaz.
   Sekmeler satır atlamaz, yana kayar.

## Hangi bileşen ne zaman

| İhtiyaç                            | Bileşen                                                              |
| ---------------------------------- | -------------------------------------------------------------------- |
| Sayfa başlığı ve ana eylem         | `PageHero` (başlık, tek satır yönlendirme, sağda ana eylem)          |
| Bir konunun alanları ya da listesi | `SectionCard` (başlık şeridi + gövde). Dikkat isteyen bölümde `tone` |
| Tablo                              | `Paper` içinde `Table.ScrollContainer` + `Table` (çizgili değil)     |
| Özet sayı                          | `StatCard`; yan yana en çok dört, hepsi aynı bölüm kartının içinde   |
| Boş liste                          | `EmptyState` (ne olduğunu ve ne yapılacağını söyler)                 |
| Uyarı, bilgi                       | `Alert` (renk anlamına göre)                                         |
| Durum                              | `Badge` (`variant="light"`, renk durum anlamına göre)                |
| Sabit alt eylem çubuğu             | `ActionBar`                                                          |

## Yapmayın

- Satır içi renk kodu (`#...`) ve `gray.0`–`gray.9` dışında rastgele ton; yüzey için CSS değişkenlerini kullanın.
- İç içe kart içinde kart içinde kart. En çok iki katman: bölüm kartı ve içinde girintili bir satır/kutu.
- Aynı sayfada birbirinin aynısı uzun kart dizileri; ilişkili sayıları tek bölümde toplayın.
- Ekran metnini değiştirmek. Metinler `tr.json` içindedir ve robot senaryoları onlara bakar.
