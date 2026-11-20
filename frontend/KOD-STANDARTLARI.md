# Frontend kod standartları (koddan çıkarılmış)

| Alan     | Değer                                                                                                               |
| -------- | ------------------------------------------------------------------------------------------------------------------- |
| Sürüm    | 1.0                                                                                                                 |
| Tarih    | 20.11.2026                                                                                                          |
| Kapsam   | `frontend/` (pnpm workspace: `apps/web`, `packages/*`)                                                              |
| Kaynağı  | Bugünkü kod: 14 modül, 5 paket, 68 test dosyası, ESLint ve TypeScript ayarları                                      |
| İlişkisi | [`docs/standartlar/react-typescript-kod-standartlari.md`](../docs/standartlar/react-typescript-kod-standartlari.md) |

## Bu doküman ne işe yarar

`docs/standartlar` altındaki doküman kod yazılmadan önce hazırlandı ve **kuralları** (`FE-…` kimlikleriyle) koyar; ESLint
ayarı o kimliklere bağlıdır ve geçerliliğini korur. Bu doküman ise **kodun bugün gerçekten nasıl yazıldığını** anlatır:
yeni bir ekran ya da alan eklerken hangi dosya nereye konur, hangi hazır parça kullanılır, hangi kalıp izlenir.

İkisi çelişirse sıra şöyledir: **ESLint/TypeScript ayarı > bu doküman > eski doküman**. Eski dokümanın kodla uyuşmayan
maddeleri [son bölümde](#eski-dokümandan-sapmalar) tek tek yazılıdır.

Kısa kural: _yeni kod, yanındaki koda benzemelidir._ Aşağıdaki her madde kodda en az bir örneğe dayanır.

---

## 1. Klasör yapısı

```
frontend/
  apps/web/src/
    app/            Uygulama kabuğu: router, sorgu istemcisi, yan menü, hata sayfaları
    modules/<ad>/   İş alanı modülleri (shipment, stock, balance, discrepancy, claim, evidence, report,
                    masterdata, users, rolepermissions, branding, audit, auth, assistant)
    modules/shared/ Hiçbir iş alanına ait olmayan ortak ekran parçaları (QueryState, ListPagination,
                    BackLink, useWideLayout)
    mocks/          Sahte API (MSW): geliştirme sunucusu ve testler aynı yanıtları kullanır
    test/           Test kurulumu ve renderApp
  apps/web/e2e/     Sahte API ile Playwright testleri (CI'da koşar)
  apps/web/robot/   Gerçek arayüze ve gerçek sunucuya karşı koşan test robotu
  packages/ui/          Tasarım sistemi: Mantine yalnızca buradan dışa açılır
  packages/api-client/  HTTP istemcisi, üretilmiş sözleşme tipleri ve sarmalayıcıları
  packages/i18n/        Türkçe metinler (tr.json), tarih biçimleri
  packages/units/       Kilo ve yüzde hesabı (big.js üzerinde)
  packages/config/      Ortak ESLint ve TypeScript ayarları
```

### Modül içi düzen

| Yer               | İçerik                                                                             |
| ----------------- | ---------------------------------------------------------------------------------- |
| `index.ts`        | Modülün dışa açtığı her şey. Başka modül yalnızca buradan alır (`@/modules/<ad>`). |
| `routes.tsx`      | `<ad>PanelRoutes`: her rota `RequirePermission` ile bir izne bağlıdır.             |
| `pages/`          | Rota başına bir sayfa. Veriyi hook ile çeker, alt bileşenlere props ile verir.     |
| `components/`     | Sayfanın parçaları. Dosya başına tek bileşen.                                      |
| `hooks/`          | Sorgu ve kayıt hook'ları (`useShipments`, `useCancelShipment`).                    |
| `<konu>Draft.ts`  | Form taslağı: tipi, boş hali, doğrulaması, API isteğine çevrilmesi. React içermez. |
| `<konu>Errors.ts` | Sunucu hata kodundan (`problem.code`) metin anahtarına eşleme.                     |
| `*.test.ts(x)`    | Test, sınadığı dosyanın yanında durur.                                             |

Büyük modül alt konulara bölünür (`shipment/create`, `shipment/dispatch`, `shipment/settlement`, `shipment/recipe`).

### Modüller arası bağımlılık

- `app → modules → packages`. Paketler uygulamayı, modüller `app/` klasörünü import etmez.
- Modül içi import göreli (`../hooks/useShipment`), modüller arası import `@/modules/<ad>` ile yapılır. İç dosyaya
  (`@/modules/shipment/components/...`) dışarıdan erişilmez; ESLint (`boundaries`) engeller.
- **Bir parça iki modülde gerekiyorsa** ve iş alanına ait değilse `modules/shared`'a girer. İş alanına aitse (ör.
  `useMasterOptions`, `InvoiceForm`) sahibi olan modülün `index.ts`'inden açılır.
- `@iplik/ui`'ye yalnızca i18n ve API bilmeyen görsel parçalar girer (`EmptyState`, `SectionCard`, `ActionBar`).
  Metin ya da sorgu bilen ortak parça `modules/shared`'dadır.

---

## 2. Adlandırma

Tanımlayıcılar ve dosya adları İngilizce ve ASCII; yorumlar, test adları ve commit açıklamaları Türkçedir.

| Öğe                 | Biçim                                                                                                     | Koddan örnek                                                                     |
| ------------------- | --------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------- |
| Bileşen dosyası     | `PascalCase.tsx`, içindeki bileşenle aynı                                                                 | `ShipmentCard.tsx` → `ShipmentCard`                                              |
| Hook dosyası        | `useCamelCase.ts`                                                                                         | `useCancelShipment.ts`                                                           |
| Diğer dosya         | `camelCase.ts`                                                                                            | `createDraft.ts`, `stockErrors.ts`                                               |
| Sayfa               | `…Page`                                                                                                   | `ShipmentListPage`, `CentralStockPage`                                           |
| Rota listesi        | `<modül>PanelRoutes`                                                                                      | `shipmentPanelRoutes`                                                            |
| Props tipi          | bileşen adı + `Props`, dışa aktarılır                                                                     | `ShipmentCardProps`                                                              |
| Boolean             | `is` / `has` / `can` / `should`                                                                           | `isDesktop`, `canReceipt`                                                        |
| Modül sabiti        | `UPPER_SNAKE_CASE`, birimiyle                                                                             | `MAX_REASON_LENGTH`, `BUTTON_HEIGHT_PX`                                          |
| Olay                | prop `onX`, işleyici `handleX` ya da satır içi ok fonksiyonu                                              | `onCreated`, `onChange`                                                          |
| Sorgu anahtarı yolu | `<KAYNAK>_PATH`                                                                                           | `SHIPMENTS_PATH`                                                                 |
| Fonksiyon fiili     | `find…` olmayabilir, `format…` metin üretir, `parse…` metinden okur, `calculate…` hesaplar, `to…` çevirir | `findDriverName`, `formatKgText`, `parseKg`, `calculateNetKg`, `toSeverityColor` |

Kısaltma yazılmaz (`shp`, `qty`, `btn` yok). Belirsiz adlar (`data`, `item`, `utils`, `helper`) kullanılmaz; yardımcı
fonksiyon ait olduğu kavramın dosyasında durur.

---

## 3. TypeScript

Ayarlar `packages/config` içindedir ve hata seviyesindedir; uyarı seviyesi yoktur.

- `any`, `!` (non-null), `enum`, `as` (yalnızca `as const` serbest) yok. Bilinmeyen veri `unknown` alınır, guard
  fonksiyonuyla daraltılır (`isKilogram`, `STATUSES.find((candidate) => candidate === value)`).
- Nesne şekilleri `type` ile yazılır; tip import'u `import type` iledir.
- Birbirini dışlayan durumlar ayrık birleşimdir: `type FieldValue = { value: string } | { missing: MissingKey }`.
- `exactOptionalPropertyTypes` açık: isteğe bağlı alan `undefined` verilerek değil, hiç yazılmayarak boş bırakılır:
  `...(filter.status === undefined ? {} : { status: filter.status })`.
- Varsayılan dışa aktarım (`export default`) ve `export *` yok.
- Sihirli sayı yok: `-1, 0, 1, 2, 100` dışındaki her sayı adlandırılmış sabittir.
- Promise ya beklenir ya `void` ile bırakılır: `onClick={() => void query.refetch()}`.

### Boyut sınırları (pazarlık konusu değil)

| Ne                   | Sınır                | Aşılınca                                |
| -------------------- | -------------------- | --------------------------------------- |
| Dosya                | 250 satır (test 400) | Alt bileşene ya da ayrı dosyaya bölünür |
| Fonksiyon / bileşen  | 60 satır, 15 ifade   | Alt bileşen ya da hook çıkarılır        |
| Parametre            | 3                    | Tek nesne parametresi                   |
| JSX derinliği        | 6                    | Alt bileşen                             |
| Karmaşıklık          | 10 (bilişsel 12)     | Erken `return`, eşleme tablosu          |
| Dosya başına bileşen | 1                    | Her bileşen kendi dosyasında            |

Sınır `eslint-disable` ile delinmez. Üretim kodunda toplam üç istisna vardır ve hepsi gerekçelidir (iki kimliksiz
taslak satırında dizi indeksi, sipariş formu PDF'inde yerleşim ölçüleri).

---

## 4. Bileşen kalıbı

```tsx
export type ShipmentCardProps = {
  shipment: ShipmentSummary;
  to: string;
};

/** Dar ekranda liste satırı: tek dokunuşla açılan büyük kart (FE-UI-07, en az 44 px hedef). */
export function ShipmentCard({ shipment, to }: ShipmentCardProps) {
  const { t } = useTranslation();
  return (/* … */);
}
```

- Yalnızca fonksiyon bileşeni; `React.FC` yok. Props parametrede açılır.
- Props tipi `…Props` adıyla ayrı yazılır ve dışa aktarılır. (Tek alanlı eski bileşenlerin bir kısmında tip satır
  içindedir; yeni ve dokunulan bileşen ayrı tiple yazılır, bkz. [bilinen borçlar](#bilinen-borçlar).)
- Her bileşenin üstünde **neden var olduğunu** söyleyen tek cümlelik Türkçe yorum bulunur. Ne yaptığını kod söyler.
- `useMemo`, `useCallback`, `memo` yazılmaz (kodda iki istisna var, ikisi de ölçülmüş bir yeniden çizim içindir).
- `useEffect` yalnızca dış sistemle eşitleme içindir (`ResizeObserver`, tarayıcı depolaması) ve temizleme fonksiyonu
  döner. Sunucu verisi çekmek ya da props'u state'e kopyalamak için kullanılmaz.
- Koşullu görünüm iç içe üçlüyle yazılmaz; erken `return` ya da ayrı bileşen kullanılır.

---

## 5. Sunucu verisi

### API katmanı (`packages/api-client`)

- İstemci Orval ile OpenAPI'den üretilir (`src/generated`, elle düzenlenmez; CI farkı yakalar).
- Uygulama üretilmiş dosyayı doğrudan kullanmaz. Her kaynak için bir **sarmalayıcı** vardır (`shipments.ts`,
  `settlements.ts`, `masterData.ts`): tipleri daraltır (kilo alanı `Kilogram` olur), yol sabitini (`…_PATH`) ve
  çağrı fonksiyonlarını dışa açar.
- `axios` yalnızca bu pakette import edilir; `fetch` hiçbir yerde çağrılmaz.
- Her istek `X-Request-Id`, durum değiştiren her istek `Idempotency-Key` taşır; bunu `httpClient` ekler, bileşen
  yazmaz.
- Hata RFC 9457 Problem Details'tir: `toProblem(error)` ile okunur, karar **`problem.code`** ile verilir (başlık ya da
  açıklama metniyle değil). Durum kodunun anlamı `classifyProblem` ile tek yerde çözülür.

### Sorgu ve kayıt hook'ları

```ts
export function useShipments(params: ListShipmentsParams) {
  return useQuery({
    queryKey: [SHIPMENTS_PATH, params],
    queryFn: () => listShipments(params),
    placeholderData: keepPreviousData,
  });
}

export function useCancelShipment(shipmentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (reason: string) => cancelShipment(shipmentId, { reason }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [SHIPMENTS_PATH] }),
  });
}
```

- Sorgu anahtarı `[<KAYNAK>_PATH, parametreler]` biçimindedir; kayıttan sonra aynı yol sabitiyle geçersiz kılınır.
- Sunucu verisi yalnızca TanStack Query'de yaşar; `useState`'e kopyalanmaz. Global durum kütüphanesi yoktur.
- Sayfalı listelerde `keepPreviousData` kullanılır; sayfa değişirken liste boşalıp titremez.
- Filtre ve sayfa numarası adres çubuğunda tutulur (`useShipmentListFilter`, `useAuditFilter`); bağlantı
  paylaşılabilir, geri tuşu çalışır.
- Geçici hatalar (5xx, bağlantı) iki kez yeniden denenir; 4xx denenmez (`app/queryClient.ts`).

### Yükleniyor / hata / boş / veri

Dört durum her listede aynı sırayla ve aynı parçalarla çizilir:

```tsx
<QueryState query={query}>
  {(page) =>
    page.items.length === 0 ? (
      <EmptyState title={t('shipment.list.empty')} />
    ) : (
      <Stack>
        <ShipmentTable shipments={page.items} />
        <ListPagination
          total={page.page.totalPages}
          value={filter.page}
          onChange={filter.setPage}
        />
      </Stack>
    )
  }
</QueryState>
```

| Durum          | Parça                                                 | Kural                                                                                                                                       |
| -------------- | ----------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------- |
| Yükleniyor     | `QueryState`                                          | Elle `<Loader />` yazılmaz. Yazmak gerekirse `aria-label={t('app.loading')}` taşır.                                                         |
| Hata           | `QueryState`                                          | Geçici hatada "Yeniden dene" çıkar; yetki ve "bulunamadı"da çıkmaz.                                                                         |
| Boş liste      | `EmptyState` (sayfanın ana listesi)                   | Bölüm kartının (`SectionCard`) içindeki küçük boş liste soluk tek satır yazıdır (`<Text c="dimmed">`); kart içinde ikinci bir kutu açılmaz. |
| Kayıt hatası   | `Alert color="red"`, düğmenin hemen üstünde           | Metin `problem.code`'dan eşlenir; bilinmeyen kod `…errors.unknown`'a düşer.                                                                 |
| Kayıt başarısı | `Alert color="green"`, yeni kodda `role="status"` ile | Kullanıcı ne olduğunu ekrandan okur; sessiz kayıt yoktur.                                                                                   |

---

## 6. Formlar

Form kütüphanesi kullanılmaz. Kalıp üç parçadır:

1. **Taslak dosyası** (`createDraft.ts`, `recordDraft.ts`, `stockDraft.ts`): taslak tipi, boş taslak, alan doğrulama
   fonksiyonları (`validateLabel` → hata anahtarı ya da `undefined`) ve taslağı API isteğine çeviren fonksiyon. Saf
   TypeScript'tir; React olmadan test edilir (`createDraft.test.ts`).
2. **Taslak hook'u** (`useRecordDraft`, `useFabricDraft`): `useState` ile taslağı tutar, alan değiştirme ve gönderme
   fonksiyonlarını verir.
3. **Form bileşenleri**: alanları çizer; hata metnini `t('…errors.' + anahtar)` ile gösterir.

Kurallar:

- Frontend yalnızca **biçim** doğrular (zorunlu alan, uzunluk, desen, sayı aralığı). İş kuralı (tolerans, stok
  yeterliliği, durum geçişi) sunucudadır; frontend en fazla uyarı gösterir.
- Sunucunun reddi her zaman ekranda nedeniyle görünür. Hata kodları `KNOWN_CODES` listesinden eşlenir.
- Zorunlu alan `required`/`withAsterisk` ile işaretlenir; isteğe bağlı alanın etiketi "(isteğe bağlı)" der.
- Gönder düğmesi istek sürerken `loading` olur; çift tıklama ikinci kayıt açmaz.
- Geri dönüşü olmayan işlem ikinci bir onay ister ("Evet, kaydet" / "Vazgeç").
- Aksiyon düğmesi yalnızca kaydın `allowedActions` listesinde varsa çizilir; durumdan (`status === …`) türetilmez.

---

## 7. Kilo, yüzde, tarih

- Kilo API'de **dize** gelir ve dize taşınır. `Number()`, `parseFloat()` ve `+` ile toplama yasaktır; hesap
  `@iplik/units` ile yapılır (`sumKg`, `subtractKg`, `calculateNetKg`, `calculateFire`).
- Ekranda kilo `formatKgText` ile yazılır: **binlik nokta, ondalık virgül, sonda `kg`** (`1.982,5 kg`). Kullanıcının
  yazdığı `parseKg` ile okunur. Elle `replace('.', ',')` yazılmaz.
- Yüzde `formatPct` / `formatPercentText` ile yazılır (`%4,03`).
- Kilo, tutar ve adet sütunları sağa yaslanır (`ta="right"`); birim sayının yanındadır.
- Tarih API'de ISO 8601 UTC'dir; ekranda `@iplik/i18n`'deki `formatDate` / `formatDateTime` ile `Europe/Istanbul`
  saatinde gösterilir. `toLocaleString()` doğrudan çağrılmaz.

---

## 8. Yetki ve güvenlik

- Görünürlük **izin anahtarıyla** verilir: `usePermission('CENTRAL_STOCK_RECORD')` ya da
  `<Can permission="…">`. Rol adına göre dallanma (`role === 'ADMIN'`) ESLint hatasıdır.
- Her rota `RequirePermission` taşır; yan menü bağlantısı aynı izinle görünür.
- Tarayıcıda token tutulmaz; oturum sunucunun `HttpOnly` çerezidir.
- Kör giriş alanı gizlenmez, **istenmez**: rol o alanı görmemeliyse yanıtta hiç gelmez. Sahte API de aynı kurala uyar.
- `localStorage` / `sessionStorage` yalnızca arayüz tercihi içindir (marka rengi önbelleği, rapor logosu, Yardım
  sohbeti). Çağrı `window.localStorage` ile, `try/catch` içinde ve kendi dosyasında yapılır (`brandingCache.ts`).
  Sevkiyat ya da kişi verisi yazılmaz.
- `dangerouslySetInnerHTML`, `eval`, `console.*` yok. Ortam değişkeni yalnızca `app/config.ts` içinde okunur.

---

## 9. Arayüz

Görsel dil [`packages/ui/TASARIM.md`](packages/ui/TASARIM.md) içindedir (renk adları, tek ana eylem, noktalı rozet,
özet hücreleri). Burada yalnızca kod tarafı yazılıdır.

- Mantine yalnızca `@iplik/ui` üzerinden kullanılır. Yeni bir Mantine parçası gerekiyorsa `packages/ui/src/index.ts`'e
  eklenir.
- Renk kodu, `white`/`black`/`dark` ve sabit ton yazılmaz; `var(--app-text)`, `var(--app-line)`, `c="dimmed"` ve
  `var(--mantine-color-<renk>-light)` kullanılır. Uygulama açık ve koyu görünümde çalışır.
- Denetimlere `size` verilmez; yükseklik temadan gelir (telefon ve tablette 44 px, masaüstünde 36 px).
- Durum yalnızca renkle verilmez; yanında yazı bulunur.
- İkon `@iplik/ui`'deki `Icon` ile çizilir, dekoratiftir (ekran okuyucudan gizli). Yalnızca ikonlu düğme
  `aria-label` taşır.

### Hazır parçalar

| İhtiyaç                            | Parça                                   | Nerede               |
| ---------------------------------- | --------------------------------------- | -------------------- |
| Sayfa başlığı ve ana eylem         | `PageHero`                              | `@iplik/ui`          |
| Bir konunun alanları ya da listesi | `SectionCard`                           | `@iplik/ui`          |
| Yükleniyor / hata                  | `QueryState`                            | `@/modules/shared`   |
| Boş liste                          | `EmptyState`                            | `@iplik/ui`          |
| Sayfalama                          | `ListPagination`                        | `@/modules/shared`   |
| "… dön" bağlantısı                 | `BackLink`                              | `@/modules/shared`   |
| Tablo mu kart mı                   | `useWideLayout()`                       | `@/modules/shared`   |
| Altta sabit eylem çubuğu           | `ActionBar`                             | `@iplik/ui`          |
| Durum rozeti                       | `DotBadge`                              | `@iplik/ui`          |
| Özet sayılar                       | `StatGrid` + `StatCell`                 | `@/modules/report`   |
| Tanımlardan seçim                  | `MasterSelect` + `useMasterOptions`     | `@/modules/shipment` |
| Excel / PDF indirme                | `InlineExportButtons`, `PanelExportBar` | `@/modules/report`   |
| Belge ve fotoğraf                  | `EvidenceUpload`, `EvidenceGallery`     | `@/modules/evidence` |

Aynı işi yapan ikinci bir parça yazılmaz; eksiği varsa mevcut parça geliştirilir.

### Ekran genişlikleri

- Önce 360 px düşünülür. 360, 768 ve 1280 px'te sayfa yatay kaymaz.
- 992 px altında (telefon ve dik tablet) geniş tablo yerine **kart listesi** çizilir. Eşik `useWideLayout()` içindedir;
  bileşene medya sorgusu yazılmaz. Her tablo bileşeninin bir kart eşi vardır (`ShipmentTable` → `ShipmentRow` /
  `ShipmentCard`).
- Kart listesine dönüşmeyen tablo `Table.ScrollContainer` içinde durur; sayfa değil tablo kayar.
- Alanlar `SimpleGrid cols={{ base: 1, sm: 2 }}` ile hizalanır; telefonda tek sütundur.
- Sayfanın altında 88 px boşluk bırakılır (`PanelLayout`), sabit Yardım düğmesi son satırı örtmez. `ActionBar` olan
  sayfada Yardım düğmesi çubuğun üstüne çıkar.
- Denetim: `pnpm check:responsive` (yatay taşma, 44 px'ten küçük hedef, kesik düğme yazısı).

### Metinler

- Ekranda görünen her metin `packages/i18n/src/locales/tr.json` içindedir; JSX'e gömülmez.
- Anahtar `modül.ekran.öğe` biçimindedir (`shipment.cancel.confirm`). Hata metinleri `…errors.<KOD>` altında, bilinmeyen
  kod için `…errors.unknown` ile durur.
- Dil fabrika çalışanının günlük dilidir: harfli kısaltma, kod, UUID, muhasebe terimi yazılmaz. Aynı kavram her
  ekranda aynı sözcükle geçer; sözlük [`apps/web/robot/README.md`](apps/web/robot/README.md) "Arayüz dili"
  bölümündedir.
- **Metin değiştirmek davranış değiştirmektir:** robot senaryoları ve testler metne bakar. Değişen sözcük aynı PR'da
  robotta ve sözlükte de değişir.
- Kullanılmayan anahtar bırakılmaz.

---

## 10. Testler

| Tür                   | Araç                           | Nerede                        | Ne zaman koşar                                  |
| --------------------- | ------------------------------ | ----------------------------- | ----------------------------------------------- |
| Birim ve bileşen      | Vitest + Testing Library + MSW | dosyanın yanında `*.test.tsx` | Her PR (CI), kapsam eşiği %70                   |
| Uçtan uca (sahte API) | Playwright                     | `apps/web/e2e`                | Her PR (CI), 360 / 768 / 1280 px                |
| Robot (gerçek sunucu) | Playwright                     | `apps/web/robot`              | Yerelde, yalnızca değişen özelliğin senaryoları |

- Sayfa testi `renderApp('/panel/…')` ile gerçek rotaları açar; rol `server.use(...createHandlers('MANAGER'))` ile
  seçilir. `axios` ya da hook `vi.mock` ile taklit edilmez; ağ yalnızca MSW ile taklit edilir.
- Öğeler rolüyle ve adıyla bulunur (`getByRole('button', { name: 'Yeniden dene' })`); sınıf adı ve `data-testid`
  son çaredir.
- `describe` sınanan birimin adı, `it` Türkçe davranış cümlesidir: `it('bulunamayan kayıtta yeniden deneme düğmesi
göstermez')`.
- Taslak ve hesap dosyaları (`*Draft.ts`, `@iplik/units`) React olmadan sınanır; `@iplik/units` kapsamı %100'dür.
- Yeni ekran: sahte API yanıtı (`mocks/`), en az bir sayfa testi ve `e2e/screens.ts` satırı aynı PR'dadır.
- Ekran, alan, düğme, doğrulama, rol ya da hesap değiştiyse robot senaryosu ve gerekiyorsa robot dokümanı aynı PR'da
  güncellenir. Robottan sonra ortam `pnpm yerel:sifirla -- --evet` ile sıfırlanır.

---

## 11. PR öncesi kontrol listesi

```bash
cd frontend
pnpm format:check && pnpm lint && pnpm typecheck && pnpm test --coverage && pnpm build
```

- [ ] Yeni metinler `tr.json`'da ve sözlükle uyumlu.
- [ ] Liste ekranı dört durumu (yükleniyor, hata, boş, veri) ortak parçalarla çiziyor.
- [ ] Ekran 360 / 768 / 1280 px'te denendi (`pnpm check:responsive`).
- [ ] Renk kodu, `size`, rol adına göre dallanma, `number` ile kilo hesabı yok.
- [ ] Robot maddesi PR şablonunda dolduruldu.
- [ ] İş kuralı, rol ya da ekran akışı değişiyorsa bunun istendiği PR açıklamasında yazılı.

---

## Eski dokümandan sapmalar

`docs/standartlar/react-typescript-kod-standartlari.md` içindeki şu maddeler kodda **uygulanmadı**. Kod bilinçli olarak
daha yalın kaldı; bu maddeler yeni kod için bağlayıcı değildir.

| Eski madde           | Eski dokümanın dediği                                              | Kodda olan                                                                       |
| -------------------- | ------------------------------------------------------------------ | -------------------------------------------------------------------------------- |
| FE-FORM-01           | React Hook Form + Zod, `schemas/` klasörü                          | Form kütüphanesi yok; `*Draft.ts` + taslak hook'u (bölüm 6)                      |
| FE-API-02            | İstemci durumu için Zustand                                        | Global durum kütüphanesi yok; sunucu verisi TanStack Query'de, gerisi `useState` |
| FE-API-03            | Orval'ın `get…QueryKey` fonksiyonları                              | Orval yalnızca istemci fonksiyonu üretir; anahtar `[<KAYNAK>_PATH, parametre]`   |
| FE-API-05            | `applyProblemToForm`                                               | Yok; hata `problem.code` → metin anahtarı eşlemesiyle gösterilir                 |
| FE-NUM-03, FE-UI-01  | `KgInput`, `DataTable`, `StatusBadge`, `CameraCapture` bileşenleri | Yok; kilo alanı `TextInput` + `parseKg`, tablo `Table`, rozet `DotBadge`         |
| FE-MOD-02, FE-SEC-05 | `offline/` klasörü, IndexedDB (Dexie) kuyruğu                      | Yok; çevrimdışı kuyruk kapsam dışı bırakıldı                                     |
| FE-SEC-05            | Depolama yalnızca `packages/ui/preferences` içinde                 | Üç modülde, her biri kendi dosyasında (bölüm 8)                                  |
| FE-UI-06             | Ayrı saha (`/saha`) kabuğu                                         | Tek kabuk (`/panel`); telefon düzeni aynı ekranların dar halidir                 |
| FE-TEST-09           | Sayfa testlerinde `axe` denetimi                                   | Yok; erişilebilirlik `jsx-a11y` kuralları ve robot senaryosu `13` ile denetlenir |
| FE-BUILD-06, 07      | `lint-staged`, `size-limit`                                        | Kurulu değil; denetim CI'dadır                                                   |

## Bilinen borçlar

Bugün kodda olan, bilinçli olarak dokunulmamış tutarsızlıklar. Yeni kod doğru kalıbı izler; eski kod dosyaya başka bir
nedenle dokunulduğunda düzeltilir (ayrı toplu değişiklik açılmaz, canlıya yakın dönemde gereksiz risk olur).

| Konu                               | Durum                                                                                               | Doğrusu                                        |
| ---------------------------------- | --------------------------------------------------------------------------------------------------- | ---------------------------------------------- |
| Props tipi                         | Yaklaşık 120 bileşende tip satır içinde (`}: { shipmentId: string })`), 140 bileşende ayrı `…Props` | Ayrı `…Props` tipi                             |
| `Icon` / `NavIcon`                 | Aynı bileşen iki adla dışa açılıyor (36 / 7 kullanım)                                               | İçerikte `Icon`; `NavIcon` yalnızca yan menüde |
| İki `StatCard`                     | `@iplik/ui`'de kutulu `StatCard`, `report` modülünde hücre `StatCard` (dışarıya `StatCell` adıyla)  | Hücre olan `StatCell` adını dosyada da alsın   |
| Kart listesine dönüşmeyen tablolar | Rapor, bakiye ve fatura tabloları dar ekranda yana kayıyor                                          | Sık kullanılanlar için kart eşi yazılsın       |
| Kullanıcılar süzgeci               | 360 px'te beş seçenekli şerit yana kayıyor                                                          | Dar ekranda açılır liste                       |
