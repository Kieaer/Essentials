package essentials

import essentials.Main.Companion.tool
import org.hjson.JsonObject
import java.util.*

class PlayerData {
    var name: String = "none"
    var uuid: String = "none"
    var country: String = Locale.getDefault().displayCountry
    var country_code: String = Locale.getDefault().toLanguageTag()
    var language: String = Locale.getDefault().displayLanguage
    var isAdmin: Boolean = false
    var placecount: Int = 0
    var breakcount: Int = 0
    var killcount: Int = 0
    var deathcount: Int = 0
    var joincount: Int = 0
    var kickcount: Int = 0
    var level: Int = 0
    var exp: Int = 0
    var reqexp: Int = 0
    var reqtotalexp: String = "0/0"
    var firstdate: Long = 0L
    var lastdate: Long = 0L
    var lastplacename: String = "none"
    var lastbreakname: String = "none"
    var lastchat: String = "none"
    var playtime: Long = 0L
    var attackclear: Int = 0
    var pvpwincount: Int = 0
    var pvplosecount: Int = 0
    var pvpbreakout: Int = 0
    var reactorcount: Int = 0
    var bantime: Long = 0L
    var translate: Boolean = false
    var crosschat: Boolean = false
    var colornick: Boolean = false
    var connected: Boolean = false
    var connserver: String = "none"
    var permission: String = "default"
    var mute: Boolean = false
    var alert: Boolean = false
    var udid: Long = 0L
    var accountid: String = "none"
    var accountpw: String = "none"
    var error: Boolean = true
    var login: Boolean = false
    var afk: Long = 0L
    var x: Int = 0
    var y: Int = 0
    var locale: Locale = Locale.getDefault()

    constructor()

    constructor(name: String, uuid: String, country: String, country_code: String, language: String, isAdmin: Boolean, placecount: Int, breakcount: Int, killcount: Int, deathcount: Int, joincount: Int, kickcount: Int, level: Int, exp: Int, reqexp: Int, firstdate: Long, lastdate: Long, lastplacename: String, lastbreakname: String, lastchat: String, playtime: Long, attackclear: Int, pvpwincount: Int, pvplosecount: Int, pvpbreakout: Int, reactorcount: Int, bantime: Long, translate: Boolean, crosschat: Boolean, colornick: Boolean, connected: Boolean, connserver: String, permission: String, mute: Boolean, alert: Boolean, udid: Long, accountid: String, accountpw: String, login: Boolean) {
        this.name = name
        this.uuid = uuid
        this.country = country
        this.country_code = country_code
        this.language = language
        this.isAdmin = isAdmin
        this.placecount = placecount
        this.breakcount = breakcount
        this.killcount = killcount
        this.deathcount = deathcount
        this.joincount = joincount
        this.kickcount = kickcount
        this.level = level
        this.exp = exp
        this.reqexp = reqexp
        this.firstdate = firstdate
        this.lastdate = lastdate
        this.lastplacename = lastplacename
        this.lastbreakname = lastbreakname
        this.lastchat = lastchat
        this.playtime = playtime
        this.attackclear = attackclear
        this.pvpwincount = pvpwincount
        this.pvplosecount = pvplosecount
        this.pvpbreakout = pvpbreakout
        this.reactorcount = reactorcount
        this.bantime = bantime
        this.translate = translate
        this.crosschat = crosschat
        this.colornick = colornick
        this.connected = connected
        this.connserver = connserver
        this.permission = permission
        this.mute = mute
        this.alert = alert
        this.udid = udid
        this.accountid = accountid
        this.accountpw = accountpw
        this.login = login
        error = false
        locale = tool.textToLocale(country_code)
    }

    fun toMap(): JsonObject {
        val map = JsonObject()
        map.add("name", name)
        map.add("uuid", uuid)
        map.add("country", country)
        map.add("country_code", country_code)
        map.add("language", language)
        map.add("isAdmin", isAdmin)
        map.add("placecount", placecount)
        map.add("breakcount", breakcount)
        map.add("killcount", killcount)
        map.add("deathcount", deathcount)
        map.add("joincount", joincount)
        map.add("kickcount", kickcount)
        map.add("level", level)
        map.add("exp", exp)
        map.add("reqexp", reqexp)
        map.add("firstdate", firstdate)
        map.add("lastdate", lastdate)
        map.add("lastplacename", lastplacename)
        map.add("lastbreakname", lastbreakname)
        map.add("lastchat", lastchat)
        map.add("playtime", playtime)
        map.add("attackclear", attackclear)
        map.add("pvpwincount", pvpwincount)
        map.add("pvplosecount", pvplosecount)
        map.add("pvpbreakout", pvpbreakout)
        map.add("reactorcount", reactorcount)
        map.add("bantime", bantime)
        map.add("translate", translate)
        map.add("crosschat", crosschat)
        map.add("colornick", colornick)
        map.add("connected", connected)
        map.add("connserver", connserver)
        map.add("permission", permission)
        map.add("mute", mute)
        map.add("alert", alert)
        map.add("udid", udid)
        map.add("accountid", accountid)
        map.add("accountpw", accountpw)
        return map
    }

    fun toData(data: JsonObject): PlayerData {
        name = data["name"].asString()
        uuid = data["uuid"].asString()
        country = data["country"].asString()
        country_code = data["country_code"].asString()
        language = data["language"].asString()
        isAdmin = data["isAdmin"].asBoolean()
        placecount = data["placecount"].asInt()
        breakcount = data["breakcount"].asInt()
        killcount = data["killcount"].asInt()
        deathcount = data["deathcount"].asInt()
        joincount = data["joincount"].asInt()
        kickcount = data["kickcount"].asInt()
        level = data["level"].asInt()
        exp = data["exp"].asInt()
        reqexp = data["reqexp"].asInt()
        firstdate = data["firstdate"].asLong()
        lastdate = data["lastdate"].asLong()
        lastplacename = data["lastplacename"].asString()
        lastbreakname = data["lastbreakname"].asString()
        lastchat = data["lastchat"].asString()
        playtime = data["playtime"].asLong()
        attackclear = data["attackclear"].asInt()
        pvpwincount = data["pvpwincount"].asInt()
        pvplosecount = data["pvplosecount"].asInt()
        pvpbreakout = data["pvpbreakout"].asInt()
        reactorcount = data["reactorcount"].asInt()
        bantime = data["bantime"].asLong()
        translate = data["translate"].asBoolean()
        crosschat = data["crosschat"].asBoolean()
        colornick = data["colornick"].asBoolean()
        connected = data["connected"].asBoolean()
        connserver = data["connserver"].asString()
        permission = data["permission"].asString()
        mute = data["mute"].asBoolean()
        alert = data["alert"].asBoolean()
        udid = data["udid"].asLong()
        accountid = data["accountid"].asString()
        accountpw = data["accountpw"].asString()
        return this
    }
}