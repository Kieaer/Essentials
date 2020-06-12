package essentials.feature;

import arc.Core;
import essentials.core.player.PlayerData;
import essentials.internal.Bundle;
import essentials.internal.CrashReport;
import essentials.internal.Log;
import essentials.internal.exception.PluginException;
import mindustry.entities.type.Player;
import org.hjson.*;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static essentials.Main.*;
import static mindustry.Vars.playerGroup;
import static org.hjson.JsonValue.readHjson;

public class Permission {
    public JsonObject permission;
    public JsonObject permission_user;
    public String default_group = null;

    public void create(PlayerData playerData) {
        JsonObject object = new JsonObject();

        object.add("name", playerData.name());
        object.add("group", default_group);
        object.add("prefix", permission.get(playerData.permission()).asObject().getString("prefix", "[orange]%1[orange] >[white] %2"));
        object.add("admin", playerData.isAdmin());

        permission_user.add(playerData.uuid(), object);
    }

    public void update() {
        for (JsonObject.Member p : permission_user) {
            JsonObject object = p.getValue().asObject();
            boolean isMatch = false;

            String group = p.getValue().asObject().get("group").asString();
            for (JsonObject.Member d : permission) {
                if (d.getName().equals(group)) {
                    isMatch = true;
                    break;
                }
            }

            if (!isMatch) permission_user.get(p.getName()).asObject().set("group", default_group);

            Player player = playerGroup.find(pl -> pl.uuid.equals(p.getName()));
            if (player != null && !p.getValue().asObject().get("name").asString().equals(player.name)) {
                player.name = object.getString("name", player.name);
            }
        }
        saveAll();
    }

    public void rename(String uuid, String new_name) {
        for (JsonObject.Member j : permission_user) {
            if (j.getName().equals(uuid)) {
                JsonObject o = j.getValue().asObject().set("name", new_name);
                o.set("name", new_name);
                permission_user.set(uuid, o);
                break;
            }
        }
    }

    public void saveAll() {
        root.child("permission_user.hjson").writeString(permission_user.toString(Stringify.FORMATTED));
    }

    public void reload(boolean init) {
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
                        } else if (init) {
                            throw new PluginException(new Bundle(config.locale).get("system.perm.duplicate"));
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

                if (default_group == null) {
                    for (JsonObject.Member data : permission) {
                        String name = data.getName();
                        if (name.equals("default")) {
                            default_group = name;
                            JsonObject json = readHjson(root.child("permission.hjson").reader()).asObject();
                            JsonArray perms = json.get("default").asObject().get("permission").asArray();
                            json.get("default").asObject().remove("permission");
                            json.get("default").asObject().add("default", true);
                            json.get("default").asObject().add("permission", perms);
                            root.child("permission.hjson").writeString(json.toString(Stringify.HJSON));
                        }
                    }
                }

                if (default_group == null) {
                    throw new PluginException(new Bundle(config.locale).get("system.perm.no-default"));
                }
            } catch (IOException e) {
                Log.err(e.getMessage());
                Core.app.dispose();
                Core.app.exit();
            } catch (Exception e) {
                new CrashReport(e);
            }
        } else {
            Log.warn("system.file-not-found", "permission.hjson");
        }

        if (root.child("permission_user.hjson").exists()) {
            try {
                permission_user = JsonValue.readHjson(root.child("permission_user.hjson").reader()).asObject();
                for (Player p : playerGroup.all()) {
                    p.isAdmin = isAdmin(vars.playerData().find(d -> d.name().equals(p.name)));
                }
            } catch (IOException | ParseException e) {
                // 이것도 유저들이 알아야 고침
                LoggerFactory.getLogger(Permission.class).error("Permission parsing", e);
            }
        } else {
            root.child("permission_user.hjson").writeString(new JsonObject().toString(Stringify.FORMATTED));
        }
    }

    public boolean check(Player player, String command) {
        PlayerData p = playerDB.get(player.uuid);

        if (!p.error()) {
            JsonValue object = permission_user.get(player.uuid);
            if (object != null) {
                JsonObject obj = object.asObject();
                int size = permission.get(obj.get("group").asString()).asObject().get("permission").asArray().size();
                for (int a = 0; a < size; a++) {
                    String permlevel = permission.get(obj.get("group").asString()).asObject().get("permission").asArray().get(a).asString();
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

    public boolean isAdmin(PlayerData player) {
        try {
            if (permission_user.has(player.uuid())) {
                return permission_user.get(player.uuid()).asObject().getBoolean("admin", false);
            } else {
                return false;
            }
        } catch (NullPointerException ignored) {
            return false;
        }
    }

    public void setPermission_user(String old, String newid) {
        if (!old.equals(newid)) {
            JsonObject oldJson = permission_user.get(old).asObject();
            permission_user.set(newid, oldJson);
            permission_user.remove(old);
            update();
        }
    }
}