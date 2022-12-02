package xyz.skaerf.MusincServer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class AccountManager {

    private static final ArrayList<Account> accountCache = new ArrayList<>();

    /*
    This exists because if an account is created with nothing other than the new xyz.skaerf.MusincServer.Account() method there may
    potentially be another account that exists with some of the same credentials.
    This method is designed to prevent that and therefore is required.
    Returns null if account was created successfully, a string with the reason if was not created.
    Only time it would not be created would be if there are preexisting accounts with similar credentials.
     */
    public static String createNew(Account account) {
        ResultSet usernameResult = MySQLInterface.executeStatement("select username from users where username = '{0}'".replace("{0}", account.getUsername()));
        if (usernameResult != null) {
            return "username";
        }
        ResultSet emailResult = MySQLInterface.executeStatement("select email from users where email = '{0}'".replace("{0}", account.getEmail()));
        if (emailResult != null) {
            return "email";
        }
        accountCache.add(account);
        return null;
    }

    public static void resetPassword(Account account) {
        // TODO add encryption system and passwords to accounts - maybe kept in a separate database for reasons of security?
    }

    /*
    Returns null if an account cannot be found under that username (or email if given string does not equal a username)
     */
    public static Account getAccount(String username) {
        if (MySQLInterface.isConnected) {
            HashMap<String, String> userInfo = new HashMap<>();
            for (Account account : accountCache) {
                if (account.getUsername().equalsIgnoreCase(username)) {
                    return account;
                }
            }
            ResultSet accountResult = MySQLInterface.executeStatement("select * from users where username = '{0}'".replace("{0}", username));
            try {
                if (accountResult != null) {
                    while (accountResult.next()) {
                        for (int i = 1; i <= accountResult.getMetaData().getColumnCount(); i++) {
                            String columnValue = accountResult.getString(i);
                            userInfo.put(accountResult.getMetaData().getColumnName(i), columnValue);
                        }
                    }
                }
                Account account = new Account(userInfo.get("username"), userInfo.get("email"), userInfo.get("firstname"), userInfo.get("surname"), null, new ArrayList<>(Arrays.asList(userInfo.get("knownIPs").split(":"))));
                accountCache.add(account);
                return account;
            }
            catch (SQLException e) {
                ErrorHandler.warn(e.getMessage(), e.getStackTrace());
            }
            accountResult = MySQLInterface.executeStatement("select * from users where email = '{0}'".replace("{0}", username));
            try {
                if (accountResult != null) {
                    while (accountResult.next()) {
                        for (int i = 1; i <= accountResult.getMetaData().getColumnCount(); i++) {
                            String columnValue = accountResult.getString(i);
                            userInfo.put(accountResult.getMetaData().getColumnName(i), columnValue);
                        }
                    }
                }
                return getAccount(userInfo.get("username"));
            }
            catch (SQLException e) {
                ErrorHandler.warn(e.getMessage(), e.getStackTrace());
            }
        }
        return null;
    }
}
