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
  '$2a$10$q1knuC.xPbjT6w/XuBHei.S3WJxS3zF3FE89Ha2xVbb1YY.kkkKb.',
  'Vinh',
  'Vu',
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
  '$2a$10$q1knuC.xPbjT6w/XuBHei.S3WJxS3zF3FE89Ha2xVbb1YY.kkkKb.',
  'Joe',
  'Schmoe',
  'Rock the JVM',
  'RECRUITER'
);
