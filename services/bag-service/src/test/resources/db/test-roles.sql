-- deploy/postgres/initdb/01-databases.sh ile aynı roller; konteynerin kullanıcısı migrator rolüdür.
CREATE ROLE bag_app LOGIN PASSWORD 'test';
CREATE ROLE bag_readonly LOGIN PASSWORD 'test';
GRANT CONNECT ON DATABASE bag_db TO bag_app, bag_readonly;
