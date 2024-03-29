package xyz.skaerf.MusincServer.Premieres;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PremiereManager implements Runnable {

    public static List<Premiere> premieres = new ArrayList<>();

    @Override
    public void run() {
        boolean go = true;
        while (go) {
            for (Premiere premiere : premieres) {
                LocalDateTime now = LocalDateTime.now();
                if (premiere.getPremiereTime().equals(now) && !premiere.isStarted()) { // maybe make this less specific?
                    System.out.println("Premiere for "+premiere.getHost().getUsername()+" is starting now");
                    premiere.start();
                }
            }
        }
    }
}
