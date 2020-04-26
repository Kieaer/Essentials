package essentials.external;

import arc.Core;
import arc.files.Fi;
import arc.struct.Array;
import essentials.internal.CrashReport;
import essentials.internal.Log;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.sql.*;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static essentials.PluginVars.DBURL;

public class DriverLoader implements Driver {
    URLClassLoader H2URL;
    Fi root = Core.settings.getDataDirectory().child("mods/Essentials/");
    Array<URL> urls = new Array<>();
    private boolean tried = false;
    private Driver driver;

    public DriverLoader(Driver driver) {
        if (driver == null) throw new IllegalArgumentException("Driver must not be null.");
        this.driver = driver;
    }

    public DriverLoader() {
        // Ugly source :worried:
        try {
            for (String url : DBURL) urls.add(new URL(url));
            Fi[] f = root.child("Driver/").list();

            for (int a = 0; a < urls.size; a++) {
                URLClassLoader cla = new URLClassLoader(new URL[]{f[a].file().toURI().toURL()}, this.getClass().getClassLoader());
                String dr = "org.sqlite.JDBC";
                for (int b = 0; b < urls.size; b++) {
                    if (f[a].name().contains("mariadb")) {
                        dr = "org.mariadb.jdbc.Driver";
                    } else if (f[a].name().contains("postgresql")) {
                        dr = "org.postgresql.Driver";
                    } else if (f[a].name().contains("h2")) {
                        dr = "org.h2.Driver";
                    }
                }
                Driver driver = (Driver) Class.forName(dr, true, cla).getDeclaredConstructor().newInstance();
                DriverManager.registerDriver(new DriverLoader(driver));
                if (dr.contains("h2")) this.H2URL = cla;
            }
        } catch (Exception e) {
            if (!tried) {
                tried = true;
                download();
            } else {
                LoggerFactory.getLogger(DriverLoader.class).error("Driver load", e);
                Core.app.exit();
            }
        }
    }

    public static void URLDownload(URL URL, File savepath) {
        try {
            BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(savepath));
            URLConnection urlConnection = URL.openConnection();
            InputStream is = urlConnection.getInputStream();
            int size = urlConnection.getContentLength();
            byte[] buf = new byte[256];
            int byteRead;
            int byteWritten = 0;
            long startTime = System.currentTimeMillis();
            while ((byteRead = is.read(buf)) != -1) {
                outputStream.write(buf, 0, byteRead);
                byteWritten += byteRead;

                printProgress(startTime, size, byteWritten);
            }
            is.close();
            outputStream.close();
        } catch (Exception e) {
            new CrashReport(e);
        }
    }

    public static void printProgress(long startTime, int total, int remain) {
        long eta = remain == 0 ? 0 :
                (total - remain) * (System.currentTimeMillis() - startTime) / remain;

        String etaHms = total == 0 ? "N/A" :
                String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(eta),
                        TimeUnit.MILLISECONDS.toMinutes(eta) % TimeUnit.HOURS.toMinutes(1),
                        TimeUnit.MILLISECONDS.toSeconds(eta) % TimeUnit.MINUTES.toSeconds(1));

        if (remain > total) {
            throw new IllegalArgumentException();
        }

        int maxBareSize = 20;
        int remainPercent = ((20 * remain) / total);
        char defaultChar = '-';
        String icon = "*";
        String bare = new String(new char[maxBareSize]).replace('\0', defaultChar) + "]";
        StringBuilder bareDone = new StringBuilder();
        bareDone.append("[");
        for (int i = 0; i < remainPercent; i++) {
            bareDone.append(icon);
        }
        String bareRemain = bare.substring(remainPercent);
        System.out.print("\r" + humanReadableByteCount(remain, true) + "/" + humanReadableByteCount(total, true) + "\t" + bareDone + bareRemain + " " + remainPercent * 5 + "%, ETA: " + etaHms);
        if (remain == total) {
            System.out.print("\n");
        }
    }

    // Source: https://programming.guide/worlds-most-copied-so-snippet.html
    public static strictfp String humanReadableByteCount(int bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        long absBytes = Math.abs(bytes);
        if (absBytes < unit) return bytes + " B";
        int exp = (int) (Math.log(absBytes) / Math.log(unit));
        long th = (long) (Math.pow(unit, exp) * (unit - 0.05));
        if (exp < 6 && absBytes >= th - ((th & 0xfff) == 0xd00 ? 52 : 0)) exp++;
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        if (exp > 4) {
            bytes /= unit;
            exp -= 1;
        }
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public void download() {
        Log.info("system.driver-downloading");

        for (Object value : urls) {
            String url = value.toString();
            String filename = url.substring(url.lastIndexOf('/') + 1);
            root.child("Driver/" + filename).writeString("");
            Log.info(filename + " Downloading...");
            URLDownload((URL) value, root.child("Driver/" + filename).file());
        }
        new DriverLoader();
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