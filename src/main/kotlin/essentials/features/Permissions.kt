package essentials.features

import arc.Core
import essentials.Config
import essentials.Main
import essentials.PlayerCore
import essentials.Main.Companion.pluginRoot
import essentials.PlayerData
import essentials.PluginVars
import essentials.internal.Bundle
import essentials.internal.CrashReport
import essentials.internal.Log
import essentials.internal.PluginException
import mindustry.gen.Groups
import mindustry.gen.Playerc
import org.hjson.JsonObject
import org.hjson.JsonValue
import org.hjson.Stringify
import java.io.IOException

object Permissions {
    var perm: JsonObject = JsonObject()
    var user: JsonObject = JsonObject()
    var default: String = "default"
    
    operator fun get(name: String): JsonObject {
        return user.get(name).asObject()
    }

    fun create(playerData: PlayerData) {
        val obj = JsonObject()
        obj.add("name", playerData.name)
        obj.add("group", default)
        obj.add("prefix", perm[playerData.permission].asObject().getString("prefix", "%1[orange] >[white] %2"))
        obj.add("admin", playerData.isAdmin)
        user.add(playerData.uuid, obj)
    }

    fun update(isSave: Boolean) {
        for (p in user) {
            val `object` = p.value.asObject()
            var isMatch = false
            val group = p.value.asObject()["group"].asString()
            for (d in perm) {
                if (d.name == group) {
                    isMatch = true
                    break
                }
            }
            if (!isMatch) user[p.name].asObject()["group"] = default
            val player = Groups.player.find { pl: Playerc -> pl.uuid() == p.name }
            if (player != null && p.value.asObject()["name"].asString() != player.name) {
                player.name(`object`.getString("name", player.name))
            }
        }
        if (isSave) saveAll()
    }

    fun rename(uuid: String, new_name: String?) {
        for (j in user) {
            if (j.name == uuid) {
                val o = j.value.asObject().set("name", new_name)
                o["name"] = new_name
                user[uuid] = o
                break
            }
        }
    }

    fun saveAll() {
        pluginRoot.child("permission_user.hjson").writeString(user.toString(Stringify.FORMATTED))
    }

    fun reload(init: Boolean) {
        if (pluginRoot.child("permission.hjson").exists()) {
            try {
                var default: String? = null
                perm = JsonValue.readHjson(pluginRoot.child("permission.hjson").reader()).asObject()
                for (data in perm) {
                    val name = data.name
                    if (get(name)["default"] != null) {
                        if (get(name)["default"].asBoolean()) {
                            default = name
                        }
                    }
                    if (get(name)["inheritance"] != null) {
                        var inheritance = get(name).getString("inheritance", null)
                        while (inheritance != null) {
                            for (a in 0 until get(inheritance).asObject()["permission"].asArray().size()) {
                                get(name)["permission"].asArray().add(get(inheritance).asObject()["permission"].asArray()[a].asString())
                            }
                            inheritance = get(inheritance).asObject().getString("inheritance", null)
                        }
                    }
                }
                if (default == null) {
                    for (data in perm) {
                        val name = data.name
                        if (name == "default") {
                            default = name
                            val json = JsonValue.readHjson(pluginRoot.child("permission.hjson").reader()).asObject()
                            val perms = json["default"].asObject()["permission"].asArray()
                            json["default"].asObject().remove("permission")
                            json["default"].asObject().add("default", true)
                            json["default"].asObject().add("permission", perms)
                            pluginRoot.child("permission.hjson").writeString(json.toString(Stringify.HJSON))
                        }
                    }
                }
                if (default == null) {
                    throw PluginException(Bundle(Config.locale)["system.Permissions.no-default"])
                }
            } catch (e: IOException) {
                Log.err(e.message!!)
                Core.app.dispose()
                Core.app.exit()
            } catch (e: Exception) {
                CrashReport(e)
            }
        } else {
            Log.warn("system.file-not-found", "permission.hjson")
        }
        if (pluginRoot.child("permission_user.hjson").exists()) {
            try {
                user = JsonValue.readHjson(pluginRoot.child("permission_user.hjson").reader()).asObject()
                for (p in Groups.player) {
                    p.admin(isAdmin(PluginVars.playerData.find { d: PlayerData -> d.name == p.name }))
                }
            } catch (e: PluginException) {
                Log.err("Permissing parsing: " + CrashReport(e).print())
            }
        } else {
            pluginRoot.child("permission_user.hjson").writeString(JsonObject().toString(Stringify.FORMATTED))
        }
    }

    fun check(player: Playerc, command: String): Boolean {
        val p = PlayerCore[player.uuid()]
        if (!p.error) {
            val `object` = user[player.uuid()]
            if (`object` != null) {
                val obj = `object`.asObject()
                val size = perm[obj["group"].asString()].asObject()["permission"].asArray().size()
                for (a in 0 until size) {
                    val permlevel = perm[obj["group"].asString()].asObject()["permission"].asArray()[a].asString()
                    if (permlevel == command || permlevel == "ALL") {
                        return true
                    }
                }
            } else {
                return false
            }
        }
        return false
    }

    fun isAdmin(player: PlayerData?): Boolean {
        if (player != null) {
            return if (user.has(player.uuid)) {
                user[player.uuid].asObject().getBoolean("admin", false)
            } else {
                false
            }
        } else {
            return false
        }
    }

    fun setUserPerm(old: String, newid: String) {
        if (old != newid) {
            val oldJson = user[old].asObject()
            user[newid] = oldJson
            user.remove(old)
            update(true)
        }
    }
}