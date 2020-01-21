package essentials.special;

import arc.Core;
import arc.files.Fi;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import static essentials.Global.nbundle;
import static essentials.Global.printProgress;

public class External {
    private List<URL> jarList = new ArrayList<>();

    public void main() {
        try {
            Fi[] f = Core.settings.getDataDirectory().child("mods/Essentials/Driver/").list();
            for (Fi fi : f) {
                jarList.add(fi.file().toURI().toURL());
            }
            load();
            Class.forName("org.sqlite.JDBC");
            Class.forName("org.mariadb.jdbc.Driver");
            Class.forName("com.mysql.jdbc.Driver");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void download() {
        try {
            List<URL> urls = new ArrayList<>();
            urls.add(new URL("https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.30.1/sqlite-jdbc-3.30.1.jar")); // SQLite
            urls.add(new URL("https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/2.5.3/mariadb-java-client-2.5.3.jar")); // MariaDB + MySQL
            urls.add(new URL("https://repo1.maven.org/maven2/org/postgresql/postgresql/42.2.9/postgresql-42.2.9.jar"));

            System.out.println(nbundle("driver-downloading"));

            for (URL value : urls) {
                String url = value.toString();
                String filename = url.substring(url.lastIndexOf('/') + 1);
                BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(Core.settings.getDataDirectory().child("mods/Essentials/Driver/" + filename).file()));
                URLConnection urlConnection = value.openConnection();
                InputStream is = urlConnection.getInputStream();
                int size = urlConnection.getContentLength();
                byte[] buf = new byte[512];
                int byteRead;
                int byteWritten = 0;
                long startTime = System.currentTimeMillis();
                System.out.println(filename + " Downloading...");
                while ((byteRead = is.read(buf)) != -1) {
                    outputStream.write(buf, 0, byteRead);
                    byteWritten += byteRead;

                    printProgress(startTime, size, byteWritten);
                }
                is.close();
                outputStream.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void load() throws Exception {
        for (URL url : jarList) {
            URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            Method method = URLClassLoader.class.getDeclaredMethod(url.getPath(), URL.class);
            method.setAccessible(true);
            method.invoke(classLoader, url);
        }
    }
}
