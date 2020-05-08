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
                if (delay == 20) {
                    for (int a = 0; a < playerGroup.size(); a++) {
                        Player p = playerGroup.all().get(a);
                        PlayerData playerData = playerDB.get(p.uuid);
                        if (playerData.error()) {
                            String message;
                            if (config.passwordMethod().equals("discord")) {
                                message = new Bundle(Locale.US).get("system.login.require.discord") + "\n" + config.discordLink();
                            } else {
                                message = new Bundle(Locale.US).get("system.login.require.password");
                            }
                            p.sendMessage(message);
                        }
                    }
                    delay = 0;
                } else {
                    delay++;
                }

                // 외부 서버 플레이어 인원 - 메세지 블럭
                for (int a = 0; a < pluginData.messagejump.size; a++) {
                    if (state.is(GameState.State.playing)) {
                        if (pluginData.messagejump.get(a).tile.entity.block != Blocks.message) {
                            pluginData.messagejump.remove(a);
                            break;
                        }
                        Call.setMessageBlockText(null, pluginData.messagejump.get(a).tile, "[green]Working...");

                        String[] arr = pluginData.messagejump.get(a).message.split(" ");
                        String ip = arr[1];
                        int port = Integer.parseInt(arr[2]);

                        int fa = a;
                        new PingHost(ip, port, result -> Call.setMessageBlockText(null, pluginData.messagejump.get(fa).tile, result != null ? "[green]" + result.players + " Players in this server." : "[scarlet]Server offline"));
                    }
                }

                // 서버 인원 확인
                for (int i = 0; i < pluginData.jumpcount.size; i++) {
                    int i2 = i;
                    PluginData.jumpcount value = pluginData.jumpcount.get(i);

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
                            pluginData.jumpcount.set(i2, new PluginData.jumpcount(value.getTile(), value.ip, value.port, result.players, digits.length));
                        } else {
                            tool.setTileText(value.getTile(), Blocks.copperWall, "no");
                        }
                    });
                }

                perm.isUse = false;

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
