package essentials.core;

import mindustry.entities.type.Player;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static essentials.Global.*;
import static essentials.core.PlayerDB.getData;
import static mindustry.Vars.playerGroup;

public class Translate {
    private static URL url;
    private static HttpURLConnection c;
    private static BufferedReader in;

    public void main(Player player, String message) {
        // 클라이언트 ID/PW 값이 비었는지 확인
        if(config.getClientId() != null && config.getClientSecret() != null) {
            if (!config.getClientId().equals("") && !config.getClientSecret().equals("")) {
                Thread t = new Thread(() -> {
                    try {
                        JSONObject orignaldata = getData(player.uuid);
                        for (int i = 0; i < playerGroup.size(); i++) {
                            Player p = playerGroup.all().get(i);
                            if (isNocore(player)) {
                                JSONObject data = getData(p.uuid);
                                String[] support = {"ko", "en", "zh-CN", "zh-TW", "es", "fr", "vi", "th", "id"};
                                String language = data.getString("language");
                                String orignal = orignaldata.getString("language");
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
                                                .data("source", orignaldata.getString("language"))
                                                .data("target", data.getString("language"))
                                                .data("text", message)
                                                .ignoreContentType(true)
                                                .followRedirects(true)
                                                .execute()
                                                .body();
                                        JSONTokener token = new JSONTokener(response);
                                        JSONObject object = new JSONObject(token);
                                        if(!object.has("error")) {
                                            String result = object.getJSONObject("message").getJSONObject("result").getString("translatedText");
                                            if (data.getBoolean("translate")) {
                                                p.sendMessage("[green]" + player.name + "[orange]: [white]" + result);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        printStackTrace(e);
                    }
                });
                t.start();
            }
        }
    }
}