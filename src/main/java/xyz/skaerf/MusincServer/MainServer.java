package xyz.skaerf.MusincServer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class MainServer {

    private ServerSocket serSoc;
    private final int port;

    /**
    Instantiates a new MainServer with the given port.
    Runs on main thread as it is the main process of the server.
     @param port port that the MainServer will run on
     */
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

    /**
    Starts MainServer's SocketServer to begin listening for connections
    from clients.
     */
    private void startServer() {
        System.out.println("Socket server started successfully on port "+port);
        System.out.println("Hosted from "+ Musinc.country+" at "+ Musinc.publicIP+":"+port);
        FileReader blacklist = null;
        try {
            blacklist = new FileReader("blacklist.txt");
        }
        catch (FileNotFoundException e) {
            ErrorHandler.warn("IP blacklist file not found");
        }
        try {
            while (!serSoc.isClosed()) {
                Socket socket = serSoc.accept();
                System.out.println("Client attempting to connect on "+socket.getInetAddress().getHostAddress());
                if (blacklist != null) {
                    BufferedReader br = new BufferedReader(blacklist);
                    String line;
                    boolean blocked = false;
                    while ((line = br.readLine()) != null) if (line.equals(socket.getInetAddress().getHostAddress())) {
                        System.out.println("Piss off Russians");
                        blocked = true;
                    }
                    if (blocked) continue;
                }
                ClientHandler clientHandler = new ClientHandler(socket);
                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        }
        catch (IOException e) {
            ErrorHandler.warn("Client could not connect to server", e.getStackTrace());
        }
    }

    /**
    Closes the MainServer's SocketServer process.
     */
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
