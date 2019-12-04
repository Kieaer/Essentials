package essentials.utils;

import io.anuke.arc.Core;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.util.Iterator;
import java.util.Map;

public class Permission {
    public static JSONObject permission;

    public void main(){
        if(Core.settings.getDataDirectory().child("mods/Essentials/permission.yml").exists()) {
            Yaml yaml = new Yaml();
            Map<Object, Object> map = yaml.load(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/permission.yml").readString()));
            permission = new JSONObject(map);
            Iterator i = permission.keys();
            while(i.hasNext()){
                String b = i.next().toString();
                if(permission.getJSONObject(b).has("inheritance")) {
                    String inheritance = permission.getJSONObject(b).getString("inheritance");
                    while(permission.getJSONObject(inheritance).has("inheritance")){
                        for(int a = 0; a< permission.getJSONObject(inheritance).getJSONArray("permission").length(); a++){
                            permission.getJSONObject(b).getJSONArray("permission").put(permission.getJSONObject(inheritance).getJSONArray("permission").get(a));
                        }
                        inheritance = permission.getJSONObject(inheritance).getString("inheritance");
                    }
                }
            }
        }
    }
}