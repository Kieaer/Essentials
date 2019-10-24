package essentials;

import io.anuke.mindustry.entities.type.Player;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static essentials.EssentialConfig.apikey;
import static essentials.EssentialPlayer.getData;
import static essentials.Global.printStackTrace;
import static io.anuke.mindustry.Vars.playerGroup;

class EssentialTR {
    private static URL url;
    private static HttpURLConnection c;
    private static BufferedReader in;

    public void main(Player player, String message) {
        if (apikey.equals("")) {
            Thread t = new Thread(() -> {
                try {
                    url = new URL("https://translation.googleapis.com/language/translate/v2/detect/?q=" + message + "&key=" + apikey);
                    c = (HttpURLConnection) url.openConnection();
                    c.setRequestMethod("GET");
                    in = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
                    String inputLine;
                    StringBuilder rs = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        rs.append(inputLine);
                    }
                    in.close();

                    int responsecode = c.getResponseCode();

                    if(responsecode == 200){
                        String langcode = rs.toString();
                        for (int i = 0; i < playerGroup.size(); i++) {
                            Player p = playerGroup.all().get(i);
                            if (!Objects.equals(p.name, player.name)) {
                                JSONObject data = getData(p.uuid);
                                url = new URL("https://translation.googleapis.com/language/translate/v2/?q=&source=" + langcode + "&target=" + data.getString("language") + "&key=" + apikey);
                                c = (HttpURLConnection) url.openConnection();
                                c.setRequestMethod("GET");
                                in = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
                                StringBuilder rb = new StringBuilder();
                                while ((inputLine = in.readLine()) != null) {
                                    rb.append(inputLine);
                                }
                                in.close();

                                JSONTokener token = new JSONTokener(rb.toString());
                                JSONObject object = new JSONObject(token);
                                String result = object.getJSONObject("data").getJSONArray("translations").getJSONObject(0).getString("translatedText");
                                p.sendMessage(player.name + ": " + result);
                            }
                        }
                    } else {
                        Global.logw(c.getResponseMessage());
                    }
                } catch (Exception e) {
                    printStackTrace(e);
                }
            });
            t.start();
        }
    }
}