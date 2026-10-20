-- Real logins, replacing the demo users that live in application.properties.
-- Only the BCrypt HASH of a password is stored; the password itself is never saved anywhere.
CREATE TABLE app_user (
    id             BIGSERIAL    PRIMARY KEY,
    username       VARCHAR(50)  NOT NULL UNIQUE,              -- no two people can share a username
    password_hash  VARCHAR(100) NOT NULL,                     -- BCrypt output, e.g. $2a$10$N9qo8uLO... (60 chars)
    role           VARCHAR(20)  NOT NULL
                   CHECK (role IN ('ADMIN', 'WRITER', 'READER')),
    -- A READER belongs to exactly one account (the only one they may see).
    -- ADMIN and WRITER see no single account, so this stays NULL for them.
    account_id     BIGINT       REFERENCES account (id),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- Enforce that pairing in the database, not only in Java
    CONSTRAINT reader_has_account CHECK (
        (role = 'READER' AND account_id IS NOT NULL) OR
        (role <> 'READER' AND account_id IS NULL)
    )
);

CREATE INDEX idx_app_user_account ON app_user (account_id);
