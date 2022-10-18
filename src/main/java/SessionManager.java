import java.util.ArrayList;
import java.util.HashMap;

public class SessionManager {

    private static final HashMap<Account, ArrayList<Account>> sessionCache = new HashMap<>();

    /*
    clientUsers SHOULD be null upon creation - there should be no clients joining.
    Variable is only passable if it is absolutely required for whatever reason.
    Due to this, there is another function.
    Returns the session that is created
     */
    public static Session createNewSession(Account hostUser, ArrayList<Account> clientUsers) {
        Session session = new Session(hostUser, clientUsers);
        sessionCache.put(hostUser, clientUsers);
        return session;
    }

    /*
    clientUsers SHOULD be null upon creation - there should be no clients joining.
    Variable is only passable if it is absolutely required for whatever reason.
    Due to this, there is another function.
    Returns the session that is created
     */
    public static Session createNewSession(Account hostUser) {
        Session session = new Session(hostUser, null);
        sessionCache.put(hostUser, null);
        return session;
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
                sessionCache.get(hostUser).add(clientUser);
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
