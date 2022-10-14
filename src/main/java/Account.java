import java.util.ArrayList;

public class Account {

    private String username;
    private String email;
    private String firstname;
    private String surname;
    private ArrayList<String> knownIPs;


    public Account(String username, String email, String firstname, String surname, ArrayList<String> knownIPs) {

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
}
