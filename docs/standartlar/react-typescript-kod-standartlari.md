# İplik Sevkiyat Sistemi – React ve TypeScript Kod Standartları

| Alan | Değer |
| --- | --- |
| Sürüm | 1.0 |
| Tarih | 21.10.2026 |
| Kapsam | Frontend: `frontend/` pnpm workspace (`apps/web`, `packages/*`) |
| Dayandığı dokümanlar | [React frontend mimarisi](../frontend/react-frontend-mimarisi.md) v1.2, [Yetkilendirme teknik analizi](../yetkilendirme/yetkilendirme-teknik-analizi.md) v1.1, [Java 25 kod standartları](java-kod-standartlari.md) v1.0 |
| Bağlayıcılık | **Projede yazılan her TypeScript ve TSX satırı bu dokümana uyar.** Kurala aykırı kod birleştirilmez. |

Bu doküman mimari kararı F-13'ün karşılığıdır: Java kod standartlarıyla aynı yapıda, frontend'e özgü kuralları içerir. Klasör yapısı, teknoloji seçimi ve ekran kararları mimari dokümandadır; burada tekrar edilmez.

Kurallar iki seviyededir:

- **ZORUNLU**: İstisnası yoktur ya da yalnızca bölüm 13'teki istisna süreciyle delinebilir. Büyük kısmı `pnpm lint` ve `pnpm typecheck` ile **hata** seviyesinde denetlenir.
- **ÖNERİLEN**: Varsayılan davranıştır; uymamak için PR açıklamasında kısa gerekçe yeterlidir.

Kural belirtilmeden yazılan her madde ZORUNLU kabul edilir. Kural kimlikleri Java dokümanındakilerle karışmasın diye `FE-` önekini taşır; review'da `FE-SIZE-02` gibi atıf yapılır.

---

## İçindekiler

1. [Temel ilkeler](#1-temel-ilkeler)
2. [TypeScript kullanımı](#2-typescript-kullanımı)
3. [Adlandırma ve dil](#3-adlandırma-ve-dil)
4. [Kod boyutu, karmaşıklık ve yapı sınırları](#4-kod-boyutu-karmaşıklık-ve-yapı-sınırları)
5. [Modül sınırları ve import kuralları](#5-modül-sınırları-ve-import-kuralları)
6. [React bileşenleri ve hook'lar](#6-react-bileşenleri-ve-hooklar)
7. [Sunucu verisi, API ve hata yönetimi](#7-sunucu-verisi-api-ve-hata-yönetimi)
8. [Formlar, kilo ve sayılar](#8-formlar-kilo-ve-sayılar)
9. [Yetki, kör giriş ve güvenlik](#9-yetki-kör-giriş-ve-güvenlik)
10. [UI, erişilebilirlik ve metinler](#10-ui-erişilebilirlik-ve-metinler)
11. [Test kuralları](#11-test-kuralları)
12. [Araç zinciri ve bağımlılıklar](#12-araç-zinciri-ve-bağımlılıklar)
13. [Git, PR ve istisna süreci](#13-git-pr-ve-istisna-süreci)
14. [Ek A: Otomatik denetim özeti](#ek-a-otomatik-denetim-özeti)

---

## 1. Temel ilkeler

- **FE-GEN-01 Araç önce gelir.** Bir kural `tsc`, ESLint veya test ile denetlenebiliyorsa orada denetlenir, review'a bırakılmaz. Ek A hangi kuralın nerede denetlendiğini listeler. ESLint'te `warn` seviyesi kullanılmaz; kural ya `error` ya kapalıdır.
- **FE-GEN-02 Backend karar verir, frontend gösterir.** Durum geçişi, yetki, tolerans ve kör giriş kuralları frontend'de yeniden kodlanmaz. Ekran `allowedActions`, izin listesi ve API'den gelen alanlara göre şekillenir (mimari doküman, "Yetki modeli").
- **FE-GEN-03 Sözleşme tek kaynaktan gelir.** API tipleri elle yazılmaz; OpenAPI'den Orval ile üretilir (FE-API-01).
- **FE-GEN-04 Değişmezlik varsayılandır.** Props, state ve sorgu verisi yerinde değiştirilmez (`push`, `splice`, alan ataması yok); yeni nesne üretilir.
- **FE-GEN-05 Açık olan örtük olandan iyidir.** Varsayılan dışa aktarım, `any`, non-null assertion (`!`) ve sihirli dize kullanılmaz.
- **FE-GEN-06 Basitlik.** İhtiyaç doğmadan genel amaçlı soyutlama, global state veya önbellek katmanı yazılmaz. Bir bileşen iki yerde kullanılmadan `packages/ui`'ye taşınmaz.

---

## 2. TypeScript kullanımı

### 2.1 Derleyici ayarları

- **FE-TS-01** Tüm paketler ortak `tsconfig.base.json`'ı genişletir. Kapatılamayan ayarlar: `strict`, `noUncheckedIndexedAccess`, `exactOptionalPropertyTypes`, `noImplicitOverride`, `noImplicitReturns`, `noFallthroughCasesInSwitch`, `noUnusedLocals`, `noUnusedParameters`, `verbatimModuleSyntax`, `isolatedModules`.
- **FE-TS-02** `// @ts-ignore` ve `// @ts-nocheck` yasaktır. `// @ts-expect-error` yalnızca açıklama ile ve yalnızca üçüncü taraf tip hatası için yazılır (`@typescript-eslint/ban-ts-comment`).

### 2.2 Tipler

| Kural | Ne | Denetleyen |
| --- | --- | --- |
| **FE-TS-03** | `any` yasaktır; bilinmeyen veri `unknown` alır ve daraltılır. | `no-explicit-any`, `no-unsafe-*` |
| **FE-TS-04** | Non-null assertion (`value!`) yasaktır; guard veya erken çıkış yazılır. | `no-non-null-assertion` |
| **FE-TS-05** | `enum` ve `namespace` yasaktır. Sabit değer kümeleri dize birleşim tipi veya `as const` nesne ile yazılır. Üretilmiş API tipleri bu kuralın dışındadır. | `no-restricted-syntax`, `no-namespace` |
| **FE-TS-06** | `as` ile tip zorlama yalnızca `as const` ve test verisinde serbesttir; üretim kodunda tip daraltma guard fonksiyonu veya Zod ile yapılır. | `consistent-type-assertions` (`assertionStyle: never` + `as const` serbest) |
| **FE-TS-07** | Birleşim tipleri üzerindeki `switch` kapsamlıdır; yeni bir durum eklendiğinde derleme kırılır. `default` dalı birleşim tiplerinde yazılmaz. | `switch-exhaustiveness-check` |
| **FE-TS-08** | Nesne şekilleri `type` ile tanımlanır; `interface` yalnızca üçüncü taraf tipini genişletmek (declaration merging) için kullanılır. | `consistent-type-definitions: type` |
| **FE-TS-09** | Tip import'ları `import type` ile yapılır. | `consistent-type-imports` |
| **FE-TS-10** | Promise'ler ya `await` edilir ya da açıkça `void` ile bırakılır; olay işleyicilere Promise dönen fonksiyon doğrudan verilmez. | `no-floating-promises`, `no-misused-promises` |
| **FE-TS-11** | Dışa açılan fonksiyonların dönüş tipi açıkça yazılır (bileşenler ve hook'lar dahil değil; onlarda çıkarım yeterlidir). | `explicit-module-boundary-types` (bileşen/hook hariç) |
| **FE-TS-12** | Eşitlik her zaman `===` / `!==` ile yapılır. | `eqeqeq` |

### 2.3 Durum tipleri

- **FE-TS-13 Geçersiz durum temsil edilemez.** Birbirini dışlayan durumlar ayrık birleşim (discriminated union) ile yazılır, birden çok boolean ile yazılmaz.

```ts
// YANLIŞ: isLoading + isError + data aynı anda tutarsız olabilir
type UploadState = { isUploading: boolean; isFailed: boolean; evidenceId?: string };

// DOĞRU
type UploadState =
  | { kind: 'queued' }
  | { kind: 'uploading'; progressPct: number }
  | { kind: 'failed'; reason: UploadFailureReason }
  | { kind: 'done'; evidenceId: EvidenceId };
```

- **FE-TS-14** Kimlikler markalı (branded) tiptir ve birbirine karışamaz: `type ShipmentId = string & { readonly __brand: 'ShipmentId' }`. Markalama Orval çıktısını saran `packages/api-client` katmanında yapılır.

---

## 3. Adlandırma ve dil

### 3.1 Dil kararı

- **FE-NAME-01** Kod tanımlayıcıları, dosya ve klasör adları İngilizcedir ve yalnızca ASCII karakter içerir (Java NAME-01, NAME-03 ile aynı).
- **FE-NAME-02** Domain terimleri Java standartları **Ek A** sözlüğünden alınır (`Subcontractor`, `DeliveryReceipt`, `Discrepancy`...). Sözlükte olmayan terim önce oraya eklenir.
- **FE-NAME-03** Yorumlar, test açıklamaları ve commit açıklamaları Türkçedir. Kullanıcıya gösterilen metinler yalnızca i18n dosyasındadır (FE-UI-05).
- **FE-NAME-04** API sözleşmesinden gelen değerler (izin anahtarları `SEVKIYAT_OLUSTUR`, durumlar `AWAITING_DELIVERY`) olduğu gibi kullanılır, frontend'de çevrilmez veya yeniden adlandırılmaz.
- **FE-NAME-05** Büyük/küçük harf dönüşümünde Türkçe yerel ayar tuzağına dikkat edilir: teknik metinlerde `toUpperCase()`/`toLowerCase()` kullanılır (JavaScript bunları yerel ayardan bağımsız çalıştırır); `toLocaleUpperCase()` yalnızca kullanıcıya gösterilen metinde ve `'tr-TR'` argümanıyla çağrılır. Türkçe sıralama `Intl.Collator('tr-TR')` ile yapılır.

### 3.2 Biçim kuralları

| Öğe | Biçim | Örnek | Denetleyen |
| --- | --- | --- | --- |
| Klasör | `kebab-case` | `master-data/`, `delivery-receipt/` | `check-file/folder-naming-convention` |
| Bileşen dosyası | `PascalCase.tsx`, içindeki bileşenle aynı ad; bileşen olmayan `.tsx` (`router.tsx`, `main.tsx`) `camelCase` | `ShipmentList.tsx` | `check-file/filename-naming-convention` (ad eşleşmesi review) |
| Hook dosyası | `useCamelCase.ts` | `usePermission.ts` | aynı |
| Diğer dosyalar | `camelCase.ts` | `shipmentSchema.ts`, `kilogram.ts` | aynı |
| Test dosyası | test edilen dosya + `.test.ts(x)`, yanında | `ShipmentList.test.tsx` | aynı |
| Bileşen | `PascalCase`, isim | `DeliveryReceiptWizard` | `naming-convention` |
| Hook | `use` + `PascalCase` | `useShipmentFile` | `react-hooks/rules-of-hooks` |
| Tip | `PascalCase`, `I`/`T` öneki yok | `ShipmentSummary` | `naming-convention` |
| Props tipi | bileşen adı + `Props` | `ShipmentListProps` | `naming-convention` |
| Fonksiyon, değişken | `camelCase` | `calculateNetKg` | `naming-convention` |
| Modül düzeyi sabit | `UPPER_SNAKE_CASE` | `MAX_PHOTO_EDGE_PX` | `naming-convention` |
| Boolean | `is` / `has` / `can` / `should` öneki | `isLocked`, `canDecide` | `naming-convention` |
| Olay prop'u / işleyici | prop `onX`, bileşen içindeki işleyici `handleX` | `onSubmit` → `handleSubmit` | review |

- **FE-NAME-06** Kısaltma kullanılmaz (`shp`, `qty`, `btn`, `msg` yok). Kabul edilenler Java NAME-06 listesi ve `props`, `ref`, `ui`.
- **FE-NAME-07** Birim adın parçasıdır: `netKg`, `diffPct`, `timeoutMs`, `maxEdgePx` (Java NAME-28).
- **FE-NAME-08** Fonksiyon fiil sözlüğü Java NAME-20 ile aynıdır: `find…` bulunamayabilir, `get…` mutlaka vardır, `calculate…` yan etkisiz hesap, `to…` dönüşüm, `format…` görüntüleme metni üretir, `parse…` metinden değer çıkarır.
- **FE-NAME-09** Yasaklı belirsiz adlar: `data`, `info`, `item`, `obj`, `temp`, `handle` (tek başına), `process`, `manager`, `helper`, `utils` (dosya veya klasör adı olarak da). Yardımcı fonksiyonlar ait oldukları kavramın dosyasına konur (`kilogram.ts`, `shipmentStatus.ts`).

---

## 4. Kod boyutu, karmaşıklık ve yapı sınırları

Bu bölümdeki sayılar Java dokümanındaki gibi **pazarlık konusu değildir**. Hepsi ESLint ile `error` seviyesinde denetlenir; CI'da `pnpm lint` kırılır ve PR birleştirilemez. Baseline veya toplu `eslint-disable` kullanılmaz. Sınırı aşan kodun çözümü sınırı esnetmek değil bölmektir: uzun bileşen alt bileşenlere, uzun fonksiyon küçük fonksiyonlara, çok parametreli fonksiyon tek nesne parametresine ayrılır.

### 4.1 Boyut sınırları

| Kural | Ne | Sınır (üretim) | Sınır (test) | ESLint kuralı |
| --- | --- | --- | --- | --- |
| **FE-SIZE-01** | Dosya uzunluğu, boş satır ve yorumlar hariç | **≤ 250 satır** | ≤ 400 satır | `max-lines` |
| **FE-SIZE-02** | Fonksiyon / bileşen uzunluğu (JSX dahil), boş satır ve yorumlar hariç | **≤ 60 satır** | kapalı (FE-SIZE-01 geçerli) | `max-lines-per-function` |
| **FE-SIZE-03** | Fonksiyondaki ifade (statement) sayısı | **≤ 15** | kapalı | `max-statements` |
| **FE-SIZE-04** | Fonksiyon parametre sayısı; fazlası tek nesne parametresine alınır | **≤ 3** | ≤ 3 | `max-params` |
| **FE-SIZE-05** | Satır uzunluğu | **≤ 100 karakter** | ≤ 100 | Prettier `printWidth` |
| **FE-SIZE-06** | JSX iç içe derinliği | **≤ 6** | — | `react/jsx-max-depth` |
| **FE-SIZE-07** | İç içe callback derinliği | **≤ 3** | ≤ 4 | `max-nested-callbacks` |
| **FE-SIZE-08** | Dosya başına bileşen sayısı | **1** | — | `react/no-multi-comp` |
| **FE-SIZE-09** | Bileşen props alanı sayısı | **≤ 8** (ÖNERİLEN) | — | review |

### 4.2 Karmaşıklık sınırları

| Kural | Ne | Sınır | ESLint kuralı |
| --- | --- | --- | --- |
| **FE-CPLX-01** | Döngüsel (cyclomatic) karmaşıklık | **≤ 10** | `complexity` |
| **FE-CPLX-02** | Bilişsel (cognitive) karmaşıklık | **≤ 12** | `sonarjs/cognitive-complexity` |
| **FE-CPLX-03** | Blok iç içe derinliği (`if`, döngü, `try`) | **≤ 3** | `max-depth` |
| **FE-CPLX-04** | İç içe üçlü operatör (`a ? b ? c : d : e`) | **yasak** | `no-nested-ternary` |
| **FE-CPLX-05** | JSX içinde iç içe koşul (`cond && (x ? <A/> : <B/>)`) | **yasak**; ayrı bileşen veya erken `return` | `sonarjs/no-nested-conditional` |
| **FE-CPLX-06** | Tekrarlanan dize sabiti | **≤ 3 kez** | `sonarjs/no-duplicate-string` |

- **FE-CPLX-07** Erken çıkış (guard clause) kullanılır; `return` ile biten `if`'ten sonra `else` yazılmaz (`no-else-return`).
- **FE-CPLX-08** Yükleniyor / hata / boş / veri durumları tek bileşende iç içe koşullarla değil, sırayla erken `return` ile yazılır:

```tsx
export function ShipmentList({ filter }: ShipmentListProps) {
  const query = useListShipments(filter);
  if (query.isPending) return <PageLoader />;
  if (query.isError) return <ProblemAlert error={query.error} />;
  if (query.data.items.length === 0) return <EmptyState messageKey="shipment.list.empty" />;
  return <ShipmentTable shipments={query.data.items} />;
}
```

### 4.3 Yapı ve biçim

| Kural | Ne | Denetleyen |
| --- | --- | --- |
| **FE-STRUC-01** | Biçim tartışılmaz: Prettier (tek tırnak, noktalı virgül, `printWidth: 100`, `trailingComma: all`). | `prettier --check` |
| **FE-STRUC-02** | Birden çok satıra yayılan her `if`, `else`, `for`, `while` gövdesi süslü parantezlidir. Parantezsiz gövde yalnızca koşulla aynı satırdaki tek ifadede serbesttir (FE-CPLX-08'deki erken `return`). | `curly: multi-line` |
| **FE-STRUC-03** | Varsayılan dışa aktarım (`export default`) yasaktır; yalnızca araç ayar dosyaları (`vite.config.ts` vb.) hariçtir. Lazy route'lar adlandırılmış `Component` dışa aktarımını kullanır. | `import-x/no-default-export` |
| **FE-STRUC-04** | Sihirli sayı yasaktır; `-1, 0, 1, 2, 100` dışındaki sayılar adlandırılmış sabittir. Testler, CSS benzeri stil değerleri ve dizi indeksleri hariçtir. | `no-magic-numbers` |
| **FE-STRUC-05** | `public` API'de boolean bayrak parametresi yazılmaz (`save(true)` değil `saveDraft()`); bileşenlerde boolean prop serbesttir. | review |
| **FE-STRUC-06** | Kullanılmayan değişken, import ve dışa aktarım bırakılmaz. | `no-unused-vars`, `tsc` |
| **FE-STRUC-07** | `console.*` yasaktır; hata izleme `packages/ui`'deki `reportError` ile yapılır. | `no-console` |
| **FE-STRUC-08** | Yorum satırına alınmış kod yasaktır; `TODO` yalnızca issue numarasıyla (`// TODO(#42): ...`). | `sonarjs/no-commented-code`, `no-warning-comments` |
| **FE-STRUC-09** | Yorum **neden**i anlatır, **ne**yi değil (Java COMM-01). | review |

---

## 5. Modül sınırları ve import kuralları

Klasör yapısı mimari dokümandadır (`apps/web/src/app`, `modules/*`, `offline/`, `packages/*`). Bu bölüm o yapının nasıl korunacağını söyler.

| Kural | Ne | Denetleyen |
| --- | --- | --- |
| **FE-MOD-01** | Bir modül başka modülün yalnızca `index.ts`'ini import eder; iç dosyalara (`modules/shipment/components/...`) dışarıdan erişilmez. | `boundaries/dependencies` |
| **FE-MOD-02** | İzinli bağımlılık yönü: `app → modules → packages`; `modules → offline → packages`; sahte API (`mocks/`) yalnızca modüllerin `index.ts`'ini kullanır. Ters yön ve `packages → apps` yasaktır. Test dosyaları bu kuralın dışındadır. | `boundaries/dependencies` |
| **FE-MOD-03** | Döngüsel import yasaktır. | `import-x/no-cycle` |
| **FE-MOD-04** | `@mantine/*` yalnızca `packages/ui` içinde import edilir; uygulama kodu `@iplik/ui` kullanır (F-05). | `no-restricted-imports` |
| **FE-MOD-05** | `axios` yalnızca `packages/api-client` içinde import edilir; `fetch` doğrudan çağrılmaz. | `no-restricted-imports`, `no-restricted-globals` |
| **FE-MOD-06** | `packages/api-client/src/generated` elle düzenlenmez ve lint'ten muaftır; CI `pnpm api:generate` sonrası fark olmadığını kontrol eder. | CI |
| **FE-MOD-07** | Paketler arası import paket adıyla (`@iplik/ui`), modül içi import göreli yolla yapılır; `../../..` gibi üç seviye yukarı çıkan göreli import yasaktır, `@/` takma adı kullanılır. | `no-restricted-imports` (desen) |
| **FE-MOD-08** | Modülün `index.ts`'i yalnızca dışa açılanı yeniden dışa aktarır (`export { ShipmentRoutes } from './routes'`); `export *` yasaktır. | `no-restricted-syntax` |

---

## 6. React bileşenleri ve hook'lar

### 6.1 Bileşenler

- **FE-REACT-01** Yalnızca fonksiyon bileşeni yazılır; sınıf bileşeni yalnızca hata sınırı (`ErrorBoundary`) için `packages/ui`'de bulunur (`react/prefer-stateless-function`).
- **FE-REACT-02** Props nesnesi parametrede açılır ve tipi ayrı bir `…Props` tipidir. `React.FC` kullanılmaz.
- **FE-REACT-03** Liste elemanlarının `key`'i kararlı kimliktir; dizi indeksi `key` olamaz (`react/no-array-index-key`, `react/jsx-key`).
- **FE-REACT-04** Bileşen içinde bileşen tanımlanmaz (`react/no-unstable-nested-components`).
- **FE-REACT-05** Sayfa bileşenleri (`pages/`) veriyi hook ile çeker ve alt bileşenlere props olarak verir; alt bileşenler (`components/`) API'yi doğrudan çağırmaz. ÖNERİLEN.
- **FE-REACT-06** `useMemo`, `useCallback` ve `memo` ölçülmüş bir performans sorunu olmadan yazılmaz; React Compiler açıksa hiç yazılmaz. ÖNERİLEN.

### 6.2 Hook'lar

- **FE-HOOK-01** Hook kuralları ve bağımlılık dizileri tam olarak uyulur (`react-hooks/rules-of-hooks`, `react-hooks/exhaustive-deps` hata seviyesinde). Bağımlılık dizisi `eslint-disable` ile susturulmaz.
- **FE-HOOK-02** `useEffect` sunucu verisi çekmek, türetilmiş state hesaplamak veya props'u state'e kopyalamak için kullanılmaz. Yalnızca dış sistemle eşitleme içindir (kamera, `online` olayı, SSE). Her effect temizleme fonksiyonu döner.
- **FE-HOOK-03** Bir bileşende 3'ten fazla `useState` veya karmaşık geçiş varsa durum `useReducer` veya ayrık birleşim tipiyle (FE-TS-13) tutulur. ÖNERİLEN.
- **FE-HOOK-04** Tekrar kullanılan mantık bileşen yerine özel hook'a alınır; hook JSX döndürmez.

---

## 7. Sunucu verisi, API ve hata yönetimi

- **FE-API-01** API istemcisi, tipleri ve TanStack Query hook'ları backend'in `src/main/resources/openapi/<servis>-api.yaml` sözleşmesinden (Java API-01) Orval ile üretilir. Elle `axios.get('/api/...')` yazılmaz; sözleşmede olmayan uç kullanılmaz.
- **FE-API-02** Sunucu verisi yalnızca TanStack Query'de yaşar; Zustand'a veya `useState`'e kopyalanmaz. Zustand yalnızca mimari dokümandaki istemci durumu için kullanılır (offline kuyruk özeti, UI tercihleri).
- **FE-API-03** Sorgu anahtarları Orval'ın ürettiği `get…QueryKey` fonksiyonlarıyla oluşturulur; elle dizi yazılmaz. Mutasyondan sonra ilgili anahtar `invalidateQueries` ile geçersiz kılınır.
- **FE-API-04** Kilo değerleri API'den **dize** gelir (Java API-03) ve dize olarak taşınır; `Number()` / `parseFloat()` ile sayıya çevrilmez (FE-NUM-01).
- **FE-API-05** Hatalar RFC 9457 Problem Details'tir. Kararlar `code` alanına göre verilir, `title`/`detail` metnine göre verilmez. Alan hataları (`errors[]`) `applyProblemToForm` ile ilgili form alanına bağlanır.
- **FE-API-06** HTTP durumlarının ortak karşılığı tek yerde (`packages/api-client`) tanımlıdır: 401 → giriş yönlendirmesi (offline kuyruk doluysa önce uyarı), 403 → yetki sayfası, 404 → bulunamadı, 409 → "kayıt başkası tarafından güncellendi / kilitli", 422 → form alanı hatası. Bileşenler bu ayrımı tekrar yazmaz.
- **FE-API-07** Durum değiştiren her istek `Idempotency-Key` başlığı taşır; anahtar işlem kaydedildiği anda üretilir ve yeniden denemede aynı kalır (Java API-04).
- **FE-API-08** Liste uçları sayfalıdır (`page`, `size`, `sort`); filtre ve sayfa bilgisi URL query parametresinde tutulur, böylece link paylaşılabilir.
- **FE-API-09** Tarih-saat API'de ISO 8601 UTC'dir; ekranda `Europe/Istanbul` ile `packages/i18n` biçimlendiricisinden gösterilir. `new Date().toLocaleString()` doğrudan kullanılmaz.

---

## 8. Formlar, kilo ve sayılar

- **FE-FORM-01** Formlar React Hook Form + Zod ile yazılır; şema modülün `schemas/` klasöründedir ve form tipi şemadan çıkarılır (`z.infer`). Aynı form için ayrı elle yazılmış tip olmaz.
- **FE-FORM-02** Zod şeması yalnızca biçim doğrulaması yapar (zorunlu alan, biçim, aralık). İş kuralları (tolerans, dara sapması) backend'dedir; frontend en fazla uyarı gösterir, kaydı engellemez.
- **FE-FORM-03** Türetilmiş değerler kullanıcıdan istenmez: net kilo = brüt − dara otomatik hesaplanır ve salt okunur gösterilir.
- **FE-NUM-01 Kilo ve yüzde için JavaScript `number` aritmetiği yasaktır.** Hesaplama `@iplik/units` paketindeki `Kilogram` ve `Percentage` yardımcılarıyla (ondalık kütüphane `big.js` üzerinde) yapılır. `0.1 + 0.2 !== 0.3` sevkiyat farkında yanlış bant rengi demektir.
- **FE-NUM-02** Kullanıcı girişi Türkçe biçimdedir (`1.982,5`); `parseKg` ile API biçimine (`"1982.500"`) çevrilir, `formatKg` ile geri gösterilir. Biçimlendirme `Intl.NumberFormat('tr-TR')` ile yapılır, elle `replace('.', ',')` yazılmaz.
- **FE-NUM-03** Kilo alanları her zaman `KgInput` bileşeniyle girilir (`inputMode="decimal"`, ondalık virgül, en fazla 3 ondalık hane).

---

## 9. Yetki, kör giriş ve güvenlik

- **FE-SEC-01 Token tarayıcıya inmez.** Frontend token okumaz, saklamaz, yenilemez; `Authorization` başlığı yazmaz. Oturum gateway'in `HttpOnly` çerezidir (yetkilendirme analizi, BFF). `jwt-decode` vb. kütüphaneler bağımlılık listesine giremez.
- **FE-SEC-02** Görünürlük yalnızca izin anahtarıyla karar verilir: `usePermission('UYUSMAZLIK_KARAR')` veya `<Can permission="...">`. Rol adına göre dallanma (`if (role === 'ADMIN')`) yasaktır (`no-restricted-syntax`, rol dizesi deseni).
- **FE-SEC-03** Aksiyon butonları yalnızca kaydın `allowedActions` listesinde ilgili aksiyon varsa gösterilir; durumdan aksiyon türetilmez (`status === 'LOADED'` → buton göster yazılmaz).
- **FE-SEC-04 Kör giriş alanı gizlenmez, istenmez.** Fasoncu ve depo ekranları kör alanları içermeyen görünüm uçlarını kullanır; çıkış kilosu gibi bir alan yanıtta varsa "CSS ile gizle" veya "render etme" çözümü kabul edilmez, backend'e hata kaydı açılır. MSW sahte yanıtları da aynı kurala uyar (FE-TEST-05).
- **FE-SEC-05** `localStorage` ve `sessionStorage` yalnızca UI tercihleri için kullanılır (tema, tablo kolon düzeni). Kişisel veri, sevkiyat verisi ve kanıt dosyası yalnızca offline kuyruk için IndexedDB'de (Dexie) tutulur ve gönderildikten sonra silinir (`no-restricted-globals` yalnızca `packages/ui/preferences` içinde serbest).
- **FE-SEC-06** `dangerouslySetInnerHTML`, `eval`, `new Function` ve `innerHTML` ataması yasaktır (`react/no-danger`, `no-eval`, `no-implied-eval`).
- **FE-SEC-07** Dış bağlantılar `rel="noreferrer"` taşır (`react/jsx-no-target-blank`).
- **FE-SEC-08** Hata izleme ve log'a kişisel veri (ad, telefon, TCKN, plaka), oturum çerezi veya CSRF değeri gönderilmez.
- **FE-SEC-09** Ortama özel adresler build'e gömülmez; `/config.json`'dan okunur. `import.meta.env` yalnızca `apps/web/src/app/config.ts` içinde kullanılır.

---

## 10. UI, erişilebilirlik ve metinler

- **FE-UI-01** Ortak bileşenler `@iplik/ui`'den gelir (`DataTable`, `KgInput`, `StatusBadge`, `CameraCapture`). Aynı işi yapan ikinci bir bileşen modül içinde yazılmaz.
- **FE-UI-02** Renk, boşluk ve yazı boyutu tema token'larından gelir; bileşen içinde `#hex`, `rgb()` ve piksel değeri yazılmaz (`no-restricted-syntax` renk deseni).
- **FE-UI-03** Durum bilgisi yalnızca renkle verilmez; renk + metin (+ ikon) birlikte kullanılır (A/B kalite, Yeşil/Sarı/Kırmızı bant).
- **FE-UI-04** Erişilebilirlik kuralları hata seviyesindedir (`jsx-a11y/recommended`): her form alanının etiketi, her ikon butonun `aria-label`'ı vardır; saha ekranlarında dokunma hedefi en az 48 px'tir.
- **FE-UI-05** Kullanıcıya görünen hiçbir metin JSX'e veya koda gömülmez (test dosyaları hariç); `t('shipment.list.title')` ile `packages/i18n/src/locales/tr.json`'dan gelir (F-12). Anahtarlar `modül.ekran.öğe` biçimindedir, İngilizce ve `camelCase` segmentlidir.
- **FE-UI-06** Saha (`/saha`) ekranlarında bir ekranda tek ana aksiyon bulunur; masaüstü bileşenleri (geniş tablo) saha kabuğunda kullanılmaz. ÖNERİLEN.

---

## 11. Test kuralları

| Kural | Ne | Denetleyen |
| --- | --- | --- |
| **FE-TEST-01** | Birim ve bileşen testleri Vitest + Testing Library; test dosyası test edilen dosyanın yanındadır. | `check-file`, Vitest |
| **FE-TEST-02** | `describe` test edilen birimin adıdır, `it` Türkçe davranış cümlesidir: `it('dara bruttan büyükse net kiloyu boş bırakır')`. | review |
| **FE-TEST-03** | Elemanlar erişilebilir rolleriyle bulunur (`getByRole`, `getByLabelText`); `container.querySelector`, sınıf adı ve `data-testid` yalnızca başka yol yoksa kullanılır. | `testing-library/*` |
| **FE-TEST-04** | Ağ yalnızca MSW ile taklit edilir; `axios` veya üretilmiş istemci `vi.mock` ile taklit edilmez. Testte gerçek ağ isteği hata verir (`onUnhandledRequest: 'error'`). | MSW ayarı |
| **FE-TEST-05** | MSW sahte yanıtları rol ve kör giriş kuralına uyar: fasoncuya dönen teslim görünümünde çıkış alanları yoktur. Bunu doğrulayan test zorunludur. | test |
| **FE-TEST-06** | Testlerde `it.only`, `describe.only` ve gerekçesiz `it.skip` yasaktır. | `vitest/no-focused-tests`, `vitest/no-disabled-tests` |
| **FE-TEST-07** | Öncelikli kapsam: `@iplik/units` (kilo/yüzde) **%100 satır ve dal**, `@iplik/api-client` hata eşlemesi ve yetki bileşenleri (`Can`, `usePermission`) **%90**, uygulama geneli **%70 satır**. Eşiğin altı CI'ı kırar. | Vitest `coverage.thresholds` |
| **FE-TEST-08** | Uçtan uca testler Playwright ile; mimari dokümandaki zorunlu senaryolar (tam döngü, kör giriş, offline fotoğraf, Yönetici'de düzenleme butonu olmaması) her PR'da değil, `main`'e birleşmede çalışır. | CI |
| **FE-TEST-09** | Erişilebilirlik: sayfa testlerinde `axe` kontrolü ihlal olmadan geçer. ÖNERİLEN, Faz 1 sonunda ZORUNLU. | `vitest-axe` |

---

## 12. Araç zinciri ve bağımlılıklar

- **FE-BUILD-01** Node 22 LTS ve pnpm (sürüm `packageManager` alanında sabit). `npm` ve `yarn` kilit dosyası commit edilmez. TypeScript 6.0 ve ESLint 9 kullanılır: `typescript-eslint` henüz TypeScript 7'yi, `eslint-plugin-react` ve `eslint-plugin-jsx-a11y` henüz ESLint 10'u desteklemiyor. Destek gelince yükseltilir.
- **FE-BUILD-02** Bağımlılık sürümleri `pnpm-lock.yaml` ile sabittir; CI `pnpm install --frozen-lockfile` çalıştırır.
- **FE-BUILD-03** Yeni bağımlılık PR açıklamasında gerekçelendirilir: ne işe yaradığı, lisansı (MIT/Apache/BSD; GPL yasak) ve paket boyutu. Aynı işi yapan ikinci kütüphane eklenmez (iki tarih kütüphanesi, iki HTTP istemcisi vb.).
- **FE-BUILD-04** Ortak ESLint, TypeScript, Prettier ve Vitest ayarları `packages/config`'tedir; uygulamalar ve paketler bunları genişletir, kural kapatmaz.
- **FE-BUILD-05** CI adımları sırayla: `pnpm install --frozen-lockfile` → `pnpm format:check` → `pnpm lint` → `pnpm typecheck` → `pnpm test --coverage` → `pnpm build`. Herhangi biri kırılırsa PR birleştirilemez.
- **FE-BUILD-06** Commit öncesi `lint-staged` değişen dosyalarda Prettier ve ESLint çalıştırır; `--no-verify` ile atlanan ihlal CI'da yine yakalanır.
- **FE-BUILD-07** Ana sayfa paket boyutu (gzip) saha kabuğu için **≤ 250 KB**'dir; `size-limit` CI'da kontrol eder. ÖNERİLEN, Faz 1 sonunda ZORUNLU.

---

## 13. Git, PR ve istisna süreci

Java standartları bölüm 16 (GIT-01…10) frontend için de aynen geçerlidir. Frontend'e özgü farklar:

- **FE-GIT-01** Commit ve PR başlığı kapsamı modül veya paket adıdır: `feat(shipment): sevkiyat listesi eklendi`, `chore(config): eslint sınırları`.
- **FE-GIT-02** Bitti tanımına ek olarak: OpenAPI değiştiyse üretilmiş istemci aynı PR'da güncellenmiştir; yeni ekran için MSW handler'ı ve en az bir bileşen testi vardır; yeni metinler `tr.json`'dadır.
- **FE-GIT-03** Tek seferlik kural istisnası yalnızca satır düzeyinde yazılır ve gerekçe taşır: `// eslint-disable-next-line max-params -- İstisna: FE-SIZE-04, üçüncü taraf callback imzası`. Dosya düzeyinde `/* eslint-disable */` yasaktır (`eslint-comments/no-use` yerine `--report-unused-disable-directives` ve `eslint-comments/require-description`).
- **FE-GIT-04** Bu dokümanın değiştirilmesi Java GIT-09 ile aynıdır: PR + proje sahibinin onayı. Bir sınır değişirse `packages/config/eslint` ayarı aynı PR'da değişir.

---

## Ek A: Otomatik denetim özeti

| Kural(lar) | Nerede denetlenir |
| --- | --- |
| FE-TS-01 | `tsconfig.base.json` + `pnpm typecheck` (`tsc --noEmit`) |
| FE-TS-02…12, FE-NAME-01, FE-NAME-05 (tablo), FE-STRUC-02…04, 06…08 | ESLint: `typescript-eslint` (`strictTypeChecked`), çekirdek kurallar |
| FE-SIZE-01…08, FE-CPLX-01…07 | ESLint: çekirdek kurallar, `eslint-plugin-react`, `eslint-plugin-sonarjs` |
| FE-NAME (dosya ve klasör adları) | `eslint-plugin-check-file` |
| FE-MOD-01…05, 07, 08 | `eslint-plugin-boundaries`, `eslint-plugin-import-x`, `no-restricted-imports` |
| FE-MOD-06 | CI: `pnpm api:generate && git diff --exit-code` |
| FE-REACT-01…04, FE-HOOK-01 | `eslint-plugin-react`, `eslint-plugin-react-hooks` |
| FE-SEC-02, 05…07, 09, FE-UI-02 | ESLint `no-restricted-syntax` / `no-restricted-globals`, `eslint-plugin-react` |
| FE-UI-04 | `eslint-plugin-jsx-a11y` |
| FE-UI-05 | `eslint-plugin-i18next` (`no-literal-string`, JSX metinleri) |
| FE-TEST-03, 06 | `eslint-plugin-testing-library`, `@vitest/eslint-plugin` |
| FE-TEST-07 | Vitest `coverage.thresholds` |
| FE-STRUC-01, FE-SIZE-05 | Prettier `--check` |
| FE-GIT-03 | ESLint `--report-unused-disable-directives`, `eslint-plugin-eslint-comments` |
| Diğerleri | Code review (kural kimliğiyle) |

Ayarların kendisi `frontend/packages/config/` altındadır ve bu dokümanla aynı PR'da değişir; doküman ile ayar arasında fark olursa ayar düzeltilir.
