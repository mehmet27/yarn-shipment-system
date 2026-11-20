#!/usr/bin/env bash
# Ubuntu sunucuda tek komutla kurulum / güncelleme:  ./deploy/prod/install.sh
# - Docker yoksa kurar, .env yoksa örnekten oluşturup boş sırları rastgele üretir,
#   imajları derler, her şeyi başlatır, uygulama açılana kadar bekler ve gece yedeğini kurar.
# - Güncellemede (veritabanı zaten çalışıyorsa) önce veritabanı dökümü alır: yeni sürüm açılışta şemayı değiştirir.
# Yeniden çalıştırmak güvenlidir (var olan .env ve veriler korunur); kod güncellemesi için: git pull && ./deploy/prod/install.sh
set -euo pipefail

cd "$(dirname "$0")"
# shellcheck source=deploy/prod/ortak.sh
source ./ortak.sh

if ! command -v docker >/dev/null 2>&1; then
  echo ">> Docker kuruluyor..."
  curl -fsSL https://get.docker.com | sh
fi
if ! docker compose version >/dev/null 2>&1; then
  dur "Docker Compose eklentisi bulunamadı; 'sudo apt-get install docker-compose-plugin' çalıştırın."
fi

# Yedek ve geri yükleme kanıt dosyalarını rsync ile kopyalar.
if ! command -v rsync >/dev/null 2>&1; then
  if [ "$(id -u)" = 0 ] && command -v apt-get >/dev/null 2>&1; then
    echo ">> rsync kuruluyor..."
    apt-get install -y -qq rsync >/dev/null || echo "UYARI: rsync kurulamadı; yedek için elle kurun (apt-get install rsync)."
  else
    echo "UYARI: rsync kurulu değil; yedek için kurun (sudo apt-get install rsync)."
  fi
fi

if [ ! -f "$ENV_FILE" ]; then
  cp .env.example "$ENV_FILE"
  chmod 600 "$ENV_FILE"
  echo ">> $ENV_FILE oluşturuldu."
fi

# openssl yoksa çekirdeğin rastgele kaynağı kullanılır.
rand() {
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -base64 96 | tr -dc 'A-Za-z0-9' | head -c "$1"
  else
    LC_ALL=C tr -dc 'A-Za-z0-9' < /dev/urandom | head -c "$1"
  fi
}
set_var() { sed -i "s|^$1=.*|$1=$2|" "$ENV_FILE"; }

# Boş sırları üret (dolu olanlara dokunulmaz).
for v in POSTGRES_PASSWORD DB_PASSWORD EVIDENCE_STORAGE_SIGNING_KEY; do
  [ -n "$(env_al "$v")" ] || set_var "$v" "$(rand 32)"
done
GENERATED_ADMIN_PASSWORD=""
if [ -z "$(env_al INITIAL_ADMIN_PASSWORD)" ]; then
  GENERATED_ADMIN_PASSWORD="$(rand 16)Aa1"
  set_var INITIAL_ADMIN_PASSWORD "$GENERATED_ADMIN_PASSWORD"
fi

# Örnek alan adı ve e-posta değiştirilmeden kurulum yapılmaz (sertifika alınamaz).
if grep -Eq '^(APP_HOST|ACME_EMAIL)=.*ornek\.com\.tr' "$ENV_FILE" || [ -z "$(env_al APP_HOST)" ] || [ -z "$(env_al ACME_EMAIL)" ]; then
  dur "$PWD/$ENV_FILE içindeki APP_HOST ve ACME_EMAIL alanlarını kendi alan adınız ve e-postanızla doldurup betiği yeniden çalıştırın."
fi

# Ayar dosyası geçerli mi (eksik değişken, bozuk YAML) derlemeye başlamadan görülsün.
"${COMPOSE[@]}" config --quiet

# Güncelleme: çalışan bir veritabanı varsa önce dökümünü al (yeni sürümün şema değişikliği geri alınamaz).
PG="$(konteyner postgres)"
if [ -n "$PG" ] && [ "$(docker inspect -f '{{.State.Status}}' "$PG" 2>/dev/null || true)" = running ] \
  && "${COMPOSE[@]}" exec -T postgres psql -U postgres -tAc "SELECT 1 FROM pg_database WHERE datname = 'yarnshipment'" 2>/dev/null | grep -q 1; then
  echo ">> Güncelleme öncesi veritabanı dökümü alınıyor..."
  ./yedekle.sh --yalnizca-veritabani
fi

echo ">> İmajlar derleniyor ve servisler başlatılıyor (ilk seferde 10-20 dakika sürebilir)..."
"${COMPOSE[@]}" up -d --build --remove-orphans

uygulamayi_bekle 600 || dur "uygulama açılmadı. Tüm günlük: docker compose -f deploy/prod/docker-compose.yml --env-file deploy/prod/.env logs yarnshipment-app"

# Gece yedeği (root değilse ya da cron yoksa atlanır; elle: sudo ./deploy/prod/yedek-kur.sh).
if [ "$(id -u)" = 0 ] && [ -d /etc/cron.d ]; then
  ./yedek-kur.sh
else
  echo "UYARI: gece yedeği kurulmadı (root değilsiniz ya da cron yok). Kurmak için: sudo ./deploy/prod/yedek-kur.sh"
fi

echo
echo "Kurulum tamam. Durum: ./deploy/prod/durum.sh"
echo "Uygulama:  https://$(env_al APP_HOST)"
if [ -n "$GENERATED_ADMIN_PASSWORD" ]; then
  echo "Uygulama yöneticisi: admin / $GENERATED_ADMIN_PASSWORD  (ilk girişte şifre değiştirilir)"
  echo "Not: şifreler ve sırlar $PWD/$ENV_FILE içindedir; bu dosyayı güvenli bir yere yedekleyin."
else
  echo "Uygulama yöneticisi: admin (ilk kurulumda şifre $PWD/$ENV_FILE içindeki INITIAL_ADMIN_PASSWORD; ilk girişte değiştirilir)"
fi
