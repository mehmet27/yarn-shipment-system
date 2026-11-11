# Şoför ve fasoncuya giriş hesabı açma

Şoför ve fasoncu kaydı uygulamada (`POST /api/v1/master-data/drivers` ve `/subcontractors`) açılır; **giriş hesabı**
Keycloak'ta tutulur. Hesabı yönetici uygulamadan açar, Keycloak adımlarını sistem yapar:

| Kayıt | Uç |
|---|---|
| Şoför | `POST /api/v1/master-data/drivers/{driverId}/login-account` |
| Fasoncu | `POST /api/v1/master-data/subcontractors/{subcontractorId}/login-account` |

İstek gövdesi: `username` (3-64 karakter; küçük harf, rakam, `.`, `_`, `-`), `phone` (cep telefonu), isteğe bağlı
`email` ve `siteIds` (kişinin sevkiyatlarını göreceği etkin lokasyonlar, en az bir tane). Yalnızca
`MASTER_DATA_MANAGE` yetkisi olan yönetici çağırabilir. Yanıt (201) gövdesizdir: **şifre üretilmez ve hiçbir yerde
gösterilmez.** Kişinin telefonuna şifre belirleme kodu SMS'le gider (aşağıda "Şifre belirleme ve sıfırlama").

Sistem Keycloak'ta şunları yapar: kullanıcıyı şifresiz açar (`UPDATE_PASSWORD` bekler), şoför için `driver`, fasoncu için
`subcontractor` rolünü verir, kayıt kimliğini (`driver_id` / `subcontractor_id`) ve `site_ids` özniteliklerini yazar.
Rol verilemezse açılan kullanıcı silinir; yarım hesap kalmaz.

| Durum | Yanıt |
|---|---|
| Kayıt yok ya da pasif | 404 `MASTER_DATA_NOT_FOUND` |
| Lokasyon boş, yok ya da pasif | 400 `INVALID_MASTER_DATA` (`errors[0].field` = `siteIds`) |
| Kayıt için hesap zaten var ya da giriş adı alınmış | 409 `LOGIN_ACCOUNT_EXISTS` |
| Keycloak'a ulaşılamadı ya da hesap açma yapılandırılmamış | 502 `LOGIN_ACCOUNT_UNAVAILABLE` |
| SMS sağlayıcısı yok ya da ulaşılamadı (hesap açıldı, kod gitmedi) | 503 `SMS_UNAVAILABLE` |

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

- Kaydı uygulamada pasife almak giriş hesabını **kapatmaz**; hesabı Kullanıcılar ekranından "Pasife al" ile kapatın.
- Lokasyon değiştirme Keycloak konsolundan yapılır.
- Doğrulama: kullanıcıyla `http://localhost:5173` adresinden giriş yapın; kendi sevkiyatlarını görmeli, başkalarınınkini
  görmemelidir. Token'daki claim'leri `tools/keycloak/verify-realm.mjs` ile görebilirsiniz.

## Kullanıcılar ekranı: tüm roller

Tüm giriş hesapları **Kullanıcılar** ekranından (`/panel/kullanicilar`, yalnızca `USER_MANAGE`) yönetilir: depo, kalite,
müdür, yönetici, şoför ve fasoncu hesabı açılır; şoför ve fasoncu hesabı ilgili kayda bağlanarak açılır. Şifre hiçbir
yerde üretilmez, sorulmaz ya da gösterilir.

| İşlem | Uç |
|---|---|
| Fabrika içi hesap aç | `POST /api/v1/master-data/login-accounts` (`username`, `phone`, `email?`, `fullName`, `role`, `siteIds`) → 201 |
| Rol değiştir | `PUT /api/v1/master-data/login-accounts/{userId}/role` (`role`) → 204 |
| Pasife al / yeniden aç | `PUT /api/v1/master-data/login-accounts/{userId}/enabled` (`enabled`) → 204 |
| Şifre sıfırlamayı başlat | `POST /api/v1/master-data/login-accounts/{userId}/password-reset` → 204 |

`role` yalnızca `ADMIN`, `WAREHOUSE`, `QUALITY`, `MANAGER`, `GENERAL_MANAGER` olabilir. Rol değişince eski fabrika içi rol kaldırılır ve
kişinin açık oturumları kapatılır. Kişi kendi rolünü değiştiremez ve kendi hesabını kapatamaz (400
`INVALID_MASTER_DATA`); şoför/fasoncu hesabının rolü değişmez (409 `LOGIN_ACCOUNT_ROLE_LOCKED`); kullanıcı yoksa 404
`LOGIN_ACCOUNT_NOT_FOUND`. Giriş adı ya da e-posta alınmışsa 409 `LOGIN_ACCOUNT_EXISTS`. Rol verilemezse açılan
kullanıcı silinir; yarım hesap kalmaz. Telefon listede yalnızca son dört haneyle (`***4567`) görünür.

## Şifre belirleme ve sıfırlama (SMS)

Şifreyi her zaman kişinin kendisi belirler; yönetici göremez.

1. **Hesap açılınca** kişinin telefonuna giriş adını içeren bir SMS ve altı haneli kod gider (3 gün geçerli).
2. Kişi giriş ekranındaki **"Şifremi unuttum"** bağlantısından (`/sifremi-unuttum`) giriş adını yazar, SMS'teki kodla
   yeni şifresini belirler. Şifre en az 8 karakter olmalıdır; Keycloak'ın şifre politikası da geçerlidir.
3. **Şifresini unutan** aynı sayfadan kendisi kod ister (10 dakika geçerli); **yönetici** Kullanıcılar ekranında
   "Şifre sıfırla" ile kodu kişinin kayıtlı telefonuna gönderebilir (3 gün geçerli).
4. **İlk girişte şifre değişimi**: şifresini belirlemeyen hesap "İlk giriş bekleniyor" görünür; şifre belirlenince
   Keycloak'taki `UPDATE_PASSWORD` zorunluluğu kalkar.
5. **Profilden değiştirme**: giriş yapmış herkes üst çubuktaki "Şifremi değiştir" ile Keycloak'ın şifre ekranına gider
   (`kc_action=UPDATE_PASSWORD`); şifre bu uygulamadan geçmez.

Herkese açık uçlar `POST /api/v1/password-reset/request` ve `/confirm`'dir. İstek yanıtı hesabın var olup olmadığını
açıklamaz (hesap yok, pasif ya da telefonsuzsa da 204). Kod tuzlanmış özetiyle saklanır (`md_password_reset_code`); beş
yanlış denemede yanar, aynı kullanıcıya bir dakikada en fazla bir SMS gider. Yanlış/süresi dolmuş kod için 400
`INVALID_RESET_CODE`, kurala uymayan şifre için 400 `WEAK_PASSWORD` (kod yanmaz), SMS gidemezse 503 `SMS_UNAVAILABLE`.
Uçlarda IP bazlı sınır yoktur, yalnızca kullanıcı bazlı sınır vardır; canlıda gateway önünde hız sınırı önerilir.

### SMS sağlayıcısı

SMS bir porttan (`SmsPort`) gider; sağlayıcı `yarnshipment.sms.provider` ile seçilir:

| Değer | Davranış |
|---|---|
| `log` | Gerçek SMS göndermez, mesajı (kodla birlikte) uygulama loguna yazar. Yalnızca yerel geliştirme (`application-local.yml`). |
| `none` (varsayılan) | Sağlayıcı yok: SMS gerektiren işlemler 503 `SMS_UNAVAILABLE` döner. |

Gerçek sağlayıcı (Netgsm, İleti Merkezi vb.) henüz bağlı değildir; seçim yapıldığında `adapter/out/sms` altına yeni bir
adaptör eklenir, uygulamanın geri kalanı değişmez.

### Giriş ekranı: "Şifremi unuttum" bağlantısı

Bağlantıyı Keycloak giriş teması `yarnshipment` ekler (`deploy/keycloak/themes/yarnshipment`). Yeni kurulumda realm
dosyası temayı seçer. Daha önce kurulmuş Keycloak'ta **Realm settings → Themes → Login theme = yarnshipment** seçin
(tema dosyaları docker-compose ile bağlanır). Bağlantının gideceği adres `resources/js/frontend-config.js` içindeki
`FRONTEND_URL` değeridir; canlıda bunu uygulamanın adresiyle değiştirin.

### Giriş ve şifre değiştirme ekranlarının görünümü

Aynı `yarnshipment` teması iki ekranın da görünümünü uygulamayla eşler (`resources/js/branding.js`): marka gradyanlı
arka plan, beyaz kart, marka renkli düğme ve firma adı. Renk ve ad `resources/js/frontend-config.js` içindeki
`YARNSHIPMENT_BRAND_COLOR` ve `YARNSHIPMENT_BRAND_NAME` değerlerinden gelir. Her firmanın kurulumunda bu iki değeri,
uygulamadaki **Görünüm** ekranında seçilen renk ve adla aynı yapın; Keycloak sayfası uygulamanın dışında olduğu için
ekrandaki değişiklik oraya kendiliğinden yansımaz. Stil PatternFly 5 sınıflarına bağlıdır (Keycloak 26.x); Keycloak'ı
yükseltirken giriş ekranına bir kez bakın.
