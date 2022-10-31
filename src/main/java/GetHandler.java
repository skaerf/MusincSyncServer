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
        String[] request = requestedUri.toString().split("\\?")[1].split("&");
        HashMap<String, String> requestInfo = new HashMap<>();
        for (String str : request) {
            String[] split = str.split("=");
            requestInfo.put(split[0], split[1]);
        }
        String response = "Request received\n\n\n"+ requestInfo;
        // TODO EXECUTE REQUESTS
        if (requestInfo.get("task").equalsIgnoreCase("createAccount")) {
            // TODO KNOWN IPS SHOULD NOT BE NULL
            String creationResponse = AccountManager.createNew(new Account(requestInfo.get("username"), requestInfo.get("email"), requestInfo.get("firstname"), requestInfo.get("surname"), null, null));
            if (creationResponse != null) {
                response = response + "\n\n\nRESULT="+creationResponse;
            }
            else {
                response = response = "\n\n\nRESULT=successful";
            }
        }
        Main.parseQuery(requestedUri.getRawQuery(), parameters);
        he.sendResponseHeaders(200, response.length());
        OutputStream outStream = he.getResponseBody();
        outStream.write(response.getBytes());
        outStream.close();
    }
}
