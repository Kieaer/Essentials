package essentials;

import arc.ApplicationCore;
import arc.Core;
import arc.Events;
import arc.Settings;
import arc.backend.headless.HeadlessApplication;
import arc.files.Fi;
import arc.graphics.Color;
import arc.util.CommandHandler;
import com.github.javafaker.Faker;
import essentials.core.player.PlayerData;
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
import mindustry.net.NetConnection;
import mindustry.type.UnitType;
import org.hjson.JsonObject;
import org.junit.*;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.runners.MethodSorters;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static essentials.Main.*;
import static mindustry.Vars.*;
import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PluginTest {
    static Main main;
    static Fi root;
    static Fi testroot;
    static CommandHandler serverHandler = new CommandHandler("");
    static CommandHandler clientHandler = new CommandHandler("/");
    static Map testMap;
    static Player player;

    Random r = new Random();

    @ClassRule
    public final static SystemOutRule out = new SystemOutRule().enableLog();

    public String randomString(int length) {
        int leftLimit = 48;
        int rightLimit = 122;

        return r.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    public Player createNewPlayer(boolean isFull) {
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
        player.name = new Faker().name().lastName();
        player.uuid = randomString(22) + "==";
        player.isMobile = false;
        player.dead = false;
        player.setNet(r.nextInt(300), r.nextInt(500));
        player.color.set(Color.rgb(r.nextInt(255), r.nextInt(255), r.nextInt(255)));
        player.color.a = r.nextFloat();
        player.add();
        playerGroup.updateEvents();

        if (isFull) {
            playerDB.register(player.name, player.uuid, "South Korea", "ko_KR", "ko-KR", true, "127.0.0.1", "default", 0L, player.name, "none");
            playerDB.load(player.uuid);

            perm.create(playerDB.get(player.uuid));
            perm.saveAll();
        }

        return player;
    }

    @Before
    @After
    public void resetLog() {
        out.clearLog();
    }

    @BeforeClass
    public static void init() throws PluginException {
        Core.settings = new Settings();
        Core.settings.setDataDirectory(new Fi(""));
        Core.settings.getDataDirectory().child("locales").writeString("en");
        Core.settings.getDataDirectory().child("version.properties").writeString("modifier=release\ntype=official\nnumber=5\nbuild=custom build");
        testroot = Core.settings.getDataDirectory();

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

        Vars.playerGroup = entities.add(Player.class).enableMapping();

        world.loadMap(testMap);
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
    public void test12_clientCommandTest() {
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

        clientHandler.handleMessage("/setperm " + player.name + " newadmin", player);
        assertEquals("newadmin", playerDB.get(player.uuid).permission());
        serverHandler.handleMessage("setperm " + player.name + " owner");

        clientHandler.handleMessage("/spawn-core big", player);
        assertSame(Blocks.coreNucleus, world.tile(player.tileX(), player.tileY()).block());

        clientHandler.handleMessage("/setmech omega", player);
        assertSame(Mechs.omega, player.mech);

        clientHandler.handleMessage("/suicide", player);
        assertTrue(player.isDead());
        player.dead = false;

        state.rules.pvp = true;
        Call.onConstructFinish(world.tile(100, 40), Blocks.coreFoundation, 1, (byte) 0, Team.crux, true);
        clientHandler.handleMessage("/team crux", player);
        assertSame(Team.crux, player.getTeam());
        state.rules.pvp = false;

        Player dummy = createNewPlayer(true);
        clientHandler.handleMessage("/tempban " + dummy.name + " 10 test", player);
        assertNotEquals("none", playerDB.get(dummy.uuid).bantimeset());

        clientHandler.handleMessage("/tp " + dummy.name, player);
        assertTrue(player.x == dummy.x && player.y == dummy.y);

        Player dummy2 = createNewPlayer(true);
        clientHandler.handleMessage("/tpp " + dummy.name + " " + dummy2.name, player);
        assertTrue(dummy.x == dummy2.x && dummy.y == dummy2.y);

        clientHandler.handleMessage("/tppos 50 50", player);
        assertTrue(player.x == 50 && player.y == 50);

        //clientHandler.handleMessage("/vote", player);

        clientHandler.handleMessage("/weather eday", player);
        assertEquals(0.3f, state.rules.ambientLight.a, 0.0f);

        clientHandler.handleMessage("/mute " + dummy.name, player);
        assertTrue(playerDB.get(dummy.uuid).mute());

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
        TimeUnit.SECONDS.sleep(3);
        assertTrue(playerDB.get(dummy.uuid).login());

        Events.fire(new PlayerLeave(dummy));
        assertTrue(playerDB.get(dummy.uuid).error());

        Events.fire(new PlayerChatEvent(player, "hi"));

        Events.fire(new BlockBuildEndEvent(world.tile(r.nextInt(50), r.nextInt(50)), player, Team.sharded, false));

        Events.fire(new BuildSelectEvent(world.tile(r.nextInt(50), r.nextInt(50)), Team.sharded, player, false));

        Events.fire(new UnitDestroyEvent(player));

        Events.fire(new ServerLoadEvent());
    }

    @AfterClass
    public static void shutdown() {
        Core.app.getListeners().get(1).dispose();
        assertTrue(out.getLogWithNormalizedLineSeparator().contains(new Bundle(locale).get("thread-disabled")));
        testroot.child("locales").delete();
        testroot.child("version.properties").delete();
    }
}