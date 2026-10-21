# İplik Sevkiyat – Frontend

React + TypeScript pnpm workspace. Mimari: [`docs/frontend/react-frontend-mimarisi.md`](../docs/frontend/react-frontend-mimarisi.md), kod kuralları: [`docs/standartlar/react-typescript-kod-standartlari.md`](../docs/standartlar/react-typescript-kod-standartlari.md).

| Klasör                | İçerik                                                               |
| --------------------- | -------------------------------------------------------------------- |
| `apps/web`            | Yönetim Paneli (`/panel`) ve Saha PWA (`/saha`)                      |
| `packages/config`     | Ortak ESLint ve TypeScript ayarları (kod standartlarındaki sınırlar) |
| `packages/ui`         | Mantine'i saran tasarım sistemi                                      |
| `packages/api-client` | Gateway istemcisi (oturum çerezi, CSRF, Problem Details)             |
| `packages/i18n`       | Türkçe metinler ve tarih biçimlendirme                               |
| `packages/units`      | Kilo ve yüzde hesabı (`number` aritmetiği yerine)                    |

## Çalıştırma

Node 22 gerekir (`engines` ve `engine-strict` ile zorunlu; `.nvmrc` var, `nvm use` ile seçilir). Node 24 gibi başka bir sürümde `pnpm install` hata verir. pnpm'i corepack getirir: `corepack enable` bir kez çalıştırılmalı, aksi halde `pnpm` komutu bulunamaz ve kökteki `pnpm dev` de içeride `pnpm` çağırdığı için çalışmaz.

```bash
corepack enable
pnpm install
pnpm dev            # http://localhost:5173, sahte API (MSW) açık
```

Sahte API'de rol, sayfa ilk açılırken adres çubuğundan seçilir: `/?mockRole=SUBCONTRACTOR` (`ADMIN`, `WAREHOUSE`, `DRIVER`, `SUBCONTRACTOR`, `QUALITY`, `MANAGER`). Gerçek gateway ile çalışmak için `VITE_USE_MOCK_API=false pnpm dev`; istekler `localhost:8080`'e yönlenir. Önce kökteki README'deki yerel ortamı (Postgres, Keycloak, gateway) başlatın; `deploy/.env` dosyanız eskiyse `deploy/.env.example` içindeki yeni değişkenleri (`KEYCLOAK_TEST_USER_PASSWORD`, `GATEWAY_CLIENT_SECRET`) ekleyin, yoksa compose başlamaz. Oturum yoksa uygulama `/oauth2/authorization/keycloak` adresine gider (gateway girişi yönetir); oturum sonradan düşerse ilk 401'de kullanıcı sorgusu yenilenir ve yine girişe yönlenir. Tarayıcıda token tutulmaz.

## Kontroller (CI ile aynı sıra, FE-BUILD-05)

```bash
pnpm format:check && pnpm lint && pnpm typecheck && pnpm test --coverage && pnpm build
```
