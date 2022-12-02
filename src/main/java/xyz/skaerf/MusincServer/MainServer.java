package xyz.skaerf.MusincServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MainServer {

    private ServerSocket serSoc;
    private final int port;

    public MainServer(int port) {
        this.port = port;
        try {
            serSoc = new ServerSocket(port);
            startServer();
        }
        catch (IOException e) {
            ErrorHandler.fatal("ServerSocket could not be initialised", e.getStackTrace());
        }
    }

    private void startServer() {
        System.out.println("Socket server started successfully on port "+port);
        System.out.println(Main.getIP()+":"+port);
        try {
            while (!serSoc.isClosed()) {
                Socket socket = serSoc.accept();
                System.out.println("Client attempting to connect on "+socket.getInetAddress());
                ClientHandler clientHandler = new ClientHandler(socket);
                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        }
        catch (IOException e) {
            ErrorHandler.warn("Client could not connect to server", e.getStackTrace());
        }
    }

    public void closeServer() {
        try {
            if (serSoc != null) {
                serSoc.close();
            }
        }
        catch (IOException e) {
            ErrorHandler.warn("Server socket could not be closed correctly", e.getStackTrace());
        }
    }
}
