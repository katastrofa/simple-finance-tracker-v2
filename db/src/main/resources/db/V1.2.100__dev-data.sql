INSERT INTO users (email, display_name, permissions) VALUE
    ('katastrofa@katastrofa.com', 'Ofca',
        '{"global": ["Basic"], "perAccount": {}, "default": ["Basic", "ModifyOwnCategory", "ModifyOwnMoneyAccount", "ModifyOwnTransactions"]}'
    );

INSERT INTO users (email, display_name, permissions) VALUE
    ('chudera@katastrofa.com', 'Chudera',
        '{"global": ["Basic"], "perAccount": {}, "default": ["Basic", "ModifyOwnCategory", "ModifyOwnMoneyAccount", "ModifyOwnTransactions"]}'
    );

INSERT INTO patrons VALUES (1, 1), (2, 1), (3, 1);
