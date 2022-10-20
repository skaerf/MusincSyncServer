import java.util.ArrayList;

public class Account {

    private final String username;
    private final String email;
    private final String firstname;
    private final String surname;
    private Session currentSession;
    private final ArrayList<String> knownIPs;


    public Account(String username, String email, String firstname, String surname, Session currentSession, ArrayList<String> knownIPs) {
        this.username = username;
        this.email = email;
        this.firstname = firstname;
        this.surname = surname;
        this.currentSession = currentSession;
        this.knownIPs = knownIPs;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getSurname() {
        return surname;
    }

    public ArrayList<String> getKnownIPs() {
        return knownIPs;
    }

    public Session getSession() {
        return currentSession;
    }

    public void joinSession(Session session) {
        this.currentSession = session;
    }

    public void leaveSession() {
        this.currentSession.getClientUsers().remove(this);
    }

    public void createSession() {
        this.currentSession = new Session(this);
    }
}
