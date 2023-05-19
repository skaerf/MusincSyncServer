package xyz.skaerf.MusincServer;

import se.michaelthelin.spotify.model_objects.specification.Track;

import java.io.*;
import java.net.Socket;
import java.net.URI;
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
            String data = "";
            try {
                arg = read.split(";")[0]+";";
                data = read.split(";")[1];
            }
            catch (PatternSyntaxException e) {
                System.out.println("Client is not correctly formatting requests to server. Closing connection");
                this.closeConnection();
            }
            if (arg.equalsIgnoreCase(RequestArgs.LOG_IN)) {
                userAccount = AccountManager.getAccount(data);
                if (userAccount != null && userAccount.getUsername() != null) {
                    this.buffWriter.println(RequestArgs.ACCEPTED+ userAccount.getUsername()+":!:"+ userAccount.getFirstname()+":!:"+ userAccount.getSurname()+":!:"+userAccount.getEmail()+"\n");
                    Main.activeClients.add(this);
                    System.out.println(socket.getInetAddress().getHostAddress()+" connected as "+userAccount.getUsername());
                }
                else {
                    this.buffWriter.println(format(RequestArgs.DENIED));
                    this.closeConnection();
                }
            }
            else if (arg.equalsIgnoreCase(RequestArgs.CREATE_ACCOUNT)) {
                String[] dataList = data.split(":!:");
                if (dataList.length != 3) {
                    this.buffWriter.println(format(RequestArgs.NOT_ENOUGH_ARGS));
                    this.closeConnection();
                }
                Object verify = AccountManager.createNew(userAccount = new Account(dataList[0], dataList[1], dataList[2], dataList[3], null, null));
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

    /**
    Closes the active connection with the client and removes itself from AccountManager's account cache
    as well as removing itself as an active client.
    Throws an error to ErrorHandler if it fails to complete any of this.
     */
    public void closeConnection() {
        Main.activeClients.remove(this);
        AccountManager.removeFromCache(userAccount);
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
                            String trackName = currentTrack.getName();
                            String artist = currentTrack.getArtists()[0].getName();
                            if (albumCover == null) {
                                this.buffWriter.println(RequestArgs.DENIED+"couldNotGrab");
                            }
                            else {
                                this.buffWriter.println(RequestArgs.ACCEPTED + albumCover + ":!:" + trackName + ":!:" + artist);
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
