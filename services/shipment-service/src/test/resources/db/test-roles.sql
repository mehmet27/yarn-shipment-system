-- deploy/postgres/initdb/01-databases.sh ile aynı roller; konteynerin kullanıcısı migrator rolüdür.
CREATE ROLE shipment_app LOGIN PASSWORD 'test';
CREATE ROLE shipment_readonly LOGIN PASSWORD 'test';
GRANT CONNECT ON DATABASE shipment_db TO shipment_app, shipment_readonly;
