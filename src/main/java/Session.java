import java.util.ArrayList;

public class Session {

    private Account hostUser;
    private ArrayList<Account> clientUsers;

    public Session(Account hostUser, ArrayList<Account> clientUsers) {
        this.hostUser = hostUser;
        this.clientUsers = clientUsers;
    }

    public ArrayList<Account> getClientUsers() {
        return clientUsers;
    }

    public Account getHostUser() {
        return hostUser;
    }
}
