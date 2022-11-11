package xyz.skaerf.MusincServer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class HTTPCreateAccountHandler implements HttpHandler {

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
        // TODO KNOWN IPS SHOULD NOT BE NULL - parse arraylist from string (string from requestInfo should contain at least the ip that the request is coming from which in worst case can be sent by the client
        ArrayList<String> knownIPs = new ArrayList<>();
        Collections.addAll(knownIPs, requestInfo.get("knownIPs").split(":"));
        String creationResponse = AccountManager.createNew(new Account(requestInfo.get("username"), requestInfo.get("email"), requestInfo.get("firstname"), requestInfo.get("surname"), null, knownIPs));
        if (creationResponse != null) {
            response = response + "\n\n\nRESULT="+creationResponse;
        }
        else {
            response = response + "\n\n\nRESULT=successful";
        }
        Main.parseQuery(requestedUri.getRawQuery(), parameters);
        he.sendResponseHeaders(200, response.length());
        OutputStream outStream = he.getResponseBody();
        outStream.write(response.getBytes());
        outStream.close();
    }
}
