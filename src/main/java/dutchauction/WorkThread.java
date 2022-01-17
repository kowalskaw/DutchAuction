package dutchauction;

import dutchauction.backend.BackendException;
import dutchauction.backend.BackendSession;

import java.io.IOException;
import java.util.Properties;
import java.util.Random;

public class WorkThread implements Runnable {
    private static final String PROPERTIES_FILENAME = "config.properties";
    private final BackendSession SESSION;
    private final String MY_NODE_ID;

    public WorkThread(BackendSession session, String nodeId) {
        SESSION = session;
        MY_NODE_ID = nodeId;
    }

    @Override
    public void run() {
        try {
            Random rnd = new Random();
            String[] availUsernames = {"Adam", "Ola", "Ewa", "Kasia"};
            String username, nodeId;

            boolean runFlag = true;
            while (runFlag) {
                username = availUsernames[rnd.nextInt(4)];
                nodeId = SESSION.getUserNode(username);
                if (nodeId != null) {
//                    System.out.println(MY_NODE_ID + ": Username " + username + " already taken by " + nodeId);
                    continue;
                } else {
                    SESSION.loginUser(username, MY_NODE_ID);
                    nodeId = SESSION.getUserNode(username);
                    if (MY_NODE_ID.equals(nodeId)) {
                        System.out.println(MY_NODE_ID + ": Username " + username + " taken by me. OK");
                    } else {
                        System.out.println(MY_NODE_ID + ": Username " + username + " taken by " + nodeId + ". NOT OK!");
                        continue;
                    }
                    Thread.sleep(1);
                    SESSION.logoutUser(username);
                }
            }
        } catch (BackendException b) {
            System.out.println(b.getMessage());
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }
}
