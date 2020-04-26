package essentials;

import arc.func.Boolf;
import arc.struct.Array;
import essentials.core.player.PlayerData;
import mindustry.entities.type.Player;

import java.time.LocalTime;

import static essentials.Main.tool;

public class PluginVars {
    private int dbVersion = 5;
    private int buildVersion = 104;
    private int buildRevision = 10;
    private int configVersion = 13;
    private String[] DBURL = new String[]{
            "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.30.1/sqlite-jdbc-3.30.1.jar",
            "https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/2.6.0/mariadb-java-client-2.6.0.jar",
            "https://repo1.maven.org/maven2/org/postgresql/postgresql/42.2.11/postgresql-42.2.11.jar",
            "https://repo1.maven.org/maven2/com/h2database/h2/1.4.200/h2-1.4.200.jar",
    };
    private String serverIP = tool.getHostIP();
    private String pluginVersion;
    private LocalTime uptime = LocalTime.of(0, 0, 0);
    private LocalTime playtime = LocalTime.of(0, 0, 0);
    private Array<PlayerData> playerData = new Array<>();
    private Array<Player> players = new Array<>();
    private boolean PvPPeace = false;

    public int dbVersion() {
        return dbVersion;
    }

    public void dbVersion(int dbVersion) {
        this.dbVersion = dbVersion;
    }

    public int buildVersion() {
        return buildVersion;
    }

    public void buildVersion(int buildVersion) {
        this.buildVersion = buildVersion;
    }

    public int buildRevision() {
        return buildRevision;
    }

    public void buildRevision(int buildRevision) {
        this.buildRevision = buildRevision;
    }

    public int configVersion() {
        return configVersion;
    }

    public void configVersion(int configVersion) {
        this.configVersion = configVersion;
    }

    public String[] dburl() {
        return DBURL;
    }

    public void dburl(String[] dburl) {
        this.DBURL = dburl;
    }

    public String serverIP() {
        return serverIP;
    }

    public void serverIP(String serverIP) {
        this.serverIP = serverIP;
    }

    public String pluginVersion() {
        return pluginVersion;
    }

    public void pluginVersion(String pluginVersion) {
        this.pluginVersion = pluginVersion;
    }

    public LocalTime uptime() {
        return uptime;
    }

    public void uptime(LocalTime uptime) {
        this.uptime = uptime;
    }

    public LocalTime playtime() {
        return playtime;
    }

    public void playtime(LocalTime playtime) {
        this.playtime = playtime;
    }

    public Array<PlayerData> playerData() {
        return playerData;
    }

    public void addPlayerData(PlayerData p) {
        playerData.add(p);
    }

    public void removePlayerData(Boolf<PlayerData> p) {
        playerData.remove(p);
    }

    public Array<Player> players() {
        return players;
    }

    public void addPlayers(Player player) {
        players.add(player);
    }

    public void removePlayers(Player player) {
        players.remove(player);
    }

    public void players(Array<Player> players) {
        this.players = players;
    }

    public boolean isPvPPeace() {
        return PvPPeace;
    }

    public void setPvPPeace(boolean pvpPeace) {
        PvPPeace = pvpPeace;
    }
}
