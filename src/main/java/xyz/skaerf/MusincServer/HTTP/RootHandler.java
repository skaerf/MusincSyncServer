package xyz.skaerf.MusincServer.HTTP;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.util.stream.Collectors;

public class RootHandler implements HttpHandler {

    /**
     * Handles Root requests for the web server.
     * @param he the exchange containing the request from the
     *                 client and used to send the response
     * @throws IOException if request cannot be handled
     */
    @Override
    public void handle(HttpExchange he) throws IOException {
        System.out.println("Root request received from "+he.getRemoteAddress()+" for "+he.getRequestURI());
        StringBuilder response = new StringBuilder("<h1>MusincSyncServer</h1>\n");
        he.sendResponseHeaders(200, response.length());
        OutputStream outStream = he.getResponseBody();
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("index.html");
        assert in != null;
        String s = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
        outStream.write(s.getBytes());
        outStream.close();
    }
}
