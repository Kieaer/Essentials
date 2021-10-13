package remake

import arc.Core
import arc.files.Fi
import arc.util.Log
import org.hjson.JsonObject
import org.hjson.Stringify
import java.awt.Desktop
import java.net.URI
import java.util.*

object Config {
    var obj = JsonObject()

    var update = true
    var channel = "release"

    var afk = false
    var afkTime = 300
    var border = false
    var report = true

    var authType = AuthType.None
    var chatFormat = "%1[orange] >[white] %2"

    private val root: Fi = Core.settings.dataDirectory.child("mods/Essentials/config.hjson")

    private fun wizard() {
        Log.info("Do you want to read Essentials plugin documents (y/N)")
        val sc = Scanner(System.`in`)

        when (sc.nextLine()) {
            "y", "Y" -> {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI("https://github.com/Kieaer/Essentials/wiki"));
                }
            }
        }
    }

    enum class AuthType {
        None {
            override fun toString(): String {
                return "none"
            }
        },
        Password {
            override fun toString(): String {
                return "password"
            }
        },
        Discord {
            override fun toString(): String {
                return "discord"
            }
        }
    }

    fun save(){
        if(!root.exists()) {
            wizard()

            val plugin = JsonObject()
            plugin.add("update", update, "Plugin auto update")
            plugin.add("channel", channel, "Plugin update release channel")
            plugin.add("report", report, "Auto report plugin error")
            plugin.add("authType", authType.toString(), "Server authorize type")

            val features = JsonObject()
            features.add("afk", afk, "Auto AFK player kick")
            features.add("afkTime", afkTime, "Auto AFK player kick time")
            features.add("border", border, "Kill units world outside")
            features.add("chatFormat", chatFormat, "Set default chat format")

            obj.add("plugin", plugin)
            obj.add("features", features)
            obj.add("database", "jdbc:sqlite:file:./config/mods/Essentials/data/player")

            root.writeString(obj.toString(Stringify.HJSON_COMMENTS))
        }
    }

    fun load(){
        if(!root.exists()) {
            save()
        }

        val config = JsonObject.readHjson(root.readString("utf-8")).asObject()
        val plugin = config.get("plugin").asObject()
        val features = config.get("features").asObject()

        update = plugin.get("update").asBoolean()
        channel = plugin.get("channel").asString()
        afk = features.get("afk").asBoolean()
        afkTime = features.get("afkTime").asInt()
        border = features.get("border").asBoolean()
        report = plugin.get("report").asBoolean()
        authType = AuthType.valueOf(plugin.get("authType").asString().replaceFirstChar { it.uppercase() })
        chatFormat = features.get("chatFormat").asString()
    }
}