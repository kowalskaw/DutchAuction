package dutchauction.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import java.time.LocalDateTime;
import java.util.*;

/*
 * For error handling done right see: 
 * https://www.datastax.com/dev/blog/cassandra-error-handling-done-right
 * 
 * Performing stress tests often results in numerous WriteTimeoutExceptions, 
 * ReadTimeoutExceptions (thrown by Cassandra replicas) and 
 * OpetationTimedOutExceptions (thrown by the client). Remember to retry
 * failed operations until success (it can be done through the RetryPolicy mechanism:
 * https://stackoverflow.com/questions/30329956/cassandra-datastax-driver-retry-policy )
 */

public class BackendSession {

	private static final Logger logger = LoggerFactory.getLogger(BackendSession.class);

	private Session session;

	public BackendSession(String contactPoint, String keyspace) throws BackendException {

		Cluster cluster = Cluster.builder().addContactPoint(contactPoint).build();
		try {
			session = cluster.connect(keyspace);
		} catch (Exception e) {
			throw new BackendException("Could not connect to the cluster. " + e.getMessage() + ".", e);
		}
		prepareStatements();
	}

	private static PreparedStatement SELECT_USER;
	private static PreparedStatement INSERT_USER;
	private static PreparedStatement DELETE_USER;
	private static PreparedStatement DELETE_AUCTION;
	private static PreparedStatement SELECT_AUCTION;
	private static PreparedStatement SELECT_ALL_AUCTION;
	private static PreparedStatement SELECT_ALL_RUNNING_AUCTION;
	private static PreparedStatement INSERT_AUCTION; //init Auction
	private static PreparedStatement INSERT_WON_AUCTION;
	private static PreparedStatement BIDDER_UPDATE_AUCTION;
	private static PreparedStatement OWNER_UPDATE_AUCTION;


	private static final String USER_FORMAT = "- %-10s  %-16s %-10s %-10s\n";
	// private static final SimpleDateFormat df = new
	// SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private void prepareStatements() throws BackendException {
		try {
			SELECT_USER = session.prepare("SELECT * FROM Users WHERE username = ?;");
			INSERT_USER = session
					.prepare("INSERT INTO Users (username) VALUES (?);");
			DELETE_USER = session.prepare("DELETE FROM Users WHERE username = ?;");
			SELECT_AUCTION = session.prepare("SELECT * FROM Auction WHERE finished = ? AND id = ?;");
			SELECT_ALL_AUCTION = session.prepare("SELECT * FROM Auction;");

			SELECT_ALL_RUNNING_AUCTION = session.prepare("SELECT * FROM Auction WHERE finished = false;");
			DELETE_AUCTION =
					session.prepare("DELETE FROM Auction WHERE finished = ? AND id = ?;");
			INSERT_AUCTION = session
					.prepare("INSERT INTO Auction (finished, id, product_name, product_description, price_drop_factor, epoch, epoch_period, initial_price, current_price, owner) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
			INSERT_WON_AUCTION = session
					.prepare("INSERT INTO Auction (finished, id, bidders, product_name, product_description, price_drop_factor, epoch, epoch_period, initial_price, current_price, owner, winner)" +
							" VALUES (true, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
			BIDDER_UPDATE_AUCTION = session.prepare("UPDATE Auction SET bidders = bidders + ? WHERE finished = false AND id = ?;");
			OWNER_UPDATE_AUCTION = session.prepare("UPDATE Auction SET winner = ? WHERE finished = false AND id = ?;");
		} catch (Exception e) {
			throw new BackendException("Could not prepare statements. " + e.getMessage() + ".", e);
		}
		logger.info("Statements prepared");
	}

	public void loginUser(String username) throws BackendException {
		BoundStatement bs = new BoundStatement(INSERT_USER);
		bs.bind(username);

		try {
			session.execute(bs);
		} catch (Exception e) {
			throw new BackendException("Could not perform an upsert. " + e.getMessage() + ".", e);
		}

//		logger.info("User " + username + " logged in on node " + nodeId);
	}

	public void logoutUser(String username) throws BackendException {
		BoundStatement bs = new BoundStatement(DELETE_USER);
		bs.bind(username);

		try {
			session.execute(bs);
		} catch (Exception e) {
			throw new BackendException("Could not perform a delete operation. " + e.getMessage() + ".", e);
		}

//		logger.info("User " + username + " released node");
	}

	public void initializeAuction(String id, String productName, String productDescription, int priceDropFactor, int epoch, int epochPeriod, int initialPrice, int currentPrice, String owner) throws BackendException{
		BoundStatement bs = new BoundStatement(INSERT_AUCTION);
		bs.bind(false, id, productName, productDescription, priceDropFactor, epoch, epochPeriod, initialPrice, currentPrice, owner);
		try {
			session.execute(bs);
		} catch (Exception e) {
			throw new BackendException("Could not perform an insert auction operation. " + e.getMessage() + ".", e);
		}
	}

	public Row getOneAuction(String id, boolean finished) throws BackendException {
		BoundStatement bs = new BoundStatement(SELECT_AUCTION);
		bs.bind(finished, id);
		ResultSet rs;
		try {
			rs = session.execute(bs);
		} catch (Exception e) {
			throw new BackendException("Could not perform a query. " + e.getMessage() + ".", e);
		}
		return rs.one();
	}

	public void deleteOneAuction(String id, boolean finished) throws BackendException {
		BoundStatement bs = new BoundStatement(DELETE_AUCTION);
		bs.bind(finished, id);
		try {
			session.execute(bs);
		} catch (Exception e) {
			throw new BackendException("Could not perform a query. " + e.getMessage() + ".", e);
		}
	}

	public Row getOneAuction(String id) throws BackendException {
		return getOneAuction(id, false);
	}
	public ResultSet getAllAuctions() throws BackendException {
		BoundStatement bs = new BoundStatement(SELECT_ALL_AUCTION);
		ResultSet rs;
		try {
			rs = session.execute(bs);
		} catch (Exception e) {
			throw new BackendException("Could not perform a select all auctions operation. " + e.getMessage() + ".", e);
		}
		return rs;
	}

	public ResultSet getAllRunningAuctions() throws BackendException {
		BoundStatement bs = new BoundStatement(SELECT_ALL_RUNNING_AUCTION);
		ResultSet rs;
		try {
			rs = session.execute(bs);
		} catch (Exception e) {
			throw new BackendException("Could not perform a select all running auctions operation. " + e.getMessage() + ".", e);
		}
		return rs;
	}

	public void updateAuctionBidder(String username, int price, LocalDateTime timestamp, String auctionId) throws BackendException {
		BoundStatement bsUpdate = new BoundStatement(BIDDER_UPDATE_AUCTION);
		String value = price+";"+timestamp;
		Map<String, String> bidders = new HashMap<>();
		bidders.put(username,value);
		bsUpdate.bind(bidders, auctionId);
		try {
			session.execute(bsUpdate);
		} catch (Exception e) {
			throw new BackendException("Could not perform an update. " + e.getMessage() + ".", e);
		}
	}

	public void updateAuctionWinner(String username, String auctionId) throws BackendException {
		Row auction = this.getOneAuction(auctionId, false);

		BoundStatement bs = new BoundStatement(INSERT_WON_AUCTION);
		bs.bind(auctionId, auction.getMap("bidders",String.class,String.class),
				auction.getString("product_name"), auction.getString("product_description"),
				auction.getInt("price_drop_factor"), auction.getInt("epoch"), auction.getInt("epoch_period"),
				auction.getInt("initial_price"), auction.getInt("current_price"),
				auction.getString("owner"), username);

		BoundStatement deleteBS = new BoundStatement(DELETE_AUCTION);
		deleteBS.bind(false, auctionId);
		try {
			session.execute(bs);
			session.execute(deleteBS);
		} catch (Exception e) {
			throw new BackendException("Could not perform an update. " + e.getMessage() + ".", e);
		}
	}

	protected void finalize() {
		try {
			if (session != null) {
				session.getCluster().close();
			}
		} catch (Exception e) {
			logger.error("Could not close existing cluster", e);
		}
	}

}
