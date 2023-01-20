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
    var report = true
    var authType = AuthType.None
    var database: String = Core.settings.dataDirectory.child("mods/Essentials/database.db").absolutePath()

    var afk = false
    var afkServer = ""
    var afkTime = 300
    var antiVPN = false
    var blockIP = false
    var border = false
    var chatBlacklist = false
    var chatBlacklistRegex = false
    var chatlanguage = "ko,en"
    var chatlimit = false
    var countAllServers = false
    var destroyCore = false
    var expDisplay = false
    var fixedName = true
    var message = false
    var messageTime = 10
    var moveEffects = true
    var pvpPeace = false
    var pvpPeaceTime = 300
    var pvpSpector = true
    var rollbackTime = 10
    var spawnLimit = 3000
    var unbreakableCore = false
    var vote = true
    var waveskip = 1

    var chatFormat = "%1[orange] >[white] %2"

    // security
    var votekick = false
    var antiGrief = false
    var minimalName = false
    var blockfooclient = false
    var allowMobile = true

    // server
    var shareBanList = false
    var shareBanListType = BanType.Server
    var shareBanListServer = "mindustry.kr"

    // discord
    var botToken = ""
    var channelToken = ""
    var discordURL = ""
    var banChannelToken = ""

    private var configVersion = 12

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
        if (System.getenv("DEBUG_KEY") == null && !root.exists()) wizard()

        val plugin = JsonObject()
        plugin.add("update", update, bundle["config.update"])
        plugin.add("report", report, bundle["config.report"])
        plugin.add("authType", authType.toString(), bundle["config.authtype"])
        plugin.add("database", database, bundle["config.database"])

        val features = JsonObject()
        features.add("afk", afk, bundle["config.afk"])
        features.add("afkServer", afkServer, bundle["config.afk.server"])
        features.add("afkTime", afkTime, bundle["config.afk.time"])
        features.add("antiVPN", antiVPN, bundle["config.antivpn"])
        features.add("blockIP", blockIP, bundle["config.blockIP"])
        features.add("border", border, bundle["config.border"])
        features.add("chatBlacklist", chatBlacklist, bundle["config.chatblacklist"])
        features.add("chatBlacklistRegex", chatBlacklistRegex, bundle["config.chatblacklist.regex"])
        features.add("chatFormat", chatFormat, bundle["config.chatformat"])
        features.add("chatlanguage", chatlanguage, bundle["config.chatlanguage"])
        features.add("chatlimit", chatlimit, bundle["config.chatlimit"])
        features.add("countAllServers", countAllServers, bundle["config.countallservers"])
        features.add("destroyCore", destroyCore, bundle["config.destroycore"])
        features.add("expDisplay", expDisplay, bundle["config.expDisplay"])
        features.add("fixedName", fixedName, bundle["config.fixedname"])
        features.add("message", message, bundle["config.message"])
        features.add("messageTime", messageTime, bundle["config.message.time"])
        features.add("moveEffects", moveEffects, bundle["config.moveeffects"])
        features.add("pvpPeace", pvpPeace, bundle["config.pvp"])
        features.add("pvpPeaceTime", pvpPeaceTime, bundle["config.pvp.time"])
        features.add("pvpSpector", pvpSpector, bundle["config.pvp.spector"])
        features.add("rollbackTime", rollbackTime, bundle["config.rollback.time"])
        features.add("spawnLimit", spawnLimit, bundle["config.spawnlimit"])
        features.add("unbreakableCore", unbreakableCore, bundle["config.unbreakablecore"])
        features.add("vote", vote, bundle["config.vote"])
        features.add("waveskip", waveskip, bundle["config.waveskip"])

        val ban = JsonObject()
        ban.add("shareBanList", shareBanList, bundle["config.share.list"])
        ban.add("shareBanListType", shareBanListType.toString(), bundle["config.share.list.type"])
        ban.add("shareBanListServer", shareBanListServer, bundle["config.share.server"])

        val security = JsonObject()
        security.add("votekick", votekick, bundle["config.votekick"])
        security.add("antiGrief", antiGrief, bundle["config.antigrief"])
        security.add("minimumName", minimalName, bundle["config.minimumName"])
        security.add("blockfooclient", blockfooclient, bundle["config.blockfooclient"])
        security.add("allowMobile", allowMobile, bundle["config.allow.mobile"])

        val discord = JsonObject()
        discord.add("botToken", botToken, bundle["config.discord.token"])
        discord.add("channelToken", channelToken, bundle["config.discord.channel"])
        discord.add("discordURL", discordURL, bundle["config.discord.url"])
        discord.add("banChannelToken", banChannelToken, bundle["config.discord.ban"])

        obj.setComment(bundle["config.detail", "https://github.com/Kieaer/Essentials/wiki/Config-detail-information"])
        obj.add("plugin", plugin)
        obj.add("features", features)
        obj.add("discord", discord)
        obj.add("ban", ban)
        obj.add("security", security)
        obj.add("configVersion", configVersion)

        root.writeString(obj.toString(Stringify.HJSON_COMMENTS))
    }

    fun load() {
        val config = JsonObject.readHjson(root.readString("utf-8")).asObject()
        val plugin = config.get("plugin").asObject()
        val features = config.get("features").asObject()
        val discord = config.get("discord").asObject()
        val security = config.get("security").asObject()

        update = plugin.getBoolean("update", update)
        report = plugin.getBoolean("report", report)
        authType = AuthType.valueOf(plugin.get("authType").asString().replaceFirstChar { it.uppercase() })
        database = plugin.getString("database", database)

        afk = features.getBoolean("afk", afk)
        afkServer = features.getString("afkServer", afkServer)
        afkTime = features.getInt("afkTime", afkTime)
        antiVPN = features.getBoolean("antiVPN", antiVPN)
        blockIP = features.getBoolean("blockIP", blockIP)
        border = features.getBoolean("border", border)
        chatBlacklist = features.getBoolean("chatBlacklist", chatBlacklist)
        chatBlacklistRegex = features.getBoolean("chatBlacklistRegex", chatBlacklistRegex)
        chatFormat = features.getString("chatFormat", chatFormat)
        chatlanguage = features.getString("chatlanguage", chatlanguage)
        chatlimit = features.getBoolean("chatlimit", chatlimit)
        countAllServers = features.getBoolean("countAllServers", countAllServers)
        destroyCore = features.getBoolean("destroyCore", destroyCore)
        expDisplay = features.getBoolean("expDisplay", expDisplay)
        fixedName = features.getBoolean("fixedName", fixedName)
        message = features.getBoolean("message", message)
        messageTime = features.getInt("messageTime", messageTime)
        moveEffects = features.getBoolean("moveEffects", moveEffects)
        pvpPeace = features.getBoolean("pvpPeace", pvpPeace)
        pvpPeaceTime = features.getInt("pvpPeaceTime", pvpPeaceTime)
        pvpSpector = features.getBoolean("pvpSpector", pvpSpector)
        rollbackTime = features.getInt("rollbackTime", rollbackTime)
        spawnLimit = features.getInt("spawnLimit", spawnLimit)
        unbreakableCore = features.getBoolean("unbreakableCore", unbreakableCore)
        vote = features.getBoolean("vote", vote)
        waveskip = features.getInt("waveskip", waveskip)

        votekick = security.getBoolean("votekick", votekick)
        antiGrief = security.getBoolean("antiGrief", antiGrief)
        minimalName = security.getBoolean("minimalName", minimalName)
        blockfooclient = security.getBoolean("blockfooclient", blockfooclient)
        allowMobile = security.getBoolean("allowMobile", allowMobile)

        botToken = discord.getString("botToken", botToken)
        channelToken = discord.getString("channelToken", channelToken)
        discordURL = discord.getString("discordURL", discordURL)
        banChannelToken = discord.getString("banChannelToken", banChannelToken)

        if (chatlimit) {
            if (!chatlanguage.matches(Regex("en|ja|ko|ru|uk|zh"))) {
                chatlimit = false
            }
        }
    }

    fun update() {
        val version = JsonObject.readHjson(root.readString("utf-8")).asObject().getInt("configVersion", 1)
        if (configVersion > version) {
            save()
        }
    }
}