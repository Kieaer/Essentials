package remake.feature;

import mindustry.entities.type.Player;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import remake.core.player.PlayerData;
import remake.internal.CrashReport;
import remake.internal.Log;

import static remake.Main.playerDB;
import static remake.Main.root;

public class Permission {
    public JsonObject permission;

    public Permission() {
        reload();
    }

    public void reload() {
        if (root.child("permission.hjson").exists()) {
            try {
                permission = JsonValue.readHjson(root.child("permission.hjson").reader()).asObject();
                for (JsonObject.Member data : permission) {
                    String name = data.getName();
                    if (permission.get(name).asObject().get("inheritance") != null) {
                        String inheritance = permission.get(name).asObject().getString("inheritance", null);
                        while (inheritance != null) {
                            for (int a = 0; a < permission.get(inheritance).asObject().get("permission").asArray().size(); a++) {
                                permission.get(name).asObject().get("permission").asArray().add(permission.get(inheritance).asObject().get("permission").asArray().get(a).asString());
                            }
                            inheritance = permission.get(inheritance).asObject().getString("inheritance", null);
                        }
                    }
                }

            } catch (Exception e) {
                new CrashReport(e);
            }
        } else {
            Log.warn("file-not-found");
        }
    }

    public boolean check(Player player, String command) {
        PlayerData p = playerDB.get(player.uuid);

        if (!p.error) {
            int size = permission.get(p.permission).asObject().get("permission").asArray().size();
            for (int a = 0; a < size; a++) {
                String permlevel = permission.get(p.permission).asObject().get("permission").asArray().get(a).asString();
                if (permlevel.equals(command) || permlevel.equals("ALL")) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isAdmin(Player player) {
        PlayerData p = playerDB.get(player.uuid);
        return permission.get(p.permission).asObject().getBoolean("admin", false);
    }
}