# Üretim Keycloak'ında OTP (TOTP) kurulumu

Kalan iş listesi madde 10 (bkz. `docs/yonetim/kalan-is-takip.md`). Servis tarafı hazırdır: `SecurityProperties`
varsayılanı ADMIN ve QUALITY rollerinin izinlerini yalnızca token'da `amr` içinde `otp`/`mfa` ya da `acr` = `2`
varsa verir (karar Y5). Yerel realm (`deploy/keycloak/yarnshipment-realm-local.json`) bilerek OTP'siz tutulur;
bu belge **üretim** realm'i içindir. Aşağıdaki adımlar Keycloak yönetim konsolunda uygulanır; bu depodan
otomatik uygulanmaz.

## 1. Akış

1. Realm › Authentication › `browser` akışını kopyala (`browser-iplik`).
2. Akışa **Conditional OTP** alt akışı ekle: koşul **Condition - User Role** (`ADMIN`, `QUALITY`),
   adım **OTP Form** = `REQUIRED`.
3. Realm › Authentication › Bindings › Browser flow = `browser-iplik`.
4. ADMIN ve QUALITY rolü olan kullanıcılara **Required action: Configure OTP** ata (ilk girişte kurulum).

## 2. Token'a `acr` / `amr`

1. Realm › Authentication › `browser-iplik`: OTP alt akışının **Level of Authentication** değerini 2 yap
   (parola alt akışı 1).
2. `iplik-gateway` client'ında `acr` claim'inin access token'a yazıldığını doğrula (Client scopes › `acr loa level`).
3. `amr` isteniyorsa client'a hardcoded/script yerine desteklenen bir mapper ekle; yalnızca `acr` = 2 de yeterlidir.

## 3. Doğrulama (hazırlık ortamında, ardından üretimde)

- [ ] ADMIN kullanıcısı parolayla giriş yapınca OTP kurulum ekranı gelir.
- [ ] Giriş sonrası `GET /api/v1/me` izinleri içinde ADMIN izinleri görünür; token'da `acr` = `2`.
- [ ] OTP'siz oturum (ör. akış devre dışıyken alınmış token) ADMIN/QUALITY izinlerini **alamaz** (403).
- [ ] WAREHOUSE / MANAGER / GENERAL_MANAGER girişinde OTP istenmez.
- [ ] Yedek: en az iki ADMIN'in OTP cihazı kayıtlı; kurtarma kodu süreci yazılı.

## 4. Geri alma

Bindings › Browser flow'u eski `browser` akışına çevir. Servis tarafı MFA'sız token'da ADMIN/QUALITY
izinlerini vermeyeceği için bu durumda bu roller erişimini kaybeder; geri almadan önce bunun bilinçli
olduğundan emin ol.
