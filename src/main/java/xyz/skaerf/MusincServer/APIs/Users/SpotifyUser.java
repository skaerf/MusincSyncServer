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

    private String accessToken;
    private SpotifyApi clientAPI;
    private final Account account;
    private URI userURI;

    private String currentID;
    private long progressMS;
    private long timeOfCall;
    private long curTrackLength;
    private boolean isPaused;

    /**
     * Instantiates a new SpotifyUser for a user - allows a Spotify account to be connected
     * @param account the account for which the SpotifyUser will be instantiated
     * @param isNew determines whether the account is a refresh of a previous instantiation
     *              or if it is a new creation without a refresh token
     */
    public SpotifyUser(Account account, boolean isNew) {
        if (isNew) this.userURI = Spotify.requestURI();
        this.account = account;
    }

    /**
     * Used to refresh previous Spotify API access using a refresh token.
     * @param refreshToken the token to use to grab a new access token
     * @return true if succeeded, otherwise false
     */
    public boolean refreshPastAccess(String refreshToken) {
        SpotifyApi newAPI = Spotify.instantiatePreviousAccess(refreshToken);
        if (newAPI != null) {
            this.clientAPI = newAPI;
            this.userURI = this.clientAPI.getRedirectURI();
            this.accessToken = this.clientAPI.getAccessToken();
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Get the refresh token of the SpotifyUser.
     * @return Spotify user's refresh token
     */
    public String getRefreshToken() {
        return clientAPI.getRefreshToken();
    }

    /**
     * Used to confirm the authentication code for a Spotify user. This allows the actual client user to be instantiated by
     * the server's Spotify login.
     * @param accessToken the access token to be confirmed
     * @return boolean of the result of the client authentication tokens being set for use
     */
    public boolean confirm(String accessToken) {
        this.accessToken = accessToken;
        clientAPI = Spotify.instantiateClientUser();
        return setAuthTokens();
    }

    /**
     * Gets the Spotify user's URI. Used for generating links used for authorisation.
     * @return the user's Spotify account URI
     */
    public URI getUserURI() {
        return this.userURI;
    }

    /**
     * Used internally by the class to set the authentication tokens from the user based on the class' authCode variable.
     * @return True if succeeded to grab auth tokens, false if not
     */
    private boolean setAuthTokens() {
        final AuthorizationCodeCredentials authorizationCodeCredentials;
        try {
            authorizationCodeCredentials = clientAPI.authorizationCode(accessToken).build().execute();
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
     * Used to pause the playback of the user's Spotify account. Throws an error to ErrorHandler if it is unable to do so.
     * @return true if successful, otherwise false
     */
    public boolean pausePlayback() {
        try {
            clientAPI.pauseUsersPlayback().build().execute();
            isPaused = true;
            return true;
        }
        catch (IOException | SpotifyWebApiException | ParseException e) {
            ErrorHandler.warn("Could not pause user playback", e.getStackTrace());
            return false;
        }
    }

    /**
     * Gets the user's currently playing Spotify song. Rate limited to once every 1.5 seconds to prevent spamming Spotify.
     * If a second requests is made within two seconds of the first, the same results will be returned.
     * @return a Track variable of the user's currently playing song.
     */
    public Track getCurrentlyPlaying() {
        if (currentID != null && System.currentTimeMillis() - Long.parseLong(currentID.split(":")[0]) <= 1500) {
            try {
                Track track = clientAPI.getTrack(currentID.split(":")[1]).build().execute();
                if (track != null) {
                    track = clientAPI.getTrack(currentID.split(":")[1]).build().execute();
                    return track;
                }
            }
            catch (IOException | SpotifyWebApiException | ParseException e) {
                ErrorHandler.warn("Could not get track", e.getStackTrace());
            }
        }
        try {
            CurrentlyPlaying curPlay = clientAPI.getUsersCurrentlyPlayingTrack().additionalTypes("track,episode").build().execute();
            if (curPlay.getIs_playing()) {
                Track track = clientAPI.getTrack(curPlay.getItem().getId()).build().execute();
                if (track == null) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(50);
                        track = clientAPI.getTrack(curPlay.getItem().getId()).build().execute();
                        this.currentID = System.currentTimeMillis()+":"+track.getId();
                        this.timeOfCall = curPlay.getTimestamp();
                        this.progressMS = curPlay.getProgress_ms();
                        this.isPaused = !curPlay.getIs_playing();
                        this.curTrackLength = track.getDurationMs();
                        return track;
                    }
                    catch (InterruptedException e) {
                        ErrorHandler.warn("Could not pause to wait for user's track to start playing", e.getStackTrace());
                    }
                }
                else {
                    this.currentID = System.currentTimeMillis() + ":" + track.getId();
                    this.timeOfCall = curPlay.getTimestamp();
                    this.progressMS = curPlay.getProgress_ms();
                    this.isPaused = !curPlay.getIs_playing();
                    this.curTrackLength = track.getDurationMs();
                    return track;
                }
            }
            else {
                TimeUnit.SECONDS.sleep(5);
                curPlay = clientAPI.getUsersCurrentlyPlayingTrack().additionalTypes("track,episode").build().execute();
                if (curPlay == null || !curPlay.getIs_playing()) return null;
                Track track = clientAPI.getTrack(curPlay.getItem().getId()).build().execute();
                if (track == null) return null;
                this.currentID = System.currentTimeMillis()+":"+track.getId();
                this.timeOfCall = curPlay.getTimestamp();
                this.progressMS = curPlay.getProgress_ms();
                this.isPaused = !curPlay.getIs_playing();
                this.curTrackLength = track.getDurationMs();
                return track;
            }
        }
        catch (IOException | SpotifyWebApiException | ParseException e) {
            ErrorHandler.warn("Could not get user's currently playing song", e.getStackTrace());
            return null;
        }
        catch (NullPointerException e) {
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
     * Returns the length of the most recently requested playing track in milliseconds.
     * @return current track length in ms
     */
    public long getCurrentTrackLength() {
        return this.curTrackLength;
    }

    /**
     * Gets the album cover as a URL (returned as a String) for the user's currently playing song.
     * getCurrentlyPlaying() MUST be called first. This is purely for rate limiting reasons - ends up being way too many requests
     * otherwise and results in program being inoperable.
     * @return the URL (in form of a String) for the user's currently playing song's album cover. Used for display on client.
     */
    public String getCurrentAlbumCover() {
        // the single reason this does not conform to everything else is because the API I am using does not allow me to grab album covers
        HttpClient client = HttpClient.newHttpClient();
        if (this.currentID != null) {
            String currID = this.currentID.split(":")[1];
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
            }
            catch (IOException | InterruptedException e) {
                ErrorHandler.warn("Could not send request to Spotify for album cover", e.getStackTrace());
                // TODO add the refresh token from the client in UPDATE_PLAYING requests and pass it to this function. if it can reconnect, run again, otherwise return null
                return null;
            }
            catch (org.json.simple.parser.ParseException e) {
                ErrorHandler.warn("Could not parse Spotify's response as JSON", e.getStackTrace());
                return null;
            }
        }
        return null;
    }

    /**
     * Skips the user's song to the next one.
     * @return true if successful, otherwise false
     */
    public boolean nextTrack() {
        try {
            clientAPI.skipUsersPlaybackToNextTrack().build().execute(); // TODO WHY DIDNT IT WORK I HATE MY PROJECT WHY DID I DO THIS TO MYSELF
            return true;
        }
        catch (IOException | SpotifyWebApiException | ParseException e) {
            ErrorHandler.warn("Could not skip user's track", e.getStackTrace());
            return false;
        }
    }

    /**
     * Rewinds to the user's previously played track.
     * @return true if successful, otherwise false
     */
    public boolean previousTrack() {
        try {
            clientAPI.skipUsersPlaybackToPreviousTrack().build().execute();
            return true;
        }
        catch (IOException | SpotifyWebApiException | ParseException e) {
            ErrorHandler.warn("Could not rewind to user's previous track", e.getStackTrace());
            return false;
        }
    }

    /**
     * Resumes the Spotify user's playback if it was paused.
     * Throws error to ErrorHandler if unable to resume.
     * @return true if successful, otherwise false
     */
    public boolean resumePlayback() {
        try {
            clientAPI.startResumeUsersPlayback().build().execute();
            isPaused = false;
            return true;
        }
        catch (IOException | SpotifyWebApiException | ParseException e) {
            ErrorHandler.warn("Could not start or resume user's playback", e.getStackTrace());
            return false;
        }
    }

    /**
     * Determines whether this Spotify user is currently playing music or not.
     * @return true if is paused, otherwise false
     */
    public boolean isPaused() {
        return this.isPaused;
    }

    /**
     * Creates a playlist on the user's behalf.
     * @param playlistName the name for the playlist
     * @return the playlist that is created, null if did not succeed.
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
     * Adds one or many songs to a given playlist via its Spotify ID.
     * Songs are received in a string list to allow simple iteration.
     * @param playlistID the Spotify ID of the playlist to be added to
     * @param uris String array of Spotify song URIs to be added to the given playlist
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
     * Gets the playlists that the user has on their account.
     * @return Array of PlaylistSimplified to allow search, iteration etc.
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
