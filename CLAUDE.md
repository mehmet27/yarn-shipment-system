# Proje notları

- **Robot testleri:** `frontend/apps/web/robot` gerçek arayüze karşı çalışan test robotudur. Yeni ekran, rota, form alanı,
  doğrulama, rol ya da hesaplama eklerken aynı PR'da robot senaryolarını güncelleyin; kural ve örnekler
  `frontend/apps/web/robot/README.md` içindedir. PR şablonundaki robot maddesini doldurun.
  **Koşu kuralı:** her geliştirmede yalnızca değişen özelliğin senaryoları koşulur (`pnpm robot -- --rol=<ad>`); tam koşu yalnızca
  kullanıcı isterse yapılır. **Her robot koşusundan sonra** (tam ya da PR koşusu) ortam `cd frontend && pnpm yerel:sifirla -- --evet`
  ile sıfırlanır: yalnızca `admin` kalır, robotun açtığı tüm kullanıcılar ve veriler silinir.

- **Yerel ortamı sıfırlama:** `cd frontend && pnpm yerel:sifirla` (`tools/yerel-sifirla.mjs`) yerel Docker verisini silip yalnızca `admin`'li temiz bir ortam kurar ve yığını gizli (pencere açmadan) yeniden başlatır; robotu başlatmaz. `pnpm yerel:durdur` / `pnpm yerel:baslat` ile servisler durdurulur/başlatılır. Yalnızca yerel geliştirme içindir; canlı/CI/uzak Docker ortamında kendiliğinden durur. Ayrıntı: kök `README.md`.
