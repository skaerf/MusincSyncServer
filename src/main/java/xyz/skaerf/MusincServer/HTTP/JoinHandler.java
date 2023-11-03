package xyz.skaerf.MusincServer.HTTP;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import xyz.skaerf.MusincServer.Musinc;
import xyz.skaerf.MusincServer.Premieres.Premiere;
import xyz.skaerf.MusincServer.Session;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class JoinHandler implements HttpHandler {

    /**
     * Handles the Join Session requests for the premiere server.
     * @param he the exchange containing the request from the
     *                 client and used to send the response
     * @throws IOException if the request cannot be handled
     */
    @Override
    public void handle(HttpExchange he) throws IOException {
        URI requestedUri = he.getRequestURI();
        String[] request = requestedUri.toString().split("\\?")[1].split("&");
        HashMap<String, String> requestInfo = new HashMap<>();
        for (String str : request) {
            String[] split = str.split("=");
            requestInfo.put(split[0], split[1]);
        }
        String response = "<h1>MusincServer</h1><br><br><br><p>Request received</p><br><br><br><p>"+ requestInfo + "</p><br><br>";
        Session session = Musinc.activeSessions.get(requestInfo.get("c"));
        if (session == null) {
            response = response + "<p>Session with given code could not be found</p>";
        }
        else {
            if (!(session instanceof Premiere)) {
                response = response + "<p>Provided code is not for a Premiere, please join normally through the Musinc client.</p>";
            }
            else {
                Premiere premiere = (Premiere) session;
                if (premiere.getClientUsers() != null) {
                    response = response + "<p>Session contains " + premiere.getClientUsers().size() + " people.</p><br>" +
                            "<p>The Premiere starts at "+premiere.getPremiereTime().format(DateTimeFormatter.ISO_TIME)+" on "
                            + premiere.getPremiereTime().format(DateTimeFormatter.ISO_DATE)+".</p>";
                }
                else {
                    response = response + "<p>The Premiere starts at "+premiere.getPremiereTime().format(DateTimeFormatter.ISO_TIME)+" on "
                            + premiere.getPremiereTime().format(DateTimeFormatter.ISO_DATE)+".</p>";
                }
            }
        }
        he.sendResponseHeaders(200, response.length());
        OutputStream outStream = he.getResponseBody();
        outStream.write(response.getBytes());
        outStream.close();
    }
}
