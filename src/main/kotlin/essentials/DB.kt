package essentials

import arc.Core
import arc.struct.ObjectMap
import arc.struct.Seq
import mindustry.gen.Playerc
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.util.*


class DB {
    val players: Seq<PlayerData> = Seq()
    lateinit var db: Database

    fun open() {
        try {
            db = Database.connect("jdbc:sqlite:${Core.settings.dataDirectory.child("mods/Essentials/database.db").absolutePath()}")
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
        val languageTag = text("languageTag")
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
        val permission = text("permission")
        val mute = bool("mute")
        val accountid = text("id")
        val accountpw = text("pw")
        val status = text("status")
    }

    class PlayerData {
        var name: String = "none"
        var uuid: String = "none"
        var languageTag: String = Locale.getDefault().toLanguageTag()
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
        var status: ObjectMap<String, String> = ObjectMap()
        var x: Int = 0
        var y: Int = 0
        var afkTime: Int = 0
        var player: Playerc? = null

        override fun toString(): String {
            return "name: $name, uuid: $uuid, languageTag: $languageTag, placecount: $placecount, breakcount: $breakcount, joincount: $joincount, kickcount: $kickcount, level: $level, exp: $exp, joinDate: $joinDate, lastdate: $lastdate, playtime: $playtime, attackclear: $attackclear, pvpwincount: $pvpwincount, pvplosecount: $pvplosecount, colornick: $colornick, permission: $permission, mute: $mute, id: $id, pw: $pw, status: $status, x: $x, y: $y, afkTime: $afkTime"
        }
    }

    fun createData(data: PlayerData) {
        transaction {
            Player.insert {
                it[name] = data.name
                it[uuid] = data.uuid
                it[languageTag] = data.languageTag
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
                it[permission] = data.permission
                it[mute] = data.mute
                it[accountid] = data.id
                it[accountpw] = data.pw
                it[status] = data.status.toString()
            }
        }
    }

    operator fun get(uuid: String): PlayerData? {
        transaction { Player.select { Player.uuid.eq(uuid) }.firstOrNull() }.run {
            if (this != null) {
                val data = PlayerData()
                data.name = this[Player.name]
                data.uuid = this[Player.uuid]
                data.languageTag = this[Player.languageTag]
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
                data.permission = this[Player.permission]
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

    fun getAll(): Seq<PlayerData> {
        val d = Seq<PlayerData>()
        transaction {
            Player.selectAll().map {
                val data = PlayerData()
                data.name = it[Player.name]
                data.uuid = it[Player.uuid]
                data.languageTag = it[Player.languageTag]
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
                data.permission = it[Player.permission]
                data.mute = it[Player.mute]
                data.id = it[Player.accountid]
                data.pw = it[Player.accountpw]
                data.status = ObjectMap.of(it[Player.status])
                d.add(data)
            }
        }
        return d
    }

    fun update(id: String, data: PlayerData) {
        transaction {
            Player.update({ Player.uuid eq id }) {
                it[name] = data.name
                it[uuid] = data.uuid
                it[languageTag] = data.languageTag
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
                it[permission] = data.permission
                it[mute] = data.mute
                it[accountid] = data.id
                it[accountpw] = data.pw
                it[status] = data.status.toString()
            }
        }
    }

    fun search(id: String, pw: String): PlayerData? {
        transaction { Player.select { Player.accountid eq id }.firstOrNull() }.run {
            if (this != null) {
                val data = PlayerData()
                data.name = this[Player.name]
                data.uuid = this[Player.uuid]
                data.languageTag = this[Player.languageTag]
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
                data.permission = this[Player.permission]
                data.mute = this[Player.mute]
                data.id = this[Player.accountid]
                data.pw = this[Player.accountpw]
                data.status = ObjectMap.of(this[Player.status])

                return if (data.id == data.pw) data else if (BCrypt.checkpw(pw, data.pw)) data else null
            } else {
                return null
            }
        }
    }

    fun close() {
        TransactionManager.closeAndUnregister(db)
    }
}