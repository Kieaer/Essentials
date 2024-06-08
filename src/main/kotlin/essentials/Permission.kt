package essentials

import arc.Core
import arc.files.Fi
import essentials.Main.Companion.database
import mindustry.Vars.netServer
import org.hjson.*
import java.util.*

object Permission {
    private var main = JsonObject()
    var user = JsonArray()
    var default = if (Config.authType == Config.AuthType.None) "user" else "visitor"
    val mainFile : Fi = Core.settings.dataDirectory.child("mods/Essentials/permission.txt")
    val userFile : Fi = Core.settings.dataDirectory.child("mods/Essentials/permission_user.txt")

    val bundle = Bundle(Locale.getDefault().toLanguageTag())

    val comment = """
        ${bundle["permission.wiki"]}
        ${bundle["permission.sort"]}
        ${bundle["permission.notice"]}
        ${bundle["permission.usage"]}
        {
            uuid: ${bundle["permission.usage.uuid"]}
            name: ${bundle["permission.usage.name"]}
            group: ${bundle["permission.usage.group"]}
            chatFormat: ${bundle["permission.usage.chatformat"]}
            admin: ${bundle["permission.usage.admin"]}
            isAlert: ${bundle["permission.usage.isAlert"]}
            alertMessage: ${bundle["permission.usage.alertMessage"]}
        }
        
        ${bundle["permission.example"]}
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
                chatFormat: "[blue][ADMIN] []%1[orange] > [white] %2"
                admin: true
                isAlert: true
                alertMessage: Player asdfg has entered the server!
            }
        ]""".trimIndent()

    init {
        if (!mainFile.exists()) {
            val json = JsonObject()

            val owner = JsonObject()
            owner.add("admin", true)
            owner.add("chatFormat", "[sky][Owner] %1[orange] > [white]%2")
            owner.add("permission", JsonArray().add("all"))

            val admin = JsonObject()
            val adminPerm = JsonArray()
            adminPerm.add("afk.admin")
            adminPerm.add("changemap")
            adminPerm.add("chars")
            adminPerm.add("chat")
            adminPerm.add("chat.admin")
            adminPerm.add("color")
            adminPerm.add("dps")
            adminPerm.add("fillitems")
            adminPerm.add("freeze")
            adminPerm.add("gg")
            adminPerm.add("god")
            adminPerm.add("hud.enemy")
            adminPerm.add("info.other")
            adminPerm.add("kick.admin")
            adminPerm.add("kill")
            adminPerm.add("kill.other")
            adminPerm.add("meme")
            adminPerm.add("mute")
            adminPerm.add("pause")
            adminPerm.add("pm.other")
            adminPerm.add("pvp.spector")
            adminPerm.add("rollback")
            adminPerm.add("search")
            adminPerm.add("skip")
            adminPerm.add("spawn")
            adminPerm.add("strict")
            adminPerm.add("team")
            adminPerm.add("team.other")
            adminPerm.add("tempban")
            adminPerm.add("tpp")
            adminPerm.add("unmute")
            adminPerm.add("vote.pass")
            adminPerm.add("vote.reset")
            adminPerm.add("weather")

            admin.add("inheritance", "user")
            admin.add("admin", true)
            admin.add("chatFormat", "[yellow][Admin] %1[orange] > [white]%2")
            admin.add("permission", adminPerm)

            val user = JsonObject()
            val userPerm = JsonArray()
            userPerm.add("*login")
            userPerm.add("*reg")
            userPerm.add("changepw")
            userPerm.add("discord")
            userPerm.add("effect")
            userPerm.add("hud")
            userPerm.add("info")
            userPerm.add("lang")
            userPerm.add("maps")
            userPerm.add("me")
            userPerm.add("motd")
            userPerm.add("players")
            userPerm.add("pm")
            userPerm.add("ranking")
            userPerm.add("report")
            userPerm.add("status")
            userPerm.add("time")
            userPerm.add("tp")
            userPerm.add("track")
            userPerm.add("url")
            userPerm.add("vote")
            userPerm.add("vote.back")
            userPerm.add("vote.gg")
            userPerm.add("vote.kick")
            userPerm.add("vote.map")
            userPerm.add("vote.random")
            userPerm.add("vote.skip")

            user.add("inheritance", "visitor")
            user.add("chatFormat", "%1[orange] > [white]%2")
            user.add("permission", userPerm)

            val visitor = JsonObject()
            val visitorPerm = JsonArray()
            visitorPerm.add("help")
            visitorPerm.add("login")
            visitorPerm.add("reg")

            visitor.add("chatFormat", "%1[scarlet] > [white]%2")
            visitor.add("default", true)
            visitor.add("permission", visitorPerm)

            json.add("owner", owner)
            json.add("admin", admin)
            json.add("user", user)
            json.add("visitor", visitor)

            mainFile.writeString(json.toString(Stringify.HJSON))
        }

        if (!userFile.exists()) {
            val obj = JsonArray()
            obj.setComment(comment)
            userFile.writeString(obj.toString(Stringify.HJSON_COMMENTS))
        }
    }

    fun sort() {
        userFile.writeString(user.setComment(comment).toString(Stringify.HJSON_COMMENTS))
        mainFile.writeString(JsonValue.readHjson(mainFile.reader()).toString(Stringify.HJSON))
    }

    @Throws(ParseException::class)
    fun load() {
        main = JsonValue.readHjson(mainFile.reader()).asObject()
        user = JsonValue.readHjson(userFile.reader()).asArray()

        main.forEach {
            val name = it.name
            if (Config.authType == Config.AuthType.None && main[name].asObject().has("default")) {
                default = name
            }

            if (main[name].asObject().has("inheritance")) {
                var inheritance = main[name].asObject().getString("inheritance", null)
                while (inheritance != null) {
                    for (value in main[inheritance].asObject()["permission"].asArray()) {
                        if (!value.asString().contains("*")) {
                            main[name].asObject()["permission"].asArray().add(value.asString())
                        }
                    }
                    inheritance = main[inheritance].asObject().getString("inheritance", null)
                }
            }
        }

        apply()
    }

    fun apply() {
        JsonValue.readHjson(userFile.reader()).asArray().forEach {
            val b = it.asObject()
            val c = database.players.find { e -> e.uuid == b["uuid"].asString() }
            if (c == null) {
                val data = database[b["uuid"].asString()]
                if (data != null && b.has("group")) {
                    data.permission = b["group"].asString()
                    data.name = b.getString("name", data.name)
                    database.queue(data)
                }
            } else {
                c.permission = b.getString("group", default)
                if (!netServer.admins.findByName(c.player.uuid()).isEmpty) {
                    c.name = b.getString("name", netServer.admins.findByName(c.player.uuid()).first().lastName)
                    c.player.admin(b.getBoolean("admin", false))
                    c.player.name(b.getString("name", netServer.admins.findByName(c.player.uuid()).first().lastName))
                } else {
                    c.name = b.getString("name", c.name)
                    c.player.admin(b.getBoolean("admin", false))
                    c.player.name(b.getString("name", c.name))
                }
                database.queue(c)
            }
        }
    }

    operator fun get(data : DB.PlayerData) : PermissionData {
        val result = PermissionData()
        val u = user.find { it.asObject().has("uuid") && it.asObject()["uuid"].asString() == data.uuid }
        if (u != null) {
            result.uuid = u.asObject().getString("uuid", data.uuid)
            result.name = u.asObject().getString("name", data.player.name())
            result.group = u.asObject().getString("group", data.permission)
            result.chatFormat = u.asObject().getString("chatFormat", Config.chatFormat)
            result.admin = u.asObject().getBoolean("admin", false)
            result.isAlert = u.asObject().getBoolean("isAlert", false)
            result.alertMessage = u.asObject().getString("alertMessage", "")
        } else {
            result.uuid = data.uuid
            result.name = data.player.name()
            result.group = data.permission
            result.admin = false
            result.isAlert = false
            result.alertMessage = ""
        }

        return result
    }

    fun check(data : DB.PlayerData, command : String) : Boolean {
        main[this[data].group].asObject()["permission"].asArray().forEach {
            if (it.asString() == command || it.asString().equals("all", true)) {
                return true
            }
        }
        return false
    }

    class PermissionData {
        var name = ""
        var uuid = ""
        var group = default
        var chatFormat = Config.chatFormat
        var admin = false
        var isAlert = false
        var alertMessage = ""
    }
}