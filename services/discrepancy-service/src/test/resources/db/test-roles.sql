-- deploy/postgres/initdb/01-databases.sh ile aynı roller; konteynerin kullanıcısı migrator rolüdür.
CREATE ROLE discrepancy_app LOGIN PASSWORD 'test';
CREATE ROLE discrepancy_readonly LOGIN PASSWORD 'test';
GRANT CONNECT ON DATABASE discrepancy_db TO discrepancy_app, discrepancy_readonly;
