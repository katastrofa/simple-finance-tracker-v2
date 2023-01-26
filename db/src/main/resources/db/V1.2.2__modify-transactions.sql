ALTER TABLE transactions ADD COLUMN patron INT;
UPDATE transactions SET patron = owner;
ALTER TABLE transactions CHANGE COLUMN patron patron INT NOT NULL;
ALTER TABLE transactions ADD FOREIGN KEY (patron) REFERENCES users (id) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE transactions ADD COLUMN affiliation INT DEFAULT NULL
    REFERENCES transactions_affilition (id) ON DELETE SET NULL ON UPDATE SET NULL;
