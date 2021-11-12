
CREATE TABLE IF NOT EXISTS user_account (
    user INT NOT NULL REFERENCES users(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    account INT NOT NULL REFERENCES accounts(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
) DEFAULT CHARACTER SET 'utf8mb4';