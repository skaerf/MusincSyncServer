package xyz.skaerf.MusincServer;

import com.sun.net.httpserver.HttpServer;
import xyz.skaerf.MusincServer.APIs.Spotify;
import xyz.skaerf.MusincServer.HTTP.HTTPCreateAccountHandler;
import xyz.skaerf.MusincServer.HTTP.HTTPGetUser;
import xyz.skaerf.MusincServer.HTTP.JoinHandler;
import xyz.skaerf.MusincServer.HTTP.RootHandler;
import xyz.skaerf.MusincServer.Premieres.Premiere;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.*;

public class Musinc {

    static HashMap<String, String> configValues = new HashMap<>();
    /**
     * List of ClientHandler instantiations to show clients actively connected to the server
     */
    public static List<ClientHandler> activeClients = new ArrayList<>();
    /**
     * HashMap of strings and Sessions - the string is the ID of the session. Used mostly for active session lookup
     * as well as to represent all active sessions on the server.
     */
    public static HashMap<String, Session> activeSessions = new HashMap<>();
    static File configFile;
    static String localIP;
    static String publicIP;
    static String country;
    static HttpServer httpServer;
    static MainServer server;
    static int socketServerPort;
    static int premierePort;
    static File logFolder;
    static Object defaultAccount;

    /**
     * Musinc main method. Run when the jar is started
     * @param args args provided by the JVM
     */
    public static void main(String[] args) {
        System.out.println("Initialising MusincSyncServer");
        configFile = new File("config.txt");
        logFolder = new File("logs/");
        createLogFolder();
        createConfigFile();
        MySQLInterface.connectDatabase();
        Spotify.initialiseAPILink();
        getLocCredentials();

        defaultAccount = AccountManager.createNew(new Account("skaerf","seitna@outlook.com","Lawrence","Harrison",null, null));
        //defaultAccount.createSpotifyUser();
        //defaultAccount.createSession();
        //String[] uris = {"spotify:track:2IPKXJWPjC5AdDtOxTDkqA","spotify:track:45ewhNby8MgyWw6HmT7HKJ", "spotify:track:4ESWJepzBtY2lR9oZDYVaP"};
        //defaultAccount.getSpotifyUser().addToPlaylist(defaultAccount.getSpotifyUser().createPlaylist("hell yeah").getId(), uris);
        //defaultAccount.createDeezerUser();
        String id = Musinc.generateSessionID();
        System.out.println(id);
        activeSessions.put(id, new Premiere((Account) defaultAccount, LocalDateTime.now().plusHours(2)));

        socketServerPort = Integer.parseInt(configValues.get("socketPort"));
        premierePort = Integer.parseInt(configValues.get("premierePort"));
        if (configValues.get("webserver").equalsIgnoreCase("true")) {
            System.out.println("Initialising web server. USED ONLY FOR PREMIERES. SOCKETSERVER WILL STILL BE INITIALISED.");
            initialiseServer(premierePort);
        }
        System.out.println("Initialising server socket");
        initialiseServerSocket(socketServerPort);
    }

    /**
     * Creates the 'logs' folder that fatal and warn errors are contained in, if it does not exist already
     */
    private static void createLogFolder() {
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
    }

    /**
     * Generates a six-letter/number ID to be used as a Session identifier.
     * Runs a check to see if the ID already exists - extremely low chance of
     * this occurring but done just in case!
     * @return the ID that was generated
     */
    public static String generateSessionID() {
        StringBuilder res = new StringBuilder();
        String letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        Random rand = new Random();
        for (int i = 0; i < 6; i++) {
            res.append(letters.charAt(rand.nextInt(letters.length())));
        }
        while (activeSessions.containsKey(res.toString())) {
            res = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                res.append(letters.charAt(rand.nextInt(letters.length())));
            }
        }
        return res.toString();
    }

    /**
     * Searches through the active sessions and returns the one requested via the ID.
     * @param sessionID the ID of the Session that is being requested
     * @return the Session, null if it doesn't exist
     */
    public static Session getActiveSession(String sessionID) {
        return activeSessions.get(sessionID);
    }

    /**
     * Creates the config file if it does not exist already.
     */
    private static void createConfigFile() {
        if (!configFile.exists()) {
            try {
                if (configFile.createNewFile()) {
                    System.out.println("Config file successfully created");
                    System.out.println("Please enter the ports into the config file and restart the program");
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
    }

    /**
     * Initialises the web server.
     * Used only for the Premiere system - can no longer be used as a REST API.
     * @param port the port that the web server is to be started on
     */
    public static void initialiseServer(int port) {
        try {
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            System.out.println("Web server started successfully on port "+port);
            httpServer.createContext("/", new RootHandler());
            httpServer.createContext("/join", new JoinHandler());
            httpServer.createContext("/createaccount", new HTTPCreateAccountHandler());
            httpServer.createContext("/getuser", new HTTPGetUser());
            httpServer.setExecutor(null);
            httpServer.start();
            localIP = Inet4Address.getLocalHost().getHostAddress();
            System.out.println("http://"+ publicIP+":"+port);
        }
        catch (IOException e) {
            ErrorHandler.fatal("Could not start web server", e.getStackTrace());
        }

    }

    /**
     * Initialises the SocketServer using MainServer's instantiation method.
     * @param port the port that the SocketServer is to be started on
     */
    public static void initialiseServerSocket(int port) {
        server = new MainServer(port);
    }

    /**
     * Ends the main process of the server with an exit integer to show error.
     * Closes SocketServer and web server, if either are active.
     * @param exitInt error integer to be used as exit reason
     */
    public static void endProcess(int exitInt) {
        System.out.println("Ending process");
        if (configValues.get("webserver").equalsIgnoreCase("true")) {
            httpServer.stop(0);
        }
        else if (configValues.get("webserver").equalsIgnoreCase("false")) {
            if (!(server == null)) {
                server.closeServer();
            }
        }
        System.exit(exitInt);
    }

    /**
     * Gets the credentials of the server's physical location - the IP address
     * and the country in which it is being hosted.
     * Saves to variables to be used elsewhere and to prevent being called multiple times.
     */
    public static void getLocCredentials() {
        try {
            URLConnection con = new URL("https://api.myip.com").openConnection();
            InputStream response = con.getInputStream();
            String responseBody;
            try (Scanner scanner = new Scanner(response)) {
                responseBody = scanner.useDelimiter("\\A").next();
            }
            publicIP = responseBody.split(",")[0].split(":")[1].replace('"', ' ').trim();
            String result = responseBody.split(",")[1].split(":")[1];
            country = result.substring(1, result.length()-1);
        }
        catch (IOException e) {
            ErrorHandler.warn("Could not resolve IP API", e.getStackTrace());
        }
    }

    /**
     * Parses a query for the web server.
     * Mostly disused as web server is only used for the Premiere system
     * as the client programs cannot function without SocketServer running.
     * @param query the query to be parsed
     * @param parameters any parameters that come with the query
     */
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
