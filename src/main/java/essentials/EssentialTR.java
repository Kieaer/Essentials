package essentials;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.type.Player;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static essentials.EssentialConfig.clientId;
import static essentials.EssentialConfig.clientSecret;
import static essentials.EssentialPlayer.getData;
import static essentials.Global.printStackTrace;
import static io.anuke.mindustry.Vars.playerGroup;

class EssentialTR {
    private static URL url;
    private static HttpURLConnection c;
    private static BufferedReader in;

    public static void main(Player player, String message) {
        if (!clientId.equals("") && !clientSecret.equals("")) {
            Thread t = new Thread(() -> {
                try {
                    JSONObject orignaldata = getData(player.uuid);
                    for (int i = 0; i < playerGroup.size(); i++) {
                        Player p = playerGroup.all().get(i);
                        if (!Vars.state.teams.get(player.getTeam()).cores.isEmpty()) {
                            JSONObject data = getData(p.uuid);
                            String[] support = {"ko", "en", "zh-CN", "zh-TW", "es", "fr", "vi", "th", "id"};
                            String language = data.getString("language");
                            String orignal = orignaldata.getString("language");
                            if(!language.equals(orignal)){
                                boolean found = false;
                                for (String s : support) {
                                    if (orignal.equals(s)) {
                                        found = true;
                                        break;
                                    }
                                }
                                if(found){
                                    url = new URL("https://openapi.naver.com/v1/papago/n2mt");
                                    c = (HttpURLConnection) url.openConnection();
                                    c.setRequestMethod("POST");
                                    c.setRequestProperty("X-NCP-APIGW-API-KEY-ID", clientId);
                                    c.setRequestProperty("X-NCP-APIGW-API-KEY", clientSecret);
                                    String postParams;
                                    if(orignal.equals("zh")){
                                        if(data.getString("language").equals("zh")){
                                            postParams = "source="+orignaldata.getString("language")+"&target=zh-"+data.getString("language")+"&text=" + message;
                                        } else {
                                            postParams = "source=zh-"+orignaldata.getString("language")+"&target="+data.getString("language")+"&text=" + message;
                                        }
                                    } else {
                                        postParams = "source="+orignaldata.getString("language")+"&target="+data.getString("language")+"&text=" + message;
                                    }
                                    c.setDoOutput(true);
                                    DataOutputStream wr = new DataOutputStream(c.getOutputStream());
                                    wr.writeBytes(postParams);
                                    wr.flush();
                                    wr.close();
                                    int response = c.getResponseCode();
                                    if(response == 200) {
                                        in = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
                                    } else {
                                        in = new BufferedReader(new InputStreamReader(c.getErrorStream(), StandardCharsets.UTF_8));
                                    }
                                    StringBuilder rb = new StringBuilder();
                                    String inputLine;
                                    while ((inputLine = in.readLine()) != null) {
                                        rb.append(inputLine);
                                    }
                                    in.close();

                                    if(response == 200){
                                        JSONTokener token = new JSONTokener(rb.toString());
                                        JSONObject object = new JSONObject(token);
                                        String result = object.getJSONObject("message").getJSONObject("result").getString("translatedText");
                                        if (data.getBoolean("translate")) {
                                            p.sendMessage("[green]"+player.name + "[orange]: [white]" + result);
                                        }
                                    } else {
                                        Global.logw(rb.toString());
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