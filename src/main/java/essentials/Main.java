package essentials;

import arc.ApplicationListener;
import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.math.Mathf;
import arc.struct.Array;
import arc.util.CommandHandler;
import arc.util.Strings;
import arc.util.Time;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import essentials.Threads.login;
import essentials.Threads.*;
import essentials.core.Discord;
import essentials.core.Log;
import essentials.core.PlayerDB;
import essentials.net.Client;
import essentials.net.Server;
import essentials.special.IpAddressMatcher;
import essentials.utils.Config;
import essentials.utils.Permission;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.UnitTypes;
import mindustry.entities.type.BaseUnit;
import mindustry.entities.type.Player;
import mindustry.entities.type.Unit;
import mindustry.game.Difficulty;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.io.SaveIO;
import mindustry.net.Administration.PlayerInfo;
import mindustry.net.Packets;
import mindustry.plugin.Plugin;
import mindustry.type.Item;
import mindustry.type.ItemType;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.power.NuclearReactor;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.mindrot.jbcrypt.BCrypt;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static essentials.Global.*;
import static essentials.Threads.Vote.isvoting;
import static essentials.Threads.*;
import static essentials.core.Log.writelog;
import static essentials.core.PlayerDB.*;
import static essentials.net.Client.serverconn;
import static essentials.utils.Config.jumpall;
import static essentials.utils.Config.jumpzone;
import static essentials.utils.Config.*;
import static essentials.utils.Permission.permission;
import static java.lang.Thread.sleep;
import static mindustry.Vars.*;
import static mindustry.core.NetClient.colorizeName;

public class Main extends Plugin {
    private ArrayList<String> nukeblock = new ArrayList<>();
    private static ArrayList<String> eventservers = new ArrayList<>();
    static ArrayList<String> powerblock = new ArrayList<>();
    static ArrayList<String> messagemonitor = new ArrayList<>();
    static ArrayList<String> messagejump = new ArrayList<>();
    static ArrayList<Tile> scancore = new ArrayList<>();
    private ArrayList<Tile> nukedata = new ArrayList<>();
    private Array<mindustry.maps.Map> maplist = Vars.maps.all();

    public static boolean threadactive = true;

    boolean isUpdating = false;

    public Main() {
        // 설정 시작
        config.main();

        // 클라이언트 연결 확인
        if (config.isClientenable()) {
            log("client","server-connecting");
            Client client = new Client();
            client.main(null, null, null);
        }

        // 플레이어 DB 연결
        PlayerDB playerdb = new PlayerDB();
        playerdb.openconnect();

        // 플레이어 DB 생성
        playerdb.createNewDataFile();

        // 모든 플레이어 연결 상태를 0으로 설정
        try{
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id,lastdate FROM players");
            while(rs.next()){
                if(isLoginold(rs.getString("lastdate"))){
                    writeData("UPDATE players SET connected = ?, connserver = ? WHERE id = ?", false, "none", rs.getInt("id"));
                }
            }
        } catch (SQLException e) {
            printStackTrace(e);
        }

        // 플레이어 DB 업그레이드
        Upgrade();

        // 클라이언트 플레이어 카운트 (중복 실행을 방지하기 위해 별도 스레드로 실행)
        executorService.submit(new jumpcheck());

        // 메세지 블럭에 의한 클라이언트 플레이어 카운트
        executorService.submit(new jumpdata());

        // 코어 자원소모 감시 시작
        executorService.submit(new monitorresource());

        // 임시로 밴한 플레이어들 밴 해제시간 카운트
        executorService.submit(new bantime());

        // 기록 시작
        if (config.isLogging()) {
            Log log = new Log();
            log.main();
        }

        //EssentialAI.main();

        // 서버기능 시작
        Thread server = new Thread(new Server());
        if (config.isServerenable()) {
            server.start();
        }

        // 권한 기능 시작
        new Permission().main();

        Events.on(TapConfigEvent.class, e -> {
            if (e.tile.entity != null && e.tile.entity.block != null && e.player != null && e.player.name != null && config.isBlockdetect() && config.isAlertdeposit()) {
                allsendMessage("tap-config", e.player.name, e.tile.entity.block.name);
                if(config.isDebug() && config.isAntigrief()){
                    log("log","antigrief-build-config", e.player.name, e.tile.block().name, e.tile.x, e.tile.y);
                }
            }
        });

        Events.on(TapEvent.class, e -> {
            if(isLogin(e.player)) {
                Thread t = new Thread(() -> {
                    for (String jumpdata : jumpzone) {
                        if (jumpdata.equals("")) return;
                        String[] data = jumpdata.split("/");
                        int startx = Integer.parseInt(data[0]);
                        int starty = Integer.parseInt(data[1]);
                        int tilex = Integer.parseInt(data[2]);
                        int tiley = Integer.parseInt(data[3]);
                        String serverip = data[4];
                        int serverport = Integer.parseInt(data[5]);
                        if (e.tile.x > startx && e.tile.x < tilex) {
                            if (e.tile.y > starty && e.tile.y < tiley) {
                                log("log", "player-jumped", e.player.name, serverip + ":" + serverport);
                                writeData("UPDATE players SET connected = ?, connserver = ? WHERE uuid = ?", false, "none", e.player.uuid);
                                Call.onConnect(e.player.con, serverip, serverport);
                            }
                        }
                    }
                });
                t.start();
            }
        });

        Events.on(WithdrawEvent.class, e->{
            if (e.tile.entity != null && e.tile.entity.block != null && e.player != null && e.player.name != null && config.isAntigrief()) {
                allsendMessage("log-withdraw", e.player.name, e.tile.entity.block.name, e.amount, e.tile.block().name);
                if (config.isDebug() && config.isAntigrief()) {
                    log("log","log-withdraw", e.player.name, e.tile.entity.block.name, e.amount, e.tile.block().name);
                }
            }
            if(e.tile.entity != null && e.tile.entity.block != null && e.player != null && e.player.name != null && config.isAntigrief() && state.rules.pvp){
                if(e.item.flammability > 0.001f) {
                    e.player.sendMessage(bundle(e.player, "flammable-disabled"));
                    e.player.clearItem();
                }
            }
        });

        // 게임오버가 되었을 때
        Events.on(GameOverEvent.class, e -> {
            if (state.rules.pvp) {
                int index = 5;
                for (int a = 0; a < 5; a++) {
                    if (state.teams.get(Team.all()[index]).cores.isEmpty()) {
                        index--;
                    }
                }
                if (index == 1) {
                    for (int i = 0; i < playerGroup.size(); i++) {
                        Player player = playerGroup.all().get(i);
                        if (isLogin(player)) {
                            if (player.getTeam().name.equals(e.winner.name)) {
                                JsonObject db = getData(player.uuid);
                                int pvpwin = db.getInt("pvpwincount");
                                pvpwin++;
                                writeData("UPDATE players SET pvpwincount = ? WHERE uuid = ?", pvpwin, player.uuid);
                            } else if (!player.getTeam().name.equals(e.winner.name)) {
                                JsonObject db = getData(player.uuid);
                                int pvplose = db.getInt("pvplosecount");
                                pvplose++;
                                writeData("UPDATE players SET pvplosecount = ? WHERE uuid = ?", pvplose, player.uuid);
                            }
                        }
                    }
                    pvpteam.clear();
                }
            } else if(state.rules.attackMode){
                for (int i = 0; i < playerGroup.size(); i++) {
                    Player player = playerGroup.all().get(i);
                    if (isLogin(player)) {
                        JsonObject db = getData(player.uuid);
                        int attackclear = db.getInt("attackclear");
                        attackclear++;
                        writeData("UPDATE players SET attackclear = ? WHERE uuid = ?", attackclear, player.uuid);
                    }
                }
            }
        });

        // 맵이 불러와졌을 때
        Events.on(WorldLoadEvent.class, e -> {
            playtime = "00:00.00";

            // 전력 노드 정보 초기화
            powerblock.clear();

            peacetime = true;
        });

        Events.on(PlayerConnect.class, e -> {
            // 닉네임이 블랙리스트에 등록되어 있는지 확인
            for (String s : blacklist) {
                if (e.player.name.matches(s)) {
                    try{
                        String json = Jsoup.connect("http://ipapi.co/" + Vars.netServer.admins.getInfo(e.player.uuid).lastIP + "/json").ignoreContentType(true).execute().body();
                        JsonObject result = JsonParser.object().from(json);
                        String language = result.getString("languages") == null ? "en" : result.getString("languages");
                        Call.onKick(e.player.con, nbundle(language, "nickname-blacklisted-kick"));
                        log("log", "nickname-blacklisted", e.player.name);
                    } catch (Exception ex) {
                        printStackTrace(ex);
                    }
                }
            }

            if (e.player.name.length() > 32) e.player.con.kick("Nickname too long!");

            /*if(config.isStrictname()){
                if(e.player.name.length() < 3){
                    player.con.kick("The nickname is too short!\n닉네임이 너무 짧습니다!");
                    log("log","nickname-short");
                }
                if(e.player.name.matches("^(?=.*\\\\d)(?=.*[~`!@#$%\\\\^&*()-])(?=.*[a-z])(?=.*[A-Z])$")){
                    e.player.con.kick("Server doesn't allow special characters.\n서버가 특수문자를 허용하지 않습니다.");
                    log("log","nickname-special", player.name);
                }
            }*/
        });

        // 플레이어가 아이템을 특정 블록에다 직접 가져다 놓았을 때
        Events.on(DepositEvent.class, e -> {
            // 만약 그 특정블록이 토륨 원자로이며, 맵 설정에서 원자로 폭발이 비활성화 되었을 경우
            if (e.tile.block() == Blocks.thoriumReactor && config.isDetectreactor() && !state.rules.reactorExplosions) {
                nukeblock.add(e.tile.entity.tileX() + "/" + e.tile.entity.tileY() + "/" + e.player.name);
                Thread t = new Thread(() -> {
                    try {
                        for (String nukedata : nukeblock) {
                            String[] data = nukedata.split("/");
                            int x = Integer.parseInt(data[0]);
                            int y = Integer.parseInt(data[1]);
                            String builder = data[2];
                            NuclearReactor.NuclearReactorEntity entity = (NuclearReactor.NuclearReactorEntity) world.tile(x, y).entity;
                            if (entity.heat >= 0.01) {
                                sleep(50);
                                for (int a = 0; a < playerGroup.size(); a++) {
                                    Player other = playerGroup.all().get(a);
                                    other.sendMessage(bundle(other, "detect-thorium"));
                                }

                                writelog("griefer",nbundle("griefer-detect-reactor-log",getTime(),builder));
                                Call.onTileDestroyed(world.tile(x, y));
                            } else {
                                sleep(1950);
                                if (entity.heat >= 0.01) {
                                    for (int a = 0; a < playerGroup.size(); a++) {
                                        Player other = playerGroup.all().get(a);
                                        other.sendMessage(bundle(other, "detect-thorium"));
                                    }
                                    writelog("griefer",nbundle("griefer-detect-reactor-log",getTime(),builder));
                                    Call.onTileDestroyed(world.tile(x, y));
                                }
                            }
                        }
                    } catch (Exception ex) {
                        printStackTrace(ex);
                    }
                });
                t.start();
            }
            if(config.isAlertdeposit()) {
                allsendMessage("depositevent", e.player.name, e.player.item().item.name, e.tile.block().name);
            }
        });

        // 플레이어가 서버에 들어왔을 때
        Events.on(PlayerJoin.class, e -> {
            e.player.isAdmin = false;

            Team team = Team.crux;
            int index = e.player.getTeam().id+1;
            while (index != e.player.getTeam().id){
                if (index >= Team.all().length){
                    index = 0;
                }
                if (state.teams.get(Team.all()[index]).cores.isEmpty()){
                    team = Team.all()[index];
                }
                index++;
            }
            e.player.setTeam(team);
            Call.onPlayerDeath(e.player);

            Thread t = new Thread(() -> {
                Thread.currentThread().setName(e.player.name+" Player Join");
                if (config.isLoginenable()) {
                    if (isNocore(e.player)) {
                        JsonObject db = getData(e.player.uuid);
                        if (db.has("uuid")) {
                            if (db.getString("uuid").equals(e.player.uuid)) {
                                e.player.sendMessage(bundle(e.player, "autologin"));
                                playerdb.load(e.player, null);
                            }
                        } else {
                            // 로그인 요구
                            try {
                                String message;
                                String json = Jsoup.connect("http://ipapi.co/" + Vars.netServer.admins.getInfo(e.player.uuid).lastIP + "/json").ignoreContentType(true).execute().body();
                                JsonObject result = JsonParser.object().from(json);
                                String language = result.getString("languages") == null ? "en" : result.getString("languages");

                                /*JsonObject translate = JsonParser.object().from(Jsoup.connect("https://api.mymemory.translated.net/get?q=" + text + "&langpair=" + source + "|+" + target).ignoreContentType(true).execute().body());
                                String translate_result = translate.getObject("responseData").getString("translatedText");
                                Data.put("data",translate_result);*/
                                if (config.getPasswordmethod().equals("discord")) {
                                    message = nbundle(language, "login-require-discord")+"\n"+config.getDiscordLink();
                                } else {
                                    message = nbundle(language, "login-require-password");
                                }
                                Call.onInfoMessage(e.player.con, message);
                            } catch (Exception ex){
                                printStackTrace(ex);
                            }
                        }
                    }
                } else if (!config.isLoginenable()) {
                    // 로그인 기능이 꺼져있을 때, 바로 계정 등록을 하고 데이터를 로딩함
                    if (playerdb.register(e.player)) {
                        playerdb.load(e.player, null);
                    } else {
                        Call.onKick(e.player.con, nbundle("plugin-error-kick"));
                    }
                }
                // VPN을 사용중인지 확인
                if (config.isAntivpn()) {
                    try {
                        InputStream reader = getClass().getResourceAsStream("/ipv4.txt");
                        BufferedReader br = new BufferedReader(new InputStreamReader(reader));

                        String ip = netServer.admins.getInfo(e.player.uuid).lastIP;
                        String line;
                        while ((line = br.readLine()) != null) {
                            IpAddressMatcher match = new IpAddressMatcher(line);
                            if (match.matches(ip)) {
                                e.player.con.kick(nbundle("antivpn-kick"));
                            }
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            });
            t.start();

            // PvP 평화시간 설정
            if (config.isEnableantirush() && state.rules.pvp && peacetime) {
                state.rules.playerDamageMultiplier = 0f;
                state.rules.playerHealthMultiplier = 0.001f;
            }

            // 플레이어 인원별 난이도 설정
            if(config.isAutodifficulty()){
                int total = playerGroup.size();
                if(config.getEasy() >= total){
                    state.rules.waveSpacing = Difficulty.valueOf("easy").waveTime * 60 * 60 * 2;
                } else if(config.getNormal() == total){
                    state.rules.waveSpacing = Difficulty.valueOf("normal").waveTime * 60 * 60 * 2;
                } else if(config.getHard() == total){
                    state.rules.waveSpacing = Difficulty.valueOf("hard").waveTime * 60 * 60 * 2;
                } else if(config.getInsane() <= total){
                    state.rules.waveSpacing = Difficulty.valueOf("insane").waveTime * 60 * 60 * 2;
                }
            }
        });

        // 플레이어가 서버에서 탈주했을 때
        Events.on(PlayerLeave.class, e -> {
            String uuid = e.player.uuid;
            if (isLogin(e.player)) {
                writeData("UPDATE players SET connected = ?, connserver = ? WHERE uuid = ?", false, "none", uuid);
            }
        });

        // 플레이어가 수다떨었을 때
        Events.on(PlayerChatEvent.class, e -> {
            if (isLogin(e.player)) {
                String check = String.valueOf(e.message.charAt(0));
                // 명령어인지 확인
                if (!check.equals("/")) {
                    JsonObject db = getData(e.player.uuid);

                    if (e.message.matches("(.*쌍[\\S\\s]{0,2}(년|놈).*)|(.*(씨|시)[\\S\\s]{0,2}(벌|빨|발|바).*)|(.*장[\\S\\s]{0,2}애.*)|(.*(병|븅)[\\S\\s]{0,2}(신|쉰|싄).*)|(.*(좆|존|좃)[\\S\\s]{0,2}(같|되|는|나).*)|(.*(개|게)[\\S\\s]{0,2}(같|갓|새|세|쉐).*)|(.*(걸|느)[\\S\\s]{0,2}(레|금).*)|(.*(꼬|꽂|고)[\\S\\s]{0,2}(추|츄).*)|(.*(니|너)[\\S\\s]{0,2}(어|엄|엠|애|m|M).*)|(.*(노)[\\S\\s]{0,1}(애|앰).*)|(.*(섹|쎅)[\\S\\s]{0,2}(스|s|쓰).*)|(ㅅㅂ|ㅄ|ㄷㅊ)|(.*(섹|쎅)[\\S\\s]{0,2}(스|s|쓰).*)|(.*s[\\S\\s]{0,1}e[\\S\\s]{0,1}x.*)")) {
                        Call.onKick(e.player.con, nbundle(db.getString("language"),"kick-swear"));
                    } else if(e.message.equals("y") && isvoting) {
                        // 투표가 진행중일때
                        if (Vote.list.contains(e.player.uuid)) {
                            e.player.sendMessage(bundle(e.player, "vote-already"));
                        } else {
                            Vote.list.add(e.player.uuid);
                            int current = Vote.list.size();
                            if (Vote.require - current <= 0) {
                                Vote.cancel();
                                return;
                            }
                            for (Player others : playerGroup.all()) {
                                if (isLogin(others)) others.sendMessage(bundle(others, "vote-current", current, Vote.require - current));
                            }
                        }
                    } else {
                        String perm = db.getString("permission");
                        if(permission.getObject(perm).has("prefix")) {
                            Call.sendMessage(permission.getObject(perm).getString("prefix").replace("%1",colorizeName(e.player.id,e.player.name)).replace("%2", e.message));
                        } else {
                            Call.sendMessage(colorizeName(e.player.id, e.player.name) + "[white] : " + e.message);
                        }
                    }

                    // 서버간 대화기능 작동
                    if (db.getBoolean("crosschat")) {
                        if (config.isClientenable()) {
                            Client client = new Client();
                            client.main("chat", e.player, e.message);
                        } else if (config.isServerenable()) {
                            // 메세지를 모든 클라이언트에게 전송함
                            String msg = "[" + e.player.name + "]: " + e.message;
                            try {
                                for (Server.Service ser : Server.list) {
                                    ser.os.writeBytes(Base64.encode(encrypt(msg,ser.spec,ser.cipher)));
                                    ser.os.flush();
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        } else if (!config.isClientenable() && !config.isServerenable()) {
                            e.player.sendMessage(bundle(e.player, "no-any-network"));
                            writeData("UPDATE players SET crosschat = ? WHERE uuid = ?", false, e.player.uuid);
                        }
                    }
                }

                // 마지막 대화 데이터를 DB에 저장함
                writeData("UPDATE players SET lastchat = ? WHERE uuid = ?", e.message, e.player.uuid);

                // 번역
                if(config.getClientId() != null && config.getClientSecret() != null) {
                    if (!config.getClientId().equals("") && !config.getClientSecret().equals("")) {
                        Thread t = new Thread(() -> {
                            try {
                                JsonObject orignaldata = getData(e.player.uuid);
                                for (int i = 0; i < playerGroup.size(); i++) {
                                    Player p = playerGroup.all().get(i);
                                    if (!isNocore(p)) {
                                        JsonObject data = getData(p.uuid);
                                        String[] support = {"ko", "en", "zh-CN", "zh-TW", "es", "fr", "vi", "th", "id"};
                                        String language = data.getString("language");
                                        String orignal = orignaldata.getString("language");
                                        if (!language.equals(orignal)) {
                                            boolean found = false;
                                            for (String s : support) {
                                                if (orignal.equals(s)) {
                                                    found = true;
                                                    break;
                                                }
                                            }
                                            if (found) {
                                                String response = Jsoup.connect("https://naveropenapi.apigw.ntruss.com/nmt/v1/translation")
                                                        .method(Connection.Method.POST)
                                                        .header("X-NCP-APIGW-API-KEY-ID", config.getClientId())
                                                        .header("X-NCP-APIGW-API-KEY", config.getClientSecret())
                                                        .data("source", orignaldata.getString("language"))
                                                        .data("target", data.getString("language"))
                                                        .data("text", e.message)
                                                        .ignoreContentType(true)
                                                        .followRedirects(true)
                                                        .execute()
                                                        .body();
                                                JsonObject object = JsonParser.object().from(response);
                                                if(!object.has("error")) {
                                                    String result = object.getObject("message").getObject("result").getString("translatedText");
                                                    if (data.getBoolean("translate")) {
                                                        p.sendMessage("[green]" + e.player.name + "[orange]: [white]" + result);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (Exception ex) {
                                printStackTrace(ex);
                            }
                        });
                        t.start();
                    }
                }
            }
        });

        // 플레이어가 블럭을 건설했을 때
        Events.on(BlockBuildEndEvent.class, e -> {
            if (!e.breaking && e.player != null && e.player.buildRequest() != null && !isNocore(e.player) && e.tile != null && e.player.buildRequest() != null) {
                Thread t = new Thread(() -> {
                    JsonObject db = getData(e.player.uuid);
                    String name = e.tile.block().name;
                    try {
                        int data = db.getInt("placecount");
                        int exp = db.getInt("exp");

                        Yaml yaml = new Yaml();
                        Map<String, Object> obj = yaml.load(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/Exp.yml").readString()));
                        int blockexp;
                        if (obj.get(name) != null) {
                            blockexp = (int) obj.get(name);
                        } else {
                            blockexp = 0;
                        }
                        int newexp = exp + blockexp;
                        data++;

                        writeData("UPDATE players SET lastplacename = ?, placecount = ?, exp = ? WHERE uuid = ?", e.tile.block().name, data, newexp, e.player.uuid);

                        if (e.player.buildRequest().block == Blocks.thoriumReactor) {
                            int reactorcount = db.getInt("reactorcount");
                            reactorcount++;
                            writeData("UPDATE players SET reactorcount = ? WHERE uuid = ?", reactorcount, e.player.uuid);
                        }
                    } catch (Exception ex) {
                        printStackTrace(ex);
                    }


                    // 메세지 블럭을 설치했을 경우, 해당 블럭을 감시하기 위해 위치를 저장함.
                    if (e.tile.entity.block == Blocks.message) {
                        messagemonitor.add(e.tile.x+"|"+e.tile.y);
                    }

                    // 플레이어가 토륨 원자로를 만들었을 때, 감시를 위해 그 원자로의 위치를 저장함.
                    if (e.tile.entity.block == Blocks.thoriumReactor) {
                        nukeposition.add(e.tile.entity.tileX() + "/" + e.tile.entity.tileY());
                        nukedata.add(e.tile);
                    }
                });
                executorService.submit(t);
                if(config.isDebug() && config.isAntigrief()){
                    log("log","antigrief-build-finish", e.player.name, e.tile.block().name, e.tile.x, e.tile.y);
                }
            }
        });

        // 플레이어가 블럭을 뽀갰을 때
        Events.on(BuildSelectEvent.class, e -> {
            if (e.builder instanceof Player && e.builder.buildRequest() != null && !e.builder.buildRequest().block.name.matches(".*build.*")) {
                if (e.breaking) {
                    Thread t = new Thread(() -> {
                        JsonObject db = getData(((Player) e.builder).uuid);
                        String name = e.tile.block().name;
                        try {
                            int data = db.getInt("breakcount");
                            int exp = db.getInt("exp");

                            Yaml yaml = new Yaml();
                            Map<String, Object> obj = yaml.load(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/Exp.yml").readString()));
                            int blockexp;
                            if (obj.get(name) != null) {
                                blockexp = (int) obj.get(name);
                            } else {
                                blockexp = 0;
                            }
                            int newexp = exp + blockexp;
                            data++;

                            writeData("UPDATE players SET lastplacename = ?, breakcount = ?, exp = ? WHERE uuid = ?", e.tile.block().name, data, newexp, ((Player) e.builder).uuid);
                            if (e.builder.buildRequest() != null && e.builder.buildRequest().block == Blocks.thoriumReactor) {
                                int reactorcount = db.getInt("reactorcount");
                                reactorcount++;
                                writeData("UPDATE players SET reactorcount = ? WHERE uuid = ?", reactorcount, ((Player) e.builder).uuid);
                            }
                        } catch (Exception ex) {
                            printStackTrace(ex);
                            Call.onKick(((Player) e.builder).con, nbundle(db.getString("language"),"not-logged"));
                        }

                        // 메세지 블럭을 파괴했을 때, 위치가 저장된 데이터를 삭제함
                        if (e.builder.buildRequest().block == Blocks.message) {
                            try {
                                for (int i = 0; i < powerblock.size(); i++) {
                                    String raw = powerblock.get(i);
                                    String[] data = raw.split("/");

                                    int x = Integer.parseInt(data[0]);
                                    int y = Integer.parseInt(data[1]);

                                    if (x == e.tile.x && y == e.tile.y) {
                                        powerblock.remove(i);
                                        break;
                                    }
                                }
                            } catch (Exception ex) {
                                printStackTrace(ex);
                            }
                        }

                        // Exp Playing Game (EPG)
                        if (config.isExplimit()) {
                            int level = (int) db.get("level");
                            Yaml yaml = new Yaml();
                            Map<String, Integer> obj = yaml.load(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/BlockReqExp.yml").readString()));
                            int blockreqlevel = 100;
                            if (obj.get(name) != null) {
                                blockreqlevel = obj.get(name);
                            } else if (!e.tile.block().name.equals("air")) {
                                log("err", "epg-block-not-valid", name);
                            } else {
                                return;
                            }

                            if (level < blockreqlevel && !e.tile.block().name.equals("air")) {
                                Call.onDeconstructFinish(e.tile, e.tile.block(), ((Player) e.builder).id);
                                ((Player) e.builder).sendMessage(nbundle(((Player) e.builder), "epg-block-require", name, blockreqlevel));
                            }
                        }
                    });
                    executorService.submit(t);
                }
                if(config.isDebug() && config.isAntigrief()){
                    log("log","antigrief-destroy", ((Player) e.builder).name, e.tile.block().name, e.tile.x, e.tile.y);
                }
            }
        });

        // 유닛을 박살냈을 때
        Events.on(UnitDestroyEvent.class, e -> {
            // 뒤진(?) 유닛이 플레이어일때
            if (e.unit instanceof Player) {
                Player player = (Player) e.unit;
                JsonObject db = getData(player.uuid);
                if (!state.teams.get(player.getTeam()).cores.isEmpty() && !db.isNull("deathcount")) {
                    int deathcount = db.getInt("deathcount");
                    deathcount++;
                    writeData("UPDATE players SET deathcount = ? WHERE uuid = ?", deathcount, player.uuid);
                }
            }

            // 터진 유닛수만큼 카운트해줌
            if (playerGroup != null && playerGroup.size() > 0) {
                Thread t = new Thread(() -> {
                    for (int i = 0; i < playerGroup.size(); i++) {
                        Player player = playerGroup.all().get(i);
                        if (!state.teams.get(player.getTeam()).cores.isEmpty()) {
                            JsonObject db = getData(player.uuid);
                            int killcount;
                            if (db.has("killcount")) {
                                killcount = db.getInt("killcount");
                            } else {
                                return;
                            }
                            killcount++;
                            writeData("UPDATE players SET killcount = ? WHERE uuid = ?", killcount, player.uuid);
                        }
                    }
                });
                executorService.submit(t);
            }
        });

        // 플레이어가 밴당했을 때 공유기능 작동
        Events.on(PlayerBanEvent.class, e -> {
            Thread bansharing = new Thread(() -> {
                if (config.isBanshare() && config.isClientenable()) {
                    Client client = new Client();
                    client.main("bansync", null, null);
                }

                for (Player player : playerGroup.all()) {
                    player.sendMessage(bundle(player, "player-banned", e.player.name));
                    if (netServer.admins.isIDBanned(player.uuid)) {
                        player.con.kick(Packets.KickReason.banned);
                    }
                }

                accountban(true, e.player.uuid);
            });
            executorService.submit(bansharing);
        });

        // 이건 IP 밴당했을때 작동
        Events.on(PlayerIpBanEvent.class, e -> {
            Thread bansharing = new Thread(() -> {
                if (config.isBanshare() && config.isClientenable()) {
                    Client client = new Client();
                    client.main("bansync", null, null);
                }
            });
           executorService.submit(bansharing);
        });

        // 이건 밴 해제되었을 때 작동
        Events.on(PlayerUnbanEvent.class, e -> {
            if(serverconn) {
                Client client = new Client();
                client.main("unban", null, e.player.uuid + "|<unknown>");
            }
        });

        // 이건 IP 밴이 해제되었을 때 작동
        Events.on(PlayerIpUnbanEvent.class, e -> {
            if(serverconn) {
                Client client = new Client();
                client.main("unban", null, "<unknown>|"+e.ip);
            }
        });

        // 로그인 기능이 켜져있을때, 비 로그인 사용자들에게 알림을 해줌
        Timer timer = new Timer(true);
        if (config.isLoginenable()) {
            timer.scheduleAtFixedRate(new login(), 60000, 60000);
        }

        // 1초마다 실행되는 작업 시작
        timer.scheduleAtFixedRate(new Threads(), 1000, 1000);

        // 롤백 명령어에서 사용될 자동 저장작업 시작
        timer.scheduleAtFixedRate(new AutoRollback(), config.getSavetime() * 60000, config.getSavetime() * 60000);

        // 0.016초마다 실행 및 서버 종료시 실행할 작업
        Core.app.addListener(new ApplicationListener() {
            int scandelay,delaycount,resetdelay,copper,lead,graphite,titanium,thorium,silicon,phase_fabric,surge_alloy,plastanium,metaglass = 0;
            boolean a1, a2, a3 = false;
            StringBuilder scancore_text = new StringBuilder();
            // public Array<Integer> pre = new Array<>();
            // public Array<Item> name = new Array<>();

            void setText(int orignal, int amount, Item item){
                String color;
                String data;
                int val;
                if (state.teams.get(Team.sharded).cores.first().items.has(item)) {
                    val = amount - orignal;
                    if (val > 0) {
                        color = "[#00f]+";
                    } else {
                        color = "[red]-";
                    }
                    data = "[]" + item.name + ": " + color + val + "/s\n";
                    scancore_text.append(data);
                }
            }

            @Override
            public void update() {
/*                if(resetdelay >= 300){
                    resetdelay = 0;
                    cool = false;
                } else {
                    resetdelay++;
                }*/

                if (delaycount == 30) {
                    try {
                        // 메세지 블럭에다 전력량을 표시 (반드시 게임 시간과 똑같이 작동되어야만 함)
                        for (int i = 0; i < powerblock.size(); i++) {
                            String raw = powerblock.get(i);

                            String[] data = raw.split("/");

                            int x = Integer.parseInt(data[0]);
                            int y = Integer.parseInt(data[1]);
                            int target_x = Integer.parseInt(data[2]);
                            int target_y = Integer.parseInt(data[3]);

                            if (world.tile(x, y).block() != Blocks.message) {
                                powerblock.remove(i);
                                return;
                            }

                            float current;
                            float product;
                            float using;
                            try {
                                current = world.tile(target_x, target_y).entity.power.graph.getPowerBalance() * 60;
                                using = world.tile(target_x, target_y).entity.power.graph.getPowerNeeded() * 60;
                                product = world.tile(target_x, target_y).entity.power.graph.getPowerProduced() * 60;
                            } catch (Exception ignored) {
                                powerblock.remove(i);
                                return;
                            }
                            String text = "Power status\n" +
                                    "Current: [sky]" + Math.round(current) + "[]\n" +
                                    "Using: [red]" + Math.round(using) + "[]\n" +
                                    "Production: [green]" + Math.round(product) + "[]";
                            Call.setMessageBlockText(null, world.tile(x, y), text);
                        }
                        // 타이머 초기화
                        delaycount = 0;
                        a1 = false;
                        a2 = false;
                        a3 = false;
                    } catch (Exception ignored) {}
                } else {
                    delaycount++;
                }

                if(scandelay == 60){
                    /*if(state.is(GameState.State.playing)) {
                        for (Item item : content.items()) {
                            if (item.type == ItemType.material) {
                                pre.add(state.teams.get(Team.sharded).cores.first().items.get(item));
                                name.add(item);
                            }
                        }

                        for (int a=0;a<content.items().size;a++){
                            Item item = content.item(a);
                            if (item.type == ItemType.material) {
                                int resource;
                                if (state.teams.get(Team.sharded).cores.isEmpty()) return;
                                if (state.teams.get(Team.sharded).cores.first().items.has(item)) {
                                    resource = state.teams.get(Team.sharded).cores.first().items.get(item);
                                } else {
                                    return;
                                }
                                int temp = resource - pre.get(a);
                                nlog("debug",resource+"/"+pre.get(a));
                                if(resetdelay >= 600){
                                    pre.set(a,resource);
                                }
                                if (temp < -55 && !cool) {
                                    StringBuilder using = new StringBuilder();
                                    for (int b = 0; b < playerGroup.size(); b++) {
                                        Player p = playerGroup.all().get(b);
                                        if (p.buildRequest() != null) {
                                            for (int c = 0; c < p.buildRequest().block.requirements.length; c++) {
                                                Item ad = p.buildRequest().block.requirements[c].item;
                                                if (ad == name.get(a)) {
                                                    using.append(p.name).append(", ");
                                                }
                                            }
                                        }
                                    }
                                    allsendMessage("resource-fast", name.get(a).name);
                                    allsendMessage("resource-fast-use", name.get(a).name, using);
                                    cool = true;
                                }
                            }
                        }
                    }*/
                    // 코어 자원 소모량 감시
                    try {
                        for (Item item : content.items()) {
                            if (item.type == ItemType.material) {
                                int amount = state.teams.get(Team.sharded).cores.first().items.get(item);
                                switch (item.name) {
                                    case "copper":
                                        setText(copper, amount, Items.copper);
                                        copper = amount;
                                        break;
                                    case "lead":
                                        setText(lead, amount, Items.lead);
                                        lead = amount;
                                        break;
                                    case "graphite":
                                        setText(graphite, amount, Items.graphite);
                                        graphite = amount;
                                        break;
                                    case "titanium":
                                        setText(titanium, amount, Items.titanium);
                                        titanium = amount;
                                        break;
                                    case "thorium":
                                        setText(thorium, amount, Items.thorium);
                                        thorium = amount;
                                        break;
                                    case "silicon":
                                        setText(silicon, amount, Items.silicon);
                                        silicon = amount;
                                        break;
                                    case "phase-fabric":
                                        setText(phase_fabric, amount, Items.phasefabric);
                                        phase_fabric = amount;
                                        break;
                                    case "surge-alloy":
                                        setText(surge_alloy, amount, Items.surgealloy);
                                        surge_alloy = amount;
                                        break;
                                    case "plastanium":
                                        setText(plastanium, amount, Items.plastanium);
                                        plastanium = amount;
                                        break;
                                    case "metaglass":
                                        setText(metaglass, amount, Items.metaglass);
                                        metaglass = amount;
                                        break;
                                }
                            }
                        }

                        for (int a = 0; a < scancore.size(); a++) {
                            if (scancore.get(a).entity == null || scancore.get(a).entity.block != Blocks.message) {
                                scancore.remove(a);
                                break;
                            }
                            Call.setMessageBlockText(null, scancore.get(a), scancore_text.toString());
                            scancore_text.setLength(0);
                        }
                    }catch (Exception ignored){}
                    scandelay = 0;
                } else {
                    scandelay++;
                }

                // 핵 폭발감지
                if(config.isDetectreactor()) {
                    for (int i = 0; i < nukedata.size(); i++) {
                        Tile target = nukedata.get(i);
                        try {
                            NuclearReactor.NuclearReactorEntity entity = (NuclearReactor.NuclearReactorEntity) target.entity;
                            if (entity.heat >= 0.2f && entity.heat <= 0.39f && !a1) {
                                allsendMessage("thorium-overheat-green", Math.round(entity.heat * 100), target.x, target.y);
                                a1 = true;
                            }
                            if (entity.heat >= 0.4f && entity.heat <= 0.79f && !a2) {
                                allsendMessage("thorium-overheat-yellow", Math.round(entity.heat * 100), target.x, target.y);
                                a2 = true;
                            }
                            if (entity.heat >= 0.8f && entity.heat <= 0.95f && !a3) {
                                allsendMessage("thorium-overheat-red", Math.round(entity.heat * 100), target.x, target.y);
                                a3 = true;
                            }
                            if (entity.heat >= 0.95f) {
                                for (int a = 0; a < playerGroup.size(); a++) {
                                    Player p = playerGroup.all().get(a);
                                    p.sendMessage(bundle(p, "thorium-overheat-red", Math.round(entity.heat * 100), target.x, target.y));
                                    if (p.isAdmin) {
                                        p.setNet(target.x * 8, target.y * 8);
                                    }
                                }
                                Call.onDeconstructFinish(target, Blocks.air, 0);
                                allsendMessage("thorium-removed");
                            }
                        } catch (Exception e) {
                            nukeblock.remove(i);
                            break;
                        }
                    }
                }
            }

            public void dispose() {
                threadactive = false;

                // 타이머 스레드 종료
                try {
                    timer.cancel();
                    if (isvoting) {
                        Vote.cancel();
                    }
                    log("log","count-thread-disabled");
                } catch (Exception e) {
                    log("err","count-thread-disable-error");
                    printStackTrace(e);
                }

                // 서버 종료
                if (config.isServerenable()) {
                    try {
                        for (Server.Service ser : Server.list) {
                            ser.interrupt();
                            ser.os.close();
                            ser.in.close();
                            ser.socket.close();
                            if (ser.isInterrupted()) {
                                Server.list.remove(ser);
                            } else {
                                log("err","server-thread-disable-error");
                            }
                        }

                        Server.active = false;
                        Server.serverSocket.close();
                        server.interrupt();

                        log("log","server-thread-disabled");
                    } catch (Exception e) {
                        printStackTrace(e);
                        log("err","server-thread-disable-error");
                    }
                }

                // 클라이언트 종료
                if (config.isClientenable() && serverconn) {
                    Client client = new Client();
                    client.main("exit", null, null);
                    log("log","client-thread-disabled");
                }

                // 모든 이벤트 서버 종료
                for (Process value : process) {
                    value.destroy();
                    if (value.isAlive()) {
                        try {
                            throw new Exception("Process stop failed");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                // 모든 스레드 종료
                executorService.shutdown();
                if (executorService.isTerminated() && executorService.isShutdown() && config.isDebug()) log("debug","executorservice dead");

                // DB 종료
                if (!closeconnect() && config.isDebug()) {
                    try {
                        throw new Exception("DB stop failed");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                log("log","thread-disabled");
                try {
                    JsonObject data = new JsonObject();
                    data.put("banned",banned);
                    data.put("blacklist",blacklist);
                    data.put("jumpzone",jumpzone);
                    data.put("jumpall",jumpall);
                    data.put("jumpcount",jumpcount);
                    data.put("servername", Core.settings.getString("servername"));
                    new ObjectMapper().writeValue(Core.settings.getDataDirectory().child("mods/Essentials/data/data.json").file(), data);

                    sleep(500);
                    if(!isUpdating) System.exit(0);
                } catch (Exception ignored) {}
            }
        });

        // 서버가 켜진 시간을 0으로 설정
        uptime = "00:00.00";

        Events.on(ServerLoadEvent.class, e-> {
            // 업데이트 확인
            if(config.isUpdate()) {
                log("client","client-checking-version");
                try {
                    JsonObject json = JsonParser.object().from(Jsoup.connect("https://api.github.com/repos/kieaer/Essentials/releases/latest").ignoreContentType(true).execute().body());

                    for(int a=0;a<mods.list().size;a++){
                        if(mods.list().get(a).meta.name.equals("Essentials")){
                            version = mods.list().get(a).meta.version;
                        }
                    }

                    DefaultArtifactVersion latest = new DefaultArtifactVersion(json.getString("tag_name"));
                    DefaultArtifactVersion current = new DefaultArtifactVersion(version);

                    if (latest.compareTo(current) > 0) {
                        log("client","version-new");
                        net.dispose();
                        Thread t = new Thread(() -> {
                            try {
                                nlog("log", nbundle("update-description", json.get("tag_name")));
                                System.out.println(json.getString("body"));
                                URL url = new URL(json.getArray("assets").getObject(0).getString("browser_download_url"));

                                System.out.println(nbundle("plugin-downloading-standby"));
                                threadactive = false;
                                timer.cancel();
                                if (config.isServerenable()) {
                                    try {
                                        for (Server.Service ser : Server.list) {
                                            ser.interrupt();
                                            ser.os.close();
                                            ser.in.close();
                                            ser.socket.close();
                                            Server.list.remove(ser);
                                        }

                                        Server.active = false;
                                        Server.serverSocket.close();
                                        server.interrupt();
                                    } catch (Exception ignored) {}
                                }
                                if (config.isClientenable() && serverconn) {
                                    Client client = new Client();
                                    client.main("exit", null, null);
                                }
                                executorService.shutdown();
                                closeconnect();
                                try {
                                    JsonObject data = new JsonObject();
                                    data.put("banned",banned);
                                    data.put("blacklist",blacklist);
                                    data.put("jumpzone",jumpzone);
                                    data.put("jumpall",jumpall);
                                    data.put("jumpcount",jumpcount);
                                    data.put("servername", Core.settings.getString("servername"));
                                    new ObjectMapper().writeValue(Core.settings.getDataDirectory().child("mods/Essentials/data/data.json").file(), data);
                                } catch (Exception ignored) {}

                                BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(Core.settings.getDataDirectory().child("mods/Essentials.jar").file()));
                                URLConnection urlConnection = url.openConnection();
                                InputStream is = urlConnection.getInputStream();
                                int size = urlConnection.getContentLength();
                                byte[] buf = new byte[512];
                                int byteRead;
                                int byteWritten = 0;
                                long startTime = System.currentTimeMillis();
                                System.out.println(nbundle("plugin-downloading"));
                                while ((byteRead = is.read(buf)) != -1) {
                                    outputStream.write(buf, 0, byteRead);
                                    byteWritten += byteRead;

                                    printProgress(startTime,size,byteWritten);
                                }
                                is.close();
                                outputStream.close();
                                System.out.println("\n"+nbundle("plugin-downloading-done"));
                                Core.app.exit();
                                System.exit(0);
                            }catch (Exception ex){
                                System.out.println("\n"+nbundle("plugin-downloading-fail"));
                                printStackTrace(ex);
                                Core.app.exit();
                                System.exit(0);
                            }
                        });
                        t.start();
                    } else if (latest.compareTo(current) == 0) {
                        log("client","version-current");
                    } else if (latest.compareTo(current) < 0) {
                        log("client","version-devel");
                    }
                } catch (Exception ex) {
                    printStackTrace(ex);
                }
            }

            // Discord 봇 시작
            if(config.getPasswordmethod().equals("discord")){
                Discord ds = new Discord();
                ds.main();
            }

            // 채팅 포맷 변경
            netServer.admins.addChatFilter((player, text) -> null);
        });
    }

    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.register("accountban","<ban/unban> <account_uuid>", (arg) -> {
            if(arg[0].equals("ban")) {
                if (arg[1].length() == 12) {
                    if(accountban(true, arg[1])){
                        log("player","success");
                    } else {
                        log("playerwarn","failed");
                    }
                } else {
                    log("warn","wrong-command");
                }
            } else if(arg[0].equals("unban")){
                if (arg[1].length() == 12) {
                    if(accountban(false, arg[1])){
                        log("player","success");
                    } else {
                        log("playerwarn","failed");
                    }
                } else {
                    log("warn","wrong-command");
                }
            } else {
                log("warn","wrong-command");
            }
        });
        handler.register("admin", "<name>","Set admin status to player.", (arg) -> {
            if(arg.length == 0) {
                log("warn","no-parameter");
                return;
            }
            log("log","use-setperm");
        });
        handler.register("allinfo", "<name>", "Show player information.", (arg) -> {
            if(arg.length == 0) {
                log("warn","no-parameter");
                return;
            }
            Thread t = new Thread(() -> {
                try{
                    String sql = "SELECT * FROM players WHERE name='"+arg[0]+"'";
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(sql);
                    nlog("log","Data line start.");
                    while(rs.next()){
                        String datatext = "\nPlayer Information\n" +
                                "========================================\n" +
                                "Name: "+rs.getString("name")+"\n" +
                                "UUID: "+rs.getString("uuid")+"\n" +
                                "Country: "+rs.getString("country")+"\n" +
                                "Block place: "+rs.getInt("placecount")+"\n" +
                                "Block break: "+rs.getInt("breakcount")+"\n" +
                                "Kill units: "+rs.getInt("killcount")+"\n" +
                                "Death count: "+rs.getInt("deathcount")+"\n" +
                                "Join count: "+rs.getInt("joincount")+"\n" +
                                "Kick count: "+rs.getInt("kickcount")+"\n" +
                                "Level: "+rs.getInt("level")+"\n" +
                                "XP: "+rs.getString("reqtotalexp")+"\n" +
                                "First join: "+rs.getString("firstdate")+"\n" +
                                "Last join: "+rs.getString("lastdate")+"\n" +
                                "Playtime: "+rs.getString("playtime")+"\n" +
                                "Attack clear: "+rs.getInt("attackclear")+"\n" +
                                "PvP Win: "+rs.getInt("pvpwincount")+"\n" +
                                "PvP Lose: "+rs.getInt("pvplosecount")+"\n" +
                                "PvP Surrender: "+rs.getInt("pvpbreakout")+"\n" +
                                "Permission: "+rs.getString("permission");
                        nlog("log",datatext);
                    }
                    rs.close();
                    stmt.close();
                    nlog("log","Data line end.");
                }catch (Exception e){
                    printStackTrace(e);
                }
            });
            executorService.execute(t);
        });
        handler.register("bansync", "Ban list synchronization from main server.", (arg) -> {
            if(!config.isServerenable()){
                if(config.isBanshare()){
                    Client client = new Client();
                    client.main("bansync", null, null);
                } else {
                    log("warn","banshare-disabled");
                }
            } else {
                log("warn","banshare-server");
            }
        });
        handler.register("blacklist", "<add/remove> <nickname>", "Block special nickname.", arg -> {
            if(arg.length < 1) {
                log("warn","no-parameter");
                return;
            }
            if (arg[0].equals("add")) {
                blacklist.add(arg[1]);
                log("log", "blacklist-add", arg[1]);
            } else if (arg[0].equals("remove")) {
                for (int i = 0; i < blacklist.size(); i++) {
                    if (blacklist.get(i).equals(arg[1])) {
                        blacklist.remove(i);
                        log("log", "blacklist-remove", arg[1]);
                        return;
                    }
                }
            } else {
                log("warn", "blacklist-invalid");
            }
        });
        handler.register("reset", "<zone/count/total>", "Clear a server-to-server jumping zone data.", arg -> {
            if(arg.length == 0) {
                log("warn","no-parameter");
                return;
            }
            switch(arg[0]){
                case "zone":
                    for (String jumpdata : jumpzone) {
                        String[] data = jumpdata.split("/");
                        int startx = Integer.parseInt(data[0]);
                        int starty = Integer.parseInt(data[1]);
                        int tilex = Integer.parseInt(data[2]);
                        int block = Integer.parseInt(data[6]);

                        Block target;
                        switch (block) {
                            case 1:
                            default:
                                target = Blocks.metalFloor;
                                break;
                            case 2:
                                target = Blocks.metalFloor2;
                                break;
                            case 3:
                                target = Blocks.metalFloor3;
                                break;
                            case 4:
                                target = Blocks.metalFloor5;
                                break;
                            case 5:
                                target = Blocks.metalFloorDamaged;
                                break;
                        }

                        int size = tilex - startx;
                        for (int x = 0; x < size; x++) {
                            for (int y = 0; y < size; y++) {
                                Tile tile = world.tile(startx + x, starty + y);
                                Call.onDeconstructFinish(tile, target, 0);
                            }
                        }
                    }
                    jumpzone.clear();
                    log("log","jump-reset", "zone");
                    break;
                case "count":
                    jumpcount.clear();
                    log("log","jump-reset", "count");
                    break;
                case "total":
                    jumpall.clear();
                    log("log","jump-reset", "total");
                    break;
                default:
                    log("warn","Invalid option!");
                    break;
            }
        });
        handler.register("reload", "Reload Essentials config", arg -> {
            config = new Config();
            log("config","config-reloaded");
        });
        handler.register("reconnect", "Reconnect remote server (Essentials server only!)", arg -> {
            if(config.isClientenable()){
                log("client","server-connecting");
                Client client = new Client();
                if(serverconn){
                    client.main("exit", null, null);
                } else {
                    client.main(null, null, null);
                }
            } else {
                log("client","client-disabled");
            }

            log("client","db-connecting");
            closeconnect();
            PlayerDB db = new PlayerDB();
            db.openconnect();
        });
        handler.register("unadminall", "<default_group_name>", "Remove all player admin status", arg -> {
            for (String b : permission.keySet()) {
                if (b.equals(arg[0])) {
                    writeData("UPDATE players SET permission = ?", arg[0]);
                    log("log", "success");
                    return;
                }
            }
            log("warn","perm-group-not-found");
        });
        handler.register("kickall", "Kick all players.",  arg -> {
            for(int a=0;a<playerGroup.size();a++){
                Player others = playerGroup.all().get(a);
                Call.onKick(others.con, "All kick players by administrator.");
            }
            nlog("log","It's done.");
        });
        handler.register("kill", "<username>", "Kill target player.", arg -> {
            if(arg.length == 0) {
                log("warn","no-parameter");
                return;
            }
            Player other = playerGroup.find(p -> p.name.equalsIgnoreCase(arg[0]));
            if(other != null){
                other.kill();
            } else {
                log("warn","player-not-found");
            }
        });
        handler.register("nick", "<name> <newname...>", "Show player information.", (arg) -> {
            if(arg.length < 1) {
                log("warn","no-parameter");
                return;
            }
            try{
                writeData("UPDATE players SET name = ? WHERE name = ?",arg[1],arg[0]);
                log("log","player-nickname-change-to", arg[0], arg[1]);
            }catch (Exception e){
                printStackTrace(e);
                log("warn","player-not-found");
            }
        });
        handler.register("pvp", "<anticoal/timer> [time...]", "Set gamerule with PvP mode.", arg -> {
            /*
            if(Vars.state.rules.pvp){
                switch(arg[0]){
                    case "anticoal":
                        break;
                    case "timer":
                        break;
                    default:
                        break;
                }
            }
            */
            nlog("log","Currently not supported!");
        });
        handler.register("setperm", "<player_name> <group>", "Set player permission group", arg -> {
            if(playerGroup.find(p -> p.name.equals(arg[0])) == null){
                log("warn","player-not-found");
            }
            for (String b : permission.keySet()) {
                if (b.equals(arg[1])) {
                    writeData("UPDATE players SET permission = ? WHERE name = ?", arg[1], arg[0]);
                    log("player", "success");
                    return;
                }
            }
            log("playererror","perm-group-not-found");
        });
        handler.register("sync", "<player>", "Force sync request from the target player.", arg -> {
            Player other = playerGroup.find(p -> p.name.equalsIgnoreCase(arg[0]));
            if(other != null){
                Call.onWorldDataBegin(other.con);
                netServer.sendWorldData(other);
            } else {
                log("warn","player-not-found");
            }
        });
        handler.register("team","[name]", "Change target player team.", (arg) -> {
            Player other = playerGroup.find(p -> p.name.equalsIgnoreCase(arg[0]));
            if(other != null){
                int i = other.getTeam().id+1;
                while(i != other.getTeam().id){
                    if (i >= Team.all().length) i = 0;
                    if(!state.teams.get(Team.all()[i]).cores.isEmpty()){
                        other.setTeam(Team.all()[i]);
                        break;
                    }
                    i++;
                }
                other.kill();
            } else {
                log("warn","player-not-found");
            }
        });
        handler.register("tempban", "<type-id/name/ip> <username/IP/ID> <time...>", "Temporarily ban player. time unit: 1 hours.", arg -> {
            int bantimeset = Integer.parseInt(arg[1]);
            Player other = playerGroup.find(p -> p.name.equalsIgnoreCase(arg[0]));
            if(other == null){
                log("warn","player-not-found");
                return;
            }
            addtimeban(other.name, other.uuid, bantimeset);
            switch (arg[0]) {
                case "id":
                    netServer.admins.banPlayerID(arg[1]);
                    break;
                case "name":
                    Player target = playerGroup.find(p -> p.name.equalsIgnoreCase(arg[0]));
                    if (target != null) {
                        netServer.admins.banPlayer(target.uuid);
                        log("log","tempban", other.name, arg[1]);
                    } else {
                        log("warn","player-not-found");
                    }
                    break;
                case "ip":
                    netServer.admins.banPlayerIP(arg[1]);
                    log("log","tempban", other.name, arg[1]);
                    break;
                default:
                    nlog("log","Invalid type.");
                    break;
            }
            other.con.kick("Temp kicked");
            if(config.isClientenable()){
                Client client = new Client();
                client.main("bansync", null, null);
            }
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.removeCommand("vote");
        handler.removeCommand("votekick");

        handler.<Player>register("ch", "Send chat to another server.", (arg, player) -> {
            if(!checkperm(player,"ch")) return;

            JsonObject db = getData(player.uuid);
            boolean value = db.getBoolean("crosschat");
            int set;
            if (!value) {
                set = 1;
                player.sendMessage(bundle(player, "crosschat"));
            } else {
                set = 0;
                player.sendMessage(bundle(player, "crosschat-disable"));
            }

            writeData("UPDATE players SET crosschat = ? WHERE uuid = ?",set,player.uuid);
        });
        handler.<Player>register("changepw", "<new_password>", "Change account password", (arg, player) -> {
            if(!checkperm(player,"changepw")) return;
            if(checkpw(player, arg[0], arg[1])){
                player.sendMessage(bundle(player, "need-new-password"));
                return;
            }
            try{
                Class.forName("org.mindrot.jbcrypt.BCrypt");
                String hashed = BCrypt.hashpw(arg[0], BCrypt.gensalt(11));
                writeData("UPDATE players SET accountpw = ? WHERE uuid = ?",hashed,player.uuid);
                player.sendMessage(bundle(player,"success"));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        });
        handler.<Player>register("color", "Enable color nickname", (arg, player) -> {
            if (!checkperm(player, "color")) return;
            JsonObject db = getData(player.uuid);
            boolean value = db.getBoolean("colornick");
            int set;
            if (!value) {
                set = 1;
                player.sendMessage(bundle(player, "colornick"));
            } else {
                set = 0;
                player.sendMessage(bundle(player, "colornick-disable"));
            }
            writeData("UPDATE players SET colornick = ? WHERE uuid = ?",set,player.uuid);
        });
        handler.<Player>register("difficulty", "<difficulty>", "Set server difficulty", (arg, player) -> {
            if (!checkperm(player, "difficulty")) return;
            try {
                Difficulty.valueOf(arg[0]);
                player.sendMessage(bundle(player, "difficulty-set", arg[0]));
            } catch (IllegalArgumentException e) {
                player.sendMessage(bundle(player, "difficulty-not-found", arg[0]));
            }
        });
        handler.<Player>register("despawn","Kill all enemy units", (arg, player) -> {
            if (!checkperm(player, "despawn")) return;
            for(int a=0;a<Team.all().length;a++){
                unitGroup.all().each(Unit::kill);
            }
        });
        handler.<Player>register("event", "<host/join> <roomname> [map] [gamemode]", "Host your own server", (arg, player) -> {
            if(!checkperm(player,"event")) return;
            Thread t = new Thread(() -> {
                String currentip = new getip().main();
                switch (arg[0]) {
                    case "host":
                        Thread work = new Thread(() -> {
                            JsonObject db = getData(player.uuid);
                            if (db.getInt("level") > 20 || player.isAdmin) {
                                if (arg.length == 2) {
                                    player.sendMessage(bundle(player, "event-host-no-mapname"));
                                    return;
                                }
                                if (arg.length == 3) {
                                    player.sendMessage(bundle(player, "event-host-no-gamemode"));
                                    return;
                                }
                                player.sendMessage(bundle(player, "event-making"));

                                String[] range = config.getEventport().split("-");
                                int firstport = Integer.parseInt(range[0]);
                                int lastport = Integer.parseInt(range[1]);
                                int customport = ThreadLocalRandom.current().nextInt(firstport,lastport+1);

                                eventservers.add(arg[1]+"/"+customport);

                                Threads.eventserver es = new Threads.eventserver(arg[1],arg[2],arg[3],customport);
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
                                    e.printStackTrace();
                                }
                                log("log","event-host-opened", player.name, customport);

                                writeData("UPDATE players SET connected = ?, connserver = ? WHERE uuid = ?",false, "none", player.uuid);
                                if(db.getString("country").equals("Local IP")){
                                    Call.onConnect(player.con,"127.0.0.1",customport);
                                } else {
                                    Call.onConnect(player.con, currentip, customport);
                                }
                                nlog("log",currentip+":"+customport);
                            } else {
                                player.sendMessage(bundle(player, "event-level"));
                            }
                        });
                        work.start();
                        break;
                    case "join":
                        for (String eventserver : eventservers) {
                            String[] data = eventserver.split("/");
                            String name = data[0];
                            if (name.equals(arg[1])) {
                                writeData("UPDATE players SET connected = ?, connserver = ? WHERE uuid = ?", false, "none", player.uuid);
                                Call.onConnect(player.con, currentip, Integer.parseInt(data[1]));
                                nlog("log",currentip+":"+Integer.parseInt(data[1]));
                                break;
                            }
                        }
                        break;
                    default:
                        player.sendMessage(bundle(player, "wrong-command"));
                        break;
                }
            });
            t.start();
        });
        handler.<Player>register("getpos", "Get your current position info", (arg, player) -> {
            if(!checkperm(player,"getpos")) return;
            player.sendMessage("X: " + Math.round(player.x) + " Y: " + Math.round(player.y));
        });
        handler.<Player>register("help", "[page]", "Show command lists", (arg, player) -> {
            if(arg.length > 0 && !Strings.canParseInt(arg[0])){
                player.sendMessage(bundle(player, "page-number"));
                return;
            }

            ArrayList<String> temp = new ArrayList<>();
            for(int a=0;a<netServer.clientCommands.getCommandList().size;a++){
                CommandHandler.Command command = netServer.clientCommands.getCommandList().get(a);
                if(checkperm(player,command.text) || command.text.equals("t") || command.text.equals("sync")){
                    temp.add("[orange] /"+command.text+" [white]"+command.paramText+" [lightgray]- "+command.description+"\n");
                }
            }

            List<String> deduped = temp.stream().distinct().collect(Collectors.toList());

            StringBuilder result = new StringBuilder();
            int perpage = 8;
            int page = arg.length > 0 ? Strings.parseInt(arg[0]) : 1;
            int pages = Mathf.ceil((float)deduped.size() / perpage);

            page --;

            if(page > pages || page < 0){
                player.sendMessage("[scarlet]'page' must be a number between[orange] 1[] and[orange] " + pages + "[scarlet].");
                return;
            }

            result.append(Strings.format("[orange]-- Commands Page[lightgray] {0}[gray]/[lightgray]{1}[orange] --\n", (page+1), pages));
            for(int a=perpage*page;a<Math.min(perpage*(page+1), deduped.size());a++){
                result.append(deduped.get(a));
            }
            player.sendMessage(result.toString().substring(0, result.length()-1));
        });
        handler.<Player>register("info", "Show your information", (arg, player) -> {
            if(!checkperm(player,"info")) return;
            Thread t = new Thread(() -> {
                String ip = Vars.netServer.admins.getInfo(player.uuid).lastIP;
                JsonObject db = getData(player.uuid);
                String datatext = "[#DEA82A]" + nbundle(player, "player-info") + "[]\n" +
                        "[#2B60DE]========================================[]\n" +
                        "[green]" + nbundle(player, "player-name") + "[] : " + player.name + "[white]\n" +
                        "[green]" + nbundle(player, "player-uuid") + "[] : " + player.uuid + "\n" +
                        "[green]" + nbundle(player, "player-isMobile") + "[] : " + player.isMobile + "\n" +
                        "[green]" + nbundle(player, "player-ip") + "[] : " + ip + "\n" +
                        "[green]" + nbundle(player, "player-country") + "[] : " + db.get("country") + "\n" +
                        "[green]" + nbundle(player, "player-placecount") + "[] : " + db.get("placecount") + "\n" +
                        "[green]" + nbundle(player, "player-breakcount") + "[] : " + db.get("breakcount") + "\n" +
                        "[green]" + nbundle(player, "player-killcount") + "[] : " + db.get("killcount") + "\n" +
                        "[green]" + nbundle(player, "player-deathcount") + "[] : " + db.get("deathcount") + "\n" +
                        "[green]" + nbundle(player, "player-joincount") + "[] : " + db.get("joincount") + "\n" +
                        "[green]" + nbundle(player, "player-kickcount") + "[] : " + db.get("kickcount") + "\n" +
                        "[green]" + nbundle(player, "player-level") + "[] : " + db.get("level") + "\n" +
                        "[green]" + nbundle(player, "player-reqtotalexp") + "[] : " + db.get("reqtotalexp") + "\n" +
                        "[green]" + nbundle(player, "player-firstdate") + "[] : " + db.get("firstdate") + "\n" +
                        "[green]" + nbundle(player, "player-lastdate") + "[] : " + db.get("lastdate") + "\n" +
                        "[green]" + nbundle(player, "player-playtime") + "[] : " + db.get("playtime") + "\n" +
                        "[green]" + nbundle(player, "player-attackclear") + "[] : " + db.get("attackclear") + "\n" +
                        "[green]" + nbundle(player, "player-pvpwincount") + "[] : " + db.get("pvpwincount") + "\n" +
                        "[green]" + nbundle(player, "player-pvplosecount") + "[] : " + db.get("pvplosecount") + "\n" +
                        "[green]" + nbundle(player, "player-pvpbreakout") + "[] : " + db.get("pvpbreakout");
                Call.onInfoMessage(player.con, datatext);
            });
            executorService.submit(t);
        });
        handler.<Player>register("jump", "<zone/count/total> [serverip] [port] [range] [block-type(1~6)]", "Create a server-to-server jumping zone.", (arg, player) -> {
            if (!checkperm(player, "jump")) return;
            switch (arg[0]){
                case "zone":
                    if(arg.length != 5){
                        player.sendMessage(bundle(player, "jump-incorrect"));
                        return;
                    }
                    int size;
                    try {
                        size = Integer.parseInt(arg[3]);
                    } catch (Exception ignored) {
                        player.sendMessage(bundle(player, "jump-not-int"));
                        return;
                    }
                    int block;
                    try {
                        block = Integer.parseInt(arg[4]);
                    } catch (Exception ignored) {
                        player.sendMessage(bundle(player, "jump-not-block"));
                        return;
                    }
                    Block target;
                    switch (block) {
                        case 1:
                        default:
                            target = Blocks.metalFloor;
                            break;
                        case 2:
                            target = Blocks.metalFloor2;
                            break;
                        case 3:
                            target = Blocks.metalFloor3;
                            break;
                        case 4:
                            target = Blocks.metalFloor5;
                            break;
                        case 5:
                            target = Blocks.metalFloorDamaged;
                            break;
                        case 6:
                            target = Blocks.air;
                            break;
                    }
                    int xt = player.tileX();
                    int yt = player.tileY();
                    int tilexfinal = xt + size;
                    int tileyfinal = yt + size;

                    for (int x = 0; x < size; x++) {
                        for (int y = 0; y < size; y++) {
                            Tile tile = world.tile(xt + x, yt + y);
                            Call.onConstructFinish(tile, target, 0, (byte) 0, Team.sharded, false);
                        }
                    }

                    jumpzone.add(xt + "/" + yt + "/" + tilexfinal + "/" + tileyfinal + "/" + arg[1] + "/" + arg[2] + "/" + block);
                    player.sendMessage(bundle(player, "jump-added"));
                    break;
                case "count":
                    jumpcount.add(arg[1] + "/" + arg[2] + "/" + player.tileX() + "/" + player.tileY() + "/0/0");
                    player.sendMessage(bundle(player, "jump-added"));
                    break;
                case "total":
                    jumpall.add(player.tileX() + "/" + player.tileY() + "/0/0");
                    player.sendMessage(bundle(player, "jump-added"));
                    break;
                default:
                    player.sendMessage(bundle(player, "command-invalid"));
            }
        });
        handler.<Player>register("kickall", "Kick all players", (arg, player) -> {
            if (!checkperm(player, "kickall")) return;
            Vars.netServer.kickAll(Packets.KickReason.kick);
        });
        handler.<Player>register("kill", "<player>", "Kill player.", (arg, player) -> {
            if (!checkperm(player, "kill")) return;
            Player other = Vars.playerGroup.find(p -> p.name.equalsIgnoreCase(arg[0]));
            if (other == null) {
                player.sendMessage(bundle(player, "player-not-found"));
                return;
            }
            Player.onPlayerDeath(other);
        });
        handler.<Player>register("login", "<id> <password>", "Access your account", (arg, player) -> {
            if (config.isLoginenable()) {
                if(!isLogin(player)) {
                    if (PlayerDB.login(player, arg[0], arg[1])) {
                        PlayerDB playerdb = new PlayerDB();
                        playerdb.load(player, arg[0]);
                        if (!isLogin(player)) {
                            player.sendMessage("[green][EssentialPlayer][] Login successful!/로그인 성공!");
                        } else {
                            player.sendMessage(bundle(player, "login-success"));
                        }
                    } else {
                        player.sendMessage("[green][EssentialPlayer] [scarlet]Login failed/로그인 실패!!");
                    }
                } else {
                    player.sendMessage("[green][EssentialPlayer] [scarlet]You're already logged./이미 로그인한 상태입니다.");
                }
            } else {
                player.sendMessage(bundle(player, "login-not-use"));
            }
        });
        handler.<Player>register("logout","Log-out of your account.", (arg, player) -> {
            if(!checkperm(player,"logout")) return;
            if(config.isLoginenable()) {
                writeData("UPDATE players SET connected = ?, uuid = ? WHERE uuid = ?", false, "LogoutAAAAA=", player.uuid);
                Call.onKick(player.con, nbundle("logout"));
            } else {
                player.sendMessage(bundle(player, "login-not-use"));
            }
        });
        handler.<Player>register("maps", "[page]", "Show server maps", (arg, player) -> {
            if(!checkperm(player,"maps")) return;
            StringBuilder build = new StringBuilder();
            int page = arg.length > 0 ? Strings.parseInt(arg[0]) : 1;
            int pages = Mathf.ceil((float)maplist.size / 6);

            page --;
            if(page > pages || page < 0){
                player.sendMessage("[scarlet]'page' must be a number between[orange] 1[] and[orange] " + pages + "[scarlet].");
                return;
            }

            build.append("[green]==[white] Server maps page ").append(page).append("/").append(pages).append(" [green]==[white]\n");
            for(int a=6*page;a<Math.min(6 * (page + 1), maplist.size);a++){
                build.append("[gray]").append(a).append("[] ").append(maplist.get(a).name()).append("\n");
            }
            player.sendMessage(build.toString());
        });
        handler.<Player>register("me", "<text...>", "broadcast * message", (arg, player) -> {
            if(!checkperm(player,"me")) return;
            Call.sendMessage("[orange]*[] " + player.name + "[white] : " + arg[0]);
        });
        handler.<Player>register("motd", "Show server motd.", (arg, player) -> {
            if(!checkperm(player,"motd")) return;
            String motd = getmotd(player);
            int count = motd.split("\r\n|\r|\n").length;
            if (count > 10) {
                Call.onInfoMessage(player.con, motd);
            } else {
                player.sendMessage(motd);
            }
        });
        handler.<Player>register("save", "Auto rollback map early save", (arg, player) -> {
            if (!checkperm(player, "save")) return;
            Core.app.post(() -> {
                Fi file = saveDirectory.child(config.getSlotnumber() + "." + saveExtension);
                SaveIO.save(file);
                player.sendMessage(bundle(player, "mapsaved"));
            });
        });
        switch (config.getPasswordmethod()) {
            /*case "email":
                handler.<Player>register("register", "<accountid> <password> <email>", "Register account", (arg, player) -> {
                    if (config.isLoginenable()) {
                        PlayerDB playerdb = new PlayerDB();
                        if (playerdb.register(player, arg[0], arg[1], "email", arg[2])) {
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
                break;
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
                        PlayerDB playerdb = new PlayerDB();
                        if (playerdb.register(player, arg[0], arg[1])) {
                            if (Vars.state.rules.pvp) {
                                int index = player.getTeam().id + 1;
                                while (index != player.getTeam().id) {
                                    if (index >= Team.all().length) {
                                        index = 0;
                                    }
                                    if (!Vars.state.teams.get(Team.all()[index]).cores.isEmpty()) {
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
                        player.sendMessage(bundle(player, "login-not-use"));
                    }
                });
                break;
            case "discord":
                handler.<Player>register("register", "Register account", (arg, player) -> player.sendMessage("Join discord and use !signup command!\n" + config.getDiscordLink()));
        }
        handler.<Player>register("spawn", "<mob_name> <count> [team] [playername]", "Spawn mob in player position", (arg, player) -> {
            if (!checkperm(player, "spawn")) return;

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
                    player.sendMessage(bundle(player, "mob-not-found"));
                    return;
            }
            int count;
            try {
                count = Integer.parseInt(arg[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(bundle(player, "mob-spawn-not-number"));
                return;
            }
            if(config.getSpawnlimit() == count){
                player.sendMessage(bundle(player, "spawn-limit"));
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
                        player.sendMessage(bundle(player, "team-not-found"));
                        return;
                }
            }
            Player targetplayer = null;
            if (arg.length >= 4) {
                Player target = playerGroup.find(p -> p.name.equals(arg[3]));
                if (target == null) {
                    player.sendMessage(bundle(player, "player-not-found"));
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
            if(!checkperm(player,"setperm")) return;
            if(playerGroup.find(p -> p.name.equals(arg[0])) == null){
                player.sendMessage(bundle(player, "player-not-found"));
            }
            for (String b : permission.keySet()) {
                if (b.equals(arg[1])) {
                    writeData("UPDATE players SET permission = ? WHERE name = ?", arg[0]);
                    player.sendMessage(bundle(player, "success"));
                    return;
                }
            }
            player.sendMessage(bundle(player, "perm-group-not-found"));
        });
        handler.<Player>register("status", "Show server status", (arg, player) -> {
            if(!checkperm(player,"status")) return;
            player.sendMessage(nbundle(player, "server-status"));
            player.sendMessage("[#2B60DE]========================================[]");
            float fps = Math.round((int) 60f / Time.delta());
            player.sendMessage(nbundle(player, "server-status-online", fps, Vars.playerGroup.size()));
            int idb = 0;
            int ipb = 0;

            Array<PlayerInfo> bans = Vars.netServer.admins.getBanned();
            for (PlayerInfo ignored : bans) {
                idb++;
            }

            Array<String> ipbans = Vars.netServer.admins.getBannedIPs();
            for (String ignored : ipbans) {
                ipb++;
            }
            int bancount = idb + ipb;
            player.sendMessage(nbundle(player, "server-status-banstat", bancount, idb, ipb, Threads.playtime, Threads.uptime));
        });
        handler.<Player>register("suicide", "Kill yourself.", (arg, player) -> {
            if(!checkperm(player,"suicide")) return;
            Player.onPlayerDeath(player);
            if (playerGroup != null && playerGroup.size() > 0) {
                allsendMessage("suicide", player.name);
            }
        });
        handler.<Player>register("team", "[Team...]", "Change team (PvP only)", (arg, player) -> {
            if(!checkperm(player,"team")) return;
            if (Vars.state.rules.pvp) {
                int i = player.getTeam().id + 1;
                while (i != player.getTeam().id) {
                    if (i >= Team.all().length) i = 0;
                    if (!Vars.state.teams.get(Team.all()[i]).cores.isEmpty()) {
                        player.setTeam(Team.all()[i]);
                        break;
                    }
                    i++;
                }
                Call.onPlayerDeath(player);
            } else {
                player.sendMessage(bundle(player, "command-only-pvp"));
            }
        });
        handler.<Player>register("tempban", "<player> <time>", "Temporarily ban player. time unit: 1 hours", (arg, player) -> {
            if (!checkperm(player, "tempban")) return;
            Player other = null;
            for (Player p : playerGroup.all()) {
                boolean result = p.name.contains(arg[0]);
                if (result) {
                    other = p;
                }
            }
            if (other != null) {
                int bantimeset = Integer.parseInt(arg[1]);
                PlayerDB.addtimeban(other.name, other.uuid, bantimeset);
                other.con.kick("Temp kicked");
                for (int a = 0; a < playerGroup.size(); a++) {
                    Player current = playerGroup.all().get(a);
                    current.sendMessage(bundle(current, "ban-temp", other.name, player.name));
                }
            } else {
                player.sendMessage(bundle(player, "player-not-found"));
            }
        });
        handler.<Player>register("time", "Show server time", (arg, player) -> {
            if(!checkperm(player,"time")) return;
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd a hh:mm.ss");
            String nowString = now.format(dateTimeFormatter);
            player.sendMessage(bundle(player, "servertime", nowString));
        });
        handler.<Player>register("tp", "<player>", "Teleport to other players", (arg, player) -> {
            if(!checkperm(player,"tp")) return;
            if(player.isMobile){
                player.sendMessage(bundle(player, "tp-not-support"));
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
                player.sendMessage(bundle(player, "player-not-found"));
                return;
            }
            player.setNet(other.x, other.y);
        });
        handler.<Player>register("tpp", "<player> <player>", "Teleport to other players", (arg, player) -> {
            if (!checkperm(player, "tpp")) return;
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
                player.sendMessage(bundle(player, "player-not-found"));
                return;
            }
            if (!other1.isMobile || !other2.isMobile) {
                other1.setNet(other2.x, other2.y);
            } else {
                player.sendMessage(bundle(player, "tp-ismobile"));
            }
        });
        handler.<Player>register("tppos", "<x> <y>", "Teleport to coordinates", (arg, player) -> {
            if(!checkperm(player,"tppos")) return;
            int x;
            int y;
            try{
                x = Integer.parseInt(arg[0]);
                y = Integer.parseInt(arg[1]);
            }catch (Exception ignored){
                player.sendMessage(bundle(player, "tp-not-int"));
                return;
            }
            player.setNet(x, y);
        });
        handler.<Player>register("tr", "Enable/disable Translate all chat", (arg, player) -> {
            if(!checkperm(player,"tr")) return;
            JsonObject db = getData(player.uuid);
            boolean value = db.getBoolean("translate");
            int set;
            if (!value) {
                set = 1;
                player.sendMessage(bundle(player, "translate"));
            } else {
                set = 0;
                player.sendMessage(bundle(player, "translate-disable"));
            }

            writeData("UPDATE players SET translate = ? WHERE uuid = ?", set, player.uuid);
        });
        handler.<Player>register("vote", "<gameover/skipwave/kick/rollback/map> [mapid/mapname/playername...]", "Vote surrender or skip wave, Long-time kick", (arg, player) -> {
            if(!checkperm(player,"vote")) return;
            if(arg.length == 2) {
                if(arg[0].equals("kick")) {
                    Player other = Vars.playerGroup.find(p -> p.name.equalsIgnoreCase(arg[1]));
                    if (other == null) {
                        player.sendMessage(bundle(player, "player-not-found"));
                        return;
                    }
                    if (other.isAdmin){
                        player.sendMessage(bundle(player, "vote-target-admin"));
                        return;
                    }
                    // 강퇴 투표
                    Vote vote = new Vote(player, arg[0], other);
                    vote.command();
                } else if(arg[0].equals("map")){
                    // 맵 투표
                    mindustry.maps.Map world = maps.all().find(map -> map.name().equalsIgnoreCase(arg[1].replace('_', ' ')) || map.name().equalsIgnoreCase(arg[1]));
                    if (world == null){
                        world = maplist.get(Integer.parseInt(arg[1]));
                    }
                    if (world == null) {
                        player.sendMessage(bundle(player, "vote-map-not-found"));
                    } else {
                        Vote vote = new Vote(player, arg[0], world);
                        vote.command();
                    }
                }
            } else {
                if(arg[0].equals("map") || arg[0].equals("kick")){
                    player.sendMessage(bundle(player, "vote-map-not-found"));
                    return;
                }
                // 게임 오버, wave 넘어가기, 롤백
                Vote vote = new Vote(player, arg[0]);
                vote.command();
            }
        });
        handler.<Player>register("votekick", "<player_name>", "Player kick starts voting.", (arg, player) -> {
            if(!checkperm(player,"votekick")) return;
            Player other = Vars.playerGroup.find(p -> p.name.equalsIgnoreCase(arg[1]));
            if (other == null) {
                player.sendMessage(bundle(player, "player-not-found"));
                return;
            }
            if (other.isAdmin){
                player.sendMessage(bundle(player, "vote-target-admin"));
                return;
            }

            Vote vote = new Vote(player, arg[0], other);
            vote.command();
        });

        /*
        handler.<Player>register("special", "Check that the plug-in is working properly.", (arg, player) -> {
            Thread t = new Thread(() -> {
                Tile start = world.tile(player.tileX(), player.tileY());
                try {
                    sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Tile target = world.tile(player.tileX(), player.tileY());
                //new AI(start, target).target();
                new AI(start, target).findore();
            });
            t.start();

            Thread t = new Thread(() -> {
                int count = 0;
                while(true){
                    Call.createBullet(Bullets.flakExplosive,player.getTeam(),player.x,player.y,Mathf.random(360),(float) (Math.random() * (1.0 - 0.5) + 0.5),(float) (Math.random() * (1.0 - 0.2) + 0.2));
                    count++;
                    if(count == 500){
                        break;
                    } else {
                        try {
                            sleep(17);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                count = 0;
                while(true) {
                    Call.createBullet(Bullets.artilleryHoming, player.getTeam(), player.x, player.y, Mathf.random(360), (float) (Math.random() * (1.0 - 0.5) + 0.5), (float) (Math.random() * (1.0 - 0.2) + 0.2));
                    count++;
                    if (count == 500) {
                        break;
                    } else {
                        try {
                            sleep(17);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                count = 0;
                while(true) {
                    Call.createBullet(Bullets.missileSwarm, player.getTeam(), player.x, player.y,new Random().nextInt((int) (((player.rotation+20) - (player.rotation-20)) + 1)) + player.rotation-20,1, 1);
                    count++;
                    if (count == 500) {
                        break;
                    } else {
                        try {
                            sleep(17);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            t.start();
        });
        */
    }
}