import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Main {

    HashMap<String, String> configValues = new HashMap<>();
    static String localIP;
    static HttpServer server;
    static int port;

    public static void main(String[] args) {
        System.out.println("Initialising MusincSyncServer");
        port = 1905;
        File configFile = new File("config.txt");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            }
            catch (IOException e) {
                System.out.println("Could not create config file, please allow access for file creation");
                // kill process?
            }
        }
        else {
            // READ HASHMAP FILE INTO MEMORY - YOUVE DONE THIS BEFORE, DONT BE STUPID
        }
        initialiseServer(port);
    }

    public static void initialiseServer(int port) {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            System.out.println("Server started successfully on port "+port);
            server.createContext("/", new RootHandler());
            server.setExecutor(null);
            server.start();
            localIP = Inet4Address.getLocalHost().getHostAddress();
            System.out.println("http://"+Inet4Address.getLocalHost().getHostAddress()+":"+port);
        }
        catch (IOException e) {
            System.out.println(e);
        }

    }

    public static void parseQuery(String query, HashMap<String, Object> parameters) throws UnsupportedEncodingException {
        if (query != null) {
            String[] pairs = query.split("[&]");
            for (String pair : pairs) {
                String[] param = pair.split("[=]");
                String key = null;
                String value = null;
                if (param.length > 0) {
                    key = URLDecoder.decode(param[0],
                            System.getProperty("file.encoding"));
                }

                if (param.length > 1) {
                    value = URLDecoder.decode(param[1],
                            System.getProperty("file.encoding"));
                }

                if (parameters.containsKey(key)) {
                    Object obj = parameters.get(key);
                    if (obj instanceof List<?>) {
                        List<String> values = (List<String>) obj;
                        values.add(value);

                    } else if (obj instanceof String) {
                        List<String> values = new ArrayList<String>();
                        values.add((String) obj);
                        values.add(value);
                        parameters.put(key, values);
                    }
                } else {
                    parameters.put(key, value);
                }
            }
        }
    }
}
