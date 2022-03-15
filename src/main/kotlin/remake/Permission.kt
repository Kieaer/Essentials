package remake

import arc.Core
import arc.files.Fi
import arc.util.Log
import mindustry.Vars.netServer
import mindustry.gen.Groups
import mindustry.gen.Playerc
import org.hjson.JsonArray
import org.hjson.JsonObject
import org.hjson.JsonValue
import org.hjson.Stringify

object Permission {
    var perm = JsonObject()
    var data = JsonArray()
    var default = "visitor"
    private val root: Fi = Core.settings.dataDirectory.child("mods/Essentials/permission.hjson")
    private val user: Fi = Core.settings.dataDirectory.child("mods/Essentials/permission_user.hjson")

    fun save(){
        root.writeString(perm.toString(Stringify.HJSON))
        user.writeString(data.toString(Stringify.HJSON))
    }

    fun load() {
        perm = JsonValue.readHjson(root.reader()).asObject()
        data = JsonArray()

        for (data in perm) {
            val name = data.name
            if (Config.authType == Config.AuthType.None && perm.get(name).asObject().has("default")) {
                default = name
            }

            if (perm.get(name).asObject().has("inheritance")) {
                var inheritance = perm.get(name).asObject().getString("inheritance", null)
                while (inheritance != null) {
                    for (a in 0 until perm.get(inheritance).asObject()["permission"].asArray().size()) {
                        perm.get(name).asObject().get("permission").asArray()
                            .add(perm.get(inheritance).asObject()["permission"].asArray()[a].asString())
                    }
                    inheritance = perm.get(inheritance).asObject().getString("inheritance", null)
                }
            }
        }

        for (a in JsonValue.readHjson(user.reader()).asArray()) {
            val b = a.asObject()
            val result = JsonObject()

            if (b.has("uuid")) result.add("uuid",b.get("uuid").asString())
            if (b.has("name")) result.add("name",b.get("name").asString())
            if (b.has("group")) result.add("group",b.get("group").asString())
            if (b.has("chatFormat")) result.add("chatFormat",b.get("chatFormat").asString())
            if (b.has("admin")) result.add("admin",b.get("admin").asBoolean())

            data.add(result)
        }
    }

    operator fun get(player: Playerc) : PermissionData{
        val result = PermissionData

        result.uuid = player.uuid()
        result.name = netServer.admins.findByIP(player.ip()).lastName
        result.group = default
        result.chatFormat = "%1[orange] > [white]%2"
        result.admin = false

        data.forEach {
            val data = it.asObject()

            if(data.has("uuid") && data.get("uuid").asString().equals(player.uuid())){
                result.uuid = data.getString("uuid", player.uuid())
                result.name = data.getString("name", netServer.admins.findByIP(player.ip()).lastName)
                result.group = data.getString("group", default)
                result.chatFormat = data.getString("chatFormat", "%1[orange] > [white]%2")
                result.admin = data.getBoolean("admin", false)
            }

            return if (player.uuid() == data.get("uuid").asString()) result else PermissionData
        }
        return result
    }

    fun check(player: Playerc, command: String): Boolean {
        val data = get(player).group
        val size = perm[data].asObject()["permission"].asArray().size()
        for(a in 0 until size) {
            val node = perm[data].asObject()["permission"].asArray()[a].asString()
            if(node == command || node.equals("all", true)) {
                return true
            }
        }
        return false
    }

    object PermissionData{
        var name = ""
        var uuid = ""
        var group = default
        var chatFormat = "%1[orange] > [white]%2"
        var admin = false
    }
}