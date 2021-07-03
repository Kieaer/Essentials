package essentials

import org.hjson.JsonObject
import java.util.*

class PlayerData {
    var name: String = "none"
    var uuid: String = "none"
    var countryCode: String = Locale.getDefault().toLanguageTag()
    var placecount: Int = 0
    var breakcount: Int = 0
    var joincount: Int = 0
    var kickcount: Int = 0
    var level: Int = 0
    var exp: Int = 0
    var firstdate: Long = 0L
    var lastdate: Long = 0L
    var playtime: Long = 0L
    var attackclear: Int = 0
    var pvpwincount: Int = 0
    var pvplosecount: Int = 0
    var bantime: Long = 0L
    var crosschat: Boolean = false
    var colornick: Boolean = false
    var permission: String = "visitor"
    var mute: Boolean = false
    var udid: Long = 0L
    var accountid: String? = name
    var accountpw: String? = "none"
    var afk: Int = 0
    var x: Int = 0
    var y: Int = 0

    // 계정 생성 데이터
    constructor(name: String, uuid: String, countryCode: String, id: String, pw: String, permission: String){
        this.name = name
        this.uuid = uuid
        this.countryCode = countryCode
        this.accountid = id
        this.accountpw = pw
        this.permission = permission
    }

    // 플레이어 전체 데이터
    constructor(name: String, uuid: String, countryCode: String, placecount: Int, breakcount: Int, joincount: Int, kickcount: Int, level: Int, exp: Int, firstdate: Long, lastdate: Long, playtime: Long, attackclear: Int, pvpwincount: Int, pvplosecount: Int, bantime: Long, crosschat: Boolean, colornick: Boolean, permission: String, mute: Boolean, udid: Long, accountid: String?, accountpw: String?) {
        this.name = name
        this.uuid = uuid
        this.countryCode = countryCode
        this.placecount = placecount
        this.breakcount = breakcount
        this.joincount = joincount
        this.kickcount = kickcount
        this.level = level
        this.exp = exp
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
    }
}