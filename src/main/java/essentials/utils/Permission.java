package essentials.utils;

import org.hjson.JsonObject;
import org.hjson.JsonValue;

import static essentials.Global.*;

public class Permission {
    public static JsonObject permission;

    public Permission(){
        if(root.child("permission.hjson").exists()) {
            try {
                permission = JsonValue.readJSON(root.child("permission.hjson").reader()).asObject();
                for (JsonObject.Member data : permission) {
                    String name = data.getName();
                    nlog("debug", "target: " + name);
                    if(permission.get(name).asObject().get("inheritance") != null) {
                        String inheritance = permission.get(name).asObject().getString("inheritance", null);
                        nlog("debug", "target inheritance: " + name + "/" + inheritance);
                        while (inheritance != null) {
                            for (int a = 0; a < permission.get(inheritance).asObject().get("permission").asArray().size(); a++) {
                                permission.get(name).asObject().get("permission").asArray().add(permission.get(inheritance).asObject().get("permission").asArray().get(a));
                                nlog("debug", "target inheritance add: " + name + "/" + inheritance + "/" + permission.get(inheritance).asObject().get("permission").asArray().get(a));
                            }
                            inheritance = permission.get(inheritance).asObject().getString("inheritance", null);
                        }
                    }
                }
            }catch (Exception e){
                printError(e);
            }
        }
    }
}