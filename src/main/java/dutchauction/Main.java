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

			Thread[] owners = new Thread[3];
			Thread[] bidders = new Thread[1];

			owners[0] = new Thread(new AuctionOwner(session, "1", "chleb", "pszenny", 1, 0, 60,3, "Mirek", "node1"));
			owners[1] = new Thread(new AuctionOwner(session, "2", "bułka", "kajzerka", 1, 0, 60,2, "Mariusz", "node2"));
			owners[2] = new Thread(new AuctionOwner(session, "3", "pączek", "z lukrem", 1, 0, 60,4, "Jan", "node3"));

			bidders[0] = new Thread(new Bidder(session, "Krycha", "node2"));

			for (Integer i=0; i<3; i++) {
				//threads[i] = new Thread(new WorkThread(session, nodeIds[i]));
				String finalUrl = i.toString();
				owners[i].start();
				owners[i].join();
			}
			bidders[0].start();
			bidders[0].join();

		} catch (BackendException b) {
			System.out.println(b.getMessage());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		System.exit(0);
	}
}
