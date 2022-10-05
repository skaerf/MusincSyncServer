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
}
//ass