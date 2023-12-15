CREATE TABLE jobs(
  id UUID DEFAULT gen_random_uuid(),
  date BIGINT NOT NULL,
  ownerEmail TEXT NOT NULL,
  company TEXT NOT NULL,
  title TEXT NOT NULL,
  description TEXT NOT NULL,
  externalURL TEXT NOT NULL,
  remote BOOLEAN NOT NULL DEFAULT false,
  location TEXT,
  salaryLo INTEGER,
  salaryHi INTEGER,
  currency TEXT,
  country TEXT,
  tags TEXT[],
  image TEXT,
  seniority TEXT,
  other TEXT,
  active BOOLEAN NOT NULL DEFAULT false
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
  externalURL,
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
  'me@home.com', -- ownerEmail
  'Awesome Company', -- company
  'Tech Lead', -- title
  'An awesome job in Berlin', -- description
  'https://home.com/awesomejob', -- externalURL
  false, -- remote
  'Berlin', -- location
  2000, -- salaryLo
  3000, -- salaryHi
  'EUR', -- currency
  'Germany', -- country
  ARRAY [ 'scala', 'scala-3', 'cats' ], -- tags
  NULL, -- image
  'Senior', -- seniority
  NULL, -- other
  false -- active
);
