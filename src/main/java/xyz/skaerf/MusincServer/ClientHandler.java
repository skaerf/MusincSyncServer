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
                            this.buffWriter.println(RequestArgs.ACCEPTED + userAccount.getSpotifyUser().getCurrentAlbumCover() + ":!:" + currentTrack.getName() + ":!:" + currentTrack.getArtists()[0].getName());
                        }
                        catch (NullPointerException ignored) {}
                    }
                }
            }
            catch (IOException e) {
                ErrorHandler.warn("Client at "+socket.getInetAddress().getHostAddress()+" ("+userAccount.getUsername()+") dropped connection");
                this.closeConnection();
                break;
            }
        }
    }

    private Account getAccount() {
        return this.userAccount;
    }

    private String format(String var) {
        return var+"\n";
    }
}
