package essentials.special;

import arc.Core;
import arc.files.Fi;

import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import static essentials.Global.URLDownload;
import static essentials.Global.nbundle;

public class External implements Driver {
    private boolean tried = false;
    private List<String> drivers = new ArrayList<>(Arrays.asList("org.mariadb.jdbc.Driver","org.postgresql.Driver","org.sqlite.JDBC"));
    private Driver driver;

    public External(Driver driver) {
        if (driver == null) throw new IllegalArgumentException("Driver must not be null.");
        this.driver = driver;
    }

    public External(){}

    public void main() {
        try {
            Fi[] f = Core.settings.getDataDirectory().child("mods/Essentials/Driver/").list();
            for (int a=0;a<drivers.size();a++) {
                URLClassLoader classLoader = new URLClassLoader(new URL[]{f[a].file().toURI().toURL()}, this.getClass().getClassLoader());
                Driver driver = (Driver) Class.forName(drivers.get(a), true, classLoader).getDeclaredConstructor().newInstance();
                DriverManager.registerDriver(new External(driver));
            }
        } catch (Exception e) {
            if(!tried){
                download();
            } else {
                e.printStackTrace();
                Core.app.exit();
            }
        }
    }

    public void download() {
        try {
            List<URL> urls = new ArrayList<>();
            urls.add(new URL("https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.30.1/sqlite-jdbc-3.30.1.jar")); // SQLite
            urls.add(new URL("https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/2.5.3/mariadb-java-client-2.5.3.jar")); // MariaDB + MySQL
            urls.add(new URL("https://repo1.maven.org/maven2/org/postgresql/postgresql/42.2.9/postgresql-42.2.9.jar")); // postgreSQL

            System.out.println(nbundle("driver-downloading"));

            for (URL value : urls) {
                String url = value.toString();
                String filename = url.substring(url.lastIndexOf('/') + 1);
                URLDownload(value,
                        Core.settings.getDataDirectory().child("mods/Essentials/Driver/" + filename).file(),
                        filename + " Downloading...",
                        null, null);
            }
            tried = true;
            main();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Connection connect(String url, Properties info) throws SQLException {
        return driver.connect(url, info);
    }

    public boolean acceptsURL(String url) throws SQLException {
        return driver.acceptsURL(url);
    }

    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return driver.getPropertyInfo(url, info);
    }

    public int getMajorVersion() {
        return driver.getMajorVersion();
    }

    public int getMinorVersion() {
        return driver.getMinorVersion();
    }

    public boolean jdbcCompliant() {
        return driver.jdbcCompliant();
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return driver.getParentLogger();
    }
}