package xyz.skaerf.MusincServer;

public class PassManager {

    /**
     * Checks the password database to see if the given password for a user is the correct password
     * @param account the account to be checked
     * @param password the password to be verified
     * @return true if pass is correct, otherwise false
     */
    public boolean checkPass(Account account, String password) {

        return false;
    }

    /**
     * Resets the given user's password
     * @param account the Account instance to have its password reset
     */
    public static void resetPassword(Account account) {
        // TODO add encryption system and passwords to accounts - maybe kept in a separate database for reasons of security?
    }
}
