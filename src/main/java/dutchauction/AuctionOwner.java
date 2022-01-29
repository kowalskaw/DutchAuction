package dutchauction;

import com.datastax.driver.core.Row;
import dutchauction.backend.BackendException;
import dutchauction.backend.BackendSession;

import java.beans.PropertyEditorSupport;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.Random;

public class AuctionOwner implements Runnable {
    private final BackendSession SESSION;
    private String productName;
    private String productDescription;
    private int priceDropFactor;
    private int epoch;
    private int epochPeriod;
    private int currentPrice;
    private String username;
    private String auctionId;
    private String winner;
    private Random random;

    public AuctionOwner(BackendSession SESSION, String id, String productName, String productDescription, int priceDropFactor, int epoch, int epochPeriod, int initialPrice, String username, String nodeId) {
        this.SESSION = SESSION;
        try {
            // login user
            SESSION.loginUser(username, nodeId);
            this.username = username;
            this.auctionId = id;
            this.productName = productName;
            this.productDescription = productDescription;
            this.priceDropFactor = priceDropFactor;
            this.epoch = epoch;
            this.epochPeriod = epochPeriod;
            this.currentPrice = initialPrice;
            this.winner = null;
            this.random = new Random();
        } catch (BackendException e) {
            e.printStackTrace();
        }
        try {
            // initialize auction
            SESSION.initializeAuction(id, productName, productDescription, priceDropFactor, epoch, epochPeriod, initialPrice, username);
        } catch (BackendException e) {
            e.printStackTrace();
        }
    }

    private void dropPrice(Row auction) throws BackendException {
        epoch++;
        int newPrice = auction.getInt("initial_price") - auction.getInt("price_drop_factor");
        if( newPrice > 0) {
            SESSION.initializeAuction(auctionId, productName, productDescription, priceDropFactor, epoch, epochPeriod, newPrice, username);
            this.currentPrice = newPrice;
        }
    }

    private void waitEpoch(){
        try {
            Thread.sleep(epoch * 1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean checkWinner(Row auction) throws ParseException, BackendException {
        Map<String, String> map;
        map = auction.getMap("bidders", String.class, String.class);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
        LocalDateTime winnerDate = null;

        for (Map.Entry<String, String> bidder : map.entrySet()) {
            String[] split = bidder.getValue().split(";");
            int wantedPrice = Integer.parseInt(split[0]);
            LocalDateTime parsedDate = LocalDateTime.parse(split[1].replace('T',' '), dateTimeFormatter);
            if (wantedPrice >= currentPrice) {
                if (winnerDate == null || parsedDate.isBefore(winnerDate)){
                    winnerDate = parsedDate;
                    this.winner = bidder.getKey();
                }
                if(winnerDate.isEqual(parsedDate)){
                    if (random.nextInt(2) == 0){
                        this.winner = bidder.getKey();
                    }
                }
            }
        }
        boolean result = this.winner != null;
        if (result) SESSION.updateAuctionWinner(this.winner,this.auctionId);
        return result;
    }

    private void auctionInfo(){
        System.out.printf("%s: auction is at %d\n", this.username, this.currentPrice);
    }

    @Override
    public void run() {
        while(this.winner == null) {
            auctionInfo();
            try {
                waitEpoch();
                Row auction = SESSION.getOneAuction(this.auctionId);
                if(!checkWinner(auction))
                    dropPrice(auction);
            } catch (BackendException | ParseException e) {
                e.printStackTrace();
            }
        }
        System.out.printf("%s: auction at %d won by %s\n", this.username, this.currentPrice, this.winner);

    }

}
