package xyz.skaerf.MusincServer.APIs;

import se.michaelthelin.spotify.SpotifyApi;

public class Spotify {

    private static SpotifyApi api;

    public static void initialiseAPILink() {
        api = new SpotifyApi.Builder()
                .setClientId("")
                .setClientSecret("")
                .setRedirectUri(null)
                .build();
    }
}
