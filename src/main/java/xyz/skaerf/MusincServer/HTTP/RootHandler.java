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
        File indexHtmlFile = new File("index.html");
        if (indexHtmlFile.exists()) {
            try (InputStream in = new FileInputStream(indexHtmlFile)) {
                String s = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
                he.getResponseHeaders().set("Content-Type", "text/html");
                he.sendResponseHeaders(200, s.length());
                try (OutputStream outStream = he.getResponseBody()) {
                    outStream.write(s.getBytes());
                }
            }
        }
        else {
            // If index.html is not found, return a 404 error
            String response = "File not found";
            he.sendResponseHeaders(404, response.length());
            try (OutputStream outStream = he.getResponseBody()) {
                outStream.write(response.getBytes());
            }
        }
        }
    }
