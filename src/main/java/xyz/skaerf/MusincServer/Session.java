package xyz.skaerf.MusincServer;

import java.util.ArrayList;

public class Session {

    private final Account hostUser;
    private final String sessionID;
    private final ArrayList<Account> clientUsers;

    /**
     * Instantiates a new Session.
     * @param hostUser the Account to be used as the host of the Session
     */
    public Session(Account hostUser) {
        this.hostUser = hostUser;
        this.clientUsers = null;
        this.sessionID = Musinc.generateSessionID();
    }

    /**
     * @return the Session's ID
     */
    public String getSessionID() {
        return this.sessionID;
    }

    /**
     * Gets a list of client users in the form of their Accounts.
     * @return ArrayList of accounts for clients in the Session
     */
    public ArrayList<Account> getClientUsers() {
        return clientUsers;
    }

    /**
     * Gets the session host's Account
     * @return Account of Session's host
     */
    public Account getHostUser() {
        return this.hostUser;
    }

    /**
     * Removes the given user from this session. Done in this manner to prevent conflicts.
     * @param account the account that is being removed from the session
     */
    public void removeUser(Account account) {
        if (this.clientUsers != null) this.clientUsers.remove(account);
        if (this.hostUser.equals(account)) {
            Musinc.activeSessions.remove(this.sessionID);
        }
    }

    /**
     * Adds a user to the Session. Done this way rather than just modifying the clientUsers list because
     * all other connected clients must be informed of the change
     * @param account the account to be added
     */
    public void addUser(Account account) {
        this.clientUsers.add(account);
        for (Account user : this.clientUsers) {
            // TODO broadcast that a user has joined somehow??? maybe other thread in client when user is in session?
        }
    }

    /**
     * Pauses the Spotify music for every client in the Session.
     * Used to attempt to maintain track sync.
     */
    public void pauseAll() {
        for (Account user : this.clientUsers) {
            user.getSpotifyUser().pausePlayback();
        }
    }

    /**
     * Resumes the Spotify music for every client in the Session.
     * Used to attempt to maintain track sync.
     */
    public void resumeAll() {
        for (Account user : this.clientUsers) {
            user.getSpotifyUser().resumePlayback();
        }
    }

    /**
     * Sends a 'previous' command to every Spotify user in the Session.
     */
    public void previousAll() {
        if (this.clientUsers != null) for (Account user : this.clientUsers) {
            user.getSpotifyUser().previousTrack();
        }
    }

    /**
     * Sends a 'next' command to every Spotify user in the Session;
     */
    public void nextAll() {
        if (this.clientUsers != null) for (Account user : this.clientUsers) {
            user.getSpotifyUser().nextTrack();
        }
    }
}
