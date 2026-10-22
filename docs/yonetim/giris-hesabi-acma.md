# Şoför ve fasoncuya giriş hesabı açma

Şoför ve fasoncu kaydı uygulamada (`POST /api/v1/master-data/drivers` ve `/subcontractors`) açılır; **giriş hesabı** ise
Keycloak'ta ayrıca açılır. Uygulama henüz Keycloak'ta kullanıcı oluşturmaz. İki adım bilinçli olarak ayrıdır: kayıt
açmak yönetici işidir (`MASTER_DATA_MANAGE`), hesap açmak Keycloak yöneticisi işidir. Otomatik hesap açma (Keycloak
Admin API'si ile) sonraki iştir.

> Not: Bu adımlar realm dosyasındaki (`deploy/keycloak/yarnshipment-realm-local.json`) örnek kullanıcılardan
> (`sofor`, `fasoncu`) türetilmiştir. Gerçek Keycloak üzerinde uçtan uca denenmemiştir; ilk uygulamada bir test
> hesabıyla doğrulayın.

## 1. Kaydı uygulamada aç

Yönetici olarak şoförü ya da fasoncuyu ekleyin ve dönen `id` değerini not edin. Şoför fasoncuya bağlıysa önce fasoncu
kaydı açılmış olmalıdır.

## 2. Keycloak'ta kullanıcıyı oluştur

Yönetim konsolunda (`http://localhost:8180`, `yarnshipment` realm'i) **Users → Add user**:

| Alan | Değer |
|---|---|
| Username | Kişinin giriş adı (ör. `ali.yilmaz`) |
| Required user actions | `Update Password` (ilk girişte şifre değiştirsin) |

Ardından **Credentials** sekmesinde geçici şifre verin.

## 3. Rolü ver

**Role mapping → Assign role**: şoför için `driver`, fasoncu için `subcontractor`.

## 4. Kimlik ve lokasyon özniteliklerini ekle

**Attributes** sekmesinde (değerler ilk adımdaki kayıt kimlikleridir; claim adları tam böyle yazılmalıdır):

| Kullanıcı | Öznitelik | Değer |
|---|---|---|
| Şoför | `driver_id` | Şoför kaydının `id`'si |
| Fasoncu | `subcontractor_id` | Fasoncu kaydının `id`'si |
| İkisi de | `site_ids` | Görebileceği lokasyon `id`'leri (her biri ayrı değer) |

Şoför fasoncuya bağlı çalışıyorsa yalnızca `driver_id` yeter; sevkiyat görünürlüğü şoförün kendi kimliğine göre
verilir. `site_ids` boşsa kişi hiçbir lokasyonun sevkiyatını göremez (GEN-07).

## 5. Doğrula

Kullanıcıyla `http://localhost:5173` adresinden giriş yapın; kendi sevkiyatlarını görmeli, başkalarınınkini
görmemelidir. Token'daki claim'leri görmek için `tools/keycloak/verify-realm.mjs` betiğini kullanabilirsiniz.

## Kayıt pasife alınınca

Kaydı uygulamada pasife almak giriş hesabını kapatmaz. Kişi artık çalışmıyorsa Keycloak'ta kullanıcıyı da
**devre dışı bırakın** (`Enabled` kapalı).
