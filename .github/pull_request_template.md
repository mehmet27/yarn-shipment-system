## Özet

<!-- Ne değişti, neden? Alınan kararları ve bilinçli olarak yapılmayanları da yazın. -->

## Test

<!-- Çalıştırılan testler, elle denenen ekranlar. Denenemeyen bir şey varsa açıkça yazın. -->

## Robot (frontend/apps/web/robot)

- [ ] Yeni/değişen ekran, form, alan, doğrulama, rol ya da hesaplama için robot senaryosu güncellendi (`robot/README.md`, "Yeni özellik eklerken robotu güncelleme kuralı")
- [ ] Yalnızca değişen özelliğin senaryoları koşuldu (`pnpm robot -- --rol=<ad> --sifirla`); koşudan sonra ortam sıfırlandı
- [ ] Robotu ilgilendiren bir değişiklik yok

## Denetim listesi

<!-- İlgisiz satırları silin. Kurallar: docs/standartlar/surec-standartlari.md -->

- [ ] Mevcut bir davranış değiştiyse: git geçmişine ve dokümanlara bakıldı, fabrikanın bilinçli isteği değil (emin değilsem değiştirmedim, öneri olarak yazdım)
- [ ] Ekran metni eklendi/değişti: `tr.json` ve "Arayüz dili" sözlüğüne uygun, robot senaryolarındaki sözcük de güncellendi
- [ ] Veritabanı değişti: yeni migration dosyası eklendi (eski migration düzenlenmedi), `docs/veritabani-tasarimi.md` güncellendi
- [ ] Yeni ortam değişkeni ya da kurulum adımı: `deploy/.env.example`, `deploy/prod/.env.example` ve `DEPLOY.md` güncellendi
- [ ] Yeni bağımlılık: gerekçesi ve lisansı özet bölümünde yazıyor (BUILD-09, FE-BUILD-03)
