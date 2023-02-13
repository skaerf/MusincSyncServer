package xyz.skaerf.MusincServer.APIs.Users;

import xyz.skaerf.MusincServer.APIs.Deezer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

public class DeezerUser {

    private static String accessToken;


    public DeezerUser() {
        HashMap<String,String> data = Deezer.getAccessCode();
        if (data != null) {
            accessToken = data.get("access_token");
        }

    }

    public void pausePlayback() {
        try {
            URL url = new URL("https://api.deezer.com/player/stop"); // this doesn't exist idiot
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            connection.setDoOutput(true);

            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes("");
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println(responseCode);
            }
            else {
                System.out.println(responseCode);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
