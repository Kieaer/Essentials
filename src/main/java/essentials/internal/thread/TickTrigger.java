package essentials.internal.thread;

import arc.Core;
import arc.Events;
import arc.struct.ArrayMap;
import arc.struct.ObjectMap;
import arc.util.Strings;
import essentials.core.player.PlayerData;
import essentials.core.plugin.PluginData;
import essentials.feature.Exp;
import essentials.internal.Bundle;
import essentials.internal.CrashReport;
import essentials.internal.Log;
import mindustry.content.Blocks;
import mindustry.core.GameState;
import mindustry.entities.type.Player;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.type.Item;
import mindustry.type.ItemType;
import mindustry.world.Tile;
import mindustry.world.blocks.logic.MessageBlock;
import org.hjson.JsonObject;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static essentials.Main.*;
import static mindustry.Vars.*;
import static mindustry.core.NetClient.onSetRules;

public class TickTrigger {
    private final ArrayMap<Item, Integer> ores = new ArrayMap<>();
    private final SecureRandom random = new SecureRandom();

    public TickTrigger() {
        Events.on(EventType.ServerLoadEvent.class, () -> {
            for (Item item : content.items()) {
                if (item.type == ItemType.material) {
                    ores.put(item, 0);
                }
            }
        });

        Events.on(EventType.Trigger.update, new Runnable() {
            int tick = 0;
            final ObjectMap<String, Integer> resources = new ObjectMap<>();

            public String writeOreStatus(Item item, int orignal) {
                int val;
                String color;
                if (state.teams.get(Team.sharded).cores.first().items.has(item)) {
                    val = orignal - ores.get(item);
                    if (val > 0) {
                        color = "[green]+";
                    } else if (val < 0) {
                        color = "[red]-";
                    } else {
                        color = "[yellow]";
                    }
                    ores.put(item, orignal);
                    return "[]" + item.name + ": " + color + val + "/s\n";
                }

                return null;
            }

            @Override
            public void run() {
                if (tick < 86400) {
                    tick++;
                } else {
                    tick = 0;
                }

                if (state.is(GameState.State.playing)) {
                    if (config.border()) {
                        for (Player p : playerGroup.all()) {
                            if (p.x > world.width() * 8 || p.x < 0 || p.y > world.height() * 8 || p.y < 0)
                                Call.onPlayerDeath(p);
                        }
                    }
                }

                // 1초마다 실행
                if ((tick % 60) == 0) {
                    // 서버 켜진시간 카운트
                    vars.uptime(vars.uptime() + 1);

                    // 데이터 저장
                    JsonObject json = new JsonObject();
                    json.add("servername", Core.settings.getString("servername"));
                    root.child("data/data.json").writeString(json.toString());

                    // 현재 서버 이름에다가 클라이언트 서버에 대한 인원 새기기
                    // new changename().start();

                    // 임시로 밴당한 유저 감시
                    for (int a = 0; a < pluginData.banned.size; a++) {
                        LocalDateTime time = LocalDateTime.now();
                        if (time.isAfter(pluginData.banned.get(a).getTime())) {
                            pluginData.banned.remove(a);
                            netServer.admins.unbanPlayerID(pluginData.banned.get(a).uuid);
                            Log.info("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + pluginData.banned.get(a).name + "/" + pluginData.banned.get(a).uuid + " player unbanned!");
                            break;
                        }
                    }

                    // 맵이 돌아가고 있을 때
                    if (state.is(GameState.State.playing)) {
                        // 서버간 이동 패드에 플레이어가 있는지 확인
                        // new jumpzone().start();

                        // 맵 플탐 카운트
                        vars.playtime(vars.playtime() + 1);

                        // PvP 평화시간 카운트
                        if (config.antiRush() && state.rules.pvp && vars.playtime() < config.antiRushtime() && vars.isPvPPeace()) {
                            state.rules.playerDamageMultiplier = 0.66f;
                            state.rules.playerHealthMultiplier = 0.8f;
                            onSetRules(state.rules);
                            for (Player p : playerGroup.all()) {
                                player.sendMessage(new Bundle(playerDB.get(p.uuid).locale()).get("pvp-peacetime"));
                                player.kill();
                            }
                            vars.setPvPPeace(false);
                        }

                        // 모든 클라이언트 서버에 대한 인원 총합 카운트
                        for (int a = 0; a < pluginData.warptotals.size; a++) {
                            int result = 0;
                            for (PluginData.warpcount value : pluginData.warpcounts) result = result + value.players;

                            String str = String.valueOf(result);
                            // TODO 인원 카운트 다시 만들기
                            int[] digits = new int[str.length()];
                            for (int b = 0; b < str.length(); b++) digits[b] = str.charAt(b) - '0';

                            Tile tile = pluginData.warptotals.get(a).getTile();
                            if (pluginData.warptotals.get(a).totalplayers != result) {
                                if (pluginData.warptotals.get(a).numbersize != digits.length) {
                                    for (int px = 0; px < 3; px++) {
                                        for (int py = 0; py < 5; py++) {
                                            Call.onDeconstructFinish(world.tile(tile.x + 4 + px, tile.y + py), Blocks.air, 0);
                                        }
                                    }
                                }
                            }

                            tool.setTileText(tile, Blocks.copperWall, String.valueOf(result));

                            pluginData.warptotals.set(a, new PluginData.warptotal(world.getMap().name(), tile, result, digits.length));
                        }

                        // 플레이어 플탐 카운트 및 잠수확인
                        for (Player p : playerGroup.all()) {
                            PlayerData target = playerDB.get(p.uuid);
                            boolean kick = false;

                            if (target.login()) {
                                // Exp 계산
                                target.exp(target.exp() + (random.nextInt(50)));

                                // 잠수 및 플레이 시간 계산
                                target.playtime(target.playtime() + 1);
                                if (target.tilex() == p.tileX() && target.tiley() == p.tileY()) {
                                    target.afk(target.afk() + 1);
                                    if (config.afktime() != 0L && config.afktime() < target.afk()) {
                                        kick = true;
                                    }
                                } else {
                                    target.afk(0L);
                                }
                                target.tilex(p.tileX());
                                target.tiley(p.tileY());

                                if (!state.rules.editor) new Exp(target);
                                if (kick) Call.onKick(p.con, "AFK");
                            }
                        }

                        // 메세지 블럭 감시
                        for (PluginData.messagemonitor data : pluginData.messagemonitor) {
                            String msg;
                            MessageBlock.MessageBlockEntity entity;
                            try {
                                entity = (MessageBlock.MessageBlockEntity) data.tile.entity;
                                msg = entity.message;
                            } catch (NullPointerException e) {
                                pluginData.messagemonitor.remove(data);
                                return;
                            }

                            if (msg.equals("powerblock")) {
                                for (int rot = 0; rot < 4; rot++) {
                                    if (entity.tile.link().getNearby(rot).entity != null) {
                                        pluginData.powerblock.add(new PluginData.powerblock(entity.tile, entity.tile.getNearby(rot).link(), rot));
                                        break;
                                    }
                                }
                                pluginData.messagemonitor.remove(data);
                                break;
                            } else if (msg.contains("warp")) {
                                pluginData.messagewarp.add(new PluginData.messagewarp(data.tile, msg));
                                pluginData.messagemonitor.remove(data);
                                break;
                            } else if (msg.equals("scancore")) {
                                pluginData.scancore.add(data.tile);
                                pluginData.messagemonitor.remove(data);
                                break;
                            }
                        }

                        // 서버간 이동 영역에 플레이어가 있는지 확인
                        for (PluginData.warpzone value : pluginData.warpzones) {
                            if (!value.touch) {
                                for (int ix = 0; ix < playerGroup.size(); ix++) {
                                    Player player = playerGroup.all().get(ix);
                                    if (player.tileX() > value.startx && player.tileX() < value.finishx) {
                                        if (player.tileY() > value.starty && player.tileY() < value.finishy) {
                                            String resultIP = value.ip;
                                            int port = 6567;
                                            if (resultIP.contains(":") && Strings.canParsePostiveInt(resultIP.split(":")[1])) {
                                                String[] temp = resultIP.split(":");
                                                resultIP = temp[0];
                                                port = Integer.parseInt(temp[1]);
                                            }
                                            Log.info("player.warped", player.name, resultIP + ":" + port);
                                            Call.onConnect(player.con, resultIP, port);
                                        }
                                    }
                                }
                            }
                        }

                        // 메세지 블럭에 있는 자원 소모량 감시
                        if (playerGroup.size() > 0) {
                            StringBuilder items = new StringBuilder();
                            for (Item item : content.items()) {
                                if (item.type == ItemType.material) {
                                    Player player = playerGroup.all().get(random.nextInt(playerGroup.size()));
                                    Team team;
                                    if (player != null && !state.teams.get(player.getTeam()).cores.isEmpty()) {
                                        team = player.getTeam();
                                    } else {
                                        return;
                                    }

                                    int amount = state.teams.get(team).cores.first().items.get(item);
                                    if (state.teams.get(team).cores.first().items.has(item))
                                        items.append(writeOreStatus(item, amount));
                                }
                            }

                            for (Tile data : pluginData.scancore) {
                                if (data.block() != Blocks.message) {
                                    data.remove();
                                    break;
                                }
                                Call.setMessageBlockText(null, data, items.toString());
                            }
                        }

                        // 메세지 블럭에 있는 근처 전력 계산
                        for (PluginData.powerblock data : pluginData.powerblock) {
                            if (data.messageblock.block() != Blocks.message) {
                                pluginData.powerblock.remove(data);
                                return;
                            }

                            String arrow;
                            switch (data.rotate) {
                                case 0:
                                    arrow = "⇨";
                                    break;
                                case 1:
                                    arrow = "⇧";
                                    break;
                                case 2:
                                    arrow = "⇦";
                                    break;
                                case 3:
                                    arrow = "⇩";
                                    break;
                                default:
                                    arrow = "null";
                            }

                            float current;
                            float product;
                            float using;
                            try {
                                current = data.tile.link().entity.power.graph.getPowerBalance() * 60;
                                using = data.tile.link().entity.power.graph.getPowerNeeded() * 60;
                                product = data.tile.link().entity.power.graph.getPowerProduced() * 60;
                            } catch (Exception e) {
                                pluginData.powerblock.remove(data);
                                Call.setMessageBlockText(null, data.messageblock, arrow + " Tile doesn't have powers!");
                                return;
                            }

                            String text = "[accent]" + arrow + "[] Power status [accent]" + arrow + "[]\n" +
                                    "Current: [sky]" + Math.round(current) + "/s[]\n" +
                                    "Using: [red]" + Math.round(using) + "[]/s\n" +
                                    "Production: [green]" + Math.round(product) + "/s[]";
                            Call.setMessageBlockText(null, data.messageblock, text);
                        }
                    }
                }

                // 1.5초마다 실행
                if ((tick % 90) == 0) {
                    if (state.is(GameState.State.playing) && config.scanResource() && state.rules.waves && playerGroup.size() > 0) {
                        for (Item item : content.items()) {
                            if (item.type == ItemType.material) {
                                Player player = playerGroup.all().get(random.nextInt(playerGroup.size()));
                                Team team;
                                if (player != null && state.teams.get(player.getTeam()).cores.isEmpty()) {
                                    team = player.getTeam();
                                } else {
                                    return;
                                }

                                if (state.teams.get(team).cores.first().items.has(item)) {
                                    int cur = state.teams.get(team).cores.first().items.get(item);
                                    if (resources.get(item.name) != null) {
                                        if ((cur - resources.get(item.name)) <= -55) {
                                            StringBuilder using = new StringBuilder();
                                            for (Player p : playerGroup) {
                                                if (p.buildRequest() != null) {
                                                    for (int c = 0; c < p.buildRequest().block.requirements.length; c++) {
                                                        if (p.buildRequest().block.requirements[c].item.name.equals(item.name)) {
                                                            using.append(p.name).append(", ");
                                                        }
                                                    }
                                                }
                                            }
                                            if (using.length() > 2)
                                                tool.sendMessageAll("resource-fast-use", item.name, using);
                                        }
                                    } else {
                                        resources.put(item.name, cur);
                                    }
                                }
                            }
                        }
                    }
                }

                // 3초마다
                if ((tick % 180) == 0) {
                    try {
                        playerDB.saveAll();
                        pluginData.saveAll();
                    } catch (Exception e) {
                        new CrashReport(e);
                    }
                }

                // 1분마다
                if ((tick % 3600) == 0) {
                    for (Player p : playerGroup.all()) {
                        PlayerData playerData = playerDB.get(p.uuid);
                        if (playerData.error()) {
                            String message;
                            if (playerData.locale() == null) {
                                playerData.locale(tool.getGeo(p));
                            }

                            if (config.passwordMethod().equals("discord")) {
                                message = new Bundle(playerData.locale()).get("system.login.require.discord") + "\n" + config.discordLink();
                            } else {
                                message = new Bundle(playerData.locale()).get("system.login.require.password");
                            }
                            p.sendMessage(message);
                        }
                    }
                }
            }
        });
    }
}
