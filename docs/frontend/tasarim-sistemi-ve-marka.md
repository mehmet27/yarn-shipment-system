# Tasarım sistemi ve marka ayarı

Arayüz, `frontend/packages/ui` içindeki Mantine tabanlı tasarım sistemini kullanır. Uygulama kodu Mantine'i
yalnızca bu paket üzerinden alır. Bu belge ortak bileşenleri ve her kurulumun kendi rengini nasıl belirlediğini anlatır.

## Marka rengi her kurulum için ayrıdır

Her firma ayrı kurulumdur (ayrı veritabanı), bu yüzden tek bir marka ayarı vardır.

- Yönetici **Yönetim > Görünüm** ekranından ana rengi, firma adını ve logoyu belirler (yetki: `SETTINGS_MANAGE`).
- Ayar `app_setting` tablosunda `branding_company_name`, `branding_primary_color` ve `branding_logo` anahtarlarıdır (eski `md_branding` birleştirildi); her değişiklik `audit_local` ile denetim kaydına düşer.
- `GET /api/v1/branding` girişsizdir (giriş sayfası da markalı olsun). `PUT /api/v1/branding` yalnızca `SETTINGS_MANAGE`.
- Kayıt yoksa varsayılan renk `#17527a` kullanılır.

### Tema nasıl üretilir

`createBrandPalette(hex)` tek bir renkten 10 tonluk Mantine paleti üretir:

- Açık tonlar renge beyaz, koyu tonlar siyah katarak türetilir.
- `brand.7` (ana ton), beyaz zemin üzerinde WCAG AA kontrastını (4.5) sağlayacak şekilde gerekirse otomatik koyulaştırılır.
- Başlık, yan menü ve sayfa başlığı gradient'leri bu paletten türer (`BRAND_GRADIENT`, `BRAND_SIDEBAR_GRADIENT`).
- Durum renkleri (yolda, tamam, iptal vb.) markadan bağımsızdır ve sabit kalır.

Uygulama açılırken `BrandedUiProvider` marka ayarını okur ve `UiProvider`'a verir. Yanıt gelene kadar tarayıcıda
saklanan son ayar (yoksa varsayılan renk) kullanılır, böylece renk titremez.

### Yeni firma kurulumu

1. Kurulum sonrası yönetici hesabıyla girin.
2. **Yönetim > Görünüm** ekranından firma adı, ana renk ve logoyu girin (logo en çok 140 KB; PNG, JPEG, WEBP veya SVG).
3. Önizlemeyi kontrol edip kaydedin; değişiklik herkese hemen yansır.

## Ortak bileşenler (`packages/ui`)

| Bileşen | Kullanım |
| --- | --- |
| `PageHero` | Sayfa başlığı; marka gradient'i, sağda eylemler, altına taşan içerik |
| `StatCard` | Liste başındaki özet göstergesi (renk + ikon + metin) |
| `ProgressSteps`, `MiniProgress` | Sevkiyat sürecini adım adım / satır içinde gösterir |
| `ListGroupHeader` | Gruplu listelerde grup başlığı |
| `ActivityFeed` | Zaman çizelgesi (geçmiş sekmesi) |
| `EmptyState` | Boş liste ve "kaydedilmedi" durumları |
| `ActionBar` | Ana eylemleri altta sabit tutar (telefonda tek elle erişim) |
| `NavIcon` | Yan menü ikonları (satır içi SVG) |

## Kurallar

- Renk değerleri yalnızca `theme.ts` ve `brandPalette*.ts` içinde yazılır; uygulama kodu tema token'larını kullanır.
- Durum yalnızca renkle verilmez; metin ya da ikon eşlik eder (FE-UI-03).
- Etkileşimli ögeler 360 px genişlikte yatay kaydırma olmadan çalışır; e2e testleri bunu denetler.
