CREATE TABLE users (
    email TEXT NOT NULL,
    hashedPassword TEXT NOT NULL,
    firstName TEXT,
    lastName TEXT,
    company TEXT,
    role TEXT NOT NULL
);

ALTER TABLE users
ADD CONSTRAINT pk_users PRIMARY KEY (email);

INSERT INTO users (
    email,
    hashedPassword,
    firstName,
    lastName,
    company,
    role
) VALUES (
    'norbert@home.com',
    '$2a$10$crTNsbU2c/JK3vYgF3ADTedEjHwHur03COlnZd.MJql6Tj7o5IJZK',
    'Norbert',
    'Scheller',
    'Home.com',
    'ADMIN'
);

INSERT INTO users (
    email,
    hashedPassword,
    firstName,
    lastName,
    company,
    role
) VALUES (
    'jana@home.com',
    '$2a$10$8b3QoQpH045L57Y1jSKVf.FNvPkeScE6nN7a.SMbVnrt5TXPftq2G',
    'Jana',
    'Otte',
    'Home.com',
    'RECRUITER'
);
