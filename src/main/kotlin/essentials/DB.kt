package essentials

import arc.Core
import arc.files.Fi
import arc.struct.ObjectMap
import arc.struct.Seq
import mindustry.gen.Playerc
import org.h2.tools.Server
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.util.*


class DB {
    val players: Seq<PlayerData> = Seq()
    var isRemote: Boolean = false
    lateinit var db: Database
    lateinit var dbServer: Server

    fun open() {
        isRemote = !Config.database.equals(Core.settings.dataDirectory.child("mods/Essentials/database.db").absolutePath(), false)
        try {
            try {
                if (Fi(Config.database).exists()) {
                    // SQLite DB 열기
                    db = Database.connect("jdbc:sqlite:${Config.database}", "org.sqlite.JDBC")
                    val playerData = getAll()
                    var oldcount = playerData.size
                    PluginData.load()
                    TransactionManager.closeAndUnregister(db)

                    Fi(Config.database).copyTo(Core.settings.dataDirectory.child("mods/Essentials/database_backup.db"))
                    Fi(Config.database).delete()

                    // H2 DB 열기
                    db = Database.connect("jdbc:h2:file:${Config.database}", "org.h2.Driver", "sa", "")
                    transaction {
                        SchemaUtils.create(Player)
                        SchemaUtils.create(Data)
                    }

                    var count = 0
                    try {

                        for (a in playerData) {
                            createData(a)
                            count++
                            print("DB is being upgraded! please wait for a moment... $count/$oldcount\r")
                        }
                        print("\n")
                        PluginData.save()

                        TransactionManager.closeAndUnregister(db)
                        count = 0

                        // H2 DB 검사
                        db = Database.connect("jdbc:h2:file:${Config.database}", "org.h2.Driver", "sa", "")
                        for (a in playerData) {
                            get(a.uuid)
                            count++
                            print("Validating... $count/$oldcount\r")
                        }
                        print("\n")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    TransactionManager.closeAndUnregister(db)
                    db = Database.connect("jdbc:h2:${if (isRemote) "tcp://" else "file:"}${Config.database}${if (isRemote) ":9092/db" else ""}", "org.h2.Driver", "sa", "")
                } else {
                    if (!isRemote) {
                        dbServer = Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9092", "-ifNotExists", "-key", "db", Core.settings.dataDirectory.child("mods/Essentials/database.db").absolutePath())
                        dbServer.start()
                        db = Database.connect("jdbc:h2:tcp://127.0.0.1:9092/db", "org.h2.Driver", "sa", "")
                    } else {
                        db = Database.connect("jdbc:h2:tcp://${Config.database}:9092/db", "org.h2.Driver", "sa", "")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }



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
        var afkTime: Int = 0
        var player: Playerc = mindustry.gen.Player.create()
        var entityid: Int = 0

        override fun toString(): String {
            return "name: $name, uuid: $uuid, languageTag: $languageTag, placecount: $placecount, breakcount: $breakcount, joincount: $joincount, kickcount: $kickcount, level: $level, exp: $exp, joinDate: $joinDate, lastdate: $lastdate, playtime: $playtime, attackclear: $attackclear, pvpwincount: $pvpwincount, pvplosecount: $pvplosecount, colornick: $colornick, permission: $permission, mute: $mute, id: $id, pw: $pw, status: $status, afkTime: $afkTime, entityid: $entityid"
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