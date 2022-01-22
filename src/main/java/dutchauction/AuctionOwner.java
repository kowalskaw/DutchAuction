package dutchauction;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import dutchauction.backend.BackendException;
import dutchauction.backend.BackendSession;

import java.util.Random;

public class AuctionOwner implements Runnable {
    private final BackendSession SESSION;
    private final String URL;

    public AuctionOwner(BackendSession SESSION, String URL) {
        this.SESSION = SESSION;
        this.URL = URL;
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

        try {
            SESSION.truncatePageViewCounts();
        } catch (BackendException e) {
            e.printStackTrace();
        }
        Random rnd = new Random();
        Row row;
        ResultSet rs = null;
        for (int i=0; i<40000; i++){
            try {
                SESSION.updatePageViewCounts(URL);
            } catch (BackendException e) {
                e.printStackTrace();
            }
            try {
                rs = SESSION.getPageViewCounts(URL);
            } catch (BackendException e) {
                e.printStackTrace();
            }
            row = rs.one();
            String url = row.getString("url"); //or rs.getString("column name");
            long counter = row.getLong("views");
            System.out.println("URL: " + url + " - COUNTER: " + counter);
        }
    }

}
