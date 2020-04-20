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
import java.text.ParseException;

import static essentials.Main.playerDB;
import static essentials.Main.root;
import static mindustry.Vars.playerGroup;

public class Permission {
    public JsonObject permission;
    public JsonObject permission_user;
    public String default_group = null;
    public boolean isUse = false;

    public Permission() {
        reload(true);
    }

    public void create(PlayerData playerData) {
        // JsonArray list = new JsonArray();
        JsonObject object = new JsonObject();

        /*int size = permission.get(playerData.permission).asObject().get("permission").asArray().size();
        for (int a = 0; a < size; a++) {
            String permlevel = permission.get(playerData.permission).asObject().get("permission").asArray().get(a).asString();
            list.add(permlevel);
        }*/

        object.add("uuid", playerData.uuid);
        object.add("group", default_group);
        // object.add("permission", list);
        object.add("prefix", permission.get(playerData.permission).asObject().getString("prefix", "[orange]%1[orange] >[white] %2"));
        object.add("admin", playerData.isAdmin);

        permission_user.add(playerData.name, object);
    }

    public void update() {
        for (JsonObject.Member p : permission_user) {
            boolean isMatch = false;
            boolean isPerm = false;

            String group = p.getValue().asObject().get("group").asString();
            for (JsonObject.Member d : permission) {
                if (d.getName().equals(group)) {
                    isMatch = true;

                    /*for (JsonValue v : p.getValue().asObject().get("permission").asArray()) {
                        for (JsonValue o : d.getValue().asObject().get("permission").asArray()){
                            System.out.println(v.asString()+"/"+o.asString());
                            if (v.asString().equals(o.asString())) {
                                break;
                            }
                        }
                    }*/
                }
            }

            if (!isMatch) {
                JsonArray list = new JsonArray();
                int size = permission.get(default_group).asObject().get("permission").asArray().size();
                for (int a = 0; a < size; a++) {
                    String permlevel = permission.get(default_group).asObject().get("permission").asArray().get(a).asString();
                    list.add(permlevel);
                }
                // permission_user.get(p.getName()).asObject().set("permission", list);
                permission_user.get(p.getName()).asObject().set("group", default_group);
            }

            if (isPerm) {
                JsonArray buf = new JsonArray();

                for (JsonValue v : p.getValue().asObject().get("permission").asArray()) {
                    for (JsonObject.Member d : permission) {
                        for (JsonValue b : d.getValue().asObject().get("permission").asArray()) {
                            if (!b.asString().equals(v.asString())) {
                                buf.add(v.asString());
                                break;
                            }
                        }
                    }
                }

                int size = permission.get(group).asObject().get("permission").asArray().size();
                for (int b = 0; b < size; b++) {
                    for (JsonValue a : buf) {
                        if (!a.asString().equals(permission.get(group).asObject().get("permission").asArray().get(b).asString())) {
                            buf.add(permission.get(group).asObject().get("permission").asArray().get(b).asString());
                        }
                    }
                }
                // permission_user.get(p.getName()).asObject().set("permission", buf);
                permission_user.get(p.getName()).asObject().set("group", group);
                break;
            }
        }
        saveAll();
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
                e.printStackTrace();
            }
        } else {
            root.child("permission_user.hjson").writeString(new JsonObject().toString(Stringify.FORMATTED));
        }
    }

    public boolean check(Player player, String command) {
        PlayerData p = playerDB.get(player.uuid);

        if (!p.error) {
            JsonObject object = permission_user.get(player.name).asObject();
            if (object != null) {
                /*int size = object.get("permission").asArray().size();
                for (int a = 0; a < size; a++) {
                    String permlevel = object.get("permission").asArray().get(a).asString();
                    if (permlevel.equals(command) || permlevel.equals("ALL")) {
                        return true;
                    }
                }*/
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
        PlayerData p = playerDB.get(player.uuid);
        return permission_user.get(p.name).asObject().getBoolean("admin", false);
    }
}