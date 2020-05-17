package essentials;

import arc.ApplicationCore;
import arc.Core;
import arc.Events;
import arc.Settings;
import arc.backend.headless.HeadlessApplication;
import arc.files.Fi;
import arc.util.CommandHandler;
import essentials.core.player.PlayerData;
import essentials.external.DataMigration;
import essentials.feature.Exp;
import essentials.internal.Bundle;
import essentials.internal.CrashReport;
import essentials.internal.Log;
import essentials.internal.exception.PluginException;
import essentials.network.Client;
import essentials.network.Server;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.Mechs;
import mindustry.core.FileTree;
import mindustry.core.Logic;
import mindustry.core.NetServer;
import mindustry.entities.type.BaseUnit;
import mindustry.entities.type.Player;
import mindustry.game.Difficulty;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.maps.Map;
import mindustry.net.Net;
import mindustry.type.UnitType;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.junit.*;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.runners.MethodSorters;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static essentials.Main.*;
import static essentials.PluginTestDB.createNewPlayer;
import static essentials.PluginTestDB.setupDB;
import static mindustry.Vars.*;
import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PluginTest {
    static final PluginTestVars testVars = new PluginTestVars();
    static final Random r = new Random();
    static Fi root;
    static Fi testroot;
    static Main main;
    static CommandHandler serverHandler = new CommandHandler("");
    static CommandHandler clientHandler = new CommandHandler("/");
    static Player player;

    @ClassRule
    public final static SystemOutRule out = new SystemOutRule().enableLog();

    @Before
    @After
    public void resetLog() {
        out.clearLog();
    }

    @BeforeClass
    public static void init() {
        Core.settings = new Settings();
        Core.settings.setDataDirectory(new Fi(""));
        Core.settings.getDataDirectory().child("locales").writeString("en");
        Core.settings.getDataDirectory().child("version.properties").writeString("modifier=release\ntype=official\nnumber=5\nbuild=custom build");
        testroot = Core.settings.getDataDirectory();

        final Map[] testMap = new Map[1];
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
                    testMap[0] = maps.loadInternalMap("maze");
                    Thread.currentThread().interrupt();
                }
            };

            new HeadlessApplication(core, null, throwable -> exceptionThrown[0] = throwable);

            while (!begins[0]) {
                if (exceptionThrown[0] != null) {
                    exceptionThrown[0].printStackTrace();
                    Assert.fail();
                }
                Thread.sleep(10);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Core.settings.setDataDirectory(new Fi("config"));
        root = Core.settings.getDataDirectory().child("mods/Essentials");

        // Reset status
        if (testVars.clean) root.deleteDirectory();

        try (FileInputStream fis = new FileInputStream("./build/libs/Essentials.jar");
             FileOutputStream fos = new FileOutputStream("./config/mods/Essentials.jar")) {
            int availableLen = fis.available();
            byte[] buf = new byte[availableLen];
            fis.read(buf);
            fos.write(buf);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Vars.playerGroup = entities.add(Player.class).enableMapping();

        world.loadMap(testMap[0]);
    }

    @Test
    public void test00_start() throws PluginException {
        root.child("config.hjson").writeString(testVars.config);

        main = new Main();
        main.init();
        main.registerServerCommands(serverHandler);
        main.registerClientCommands(clientHandler);
    }

    @Test
    public void test01_config() {
        assertEquals(config.dbUrl(), "jdbc:h2:file:./config/mods/Essentials/data/player");
    }

    @Test
    public void test02_register() {
        player = createNewPlayer(true);
        assertFalse(playerDB.get(player.uuid).error());

        JsonObject json = JsonObject.readJSON(root.child("permission_user.hjson").readString()).asObject();
        assertNotNull(json.get(player.uuid).asObject());
    }

    @Test
    public void test03_motd() {
        PlayerData playerData = playerDB.get("fakeuuid");
        assertNotNull(tool.getMotd(playerData.locale()));
    }

    @Test
    public void test04_crashReport() {
        try {
            throw new Exception("Crash Report Test");
        } catch (Exception e) {
            new CrashReport(e);
        }
    }

    @Test
    public void test05_log() {
        Log.info("Info Test");
        assertEquals("Info Test\n", out.getLogWithNormalizedLineSeparator());
        out.clearLog();

        Log.info("success");
        assertEquals(new Bundle().get("success") + "\n", out.getLogWithNormalizedLineSeparator());
        out.clearLog();

        Log.warn("warning");
        assertEquals("warning\n", out.getLogWithNormalizedLineSeparator());
        out.clearLog();

        Log.server("server");
        assertEquals("[EssentialServer] server\n", out.getLogWithNormalizedLineSeparator());
        out.clearLog();

        Log.client("client");
        assertEquals("[EssentialClient] client\n", out.getLogWithNormalizedLineSeparator());
        out.clearLog();

        Log.player("player");
        assertEquals("[EssentialPlayer] player\n", out.getLogWithNormalizedLineSeparator());
        out.clearLog();

        root.child("log/player.log").delete();
        root.child("log/player.log").writeString("");
        Log.write(Log.LogType.player, "Log write test");
        assertTrue(root.child("log/player.log").readString().contains("Log write test"));
    }

    @Test
    public void test06_remoteDatabase() throws Exception {
        database.server_start();
        assertNotNull(database.cl);
        database.server_stop();
        assertNull(database.cl);
    }

    @Test
    public void test07_geo() {
        Locale locale = tool.getGeo(vars.serverIP());
        assertNotEquals(locale.getDisplayCountry(), "");
    }

    @Test
    public void test08_exp() {
        PlayerData playerData = playerDB.get("fakeuuid");
        int buf = playerData.reqexp();
        new Exp(playerData);
        assertNotEquals(playerData.reqexp(), buf);
    }

    @Test
    public void test09_network() throws InterruptedException {
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

        out.clearLog();
        client.request(Client.Request.chat, player, "Cross-chat message!");
        assertTrue(out.getLogWithNormalizedLineSeparator().contains("[EssentialClient]"));

        client.request(Client.Request.unbanip, null, "127.0.0.1");

        client.request(Client.Request.unbanid, null, player.uuid);

        // Connection close test
        client.request(Client.Request.exit, null, null);
        TimeUnit.SECONDS.sleep(1);
        server.stop();
    }

    @Test
    public void test10_playerLoad() {
        playerCore.load(player);
        assertTrue(playerDB.get(player.uuid).login());
    }

    @Test
    public void test11_serverCommand() {
        serverHandler.handleMessage("saveall");

        root.child("README.md").delete();
        serverHandler.handleMessage("gendocs");
        assertTrue(root.child("README.md").exists());

        serverHandler.handleMessage("admin " + player.name);
        assertEquals("newadmin", playerDB.get(player.uuid).permission());

        serverHandler.handleMessage("info " + player.uuid);
        assertNotEquals("Player not found!\n", out.getLogWithNormalizedLineSeparator());

        serverHandler.handleMessage("setperm " + player.name + " owner");
        assertEquals("owner", playerDB.get(player.uuid).permission());
    }

    @Test
    public void test12_clientCommandTest() throws InterruptedException {
        playerDB.get(player.uuid).level(50);

        clientHandler.handleMessage("/alert", player);
        assertTrue(playerDB.get(player.uuid).alert());

        clientHandler.handleMessage("/ch", player);
        assertTrue(playerDB.get(player.uuid).crosschat());

        clientHandler.handleMessage("/changepw testpw123 testpw123", player);
        assertNotEquals("none", playerDB.get(player.uuid).accountpw());

        clientHandler.handleMessage("/chars hi", player);
        assertSame(world.tile(player.tileX(), player.tileY()).block(), Blocks.copperWall);

        clientHandler.handleMessage("/color", player);
        assertTrue(playerDB.get(player.uuid).colornick());

        clientHandler.handleMessage("/difficulty easy", player);
        assertEquals(state.rules.waveSpacing, Difficulty.easy.waveTime * 60 * 60 * 2, 0.0);

        UnitType targetUnit = tool.getUnitByName("reaper");
        BaseUnit baseUnit = targetUnit.create(Team.sharded);
        baseUnit.set(player.getX() + 20, player.getY() + 20);
        baseUnit.add();
        clientHandler.handleMessage("/killall", player);
        assertEquals(0, unitGroup.size());

        // Junit 에서 UI Test 불가능
        try {
            clientHandler.handleMessage("/event host testroom maze survival", player);
            TimeUnit.SECONDS.sleep(10);
        } catch (NullPointerException ignored) {
        }
        assertEquals(1, pluginData.eventservers.size);

        clientHandler.handleMessage("/help", player);

        clientHandler.handleMessage("/info", player);

        clientHandler.handleMessage("/jump count localhost 6567", player);
        assertEquals(1, pluginData.jumpcount.size);

        Player dummy1 = createNewPlayer(true);
        clientHandler.handleMessage("/kill " + dummy1.name, player);
        assertTrue(dummy1.isDead());

        //clientHandler.handleMessage("/login", player);

        //clientHandler.handleMessage("/logout", player);

        clientHandler.handleMessage("/maps", player);

        clientHandler.handleMessage("/me It's me!", player);

        clientHandler.handleMessage("/motd", player);

        clientHandler.handleMessage("/players", player);

        clientHandler.handleMessage("/save", player);

        clientHandler.handleMessage("/reset count localhost", player);
        assertEquals(0, pluginData.jumpcount.size);

        //clientHandler.handleMessage("/register testacount testas123 testas123", player);

        clientHandler.handleMessage("/spawn dagger 5 crux", player);

        clientHandler.handleMessage("/setperm " + player.name + " newadmin", player);
        assertEquals("newadmin", playerDB.get(player.uuid).permission());
        serverHandler.handleMessage("setperm " + player.name + " owner");

        clientHandler.handleMessage("/spawn-core big", player);
        assertSame(Blocks.coreNucleus, world.tile(player.tileX(), player.tileY()).block());

        clientHandler.handleMessage("/setmech omega", player);
        assertSame(Mechs.omega, player.mech);

        clientHandler.handleMessage("/status", player);

        clientHandler.handleMessage("/suicide", player);
        assertTrue(player.isDead());
        player.dead = false;

        state.rules.pvp = true;
        Call.onConstructFinish(world.tile(100, 40), Blocks.coreFoundation, 1, (byte) 0, Team.crux, true);
        clientHandler.handleMessage("/team crux", player);
        assertSame(Team.crux, player.getTeam());
        state.rules.pvp = false;

        Player dummy2 = createNewPlayer(true);
        clientHandler.handleMessage("/tempban " + dummy2.name + " 10 test", player);
        assertNotEquals("none", playerDB.get(dummy2.uuid).bantimeset());

        clientHandler.handleMessage("/time", player);

        clientHandler.handleMessage("/tp " + dummy2.name, player);
        assertTrue(player.x == dummy2.x && player.y == dummy2.y);

        Player dummy3 = createNewPlayer(true);
        clientHandler.handleMessage("/tpp " + dummy2.name + " " + dummy3.name, player);
        assertTrue(dummy2.x == dummy3.x && dummy2.y == dummy3.y);

        clientHandler.handleMessage("/tppos 50 50", player);
        assertTrue(player.x == 50 && player.y == 50);

        Player dummy4 = createNewPlayer(true);
        clientHandler.handleMessage("/vote kick " + dummy4.name, player);
        Events.fire(new PlayerChatEvent(player, "y"));
        Events.fire(new PlayerChatEvent(dummy1, "y"));
        Events.fire(new PlayerChatEvent(dummy3, "y"));

        clientHandler.handleMessage("/weather eday", player);
        assertEquals(0.3f, state.rules.ambientLight.a, 0.0f);

        clientHandler.handleMessage("/mute " + dummy2.name, player);
        assertTrue(playerDB.get(dummy2.uuid).mute());

        //clientHandler.handleMessage("/votekick");
    }

    @Test
    public void test13_events() throws InterruptedException {
        Events.fire(new TapConfigEvent(world.tile(r.nextInt(50), r.nextInt(50)), player, 5));

        Events.fire(new TapEvent(world.tile(r.nextInt(50), r.nextInt(50)), player));

        state.rules.attackMode = true;
        Call.onSetRules(state.rules);

        Events.fire(new GameOverEvent(Team.sharded));
        assertEquals(1, playerDB.get(player.uuid).attackclear());

        Events.fire(new WorldLoadEvent());
        assertSame(LocalTime.of(0, 0, 0), vars.playtime());
        assertEquals(0, pluginData.powerblock.size);

        Events.fire(new PlayerConnect(player));

        Events.fire(new DepositEvent(world.tile(r.nextInt(50), r.nextInt(50)), player, Items.copper, 5));

        Player dummy = createNewPlayer(false);
        Events.fire(new PlayerJoin(dummy));
        TimeUnit.SECONDS.sleep(1);

        clientHandler.handleMessage("/register hello testas123", dummy);
        TimeUnit.SECONDS.sleep(1);

        clientHandler.handleMessage("/logout", dummy);
        TimeUnit.SECONDS.sleep(1);

        Player dummy2 = createNewPlayer(false);
        Events.fire(new PlayerJoin(dummy2));

        clientHandler.handleMessage("/login hello testas123", dummy2);
        TimeUnit.SECONDS.sleep(1);
        assertTrue(playerDB.get(dummy2.uuid).login());

        Events.fire(new PlayerLeave(dummy2));
        assertTrue(playerDB.get(dummy2.uuid).error());

        Events.fire(new PlayerChatEvent(player, "hi"));

        Events.fire(new BlockBuildEndEvent(world.tile(r.nextInt(50), r.nextInt(50)), player, Team.sharded, false));

        Events.fire(new BuildSelectEvent(world.tile(r.nextInt(50), r.nextInt(50)), Team.sharded, player, false));

        Events.fire(new UnitDestroyEvent(player));

        Events.fire(new PlayerBanEvent(dummy));

        Events.fire(new PlayerIpBanEvent("127.0.0.3"));

        Events.fire(new PlayerUnbanEvent(dummy));

        Events.fire(new PlayerIpUnbanEvent("127.0.0.3"));

        Events.fire(new ServerLoadEvent());

        for (int a = 0; a < 600; a++) {
            Events.fire(Trigger.update);
            TimeUnit.MILLISECONDS.sleep(16);
        }
    }

    @Test
    public void test14_internal() throws SQLException, ClassNotFoundException {
        setupDB();

        DataMigration dataMigration = new DataMigration();
        dataMigration.MigrateDB();

        playerCore.isLocal(player);
        config.obj = JsonValue.readHjson(testVars.json).asObject();
        config.LegacyUpgrade();
    }

    @Test
    public void test16_complexCommand() {
    }

    @AfterClass
    public static void shutdown() {
        Core.app.getListeners().get(1).dispose();
        assertTrue(out.getLogWithNormalizedLineSeparator().contains(config.bundle.get("thread-disabled")));
        testroot.child("locales").delete();
        testroot.child("version.properties").delete();
    }
}