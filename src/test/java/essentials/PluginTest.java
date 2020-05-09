package essentials;

import arc.ApplicationCore;
import arc.Core;
import arc.backend.headless.HeadlessApplication;
import arc.files.Fi;
import arc.graphics.Color;
import arc.util.CommandHandler;
import essentials.core.player.PlayerData;
import essentials.feature.Exp;
import essentials.internal.Bundle;
import essentials.internal.CrashReport;
import essentials.internal.Log;
import essentials.internal.exception.PluginException;
import essentials.network.Client;
import essentials.network.Server;
import mindustry.Vars;
import mindustry.core.FileTree;
import mindustry.core.Logic;
import mindustry.core.NetServer;
import mindustry.entities.type.Player;
import mindustry.maps.Map;
import mindustry.net.Net;
import mindustry.net.NetConnection;
import org.hjson.JsonObject;
import org.junit.*;
import org.junit.contrib.java.lang.system.SystemOutRule;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static essentials.Main.*;
import static mindustry.Vars.*;
import static org.junit.Assert.*;

public class PluginTest {
    static Main main;
    static Fi root;
    static CommandHandler serverHandler = new CommandHandler("");
    static CommandHandler clientHandler = new CommandHandler("");
    static Map testMap;

    @ClassRule
    public static final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

    public Player createNewPlayer() {
        Player player = new Player();
        player.isAdmin = false;
        player.con = new NetConnection("127.0.0.1") {
            @Override
            public void send(Object o, Net.SendMode sendMode) {

            }

            @Override
            public void close() {

            }
        };
        player.usid = "fake usid";
        player.name = "i am fake";
        player.uuid = "fake uuid";
        player.isMobile = false;
        player.dead = true;
        player.setNet(1, 1);
        player.color.set(Color.orange);
        player.color.a = 1f;

        playerGroup.add(Vars.player);
        return player;
    }

    @Before
    public void clearLog() {
        systemOutRule.clearLog();
    }

    @BeforeClass
    public static void init() throws PluginException {
        try {
            boolean[] begins = {false};
            Throwable[] exceptionThrown = {null};
            arc.util.Log.setUseColors(false);

            ApplicationCore core = new ApplicationCore() {
                @Override
                public void setup() {
                    headless = true;
                    net = new Net(null);
                    tree = new FileTree();
                    Vars.init();
                    content.createBaseContent();

                    add(logic = new Logic());
                    add(netServer = new NetServer());

                    content.init();
                }

                @Override
                public void init() {
                    super.init();
                    begins[0] = true;
                    testMap = maps.loadInternalMap("maze");
                    Thread.currentThread().interrupt();
                }
            };

            new HeadlessApplication(core, null, throwable -> exceptionThrown[0] = throwable);

            while (!begins[0]) {
                if (exceptionThrown[0] != null) {
                    Assert.fail(String.valueOf(exceptionThrown[0]));
                }
                Thread.sleep(10);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Core.settings.setDataDirectory(new Fi("config"));
        root = Core.settings.getDataDirectory().child("mods/Essentials");

        // Reset status
        // root.deleteDirectory();

        try (FileInputStream fis = new FileInputStream("build/libs/Essentials.jar");
             FileOutputStream fos = new FileOutputStream("config/mods/Essentials.jar")) {
            int availableLen = fis.available();
            byte[] buf = new byte[availableLen];
            fis.read(buf);
            fos.write(buf);
        } catch (IOException e) {
            e.printStackTrace();
        }

        main = new Main();
        main.init();
        main.registerServerCommands(serverHandler);
        main.registerClientCommands(clientHandler);

        world.loadMap(testMap);
    }

    @Test
    public void configTest() {
        assertEquals(config.dbUrl(), "jdbc:h2:file:./config/mods/Essentials/data/player");
    }

    @Test
    public void playerDataTest() {
        PlayerData playerData = playerCore.NewData("Tester", "fakeuuid", "South Korea", "ko_KR", "ko-KR", true, "127.0.0.1", "default", 0L, "Tester", "none");
        assertEquals(playerData.locale().getLanguage(), Locale.KOREAN.getLanguage());
        assertTrue(playerDB.save(playerData));
    }

    @Test
    public void registerTest() {
        playerDB.register("Tester", "fakeuuid", "South Korea", "ko_KR", "ko-KR", true, "127.0.0.1", "default", 0L, "Tester", "none");
        playerDB.load("fakeuuid");
        assertFalse(playerDB.get("fakeuuid").error());

        perm.create(playerDB.get("fakeuuid"));
        perm.saveAll();

        JsonObject json = JsonObject.readJSON(root.child("permission_user.hjson").readString()).asObject();
        assertNotNull(json.get("fakeuuid").asObject());
    }

    @Test
    public void motdTest() {
        PlayerData playerData = playerDB.get("fakeuuid");
        assertNotNull(tool.getMotd(playerData.locale()));
    }

    @Test
    public void crashReportTest() {
        try {
            throw new Exception("Crash Report Test");
        } catch (Exception e) {
            new CrashReport(e);
        }
    }

    @Test
    public void logTest() {
        Log.info("Info Test");
        assertEquals("Info Test\n", systemOutRule.getLogWithNormalizedLineSeparator());
        systemOutRule.clearLog();

        Log.info("success");
        assertNotEquals("success\n", systemOutRule.getLogWithNormalizedLineSeparator());
        systemOutRule.clearLog();

        Log.warn("warning");
        assertEquals("warning\n", systemOutRule.getLogWithNormalizedLineSeparator());
        systemOutRule.clearLog();

        Log.server("server");
        assertEquals("[EssentialServer] server\n", systemOutRule.getLogWithNormalizedLineSeparator());
        systemOutRule.clearLog();

        Log.client("client");
        assertEquals("[EssentialClient] client\n", systemOutRule.getLogWithNormalizedLineSeparator());
        systemOutRule.clearLog();

        Log.player("player");
        assertEquals("[EssentialPlayer] player\n", systemOutRule.getLogWithNormalizedLineSeparator());
        systemOutRule.clearLog();

        Log.write(Log.LogType.player, "Log write test");
        assertTrue(root.child("log/player.log").readString().contains("Log write test"));
        systemOutRule.clearLog();
    }

    @Test
    public void remoteDatabaseTest() throws Exception {
        database.server_start();
        assertNotNull(database.cl);
        database.server_stop();
        assertNull(database.cl);
    }

    @Test
    public void geoTest() {
        Locale locale = tool.getGeo(vars.serverIP());
        assertNotEquals(locale.getDisplayCountry(), "");
    }

    @Test
    public void expTest() {
        PlayerData playerData = playerDB.get("fakeuuid");
        int buf = playerData.reqexp();
        new Exp(playerData);
        assertNotEquals(playerData.reqexp(), buf);
    }

    @Test
    public void NetworkTest() throws InterruptedException {
        Server server = new Server();
        Client client = new Client();
        config.clientHost("127.0.0.1");

        // Server start test
        mainThread.submit(server);
        TimeUnit.SECONDS.sleep(1);
        assertNotNull(server.serverSocket);

        // Client start test
        mainThread.submit(client);
        TimeUnit.SECONDS.sleep(1);
        client.wakeup();
        assertTrue(client.activated);

        // Ban data sharing test
        client.request(Client.Request.bansync, null, null);
        TimeUnit.SECONDS.sleep(1);
        assertTrue(client.activated);
        assertTrue(server.list.size != 0);

        // Connection close test
        client.request(Client.Request.exit, null, null);
        TimeUnit.SECONDS.sleep(1);
        server.stop();
    }

    @Test
    public void serverCommandTest() {
        serverHandler.handleMessage("info fakeuuid");
        assertNotEquals("Player not found!", systemOutRule.getLogWithNormalizedLineSeparator());
        systemOutRule.clearLog();
    }

    @AfterClass
    public static void shutdown() {
        Core.app.getListeners().get(1).dispose();
        assertTrue(systemOutRule.getLogWithNormalizedLineSeparator().contains(new Bundle(locale).get("thread-disabled")));
    }
}