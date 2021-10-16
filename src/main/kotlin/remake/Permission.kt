package remake

import arc.Core
import arc.files.Fi
import essentials.data.Config
import essentials.event.feature.Permissions
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

    fun load(){
        perm = JsonValue.readHjson(root.reader()).asObject()
        data = JsonValue.readHjson(user.reader()).asArray()

        for(data in perm) {
            val name = data.name
            if(Config.authType == Config.AuthType.None && Permissions.perm.get(name).asObject().has("default")) {
                default = name
            }

            if(Permissions.perm.get(name).asObject().has("inheritance")) {
                var inheritance = perm.get(name).asObject().getString("inheritance", null)
                while(inheritance != null) {
                    for(a in 0 until perm.get(inheritance).asObject()["permission"].asArray().size()) {
                        perm.get(name).asObject().get("permission").asArray().add(perm.get(inheritance).asObject()["permission"].asArray()[a].asString())
                    }
                    inheritance = perm.get(inheritance).asObject().getString("inheritance", null)
                }
            }
        }
    }

    fun read() : PermissionData{
        data.forEach {
            val data = it.asObject()
            val result = PermissionData

            if (data.has("uuid")) result.uuid = data.get("uuid").asString()
            if (data.has("name")) result.name = data.get("name").asString()
            if (data.has("group")) result.group = data.get("group").asString()
            if (data.has("chatFormat")) result.chatFormat = data.get("chatFormat").asString()
            if (data.has("admin")) result.admin = data.get("admin").asBoolean()
            return result
        }
        return PermissionData
    }

    object PermissionData{
        var name = ""
        var uuid = ""
        var group = ""
        var chatFormat = ""
        var admin = false
    }
}