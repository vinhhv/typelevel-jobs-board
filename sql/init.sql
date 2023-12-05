CREATE DATABASE board;
\c board;

CREATE TABLE jobs(
    id UUID DEFAULT gen_random_uuid(),
    date BIGINT NOT NULL,
    ownerEmail TEXT NOT NULL,
    company TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    externalUrl TEXT NOT NULL,
    remote BOOLEAN NOT NULL DEFAULT FALSE,
    location TEXT,
    salaryLo INTEGER,
    salaryHi INTEGER,
    currency TEXT,
    country TEXT,
    tags TEXT[],
    image TEXT,
    seniority TEXT,
    other TEXT,
    active BOOLEAN NOT NULL DEFAULT FALSE
);

ALTER TABLE jobs
ADD CONSTRAINT pk_jobs PRIMARY KEY (id);

CREATE TABLE users(
    email TEXT NOT NULL,
    hashedPassword TEXT NOT NULL,
    firstName TEXT,
    lastName TEXT,
    company TEXT,
    role TEXT NOT NULL
);

ALTER TABLE users
ADD CONSTRAINT pk_users PRIMARY KEY (email);

CREATE TABLE recoverytokens (
    email TEXT NOT NULL,
    token TEXT NOT NULL,
    expiration BIGINT NOT NULL
);

ALTER TABLE recoverytokens
ADD CONSTRAINT pk_recoverytokens PRIMARY KEY (email);
