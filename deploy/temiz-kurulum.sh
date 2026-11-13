#!/usr/bin/env bash
# Yerel ortamı sıfırlar: Docker hacimlerini (PostgreSQL, Keycloak, MinIO verisi) SİLER ve yalnızca `admin` kullanıcılı
# temiz bir Keycloak ile yeniden başlatır. Yalnızca yerel geliştirme içindir; geri alınamaz.
#   bash deploy/temiz-kurulum.sh
# Ardından yarnshipment-app'i yerel profille, ama örnek sevkiyatlarla değil tanımlarla başlatmak için bkz. robot/README.md.
set -euo pipefail
cd "$(dirname "$0")"
docker compose -f docker-compose.yml -f docker-compose.temiz.yml down -v
docker compose -f docker-compose.yml -f docker-compose.temiz.yml up -d
echo "Temiz ortam hazır. Giriş: admin (şifre: deploy/.env içindeki KEYCLOAK_TEST_USER_PASSWORD)."
