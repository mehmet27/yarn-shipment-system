#!/usr/bin/env bash
# Servis başına veritabanı ve roller (veritabanı tasarımı, "Servis başına veritabanı" ve
# "Kullanıcılar ve yetkiler"). Tabloları bu script kurmaz; her servis açılışta Flyway ile kurar.
#
# Her servis için:
#   <servis>_migrator  veritabanının sahibi, yalnızca Flyway kullanır (DDL)
#   <servis>_app       uygulamanın bağlandığı rol (DML; DELETE yetkisi migration'larda hiç verilmez)
#   <servis>_readonly  raporlama ve inceleme için salt okuma
# audit_db istisnadır: audit_writer yalnızca audit_append() çalıştırır, audit_reader yalnızca okur.
set -euo pipefail

: "${LOCAL_DB_PASSWORD:?LOCAL_DB_PASSWORD tanımlı değil}"

SERVICES=(masterdata bag shipment evidence discrepancy notification reporting)

run_sql() {
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
    -v password="$LOCAL_DB_PASSWORD" "$@"
}

create_role() {
  local role="$1"
  run_sql -v role="$role" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'role', :'password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'role')
\gexec
SQL
}

create_database() {
  local database="$1" owner="$2"
  run_sql -v database="$database" -v owner="$owner" <<'SQL'
SELECT format('CREATE DATABASE %I OWNER %I ENCODING ''UTF8'' TEMPLATE template0', :'database', :'owner')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'database')
\gexec
SELECT format('REVOKE ALL ON DATABASE %I FROM PUBLIC', :'database')
\gexec
SELECT format('ALTER DATABASE %I SET timezone TO ''UTC''', :'database')
\gexec
SQL
}

grant_connect() {
  local database="$1"
  shift
  for role in "$@"; do
    run_sql -v database="$database" -v role="$role" <<'SQL'
SELECT format('GRANT CONNECT ON DATABASE %I TO %I', :'database', :'role')
\gexec
SQL
  done
}

for service in "${SERVICES[@]}"; do
  create_role "${service}_migrator"
  create_role "${service}_app"
  create_role "${service}_readonly"
  create_database "${service}_db" "${service}_migrator"
  grant_connect "${service}_db" "${service}_app" "${service}_readonly"
done

create_role audit_migrator
create_role audit_writer
create_role audit_reader
create_database audit_db audit_migrator
grant_connect audit_db audit_writer audit_reader

create_role keycloak
create_database keycloak_db keycloak
