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
                if (premiere.getPremiereTime().equals(now)) { // maybe make this less specific?

                }
            }
        }
    }
}
