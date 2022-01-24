
CREATE TABLE IF NOT EXISTS transactions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    date DATE NOT NULL,
    type ENUM('income','expense','transfer') NOT NULL DEFAULT 'expense',
    amount DECIMAL(24, 2) NOT NULL,
    description VARCHAR(512) NOT NULL DEFAULT '',
    category INT NOT NULL REFERENCES categories(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    money_account INT NOT NULL REFERENCES money_accounts(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    tracking ENUM('none','auto','verified') NOT NULL DEFAULT 'none',
    dest_amount DECIMAL(24, 2),
    dest_money_account INT REFERENCES money_accounts(id)
        ON UPDATE SET NULL
        ON DELETE SET NULL,
    owner INT REFERENCES users(id)
        ON UPDATE SET NULL
        ON DELETE SET NULL
) DEFAULT CHARACTER SET 'utf8mb4';
