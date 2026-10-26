# Şoför ve fasoncuya giriş hesabı açma

Şoför ve fasoncu kaydı uygulamada (`POST /api/v1/master-data/drivers` ve `/subcontractors`) açılır; **giriş hesabı**
Keycloak'ta tutulur. Hesabı yönetici uygulamadan açar, Keycloak adımlarını sistem yapar:

| Kayıt | Uç |
|---|---|
| Şoför | `POST /api/v1/master-data/drivers/{driverId}/login-account` |
| Fasoncu | `POST /api/v1/master-data/subcontractors/{subcontractorId}/login-account` |

İstek gövdesi: `username` (3-64 karakter; küçük harf, rakam, `.`, `_`, `-`), `email` ve `siteIds`
(kişinin sevkiyatlarını göreceği etkin lokasyonlar, en az bir tane). Yalnızca `MASTER_DATA_MANAGE` yetkisi olan
yönetici çağırabilir. Yanıt (201) giriş adını ve **geçici şifreyi** döner; şifre başka hiçbir yerde saklanmaz ya da
loglanmaz, yöneticinin kişiye iletmesi gerekir. Kişi ilk girişte şifresini değiştirmek zorundadır.

Sistem Keycloak'ta şunları yapar: kullanıcıyı açar (geçici şifre + `UPDATE_PASSWORD`), şoför için `driver`, fasoncu için
`subcontractor` rolünü verir, kayıt kimliğini (`driver_id` / `subcontractor_id`) ve `site_ids` özniteliklerini yazar.
Rol verilemezse açılan kullanıcı silinir; yarım hesap kalmaz.

| Durum | Yanıt |
|---|---|
| Kayıt yok ya da pasif | 404 `MASTER_DATA_NOT_FOUND` |
| Lokasyon boş, yok ya da pasif | 400 `INVALID_MASTER_DATA` (`errors[0].field` = `siteIds`) |
| Kayıt için hesap zaten var ya da giriş adı alınmış | 409 `LOGIN_ACCOUNT_EXISTS` |
| Keycloak'a ulaşılamadı ya da hesap açma yapılandırılmamış | 502 `LOGIN_ACCOUNT_UNAVAILABLE` |

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

- Kaydı uygulamada pasife almak giriş hesabını **kapatmaz**; kişi artık çalışmıyorsa Keycloak'ta kullanıcıyı
  devre dışı bırakın (`Enabled` kapalı). Bunun otomatikleştirilmesi sonraki iştir.
- Hesap açıldıktan sonra şifre sıfırlama ve lokasyon değiştirme Keycloak konsolundan yapılır.
- Doğrulama: kullanıcıyla `http://localhost:5173` adresinden giriş yapın; kendi sevkiyatlarını görmeli, başkalarınınkini
  görmemelidir. Token'daki claim'leri `tools/keycloak/verify-realm.mjs` ile görebilirsiniz.
