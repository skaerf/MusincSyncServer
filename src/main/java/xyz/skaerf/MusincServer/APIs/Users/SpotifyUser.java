package xyz.skaerf.MusincServer.APIs.Users;

import org.apache.hc.core5.http.ParseException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Track;
import xyz.skaerf.MusincServer.APIs.Spotify;
import xyz.skaerf.MusincServer.Account;
import xyz.skaerf.MusincServer.ErrorHandler;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

public class SpotifyUser {

    private String authCode;
    private SpotifyApi clientAPI;
    private final Account account;
    private final URI userURI;

    public SpotifyUser(Account account) {
        userURI = Spotify.requestURI();
        this.account = account;
    }

    public boolean confirm(String authCode) {
        this.authCode = authCode;
        clientAPI = Spotify.instantiateClientUser();
        return setAuthTokens();
    }

    public URI getUserURI() {
        return this.userURI;
    }

    private boolean setAuthTokens() {
        final AuthorizationCodeCredentials authorizationCodeCredentials;
        try {
            authorizationCodeCredentials = clientAPI.authorizationCode(authCode).build().execute();
            clientAPI.setAccessToken(authorizationCodeCredentials.getAccessToken());
            clientAPI.setRefreshToken(authorizationCodeCredentials.getRefreshToken());
            System.out.println("Client API for "+clientAPI.getCurrentUsersProfile().build().execute().getDisplayName()+" expires in: " + authorizationCodeCredentials.getExpiresIn());
            return true;
        }
        catch (IOException | ParseException | SpotifyWebApiException e) {
            ErrorHandler.warn("could not generate authentication tokens for a user account", e.getStackTrace());
            return false;
        }
    }

    public void pausePlayback() {
        try {
            clientAPI.pauseUsersPlayback().build().execute();
        }
        catch (IOException | SpotifyWebApiException | ParseException e) {
            ErrorHandler.warn("Could not pause user playback", e.getStackTrace());
        }
    }

    public Track getCurrentlyPlaying() {
        try {
            Track track = clientAPI.getTrack(clientAPI.getUsersCurrentlyPlayingTrack().additionalTypes("track,episode").build().execute().getItem().getId()).build().execute();
            if (track == null) {
                try {
                    TimeUnit.MILLISECONDS.sleep(50);
                    track = clientAPI.getTrack(clientAPI.getUsersCurrentlyPlayingTrack().additionalTypes("track,episode").build().execute().getItem().getId()).build().execute();
                }
                catch (InterruptedException e) {
                    ErrorHandler.warn("Could not pause to wait for user's track to start playing", e.getStackTrace());
                }
            }
            return track;
        }
        catch (IOException | SpotifyWebApiException | ParseException | NullPointerException e) {
            ErrorHandler.warn("Could not get user's currently playing song", e.getStackTrace());
        }
        return null;
    }

    public String getCurrentAlbumCover() {
        // the single reason this does not conform to everything else is because the API I am using does not allow me to grab album covers
        HttpClient client = HttpClient.newHttpClient();
        if (this.getCurrentlyPlaying() != null) {
            String currID = this.getCurrentlyPlaying().getId();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.spotify.com/v1/tracks/" + currID))
                    .header("Authorization", "Bearer " + Spotify.getAccessToken())
                    .build();
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JSONParser parser = new JSONParser();
                JSONObject jsonResponse = (JSONObject) parser.parse(response.body());
                JSONObject album = (JSONObject) jsonResponse.get("album");
                JSONArray images = (JSONArray) album.get("images");
                JSONObject image0 = (JSONObject) images.get(0);
                return (String) image0.get("url");
            } catch (IOException | InterruptedException e) {
                System.out.println("Could not send request to Spotify for album cover");
            } catch (org.json.simple.parser.ParseException e) {
                System.out.println("Could not parse Spotify's response as JSON");
            }
        }
        return null;
    }

    public void resumePlayback() {
        try {
            clientAPI.startResumeUsersPlayback().build().execute();
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            ErrorHandler.warn("Could not start or resume user's playback", e.getStackTrace());
        }
    }

    public Playlist createPlaylist(String playlistName) {
        try {
            for (PlaylistSimplified p : getPlaylists()) {
                if (p.getName().equalsIgnoreCase(playlistName)) {
                    ErrorHandler.warn("Tried to create a playlist when one with the same name already exists");
                    return null;
                }
            }
            return clientAPI.createPlaylist(clientAPI.getCurrentUsersProfile().build().execute().getId(), playlistName).description("Playlist created automatically by Musinc! Songs are from your listening session on "+LocalDate.now().getDayOfMonth()+"/"+LocalDate.now().getMonthValue()+"/"+LocalDate.now().getYear()+" with "+account.getSession().getHostUser().getUsername()).build().execute();
        }
        catch (IOException | SpotifyWebApiException | ParseException e) {
            ErrorHandler.warn("Could not create spotify playlist", e.getStackTrace());
        }
        return null;
    }

    public void addToPlaylist(String playlistID, String[] uris) {
        try {
            clientAPI.addItemsToPlaylist(playlistID, uris).build().execute();
        }
        catch (IOException | SpotifyWebApiException | ParseException e) {
            ErrorHandler.warn("Could not add songs to spotify playlist", e.getStackTrace());
        }
    }

    public PlaylistSimplified[] getPlaylists() {
        try {
            return clientAPI.getListOfCurrentUsersPlaylists().build().execute().getItems();
        }
        catch (IOException | SpotifyWebApiException | ParseException e) {
            ErrorHandler.warn("Could not get user's spotify playlists", e.getStackTrace());
        }
        return null;
    }

}
