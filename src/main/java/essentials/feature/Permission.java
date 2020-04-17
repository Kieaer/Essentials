package essentials.feature;

import essentials.core.player.PlayerData;
import essentials.internal.CrashReport;
import essentials.internal.Log;
import mindustry.entities.type.Player;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.io.IOException;

import static essentials.Main.playerDB;
import static essentials.Main.root;

public class Permission {
    public JsonObject permission;
    private JsonObject permission_user;

    public Permission() {
        reload();
    }

    public void create(PlayerData playerData) {
        JsonArray list = new JsonArray();
        JsonObject object = new JsonObject();

        int size = permission.get(playerData.permission).asObject().get("permission").asArray().size();
        for (int a = 0; a < size; a++) {
            String permlevel = permission.get(playerData.permission).asObject().get("permission").asArray().get(a).asString();
            list.add(permlevel);
        }

        object.add("permission", list);
        object.add("prefix", "");
        object.add("admin", playerData.isAdmin);

        permission_user.add(playerData.name, object);
    }

    public void update(PlayerData playerData) {
        // TODO 권한 수정시 플레이어 업데이트 만들기
    }

    public void read(PlayerData playerData) {
        // TODO 플레이어별 권한 읽기
    }

    public void reload() {
        if (root.child("permission.hjson").exists()) {
            try {
                permission = JsonValue.readHjson(root.child("permission.hjson").reader()).asObject();
                if (permission.get("default").asObject() == null)
                    throw new IOException("Don't delete `default` group in permission.hjson!");
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
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                new CrashReport(e);
            }
        } else {
            Log.warn("file-not-found", "permission.hjson");
        }

        if (root.child("permission_user.hjson").exists()) {
            try {
                permission_user = JsonValue.readHjson(root.child("permission_user.hjson").reader()).asObject();
            } catch (IOException e) {
                new CrashReport(e);
            }
        } else {
            root.child("permission_user.hjson").writeString("{}");
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
        if (permission.get(p.permission).asObject() == null) p.permission("default");
        return permission.get(p.permission).asObject().getBoolean("admin", false);
    }
}