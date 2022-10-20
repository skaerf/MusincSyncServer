import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class AccountManager {

    private static final ArrayList<Account> accountCache = new ArrayList<>();

    /*
    This exists because if an account is created with nothing other than the new Account() method there may
    potentially be another account that exists with some of the same credentials.
    This method is designed to prevent that and therefore is required.
    Returns true if account was created successfully, false if was not created.
    Only time it would not be created would be if there are preexisting accounts with similar credentials.
     */
    public static boolean createNew(Account account) {
        // check account with same/some same details does not exist, add account to database
        return false;
    }

    public static void resetPassword(Account account) {
        // TODO add encryption system and passwords to accounts - maybe kept in a separate database for reasons of security?
    }

    /*
    Returns null if an account cannot be found in cache under that username
     */
    public static Account getAccountForceFromCache(String username) {
        for (Account account : accountCache) {
            if (account.getUsername().equalsIgnoreCase(username)) {
                return account;
            }
        }
        return null;
    }

    /*
    Returns null if an account cannot be found in database under that username
     */
    public static Account getAccountForceFromDatabase(String username) {
        ResultSet accountResult = MySQLInterface.executeStatement("select * from users where username = '{0}'".replace("{0}", username));
        try {
            if (accountResult != null) {
                while (accountResult.next()) {
                    for (int i = 1; i <= accountResult.getMetaData().getColumnCount(); i++) {
                        String columnValue = accountResult.getString(i);
                        //userInfo.put(accountResult.getMetaData().getColumnName(i), columnValue);
                    }
                }
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            System.out.println("System could not iterate through MySQL response");
        }
        return null;  //RETURN ACCOUNT
    }

    /*
    Returns null if an account cannot be found under that username
     */
    public static Account getAccount(String username) {
        for (Account account : accountCache) {
            if (account.getUsername().equalsIgnoreCase(username)) {
                return account;
            }
        }
        ResultSet accountResult = MySQLInterface.executeStatement("select * from users where username = '{0}'".replace("{0}", username));
        try {
            HashMap<String, String> userInfo = new HashMap<>();
            if (accountResult != null) {
                while (accountResult.next()) {
                    for (int i = 1; i <= accountResult.getMetaData().getColumnCount(); i++) {
                        String columnValue = accountResult.getString(i);
                        userInfo.put(accountResult.getMetaData().getColumnName(i), columnValue);
                    }
                }
            }
            addAccountToCache(new Account(userInfo.get("username"), userInfo.get("email"), userInfo.get("firstname"), userInfo.get("surname"), null, null));
            // TODO KNOWN IPS SHOULD NOT BE NULL - REQUIRES OF CONVERSION FROM ARRAYLIST TO JSON FOR UPLOAD TO DATABASE
        }
        catch (SQLException e) {
            e.printStackTrace();
            System.out.println("System could not iterate through MySQL response");
        }
        return null;
    }

    /*
    Returns null if an account cannot be found under that email
     */
    public static Account getAccountByEmail(String email) {
        // TODO pull account from database using email
        return null;
    }

    private static void addAccountToCache(Account account) {
        accountCache.add(account);
    }
}
