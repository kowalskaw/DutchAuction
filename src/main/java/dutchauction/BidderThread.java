package dutchauction;

import dutchauction.backend.BackendException;
import dutchauction.backend.BackendSession;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.ResultSet;

import java.util.Random;

public class BidderThread implements Runnable {
    private final BackendSession SESSION;
    private final String URL;

    public BidderThread(BackendSession SESSION, String URL) {
        this.SESSION = SESSION;
        this.URL = URL;
    }

    @Override
    public void run() {
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
