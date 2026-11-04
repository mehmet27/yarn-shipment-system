#!/usr/bin/env bash
# Tüm servislerin Flyway migration'larını geçici bir PostgreSQL 17 konteynerine uygular ve
# tools/db/tests altındaki kısıt testlerini çalıştırır. Servis modülleri Testcontainers
# entegrasyon testlerini (TEST-02) alana kadar migration'ların tek doğrulamasıdır.
#
# Kullanım: tools/db/verify-migrations.sh
# Ortam değişkenleri: POSTGRES_IMAGE (varsayılan postgres:17), FLYWAY_IMAGE (varsayılan flyway/flyway:11)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
POSTGRES_IMAGE="${POSTGRES_IMAGE:-postgres:17}"
FLYWAY_IMAGE="${FLYWAY_IMAGE:-flyway/flyway:11}"
CONTAINER="yarn-shipment-db-verify-$$"
PASSWORD="verify-only"

cleanup() { docker rm -f "$CONTAINER" >/dev/null 2>&1 || true; }
trap cleanup EXIT

docker run -d --name "$CONTAINER" \
  -e POSTGRES_PASSWORD="$PASSWORD" -e LOCAL_DB_PASSWORD="$PASSWORD" -e TZ=UTC \
  -v "$ROOT/deploy/postgres/initdb:/docker-entrypoint-initdb.d:ro" \
  "$POSTGRES_IMAGE" >/dev/null

# İlk açılışta init script'leri biter, sunucu yeniden başlar; hazır olduğunda yarnshipment de vardır.
for _ in $(seq 1 60); do
  if docker exec "$CONTAINER" psql -U postgres -tAc "SELECT 1 FROM pg_database WHERE datname = 'yarnshipment'" \
      2>/dev/null | grep -q 1 && docker exec "$CONTAINER" pg_isready -U postgres -h 127.0.0.1 >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

migrate() {
  local service="$1" schema="$2"
  echo "==> $service (şema $schema)"
  docker run --rm --network "container:$CONTAINER" \
    -v "$ROOT/services/$service/src/main/resources/db/migration:/flyway/sql:ro" \
    "$FLYWAY_IMAGE" -q \
    -url="jdbc:postgresql://127.0.0.1:5432/yarnshipment" -user=yarnshipment -password="$PASSWORD" \
    -defaultSchema="$schema" -schemas="$schema" -createSchemas=true \
    -locations=filesystem:/flyway/sql -validateMigrationNaming=true \
    -placeholders.app_role=yarnshipment -placeholders.readonly_role=yarnshipment \
    migrate
  local tests="$ROOT/tools/db/tests/$service.sql"
  if [[ -f "$tests" ]]; then
    docker exec -i -e PGOPTIONS="-c search_path=$schema" "$CONTAINER" \
      psql -q -o /dev/null -v ON_ERROR_STOP=1 -U postgres -d yarnshipment < "$tests"
    echo "    kısıt testleri geçti"
  fi
}

migrate master-data-service masterdata
migrate bag-service bag
migrate shipment-service shipment
migrate evidence-service evidence
migrate discrepancy-service discrepancy
migrate notification-service notification
migrate reporting-service reporting
migrate audit-service audit

echo "Tüm migration'lar ve kısıt testleri başarılı."
