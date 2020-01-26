package essentials.special;

import arc.Core;
import arc.files.Fi;

import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import static essentials.Global.URLDownload;
import static essentials.Global.dbundle;

public class DriverLoader implements Driver {
    private boolean tried = false;
    private Driver driver;
    Fi root = Core.settings.getDataDirectory().child("mods/Essentials/");

    public DriverLoader(Driver driver) {
        if (driver == null) throw new IllegalArgumentException("Driver must not be null.");
        this.driver = driver;
    }

    public DriverLoader(){
        run();
    }

    public void run() {
        try {
            Fi[] f = root.child("Driver/").list();
            for (int a=0;a<3;a++) {
                URLClassLoader classLoader = new URLClassLoader(new URL[]{f[a].file().toURI().toURL()}, this.getClass().getClassLoader());
                String dr = "org.sqlite.JDBC";
                for(int b=0;b<3;b++){
                    if(f[a].name().contains("mariadb")){
                        dr = "org.mariadb.jdbc.Driver";
                    } else if(f[a].name().contains("postgresql")){
                        dr = "org.postgresql.Driver";
                    }
                }
                Driver driver = (Driver) Class.forName(dr, true, classLoader).getDeclaredConstructor().newInstance();
                DriverManager.registerDriver(new DriverLoader(driver));
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

            System.out.println(dbundle("driver-downloading"));

            for (URL value : urls) {
                String url = value.toString();
                String filename = url.substring(url.lastIndexOf('/') + 1);
                root.child("Driver/" + filename).writeString("");
                URLDownload(value,
                        root.child("Driver/" + filename).file(),
                        filename + " Downloading...",
                        null, null);
            }
            tried = true;
            run();
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