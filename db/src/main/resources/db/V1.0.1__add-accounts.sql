
CREATE TABLE IF NOT EXISTS accounts (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) UNIQUE NOT NULL,
    permalink VARCHAR(100) UNIQUE NOT NULL,
    owner INT REFERENCES users(id)
        ON UPDATE SET NULL
        ON DELETE SET NULL
) DEFAULT CHARACTER SET 'utf8mb4';
