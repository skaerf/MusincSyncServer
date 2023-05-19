package xyz.skaerf.MusincServer;

import xyz.skaerf.MusincServer.APIs.Users.DeezerUser;
import xyz.skaerf.MusincServer.APIs.Users.SpotifyUser;

import java.net.URI;
import java.util.ArrayList;

public class Account {

    private final String username;
    private final String email;
    private final String firstname;
    private final String surname;
    private Session currentSession;
    private final ArrayList<String> knownIPs;
    private SpotifyUser spotify;
    private DeezerUser deezer;

    /**
     * Instantiates a new Account with the given credentials
     * @param username the username for the account to be created
     * @param email the email for the account to be created
     * @param firstname the first name of the user
     * @param surname the surname of the user
     * @param currentSession the current session that the user is in
     * @param knownIPs the known IP addresses of the user (for session keepalive)
     */
    public Account(String username, String email, String firstname, String surname, Session currentSession, ArrayList<String> knownIPs) {
        this.username = username;
        this.email = email;
        this.firstname = firstname;
        this.surname = surname;
        this.currentSession = currentSession;
        this.knownIPs = knownIPs;
    }

    /**
     * Gets the username of the Account
     * @return account username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the email of the Account
     * @return account email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Gets the first name set on the Account
     * @return first name of account user
     */
    public String getFirstname() {
        return firstname;
    }

    /**
     * Gets the surname set on the Account
     * @return surname of account user
     */
    public String getSurname() {
        return surname;
    }

    /**
     * Gets the array of known IP addresses for the Account's previous logins
     * @return ArrayList of Strings containing the known IPs
     */
    public ArrayList<String> getKnownIPs() {
        return knownIPs;
    }

    /**
     * Gets the current Session that the user is in, if any
     * @return current Session if in one, otherwise null
     */
    public Session getSession() {
        return currentSession;
    }

    /**
     * Adds the user to a given Session
     * @param session the session that the user is being added to
     */
    public void joinSession(Session session) {
        this.currentSession = session;
    }

    /**
     * Removes the user from their current Session
     */
    public void leaveSession() {
        this.currentSession.getClientUsers().remove(this);
    }

    /**
     * Creates a new Session under the user's account, setting them as the host
     */
    public void createSession() {
        this.currentSession = new Session(this);
    }

    /**
     * Creates a new instance of SpotifyUser to add the user's Spotify account
     * @return the user URI of the Spotify account
     */
    public URI createSpotifyUser() {
        spotify = new SpotifyUser(this);
        return spotify.getUserURI();
    }

    /**
     * Creates a new instance of DeezerUser to add the user's Deezer account
     */
    public void createDeezerUser() {
        deezer = new DeezerUser(this);
    }

    /**
     * Gets the user's SpotifyUser
     * @return SpotifyUser instance, null if none set
     */
    public SpotifyUser getSpotifyUser() {
        return spotify;
    }

    /**
     * Gets the user's DeezerUser
     * @return DeezerUser instance, null if none set
     */
    public DeezerUser getDeezerUser() {
        return deezer;
    }


}
