package essentials;

import arc.ApplicationListener;
import arc.Core;
import arc.files.Fi;
import arc.math.Mathf;
import arc.struct.Array;
import arc.util.CommandHandler;
import arc.util.Strings;
import arc.util.Time;
import essentials.core.player.Database;
import essentials.core.player.PlayerCore;
import essentials.core.player.PlayerDB;
import essentials.core.player.PlayerData;
import essentials.core.plugin.Config;
import essentials.core.plugin.PluginData;
import essentials.external.DriverLoader;
import essentials.external.StringUtils;
import essentials.feature.*;
import essentials.internal.*;
import essentials.internal.exception.PluginException;
import essentials.internal.thread.*;
import essentials.network.Client;
import essentials.network.Server;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Mechs;
import mindustry.core.Version;
import mindustry.entities.type.BaseUnit;
import mindustry.entities.type.Player;
import mindustry.entities.type.Unit;
import mindustry.game.Difficulty;
import mindustry.game.Gamemode;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.io.SaveIO;
import mindustry.maps.Map;
import mindustry.net.Packets;
import mindustry.plugin.Plugin;
import mindustry.type.Mech;
import mindustry.type.UnitType;
import mindustry.world.Block;
import org.hjson.JsonObject;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.Timer;
import java.util.concurrent.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static mindustry.Vars.*;
import static org.hjson.JsonValue.readJSON;

public class Main extends Plugin {
    public static final Fi root = Core.settings.getDataDirectory().child("mods/Essentials/");
    public static final Timer timer = new Timer(true);
    public static final ExecutorService mainThread = new ThreadPoolExecutor(0, 8, 10L, TimeUnit.SECONDS, new SynchronousQueue<>());

    public static final Array<Vote> vote = new Array<>();

    public static final Tools tool = new Tools();
    public static final PlayerDB playerDB = new PlayerDB();
    public static final Database database = new Database();
    public static final PluginData pluginData = new PluginData();
    public static final Server server = new Server();
    public static final Client client = new Client();
    public static final PlayerCore playerCore = new PlayerCore();
    public static final ColorNick colornick = new ColorNick();
    public static final Permission perm = new Permission();
    public static final Discord discord = new Discord();
    public static final AutoRollback rollback = new AutoRollback();
    public static final EventServer eventServer = new EventServer();
    public static final JumpBorder jumpBorder = new JumpBorder();
    public static final PluginVars vars = new PluginVars();
    public static final Config config = new Config();

    public final ApplicationListener listener;
    public final Array<EventServer.EventService> eventServers = new Array<>();

    final Logger log = LoggerFactory.getLogger(Main.class);

    public static Locale locale;

    public Main() {
        // 서버 버전 확인
        if (Version.build != vars.buildVersion() && Version.revision < vars.buildRevision()) {
            try (InputStream reader = getClass().getResourceAsStream("/plugin.json");
                 BufferedReader br = new BufferedReader(new InputStreamReader(reader))) {
                throw new PluginException("Essentials " + readJSON(br).asObject().get("version").asString() + " plugin only works with Build " + vars.buildVersion() + "." + vars.buildRevision() + " or higher.");
            } catch (PluginException | IOException e) {
                log.warn("Plugin", e);
                System.exit(0);
            }
        }


        // 파일 압축해제
        try (final JarFile jar = new JarFile(new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()))) {
            final Enumeration<JarEntry> enumEntries = jar.entries();
            while (enumEntries.hasMoreElements()) {
                JarEntry file = enumEntries.nextElement();
                String renamed = file.getName().replace("configs/", "");
                if (file.getName().startsWith("configs") && !root.child(renamed).exists()) {
                    if (file.isDirectory()) {
                        root.child(renamed).file().mkdir();
                        continue;
                    }
                    try (InputStream is = jar.getInputStream(file)) {
                        root.child(renamed).write(is, false);
                    }
                }
            }
        } catch (IOException | URISyntaxException e) {
            log.warn("File Extract", e);
            System.exit(0);
        }

        // 설정 불러오기
        config.init();
        Log.info("config.language", config.language().getDisplayLanguage()); // TODO 국가명 안뜨는 이유 찾기

        // 플러그인 데이터 불러오기
        pluginData.loadall();

        // 플레이어 권한 목록 불러오기
        perm.reload(true);

        // 스레드 시작
        new TickTrigger();
        mainThread.submit(new Threads());
        mainThread.submit(new ColorNick());
        timer.scheduleAtFixedRate(new AutoRollback(), 600000, 600000);
        mainThread.submit(new PermissionWatch());
        mainThread.submit(colornick);
        mainThread.submit(jumpBorder);

        // DB 드라이버 로딩
        new DriverLoader();

        // DB 연결
        try {
            database.connect();
            database.create();
            database.LegacyUpgrade();
            if (config.dbserver()) database.server_start();
        } catch (SQLException e) {
            new CrashReport(e);
        }

        // Client 연결
        if (config.clienten()) new Client();

        // Server 시작
        if (config.serverenable()) new Server();

        // 기록 시작
        if (config.logging()) new ActivityLog();

        // 이벤트 시작
        new Event();

        // 서버 종료 이벤트 설정
        this.listener = new ApplicationListener() {
            @Override
            public void dispose() {
                try {
                    boolean error = false;

                    discord.shutdownNow(); // Discord 서비스 종료
                    playerDB.saveAll(); // 플레이어 데이터 저장
                    pluginData.saveAll(); // 플러그인 데이터 저장
                    mainThread.shutdownNow(); // 스레드 종료
                    // config.singleService.shutdownNow(); // 로그 스레드 종료
                    timer.cancel(); // 일정 시간마다 실행되는 스레드 종료
                    if (vote.size != 0) vote.get(0).interrupt(); // 투표 종료
                    database.dispose(); // DB 연결 종료

                    if (config.serverenable()) {
                        try {
                            Iterator<Server.service> servers = server.list.iterator();
                            while (servers.hasNext()) {
                                Server.service ser = servers.next();
                                ser.os.close();
                                ser.in.close();
                                ser.socket.close();
                                servers.remove();
                            }
                            server.stop();
                            Log.info("server-thread-disabled");
                        } catch (Exception e) {
                            error = true;
                            Log.err("server-thread-disable-error");
                            new CrashReport(e);
                        }
                    }

                    // 클라이언트 종료
                    if (config.clienten() && client.activated) {
                        client.request(Client.Request.exit, null, null);
                        Log.info("client.shutdown");
                    }

                    // 모든 이벤트 서버 종료
                    for (Process value : eventServer.servers) value.destroy();
                    if (!error) {
                        Log.info("thread-disabled");
                    } else {
                        Log.warn("thread-not-dead");
                    }
                } catch (Exception e) {
                    new CrashReport(e);
                    System.exit(1); // 오류로 인한 강제 종료
                }
            }
        };
        Core.app.addListener(listener);
    }

    @Override
    public void init() {
        // 채팅 포맷 변경
        netServer.admins.addChatFilter((player, text) -> null);

        // 비 로그인 유저 통제
        netServer.admins.addActionFilter(action -> {
            if (action.player == null) return true;

            PlayerData playerData = playerDB.get(action.player.uuid);
            return playerData.login();
        });
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.register("saveall", "desc", (arg) -> {
            try {
                pluginData.saveAll();
            } catch (Exception e) {
                log.warn("PluginData save", e);
            }
        });
        handler.register("gendocs", "Generate Essentials README.md", (arg) -> {
            String[] servercommands = new String[]{
                    "help", "version", "exit", "stop", "host", "maps", "reloadmaps", "status",
                    "mods", "mod", "js", "say", "difficulty", "rules", "fillitems", "playerlimit",
                    "config", "subnet-ban", "whitelisted", "whitelist-add", "whitelist-remove",
                    "shuffle", "nextmap", "kick", "ban", "bans", "unban", "admin", "unadmin",
                    "admins", "runwave", "load", "save", "saves", "gameover", "info", "search", "gc"
            };
            String[] clientcommands = new String[]{
                    "help", "t", "sync", "pardon", "players"
            };
            String serverdoc = "## Server commands\n\n| Command | Parameter | Description |\n|:---|:---|:--- |\n";
            String clientdoc = "## Client commands\n\n| Command | Parameter | Description |\n|:---|:---|:--- |\n";
            String gentime = "\nREADME.md Generated time: " + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());

            Log.info("readme-generating");

            String header = "# Essentials\n" +
                    "Add more commands to the server.\n\n" +
                    "I'm getting a lot of suggestions.<br>\n" +
                    "Please submit your idea to this repository issues or Mindustry official discord!\n\n" +
                    "## Requirements for running this plugin\n" +
                    "Minimum require java version: Only __Java 8__<br>\n" +
                    "This plugin does a lot of disk read/write operations depending on the features usage.\n\n" +
                    "### Minimum\n" +
                    "CPU: Athlon 200GE or Intel i5 2300<br>\n" +
                    "RAM: 20MB<br>\n" +
                    "Disk: HDD capable of more than 2MB/s random read/write.\n\n" +
                    "### Recommend\n" +
                    "CPU: Ryzen 3 2200G or Intel i3 8100<br>\n" +
                    "RAM: 50MB<br>\n" +
                    "Disk: HDD capable of more than 5MB/s random read/write.\n\n" +
                    "## Installation\n\n" +
                    "Put this plugin in the ``<server folder location>/config/mods`` folder.\n\n";

            StringBuilder tempbuild = new StringBuilder();
            for (int a = 0; a < netServer.clientCommands.getCommandList().size; a++) {
                CommandHandler.Command command = netServer.clientCommands.getCommandList().get(a);
                boolean dup = false;
                for (String as : clientcommands) {
                    if (command.text.equals(as)) {
                        dup = true;
                        break;
                    }
                }
                if (!dup) {
                    String temp = "| " + command.text + " | " + StringUtils.encodeHtml(command.paramText) + " | " + command.description + " |\n";
                    tempbuild.append(temp);
                }
            }

            String tmp = header + clientdoc + tempbuild.toString() + "\n";
            tempbuild = new StringBuilder();

            for (CommandHandler.Command command : handler.getCommandList()) {
                boolean dup = false;
                for (String as : servercommands) {
                    if (command.text.equals(as)) {
                        dup = true;
                        break;
                    }
                }
                if (!dup) {
                    String temp = "| " + command.text + " | " + StringUtils.encodeHtml(command.paramText) + " | " + command.description + " |\n";
                    tempbuild.append(temp);
                }
            }

            root.child("README.md").writeString(tmp + serverdoc + tempbuild.toString() + gentime);

            Log.info("success");
        });
        handler.register("admin", "<name>", "Set admin status to player.", (arg) -> {
            if (arg.length != 0) {
                Player player = playerGroup.find(p -> p.name.equals(arg[0]));

                if (player == null) {
                    Log.warn("player.not-found");
                } else {
                    for (JsonObject.Member data : perm.permission) {
                        if (data.getName().equals(arg[0])) {
                            PlayerData p = playerDB.get(player.uuid);
                            p.permission("new_admin");
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
        handler.register("info", "<player/uuid>", "Show player information", (arg) -> {
            Player player = playerGroup.find(p -> p.name.equalsIgnoreCase(arg[0]));
            String uuid = arg[0];
            if (player != null) uuid = player.uuid;

            try {
                PreparedStatement pstmt = database.conn.prepareStatement("SELECT * from players WHERE uuid=?");
                pstmt.setString(1, uuid);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String datatext = "\n" + rs.getString("name") + " Player information\n" +
                            "=====================================" + "\n" +
                            "name: " + rs.getString("name") + "\n" +
                            "uuid: " + rs.getString("uuid") + "\n" +
                            "country: " + rs.getString("country") + "\n" +
                            "country_code: " + rs.getString("country_code") + "\n" +
                            "language: " + rs.getString("language") + "\n" +
                            "isAdmin: " + rs.getBoolean("isAdmin") + "\n" +
                            "placecount: " + rs.getInt("placecount") + "\n" +
                            "breakcount: " + rs.getInt("breakcount") + "\n" +
                            "killcount: " + rs.getInt("killcount") + "\n" +
                            "deathcount: " + rs.getInt("deathcount") + "\n" +
                            "joincount: " + rs.getInt("joincount") + "\n" +
                            "kickcount: " + rs.getInt("kickcount") + "\n" +
                            "level: " + rs.getInt("level") + "\n" +
                            "exp: " + rs.getInt("exp") + "\n" +
                            "reqexp: " + rs.getInt("reqexp") + "\n" +
                            "reqtotalexp: " + rs.getString("reqtotalexp") + "\n" +
                            "firstdate: " + rs.getString("firstdate") + "\n" +
                            "lastdate: " + rs.getString("lastdate") + "\n" +
                            "lastplacename: " + rs.getString("lastplacename") + "\n" +
                            "lastbreakname: " + rs.getString("lastbreakname") + "\n" +
                            "lastchat: " + rs.getString("lastchat") + "\n" +
                            "playtime: " + rs.getString("playtime") + "\n" +
                            "attackclear: " + rs.getInt("attackclear") + "\n" +
                            "pvpwincount: " + rs.getInt("pvpwincount") + "\n" +
                            "pvplosecount: " + rs.getInt("pvplosecount") + "\n" +
                            "pvpbreakout: " + rs.getInt("pvpbreakout") + "\n" +
                            "reactorcount: " + rs.getInt("reactorcount") + "\n" +
                            "bantimeset: " + rs.getString("bantimeset") + "\n" +
                            "bantime: " + rs.getString("bantime") + "\n" +
                            "banned: " + rs.getBoolean("banned") + "\n" +
                            "translate: " + rs.getBoolean("translate") + "\n" +
                            "crosschat: " + rs.getBoolean("crosschat") + "\n" +
                            "colornick: " + rs.getBoolean("colornick") + "\n" +
                            "connected: " + rs.getBoolean("connected") + "\n" +
                            "connserver: " + rs.getString("connserver") + "\n" +
                            "permission: " + rs.getString("permission") + "\n" +
                            "mute: " + rs.getBoolean("mute") + "\n" +
                            "alert: " + rs.getBoolean("alert") + "\n" +
                            "udid: " + rs.getLong("udid") + "\n" +
                            "accountid: " + rs.getString("accountid");
                    PlayerData current = playerDB.get(uuid);
                    if (!current.error()) {
                        datatext = datatext + "\n\n== " + current.name() + " Player internal data ==\n" +
                                "isLogin: " + current.login() + "\n" +
                                "afk: " + current.afk().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "\n" +
                                "afk_tilex: " + current.tilex() + "\n" +
                                "afk_tiley: " + current.tiley();

                    }
                    Log.info(datatext);
                } else {
                    Log.info("Player not found!");
                }
            } catch (SQLException e) {
                new CrashReport(e);
            }
        });
        // TODO 모든 권한 그룹 변경 만들기
        handler.register("setperm", "<player_name/uuid> <group>", "Set player permission", (arg) -> {
            Player target = playerGroup.find(p -> p.name.equals(arg[0]));
            Bundle bundle = new Bundle();
            PlayerData playerData;
            if (target == null) {
                Log.warn(bundle.get("player.not-found"));
                return;
            }

            for (JsonObject.Member p : perm.permission) {
                if (p.getName().equals(arg[1])) {
                    playerData = playerDB.get(target.uuid);
                    playerData.permission(arg[1]);

                    target.isAdmin = perm.isAdmin(target);

                    Log.info(bundle.get("success"));
                    target.sendMessage(new Bundle(playerDB.get(target.uuid).locale()).prefix("perm-changed"));
                    return;
                }
            }
            Log.warn(bundle.get("perm-group-not-found"));
        });
        handler.register("reload", "Reload Essential plugin data", (arg) -> {

        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("test", "Test geo", (arg, player) -> {
            tool.getGeo(player);
        });
        handler.<Player>register("alert", "Turn on/off alerts", (arg, player) -> {
            if (!perm.check(player, "alert")) return;

            PlayerData playerData = playerDB.get(player.uuid);
            if (playerData.alert()) {
                playerData.alert(false);
                player.sendMessage(new Bundle(playerData.locale()).prefix("anti-grief.alert.disable"));
            } else {
                playerData.alert(true);
                player.sendMessage(new Bundle(playerData.locale()).prefix("anti-grief.alert.enable"));
            }

        });
        handler.<Player>register("ch", "Send chat to another server.", (arg, player) -> {
            if (!perm.check(player, "ch")) return;

            PlayerData playerData = playerDB.get(player.uuid);
            if (playerData.crosschat()) {
                playerData.crosschat(false);
                player.sendMessage(new Bundle(playerData.locale()).prefix("player.crosschat.disable"));
            } else {
                playerData.crosschat(true);
                player.sendMessage(new Bundle(playerData.locale()).prefix("player.crosschat"));
            }
        });
        handler.<Player>register("changepw", "<new_password> <new_password_repeat>", "Change account password", (arg, player) -> {
            if (!perm.check(player, "changepw")) return;

            PlayerData playerData = playerDB.get(player.uuid);
            Bundle bundle = new Bundle(playerData.locale());
            if (!tool.checkPassword(player, playerData.accountid(), arg[1], arg[2])) {
                player.sendMessage(bundle.prefix("system.account.need-new-password"));
                return;
            }
            try {
                Class.forName("org.mindrot.jbcrypt.BCrypt");
                playerData.accountpw(BCrypt.hashpw(arg[0], BCrypt.gensalt(11)));
                player.sendMessage(bundle.prefix("success"));
            } catch (ClassNotFoundException e) {
                new CrashReport(e);
            }
        });
        handler.<Player>register("chars", "<Text...>", "Make pixel texts", (arg, player) -> {
            if (!perm.check(player, "chars")) return;
            tool.setTileText(world.tile(player.tileX(), player.tileY()), Blocks.copperWall, arg[0]);
        });
        handler.<Player>register("color", "Enable color nickname", (arg, player) -> {
            if (!perm.check(player, "color")) return;
            PlayerData playerData = playerDB.get(player.uuid);
            playerData.colornick(!playerData.colornick());
            player.sendMessage(new Bundle(playerData.locale()).prefix(playerData.colornick() ? "feature.colornick.disable" : "feature.colornick.enable"));
        });
        handler.<Player>register("difficulty", "<difficulty>", "Set server difficulty", (arg, player) -> {
            if (!perm.check(player, "difficulty")) return;
            PlayerData playerData = playerDB.get(player.uuid);
            try {
                Difficulty.valueOf(arg[0]);
                player.sendMessage(new Bundle(playerData.locale()).prefix("system.difficulty.set", arg[0]));
            } catch (IllegalArgumentException e) {
                player.sendMessage(new Bundle(playerData.locale()).prefix("system.difficulty.not-found", arg[0]));
            }
        });
        handler.<Player>register("killall", "Kill all enemy units", (arg, player) -> {
            if (!perm.check(player, "killall")) return;
            for (int a = 0; a < Team.all().length; a++) unitGroup.all().each(Unit::kill);
            player.sendMessage(new Bundle(playerDB.get(player.uuid).locale()).prefix("success"));
        });
        handler.<Player>register("event", "<host/join> <roomname> [map] [gamemode]", "Host your own server", (arg, player) -> {
            if (!perm.check(player, "event")) return;
            PlayerData playerData = playerDB.get(player.uuid);
            switch (arg[0]) {
                case "host":
                    if (playerData.level() > 20 || player.isAdmin) {
                        if (arg.length == 2) {
                            player.sendMessage(new Bundle(playerData.locale()).prefix("system.event.host.no-mapname"));
                            return;
                        }
                        if (arg.length == 3) {
                            player.sendMessage(new Bundle(playerData.locale()).prefix("system.event.host.no-gamemode"));
                            return;
                        }
                        player.sendMessage(new Bundle(playerData.locale()).prefix("system.event.making"));

                        String[] range = config.eventport().split("-");
                        int firstport = Integer.parseInt(range[0]);
                        int lastport = Integer.parseInt(range[1]);
                        int customport = ThreadLocalRandom.current().nextInt(firstport, lastport + 1);

                        pluginData.eventservers.add(new PluginData.eventservers(arg[1], customport));

                        // TODO 이벤트 서버 생성 성공/실패 여부 수정
                        boolean result = eventServer.create(arg[1], arg[2], arg[3], customport);
                        if (result) {
                            Log.info("event.host.opened", player.name, customport);
                            playerData.connected(false);
                            playerData.connserver("none");
                            Call.onConnect(player.con, vars.serverIP(), customport);
                            Log.info("Player " + playerData.name() + " joined to " + customport + " port");
                        }
                    } else {
                        player.sendMessage(new Bundle(playerData.locale()).prefix("system.event.level"));
                    }
                    break;
                case "join":
                    for (EventServer.EventService server : eventServers) {
                        if (server.roomname.equals(arg[1])) {
                            PlayerData val = playerDB.get(player.uuid);
                            val.connected(false);
                            val.connserver("none");
                            Call.onConnect(player.con, vars.serverIP(), server.port);
                            Log.info(vars.serverIP() + ":" + server.port);
                            break;
                        }
                    }
                    break;
                default:
                    player.sendMessage(new Bundle(playerData.locale()).prefix("system.wrong-command"));
                    break;
            }
        });
        handler.<Player>register("help", "[page]", "Show command lists", (arg, player) -> {
            if (arg.length > 0 && !Strings.canParseInt(arg[0])) {
                player.sendMessage(new Bundle(playerDB.get(player.uuid).locale()).prefix("page-number"));
                return;
            }

            Array<String> temp = new Array<>();
            for (int a = 0; a < netServer.clientCommands.getCommandList().size; a++) {
                CommandHandler.Command command = netServer.clientCommands.getCommandList().get(a);
                if (perm.check(player, command.text) || command.text.equals("t") || command.text.equals("sync")) {
                    temp.add("[orange] /" + command.text + " [white]" + command.paramText + " [lightgray]- " + command.description + "\n");
                }
            }

            StringBuilder result = new StringBuilder();
            int perpage = 8;
            int page = arg.length > 0 ? Strings.parseInt(arg[0]) : 1;
            int pages = Mathf.ceil((float) temp.size / perpage);

            page--;

            if (page > pages || page < 0) {
                player.sendMessage("[scarlet]'page' must be a number between[orange] 1[] and[orange] " + pages + "[scarlet].");
                return;
            }

            result.append(Strings.format("[orange]-- Commands Page[lightgray] {0}[gray]/[lightgray]{1}[orange] --\n", (page + 1), pages));
            for (int a = perpage * page; a < Math.min(perpage * (page + 1), temp.size); a++) {
                result.append(temp.get(a));
            }
            player.sendMessage(result.toString().substring(0, result.length() - 1));
        });
        handler.<Player>register("info", "Show your information", (arg, player) -> {
            if (!perm.check(player, "info")) return;
            PlayerData playerData = playerDB.get(player.uuid);
            Bundle bundle = new Bundle(playerData.locale());
            String datatext = "[#DEA82A]" + new Bundle(playerData.locale()).get("player.info") + "[]\n" +
                    "[#2B60DE]====================================[]\n" +
                    "[green]" + bundle.get("player.name") + "[] : " + player.name + "[white]\n" +
                    "[green]" + bundle.get("player.country") + "[] : " + playerData.locale().getDisplayCountry(playerData.locale()) + "\n" +
                    "[green]" + bundle.get("player.placecount") + "[] : " + playerData.placecount() + "\n" +
                    "[green]" + bundle.get("player.breakcount") + "[] : " + playerData.breakcount() + "\n" +
                    "[green]" + bundle.get("player.killcount") + "[] : " + playerData.killcount() + "\n" +
                    "[green]" + bundle.get("player.deathcount") + "[] : " + playerData.deathcount() + "\n" +
                    "[green]" + bundle.get("player.joincount") + "[] : " + playerData.joincount() + "\n" +
                    "[green]" + bundle.get("player.kickcount") + "[] : " + playerData.kickcount() + "\n" +
                    "[green]" + bundle.get("player.level") + "[] : " + playerData.level() + "\n" +
                    "[green]" + bundle.get("player.reqtotalexp") + "[] : " + playerData.reqtotalexp() + "\n" +
                    "[green]" + bundle.get("player.firstdate") + "[] : " + playerData.firstdate() + "\n" +
                    "[green]" + bundle.get("player.lastdate") + "[] : " + playerData.lastdate() + "\n" +
                    "[green]" + bundle.get("player.playtime") + "[] : " + playerData.playtime() + "\n" +
                    "[green]" + bundle.get("player.attackclear") + "[] : " + playerData.attackclear() + "\n" +
                    "[green]" + bundle.get("player.pvpwincount") + "[] : " + playerData.pvpwincount() + "\n" +
                    "[green]" + bundle.get("player.pvplosecount") + "[] : " + playerData.pvplosecount() + "\n" +
                    "[green]" + bundle.get("player.pvpbreakout") + "[] : " + playerData.pvpbreakout();
            Call.onInfoMessage(player.con, datatext);
        });
        handler.<Player>register("jump", "<zone/count/total> [ip] [port] [range] [clickable]", "Create a server-to-server jumping zone.", (arg, player) -> {
            if (!perm.check(player, "jump")) return;
            PlayerData playerData = playerDB.get(player.uuid);
            Bundle bundle = new Bundle(playerData.locale());

            String type = arg[0];
            // boolean touchable = Boolean.parseBoolean(arg[1]);
            // String ip = arg[2];
            // int port = Integer.parseInt(arg[3]);
            // int range = Integer.parseInt(arg[4]);

            switch (type) {
                case "zone":
                    if (arg.length != 5) {
                        player.sendMessage(bundle.prefix("system.server-to-server.incorrect"));
                        return;
                    }

                    int size;
                    boolean touchable;
                    String ip;
                    int port;

                    try {
                        size = Integer.parseInt(arg[3]);
                        touchable = Boolean.parseBoolean(arg[4]);
                        ip = arg[1];
                        port = Integer.parseInt(arg[2]);
                    } catch (NumberFormatException ignored) {
                        player.sendMessage(bundle.prefix("system.server-to-server.not-int"));
                        return;
                    }

                    int tf = player.tileX() + size;
                    int ty = player.tileY() + size;

                    pluginData.jumpzone.add(new PluginData.jumpzone(world.tile(player.tileX(), player.tileY()), world.tile(tf, ty), touchable, ip, port));
                    player.sendMessage(bundle.prefix("system.server-to-server.added"));
                    break;
                case "count":
                    try {
                        ip = arg[1];
                        port = Integer.parseInt(arg[2]);
                    } catch (NumberFormatException ignored) {
                        player.sendMessage(bundle.prefix("system.server-to-server.port-not-int"));
                        return;
                    }

                    pluginData.jumpcount.add(new PluginData.jumpcount(world.tile(player.tileX(), player.tileY()), ip, port, 0, 0));
                    player.sendMessage(bundle.prefix("system.server-to-server.added"));
                    break;
                case "total":
                    pluginData.jumptotal.add(new PluginData.jumptotal(world.tile(player.tileX(), player.tileY()), 0, 0));
                    player.sendMessage(bundle.prefix("system.server-to-server.added"));
                    break;
                default:
                    player.sendMessage(bundle.prefix("command.invalid"));
            }
        });
        handler.<Player>register("kickall", "Kick all players", (arg, player) -> {
            if (!perm.check(player, "kickall")) return;
            for (Player p : playerGroup.all()) {
                if (player != p) Call.onKick(p.con, Packets.KickReason.kick);
            }
        });
        handler.<Player>register("kill", "<player>", "Kill player.", (arg, player) -> {
            if (!perm.check(player, "kill")) return;
            Player other = playerGroup.find(p -> p.name.equalsIgnoreCase(arg[0]));
            if (other == null) {
                player.sendMessage(new Bundle(playerDB.get(player.uuid).locale()).prefix("player.not-found"));
                return;
            }
            Player.onPlayerDeath(other);
        });
        handler.<Player>register("login", "<id> <password>", "Access your account", (arg, player) -> {
            PlayerData playerData = playerDB.get(player.uuid);
            if (config.loginenable()) {
                if (playerData.error()) {
                    if (playerCore.login(player, arg[0], arg[1])) {
                        if (config.passwordmethod().equals("discord")) {
                            playerCore.load(player, arg[0]);
                        } else {
                            playerCore.load(player);
                        }
                        player.sendMessage(new Bundle(playerData.locale()).prefix("system.login.success"));
                    } else {
                        player.sendMessage("[green][EssentialPlayer] [scarlet]Login failed/로그인 실패!!");
                    }
                } else {
                    if (config.passwordmethod().equals("mixed")) {
                        if (playerCore.login(player, arg[0], arg[1])) Call.onConnect(player.con, vars.serverIP(), 7060);
                    } else {
                        player.sendMessage("[green][EssentialPlayer] [scarlet]You're already logged./이미 로그인한 상태입니다.");
                    }
                }
            } else {
                player.sendMessage(new Bundle(playerData.locale()).prefix("system.login.disabled"));
            }
        });
        handler.<Player>register("logout", "Log-out of your account.", (arg, player) -> {
            if (!perm.check(player, "logout")) return;

            PlayerData playerData = playerDB.get(player.uuid);
            Bundle bundle = new Bundle(playerData.locale());
            if (config.loginenable()) {
                playerData.connected(false);
                playerData.connserver("none");
                playerData.uuid("Logout");
                Call.onKick(player.con, bundle.get("system.logout"));
            } else {
                player.sendMessage(bundle.prefix("system.login.disabled"));
            }
        });
        handler.<Player>register("maps", "[page]", "Show server maps", (arg, player) -> {
            if (!perm.check(player, "maps")) return;
            Array<Map> maplist = maps.all();
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
            String motd = tool.getMotd(playerDB.get(player.uuid).locale());
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
            int pages = Mathf.ceil((float) playerGroup.size() / 6);

            page--;
            if (page > pages || page < 0) {
                player.sendMessage("[scarlet]'page' must be a number between[orange] 1[] and[orange] " + pages + "[scarlet].");
                return;
            }

            build.append("[green]==[white] Players list page ").append(page).append("/").append(pages).append(" [green]==[white]\n");
            for (int a = 6 * page; a < Math.min(6 * (page + 1), playerGroup.size()); a++) {
                build.append("[gray]").append(a).append("[] ").append(playerGroup.all().get(a).name).append("\n");
            }
            player.sendMessage(build.toString());
        });
        handler.<Player>register("save", "Auto rollback map early save", (arg, player) -> {
            if (!perm.check(player, "save")) return;
            Fi file = saveDirectory.child(config.slownumber() + "." + saveExtension);
            SaveIO.save(file);
            player.sendMessage(new Bundle(playerDB.get(player.uuid).locale()).prefix("system.map-saved"));
        });
        handler.<Player>register("reset", "<zone/count/total> [ip]", "Remove a server-to-server jumping zone data.", (arg, player) -> {
            if (!perm.check(player, "reset")) return;
            PlayerData playerData = playerDB.get(player.uuid);
            Bundle bundle = new Bundle(playerData.locale());
            switch (arg[0]) {
                case "zone":
                    for (int a = 0; a < pluginData.jumpzone.size; a++) {
                        if (arg.length != 2) {
                            player.sendMessage(bundle.prefix("no-parameter"));
                            return;
                        }
                        if (arg[1].equals(pluginData.jumpzone.get(a).ip)) {
                            pluginData.jumpzone.remove(a);
                            for (Thread value : jumpBorder.thread) {
                                value.interrupt();
                            }
                            jumpBorder.thread.clear();
                            jumpBorder.main();
                            player.sendMessage(bundle.prefix("success"));
                            break;
                        }
                    }
                    break;
                case "count":
                    pluginData.jumpcount.clear();
                    player.sendMessage(bundle.prefix("system.server-to-server.reset", "count"));
                    break;
                case "total":
                    pluginData.jumptotal.clear();
                    player.sendMessage(bundle.prefix("system.server-to-server.reset", "total"));
                    break;
                default:
                    player.sendMessage(bundle.prefix("command.invalid"));
                    break;
            }
        });
        handler.<Player>register("router", "Router", (arg, player) -> {
            if (!perm.check(player, "router")) return;
            Vars.playerGroup.getByID(player.id).name =
                    "[#6B6B6B][#585858][#6B6B6B]\n" +
                            "[#6B6B6B][#828282][#6B6B6B]\n" +
                            "[#585858][#6B6B6B][#828282][#585858]\n" +
                            "[#585858][#6B6B6B][#828282][#6B6B6B][#828282][#585858]\n" +
                            "[#585858][#6B6B6B][#828282][#6B6B6B][#828282][#585858]\n" +
                            "[#585858][#6B6B6B][#828282][#585858]\n" +
                            "[#6B6B6B][#828282][#6B6B6B]\n" +
                            "[#6B6B6B][#585858][#6B6B6B]";
        });
        handler.<Player>register("register", config.passwordmethod().equals("password") ? "<accountid> <password>" : config.passwordmethod().equals("discord") ? "[PIN]" : "", "Register account", (arg, player) -> {
            if (config.loginenable()) {
                switch (config.passwordmethod()) {
                    case "discord":
                        player.sendMessage("Join discord and use !register command!\n" + config.discordlink());
                        discord.queue(player);
                        break;
                    default:
                    case "password":
                        Locale lc = tool.getGeo(player);
                        boolean register = playerDB.register(player.name, player.uuid, lc.getDisplayCountry(), lc.toString(), lc.getDisplayLanguage(), true, vars.serverIP(), "default", 0L, arg[0], arg[1]);
                        if (register) {
                            PlayerData playerData = playerDB.load(player.uuid, arg[0]);
                            player.sendMessage(new Bundle(playerData.locale()).prefix("register-success"));
                        } else {
                            player.sendMessage("[green][Essentials] [scarlet]Register failed/계정 등록 실패!");
                        }
                        break;
                }
            } else {
                player.sendMessage(new Bundle(playerDB.get(player.uuid) == null ? playerDB.get(player.uuid).locale() : locale).prefix("system.login.disabled"));
            }
        });
        handler.<Player>register("spawn", "<mob_name> <count> [team] [playerName]", "Spawn mob in player position", (arg, player) -> {
            if (!perm.check(player, "spawn")) return;
            PlayerData playerData = playerDB.get(player.uuid);
            Bundle bundle = new Bundle(playerData.locale());

            UnitType targetUnit = tool.getUnitByName(arg[0]);
            if (targetUnit == null) {
                player.sendMessage(bundle.prefix("system.mob.not-found"));
                return;
            }
            int count;
            try {
                count = Integer.parseInt(arg[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(bundle.prefix("syttem.mob.not-number"));
                return;
            }
            if (config.spawnlimit() == count) {
                player.sendMessage(bundle.prefix("spawn-limit"));
                return;
            }
            Player targetPlayer = arg.length > 3 ? tool.findPlayer(arg[3]) : player;
            if (targetPlayer == null) {
                player.sendMessage(bundle.prefix("player.not-found"));
                targetPlayer = player;
            }
            Team targetTeam = arg.length > 2 ? tool.getTeamByName(arg[2]) : targetPlayer.getTeam();
            if (targetTeam == null) {
                player.sendMessage(bundle.prefix("team-not-found"));
                targetTeam = targetPlayer.getTeam();
            }
            for (int i = 0; count > i; i++) {
                BaseUnit baseUnit = targetUnit.create(targetTeam);
                baseUnit.set(targetPlayer.getX(), targetPlayer.getY());
                baseUnit.add();
            }
        });

        handler.<Player>register("setperm", "<player_name> <group>", "Set player permission", (arg, player) -> {
            if (!perm.check(player, "setperm")) return;
            PlayerData playerData = playerDB.get(player.uuid);
            Bundle bundle = new Bundle(playerData.locale());

            Player target = playerGroup.find(p -> p.name.equals(arg[0]));
            if (target == null) {
                player.sendMessage(bundle.prefix("player.not-found"));
                return;
            }
            for (JsonObject.Member perm : perm.permission) {
                if (perm.getName().equals(arg[0])) {
                    PlayerData val = playerDB.get(target.uuid);
                    val.permission(arg[1]);
                    player.sendMessage(bundle.prefix("success"));
                    target.sendMessage(new Bundle(playerData.locale()).prefix("perm-changed"));
                    return;
                }
            }
            player.sendMessage(new Bundle(playerData.locale()).prefix("perm-group-not-found"));
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
            Bundle bundle = new Bundle(playerData.locale());
            Mech mech = Mechs.starter;
            switch (arg[0]) {
                case "alpha":
                    mech = Mechs.alpha;
                    break;
                case "dart":
                    mech = Mechs.dart;
                    break;
                case "glaive":
                    mech = Mechs.glaive;
                    break;
                case "delta":
                    mech = Mechs.delta;
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
                    player.sendMessage(bundle.prefix("player.not-found"));
                    return;
                }
                target.mech = mech;
            }
            player.sendMessage(bundle.prefix("success"));
        });
        handler.<Player>register("status", "Show server status", (arg, player) -> {
            if (!perm.check(player, "status")) return;
            PlayerData playerData = playerDB.get(player.uuid);
            Bundle bundle = new Bundle(playerData.locale());
            player.sendMessage(bundle.prefix("server.status"));
            player.sendMessage("[#2B60DE]========================================[]");
            float fps = Math.round((int) 60f / Time.delta());
            int bans = netServer.admins.getBanned().size;
            int ipbans = netServer.admins.getBannedIPs().size;
            int bancount = bans + ipbans;
            player.sendMessage(bundle.prefix("server.status.banstat", fps, playerGroup.size(), bancount, bans, ipbans, vars.playtime(), vars.uptime(), vars.pluginVersion()));
        });
        handler.<Player>register("suicide", "Kill yourself.", (arg, player) -> {
            if (!perm.check(player, "suicide")) return;
            Player.onPlayerDeath(player);
            if (playerGroup != null && playerGroup.size() > 0) {
                tool.sendMessageAll("suicide", player.name);
            }
        });
        handler.<Player>register("team", "[Team...]", "Change team (PvP only)", (arg, player) -> {
            if (!perm.check(player, "team")) return;
            PlayerData playerData = playerDB.get(player.uuid);
            if (state.rules.pvp) {
                int i = player.getTeam().id + 1;
                while (i != player.getTeam().id) {
                    if (i >= Team.all().length) i = 0;
                    if (!state.teams.get(Team.all()[i]).cores.isEmpty()) {
                        player.setTeam(Team.all()[i]);
                        break;
                    }
                    i++;
                }
                Call.onPlayerDeath(player);
            } else {
                player.sendMessage(new Bundle(playerData.locale()).prefix("command.only-pvp"));
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
                LocalTime bantime = LocalTime.parse(arg[1], DateTimeFormatter.ofPattern("HH:mm:ss"));
                playerCore.tempban(other, bantime, arg[2]);
                other.con.kick("Temp kicked");
                for (int a = 0; a < playerGroup.size(); a++) {
                    Player current = playerGroup.all().get(a);
                    PlayerData target = playerDB.get(current.uuid);
                    current.sendMessage(new Bundle(target.locale()).prefix("account.ban.temp", other.name, player.name));
                }
            } else {
                player.sendMessage(new Bundle(playerData.locale()).prefix("player.not-found"));
            }
        });
        handler.<Player>register("time", "Show server time", (arg, player) -> {
            if (!perm.check(player, "time")) return;
            PlayerData playerData = playerDB.get(player.uuid);
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss");
            String nowString = now.format(dateTimeFormatter);
            player.sendMessage(new Bundle(playerData.locale()).prefix("servertime", nowString));
        });
        handler.<Player>register("tp", "<player>", "Teleport to other players", (arg, player) -> {
            if (!perm.check(player, "tp")) return;
            PlayerData playerData = playerDB.get(player.uuid);
            Bundle bundle = new Bundle(playerData.locale());
            if (player.isMobile) {
                player.sendMessage(bundle.prefix("tp-not-support"));
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
                player.sendMessage(bundle.prefix("player.not-found"));
                return;
            }
            player.set(other.getX(), other.getY());
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
                player.sendMessage(new Bundle(playerData.locale()).prefix("player.not-found"));
                return;
            }
            if (!other1.isMobile || !other2.isMobile) {
                other1.setNet(other2.x, other2.y);
            } else {
                player.sendMessage(new Bundle(playerData.locale()).prefix("tp-ismobile"));
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
                player.sendMessage(new Bundle(playerData.locale()).prefix("tp-not-int"));
                return;
            }
            player.setNet(x, y);
        });
        handler.<Player>register("tr", "Enable/disable Translate all chat", (arg, player) -> {
            if (!perm.check(player, "tr")) return;
            PlayerData playerData = playerDB.get(player.uuid);
            playerDB.get(player.uuid).translate(!playerData.translate());
            player.sendMessage(new Bundle(playerData.locale()).prefix(playerData.translate() ? "translate" : "translate-disable", player.name));

        });
        // TODO 투표기능 다시 만들기
        if (config.vote()) {
            handler.<Player>register("vote", "<mode> [parameter...]", "Voting system (Use /vote to check detail commands)", (arg, player) -> {
                if (!perm.check(player, "vote")) return;
                PlayerData playerData = playerDB.get(player.uuid);
                Bundle bundle = new Bundle(playerData.locale());

                if (vote.size != 0) {
                    player.sendMessage(bundle.prefix("vote.in-processing"));
                    return;
                }

                switch (arg[0]) {
                    case "kick":
                        Player target = playerGroup.find(p -> p.name.equalsIgnoreCase(arg[1]));
                        if (target == null) target = vars.players().get(Integer.parseInt(arg[1]));
                        if (target == null) {
                            player.sendMessage(bundle.prefix("player.not-found"));
                            return;
                        }

                        if (target.isAdmin) {
                            player.sendMessage(bundle.prefix("vote.target-admin"));
                            return;
                        }

                        // 강퇴 투표
                        vote.add(new Vote(player, Vote.VoteType.kick, target, arg[1]));
                        break;
                    case "map":
                        // 맵 투표
                        Map world = maps.all().find(map -> map.name().equalsIgnoreCase(arg[1].replace('_', ' ')) || map.name().equalsIgnoreCase(arg[1]));
                        if (world == null) world = Vars.maps.all().get(Integer.parseInt(arg[1]));
                        if (world == null) {
                            player.sendMessage(bundle.prefix("vote.map.not-found"));
                        } else {
                            vote.add(new Vote(player, Vote.VoteType.map, world));
                        }
                        break;
                    case "gameover":
                        vote.add(new Vote(player, Vote.VoteType.gameover));
                        break;
                    case "rollback":
                        vote.add(new Vote(player, Vote.VoteType.rollback));
                        break;
                    case "gamemode":
                        try {
                            vote.add(new Vote(player, Vote.VoteType.gamemode, Gamemode.valueOf(arg[1])));
                        } catch (IllegalArgumentException e) {
                            player.sendMessage(bundle.prefix("vote.wrong-gamemode"));
                        }
                        break;
                    default:
                        switch (arg[0]) {
                            case "gamemode":
                                player.sendMessage(bundle.prefix("vote.list.gamemode"));
                                break;
                            case "map":
                                player.sendMessage(bundle.prefix("vote.map.not-found"));
                                break;
                            case "kick":
                                player.sendMessage(bundle.prefix("vote.kick.parameter"));
                                break;
                            default:
                                player.sendMessage(bundle.prefix("vote.list"));
                                break;
                        }
                        break;
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
            player.sendMessage(new Bundle(playerDB.get(player.uuid).locale()).prefix("success"));
        });
        handler.<Player>register("mute", "<Player_name>", "Mute/unmute player", (arg, player) -> {
            if (!perm.check(player, "mute")) return;
            Player other = playerGroup.find(p -> p.name.equalsIgnoreCase(arg[0]));
            PlayerData playerData = playerDB.get(player.uuid);
            if (other == null) {
                player.sendMessage(new Bundle(playerData.locale()).prefix("player.not-found"));
            } else {
                PlayerData target = playerDB.get(other.uuid);
                target.mute(!target.mute());
                player.sendMessage(new Bundle(target.locale()).prefix(target.mute() ? "player.unmute" : "player.muted", target.name()));
            }
        });
        if (config.vote()) {
            handler.<Player>register("votekick", "[player_name]", "Player kick starts voting.", (arg, player) -> {
                if (!perm.check(player, "votekick")) return;
                Player other = playerGroup.find(p -> p.name.equalsIgnoreCase(arg[1]));
                PlayerData playerData = playerDB.get(player.uuid);
                Bundle bundle = new Bundle(playerData.locale());

                if (other == null) other = vars.players().get(Integer.parseInt(arg[1]));
                if (other == null) {
                    player.sendMessage(bundle.prefix("player.not-found"));
                    return;
                }
                if (other.isAdmin) {
                    player.sendMessage(bundle.prefix("vote.target-admin"));
                    return;
                }

                //vote.start(Vote.VoteType.kick, player, other);
            });
        }
    }
}
