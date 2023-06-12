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
    public BufferedReader buffReader;
    public PrintWriter buffWriter;
    private Account userAccount;

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
            String arg = "";
            String[] data = new String[]{};
            try {
                arg = read.split(";")[0]+";";
                data = read.split(";")[1].split(":!:");
            }
            catch (PatternSyntaxException e) {
                ErrorHandler.warn("Client is not correctly formatting requests to server. Closing connection");
                this.closeConnection();
            }
            if (arg.equalsIgnoreCase(RequestArgs.KEEPALIVE)) {
                // client has submitted a keepalive rather than a username
                long time = Long.parseLong(data[0].split(":")[0]);
                long elapsed = System.currentTimeMillis()-time;
                if (elapsed >= (86400000L * 30)) { // 30 day keepalive limit
                    System.out.println("User submitted a keepalive, but it was expired. Has been removed from database");
                    MySQLInterface.executeUpdate("delete from keepalives where keepalive = '"+data[0]+"'");
                    this.buffWriter.println(RequestArgs.DENIED);
                    this.closeConnection();
                }
                ResultSet keepaliveTable = MySQLInterface.executeStatement("select userid from keepalives where keepalive = '"+data[0]+"'");
                if (keepaliveTable == null) {
                    System.out.println("No records with provided keepalive were found");
                    this.buffWriter.println(RequestArgs.DENIED);
                    this.closeConnection();
                }
                int userid = 0;
                try {
                    if (keepaliveTable != null) while (keepaliveTable.next()) {
                        userid = keepaliveTable.getInt("userid");
                    }
                }
                catch (SQLException e) {
                    ErrorHandler.warn("Unable to iterate over provided keepalive data from SQL database. Denying access", e.getStackTrace());
                    this.buffWriter.println(RequestArgs.DENIED);
                    this.closeConnection();
                }
                ResultSet users = MySQLInterface.executeStatement("select username from users where userid = '"+userid+"'");
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
                    this.buffWriter.println(RequestArgs.ACCEPTED + userAccount.getUsername() + ":!:" + userAccount.getFirstname() + ":!:" + userAccount.getSurname() + ":!:" + userAccount.getEmail() + "\n");
                    Main.activeClients.add(this);
                    System.out.println(socket.getInetAddress().getHostAddress() + " connected as " + userAccount.getUsername() + " using a keepalive");
                }
                else {
                    this.buffWriter.println(format(RequestArgs.DENIED));
                    this.closeConnection();
                }
            }
            else if (arg.equalsIgnoreCase(RequestArgs.LOG_IN)) {
                userAccount = AccountManager.getAccount(data[0]); // first split is username, second is password hash
                if (userAccount != null && userAccount.getUsername() != null) {
                    try {
                        final String datum = data[1]; // only reason this is here is to confirm that a password (or something) was provided as a second argument
                    }
                    catch (ArrayIndexOutOfBoundsException e) {
                        ErrorHandler.warn("Client is not correctly formatting requests to server. Closing connection");
                        this.closeConnection();
                        return;
                    }
                    if (userAccount.checkPassword(data[1])) {
                        this.buffWriter.println(RequestArgs.ACCEPTED + userAccount.getUsername() + ":!:" + userAccount.getFirstname() + ":!:" + userAccount.getSurname() + ":!:" + userAccount.getEmail() + ":!:" + this.generateKeepalive() + "\n");
                        Main.activeClients.add(this);
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
                }
                else {
                    this.buffWriter.println(format(RequestArgs.DENIED+verify));
                    this.closeConnection();
                }
            }
        }
        catch (IOException e) {
            ErrorHandler.fatal("Could not instantiate client socket", e.getStackTrace());
            this.closeConnection();
        }
    }

    private String generateKeepalive() {
        ResultSet userIDs = MySQLInterface.executeStatement("select userid from users where username = '"+userAccount.getUsername()+"'");
        int userid = 0;
        String keepalive = System.currentTimeMillis()+":"+PassManager.generateSalt();
        try {
            if (userIDs != null) while (userIDs.next()) {
                userid = userIDs.getInt("userid");
            }
        }
        catch (SQLException e) {
            ErrorHandler.warn("Could not iterate through user IDs provided whilst attempting to save keepalive", e.getStackTrace());
        }

        MySQLInterface.executeUpdate("insert into keepalives (userid, keepalive) values ("+userid+", '"+keepalive+"')");
        return keepalive;
    }

    /**
    Closes the active connection with the client and removes itself from AccountManager's account cache
    as well as removing itself as an active client.
    Throws an error to ErrorHandler if it fails to complete any of this.
     */
    public void closeConnection() {
        Main.activeClients.remove(this);
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
                    String[] data;
                    if (!msgFromClient.equalsIgnoreCase(arg)) {
                        data = msgFromClient.split(";")[1].split(":!:");
                    }
                    if (arg.equalsIgnoreCase(RequestArgs.CREATE_SPOTIFY_ACCOUNT)) {
                        URI uri = userAccount.createSpotifyUser(); // this should return the log in link. add in SpotifyUser a 'confirm' function where the response can be pasted
                        this.buffWriter.println(RequestArgs.ACCEPTED+uri);

                        msgFromClient = this.buffReader.readLine(); // wait for response
                        arg = msgFromClient.split(";")[0]+";";
                        data = msgFromClient.split(";")[1].split(":!:");
                        if (!arg.equalsIgnoreCase(RequestArgs.GENERAL)) {
                            System.out.println("Client is not correctly formatting requests to server. Closing connection");
                            this.closeConnection();
                        }
                        if (userAccount.getSpotifyUser().confirm(data[0])) {
                            String alCov = userAccount.getSpotifyUser().getCurrentAlbumCover();
                            if (alCov != null) {
                                this.buffWriter.println(RequestArgs.ACCEPTED+alCov);
                            }
                            else {
                                this.buffWriter.println(RequestArgs.DENIED);
                            }
                        }
                        else {
                            this.buffWriter.println(RequestArgs.DENIED);
                        }
                    }
                    if (arg.equalsIgnoreCase(RequestArgs.UPDATE_PLAYING)) {
                        try {
                            Track currentTrack = userAccount.getSpotifyUser().getCurrentlyPlaying();
                            String albumCover = userAccount.getSpotifyUser().getCurrentAlbumCover();
                            long timestamp = userAccount.getSpotifyUser().getSongProgress();
                            String trackName = currentTrack.getName();
                            String artist = currentTrack.getArtists()[0].getName();
                            if (albumCover == null) {
                                this.buffWriter.println(RequestArgs.DENIED+"couldNotGrab");
                            }
                            else {
                                this.buffWriter.println(RequestArgs.ACCEPTED + albumCover + ":!:" + trackName + ":!:" + artist + ":!:" + timestamp);
                            }
                        }
                        catch (NullPointerException ignored) {}
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
