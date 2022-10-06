import java.sql.*;

public class MySQLInterface {

    private static String username = "serverTest";
    private static String password = "muchTestMoment";
    private static Statement statement;

    public static boolean connectDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            Connection connection = DriverManager.getConnection("jdbc:mysql://home.skaerf.xyz:2291/usrs", username, password);
            statement = connection.createStatement();
            System.out.println("Successfully connected to database");
            //ResultSet resultSet = statement.executeQuery("select * from *");
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static String getUsername(String username) {
        ResultSet resSet;
        try {
            resSet = statement.executeQuery("select * from users where username = {0}".replace("{0}", username));
        }
        catch (SQLException e) {
            System.out.println("Username request could not be processed by MySQL");
            return null;
        }
        return null;
    }
}
//ass