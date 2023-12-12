CREATE TABLE recoverytokens (
    email TEXT NOT NULL,
    token TEXT NOT NULL,
    expiration BIGINT NOT NULL
);

ALTER TABLE recoverytokens
ADD CONSTRAINT pk_recoverytokens PRIMARY KEY (email);
