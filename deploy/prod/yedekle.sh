#!/usr/bin/env bash
# Gece yedeği: veritabanı dökümü + kanıt dosyaları (kanıt klasörü yalnızca eklenir, silinenler yedekte korunur).
# Kullanım (root ile, depo kökünden): ./deploy/prod/yedekle.sh
# Cron örneği (her gece 02:30):  30 2 * * * cd /opt/yarn-shipment-system && ./deploy/prod/yedekle.sh >> /var/log/yedekle.log 2>&1
# Ortam değişkenleri:
#   YEDEK_DIZINI   yedeğin yazıldığı yer (varsayılan /var/backups/yarn-shipment); ayrı bir disk ya da bağlı NAS olmalı
#   YEDEK_UZAK     isteğe bağlı şehir dışı kopya hedefi, rsync biçiminde (ör. yedek@sunucu:/yedekler/iplik)
#   YEDEK_GUN      veritabanı dökümlerinin saklanacağı gün sayısı (varsayılan 30)
set -euo pipefail
cd "$(dirname "$0")"

COMPOSE=(docker compose -f docker-compose.yml --env-file .env)
HEDEF="${YEDEK_DIZINI:-/var/backups/yarn-shipment}"
GUN="${YEDEK_GUN:-30}"
TARIH="$(date +%Y%m%d-%H%M%S)"
mkdir -p "$HEDEF/veritabani" "$HEDEF/evidence"

echo ">> $TARIH veritabanı dökümü alınıyor"
"${COMPOSE[@]}" exec -T postgres pg_dump -U postgres --format=custom yarnshipment > "$HEDEF/veritabani/yarnshipment-$TARIH.dump.part"
mv "$HEDEF/veritabani/yarnshipment-$TARIH.dump.part" "$HEDEF/veritabani/yarnshipment-$TARIH.dump"
find "$HEDEF/veritabani" -name 'yarnshipment-*.dump' -mtime +"$GUN" -delete

echo ">> kanıt dosyaları kopyalanıyor"
PROJE="$("${COMPOSE[@]}" config --format json | sed -n 's/.*"name": *"\([^"]*\)".*/\1/p' | head -1)"
KAYNAK="$(docker volume inspect -f '{{ .Mountpoint }}' "${PROJE}_evidence-data")"
# --delete YOK: uygulama dosya silmez; yedekte de hiçbir dosya silinmez.
rsync -a --exclude '.tmp/' "$KAYNAK/" "$HEDEF/evidence/"

if [ -n "${YEDEK_UZAK:-}" ]; then
  echo ">> şehir dışı kopya: $YEDEK_UZAK"
  rsync -a "$HEDEF/" "$YEDEK_UZAK/"
fi
echo ">> yedek tamam: $HEDEF"
