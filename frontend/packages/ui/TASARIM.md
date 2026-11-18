# Tasarım kılavuzu

Ekranların ortak görsel dili: shadcn/ui çizgisinde sade, nötr, ince çizgili bir arayüz; açık ve koyu görünüm.
Bileşen kütüphanesi Mantine'dir ve yalnızca `@iplik/ui` üzerinden kullanılır; görünüm `theme.ts` ve `styles.css`
içinde tek yerden yönetilir.

## Kim kullanıyor

Depo, kalite, müdür ve genel müdür. Depo ve kalite ekranı çoğunlukla telefonda, ayakta kullanır. Ekranın işi:
**şimdi ne yapmam gerekiyor** sorusunu ilk bakışta cevaplamak ve kiloyu yanlış okutmamak.

## Renk: yalnızca adlarla

Ekran iki görünümde çalışır (kullanıcı menüsünden geçilir). Bu yüzden bileşenlerde **renk kodu ve sabit ton yazılmaz**
(`#fff`, `white`, `black`, `gray.0`, `gray.9`, `brand.8` gibi); aşağıdaki adlar kullanılır, koyu karşılıkları
kendiliğinden gelir.

| Ne için                                    | Kullanın                                                     |
| ------------------------------------------ | ------------------------------------------------------------ |
| Sayfa ve kart zemini                       | `var(--app-surface)` (kartı zeminden ince çizgi ayırır)      |
| Girinti: kart başlığı, tablo başlığı, özet | `var(--app-inset)`                                           |
| Çizgi, kenar                               | `var(--app-line)`; daha hafif ayırıcı `var(--app-line-soft)` |
| Ana yazı                                   | varsayılan (renk vermeyin) ya da `var(--app-text)`           |
| İkincil yazı                               | `c="dimmed"`                                                 |
| Anlamlı renkli zemin (uyarı, hata, başarı) | `var(--mantine-color-<renk>-light)`                          |
| Anlamlı renkli yazı ve ikon                | `var(--mantine-color-<renk>-light-color)`                    |
| Dolgulu renk (nokta, çubuk)                | `var(--mantine-color-<renk>-filled)`                         |

`<renk>`: `red` (hata, iptal, süresi geçen), `yellow`/`orange` (uyarı, bekleyen), `green` (tamamlanan), `blue`
(bilgi). Mantine bileşenlerinin `color="red"` gibi özellikleri iki görünümde de doğru çalışır; onları kullanmak
serbesttir. Ana eylem rengi (`ink`) açıkta siyah, koyuda beyazdır; elle renk vermeyin.

## İlkeler

1. **Beyaz zemin, ince çizgili kart.** Gölge yok (menü, pencere ve sabit eylem çubuğu hariç). Köşe 8 px. Sayfa
   zeminine doğrudan tablo ya da form konmaz; çizgili bir karta (`Paper`, `SectionCard`) girer.
2. **Ekranda tek ana eylem.** Dolgulu (varsayılan) düğme ekranda ya da bölümde bir tanedir. İkincil eylemler
   `variant="default"` (kenarlı), önemsizler `variant="subtle"`, geri dönüşü olmayanlar `color="red"`.
   `variant="white"` ve `variant="light"` düğme kullanılmaz.
3. **Durum: noktalı rozet.** Durum rozetleri `variant="outline"` görünümünde, nötr kenarlı, solunda durum renginde
   küçük nokta (örnek: `ShipmentStatusBadge`). Renkli dolgulu büyük rozet kullanılmaz.
4. **Sıradaki adım görünür.** Kaydın sıradaki adımı girintili bir kutuda, üstünde küçük soluk "Sıradaki adım"
   etiketi, altında yarı kalın cümle (örnek: `NextStepBand`). Kullanıcının yapacağı iş varsa düğmesi yakınındadır;
   telefonda ekranın altında sabit, tam genişlikte (`ActionBar`).
5. **Özet sayılar tek kutuda hücreler.** Yan yana sayılar ayrı ayrı kartlar değil, tek çizgili kutunun dikey çizgiyle
   ayrılmış hücreleridir: üstte soluk etiket, altında 24–26 px yarı kalın sayı, en altta küçük açıklama.
6. **Sıkı ama dokunulabilir.** Denetimler masaüstünde 36 px, telefonda 44 px (tema halleder; `size` vermeyin).
   Etiket 14 px orta kalınlıkta. Alanlar `SimpleGrid` ile hizalanır.
7. **Sayılar hizalı.** Kilo, tutar, adet sütunları sağa yaslanır; birim sayının yanında yazılır.
8. **Yazı.** Büyük harfli etiket, açık harf aralığı yok. Sayfa başlığı `Title order={1}` (24 px), bölüm
   `order={2}`, alt bölüm `order={3}`; hepsi yarı kalın. Açıklama `c="dimmed" size="sm"`.
9. **Telefon.** Geniş tablo yerine kart listesi. Yatay taşma olmaz. Sekmeler satır atlamaz, yana kayar.

## Hangi bileşen ne zaman

| İhtiyaç                            | Bileşen                                                              |
| ---------------------------------- | -------------------------------------------------------------------- |
| Sayfa başlığı ve ana eylem         | `PageHero` (başlık, tek satır yönlendirme, sağda ana eylem)          |
| Bir konunun alanları ya da listesi | `SectionCard` (başlık şeridi + gövde). Dikkat isteyen bölümde `tone` |
| Tablo                              | `Paper` içinde `Table.ScrollContainer` + `Table` (çizgili değil)     |
| Özet sayı                          | `StatCard` ya da tek kutuda hücreler (ilke 5)                        |
| Boş liste                          | `EmptyState`                                                         |
| Uyarı, bilgi                       | `Alert`                                                              |
| Durum                              | `Badge` (ilke 3)                                                     |
| Sabit alt eylem çubuğu             | `ActionBar`                                                          |

## Yapmayın

- Renk kodu, `white`/`black`, sabit gri ya da marka tonu (bkz. "Renk").
- İç içe üç katman kart. En çok iki: bölüm kartı ve içinde girintili bir satır/kutu.
- Geçişli zemin (gradient), renkli başlık kutusu, süs ikonu.
- Ekran metnini değiştirmek. Metinler `tr.json` içindedir ve robot senaryoları onlara bakar.
