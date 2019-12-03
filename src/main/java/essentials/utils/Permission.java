package essentials.utils;

import io.anuke.arc.Core;
import io.anuke.mindustry.entities.type.Player;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.util.Iterator;
import java.util.Map;

import static essentials.core.PlayerDB.getData;
import static essentials.core.PlayerDB.writeData;

public class Permission {
    public static JSONObject result;

    public void main(){
        if(Core.settings.getDataDirectory().child("mods/Essentials/permission.yml").exists()) {
            Yaml yaml = new Yaml();
            Map<Object, Object> map = yaml.load(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/permission.yml").readString()));
            result = new JSONObject(map);
            Iterator i = result.keys();
            while(i.hasNext()){
                String b = i.next().toString();
                if(result.getJSONObject(b).has("inheritance")) {
                    String inheritance = result.getJSONObject(b).getString("inheritance");
                    while(result.getJSONObject(inheritance).has("inheritance")){
                        for(int a=0;a<result.getJSONObject(inheritance).getJSONArray("permission").length();a++){
                            result.getJSONObject(b).getJSONArray("permission").put(result.getJSONObject(inheritance).getJSONArray("permission").get(a));
                        }
                        inheritance = result.getJSONObject(inheritance).getString("inheritance");
                    }
                }
            }
        }
    }

    public static void setAdmin(Player player){
        JSONObject db = getData(player.uuid);
        String perm = db.getString("permission");
        if(result.getJSONObject(perm).getBoolean("admin")){
            player.isAdmin = true;
            writeData("UPDATE players SET isAdmin = '1' WHERE uuid = '"+player.uuid+"'");
        }
    }
}