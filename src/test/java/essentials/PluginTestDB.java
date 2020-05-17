package essentials;

import arc.graphics.Color;
import com.github.javafaker.Faker;
import mindustry.entities.type.Player;
import mindustry.net.Net;
import mindustry.net.NetConnection;

import java.sql.*;

import static essentials.Main.*;
import static essentials.PluginTest.root;
import static essentials.PluginTest.*;
import static mindustry.Vars.playerGroup;

public class PluginTestDB {
    public static String randomString(int length) {
        int leftLimit = 48;
        int rightLimit = 122;

        return r.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    public static Player createNewPlayer(boolean isFull, String... password) {
        Player player = new Player();
        player.isAdmin = false;
        player.con = new NetConnection(r.nextInt(255) + "." + r.nextInt(255) + "." + r.nextInt(255) + "." + r.nextInt(255)) {
            @Override
            public void send(Object o, Net.SendMode sendMode) {

            }

            @Override
            public void close() {

            }
        };
        player.usid = randomString(22) + "==";
        player.name = new Faker().cat().name();
        player.uuid = randomString(22) + "==";
        player.isMobile = false;
        player.dead = false;
        player.setNet(r.nextInt(300), r.nextInt(500));
        player.color.set(Color.rgb(r.nextInt(255), r.nextInt(255), r.nextInt(255)));
        player.color.a = r.nextFloat();
        player.add();
        playerGroup.updateEvents();

        if (isFull) {
            playerDB.register(player.name, player.uuid, "South Korea", "ko_KR", "ko-KR", true, "127.0.0.1", "default", 0L, player.name, password.length != 0 ? password[0] : "none");
            playerDB.load(player.uuid);

            perm.create(playerDB.get(player.uuid));
            perm.saveAll();
        }

        return player;
    }

    public static void setupDB() throws ClassNotFoundException, SQLException {
        root.child("data/player.sqlite3").delete();
        Class.forName("org.sqlite.JDBC");
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + root.child("data/player.sqlite3").absolutePath());
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(testVars.sql);
        }
        String insert = "INSERT INTO 'main'.'players' ('name', 'uuid', 'country', 'country_code', 'language', 'isadmin', 'placecount', 'breakcount', 'killcount', 'deathcount', 'joincount', 'kickcount', 'level', 'exp', 'reqexp', 'reqtotalexp', 'firstdate', 'lastdate', 'lastplacename', 'lastbreakname', 'lastchat', 'playtime', 'attackclear', 'pvpwincount', 'pvplosecount', 'pvpbreakout', 'reactorcount', 'bantimeset', 'bantime', 'banned', 'translate', 'crosschat', 'colornick', 'connected', 'connserver', 'permission', 'mute', 'udid', 'accountid', 'accountpw') VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insert)) {
            for (int a = 0; a < 30; a++) {
                pstmt.setString(1, new Faker().name().lastName());
                pstmt.setString(2, randomString(22) + "==");
                pstmt.setString(3, "South Korea");
                pstmt.setString(4, "ko-KR");
                pstmt.setString(5, "ko-KR");
                pstmt.setBoolean(6, false);
                pstmt.setInt(7, 0);
                pstmt.setInt(8, 0);
                pstmt.setInt(9, 0);
                pstmt.setInt(10, 0);
                pstmt.setInt(11, 0);
                pstmt.setInt(12, 0);
                pstmt.setInt(13, 1);
                pstmt.setInt(14, 0);
                pstmt.setInt(15, 500);
                pstmt.setString(16, "0(500) / 500");
                pstmt.setString(17, tool.getTime());
                pstmt.setString(18, tool.getTime());
                pstmt.setString(19, "none");
                pstmt.setString(20, "none");
                pstmt.setString(21, "none");
                pstmt.setString(22, "00:00.00");
                pstmt.setInt(23, 0);
                pstmt.setInt(24, 0);
                pstmt.setInt(25, 0);
                pstmt.setInt(26, 0);
                pstmt.setInt(27, 0);
                pstmt.setInt(28, 0);
                pstmt.setString(29, "none");
                pstmt.setBoolean(30, false);
                pstmt.setBoolean(31, false);
                pstmt.setBoolean(32, false);
                pstmt.setBoolean(33, false);
                pstmt.setBoolean(34, false);
                pstmt.setString(35, "127.0.0.1");
                pstmt.setString(36, "default");
                pstmt.setBoolean(37, false);
                pstmt.setLong(38, 0L); // UDID
                pstmt.setString(39, new Faker().name().lastName());
                pstmt.setString(40, "none");
                pstmt.execute();
            }
        }
    }
}
