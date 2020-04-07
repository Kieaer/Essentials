package essentials.internal.thread;

import essentials.core.player.PlayerData;
import essentials.external.PingHost;
import essentials.internal.Bundle;
import essentials.internal.CrashReport;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.core.GameState;
import mindustry.entities.type.Player;
import mindustry.gen.Call;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static essentials.Main.*;
import static mindustry.Vars.playerGroup;
import static mindustry.Vars.state;

public class Threads implements Runnable {
    List<Thread> zone_border = new ArrayList<>();

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // 로그인 요청 알림
                for (int a = 0; a < playerGroup.size(); a++) {
                    Player p = playerGroup.all().get(a);
                    PlayerData playerData = playerDB.get(p.uuid);
                    if (playerData.error) {
                        String message;
                        String json = Jsoup.connect("http://ipapi.co/" + Vars.netServer.admins.getInfo(p.uuid).lastIP + "/json").ignoreContentType(true).execute().body();
                        JsonObject result = JsonValue.readJSON(json).asObject();
                        Locale language = tool.TextToLocale(result.getString("languages", locale.toString()));
                        if (config.passwordmethod.equals("discord")) {
                            message = new Bundle(language).get("login-require-discord") + "\n" + config.discordlink;
                        } else {
                            message = new Bundle(language).get("login-require-password");
                        }
                        p.sendMessage(message);
                    }
                }

                // 외부 서버 플레이어 인원 - 메세지 블럭
                for (int a = 0; a < pluginData.messagejump.size(); a++) {
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
