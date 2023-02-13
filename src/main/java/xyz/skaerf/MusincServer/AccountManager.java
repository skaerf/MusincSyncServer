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
    Returns account if account was created successfully, null if was not created.
    Only time it would not be created would be if there are preexisting accounts with similar credentials.
     */
    public static Account createNew(Account account) {
        ResultSet usernameResult = MySQLInterface.executeStatement("select username from users where username = '{0}'".replace("{0}", account.getUsername()));
        try {
            if (usernameResult.next()) {
                System.out.print("returning null user");
                return null;
            }
            ResultSet emailResult = MySQLInterface.executeStatement("select email from users where email = '{0}'".replace("{0}", account.getEmail()));
            if (emailResult.next()) {
                System.out.print("returning null email");
                return null;
            }
        }
        catch (SQLException e) {
            ErrorHandler.warn("could not parse response from ResultSet upon requesting createNew account data", e.getStackTrace());
        }
        accountCache.add(account);
        return account;
    }

    public static void resetPassword(Account account) {
        // TODO add encryption system and passwords to accounts - maybe kept in a separate database for reasons of security?
    }

    /*
    Returns null if an account cannot be found under that username (or email if given string does not equal a username)
     */
    public static Account getAccount(String username) {
        if (MySQLInterface.isConnected) {
            System.out.println("yes");
            HashMap<String, String> userInfo = new HashMap<>();
            for (Account account : accountCache) {
                System.out.println(account.getUsername());
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
        else {
            ErrorHandler.warn("cannot check database for accounts, only live cache. account may still exist even if it does not get returned");
            for (Account account : accountCache) {
                System.out.println(account.getUsername());
                if (account.getUsername().equalsIgnoreCase(username)) {
                    return account;
                }
            }
        }
        return null;
    }
}
