# DutchAuction

## About The Project
The purpose of this project is to present Cassandra capabilites in an unstable environment, where data consistency is important.
Performance test simulates multiple auction owners and multiple participants performing constant operation on tables at the same time,
with Cassandra running on 4 nodes.
Schema consists of 2 tables: User and Auction.

## Technical data
App developed with Java 17, Gradle and Cassandra 3.11.0


## Scheme
Table "auction" stores data about auctions in progress and archived. It contains following fields:
- finished - flag if auction is ended
- id - identifier of auction
- product_name - product that is on sale
- product_description - description of auctioned product
- price_drop_factor - amount of money subtracted each period
- epoch - number of time intervals passed
- epoch_period - time interval before decreasing current_price
- initial_price - price that was at the start of auction
- current_price - actual price after drops of initial price
- winner - user who won auction
- bidders - map of users thresholds
- owner - user that create auction

finished and id are primary key of the table

User:
- username - username that are "logged" into system

## Anomalies

- Data races 
  - if the user tries to put his threshold for auction during checking by owner, it can be not seen by owner

- Weaker consistency levels than "ALL" - the change done by user/owner can be not seen because won't be propagated to all nodes
  - Quorum - the change was on the node not in quorum group
  - One and Any - client have connected to node without change
  - Two partitions were changed by two different bidders  

- Tombstones - client can have archived auction (it means auction deleted and inserted with flag finished = true) and insert it again into database

## How does it work

We have processes of owners, that maintain their auctions, and bidders, that put their threshold and watch auctions.


Owner waits for epoch_period and after it checks if there's any user that has threshold at current_price.
If it finds, owner deletes old auction and adds it as finished, it has to be done that way because we use flag finished to filter by status of auction, therefore it is in primary key of a table
If it doesn't find any user at given price, owner drops it by price_drop_factor and updates in database.
After ended auction, owner begins new one with random properties.

Bidder if it's not participating in any auction, retrieves all non-finished auctions
and randomly choose one to participate, then randomly set up threshold/bid - the maximal price that user is able to pay 

The winning bidder is the one with the highest threshold and when we detect the other,
we flip "virtual coin" to choose which one wins at conflict.

We let for mentioned anomalies because if we have enough bidders,
they will set some range of thresholds and losing some data about them,
even the highest ones, it won't cause a big financial lost because we have some variety.

If there'll be a lost of auction, it also won't be a big problem as we have many of them.

There'll be some cases of disappointed customers.


## How to setup one Cassandra node on docker:

Start Docker daemon (docker desktop)

Terminal ->
```
docker run --rm -d -v ~/directory_location/DutchAuction/schema:/var/lib/cassandra -p 9042:9042 --name cassandra --hostname cassandra --network dutchauction-network cassandra:3.11
```

To build Cassandra schema, enter the Docker container and enter:

* to drop schema
```
cqlsh -f /var/lib/cassandra/drop_schema.cql
```

* to create schema
```
cqlsh -f /var/lib/cassandra/create_schema.cql
```

# How to run project

Enter project directory and run in terminal
```
gradle build
```
Then run Main.java file

Remember to modify ~/DutchAuction/src/main/resources/config.properties file if you decide to make changes in Cassandra setup
