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
    'hashedPassword',
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
    'hashedPassword',
    'Jana',
    'Otte',
    'Home.com',
    'RECRUITER'
);
