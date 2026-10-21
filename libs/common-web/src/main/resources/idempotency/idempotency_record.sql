-- Referans şema (API-04): her servis bu tabloyu kendi Flyway migration'ına kopyalar ve uygulama rolüne
-- SELECT, INSERT, UPDATE, DELETE yetkisi verir. Kayıtlar kullanıcıya özeldir (actor + idempotency_key).
CREATE TABLE idempotency_record (
    actor                 text        NOT NULL,
    idempotency_key       text        NOT NULL,
    fingerprint           text        NOT NULL,
    state                 text        NOT NULL CHECK (state IN ('IN_PROGRESS', 'COMPLETED')),
    response_status       integer,
    response_content_type text,
    response_location     text,
    response_body         bytea,
    created_at            timestamptz NOT NULL,
    PRIMARY KEY (actor, idempotency_key)
);

CREATE INDEX idempotency_record_created_idx ON idempotency_record (created_at);
