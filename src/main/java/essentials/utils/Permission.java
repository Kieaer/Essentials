package essentials.utils;

import arc.Core;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;

import static essentials.Global.nlog;
import static essentials.Global.printError;

public class Permission {
    public static JsonObject permission;

    public Permission(){
        if(Core.settings.getDataDirectory().child("mods/Essentials/permission.yml").exists()) {
            try {
                ObjectMapper yamlread = new ObjectMapper(new YAMLFactory());
                Object obj = yamlread.readValue(Core.settings.getDataDirectory().child("mods/Essentials/permission.yml").readString(), Object.class);
                ObjectMapper jsonwrite = new ObjectMapper();
                permission = JsonParser.object().from(jsonwrite.writeValueAsString(obj));
                for (String b : permission.keySet()) {
                    nlog("debug", "target: " + b);
                    if (permission.getObject(b).has("inheritance")) {
                        String inheritance = permission.getObject(b).getString("inheritance");
                        nlog("debug", "target inheritance: " + b + "/" + inheritance);
                        while (permission.getObject(inheritance).has("inheritance")) {
                            for (int a = 0; a < permission.getObject(inheritance).getArray("permission").size(); a++) {
                                permission.getObject(b).getArray("permission").add(permission.getObject(inheritance).getArray("permission").get(a));
                                nlog("debug", "target inheritance add: " + b + "/" + inheritance + "/" + permission.getObject(inheritance).getArray("permission").get(a));
                            }
                            inheritance = permission.getObject(inheritance).getString("inheritance");
                        }
                        if (!permission.getObject(inheritance).has("inheritance")) {
                            for (int a = 0; a < permission.getObject(inheritance).getArray("permission").size(); a++) {
                                permission.getObject(b).getArray("permission").add(permission.getObject(inheritance).getArray("permission").get(a));
                                nlog("debug", "target inheritance add: " + b + "/" + inheritance + "/" + permission.getObject(inheritance).getArray("permission").get(a));
                            }
                        }
                    }
                }
            }catch (Exception e){
                printError(e);
            }
        }
    }
}