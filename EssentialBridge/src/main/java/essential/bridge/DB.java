package essential.bridge;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.util.Arrays;
import java.util.NoSuchElementException;

import static essential.core.Main.root;

public class DB {
    void load() {
        try {
            Class.forName(
                    "org.sqlite.JDBC",
                    true,
                    new URLClassLoader(
                            new URL[]{Arrays.stream(root.child("drivers/").list()).filter(name -> name.name().contains("sqlite")).findFirst().get().file().toURI().toURL()},
                            this.getClass().getClassLoader()
                    )
            );
        } catch (MalformedURLException | ClassNotFoundException | NoSuchElementException e) {
            throw new RuntimeException(e);
        }
    }

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
