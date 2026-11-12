#!/usr/bin/env bash
# Tek veritabanı (yarnshipment) ve tek kullanıcı (yarnshipment). Servis tabloları servis başına şemada durur
# (şemaları Flyway açar: masterdata, shipment, evidence, discrepancy, reporting, audit); Keycloak da aynı veritabanında "keycloak" şemasını kullanır.
# Tabloları bu script kurmaz; her servis açılışta Flyway ile kurar. Bkz. docs/veritabani-tasarimi.md.
set -euo pipefail

: "${LOCAL_DB_PASSWORD:?LOCAL_DB_PASSWORD tanımlı değil}"

run_sql() {
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
    -v password="$LOCAL_DB_PASSWORD" "$@"
}

run_sql <<'SQL'
SELECT format('CREATE ROLE yarnshipment LOGIN PASSWORD %L', :'password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'yarnshipment')
\gexec
SELECT 'CREATE DATABASE yarnshipment OWNER yarnshipment ENCODING ''UTF8'' TEMPLATE template0'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'yarnshipment')
\gexec
REVOKE ALL ON DATABASE yarnshipment FROM PUBLIC;
ALTER DATABASE yarnshipment SET timezone TO 'UTC';
-- Veritabanına bağlanan herkes tüm servis şemalarını şema adı yazmadan görebilsin (psql, DBeaver vb.).
-- Aynı ada sahip tablolar (idempotency_record vb.) için ilk şema kazanır; kesin erişim için shipment.idempotency_record gibi yazın.
ALTER ROLE yarnshipment IN DATABASE yarnshipment
  SET search_path TO masterdata, shipment, evidence, discrepancy, reporting, audit, public;
SQL

# Keycloak şemasını Keycloak kendi tablolarıyla doldurur (KC_DB_SCHEMA=keycloak).
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname yarnshipment <<'SQL'
CREATE SCHEMA IF NOT EXISTS keycloak AUTHORIZATION yarnshipment;
SQL
