package xyz.skaerf.MusincServer;

import java.sql.*;

public class MySQLInterface {

    private static final String username = "serverTest";
    private static final String password = "muchTestMoment";
    private static Connection connection;

    public static boolean isConnected = true;

    public static void connectDatabase() {
        try {
            connection = DriverManager.getConnection("jdbc:mysql://home.skaerf.xyz:2291/usrs", username, password);
            if (connection.isValid(50)) {
                System.out.println("Successfully connected to database");
            }
        }
        catch (SQLException e) {
            isConnected = false;
            ErrorHandler.warn("Could not connect to database", e.getStackTrace());
        }
    }

    public static ResultSet executeStatement(String sqlString) {
        ResultSet resSet;
        try {
            if (connection != null) {
                Statement statement = connection.createStatement();
                resSet = statement.executeQuery(sqlString);
                return resSet;
            }
            else {
                return null;
            }
        }
        catch (SQLException e) {
            ErrorHandler.fatal("Could not execute SQL statement", e.getStackTrace());
            return null;
        }
    }
}
//ass