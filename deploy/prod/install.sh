#!/usr/bin/env bash
# Ubuntu sunucuda tek komutla kurulum / güncelleme:  ./deploy/prod/install.sh
# - Docker yoksa kurar, .env yoksa örnekten oluşturup boş sırları rastgele üretir,
#   imajları derler ve her şeyi başlatır.
# Yeniden çalıştırmak güvenlidir (var olan .env ve veriler korunur); kod güncellemesi için: git pull && ./deploy/prod/install.sh
set -euo pipefail

cd "$(dirname "$0")"
ENV_FILE=.env

if ! command -v docker >/dev/null 2>&1; then
  echo ">> Docker kuruluyor..."
  curl -fsSL https://get.docker.com | sh
fi
if ! docker compose version >/dev/null 2>&1; then
  echo "Docker Compose eklentisi bulunamadı; 'sudo apt-get install docker-compose-plugin' çalıştırın." >&2
  exit 1
fi

if [ ! -f "$ENV_FILE" ]; then
  cp .env.example "$ENV_FILE"
  chmod 600 "$ENV_FILE"
  echo ">> $ENV_FILE oluşturuldu."
fi

rand() { openssl rand -base64 48 | tr -dc 'A-Za-z0-9' | head -c "$1"; }
get() { grep -E "^$1=" "$ENV_FILE" | head -1 | cut -d= -f2-; }
set_var() { sed -i "s|^$1=.*|$1=$2|" "$ENV_FILE"; }

# Boş sırları üret (dolu olanlara dokunulmaz).
for v in POSTGRES_PASSWORD DB_PASSWORD \
         EVIDENCE_STORAGE_SIGNING_KEY; do
  [ -n "$(get "$v")" ] || set_var "$v" "$(rand 32)"
done
GENERATED_ADMIN_PASSWORD=""
if [ -z "$(get INITIAL_ADMIN_PASSWORD)" ]; then
  GENERATED_ADMIN_PASSWORD="$(rand 16)Aa1"
  set_var INITIAL_ADMIN_PASSWORD "$GENERATED_ADMIN_PASSWORD"
fi

# Örnek alan adları değiştirilmeden kurulum yapılmaz.
if grep -Eq '^(APP_HOST)=.*ornek\.com\.tr' "$ENV_FILE"; then
  echo "HATA: $PWD/$ENV_FILE içindeki APP_HOST ve e-posta alanlarını kendi alan adınızla doldurup betiği yeniden çalıştırın." >&2
  exit 1
fi

echo ">> İmajlar derleniyor ve servisler başlatılıyor (ilk seferde 10-20 dakika sürebilir)..."
docker compose --env-file "$ENV_FILE" up -d --build

echo
echo "Kurulum başlatıldı. Durum: docker compose -f deploy/prod/docker-compose.yml --env-file deploy/prod/.env ps"
echo "Uygulama:  https://$(get APP_HOST)"
echo "Uygulama yöneticisi: admin / $(get INITIAL_ADMIN_PASSWORD)  (ilk girişte şifre değiştirilir)"
[ -z "$GENERATED_ADMIN_PASSWORD" ] || echo "Not: şifreler ve sırlar $PWD/$ENV_FILE içindedir; bu dosyayı güvenli bir yere yedekleyin."
