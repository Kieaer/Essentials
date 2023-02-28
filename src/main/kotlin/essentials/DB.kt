package essentials

import arc.Core
import arc.struct.ObjectMap
import arc.struct.Seq
import arc.util.Log
import mindustry.gen.Playerc
import org.h2.tools.Server
import org.hjson.JsonObject
import org.hjson.ParseException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.io.BufferedInputStream
import java.io.FileOutputStream
import java.net.URL
import java.util.*

class DB {
    val players : Seq<PlayerData> = Seq()
    var isRemote : Boolean = false
    lateinit var db : Database
    var dbServer : Server? = null

    fun open() {
        try {
            if(Main.root.child("database.db.mv.db").exists()) {
                if(!Main.root.child("data/h2-1.4.200.jar").exists()) {
                    Main.root.child("data").mkdirs()
                    URL("https://repo1.maven.org/maven2/com/h2database/h2/1.4.200/h2-1.4.200.jar").openStream().use { b ->
                        BufferedInputStream(b).use { bis ->
                            FileOutputStream(Main.root.child("data/h2-1.4.200.jar").absolutePath()).use { fos ->
                                val data = ByteArray(1024)
                                var count : Int
                                while(bis.read(data, 0, 1024).also { count = it } != -1) {
                                    fos.write(data, 0, count)
                                }
                            }
                        }
                    }
                }

                val os = System.getProperty("os.name").lowercase(Locale.getDefault())
                if(os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                    val cmd = arrayOf("/bin/bash", "-c", "cd ${Main.root.child("data").absolutePath()} && java -cp h2-1.4.200.jar org.h2.tools.Script -url jdbc:h2:../database.db;AUTO_SERVER=TRUE -user sa -script script.sql")
                    Runtime.getRuntime().exec(cmd).waitFor()
                } else {
                    Runtime.getRuntime().exec("cmd /c cd /D ${Main.root.child("data").absolutePath()} && java -cp h2-1.4.200.jar org.h2.tools.Script -url jdbc:h2:../database.db;AUTO_SERVER=TRUE -user sa -script script.sql").waitFor()
                }

                Main.root.child("database.db.mv.db").moveTo(Main.root.child("old-database.db"))
                Config.database = Main.root.child("database").absolutePath()
                Config.save()
            }

            isRemote = !Config.database.equals(Main.root.child("database").absolutePath(), false)
            if(!isRemote) {
                try {
                    dbServer = Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9092", "-ifNotExists", "-key", "db", Config.database).start()
                    db = Database.connect("jdbc:h2:tcp://127.0.0.1:9092/db;AUTO_SERVER=TRUE", "org.h2.Driver", "sa", "")
                } catch(e : Exception) {
                    db = Database.connect("jdbc:h2:tcp://127.0.0.1:9092/db;AUTO_SERVER=TRUE", "org.h2.Driver", "sa", "")
                    Log.info(Bundle()["event.database.remote"])
                }
            } else {
                db = Database.connect("jdbc:h2:tcp://${Config.database}:9092/db;AUTO_SERVER=TRUE", "org.h2.Driver", "sa", "")
            }

            if(Main.root.child("data/script.sql").exists()) {
                transaction {
                    exec(Main.root.child("data/script.sql").readString())
                }
                Main.root.child("data/script.sql").moveTo(Main.root.child("data/script_backup.sql"))
            }

            transaction {
                if(!connection.isClosed) {
                    // todo 데이터가 손상된 경우를 위해 백업하는 테이블이나 기능 만들기
                    SchemaUtils.create(Player)
                    SchemaUtils.create(Data)

                    if(!isRemote) {
                        try {
                            getAll()
                        } catch(e : ParseException) {
                            transaction {
                                Player.update {
                                    it[status] = "{}"
                                }
                            }
                        }

                        val sql = """
                        SELECT * FROM player cc INNER JOIN (SELECT
                        "NAME", COUNT(*) AS CountOf
                        FROM player
                        GROUP BY "NAME"
                        HAVING COUNT(*)>1
                        ) dt ON cc.name=dt.name
                        """.trimIndent()

                        exec(sql) { rs ->
                            while(rs.next()) {
                                val data = get(rs.getString("uuid"))
                                if(data != null) {
                                    try {
                                        val json = JsonObject.readHjson(rs.getString("status")).asObject()
                                        if(json.get("duplicateName") != null) {
                                            json.add("duplicateName", data.name)
                                        }
                                    } catch(e : ParseException) {
                                        if(!data.status.containsKey("duplicateName")) {
                                            data.status.put("duplicateName", data.name)
                                        }
                                    }
                                    update(data.uuid, data)
                                }
                            }
                            print("\n")
                        }
                    }
                } else {
                    Log.err(Bundle()["event.plugin.db.wrong"])
                    dbServer?.stop()
                    Core.app.exit()
                }
            }
        } catch(e : Exception) {
            e.printStackTrace()
            Core.app.exit()
        }
    }

    object Data: Table() {
        val data = text("data")
    }

    object Player: Table() {
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
        var name : String = "none"
        var uuid : String = "none"
        var languageTag : String = Locale.getDefault().toLanguageTag()
        var placecount : Int = 0
        var breakcount : Int = 0
        var joincount : Int = 0
        var kickcount : Int = 0
        var level : Int = 0
        var exp : Int = 0
        var joinDate : Long = 0
        var lastdate : Long = 0
        var playtime : Long = 0
        var attackclear : Int = 0
        var pvpwincount : Int = 0
        var pvplosecount : Int = 0
        var colornick : Boolean = false
        var permission : String = "visitor"
        var mute : Boolean = false
        var id : String = name
        var pw : String = "none"
        var status : ObjectMap<String, String> = ObjectMap()
        var afkTime : Int = 0
        var player : Playerc = mindustry.gen.Player.create()
        var entityid : Int = 0

        override fun toString() : String {
            return "name: $name, uuid: $uuid, languageTag: $languageTag, placecount: $placecount, breakcount: $breakcount, joincount: $joincount, kickcount: $kickcount, level: $level, exp: $exp, joinDate: $joinDate, lastdate: $lastdate, playtime: $playtime, attackclear: $attackclear, pvpwincount: $pvpwincount, pvplosecount: $pvplosecount, colornick: $colornick, permission: $permission, mute: $mute, id: $id, pw: $pw, status: $status, afkTime: $afkTime, entityid: $entityid"
        }
    }

    fun createData(data : PlayerData) {
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

    operator fun get(uuid : String) : PlayerData? {
        // todo 메인 서버가 사망할 경우 하위 서버도 사망하는 문제
        val d = transaction { Player.select { Player.uuid.eq(uuid) }.firstOrNull() }
        if(d != null) {
            val data = PlayerData()
            data.name = d[Player.name]
            data.uuid = d[Player.uuid]
            data.languageTag = d[Player.languageTag]
            data.placecount = d[Player.placecount]
            data.breakcount = d[Player.breakcount]
            data.joincount = d[Player.joincount]
            data.kickcount = d[Player.kickcount]
            data.level = d[Player.level]
            data.exp = d[Player.exp]
            data.joinDate = d[Player.joinDate]
            data.lastdate = d[Player.lastdate]
            data.playtime = d[Player.playtime]
            data.attackclear = d[Player.attackclear]
            data.pvpwincount = d[Player.pvpwincount]
            data.pvplosecount = d[Player.pvplosecount]
            data.colornick = d[Player.colornick]
            data.permission = d[Player.permission]
            data.mute = d[Player.mute]
            data.id = d[Player.accountid]
            data.pw = d[Player.accountpw]

            val obj = ObjectMap<String, String>()
            for(a in JsonObject.readHjson(d[Player.status]).asObject()) {
                obj.put(a.name, a.value.asString())
            }
            data.status = obj
            return data
        } else {
            return null
        }
    }

    fun getAll() : Seq<PlayerData> {
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

                val obj = ObjectMap<String, String>()
                for(a in JsonObject.readHjson(it[Player.status]).asObject()) {
                    obj.put(a.name, a.value.asString())
                }
                data.status = obj
                d.add(data)
            }
        }
        return d
    }

    fun getAllByExp() : Seq<PlayerData> {
        val d = Seq<PlayerData>()

        transaction {
            Player.selectAll().orderBy(Player.exp).map {
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

                val obj = ObjectMap<String, String>()
                for(a in JsonObject.readHjson(it[Player.status]).asObject()) {
                    obj.put(a.name, a.value.asString())
                }
                data.status = obj
                d.add(data)
            }
        }

        return d
    }

    fun queue(data : PlayerData) {
        Trigger.UpdateThread.queue.add(data)
    }

    fun update(id : String, data : PlayerData) {
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

                val json = JsonObject()
                for(a in data.status) json.add(a.key, a.value)
                it[status] = json.toString()
            }
        }
    }

    fun search(id : String, pw : String) : PlayerData? {
        transaction { Player.select { Player.accountid eq id }.firstOrNull() }.run {
            if(this != null) {
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

                val obj = ObjectMap<String, String>()
                for(a in JsonObject.readHjson(this[Player.status]).asObject()) {
                    obj.put(a.name, a.value.asString())
                }
                data.status = obj

                return if(data.id == data.pw) data else if(BCrypt.checkpw(pw, data.pw)) data else null
            } else {
                return null
            }
        }
    }

    fun close() {
        TransactionManager.closeAndUnregister(db)
    }
}