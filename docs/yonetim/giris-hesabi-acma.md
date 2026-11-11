# Giriş hesabı açma

> Sisteme yalnızca fabrika personeli (yönetici, depo, kalite, müdür, genel müdür) giriş yapar; fasoncu ve şoför giriş
> yapmaz, yalnızca ana veridir (KR-17). **SMS ya da e-posta doğrulaması yoktur:** hesabı yönetici açar, ilk (geçici)
> şifreyi kendisi yazar ve kişiye iletir; kişi ilk girişte şifresini değiştirir. Fabrikada tek merkez lokasyon olduğu
> için yeni hesap etkin tüm lokasyonları görür (lokasyon seçilmez).

Hesap **Kullanıcılar** ekranından (`/panel/kullanicilar`, yalnızca `USER_MANAGE`) açılır; Keycloak adımlarını sistem yapar.

## Kurulum: servis hesabı istemcisi

shipment-service Keycloak'a `yarnshipment-admin` servis hesabıyla bağlanır (yalnızca `manage-users`, `view-users`,
`view-realm` yetkisi). Sırrı `YARNSHIPMENT_KEYCLOAK_ADMIN_CLIENT_SECRET` ortam değişkeniyle verilir; boşsa uçlar
502 döner.

- **Yeni kurulum**: istemci `deploy/keycloak/yarnshipment-realm-local.json` ile gelir. Sır `deploy/.env` içindeki
  `KEYCLOAK_ADMIN_CLIENT_SECRET` değeridir (yoksa `local-admin-client-secret`).
- **Daha önce kurulmuş Keycloak**: realm dosyası yalnızca realm yokken içe aktarıldığı için istemciyi şu betikle ekleyin:

  ```
  KEYCLOAK_ADMIN_PASSWORD=<deploy/.env değeri> KEYCLOAK_ADMIN_CLIENT_SECRET=<seçtiğiniz sır> \
    node tools/keycloak/setup-admin-client.mjs
  ```

  Ardından shipment-service'i aynı sırla başlatın:
  `YARNSHIPMENT_KEYCLOAK_ADMIN_CLIENT_SECRET=<aynı sır>`.

## Sonradan

- Hesabı Kullanıcılar ekranından "Pasife al" ile kapatın; kayıtların (şoför, fasoncu) pasife alınması hesabı kapatmaz.
- Doğrulama: kullanıcıyla `http://localhost:5173` adresinden giriş yapın. Token'daki claim'leri `tools/keycloak/verify-realm.mjs` ile görebilirsiniz.

## Kullanıcılar ekranı

| İşlem | Uç |
|---|---|
| Fabrika içi hesap aç | `POST /api/v1/master-data/login-accounts` (`username`, `fullName`, `role`, `password`) → 201 |
| Rol değiştir | `PUT /api/v1/master-data/login-accounts/{userId}/role` (`role`) → 204 |
| Pasife al / yeniden aç | `PUT /api/v1/master-data/login-accounts/{userId}/enabled` (`enabled`) → 204 |
| Yeni geçici şifre ver | `PUT /api/v1/master-data/login-accounts/{userId}/password` (`password`) → 204 |

`role` yalnızca `ADMIN`, `WAREHOUSE`, `QUALITY`, `MANAGER`, `GENERAL_MANAGER` olabilir. Rol değişince eski fabrika içi rol
kaldırılır ve kişinin açık oturumları kapatılır. Kişi kendi rolünü değiştiremez ve kendi hesabını kapatamaz (400
`INVALID_MASTER_DATA`); kullanıcı yoksa 404 `LOGIN_ACCOUNT_NOT_FOUND`. Giriş adı alınmışsa 409 `LOGIN_ACCOUNT_EXISTS`.
Şifre kurala uymuyorsa 400 `WEAK_PASSWORD`. Rol verilemezse açılan kullanıcı silinir; yarım hesap kalmaz.

## Şifre

1. **Hesap açılınca** yönetici geçici şifreyi yazar (en az 8 karakter; Keycloak'ın şifre politikası da geçerlidir) ve
   kişiye iletir. Şifre hiçbir yanıtta ya da logda yer almaz.
2. **İlk giriş**: hesap "İlk giriş bekleniyor" görünür; Keycloak ilk girişte şifre değiştirtir (`UPDATE_PASSWORD`).
3. **Şifresini unutan**: giriş ekranında "Şifremi unuttum" yoktur; yönetici Kullanıcılar ekranında "Şifre ver" ile yeni
   geçici şifre belirler. Kişinin açık oturumları kapanır, bir sonraki girişte şifresini değiştirir.
4. **Profilden değiştirme**: giriş yapmış herkes üst çubuktaki "Şifremi değiştir" ile Keycloak'ın şifre ekranına gider
   (`kc_action=UPDATE_PASSWORD`); şifre bu uygulamadan geçmez.

### Giriş ve şifre değiştirme ekranlarının görünümü

Aynı `yarnshipment` teması iki ekranın da görünümünü uygulamayla eşler (`resources/js/branding.js`): marka gradyanlı
arka plan, beyaz kart, marka renkli düğme ve firma adı. Renk ve ad `resources/js/frontend-config.js` içindeki
`YARNSHIPMENT_BRAND_COLOR` ve `YARNSHIPMENT_BRAND_NAME` değerlerinden gelir. Her firmanın kurulumunda bu iki değeri,
uygulamadaki **Görünüm** ekranında seçilen renk ve adla aynı yapın; Keycloak sayfası uygulamanın dışında olduğu için
ekrandaki değişiklik oraya kendiliğinden yansımaz. Stil PatternFly 5 sınıflarına bağlıdır (Keycloak 26.x); Keycloak'ı
yükseltirken giriş ekranına bir kez bakın.
