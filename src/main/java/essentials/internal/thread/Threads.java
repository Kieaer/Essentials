package essentials.internal.thread;

import essentials.core.player.PlayerData;
import essentials.core.plugin.PluginData;
import essentials.external.PingHost;
import essentials.internal.Bundle;
import essentials.internal.CrashReport;
import mindustry.content.Blocks;
import mindustry.core.GameState;
import mindustry.entities.type.Player;
import mindustry.gen.Call;
import mindustry.world.Tile;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static essentials.Main.*;
import static mindustry.Vars.*;

public class Threads implements Runnable {
    int delay = 0;

    @Override
    public void run() {
        Thread.currentThread().setName("Essential thread");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // 로그인 요청 알림
                if (delay == 60) {
                    for (int a = 0; a < playerGroup.size(); a++) {
                        Player p = playerGroup.all().get(a);
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
                    delay = 0;
                } else {
                    delay++;
                }

                // 외부 서버 플레이어 인원 - 메세지 블럭
                for (int a = 0; a < pluginData.messagewarp.size; a++) {
                    if (state.is(GameState.State.playing)) {
                        if (pluginData.messagewarp.get(a).tile.entity.block != Blocks.message) {
                            pluginData.messagewarp.remove(a);
                            break;
                        }
                        Call.setMessageBlockText(null, pluginData.messagewarp.get(a).tile, "[green]Working...");

                        String[] arr = pluginData.messagewarp.get(a).message.split(" ");
                        String ip = arr[0];
                        int port = 6567;
                        if (arr.length == 2) {
                            port = Integer.parseInt(arr[1]);
                        }

                        int fa = a;
                        new PingHost(ip, port, result -> Call.setMessageBlockText(null, pluginData.messagewarp.get(fa).tile, result != null ? "[green]" + result.players + " Players in this server." : "[scarlet]Server offline"));
                    }
                }

                // 서버 인원 확인
                for (int i = 0; i < pluginData.warpcounts.size; i++) {
                    int i2 = i;
                    PluginData.warpcount value = pluginData.warpcounts.get(i);

                    new PingHost(value.ip, value.port, result -> {
                        if (result.name != null) {
                            String str = String.valueOf(result.players);
                            int[] digits = new int[str.length()];
                            for (int a = 0; a < str.length(); a++) digits[a] = str.charAt(a) - '0';

                            Tile tile = value.getTile();
                            if (value.players != result.players) {
                                if (value.numbersize != digits.length) {
                                    for (int px = 0; px < 3; px++) {
                                        for (int py = 0; py < 5; py++) {
                                            Call.onDeconstructFinish(world.tile(tile.x + 4 + px, tile.y + py), Blocks.air, 0);
                                        }
                                    }
                                }
                            }
                            tool.setTileText(tile, Blocks.copperWall, str);
                            // i 번째 server ip, 포트, x좌표, y좌표, 플레이어 인원, 플레이어 인원 길이
                            pluginData.warpcounts.set(i2, new PluginData.warpcount(world.getMap().name(), value.getTile(), value.ip, value.port, result.players, digits.length));
                        } else {
                            tool.setTileText(value.getTile(), Blocks.copperWall, "no");
                        }
                    });
                }

                // 3초마다 실행
                if ((delay % 3) == 0) {
                    try {
                        playerDB.saveAll();
                        pluginData.saveAll();
                    } catch (Exception e) {
                        new CrashReport(e);
                    }

                    for (int a = 0; a < pluginData.warpblocks.size; a++) {
                        PluginData.warpblock value = pluginData.warpblocks.get(a);
                        Tile tile = world.tile(value.tilex, value.tiley);
                        if (tile.block() == Blocks.air) {
                            pluginData.warpblocks.remove(a);
                        } else {
                            new PingHost(value.ip, value.port, result -> {
                                float margin = 0f;
                                boolean isDup = false;
                                float x = tile.drawx();

                                switch (value.size) {
                                    case 1:
                                        margin = 8f;
                                        break;
                                    case 2:
                                        margin = 16f;
                                        x = tile.drawx() - 4f;
                                        isDup = true;
                                        break;
                                    case 3:
                                        margin = 16f;
                                        break;
                                    case 4:
                                        x = tile.drawx() - 4f;
                                        margin = 24f;
                                        isDup = true;
                                        break;
                                }

                                float y = tile.drawy() + (isDup ? margin - 8 : margin);
                                if (result.name != null) {
                                    Call.onLabel("[yellow]" + result.players + "[] Players", 3f, x, y);
                                } else {
                                    Call.onLabel("[scarlet]Offline", 3f, x, y);
                                }
                                Call.onLabel(value.description, 3f, x, tile.drawy() - margin);
                            });
                        }
                    }
                }

                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                for (Player p : playerGroup.all())
                    Call.onKick(p.con, new Bundle(Locale.ENGLISH).get("plugin-error-kick"));
                new CrashReport(e);
            }
        }
    }
}
