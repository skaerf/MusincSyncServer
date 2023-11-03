package xyz.skaerf.MusincServer.Premieres;

import xyz.skaerf.MusincServer.Account;
import xyz.skaerf.MusincServer.Session;

import java.time.LocalDateTime;

public class Premiere extends Session {

    private Account host;
    private LocalDateTime premiereTime;

    public Premiere(Account host, LocalDateTime premiereTime) {
        super(host);
        this.host = host;
        this.premiereTime = premiereTime;
        PremiereManager.premieres.add(this);
        System.out.println("New premiere created for "+this.host.getLastName()+", "+this.host.getFirstName());
        System.out.println("Starting at "+this.premiereTime.getHour()+":"+this.premiereTime.getMinute()+" UTC");
    }

    protected Account getHost() {
        return this.host;
    }

    public LocalDateTime getPremiereTime() {
        return this.premiereTime;
    }
}
