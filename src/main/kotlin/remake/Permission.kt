package remake

import arc.Core
import arc.files.Fi
import mindustry.Vars.netServer
import mindustry.gen.Playerc
import org.hjson.*
import remake.Main.Companion.database
import java.util.*

object Permission {
    var perm = JsonObject()
    var data = JsonArray()
    var default = if (Config.authType == Config.AuthType.None) "user" else "visitor"
    private val root: Fi = Core.settings.dataDirectory.child("mods/Essentials/permission.txt")
    private val user: Fi = Core.settings.dataDirectory.child("mods/Essentials/permission_user.txt")

    val bundle = Bundle(Locale(System.getProperty("user.language"), System.getProperty("user.country")).toLanguageTag())

    val comment = """
        ${bundle["permission.sort"]}
        ${bundle["permission.notice"]}
        Usage
        {
            uuid: ${bundle["permission.usage.uuid"]}
            name: ${bundle["permission.usage.name"]}
            group: ${bundle["permission.usage.group"]}
            chatFormat: ${bundle["permission.usage.chatformat"]}
            admin: ${bundle["permission.usage.admin"]}
        }
        
        Examples
        [
            {
                uuid: uuids
                name: "my fun name"
            },
            {
                uuid: uuida
                chatFormat: "%1: %2"
            },
            {
                uuid: uuid123
                name: asdfg
                group: admin
                chatFormat: "[blue][ADMIN][]%1[white]: %2"
                admin: true
            }
        ]""".trimIndent()

    init {
        if (!root.exists()) {
            val json = JsonObject()

            val owner = JsonObject()
            owner.add("admin", true)
            owner.add("chatFormat", "[sky][Owner] %1[orange] > [white]%2")
            owner.add("permission", JsonArray().add("all"))

            val admin = JsonObject()
            val adminPerm = JsonArray()
            adminPerm.add("color")
            adminPerm.add("kill")
            adminPerm.add("mute")
            adminPerm.add("spawn")
            adminPerm.add("team")
            adminPerm.add("team.other")
            adminPerm.add("weather")
            adminPerm.add("info.other")

            admin.add("inheritance", "user")
            admin.add("admin", true)
            admin.add("chatFormat", "[yellow][Admin] %1[orange] > [white]%2")
            admin.add("permission", adminPerm)

            val user = JsonObject()
            val userPerm = JsonArray()
            userPerm.add("*login")
            userPerm.add("*reg")
            userPerm.add("ch")
            userPerm.add("discord")
            userPerm.add("info")
            userPerm.add("maps")
            userPerm.add("me")
            userPerm.add("motd")
            userPerm.add("players")
            userPerm.add("status")
            userPerm.add("time")
            userPerm.add("tp")
            userPerm.add("vote")

            user.add("inheritance", "visitor")
            user.add("chatFormat", "%1[orange] > [white]%2")
            user.add("permission", userPerm)

            val visitor = JsonObject()
            val visitorPerm = JsonArray()
            visitorPerm.add("help")
            visitorPerm.add("login")
            visitorPerm.add("reg")
            visitorPerm.add("t")

            visitor.add("chatFormat", "%1[scarlet] > [white]%2")
            visitor.add("default", true)
            visitor.add("permission", visitorPerm)

            json.add("owner", owner)
            json.add("admin", admin)
            json.add("user", user)
            json.add("visitor", visitor)

            root.writeString(json.toString(Stringify.HJSON))
        }

        if (!user.exists()) {
            val obj = JsonArray()
            obj.setComment(comment)
            Core.settings.dataDirectory.child("mods/Essentials/permission_user.txt").writeString(obj.toString(Stringify.HJSON_COMMENTS))
        }
    }

    fun save() {
        root.writeString(perm.toString(Stringify.HJSON))
    }

    fun sort() {
        user.writeString(data.setComment(comment).toString(Stringify.HJSON_COMMENTS))
    }

    @Throws(ParseException::class)
    fun load() {
        perm = JsonValue.readHjson(root.reader()).asObject()
        data = JsonValue.readHjson(user.reader()).asArray()

        for (data in perm) {
            val name = data.name
            if (Config.authType == Config.AuthType.None && perm.get(name).asObject().has("default")) {
                default = name
            }

            if (perm.get(name).asObject().has("inheritance")) {
                var inheritance = perm.get(name).asObject().getString("inheritance", null)
                while (inheritance != null) {
                    for (a in 0 until perm.get(inheritance).asObject()["permission"].asArray().size()) {
                        if (!perm.get(inheritance).asObject()["permission"].asArray()[a].asString().contains("*")) {
                            perm.get(name).asObject().get("permission").asArray().add(perm.get(inheritance).asObject()["permission"].asArray()[a].asString())
                        }
                    }
                    inheritance = perm.get(inheritance).asObject().getString("inheritance", null)
                }
            }
        }

        for (a in JsonValue.readHjson(user.reader()).asArray()) {
            val b = a.asObject()
            val c = database.players.find { e -> e.uuid == b.get("uuid").asString() }
            if (c == null){
                val data = database[b.get("uuid").asString()]
                if (data != null && b.has("group")) {
                    data.permission = b.get("group").asString()
                    database.update(b.get("uuid").asString(), data)
                }
            } else {
                c.permission = b.get("group").asString()
            }
        }
    }

    operator fun get(player: Playerc): PermissionData {
        val result = PermissionData
        val p = database.players.find { e -> e.uuid == player.uuid() }

        result.uuid = player.uuid()
        result.name = netServer.admins.findByIP(player.ip()).lastName
        result.group = p?.permission ?: default
        result.chatFormat = PermissionData.chatFormat
        result.admin = false

        data.forEach {
            val data = it.asObject()

            if (data.has("uuid") && data.get("uuid").asString().equals(player.uuid())) {
                result.uuid = data.getString("uuid", player.uuid())
                result.name = data.getString("name", netServer.admins.findByIP(player.ip()).lastName)
                result.group = data.getString("group", default)
                result.chatFormat = data.getString("chatFormat", PermissionData.chatFormat)
                result.admin = data.getBoolean("admin", false)
            }

            return result
        }
        return result
    }

    fun check(player: Playerc, command: String): Boolean {
        val data = get(player).group
        val size = perm[data].asObject()["permission"].asArray().size()
        for (a in 0 until size) {
            val node = perm[data].asObject()["permission"].asArray()[a].asString()
            if (node == command || node.equals("all", true)) {
                return true
            }
        }
        return false
    }

    object PermissionData {
        var name = ""
        var uuid = ""
        var group = default
        var chatFormat = "%1[orange] > [white]%2"
        var admin = false
    }
}