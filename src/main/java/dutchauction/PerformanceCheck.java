package dutchauction;

import dutchauction.backend.BackendException;
import dutchauction.backend.BackendSession;

import java.io.IOException;
import java.util.Properties;
import java.util.Random;

public class PerformanceCheck {

    private static final String PROPERTIES_FILENAME = "config.properties";
    private Thread[] owners, bidders;
    BackendSession session;

    public PerformanceCheck() {
        String contactPoint = null;
        String keyspace = null;
        int port = 9042;
        Properties properties = new Properties();
        try {
            properties.load(Main.class.getClassLoader().getResourceAsStream(PROPERTIES_FILENAME));
            contactPoint = properties.getProperty("contact_point");
            keyspace = properties.getProperty("keyspace");
            port = Integer.parseInt(properties.getProperty("port"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        try {
            session = new BackendSession(contactPoint, keyspace, port);
            owners = new Thread[50];
            bidders = new Thread[100];

        } catch (BackendException b) {
            System.out.println(b.getMessage());
        }
    }

    private void biddersOwnersSetup(){
        String username, productName, productDescription;
        int epoch=0, epochPeriod=60, initialPrice,priceDropFactor;
        Random rand = new Random();
        for (int i=0; i<owners.length; i++){
            username = "Owner" + i;
            productName = "Product" + i;
            productDescription = productName + " description.";
            initialPrice = rand.nextInt(1000);
            priceDropFactor = 1;
            owners[i] = new Thread(new AuctionOwner(session, productName, productDescription, priceDropFactor,
                    epoch, epochPeriod,initialPrice, username));
        }
        for (int i=0; i<bidders.length; i++){
            username = "Bidder" + i;
            bidders[i] = new Thread(new Bidder(session, username));
        }
    }

    public void run() throws InterruptedException {
        biddersOwnersSetup();
        // start threads
        for (Thread owner : owners) {
            owner.start();
        }
        for (Thread bidder : bidders) {
            bidder.start();
        }
        // join threads
        for (Thread owner : owners) {
            owner.join();
        }
        for (Thread bidder : bidders) {
            bidder.join();
        }
    }
}
