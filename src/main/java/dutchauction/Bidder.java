package dutchauction;

import dutchauction.backend.BackendException;
import dutchauction.backend.BackendSession;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.ResultSet;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;

public class Bidder implements Runnable {
    private final BackendSession SESSION;
    private final List<String> auctionId; // auctions in which bidder is participating
    private final String username;

    public Bidder(BackendSession SESSION, String username, String nodeId) {
        this.SESSION = SESSION;
        this.auctionId = new ArrayList<String>(2){};
        this.username = username;
        try {
            // login user
            SESSION.loginUser(username, nodeId);
        } catch (BackendException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        // pobrac wszystkie akucje
        // wylosowac 2 w jakich bierze udzial
        // zapisac id aukcji do listy
        // wykonac update na akucji, gdzie wstawiamy username, cene, timestamp
        // cyklicznie sprawdzamy, czy w polu winner jest nasz username
        // jesli tak to wygralismy
        // jesli nie to przegralismy
        // aukcja zakonczona koniec watku

        ResultSet rs = null;
        try {
            rs = SESSION.getAllAuctions();
        } catch (BackendException e) {
            e.printStackTrace();
        }

        List<Row> rows = rs.all();

        Random rand = new Random();
        Integer firstAuctionIndex = rand.nextInt(rows.size());

        this.auctionId.add(rows.get(firstAuctionIndex).getString("id"));
        rows.remove(firstAuctionIndex);

        Integer secondAuctionIndex = rand.nextInt(rows.size());
        this.auctionId.add(rows.get(secondAuctionIndex).getString("id"));

        int boundary = 0;
        for (Row row : rows) {
            if(Integer.parseInt(this.auctionId.get(0))==Integer.parseInt(row.getString("id"))) {
                boundary = row.getInt("initial_price");
            }
            System.out.println(row.getString("id"));
            System.out.println(row.getString("product_name"));
            System.out.println(row.getString("owner"));
            System.out.println("-------------");
        }
        int price = rand.nextInt(boundary+1)+1;
        LocalDateTime timestamp = LocalDateTime.now();
        try {
            SESSION.updateAuctionBidder(this.username, price, timestamp, this.auctionId.get(0));
        } catch (BackendException e) {
            e.printStackTrace();
        }
        try {
            rs = SESSION.getAllAuctions();
        } catch (BackendException e) {
            e.printStackTrace();
        }
        List<Row> modified = rs.all();
        for (Row row : modified) {

            System.out.println(row.getString("id"));
            System.out.println(row.getString("product_name"));
            System.out.println(row.getString("owner"));
            System.out.println(row.getMap("bidders",String.class, String.class));
            System.out.println("-------------");
        }
    }
}
