package xyz.skaerf.MusincServer.HTTP;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import xyz.skaerf.MusincServer.Account;
import xyz.skaerf.MusincServer.AccountManager;
import xyz.skaerf.MusincServer.Main;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;

public class HTTPGetUser implements HttpHandler {
    @Override
    public void handle(HttpExchange he) throws IOException {
        HashMap<String, Object> parameters = new HashMap<>();
        URI requestedUri = he.getRequestURI();
        String[] request = requestedUri.toString().split("\\?")[1].split("&");
        HashMap<String, String> requestInfo = new HashMap<>();
        for (String str : request) {
            String[] split = str.split("=");
            requestInfo.put(split[0], split[1]);
        }
        String response = "Request received\n\n\n"+ requestInfo;
        Account userAccount = AccountManager.getAccount(requestInfo.get("username"));
        if (userAccount == null) {
            response = response + "\n\n\nRESULT:null";
        }
        else {
            response = response + "\n\n\nRESULT:";
        }
        Main.parseQuery(requestedUri.getRawQuery(), parameters);
        he.sendResponseHeaders(200, response.length());
        OutputStream outStream = he.getResponseBody();
        outStream.write(response.getBytes());
        outStream.close();
    }
}
