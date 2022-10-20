import java.util.ArrayList;

public class Session {

    private final Account hostUser;
    private final ArrayList<Account> clientUsers;

    public Session(Account hostUser) {
        this.hostUser = hostUser;
        this.clientUsers = null;
    }

    public ArrayList<Account> getClientUsers() {
        return clientUsers;
    }

    public Account getHostUser() {
        return hostUser;
    }
}
