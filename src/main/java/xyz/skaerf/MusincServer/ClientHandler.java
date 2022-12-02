package xyz.skaerf.MusincServer;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader buffReader;
    private BufferedWriter buffWriter;
    private Account userAccount;

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.buffWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.buffReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            userAccount = AccountManager.getAccount(this.buffReader.readLine());
            if (userAccount != null) {
                this.buffWriter.write("acc;"+ userAccount.getUsername()+":"+ userAccount.getFirstname()+":"+ userAccount.getSurname());
                Main.activeClients.add(this);
            }
            else {
                this.buffWriter.write("den;");
            }
        }
        catch (IOException e) {
            ErrorHandler.fatal("Could not instantiate client socket", e.getStackTrace());
            this.closeConnection();
        }
    }

    public void closeConnection() {
        Main.activeClients.remove(this);
        try {
            if (this.buffReader != null) {
                this.buffReader.close();
            }
            if (this.buffWriter != null) {
                this.buffWriter.close();
            }
            if (this.socket != null) {
                this.socket.close();
            }
        }
        catch (IOException e) {
            ErrorHandler.fatal("Could not close connection with client", e.getStackTrace());
        }
    }

    @Override
    public void run() {
        String msgFromClient;
        while (socket.isConnected()) {
            try {
                msgFromClient = this.buffReader.readLine();
                System.out.println(msgFromClient);
            }
            catch (IOException e) {
                ErrorHandler.fatal("Could not read message from client", e.getStackTrace());
                break;
            }
        }
    }

    private Account getAccount() {
        return this.userAccount;
    }
}
