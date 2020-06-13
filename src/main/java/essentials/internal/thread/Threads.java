package essentials.internal.thread;

import arc.struct.Array;
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

import static essentials.Main.pluginData;
import static essentials.Main.tool;
import static mindustry.Vars.*;

public class Threads implements Runnable {
    @Override
    public void run() {
        Thread.currentThread().setName("Essential thread");
        while (!Thread.currentThread().isInterrupted()) {
            try {
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

                final double[] ping = {0.000};
                Array<String> memory = new Array<>();
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
                                ping[0] = ping[0] + Double.parseDouble("0." + result.ping);
                                memory.add("[yellow]" + result.players + "[] Players///" + x + "///" + y);
                            } else {
                                ping[0] = ping[0] + 1.000;
                                memory.add("[scarlet]Offline///" + x + "///" + y);
                            }
                            memory.add(value.description + "///" + x + "///" + (tile.drawy() - margin));
                        });
                    }
                }
                for (String m : memory) {
                    String[] a = m.split("///");
                    Call.onLabel(a[0], ((float) ping[0]) + 3f, Float.parseFloat(a[1]), Float.parseFloat(a[2]));
                }

                TimeUnit.SECONDS.sleep(3);
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
