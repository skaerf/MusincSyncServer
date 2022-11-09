import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.HashMap;

public class MySQLInterface {

    private static final String username = "serverTest";
    private static final String password = "muchTestMoment";
    private static final Statement statement = null;
    private static Connection connection; //.createStatement()

    public static void connectDatabase() {

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
        }
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