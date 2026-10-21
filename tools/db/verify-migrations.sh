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

# İlk açılışta init script'leri biter, sunucu yeniden başlar; hazır olduğunda keycloak_db de vardır.
for _ in $(seq 1 60); do
  if docker exec "$CONTAINER" psql -U postgres -tAc "SELECT 1 FROM pg_database WHERE datname = 'keycloak_db'" \
      2>/dev/null | grep -q 1 && docker exec "$CONTAINER" pg_isready -U postgres -h 127.0.0.1 >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

migrate() {
  local service="$1" database="$2" prefix="$3" app_role="$4" readonly_role="$5"
  echo "==> $service ($database)"
  docker run --rm --network "container:$CONTAINER" \
    -v "$ROOT/services/$service/src/main/resources/db/migration:/flyway/sql:ro" \
    "$FLYWAY_IMAGE" -q \
    -url="jdbc:postgresql://127.0.0.1:5432/$database" -user="${prefix}_migrator" -password="$PASSWORD" \
    -locations=filesystem:/flyway/sql -validateMigrationNaming=true \
    -placeholders.app_role="$app_role" -placeholders.readonly_role="$readonly_role" \
    migrate
  local tests="$ROOT/tools/db/tests/$service.sql"
  if [[ -f "$tests" ]]; then
    docker exec -i "$CONTAINER" psql -q -o /dev/null -v ON_ERROR_STOP=1 -U postgres -d "$database" < "$tests"
    echo "    kısıt testleri geçti"
  fi
}

migrate master-data-service masterdata_db masterdata masterdata_app masterdata_readonly
migrate bag-service bag_db bag bag_app bag_readonly
migrate shipment-service shipment_db shipment shipment_app shipment_readonly
migrate evidence-service evidence_db evidence evidence_app evidence_readonly
migrate discrepancy-service discrepancy_db discrepancy discrepancy_app discrepancy_readonly
migrate notification-service notification_db notification notification_app notification_readonly
migrate reporting-service reporting_db reporting reporting_app reporting_readonly
migrate audit-service audit_db audit audit_writer audit_reader

echo "Tüm migration'lar ve kısıt testleri başarılı."
