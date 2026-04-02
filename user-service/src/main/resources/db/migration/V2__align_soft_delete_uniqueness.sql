ALTER TABLE users DROP CONSTRAINT uk_users_username;
ALTER TABLE users DROP CONSTRAINT uk_users_email;

-- Enforce uniqueness only for active (non-deleted) users.
CREATE UNIQUE INDEX uk_users_username_active
    ON users (CASE WHEN deleted = 0 THEN LOWER(username) END);

CREATE UNIQUE INDEX uk_users_email_active
    ON users (CASE WHEN deleted = 0 THEN LOWER(email) END);
