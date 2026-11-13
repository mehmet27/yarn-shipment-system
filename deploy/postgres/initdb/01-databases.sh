#!/usr/bin/env bash
# Tek veritabanı (yarnshipment) ve tek kullanıcı (yarnshipment). Servis tabloları servis başına şemada durur
# (şemaları Flyway açar: platform, shipment, evidence, discrepancy, reporting, audit).
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
-- Denetim kaydı rolleri (audit şemasındaki V202611181000 migration'ı yetkileri verir). İkisi de giriş yapamaz; uygulama
-- kullanıcısı üyedir ama yetkilerini miras almaz, denetim adaptörleri SET LOCAL ROLE ile o rolün altında çalışır.
SELECT 'CREATE ROLE audit_writer NOLOGIN'
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'audit_writer')
\gexec
SELECT 'CREATE ROLE audit_reader NOLOGIN'
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'audit_reader')
\gexec
GRANT audit_writer, audit_reader TO yarnshipment WITH INHERIT FALSE, SET TRUE;
ALTER DATABASE yarnshipment SET timezone TO 'UTC';
-- Veritabanına bağlanan herkes tüm servis şemalarını şema adı yazmadan görebilsin (psql, DBeaver vb.).
-- Ortak tablolar (idempotency_record, audit_local) platform şemasındadır; platform.idempotency_record diye yazın.
ALTER ROLE yarnshipment IN DATABASE yarnshipment
  SET search_path TO platform, shipment, evidence, discrepancy, reporting, audit, public;
SQL
