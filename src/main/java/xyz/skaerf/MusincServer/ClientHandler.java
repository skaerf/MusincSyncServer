package xyz.skaerf.MusincServer;

import se.michaelthelin.spotify.model_objects.specification.Track;

import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.PatternSyntaxException;

public class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader buffReader;
    private PrintWriter buffWriter;
    private Account userAccount;
    private int userID = 0;

    private Thread sessionListenerThread;

    /**
    Instantiates a new ClientHandler with the socket that was created for it.
     Only used by MainServer to allow the server's main thread to create child
     threads, allowing more than one connection simultaneously.
     Only instantiated in a new thread for this reason.
     @param socket the socket that the client is connected to
     */
    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.buffWriter = new PrintWriter(socket.getOutputStream(), true);
            this.buffReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String read = this.buffReader.readLine();
            String arg;
            String[] data;
            try {
                arg = read.split(";")[0] + ";";
                data = read.split(";")[1].split(":!:");

                if (arg.equalsIgnoreCase(RequestArgs.KEEPALIVE)) {
                    MySQLInterface.connectDatabase();
                    // client has submitted a keepalive rather than a username
                    long time = Long.parseLong(data[0].split(":")[0]);
                    String ipHash = data[0].split(":")[2];
                    String clientIpHash = PassManager.getPassHash(this.socket.getRemoteSocketAddress().toString().replace(".", "").split(":")[0]);
                    long elapsed = System.currentTimeMillis() - time;
                    if (elapsed >= (86400000L * 30)) { // 30 day keepalive limit
                        System.out.println("User submitted a keepalive, but it was expired. Has been removed from database");
                        MySQLInterface.executeUpdate("delete from keepalives where keepalive = '" + data[0] + "'");
                        this.buffWriter.println(RequestArgs.DENIED);
                        this.closeConnection();
                    }
                    if (!ipHash.equals(clientIpHash)) {
                        System.out.println("User submitted a keepalive, but it was from the wrong IP address.\nRemoving from database to prevent malicious use");
                        MySQLInterface.executeUpdate("delete from keepalives where keepalive = '" + data[0] + "'");
                        this.buffWriter.println(RequestArgs.DENIED);
                        this.closeConnection();
                    }
                    ResultSet keepaliveTable = MySQLInterface.executeStatement("select userid from keepalives where keepalive = '" + data[0] + "'");
                    if (keepaliveTable == null) {
                        System.out.println("No records with provided keepalive were found");
                        this.buffWriter.println(RequestArgs.DENIED);
                        this.closeConnection();
                    }
                    try {
                        if (keepaliveTable != null) while (keepaliveTable.next()) {
                            userID = keepaliveTable.getInt("userid");
                        }
                    }
                    catch (SQLException e) {
                        ErrorHandler.warn("Unable to iterate over provided keepalive data from SQL database. Denying access", e.getStackTrace());
                        this.buffWriter.println(RequestArgs.DENIED);
                        this.closeConnection();
                    }
                    ResultSet users = MySQLInterface.executeStatement("select username from users where userid = '" + userID + "'");
                    String username = "";
                    try {
                        if (users != null) while (users.next()) {
                            username = users.getString("username");
                        }
                    }
                    catch (SQLException e) {
                        ErrorHandler.warn("Could not iterate through provided user data from SQL. Denying user access", e.getStackTrace());
                        this.buffWriter.println(RequestArgs.DENIED);
                        this.closeConnection();
                    }
                    userAccount = AccountManager.getAccount(username);
                    if (userAccount != null && userAccount.getUsername() != null) {
                        this.buffWriter.println(RequestArgs.ACCEPTED + userAccount.getUsername() + ":!:" + userAccount.getFirstName() + ":!:" + userAccount.getLastName() + ":!:" + userAccount.getEmail() + "\n");
                        Musinc.activeClients.add(this);
                        System.out.println(socket.getInetAddress().getHostAddress() + " connected as " + userAccount.getUsername() + " using a keepalive");
                    }
                    else {
                        this.buffWriter.println(format(RequestArgs.DENIED));
                        this.closeConnection();
                    }
                }
                else if (arg.equalsIgnoreCase(RequestArgs.LOG_IN)) {
                    boolean newPass = false;
                    // TODO THIS IS TEMPORARY I PROMISE
                    if (data[0].endsWith("^$^")) {
                        newPass = true;
                        data[0] = data[0].replace("^$^", "");
                        System.out.println("Creating a new password for user");
                    }
                    userAccount = AccountManager.getAccount(data[0]); // first split is username, second is password hash
                    if (userAccount != null && userAccount.getUsername() != null) {
                        if (newPass) userAccount.createPassword(data[1]);
                        if (userAccount.checkPassword(data[1])) {
                            this.buffWriter.println(RequestArgs.ACCEPTED + userAccount.getUsername() + ":!:" + userAccount.getFirstName() + ":!:" + userAccount.getLastName() + ":!:" + userAccount.getEmail() + ":!:" + this.generateKeepalive() + "\n");
                            Musinc.activeClients.add(this);
                            System.out.println(socket.getInetAddress().getHostAddress() + " connected as " + userAccount.getUsername());
                        }
                        else {
                            this.buffWriter.println(format(RequestArgs.DENIED));
                            this.closeConnection();
                        }
                    }
                    else {
                        this.buffWriter.println(format(RequestArgs.DENIED));
                        this.closeConnection();
                    }
                }
                else if (arg.equalsIgnoreCase(RequestArgs.CREATE_ACCOUNT)) {
                    if (data.length != 3) {
                        this.buffWriter.println(format(RequestArgs.NOT_ENOUGH_ARGS));
                        this.closeConnection();
                    }
                    Object verify = AccountManager.createNew(userAccount = new Account(data[0], data[1], data[2], data[3], null, null));
                    if (verify instanceof Account) {
                        this.buffWriter.println(format(RequestArgs.CREATE_ACCOUNT));
                        userAccount = (Account) verify;
                    } else {
                        this.buffWriter.println(format(RequestArgs.DENIED + verify));
                        this.closeConnection();
                    }
                }
            }
            catch (PatternSyntaxException | ArrayIndexOutOfBoundsException e) {
                ErrorHandler.warn("Client is sending malformed requests to server. Closing connection");
                this.closeConnection();
            }
        }
        catch (IOException e) {
            ErrorHandler.fatal("Could not instantiate client socket", e.getStackTrace());
            this.closeConnection();
        }
    }

    /**
     * Generates a keepalive key for a client. Keepalives are formatted as the time at generation in milliseconds with a random
     * string of letters at the end and then the connection's IP address at the time of generation. I've used the
     * PassManager.generateSalt() method for this as there's no point creating two separate string randomizers.
     * @return the keepalive that was generated
     */
    private String generateKeepalive() {
        ResultSet userIDs = MySQLInterface.executeStatement("select userid from users where username = '"+userAccount.getUsername()+"'");
        String keepalive = System.currentTimeMillis()+":"+PassManager.generateSalt()+":"+PassManager.getPassHash(this.socket.getRemoteSocketAddress().toString().replace(".", "").split(":")[0]);
        try {
            if (userIDs != null) while (userIDs.next()) {
                userID = userIDs.getInt("userid");
            }
        }
        catch (SQLException e) {
            ErrorHandler.warn("Could not iterate through user IDs provided whilst attempting to save keepalive", e.getStackTrace());
        }

        MySQLInterface.executeUpdate("insert into keepalives (userid, keepalive) values ("+userID+", '"+keepalive+"')");
        return keepalive;
    }

    /**
     * Closes the active connection with the client
     * as well as removing itself as an active client.
     * Throws an error to ErrorHandler if it fails to complete any of this.
     */
    public void closeConnection() {
        Musinc.activeClients.remove(this);
        try {
            if (this.buffReader != null) {
                this.buffReader.close();
            }
            if (this.buffWriter != null) {
                this.buffWriter.close();
            }
            if (this.socket != null) {
                this.socket.close();
            }
        }
        catch (IOException e) {
            ErrorHandler.warn("Could not close connection with client", e.getStackTrace());
        }
    }

    /**
    Runnable that the instantiated thread uses to indefinitely listen for requests from the client
    until the connection is closed by either the server or the client.
     */
    @Override
    public void run() {
        String msgFromClient;
        while (socket.isConnected()) {
            try {
                msgFromClient = this.buffReader.readLine();
                if (msgFromClient != null && !msgFromClient.equals("")) {
                    String arg = msgFromClient.split(";")[0]+";";
                    String[] data = null;
                    if (!msgFromClient.equalsIgnoreCase(arg)) {
                        data = msgFromClient.split(";")[1].split(":!:");
                    }
                    if (arg.equalsIgnoreCase(RequestArgs.CREATE_SPOTIFY_ACCOUNT)) {
                        URI uri = userAccount.createSpotifyUser();
                        this.buffWriter.println(RequestArgs.ACCEPTED+uri);

                        msgFromClient = this.buffReader.readLine(); // wait for response
                        arg = msgFromClient.split(";")[0]+";";
                        data = msgFromClient.split(";")[1].split(":!:");
                        if (!arg.equalsIgnoreCase(RequestArgs.GENERAL)) {
                            System.out.println("Client is not correctly formatting requests to server. Closing connection");
                            this.closeConnection();
                        }
                        if (userAccount.getSpotifyUser().confirm(data[0])) {
                            Track currentlyPlaying = userAccount.getSpotifyUser().getCurrentlyPlaying();
                            String alCov = userAccount.getSpotifyUser().getCurrentAlbumCover();
                            if (alCov != null) {
                                this.buffWriter.println(RequestArgs.ACCEPTED+userAccount.getSpotifyUser().getRefreshToken()+":!:"+alCov+":!:"+currentlyPlaying.getName()+":!:"+currentlyPlaying.getArtists()[0].getName()+":!:"+userAccount.getSpotifyUser().getSongProgress()+"/"+userAccount.getSpotifyUser().getCurrentTrackLength());
                            }
                            else {
                                this.buffWriter.println(RequestArgs.ACCEPTED+userAccount.getSpotifyUser().getRefreshToken());
                            }
                        }
                        else {
                            this.buffWriter.println(RequestArgs.DENIED);
                        }
                    }
                    if (arg.equalsIgnoreCase(RequestArgs.REAUTHENTICATE_SPOTIFY_ACCOUNT)) {
                        ResultSet sqlResponse;
                        if (userID == 0) {
                            sqlResponse = MySQLInterface.executeStatement("select userid from users where username = '"+userAccount.getUsername()+"'");
                            if (sqlResponse != null) while (sqlResponse.next()) {
                                userID = sqlResponse.getInt("userid");
                            }
                        }
                        sqlResponse = MySQLInterface.executeStatement("select spotify from connections where userid = "+userID);
                        String refreshToken = "";
                        if (sqlResponse != null) while (sqlResponse.next()) {
                            refreshToken = sqlResponse.getString("spotify");
                        }
                        else {
                            this.buffWriter.println(RequestArgs.DENIED);
                        }
                        if (!refreshToken.equalsIgnoreCase("")) {
                            userAccount.refreshSpotifyAccess(refreshToken);
                        }
                        // OLD
                        String token = msgFromClient.split(";")[1];
                        if (userAccount.refreshSpotifyAccess(token)) {
                            this.buffWriter.println(RequestArgs.ACCEPTED);
                        }
                    }
                    if (arg.equalsIgnoreCase(RequestArgs.UPDATE_PLAYING)) {
                        System.out.println("Received a request to update playing information from "+userAccount.getUsername());
                        Track currentTrack = userAccount.getSpotifyUser().getCurrentlyPlaying(); // TODO THIS CAN BE NULL IF ACCOUNT HAS NOT BEEN CREATED AND AN UPDATE IS MANUALLY REQUESTED
                        System.out.println("yee 1");
                        long timestamp;
                        String trackName;
                        String artist;
                        if (currentTrack != null) {
                            System.out.println("yee 2");
                            timestamp = userAccount.getSpotifyUser().getSongProgress();
                            if (timestamp > userAccount.getSpotifyUser().getCurrentTrackLength()) {
                                if (userAccount.refreshSpotifyAccess(userAccount.getSpotifyUser().getRefreshToken())) {
                                    currentTrack = userAccount.getSpotifyUser().getCurrentlyPlaying();
                                    timestamp = userAccount.getSpotifyUser().getSongProgress();
                                }
                                else {
                                    ErrorHandler.warn("Attempted to refresh user account credentials when the timestamp was longer than the length, but failed");
                                    this.buffWriter.println(RequestArgs.DENIED);
                                }
                            }
                            String formattedTimestamp = timestamp+"/"+userAccount.getSpotifyUser().getCurrentTrackLength();
                            trackName = currentTrack.getName();
                            artist = currentTrack.getArtists()[0].getName();
                            String albumCover = userAccount.getSpotifyUser().getCurrentAlbumCover();
                            System.out.println("yee 2.5");
                            this.buffWriter.println(RequestArgs.ACCEPTED + albumCover + ":!:" + trackName + ":!:" + artist + ":!:" + formattedTimestamp);
                        }
                        else {
                            System.out.println("yee 3");
                            userAccount.refreshSpotifyAccess(userAccount.getSpotifyUser().getRefreshToken());
                            System.out.println("yee 4");
                            currentTrack = userAccount.getSpotifyUser().getCurrentlyPlaying();
                            System.out.println("yee 5");
                            if (currentTrack == null) {
                                System.out.println("yee 6");
                                this.buffWriter.println(RequestArgs.DENIED);
                            }
                            else {
                                timestamp = userAccount.getSpotifyUser().getSongProgress();
                                trackName = currentTrack.getName();
                                artist = currentTrack.getArtists()[0].getName();
                                System.out.println("yee 7");
                                String albumCover = userAccount.getSpotifyUser().getCurrentAlbumCover();
                                System.out.println("yee 8");
                                this.buffWriter.println(RequestArgs.ACCEPTED + albumCover + ":!:" + trackName + ":!:" + artist + ":!:" + timestamp+"/"+userAccount.getSpotifyUser().getCurrentTrackLength());
                            }
                        }
                    }
                    if (arg.equalsIgnoreCase(RequestArgs.PLAY_PAUSE)) {
                        if (userAccount.getSpotifyUser().isPaused()) {
                            if (userAccount.getSpotifyUser().resumePlayback()) {
                                if (userAccount.isSessionHost()) {
                                    userAccount.getSession().resumeAll();
                                }
                                this.buffWriter.println(RequestArgs.ACCEPTED);
                            }
                            else {
                                this.buffWriter.println(RequestArgs.DENIED);
                            }
                        }
                        else {
                            if (userAccount.getSpotifyUser().pausePlayback()) {
                                if (userAccount.isSessionHost()) {
                                    userAccount.getSession().pauseAll();
                                }
                                this.buffWriter.println(RequestArgs.ACCEPTED);
                            }
                            else {
                                this.buffWriter.println(RequestArgs.DENIED);
                            }
                        }
                    }
                    if (arg.equalsIgnoreCase(RequestArgs.PREVIOUS_TRACK)) {
                       if (userAccount.getSpotifyUser().previousTrack()) {
                           if (userAccount.isSessionHost()) {
                               userAccount.getSession().previousAll();
                           }
                           this.buffWriter.println(RequestArgs.ACCEPTED);
                       }
                       else {
                           this.buffWriter.println(RequestArgs.DENIED);
                       }
                    }
                    if (arg.equalsIgnoreCase(RequestArgs.NEXT_TRACK)) {
                        if (userAccount.getSpotifyUser().nextTrack()) {
                            if (userAccount.isSessionHost()) {
                                userAccount.getSession().nextAll();
                            }
                            this.buffWriter.println(RequestArgs.ACCEPTED);
                        }
                        else {
                            this.buffWriter.println(RequestArgs.DENIED);
                        }
                    }
                    if (arg.equalsIgnoreCase(RequestArgs.CREATE_SESSION)) {
                        this.buffWriter.println(RequestArgs.ACCEPTED+userAccount.createSession().getSessionID());

                    }
                    if (arg.equalsIgnoreCase(RequestArgs.JOIN_SESSION)) {
                        if (data == null) {
                            this.buffWriter.println(RequestArgs.NOT_ENOUGH_ARGS);
                        }
                        else {
                            String sessionID = data[0];
                            Session session = Musinc.getActiveSession(sessionID);
                            if (session == null) {
                                this.buffWriter.println(RequestArgs.DENIED);
                            }
                            else {
                                StringBuilder clients = new StringBuilder();
                                if (session.getClientUsers() != null) {
                                    for (Account user : session.getClientUsers()) {
                                        clients.append(user.getUsername()).append(":!:");
                                    }
                                    clients.deleteCharAt(clients.length());
                                    clients.deleteCharAt(clients.length());
                                    clients.deleteCharAt(clients.length());
                                    this.buffWriter.println(RequestArgs.ACCEPTED+session.getHostUser()+":!:"+ clients);
                                }
                                else {
                                    this.buffWriter.println(RequestArgs.ACCEPTED+session.getHostUser().getUsername());
                                }
                                userAccount.joinSession(session);
                            }
                        }
                    }
                }
            }
            catch (IOException e) {
                if (userAccount != null) {
                    ErrorHandler.warn("Client at "+socket.getInetAddress().getHostAddress()+" ("+userAccount.getUsername()+") dropped connection");
                }
                else {
                    ErrorHandler.warn("Client at "+socket.getInetAddress().getHostAddress()+" (null) dropped connection");
                }
                this.closeConnection();
                break;
            }
            catch (SQLException e) {
                ErrorHandler.warn("Could not iterate through provided SQL data", e.getStackTrace());
            }
        }
    }

    /**
    Grabs the Account that is linked to this ClientHandler.
    @return Account of active client
     */
    private Account getAccount() {
        return this.userAccount;
    }

    /**
    Formats a response for the client to receive.
    Just simplifies my strings to prevent me from having to add the +"\n"; to everything.
     @param var string to be formatted
    @return Given response string with correct transmission formatting.
     */
    private String format(String var) {
        return var+"\n";
    }
}
