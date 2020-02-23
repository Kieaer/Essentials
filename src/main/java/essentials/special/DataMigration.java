package essentials.special;

import arc.Core;
import arc.files.Fi;
import essentials.PluginData;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.time.LocalDateTime;

import static essentials.Global.config;
import static essentials.Global.nbundle;
import static essentials.PluginData.saveall;

public class DataMigration {
    Fi root = Core.settings.getDataDirectory().child("mods/Essentials/");

    public DataMigration(){
        if(root.child("config.yml").exists()) {
            System.out.print("\r"+nbundle("data-migration"));
            String condata = root.child("config.yml").readString();
            condata = condata.replace("colornick update interval","cupdatei").replace("null","none");
            root.child("config.hjson").writeString("{\n"+condata+"\n}");
            root.child("config.yml").delete();

            if (root.child("BlockReqExp.yml").exists()) move("BlockReqExp");
            if (root.child("Exp.yml").exists()) move("Exp");
            if (root.child("permission.yml").exists()) {
                root.child("permission.yml").delete();
                config.validfile();
            }
            if (root.child("data/data.json").exists()) {
                String data = root.child("data/data.json").readString();
                JsonObject value = JsonValue.readJSON(data).asObject();
                if(value.get("banned") != null) {
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
            }
            System.out.print("\r"+nbundle("data-migration")+" "+nbundle("success")+"\n");
        }
    }

    public void move(String path){
        String data = root.child(path+".yml").readString();
        root.child(path+".hjson").writeString("{\n"+data+"\n}");
        root.child(path+".yml").delete();
    }
}
