# Uçtan uca testler (Playwright)

Elle yapılan QA turunun otomatik hali. Uygulamayı gerçek bir tarayıcıda, sahte API (MSW) ile açar ve
her rolün akışlarını kullanıcı gibi tıklayarak dener (FE-TEST-08).

## Çalıştırma

`frontend` klasöründe:

```bash
pnpm test:e2e          # hepsi (≈ 1 dk)
pnpm test:e2e:smoke    # yalnızca @smoke: her ekran açılıyor mu + ana akışlar
pnpm test:e2e:rapor    # son koşunun HTML raporu (hata ekran görüntüleriyle)
```

İlk kez çalıştırmadan önce tarayıcıyı bir kez indirin: `pnpm --filter @iplik/web exec playwright install chromium`
(makinede kurulu bir Chromium varsa yolu `CHROMIUM_PATH` ile verilebilir).

Ek seçenekler `--` olmadan komutun sonuna yazılır:

```bash
pnpm test:e2e --grep "Teslim tutanağı"   # adında bu geçen testler
pnpm test:e2e --project telefon-360      # tek genişlik
pnpm test:e2e --headed --workers 1       # tarayıcıyı görerek
pnpm test:e2e --trace on                 # adım adım iz kaydı (rapordan açılır)
```

## Neden hızlı

- Testler `vite build --mode e2e` ile derlenen paketi önizleme sunucusunda açar (birkaç saniyede
  derlenir); geliştirme sunucusundaki yüzlerce modül isteği yoktur. Sahte API yalnızca bu kipte ve
  geliştirme sunucusunda açılır; üretim derlemesi (`mode=production`) etkilenmez.
- Sahte API'nin verisi tarayıcıdadır ve her sayfa açılışında baştan kurulur. Tohum veri
  `src/mocks` içindeki sabit kayıtlardır (ör. `shp-0001` taslak, `shp-0004` teslim bekliyor), testler
  veriyi arayüzden hazırlamaz ve birbirini beklemeden paralel koşar.
- Rol girişi gerekmez: `openAs('WAREHOUSE', '/panel/sevkiyat')` sayfayı o rolle açar.
- Ekran denetimleri üç genişlikte, akışlar kendi cihazında koşar.

| Proje           | Genişlik | Koşan testler                         |
| --------------- | -------- | ------------------------------------- |
| `telefon-360`   | 360 px   | `@ekran` ve saha akışları (`@saha`)   |
| `tablet-768`    | 768 px   | `@ekran`                              |
| `masaustu-1280` | 1280 px  | `@ekran` ve panel akışları (`@panel`) |

## Her testte otomatik denetlenenler

- Konsola düşen hata ya da yakalanmamış istisna testi kırar (`support/test.ts`).
- `ekranlar.e2e.ts`, `screens.ts` listesindeki her ekranı her genişlikte açar: yatay taşma, kesik
  düğme yazısı ve 768 px altında 44 px'ten küçük dokunma hedefi arar (FE-UI-07).

## Yeni ekran eklerken

1. İskeleti üretin (`frontend` klasöründe):

   ```bash
   pnpm test:e2e:yeni-ekran raporlar ADMIN /panel/raporlar "Raporlar"
   ```

   Bu komut `screens/raporlar.e2e.ts` dosyasını açar ve ekranı `screens.ts` listesine ekler. Ekran
   böylece hemen @smoke koşusuna girer (açılıyor mu, taşıyor mu).
   Dördüncü argüman ekranın adı, isteğe bağlı beşinci argüman sayfa hazır olunca görünen metindir.

2. `screens/raporlar.e2e.ts` içine ekranın davranışını yazın. Kurallar:
   - Elemanları kullanıcının gördüğü gibi bulun: `getByRole`, `getByLabel`, `getByText`
     (FE-TEST-03). `data-testid` yalnızca başka yol yoksa (ör. gizli dosya girişi).
   - Her test kendi sayfasını açar ve bir öncekine güvenmez.
   - Testin adı Türkçe davranış cümlesidir: `test('fasoncu seçilmeden göndermeyi engeller')`.
   - Ana akışa `{ tag: '@smoke' }` ekleyin; saha ekranları `@saha`, panel ekranları `@panel` alır.
   - Ekranın veriye ihtiyacı varsa sahte API'ye (`src/mocks`) kayıt ekleyin; arayüzden hazırlamayın.
   - Yetkisi olmayan bir rolle açıldığında ne olduğunu da yazın.

3. `pnpm format && pnpm lint && pnpm test:e2e --grep Raporlar` ile deneyin.

Ortak yardımcılar `support/` altındadır: `openAs` ve konsol denetimi (`test.ts`), açılır liste ve alan
doldurma (`form.ts`), kantar fişi yükleme (`ticket.ts`), düzen ölçümleri (`layout.ts`).

## CI

`frontend-e2e` iş akışı `main`'e her birleşmede tüm testleri koşar (FE-TEST-08) ve raporu artifact
olarak saklar. GitHub'da Actions sekmesinden elle de başlatılabilir.

## Henüz kapsanmayanlar

- Gerçek backend'le koşu: tam sevkiyat döngüsü (Taslak → Tamamlandı) birden çok rolün aynı veriyi
  sırayla değiştirmesini ister. Sahte API her açılışta sıfırlandığı için bu senaryo Docker Compose
  ortamı ve API ile tohum veri gerektirir.
- Offline fotoğraf çekip sonradan gönderme: offline kuyruk henüz uygulamada yok.
