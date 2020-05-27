package essentials;

import arc.ApplicationCore;
import arc.Core;
import arc.Events;
import arc.Settings;
import arc.backend.headless.HeadlessApplication;
import arc.files.Fi;
import arc.util.CommandHandler;
import arc.util.Time;
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
import mindustry.core.GameState;
import mindustry.core.Logic;
import mindustry.core.NetServer;
import mindustry.entities.traits.BuilderTrait;
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
import org.junit.*;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.runners.MethodSorters;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static essentials.Main.*;
import static essentials.PluginTestDB.createNewPlayer;
import static java.lang.Thread.sleep;
import static mindustry.Vars.*;
import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PluginTest {
    static final PluginTestVars testVars = new PluginTestVars();
    static final SecureRandom r = new SecureRandom();
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
    public static void init() throws PluginException, InterruptedException {
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
                sleep(10);
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

        state.set(GameState.State.playing);

        new Thread(() -> {
            while (true) {
                try {
                    Time.update();
                    unitGroup.update();
                    puddleGroup.update();
                    shieldGroup.update();
                    bulletGroup.update();
                    tileGroup.update();
                    fireGroup.update();
                    collisions.collideGroups(bulletGroup, unitGroup);
                    collisions.collideGroups(bulletGroup, playerGroup);
                    unitGroup.updateEvents();
                    collisions.updatePhysics(unitGroup);
                    playerGroup.update();
                    effectGroup.update();
                    sleep(16);
                } catch (InterruptedException ignored) {
                }
            }
        }).start();

        testroot.child("locales").delete();
        testroot.child("version.properties").delete();

        root.child("config.hjson").writeString(testVars.config);

        main = new Main();
        main.init();
        main.registerServerCommands(serverHandler);
        main.registerClientCommands(clientHandler);
        player = createNewPlayer(true);
    }

/*    @Test
    public void test00_start() throws PluginException {

    }*/

    @Test
    public void test01_config() {
        assertEquals(config.dbUrl(), "jdbc:h2:file:./config/mods/Essentials/data/player");
    }

    @Test
    public void test02_register() {
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
    public void test04_DBCheck() {
        assertTrue(new CrashReport(new Exception("test")).success);
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
    public void test06_remoteDatabase() {
        if (config.dbServer()) {
            assertNotNull(database.server);
        }
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
    public void test09_network() {
        try {
            Server server = new Server();
            Client client = new Client();

            // Server start test
            mainThread.submit(server);
            sleep(1000);
            assertNotNull(server.serverSocket);

            // Client start test
            mainThread.submit(client);
            client.wakeup();
            sleep(1000);
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

            // Server http server test
            try {
                HttpURLConnection con = (HttpURLConnection) new URL("http://127.0.0.1:25000/rank").openConnection();
                con.setRequestMethod("GET");
                con.setDoInput(true);
                if (con.getResponseCode() != 200) {
                    fail("HTTP test failed");
                } else {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                        String inputLine;
                        StringBuilder response = new StringBuilder();
                        while ((inputLine = br.readLine()) != null) {
                            response.append(inputLine);
                        }
                        assertTrue(response.toString().contains("attack"));
                    }
                }
                con.disconnect();
            } catch (Exception ignored) {
            }

            // Connection close test
            client.request(Client.Request.exit, null, null);
            sleep(1000);
            server.shutdown();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void test10_playerLoad() {
        playerCore.load(player);
        assertTrue(playerDB.get(player.uuid).login());
    }

    @Test
    public void test11_serverCommand() {
        serverHandler.handleMessage("saveall");

        serverHandler.handleMessage("edit " + player.uuid + " lastchat Manually");
        assertEquals("Manually", playerDB.get(player.uuid).lastchat());

        root.child("README.md").delete();
        serverHandler.handleMessage("gendocs");
        assertTrue(root.child("README.md").exists());

        serverHandler.handleMessage("admin " + player.name);
        assertEquals("newadmin", playerDB.get(player.uuid).permission());

        serverHandler.handleMessage("bansync");

        serverHandler.handleMessage("info " + player.uuid);
        assertNotEquals("Player not found!\n", out.getLogWithNormalizedLineSeparator());

        serverHandler.handleMessage("setperm " + player.name + " owner");
        assertEquals("owner", playerDB.get(player.uuid).permission());

        serverHandler.handleMessage("reload");
    }

    @Test
    public void test12_clientCommand() throws InterruptedException {
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
        for (int a = 0; a < 20; a++) baseUnit.add();
        clientHandler.handleMessage("/killall", player);
        assertEquals(0, unitGroup.size());

        try {
            clientHandler.handleMessage("/event host testroom maze survival", player);
            for (int a = 0; a < 60; a++) {
                if (pluginData.eventservers.size == 0) {
                    System.out.print("\rWaiting... " + a);
                    TimeUnit.SECONDS.sleep(1);
                } else {
                    break;
                }
            }
            clientHandler.handleMessage("/event join testroom", player);

            if (pluginData.eventservers.size == 0) System.out.println("\n");
        } catch (NullPointerException ignored) {
        }
        assertEquals(1, pluginData.eventservers.size);

        clientHandler.handleMessage("/help", player);

        clientHandler.handleMessage("/info", player);

        clientHandler.handleMessage("/jump count 192.168.35.100 6567", player);
        assertEquals(1, pluginData.jumpcount.size);

        clientHandler.handleMessage("/jump zone 192.168.35.100 6567 20 true", player);
        assertEquals(1, pluginData.jumpzone.size);
        sleep(4000);

        clientHandler.handleMessage("/jump total", player);
        assertEquals(1, pluginData.jumptotal.size);

        Player dummy1 = createNewPlayer(true);
        clientHandler.handleMessage("/kill " + dummy1.name, player);
        assertTrue(dummy1.isDead());
        dummy1.setDead(false);

        clientHandler.handleMessage("/maps", player);

        clientHandler.handleMessage("/me It's me!", player);

        clientHandler.handleMessage("/motd", player);

        clientHandler.handleMessage("/players", player);

        clientHandler.handleMessage("/save", player);

        clientHandler.handleMessage("/r " + dummy1.name + " Hi!", player);

        clientHandler.handleMessage("/reset count 192.168.35.100", player);
        assertEquals(0, pluginData.jumpcount.size);

        clientHandler.handleMessage("/reset zone 192.168.35.100", player);
        assertEquals(0, pluginData.jumpzone.size);

        clientHandler.handleMessage("/reset total", player);
        assertEquals(0, pluginData.jumptotal.size);

        //clientHandler.handleMessage("/register testacount testas123 testas123", player);

        clientHandler.handleMessage("/spawn dagger 5 crux", player);

        clientHandler.handleMessage("/setperm " + player.name + " newadmin", player);
        assertEquals("newadmin", playerDB.get(player.uuid).permission());
        serverHandler.handleMessage("setperm " + player.name + " owner");

        player.set(80, 80);
        player.setNet(80, 80);
        clientHandler.handleMessage("/spawn-core big", player);
        assertSame(Blocks.coreNucleus, world.tileWorld(80, 80).block());

        clientHandler.handleMessage("/setmech alpha", player);
        assertSame(Mechs.alpha, player.mech);

        clientHandler.handleMessage("/setmech dart", player);
        assertSame(Mechs.dart, player.mech);

        clientHandler.handleMessage("/setmech glaive", player);
        assertSame(Mechs.glaive, player.mech);

        clientHandler.handleMessage("/setmech javelin", player);
        assertSame(Mechs.javelin, player.mech);

        clientHandler.handleMessage("/setmech omega", player);
        assertSame(Mechs.omega, player.mech);

        clientHandler.handleMessage("/setmech tau", player);
        assertSame(Mechs.tau, player.mech);

        clientHandler.handleMessage("/setmech trident", player);
        assertSame(Mechs.trident, player.mech);

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

        System.out.println("== votekick");
        clientHandler.handleMessage("/vote kick " + dummy4.id, player);
        Events.fire(new PlayerChatEvent(player, "y"));
        Events.fire(new PlayerChatEvent(dummy1, "y"));
        Events.fire(new PlayerChatEvent(dummy3, "y"));
        // Can't check player kicked

        System.out.println("== votemap");
        clientHandler.handleMessage("/vote map Glacier", player);
        Events.fire(new PlayerChatEvent(player, "y"));
        Events.fire(new PlayerChatEvent(dummy1, "y"));
        Events.fire(new PlayerChatEvent(dummy3, "y"));
        sleep(150);
        assertEquals("Glacier", world.getMap().name());

        System.out.println("== vote gameover");
        clientHandler.handleMessage("/vote gameover", player);
        Events.fire(new PlayerChatEvent(player, "y"));
        Events.fire(new PlayerChatEvent(dummy1, "y"));
        Events.fire(new PlayerChatEvent(dummy3, "y"));

        System.out.println("== vote rollback");
        serverHandler.handleMessage("save 1000");
        clientHandler.handleMessage("/vote rollback", player);
        TimeUnit.SECONDS.sleep(1);
        Events.fire(new PlayerChatEvent(player, "y"));
        Events.fire(new PlayerChatEvent(dummy1, "y"));
        Events.fire(new PlayerChatEvent(dummy3, "y"));

        System.out.println("== vote skipwave");
        clientHandler.handleMessage("/vote skipwave", player);
        Events.fire(new PlayerChatEvent(player, "y"));
        Events.fire(new PlayerChatEvent(dummy1, "y"));
        Events.fire(new PlayerChatEvent(dummy3, "y"));

        clientHandler.handleMessage("/weather day", player);
        assertEquals(0.0f, state.rules.ambientLight.a, 0.0f);

        clientHandler.handleMessage("/weather eday", player);
        assertEquals(0.3f, state.rules.ambientLight.a, 0.0f);

        clientHandler.handleMessage("/weather night", player);
        assertEquals(0.7f, state.rules.ambientLight.a, 0.0f);

        clientHandler.handleMessage("/weather enight", player);
        assertEquals(0.85f, state.rules.ambientLight.a, 0.0f);

        clientHandler.handleMessage("/mute " + dummy3.name, player);
        assertNotNull(playerGroup.find(p -> p.uuid.equals(dummy3.uuid)));
        assertTrue(playerDB.get(dummy3.uuid).mute());

        //clientHandler.handleMessage("/votekick");
    }

    @Test
    public void test13_events() throws InterruptedException {
        Events.fire(new TapConfigEvent(world.tile(r.nextInt(50), r.nextInt(50)), player, 5));

        Events.fire(new TapEvent(world.tile(r.nextInt(50), r.nextInt(50)), player));

        Events.fire(new WithdrawEvent(world.tile(r.nextInt(50), r.nextInt(50)), player, Items.coal, 10));

        state.rules.attackMode = true;
        Call.onSetRules(state.rules);
        Events.fire(new GameOverEvent(player.getTeam()));
        assertEquals(1, playerDB.get(player.uuid).attackclear());

        Events.fire(new WorldLoadEvent());
        assertEquals(0L, vars.playtime());
        assertEquals(0, pluginData.powerblock.size);

        Events.fire(new PlayerConnect(player));

        Events.fire(new DepositEvent(world.tile(r.nextInt(50), r.nextInt(50)), player, Items.copper, 5));

        Player dummy = createNewPlayer(false);
        Events.fire(new PlayerJoin(dummy));

        clientHandler.handleMessage("/register hello testas123", dummy);

        clientHandler.handleMessage("/logout", dummy);

        Player dummy2 = createNewPlayer(false);
        Events.fire(new PlayerJoin(dummy2));

        clientHandler.handleMessage("/login hello testas123", dummy2);
        assertTrue(playerDB.get(dummy2.uuid).login());

        Events.fire(new PlayerLeave(dummy2));
        assertTrue(playerDB.get(dummy2.uuid).error());

        Events.fire(new PlayerChatEvent(player, "hi"));

        /*Time.update();
        unitGroup.update();
        puddleGroup.update();
        shieldGroup.update();
        bulletGroup.update();
        tileGroup.update();
        fireGroup.update();
        collisions.collideGroups(bulletGroup, unitGroup);
        collisions.collideGroups(bulletGroup, playerGroup);
        unitGroup.updateEvents();
        collisions.updatePhysics(unitGroup);
        playerGroup.update();
        effectGroup.update();*/

        player.addBuildRequest(new BuilderTrait.BuildRequest(5, 5, 0, Blocks.copperWall));
        Call.onConstructFinish(Vars.world.tile(5, 5), Blocks.copperWall, player.id, (byte) 0, Team.sharded, false);
        Events.fire(new BlockBuildEndEvent(world.tile(r.nextInt(50), r.nextInt(50)), player, Team.sharded, false));

        player.buildQueue().clear();
        player.addBuildRequest(new BuilderTrait.BuildRequest(5, 5));
        Call.onDeconstructFinish(Vars.world.tile(5, 5), Blocks.air, player.id);
        Events.fire(new BuildSelectEvent(world.tile(r.nextInt(50), r.nextInt(50)), Team.sharded, player, false));
        player.buildQueue().clear();

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
    public void test14_internal() {
        playerCore.isLocal(player);
    }

    public static int pin;

    @Test
    public void test16_complexCommand() {
        Call.onConstructFinish(world.tile(120, 120), Blocks.message, player.id, (byte) 0, Team.sharded, true);
        Events.fire(new BlockBuildEndEvent(world.tile(120, 120), player, Team.sharded, false));
        Call.setMessageBlockText(player, world.tile(120, 120), "powerblock");
        try {
            config.discordToken(new String(Files.readAllBytes(Paths.get("./token.txt"))));
            discord.start();
            discord.queue(player);
            System.out.println("PIN: " + discord.pins.get(player.name));
            sleep(10000);
        } catch (IOException | InterruptedException ignored) {
        }
    }

    @AfterClass
    public static void shutdown() {
        Core.app.getListeners().get(1).dispose();
        assertTrue(out.getLogWithNormalizedLineSeparator().contains(config.bundle.get("thread-disabled")));
    }
}