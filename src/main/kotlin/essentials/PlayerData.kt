package essentials

import org.hjson.JsonObject
import java.util.*

class PlayerData(var uuid: String, var json: String, ID: String, PW: String) {
    /** Player name */
    var name: String

    /** Player Country Code */
    var countryCode: String

    /** Block place count */
    var placecount: Int

    /** Block break count */
    var breakcount: Int

    /** Server join count */
    var joincount: Int

    /** Player kicked count */
    var kickcount: Int

    /** Player level */
    var level: Int

    /** Player experience value */
    var exp: Int

    /** Server first join date */
    var firstdate: Long

    /** Last played date */
    var lastdate: Long

    /** Play time on the server */
    var playtime: Long

    /** Attack server clear count */
    var attackclear: Int

    /** PvP Win count */
    var pvpwincount: Int

    /** Pvp Lose count */
    var pvplosecount: Int

    /** Unbanned time */
    var bantime: Long

    /** Server to server chat */
    var crosschat: Boolean

    /** Animated color nickname */
    var colornick: Boolean

    /** Player permission level */
    var permission: String

    /** Player is muted */
    var mute: Boolean

    /** Discord UUID */
    var udid: Long

    /** Account ID */
    var accountid: String? = ID

    /** Account Password */
    var accountpw: String? = PW

    /** AFK Time */
    var afk: Int = 0

    /** Player X position */
    var x: Int = 0

    /** Player Y position */
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
        this.colornick = data.getBoolean("colornick",false)
        this.permission = data.getString("permission", "default")
        this.mute = data.getBoolean("mute", false)
        this.udid = data.getLong("udid", 0L)
    }
}