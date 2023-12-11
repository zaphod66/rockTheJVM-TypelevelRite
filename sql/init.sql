CREATE DATABASE board;
\c board

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
