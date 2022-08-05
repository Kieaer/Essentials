package remake

import arc.Core
import arc.struct.ObjectMap
import arc.struct.Seq
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*


class DB {
    val players: Seq<PlayerData> = Seq()
    lateinit var db: Database

    fun open() {
        try {
            db = Database.connect(
                "jdbc:sqlite:${
                    Core.settings.dataDirectory.child("mods/Essentials/database.db").absolutePath()
                }"
            )
            transaction {
                SchemaUtils.create(Player)
                SchemaUtils.create(Data)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Core.app.exit()
        }
    }

    object Data : Table() {
        val data = text("data")
    }

    object Player : Table() {
        val name = text("name").index()
        val uuid = text("uuid")
        val countryCode = text("countryCode")
        val placecount = integer("placecount")
        val breakcount = integer("breakcount")
        val joincount = integer("joincount")
        val kickcount = integer("kickcount")
        val level = integer("level")
        val exp = integer("exp")
        val joinDate = long("joinDate")
        val lastdate = long("lastdate")
        val playtime = long("playtime")
        val attackclear = integer("attackclear")
        val pvpwincount = integer("pvpwincount")
        val pvplosecount = integer("pvplosecount")
        val colornick = bool("colornick")
        val mute = bool("mute")
        val accountid = text("id")
        val accountpw = text("pw")
        val status = text("status")
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
        var joinDate: Long = 0
        var lastdate: Long = 0
        var playtime: Long = 0
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

    fun createData(data: PlayerData) {
        transaction {
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
                it[status] = data.status.toString()
            }
        }
    }

    operator fun get(uuid: String): PlayerData? {
        transaction { Player.select { Player.uuid.eq(uuid) }.firstOrNull() }.apply {
            if (this != null) {
                val data = PlayerData
                data.name = this[Player.name]
                data.uuid = this[Player.uuid]
                data.countryCode = this[Player.countryCode]
                data.placecount = this[Player.placecount]
                data.breakcount = this[Player.breakcount]
                data.joincount = this[Player.joincount]
                data.kickcount = this[Player.kickcount]
                data.level = this[Player.level]
                data.exp = this[Player.exp]
                data.joinDate = this[Player.joinDate]
                data.lastdate = this[Player.lastdate]
                data.playtime = this[Player.playtime]
                data.attackclear = this[Player.attackclear]
                data.pvpwincount = this[Player.pvpwincount]
                data.pvplosecount = this[Player.pvplosecount]
                data.colornick = this[Player.colornick]
                data.mute = this[Player.mute]
                data.id = this[Player.accountid]
                data.pw = this[Player.accountpw]
                data.status = ObjectMap.of(this[Player.status])
                return data
            } else {
                return null
            }
        }
    }

    fun update(id: String, data: PlayerData) {
        transaction {
            Player.update({ Player.uuid eq id }) {
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
                it[status] = data.status.toString()
            }
        }
    }

    fun delete(id: String) {
        transaction {
            Player.deleteWhere { Player.uuid.eq(id) }
        }
    }

    fun search(id: String, pw: String): PlayerData? {
        transaction { Player.select { (Player.accountid eq id) and (Player.accountpw eq pw) }.firstOrNull() }.apply {
            if (this != null) {
                val data = PlayerData
                data.name = this[Player.name]
                data.uuid = this[Player.uuid]
                data.countryCode = this[Player.countryCode]
                data.placecount = this[Player.placecount]
                data.breakcount = this[Player.breakcount]
                data.joincount = this[Player.joincount]
                data.kickcount = this[Player.kickcount]
                data.level = this[Player.level]
                data.exp = this[Player.exp]
                data.joinDate = this[Player.joinDate]
                data.lastdate = this[Player.lastdate]
                data.playtime = this[Player.playtime]
                data.attackclear = this[Player.attackclear]
                data.pvpwincount = this[Player.pvpwincount]
                data.pvplosecount = this[Player.pvplosecount]
                data.colornick = this[Player.colornick]
                data.mute = this[Player.mute]
                data.id = this[Player.accountid]
                data.pw = this[Player.accountpw]
                data.status = ObjectMap.of(this[Player.status])
                return data
            } else {
                return null
            }
        }
    }

    fun close() {
        TransactionManager.closeAndUnregister(db)
    }
}