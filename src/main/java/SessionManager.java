import java.util.ArrayList;
import java.util.HashMap;

public class SessionManager {

    private static HashMap<Account, ArrayList<Account>> sessionCache = new HashMap<>();

    public static void createNewSession(Account hostUser) {
        Session session = new Session(hostUser);
        // TODO add session to sessionCache
    }

    /*
    Can return false under two conditions:
     - The session requested does not exist
     - The user is already in the session
     */
    public static boolean addClientToSession(Account hostUser, Account clientUser) {
        ArrayList<Account> session = sessionCache.get(hostUser);
        boolean accountSearch = false;
        if (session != null) {
            for (Account currentSessionClients : session) {
                if (currentSessionClients == clientUser) {
                    accountSearch = true;
                    break;
                }
            }
            if (accountSearch) {
                session.add(clientUser);
                return true;
            }
            else {
                return false;
            }
        }
        return false;
    }

    public static void removeClientFromSession(Account clientUser) {

    }

    public static void terminateSession(Session session) {

    }
}
