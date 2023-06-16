package xyz.skaerf.MusincServer.APIs;

import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import xyz.skaerf.MusincServer.ErrorHandler;

import java.io.IOException;
import java.net.URI;

public class Spotify {

    private static SpotifyApi api;
    private static final String clientID = "e31b70f90fe746a79c945f48a10b7dda";
    private static final String clientSecret = "d75a335cbe0d4e7fbc75ddb934c90eac";
    private static String accessToken = "";
    public static URI redirectUri = SpotifyHttpManager.makeUri("http://127.0.0.1:8080/scallb");

    /**
     * Initialises the API link for the server's connection to Spotify.
     */
    public static void initialiseAPILink() {
        api = new SpotifyApi.Builder()
                .setClientId(clientID)
                .setClientSecret(clientSecret)
                .setRedirectUri(redirectUri)
                .build();
        System.out.println("Connected to Spotify");
        requestClientCredentials();
    }

    /**
     * Instantiates a new client user under the server's API link for clients to connect
     * to their Spotify account.
     * @return SpotifyApi instance created to be added to a SpotifyUser instance
     */
    public static SpotifyApi instantiateClientUser() {
        return new SpotifyApi.Builder()
                .setClientId(clientID)
                .setClientSecret(clientSecret)
                .setRedirectUri(redirectUri)
                .build();
    }

    /**
     * Instantiates a new client user under the server's API link. Difference this has from
     * instantiateClientUser() is that this is designed to refresh a previous connection
     * using a refresh token, provided by a client.
     * @param refreshToken the refresh token to be used for the refresh
     */
    public static SpotifyApi instantiatePreviousAccess(String refreshToken) {
        SpotifyApi conn = new SpotifyApi.Builder()
                .setClientId(clientID)
                .setClientSecret(clientSecret)
                .setRedirectUri(redirectUri)
                .setRefreshToken(refreshToken)
                .build();
        try {
            AuthorizationCodeCredentials authCodeCred = conn.authorizationCodeRefresh().build().execute();
            conn.setAccessToken(authCodeCred.getAccessToken());
            System.out.println("Spotify account credentials for user were refreshed");
            return conn;
        }
        catch (IOException | ParseException | SpotifyWebApiException e) {
            ErrorHandler.warn("Could not refresh Spotify user credentials", e.getStackTrace());
        }
        return null;
    }

    /**
     * Requests the credentials from Spotify for server's API connection
     */
    private static void requestClientCredentials() {
        try {
            final ClientCredentials credentials = api.clientCredentials().build().execute();
            accessToken = credentials.getAccessToken();
            api.setAccessToken(accessToken);
            System.out.println("Client credentials expire in "+credentials.getExpiresIn());
        }
        catch (IOException | ParseException | SpotifyWebApiException e) {
            ErrorHandler.fatal("Could not request client credentials", e.getStackTrace());
        }
    }

    /**
     * @return the request URI for the server
     */
    public static URI requestURI() {
        return api.authorizationCodeUri().scope("user-read-currently-playing,playlist-modify-public,playlist-modify-private,user-read-playback-position,user-library-read,user-modify-playback-state,").build().execute();
    }

    /**
     * @return the server's API instance access token
     */
    public static String getAccessToken() {
        return accessToken;
    }
}
