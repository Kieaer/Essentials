package essentials.internal.thread;

import arc.graphics.Color;
import arc.struct.Array;
import essentials.core.plugin.PluginData;
import essentials.external.PingHost;
import essentials.internal.Log;
import mindustry.content.Fx;
import mindustry.core.GameState;
import mindustry.gen.Call;
import mindustry.world.Tile;

import java.util.concurrent.TimeUnit;

import static essentials.Main.config;
import static essentials.Main.pluginData;
import static java.lang.Thread.sleep;
import static mindustry.Vars.state;
import static mindustry.Vars.world;

public class JumpBorder implements Runnable {
    public int length = 0;
    public Array<Thread> thread = new Array<>();

    @Override
    public void run() {
        main();
    }

    public void main() {
        this.length = pluginData.jumpzone.size;

        for (PluginData.jumpzone data : pluginData.jumpzone) {
            Thread t = new Thread(() -> {
                while (true) {
                    String ip = data.ip;
                    if (state.is(GameState.State.playing)) {
                        new PingHost(ip, data.port, result -> {
                            try {
                                if (result.name != null) {
                                    int size = data.getFinishTile().x - data.getStartTile().x;

                                    for (int x = 0; x < size; x++) {
                                        Tile tile = world.tile(data.getStartTile().x + x, data.getStartTile().y);
                                        Call.onEffect(Fx.placeBlock, tile.getX(), tile.getY(), 0, Color.orange);
                                        sleep(96);
                                    }
                                    for (int y = 0; y < size; y++) {
                                        Tile tile = world.tile(data.getFinishTile().x, data.getStartTile().y + y);
                                        Call.onEffect(Fx.placeBlock, tile.getX(), tile.getY(), 0, Color.orange);
                                        sleep(96);
                                    }
                                    for (int x = 0; x < size; x++) {
                                        Tile tile = world.tile(data.getFinishTile().x - x, data.getFinishTile().y);
                                        Call.onEffect(Fx.placeBlock, tile.getX(), tile.getY(), 0, Color.orange);
                                        sleep(96);
                                    }
                                    for (int y = 0; y < size; y++) {
                                        Tile tile = world.tile(data.getStartTile().x, data.getFinishTile().y - y);
                                        Call.onEffect(Fx.placeBlock, tile.getX(), tile.getY(), 0, Color.orange);
                                        sleep(96);
                                    }
                                    if (size < 5) sleep(2000);
                                } else {
                                    if (config.debug)
                                        Log.info("jump zone " + ip + " offline! After 1 minute, try to connect again.");
                                    TimeUnit.MINUTES.sleep(1);
                                }
                            } catch (InterruptedException ignored) {
                            }
                        });
                    } else {
                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch (InterruptedException ignored) {
                            return;
                        }
                    }
                }
            });
            thread.add(t);
            t.start();
        }
    }
}
