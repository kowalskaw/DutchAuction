package dutchauction;

import dutchauction.backend.BackendException;
import dutchauction.backend.BackendSession;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

public class Bidder implements Runnable {
    private final BackendSession SESSION;
    private List<String> auctionIds; // auctions in which bidder is participating
    private final String username;
    private final Random rand;
    private ResultSet rs;
    private static final Logger logger = LoggerFactory.getLogger(Bidder.class);

    public Bidder(BackendSession SESSION, String username) {
        this.SESSION = SESSION;
        this.auctionIds = new ArrayList<>(2) {
        };
        this.username = username;
        this.rand = new Random();
        this.rs = null;
        try {
            // login user
            SESSION.loginUser(username);
        } catch (BackendException e) {
            e.printStackTrace();
        }
    }

    private void getRandomIndexFromList(List<Row> rows){
        Integer index = rand.nextInt(rows.size());
        auctionIds.add(rows.get(index).getString("id"));
        rows.remove(index);
    }

    private void randomizeAuctions(){
        try {
            rs = SESSION.getAllRunningAuctions();
        } catch (BackendException e) {
            e.printStackTrace();
        }

        List<Row> rows = rs.all();
        //2 auction id's
        getRandomIndexFromList(rows);
        getRandomIndexFromList(rows);

    }

    private void participateInAuction(String id){
        int boundary;
        try {
            Row auction = SESSION.getOneAuction(id);
            boundary = auction.getInt("initial_price");
            logger.info("<{}> Participating in auction: { id={}, owner={}, name={}, description={}," +
                            " initial_price={}, current_price={} }", username, auction.getString("id"),
                    auction.getString("owner"), auction.getString("product_name"),
                    auction.getString("product_description"), auction.getInt("initial_price"),
                    auction.getInt("current_price"));
            int price = 1;
            if(boundary>1){
                price = rand.nextInt(boundary-1)+1;
            }
            logger.info("<{}> Proposing price {} for product {} in auction {} whose owner is {}", username, price,
                    auction.getString("product_name"), auction.getString("id"),
                    auction.getString("owner"));
            LocalDateTime timestamp = LocalDateTime.now();
            SESSION.updateAuctionBidder(username, price, timestamp, id);
        } catch (BackendException e) {
            e.printStackTrace();
        }
    }

    private boolean checkResult(String id, boolean finished) throws BackendException {
        Row row = null;
        try {
            row = SESSION.getOneAuction(id, finished);
        } catch (BackendException e) {
            e.printStackTrace();
        }
        if(row == null) {
            return true;
        }
        if (row.getString("owner")==null) {
            logger.debug("Tombstone detected in auction with id={}. Now removing it", row.getString("id"));
            SESSION.deleteOneAuction(id,false);
            return true;
        }
        if (row.getString("winner")==null){
            Map<String, String> bidders = row.getMap("bidders",String.class, String.class);
            logger.info("<{}> Waiting... Current info on auction: { id={}, current_price={}, bidders={} }",
                    username, row.getString("id"), row.getInt("current_price"), bidders.toString());
            return false;
        }
        else if(row.getString("winner").equals(username)){
            logger.info("<{}> I won an auction {} with product {} {} from {}", username, row.getString("id"),
                    row.getString("product_name"), row.getString("product_description"),
                    row.getString("owner"));
            return true;
        }
        else {
            logger.info("<{}> I lost. The winner is {} and he won the auction for {} with id {}",
                    username, row.getString("winner"), row.getString("product_name"),
                    row.getString("id"));
            return true;
        }
    }

    private void checkResultsPeriodically(String id) throws BackendException, InterruptedException {
        boolean finished = false;
        Row row = SESSION.getOneAuction(id);
        int epoch = 0;
        if (row.getInt("epoch_period")!=0) {
            epoch = row.getInt("epoch_period");
        }
        while(!finished){
            System.out.println(id);
            Thread.sleep(epoch * 100L);
            finished = checkResult(id, finished);
            if (finished)
                finished = checkResult(id, finished);
        }
        this.auctionIds = new ArrayList<>(2) {
        };
    }

    @Override
    public void run() {
        while(true) {
            randomizeAuctions();
            String id = auctionIds.get(rand.nextInt(auctionIds.size()));
            participateInAuction(id);
            try {
                // check on results every 1 minute
                checkResultsPeriodically(id);
            } catch (BackendException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
