CREATE TABLE IF NOT EXISTS transactions_affiliation (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    split_type ENUM('Exact', 'Percentage', 'Even') NOT NULL DEFAULT 'Even',
    info VARCHAR(500)
) DEFAULT CHARACTER SET 'utf8mb4';
