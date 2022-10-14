import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.HashMap;

public class MySQLInterface {

    private static String username = "serverTest";
    private static String password = "muchTestMoment";
    private static Statement statement;
    private static Connection connection; //.createStatement()

    public static boolean connectDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            if (!Inet4Address.getLocalHost().getHostAddress().equalsIgnoreCase("192.168.56.1")) {
                connection = DriverManager.getConnection("jdbc:mysql://home.skaerf.xyz:2291/usrs", username, password);
                System.out.println("Successfully connected to database");
                //ResultSet resultSet = statement.executeQuery("select * from *");
            }
            else {
                System.out.println("Not going to attempt a connection to database due to MySQL access being blocked by institution network");
                System.out.println("Notice some features will be unavailable and/or fire errors due to this");
            }
        } catch (SQLException | UnknownHostException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static HashMap<String, String> getUser(String username) {
        ResultSet resSet;
        HashMap<String, String> userInfo = new HashMap<>();
        try {
            resSet = statement.executeQuery("select * from users where username = '{0}'".replace("{0}", username));
        }
        catch (SQLException e) {
            System.out.println("Username request could not be processed by MySQL");
            return null;
        }
        if (resSet != null) {
            try {
                while (resSet.next()) {
                    for (int i = 1; i <= resSet.getMetaData().getColumnCount(); i++) {
                        if (i > 1) System.out.print(",  ");
                        String columnValue = resSet.getString(i);
                        userInfo.put(resSet.getMetaData().getColumnName(i), columnValue);
                    }
                }
            }
            catch (SQLException e) {
                e.printStackTrace();
                System.out.println("System could not iterate through MySQL response");
            }
        }
        return userInfo;
    }

    public static ResultSet executeStatement(String sqlString) {
        ResultSet resSet;
        try {
            resSet = statement.executeQuery(sqlString);
            return resSet;
        }
        catch (SQLException e) {
            return null;
        }
    }
}
//ass