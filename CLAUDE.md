# Proje notları

- **Robot testleri:** `frontend/apps/web/robot` gerçek arayüze karşı çalışan test robotudur. Yeni ekran, rota, form alanı,
  doğrulama, rol ya da hesaplama eklerken aynı PR'da robot senaryolarını güncelleyin; kural ve örnekler
  `frontend/apps/web/robot/README.md` içindedir. PR şablonundaki robot maddesini doldurun.
  **Koşu kuralı:** her geliştirmede yalnızca değişen özelliğin senaryoları koşulur (`cd frontend && pnpm robot -- --rol=<ad> --sifirla`); tam koşu yalnızca
  kullanıcı isterse yapılır. **Her robot koşusundan sonra** (tam ya da PR koşusu) ortam sıfırlanır: `--sifirla` bunu koşunun sonunda kendisi yapar
  (elle: `cd frontend && pnpm yerel:sifirla -- --evet`); yalnızca `admin` kalır, robotun açtığı tüm kullanıcılar ve veriler silinir.
  Sıfırlamadan sonraki koşuda admin'in ilk girişini ve şifresini robot kendisi halleder (`robot/support/hazirlik.ts`).

- **Yerel ortamı sıfırlama:** `cd frontend && pnpm yerel:sifirla` (`tools/yerel-sifirla.mjs`) yerel Docker verisini silip yalnızca `admin`'li temiz bir ortam kurar ve yığını gizli (pencere açmadan) yeniden başlatır; robotu başlatmaz. `pnpm yerel:durdur` / `pnpm yerel:baslat` ile servisler durdurulur/başlatılır. Yalnızca yerel geliştirme içindir; canlı/CI/uzak Docker ortamında kendiliğinden durur. Ayrıntı: kök `README.md`.

- **Arayüz dili:** ekran metinleri fabrika çalışanının günlük diliyle yazılır (harfli kısaltma, kod, UUID ya da muhasebe terimi yok). Metinler tek yerde: `frontend/packages/i18n/src/locales/tr.json`. Aynı kavram her ekranda aynı sözcükle geçer; sözlük `frontend/apps/web/robot/README.md` "Arayüz dili" bölümündedir, yeni metin eklerken oraya uyun ve değiştirdiğiniz sözcüğü robot senaryolarında da güncelleyin.

- **Süreç standartları:** dal, commit, PR, CI, robot, kurulum ve yedek kuralları `docs/standartlar/surec-standartlari.md` içindedir.
  Commit'ten önce biçimi düzeltin: frontend için `cd frontend && pnpm format`, Java için `./mvnw spotless:apply` (CI biçimsiz kodu reddeder;
  `git config core.hooksPath tools/git-hooks` commit öncesi denetimi açar). Kurulum betikleri (`deploy/prod`) değişince `tools/ci/deploy-smoke.sh` koşar.

- **Fabrikanın bilinçli kararları:** mevcut davranışların bir kısmı fabrikanın isteğiyle böyledir. Bir iş kuralını, ekran akışını, alanı ya da rol
  davranışını "hata" ya da "gereksiz" sayıp değiştirmeden önce git geçmişine, PR açıklamalarına ve dokümanlara bakın; bilinçli görünüyorsa ya da emin
  değilseniz değiştirmeyin, öneri olarak raporlayın. Bilinen kararlar: kalite itirazında laboratuvar adımı yok; sevkiyatta Düzeltmeler bölümü yok;
  fire dosyasından fatura girilmez (Faturalama ekranından girilir); tüm iplik girişlerinde tedarikçi zorunlu; kg biçimi binlik nokta ve ondalık virgül;
  bildirim, kamerayla QR ve çevrimdışı fotoğraf kuyruğu istenmiyor.
