#!/usr/bin/env bash
# Yerel ortamı sıfırlar: Docker hacimlerini (PostgreSQL verisi) SİLER; kanıt dosyaları uygulama klasöründedir (EVIDENCE_STORAGE_ROOT), onları ayrıca silin ve yeniden başlatır. Yalnızca yerel
# geliştirme içindir; geri alınamaz. Yalnızca `admin` kullanıcısı için yarnshipment-app'i LOCAL_SAMPLE_USERS=false ile
# başlatın (bkz. kök README.md); admin şifresi LOCAL_TEST_USER_PASSWORD değeridir.
#   bash deploy/temiz-kurulum.sh
set -euo pipefail
cd "$(dirname "$0")"
docker compose -f docker-compose.yml down -v
docker compose -f docker-compose.yml up -d
echo "Temiz ortam hazır. Uygulamayı LOCAL_SAMPLE_USERS=false ile başlatın; giriş: admin (şifre: LOCAL_TEST_USER_PASSWORD)."
