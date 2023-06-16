package xyz.skaerf.MusincServer;

import xyz.skaerf.MusincServer.APIs.Users.DeezerUser;
import xyz.skaerf.MusincServer.APIs.Users.SpotifyUser;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class Account {

    private final String username;
    private final String email;
    private final String firstName;
    private final String lastName;
    private Session currentSession;
    private final ArrayList<String> knownIPs;
    private SpotifyUser spotify;
    private DeezerUser deezer;

    /**
     * Instantiates a new Account with the given credentials
     * @param username the username for the account to be created
     * @param email the email for the account to be created
     * @param firstName the first name of the user
     * @param lastName the last name of the user
     * @param currentSession the current session that the user is in
     * @param knownIPs the known IP addresses of the user (for session keepalive)
     */
    public Account(String username, String email, String firstName, String lastName, Session currentSession, ArrayList<String> knownIPs) {
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.currentSession = currentSession;
        this.knownIPs = knownIPs;
    }

    /**
     * Creates a doubly-hashed password for the user. Hashes with a salt for extra security.
     * @param hashedPass the pre-hashed password sent by the client
     */
    public void createPassword(String hashedPass) {
        String salt = PassManager.generateSalt();
        String dHashed = PassManager.getPassHash(salt+hashedPass); // returns re-hashed password with salt incorporated
        int userID = 0;
        ResultSet acc = MySQLInterface.executeStatement("select userid from users where username = '"+username+"'");
        try {
            if (acc != null) {
                while (acc.next()) {
                    userID = acc.getInt("userid");
                }
            }
        }
        catch (SQLException e) {
            ErrorHandler.warn("Could not iterate through the SQl server's response for UserID request", e.getStackTrace());
        }
        MySQLInterface.executeStatement("insert into passwords (userid, salt, hash) values ("+userID+","+salt+","+dHashed+")");
    }

    /**
     * Gets the username of the Account
     * @return account username
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Gets the email of the Account
     * @return account email
     */
    public String getEmail() {
        return this.email;
    }

    /**
     * Gets the first name set on the Account
     * @return first name of account user
     */
    public String getFirstName() {
        return this.firstName;
    }

    /**
     * Gets the last name set on the Account
     * @return last name of account user
     */
    public String getLastName() {
        return this.lastName;
    }

    /**
     * Gets the array of known IP addresses for the Account's previous logins
     * @return ArrayList of Strings containing the known IPs
     */
    public ArrayList<String> getKnownIPs() {
        return this.knownIPs;
    }

    /**
     * Checks if the given password is the correct password for the user's account.
     * @param password the password that the user has provided
     * @return false if password is incorrect, otherwise true
     */
    public boolean checkPassword(String password) {
        return PassManager.checkPass(this, password);
    }

    /**
     * Gets the current Session that the user is in, if any
     * @return current Session if in one, otherwise null
     */
    public Session getSession() {
        return this.currentSession;
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
        this.spotify = new SpotifyUser(this, true);
        return this.spotify.getUserURI();
    }

    /**
     * Creates a new instance of SpotifyUser if one does not exist that is
     * linked to this Account, otherwise it will refresh the preexisting one.
     * Used to refresh access upon a request being denied or when a client
     * reconnects and transmits a refresh token.
     * @param refreshToken the token to be used to refresh access
     * @return true if successful, otherwise false
     */
    public boolean refreshSpotifyAccess(String refreshToken) {
        if (this.spotify == null) {
            this.spotify = new SpotifyUser(this, false);
        }
        return this.spotify.refreshPastAccess(refreshToken);
    }

    /**
     * Creates a new instance of DeezerUser to add the user's Deezer account
     */
    public void createDeezerUser() {
        this.deezer = new DeezerUser(this);
    }

    /**
     * Gets the user's SpotifyUser
     * @return SpotifyUser instance, null if none set
     */
    public SpotifyUser getSpotifyUser() {
        return this.spotify;
    }

    /**
     * Gets the user's DeezerUser
     * @return DeezerUser instance, null if none set
     */
    public DeezerUser getDeezerUser() {
        return this.deezer;
    }


}
