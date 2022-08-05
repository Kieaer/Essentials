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
    var spawnLimit = 3000
    var vote = true
    var fixedName = true
    var antiVPN = false

    var authType = AuthType.None
    var chatFormat = "%1[orange] >[white] %2"

    var shareBanList = false
    var shareBanListType = BanType.Server
    var shareBanListServer = ""

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

    enum class BanType {
        Server {
            override fun toString(): String {
                return "server"
            }
        },
        Client {
            override fun toString(): String {
                return "client"
            }
        },
        Global {
            override fun toString(): String {
                return "global"
            }
        }
    }

    fun save() {
        if (!root.exists()) {
            wizard()

            val plugin = JsonObject()
            plugin.add("update", update, "Plugin auto update")
            plugin.add("channel", channel, "Plugin update release channel. (release/beta/dev)")
            plugin.add("report", report, "Auto report plugin error")
            plugin.add("authType", authType.toString(), "Server authorize type (none, password, discord)")

            val features = JsonObject()
            features.add("afk", afk, "Automatically kick AFK users.")
            features.add("afkTime", afkTime, "Automatic kick time (unit: seconds)")
            features.add("border", border, "Destroy units that moved out of the world")
            features.add("chatFormat", chatFormat, "Set default chat format (%1 is name, %2 is chat)")
            features.add("spawnLimit", spawnLimit, "Specifies the maximum number of units that can be accommodated in the world.")
            features.add("vote", vote, "Use the voting feature provided by the plugin, not the voting feature built into the game.")
            features.add("fixedName", fixedName, "Even if the player joins after changing the name, it's forcibly changed to the original nickname.")
            features.add("antiVPN", antiVPN, "Block access to the IP used by the VPN service.")

            val ban = JsonObject()
            ban.add("shareBanList", shareBanList, "Share the current server's ban list with other servers.")
            ban.add("shareBanListType", shareBanListType.toString(), "Set the sharing mode. (server/client/global)")

            val discord = JsonObject()
            discord.add("botToken", botToken, "Set discord bot token")
            discord.add("channelToken", channelToken, "Set channel ID")

            obj.setComment("See https://github.com/Kieaer/Essentials/wiki/Config-detail-information for a detailed explanation.")
            obj.add("plugin", plugin)
            obj.add("features", features)
            obj.add("discord", discord)
            obj.add("ban", ban)
            obj.add("database", "jdbc:sqlite:file:./config/mods/Essentials/database")

            root.writeString(obj.toString(Stringify.HJSON_COMMENTS))
        }
    }

    fun load() {
        if (!root.exists()) save()

        val config = JsonObject.readHjson(root.readString("utf-8")).asObject()
        val plugin = config.get("plugin").asObject()
        val features = config.get("features").asObject()
        val discord = config.get("discord").asObject()

        update = plugin.getBoolean("update", update)
        channel = plugin.getString("channel", channel)
        report = plugin.getBoolean("report", report)
        authType = AuthType.valueOf(plugin.get("authType").asString().replaceFirstChar { it.uppercase() })

        afk = features.getBoolean("afk", afk)
        afkTime = features.getInt("afkTime", afkTime)
        border = features.getBoolean("border", border)
        chatFormat = features.getString("chatFormat", chatFormat)
        spawnLimit = features.getInt("spawnLimit", spawnLimit)
        vote = features.getBoolean("vote", vote)
        fixedName = features.getBoolean("fixedName", fixedName)
        antiVPN = features.getBoolean("antiVPN", antiVPN)

        botToken = discord.getString("botToken", botToken)
        channelToken = discord.getString("channelToken", channelToken)
    }
}