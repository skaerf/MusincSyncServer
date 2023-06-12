package xyz.skaerf.MusincServer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class PassManager {

    /**
     * Checks the password database to see if the given password for a user is the correct password
     * @param account the account to be checked
     * @param password the password hash to be verified
     * @return true if pass is correct, otherwise false
     */
    public static boolean checkPass(Account account, String password) {
        String salt = "", hash = "";
        int userID = 0;
        ResultSet userResults = MySQLInterface.executeStatement("select userid from users where username = '"+account.getUsername()+"'");
        if (userResults != null) {
            try {
                while (userResults.next()) {
                    userID = userResults.getInt("userid");
                }
            }
            catch (SQLException e) {
                ErrorHandler.warn("Could not iterate through provided userResults from SQL server", e.getStackTrace());
                return false;
            }
        }
        else {
            return false;
        }
        ResultSet passResults = MySQLInterface.executeStatement("select salt,hash from passwords where userid = '"+userID+"'");
        if (passResults != null) {
            try {
                while (passResults.next()) {
                    salt = passResults.getString("salt");
                    hash = passResults.getString("hash");
                }
            }
            catch (SQLException e) {
                ErrorHandler.warn("Could not iterate through provided passResults from SQL server", e.getStackTrace());
            }
        }
        else {
            return false;
        }

        password = salt+password;
        if (hash == null) {
            ErrorHandler.warn("There is no password set for given user");
            return false;
        }
        return Objects.equals(getPassHash(password), hash);
    }

    /**
     * Re-hashes the provided password hash. Prevents anything other than the client side of things from knowing what the
     * actual password is and means that it is not transmitted without encryption. The result of this function is what is
     * actually stored in the password database, allowing for maximum security.
     * @param password the password hash to be re-hashed. Must start with a salt
     * @return a re-hashed (with salt) version of the provided password hash.
     */
    public static String getPassHash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] passHash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : passHash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }
        catch (NoSuchAlgorithmException e) {
            ErrorHandler.warn("MessageDigest claims that SHA-256 does not exist. Cannot hash passwords", e.getStackTrace());
        }
        return null;
    }

    /**
     * Resets the given user's password
     * @param account the Account instance to have its password reset
     */
    public static void resetPassword(Account account) {
        // TODO add encryption system and passwords to accounts - maybe kept in a separate database for reasons of security?
    }

    public static String generateSalt() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        StringBuilder sb = new StringBuilder();
        for (byte b : salt) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
