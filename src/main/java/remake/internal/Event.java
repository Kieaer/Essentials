package remake.internal;

import arc.Core;
import arc.Events;
import arc.util.Strings;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import mindustry.content.Blocks;
import mindustry.entities.type.Player;
import mindustry.game.Difficulty;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.net.Packets;
import mindustry.world.Tile;
import mindustry.world.blocks.power.NuclearReactor;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalTime;
import java.util.Locale;

import static java.lang.Thread.sleep;
import static mindustry.Vars.*;
import static mindustry.core.NetClient.colorizeName;
import static mindustry.core.NetClient.onSetRules;

public class Event {
    public Event() {
        Events.on(EventType.TapConfigEvent.class, e -> {
            if (e.tile.entity != null && e.tile.entity.block != null && e.player != null && e.player.name != null && config.isAlertaction() && config.isAlertaction()) {
                allsendMessage("tap-config", e.player.name, e.tile.entity.block.name);
                if (config.isDebug())
                    log(Global.LogType.log, "antigrief-build-config", e.player.name, e.tile.block().name, e.tile.x, e.tile.y);
            }
        });

        Events.on(EventType.TapEvent.class, e -> {
            if (isLogin(e.player)) {
                for (PluginData.jumpzone data : data.jumpzone) {
                    int port = 6567;
                    String ip = data.ip;
                    if (data.ip.contains(":") && Strings.canParsePostiveInt(data.ip.split(":")[1])) {
                        ip = data.ip.split(":")[0];
                        port = Strings.parseInt(data.ip.split(":")[1]);
                    }

                    if (e.tile.x > data.getStartTile().x && e.tile.x < data.getFinishTile().x) {
                        if (e.tile.y > data.getStartTile().y && e.tile.y < data.getFinishTile().y) {
                            log(LogType.log, "player-jumped", e.player.name, data.ip);
                            PlayerDB.PlayerData player = PlayerData(e.player.uuid);
                            player.connected = false;
                            player.connserver = "none";
                            PlayerDataSave(player);
                            Call.onConnect(e.player.con, ip, port);
                        }
                    }
                }
            }
        });

        Events.on(EventType.WithdrawEvent.class, e -> {
            if (e.tile.entity != null && e.player.item().item != null && e.player.name != null && config.isAntigrief()) {
                allsendMessage("log-withdraw", e.player.name, e.player.item().item.name, e.amount, e.tile.block().name);
                if (config.isDebug())
                    log(LogType.log, "log-withdraw", e.player.name, e.player.item().item.name, e.amount, e.tile.block().name);
                if (state.rules.pvp) {
                    if (e.item.flammability > 0.001f) {
                        e.player.sendMessage(bundle(PlayerData(e.player.uuid).locale, "flammable-disabled"));
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
                        PlayerData target = PlayerData(player.uuid);
                        if (target.isLogin) {
                            if (player.getTeam().name.equals(e.winner.name)) {
                                target.pvpwincount++;
                            } else if (!player.getTeam().name.equals(e.winner.name)) {
                                target.pvplosecount++;
                            }
                            PlayerDataSet(target);
                        }
                    }
                    pvpteam.clear();
                }
            } else if (state.rules.attackMode) {
                for (int i = 0; i < playerGroup.size(); i++) {
                    Player player = playerGroup.all().get(i);
                    PlayerData target = PlayerData(player.uuid);
                    if (target.isLogin) {
                        target.attackclear++;
                        PlayerDataSet(target);
                    }
                }
            }
        });

        // 맵이 불러와졌을 때
        Events.on(EventType.WorldLoadEvent.class, e -> {
            threads.playtime = LocalTime.of(0, 0, 0);

            // 전력 노드 정보 초기화
            data.powerblock.clear();
        });

        Events.on(EventType.PlayerConnect.class, e -> {
            // 닉네임이 블랙리스트에 등록되어 있는지 확인
            for (String s : data.blacklist) {
                if (e.player.name.matches(s)) {
                    try {
                        Locale locale = geolocation(e.player);
                        Call.onKick(e.player.con, nbundle(locale, "nickname-blacklisted-kick"));
                        log(LogType.log, "nickname-blacklisted", e.player.name);
                    } catch (Exception ex) {
                        printError(ex);
                    }
                }
            }

            if (config.isStrictname()) {
                if (e.player.name.length() > 32) Call.onKick(e.player.con, "Nickname too long!");
                if (e.player.name.matches(".*\\[.*].*"))
                    Call.onKick(e.player.con, "Color tags can't be used for nicknames on this server.");
                if (e.player.name.contains("　"))
                    Call.onKick(e.player.con, "Don't use blank speical charactor nickname!");
                if (e.player.name.contains(" ")) Call.onKick(e.player.con, "Nicknames can't be used on this server!");
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

            // 만약 그 특정블록이 토륨 원자로이며, 맵 설정에서 원자로 폭발이 비활성화 되었을 경우
            if (e.tile.block() == Blocks.thoriumReactor && config.isDetectreactor() && !state.rules.reactorExplosions) {
                data.nukeblock.add(new PluginData.nukeblock(e.tile, e.player.name));
                Thread t = new Thread(() -> {
                    try {
                        for (PluginData.nukeblock data : data.nukeblock) {
                            NuclearReactor.NuclearReactorEntity entity = (NuclearReactor.NuclearReactorEntity) data.tile.entity;
                            if (entity.heat >= 0.01) {
                                sleep(50);
                                allsendMessage("detect-thorium");

                                writeLog(LogType.griefer, nbundle("griefer-detect-reactor-log", getTime(), data.name));
                                Call.onTileDestroyed(data.tile);
                            } else {
                                sleep(1950);
                                if (entity.heat >= 0.01) {
                                    allsendMessage("detect-thorium");
                                    writeLog(LogType.griefer, nbundle("griefer-detect-reactor-log", getTime(), data.name));
                                    Call.onTileDestroyed(data.tile);
                                }
                            }
                        }
                    } catch (Exception ex) {
                        printError(ex);
                    }
                });
                t.start();
            }
            if (config.isAlertaction())
                allsendMessage("depositevent", e.player.name, e.player.item().item.name, e.tile.block().name);
        });

        // 플레이어가 서버에 들어왔을 때
        Events.on(EventType.PlayerJoin.class, e -> {
            players.add(e.player);
            e.player.isAdmin = false;

            e.player.kill();
            e.player.setTeam(Team.derelict);

            Thread t = new Thread(() -> {
                Thread.currentThread().setName(e.player.name + " Player Join thread");
                PlayerData playerData = getInfo("uuid", e.player.uuid);
                if (config.isLoginenable() && isNocore(e.player)) {
                    if (config.getPasswordmethod().equals("mixed")) {
                        if (!playerData.error) {
                            if (playerData.udid != 0L) {
                                new Thread(() -> Call.onConnect(e.player.con, hostip, 7060)).start();
                            } else {
                                e.player.sendMessage(bundle(playerData.locale, "autologin"));
                                playerDB.load(e.player, playerData.accountid);
                            }
                        } else {
                            if (playerDB.register(e.player)) {
                                playerDB.load(e.player);
                            } else {
                                Call.onKick(e.player.con, nbundle("plugin-error-kick"));
                            }
                        }
                    } else if (config.getPasswordmethod().equals("discord")) {
                        if (!playerData.error) {
                            e.player.sendMessage(bundle(playerData.locale, "autologin"));
                            playerDB.load(e.player, playerData.accountid);
                        } else {
                            String message;
                            Locale language = geolocation(e.player);
                            if (config.getPasswordmethod().equals("discord")) {
                                message = nbundle(language, "login-require-discord") + "\n" + config.getDiscordLink();
                            } else {
                                message = nbundle(language, "login-require-password");
                            }
                            Call.onInfoMessage(e.player.con, message);
                        }
                    } else {
                        if (!playerData.error) {
                            e.player.sendMessage(bundle(playerData.locale, "autologin"));
                            playerDB.load(e.player);
                        } else {
                            String message;
                            Locale language = geolocation(e.player);
                            if (config.getPasswordmethod().equals("discord")) {
                                message = nbundle(language, "login-require-discord") + "\n" + config.getDiscordLink();
                            } else {
                                message = nbundle(language, "login-require-password");
                            }
                            Call.onInfoMessage(e.player.con, message);
                        }
                    }
                } else {
                    // 로그인 기능이 꺼져있을 때, 바로 계정 등록을 하고 데이터를 로딩함
                    if (!playerData.error) {
                        e.player.sendMessage(bundle(playerData.locale, "autologin"));
                        playerDB.load(e.player);
                    } else {
                        if (playerDB.register(e.player)) {
                            playerDB.load(e.player);
                        } else {
                            Call.onKick(e.player.con, nbundle("plugin-error-kick"));
                        }
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
                                Call.onKick(e.player.con, nbundle("antivpn-kick"));
                            }
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }

                // PvP 평화시간 설정
                if (config.isEnableantirush() && state.rules.pvp && threads.playtime.isBefore(config.getAntirushtime())) {
                    state.rules.playerDamageMultiplier = 0f;
                    state.rules.playerHealthMultiplier = 0.001f;
                    onSetRules(state.rules);
                    threads.PvPPeace = true;
                }

                // 플레이어 인원별 난이도 설정
                if (config.isAutodifficulty()) {
                    int total = playerGroup.size();
                    if (config.getEasy() >= total) {
                        state.rules.waveSpacing = Difficulty.valueOf("easy").waveTime * 60 * 60 * 2;
                        allsendMessage("difficulty-easy");
                    } else if (config.getNormal() == total) {
                        state.rules.waveSpacing = Difficulty.valueOf("normal").waveTime * 60 * 60 * 2;
                        allsendMessage("difficulty-normal");
                    } else if (config.getHard() == total) {
                        state.rules.waveSpacing = Difficulty.valueOf("hard").waveTime * 60 * 60 * 2;
                        allsendMessage("difficulty-hard");
                    } else if (config.getInsane() <= total) {
                        state.rules.waveSpacing = Difficulty.valueOf("insane").waveTime * 60 * 60 * 2;
                        allsendMessage("difficulty-insane");
                    }
                    onSetRules(state.rules);
                }
            });
            config.executorService.submit(t);
        });

        // 플레이어가 서버에서 탈주했을 때
        Events.on(EventType.PlayerLeave.class, e -> {
            players.remove(e.player);
            PlayerData player = PlayerData(e.player.uuid);
            if (player.isLogin) {
                player.connected = false;
                player.connserver = "none";
                if (state.rules.pvp && !state.gameOver) player.pvpbreakout++;
                if (config.getPasswordmethod().equals("discord")) {
                    PlayerDataSaveUUID(player, player.accountid);
                } else {
                    PlayerDataSave(player);
                }
            } else {
                PlayerDataRemove(player);
            }
        });

        // 플레이어가 수다떨었을 때
        Events.on(EventType.PlayerChatEvent.class, e -> {
            if (isLogin(e.player)) {
                PlayerData playerData = PlayerData(e.player.uuid);
                String check = String.valueOf(e.message.charAt(0));
                // 명령어인지 확인
                if (!check.equals("/")) {
                    if (e.message.matches("(.*쌍[\\S\\s]{0,2}(년|놈).*)|(.*(씨|시)[\\S\\s]{0,2}(벌|빨|발|바).*)|(.*장[\\S\\s]{0,2}애.*)|(.*(병|븅)[\\S\\s]{0,2}(신|쉰|싄).*)|(.*(좆|존|좃)[\\S\\s]{0,2}(같|되|는|나).*)|(.*(개|게)[\\S\\s]{0,2}(같|갓|새|세|쉐).*)|(.*(걸|느)[\\S\\s]{0,2}(레|금).*)|(.*(꼬|꽂|고)[\\S\\s]{0,2}(추|츄).*)|(.*(니|너)[\\S\\s]{0,2}(어|엄|엠|애|m|M).*)|(.*(노)[\\S\\s]{0,1}(애|앰).*)|(.*(섹|쎅)[\\S\\s]{0,2}(스|s|쓰).*)|(ㅅㅂ|ㅄ|ㄷㅊ)|(.*(섹|쎅)[\\S\\s]{0,2}(스|s|쓰).*)|(.*s[\\S\\s]{0,1}e[\\S\\s]{0,1}x.*)")) {
                        Call.onKick(e.player.con, nbundle(playerData.locale, "kick-swear"));
                    } else if (e.message.equals("y") && isvoting) {
                        // 투표가 진행중일때
                        if (Threads.Vote.list.contains(e.player.uuid)) {
                            e.player.sendMessage(bundle(playerData.locale, "vote-already"));
                        } else {
                            Threads.Vote.list.add(e.player.uuid);
                            int current = Threads.Vote.list.size();
                            if (Threads.Vote.require - current <= 0) {
                                Threads.Vote.cancel();
                                return;
                            }
                            for (Player others : playerGroup.all()) {
                                if (isLogin(others))
                                    others.sendMessage(bundle(PlayerData(others.uuid).locale, "vote-current", current, Threads.Vote.require - current));
                            }
                        }
                    } else {
                        if (!playerData.mute) {
                            if (perm.permission.get(playerData.permission).asObject().get("prefix") != null) {
                                if (!playerData.crosschat)
                                    Call.sendMessage(perm.permission.get(playerData.permission).asObject().get("prefix").asString().replace("%1", colorizeName(e.player.id, e.player.name)).replaceAll("%2", e.message));
                            } else {
                                if (!playerData.crosschat)
                                    Call.sendMessage("[orange]" + colorizeName(e.player.id, e.player.name) + "[orange] :[white] " + e.message);
                            }

                            // 서버간 대화기능 작동
                            if (playerData.crosschat) {
                                if (config.isClientenable()) {
                                    client.request(Client.Request.chat, e.player, e.message);
                                } else if (config.isServerenable()) {
                                    // 메세지를 모든 클라이언트에게 전송함
                                    String msg = "[" + e.player.name + "]: " + e.message;
                                    try {
                                        for (Server.Service ser : Server.list) {
                                            ser.os.writeBytes(Base64.encode(encrypt(msg, ser.spec, ser.cipher)));
                                            ser.os.flush();
                                        }
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                } else if (!config.isClientenable() && !config.isServerenable()) {
                                    e.player.sendMessage(bundle(playerData.locale, "no-any-network"));
                                    playerData.crosschat = false;
                                }
                            }
                        }
                        arc.util.Log.info("<&y{0}: &lm{1}&lg>", e.player.name, e.message);
                    }
                }

                // 마지막 대화 데이터를 DB에 저장함
                playerData.lastchat = e.message;

                // 번역
                if (config.isEnableTranslate()) {
                    try {
                        for (int i = 0; i < playerGroup.size(); i++) {
                            Player p = playerGroup.all().get(i);
                            if (!isNocore(p)) {
                                PlayerData target = PlayerData(p.uuid);
                                String[] support = {"ko", "en", "zh-CN", "zh-TW", "es", "fr", "vi", "th", "id"};
                                String language = target.language;
                                String orignal = playerData.language;
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
                                                .data("source", playerData.language)
                                                .data("target", target.language)
                                                .data("text", e.message)
                                                .ignoreContentType(true)
                                                .followRedirects(true)
                                                .execute()
                                                .body();
                                        JsonObject object = JsonValue.readJSON(response).asObject();
                                        if (object.get("error") != null) {
                                            String result = object.get("message").asObject().get("result").asObject().getString("translatedText", "none");
                                            if (target.translate) {
                                                p.sendMessage("[green]" + e.player.name + "[orange]: [white]" + result);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception ex) {
                        printError(ex);
                    }
                }

                PlayerDataSet(playerData);
            }
        });

        // 플레이어가 블럭을 건설했을 때
        Events.on(EventType.BlockBuildEndEvent.class, e -> {
            if (!e.breaking && e.player != null && e.player.buildRequest() != null && !isNocore(e.player) && e.tile != null && e.player.buildRequest() != null) {
                PlayerData target = PlayerData(e.player.uuid);
                String name = e.tile.block().name;
                try {
                    JsonObject obj = JsonValue.readHjson(root.child("Exp.hjson").reader()).asObject();
                    int blockexp = obj.getInt(name, 0);

                    target.lastplacename = e.tile.block().name;
                    target.placecount++;
                    target.exp = target.exp + blockexp;
                    if (e.player.buildRequest().block == Blocks.thoriumReactor) target.reactorcount++;
                } catch (Exception ex) {
                    printError(ex);
                }

                target.grief_build_count++;
                target.grief_tilelist.add(new short[]{e.tile.x, e.tile.y});

                // 메세지 블럭을 설치했을 경우, 해당 블럭을 감시하기 위해 위치를 저장함.
                if (e.tile.entity.block == Blocks.message) {
                    data.messagemonitor.add(new PluginData.messagemonitor(e.tile));
                }

                // 플레이어가 토륨 원자로를 만들었을 때, 감시를 위해 그 원자로의 위치를 저장함.
                if (e.tile.entity.block == Blocks.thoriumReactor) {
                    data.nukeposition.add(e.tile);
                    data.nukedata.add(e.tile);
                }

                if (config.isDebug() && config.isAntigrief()) {
                    log(LogType.log, "antigrief-build-finish", e.player.name, e.tile.block().name, e.tile.x, e.tile.y);
                }

                // 필터 아트 감지
                int sorter_count = 0;
                //int conveyor_count = 0;
                for (short[] t : target.grief_tilelist) {
                    Tile tile = world.tile(t[0], t[1]);
                    if (tile != null && tile.block() != null) {
                        if (tile.block() == Blocks.sorter || tile.block() == Blocks.invertedSorter) sorter_count++;
                    }
                    //if(tile.entity.block == Blocks.conveyor || tile.entity.block == Blocks.armoredConveyor || tile.entity.block == Blocks.titaniumConveyor) conveyor_count++;
                }
                if (sorter_count > 20) {
                    for (short[] t : target.grief_tilelist) {
                        Tile tile = world.tile(t[0], t[1]);
                        if (tile != null && tile.entity != null && tile.entity.block != null) {
                            if (tile.entity.block == Blocks.sorter || tile.entity.block == Blocks.invertedSorter) {
                                Call.onDeconstructFinish(tile, Blocks.air, e.player.id);
                            }
                        }
                    }
                    target.grief_tilelist.clear();
                }

                PlayerDataSet(target);
            }
        });

        // 플레이어가 블럭을 뽀갰을 때
        Events.on(EventType.BuildSelectEvent.class, e -> {
            if (e.builder instanceof Player && e.builder.buildRequest() != null && !e.builder.buildRequest().block.name.matches(".*build.*") && e.tile.block() != Blocks.air) {
                if (e.breaking) {
                    PlayerData target = PlayerData(((Player) e.builder).uuid);
                    String name = e.tile.block().name;
                    try {
                        JsonObject obj = JsonValue.readHjson(root.child("Exp.hjson").reader()).asObject();
                        int blockexp = obj.getInt(name, 0);

                        target.lastbreakname = e.tile.block().name;
                        target.breakcount++;
                        target.exp = target.exp + blockexp;
                    } catch (Exception ex) {
                        printError(ex);
                        Call.onKick(((Player) e.builder).con, nbundle(target.locale, "not-logged"));
                    }

                    // 메세지 블럭을 파괴했을 때, 위치가 저장된 데이터를 삭제함
                    if (e.builder.buildRequest().block == Blocks.message) {
                        try {
                            for (int i = 0; i < data.powerblock.size(); i++) {
                                if (data.powerblock.get(i).tile == e.tile) {
                                    data.powerblock.remove(i);
                                    break;
                                }
                            }
                        } catch (Exception ex) {
                            printError(ex);
                        }
                    }

                    // Exp Playing Game (EPG)
                    if (config.isExplimit()) {
                        int level = target.level;
                        try {
                            JsonObject obj = JsonValue.readHjson(root.child("Exp.hjson").reader()).asObject();
                            if (obj.get(name) != null) {
                                int blockreqlevel = obj.getInt(name, 999);
                                if (level < blockreqlevel) {
                                    Call.onDeconstructFinish(e.tile, e.tile.block(), ((Player) e.builder).id);
                                    ((Player) e.builder).sendMessage(nbundle(PlayerData(((Player) e.builder).uuid).locale, "epg-block-require", name, blockreqlevel));
                                }
                            } else {
                                log(LogType.error, "epg-block-not-valid", name);
                            }
                        } catch (Exception ex) {
                            printError(ex);
                        }
                    }

                    /*if(e.builder.buildRequest().block == Blocks.conveyor || e.builder.buildRequest().block == Blocks.armoredConveyor || e.builder.buildRequest().block == Blocks.titaniumConveyor){
                        for(int a=0;a<conveyor.size();a++){
                            if(conveyor.get(a) == e.tile){
                                conveyor.remove(a);
                                break;
                            }
                        }
                    }*/

                    target.grief_destory_count++;
                    // Call.sendMessage(String.valueOf(target.grief_destory_count));
                    // if (target.grief_destory_count > 30) nlog(LogType.log, target.name + " 가 블럭을 빛의 속도로 파괴하고 있습니다.");
                    PlayerDataSet(target);
                }
                if (config.isDebug() && config.isAntigrief()) {
                    log(LogType.log, "antigrief-destroy", ((Player) e.builder).name, e.tile.block().name, e.tile.x, e.tile.y);
                }
            }
        });

        // 유닛을 박살냈을 때
        Events.on(EventType.UnitDestroyEvent.class, e -> {
            // 뒤진(?) 유닛이 플레이어일때
            if (e.unit instanceof Player) {
                Player player = (Player) e.unit;
                PlayerData target = PlayerData(player.uuid);
                if (!state.teams.get(player.getTeam()).cores.isEmpty()) {
                    target.deathcount++;
                    PlayerDataSet(target);
                }
            }

            // 터진 유닛수만큼 카운트해줌
            if (playerGroup != null && playerGroup.size() > 0) {
                for (int i = 0; i < playerGroup.size(); i++) {
                    Player player = playerGroup.all().get(i);
                    PlayerData target = PlayerData(player.uuid);
                    if (!state.teams.get(player.getTeam()).cores.isEmpty()) {
                        target.killcount++;
                        PlayerDataSet(target);
                    }
                }
            }
        });

        // 플레이어가 밴당했을 때 공유기능 작동
        Events.on(EventType.PlayerBanEvent.class, e -> {
            Thread bansharing = new Thread(() -> {
                if (config.isBanshare() && config.isClientenable()) {
                    client.request(Client.Request.bansync, null, null);
                }

                for (Player player : playerGroup.all()) {
                    player.sendMessage(bundle(PlayerData(player.uuid).locale, "player-banned", e.player.name));
                    if (netServer.admins.isIDBanned(player.uuid)) {
                        player.con.kick(Packets.KickReason.banned);
                    }
                }
            });
            config.executorService.submit(bansharing);
        });

        // 이건 IP 밴당했을때 작동
        Events.on(EventType.PlayerIpBanEvent.class, e -> {
            Thread bansharing = new Thread(() -> {
                if (config.isBanshare() && config.isClientenable()) {
                    client.request(Client.Request.bansync, null, null);
                }
            });
            config.executorService.submit(bansharing);
        });

        // 이건 밴 해제되었을 때 작동
        Events.on(EventType.PlayerUnbanEvent.class, e -> {
            if (server_active) {
                client.request(Client.Request.unbanid, null, e.player.uuid + "|<unknown>");
            }
        });

        // 이건 IP 밴이 해제되었을 때 작동
        Events.on(EventType.PlayerIpUnbanEvent.class, e -> {
            if (server_active) {
                client.request(Client.Request.unbanip, null, "<unknown>|" + e.ip);
            }
        });

        Events.on(EventType.ServerLoadEvent.class, e -> {
            // 예전 DB 변환
            if (config.isOldDBMigration()) dataMigration.MigrateDB();
            // 업데이트 확인
            if (config.isUpdate()) {
                log(Global.LogType.client, "client-checking-version");
                try {
                    JsonObject json = JsonValue.readJSON(Jsoup.connect("https://api.github.com/repos/kieaer/Essentials/releases/latest").ignoreContentType(true).execute().body()).asObject();

                    for (int a = 0; a < mods.list().size; a++) {
                        if (mods.list().get(a).meta.name.equals("Essentials")) {
                            plugin_version = mods.list().get(a).meta.version;
                        }
                    }

                    DefaultArtifactVersion latest = new DefaultArtifactVersion(json.getString("tag_name", plugin_version));
                    DefaultArtifactVersion current = new DefaultArtifactVersion(plugin_version);

                    if (latest.compareTo(current) > 0) {
                        log(Global.LogType.client, "version-new");
                        net.dispose();
                        Thread t = new Thread(() -> {
                            try {
                                log(Global.LogType.log, nbundle("update-description", json.get("tag_name")));
                                System.out.println(json.getString("body", "No description found."));
                                System.out.println(nbundle("plugin-downloading-standby"));
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
                                        Server.serverSocket.close();
                                        server.interrupt();
                                    } catch (Exception ignored) {
                                    }
                                }
                                if (config.isClientenable() && server_active) {
                                    client.request(Client.Request.exit, null, null);
                                }
                                config.executorService.shutdown();
                                closeconnect();

                                URLDownload(new URL(json.get("assets").asArray().get(0).asObject().getString("browser_download_url", null)),
                                        Core.settings.getDataDirectory().child("mods/Essentials.jar").file(),
                                        nbundle("plugin-downloading"),
                                        nbundle("plugin-downloading-done"), null);
                                Core.app.exit();
                                System.exit(0);
                            } catch (Exception ex) {
                                System.out.println("\n" + nbundle("plugin-downloading-fail"));
                                printError(ex);
                                Core.app.exit();
                                System.exit(0);
                            }
                        });
                        t.start();
                    } else if (latest.compareTo(current) == 0) {
                        log(LogType.client, "version-current");
                    } else if (latest.compareTo(current) < 0) {
                        log(LogType.client, "version-devel");
                    }
                } catch (Exception ex) {
                    printError(ex);
                }
            }

            // Discord 봇 시작
            if (config.getPasswordmethod().equals("discord") || config.getPasswordmethod().equals("mixed")) {
                discord = new Discord();
                discord.start();
            }

            // 채팅 포맷 변경
            netServer.admins.addChatFilter((player, text) -> null);
        });
    }
}
