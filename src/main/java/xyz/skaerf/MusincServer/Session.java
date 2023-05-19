package xyz.skaerf.MusincServer;

import java.util.ArrayList;

public class Session {

    private final Account hostUser;
    private final ArrayList<Account> clientUsers;

    /**
     * Instantiates a new Session.
     * @param hostUser the Account to be used as the host of the Session
     */
    public Session(Account hostUser) {
        this.hostUser = hostUser;
        this.clientUsers = null;
    }

    /**
     * Gets a list of client users in the form of their Accounts.
     * @return ArrayList<Account> of clients in the Session
     */
    public ArrayList<Account> getClientUsers() {
        return clientUsers;
    }

    /**
     * Gets the session host's Account
     * @return Account of Session's host
     */
    public Account getHostUser() {
        return hostUser;
    }

    /**
     * Pauses the Spotify music for every client in the Session.
     * Used to attempt to maintain track sync.
     * Will not work in its current state (could potentially error) and is therefore in temporary disuse.
     */
    public void pauseAll() {
        for (Account user : getClientUsers()) {
            user.getSpotifyUser().pausePlayback();
        }
    }
}
