package xyz.skaerf.MusincServer.APIs.Users;

import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import xyz.skaerf.MusincServer.APIs.Spotify;
import xyz.skaerf.MusincServer.Account;
import xyz.skaerf.MusincServer.ErrorHandler;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.util.Scanner;

public class SpotifyUser {

    private final String authCode;
    private final SpotifyApi clientAPI;
    private final Account account;

    public SpotifyUser(Account account) {
        URI userURI = Spotify.requestURI();
        System.out.println(userURI);
        Scanner scanner = new Scanner(System.in);
        authCode = scanner.nextLine();
        scanner.close();
        clientAPI = Spotify.instantiateClientUser();
        setAuthTokens();
        this.account = account;
    }

    private void setAuthTokens() {
        final AuthorizationCodeCredentials authorizationCodeCredentials;
        try {
            authorizationCodeCredentials = clientAPI.authorizationCode(authCode).build().execute();
            clientAPI.setAccessToken(authorizationCodeCredentials.getAccessToken());
            clientAPI.setRefreshToken(authorizationCodeCredentials.getRefreshToken());
            System.out.println("Client API for "+clientAPI.getCurrentUsersProfile().build().execute().getDisplayName()+" expires in: " + authorizationCodeCredentials.getExpiresIn());
        }
        catch (IOException | ParseException | SpotifyWebApiException e) {
            ErrorHandler.warn("could not generate authentication tokens for a user account", e.getStackTrace());
        }
    }

    public void pausePlayback() {
        try {
            clientAPI.pauseUsersPlayback().build().execute();
        }
        catch (IOException | SpotifyWebApiException | ParseException e) {
            ErrorHandler.warn("could not pause user playback", e.getStackTrace());
        }
    }

    public CurrentlyPlaying getCurrentlyPlaying() {
        try {
            return clientAPI.getUsersCurrentlyPlayingTrack().additionalTypes("track,episode").build().execute();
        }
        catch (IOException | SpotifyWebApiException | ParseException e) {
            ErrorHandler.warn("could not get user's currently playing song", e.getStackTrace());
        }
        return null;
    }

    public void resumePlayback() {
        try {
            clientAPI.startResumeUsersPlayback().build().execute();
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            ErrorHandler.warn("could not start or resume user's playback", e.getStackTrace());
        }
    }

    public Playlist createPlaylist(String playlistName) {
        try {
            for (PlaylistSimplified p : getPlaylists()) {
                if (p.getName().equalsIgnoreCase(playlistName)) {
                    ErrorHandler.warn("tried to create a playlist when one with the same name already exists");
                    return null;
                }
            }
            return clientAPI.createPlaylist(clientAPI.getCurrentUsersProfile().build().execute().getId(), playlistName).description("Playlist created automatically by Musinc! Songs are from your listening session on "+LocalDate.now().getDayOfMonth()+"/"+LocalDate.now().getMonthValue()+"/"+LocalDate.now().getYear()+" with "+account.getSession().getHostUser().getUsername()).build().execute();
        }
        catch (IOException | SpotifyWebApiException | ParseException e) {
            ErrorHandler.warn("could not create spotify playlist", e.getStackTrace());
        }
        return null;
    }

    public void addToPlaylist(String playlistID, String[] uris) {
        try {
            clientAPI.addItemsToPlaylist(playlistID, uris).build().execute();
        }
        catch (IOException | SpotifyWebApiException | ParseException e) {
            ErrorHandler.warn("could not add songs to spotify playlist", e.getStackTrace());
        }
    }

    public PlaylistSimplified[] getPlaylists() {
        try {
            return clientAPI.getListOfCurrentUsersPlaylists().build().execute().getItems();
        }
        catch (IOException | SpotifyWebApiException | ParseException e) {
            ErrorHandler.warn("could not get user's spotify playlists", e.getStackTrace());
        }
        return null;
    }

}
