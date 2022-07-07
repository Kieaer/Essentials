package essentials.data

import essentials.Main.Companion.pluginRoot
import essentials.form.Configs
import essentials.form.Garbage.EqualsIgnoreCase
import essentials.internal.Bundle
import essentials.internal.Tool
import org.hjson.JsonArray
import org.hjson.JsonObject
import org.hjson.JsonValue
import org.hjson.Stringify
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

object Config : Configs() {
    lateinit var obj: JsonObject
    var locale: Locale = Locale.ENGLISH
    var bundle: Bundle = Bundle(locale)

    var networkMode = NetworkMode.Client
    var networkAddress: String = "127.0.0.1:5000"
    var nameFixed = false
    var motd = false
    var afktime = 0
    var blockEXP = false
    var levelUpAlarm = false
    var banShare = false
    var banTrust = JsonArray()
    var antiVPN = false
    var vote = false
    var logging = false
    var update = false
    var dbServer = false
    var dbUrl = "jdbc:h2:file:./config/mods/Essentials/data/player"
    var authType = AuthType.None
    var discordToken = "none"
    var debug = false
    var crashReport = false
    var saveTime = LocalTime.of(0, 10)
    var border = false
    var spawnLimit = 0
    var prefix = "[green][Essentials] []"

    enum class AuthType {
        None, Password, Discord
    }

    enum class NetworkMode {
        Server, Client
    }

    override fun createFile() {
        if(!pluginRoot.child("config.hjson").exists()) {
            obj = JsonObject().add("language", Locale.getDefault().toString())
            save()
        } else {
            obj = JsonValue.readHjson(pluginRoot.child("config.hjson").readString()).asObject()
        }
        load()
    }

    override fun save() {
        locale = Tool.textToLocale(obj.getString("language", locale.toString()))
        bundle = Bundle(locale)
        val config = JsonObject()
        val settings = JsonObject()
        val db = JsonObject()
        val network = JsonObject()
        val anti = JsonObject()
        val features = JsonObject()
        val auth = JsonObject()
        val discord = JsonObject()

        config.add("settings", settings, bundle["config-description"])
        config.add("network", network)
        config.add("antigrief", anti)
        config.add("features", features)
        config.add("auth", auth)

        // 플러그인 설정
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
        network.add("networkMode", networkMode.name.toLowerCase(), bundle["config.network"])
        network.add("networkAddress", networkAddress)
        network.add("banshare", banShare, bundle["config.server.banshare"])
        network.add("bantrust", banTrust, bundle["config.server.bantrust"])

        // 테러방지 설정
        anti.add("antivpn", antiVPN, bundle["config.anti-grief.vpn"])
        anti.add("nameFixed", nameFixed, bundle["config-strict-name-description"])

        // 특별한 기능 설정
        features.add("motd", motd)
        features.add("blockEXP", blockEXP, bundle["config.feature.exp.limit"])
        features.add("levelupalarm", levelUpAlarm, bundle["config.feature.exp.levelup-alarm"])
        features.add("vote", vote, bundle["config.feature.vote"])
        features.add("savetime", saveTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")), bundle["config.feature.save-time"])
        features.add("border", border, bundle["config.feature.border"])
        features.add("spawnlimit", spawnLimit, bundle["config.feature.spawn-limit"])
        features.add("afktime", afktime, bundle["config.feature.afktime"])

        // 로그인 설정
        auth.add("authType", authType.name.toLowerCase(), bundle["config.account.login.method"])

        // Discord 설정 (auth 상속)
        auth.add("discord", discord, bundle["config.feature.discord.desc"])
        discord.add("token", discordToken)

        obj = config
        pluginRoot.child("config.hjson").writeString(config.toString(Stringify.HJSON_COMMENTS))
    }

    override fun load() {
        val settings: JsonObject = obj["settings"].asObject()
        val lc = settings.getString("language", System.getProperty("user.language") + "_" + System.getProperty("user.country")).split(",").toTypedArray()[0]
        locale = if(lc.split("_").toTypedArray().size == 2) {
            val array = lc.split("_").toTypedArray()
            Locale(array[0], array[1])
        } else {
            Locale.ENGLISH
        }

        logging = settings.getBoolean("logging", true)
        update = settings.getBoolean("update", true)
        debug = settings.getBoolean("debug", false)
        crashReport = settings.getBoolean("crashreport", false)
        prefix = settings.getString("prefix", "[green][Essentials] []")

        val database = settings["database"].asObject()
        dbServer = database.getBoolean("DBServer", false)
        dbUrl = database.getString("DBurl", "jdbc:h2:file:./config/mods/Essentials/data/player")

        val network: JsonObject = obj["network"].asObject()
        networkMode = EqualsIgnoreCase(NetworkMode.values(), network.get("networkMode").asString(), NetworkMode.Client)
        networkAddress = network.getString("server-port", "127.0.0.1:5000")
        banShare = network.getBoolean("banshare", false)
        banTrust = if(network["bantrust"] == null) JsonValue.readJSON("[\"127.0.0.1\",\"localhost\"]").asArray() else network["bantrust"].asArray()

        val anti: JsonObject = obj["antigrief"].asObject()
        antiVPN = anti.getBoolean("antivpn", false)
        nameFixed = anti.getBoolean("strict-name", false)

        val features: JsonObject = obj["features"].asObject()
        motd = features.getBoolean("motd", false)
        blockEXP = features.getBoolean("explimit", false)
        levelUpAlarm = features.getBoolean("levelupalarm", false)
        vote = features.getBoolean("vote", true)
        saveTime = LocalTime.parse(features.getString("savetime", "00:10:00"), DateTimeFormatter.ofPattern("HH:mm:ss"))
        border = features.getBoolean("border", false)
        spawnLimit = features.getInt("spawnlimit", 500)
        afktime = features.getInt("afktime", 0)

        val auth: JsonObject = obj["auth"].asObject()
        authType = EqualsIgnoreCase(AuthType.values(), auth.get("authType").asString(), AuthType.None)

        val discord = auth["discord"].asObject()
        discordToken = discord.getString("token", "none")
    }
}
