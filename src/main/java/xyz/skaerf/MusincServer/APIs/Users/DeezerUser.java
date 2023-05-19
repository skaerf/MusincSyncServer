package xyz.skaerf.MusincServer.APIs.Users;

import xyz.skaerf.MusincServer.APIs.Deezer;
import xyz.skaerf.MusincServer.Account;
import xyz.skaerf.MusincServer.ErrorHandler;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

public class DeezerUser {

    private String accessToken;
    private Account account;


    /**
     * Create a new DeezerUser instance and links it to the given account
     * @param account account to link the DeezerUser instance to
     */
    public DeezerUser(Account account) {
        this.account = account;
        HashMap<String,String> data = Deezer.getAccessCode();
        if (data != null) {
            this.accessToken = data.get("access_token");
            this.setUserInformation();
        }

    }

    /**
     * Sets the information in order to make it easier to pull data quickly without repeatedly
     * contacting the Deezer API
     */
    public void setUserInformation() {
        try {
            URL url = new URL("https://api.deezer.com/user/me");

        }
        catch (IOException e) {
            ErrorHandler.warn("Could not perform request to Deezer for user information", e.getStackTrace());
        }
    }

    /**
     * Creates a Deezer playlist on the user's Deezer account
     * @param name the name of the playlist to be created
     */
    public void createPlaylist(String name) {
        try {
            URL url = new URL("https://connect.deezer.com/user/{user_id}/playlists?title={title}");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            StringBuilder result = new StringBuilder();
        }
        catch (IOException e) {
            System.out.print(e);
        }
    }


}
