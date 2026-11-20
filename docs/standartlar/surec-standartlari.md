# Süreç standartları

Kodun nasıl yazılacağını [Java](java-kod-standartlari.md) ve [React/TypeScript](react-typescript-kod-standartlari.md)
standartları söyler; bu doküman işin **nasıl yürütüleceğini** söyler: bir değişikliğin fikirden canlıya kadar geçtiği
adımlar, her adımda neyin zorunlu olduğu ve bunu hangi aracın denetlediği. Dal, commit ve PR biçimi için Java standartları
bölüm 16 (GIT-01…10) geçerlidir; burada yalnızca onlara eklenenler yazılıdır.

Kural kimlikleri `SUREC-NN` biçimindedir. ZORUNLU kurallar araçla denetlenir ya da PR şablonunda işaretlenir.

## 1. Değişikliğe başlamadan

- **SUREC-01 Önce neden böyle olduğuna bakılır.** Mevcut davranışların bir kısmı fabrikanın bilinçli isteğidir. Bir iş
  kuralı, ekran akışı, alan ya da rol davranışı "hata" ya da "gereksiz" görünüyorsa değiştirmeden önce git geçmişine
  (`git log -S "<metin>"`), ilgili PR açıklamasına ve `docs/analiz` altındaki süreç analizine bakılır. Bilinçli görünen ya
  da emin olunamayan davranış değiştirilmez; öneri olarak proje sahibine raporlanır. Bilinen kararların listesi kök
  `CLAUDE.md` içindedir ve yeni karar öğrenildikçe oraya eklenir.
- **SUREC-02 İş kuralı kararı proje sahibinindir.** Kod, arayüz ve süreç iyileştirmesi iş kuralını değiştirmez. Kuralı
  değiştiren iş ayrı PR'dır ve açıklamasında kararı kimin verdiği yazar.
- **SUREC-03 Bir PR tek amaca hizmet eder** (GIT-05). Biçim düzeltmesi, yeniden adlandırma ve davranış değişikliği aynı
  PR'da karıştırılmaz; karıştırılırsa gözden geçiren davranış değişikliğini göremez.

## 2. Geliştirirken

- **SUREC-04 Yerel ortam tek komutla kurulur ve sıfırlanır.** `cd frontend && pnpm yerel:sifirla` yalnızca `admin`'li temiz
  ortam kurar; `pnpm yerel:baslat` veriye dokunmadan servisleri (gerekirse veritabanı konteynerini de) başlatır;
  `pnpm yerel:durdur` durdurur. Elle `docker compose down -v` ya da elle veritabanı düzenlemesi yapılmaz.
- **SUREC-05 Biçim commit'ten önce düzeltilir.** Frontend: `cd frontend && pnpm format`. Java: `./mvnw spotless:apply`.
  `git config core.hooksPath tools/git-hooks` (bir kez) commit öncesi denetimi açar: hazırlanan dosyalarda Prettier, ESLint
  ve Spotless denetlenir. Denetim atlanırsa CI yine reddeder; "biçim düzeltmesi" commit'i atmak zorunda kalmamak için atlamayın.
- **SUREC-06 Ekran metni tek yerdedir ve sözlüğe uyar.** Metinler `frontend/packages/i18n/src/locales/tr.json` içindedir;
  sözlük `frontend/apps/web/robot/README.md` "Arayüz dili" bölümüdür. Değişen sözcük robot senaryolarında da değişir.
- **SUREC-07 Veritabanı yalnızca yeni migration ile değişir.** Birleşmiş migration dosyası düzenlenmez. Değişiklik
  `docs/veritabani-tasarimi.md` ile birlikte gelir; `db-migrations` iş akışı migration'ları PostgreSQL 17'de uygular.
- **SUREC-08 Yeni ayar belgelenir.** Yeni ortam değişkeni ya da kurulum adımı aynı PR'da `deploy/.env.example`,
  `deploy/prod/.env.example` ve `DEPLOY.md` içine yazılır; sır değeri hiçbir dosyaya, koda ya da sohbete yazılmaz (SEC-14).

## 3. Robot

- **SUREC-09 Robot özellikle birlikte güncellenir.** Yeni ekran, rota, alan, düğme, doğrulama, rol ya da hesap kuralı aynı
  PR'da robot senaryosuna ve gerekiyorsa robot dokümanına işlenir (`robot/README.md`, "Yeni özellik eklerken robotu güncelleme kuralı").
- **SUREC-10 Yalnızca değişen özelliğin senaryoları koşulur.** `cd frontend && pnpm robot -- --rol=<senaryo adı> --sifirla`.
  Tam koşu (tüm senaryolar) yalnızca proje sahibi açıkça isterse yapılır.
- **SUREC-11 Her robot koşusundan sonra ortam sıfırlanır.** `--sifirla` bunu koşunun sonunda kendisi yapar; verilmediyse
  `pnpm yerel:sifirla -- --evet` elle çalıştırılır. Sistemde yalnızca `admin` kalır.
- **SUREC-12 Robot kendi kendine yeter.** Koşu, sıfırlanmış ortamda elle hazırlık istemeden başlar (admin'in ilk girişini
  `support/hazirlik.ts` yapar, diğer hesapları `00-kurulum` açar). Elle ön adım gerektiren senaryo yazılmaz.
- **SUREC-13 Kırmızı senaryo susturulmaz.** Senaryo atlanarak, beklenti gevşetilerek ya da zaman aşımı büyütülerek yeşile
  çevrilmez; önce neden bulunur. Neden uygulamadaysa uygulama, robottaysa robot düzeltilir ve PR açıklamasına yazılır.

## 4. PR ve CI

- **SUREC-14 PR şablonu doldurulur.** Özet (ne, neden, alınan kararlar), Test (ne koşuldu, **ne denenemedi**), Robot ve
  denetim listesi. "Denenemedi" yazmak kabul edilir; denenmemişi denenmiş göstermek edilmez.
- **SUREC-15 CI yeşil olmadan birleştirilmez.** İş akışları ve neyi korudukları:

  | İş akışı | Ne zaman | Ne denetler |
  |---|---|---|
  | `backend` | Java ya da kök dosyalar değişince | Spotless, derleme (Error Prone, NullAway), Checkstyle, birim + ArchUnit, PMD, JaCoCo |
  | `frontend` | `frontend/**` ya da OpenAPI değişince | üretilmiş istemci güncel mi, Prettier, ESLint, tip denetimi, birim testleri, derleme |
  | `frontend-e2e` | `frontend/**` değişince | sahte API ile uçtan uca testler |
  | `windows` | `frontend/**` değişince | Windows'ta biçim, tip, test, derleme ve `@smoke` turu |
  | `db-migrations` | migration ya da `deploy/postgres` değişince | migration'lar PostgreSQL 17'de uygulanır, kısıt testleri |
  | `portable-paths` | her PR | dosya adları Windows ve macOS'ta geçerli mi |
  | `deploy` | `deploy/**` ya da iş akışları değişince; haftada bir | shellcheck, actionlint, Compose dosyaları, üretim kurulumunun uçtan uca duman testi |

- **SUREC-16 Kırmızı CI zorlanmaz.** Test kapatılarak, `--no-verify` ile ya da boş commit'le CI yeşile çevrilmez.
- **SUREC-17 Birleştirmeyi proje sahibi yapar** (GIT-04, GIT-06).

## 5. Kurulum, güncelleme ve yedek

- **SUREC-18 Sunucuda her iş betikle yapılır.** Kurulum ve güncelleme `deploy/prod/install.sh`, durum `durum.sh`, yedek
  `yedekle.sh`, geri yükleme `geri-yukle.sh`. Sunucuda elle `docker compose` komutu yalnızca günlük okumak ve durdurmak
  içindir. Betikler tekrar çalıştırılabilir olmalı ve hata durumunda sıfırdan farklı kodla, nedeni söyleyerek çıkmalıdır.
- **SUREC-19 Güncellemeden önce döküm alınır.** `install.sh` bunu kendisi yapar; yeni sürüm açılışta şemayı değiştirir ve
  bu geri alınamaz. Geri dönüş yolu: önceki sürüme `git checkout` + `geri-yukle.sh <güncelleme öncesi döküm>`.
- **SUREC-20 Yedek otomatiktir ve ayrı diskte durur.** Gece yedeğini `install.sh` kurar. `YEDEK_DIZINI` uygulamadan ayrı
  bir disk ya da NAS olmalıdır; `durum.sh` son yedek 26 saatten eskiyse uyarır.
- **SUREC-21 Denenmemiş yedek, yedek değildir.** Ayda bir, yedek ayrı bir deneme sunucusunda `geri-yukle.sh` ile geri
  yüklenir ve bir sevkiyat ile bir kanıt dosyası açılarak doğrulanır.
- **SUREC-22 Kurulum betikleri değişince duman testi koşar.** `tools/ci/deploy-smoke.sh` kurulum → giriş → yedek →
  güncelleme → felaket → geri yükleme zincirini dener; CI'da `deploy` iş akışı çalıştırır. Betik gerçek sunucuda
  çalıştırılmaz (yığını ve verisini siler; `deploy/prod/.env` varsa durur).

## 6. Bu dokümanın bakımı

- **SUREC-23** Kural ekleme, değiştirme ya da kaldırma PR ile yapılır ve proje sahibinin onayını gerektirir (GIT-09).
  Bir kural araçla denetlenebiliyorsa araca bağlanır; yalnızca yazıda duran kural zamanla unutulur.
