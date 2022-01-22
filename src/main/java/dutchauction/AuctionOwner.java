package dutchauction;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import dutchauction.backend.BackendException;
import dutchauction.backend.BackendSession;

import java.math.BigInteger;
import java.util.Random;

public class AuctionOwner implements Runnable {
    private final BackendSession SESSION;
    private String productName;
    private String productDescription;
    private double priceDropFactor;
    private int epoch;
    private int epochPeriod;
    private double currentPrice;
    private String username;

    public AuctionOwner(BackendSession SESSION, String id, String productName, String productDescription, double priceDropFactor, int epoch, int epochPeriod, double currentPrice, String username, String nodeId) {
        this.SESSION = SESSION;
        try {
            // login user
            SESSION.loginUser(username, nodeId);
        } catch (BackendException e) {
            e.printStackTrace();
        }
        try {
            // initialize auction
            SESSION.initializeAuction(id, productName, productDescription, priceDropFactor, epoch, epochPeriod, currentPrice, username);
        } catch (BackendException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        // moze miec jedna aukcje na raz
        // w konstruktorze mozna podac parametry aukcji
        // tworzy aukcje
        // co okres T sprawdza czy biddersi sie kwalifikuja do wygrania aukcji
        // wybiera tego o najwczesniejszym timestampie
        // jesli timestampy sie pokrywaja u kilku, to losuje jednego z nich
        // ustawia w polu winner username zwycieskiego biddera
        // konczy aukcje

        //TODO
        System.out.println("Auction owner running");
//
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
