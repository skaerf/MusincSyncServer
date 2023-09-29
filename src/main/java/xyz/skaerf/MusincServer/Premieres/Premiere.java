package xyz.skaerf.MusincServer.Premieres;

import xyz.skaerf.MusincServer.Account;

import java.time.LocalDateTime;

public class Premiere {

    private Account host;
    private LocalDateTime premiereTime;

    public Premiere(Account host, LocalDateTime premiereTime) {
        this.host = host;
        this.premiereTime = premiereTime;
        PremiereManager.premieres.add(this);
        System.out.println("New premiere created for "+this.host.getLastName()+", "+this.host.getFirstName());
        System.out.println("Starting at "+this.premiereTime.getHour()+":"+this.premiereTime.getMinute()+" UTC");
    }
}
