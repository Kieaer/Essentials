package essentials;

import essentials.Threads.login;
import essentials.Threads.*;
import essentials.core.EPG;
import essentials.core.Log;
import essentials.core.PlayerDB;
import essentials.core.Translate;
import essentials.net.Client;
import essentials.net.Server;
import essentials.special.IpAddressMatcher;
import essentials.utils.Config;
import io.anuke.arc.ApplicationListener;
import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.collection.Array;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.util.CommandHandler;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.content.UnitTypes;
import io.anuke.mindustry.entities.type.BaseUnit;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.game.Difficulty;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.mindustry.net.Administration.PlayerInfo;
import io.anuke.mindustry.net.Packets.KickReason;
import io.anuke.mindustry.net.ValidateException;
import io.anuke.mindustry.plugin.Plugin;
import io.anuke.mindustry.type.UnitType;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.power.NuclearReactor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.mindrot.jbcrypt.BCrypt;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;

import static essentials.Global.*;
import static essentials.Threads.*;
import static essentials.core.Log.writelog;
import static essentials.core.PlayerDB.*;
import static essentials.net.Client.serverconn;
import static essentials.utils.Config.jumpall;
import static essentials.utils.Config.jumpzone;
import static essentials.utils.Config.*;
import static io.anuke.arc.util.Log.err;
import static io.anuke.mindustry.Vars.*;

public class Main extends Plugin {
    public Config config = new Config();
    static ArrayList<String> powerblock = new ArrayList<>();
    private JSONArray nukeblock = new JSONArray();
    static ArrayList<Tile> messagemonitor = new ArrayList<>();
    private ArrayList<Tile> nukedata = new ArrayList<>();
    private boolean making = false;

    public Main() {
        //NetClient.visual = false;

        // 설정 시작
        config.main();

        // 클라이언트 연결 확인
        if (config.isClientenable()) {
            Global.logc(nbundle("server-connecting"));
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
                    writeData("UPDATE players SET connected = 0, connserver = 'none' WHERE id='"+rs.getInt("id")+"'");
                }
            }
        } catch (SQLException e) {
            printStackTrace(e);
        }

        // 플레이어 DB 업그레이드
        PlayerDB.Upgrade();

        // 클라이언트 플레이어 카운트 (중복 실행을 방지하기 위해 별도 스레드로 실행)
        Thread jumpcheck = new Thread(new jumpcheck());
        jumpcheck.start();

        // 코어 자원소모 감시 시작
        Thread monitorresource = new Thread(new monitorresource());
        if(config.isScanresource()) {
            monitorresource.start();
        }

        // 임시로 밴한 플레이어들 밴 해제시간 카운트
        Thread bantime = new Thread(new bantime());

        // 기록 시작
        if (config.isLogging()) {
            Log log = new Log();
            log.main();
        }

        //EssentialAI.main();

        // 업데이트 확인
        if (config.isUpdate()) {
            Client client = new Client();
            client.update();
        }

        // 서버기능 시작
        if (config.isServerenable()) {
            new essentials.net.Server();
        }

        // Essentials EPG 기능 시작
        EPG epg = new EPG();
        epg.main();

        Events.on(TapConfigEvent.class, e -> {
            if (e.tile.entity != null && e.tile.entity.block != null && e.tile.entity() != null && e.player != null && e.player.name != null && config.isBlockdetect()) {
                allsendMessage("tap-config", e.player.name, e.tile.entity.block.name);
            }
        });

        Events.on(TapEvent.class, e -> {
            if(isLogin(e.player)) {
                Thread t = new Thread(() -> {
                    for (int i = 0; i < jumpzone.length(); i++) {
                        String jumpdata = jumpzone.getString(i);
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
                                Global.log(nbundle("player-jumped", e.player.name, serverip + ":" + serverport));
                                writeData("UPDATE players SET connected = '0', connserver = 'none' WHERE uuid = '" + e.player.uuid + "'");
                                Call.onConnect(e.player.con, serverip, serverport);
                            }
                        }
                    }
                });
                t.start();
            }
        });

        /*
        Events.on(WithdrawEvent.class, e->{
            Global.log("WithdrawEvent");
            Global.log("player: " + e.player.name + ", tile: " + e.tile.entity.block.name);
            Global.log("item: "+e.item.name+", amount: "+e.amount);
        });
         */

        // 만약 동기화에 실패했을 경우 (아마도 될꺼임)
        Events.on(ValidateException.class, e -> {
            Call.onInfoMessage(e.player.con, "You're desynced! The server will send data again.");
            Call.onWorldDataBegin(e.player.con);
            netServer.sendWorldData(e.player);
        });

        // 게임오버가 되었을 때
        Events.on(GameOverEvent.class, e -> {
            if (Vars.state.rules.pvp) {
                int index = 5;
                for(int a=0;a<5;a++) {
                    if (Vars.state.teams.get(Team.all[index]).cores.isEmpty()) {
                        index--;
                    }
                }
                if(index == 1) {
                    for (int i = 0; i < playerGroup.size(); i++) {
                        Player player = playerGroup.all().get(i);
                        if (isLogin(player)) {
                            if (player.getTeam().name().equals(e.winner.name())) {
                                JSONObject db = getData(player.uuid);
                                int pvpwin = db.getInt("pvpwincount");
                                pvpwin++;
                                writeData("UPDATE players SET pvpwincount = '" + pvpwin + "' WHERE uuid = '" + player.uuid + "'");
                            } else if (!player.getTeam().name().equals(e.winner.name())) {
                                JSONObject db = getData(player.uuid);
                                int pvplose = db.getInt("pvplosecount");
                                pvplose++;
                                writeData("UPDATE players SET pvplosecount = '" + pvplose + "' WHERE uuid = '" + player.uuid + "'");
                            }
                        }
                    }
                    pvpteam.clear();
                }
            }
        });

        // 맵이 불러와졌을 때
        Events.on(WorldLoadEvent.class, e -> {
            Threads.playtime = "00:00.00";

            // 전력 노드 정보 초기화
            powerblock.clear();

            peacetime = true;
        });

        Events.on(PlayerConnect.class, e -> {
            if(e.player.name.contains("Owner") || e.player.name.contains("Admin")){
                Call.onKick(e.player.con, "You're tried bad nickname!");
            }

            // 닉네임이 블랙리스트에 등록되어 있는지 확인
            String blacklist = Core.settings.getDataDirectory().child("mods/Essentials/data/blacklist.json").readString();
            JSONTokener parser = new JSONTokener(blacklist);
            JSONArray array = new JSONArray(parser);

            for (int i = 0; i < array.length(); i++) {
                if (array.getString(i).matches(e.player.name)) {
                    e.player.con.kick("Server doesn't allow blacklisted nickname.\n서버가 이 닉네임을 허용하지 않습니다.\nBlack listed nickname: " + e.player.name);
                    Global.log(nbundle("nickname-blacklisted", e.player.name));
                }
            }
        });

        // 플레이어가 아이템을 특정 블록에다 직접 가져다 놓았을 때
        Events.on(DepositEvent.class, e -> {
            // 만약 그 특정블록이 토륨 원자로이며, 맵 설정에서 원자로 폭발이 비활성화 되었을 경우
            if (e.tile.block() == Blocks.thoriumReactor && config.isDetectreactor() && !state.rules.reactorExplosions) {
                nukeblock.put(e.tile.entity.tileX() + "/" + e.tile.entity.tileY() + "/" + e.player.name);
                Thread t = new Thread(() -> {
                    try {
                        for (int i = 0; i < nukeblock.length(); i++) {
                            String nukedata = nukeblock.getString(i);
                            String[] data = nukedata.split("/");
                            int x = Integer.parseInt(data[0]);
                            int y = Integer.parseInt(data[1]);
                            String builder = data[2];
                            NuclearReactor.NuclearReactorEntity entity = (NuclearReactor.NuclearReactorEntity) world.tile(x, y).entity;
                            if (entity.heat >= 0.01) {
                                Thread.sleep(50);
                                for (int a = 0; a < playerGroup.size(); a++) {
                                    Player other = playerGroup.all().get(a);
                                    other.sendMessage(bundle(other, "detect-thorium"));
                                }

                                if (Global.config.getLanguage().equals("ko")) {
                                    writelog("griefer", getTime() + builder + " 플레이어가 냉각수가 공급되지 않는 토륨 원자로에 토륨을 넣었습니다.");
                                } else {
                                    writelog("griefer", getTime() + builder + "put thorium in Thorium Reactor without Cryofluid.");
                                }
                                Call.onTileDestroyed(world.tile(x, y));
                            } else {
                                Thread.sleep(1950);
                                if (entity.heat >= 0.01) {
                                    for (int a = 0; a < playerGroup.size(); a++) {
                                        Player other = playerGroup.all().get(a);
                                        other.sendMessage(bundle(other, "detect-thorium"));
                                    }

                                    if (Global.config.getLanguage().equals("ko")) {
                                        writelog("griefer", getTime() + builder + " 플레이어가 냉각수가 공급되지 않는 토륨 원자로에 토륨을 넣었습니다.");
                                    } else {
                                        writelog("griefer", getTime() + builder + "put thorium in Thorium Reactor without Cryofluid.");
                                    }
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

            Team no_core = getTeamNoCore(e.player);
            e.player.setTeam(no_core);
            Call.onPlayerDeath(e.player);

            Thread t = new Thread(() -> {
                Thread.currentThread().setName(e.player.name+" Player Join");
                if (config.isLoginenable()) {
                    if (isNocore(e.player)) {
                        JSONObject db = getData(e.player.uuid);
                        if (db.has("uuid")) {
                            if (db.getString("uuid").equals(e.player.uuid)) {
                                e.player.sendMessage(bundle(e.player, "autologin"));
                                playerdb.load(e.player, null);
                            }
                        } else {
                            // 로그인 요구
                            String message = "You will need to login with [accent]/login <username> <password>[] to get access to the server.\n" +
                                    "If you don't have an account, use the command [accent]/register <password>[].\n\n" +
                                    "서버를 플레이 할려면 [accent]/login <사용자 이름> <비밀번호>[] 를 입력해야 합니다.\n" +
                                    "만약 계정이 없다면 [accent]/register <비밀번호>[]를 입력해야 합니다.";
                            Call.onInfoMessage(e.player.con, message);
                        }
                    }
                } else if (!config.isLoginenable()) {
                    // 로그인 기능이 꺼져있을 때, 바로 계정 등록을 하고 데이터를 로딩함
                    if (playerdb.register(e.player)) {
                        playerdb.load(e.player, null);
                    } else {
                        Call.onKick(e.player.con, nbundle("plugin-error-kick"));
                    }
                } else {
                    Call.onKick(e.player.con, nbundle("plugin-error-kick"));
                    Global.loge("!! Account system fatal error occurred !!");
                    try {
                        throw new Exception("Account system failed");
                    } catch (Exception ex) {
                        Global.loge("Be sure to send this issue to the plugin developer!");
                        JSONObject db = getData(player.uuid);
                        Global.loge("Target player data: " + db.toString());
                        Global.loge("====== Stacktrace info start ======");
                        ex.printStackTrace();
                        Global.loge("====== Stacktrace info end ======");
                        net.dispose();
                        Core.app.exit();
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
                                Call.onKick(e.player.con, "Server isn't allow VPN connection.");
                            }
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            });
            t.start();

            // PvP 평화시간 설정
            if (config.isEnableantirush() && Vars.state.rules.pvp && peacetime) {
                state.rules.playerDamageMultiplier = 0f;
                state.rules.playerHealthMultiplier = 0.001f;
            }

            // 플레이어 인원별 난이도 설정
            if(config.isAutodifficulty()){
                int total = playerGroup.size();
                if(config.getEasy() >= total){
                    Difficulty.valueOf("easy");
                } else if(config.getNormal() == total){
                    Difficulty.valueOf("normal");
                } else if(config.getHard() == total){
                    Difficulty.valueOf("hard");
                } else if(config.getInsane() <= total){
                    Difficulty.valueOf("insane");
                }
            }
        });

        // 플레이어가 서버에서 탈주했을 때
        Events.on(PlayerLeave.class, e -> {
            if (isLogin(e.player)) {
                writeData("UPDATE players SET connected = '0', connserver = 'none' WHERE uuid = '" + e.player.uuid + "'");
            }
        });

        // 플레이어가 수다떨었을 때
        Events.on(PlayerChatEvent.class, e -> {
            if (isLogin(e.player)) {
                String check = String.valueOf(e.message.charAt(0));
                // 명령어인지 확인
                if (!check.equals("/")) {
                    JSONObject db = getData(e.player.uuid);

                    if (e.message.equals("y") && Vote.isvoting) {
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
                                if (getData(others.uuid).toString().equals("{}")) return;
                                others.sendMessage(bundle(others, "vote-current", current, Vote.require - current));
                            }
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
                                    ser.bw.write(msg + "\n");
                                    ser.bw.flush();
                                }
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        } else if (!config.isClientenable() && !config.isServerenable()) {
                            e.player.sendMessage(bundle(e.player, "no-any-network"));
                            writeData("UPDATE players SET crosschat = '0' WHERE uuid = '" + e.player.uuid + "'");
                        }
                    }
                }

                // 마지막 대화 데이터를 DB에 저장함
                Thread t = new Thread(() -> {
                    Thread.currentThread().setName("DB Thread");
                    String sql = "UPDATE players SET lastchat = ? WHERE uuid = ?";
                    try {
                        PreparedStatement pstmt = conn.prepareStatement(sql);
                        pstmt.setString(1, e.message);
                        pstmt.setString(2, e.player.uuid);
                        pstmt.executeUpdate();
                        pstmt.close();
                    } catch (Exception ex) {
                        Global.loge(sql);
                        printStackTrace(ex);
                    }
                });
                t.start();

                // 번역기능 작동
                Translate tr = new Translate();
                tr.main(e.player, e.message);
            }
        });
        /*Events.on(PlayerChatEvent.class, e -> {
            if(isLogin(e.player)) {
                String check = String.valueOf(e.message.charAt(0));
                // 명령어인지 확인
                if (!check.equals("/")) {
                    if (e.message.matches("(.*쌍[\\S\\s]{0,2}(년|놈).*)|(.*(씨|시)[\\S\\s]{0,2}(벌|빨|발|바).*)|(.*장[\\S\\s]{0,2}애.*)|(.*(병|븅)[\\S\\s]{0,2}(신|쉰|싄).*)|(.*(좆|존|좃)[\\S\\s]{0,2}(같|되|는|나).*)|(.*(개|게)[\\S\\s]{0,2}(같|갓|새|세|쉐).*)|(.*(걸|느)[\\S\\s]{0,2}(레|금).*)|(.*(꼬|꽂|고)[\\S\\s]{0,2}(추|츄).*)|(.*(니|너)[\\S\\s]{0,2}(어|엄|엠|애|m|M).*)|(.*(노)[\\S\\s]{0,1}(애|앰).*)|(.*(섹|쎅)[\\S\\s]{0,2}(스|s|쓰).*)|(ㅅㅂ|ㅄ|ㄷㅊ)|(.*(섹|쎅)[\\S\\s]{0,2}(스|s|쓰).*)|(.*s[\\S\\s]{0,1}e[\\S\\s]{0,1}x.*)")) {
                        Call.onKick(e.player.con, "You're kicked because using bad words.\n욕설 사용에 의해 강퇴되었습니다.");
                    } else {
                        JSONObject db = getData(e.player.uuid);
                        NetClient.visual = true;
                        if (e.player.uuid.equals("hMHCIDJpHKQ=")) {
                            if(!db.getBoolean("crosschat")){
                                Call.sendMessage("[sky][Owner] " + colorizeName(e.player.id, e.player.name) + "[white] : " + e.message);
                            }
                        } else if (e.player.isAdmin) {
                            if(!db.getBoolean("crosschat")) {
                                Call.sendMessage("[green][Admin] " + colorizeName(e.player.id, e.player.name) + "[white] : " + e.message);
                            }
                        } else {
                            Call.sendMessage(colorizeName(e.player.id, e.player.name) + "[white] : " + e.message);
                        }
                        NetClient.visual = false;

                        if (e.message.equals("y") && Vote.isvoting) {
                            // 투표가 진행중일때
                            if (Vote.list.contains(e.player.uuid)) {
                                e.player.sendMessage(bundle(player, "vote-already"));
                            } else {
                                Vote.list.add(e.player.uuid);
                                int current = Vote.list.size();
                                for (Player others : playerGroup.all()) {
                                    if (getData(others.uuid).toString().equals("{}")) return;
                                    others.sendMessage(bundle(others, "vote-current", current, Vote.require - current));
                                }
                                if (Vote.require - current == 0) {
                                    Vote.counting.interrupt();
                                }
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
                                        ser.bw.write(msg + "\n");
                                        ser.bw.flush();
                                    }
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }
                                NetClient.visual = true;
                                Call.sendMessage("[#357EC7][SC] " + msg);
                                NetClient.visual = false;
                            } else if (!config.isClientenable() && !config.isServerenable()) {
                                e.player.sendMessage(bundle(e.player, "no-any-network"));
                                writeData("UPDATE players SET crosschat = '0' WHERE uuid = '" + e.player.uuid + "'");
                            }
                        }
                    }

                    // 마지막 대화 데이터를 DB에 저장함
                    Thread t = new Thread(() -> {
                        Thread.currentThread().setName("DB Thread");
                        String sql = "UPDATE players SET lastchat = ? WHERE uuid = ?";
                        try {
                            PreparedStatement pstmt = conn.prepareStatement(sql);
                            pstmt.setString(1, e.message);
                            pstmt.setString(2, e.player.uuid);
                            pstmt.executeUpdate();
                            pstmt.close();
                        } catch (Exception ex) {
                            Global.loge(sql);
                            printStackTrace(ex);
                        }
                    });
                    t.start();

                    // 번역기능 작동
                    Translate tr = new Translate();
                    tr.main(e.player, e.message);
                }
            } else {
                String check = String.valueOf(e.message.charAt(0));
                if (!check.equals("/")) {
                    e.player.sendMessage("You're not logged!/로그인 하지 않은 상태에서는 채팅을 칠 수 없습니다.");
                }
            }
        });*/

        // 플레이어가 블럭을 건설했을 때
        Events.on(BlockBuildEndEvent.class, e -> {
            if (!e.breaking && e.player != null && e.player.buildRequest() != null && isNocore(e.player)) {
                Thread t = new Thread(() -> {
                    JSONObject db = getData(e.player.uuid);
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

                        writeData("UPDATE players SET lastplacename = '" + e.tile.block().name + "', placecount = '" + data + "', exp = '" + newexp + "' WHERE uuid = '" + e.player.uuid + "'");

                        if (e.player.buildRequest().block == Blocks.thoriumReactor) {
                            int reactorcount = db.getInt("reactorcount");
                            reactorcount++;
                            writeData("UPDATE players SET reactorcount = '" + reactorcount + "' WHERE uuid = '" + e.player.uuid + "'");
                        }
                    } catch (Exception ex) {
                        printStackTrace(ex);
                    }


                    // 메세지 블럭을 설치했을 경우, 해당 블럭을 감시하기 위해 위치를 저장함.
                    if (e.tile.entity.block == Blocks.message) {
                        messagemonitor.add(e.tile);
                    }

                    // 플레이어가 토륨 원자로를 만들었을 때, 감시를 위해 그 원자로의 위치를 저장함.
                    if (e.tile.entity.block == Blocks.thoriumReactor) {
                        nukeposition.put(e.tile.entity.tileX() + "/" + e.tile.entity.tileY());
                        nukedata.add(e.tile);
                    }
                });
                PlayerDB.ex.submit(t);
            }
        });

        // 플레이어가 블럭을 뽀갰을 때
        Events.on(BuildSelectEvent.class, e -> {
            if (e.builder instanceof Player && e.builder.buildRequest() != null && !e.builder.buildRequest().block.name.matches(".*build.*")) {
                if (e.breaking) {
                    Thread t = new Thread(() -> {
                        JSONObject db = getData(((Player) e.builder).uuid);
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

                            writeData("UPDATE players SET lastplacename = '" + e.tile.block().name + "', breakcount = '" + data + "', exp = '" + newexp + "' WHERE uuid = '" + ((Player) e.builder).uuid + "'");

                            if (e.builder.buildRequest() != null && e.builder.buildRequest().block == Blocks.thoriumReactor) {
                                int reactorcount = db.getInt("reactorcount");
                                reactorcount++;
                                writeData("UPDATE players SET reactorcount = '" + reactorcount + "' WHERE uuid = '" + ((Player) e.builder).uuid + "'");
                            }
                        } catch (Exception ex) {
                            printStackTrace(ex);
                            Call.onKick(((Player) e.builder).con, "You're not logged!");
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
                    });
                    PlayerDB.ex.submit(t);
                }
            }
        });

        // 유닛을 박살냈을 때
        Events.on(UnitDestroyEvent.class, e -> {
            // 뒤진(?) 유닛이 플레이어일때
            if (e.unit instanceof Player) {
                Player player = (Player) e.unit;
                JSONObject db = getData(player.uuid);
                if (!Vars.state.teams.get(player.getTeam()).cores.isEmpty() && !db.isNull("deathcount")) {
                    int deathcount = db.getInt("deathcount");
                    deathcount++;
                    writeData("UPDATE players SET killcount = '" + deathcount + "' WHERE uuid = '" + player.uuid + "'");
                }
            }

            // 터진 유닛수만큼 카운트해줌
            if (playerGroup != null && playerGroup.size() > 0) {
                Thread t = new Thread(() -> {
                    for (int i = 0; i < playerGroup.size(); i++) {
                        Player player = playerGroup.all().get(i);
                        if (!Vars.state.teams.get(player.getTeam()).cores.isEmpty()) {
                            JSONObject db = getData(player.uuid);
                            int killcount;
                            if (db.has("killcount")) {
                                killcount = db.getInt("killcount");
                            } else {
                                return;
                            }
                            killcount++;
                            writeData("UPDATE players SET killcount = '" + killcount + "' WHERE uuid = '" + player.uuid + "'");
                        }
                    }
                });
                PlayerDB.ex.submit(t);
            }
        });

        /*
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
                        player.con.kick(KickReason.banned);
                    }
                }
            });
            ex.submit(bansharing);
        });

        // 이건 IP 밴당했을때 작동
        Events.on(PlayerIpBanEvent.class, e -> {
            Thread bansharing = new Thread(() -> {
                if (config.isBanshare() && config.isClientenable()) {
                    Client client = new Client();
                    client.main("bansync", null, null);
                }
            });
           ex.submit(bansharing);
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

         */

        // 로그인 기능이 켜져있을때, 비 로그인 사용자들에게 알림을 해줌
        if (config.isLoginenable()) {
            Timer alerttimer = new Timer(true);
            alerttimer.scheduleAtFixedRate(new login(), 60000, 60000);
        }

        // 1초마다 실행되는 작업 시작
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new Threads(), 1000, 1000);

        // 롤백 명령어에서 사용될 자동 저장작업 시작
        Timer rt = new Timer(true);
        rt.scheduleAtFixedRate(new AutoRollback(), config.getSavetime() * 60000, config.getSavetime() * 60000);

        // 0.016초마다 실행 및 서버 종료시 실행할 작업
        Core.app.addListener(new ApplicationListener() {
            int delaycount = 0;
            boolean a1, a2, a3 = false;

            @Override
            public void update() {
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
                        }
                    }
                }
            }

            public void dispose() {
                // 자원감시 종료
                monitorresource.interrupt();

                // 임시로 밴한 유저들 시간 카운트 종료
                bantime.interrupt();

                // 타이머 스레드 종료
                try {
                    timer.cancel();
                    Global.log(Global.nbundle("count-thread-disabled"));
                } catch (Exception e) {
                    Global.loge(Global.nbundle("count-thread-disable-error"));
                    printStackTrace(e);
                }

                // 서버 종료
                if (config.isServerenable()) {
                    try {
                        for (Server.Service ser : Server.list) {
                            ser.interrupt();
                            Server.list.remove(ser);
                        }

                        Server.active = false;
                        Server.serverSocket.close();

                        Global.log(nbundle("server-thread-disabled"));
                    } catch (Exception e) {
                        printStackTrace(e);
                        Global.loge(nbundle("server-thread-disable-error"));
                    }
                }

                // 클라이언트 종료
                if (config.isClientenable()) {
                    Client client = new Client();
                    client.main("exit", null, null);
                    //client.interrupt();
                    Global.log(nbundle("client-thread-disabled"));
                }

                // 모든 이벤트 서버 종료
                for (Process value : process) {
                    value.destroy();
                }

                // 모든 스레드 종료
                executorService.shutdown();
                Log.ex.shutdown();
                PlayerDB.ex.shutdown();

                // DB 종료
                closeconnect();

                // 클라이언트 플레이어 카운트 스레드 종료
                jumpcheck.interrupt();
            }
        });

        // 서버가 켜진 시간을 0으로 설정
        Threads.uptime = "00:00.00";

        // 활성화된 기능 목록들을 표시함
        Global.logco(config.checkfeatures());
    }

    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.register("admin", "<name>","Set admin status to player.", (arg) -> {
            if(arg.length == 0) {
                Global.log(nbundle("no-parameter"));
                return;
            }
            Thread t = new Thread(() -> {
                Player other = playerGroup.find(p -> p.name.equalsIgnoreCase(arg[0]));
                if(other == null){
                    Global.log(nbundle("player-not-found"));
                } else {
                    writeData("UPDATE players SET isadmin = 1 WHERE uuid = '"+other.uuid+"'");
                    other.isAdmin = true;
                    Global.log("Done!");
                }
            });
            executorService.execute(t);
        });
        handler.register("allinfo", "<name>", "Show player information.", (arg) -> {
            if(arg.length == 0) {
                Global.log(nbundle("no-parameter"));
                return;
            }
            Thread t = new Thread(() -> {
                try{
                    String sql = "SELECT * FROM players WHERE name='"+arg[0]+"'";
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(sql);
                    Global.logn("Data line start.");
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
                                "PvP Surrender: "+rs.getInt("pvpbreakout");
                        Global.logn(datatext);
                    }
                    rs.close();
                    stmt.close();
                    Global.logn("Data line end.");
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
                    Global.logw(nbundle("banshare-disabled"));
                }
            } else {
                Global.logw(nbundle("banshare-server"));
            }
        });
        handler.register("blacklist", "<add/remove> <nickname>", "Block special nickname.", arg -> {
            if(arg.length < 1) {
                Global.log(nbundle("no-parameter"));
                return;
            }
            if(arg[0].equals("add")){
                String db = Core.settings.getDataDirectory().child("mods/Essentials/data/blacklist.json").readString();
                JSONTokener parser = new JSONTokener(db);
                JSONArray object = new JSONArray(parser);
                object.put(arg[1]);
                Core.settings.getDataDirectory().child("mods/Essentials/data/blacklist.json").writeString(String.valueOf(object));
                Global.log(nbundle("blacklist-add", arg[1]));
            } else if (arg[0].equals("remove")) {
                String db = Core.settings.getDataDirectory().child("mods/Essentials/data/blacklist.json").readString();
                JSONTokener parser = new JSONTokener(db);
                JSONArray object = new JSONArray(parser);
                for (int i = 0; i < object.length(); i++) {
                    if (object.get(i).equals(arg[1])) {
                        object.remove(i);
                    }
                }
                Core.settings.getDataDirectory().child("mods/Essentials/data/blacklist.json").writeString(String.valueOf(object));
                Global.log(nbundle("blacklist-remove", arg[1]));
            } else {
                Global.logw(nbundle("blacklist-invalid"));
            }
        });
        handler.register("reset", "<zone/count/total>", "Clear a server-to-server jumping zone data.", arg -> {
            if(arg.length == 0) {
                Global.log(nbundle("no-parameter"));
                return;
            }
            switch(arg[0]){
                case "zone":
                    for (int i = 0; i < jumpzone.length(); i++) {
                        String jumpdata = jumpzone.get(i).toString();
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
                    jumpzone = new JSONArray();
                    Global.log(nbundle("jump-reset", "zone"));
                    break;
                case "count":
                    jumpcount = new JSONArray();
                    Global.log(nbundle("jump-reset", "count"));
                    break;
                case "total":
                    jumpall = new JSONArray();
                    Global.log(nbundle("jump-reset", "total"));
                    break;
                default:
                    Global.log("Invalid option!");
                    break;
            }
        });
        handler.register("reload", "Reload Essentials config", arg -> {
            Config config = new Config();
            config.main();
            Global.log(nbundle("config-reloaded"));
        });
        handler.register("reconnect", "Reconnect remote server (Essentials server only!)", arg -> {
            if(config.isClientenable()){
                Global.logc(nbundle("server-connecting"));

                if(serverconn){
                    Client client = new Client();
                    client.main("exit", null, null);
                }
                Client client = new Client();
                client.main(null, null, null);

            } else {
                Global.log(nbundle("client-disabled"));
            }

            Global.logc(nbundle("db-connecting"));
            closeconnect();
            PlayerDB db = new PlayerDB();
            db.openconnect();
        });
        handler.register("kickall", "Kick all players.",  arg -> {
            for(int a=0;a<playerGroup.size();a++){
                Player others = playerGroup.all().get(a);
                Call.onKick(others.con, "All kick players by administrator.");
            }
            Global.log("It's done.");
        });
        handler.register("kill", "<username>", "Kill target player.", arg -> {
            if(arg.length == 0) {
                Global.log(nbundle("no-parameter"));
                return;
            }
            Player other = playerGroup.find(p -> p.name.equalsIgnoreCase(arg[0]));
            if(other != null){
                other.kill();
            } else {
                Global.log(nbundle("player-not-found"));
            }
        });
        handler.register("nick", "<name> <newname...>", "Show player information.", (arg) -> {
            if(arg.length < 1) {
                Global.log(nbundle("no-parameter"));
                return;
            }
            try{
                writeData("UPDATE players SET name='"+arg[1]+"' WHERE name = '"+arg[0]+"'");
                Global.log(nbundle("player-nickname-change-to", arg[0], arg[1]));
            }catch (Exception e){
                printStackTrace(e);
                Global.log(nbundle("player-not-found"));
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
            Global.log("Currently not supported!");
        });
        handler.register("sync", "<player>", "Force sync request from the target player.", arg -> {
            Player other = playerGroup.find(p -> p.name.equalsIgnoreCase(arg[0]));
            if(other != null){
                Call.onWorldDataBegin(other.con);
                netServer.sendWorldData(other);
            } else {
                Global.logw(nbundle("player-not-found"));
            }
        });
        handler.register("team","[name]", "Change target player team.", (arg) -> {
            Player other = playerGroup.find(p -> p.name.equalsIgnoreCase(arg[0]));
            if(other != null){
                int i = other.getTeam().ordinal()+1;
                while(i != other.getTeam().ordinal()){
                    if (i >= Team.all.length) i = 0;
                    if(!state.teams.get(Team.all[i]).cores.isEmpty()){
                        other.setTeam(Team.all[i]);
                        break;
                    }
                    i++;
                }
                other.kill();
            } else {
                Global.logw(nbundle("player-not-found"));
            }
        });
        handler.register("tempban", "<type-id/name/ip> <username/IP/ID> <time...>", "Temporarily ban player. time unit: 1 hours.", arg -> {
            int bantimeset = Integer.parseInt(arg[1]);
            Player other = playerGroup.find(p -> p.name.equalsIgnoreCase(arg[0]));
            if(other == null){
                Global.logw(nbundle("player-not-found"));
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
                        Global.log(nbundle("tempban", other.name, arg[1]));
                    } else {
                        Global.logw(nbundle("player-not-found"));
                    }
                    break;
                case "ip":
                    netServer.admins.banPlayerIP(arg[1]);
                    Global.log(nbundle("tempban", other.name, arg[1]));
                    break;
                default:
                    err("Invalid type.");
                    break;
            }
            Call.onKick(other.con, "Temp kicked");
            if(config.isClientenable()){
                Client client = new Client();
                client.main("bansync", null, null);
            }
        });
        handler.register("test", "Check that the plug-in is working properly.", arg -> {

        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.removeCommand("votekick");

        handler.<Player>register("ch", "Send chat to another server.", (arg, player) -> {
            if(checklogin(player)) return;

            JSONObject db = getData(player.uuid);
            boolean value = db.getBoolean("crosschat");
            int set;
            if (!value) {
                set = 1;
                player.sendMessage(bundle(player, "crosschat"));
            } else {
                set = 0;
                player.sendMessage(bundle(player, "crosschat-disable"));
            }

            writeData("UPDATE players SET crosschat = '" + set + "' WHERE uuid = '" + player.uuid + "'");
        });
        handler.<Player>register("changepw", "<new_password>", (arg, player) -> {
            if(checklogin(player)) return;
            if(!checkpw(player, arg[0])) return;
            try{
                Class.forName("org.mindrot.jbcrypt.BCrypt");
                String hashed = BCrypt.hashpw(arg[0], BCrypt.gensalt(11));
                writeData("UPDATE players SET accountpw = '"+hashed+"' WHERE accountid = '"+player.uuid+"'");
                player.sendMessage(bundle(player,"success"));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            writeData("UPDATE");
        });
        handler.<Player>register("color", "Enable color nickname", (arg, player) -> {
            if(checklogin(player)) return;

            if (!player.isAdmin) {
                player.sendMessage(bundle(player, "notadmin"));
            } else {
                JSONObject db = getData(player.uuid);
                boolean value = db.getBoolean("colornick");
                int set;
                if (!value) {
                    set = 1;
                    player.sendMessage(bundle(player, "colornick"));
                } else {
                    set = 0;
                    player.sendMessage(bundle(player, "colornick-disable"));
                }
                writeData("UPDATE players SET colornick = '" + set + "' WHERE uuid = '" + player.uuid + "'");
            }
        });
        handler.<Player>register("difficulty", "<difficulty>", "Set server difficulty", (arg, player) -> {
            if(checklogin(player)) return;

            if (!player.isAdmin) {
                player.sendMessage(bundle(player, "notadmin"));
            } else {
                try {
                    Difficulty.valueOf(arg[0]);
                    player.sendMessage(bundle(player, "difficulty-set"));
                } catch (IllegalArgumentException e) {
                    player.sendMessage(bundle(player, "difficulty-not-found"));
                }
            }
        });
        handler.<Player>register("event", "<host/join> <roomname> [map] [gamemode]", "Host your own server", (arg, player) -> {
            if(checklogin(player)) return;

            Thread t = new Thread(() -> {
                String currentip = new getip().main();

                switch (arg[0]) {
                    case "host":
                        Thread work = new Thread(() -> {
                            JSONObject db = getData(player.uuid);
                            if (db.toString().equals("{}")) return;
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
                                making = true;

                                int customport = (int) (Math.random() * (8000 - 7950 + 1)) + 8000;
                                String settings = Core.settings.getDataDirectory().child("mods/Essentials/data/data.json").readString();
                                JSONTokener parser = new JSONTokener(settings);
                                JSONObject object = new JSONObject(parser);

                                JSONArray array = new JSONArray();
                                JSONObject item = new JSONObject();
                                item.put("name", arg[1]);
                                item.put("port", customport);
                                array.put(item);

                                object.put("servers", array);
                                Core.settings.getDataDirectory().child("mods/Essentials/data/data.json").writeString(String.valueOf(object));

                                Threads.eventserver es = new Threads.eventserver();
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
                                Global.log(nbundle("event-host-opened", player.name, customport));

                                writeData("UPDATE players SET connected = '0', connserver = 'none' WHERE uuid = '" + player.uuid + "'");
                                Call.onConnect(player.con, currentip, customport);
                            } else {
                                player.sendMessage(bundle(player, "event-level"));
                            }
                        });
                        if (!making) {
                            work.start();
                        }
                        break;
                    case "join":
                        String settings = Core.settings.getDataDirectory().child("mods/Essentials/data/data.json").readString();
                        JSONTokener parser = new JSONTokener(settings);
                        JSONObject object = new JSONObject(parser);
                        JSONArray arr = object.getJSONArray("servers");
                        for (int a = 0; a < arr.length(); a++) {
                            JSONObject ob = arr.getJSONObject(a);
                            String name = ob.getString("name");
                            if (name.equals(arg[1])) {
                                writeData("UPDATE players SET connected = '0', connserver = 'none' WHERE uuid = '" + player.uuid + "'");
                                Call.onConnect(player.con, currentip, ob.getInt("port"));
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
            if(checklogin(player)) return;
            player.sendMessage("X: " + Math.round(player.x) + " Y: " + Math.round(player.y));
        });
        handler.<Player>register("info", "Show your information", (arg, player) -> {
            Thread t = new Thread(() -> {
                if (checklogin(player)) return;
                String ip = Vars.netServer.admins.getInfo(player.uuid).lastIP;
                JSONObject db = getData(player.uuid);
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
            PlayerDB.ex.submit(t);
        });
        handler.<Player>register("jump", "<serverip> <port> <range> <block-type>", "Create a server-to-server jumping zone.", (arg, player) -> {
            if(checklogin(player)) return;
            if(player.isAdmin){
                int size;
                try{
                    size = Integer.parseInt(arg[2]);
                } catch (Exception ignored){
                    player.sendMessage(bundle(player, "jump-not-int"));
                    return;
                }
                int block;
                try{
                    block = Integer.parseInt(arg[3]);
                } catch (Exception ignored){
                    player.sendMessage(bundle(player, "jump-not-block"));
                    return;
                }
                Block target;
                switch(block){
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
                int xt = player.tileX();
                int yt = player.tileY();
                int tilexfinal = xt+size;
                int tileyfinal = yt+size;

                for(int x=0;x<size;x++){
                    for(int y=0;y<size;y++){
                        Tile tile = world.tile(xt+x, yt+y);
                        Call.onConstructFinish(tile, target, 0, (byte) 0, Team.sharded, false);
                    }
                }

                jumpzone.put(xt+"/"+yt+"/"+tilexfinal+"/"+tileyfinal+"/"+arg[0]+"/"+arg[1]+"/"+block);
                player.sendMessage(bundle(player, "jump-added"));
            } else {
                player.sendMessage(bundle(player, "notadmin"));
            }
        });
        handler.<Player>register("jumpcount", "<serverip> <port>", "Add server player counting", (arg, player) -> {
            if(checklogin(player)) return;
            if (!player.isAdmin) {
                player.sendMessage(bundle(player, "notadmin"));
            } else {
                jumpcount.put(arg[0] + "/" + arg[1] + "/" + player.tileX() + "/" + player.tileY() + "/0/0");
                player.sendMessage(bundle(player, "jump-added"));
            }
        });
        handler.<Player>register("jumptotal", "Counting all server players", (arg, player) -> {
            if(checklogin(player)) return;
            if (!player.isAdmin) {
                player.sendMessage(bundle(player, "notadmin"));
            } else {
                jumpall.put(player.tileX() + "/" + player.tileY() + "/0/0");
                player.sendMessage(bundle(player, "jump-added"));
            }
        });
        handler.<Player>register("kickall", "Kick all players", (arg, player) -> {
            if(checklogin(player)) return;
            if (!player.isAdmin) {
                player.sendMessage(bundle(player, "notadmin"));
            } else {
                Vars.netServer.kickAll(KickReason.gameover);
            }
        });
        handler.<Player>register("kill", "<player>", "Kill player.", (arg, player) -> {
            if(checklogin(player)) return;
            if (player.isAdmin) {
                Player other = Vars.playerGroup.find(p -> p.name.equalsIgnoreCase(arg[0]));
                if (other == null) {
                    player.sendMessage(bundle(player, "player-not-found"));
                    return;
                }
                Player.onPlayerDeath(other);
            } else {
                player.sendMessage(bundle(player, "notadmin"));
            }
        });
        handler.<Player>register("login", "<id> <password>", "Access your account", (arg, player) -> {
            if (config.isLoginenable()) {
                if(!isLogin(player)) {
                    if (PlayerDB.login(player, arg[0], arg[1])) {
                        PlayerDB playerdb = new PlayerDB();
                        playerdb.load(player, arg[0]);
                        if (getData(player.uuid).toString().equals("{}")) {
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
            if(checklogin(player)) return;
            if(config.isLoginenable()) {
                writeData("UPDATE players SET connected = '0', uuid = 'LogoutAAAAA=' WHERE uuid = '" + player.uuid + "'");
                Call.onKick(player.con, nbundle("logout"));
            } else {
                player.sendMessage(bundle(player, "login-not-use"));
            }
        });
        handler.<Player>register("me", "[text...]", "broadcast * message", (arg, player) -> {
            if(checklogin(player)) return;
            Call.sendMessage("[orange]*[] " + player.name + "[white] : " + arg[0]);
        });
        handler.<Player>register("motd", "Show server motd.", (arg, player) -> {
            if(checklogin(player)) return;
            String motd = getmotd(player);
            int count = motd.split("\r\n|\r|\n").length;
            if (count > 10) {
                Call.onInfoMessage(player.con, motd);
            } else {
                player.sendMessage(motd);
            }
        });
        handler.<Player>register("register", "<password>", "Register account", (arg, player) -> {
            if (config.isLoginenable()) {
                PlayerDB playerdb = new PlayerDB();
                if (playerdb.register(player, arg[0])) {
                    if (Vars.state.rules.pvp) {
                        int index = player.getTeam().ordinal() + 1;
                        while (index != player.getTeam().ordinal()) {
                            if (index >= Team.all.length) {
                                index = 0;
                            }
                            if (!Vars.state.teams.get(Team.all[index]).cores.isEmpty()) {
                                player.setTeam(Team.all[index]);
                                break;
                            }
                            index++;
                        }
                    } else {
                        player.setTeam(Vars.defaultTeam);
                    }
                    Call.onPlayerDeath(player);
                    player.sendMessage("[green][Essentials] [white]Register success!/계정 등록 성공!");
                } else {
                    player.sendMessage("[green][Essentials] [scarlet]Register failed/계정 등록 실패!");
                }
            } else {
                player.sendMessage(bundle(player,"login-not-use"));
            }
        });
        handler.<Player>register("save", "Map save", (arg, player) -> {
            if(checklogin(player)) return;
            if (player.isAdmin) {
                Core.app.post(() -> {
                    FileHandle file = saveDirectory.child(config.getSlotnumber() + "." + saveExtension);
                    SaveIO.save(file);
                    player.sendMessage(bundle(player, "mapsaved"));
                });
            } else {
                player.sendMessage(bundle(player, "notadmin"));
            }
        });
        handler.<Player>register("spawn", "<mob_name> <count> [team] [playername]", "Spawn mob in player position", (arg, player) -> {
            if(checklogin(player)) return;
            if(player.isAdmin) {
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
                        for(int i=0;count>i;i++){
                            BaseUnit baseUnit = targetunit.create(targetplayer.getTeam());
                            baseUnit.set(targetplayer.getX(), targetplayer.getY());
                            baseUnit.add();
                        }
                    } else {
                        for(int i=0;count>i;i++){
                            BaseUnit baseUnit = targetunit.create(targetteam);
                            baseUnit.set(player.getX(), player.getY());
                            baseUnit.add();
                        }
                    }
                } else {
                    for(int i=0;count>i;i++){
                        BaseUnit baseUnit = targetunit.create(player.getTeam());
                        baseUnit.set(player.getX(), player.getY());
                        baseUnit.add();
                    }
                }
            } else {
                player.sendMessage(bundle(player, "notadmin"));
            }
        });
        handler.<Player>register("status", "Show server status", (arg, player) -> {
            if(checklogin(player)) return;
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
            if(checklogin(player)) return;
            Player.onPlayerDeath(player);
            if (playerGroup != null && playerGroup.size() > 0) {
                allsendMessage("suicide", player.name);
            }
        });
        handler.<Player>register("team", "Change team (PvP only)", (arg, player) -> {
            if(checklogin(player)) return;
            if (Vars.state.rules.pvp) {
                if (player.isAdmin) {
                    int i = player.getTeam().ordinal() + 1;
                    while (i != player.getTeam().ordinal()) {
                        if (i >= Team.all.length) i = 0;
                        if (!Vars.state.teams.get(Team.all[i]).cores.isEmpty()) {
                            player.setTeam(Team.all[i]);
                            break;
                        }
                        i++;
                    }
                    Call.onPlayerDeath(player);
                } else {
                    player.sendMessage(bundle(player, "notadmin"));
                }
            } else {
                player.sendMessage(bundle(player, "command-only-pvp"));
            }
        });
        handler.<Player>register("tempban", "<player> <time>", "Temporarily ban player. time unit: 1 hours", (arg, player) -> {
            if(checklogin(player)) return;
            if (!player.isAdmin) {
                player.sendMessage(bundle(player, "notadmin"));
            } else {
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
                    Call.onKick(other.con, "Temp kicked");
                    for(int a=0;a<playerGroup.size();a++){
                        Player current = playerGroup.all().get(a);
                        current.sendMessage(bundle(current, "ban-temp", other.name, player.name));
                    }
                } else {
                    player.sendMessage(bundle(player, "player-not-found"));
                }
            }
        });
        handler.<Player>register("time", "Show server time", (arg, player) -> {
            if(checklogin(player)) return;
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd a hh:mm.ss");
            String nowString = now.format(dateTimeFormatter);
            player.sendMessage(bundle(player, "servertime", nowString));
        });
        handler.<Player>register("tp", "<player>", "Teleport to other players", (arg, player) -> {
            if(checklogin(player)) return;
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
            if(checklogin(player)) return;
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
            if (!player.isAdmin) {
                player.sendMessage(bundle(player, "notadmin"));
            } else {
                if (other1 == null || other2 == null) {
                    player.sendMessage(bundle(player, "player-not-found"));
                    return;
                }
                if (!other1.isMobile || !other2.isMobile) {
                    other1.setNet(other2.x, other2.y);
                } else {
                    player.sendMessage(bundle(player, "tp-ismobile"));
                }
            }
        });
        handler.<Player>register("tppos", "<x> <y>", "Teleport to coordinates", (arg, player) -> {
            if(checklogin(player)) return;
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
            if(checklogin(player)) return;
            JSONObject db = getData(player.uuid);
            boolean value = db.getBoolean("translate");
            int set;
            if (!value) {
                set = 1;
                player.sendMessage(bundle(player, "translate"));
            } else {
                set = 0;
                player.sendMessage(bundle(player, "translate-disable"));
            }

            writeData("UPDATE players SET translate = '" + set + "' WHERE uuid = '" + player.uuid + "'");
        });
        handler.<Player>register("vote", "<gameover/skipwave/kick/rollback> [playername...]", "Vote surrender or skip wave, Long-time kick", (arg, player) -> {
            if(checklogin(player)) return;
            if(arg.length == 2){
                Vote vote = new Vote(player, arg[0], arg[1]);
                vote.command();
            } else {
                Vote vote = new Vote(player, arg[0]);
                vote.command();
            }
        });

        handler.<Player>register("test", "pathfinding test", (arg, player) -> {
            //getcount(world.tile(player.tileX(), player.tileY()), Integer.parseInt(arg[0]));
            /*
            if (player.isAdmin) {
                Thread work = new Thread(() -> {
                    EssentialAI ai = new EssentialAI();

                    Tile tile = world.tile(player.tileX(), player.tileY());
                    ai.start = tile;
                    ai.player = player;

                    Call.onConstructFinish(tile, Blocks.copperWall, 0, (byte) 0, Team.sharded, false);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    ai.target = world.tile(player.tileX(), player.tileY());
                    ai.main();
                });
                work.start();
            } else {
                player.sendMessage(bundle(player, "notadmin");
            }

             */
            player.sendMessage(bundle(player, "command-not-avaliable"));
        });
        /*
        handler.<Player>register("votekick", "Player kick starts voting.", (arg, player) -> {
            if(Vars.state.teams.get(player.getTeam()).cores.isEmpty()){
                player.sendMessage("[green][Essentials][scarlet] You aren't allowed to use the command until you log in.");
                return;
            }

            Vote vote = new Vote();
            Player other = Vars.playerGroup.find(p -> p.name.equalsIgnoreCase(arg[0]));
            if (other == null) {
                player.sendMessage(bundle(player, "player-not-found");
                return;
            }
            vote.main(player, "kick", other.name);
        });
         */
    }
}