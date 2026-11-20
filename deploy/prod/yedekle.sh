#!/usr/bin/env bash
# Yedek: veritabanı dökümü + kanıt dosyaları (kanıt klasörü yalnızca eklenir, silinenler yedekte korunur).
# Kullanım (root ile):  ./deploy/prod/yedekle.sh [--yalnizca-veritabani]
# Gece yedeğini kurmak için:  sudo ./deploy/prod/yedek-kur.sh   (install.sh bunu kendiliğinden yapar)
# Geri yükleme:  ./deploy/prod/geri-yukle.sh   (ayrıntı: DEPLOY.md, "Yedekleme ve geri yükleme")
#
# Ayarlar ortam değişkeninden ya da deploy/prod/.env dosyasından okunur (ortam değişkeni önceliklidir):
#   YEDEK_DIZINI   yedeğin yazıldığı yer (varsayılan /var/backups/yarn-shipment); ayrı bir disk ya da bağlı NAS olmalı
#   YEDEK_UZAK     isteğe bağlı şehir dışı kopya hedefi, rsync biçiminde (ör. yedek@sunucu:/yedekler/iplik)
#   YEDEK_GUN      veritabanı dökümlerinin saklanacağı gün sayısı (varsayılan 30)
#
# Başarısız olursa sıfırdan farklı kodla çıkar ve eski dökümlere dokunmaz. Her başarılı yedekten sonra
# <YEDEK_DIZINI>/son-basarili-yedek.txt güncellenir (durum.sh bunu okur).
set -euo pipefail
cd "$(dirname "$0")"
# shellcheck source=deploy/prod/ortak.sh
source ./ortak.sh

YALNIZCA_VT=false
[ "${1:-}" = "--yalnizca-veritabani" ] && YALNIZCA_VT=true

HEDEF="$(ayar YEDEK_DIZINI /var/backups/yarn-shipment)"
GUN="$(ayar YEDEK_GUN 30)"
UZAK="$(ayar YEDEK_UZAK)"
TARIH="$(date +%Y%m%d-%H%M%S)"
mkdir -p "$HEDEF/veritabani" "$HEDEF/evidence"
chmod 700 "$HEDEF"

# Aynı anda iki yedek çalışmasın (uzun süren gece yedeği ile elle başlatılan çakışmasın).
if command -v flock >/dev/null 2>&1; then
  exec 9>"$HEDEF/.yedek.kilit"
  flock -n 9 || dur "başka bir yedekleme sürüyor ($HEDEF/.yedek.kilit)."
fi

DOKUM="$HEDEF/veritabani/yarnshipment-$TARIH.dump"
trap 'rm -f "$DOKUM.part"' EXIT

echo ">> $TARIH veritabanı dökümü alınıyor"
"${COMPOSE[@]}" exec -T postgres pg_dump -U postgres --format=custom yarnshipment > "$DOKUM.part"
# Döküm okunabiliyor mu? Bozuk ya da yarım döküm yedek sayılmaz.
[ -s "$DOKUM.part" ] || dur "veritabanı dökümü boş."
TABLO="$("${COMPOSE[@]}" exec -T postgres pg_restore --list < "$DOKUM.part" | grep -c ' TABLE DATA ' || true)"
[ "$TABLO" -gt 0 ] || dur "veritabanı dökümü doğrulanamadı (içinde tablo verisi yok)."
mv "$DOKUM.part" "$DOKUM"
chmod 600 "$DOKUM"
echo "   $(du -h "$DOKUM" | cut -f1), $TABLO tablo"
# Eski dökümler yalnızca yeni döküm doğrulandıktan sonra silinir.
find "$HEDEF/veritabani" -name 'yarnshipment-*.dump' -mtime +"$GUN" -delete

if [ "$YALNIZCA_VT" = false ]; then
  command -v rsync >/dev/null 2>&1 || dur "rsync kurulu değil (apt-get install rsync)."
  echo ">> kanıt dosyaları kopyalanıyor"
  KAYNAK="$(kanit_klasoru)"
  # --delete YOK: uygulama dosya silmez; yedekte de hiçbir dosya silinmez.
  rsync -a --exclude '.tmp/' "$KAYNAK/" "$HEDEF/evidence/"
  echo "   $(find "$HEDEF/evidence" -type f | wc -l) dosya"

  if [ -n "$UZAK" ]; then
    echo ">> şehir dışı kopya: $UZAK"
    rsync -a --exclude '.yedek.kilit' "$HEDEF/" "$UZAK/"
  fi
  date '+%Y-%m-%d %H:%M:%S' > "$HEDEF/son-basarili-yedek.txt"
fi
echo ">> yedek tamam: $DOKUM"
