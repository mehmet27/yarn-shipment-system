#!/usr/bin/env bash
# Yedekten geri yükleme: veritabanını bir dökümden, kanıt dosyalarını yedek klasöründen yerine koyar.
# MEVCUT VERİTABANININ YERİNE DÖKÜM GEÇER (önce mevcut hâlin dökümü "geri-yukleme-oncesi-…" adıyla alınır).
#
# Kullanım (root ile; kurulum yapılmış olmalı: ./deploy/prod/install.sh):
#   ./deploy/prod/geri-yukle.sh                      en son dökümü geri yükler (onay sorar)
#   ./deploy/prod/geri-yukle.sh <döküm dosyası>      belirli bir dökümü geri yükler
#   --evet          onay sormaz
#   --kanit-atla    yalnızca veritabanını geri yükler, kanıt dosyalarına dokunmaz
#
# Yedeğin yeri: YEDEK_DIZINI (ortam değişkeni ya da deploy/prod/.env; varsayılan /var/backups/yarn-shipment).
# Kanıt dosyaları yalnızca eklenir/üzerine yazılır; sunucudaki dosyalar silinmez.
set -euo pipefail
cd "$(dirname "$0")"
# shellcheck source=deploy/prod/ortak.sh
source ./ortak.sh

EVET=false
KANIT=true
DOKUM=""
for arg in "$@"; do
  case "$arg" in
    --evet) EVET=true ;;
    --kanit-atla) KANIT=false ;;
    -*) dur "bilinmeyen seçenek: $arg" ;;
    *) DOKUM="$arg" ;;
  esac
done

HEDEF="$(ayar YEDEK_DIZINI /var/backups/yarn-shipment)"
if [ -z "$DOKUM" ]; then
  DOKUM="$(find "$HEDEF/veritabani" -name 'yarnshipment-*.dump' 2>/dev/null | sort | tail -1)"
  [ -n "$DOKUM" ] || dur "$HEDEF/veritabani içinde döküm bulunamadı. Döküm dosyasını parametre olarak verin."
fi
[ -s "$DOKUM" ] || dur "döküm dosyası yok ya da boş: $DOKUM"
if [ "$KANIT" = true ]; then
  [ -d "$HEDEF/evidence" ] || dur "$HEDEF/evidence yok. Yalnızca veritabanı için --kanit-atla kullanın."
  command -v rsync >/dev/null 2>&1 || dur "rsync kurulu değil (apt-get install rsync)."
fi

"${COMPOSE[@]}" up -d --wait postgres >/dev/null
TABLO="$("${COMPOSE[@]}" exec -T postgres pg_restore --list < "$DOKUM" | grep -c ' TABLE DATA ' || true)"
[ "$TABLO" -gt 0 ] || dur "döküm okunamadı ya da içinde tablo verisi yok: $DOKUM"

echo "Geri yüklenecek döküm : $DOKUM ($(du -h "$DOKUM" | cut -f1), $TABLO tablo)"
[ "$KANIT" = true ] && echo "Kanıt dosyaları       : $HEDEF/evidence ($(find "$HEDEF/evidence" -type f | wc -l) dosya)"
echo "Mevcut veritabanının yerine bu döküm geçecek; dökümden sonra girilen kayıtlar kaybolur."
if [ "$EVET" = false ]; then
  read -r -p "Devam etmek için GERI-YUKLE yazın: " yanit
  [ "$yanit" = "GERI-YUKLE" ] || dur "onay verilmedi, hiçbir şey değişmedi."
fi

echo ">> uygulama durduruluyor"
"${COMPOSE[@]}" stop yarnshipment-app >/dev/null 2>&1 || true

psql_calistir() { "${COMPOSE[@]}" exec -T postgres psql -U postgres -v ON_ERROR_STOP=1 -q "$@"; }

if [ "$(psql_calistir -d postgres -tAc "SELECT count(*) FROM pg_database WHERE datname = 'yarnshipment'")" = 1 ]; then
  ONCE="$HEDEF/veritabani/geri-yukleme-oncesi-$(date +%Y%m%d-%H%M%S).dump"
  mkdir -p "$HEDEF/veritabani"
  echo ">> mevcut veritabanının dökümü alınıyor: $ONCE"
  "${COMPOSE[@]}" exec -T postgres pg_dump -U postgres --format=custom yarnshipment > "$ONCE"
  chmod 600 "$ONCE"
  psql_calistir -d postgres -c "DROP DATABASE yarnshipment WITH (FORCE)"
fi

echo ">> veritabanı geri yükleniyor"
# Boş veritabanı, kullanıcı, roller ve veritabanı ayarları ilk kurulumdaki betikle açılır (tekrar çalıştırılabilir).
"${COMPOSE[@]}" exec -T postgres bash /docker-entrypoint-initdb.d/01-databases.sh >/dev/null
"${COMPOSE[@]}" exec -T postgres pg_restore -U postgres -d yarnshipment --exit-on-error < "$DOKUM"

if [ "$KANIT" = true ]; then
  echo ">> kanıt dosyaları yerine konuyor"
  # Birim henüz yoksa (boş kurulum) oluşsun diye uygulama konteyneri başlatılmadan oluşturulur.
  "${COMPOSE[@]}" up --no-start yarnshipment-app >/dev/null 2>&1 || true
  KLASOR="$(kanit_klasoru)"
  rsync -a "$HEDEF/evidence/" "$KLASOR/"
  # Uygulama konteynerde 10001 numaralı kullanıcıyla çalışır.
  chown -R 10001 "$KLASOR"
fi

echo ">> uygulama başlatılıyor"
"${COMPOSE[@]}" up -d >/dev/null
uygulamayi_bekle 300 || dur "geri yüklemeden sonra uygulama açılmadı."
echo ">> geri yükleme tamam. Herkes yeniden giriş yapar; bir sevkiyatı ve bir kanıt dosyasını arayüzden açarak doğrulayın."
