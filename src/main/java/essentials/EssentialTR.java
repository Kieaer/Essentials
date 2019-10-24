package essentials;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.type.Player;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static essentials.EssentialConfig.apikey;
import static essentials.EssentialPlayer.getData;
import static essentials.Global.printStackTrace;
import static io.anuke.mindustry.Vars.playerGroup;

class EssentialTR {
    private static URL url;
    private static HttpURLConnection c;
    private static BufferedReader in;

    public static void main(Player player, String message) {
        if (!apikey.equals("")) {
            Thread t = new Thread(() -> {
                try {
                    JSONObject orignaldata = getData(player.uuid);
                    for (int i = 0; i < playerGroup.size(); i++) {
                        Player p = playerGroup.all().get(i);
                        if (!Vars.state.teams.get(player.getTeam()).cores.isEmpty()) {
                            JSONObject data = getData(p.uuid);
                            if(!data.getString("language").equals(orignaldata.getString("language"))){
                                url = new URL("https://translation.googleapis.com/language/translate/v2/?q=" + message + "&source=" + orignaldata.getString("language") + "&target=" + data.getString("language") + "&key=" + apikey);
                                c = (HttpURLConnection) url.openConnection();
                                c.setRequestMethod("GET");
                                in = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
                                StringBuilder rb = new StringBuilder();
                                String inputLine;
                                while ((inputLine = in.readLine()) != null) {
                                    rb.append(inputLine);
                                }
                                in.close();
                                int response = c.getResponseCode();
                                if(response == 200){
                                    JSONTokener token = new JSONTokener(rb.toString());
                                    JSONObject object = new JSONObject(token);
                                    String result = object.getJSONObject("data").getJSONArray("translations").getJSONObject(0).getString("translatedText");
                                    if (data.getBoolean("translate")) {
                                        p.sendMessage("[green]"+player.name + "[orange]: [white]" + result);
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