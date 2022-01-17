CREATE KEYSPACE IF NOT EXISTS DutchAuction
  WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 3 };

USE DutchAuction;

CREATE TABLE LoggedUsers (
  username varchar,
  nodeId varchar,
  PRIMARY KEY(username)
);

CREATE TABLE Auction (
  id bigint,
  product_name varchar,
  product_description varchar,
  price_drop_fractor double,  // constant, determines price drop
  epoch bigint,     // number of epochs
  epoch_period int, // time that epoch lasts
  bid_price double, // at that price, user will automatically bid auction
  winner varchar,
  PRIMARY KEY(id)
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
