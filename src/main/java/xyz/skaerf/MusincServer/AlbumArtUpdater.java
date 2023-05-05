package xyz.skaerf.MusincServer;

public class AlbumArtUpdater implements Runnable {

    private final Account userAccount;
    private final ClientHandler clientHandler;
    private String albumCover;
    private boolean runner = true;

    public AlbumArtUpdater(ClientHandler clientHandler, Account userAccount) {
        this.userAccount = userAccount;
        this.clientHandler = clientHandler;
    }

    @Override
    public void run() {
        while (runner) {
            String newCov = userAccount.getSpotifyUser().getCurrentAlbumCover();
            if (!newCov.equalsIgnoreCase(albumCover)) {
                System.out.println("is different cover");
                clientHandler.buffWriter.println(RequestArgs.UPDATE_PLAYING+newCov);
                albumCover = newCov;
            }
        }
    }

    public void kill() {
        runner = false;
    }
}
