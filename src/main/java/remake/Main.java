package remake;

import arc.ApplicationListener;
import arc.Core;
import arc.files.Fi;
import arc.math.Mathf;
import arc.struct.Array;
import arc.util.CommandHandler;
import arc.util.Strings;
import arc.util.Time;
import mindustry.content.Blocks;
import mindustry.content.Mechs;
import mindustry.content.UnitTypes;
import mindustry.core.Version;
import mindustry.entities.type.BaseUnit;
import mindustry.entities.type.Player;
import mindustry.entities.type.Unit;
import mindustry.game.Difficulty;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.io.SaveIO;
import mindustry.net.Administration;
import mindustry.net.Packets;
import mindustry.plugin.Plugin;
import mindustry.type.Mech;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import org.hjson.JsonObject;
import org.mindrot.jbcrypt.BCrypt;
import remake.core.player.Database;
import remake.core.player.PlayerCore;
import remake.core.player.PlayerDB;
import remake.core.player.PlayerData;
import remake.core.plugin.Config;
import remake.core.plugin.PluginData;
import remake.external.DriverLoader;
import remake.external.StringUtils;
import remake.external.Tools;
import remake.feature.*;
import remake.internal.Bundle;
import remake.internal.CrashReport;
import remake.internal.Event;
import remake.internal.Log;
import remake.internal.thread.Threads;
import remake.internal.thread.TickTrigger;
import remake.network.Client;
import remake.network.Server;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import static mindustry.Vars.*;
import static remake.Vars.build_version;

public class Main extends Plugin {
    public static final Fi root = Core.settings.getDataDirectory().child("mods/Essentials/");
    public static final Timer timer = new Timer(true);
    public static final ExecutorService mainThread = new ThreadPoolExecutor(0, Runtime.getRuntime().availableProcessors() * 2, 10L, TimeUnit.SECONDS, new SynchronousQueue<>());

    public static final Tools tool = new Tools();
    public static final PlayerDB playerDB = new PlayerDB();
    public static final Database database = new Database();
    public static final PluginData pluginData = new PluginData();
    public static final Server server = new Server();
    public static final Client client = new Client();
    public static final Vote vote = new Vote();
    public static final PlayerCore playerCore = new PlayerCore();
    public static final ColorNick colornick = new ColorNick();
    public static final Permission perm = new Permission();
    public static final Discord discord = new Discord();

    public final ApplicationListener listener;

    public static Locale locale = new Locale(System.getProperty("user.language"), System.getProperty("user.country"));
    public static Config config;
    public static ArrayList<EventServer> eventServer = new ArrayList<>();

    public Main() throws Exception {
        // 서버 버전 확인
        if (Version.build != build_version) {
            InputStream reader = getClass().getResourceAsStream("/plugin.json");
            BufferedReader br = new BufferedReader(new InputStreamReader(reader));
            throw new Exception("Essentials " + JsonObject.readJSON(br).asObject().get("version").asString() + " plugin only works with mindustry build 104.");
        }


        // 파일 압축해제
        if (!root.exists()) {
            try {
                final String path = "configs";
                final JarFile jar = new JarFile(new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()));
                final Enumeration<JarEntry> entries = jar.entries();

                while (entries.hasMoreElements()) {
                    final String name = entries.nextElement().getName();
                    if (name.startsWith(path + "/")) {
                        InputStream reader = getClass().getResourceAsStream("/" + name);
                        root.child(name).write(reader, false);
                    }
                }
                jar.close();
            } catch (Exception e) {
                new CrashReport(e);
            }
        }

        // 설정 불러오기
        config = new Config();

        // 스레드 시작
        new TickTrigger();
        timer.scheduleAtFixedRate(new Threads(), 1000, 1000);
        mainThread.submit(colornick);

        // DB 드라이버 로딩
        new DriverLoader();

        // DB 연결
        database.connect();
        database.create();
        if (config.DBServer) database.server_start();

        // Client 연결
        if (config.clientenable) new remake.network.Client();

        // Server 시작
        if (config.serverenable) new Server();

        // 기록 시작
        if (config.logging) new ActivityLog();

        // 이벤트 시작
        new Event();

        // 서버 종료 이벤트 설정
        this.listener = new ApplicationListener() {
            @Override
            public void dispose() {
                try {
                    boolean error = false;

                    playerDB.saveAll(); // 플레이어 데이터 저장
                    pluginData.saveall(); // 플러그인 데이터 저장
                    mainThread.shutdownNow(); // 스레드 종료
                    // config.singleService.shutdownNow(); // 로그 스레드 종료
                    timer.cancel(); // 일정 시간마다 실행되는 스레드 종료
                    if (vote.status()) vote.interrupt(); // 투표 종료
                    discord.shutdownNow(); // Discord 서비스 종료
                    database.dispose(); // DB 연결 종료

                    if (config.serverenable) {
                        try {
                            Iterator<Server.service> servers = server.list.iterator();
                            while (servers.hasNext()) {
                                Server.service ser = servers.next();
                                ser.os.close();
                                ser.in.close();
                                ser.socket.close();
                                servers.remove();
                            }

                            server.serverSocket.close();
                            Log.info("server-thread-disabled");
                        } catch (Exception e) {
                            error = true;
                            Log.err("server-thread-disable-error");
                            new CrashReport(e);
                        }
                    }

                    // 클라이언트 종료
                    if (config.clientenable && client.activated) {
                        client.request(Client.Request.exit, null, null);
                        Log.info("client-thread-disabled");
                    }

                    // 모든 이벤트 서버 종료
                    for (Process value : eventServer.process) value.destroy();
                    if (!error) {
                        Log.info("thread-disabled");
                    } else {
                        Log.warn("thread-not-dead");
                    }
                } catch (Exception e) {
                    new CrashReport(e);
                }
            }
        };
        Core.app.addListener(listener);

        // Discord 서비스 시작
        discord.start();

        // 채팅 포맷 변경
        netServer.admins.addChatFilter((player, text) -> null);
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.register("gendocs", "Generate Essentials README.md", (arg) -> {
            List<String> servercommands = new ArrayList<>(Arrays.asList(
                    "help", "version", "exit", "stop", "host", "maps", "reloadmaps", "status",
                    "mods", "mod", "js", "say", "difficulty", "rules", "fillitems", "playerlimit",
                    "config", "subnet-ban", "whitelisted", "whitelist-add", "whitelist-remove",
                    "shuffle", "nextmap", "kick", "ban", "bans", "unban", "admin", "unadmin",
                    "admins", "runwave", "load", "save", "saves", "gameover", "info", "search", "gc"
            ));
            List<String> clientcommands = new ArrayList<>(Arrays.asList(
                    "help", "t", "sync"
            ));
            String serverdoc = "## Server commands\n\n| Command | Parameter | Description |\n|:---|:---|:--- |\n";
            String clientdoc = "## Client commands\n\n| Command | Parameter | Description |\n|:---|:---|:--- |\n";
            String gentime = "\nREADME.md Generated time: " + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());

            Log.info("readme-generating");

            String header = "# Essentials\n" +
                    "Add more commands to the server.\n\n" +
                    "I'm getting a lot of suggestions.<br>\n" +
                    "Please submit your idea to this repository issues or Mindustry official discord!\n\n" +
                    "## Requirements for running this plugin\n" +
                    "This plugin does a lot of disk read/write operations depending on the features usage.\n\n" +
                    "### Minimum\n" +
                    "CPU: Athlon 200GE or Intel i5 2300<br>\n" +
                    "RAM: 20MB<br>\n" +
                    "Disk: HDD capable of more than 2MB/s random read/write.\n\n" +
                    "### Recommand\n" +
                    "CPU: Ryzen 3 2200G or Intel i3 8100<br>\n" +
                    "RAM: 50MB<br>\n" +
                    "Disk: HDD capable of more than 5MB/s random read/write.\n\n" +
                    "## Installation\n\n" +
                    "Put this plugin in the ``<server folder location>/config/mods`` folder.\n\n" +
                    "## Essentials 9.0 Plans\n" +
                    "- [ ] Server\n" +
                    "  - [ ] Control server using Web server\n" +
                    "- [x] PlayerDB\n" +
                    "  - [x] DB Server without MySQL/MariaDB..\n" +
                    "- [x] Internal\n" +
                    "  - [x] Fix server can't shutdown\n" +
                    "  - [x] Make ban reason\n" +
                    "- [ ] Anti-grief\n" +
                    "  - [ ] Make anti-filter art\n" +
                    "  - [ ] Make anti-fast build/break destroy\n" +
                    "  - [ ] Make detect custom client\n" +
                    "- [ ] Soruce code rebuild\n" +
                    "  - [ ] core\n" +
                    "    - [ ] Discord\n" +
                    "    - [ ] PlayerDB\n" +
                    "  - [ ] net\n" +
                    "    - [ ] Client\n" +
                    "    - [ ] Server\n" +
                    "  - [ ] special\n" +
                    "    - [ ] DataMigration\n" +
                    "    - [ ] DriverLoader\n" +
                    "  - [ ] utils\n" +
                    "    - [ ] Bundle\n" +
                    "    - [ ] Config\n" +
                    "  - [ ] Global\n" +
                    "  - [ ] Main\n" +
                    "  - [ ] Threads\n" +
                    "    - [ ] login\n" +
                    "    - [ ] changename\n" +
                    "    - [ ] AutoRollback\n" +
                    "    - [ ] eventserver\n" +
                    "    - [ ] ColorNick\n" +
                    "    - [ ] monitorresource\n" +
                    "    - [ ] Vote\n" +
                    "    - [ ] jumpdata\n" +
                    "    - [ ] visualjump\n\n";

            StringBuilder tempbuild = new StringBuilder();
            for (CommandHandler.Command command : netServer.clientCommands.getCommandList()) {
                if (!clientcommands.contains(command.text)) {
                    String temp = "| " + command.text + " | " + StringUtils.encodeHtml(command.paramText) + " | " + command.description + " |\n";
                    tempbuild.append(temp);
                }
            }

            String tmp = header + clientdoc + tempbuild.toString() + "\n";
            tempbuild = new StringBuilder();

            for (CommandHandler.Command command : handler.getCommandList()) {
                if (!servercommands.contains(command.text)) {
                    String temp = "| " + command.text + " | " + StringUtils.encodeHtml(command.paramText) + " | " + command.description + " |\n";
                    tempbuild.append(temp);
                }
            }

            root.child("README.md").writeString(tmp + serverdoc + tempbuild.toString() + gentime);

            Log.info("success");
        });
        handler.register("admin", "<name>", "Set admin status to player.", (arg) -> {
            if (arg.length != 0) {
                Permission perm = new Permission();
                Player player = playerGroup.find(p -> p.name.equals(arg[0]));

                if (player == null) {
                    Log.warn("player-not-found");
                } else {
                    for (JsonObject.Member data : perm.permission) {
                        if (data.getName().equals("new_admin")) {
                            //PlayerDB.PlayerData p = playerDB.get(player.uuid);
                            //p.permission = "new_admin";
                            //PlayerDataSave(p);
                            Log.info("success");
                            break;
                        }
                    }
                    Log.warn("use-setperm");
                }
            } else {
                Log.warn("no-parameter");
            }
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.removeCommand("vote");
        handler.removeCommand("votekick");

        handler.<Player>register("alert", "Turn on/off alerts", (arg, player) -> {
            if (!perm.check(player, "alert")) return;

            PlayerData playerData = playerDB.get(player.uuid);
            if (playerData.alert) {
                playerData.alert = false;
                player.sendMessage(new Bundle(playerData.locale).get("alert-disable"));
            } else {
                playerData.alert = true;
                player.sendMessage(new Bundle(playerData.locale).get("alert"));
            }
        });
        handler.<Player>register("ch", "Send chat to another server.", (arg, player) -> {
            if (!perm.check(player, "ch")) return;

            PlayerData playerData = playerDB.get(player.uuid);
            if (playerData.crosschat) {
                playerData.crosschat = false;
                player.sendMessage(new Bundle(playerData.locale).get("crosschat-disable"));
            } else {
                playerData.crosschat = true;
                player.sendMessage(new Bundle(playerData.locale).get("crosschat"));
            }
        });
        handler.<Player>register("changepw", "<new_password> <new_password_repeat>", "Change account password", (arg, player) -> {
            if (!perm.check(player, "changepw")) return;

            PlayerData playerData = playerDB.get(player.uuid);
            if (!tool.checkPassword(player, playerData.accountid, arg[1], arg[2])) {
                player.sendMessage(new Bundle(playerData.locale).get("need-new-password"));
                return;
            }
            try {
                Class.forName("org.mindrot.jbcrypt.BCrypt");
                playerData.accountpw = BCrypt.hashpw(arg[0], BCrypt.gensalt(11));
                player.sendMessage(new Bundle(playerData.locale).get("success"));
            } catch (ClassNotFoundException e) {
                new CrashReport(e);
            }
        });
        handler.<Player>register("chars", "<Text...>", "Make pixel texts", (arg, player) -> {
            if (!perm.check(player, "chars")) return;
            HashMap<String, int[]> letters = new HashMap<>();

            letters.put("A", new int[]{0, 1, 1, 1, 1, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 1, 1});
            letters.put("B", new int[]{1, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0});
            letters.put("C", new int[]{0, 1, 1, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1});
            letters.put("D", new int[]{1, 1, 1, 1, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 1, 1, 1, 0});
            letters.put("E", new int[]{1, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1});
            letters.put("F", new int[]{1, 1, 1, 1, 1, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0});
            letters.put("G", new int[]{0, 1, 1, 1, 0, 1, 0, 0, 0, 1, 1, 0, 1, 0, 1, 0, 0, 1, 1, 1});
            letters.put("H", new int[]{1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1});
            letters.put("I", new int[]{1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1});
            letters.put("J", new int[]{1, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 1, 1, 1, 0, 1, 0, 0, 0, 0});
            letters.put("K", new int[]{1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1});
            letters.put("L", new int[]{1, 1, 1, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1});
            letters.put("M", new int[]{1, 1, 1, 1, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 1, 1, 1, 1});
            letters.put("N", new int[]{1, 1, 1, 1, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1});
            letters.put("O", new int[]{0, 1, 1, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 1, 1, 1, 0});
            letters.put("P", new int[]{1, 1, 1, 1, 1, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0});
            letters.put("Q", new int[]{0, 1, 1, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 1, 1, 1, 0, 0, 0, 0, 0, 1});
            letters.put("R", new int[]{1, 1, 1, 1, 1, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 1});
            letters.put("S", new int[]{1, 1, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 1, 1, 1});
            letters.put("T", new int[]{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0});
            letters.put("U", new int[]{1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0});
            letters.put("V", new int[]{1, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 1, 1, 0, 0});
            letters.put("W", new int[]{1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0});
            letters.put("X", new int[]{1, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1});
            letters.put("Y", new int[]{1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0});
            letters.put("Z", new int[]{1, 0, 0, 1, 1, 1, 0, 1, 0, 1, 1, 1, 0, 0, 1});

            letters.put("!", new int[]{1, 1, 1, 1, 0, 1});
            letters.put("?", new int[]{0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 0, 0, 0});

            letters.put(" ", new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0});

            String[] texts = arg[0].split("");
            Tile tile = world.tile(player.tileX(), player.tileY());

            for (String text : texts) {
                ArrayList<int[]> pos = new ArrayList<>();
                int[] target = letters.get(text.toUpperCase());
                int xv = 0;
                int yv = 0;
                switch (target.length) {
                    case 20:
                        xv = 5;
                        yv = 4;
                        break;
                    case 15:
                        xv = 5;
                        yv = 3;
                        break;
                    case 18:
                        xv = 6;
                        yv = 3;
                        break;
                    case 25:
                        xv = 5;
                        yv = 5;
                        break;
                    case 6:
                        xv = 6;
                        yv = 1;
                        break;
                    case 10:
                        xv = 2;
                        yv = 5;
                        break;
                }
                for (int y = 0; y < yv; y++) {
                    for (int x = 0; x < xv; x++) {
                        pos.add(new int[]{y, -x});
                    }
                }
                for (int a = 0; a < pos.size(); a++) {
                    if (target[a] == 1) {
                        Call.onConstructFinish(world.tile(tile.x + pos.get(a)[0], tile.y + pos.get(a)[1]), Blocks.plastaniumWall, 0, (byte) 0, Team.sharded, true);
                    } else {
                        Call.onDeconstructFinish(world.tile(tile.x + pos.get(a)[0], tile.y + pos.get(a)[1]), Blocks.air, 0);
                    }
                }
                tile = world.tile(tile.x + (xv + 1), tile.y);
            }
        });
        handler.<Player>register("color", "Enable color nickname", (arg, player) -> {
            if (!perm.check(player, "color")) return;
            PlayerData playerData = playerDB.get(player.uuid);
            if (playerData.colornick) {
                playerData.colornick = false;
                player.sendMessage(new Bundle(playerData.locale).get("colornick-disable"));
            } else {
                playerData.colornick = true;
                player.sendMessage(new Bundle(playerData.locale).get("colornick"));
            }

        });
        handler.<Player>register("difficulty", "<difficulty>", "Set server difficulty", (arg, player) -> {
            if (!perm.check(player, "difficulty")) return;
            PlayerData playerData = playerDB.get(player.uuid);
            try {
                Difficulty.valueOf(arg[0]);
                player.sendMessage(new Bundle(playerData.locale).get("difficulty-set", arg[0]));
            } catch (IllegalArgumentException e) {
                player.sendMessage(new Bundle(playerData.locale).get("difficulty-not-found", arg[0]));
            }
        });
        handler.<Player>register("killall", "Kill all enemy units", (arg, player) -> {
            if (!perm.check(player, "killall")) return;
            for (int a = 0; a < Team.all().length; a++) unitGroup.all().each(Unit::kill);
            player.sendMessage(new Bundle(playerDB.get(player.uuid).locale).get("success"));
        });
        handler.<Player>register("event", "<host/join> <roomname> [map] [gamemode]", "Host your own server", (arg, player) -> {
            if (!perm.check(player, "event")) return;
            PlayerData playerData = playerDB.get(player.uuid);
            Thread t = new Thread(() -> {
                switch (arg[0]) {
                    case "host":
                        Thread work = new Thread(() -> {
                            PlayerData target = playerDB.get(player.uuid);
                            if (target.level > 20 || player.isAdmin) {
                                if (arg.length == 2) {
                                    player.sendMessage(new Bundle(playerData.locale).get("event-host-no-mapname"));
                                    return;
                                }
                                if (arg.length == 3) {
                                    player.sendMessage(new Bundle(playerData.locale).get("event-host-no-gamemode"));
                                    return;
                                }
                                player.sendMessage(new Bundle(playerData.locale).get("event-making"));

                                String[] range = config.eventport.split("-");
                                int firstport = Integer.parseInt(range[0]);
                                int lastport = Integer.parseInt(range[1]);
                                int customport = ThreadLocalRandom.current().nextInt(firstport, lastport + 1);

                                pluginData.eventservers.add(new PluginData.eventservers(arg[1], customport));

                                Threads.eventserver es = new Threads.eventserver(arg[1], arg[2], arg[3], customport);
                                es.roomname = arg[1];
                                es.map = arg[2];
                                if (arg[3].equals("wave")) {
                                    es.gamemode = "wave";
                                } else {
                                    es.gamemode = arg[3];
                                }
                                es.customport = customport;
                                es.start();
                                try {
                                    es.join();
                                } catch (InterruptedException e) {
                                    printError(e);
                                }
                                Log.info("event-host-opened", player.name, customport);

                                target.connected = false;
                                target.connserver = "none";
                                if (isLocal(netServer.admins.getInfo(player.uuid).lastIP)) {
                                    Call.onConnect(player.con, "127.0.0.1", customport);
                                } else {
                                    Call.onConnect(player.con, hostip, customport);
                                }
                                Log.info(hostip + ":" + customport);
                            } else {
                                player.sendMessage(new Bundle(playerData.locale).get("event-level"));
                            }
                        });
                        work.start();
                        break;
                    case "join":
                        for (PluginData.eventservers server : data.eventservers) {
                            if (server.roomname.equals(arg[1])) {
                                PlayerData val = playerDB.get(player.uuid);
                                val.connected = false;
                                val.connserver = "none";
                                PlayerDataSave(val);
                                Call.onConnect(player.con, hostip, server.port);
                                Log.info(hostip + ":" + server.port);
                                break;
                            }
                        }
                        break;
                    default:
                        player.sendMessage(new Bundle(playerData.locale).get("wrong-command"));
                        break;
                }
            });
            t.start();
        });
        handler.<Player>register("email", "<key>", "Email Authentication", (arg, player) -> {
            for (PluginData.maildata data : pluginData.emailauth) {
                if (data.uuid.equals(player.uuid)) {
                    if (data.authkey.equals(arg[0])) {
                        if (playerDB.register(player, data.id, data.pw, "emailauth", data.email)) {
                            playerDB.load(player);
                            return;
                        }
                    } else {
                        player.sendMessage("You have entered an incorrect authentication key.");
                    }
                } else {
                    player.sendMessage("You didn't enter your email information when you registered.");
                }
            }
        });
        handler.<Player>register("help", "[page]", "Show command lists", (arg, player) -> {
            if (arg.length > 0 && !Strings.canParseInt(arg[0])) {
                player.sendMessage(new Bundle(playerDB.get(player.uuid).locale).get("page-number"));
                return;
            }

            ArrayList<String> temp = new ArrayList<>();
            for (int a = 0; a < netServer.clientCommands.getCommandList().size; a++) {
                CommandHandler.Command command = netServer.clientCommands.getCommandList().get(a);
                if (perm.check(player, command.text) || command.text.equals("t") || command.text.equals("sync")) {
                    temp.add("[orange] /" + command.text + " [white]" + command.paramText + " [lightgray]- " + command.description + "\n");
                }
            }

            List<String> deduped = temp.stream().distinct().collect(Collectors.toList());

            StringBuilder result = new StringBuilder();
            int perpage = 8;
            int page = arg.length > 0 ? Strings.parseInt(arg[0]) : 1;
            int pages = Mathf.ceil((float) deduped.size() / perpage);

            page--;

            if (page > pages || page < 0) {
                player.sendMessage("[scarlet]'page' must be a number between[orange] 1[] and[orange] " + pages + "[scarlet].");
                return;
            }

            result.append(Strings.format("[orange]-- Commands Page[lightgray] {0}[gray]/[lightgray]{1}[orange] --\n", (page + 1), pages));
            for (int a = perpage * page; a < Math.min(perpage * (page + 1), deduped.size()); a++) {
                result.append(deduped.get(a));
            }
            player.sendMessage(result.toString().substring(0, result.length() - 1));
        });
        handler.<Player>register("info", "Show your information", (arg, player) -> {
            if (!perm.check(player, "info")) return;
            PlayerData playerData = playerDB.get(player.uuid);
            Bundle bundle = new Bundle(playerData.locale);
            String datatext = "[#DEA82A]" + new Bundle(playerData.locale).get("player-info") + "[]\n" +
                    "[#2B60DE]====================================[]\n" +
                    "[green]" + bundle.get("player-name") + "[] : " + player.name + "[white]\n" +
                    "[green]" + bundle.get("player-country") + "[] : " + locale.getDisplayCountry() + "\n" +
                    "[green]" + bundle.get("player-placecount") + "[] : " + playerData.placecount + "\n" +
                    "[green]" + bundle.get("player-breakcount") + "[] : " + playerData.breakcount + "\n" +
                    "[green]" + bundle.get("player-killcount") + "[] : " + playerData.killcount + "\n" +
                    "[green]" + bundle.get("player-deathcount") + "[] : " + playerData.deathcount + "\n" +
                    "[green]" + bundle.get("player-joincount") + "[] : " + playerData.joincount + "\n" +
                    "[green]" + bundle.get("player-kickcount") + "[] : " + playerData.kickcount + "\n" +
                    "[green]" + bundle.get("player-level") + "[] : " + playerData.level + "\n" +
                    "[green]" + bundle.get("player-reqtotalexp") + "[] : " + playerData.reqtotalexp + "\n" +
                    "[green]" + bundle.get("player-firstdate") + "[] : " + playerData.firstdate + "\n" +
                    "[green]" + bundle.get("player-lastdate") + "[] : " + playerData.lastdate + "\n" +
                    "[green]" + bundle.get("player-playtime") + "[] : " + playerData.playtime + "\n" +
                    "[green]" + bundle.get("player-attackclear") + "[] : " + playerData.attackclear + "\n" +
                    "[green]" + bundle.get("player-pvpwincount") + "[] : " + playerData.pvpwincount + "\n" +
                    "[green]" + bundle.get("player-pvplosecount") + "[] : " + playerData.pvplosecount + "\n" +
                    "[green]" + bundle.get("player-pvpbreakout") + "[] : " + playerData.pvpbreakout;
            Call.onInfoMessage(player.con, datatext);
        });
        handler.<Player>register("jump", "<zone/count/total> <touch> [serverip] [range]", "Create a server-to-server jumping zone.", (arg, player) -> {
            if (!perm.check(player, "jump")) return;
            PlayerData playerData = playerDB.get(player.uuid);
            switch (arg[0]) {
                case "zone":
                    if (arg.length != 4) {
                        player.sendMessage(new Bundle(playerData.locale).get("jump-incorrect"));
                        return;
                    }
                    int size;
                    try {
                        size = Integer.parseInt(arg[3]);
                    } catch (Exception ignored) {
                        player.sendMessage(new Bundle(playerData.locale).get("jump-not-int"));
                        return;
                    }

                    int tf = player.tileX() + size;
                    int ty = player.tileY() + size;

                    pluginData.jumpzone.add(new PluginData.jumpzone(world.tile(player.tileX(), player.tileY()), world.tile(tf, ty), Boolean.parseBoolean(arg[1]), arg[2]));
                    player.sendMessage(new Bundle(playerData.locale).get("jump-added"));
                    break;
                case "count":
                    pluginData.jumpcount.add(new PluginData.jumpcount(world.tile(player.tileX(), player.tileY()), arg[2], 0, 0));
                    player.sendMessage(new Bundle(playerData.locale).get("jump-added"));
                    break;
                case "total":
                    // tilex, tiley, total players, number length
                    pluginData.jumptotal.add(new PluginData.jumptotal(world.tile(player.tileX(), player.tileY()), 0, 0));
                    player.sendMessage(new Bundle(playerData.locale).get("jump-added"));
                    break;
                default:
                    player.sendMessage(new Bundle(playerData.locale).get("command-invalid"));
            }
        });
        handler.<Player>register("kickall", "Kick all players", (arg, player) -> {
            if (!perm.check(player, "kickall")) return;
            mindustry.Vars.netServer.kickAll(Packets.KickReason.kick);
        });
        handler.<Player>register("kill", "<player>", "Kill player.", (arg, player) -> {
            if (!perm.check(player, "kill")) return;
            Player other = mindustry.Vars.playerGroup.find(p -> p.name.equalsIgnoreCase(arg[0]));
            if (other == null) {
                player.sendMessage(new Bundle(playerDB.get(player.uuid).locale).get("player-not-found"));
                return;
            }
            Player.onPlayerDeath(other);
        });
        handler.<Player>register("login", "<id> <password>", "Access your account", (arg, player) -> {
            PlayerData playerData = playerDB.get(player.uuid);
            if (config.loginenable) {
                if (playerData.error) {
                    if (PlayerDB.login(player, arg[0], arg[1])) {
                        if (config.passwordmethod.equals("discord")) {
                            playerDB.load(player, arg[0]);
                        } else {
                            playerDB.load(player);
                        }
                        player.sendMessage(new Bundle(playerData.locale).get("login-success"));
                    } else {
                        player.sendMessage("[green][EssentialPlayer] [scarlet]Login failed/로그인 실패!!");
                    }
                } else {
                    if (config.passwordmethod.equals("mixed")) {
                        if (PlayerDB.login(player, arg[0], arg[1])) Call.onConnect(player.con, hostip, 7060);
                    } else {
                        player.sendMessage("[green][EssentialPlayer] [scarlet]You're already logged./이미 로그인한 상태입니다.");
                    }
                }
            } else {
                player.sendMessage(new Bundle(playerData.locale).get("login-not-use"));
            }
        });
        handler.<Player>register("logout", "Log-out of your account.", (arg, player) -> {
            if (!perm.check(player, "logout")) return;

            PlayerData playerData = playerDB.get(player.uuid);
            Bundle bundle = new Bundle(playerData.locale);
            if (config.loginenable) {
                playerData.connected = false;
                playerData.connserver = "none";
                playerData.uuid = "LogoutAAAAA="; // TODO set new uuid
                Call.onKick(player.con, bundle.get("logout"));
            } else {
                player.sendMessage(bundle.get("login-not-use"));
            }
        });
        handler.<Player>register("maps", "[page]", "Show server maps", (arg, player) -> {
            if (!perm.check(player, "maps")) return;
            StringBuilder build = new StringBuilder();
            int page = arg.length > 0 ? Strings.parseInt(arg[0]) : 1;
            int pages = Mathf.ceil((float) maplist.size / 6);

            page--;
            if (page > pages || page < 0) {
                player.sendMessage("[scarlet]'page' must be a number between[orange] 1[] and[orange] " + pages + "[scarlet].");
                return;
            }

            build.append("[green]==[white] Server maps page ").append(page).append("/").append(pages).append(" [green]==[white]\n");
            for (int a = 6 * page; a < Math.min(6 * (page + 1), maplist.size); a++) {
                build.append("[gray]").append(a).append("[] ").append(maplist.get(a).name()).append("\n");
            }
            player.sendMessage(build.toString());
        });
        handler.<Player>register("me", "<text...>", "broadcast * message", (arg, player) -> {
            if (!perm.check(player, "me")) return;
            Call.sendMessage("[orange]*[] " + player.name + "[white] : " + arg[0]);
        });
        handler.<Player>register("motd", "Show server motd.", (arg, player) -> {
            if (!perm.check(player, "motd")) return;
            String motd = getmotd(player);
            int count = motd.split("\r\n|\r|\n").length;
            if (count > 10) {
                Call.onInfoMessage(player.con, motd);
            } else {
                player.sendMessage(motd);
            }
        });
        handler.<Player>register("players", "Show players list", (arg, player) -> {
            if (!perm.check(player, "players")) return;
            StringBuilder build = new StringBuilder();
            int page = arg.length > 0 ? Strings.parseInt(arg[0]) : 1;
            int pages = Mathf.ceil((float) players.size / 6);

            page--;
            if (page > pages || page < 0) {
                player.sendMessage("[scarlet]'page' must be a number between[orange] 1[] and[orange] " + pages + "[scarlet].");
                return;
            }

            build.append("[green]==[white] Players list page ").append(page).append("/").append(pages).append(" [green]==[white]\n");
            for (int a = 6 * page; a < Math.min(6 * (page + 1), players.size); a++) {
                build.append("[gray]").append(a).append("[] ").append(players.get(a).name).append("\n");
            }
            player.sendMessage(build.toString());
        });
        handler.<Player>register("save", "Auto rollback map early save", (arg, player) -> {
            if (!perm.check(player, "save")) return;
            Fi file = saveDirectory.child(config.getSlotnumber() + "." + saveExtension);
            SaveIO.save(file);
            player.sendMessage(bundle(playerDB.get(player.uuid).locale, "mapsaved"));
        });
        handler.<Player>register("reset", "<zone/count/total> [ip]", "Remove a server-to-server jumping zone data.", (arg, player) -> {
            if (!perm.check(player, "reset")) return;
            PlayerData playerData = playerDB.get(player.uuid);
            switch (arg[0]) {
                case "zone":
                    for (int a = 0; a < data.jumpzone.size(); a++) {
                        if (arg.length != 2) {
                            player.sendMessage(bundle(playerData.locale, "no-parameter"));
                            return;
                        }
                        if (arg[1].equals(data.jumpzone.get(a).ip)) {
                            data.jumpzone.remove(a);
                            for (Thread value : visualjump.thread) {
                                value.interrupt();
                            }
                            visualjump.thread.clear();
                            visualjump.main();
                            player.sendMessage(new Bundle(playerData.locale).get("success"));
                            break;
                        }
                    }
                    break;
                case "count":
                    data.jumpcount.clear();
                    player.sendMessage(bundle(playerData.locale, "jump-reset", "count"));
                    break;
                case "total":
                    data.jumptotal.clear();
                    player.sendMessage(bundle(playerData.locale, "jump-reset", "total"));
                    break;
                default:
                    player.sendMessage(bundle(playerData.locale, "command-invalid"));
                    break;
            }
        });
        switch (config.getPasswordmethod()) {
            case "email":
                handler.<Player>register("register", "<accountid> <password> <email>", "Register account", (arg, player) -> {
                    if (config.isLoginenable()) {
                        if (playerDB.register(player, arg[0], arg[1], "email", arg[2])) {
                            playerDB.load(player);
                            player.sendMessage("[green][Essentials] [white]Register success!/계정 등록 성공!");
                        } else {
                            player.sendMessage("[green][Essentials] [scarlet]Register failed/계정 등록 실패!");
                        }
                    } else {
                        player.sendMessage(bundle(playerDB.get(player.uuid).locale, "login-not-use"));
                    }
                });
                break;
                /*
            case "sms":
                handler.<Player>register("register", "<accountid> <password> <phone-number>", "Register account", (arg, player) -> {
                    if (config.isLoginenable()) {
                        PlayerDB playerdb = new PlayerDB();
                        if (playerdb.register(player, arg[0], arg[1], "sms", arg[2])) {
                            setTeam(player);
                            Call.onPlayerDeath(player);
                            player.sendMessage("[green][Essentials] [white]Register success!/계정 등록 성공!");
                        } else {
                            player.sendMessage("[green][Essentials] [scarlet]Register failed/계정 등록 실패!");
                        }
                    } else {
                        player.sendMessage(bundle(player, "login-not-use"));
                    }
                });
                break;*/
            case "password":
                handler.<Player>register("register", "<accountid> <password>", "Register account", (arg, player) -> {
                    if (config.isLoginenable()) {
                        if (playerDB.register(player, arg[0], arg[1], "password")) {
                            if (mindustry.Vars.state.rules.pvp) {
                                int index = player.getTeam().id + 1;
                                while (index != player.getTeam().id) {
                                    if (index >= Team.all().length) {
                                        index = 0;
                                    }
                                    if (!mindustry.Vars.state.teams.get(Team.all()[index]).cores.isEmpty()) {
                                        player.setTeam(Team.all()[index]);
                                        break;
                                    }
                                    index++;
                                }
                            } else {
                                player.setTeam(Team.sharded);
                            }
                            Call.onPlayerDeath(player);
                            player.sendMessage("[green][Essentials] [white]Register success!/계정 등록 성공!");
                        } else {
                            player.sendMessage("[green][Essentials] [scarlet]Register failed/계정 등록 실패!");
                        }
                    } else {
                        player.sendMessage(bundle(playerDB.get(player.uuid).locale, "login-not-use"));
                    }
                });
                break;
            case "discord":
                handler.<Player>register("register", "Register account", (arg, player) -> player.sendMessage("Join discord and use !signup command!\n" + config.getDiscordLink()));
                break;
            /*case "mixed":
                handler.<Player>register("register", "<accountid> <password>", "Register account", (arg, player) -> {
                    if (config.isLoginenable()) {
                        if (playerDB.register(player, arg[0], arg[1], true)) {
                            PlayerDataRemove(player.uuid);
                            Thread t = new Thread(() -> Call.onConnect(player.con,getip(),7060));
                            t.start();
                        } else {
                            player.sendMessage("[green][Essentials] [scarlet]Register failed/계정 등록 실패!");
                        }
                    } else {
                        player.sendMessage(bundle(player, "login-not-use"));
                    }
                });
                break;*/
        }
        handler.<Player>register("spawn", "<mob_name> <count> [team] [playername]", "Spawn mob in player position", (arg, player) -> {
            if (!perm.check(player, "spawn")) return;
            PlayerData playerData = playerDB.get(player.uuid);
            UnitType targetunit;
            switch (arg[0]) {
                case "draug":
                    targetunit = UnitTypes.draug;
                    break;
                case "spirit":
                    targetunit = UnitTypes.spirit;
                    break;
                case "phantom":
                    targetunit = UnitTypes.phantom;
                    break;
                case "wraith":
                    targetunit = UnitTypes.wraith;
                    break;
                case "ghoul":
                    targetunit = UnitTypes.ghoul;
                    break;
                case "revenant":
                    targetunit = UnitTypes.revenant;
                    break;
                case "lich":
                    targetunit = UnitTypes.lich;
                    break;
                case "reaper":
                    targetunit = UnitTypes.reaper;
                    break;
                case "dagger":
                    targetunit = UnitTypes.dagger;
                    break;
                case "crawler":
                    targetunit = UnitTypes.crawler;
                    break;
                case "titan":
                    targetunit = UnitTypes.titan;
                    break;
                case "fortress":
                    targetunit = UnitTypes.fortress;
                    break;
                case "eruptor":
                    targetunit = UnitTypes.eruptor;
                    break;
                case "chaosArray":
                    targetunit = UnitTypes.chaosArray;
                    break;
                case "eradicator":
                    targetunit = UnitTypes.eradicator;
                    break;
                default:
                    player.sendMessage(new Bundle(playerData.locale).get("mob-not-found"));
                    return;
            }
            int count;
            try {
                count = Integer.parseInt(arg[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(new Bundle(playerData.locale).get("mob-spawn-not-number"));
                return;
            }
            if (config.getSpawnlimit() == count) {
                player.sendMessage(new Bundle(playerData.locale).get("spawn-limit"));
                return;
            }
            Team targetteam = null;
            if (arg.length >= 3) {
                switch (arg[2]) {
                    case "sharded":
                        targetteam = Team.sharded;
                        break;
                    case "blue":
                        targetteam = Team.blue;
                        break;
                    case "crux":
                        targetteam = Team.crux;
                        break;
                    case "derelict":
                        targetteam = Team.derelict;
                        break;
                    case "green":
                        targetteam = Team.green;
                        break;
                    case "purple":
                        targetteam = Team.purple;
                        break;
                    default:
                        player.sendMessage(new Bundle(playerData.locale).get("team-not-found"));
                        return;
                }
            }
            Player targetplayer = null;
            if (arg.length >= 4) {
                Player target = playerGroup.find(p -> p.name.equals(arg[3]));
                if (target == null) {
                    player.sendMessage(new Bundle(playerData.locale).get("player-not-found"));
                    return;
                } else {
                    targetplayer = target;
                }
            }
            if (targetteam != null) {
                if (targetplayer != null) {
                    for (int i = 0; count > i; i++) {
                        BaseUnit baseUnit = targetunit.create(targetplayer.getTeam());
                        baseUnit.set(targetplayer.getX(), targetplayer.getY());
                        baseUnit.add();
                    }
                } else {
                    for (int i = 0; count > i; i++) {
                        BaseUnit baseUnit = targetunit.create(targetteam);
                        baseUnit.set(player.getX(), player.getY());
                        baseUnit.add();
                    }
                }
            } else {
                for (int i = 0; count > i; i++) {
                    BaseUnit baseUnit = targetunit.create(player.getTeam());
                    baseUnit.set(player.getX(), player.getY());
                    baseUnit.add();
                }
            }
        });
        handler.<Player>register("setperm", "<player_name> <group>", "Set player permission", (arg, player) -> {
            if (!perm.check(player, "setperm")) return;
            PlayerData playerData = playerDB.get(player.uuid);
            Player target = playerGroup.find(p -> p.name.equals(arg[0]));
            if (target == null) {
                player.sendMessage(new Bundle(playerData.locale).get("player-not-found"));
                return;
            }
            for (JsonObject.Member perm : perm.permission) {
                if (perm.getName().equals(arg[0])) {
                    PlayerData val = PlayerData(target.uuid);
                    val.permission = arg[1];
                    PlayerDataSave(val);
                    player.sendMessage(new Bundle(playerData.locale).get("success"));
                    target.sendMessage(bundle(playerData.locale, "perm-changed"));
                    return;
                }
            }
            player.sendMessage(new Bundle(playerData.locale).get("perm-group-not-found"));
        });
        handler.<Player>register("spawn-core", "<smail/normal/big>", "Make new core", (arg, player) -> {
            if (!perm.check(player, "spawn-core")) return;
            Block core = Blocks.coreShard;
            switch (arg[0]) {
                case "normal":
                    core = Blocks.coreFoundation;
                    break;
                case "big":
                    core = Blocks.coreNucleus;
                    break;
            }
            Call.onConstructFinish(world.tile(player.tileX(), player.tileY()), core, 0, (byte) 0, player.getTeam(), false);
        });
        handler.<Player>register("setmech", "<Mech> [player]", "Set player mech", (arg, player) -> {
            if (!perm.check(player, "setmech")) return;
            PlayerData playerData = playerDB.get(player.uuid);
            Mech mech = Mechs.starter;
            switch (arg[0]) {
                case "alpha":
                    mech = Mechs.alpha;
                    break;
                case "dart":
                    mech = Mechs.dart;
                    break;
                case "delta":
                    mech = Mechs.glaive;
                    break;
                case "javalin":
                    mech = Mechs.javelin;
                    break;
                case "omega":
                    mech = Mechs.omega;
                    break;
                case "tau":
                    mech = Mechs.tau;
                    break;
                case "trident":
                    mech = Mechs.trident;
                    break;
            }
            if (arg.length == 1) {
                for (Player p : playerGroup.all()) {
                    p.mech = mech;
                }
            } else {
                Player target = playerGroup.find(p -> p.name.equals(arg[1]));
                if (target == null) {
                    player.sendMessage(new Bundle(playerData.locale).get("player-not-found"));
                    return;
                }
                target.mech = mech;
            }
            player.sendMessage(bundle(playerData.locale, "success"));
        });
        handler.<Player>register("status", "Show server status", (arg, player) -> {
            if (!perm.check(player, "status")) return;
            PlayerData playerData = playerDB.get(player.uuid);
            player.sendMessage(nnew Bundle(playerData.locale).get("server-status"));
            player.sendMessage("[#2B60DE]========================================[]");
            float fps = Math.round((int) 60f / Time.delta());
            int idb = 0;
            int ipb = 0;

            Array<Administration.PlayerInfo> bans = mindustry.Vars.netServer.admins.getBanned();
            for (Administration.PlayerInfo ignored : bans) {
                idb++;
            }

            Array<String> ipbans = mindustry.Vars.netServer.admins.getBannedIPs();
            for (String ignored : ipbans) {
                ipb++;
            }
            int bancount = idb + ipb;
            player.sendMessage(nnew Bundle(playerData.locale).get("server-status-banstat", fps, mindustry.Vars.playerGroup.size(), bancount, idb, ipb, threads.playtime, threads.uptime, plugin_version));
        });
        handler.<Player>register("suicide", "Kill yourself.", (arg, player) -> {
            if (!perm.check(player, "suicide")) return;
            Player.onPlayerDeath(player);
            if (playerGroup != null && playerGroup.size() > 0) {
                allsendMessage("suicide", player.name);
            }
        });
        handler.<Player>register("team", "[Team...]", "Change team (PvP only)", (arg, player) -> {
            if (!perm.check(player, "team")) return;
            PlayerData playerData = playerDB.get(player.uuid);
            if (mindustry.Vars.state.rules.pvp) {
                int i = player.getTeam().id + 1;
                while (i != player.getTeam().id) {
                    if (i >= Team.all().length) i = 0;
                    if (!mindustry.Vars.state.teams.get(Team.all()[i]).cores.isEmpty()) {
                        player.setTeam(Team.all()[i]);
                        break;
                    }
                    i++;
                }
                Call.onPlayerDeath(player);
            } else {
                player.sendMessage(new Bundle(playerData.locale).get("command-only-pvp"));
            }
        });
        handler.<Player>register("tempban", "<player> <time> <reason>", "Temporarily ban player. time unit: 1 hours", (arg, player) -> {
            if (!perm.check(player, "tempban")) return;
            PlayerData playerData = playerDB.get(player.uuid);
            Player other = null;
            for (Player p : playerGroup.all()) {
                boolean result = p.name.contains(arg[0]);
                if (result) {
                    other = p;
                }
            }
            if (other != null) {
                int bantimeset = Integer.parseInt(arg[1]);
                PlayerDB.addtimeban(other.name, other.uuid, bantimeset, arg[2]);
                other.con.kick("Temp kicked");
                for (int a = 0; a < playerGroup.size(); a++) {
                    Player current = playerGroup.all().get(a);
                    PlayerData target = PlayerData(current.uuid);
                    current.sendMessage(bundle(target.locale, "ban-temp", other.name, player.name));
                }
            } else {
                player.sendMessage(new Bundle(playerData.locale).get("player-not-found"));
            }
        });
        handler.<Player>register("time", "Show server time", (arg, player) -> {
            if (!perm.check(player, "time")) return;
            PlayerData playerData = playerDB.get(player.uuid);
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd a hh:mm.ss");
            String nowString = now.format(dateTimeFormatter);
            player.sendMessage(new Bundle(playerData.locale).get("servertime", nowString));
        });
        handler.<Player>register("tp", "<player>", "Teleport to other players", (arg, player) -> {
            if (!perm.check(player, "tp")) return;
            PlayerData playerData = playerDB.get(player.uuid);
            if (player.isMobile) {
                player.sendMessage(new Bundle(playerData.locale).get("tp-not-support"));
                return;
            }
            Player other = null;
            for (Player p : playerGroup.all()) {
                boolean result = p.name.contains(arg[0]);
                if (result) {
                    other = p;
                }
            }
            if (other == null) {
                player.sendMessage(new Bundle(playerData.locale).get("player-not-found"));
                return;
            }
            player.setNet(other.getX(), other.getY());
        });
        handler.<Player>register("tpp", "<player> <player>", "Teleport to other players", (arg, player) -> {
            if (!perm.check(player, "tpp")) return;
            PlayerData playerData = playerDB.get(player.uuid);
            Player other1 = null;
            Player other2 = null;
            for (Player p : playerGroup.all()) {
                boolean result1 = p.name.contains(arg[0]);
                if (result1) {
                    other1 = p;
                }
                boolean result2 = p.name.contains(arg[1]);
                if (result2) {
                    other2 = p;
                }
            }

            if (other1 == null || other2 == null) {
                player.sendMessage(new Bundle(playerData.locale).get("player-not-found"));
                return;
            }
            if (!other1.isMobile || !other2.isMobile) {
                other1.setNet(other2.x, other2.y);
            } else {
                player.sendMessage(new Bundle(playerData.locale).get("tp-ismobile"));
            }
        });
        handler.<Player>register("tppos", "<x> <y>", "Teleport to coordinates", (arg, player) -> {
            if (!perm.check(player, "tppos")) return;
            PlayerData playerData = playerDB.get(player.uuid);
            int x;
            int y;
            try {
                x = Integer.parseInt(arg[0]);
                y = Integer.parseInt(arg[1]);
            } catch (Exception ignored) {
                player.sendMessage(new Bundle(playerData.locale).get("tp-not-int"));
                return;
            }
            player.setNet(x, y);
        });
        handler.<Player>register("tr", "Enable/disable Translate all chat", (arg, player) -> {
            if (!perm.check(player, "tr")) return;
            PlayerData playerData = playerDB.get(player.uuid);
            if (playerData.translate) {
                playerData.translate = false;
                player.sendMessage(new Bundle(playerData.locale).get("translate"));
            } else {
                playerData.translate = true;
                player.sendMessage(new Bundle(playerData.locale).get("translate-disable"));
            }

        });
        if (config.isVoteEnable()) {
            handler.<Player>register("vote", "<mode> [parameter...]", "Voting system (Use /vote to check detail commands)", (arg, player) -> {
                if (!perm.check(player, "vote")) return;
                PlayerData playerData = playerDB.get(player.uuid);
                if (isvoting) {
                    player.sendMessage(new Bundle(playerData.locale).get("vote-in-processing"));
                    return;
                }
                if (arg.length == 2) {
                    if (arg[0].equals("kick")) {
                        Player other = mindustry.Vars.playerGroup.find(p -> p.name.equalsIgnoreCase(arg[1]));
                        if (other == null) other = players.get(Integer.parseInt(arg[1]));
                        if (other == null) {
                            player.sendMessage(new Bundle(playerData.locale).get("player-not-found"));
                            return;
                        }
                        if (other.isAdmin) {
                            player.sendMessage(new Bundle(playerData.locale).get("vote-target-admin"));
                            return;
                        }
                        // 강퇴 투표
                        new Vote(player, arg[0], other);
                    } else if (arg[0].equals("map")) {
                        // 맵 투표
                        mindustry.maps.Map world = maps.all().find(map -> map.name().equalsIgnoreCase(arg[1].replace('_', ' ')) || map.name().equalsIgnoreCase(arg[1]));
                        if (world == null) world = maplist.get(Integer.parseInt(arg[1]));
                        if (world == null) {
                            player.sendMessage(new Bundle(playerData.locale).get("vote-map-not-found"));
                        } else {
                            new Vote(player, arg[0], world);
                        }
                    }
                } else {
                    if (arg.length == 0) {
                        player.sendMessage(new Bundle(playerData.locale).get("vote-list"));
                        return;
                    }
                    if (arg[1].equals("gamemode")) {
                        player.sendMessage(new Bundle(playerData.locale).get("vote-list-gamemode"));
                        return;
                    }
                    if (arg[0].equals("map") || arg[0].equals("kick")) {
                        player.sendMessage(new Bundle(playerData.locale).get("vote-map-not-found"));
                        return;
                    }
                    // 게임 오버, wave 넘어가기, 롤백
                    new Vote(player, arg[0]);
                }
            });
        }
        handler.<Player>register("weather", "<day,eday,night,enight>", "Change map light", (arg, player) -> {
            if (!perm.check(player, "weather")) return;
            // Command idea from Minecraft EssentialsX and Quezler's plugin!
            // Useful with the Quezler's plugin.
            state.rules.lighting = true;
            switch (arg[0]) {
                case "day":
                    state.rules.ambientLight.a = 0f;
                    break;
                case "eday":
                    state.rules.ambientLight.a = 0.3f;
                    break;
                case "night":
                    state.rules.ambientLight.a = 0.7f;
                    break;
                case "enight":
                    state.rules.ambientLight.a = 0.85f;
                    break;
                default:
                    return;
            }
            Call.onSetRules(state.rules);
            player.sendMessage("DONE!");
        });
        handler.<Player>register("mute", "<Player_name>", "Mute/unmute player", (arg, player) -> {
            if (!perm.check(player, "mute")) return;
            Player other = mindustry.Vars.playerGroup.find(p -> p.name.equalsIgnoreCase(arg[0]));
            PlayerData playerData = playerDB.get(player.uuid);
            if (other == null) {
                player.sendMessage(new Bundle(playerData.locale).get("player-not-found"));
            } else {
                PlayerData target = PlayerData(other.uuid);
                if (target.mute) {
                    target.mute = false;
                    player.sendMessage(bundle(playerData.locale, "player-unmute", target.name));
                } else {
                    target.mute = true;
                    player.sendMessage(bundle(playerData.locale, "player-muted", target.name));
                }
                PlayerDataSet(target);
            }
        });
        handler.<Player>register("votekick", "[player_name]", "Player kick starts voting.", (arg, player) -> {
            if (!perm.check(player, "votekick")) return;
            Player other = mindustry.Vars.playerGroup.find(p -> p.name.equalsIgnoreCase(arg[1]));
            PlayerData playerData = playerDB.get(player.uuid);
            if (other == null) other = players.get(Integer.parseInt(arg[1]));
            if (other == null) {
                player.sendMessage(new Bundle(playerData.locale).get("player-not-found"));
                return;
            }
            if (other.isAdmin) {
                player.sendMessage(new Bundle(playerData.locale).get("vote-target-admin"));
                return;
            }

            new Vote(player, arg[0], other);
        });
    }
}
