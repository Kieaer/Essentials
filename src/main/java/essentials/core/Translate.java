package essentials.core;

import essentials.Global;
import essentials.utils.Config;
import io.anuke.mindustry.entities.type.Player;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static essentials.Global.isNocore;
import static essentials.Global.printStackTrace;
import static essentials.core.PlayerDB.getData;
import static io.anuke.mindustry.Vars.playerGroup;

public class Translate {
    public Config config = new Config();
    private static URL url;
    private static HttpURLConnection c;
    private static BufferedReader in;

    public void main(Player player, String message) {
        // 클라이언트 ID/PW 값이 비었는지 확인
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
                            if(!language.equals(orignal)){
                                boolean found = false;
                                for (String s : support) {
                                    if (orignal.equals(s)) {
                                        found = true;
                                        break;
                                    }
                                }
                                if(found){
                                    url = new URL("https://naveropenapi.apigw.ntruss.com/nmt/v1/translation");
                                    c = (HttpURLConnection) url.openConnection();
                                    c.setRequestMethod("POST");
                                    c.setRequestProperty("X-NCP-APIGW-API-KEY-ID", config.getClientId());
                                    c.setRequestProperty("X-NCP-APIGW-API-KEY", config.getClientSecret());
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
                                    } else if (response != 400){
                                        Global.nwarn(rb.toString());
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