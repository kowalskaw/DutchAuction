package dutchauction;

import dutchauction.backend.BackendException;
import dutchauction.backend.BackendSession;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.ResultSet;

import java.time.LocalDateTime;
import java.util.*;

public class Bidder implements Runnable {
    private final BackendSession SESSION;
    private List<String> auctionIds; // auctions in which bidder is participating
    private final String username;
    private final Random rand;
    private ResultSet rs;

    public Bidder(BackendSession SESSION, String username, String nodeId) {
        this.SESSION = SESSION;
        this.auctionIds = new ArrayList<String>(2){};
        this.username = username;
        this.rand = new Random();
        this.rs = null;
        try {
            // login user
            SESSION.loginUser(username, nodeId);
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
        int boundary = 1;
        try {
            Row auction = SESSION.getOneAuction(id);
            boundary = auction.getInt("initial_price");

            System.out.printf("<%s> Participating in auction:\n", username);
            System.out.printf("<%s> Id %s:\n",username, auction.getString("id"));
            System.out.printf("<%s> Product name %s:\n",username, auction.getString("product_name"));
            System.out.printf("<%s> Product initial price %d:\n",username, auction.getInt("initial_price"));
            System.out.printf("<%s> Product owner %s:\n",username, auction.getString("owner"));
            System.out.println("-------------");
            int price = 1;
            if(boundary>1){
                price = rand.nextInt(boundary-1)+1;
            }
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
            SESSION.deleteOneAuction(id,false);
            return true;
        }
        if (row.getString("winner")==null){
            System.out.printf("<%s> Waiting... Current price %d and bidders:\n",username, row.getInt("current_price"));
            System.out.println(row.getMap("bidders",String.class, String.class));
            System.out.println("-------------");
            return false;
        }
        else if(row.getString("winner").equals(username)){
            System.out.printf("<%s> You won the product!\n",username);
            System.out.printf("<%s> Id %s:\n",username, row.getString("id"));
            System.out.printf("<%s> Product name %s:\n",username, row.getString("product_name"));
            System.out.printf("<%s> Product description %s:\n",username, row.getString("product_description"));
            System.out.printf("<%s> Product initial price %d:\n",username, row.getInt("initial_price"));
            System.out.printf("<%s> Product owner %s:\n",username, row.getString("owner"));
            System.out.println("-------------");
            return true;
        }
        else {
            System.out.printf("<%s> You lose. The winner is %s and he bought %s\n",username, row.getString("winner"), row.getString("product_name"));
            System.out.println("-------------");
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
        this.auctionIds = new ArrayList<String>(2){};
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
