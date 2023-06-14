package xyz.skaerf.MusincServer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class AccountManager {

    private static final ArrayList<Account> accountCache = new ArrayList<>();

    /**
    This exists because if an account is created with nothing other than the new xyz.skaerf.MusincServer.Account() method there may
    potentially be another account that exists with some of the same credentials.
    This method is designed to prevent that and therefore is required.
    Returns account if account was created successfully, null if was not created.
    Only time it would not be created would be if there are preexisting accounts with similar credentials.
     @return Object - instanceof Account if succeeded, instanceof String with reason for failure if failed
     @param account the Account instance that should be created
     */
    public static Object createNew(Account account) {
        ResultSet usernameResult = MySQLInterface.executeStatement("select username from users where username = '{0}'".replace("{0}", account.getUsername()));
        try {
            if (usernameResult != null) {
                if (usernameResult.next()) {
                    System.out.print("returning null user");
                    return "username";
                }
                ResultSet emailResult = MySQLInterface.executeStatement("select email from users where email = '{0}'".replace("{0}", account.getEmail()));
                if (emailResult != null) {
                    if (emailResult.next()) {
                        System.out.print("returning null email");
                        return "email";
                    }
                }
                else {
                    ErrorHandler.warn("emailResult was null, this typically means SQL connection does not exist");
                }
            }
            else {
                ErrorHandler.warn("usernameResult was null, this typically means SQL connection does not exist");
            }
        }
        catch (SQLException e) {
            ErrorHandler.warn("could not parse response from ResultSet upon requesting createNew account data", e.getStackTrace());
        }
        accountCache.add(account);
        return account;
    }

    /**
     * Gets the Account linked to a username or email
     * @return Account of username if found, null if not
     * @param username either the username or email (to allow logging in with either) of the Account to be found
     */
    public static Account getAccount(String username) {
        if (MySQLInterface.isConnected) {
            HashMap<String, String> userInfo = new HashMap<>();
            if (accountCache.size() != 0) for (Account account : accountCache) {
                if (account.getUsername() != null && account.getUsername().equalsIgnoreCase(username)) {
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
                userInfo.putIfAbsent("knownIPs", "localhost");
                Account account = new Account(userInfo.get("username"), userInfo.get("email"), userInfo.get("firstname"), userInfo.get("lastname"), null, new ArrayList<>(Arrays.asList(userInfo.get("knownIPs").split(":"))));
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
                if (account.getUsername().equalsIgnoreCase(username)) {
                    return account;
                }
            }
        }
        return null;
    }

    /**
     * Removes the given account from the server's Account cache
     * @param account account to be removed from the cache
     */
    public static void removeFromCache(Account account) {
        accountCache.remove(account);
    }
}
