package essentials.event.feature

import arc.Core
import essentials.Main.Companion.pluginRoot
import essentials.PlayerData
import essentials.PluginData
import essentials.data.Config
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
    var default: String = "visitor"

    private val file = pluginRoot.child("permission.txt")
    private val userFile = pluginRoot.child("permission_user.txt")

    operator fun get(name: String): JsonObject? {
        return user.get(name)?.asObject()
    }

    fun create(playerData: PlayerData) {
        val obj = JsonObject()
        obj.add("name", playerData.name)
        obj.add("group", if(Config.authType == Config.AuthType.None) "user" else default)
        obj.add("chatFormat", perm[default].asObject().getString("chatFormat", "%1[orange] >[white] %2"))
        obj.add("admin", false)
        user.add(playerData.uuid, obj)
    }

    fun update(isSave: Boolean) {
        for(p in user) {
            val `object` = p.value.asObject()
            var isMatch = false
            val group = p.value.asObject()["group"].asString()
            for(d in perm) {
                if(d.name == group) {
                    isMatch = true
                    break
                }
            }
            if(!isMatch) user[p.name].asObject()["group"] = default
            val player = Groups.player.find { pl: Playerc -> pl.uuid() == p.name }
            if(player != null && p.value.asObject()["name"].asString() != player.name) {
                player.name(`object`.getString("name", player.name))
            }
        }
        if(isSave) saveAll()
    }

    fun rename(uuid: String, name: String?) {
        for(j in user) {
            if(j.name == uuid) {
                val o = j.value.asObject().set("name", name)
                o["name"] = name
                user[uuid] = o
                break
            }
        }
    }

    fun saveAll() {
        userFile.writeString(user.toString(Stringify.FORMATTED))
    }

    fun reload(init: Boolean) {
        if(file.exists()) {
            try {
                var default: String? = null
                perm = JsonValue.readHjson(file.reader()).asObject()
                for(data in perm) {
                    val name = data.name
                    if(Config.authType == Config.AuthType.None && perm.get(name).asObject().has("default")) {
                        default = name
                    }

                    if(perm.get(name).asObject().has("inheritance")) {
                        var inheritance = perm.get(name).asObject().getString("inheritance", null)
                        while(inheritance != null) {
                            for(a in 0 until perm.get(inheritance).asObject()["permission"].asArray().size()) {
                                perm.get(name).asObject().get("permission").asArray().add(perm.get(inheritance).asObject()["permission"].asArray()[a].asString())
                            }
                            inheritance = perm.get(inheritance).asObject().getString("inheritance", null)
                        }
                    }
                }

                if(default == null) {
                    throw PluginException(Bundle(Config.locale)["system.permissions.no-default"])
                }
            } catch(e: IOException) {
                Log.err(e.message!!)
                Core.app.dispose()
                Core.app.exit()
            } catch(e: Exception) {
                CrashReport(e)
            }
        } else {
            Log.warn("system.file-not-found", "permission.txt")
        }

        if(userFile.exists()) {
            try {
                user = JsonValue.readHjson(userFile.reader()).asObject()

                // KR-Plugin 하위 호환
                user.forEach {
                    if (it.value.asObject().has("prefix")) {
                        val original = it.value.asObject().getString("prefix", "%1[orange] >[white] %2")
                        it.value.asObject().set("chatFormat", original)
                    }
                }

                for(p in Groups.player) {
                    p.admin(isAdmin(PluginData.playerData.find { d: PlayerData -> d.name == p.name }))
                }
            } catch(e: PluginException) {
                Log.err("Permission parsing: " + CrashReport(e).print())
            }
        } else {
            userFile.writeString(JsonObject().toString(Stringify.FORMATTED))
        }
    }

    fun check(player: Playerc, command: String): Boolean {
        val data = user[player.uuid()]
        if(data != null) {
            val obj = data.asObject()
            val size = perm[obj["group"].asString()].asObject()["permission"].asArray().size()
            for(a in 0 until size) {
                val node = perm[obj["group"].asString()].asObject()["permission"].asArray()[a].asString()
                if(node == command || node.equals("all", true)) {
                    return true
                }
            }
        } else {
            return false
        }
        return false
    }

    fun isAdmin(player: PlayerData?): Boolean {
        return if(player != null) {
            if(user.has(player.uuid)) {
                user[player.uuid].asObject().getBoolean("admin", false)
            } else {
                false
            }
        } else {
            false
        }
    }

    fun setUserPerm(old: String, newid: String) {
        if(old != newid) {
            val oldJson = user[old].asObject()
            user[newid] = oldJson
            user.remove(old)
            update(true)
        }
    }
}