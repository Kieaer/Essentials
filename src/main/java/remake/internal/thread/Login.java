package remake.internal.thread;

import mindustry.Vars;
import mindustry.entities.type.Player;
import mindustry.gen.Call;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.jsoup.Jsoup;
import remake.internal.Bundle;
import remake.internal.CrashReport;

import java.net.UnknownHostException;
import java.util.Locale;
import java.util.TimerTask;

import static mindustry.Vars.playerGroup;
import static remake.Main.*;

public class Login extends TimerTask {
    @Override
    public void run() {
        Thread.currentThread().setName("Login alert thread");
        if (playerGroup.size() > 0) {
            for (int i = 0; i < playerGroup.size(); i++) {
                Player player = playerGroup.all().get(i);
                if (Vars.state.teams.get(player.getTeam()).cores.isEmpty()) {
                    try {
                        String message;
                        String json = Jsoup.connect("http://ipapi.co/" + Vars.netServer.admins.getInfo(player.uuid).lastIP + "/json").ignoreContentType(true).execute().body();
                        JsonObject result = JsonValue.readJSON(json).asObject();
                        Locale language = tool.TextToLocale(result.getString("languages", locale.toString()));
                        if (config.passwordmethod.equals("discord")) {
                            message = new Bundle(language).get("login-require-discord") + "\n" + config.discordlink;
                        } else {
                            message = new Bundle(language).get("login-require-password");
                        }
                        player.sendMessage(message);
                    } catch (UnknownHostException e) {
                        Bundle bundle = new Bundle();
                        Call.onKick(player.con, bundle.get("plugin-error-kick"));
                    } catch (Exception e) {
                        new CrashReport(e);
                    }
                }
            }
        }
    }
}
