package essentials.internal;

import arc.Core;
import arc.Events;
import arc.struct.ArrayMap;
import arc.struct.ObjectMap;
import essentials.core.player.PlayerData;
import essentials.core.plugin.PluginData;
import essentials.external.IpAddressMatcher;
import essentials.feature.AntiGrief;
import essentials.network.Client;
import essentials.network.Server;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.entities.type.Player;
import mindustry.game.Difficulty;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.net.Packets;
import mindustry.world.blocks.logic.MessageBlock;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

import static essentials.Main.*;
import static mindustry.Vars.*;
import static mindustry.core.NetClient.colorizeName;
import static mindustry.core.NetClient.onSetRules;
import static org.hjson.JsonValue.readJSON;

public class Event {
    Logger log = LoggerFactory.getLogger(Event.class);

    public Event() {
        Events.on(EventType.TapConfigEvent.class, e -> {
            if (e.tile.entity != null && e.tile.entity.block != null && e.player != null && config.alertAction()) {
                for (Player p : playerGroup.all()) {
                    PlayerData playerData = playerDB.get(p.uuid);
                    if (playerData.alert()) {
                        p.sendMessage(new Bundle(playerData.locale()).get("tap-config", e.player.name, e.tile.entity.block.name));
                    }
                }
                if (config.debug())
                    Log.info("anti-grief.build.config", e.player.name, e.tile.block().name, e.tile.x, e.tile.y);
                if (config.logging()) Log.write(Log.LogType.tap, "log.tap-config", e.player.name, e.tile.block().name);
            }
        });

        Events.on(EventType.TapEvent.class, e -> {
            if (config.logging()) Log.write(Log.LogType.tap, "log.tap", e.player.name, e.tile.block().name);

            PlayerData playerData = playerDB.get(e.player.uuid);

            if (!playerData.error()) {
                for (PluginData.warpzone data : pluginData.warpzones) {
                    if (e.tile.x > data.getStartTile().x && e.tile.x < data.getFinishTile().x) {
                        if (e.tile.y > data.getStartTile().y && e.tile.y < data.getFinishTile().y) {
                            Log.info("player.warped", e.player.name, data.ip + ":" + data.port);
                            playerData.connected(false);
                            playerData.connserver("none");
                            Call.onConnect(e.player.con, data.ip, data.port);
                            break;
                        }
                    }
                }

                for (PluginData.warpblock data : pluginData.warpblocks) {
                    if (e.tile.x >= world.tile(data.tilex, data.tiley).link().x && e.tile.x <= world.tile(data.tilex, data.tiley).link().x) {
                        if (e.tile.y >= world.tile(data.tilex, data.tiley).link().y && e.tile.y <= world.tile(data.tilex, data.tiley).link().y) {
                            Log.info("player.warped", e.player.name, data.ip + ":" + data.port);
                            Call.onConnect(e.player.con, data.ip, data.port);
                            break;
                        }
                    }
                }
            }
        });

        Events.on(EventType.WithdrawEvent.class, e -> {
            if (e.tile.entity != null && e.player.item().item != null && e.player.name != null && config.antiGrief()) {
                for (Player p : playerGroup.all()) {
                    PlayerData playerData = playerDB.get(p.uuid);
                    if (playerData.alert()) {
                        p.sendMessage(new Bundle(playerData.locale()).get("log.withdraw", e.player.name, e.player.item().item.name, e.amount, e.tile.block().name));
                    }
                }
                if (config.debug())
                    Log.info("log.withdraw", e.player.name, e.player.item().item.name, e.amount, e.tile.block().name);
                if (config.logging())
                    Log.write(Log.LogType.withdraw, "log.withdraw", e.player.name, e.player.item().item.name, e.amount, e.tile.block().name);
                if (state.rules.pvp) {
                    if (e.item.flammability > 0.001f) {
                        e.player.sendMessage(new Bundle(playerDB.get(e.player.uuid).locale()).get("system.flammable.disabled"));
                        e.player.clearItem();
                    }
                }
            }
        });

        // 게임오버가 되었을 때
        Events.on(EventType.GameOverEvent.class, e -> {
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
                        PlayerData target = playerDB.get(player.uuid);
                        if (target.login()) {
                            if (player.getTeam().name.equals(e.winner.name)) {
                                target.pvpwincount(target.pvpwincount() + 1);
                            } else if (!player.getTeam().name.equals(e.winner.name)) {
                                target.pvplosecount(target.pvplosecount() + 1);
                            }
                        }
                    }
                }
            } else if (state.rules.attackMode) {
                for (int i = 0; i < playerGroup.size(); i++) {
                    Player player = playerGroup.all().get(i);
                    PlayerData target = playerDB.get(player.uuid);
                    if (target.login()) {
                        target.attackclear(target.attackclear() + 1);
                    }
                }
            }
        });

        // 맵이 불러와졌을 때
        Events.on(EventType.WorldLoadEvent.class, e -> {
            vars.playtime(0);

            // 전력 노드 정보 초기화
            pluginData.powerblock.clear();
        });

        Events.on(EventType.PlayerConnect.class, e -> {
            if (config.logging())
                Log.write(Log.LogType.player, "log.player.connect", e.player.name, e.player.uuid, e.player.con.address);

            // 닉네임이 블랙리스트에 등록되어 있는지 확인
            for (String s : pluginData.blacklist) {
                if (e.player.name.matches(s)) {
                    try {
                        Locale locale = tool.getGeo(e.player);
                        Call.onKick(e.player.con, new Bundle(locale).get("system.nickname.blacklisted.kick"));
                        Log.info("system.nickname.blacklisted", e.player.name);
                    } catch (Exception ex) {
                        new CrashReport(ex);
                    }
                }
            }

            if (config.strictName()) {
                if (e.player.name.length() > 32) Call.onKick(e.player.con, "Nickname too long!");
                //if (e.player.name.matches(".*\\[.*].*"))
                //    Call.onKick(e.player.con, "Color tags can't be used for nicknames on this server.");
                if (e.player.name.contains("　"))
                    Call.onKick(e.player.con, "Don't use blank speical charactor nickname!");
                if (e.player.name.contains(" ")) Call.onKick(e.player.con, "Nicknames can't be used on this server!");
                if (Pattern.matches(".*\\[.*.].*", e.player.name))
                    Call.onKick(e.player.con, "Can't use only color tags nickname in this server.");
            }

            /*if(config.isStrictname()){
                if(e.player.name.length() < 3){
                    player.con.kick("The nickname is too short!\n닉네임이 너무 짧습니다!");
                    log(LogType.log,"nickname-short");
                }
                if(e.player.name.matches("^(?=.*\\\\d)(?=.*[~`!@#$%\\\\^&*()-])(?=.*[a-z])(?=.*[A-Z])$")){
                    e.player.con.kick("Server doesn't allow special characters.\n서버가 특수문자를 허용하지 않습니다.");
                    log(LogType.log,"nickname-special", player.name);
                }
            }*/
        });

        // 플레이어가 아이템을 특정 블록에다 직접 가져다 놓았을 때
        Events.on(EventType.DepositEvent.class, e -> {
            if (e.player.item().amount > e.player.mech.itemCapacity) {
                player.con.kick("Invalid request!");
                return;
            }
            for (Player p : playerGroup.all()) {
                PlayerData playerData = playerDB.get(p.uuid);
                if (playerData.alert()) {
                    p.sendMessage(new Bundle(playerData.locale()).get("anti-grief.deposit", e.player.name, e.player.item().item.name, e.tile.block().name));
                }
            }
            if (config.logging())
                Log.write(Log.LogType.deposit, "log.deposit", e.player.name, e.player.item().item.name, e.tile.block().name);
        });

        // 플레이어가 서버에 들어왔을 때
        Events.on(EventType.PlayerJoin.class, e -> {
            if (config.logging())
                Log.write(Log.LogType.player, "log.player.join", e.player.name, e.player.uuid, e.player.con.address);

            vars.addPlayers(e.player);
            e.player.isAdmin = false;
            Thread t = new Thread(() -> {
                Thread.currentThread().setName(e.player.name + " Player Join thread");
                PlayerData playerData = playerDB.load(e.player.uuid);
                Bundle bundle = new Bundle(playerData.locale());

                if (config.loginEnable()) {
                    if (config.passwordMethod().equals("mixed")) {
                        if (!playerData.error() && config.autoLogin()) {
                            if (playerData.udid() != 0L) {
                                new Thread(() -> Call.onConnect(e.player.con, vars.serverIP(), 7060)).start();
                            } else {
                                e.player.sendMessage(bundle.get("account.autologin"));
                                playerCore.load(e.player);
                            }
                        } else {
                            Locale lc = tool.getGeo(e.player);
                            if (playerDB.register(e.player.name, e.player.uuid, lc.getDisplayCountry(), lc.toString(), lc.getDisplayLanguage(), true, vars.serverIP(), "default", 0L, e.player.name, "none")) {
                                playerCore.load(e.player);
                            } else {
                                Call.onKick(e.player.con, new Bundle().get("plugin-error-kick"));
                            }
                        }
                    } else if (config.passwordMethod().equals("discord")) {
                        if (!playerData.error() && config.autoLogin()) {
                            e.player.sendMessage(bundle.get("account.autologin"));
                            playerCore.load(e.player);
                        } else {
                            String message;
                            Locale language = tool.getGeo(e.player);
                            if (config.passwordMethod().equals("discord")) {
                                message = new Bundle(language).get("system.login.require.discord") + "\n" + config.discordLink();
                            } else {
                                message = new Bundle(language).get("system.login.require.password");
                            }
                            Call.onInfoMessage(e.player.con, message);
                        }
                    } else {
                        if (!playerData.error() && config.autoLogin()) {
                            e.player.sendMessage(bundle.get("account.autologin"));
                            playerCore.load(e.player);
                        } else {
                            String message;
                            Locale language = tool.getGeo(e.player);
                            if (config.passwordMethod().equals("discord")) {
                                message = new Bundle(language).get("system.login.require.discord") + "\n" + config.discordLink();
                            } else {
                                message = new Bundle(language).get("system.login.require.password");
                            }
                            Call.onInfoMessage(e.player.con, message);
                        }
                    }
                } else {
                    // 로그인 기능이 꺼져있을 때, 바로 계정 등록을 하고 데이터를 로딩함
                    if (!playerData.error() && config.autoLogin()) {
                        e.player.sendMessage(bundle.get("account.autologin"));
                        playerCore.load(e.player);
                    } else {
                        Locale lc = tool.getGeo(e.player.con.address);
                        boolean register = playerDB.register(e.player.name, e.player.uuid, lc.getDisplayCountry(), lc.toString(), lc.getDisplayLanguage(), true, vars.serverIP(), "default", 0L, e.player.name, "none");
                        if (!register || !playerCore.load(e.player)) {
                            Call.onKick(e.player.con, new Bundle().get("plugin-error-kick"));
                        }
                    }
                }

                // VPN을 사용중인지 확인
                if (config.antiVPN()) {
                    try {
                        InputStream reader = getClass().getResourceAsStream("/ipv4.txt");
                        BufferedReader br = new BufferedReader(new InputStreamReader(reader));

                        String line;
                        while ((line = br.readLine()) != null) {
                            IpAddressMatcher match = new IpAddressMatcher(line);
                            if (match.matches(e.player.con.address)) {
                                Call.onKick(e.player.con, new Bundle().get("anti-grief.vpn"));
                            }
                        }
                    } catch (IOException ex) {
                        log.warn("VPN File", ex);
                    }
                }

                // PvP 평화시간 설정
                if (config.antiRush() && state.rules.pvp && vars.playtime() < config.antiRushtime()) {
                    state.rules.playerDamageMultiplier = 0f;
                    state.rules.playerHealthMultiplier = 0.001f;
                    Call.onSetRules(state.rules);
                    vars.setPvPPeace(true);
                }

                // 플레이어 인원별 난이도 설정
                if (config.autoDifficulty()) {
                    int total = playerGroup.size();
                    if (config.difficultyEasy() >= total) {
                        state.rules.waveSpacing = Difficulty.valueOf("easy").waveTime * 60 * 60 * 2;
                        //tool.sendMessageAll("system.difficulty.easy");
                    } else if (config.difficultyNormal() == total) {
                        state.rules.waveSpacing = Difficulty.valueOf("normal").waveTime * 60 * 60 * 2;
                        //tool.sendMessageAll("system.difficulty.normal");
                    } else if (config.difficultyHard() == total) {
                        state.rules.waveSpacing = Difficulty.valueOf("hard").waveTime * 60 * 60 * 2;
                        //tool.sendMessageAll("system.difficulty.hard");
                    } else if (config.difficultyInsane() <= total) {
                        state.rules.waveSpacing = Difficulty.valueOf("insane").waveTime * 60 * 60 * 2;
                        //tool.sendMessageAll("system.difficulty.insane");
                    }
                    onSetRules(state.rules);
                }
            });
            t.start();
        });

        // 플레이어가 서버에서 탈주했을 때
        Events.on(EventType.PlayerLeave.class, e -> {
            if (config.logging())
                Log.write(Log.LogType.player, "log.player.leave", e.player.name, e.player.uuid, e.player.con.address);

            PlayerData player = playerDB.get(e.player.uuid);
            if (player.login()) {
                player.connected(false);
                player.connserver("none");
                if (state.rules.pvp && !state.gameOver) player.pvpbreakout(player.pvpbreakout() + 1);
            }
            playerDB.save(player);
            vars.removePlayerData(p -> p.uuid().equals(e.player.uuid));
            vars.removePlayers(e.player);
        });

        // 플레이어가 수다떨었을 때
        Events.on(EventType.PlayerChatEvent.class, e -> {
            if (config.antiGrief() && (e.message.length() > Vars.maxTextLength || e.message.contains("Nexity#2671"))) {
                Call.onKick(e.player.con, "Hacked client detected");
            }

            PlayerData playerData = playerDB.get(e.player.uuid);
            Bundle bundle = new Bundle(playerData.locale());

            if (!e.message.startsWith("/")) Log.info("<&y" + e.player.name + ": &lm" + e.message + "&lg>");

            if (!playerData.error()) {
                // 명령어인지 확인
                if (!e.message.startsWith("/")) {
                    if (e.message.equals("y") && vote.size != 0) {
                        // 투표가 진행중일때
                        if (vote.get(0).getVoted().contains(e.player.uuid)) {
                            e.player.sendMessage(bundle.get("vote.already-voted"));
                        } else {
                            vote.get(0).set(e.player.uuid);
                        }
                    } else {
                        if (!playerData.mute()) {
                            // 서버간 대화기능 작동
                            if (playerData.crosschat()) {
                                if (config.clientEnable()) {
                                    client.request(Client.Request.chat, e.player, e.message);
                                } else if (config.serverEnable()) {
                                    // 메세지를 모든 클라이언트에게 전송함
                                    String msg = "[" + e.player.name + "]: " + e.message;
                                    try {
                                        for (Server.service ser : server.list) {
                                            ser.os.writeBytes(tool.encrypt(msg, ser.spec));
                                            ser.os.flush();
                                        }
                                    } catch (Exception ex) {
                                        log.warn("Crosschat", ex);
                                    }
                                } else {
                                    e.player.sendMessage(bundle.get("no-any-network"));
                                    playerData.crosschat(false);
                                }
                            }
                        }
                    }

                    if (config.translate()) {
                        new Thread(() -> {
                            ArrayMap<String, String> buf = new ArrayMap<>();
                            try {
                                for (Player p : playerGroup.all()) {
                                    PlayerData target = playerDB.get(p.uuid);
                                    if (!target.error() && !target.mute()) {
                                        String original = playerData.locale().getLanguage();
                                        String language = target.locale().getLanguage();

                                        if (original.equals("zh")) original = "zh-CN";
                                        if (language.equals("zh")) language = "zh-CN";

                                        boolean match = false;
                                        for (ObjectMap.Entry<String, String> b : buf) {
                                            if (language.equals(b.key)) {
                                                match = true;
                                                p.sendMessage("[orange][TR] [green]" + e.player.name + "[orange] >[white] " + b.value);
                                                break;
                                            }
                                        }

                                        if (!language.equals(original) && !match) {
                                            HttpURLConnection con = (HttpURLConnection) new URL("https://naveropenapi.apigw.ntruss.com/nmt/v1/translation").openConnection();
                                            con.setRequestMethod("POST");
                                            con.setRequestProperty("X-NCP-APIGW-API-KEY-ID", config.translateId());
                                            con.setRequestProperty("X-NCP-APIGW-API-KEY", config.translatePw());
                                            con.setDoOutput(true);
                                            try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
                                                wr.writeBytes("source=" + original + "&target=" + language + "&text=" + URLEncoder.encode(e.message, "UTF-8"));
                                                wr.flush();
                                                wr.close();

                                                if (con.getResponseCode() != 200) {
                                                    try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getErrorStream(), StandardCharsets.UTF_8))) {
                                                        String inputLine;
                                                        StringBuilder response = new StringBuilder();
                                                        while ((inputLine = br.readLine()) != null) {
                                                            response.append(inputLine);
                                                        }
                                                        Log.write(Log.LogType.error, response.toString());
                                                    }
                                                } else {
                                                    try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                                                        String inputLine;
                                                        StringBuilder response = new StringBuilder();
                                                        while ((inputLine = br.readLine()) != null) {
                                                            response.append(inputLine);
                                                        }

                                                        JsonObject object = readJSON(response.toString()).asObject();
                                                        String result = object.get("message").asObject().get("result").asObject().get("translatedText").asString();
                                                        buf.put(language, result);
                                                        p.sendMessage("[orange][TR] [green]" + e.player.name + "[orange] >[white] " + result);
                                                    }
                                                }
                                            } catch (Exception ex) {
                                                new CrashReport(ex);
                                            }
                                        } else {
                                            if (perm.permission_user.get(playerData.uuid()).asObject().get("prefix") != null) {
                                                if (!playerData.crosschat())
                                                    p.sendMessage(perm.permission_user.get(playerData.uuid()).asObject().get("prefix").asString().replace("%1", colorizeName(e.player.id, e.player.name)).replace("%2", e.message));
                                            } else {
                                                if (!playerData.crosschat())
                                                    p.sendMessage("[orange]" + colorizeName(e.player.id, e.player.name) + "[orange] >[white] " + e.message);
                                            }
                                        }
                                    }
                                }
                            } catch (Exception ex) {
                                new CrashReport(ex);
                            }
                        }).start();
                    } else if (colorizeName(e.player.id, e.player.name) != null) {
                        Call.sendMessage(perm.permission_user.get(playerData.uuid()).asObject().get("prefix").asString().replace("%1", colorizeName(e.player.id, e.player.name)).replace("%2", e.message));
                    }
                }

                // 마지막 대화 데이터를 DB에 저장함
                playerData.lastchat(e.message);
            }
        });

        // 플레이어가 블럭을 건설했을 때
        Events.on(EventType.BlockBuildEndEvent.class, e -> {
            if (e.player == null) return; // 만약 건설자가 드론일경우
            Log.write(Log.LogType.block, "log.block.place", e.player.name, e.tile.block().name);

            PlayerData target = playerDB.get(e.player.uuid);
            if (!e.breaking && e.player.buildRequest() != null && !target.error() && e.tile.block() != null) {
                String name = e.tile.block().name;
                try {
                    JsonObject obj = JsonValue.readHjson(root.child("Exp.hjson").reader()).asObject();
                    int blockexp = obj.getInt(name, 0);

                    target.lastplacename(e.tile.block().name);
                    target.placecount(target.placecount() + 1);
                    target.exp(target.exp() + blockexp);
                    if (e.player.buildRequest().block == Blocks.thoriumReactor)
                        target.reactorcount(target.reactorcount() + 1);
                } catch (Exception ex) {
                    new CrashReport(ex);
                }

                // 메세지 블럭을 설치했을 경우, 해당 블럭을 감시하기 위해 위치를 저장함.
                if (e.tile.block() == Blocks.message) {
                    if (e.tile.entity instanceof MessageBlock.MessageBlockEntity) {
                        pluginData.messagemonitor.add(new PluginData.messagemonitor((MessageBlock.MessageBlockEntity) e.tile.entity));
                    }
                }

                // 플레이어가 토륨 원자로를 만들었을 때, 감시를 위해 그 원자로의 위치를 저장함.
                if (e.tile.block() == Blocks.thoriumReactor) {
                    pluginData.nukeposition.add(e.tile);
                    pluginData.nukedata.add(e.tile);
                }

                if (config.debug() && config.antiGrief()) {
                    Log.info("anti-grief.build.finish", e.player.name, e.tile.block().name, e.tile.x, e.tile.y);
                }

                float range = new AntiGrief().getDistanceToCore(e.player, e.tile);
                if (config.antiGrief() && range < 35 && e.tile.block() == Blocks.thoriumReactor) {
                    e.player.sendMessage(new Bundle(target.locale()).get("anti-grief.reactor.close"));
                    Call.onDeconstructFinish(e.tile, Blocks.air, e.player.id);
                }/* else if (config.antiGrief()) {
                    for (int rot = 0; rot < 4; rot++) {
                        if (e.tile.getNearby(rot).block() != Blocks.liquidTank &&
                                e.tile.getNearby(rot).block() != Blocks.conduit &&
                                e.tile.getNearby(rot).block() != Blocks.bridgeConduit &&
                                e.tile.getNearby(rot).block() != Blocks.phaseConduit &&
                                e.tile.getNearby(rot).block() != Blocks.platedConduit &&
                                e.tile.getNearby(rot).block() != Blocks.pulseConduit) {
                            // TODO 냉각수 감지 추가
                            Call.sendMessage("No cryofluid reactor detected");
                        }
                    }
                }*/
            }
        });

        // 플레이어가 블럭을 뽀갰을 때
        Events.on(EventType.BuildSelectEvent.class, e -> {
            if (e.builder instanceof Player && e.builder.buildRequest() != null && !Pattern.matches(".*build.*", e.builder.buildRequest().block.name) && e.tile.block() != Blocks.air) {
                if (e.breaking) {
                    Log.write(Log.LogType.block, "log.block.remove", ((Player) e.builder).name, e.tile.block().name, e.tile.x, e.tile.y);

                    PlayerData target = playerDB.get(((Player) e.builder).uuid);
                    String name = e.tile.block().name;
                    try {
                        JsonObject obj = JsonValue.readHjson(root.child("Exp.hjson").reader()).asObject();
                        int blockexp = obj.getInt(name, 0);

                        target.lastbreakname(e.tile.block().name);
                        target.breakcount(target.breakcount() + 1);
                        target.exp(target.exp() + blockexp);
                    } catch (Exception ex) {
                        new CrashReport(ex);
                        Call.onKick(((Player) e.builder).con, new Bundle(target.locale()).get("not-logged"));
                    }

                    // 메세지 블럭을 파괴했을 때, 위치가 저장된 데이터를 삭제함
                    if (e.builder.buildRequest().block == Blocks.message) {
                        try {
                            for (int i = 0; i < pluginData.powerblock.size; i++) {
                                if (pluginData.powerblock.get(i).tile == e.tile) {
                                    pluginData.powerblock.remove(i);
                                    break;
                                }
                            }
                        } catch (Exception ex) {
                            new CrashReport(ex);
                        }
                    }

                    // Exp Playing Game (EPG)
                    if (config.expLimit()) {
                        int level = target.level();
                        try {
                            JsonObject obj = JsonValue.readHjson(root.child("Exp.hjson").reader()).asObject();
                            if (obj.get(name) != null) {
                                int blockreqlevel = obj.getInt(name, 999);
                                if (level < blockreqlevel) {
                                    Call.onDeconstructFinish(e.tile, e.tile.block(), ((Player) e.builder).id);
                                    ((Player) e.builder).sendMessage(new Bundle(playerDB.get(((Player) e.builder).uuid).locale()).get("system.epg.block-require", name, blockreqlevel));
                                }
                            } else {
                                Log.err("system.epg.block-not-valid", name);
                            }
                        } catch (Exception ex) {
                            new CrashReport(ex);
                        }
                    }
                }
                if (config.debug() && config.antiGrief()) {
                    Log.info("anti-grief.destroy", ((Player) e.builder).name, e.tile.block().name, e.tile.x, e.tile.y);
                }
            }
        });

        // 유닛을 박살냈을 때
        Events.on(EventType.UnitDestroyEvent.class, e -> {
            // 뒤진(?) 유닛이 플레이어일때
            if (e.unit instanceof Player) {
                Player player = (Player) e.unit;
                PlayerData target = playerDB.get(player.uuid);
                if (!state.teams.get(player.getTeam()).cores.isEmpty()) target.deathcount(target.deathcount() + 1);
            }

            // 터진 유닛수만큼 카운트해줌
            if (playerGroup != null && playerGroup.size() > 0) {
                for (int i = 0; i < playerGroup.size(); i++) {
                    Player player = playerGroup.all().get(i);
                    PlayerData target = playerDB.get(player.uuid);
                    if (!state.teams.get(player.getTeam()).cores.isEmpty()) target.killcount(target.killcount() + 1);
                }
            }
        });

        // 플레이어가 밴당했을 때 공유기능 작동
        Events.on(EventType.PlayerBanEvent.class, e -> {
            Thread bansharing = new Thread(() -> {
                if (config.banShare() && config.clientEnable()) {
                    client.request(Client.Request.bansync, null, null);
                }
            });

            for (Player player : playerGroup.all()) {
                if (player == e.player) {
                    tool.sendMessageAll("player.banned", e.player.name);
                    if (netServer.admins.isIDBanned(player.uuid)) {
                        player.con.kick(Packets.KickReason.banned);
                    }
                }
            }

            mainThread.submit(bansharing);
        });

        // 이건 IP 밴당했을때 작동
        Events.on(EventType.PlayerIpBanEvent.class, e -> {
            Thread bansharing = new Thread(() -> {
                if (config.banShare() && client.activated) {
                    client.request(Client.Request.bansync, null, null);
                }
            });
            mainThread.submit(bansharing);
        });

        // 이건 밴 해제되었을 때 작동
        Events.on(EventType.PlayerUnbanEvent.class, e -> {
            if (client.activated) client.request(Client.Request.unbanid, null, e.player.uuid + "|<unknown>");
        });

        // 이건 IP 밴이 해제되었을 때 작동
        Events.on(EventType.PlayerIpUnbanEvent.class, e -> {
            if (client.activated) client.request(Client.Request.unbanip, null, "<unknown>|" + e.ip);
        });

        Events.on(EventType.ServerLoadEvent.class, e -> {
            // 업데이트 확인
            if (config.update()) {
                Log.client("client.update-check");
                try {
                    JsonObject json = readJSON(tool.getWebContent("https://api.github.com/repos/kieaer/Essentials/releases/latest")).asObject();

                    for (int a = 0; a < mods.list().size; a++) {
                        if (mods.list().get(a).meta.name.equals("Essentials")) {
                            vars.pluginVersion(mods.list().get(a).meta.version);
                        }
                    }

                    DefaultArtifactVersion latest = new DefaultArtifactVersion(json.getString("tag_name", vars.pluginVersion()));
                    DefaultArtifactVersion current = new DefaultArtifactVersion(vars.pluginVersion());

                    if (latest.compareTo(current) > 0) {
                        Log.client("version-new");
                        Thread t = new Thread(() -> {
                            try {
                                Log.info(new Bundle().get("update-description", json.get("tag_name")));
                                System.out.println(json.getString("body", "No description found."));
                                System.out.println(new Bundle().get("plugin-downloading-standby"));
                                timer.cancel();
                                if (config.serverEnable()) {
                                    try {
                                        for (Server.service ser : server.list) {
                                            ser.interrupt();
                                            ser.os.close();
                                            ser.in.close();
                                            ser.socket.close();
                                            server.list.remove(ser);
                                        }
                                        server.shutdown();
                                    } catch (Exception ignored) {
                                    }
                                }
                                if (config.clientEnable() && client.activated) {
                                    client.request(Client.Request.exit, null, null);
                                }
                                mainThread.shutdown();
                                database.dispose();

                                Tools.URLDownload(new URL(json.get("assets").asArray().get(0).asObject().getString("browser_download_url", null)),
                                        Core.settings.getDataDirectory().child("mods/Essentials.jar").file());
                                Core.app.exit();
                            } catch (Exception ex) {
                                System.out.println("\n" + new Bundle().get("plugin-downloading-fail"));
                                new CrashReport(ex);
                                Core.app.exit();
                            }
                        });
                        t.start();
                    } else if (latest.compareTo(current) == 0) {
                        Log.client("version-current");
                    } else if (latest.compareTo(current) < 0) {
                        Log.client("version-devel");
                    }
                } catch (Exception ex) {
                    new CrashReport(ex);
                }
            } else {
                for (int a = 0; a < mods.list().size; a++) {
                    if (mods.list().get(a).meta.name.equals("Essentials")) {
                        vars.pluginVersion(mods.list().get(a).meta.version);
                        break;
                    }
                }
            }

            // Discord 봇 시작
            if (config.passwordMethod().equals("discord") || config.passwordMethod().equals("mixed")) {
                discord.start();
            }
        });
    }
}
