-- deploy/postgres/initdb/01-databases.sh ile aynı roller; konteynerin kullanıcısı migrator rolüdür.
CREATE ROLE audit_writer LOGIN PASSWORD 'test';
CREATE ROLE audit_reader LOGIN PASSWORD 'test';
GRANT CONNECT ON DATABASE audit_db TO audit_writer, audit_reader;
