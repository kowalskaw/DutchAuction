# DutchAuction

## About The Project
The purpose of this project is to present Cassandra capabilites in an unstable environment, where data consistency is important.
Performance test simulates multiple auction owners and multiple participants performing constant operation on tables at the same time,
with Cassandra running on 4 nodes.
Schema consists of 2 tables: User and Auction.

## Technical data
App developed with Java 17, Gradle and Cassandra 3.11.0

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
