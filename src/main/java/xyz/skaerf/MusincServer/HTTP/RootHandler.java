package xyz.skaerf.MusincServer.HTTP;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

public class RootHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange he) throws IOException {
        System.out.println("Root request received from "+he.getRemoteAddress()+" for "+he.getRequestURI());
        String ver = getClass().getPackage().getImplementationVersion();
        if (ver == null) {
            ver = "NON PRODUCTION - DEVELOPMENT BUILD";
        }
        String response = "<h1>MusincSyncServer</h1>\n" + "\n" + "<p>Node number: ?</p>\n" + "<p>Server version: " + ver + "</p>";
        he.sendResponseHeaders(200, response.length());
        OutputStream outStream = he.getResponseBody();
        outStream.write(response.getBytes());
        outStream.close();
    }
}
