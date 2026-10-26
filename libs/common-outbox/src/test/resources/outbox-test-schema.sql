-- Testler için outbox_event ve processed_event; servis migration'larındaki tanımın bölümlenmemiş eşi.
CREATE TABLE outbox_event (
    id             uuid        NOT NULL,
    created_at     timestamptz NOT NULL,
    aggregate_type text        NOT NULL,
    aggregate_id   uuid        NOT NULL,
    event_type     text        NOT NULL,
    payload        jsonb       NOT NULL,
    headers        jsonb       NOT NULL DEFAULT '{}'::jsonb,
    occurred_at    timestamptz NOT NULL,
    published_at   timestamptz,
    PRIMARY KEY (id, created_at)
);

CREATE TABLE processed_event (
    event_id     uuid        PRIMARY KEY,
    event_type   text        NOT NULL,
    processed_at timestamptz NOT NULL
);
