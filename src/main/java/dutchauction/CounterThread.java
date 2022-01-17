package dutchauction;

import dutchauction.backend.BackendException;
import dutchauction.backend.BackendSession;

import java.io.IOException;
import java.util.Properties;
import java.util.Random;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.ResultSet;

public class CounterThread implements Runnable {
    private static final String PROPERTIES_FILENAME = "config.properties";
    private final BackendSession SESSION;
    private final String URL;

    public CounterThread(BackendSession session, String url) {
        SESSION = session;
        URL = url;
    }

    @Override
    public void run() {
        try {
        	SESSION.truncatePageViewCounts();
            Random rnd = new Random();
            Row row;
            ResultSet rs;

            long startTime = System.currentTimeMillis();
            for (int i=0; i<40000; i++){
                SESSION.updatePageViewCounts(URL);
                rs = SESSION.getPageViewCounts(URL);
                row = rs.one();
                String url = row.getString("url"); //or rs.getString("column name");
                long counter = row.getLong("views");
                System.out.println("URL: " + url + " - COUNTER: " + counter);
            }

            long estimatedTime = System.currentTimeMillis() - startTime;
            System.out.println("Time of execution: " + estimatedTime);
        } catch (BackendException b) {
            System.out.println(b.getMessage());
        }
    }
}
