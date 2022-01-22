package dutchauction;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import dutchauction.backend.BackendException;
import dutchauction.backend.BackendSession;

public class Main {

	private static final String PROPERTIES_FILENAME = "config.properties";

	public static void main(String[] args) throws IOException, BackendException {
		String contactPoint = null;
		String keyspace = null;

		Properties properties = new Properties();
		try {
			properties.load(Main.class.getClassLoader().getResourceAsStream(PROPERTIES_FILENAME));

			contactPoint = properties.getProperty("contact_point");
			keyspace = properties.getProperty("keyspace");
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		try {
			BackendSession session = new BackendSession(contactPoint, keyspace);
//			String[] availUsernames = {"Adam", "Ola", "Ewa", "Kasia"};
//			for (String username: availUsernames) {
//				session.logoutUser(username);
//			}
//			String[] nodeIds = {"node1", "node2", "node3"};
//			Thread[] threads = new Thread[3];
//
//			for (Integer i=0; i<3; i++) {
//				//threads[i] = new Thread(new WorkThread(session, nodeIds[i]));
//				String finalUrl = i.toString();
//				threads[i] = new Thread(new CounterThread(session, finalUrl));
//				threads[i].start();
//			}

			Thread[] owners = new Thread[1];
			Thread[] bidders = new Thread[1];

			owners[0] = new Thread(new AuctionOwner(session, "1", "chleb", "pszenny", 1.0, 0, 60,3.0, "zdzichu", "node1"));
			bidders[0] = new Thread(new Bidder(session, "krycha", "node2"));




			try {
				owners[0].start();
				owners[0].join();
				bidders[0].start();
				bidders[0].join();
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
//			try {
//				for (int i = 0; i < 3; i++) {
//					threads[i].join();
//				}
//			} catch (Exception e) {
//				System.out.println(e.getMessage());
//			}
		} catch (BackendException b) {
			System.out.println(b.getMessage());
		}


		System.exit(0);
	}
}
