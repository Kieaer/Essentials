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
    private var obj = JsonObject()

    var update = true
    var channel = "release"

    var afk = false
    var afkTime = 300
    var border = false
    var report = true

    var authType = AuthType.None
    var chatFormat = "%1[orange] >[white] %2"

    var botToken = ""
    var channelToken = ""

    private val root: Fi = Core.settings.dataDirectory.child("mods/Essentials/config.txt")

    private fun wizard() {
        Log.info("Do you want to read Essentials plugin documents (y/N)")
        val sc = Scanner(System.`in`)

        if (sc.hasNextLine()) {
            when (sc.nextLine()) {
                "y", "Y" -> {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        Desktop.getDesktop().browse(URI("https://github.com/Kieaer/Essentials/wiki"))
                    }
                }

                else -> {

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

    fun save() {
        if (!root.exists()) {
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

            val discord = JsonObject()
            discord.add("botToken", botToken, "Set discord bot token")
            discord.add("channelToken", channelToken, "Set channel ID")

            obj.add("plugin", plugin)
            obj.add("features", features)
            obj.add("discord", discord)
            obj.add("database", "jdbc:sqlite:file:./config/mods/Essentials/database")

            root.writeString(obj.toString(Stringify.HJSON_COMMENTS))
        }
    }

    fun load() {
        if (!root.exists()) {
            save()
        }

        val config = JsonObject.readHjson(root.readString("utf-8")).asObject()
        val plugin = config.get("plugin").asObject()
        val features = config.get("features").asObject()
        val discord = config.get("discord").asObject()

        update = plugin.get("update").asBoolean()
        channel = plugin.get("channel").asString()
        afk = features.get("afk").asBoolean()
        afkTime = features.get("afkTime").asInt()
        border = features.get("border").asBoolean()
        report = plugin.get("report").asBoolean()
        authType = AuthType.valueOf(plugin.get("authType").asString().replaceFirstChar { it.uppercase() })
        chatFormat = features.get("chatFormat").asString()
        botToken = discord.get("botToken").asString()
        channelToken = discord.get("channelToken").asString()
    }
}