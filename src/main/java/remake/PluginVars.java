package remake;

import remake.core.player.PlayerData;
import remake.external.Tools;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PluginVars {
    public static int db_version = 5;
    public static int build_version = 104;
    public static int config_version = 13;
    public static String plugin_version;

    public static LocalTime uptime;
    public static LocalTime playtime;

    public static List<PlayerData> playerData = new ArrayList<>();
    public static List<String> DBURL = Arrays.asList(
            "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.30.1/sqlite-jdbc-3.30.1.jar",
            "https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/2.6.0/mariadb-java-client-2.6.0.jar",
            "https://repo1.maven.org/maven2/org/postgresql/postgresql/42.2.11/postgresql-42.2.11.jar",
            "https://repo1.maven.org/maven2/com/h2database/h2/1.4.200/h2-1.4.200.jar"
    );

    public static boolean PvPPeace = false;
    public static String serverIP = Tools.getHostIP();
}
