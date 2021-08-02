package essentials.data

import com.neovisionaries.i18n.CountryCode
import essentials.Main.Companion.pluginRoot
import essentials.form.Configs
import essentials.form.Garbage.EqualsIgnoreCase
import essentials.internal.Bundle
import org.hjson.JsonArray
import org.hjson.JsonObject
import org.hjson.JsonValue
import org.hjson.Stringify
import java.util.*

object Config : Configs() {
    var obj: JsonObject = JsonObject()

    /** 플러그인에 표시되는 언어 */
    var locale: Locale = Locale.US

    private var bundle: Bundle = Bundle(locale)

    /** 통신 모드 */
    var networkMode = NetworkMode.Disabled

    /** 서버 주소 */
    var networkAddress: String = "127.0.0.1:5000"

    /** 고정닉 설정 */
    var nameFixed = false

    /** 커스텀 motd */
    var motd = false

    /** 잠수 강퇴시간 설정 */
    var afktime = 0

    /** 블럭 설치/파괴시 경험치 */
    var blockEXP = false

    /** 서버에 레벨 업 알림 */
    var levelUpAlarm = false

    /** 밴 공유 기능 */
    var banShare = false

    /** 신뢰할 수 있는 밴 공유 서버 목록 */
    var banTrust = JsonArray()

    /** VPN 차단 */
    var antiVPN = false

    /** 플러그인 자체 투표기능 활성화 */
    var vote = false

    /** 플러그인 로그 설정 */
    var logging = true

    /** 자동 업데이트 설정 */
    var update = false

    /** DB 서버 모드 */
    var dbServer = false

    /** DB 서버 주소 */
    var dbUrl = "jdbc:h2:file:./config/mods/Essentials/data/player"

    /** 서버 인증 방식 */
    var authType = AuthType.None

    /** Discord 봇 토큰 */
    var discordBotToken = ""

    /** Discord 채널 토큰 */
    var discordChannelToken = ""

    /** 플러그인 디버그 모드 */
    var debug = false

    /** 자동 오류보고 기능 */
    var crashReport = false

    /* 맵 밖으로 나가면 사망 판정 설정 */
    var border = false

    /** /spawn 명령어로 소환할 수 있는 최대 유닛 수 */
    var spawnLimit = 0

    private val configFile = pluginRoot.child("config.hjson")

    enum class AuthType {
        None, Password, Discord
    }

    enum class NetworkMode {
        Server, Client, Disabled
    }

    override fun createFile() {
        if(!configFile.exists()) {
            save()
        } else {
            obj = JsonValue.readHjson(configFile.readString()).asObject()
        }
        load()
    }

    override fun save() {
        if(obj.has("settings")) locale = CountryCode.getByAlpha3Code(obj.get("settings").asObject().getString("language", locale.isO3Country)).toLocale()

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
        settings.add("language", locale.isO3Country.uppercase())
        settings.add("logging", logging, bundle["config.feature.logging"])
        settings.add("update", update, bundle["config.update"])
        settings.add("debug", debug, bundle["config.debug"])
        settings.add("crash-report", crashReport)

        // DB 설정 (settings 상속)
        settings.add("database", db)
        db.add("DBServer", dbServer)
        db.add("DBurl", dbUrl)

        // 네트워크 설정
        network.add("networkMode", networkMode.name.lowercase(Locale.getDefault()), bundle["config.network"])
        network.add("networkAddress", networkAddress)
        network.add("banshare", banShare, bundle["config.server.banshare"])
        network.add("bantrust", banTrust, bundle["config.server.bantrust"])

        // 테러방지 설정
        anti.add("antivpn", antiVPN, bundle["config.antigrief.vpn"])
        anti.add("nameFixed", nameFixed, bundle["config-strict-name-description"])

        // 특별한 기능 설정
        features.add("motd", motd)
        features.add("blockEXP", blockEXP, bundle["config.feature.exp.limit"])
        features.add("levelupalarm", levelUpAlarm, bundle["config.feature.exp.levelup-alarm"])
        features.add("vote", vote, bundle["config.feature.vote"])
        features.add("border", border, bundle["config.feature.border"])
        features.add("spawnlimit", spawnLimit, bundle["config.feature.spawn-limit"])
        features.add("afktime", afktime, bundle["config.feature.afktime"])

        // 로그인 설정
        auth.add("authType", authType.name.lowercase(Locale.getDefault()), bundle["config.account.login.method"])

        // Discord 설정 (auth 상속)
        auth.add("discord", discord, bundle["config.feature.discord.desc"])
        discord.add("bot-token", discordBotToken)
        discord.add("channel-token", discordChannelToken)

        obj = config
        configFile.writeString(config.toString(Stringify.HJSON_COMMENTS))
    }

    override fun load() {
        val settings: JsonObject = obj["settings"].asObject()
        locale = CountryCode.getByAlpha3Code(obj.get("settings").asObject().getString("language", locale.isO3Country)).toLocale()

        logging = settings.getBoolean("logging", true)
        update = settings.getBoolean("update", true)
        debug = settings.getBoolean("debug", false)
        crashReport = settings.getBoolean("crashreport", false)

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
        border = features.getBoolean("border", false)
        spawnLimit = features.getInt("spawnlimit", 500)
        afktime = features.getInt("afktime", 0)

        val auth: JsonObject = obj["auth"].asObject()
        authType = EqualsIgnoreCase(AuthType.values(), auth.get("authType").asString(), AuthType.None)

        val discord = auth["discord"].asObject()
        discordBotToken = discord.getString("bot-token", "")
        discordChannelToken = discord.getString("channel-token", "")
    }
}
