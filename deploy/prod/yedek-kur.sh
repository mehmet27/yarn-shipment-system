#!/usr/bin/env bash
# Gece yedeğini kurar: her gece yedekle.sh çalışır (cron). install.sh bunu kendiliğinden çağırır.
# Kullanım (root ile):
#   sudo ./deploy/prod/yedek-kur.sh             kurar ya da ayarları günceller
#   sudo ./deploy/prod/yedek-kur.sh --kaldir    gece yedeğini kaldırır (alınmış yedekler silinmez)
# Ayarlar deploy/prod/.env içindedir: YEDEK_SAATI (varsayılan 02:30, sunucu saati), YEDEK_DIZINI, YEDEK_UZAK, YEDEK_GUN.
set -euo pipefail
cd "$(dirname "$0")"
# shellcheck source=deploy/prod/ortak.sh
source ./ortak.sh

CRON=/etc/cron.d/yarn-shipment-yedek
GUNLUK=/var/log/yarn-shipment-yedek.log
DONDURME=/etc/logrotate.d/yarn-shipment-yedek

[ "$(id -u)" = 0 ] || dur "root ile çalıştırın (sudo ./deploy/prod/yedek-kur.sh)."

if [ "${1:-}" = "--kaldir" ]; then
  rm -f "$CRON" "$DONDURME"
  echo ">> gece yedeği kaldırıldı (alınmış yedeklere dokunulmadı)."
  exit 0
fi

[ -d /etc/cron.d ] || dur "/etc/cron.d yok; cron kurulu değil (apt-get install cron)."
SAAT="$(ayar YEDEK_SAATI 02:30)"
[[ "$SAAT" =~ ^([01][0-9]|2[0-3]):[0-5][0-9]$ ]] || dur "YEDEK_SAATI SS:DD biçiminde olmalı (ör. 02:30); verilen: $SAAT"
SA="${SAAT%%:*}"
DK="${SAAT##*:}"

cat > "$CRON" <<CRON_SONU
# yedek-kur.sh tarafından yazıldı; elle değiştirmeyin. Ayarlar: $PWD/.env (YEDEK_SAATI, YEDEK_DIZINI, YEDEK_UZAK, YEDEK_GUN)
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
$((10#$DK)) $((10#$SA)) * * * root "$PWD/yedekle.sh" >> $GUNLUK 2>&1
CRON_SONU
chmod 644 "$CRON"

if [ -d /etc/logrotate.d ]; then
  cat > "$DONDURME" <<DONDURME_SONU
$GUNLUK {
  monthly
  rotate 12
  compress
  missingok
  notifempty
}
DONDURME_SONU
fi

HEDEF="$(ayar YEDEK_DIZINI /var/backups/yarn-shipment)"
echo ">> gece yedeği kuruldu: her gece $SAAT, hedef $HEDEF, günlük $GUNLUK"
if [ -z "$(env_al YEDEK_DIZINI)" ]; then
  echo "   UYARI: yedek uygulamayla AYNI diskte duruyor. Disk bozulursa yedek de gider; ayrı bir disk ya da NAS bağlayıp"
  echo "   deploy/prod/.env içine YEDEK_DIZINI=/mnt/yedek yazın (şehir dışı kopya için YEDEK_UZAK)."
fi
