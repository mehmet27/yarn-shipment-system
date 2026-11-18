# Giriş hesabı açma

> Sisteme yalnızca fabrika personeli (yönetici, depo, kalite, müdür, genel müdür) giriş yapar; fasoncu ve şoför giriş
> yapmaz, yalnızca ana veridir (KR-17). **SMS ya da e-posta doğrulaması yoktur:** hesabı yönetici açar, ilk (geçici)
> şifreyi kendisi yazar ve kişiye iletir; kişi ilk girişte şifresini değiştirir. Fabrikada tek merkez lokasyon olduğu
> için yeni hesap etkin tüm lokasyonları görür (lokasyon seçilmez).

Hesaplar uygulamanın kendi veritabanında (`shipment.app_user`) tutulur; ayrı bir kimlik sunucusu kurulumu ya da ayarı yoktur.
Hesap **Kullanıcılar** ekranından (`/panel/kullanicilar`, yalnızca `USER_MANAGE`) açılır.

## İlk yönetici

Yeni kurulumda `admin` hesabı yoktur; uygulama açılışta `YARNSHIPMENT_AUTH_BOOTSTRAP_ADMIN_PASSWORD` değeriyle oluşturur
(üretim compose'unda `.env` içindeki `INITIAL_ADMIN_PASSWORD`; yerel `local` profilinde `LOCAL_TEST_USER_PASSWORD`). Hesap zaten
varsa dokunulmaz. İlk girişte şifre değiştirilir. Unutulan admin şifresi sunucuda sıfırlanır: bkz. DEPLOY.md "Admin şifresini sıfırlama" (giriş ekranından talep edilemez).

## Kullanıcılar ekranı

| İşlem | Uç |
|---|---|
| Fabrika içi hesap aç | `POST /api/v1/master-data/login-accounts` (`username`, `fullName`, `role`, `password`; kalite rolünde `workArea`) → 201 |
| Rol değiştir | `PUT /api/v1/master-data/login-accounts/{userId}/role` (`role`; kalite rolünde `workArea`) → 204 |
| Ad soyadı düzenle | `PUT /api/v1/master-data/login-accounts/{userId}/name` (`fullName`) → 204 |
| Pasife al / yeniden aç | `PUT /api/v1/master-data/login-accounts/{userId}/enabled` (`enabled`) → 204 |
| Yeni geçici şifre ver | `PUT /api/v1/master-data/login-accounts/{userId}/password` (`password`) → 204 |

`role` yalnızca `ADMIN` (Yönetici), `WAREHOUSE` (Depo), `QUALITY` (Kalite), `MANAGER` (Müdür), `GENERAL_MANAGER` (Genel müdür) olabilir.
**Kalite** hesabında ekranda "Çalışma alanı" da seçilir (`workArea`): **Merkez fabrika** (yalnız merkez fabrikanın kumaşını ve merkez iplik stokunu görür/girer) ya da **Fasoncular** (yalnız fasoncuların kumaşını görür/girer); seçilmezse hesap açılmaz. Başka rollerde çalışma alanı verilmez.
Hesabın rolü açıldıktan sonra da Kullanıcılar ekranından değişir; rolün hangi işleri yapabildiği **Yetkiler** ekranından (yalnız yönetici) ayarlanır. Rol değişince, hesap pasife alınınca ya da
yeni geçici şifre verilince kişinin açık oturumları kapanır. Kişi kendi rolünü değiştiremez ve kendi hesabını kapatamaz (400
`INVALID_MASTER_DATA`); kullanıcı yoksa 404 `LOGIN_ACCOUNT_NOT_FOUND`. Giriş adı alınmışsa 409 `LOGIN_ACCOUNT_EXISTS`.
Şifre kurala uymuyorsa 400 `WEAK_PASSWORD`.

## Şifre

1. **Hesap açılınca** yönetici geçici şifreyi yazar ve kişiye iletir. Kural: en az 8 karakter, kullanıcı adı ya da yaygın bir şifre
   (ör. `12345678`) olamaz. Şifre hiçbir yanıtta ya da logda yer almaz; veritabanında yalnızca BCrypt özeti tutulur.
2. **İlk giriş**: hesap "İlk giriş bekleniyor" görünür; kişi `/giris` sayfasında girince `/sifre-degistir` sayfasına yönlenir ve
   şifresini değiştirmeden başka işlem yapamaz.
3. **Şifresini unutan**: giriş ekranında "Şifremi unuttum" yoktur. Art arda 3 hatalı denemeden sonra giriş ekranında
   **"Adminden şifre talep et"** düğmesi çıkar; talep Kullanıcılar ekranında uyarı ve "Şifre istedi" rozetiyle görünür.
   Yönetici hesabın menüsünde "Şifre ver" ile yeni geçici şifre belirler (talep o zaman kapanır). `admin` hesabı için talep
   düğmesi çıkmaz (bkz. yukarıdaki "İlk yönetici" bölümü). Kişinin açık oturumları kapanır ve hesap kilitliyse kilit kalkar, bir sonraki girişte şifresini değiştirir.
4. **Profilden değiştirme**: giriş yapmış herkes üst çubuktaki "Şifremi değiştir" ile `/sifre-degistir` sayfasına gider; mevcut
   şifresini yazar, yeni şifresini iki kez girer.
5. **Hatalı deneme**: 5 hatalı şifreden sonra hesap 15 dakika kilitlenir (`423 ACCOUNT_LOCKED`); yönetici yeni şifre vererek
   kilidi hemen kaldırabilir. Hesap yoksa ya da şifre yanlışsa mesaj aynıdır; hangisinin yanlış olduğu söylenmez.

## Doğrulama

Yeni hesapla `http://localhost:5173/giris` adresinden giriş yapın. Otomatik doğrulama: robot `07-kullanicilar` senaryosu
(`frontend/apps/web/robot/README.md`).
