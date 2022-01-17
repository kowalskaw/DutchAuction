CREATE KEYSPACE IF NOT EXISTS DutchAuction
  WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 3 };

USE DutchAuction;

CREATE TABLE LoggedUsers (
  username varchar,
  nodeId varchar,
  PRIMARY KEY(username)
);



CREATE TABLE Zad4 (
  id int,
  col1 bigint,
  col2 bigint, 
  PRIMARY KEY(id)
);

CREATE TABLE PageViewCounts(
  url text PRIMARY KEY,
  views counter
);
