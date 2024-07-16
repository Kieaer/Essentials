package essential.achievements;

import java.sql.*;

public class DB {
    void connect() {
        try
                (Connection connection = DriverManager.getConnection("jdbc:sqlite:sample.db");
                        Statement statement = connection.createStatement();
                )
        {

            statement.setQueryTimeout(30);

            statement.executeUpdate("drop table if exists person");
            statement.executeUpdate("create table person (id integer, name string)");
            statement.executeUpdate("insert into person values(1, 'leo')");
            statement.executeUpdate("insert into person values(2, 'yui')");
            ResultSet rs = statement.executeQuery("select * from person");
            while(rs.next())
            {
                // read the result set
                System.out.println("name = " + rs.getString("name"));
                System.out.println("id = " + rs.getInt("id"));
            }
        }
        catch(SQLException e)
        {
            e.printStackTrace(System.err);
        }
    }

    void disconnect() {

    }


}
