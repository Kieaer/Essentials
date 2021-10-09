package remake

import arc.Core
import arc.files.Fi
import org.hjson.JsonArray
import org.hjson.JsonObject
import org.hjson.JsonValue
import org.hjson.Stringify

class Permission {
    var perm = JsonObject()
    var data = JsonArray()
    private val root: Fi = Core.settings.dataDirectory.child("mods/Essentials/permission.hjson")
    private val user: Fi = Core.settings.dataDirectory.child("mods/Essentials/permission_user.hjson")

    fun save(){
        root.writeString(perm.toString(Stringify.HJSON))
        user.writeString(data.toString(Stringify.HJSON))
    }

    fun load(){
        perm = JsonValue.readHjson(root.reader()).asObject()
        data = JsonValue.readHjson(user.reader()).asArray()
    }

    fun create(data: DB.PlayerData){
        val obj = JsonObject()
        obj.add("name", data.name)
        obj.add("uuid", data.uuid)
        obj.add("group", if(Config.authType == Config.AuthType.None) "user" else "visitor")
        obj.add("chatFormat", perm[data.permission].asObject().getString("chatFormat", Config.chatFormat))
        obj.add("admin", perm[data.permission].asObject().getBoolean("admin", false))
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