package dutchauction;

import dutchauction.backend.BackendException;
import dutchauction.backend.BackendSession;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.ResultSet;

import java.math.BigInteger;
import java.util.Random;

public class Bidder implements Runnable {
    private final BackendSession SESSION;
    private final BigInteger[] auctionId; // auctions in which bidder is participating

    public Bidder(BackendSession SESSION, String username, String nodeId) {
        this.SESSION = SESSION;
        this.auctionId = new BigInteger[]{};
        try {
            // login user
            SESSION.loginUser(username, nodeId);
        } catch (BackendException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        // pobrac akucje
        // wylosowac w jakiej bierzemy udzial
        // zapisac ich id do listy
        // update zrobic na akucji, gdzie wstawiamy username, cene, timestamp
        // cyklicznie sprawdzamy, czy w polu winner jest nasz username
        // jesli tak to wygralismy
        // jesli nie to przegralismy
        //aukcja zakonczona koniec watku

        Row row = null;
        ResultSet rs = null;
        try {
            rs = SESSION.getAllAuctions();
        } catch (BackendException e) {
            e.printStackTrace();
        }

        row = rs.one();
        String productName = row.getString("product_name"); //or rs.getString("column name");
        System.out.println("Product name: " + productName);

//        Random rnd = new Random();
//        Row row;
//        ResultSet rs = null;
//        for (int i=0; i<40000; i++){
//            try {
//                SESSION.updatePageViewCounts(URL);
//            } catch (BackendException e) {
//                e.printStackTrace();
//            }
//            try {
//                rs = SESSION.getPageViewCounts(URL);
//            } catch (BackendException e) {
//                e.printStackTrace();
//            }
//            row = rs.one();
//            String url = row.getString("url"); //or rs.getString("column name");
//            long counter = row.getLong("views");
//            System.out.println("URL: " + url + " - COUNTER: " + counter);
//        }
    }
}
