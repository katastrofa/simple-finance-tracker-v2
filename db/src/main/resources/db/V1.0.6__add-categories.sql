
CREATE TABLE IF NOT EXISTS categories (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(512),
    parent INT REFERENCES categories(id)
        ON UPDATE SET NULL
        ON DELETE SET NULL,
    account INT NOT NULL REFERENCES accounts(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    owner INT REFERENCES users(id)
        ON UPDATE SET NULL
        ON DELETE SET NULL
) DEFAULT CHARACTER SET 'utf8mb4';
