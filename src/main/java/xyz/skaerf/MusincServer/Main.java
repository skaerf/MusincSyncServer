package xyz.skaerf.MusincServer;

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
    static File logFolder;

    public static void main(String[] args) {
        System.out.println("Initialising MusincSyncServer");
        configFile = new File("config.txt");
        logFolder = new File("logs/");
        if (!logFolder.exists()) {
            if (logFolder.mkdir()) {
                System.out.println("Logs directory created successfully");
                ErrorHandler.logDir = true;
            }
            else {
                System.out.println("Logs directory could not be created. Logs will not be created upon error");
                ErrorHandler.logDir = false;
            }
        }
        else {
            ErrorHandler.logDir = true;
        }
        if (!configFile.exists()) {
            try {
                if (configFile.createNewFile()) {
                    System.out.println("Config file successfully created");
                    System.out.println("Please enter a port into the config file and restart the program");
                    System.exit(0);
                }
            }
            catch (IOException e) {
                ErrorHandler.fatal("Could not create config file, please allow access for file creation", e.getStackTrace());
            }
        }
        else {
            try {
                BufferedReader br = new BufferedReader(new FileReader(configFile));
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
        MySQLInterface.connectDatabase();
        port = Integer.parseInt(configValues.get("port"));
        initialiseServer(port);
    }

    public static void initialiseServer(int port) {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            System.out.println("Web server started successfully on port "+port);
            server.createContext("/", new RootHandler());
            server.createContext("/get", new GetHandler());
            server.createContext("/createaccount", new HTTPCreateAccountHandler());
            server.createContext("/getuser", new HTTPGetUser());
            server.setExecutor(null);
            server.start();
            localIP = Inet4Address.getLocalHost().getHostAddress();
            System.out.println("http://"+getIP()+":"+port);
        }
        catch (IOException e) {
            ErrorHandler.fatal("Could not start web server", e.getStackTrace());
        }

    }

    public static String getIP() {
        try {
            URLConnection con = new URL("https://api.myip.com").openConnection();
            InputStream response = con.getInputStream();
            String responseBody;
            try (Scanner scanner = new Scanner(response)) {
                responseBody = scanner.useDelimiter("\\A").next();
                System.out.println(responseBody);
            }
            return responseBody.split(",")[0].split(":")[1].replace('"', ' ').trim();
        }
        catch (IOException e) {
            ErrorHandler.fatal("Could not resolve IP API", e.getStackTrace());
        }
        return null;
    }

    public static void parseQuery(String query, HashMap<String, Object> parameters) {
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] param = pair.split("=");
                String key = null;
                String value = null;
                try {
                    if (param.length > 0) {
                        key = URLDecoder.decode(param[0],
                                System.getProperty("file.encoding"));
                    }
                    if (param.length > 1) {
                        value = URLDecoder.decode(param[1],
                                System.getProperty("file.encoding"));
                    }
                }
                catch (UnsupportedEncodingException e) {
                    ErrorHandler.fatal("Could not respond to HTTP request", e.getStackTrace());
                }
                if (parameters.containsKey(key)) {
                    Object obj = parameters.get(key);
                    if (obj instanceof List<?>) {
                        List<String> values;
                        values = (List<String>) obj;
                        values.add(value);
                    }
                    else if (obj instanceof String) {
                        List<String> values = new ArrayList<>();
                        values.add((String) obj);
                        values.add(value);
                        parameters.put(key, values);
                    }
                }
                else {
                    parameters.put(key, value);
                }
            }
        }
    }
}
