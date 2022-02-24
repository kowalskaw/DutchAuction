# DutchAuction

## About The Project
The purpose of this project is to present Cassandra capabilites in an unstable environment, where data consistency is important.
Performance test simulates multiple auction owners and multiple participants performing constant operation on tables at the same time,
with Cassandra running on 4 nodes.
Schema consists of 2 tables: User and Auction.

## Technical data
App developed with Java 17, Gradle and Cassandra 3.11.0
