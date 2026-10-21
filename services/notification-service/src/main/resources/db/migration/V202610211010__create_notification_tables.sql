-- notification_db: bildirim ve bildirim tercihi (veritabanı tasarımı, "audit_db, notification_db, reporting_db").

CREATE TABLE notification (
    id                uuid        PRIMARY KEY,
    recipient_user_id uuid        NOT NULL,
    channel           text        NOT NULL CHECK (channel IN ('EMAIL', 'IN_APP')),
    template_code     text        NOT NULL,
    payload           jsonb       NOT NULL,
    status            text        NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'READ')),
    attempts          integer     NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    sent_at           timestamptz,
    read_at           timestamptz,
    created_at        timestamptz NOT NULL,
    created_by        uuid        NOT NULL,
    updated_at        timestamptz,
    updated_by        uuid,
    version           integer     NOT NULL DEFAULT 0
);

COMMENT ON COLUMN notification.payload IS 'Şablon değişkenleri; kişisel veri içermez, kimlik taşır (EVT-08)';

CREATE INDEX notification_recipient_status_idx ON notification (recipient_user_id, status, created_at DESC);
CREATE INDEX notification_pending_idx ON notification (created_at) WHERE status = 'PENDING';

CREATE TABLE notification_preference (
    user_id    uuid        NOT NULL,
    event_type text        NOT NULL,
    channel    text        NOT NULL CHECK (channel IN ('EMAIL', 'IN_APP')),
    enabled    boolean     NOT NULL,
    created_at timestamptz NOT NULL,
    created_by uuid        NOT NULL,
    updated_at timestamptz,
    updated_by uuid,
    version    integer     NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, event_type, channel)
);

GRANT SELECT, INSERT, UPDATE ON notification, notification_preference TO ${app_role};
