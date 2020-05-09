package essentials;

import arc.Application;
import arc.ApplicationListener;
import arc.Core;
import arc.Settings;
import arc.files.Fi;
import arc.func.Cons;
import arc.graphics.Color;
import arc.struct.Array;
import arc.util.CommandHandler;
import essentials.core.player.PlayerData;
import essentials.feature.Exp;
import essentials.internal.CrashReport;
import essentials.internal.Log;
import essentials.internal.exception.PluginException;
import essentials.network.Client;
import essentials.network.Server;
import mindustry.Vars;
import mindustry.core.NetServer;
import mindustry.core.Version;
import mindustry.entities.EntityGroup;
import mindustry.entities.type.Player;
import mindustry.net.Host;
import mindustry.net.Net;
import mindustry.net.NetConnection;
import org.hjson.JsonObject;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static essentials.Main.*;
import static mindustry.Vars.netServer;
import static mindustry.Vars.playerGroup;
import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.crypto.*")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@PowerMockRunnerDelegate(JUnit4.class)
public class PluginTest {
    static Main main;
    static Fi root;
    static CommandHandler serverHandler = new CommandHandler("");
    static CommandHandler clientHandler = new CommandHandler("");

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

    @BeforeClass
    public static void init() throws PluginException {
        Core.settings = new Settings();
        Core.settings.setDataDirectory(new Fi("config"));
        root = Core.settings.getDataDirectory().child("mods/Essentials");
        Core.app = new Application() {
            Array<ApplicationListener> listeners = new Array<>();

            @Override
            public Array<ApplicationListener> getListeners() {
                return listeners;
            }

            @Override
            public ApplicationType getType() {
                return null;
            }

            @Override
            public String getClipboardText() {
                return null;
            }

            @Override
            public void setClipboardText(String s) {

            }

            @Override
            public void post(Runnable runnable) {

            }

            @Override
            public void exit() {

            }

            @Override
            public void dispose() {

            }
        };
        Version.build = 104;
        Version.revision = 10;
        Vars.playerGroup = new EntityGroup<>(0, Player.class, false);
        Vars.net = new Net(new Net.NetProvider() {
            @Override
            public void connectClient(String s, int i, Runnable runnable) throws IOException {

            }

            @Override
            public void sendClient(Object o, Net.SendMode sendMode) {

            }

            @Override
            public void disconnectClient() {

            }

            @Override
            public void discoverServers(Cons<Host> cons, Runnable runnable) {

            }

            @Override
            public void pingHost(String s, int i, Cons<Host> cons, Cons<Exception> cons1) {

            }

            @Override
            public void hostServer(int i) throws IOException {

            }

            @Override
            public Iterable<? extends NetConnection> getConnections() {
                return null;
            }

            @Override
            public void closeServer() {

            }
        });
        netServer = new NetServer();
        netServer.admins.banPlayer("fakebanid1");
        netServer.admins.banPlayer("fakebnaid2");

        Vars.player = new Player();
        Vars.player.isAdmin = false;
        Vars.player.con = new NetConnection("127.0.0.1") {
            @Override
            public void send(Object o, Net.SendMode sendMode) {

            }

            @Override
            public void close() {

            }
        };
        Vars.player.usid = "fake usid";
        Vars.player.name = "i am fake";
        Vars.player.uuid = "fake uuid";
        Vars.player.isMobile = false;
        Vars.player.dead = true;
        Vars.player.setNet(1, 1);
        Vars.player.color.set(Color.orange);
        Vars.player.color.a = 1f;
        Vars.player.reset();

        playerGroup.add(Vars.player);

        // Reset status
        root.deleteDirectory();

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
        serverHandler.handleMessage("saveall");
        assertEquals("", systemOutRule.getLogWithNormalizedLineSeparator());

        root.child("README.md").delete();
        serverHandler.handleMessage("gendocs");
        assertTrue(root.child("README.md").exists());


        serverHandler.handleMessage("info fakeuuid");
        assertNotEquals("Player not found!", systemOutRule.getLog());
    }

    @Test
    public void shutdownTest() {
        Core.app.dispose();
        //assertEquals(new Bundle(locale).get("thread-disabled")+"\n", systemOutRule.getLogWithNormalizedLineSeparator());
    }
}