# Proje notları

- **Robot testleri:** `frontend/apps/web/robot` gerçek arayüze karşı çalışan test robotudur. Yeni ekran, rota, form alanı,
  doğrulama, rol ya da hesaplama eklerken aynı PR'da robot senaryolarını güncelleyin; kural ve örnekler
  `frontend/apps/web/robot/README.md` içindedir. PR şablonundaki robot maddesini doldurun.

- **Yerel ortamı sıfırlama:** `cd frontend && pnpm yerel:sifirla` (`tools/yerel-sifirla.mjs`) yerel Docker verisini silip yalnızca `admin`'li temiz bir ortam kurar ve yığını gizli (pencere açmadan) yeniden başlatır; robotu başlatmaz. `pnpm yerel:durdur` / `pnpm yerel:baslat` ile servisler durdurulur/başlatılır. Yalnızca yerel geliştirme içindir; canlı/CI/uzak Docker ortamında kendiliğinden durur. Ayrıntı: kök `README.md`.
