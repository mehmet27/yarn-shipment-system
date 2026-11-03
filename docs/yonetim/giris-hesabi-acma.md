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

## Depo, kalite, müdür ve yönetici hesabı; rol değiştirme

Fabrika içi hesaplar **Kullanıcılar** ekranından (`/panel/kullanicilar`, yalnızca `USER_MANAGE`) açılır. Şifre hiçbir
yerde üretilmez, sorulmaz ya da gösterilir: Keycloak kullanıcıyı şifresiz açar, rolü verir ve kişiye şifre belirleme
bağlantısı (`UPDATE_PASSWORD`, 3 gün geçerli) e-postayla gider.

| İşlem | Uç |
|---|---|
| Hesap aç | `POST /api/v1/master-data/login-accounts` (`username`, `email`, `fullName`, `role`, `siteIds`) → 201, gövde yok |
| Rol değiştir | `PUT /api/v1/master-data/login-accounts/{userId}/role` (`role`) → 204 |

`role` yalnızca `ADMIN`, `WAREHOUSE`, `QUALITY`, `MANAGER` olabilir. Rol değişince eski fabrika içi rol kaldırılır ve
kişinin açık oturumları kapatılır. Kişi kendi rolünü değiştiremez (400 `INVALID_MASTER_DATA`); şoför/fasoncu hesabının
rolü değişmez (409 `LOGIN_ACCOUNT_ROLE_LOCKED`); kullanıcı yoksa 404 `LOGIN_ACCOUNT_NOT_FOUND`. Giriş adı ya da e-posta
alınmışsa 409 `LOGIN_ACCOUNT_EXISTS`. E-posta gönderilemezse (SMTP yok/yanlış) açılan kullanıcı silinir ve 502
`LOGIN_ACCOUNT_UNAVAILABLE` döner; yarım hesap kalmaz.

### SMTP

Davet e-postası Keycloak'tan gider, bu yüzden realm'de SMTP tanımlı olmalıdır. Yerelde `deploy/docker-compose.yml`
içindeki `mailpit` kullanılır (gelen kutusu http://localhost:8025); realm dosyası `KEYCLOAK_SMTP_HOST`, `_PORT`,
`_FROM` değerlerini okur. Daha önce kurulmuş Keycloak'ta `tools/keycloak/setup-admin-client.mjs` betiği SMTP'yi de
(tanımlı değilse) yazar. Canlıda gerçek SMTP bilgilerini aynı ortam değişkenleriyle verin.
