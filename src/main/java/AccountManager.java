import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AccountManager {

    private static ArrayList<Account> accountCache = new ArrayList<>();

    public static void createNew(String originClient, Account account) {
        // check account with same/some same details does not exist, add account to database

    }

    public static void resetPassword(Account account) {

    }

    public static Account getAccountForceFromCache(String username) {
        for (Account account : accountCache) {
            if (account.getUsername().equalsIgnoreCase(username)) {
                return account;
            }
        }
        return null;
    }

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
            addAccountToCache(new Account(userInfo.get("username"), userInfo.get("email"), userInfo.get("firstname"), userInfo.get("surname"), null));
            // TODO KNOWN IPS SHOULD NOT BE NULL - REQUIRES OF CONVERSION FROM ARRAYLIST TO JSON FOR UPLOAD TO DATABASE
        }
        catch (SQLException e) {
            e.printStackTrace();
            System.out.println("System could not iterate through MySQL response");
        }
        return null;
    }

    public static Account getAccountByEmail(String email) {
        return null;
    }

    public static void addAccountToCache(Account account) {
        accountCache.add(account);
    }
}
