package essentials;

import arc.struct.Array;
import essentials.core.player.PlayerData;
import mindustry.entities.type.Player;

import java.time.LocalTime;

import static essentials.Main.tool;

public class PluginVars {
    public static int db_version = 5;
    public static int build_version = 104;
    public static int config_version = 13;
    public static String plugin_version;

    public static LocalTime uptime = LocalTime.of(0, 0, 0);
    public static LocalTime playtime = LocalTime.of(0, 0, 0);

    public static Array<PlayerData> playerData = new Array<>();
    public static Array<Player> players = new Array<>();
    public static String[] DBURL = new String[]{
            "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.30.1/sqlite-jdbc-3.30.1.jar",
            "https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/2.6.0/mariadb-java-client-2.6.0.jar",
            "https://repo1.maven.org/maven2/org/postgresql/postgresql/42.2.11/postgresql-42.2.11.jar",
            "https://repo1.maven.org/maven2/com/h2database/h2/1.4.200/h2-1.4.200.jar"
    };

    public static boolean PvPPeace = false;
    public static String serverIP = tool.getHostIP();
}
