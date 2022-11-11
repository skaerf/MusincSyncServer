package xyz.skaerf.MusincServer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

public class RootHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange he) throws IOException {
        System.out.println("Root request received from "+he.getRemoteAddress()+" for "+he.getRequestURI());
        String response = "<h1>How did you find this then?</h1>";
        he.sendResponseHeaders(200, response.length());
        OutputStream outStream = he.getResponseBody();
        outStream.write(response.getBytes());
        outStream.close();
    }
}
