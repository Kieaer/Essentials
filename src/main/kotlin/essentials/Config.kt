package essentials

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
    var afk = false
    var afkTime = 300
    var border = false
    var report = true
    var spawnLimit = 3000
    var vote = true
    var fixedName = true
    var antiVPN = false
    var pvpPeace = false
    var pvpPeaceTime = 300
    var rollbackTime = 1
    var antiGrief = false
    var countAllServers = false
    var destroyCore = false

    var authType = AuthType.None
    var chatFormat = "%1[orange] >[white] %2"

    var shareBanList = false
    var shareBanListType = BanType.Server
    var shareBanListServer = "mindustry.kr"

    var botToken = ""
    var channelToken = ""

    private val root: Fi = Core.settings.dataDirectory.child("mods/Essentials/config.txt")
    private var bundle: Bundle = Bundle(Locale.getDefault().toLanguageTag())

    private fun wizard() {
        Log.info(bundle["config.wiki"])
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
            if (System.getenv("DEBUG_KEY") == null) wizard()

            val plugin = JsonObject()
            plugin.add("update", update, bundle["config.update"])
            plugin.add("report", report, bundle["config.report"])
            plugin.add("authType", authType.toString(), bundle["config.authtype"])

            val features = JsonObject()
            features.add("afk", afk, bundle["config.afk"])
            features.add("afkTime", afkTime, bundle["config.afk.time"])
            features.add("border", border, bundle["config.border"])
            features.add("chatFormat", chatFormat, bundle["config.chatformat"])
            features.add("spawnLimit", spawnLimit, bundle["config.spawnlimit"])
            features.add("vote", vote, bundle["config.vote"])
            features.add("fixedName", fixedName, bundle["config.fixedname"])
            features.add("antiVPN", antiVPN, bundle["config.antivpn"])
            features.add("pvpPeace", pvpPeace, bundle["config.pvp"])
            features.add("pvpPeaceTime", pvpPeaceTime, bundle["config.pvp.time"])
            features.add("rollbackTime", rollbackTime, bundle["config.rollback.time"])
            features.add("antiGrief", antiGrief, bundle["config.antigrief"])
            features.add("countAllServers", countAllServers, bundle["config.countallservers"])
            features.add("destroyCore", destroyCore, bundle["config.destroycore"])

            val ban = JsonObject()
            ban.add("shareBanList", shareBanList, bundle["config.share.list"])
            ban.add("shareBanListType", shareBanListType.toString(), bundle["config.share.list.type"])
            ban.add("shareBanListServer", shareBanListServer, bundle["config.share.server"])

            val discord = JsonObject()
            discord.add("botToken", botToken, bundle["config.discord.token"])
            discord.add("channelToken", channelToken, bundle["config.discord.channel"])

            obj.setComment(bundle["config.detail", "https://github.com/Kieaer/Essentials/wiki/Config-detail-information"])
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
        pvpPeace = features.getBoolean("pvpPeace", pvpPeace)
        pvpPeaceTime = features.getInt("pvpPeaceTime", pvpPeaceTime)
        rollbackTime = features.getInt("rollbackTime", rollbackTime)
        antiGrief = features.getBoolean("antiGrief", antiGrief)
        countAllServers = features.getBoolean("countAllServers", countAllServers)
        destroyCore = features.getBoolean("destroyCore", destroyCore)

        botToken = discord.getString("botToken", botToken)
        channelToken = discord.getString("channelToken", channelToken)
    }
}