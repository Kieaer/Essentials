package essentials

import essentials.Main.Companion.pluginRoot
import essentials.internal.Bundle
import essentials.internal.Log
import essentials.internal.Tool
import org.hjson.JsonArray
import org.hjson.JsonObject
import org.hjson.JsonValue
import org.hjson.Stringify
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

object Config {
    lateinit var obj: JsonObject
    var locale: Locale = Locale.getDefault()
    var bundle: Bundle = Bundle(locale)

    var version = 0
    var language: Locale = Locale.getDefault()
    var serverEnable = false
    var serverPort = 25000
    var clientEnable = false
    var clientPort = 25000
    var clientHost: String = "mindustry.kr"
    var strictName = false
    var cupdatei = 0
    var afktime: Long = 0L
    var scanResource = false
    var antiGrief = false
    var alertAction = false
    var expLimit = false
    var baseXp = 0.0
    var exponent = 0.0
    var levelUpAlarm = false
    var alarmLevel = 0
    var banShare = false
    var banTrust: JsonArray = JsonArray()
    var antiVPN = false
    var antiRush = false
    var antiRushtime: Long = 0
    var vote = false
    var logging = false
    var update = false
    var dbServer = false
    var dbUrl: String = "jdbc:h2:file:./config/mods/Essentials/data/player"
    var loginEnable = false
    var passwordMethod: String = "password"
    var autoLogin = false
    var discordToken: String = "none"
    var discordLink: String = "none"
    var translate = false
    var translateId: String = "none"
    var translatePw: String = "none"
    var debug = false
    var crashReport = false
    var saveTime: LocalTime = LocalTime.of(0, 10)
    var rollback = false
    var slotNumber = 0
    var autoDifficulty = false
    var difficultyEasy = 0
    var difficultyNormal = 0
    var difficultyHard = 0
    var difficultyInsane = 0
    var border = false
    var spawnLimit = 0
    var prefix: String = "[green][Essentials] []"

    fun init() {
        val settings: JsonObject
        val database: JsonObject
        val network: JsonObject
        val anti: JsonObject
        val features: JsonObject
        val difficulty: JsonObject
        val tr: JsonObject
        val auth: JsonObject
        val discord: JsonObject

        if (!pluginRoot.child("config.hjson").exists()) {
            val empty = JsonObject()
            obj = JsonObject()
            obj.add("settings", JsonObject().add("database", empty))
            obj.add("network", empty)
            obj.add("antigrief", empty)
            obj.add("features", JsonObject().add("difficulty", empty).add("translate", empty))
            obj.add("auth", JsonObject().add("discord", empty))
        } else {
            obj = JsonValue.readHjson(pluginRoot.child("config.hjson").readString()).asObject()
        }

        settings = obj["settings"].asObject()
        version = settings.getInt("version", PluginVars.configVersion)
        val lc = settings.getString("language", System.getProperty("user.language") + "_" + System.getProperty("user.country")).split(",").toTypedArray()[0]
        language = if (lc.split("_").toTypedArray().size == 2) {
            val array = lc.split("_").toTypedArray()
            Locale(array[0], array[1])
        } else {
            Locale(System.getProperty("user.language") + "_" + System.getProperty("user.country"))
        }
        locale = language
        logging = settings.getBoolean("logging", true)
        update = settings.getBoolean("update", true)
        debug = settings.getBoolean("debug", false)
        crashReport = settings.getBoolean("crashreport", true)
        prefix = settings.getString("prefix", "[green][Essentials] []")

        database = settings["database"].asObject()
        dbServer = database.getBoolean("DBServer", false)
        dbUrl = database.getString("DBurl", "jdbc:h2:file:./config/mods/Essentials/data/player")

        network = obj["network"].asObject()
        serverEnable = network.getBoolean("server-enable", false)
        serverPort = network.getInt("server-port", 25000)
        clientEnable = network.getBoolean("client-enable", false)
        clientPort = network.getInt("client-port", 25000)
        clientHost = network.getString("client-host", "mindustry.kr")
        banShare = network.getBoolean("banshare", false)
        banTrust = if (network["bantrust"] == null) JsonValue.readJSON("[\"127.0.0.1\",\"localhost\"]").asArray() else network["bantrust"].asArray()

        anti = obj["antigrief"].asObject()
        antiGrief = anti.getBoolean("antigrief", false)
        antiVPN = anti.getBoolean("antivpn", false)
        antiRush = anti.getBoolean("antirush", false)
        antiRushtime = if (anti["antirushtime"] == null || anti["antirushtime"].isString) 0L else anti.getLong("antirushtime", 600)
        alertAction = anti.getBoolean("alert-action", false)
        strictName = anti.getBoolean("strict-name", false)
        scanResource = anti.getBoolean("scanresource", false)

        features = obj["features"].asObject()
        expLimit = features.getBoolean("explimit", false)
        baseXp = features.getDouble("basexp", 500.0)
        exponent = features.getDouble("exponent", 1.12)
        levelUpAlarm = features.getBoolean("levelupalarm", false)
        alarmLevel = features.getInt("alarm-minimal-level", 20)
        vote = features.getBoolean("vote", true)
        saveTime = LocalTime.parse(features.getString("savetime", "00:10:00"), DateTimeFormatter.ofPattern("HH:mm:ss"))
        rollback = features.getBoolean("rollback", false)
        slotNumber = features.getInt("slotnumber", 1000)
        border = features.getBoolean("border", false)
        spawnLimit = features.getInt("spawnlimit", 500)
        cupdatei = features.getInt("cupdatei", 1000)
        afktime = features.getLong("afktime", 0)

        difficulty = features["difficulty"].asObject()
        autoDifficulty = difficulty.getBoolean("auto-difficulty", false)
        difficultyEasy = difficulty.getInt("easy", 2)
        difficultyNormal = difficulty.getInt("normal", 4)
        difficultyHard = difficulty.getInt("hard", 6)
        difficultyInsane = difficulty.getInt("insane", 10)

        tr = features["translate"].asObject()
        translate = tr.getBoolean("translate", false)
        translateId = tr.getString("translateid", "none")
        translatePw = tr.getString("translatepw", "none")

        auth = obj["auth"].asObject()
        loginEnable = auth.getBoolean("loginenable", false)
        passwordMethod = auth.getString("loginmethod", "password")
        autoLogin = auth.getBoolean("autologin", true)

        discord = auth["discord"].asObject()
        discordToken = discord.getString("token", "none")
        discordLink = discord.getString("link", "none")
        updateConfig()
    }

    fun updateConfig() {
        locale = Tool.textToLocale(obj.getString("language", locale.toString()))
        bundle = Bundle(locale)
        if (obj.getInt("version", 0) < PluginVars.configVersion) Log.info("config.updated")
        val config = JsonObject()
        val settings = JsonObject()
        val db = JsonObject()
        val network = JsonObject()
        val anti = JsonObject()
        val features = JsonObject()
        val difficulty = JsonObject()
        val auth = JsonObject()
        val discord = JsonObject()
        val tr = JsonObject()
        config.add("settings", settings, bundle["config-description"])
        config.add("network", network)
        config.add("antigrief", anti)
        config.add("features", features)
        config.add("auth", auth)

        // 플러그인 설정
        settings.add("version", version, bundle["config.version"])
        settings.add("language", language.toString(), bundle["config.language.description"])
        settings.add("logging", logging, bundle["config.feature.logging"])
        settings.add("update", update, bundle["config.update"])
        settings.add("debug", debug, bundle["config.debug"])
        settings.add("crash-report", crashReport)
        settings.add("prefix", prefix, bundle["config.prefix"])

        // DB 설정 (settings 상속)
        settings.add("database", db)
        db.add("DBServer", dbServer)
        db.add("DBurl", dbUrl)

        // 네트워크 설정
        network.add("server-enable", serverEnable, bundle["config.network"])
        network.add("server-port", serverPort)
        network.add("client-enable", clientEnable)
        network.add("client-port", clientPort)
        network.add("client-host", clientHost)
        network.add("banshare", banShare, bundle["config.server.banshare"])
        network.add("bantrust", banTrust, bundle["config.server.bantrust"])

        // 테러방지 설정
        anti.add("antigrief", antiGrief, bundle["config.anti-grief.desc"])
        anti.add("antivpn", antiVPN, bundle["config.anti-grief.vpn"])
        anti.add("antirush", antiRush, bundle["config.anti-grief.pvprush"])
        anti.add("antirushtime", antiRushtime)
        anti.add("alert-action", alertAction, bundle["config-alert-action-description"])
        anti.add("strict-name", strictName, bundle["config-strict-name-description"])
        anti.add("scanresource", scanResource, bundle["config.anti-grief.scan-resource"])

        // 특별한 기능 설정
        features.add("explimit", expLimit, bundle["config.feature.exp.limit"])
        features.add("basexp", baseXp, bundle["config.feature.exp.basexp"])
        features.add("exponent", exponent, bundle["config.feature.exp.exponent"])
        features.add("levelupalarm", levelUpAlarm, bundle["config.feature.exp.levelup-alarm"])
        features.add("alarm-minimal-level", alarmLevel, bundle["config.feature.exp.minimal-level"])
        features.add("vote", vote, bundle["config.feature.vote"])
        features.add("savetime", saveTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")), bundle["config.feature.save-time"])
        features.add("rollback", rollback, bundle["config.feature.slot-number"])
        features.add("slotnumber", slotNumber)
        features.add("border", border, bundle["config.feature.border"])
        features.add("spawnlimit", spawnLimit, bundle["config.feature.spawn-limit"])
        features.add("cupdatei", cupdatei, bundle["config.feature.colornick"])
        features.add("afktime", afktime, bundle["config.feature.afktime"])

        // 난이도 설정 (features 상속)
        features.add("difficulty", difficulty, bundle["config.auto-difficulty"])
        difficulty.add("auto-difficulty", autoDifficulty)
        difficulty.add("easy", difficultyEasy)
        difficulty.add("normal", difficultyNormal)
        difficulty.add("hard", difficultyHard)
        difficulty.add("insane", difficultyInsane)

        // 번역 설정 (features 상속)
        features.add("translate", tr, bundle["config.feature.papago"])
        tr.add("translate", translate)
        tr.add("translateid", translateId)
        tr.add("translatepw", translatePw)

        // 로그인 설정
        auth.add("loginenable", loginEnable, bundle["config.account.login"])
        auth.add("loginmethod", passwordMethod, bundle["config.account.login.method"])
        auth.add("autologin", autoLogin)

        // Discord 설정 (auth 상속)
        auth.add("discord", discord, bundle["config.feature.discord.desc"])
        discord.add("token", discordToken)
        discord.add("link", discordLink)
        pluginRoot.child("config.hjson").writeString(config.toString(Stringify.HJSON_COMMENTS))
    }
}
