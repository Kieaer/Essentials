package essentials.utils;

import essentials.Global;
import io.anuke.arc.Core;
import org.json.JSONArray;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class Permission {
    public static JSONObject json;

    public void main(){
        if(Core.settings.getDataDirectory().child("mods/Essentials/permission.yml").exists()) {
            Yaml yaml = new Yaml();
            Map<Object, Object> map = yaml.load(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/permission.yml").readString()));
            json = new JSONObject(map);
            Global.log(json.toString());
            Iterator i = json.keys();
            ArrayList<Object> keys = new ArrayList<>();
            while(i.hasNext()){
                String b = i.next().toString();
                JSONArray array = json.getJSONObject(b).getJSONArray("permission");
                String inheritance = json.getJSONObject(b).getString("inheritance");
                if(!inheritance.equals("none")){
                    JSONArray inheritjson = json.getJSONObject(inheritance).getJSONArray("permission");
                }
                for(int a=0;a<array.length();a++){
                    Global.log(b+"/"+array.get(a));
                }
            }
        }
    }
}