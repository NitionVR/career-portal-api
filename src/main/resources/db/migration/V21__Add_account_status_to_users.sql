ALTER TABLE users ADD COLUMN account_status VARCHAR(255);
UPDATE users SET account_status = 'ACTIVE' WHERE account_status IS NULL;
ALTER TABLE users ALTER COLUMN account_status SET NOT NULL;
