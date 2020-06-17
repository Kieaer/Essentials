package essentials;

import arc.graphics.Color;
import com.github.javafaker.Faker;
import mindustry.entities.type.Player;
import mindustry.net.Net;
import mindustry.net.NetConnection;

import static essentials.Main.*;
import static essentials.PluginTest.r;

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
        Groups.player.updateEvents();

        if (isFull) {
            playerDB.register(player.name, player.uuid, "South Korea", "ko_KR", "ko-KR", true, "127.0.0.1", "default", 0L, player.name, password.length != 0 ? password[0] : "none");
            playerCore.load(player);
            //playerDB.load(player.uuid());

            perm.create(playerDB.get(player.uuid()));
            perm.saveAll();
        }

        return player;
    }
}
