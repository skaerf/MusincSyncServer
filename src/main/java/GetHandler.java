import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;

public class GetHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange he) throws IOException {
        HashMap<String, Object> parameters = new HashMap<>();
        URI requestedUri = he.getRequestURI();
        String query = requestedUri.getRawQuery();
        Main.parseQuery(query, parameters);

        StringBuilder response = new StringBuilder();
        for (String key : parameters.keySet())
            response.append(key).append(" = ").append(parameters.get(key)).append("\n");
        he.sendResponseHeaders(200, response.length());
        OutputStream outStream = he.getResponseBody();
        outStream.write(response.toString().getBytes());
        outStream.close();
    }
}
