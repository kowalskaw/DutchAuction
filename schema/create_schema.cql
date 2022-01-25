CREATE KEYSPACE IF NOT EXISTS DutchAuction
  WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 3 };

USE DutchAuction;

CREATE TABLE Users (
  username varchar,
  nodeId varchar,
  PRIMARY KEY(username)
);

//CREATE TYPE bidder (
//username varchar,
//price double,
//timestamp timestamp
//);

CREATE TABLE Auction (
  id varchar,
  product_name varchar,
  product_description varchar,
  price_drop_factor int,  // constant, determines price drop
  epoch int,     // number of epochs
  epoch_period int, // time that epoch lasts
  initial_price int, // at that price
  winner varchar,
  bidders map<varchar, varchar>, // map of bidders -> {"username" : "proposed_price;timestamp"}
  owner varchar, // creator of the auction
  PRIMARY KEY(id)
);
