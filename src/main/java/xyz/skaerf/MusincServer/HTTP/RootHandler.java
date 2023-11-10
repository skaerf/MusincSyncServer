package xyz.skaerf.MusincServer.HTTP;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Scanner;

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
        File index = new File("index.html");
        if (index.exists()) {
            Scanner scanner = new Scanner(index);
            response = new StringBuilder();
            while (scanner.hasNextLine()) {
                response.append(scanner.nextLine()).append("\n");
            }
        }
        outStream.write(response.toString().getBytes());
        outStream.close();
    }
}
