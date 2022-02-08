package dutchauction;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.TypeCodec;
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
import java.util.UUID;

public class AuctionOwner implements Runnable {
    private final BackendSession SESSION;
    private String productName;
    private String productDescription;
    private int priceDropFactor;
    private int epoch;
    private int epochPeriod;
    private int initialPrice;
    private int currentPrice;
    private String username;
    private String auctionId;
    private String winner;
    private Random random;

    public AuctionOwner(BackendSession SESSION,  String productName, String productDescription, int priceDropFactor, int epoch, int epochPeriod, int initialPrice, String username, String nodeId) {
        this.SESSION = SESSION;
        this.username = username;
        try {
            // login user
            this.random = new Random();
            this.SESSION.loginUser(username, nodeId);
            initNewAuction(UUID.randomUUID().toString(), productName,productDescription,priceDropFactor,epoch,epochPeriod,initialPrice);

        } catch (BackendException e) {
            e.printStackTrace();
        }
    }

    public void initNewAuction(String id, String productName, String productDescription, int priceDropFactor, int epoch, int epochPeriod, int initialPrice) {
        this.auctionId = id.replace("-","");
        this.productName = productName;
        this.productDescription = productDescription;
        this.priceDropFactor = priceDropFactor;
        this.epoch = epoch;
        this.epochPeriod = epochPeriod;
        this.currentPrice = initialPrice;
        this.initialPrice = initialPrice;
        initNewAuction();
    }

    public void initNewAuction(){
        this.winner = null;
        try {
            // initialize auction
            SESSION.initializeAuction(auctionId, productName, productDescription, priceDropFactor, epoch, epochPeriod, initialPrice, initialPrice, username);
        } catch (BackendException e) {
            e.printStackTrace();
        }
    }

    private void dropPrice(Row auction) throws BackendException {
        epoch++;
        this.currentPrice = auction.getInt("current_price");
        this.priceDropFactor = auction.getInt("price_drop_factor");
        int newPrice = this.currentPrice - this.priceDropFactor;
        if( newPrice > 0) {
            SESSION.initializeAuction(auctionId, productName, productDescription, priceDropFactor, epoch, epochPeriod, initialPrice, newPrice, username);
            this.currentPrice = newPrice;
        }
    }

    private void waitEpoch(){
        try {
            Thread.sleep(epochPeriod * 100L);
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
            if (wantedPrice >= auction.getInt("current_price")) {
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
        while(true) {
            auctionInfo();
            try {
                waitEpoch();
                Row auction = SESSION.getOneAuction(this.auctionId);
                if(checkWinner(auction) || (this.currentPrice - this.priceDropFactor < 0)){
                    System.out.printf("%s: auction at %d won by %s\n", this.username, this.currentPrice, this.winner);
                    initNewAuction(UUID.randomUUID().toString(),this.productName,productDescription,this.priceDropFactor, 0,this.epochPeriod, random.nextInt(20)+2);
                } else {
                    dropPrice(auction);
                }
            } catch (BackendException | ParseException e) {
                e.printStackTrace();
            }
        }
    }

}
