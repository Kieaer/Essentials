package essentials

import essentials.internal.Tool
import org.hjson.JsonObject
import java.util.*

class PlayerData {
    var name: String = "none"
    var uuid: String = "none"
    var country: String = Locale.getDefault().displayCountry
    var countryCode: String = Locale.getDefault().toLanguageTag()
    var language: String = Locale.getDefault().displayLanguage
    var admin: Boolean = false
    var placecount: Int = 0
    var breakcount: Int = 0
    var joincount: Int = 0
    var kickcount: Int = 0
    var level: Int = 0
    var exp: Int = 0
    var reqexp: Int = 0
    var firstdate: Long = 0L
    var lastdate: Long = 0L
    var playtime: Long = 0L
    var attackclear: Int = 0
    var pvpwincount: Int = 0
    var pvplosecount: Int = 0
    var bantime: Long = 0L
    var crosschat: Boolean = false
    var colornick: Boolean = false
    var permission: String = "default"
    var mute: Boolean = false
    var udid: Long = 0L
    var accountid: String? = null
    var accountpw: String? = null
    var afk: Long = 0L
    var x: Int = 0
    var y: Int = 0
    var locale: Locale = Locale.getDefault()
    var isNull: Boolean = true

    constructor()

    constructor(name: String, uuid: String, country: String, countryCode: String, language: String, isAdmin: Boolean, placecount: Int, breakcount: Int, killcount: Int, deathcount: Int, joincount: Int, kickcount: Int, level: Int, exp: Int, reqexp: Int, firstdate: Long, lastdate: Long, lastplacename: String, lastbreakname: String, lastchat: String, playtime: Long, attackclear: Int, pvpwincount: Int, pvplosecount: Int, pvpbreakout: Int, reactorcount: Int, bantime: Long, crosschat: Boolean, colornick: Boolean, connected: Boolean, connserver: String, permission: String, mute: Boolean, alert: Boolean, udid: Long, accountid: String, accountpw: String, login: Boolean) {
        this.name = name
        this.uuid = uuid
        this.country = country
        this.countryCode = countryCode
        this.language = language
        this.admin = isAdmin
        this.placecount = placecount
        this.breakcount = breakcount
        this.joincount = joincount
        this.kickcount = kickcount
        this.level = level
        this.exp = exp
        this.reqexp = reqexp
        this.firstdate = firstdate
        this.lastdate = lastdate
        this.playtime = playtime
        this.attackclear = attackclear
        this.pvpwincount = pvpwincount
        this.pvplosecount = pvplosecount
        this.bantime = bantime
        this.crosschat = crosschat
        this.colornick = colornick
        this.permission = permission
        this.mute = mute
        this.udid = udid
        this.accountid = accountid
        this.accountpw = accountpw
        locale = Tool.textToLocale(countryCode)
    }

    fun toMap(): JsonObject {
        val map = JsonObject()
        map.add("name", name)
        map.add("uuid", uuid)
        map.add("country", country)
        map.add("countryCode", countryCode)
        map.add("language", language)
        map.add("isAdmin", admin)
        map.add("placecount", placecount)
        map.add("breakcount", breakcount)
        map.add("joincount", joincount)
        map.add("kickcount", kickcount)
        map.add("level", level)
        map.add("exp", exp)
        map.add("reqexp", reqexp)
        map.add("firstdate", firstdate)
        map.add("lastdate", lastdate)
        map.add("playtime", playtime)
        map.add("attackclear", attackclear)
        map.add("pvpwincount", pvpwincount)
        map.add("pvplosecount", pvplosecount)
        map.add("bantime", bantime)
        map.add("crosschat", crosschat)
        map.add("colornick", colornick)
        map.add("permission", permission)
        map.add("mute", mute)
        map.add("udid", udid)
        map.add("accountid", accountid)
        map.add("accountpw", accountpw)
        return map
    }

    fun toData(data: JsonObject): PlayerData {
        name = data["name"].asString()
        uuid = data["uuid"].asString()
        country = data["country"].asString()
        countryCode = data["countryCode"].asString()
        language = data["language"].asString()
        admin = data["isAdmin"].asBoolean()
        placecount = data["placecount"].asInt()
        breakcount = data["breakcount"].asInt()
        joincount = data["joincount"].asInt()
        kickcount = data["kickcount"].asInt()
        level = data["level"].asInt()
        exp = data["exp"].asInt()
        reqexp = data["reqexp"].asInt()
        firstdate = data["firstdate"].asLong()
        lastdate = data["lastdate"].asLong()
        playtime = data["playtime"].asLong()
        attackclear = data["attackclear"].asInt()
        pvpwincount = data["pvpwincount"].asInt()
        pvplosecount = data["pvplosecount"].asInt()
        bantime = data["bantime"].asLong()
        crosschat = data["crosschat"].asBoolean()
        colornick = data["colornick"].asBoolean()
        permission = data["permission"].asString()
        mute = data["mute"].asBoolean()
        udid = data["udid"].asLong()
        accountid = data["accountid"].asString()
        accountpw = data["accountpw"].asString()
        return this
    }
}