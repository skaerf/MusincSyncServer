package xyz.skaerf.MusincServer.Premieres;

import se.michaelthelin.spotify.model_objects.specification.Track;
import xyz.skaerf.MusincServer.Account;
import xyz.skaerf.MusincServer.Session;

import java.time.LocalDateTime;

public class Premiere extends Session {

    private final Account host;
    private final LocalDateTime premiereTime;
    private Track track = null;
    private boolean isStarted = false;

    public Premiere(Account host, LocalDateTime premiereTime) { // add ability to play mp3 files etc
        super(host);
        this.host = host;
        this.premiereTime = premiereTime;
        PremiereManager.premieres.add(this);
        System.out.println("New premiere created for "+this.host.getLastName()+", "+this.host.getFirstName());
        System.out.println("Starting at "+this.premiereTime.getHour()+":"+this.premiereTime.getMinute()+" UTC");
    }

    public Premiere(Account host, LocalDateTime premiereTime, Track track) {
        super(host);
        this.host = host;
        this.premiereTime = premiereTime;
        this.track = track;
        PremiereManager.premieres.add(this);
        System.out.println("New premiere created for "+this.host.getLastName()+", "+this.host.getFirstName());
        System.out.println("Starting at "+this.premiereTime.getHour()+":"+this.premiereTime.getMinute()+" UTC");
        System.out.println("Track has been specified as "+track.getName()+" on Spotify");
    }

    protected Account getHost() {
        return this.host;
    }

    public LocalDateTime getPremiereTime() {
        return this.premiereTime;
    }

    public void start() {
        if (track != null) {
            for (Account user : getClientUsers()) user.getSpotifyUser().queueSong(track);
            for (Account user : getClientUsers()) user.getSpotifyUser().nextTrack();
            this.isStarted = true;
        }
    }

    public boolean isStarted() {
        return this.isStarted;
    }
}
