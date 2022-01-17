package dutchauction.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

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
	private static PreparedStatement INC_COUNTER;
	private static PreparedStatement SELECT_COUNTER;
	private static PreparedStatement DELETE_ALL_COUNTER;


	private static final String USER_FORMAT = "- %-10s  %-16s %-10s %-10s\n";
	// private static final SimpleDateFormat df = new
	// SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private void prepareStatements() throws BackendException {
		try {
			SELECT_USER = session.prepare("SELECT * FROM LoggedUsers WHERE username = ?;");
			INSERT_USER = session
					.prepare("INSERT INTO LoggedUsers (username, nodeId) VALUES (?, ?);");
			DELETE_USER = session.prepare("DELETE FROM LoggedUsers WHERE username = ?;");
			INC_COUNTER = session.prepare("UPDATE PageViewCounts SET views = views + 1 WHERE url=?");
			SELECT_COUNTER = session.prepare("SELECT * FROM PageViewCounts WHERE url = ?;");
			DELETE_ALL_COUNTER = session.prepare("TRUNCATE PageViewCounts;");
		} catch (Exception e) {
			throw new BackendException("Could not prepare statements. " + e.getMessage() + ".", e);
		}

		logger.info("Statements prepared");
	}

	public String getUserNode(String username) throws BackendException {
		BoundStatement bs = new BoundStatement(SELECT_USER);
		bs.bind(username);
		ResultSet rs = null;
		try {
			rs = session.execute(bs);
		} catch (Exception e) {
			throw new BackendException("Could not perform a query. " + e.getMessage() + ".", e);
		}

		Row user = rs.one();
		if (user != null) {
			return user.getString("nodeId");
		} else {
			return null;
		}
	}

	public ResultSet getPageViewCounts(String url) throws BackendException {
		BoundStatement bs = new BoundStatement(SELECT_COUNTER);
		bs.bind(url);
		ResultSet rs = null;
		try {
			rs = session.execute(bs);
		} catch (Exception e) {
			throw new BackendException("Could not perform a query. " + e.getMessage() + ".", e);
		}

		if (rs != null) {
			return rs;
		} else {
			return null;
		}
	}

	public void updatePageViewCounts(String url) throws BackendException {
		BoundStatement bs = new BoundStatement(INC_COUNTER);
		bs.bind(url);
		try {
			session.execute(bs);
		} catch (Exception e) {
			throw new BackendException("Could not perform an upsert. " + e.getMessage() + ".", e);
		}
	}
	
	public void truncatePageViewCounts() throws BackendException {
		BoundStatement bs = new BoundStatement(DELETE_ALL_COUNTER);
		try {
			session.execute(bs);
		} catch (Exception e) {
			throw new BackendException("Could not perform an upsert. " + e.getMessage() + ".", e);
		}
	}

	public void loginUser(String username, String nodeId) throws BackendException {
		BoundStatement bs = new BoundStatement(INSERT_USER);
		bs.bind(username, nodeId);

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
