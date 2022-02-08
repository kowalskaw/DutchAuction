CREATE KEYSPACE IF NOT EXISTS DutchAuction
  WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 3 };

USE DutchAuction;

CREATE TABLE Users (
  username varchar,
  PRIMARY KEY(username)
);

CREATE TABLE IF NOT EXISTS Auction (
  finished boolean,
  id varchar,
  product_name varchar,
  product_description varchar,
  price_drop_factor int,  // constant, determines price drop
  epoch int,     // number of epochs
  epoch_period int, // time that epoch lasts
  initial_price int,
  current_price int,
  winner varchar,
  bidders map<varchar, varchar>, // map of bidders -> {"username" : "proposed_price;timestamp"}
  owner varchar, // creator of the auction
  PRIMARY KEY(finished, id)
);