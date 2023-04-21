package xyz.skaerf.MusincServer.APIs.Users;

import xyz.skaerf.MusincServer.APIs.Deezer;
import xyz.skaerf.MusincServer.Account;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

public class DeezerUser {

    private String accessToken;
    private Account account;


    public DeezerUser(Account account) {
        this.account = account;
        HashMap<String,String> data = Deezer.getAccessCode();
        if (data != null) {
            this.accessToken = data.get("access_token");
            this.setUserInformation();
        }

    }

    public void setUserInformation() {
        try {
            URL url = new URL("https://api.deezer.com/user/me");

        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

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
