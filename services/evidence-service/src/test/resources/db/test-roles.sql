-- deploy/postgres/initdb/01-databases.sh ile aynı roller; konteynerin kullanıcısı migrator rolüdür.
CREATE ROLE evidence_app LOGIN PASSWORD 'test';
CREATE ROLE evidence_readonly LOGIN PASSWORD 'test';
GRANT CONNECT ON DATABASE evidence_db TO evidence_app, evidence_readonly;
