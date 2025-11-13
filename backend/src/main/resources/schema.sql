-- Keep the users.role check constraint aligned with application roles.
ALTER TABLE IF EXISTS users DROP CONSTRAINT IF EXISTS users_role_check;

ALTER TABLE IF EXISTS users
    ADD CONSTRAINT users_role_check CHECK (
        role IN ('ADMIN', 'USER')
    );
