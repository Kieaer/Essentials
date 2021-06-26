package essentials

import org.hjson.JsonObject
import java.util.*

class PlayerData(val uuid: String, val json: String, ID: String, PW: String) {
    var name: String
    var countryCode: String
    var placecount: Int
    var breakcount: Int
    var joincount: Int
    var kickcount: Int
    var level: Int
    var exp: Int
    var firstdate: Long
    var lastdate: Long
    var playtime: Long
    var attackclear: Int
    var pvpwincount: Int
    var pvplosecount: Int
    var bantime: Long
    var crosschat: Boolean
    var colornick: Boolean
    var permission: String
    var mute: Boolean
    var udid: Long
    var accountid: String? = ID
    var accountpw: String? = PW
    var afk: Int = 0
    var x: Int = 0
    var y: Int = 0

    init {
        val data = JsonObject.readJSON(json).asObject()
        this.name = data.getString("name", "none")
        this.countryCode = data.getString("countryCode", Locale.getDefault().toLanguageTag())
        this.placecount = data.getInt("place", 0)
        this.breakcount = data.getInt("break", 0)
        this.joincount = data.getInt("join", 0)
        this.kickcount = data.getInt("kick", 0)
        this.level = data.getInt("level", 0)
        this.exp = data.getInt("exp", 0)
        this.firstdate = data.getLong("firstdate", 0)
        this.lastdate = data.getLong("lastdate", 0)
        this.playtime = data.getLong("playtime", 0)
        this.attackclear = data.getInt("attack", 0)
        this.pvpwincount = data.getInt("pvpwin", 0)
        this.pvplosecount = data.getInt("pvplose", 0)
        this.bantime = data.getLong("bantime", 0)
        this.crosschat = data.getBoolean("crosschat", false)
        this.colornick = data.getBoolean("colornick", false)
        this.permission = data.getString("permission", "visitor")
        this.mute = data.getBoolean("mute", false)
        this.udid = data.getLong("udid", 0L)
    }
}