package essentials.feature;

import arc.Core;
import essentials.core.player.PlayerData;
import essentials.internal.CrashReport;
import essentials.internal.Log;
import mindustry.entities.type.Player;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.hjson.Stringify;

import java.io.IOException;

import static essentials.Main.playerDB;
import static essentials.Main.root;

public class Permission {
    public JsonObject permission;
    private JsonObject permission_user;
    private String default_group = null;

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

        object.add("uuid", playerData.uuid);
        object.add("group", default_group);
        object.add("permission", list);
        object.add("prefix", "");
        object.add("admin", playerData.isAdmin);

        permission_user.add(playerData.name, object);
    }

    public void update(PlayerData playerData) {
        for (JsonObject.Member p : permission_user) {
            boolean isMatch = false;
            String group = p.getValue().asObject().get("group").asString();
            for (JsonObject.Member d : permission) {
                if (d.getValue().asString().equals(group)) {
                    isMatch = true;
                    break;
                }
            }

            if (!isMatch) {
                JsonObject o = permission_user.get(p.getName()).asObject();
                o.remove("permission");

                JsonArray list = new JsonArray();
                int size = permission.get(playerData.permission).asObject().get("permission").asArray().size();
                for (int a = 0; a < size; a++) {
                    String permlevel = permission.get(playerData.permission).asObject().get("permission").asArray().get(a).asString();
                    list.add(permlevel);
                }
                o.add("permission", list);
                o.set("group", default_group);
            }
        }
    }

    public void read() {
        try {
            permission_user = JsonValue.readHjson(root.child("permission_user.hjson").reader()).asObject();
        } catch (IOException e) {
            new CrashReport(e);
        }
    }

    public void saveAll() {
        root.child("permission_user.hjson").writeString(permission_user.toString(Stringify.HJSON));
    }

    public void reload() {
        if (root.child("permission.hjson").exists()) {
            try {
                permission = JsonValue.readHjson(root.child("permission.hjson").reader()).asObject();
                for (JsonObject.Member data : permission) {
                    String name = data.getName();
                    if (permission.get(name).asObject().get("default") != null) {
                        if (default_group == null) {
                            if (permission.get(name).asObject().get("default").asBoolean()) {
                                default_group = name;
                            }
                        } else {
                            // TODO 언어별 오류 메세지 추가
                            throw new IOException("Duplicate default group settings. check permission.hjson");
                        }
                    }
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
                Core.app.dispose();
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
            JsonObject object = permission_user.get(player.name).asObject();
            if (object != null) {
                int size = object.get("permission").asArray().size();
                for (int a = 0; a < size; a++) {
                    String permlevel = object.get("permission").asArray().get(a).asString();
                    if (permlevel.equals(command) || permlevel.equals("ALL")) {
                        return true;
                    }
                }
            } else {
                return false;
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