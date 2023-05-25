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

    private String currentID;
    private long progressMS;
    private long timeOfCall;

    /**
    Instantiates a new SpotifyUser for a user - allows a Spotify account to be connected
     */
    public SpotifyUser(Account account) {
        userURI = Spotify.requestURI();
        this.account = account;
    }

    /**
    Used to confirm the authentication code for a Spotify user. This allows the actual client user to be instantiated by
    the server's Spotify login.
     @return boolean of the result of the client authentication tokens being set for use
     */
    public boolean confirm(String authCode) {
        this.authCode = authCode;
        clientAPI = Spotify.instantiateClientUser();
        return setAuthTokens();
    }

    /**
    @return the user's Spotify account URI
     */
    public URI getUserURI() {
        return this.userURI;
    }

    /**
    Used internally by the class to set the authentication tokens from the user based on the class' authCode variable.
    @return True if succeeded to grab auth tokens, false if not
     */
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

    /**
    Used to pause the playback of the user's Spotify account. Throws an error to ErrorHandler if it is unable to do so.
     */
    public void pausePlayback() {
        try {
            clientAPI.pauseUsersPlayback().build().execute();
        }
        catch (IOException | SpotifyWebApiException | ParseException e) {
            ErrorHandler.warn("Could not pause user playback", e.getStackTrace());
        }
    }

    /**
    Gets the user's currently playing Spotify song. Rate limited to once every ten seconds to prevent throttling on Spotify's end.
    @return a Track variable of the user's currently playing song.
     */
    public Track getCurrentlyPlaying() {
        if (currentID != null && System.currentTimeMillis() - Long.parseLong(currentID.split(":")[0]) >= 10000) {
            try {
                return clientAPI.getTrack(currentID.split(":")[1]).build().execute();
            }
            catch (IOException | SpotifyWebApiException | ParseException e) {
                ErrorHandler.warn("Could not get track", e.getStackTrace());
            }
        }
        try {
            CurrentlyPlaying curPlay = clientAPI.getUsersCurrentlyPlayingTrack().additionalTypes("track,episode").build().execute();
            if (curPlay.getCurrentlyPlayingType() != null) {
                Track track = clientAPI.getTrack(curPlay.getItem().getId()).build().execute();
                if (track == null) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(50);
                        track = clientAPI.getTrack(curPlay.getItem().getId()).build().execute();
                    }
                    catch (InterruptedException e) {
                        ErrorHandler.warn("Could not pause to wait for user's track to start playing", e.getStackTrace());
                    }
                }
                assert track != null;
                this.currentID = System.currentTimeMillis()+":"+track.getId();
                this.timeOfCall = curPlay.getTimestamp();
                this.progressMS = curPlay.getProgress_ms();
                return track;
            }
            else {
                TimeUnit.SECONDS.sleep(5);
                curPlay = clientAPI.getUsersCurrentlyPlayingTrack().additionalTypes("track,episode").build().execute();
                Track track = clientAPI.getTrack(curPlay.getItem().getId()).build().execute();

                if (track == null) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(50);
                        track = clientAPI.getTrack(curPlay.getItem().getId()).build().execute();
                    }
                    catch (InterruptedException e) {
                        ErrorHandler.warn("Could not pause to wait for user's track to start playing", e.getStackTrace());
                    }
                }
                assert track != null;
                this.currentID = System.currentTimeMillis()+":"+track.getId();
                this.timeOfCall = curPlay.getTimestamp();
                this.progressMS = curPlay.getProgress_ms();
                return track;
            }
        }
        catch (IOException | SpotifyWebApiException | ParseException | NullPointerException e) {
            ErrorHandler.warn("Could not get user's currently playing song", e.getStackTrace());
            return null;
        }
        catch (InterruptedException e) {
            ErrorHandler.warn("Could not sleep whilst waiting for CurrentlyPlaying throttle timeout", e.getStackTrace());
        }
        return null;
    }

    /**
     * Gets the progress in MS for the song that is currently playing. Must be called immediately after getCurrentlyPlaying() or
     * it will not contain the most recently updated timestamp.
     * Uses the time that currentlyPlaying was called to Spotify plus the current time and the provided progress time.
     * @return the progress in MS for the currently playing song
     */
    public long getSongProgress() {
        long difference = System.currentTimeMillis()-this.timeOfCall;
        return this.progressMS+difference;
    }

    /**
    getCurrentlyPlaying() MUST be called first. This is purely for rate limiting reasons - ends up being way too many requests
    otherwise and results in program being inoperable.
    @return the URL (in form of a String) for the user's currently playing song's album cover. Used for display on client.
     */
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

    /**
    Resumes the Spotify user's playback if it was paused.
    Throws error to ErrorHandler if unable to resume.
     */
    public void resumePlayback() {
        try {
            clientAPI.startResumeUsersPlayback().build().execute();
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            ErrorHandler.warn("Could not start or resume user's playback", e.getStackTrace());
        }
    }

    /**
    Creates a playlist on the user's behalf.
    @return the playlist that is created, null if did not succeed.
     @param playlistName the name for the playlist
     */
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

    /**
    Adds one or many songs to a given playlist via its Spotify ID.
    Songs are received in a string list to allow simple iteration.
     @param playlistID the Spotify ID of the playlist to be added to
     @param uris String array of Spotify song URIs to be added to the given playlist
     */
    public void addToPlaylist(String playlistID, String[] uris) {
        try {
            clientAPI.addItemsToPlaylist(playlistID, uris).build().execute();
        }
        catch (IOException | SpotifyWebApiException | ParseException e) {
            ErrorHandler.warn("Could not add songs to spotify playlist", e.getStackTrace());
        }
    }

    /**
    Gets the playlists that the user has on their account.
    @return Array of PlaylistSimplified to allow search, iteration etc.
     */
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
