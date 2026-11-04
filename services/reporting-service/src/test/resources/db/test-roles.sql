-- deploy/postgres/initdb/01-databases.sh ile aynı roller; konteynerin kullanıcısı migrator rolüdür.
CREATE ROLE reporting_app LOGIN PASSWORD 'test';
CREATE ROLE reporting_readonly LOGIN PASSWORD 'test';
GRANT CONNECT ON DATABASE reporting_db TO reporting_app, reporting_readonly;
