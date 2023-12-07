# rockTheJVM-TypelevelRite

## Create A Postgres Table

1. Terminal
`> docker-compose up`
2. Terminal
`> docker exec -it rockthejvm-typelevelrite-db-1 psql -U docker`

```
docker=# create database doobie_demo;

docker=# \c doobie_demo

docker=# create table students(id serial not null, name character varying not null, primary key(id));

doobie_demo=# select * from students;
 id | name
----+------
(0 rows)
```
