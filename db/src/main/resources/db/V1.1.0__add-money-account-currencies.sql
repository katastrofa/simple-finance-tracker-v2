
CREATE TABLE IF NOT EXISTS money_account_currencies (
    id INT PRIMARY KEY AUTO_INCREMENT,
    money_account INT NOT NULL REFERENCES money_accounts(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    currency VARCHAR(16) NOT NULL REFERENCES currencies(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    start_amount DECIMAL(24, 2) NOT NULL
) DEFAULT CHARACTER SET 'utf8mb4';
