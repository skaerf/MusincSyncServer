import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class Main {

    static HashMap<String, String> configValues = new HashMap<>();
    static File configFile;
    static String localIP;
    static HttpServer server;
    static int port;

    public static void main(String[] args) throws SocketException, FileNotFoundException {
        System.out.println("Initialising MusincSyncServer");
        System.out.println(NetworkInterface.getNetworkInterfaces());
        port = 1905;
        configFile = new File("config.txt");
        if (!configFile.exists()) {
            try {
                if (configFile.createNewFile()) {
                    System.out.println("Config file successfully created");
                }
            }
            catch (IOException e) {
                System.out.println("Could not create config file, please allow access for file creation");
                // TODO kill process?
            }
        }
        else {
            BufferedReader br = new BufferedReader(new FileReader(configFile));
            try {
                String line = br.readLine();
                while (line != null) {
                    String key = line.split(":")[0];
                    String value = line.split(":")[1];
                    configValues.put(key,value);
                    line = br.readLine();
                }
                System.out.println("Config info successfully loaded");
            }
            catch (IOException e) {
                System.out.println("Config info could not be loaded in");
            }

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

            System.out.println("http://"+localIP+":"+port);
        }
        catch (IOException e) {
            e.printStackTrace();
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
                        List<String> values = new ArrayList<>();
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
