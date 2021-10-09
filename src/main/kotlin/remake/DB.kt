package remake

import arc.Core
import arc.files.Fi
import arc.struct.ObjectMap
import arc.struct.Seq
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*


object DB {
    val players: Seq<PlayerData> = Seq<PlayerData>()
    val root: Fi = Core.settings.dataDirectory.child("mods/Essentials/")
    val db: Database = Database.connect(root.child("data/database.db").absolutePath(), driver = "org.sqlite.JDBC")

    object Player : Table(){
        val name = text("name").index()
        val uuid = text("uuid")
        val countryCode = text("countryCode")
        val placecount = integer("placecount")
        val breakcount = integer("breakcount")
        val joincount = integer("joincount")
        val kickcount = integer("kickcount")
        val level = integer("level")
        val exp = integer("exp")
        val joinDate = integer("joinDate")
        val lastdate = integer("lastdate")
        val playtime = integer("playtime")
        val attackclear = integer("attackclear")
        val pvpwincount = integer("pvpwincount")
        val pvplosecount = integer("pvplosecount")
        val colornick = bool("colornick")
        val mute = bool("mute")
        val accountid = text("id")
        val accountpw = text("pw")
    }

    object PlayerData {
        var name: String = "none"
        var uuid: String = "none"
        var countryCode: String = Locale.getDefault().isO3Country
        var placecount: Int = 0
        var breakcount: Int = 0
        var joincount: Int = 0
        var kickcount: Int = 0
        var level: Int = 0
        var exp: Int = 0
        var joinDate: Int = 0
        var lastdate: Int = 0
        var playtime: Int = 0
        var attackclear: Int = 0
        var pvpwincount: Int = 0
        var pvplosecount: Int = 0
        var colornick: Boolean = false
        var permission: String = "visitor"
        var mute: Boolean = false
        var id: String = name
        var pw: String = "none"
        var status: ObjectMap<String, Any> = ObjectMap()
    }

    fun createData(data: PlayerData){
        transaction {
            SchemaUtils.create(Player)
            Player.insert {
                it[name] = data.name
                it[uuid] = data.uuid
                it[countryCode] = data.countryCode
                it[placecount] = data.placecount
                it[breakcount] = data.breakcount
                it[joincount] = data.joincount
                it[kickcount] = data.kickcount
                it[level] = data.level
                it[exp] = data.exp
                it[joinDate] = data.joinDate
                it[lastdate] = data.lastdate
                it[playtime] = data.playtime
                it[attackclear] = data.attackclear
                it[pvpwincount] = data.pvpwincount
                it[pvplosecount] = data.pvplosecount
                it[colornick] = data.colornick
                it[mute] = data.mute
                it[accountid] = data.id
                it[accountpw] = data.pw
            }
        }
    }

    operator fun get(uuid: String) : PlayerData?{
        Player.select {Player.uuid.eq(uuid)}.forEach{
            val data = PlayerData
            data.name = it[Player.name]
            data.uuid = it[Player.uuid]
            data.countryCode = it[Player.countryCode]
            data.placecount = it[Player.placecount]
            data.breakcount = it[Player.breakcount]
            data.joincount = it[Player.joincount]
            data.kickcount = it[Player.kickcount]
            data.level = it[Player.level]
            data.exp = it[Player.exp]
            data.joinDate = it[Player.joinDate]
            data.lastdate = it[Player.lastdate]
            data.playtime = it[Player.playtime]
            data.attackclear = it[Player.attackclear]
            data.pvpwincount = it[Player.pvpwincount]
            data.pvplosecount = it[Player.pvplosecount]
            data.colornick = it[Player.colornick]
            data.mute = it[Player.mute]
            data.id = it[Player.accountid]
            data.pw = it[Player.accountpw]

            return data
        }
        // 데이터를 찾지 못했을 경우 null
        return null
    }

    fun update(id: String, data: PlayerData){
        Player.update({Player.uuid eq id}) {
            it[name] = data.name
            it[uuid] = data.uuid
            it[countryCode] = data.countryCode
            it[placecount] = data.placecount
            it[breakcount] = data.breakcount
            it[joincount] = data.joincount
            it[kickcount] = data.kickcount
            it[level] = data.level
            it[exp] = data.exp
            it[joinDate] = data.joinDate
            it[lastdate] = data.lastdate
            it[playtime] = data.playtime
            it[attackclear] = data.attackclear
            it[pvpwincount] = data.pvpwincount
            it[pvplosecount] = data.pvplosecount
            it[colornick] = data.colornick
            it[mute] = data.mute
            it[accountid] = data.id
            it[accountpw] = data.pw
        }
    }

    fun delete(id: String){
        Player.deleteWhere { Player.uuid.eq(id) }
    }

    fun close(){
        TransactionManager.closeAndUnregister(db)
    }
}