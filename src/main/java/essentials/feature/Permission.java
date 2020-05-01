package essentials.feature;

import arc.Core;
import essentials.core.player.PlayerData;
import essentials.internal.CrashReport;
import essentials.internal.Log;
import mindustry.entities.type.Player;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.hjson.Stringify;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;

import static essentials.Main.playerDB;
import static essentials.Main.root;
import static mindustry.Vars.playerGroup;

public class Permission {
    public JsonObject permission;
    public JsonObject permission_user;
    public String default_group = null;
    public boolean isUse = false;

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

                if (default_group == null)
                    throw new ParseException("Please SET default group in permission.hjson file.", 0);
            } catch (ParseException | IOException e) {
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
                    p.isAdmin = isAdmin(p);
                }
            } catch (IOException e) {
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
            JsonObject object = permission_user.get(player.uuid).asObject();
            if (object != null) {
                int size = permission.get(object.get("group").asString()).asObject().get("permission").asArray().size();
                for (int a = 0; a < size; a++) {
                    String permlevel = permission.get(object.get("group").asString()).asObject().get("permission").asArray().get(a).asString();
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
        if (player == null) {
            new CrashReport(new Exception("isAdmin player NULL!"));
            Core.app.dispose();
            Core.app.exit();
            System.exit(0);
        }
        PlayerData p = playerDB.get(player.uuid);
        return permission_user.get(p.uuid()).asObject().getBoolean("admin", false);
    }
}