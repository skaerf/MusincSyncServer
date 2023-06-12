package xyz.skaerf.MusincServer;

import java.sql.*;

public class MySQLInterface {

    private static final String username = "serverTest";
    private static final String password = "muchTestMoment";
    private static Connection connection;

    public static boolean isConnected = true;

    /**
    Initialises the connection to the MySQL database.
     */
    public static void connectDatabase() {
        try {
            connection = DriverManager.getConnection("jdbc:mysql://home.skaerf.xyz:2291/usrs", username, password);
            if (connection.isValid(50)) {
                System.out.println("Successfully connected to database");
                isConnected = true;
            }
        }
        catch (SQLException e) {
            isConnected = false;
            ErrorHandler.warn("Could not connect to database", e.getStackTrace());
        }
    }


    /**
    Executes a given SQL statement, provided in String format.
    @return the ResultSet that the server provides, null if empty.
     @param sqlString the SQL statement to be executed
     */
    public static ResultSet executeStatement(String sqlString) {
        ResultSet resSet;
        try {
            if (connection.isValid(50)) {
                Statement statement = connection.createStatement();
                resSet = statement.executeQuery(sqlString);
                return resSet;
            }
        }
        catch (SQLSyntaxErrorException e) {
            e.printStackTrace();
            ErrorHandler.warn("User may have attempted to inject SQL", e.getStackTrace());
        }
        catch (SQLException e) {
            e.printStackTrace();
            ErrorHandler.fatal("Could not execute SQL statement", e.getStackTrace());
        }
        return null;
    }

    /**
     * Executes the provided SQL statement.
     * Very similar to executeStatement(), however it does not return a ResultSet.
     * Purely for statements that do not return data, such as inserting into tables or deletion.
     * @param sqlString the SQL update string to be executed
     */
    public static void executeUpdate(String sqlString) {
        try {
            if (connection.isValid(50)) {
                Statement statement = connection.createStatement();
                statement.executeUpdate(sqlString);
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
            ErrorHandler.fatal("Could not execute SQL update", e.getStackTrace());
        }
    }
}
//ass