package xyz.skaerf.MusincServer.APIs.Users;

import se.michaelthelin.spotify.SpotifyApi;
import xyz.skaerf.MusincServer.Account;

public class SpotifyUser {

    SpotifyApi userAPI;
    Account account;

    public SpotifyUser(Account account) {
        this.account = account;
        userAPI = new SpotifyApi.Builder()
                .setAccessToken("")
                .build();
    }
}
