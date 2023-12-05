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

INSERT INTO jobs(
    id,
    date,
    ownerEmail,
    company,
    title,
    description,
    externalUrl,
    remote,
    location,
    salaryLo,
    salaryHi,
    currency,
    country,
    tags,
    image,
    seniority,
    other,
    active
) VALUES (
    '843df718-ec6e-4d49-9289-f799c0f40064', -- id
    1659186086, -- date
    'vinh@rockthejvm.com', -- ownerEmail
    'Awesome Company', -- company
    'Tech Lead', -- title
    'An awesome job in Berlin', -- description
    'https://rockthejvm.com/awesomejob', -- externalUrl
    false, -- remote
    'Berlin', -- location
    2000, -- salaryLo
    3000, -- salaryHi
    'EUR', -- currency
    'Germany', -- country
    ARRAY ['scala', 'scala-3', 'cats'], -- tags
    NULL, -- image
    'Senior', -- seniority
    NULL, -- other
    false -- active
)
