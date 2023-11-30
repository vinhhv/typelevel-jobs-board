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
  'vinh@rockthejvm.com',
  'rockthejvm',
  'vinh',
  'vu',
  'Rock the JVM',
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
  'joe@rockthejvm.com',
  'rockthejvm',
  'joe',
  'schmoe',
  'Rock the JVM',
  'RECRUITER'
);
