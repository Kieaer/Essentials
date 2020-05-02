package essentials.internal;

import arc.Core;
import arc.Events;
import essentials.core.player.PlayerData;
import essentials.core.plugin.PluginData;
import essentials.external.DataMigration;
import essentials.external.IpAddressMatcher;
import essentials.network.Client;
import essentials.network.Server;
import mindustry.content.Blocks;
import mindustry.entities.type.Player;
import mindustry.game.Difficulty;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.net.Packets;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.time.LocalTime;
import java.util.Base64;
import java.util.Locale;

import static essentials.Main.*;
import static essentials.external.DriverLoader.URLDownload;
import static mindustry.Vars.*;
import static mindustry.core.NetClient.colorizeName;
import static mindustry.core.NetClient.onSetRules;
import static org.hjson.JsonValue.readJSON;

public class Event {
    Logger log = LoggerFactory.getLogger(Event.class);

    public Event() {
        Events.on(EventType.TapConfigEvent.class, e -> {
            if (e.tile.entity != null && e.tile.entity.block != null && e.player != null && config.alertaction()) {
                for (Player p : playerGroup.all()) {
                    PlayerData playerData = playerDB.get(p.uuid);
                    if (playerData.alert()) {
                        p.sendMessage(new Bundle(playerData.locale()).get("tap-config", e.player.name, e.tile.entity.block.name));
                    }
                }
                if (config.debug())
                    Log.info("anti-grief.build.config", e.player.name, e.tile.block().name, e.tile.x, e.tile.y);
            }
        });

        Events.on(EventType.TapEvent.class, e -> {
            PlayerData playerData = playerDB.get(e.player.uuid);

            if (!playerData.error()) {
                for (PluginData.jumpzone data : pluginData.jumpzone) {
                    int port = data.port;
                    String ip = data.ip;

                    if (e.tile.x > data.getStartTile().x && e.tile.x < data.getFinishTile().x) {
                        if (e.tile.y > data.getStartTile().y && e.tile.y < data.getFinishTile().y) {
                            Log.info("player.jumped", e.player.name, data.ip + ":" + data.port);
                            playerData.connected(false);
                            playerData.connserver("none");
                            Call.onConnect(e.player.con, ip, port);
                        }
                    }
                }
            }
        });

        Events.on(EventType.WithdrawEvent.class, e -> {
            if (e.tile.entity != null && e.player.item().item != null && e.player.name != null && config.antigrief()) {
                for (Player p : playerGroup.all()) {
                    PlayerData playerData = playerDB.get(p.uuid);
                    if (playerData.alert()) {
                        p.sendMessage(new Bundle(playerData.locale()).get("log.withdraw", e.player.name, e.player.item().item.name, e.amount, e.tile.block().name));
                    }
                }
                if (config.debug())
                    Log.info("log.withdraw", e.player.name, e.player.item().item.name, e.amount, e.tile.block().name);
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
            vars.playtime(LocalTime.of(0, 0, 0));

            // 전력 노드 정보 초기화
            pluginData.powerblock.clear();
        });

        Events.on(EventType.PlayerConnect.class, e -> {
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

            if (config.strictname()) {
                if (e.player.name.length() > 32) Call.onKick(e.player.con, "Nickname too long!");
                //if (e.player.name.matches(".*\\[.*].*"))
                //    Call.onKick(e.player.con, "Color tags can't be used for nicknames on this server.");
                if (e.player.name.contains("　"))
                    Call.onKick(e.player.con, "Don't use blank speical charactor nickname!");
                if (e.player.name.contains(" ")) Call.onKick(e.player.con, "Nicknames can't be used on this server!");
                if (e.player.name.replaceAll("\\([.*?]\\)", "").length() == 0)
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
        });

        // 플레이어가 서버에 들어왔을 때
        Events.on(EventType.PlayerJoin.class, e -> {
            vars.addPlayers(e.player);
            e.player.isAdmin = false;
            Thread t = new Thread(() -> {
                Thread.currentThread().setName(e.player.name + " Player Join thread");
                PlayerData playerData = playerDB.load(e.player.uuid);
                Bundle bundle = new Bundle(playerData.locale());

                if (config.loginenable()) {
                    if (config.passwordmethod().equals("mixed")) {
                        if (!playerData.error() && config.autologin()) {
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
                    } else if (config.passwordmethod().equals("discord")) {
                        if (!playerData.error() && config.autologin()) {
                            e.player.sendMessage(bundle.get("account.autologin"));
                            playerCore.load(e.player);
                        } else {
                            String message;
                            Locale language = tool.getGeo(e.player);
                            if (config.passwordmethod().equals("discord")) {
                                message = new Bundle(language).get("system.login.require.discord") + "\n" + config.discordlink();
                            } else {
                                message = new Bundle(language).get("system.login.require.password");
                            }
                            Call.onInfoMessage(e.player.con, message);
                        }
                    } else {
                        if (!playerData.error() && config.autologin()) {
                            e.player.sendMessage(bundle.get("account.autologin"));
                            playerCore.load(e.player);
                        } else {
                            String message;
                            Locale language = tool.getGeo(e.player);
                            if (config.passwordmethod().equals("discord")) {
                                message = new Bundle(language).get("system.login.require.discord") + "\n" + config.discordlink();
                            } else {
                                message = new Bundle(language).get("system.login.require.password");
                            }
                            Call.onInfoMessage(e.player.con, message);
                        }
                    }
                } else {
                    // 로그인 기능이 꺼져있을 때, 바로 계정 등록을 하고 데이터를 로딩함
                    if (!playerData.error() && config.autologin()) {
                        e.player.sendMessage(bundle.get("account.autologin"));
                        playerCore.load(e.player);
                    } else {
                        Locale lc = tool.getGeo(e.player);
                        if (playerDB.register(e.player.name, e.player.uuid, lc.getDisplayCountry(), lc.toString(), lc.getDisplayLanguage(), true, vars.serverIP(), "default", 0L, e.player.name, "none")) {
                            playerCore.load(e.player);
                        } else {
                            Call.onKick(e.player.con, new Bundle().get("plugin-error-kick"));
                        }
                    }
                }

                // VPN을 사용중인지 확인
                if (config.antivpn()) {
                    try {
                        InputStream reader = getClass().getResourceAsStream("/ipv4.txt");
                        BufferedReader br = new BufferedReader(new InputStreamReader(reader));

                        String ip = netServer.admins.getInfo(e.player.uuid).lastIP;
                        String line;
                        while ((line = br.readLine()) != null) {
                            IpAddressMatcher match = new IpAddressMatcher(line);
                            if (match.matches(ip)) {
                                Call.onKick(e.player.con, new Bundle().get("anti-grief.vpn"));
                            }
                        }
                    } catch (IOException ex) {
                        log.warn("VPN File", ex);
                    }
                }

                // PvP 평화시간 설정
                if (config.antirush() && state.rules.pvp && vars.playtime().isBefore(config.antirushtime())) {
                    state.rules.playerDamageMultiplier = 0f;
                    state.rules.playerHealthMultiplier = 0.001f;
                    Call.onSetRules(state.rules);
                    vars.setPvPPeace(true);
                }

                // 플레이어 인원별 난이도 설정
                if (config.autodifficulty()) {
                    int total = playerGroup.size();
                    if (config.difficultyeasy() >= total) {
                        state.rules.waveSpacing = Difficulty.valueOf("easy").waveTime * 60 * 60 * 2;
                        //tool.sendMessageAll("system.difficulty.easy");
                    } else if (config.difficultynormal() == total) {
                        state.rules.waveSpacing = Difficulty.valueOf("normal").waveTime * 60 * 60 * 2;
                        //tool.sendMessageAll("system.difficulty.normal");
                    } else if (config.difficultyhard() == total) {
                        state.rules.waveSpacing = Difficulty.valueOf("hard").waveTime * 60 * 60 * 2;
                        //tool.sendMessageAll("system.difficulty.hard");
                    } else if (config.difficultyinsane() <= total) {
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
            PlayerData playerData = playerDB.get(e.player.uuid);
            Bundle bundle = new Bundle(playerData.locale());

            if (!playerData.error()) {
                String check = String.valueOf(e.message.charAt(0));
                // 명령어인지 확인
                if (!check.equals("/")) {
                    if (e.message.equals("y") && vote.size != 0) {
                        // 투표가 진행중일때
                        if (vote.get(0).getVoted().contains(e.player.uuid)) {
                            e.player.sendMessage(bundle.get("vote.already-voted"));
                        } else {
                            vote.get(0).set(e.player.uuid);
                        }
                    } else {
                        if (!playerData.mute()) {
                            if (perm.permission_user.get(playerData.uuid()).asObject().get("prefix") != null) {
                                if (!playerData.crosschat())
                                    Call.sendMessage(perm.permission_user.get(playerData.uuid()).asObject().get("prefix").asString().replace("%1", colorizeName(e.player.id, e.player.name)).replaceAll("%2", e.message));
                            } else {
                                if (!playerData.crosschat())
                                    Call.sendMessage("[orange]" + colorizeName(e.player.id, e.player.name) + "[orange] >[white] " + e.message);
                            }

                            // 서버간 대화기능 작동
                            if (playerData.crosschat()) {
                                if (config.clienten()) {
                                    client.request(Client.Request.chat, e.player, e.message);
                                } else if (config.serverenable()) {
                                    // 메세지를 모든 클라이언트에게 전송함
                                    String msg = "[" + e.player.name + "]: " + e.message;
                                    try {
                                        for (Server.service ser : server.list) {
                                            ser.os.writeBytes(Base64.getEncoder().encodeToString(tool.encrypt(msg, ser.spec, ser.cipher)));
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
                        Log.info("<&y" + e.player.name + ": &lm" + e.message + "&lg>");
                    }
                }

                // 마지막 대화 데이터를 DB에 저장함
                playerData.lastchat(e.message);

                // 번역
                if (config.translate()) {
                    try {
                        for (int i = 0; i < playerGroup.size(); i++) {
                            Player p = playerGroup.all().get(i);
                            PlayerData target = playerDB.get(p.uuid);
                            if (!target.error()) {
                                String[] support = {"ko", "en", "zh-CN", "zh-TW", "es", "fr", "vi", "th", "id"};
                                String language = target.language();
                                String orignal = playerData.language();
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
                                                .header("X-NCP-APIGW-API-KEY-ID", config.translateid())
                                                .header("X-NCP-APIGW-API-KEY", config.translatepw())
                                                .data("source", playerData.language())
                                                .data("target", target.language())
                                                .data("text", e.message)
                                                .ignoreContentType(true)
                                                .followRedirects(true)
                                                .execute()
                                                .body();
                                        JsonObject object = readJSON(response).asObject();
                                        if (object.get("error") != null) {
                                            String result = object.get("message").asObject().get("result").asObject().getString("translatedText", "none");
                                            if (target.translate()) {
                                                p.sendMessage("[green]" + e.player.name + "[orange]: [white]" + result);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception ex) {
                        new CrashReport(ex);
                    }
                }
            }
        });

        // 플레이어가 블럭을 건설했을 때
        Events.on(EventType.BlockBuildEndEvent.class, e -> {
            if (e.player == null) return; // 만약 건설자가 드론일경우
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
                if (e.tile.entity.block == Blocks.message) {
                    pluginData.messagemonitor.add(new PluginData.messagemonitor(e.tile));
                }

                // 플레이어가 토륨 원자로를 만들었을 때, 감시를 위해 그 원자로의 위치를 저장함.
                if (e.tile.entity.block == Blocks.thoriumReactor) {
                    pluginData.nukeposition.add(e.tile);
                    pluginData.nukedata.add(e.tile);
                }

                if (config.debug() && config.antigrief()) {
                    Log.info("anti-grief.build.finish", e.player.name, e.tile.block().name, e.tile.x, e.tile.y);
                }
            }
        });

        // 플레이어가 블럭을 뽀갰을 때
        Events.on(EventType.BuildSelectEvent.class, e -> {
            if (e.builder instanceof Player && e.builder.buildRequest() != null && !e.builder.buildRequest().block.name.matches(".*build.*") && e.tile.block() != Blocks.air) {
                if (e.breaking) {
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
                    if (config.explimit()) {
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
                if (config.debug() && config.antigrief()) {
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
                if (config.banshare() && config.clienten()) {
                    client.request(Client.Request.bansync, null, null);
                }

                for (Player player : playerGroup.all()) {
                    player.sendMessage(new Bundle(playerDB.get(player.uuid).locale()).get("player.banned", e.player.name));
                    if (netServer.admins.isIDBanned(player.uuid)) {
                        player.con.kick(Packets.KickReason.banned);
                    }
                }
            });
            mainThread.submit(bansharing);
        });

        // 이건 IP 밴당했을때 작동
        Events.on(EventType.PlayerIpBanEvent.class, e -> {
            Thread bansharing = new Thread(() -> {
                if (config.banshare() && client.activated) {
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
            // 예전 DB 변환
            if (config.olddbmigration()) new DataMigration().MigrateDB();
            // 업데이트 확인
            if (config.isUpdate()) {
                Log.client("client.update-check");
                try {
                    JsonObject json = readJSON(Jsoup.connect("https://api.github.com/repos/kieaer/Essentials/releases/latest").ignoreContentType(true).execute().body()).asObject();

                    for (int a = 0; a < mods.list().size; a++) {
                        if (mods.list().get(a).meta.name.equals("Essentials")) {
                            vars.pluginVersion(mods.list().get(a).meta.version);
                        }
                    }

                    DefaultArtifactVersion latest = new DefaultArtifactVersion(json.getString("tag_name", vars.pluginVersion()));
                    DefaultArtifactVersion current = new DefaultArtifactVersion(vars.pluginVersion());

                    if (latest.compareTo(current) > 0) {
                        Log.client("version-new");
                        net.dispose();
                        Thread t = new Thread(() -> {
                            try {
                                Log.info(new Bundle().get("update-description", json.get("tag_name")));
                                System.out.println(json.getString("body", "No description found."));
                                System.out.println(new Bundle().get("plugin-downloading-standby"));
                                timer.cancel();
                                if (config.serverenable()) {
                                    try {
                                        for (Server.service ser : server.list) {
                                            ser.interrupt();
                                            ser.os.close();
                                            ser.in.close();
                                            ser.socket.close();
                                            server.list.remove(ser);
                                        }
                                        server.stop();
                                    } catch (Exception ignored) {
                                    }
                                }
                                if (config.clienten() && client.activated) {
                                    client.request(Client.Request.exit, null, null);
                                }
                                mainThread.shutdown();
                                database.dispose();

                                URLDownload(new URL(json.get("assets").asArray().get(0).asObject().getString("browser_download_url", null)),
                                        Core.settings.getDataDirectory().child("mods/Essentials.jar").file());
                                Core.app.exit();
                                System.exit(0);
                            } catch (Exception ex) {
                                System.out.println("\n" + new Bundle().get("plugin-downloading-fail"));
                                new CrashReport(ex);
                                Core.app.exit();
                                System.exit(0);
                            }
                        });
                        t.start();
                    } else if (latest.compareTo(current) == 0) {
                        Log.client("version-current");
                    } else if (latest.compareTo(current) < 0) {
                        Log.client("version-devel");
                    }
                } catch (SocketTimeoutException i) {
                    log.warn("SocketTimeOut");
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
            if (config.passwordmethod().equals("discord") || config.passwordmethod().equals("mixed")) {
                discord.start();
            }

            // 채팅 포맷 변경
            netServer.admins.addChatFilter((player, text) -> null);
        });
    }
}
