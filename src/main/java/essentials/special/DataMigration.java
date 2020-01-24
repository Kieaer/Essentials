package essentials.special;

import arc.Core;
import arc.files.Fi;
import essentials.PluginData;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.hjson.Stringify;

import java.time.LocalDateTime;

import static essentials.Global.dbundle;
import static essentials.Global.log;
import static essentials.PluginData.saveall;

public class DataMigration {
    Fi root = Core.settings.getDataDirectory().child("mods/Essentials/");

    public DataMigration(){
        if(root.child("config.yml").exists()) {
            log("log", dbundle("data-migration"));
            String condata = root.child("config.yml").readString();
            root.child("config.hjson").writeString("{\n"+condata+"\n}".replace("colornick update interval","cupdatei"));
            root.child("config.yml").delete();

            if (root.child("BlockReqExp.yml").exists()) move("BlockReqExp.yml");
            if (root.child("Exp.yml").exists()) move("Exp.yml");
            if (root.child("permission.yml").exists()) {
                String data = root.child("permission.yml").readString();
                try {
                    root.child("permission.hjson").writeString(JsonValue.readJSON("{\n" + data + "\n}").toString(Stringify.HJSON));
                } catch (Exception e) {
                    e.printStackTrace();
                    Core.app.exit();
                }
                root.child("permission.yml").delete();
            }
            if (root.child("data/data.json").exists()) {
                String data = root.child("data/data.json").readString();
                JsonObject value = JsonValue.readJSON(data).asObject();
                JsonObject arrays = value.get("banned").asObject();
                saveall();
                for (int a = 0; a < arrays.size(); a++) {
                    LocalDateTime date = LocalDateTime.parse(arrays.get("date").asString());
                    String name = arrays.get("name").asString();
                    String uuid = arrays.get("uuid").asString();
                    PluginData.banned.add(new PluginData.banned(date, name, uuid));
                }
                // 서버간 이동 데이터들은 변환하지 않음
                root.child("data/data.json").delete();
            }
            log("log", dbundle("success"));
        }
    }

    public void move(String path){
        String data = root.child(path+".yml").readString();
        root.child(path+".hjson").writeString("{\n"+data+"\n}");
        root.child(path+".yml").delete();
    }
}
