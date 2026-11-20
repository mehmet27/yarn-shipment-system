#!/usr/bin/env bash
# Sunucunun durumunu tek bakışta gösterir: servisler, son yedek, disk. Bir sorun varsa sıfırdan farklı kodla çıkar.
# Kullanım:  ./deploy/prod/durum.sh
set -euo pipefail
cd "$(dirname "$0")"
# shellcheck source=deploy/prod/ortak.sh
source ./ortak.sh

SORUN=0
uyar() {
  echo "   UYARI: $*"
  SORUN=1
}

echo "== Servisler"
"${COMPOSE[@]}" ps --format 'table {{.Service}}\t{{.State}}\t{{.Status}}'
for servis in caddy web postgres yarnshipment-app; do
  kimlik="$(konteyner "$servis")"
  durum=""
  [ -z "$kimlik" ] || durum="$(docker inspect -f '{{.State.Status}}' "$kimlik" 2>/dev/null || true)"
  [ "$durum" = running ] || uyar "$servis çalışmıyor."
done

echo "== Yedek"
HEDEF="$(ayar YEDEK_DIZINI /var/backups/yarn-shipment)"
if [ -f "$HEDEF/son-basarili-yedek.txt" ]; then
  echo "   Son başarılı yedek: $(cat "$HEDEF/son-basarili-yedek.txt")  ($HEDEF)"
  # 26 saatten eski yedek, gece yedeğinin atlandığını gösterir.
  [ -n "$(find "$HEDEF/son-basarili-yedek.txt" -mmin -1560)" ] || uyar "son yedek 26 saatten eski."
else
  uyar "henüz başarılı yedek yok ($HEDEF). Elle deneyin: sudo ./deploy/prod/yedekle.sh"
fi
[ -f /etc/cron.d/yarn-shipment-yedek ] || uyar "gece yedeği kurulu değil (sudo ./deploy/prod/yedek-kur.sh)."

echo "== Disk"
df -h / "$HEDEF" 2>/dev/null | awk 'NR == 1 || !gorulen[$0]++'
DOLULUK="$(df --output=pcent / | tail -1 | tr -dc '0-9')"
[ "${DOLULUK:-0}" -lt 85 ] || uyar "disk %${DOLULUK} dolu."

[ "$SORUN" = 0 ] && echo "Her şey yolunda."
exit "$SORUN"
