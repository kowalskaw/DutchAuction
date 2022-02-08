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
			Thread[] bidders = new Thread[3];

			owners[0] = new Thread(new AuctionOwner(session, "chleb", "pszenny", 1, 0, 60,3, "Mirek", "node1"));
			owners[1] = new Thread(new AuctionOwner(session, "bułka", "kajzerka", 1, 0, 60,2, "Mariusz", "node2"));
			owners[2] = new Thread(new AuctionOwner(session, "pączek", "z lukrem", 1, 0, 60,4, "Jan", "node3"));

			bidders[0] = new Thread(new Bidder(session, "Krycha", "node1"));
			bidders[1] = new Thread(new Bidder(session, "Janusz", "node2"));
			bidders[2] = new Thread(new Bidder(session, "Zdzisław", "node3"));

			for (Thread owner : owners) {
				owner.start();
			}
			for (Thread bidder : bidders) {
				bidder.start();
			}
			for (Thread owner : owners) {
				owner.join();
			}
			for (Thread bidder : bidders) {
				bidder.join();
			}


		} catch (BackendException b) {
			System.out.println(b.getMessage());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		System.exit(0);
	}
}
