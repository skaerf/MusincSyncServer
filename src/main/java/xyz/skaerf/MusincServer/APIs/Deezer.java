package xyz.skaerf.MusincServer.APIs;

import xyz.skaerf.MusincServer.ErrorHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Deezer {

    static String appID = "581984";
    static String secret = "58d59ca75fd31f26b199a9ae2015eba0";
    static String redirectURI = "https://musinc.me/dcallb";

    public static HashMap<String, String> getAccessCode() {
        try {
            URL obj = new URL("https://connect.deezer.com/oauth/auth.php?app_id={0}&redirect_uri={1}&perms=basic_access,email,offline_access,manage_library".replace("{0}", appID).replace("{1}", redirectURI));
            System.out.println(obj);
            return requestAccessToken(new Scanner(System.in).next());
        }
        catch (IOException e) {
            ErrorHandler.warn("Could not get access code for user", e.getStackTrace());
        }
        return null;
    }

    private static HashMap<String, String> requestAccessToken(String code) {
        Map<String, String> params = new HashMap<>();
        params.put("app_id", appID);
        params.put("secret", secret);
        params.put("code", code);
        try {
            URL url = new URL("https://connect.deezer.com/oauth/access_token.php");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            StringBuilder result = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (first) {
                    first = false;
                }
                else {
                    result.append("&");
                }
                result.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                result.append("=");
                result.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }
            writer.write(result.toString());
            writer.flush();
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String response = new BufferedReader(new InputStreamReader(connection.getInputStream())).lines().collect(Collectors.joining());
                HashMap<String, String> data = new HashMap<>();
                data.put("access_token", response.split("=")[1].split("&")[0]);
                data.put("expires", response.split("&")[1].split("=")[1]);
                return data;
            }
        }
        catch (IOException e) {
            ErrorHandler.warn("Could not request access token for access code "+code.substring(0,8)+"...", e.getStackTrace());
        }
        return null;
    }

    public static String makeRequest(String requestInfo) {
        try {
            URL url = new URL(redirectURI);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return response.toString();
        }
        catch (IOException e) {
            ErrorHandler.warn("Could not make web request to Deezer API", e.getStackTrace());
        }
        return null;
    }

}
