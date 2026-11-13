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
varsa dokunulmaz. İlk girişte şifre değiştirilir.

## Kullanıcılar ekranı

| İşlem | Uç |
|---|---|
| Fabrika içi hesap aç | `POST /api/v1/master-data/login-accounts` (`username`, `fullName`, `role`, `password`) → 201 |
| Rol değiştir | `PUT /api/v1/master-data/login-accounts/{userId}/role` (`role`) → 204 |
| Pasife al / yeniden aç | `PUT /api/v1/master-data/login-accounts/{userId}/enabled` (`enabled`) → 204 |
| Yeni geçici şifre ver | `PUT /api/v1/master-data/login-accounts/{userId}/password` (`password`) → 204 |

`role` yalnızca `ADMIN`, `WAREHOUSE`, `QUALITY`, `MANAGER`, `GENERAL_MANAGER` olabilir. Rol değişince, hesap pasife alınınca ya da
yeni geçici şifre verilince kişinin açık oturumları kapanır. Kişi kendi rolünü değiştiremez ve kendi hesabını kapatamaz (400
`INVALID_MASTER_DATA`); kullanıcı yoksa 404 `LOGIN_ACCOUNT_NOT_FOUND`. Giriş adı alınmışsa 409 `LOGIN_ACCOUNT_EXISTS`.
Şifre kurala uymuyorsa 400 `WEAK_PASSWORD`.

## Şifre

1. **Hesap açılınca** yönetici geçici şifreyi yazar ve kişiye iletir. Kural: en az 8 karakter, kullanıcı adı ya da yaygın bir şifre
   (ör. `12345678`) olamaz. Şifre hiçbir yanıtta ya da logda yer almaz; veritabanında yalnızca BCrypt özeti tutulur.
2. **İlk giriş**: hesap "İlk giriş bekleniyor" görünür; kişi `/giris` sayfasında girince `/sifre-degistir` sayfasına yönlenir ve
   şifresini değiştirmeden başka işlem yapamaz.
3. **Şifresini unutan**: giriş ekranında "Şifremi unuttum" yoktur; yönetici Kullanıcılar ekranında "Şifre ver" ile yeni
   geçici şifre belirler. Kişinin açık oturumları kapanır ve hesap kilitliyse kilit kalkar, bir sonraki girişte şifresini değiştirir.
4. **Profilden değiştirme**: giriş yapmış herkes üst çubuktaki "Şifremi değiştir" ile `/sifre-degistir` sayfasına gider; mevcut
   şifresini yazar, yeni şifresini iki kez girer.
5. **Hatalı deneme**: 5 hatalı şifreden sonra hesap 15 dakika kilitlenir (`423 ACCOUNT_LOCKED`); yönetici yeni şifre vererek
   kilidi hemen kaldırabilir. Hesap yoksa ya da şifre yanlışsa mesaj aynıdır; hangisinin yanlış olduğu söylenmez.

## Doğrulama

Yeni hesapla `http://localhost:5173/giris` adresinden giriş yapın. Otomatik doğrulama: robot `07-kullanicilar` senaryosu
(`frontend/apps/web/robot/README.md`).
