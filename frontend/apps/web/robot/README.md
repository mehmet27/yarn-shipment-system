# Robot testleri

Gerçek arayüze ve gerçek backend'e karşı çalışan, kullanıcı gibi davranan test robotu. `e2e/` testlerinden farkı:
sahte API yerine çalışan yığını (Keycloak girişi, gateway, servisler, veritabanı) kullanır; bu yüzden rol yetkileri,
sunucu doğrulamaları ve sayıların tutarlılığı gerçek koşullarda sınanır. Bulduğu sorunları Türkçe bir raporda toplar.

## Çalıştırma

Önce yığın ayakta olmalı (`README.md` "Uygulamayı çalıştırma"): arayüz `http://localhost:5173` (`VITE_USE_MOCK_API=false`),
gateway `8080`, Keycloak `8180`, `yarnshipment-app` `local` profiliyle.

```bash
cd frontend
# Şifre: bash için  export ROBOT_SIFRE='...'   PowerShell için  $env:ROBOT_SIFRE = '...'
# ya da robot/.env.local dosyasına  ROBOT_SIFRE=...  yazın (git'e girmez)
pnpm --filter @iplik/web robot                  # sessiz kip; rapor üretir
pnpm --filter @iplik/web robot -- --izle        # gerçek tarayıcı penceresinde, yavaşlatarak izlenir
pnpm --filter @iplik/web robot -- --izle --yavas=600   # adımlar arası bekleme (ms)
pnpm --filter @iplik/web robot -- --video       # her senaryonun videosunu rapora ekler
pnpm --filter @iplik/web robot -- --rol=depo    # adında "depo" geçen senaryolar
```

Rapor: `robot/rapor/rapor.html` (ekran görüntüleri, adım listesi, YENİ/çözüldü ayrımı) ve `rapor.md`.
Önceki koşuya göre yeni çıkan sorunlar "YENİ" işaretlenir; düzelenler özet satırında sayılır.

Ortam değişkenleri: `ROBOT_SIFRE` (ortak şifre), `ROBOT_URL` (varsayılan `http://localhost:5173`), `ROBOT_GATEWAY`
(`http://localhost:8080`), `ROBOT_TARAYICI` (`msedge`, `chrome` ya da `chromium`; Windows'ta varsayılan `msedge`).
Kullanıcılar `robot/kullanicilar.json` içindedir (rol → giriş adı). Şifresi farklı bir hesap için `robot/kullanicilar.local.json`
açın (git'e girmez): `{ "genel-mudur": "şifre" }`. Hesabı olmayan rolün senaryoları atlanır.

**Dikkat:** robot yerel veritabanına kayıt yazar (iplik girişi, sevkiyat, itiraz; adları `ROBOT-…` ile başlar).
Üretim ya da paylaşılan bir ortamda çalıştırmayın.

## Chrome eklentisi (robotu tek tıkla başlat)

Eklenti robotu doğrudan çalıştıramaz (tarayıcı kuralları); bu yüzden bu bilgisayarda küçük bir yardımcı sunucu çalışır
(yalnızca `127.0.0.1`, yalnızca eklentiden gelen isteklere yanıt verir).

1. Terminalde (açık kalsın):
   `pnpm --filter @iplik/web robot:sunucu`
2. Chrome'da `chrome://extensions` → **Geliştirici modu** açık → **Paketlenmemiş öğe yükle** → `frontend/apps/web/robot/eklenti` klasörünü seçin.
3. Araç çubuğundaki **İplik Sevkiyat Robotu** simgesi: **Robotu başlat** (isteğe bağlı: pencerede izle, video, yalnızca şu senaryolar),
   **Durdur**, **Raporu aç** (rapor.html yeni sekmede) ve **Sorun metnini kopyala** (YENİ sorunların listesi, yeniden üretme
   komutlarıyla; Claude'a yapıştırın, nedenini bulup PR açsın).

`ROBOT_SIFRE` sunucunun çalıştığı terminalde (ya da `robot/.env.local` içinde) tanımlı olmalı. Sunucunun portu
`ROBOT_SUNUCU_PORT` ile değişir (varsayılan 7777; değişirse `eklenti/popup.js` ve `manifest.json` de güncellenir).

Rapor ayrıca `rapor/sorun-ozeti.md` (yapıştırılacak sorun metni) ve `rapor/ozet.json` (son koşu özeti) üretir; HTML raporda
"Yalnızca YENİ sorunlar" süzgeci vardır.

## Temiz kurulumdan başlama

Yalnızca `admin` kullanıcısı olan temiz bir yerel ortam için (Docker verisi silinir, geri alınamaz):

```bash
bash deploy/temiz-kurulum.sh    # hacimleri siler, yalnızca admin'li Keycloak ile yeniden kurar
```

Ardından yığını başlatıp `ROBOT_SIFRE` olarak admin'in şifresini değil, **tüm robot kullanıcılarının alacağı** şifreyi verin
(en az 8 karakter ve kolay tahmin edilmeyen bir şifre; Keycloak `12345678` gibilerini reddeder). `admin` için
`robot/kullanicilar.local.json` içine kendi şifresini yazın: `{ "admin": "<admin şifresi>" }`. `00-kurulum` senaryosu
eksik kullanıcıları (depo, kalite, müdür, genel müdür) yönetici ekranından açar, her biriyle ilk girişi yapıp
`ROBOT_SIFRE` şifresini belirler; sonraki senaryolar bu hesaplarla koşar.

## Senaryolar (`robot/senaryolar`)

| Dosya                   | Ne yapar                                                                                                                                                                                                                                                                                  |
| ----------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `00-kurulum`            | Temiz kurulumda eksik kullanıcıları yönetici ekranından açar ve ilk girişlerini yapar                                                                                                                                                                                                     |
| `01-gezinti`            | Her rolle tüm ekranları açar; yetkili ekranda hata/konsol/ağ hatası, yetkisizde 403 arar                                                                                                                                                                                                  |
| `02-formlar`            | Formlara eksik/hatalı değer girer; ekranın nedeni söyleyen bir mesaj gösterdiğini ve 5xx olmadığını denetler                                                                                                                                                                              |
| `03-akis`               | İplik girişi → kumaş üretimi → sevkiyat → çuval → çıkış tartısı → tamamlama → kumaş girişi → kalite itirazı → uyuşmazlık                                                                                                                                                                  |
| `04-tutarlilik`         | Özet, bakiye ve fire dosyası sayılarını API'den çekip birbiriyle karşılaştırır                                                                                                                                                                                                            |
| `05-tum-ekranlar`       | **Gezgin:** her rol her ekranda her sekme, filtre, anahtar, liste, sayfalama ve indirme düğmesine tıklar; sevkiyat dosyasının her durumunu ve uyuşmazlık detayını da gezer. Veri değiştiren düğmeleri tıklamaz ama `support/kapsam.ts` kaydında senaryosu yoksa **kapsam açığı** raporlar |
| `06-tanimlar-partiler`  | Tanımlar (6 tür) ve partilerde ekle, düzenle, kilitle, pasife al                                                                                                                                                                                                                          |
| `07-kullanicilar`       | Hesap aç, düzenle, süzgeçler, şifre ver, pasife al ve pasif hesabın girişinin kapandığını doğrula                                                                                                                                                                                         |
| `08-gorunum-raporlar`   | Görünüm (ad, logo, varsayılan), rapor sekmeleri, tarih aralığı, Excel/PDF indirme, rapor logosu, açılış bakiyesi                                                                                                                                                                          |
| `09-sevkiyat-islemleri` | Çuval çıkarma, taslak iptali, fire oranı / fatura / kapatma, faturalanacaklar, iplik dönüşü, düzeltme, kanıt geçersiz kılma, lab sonucu ve karar                                                                                                                                          |
| `10-surec-dogrulama`    | Bilinen sayılarla (200 kg iplik, 50 kg kumaş, 100 kg sevkiyat, 90 kg kumaş) süreci yürütür; stok, bakiye, özet, tartı, ambar fişi, fire hesabı, rapor ve denetim kaydını API ve ekrandan **sonuç doğrulamalı** denetler                                                                   |

## Kural tablosu (`robot/kurallar`)

Robotun iş kuralı bilgisi senaryolardan ayrıdır: `kurallar/yetki.ts` (hangi rol hangi ekranı açar) ve `kurallar/hesap.ts` (fire formülü, stok düşümü,
kilo ve irsaliye sınırları). Kural değişince (yeni rol, yeni formül, yeni sınır) yalnızca bu dosyalar güncellenir.

## Kapsam kaydı (kimse unutulmasın)

Gezgin (`05`) veri değiştirmeyen her şeye kendisi tıklar. **Veri değiştiren bir düğme** (kaydet, sil, iptal, pasife al,
kilitle…) için `support/kapsam.ts` içine `rota|düğme adı` → senaryo kaydı eklenmelidir; yoksa rapor
"Kapsam açığı: …" yazar ve robot kırmızı olur. Yani yeni bir düğme eklenince robota senaryo yazmadan yeşil çıkmaz.

## Yeni özellik eklerken robotu güncelleme kuralı

Her geliştirmede (yeni ekran, rota, alan, düğme, doğrulama ya da hesap kuralı) aynı PR'da robota gereken bilgi verilir:

1. **Yeni ekran/rota**: `01-gezinti` içindeki `EKRANLAR` listesine ve `IZINLI` tablosuna ekleyin; `05-tum-ekranlar` içindeki `ROTALAR` listesine de ekleyin (gezgin her sekmesine tıklasın).
2. **Yeni form ya da alan**: `02-formlar` içine eksik/hatalı giriş senaryosu ekleyin. Beklenen: ekran nedeni söyleyen bir mesaj gösterir.
3. **Yeni iş akışı adımı**: `03-akis` içine adımı ekleyin; tamamlandığını ekrandan ve gerekirse API'den doğrulayın.
4. **Yeni hesaplama, toplam ya da rapor**: `04-tutarlilik` içine "A = B + C" türünden bir çapraz kontrol ekleyin.
5. **Yeni rol ya da kullanıcı**: `kullanicilar.json` ve `IZINLI` tablosunu güncelleyin.
6. **Veri değiştiren yeni düğme**: davranışını deneyen senaryoyu yazın ve `support/kapsam.ts` kaydına ekleyin.
7. **Bilinçli hata yanıtı** (ör. 409): ilgili senaryoda `robot.beklenen(/409 POST \/api\/v1\/.../)` ile beklenen olarak işaretleyin;
   işaretlenmeyen 4xx/5xx yanıtlar, konsol hataları ve yakalanmamış istisnalar sorun sayılır.

Senaryo yazarken `robot.adim('ad', async () => {...})` bir adımı kaydeder ve içinde sorun çıkarsa ekran görüntüsü alır;
`robot.kontrol('ad', koşul, ayrıntı)` beklenen bir koşulu denetler; `robot.mesajlar()` ekranda görünen uyarıları okur;
`robot.api('/api/v1/...')` oturumun çerezleriyle API'yi okur.
