CREATE TABLE IF NOT EXISTS patrons (
    user INT NOT NULL REFERENCES users(id)
        ON DELETE cascade
        ON UPDATE cascade,
    account INT NOT NULL REFERENCES accounts(id)
        ON DELETE cascade
        ON UPDATE cascade
)
