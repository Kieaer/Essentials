package essentials

import arc.Core
import arc.files.Fi
import arc.util.Log
import org.hjson.JsonObject
import org.hjson.Stringify
import java.awt.Desktop
import java.net.URI
import java.util.*
import java.util.regex.Pattern

object Config {
    private var obj = JsonObject()
    private val root : Fi = Core.settings.dataDirectory.child("mods/Essentials/config.txt")
    private var bundle : Bundle = Bundle(Locale.getDefault().toLanguageTag())
    private val allowLanguageRegex : Pattern = Pattern.compile("en|ja|ko|ru|uk|zh")

    var update = true
    var report = true
    var database : String = "postgresql://127.0.0.1:5432/essentials"
    var databaseID : String = "postgres"
    var databasePW : String = ""
    var authType = AuthType.None
    var banList : String = Core.settings.dataDirectory.child("mods/Essentials/ban.txt").absolutePath()

    var afk = false
    var afkTime = 300
    var afkServer = ""
    var border = false
    var spawnLimit = 3000
    var vote = true
    var votekick = false
    var fixedName = true
    var antiVPN = false
    var pvpPeace = false
    var pvpPeaceTime = 300
    var pvpSpector = false
    var rollbackTime = 10
    var message = false
    var messageTime = 10
    var antiGrief = false
    var countAllServers = false
    var destroyCore = false
    var chatlimit = false
    var chatlanguage = "ko,en"
    var chatBlacklist = false
    var chatBlacklistRegex = false
    var expDisplay = false
    var expAlert = false
    var minimalName = false
    var blockfooclient = false
    var allowMobile = true
    var blockIP = false
    var waveskip = 1
    var unbreakableCore = false
    var moveEffects = true
    var blockNewUser = false
    var webServer = false
    var webServerPort = 8123
    var restAPIRequestsLimit = 5
    var restAPILimitRefillPeriod = 30
    var skiplimit = 10
    var pvpAutoTeam = true

    var chatFormat = "%1[orange] >[white] %2"

    var shareBanListServer = "127.0.0.1"

    var discordURL = ""

    private var configVersion = 20

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
            override fun toString() : String {
                return "none"
            }
        },
        Password {
            override fun toString() : String {
                return "password"
            }
        },
        Discord {
            override fun toString() : String {
                return "discord"
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
        plugin.add("databaseID", databaseID, bundle["config.database.id"])
        plugin.add("databasePW", databasePW, bundle["config.database.pw"])
        plugin.add("banList", banList, bundle["config.banlist"])

        val features = JsonObject()
        features.add("afk", afk, bundle["config.afk"])
        features.add("afkTime", afkTime, bundle["config.afk.time"])
        features.add("afkServer", afkServer, bundle["config.afk.server"])
        features.add("border", border, bundle["config.border"])
        features.add("chatFormat", chatFormat, bundle["config.chatformat"])
        features.add("spawnLimit", spawnLimit, bundle["config.spawnlimit"])
        features.add("vote", vote, bundle["config.vote"])
        features.add("fixedName", fixedName, bundle["config.fixedname"])
        features.add("antiVPN", antiVPN, bundle["config.antivpn"])
        features.add("pvpPeace", pvpPeace, bundle["config.pvp"])
        features.add("pvpPeaceTime", pvpPeaceTime, bundle["config.pvp.time"])
        features.add("pvpSpector", pvpSpector, bundle["config.pvp.spector"])
        features.add("rollbackTime", rollbackTime, bundle["config.rollback.time"])
        features.add("message", message, bundle["config.message"])
        features.add("messageTime", messageTime, bundle["config.message.time"])
        features.add("countAllServers", countAllServers, bundle["config.countallservers"])
        features.add("destroyCore", destroyCore, bundle["config.destroycore"])
        features.add("chatlimit", chatlimit, bundle["config.chatlimit"])
        features.add("chatlanguage", chatlanguage, bundle["config.chatlanguage"])
        features.add("chatBlacklist", chatBlacklist, bundle["config.chatblacklist"])
        features.add("chatBlacklistRegex", chatBlacklistRegex, bundle["config.chatblacklist.regex"])
        features.add("expDisplay", expDisplay, bundle["config.expDisplay"])
        features.add("expAlert", expAlert, bundle["config.expAlert"])
        features.add("blockIP", blockIP, bundle["config.blockIP"])
        features.add("waveskip", waveskip, bundle["config.waveskip"])
        features.add("unbreakableCore", unbreakableCore, bundle["config.unbreakablecore"])
        features.add("moveEffects", moveEffects, bundle["config.moveeffects"])
        features.add("webServer", webServer, bundle["config.webserver"])
        features.add("webServerPort", webServerPort, bundle["config.webserver.port"])
        features.add("restAPIRequestsLimit", restAPIRequestsLimit, bundle["config.webserver.limit"])
        features.add("restAPILimitRefillPeriod", restAPILimitRefillPeriod, bundle["config.webserver.limit.reset"])
        features.add("skiplimit", skiplimit, bundle["config.skiplimit"])
        features.add("pvpAutoTeam", pvpAutoTeam, bundle["config.pvpAutoTeam"])

        val ban = JsonObject()
        ban.add("shareBanListServer", shareBanListServer, bundle["config.share.server"])

        val security = JsonObject()
        security.add("votekick", votekick, bundle["config.votekick"])
        security.add("antiGrief", antiGrief, bundle["config.antigrief"])
        security.add("minimumName", minimalName, bundle["config.minimumName"])
        security.add("blockfooclient", blockfooclient, bundle["config.blockfooclient"])
        security.add("allowMobile", allowMobile, bundle["config.allow.mobile"])
        security.add("blockNewUser", blockNewUser, bundle["config.blocknewuser"])

        val discord = JsonObject()
        discord.add("discordURL", discordURL, bundle["config.discord.url"])

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
        databaseID = plugin.getString("databaseID", databaseID)
        databasePW = plugin.getString("databasePW", databasePW)
        banList = plugin.getString("banList", banList)

        afk = features.getBoolean("afk", afk)
        afkTime = features.getInt("afkTime", afkTime)
        afkServer = features.getString("afkServer", afkServer)
        border = features.getBoolean("border", border)
        chatFormat = features.getString("chatFormat", chatFormat)
        spawnLimit = features.getInt("spawnLimit", spawnLimit)
        vote = features.getBoolean("vote", vote)
        fixedName = features.getBoolean("fixedName", fixedName)
        antiVPN = features.getBoolean("antiVPN", antiVPN)
        pvpPeace = features.getBoolean("pvpPeace", pvpPeace)
        pvpPeaceTime = features.getInt("pvpPeaceTime", pvpPeaceTime)
        pvpSpector = features.getBoolean("pvpSpector", pvpSpector)
        rollbackTime = features.getInt("rollbackTime", rollbackTime)
        message = features.getBoolean("message", message)
        messageTime = features.getInt("messageTime", messageTime)
        countAllServers = features.getBoolean("countAllServers", countAllServers)
        destroyCore = features.getBoolean("destroyCore", destroyCore)
        chatlimit = features.getBoolean("chatlimit", chatlimit)
        chatlanguage = features.getString("chatlanguage", chatlanguage)
        chatBlacklist = features.getBoolean("chatBlacklist", chatBlacklist)
        chatBlacklistRegex = features.getBoolean("chatBlacklistRegex", chatBlacklistRegex)
        expDisplay = features.getBoolean("expDisplay", expDisplay)
        expAlert = features.getBoolean("expAlert", expAlert)
        blockIP = features.getBoolean("blockIP", blockIP)
        waveskip = features.getInt("waveskip", waveskip)
        unbreakableCore = features.getBoolean("unbreakableCore", unbreakableCore)
        moveEffects = features.getBoolean("moveEffects", moveEffects)
        webServer = features.getBoolean("webServer", webServer)
        webServerPort = features.getInt("webServerPort", webServerPort)
        restAPIRequestsLimit = features.getInt("restAPIRequestsLimit", restAPIRequestsLimit)
        restAPILimitRefillPeriod = features.getInt("restAPILimitRefillPeriod", restAPILimitRefillPeriod)
        skiplimit = features.getInt("skiplimit", skiplimit)
        pvpAutoTeam = features.getBoolean("pvpAutoTeam", pvpAutoTeam)

        votekick = security.getBoolean("votekick", votekick)
        antiGrief = security.getBoolean("antiGrief", antiGrief)
        minimalName = security.getBoolean("minimalName", minimalName)
        blockfooclient = security.getBoolean("blockfooclient", blockfooclient)
        allowMobile = security.getBoolean("allowMobile", allowMobile)
        blockNewUser = security.getBoolean("blockNewUser", blockNewUser)

        discordURL = discord.getString("discordURL", discordURL)

        if (chatlimit) {
            if (!allowLanguageRegex.matcher(chatlanguage).matches()) {
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