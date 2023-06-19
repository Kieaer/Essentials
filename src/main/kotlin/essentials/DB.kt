package essentials

import arc.Core
import arc.struct.ObjectMap
import arc.struct.Seq
import arc.util.Log
import mindustry.gen.Playerc
import org.h2.tools.RunScript
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
import java.sql.SQLException
import java.time.LocalDate
import java.util.*

class DB {
    val players : Seq<PlayerData> = Seq()
    private var isRemote : Boolean = false
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
                    val cmd = arrayOf("/bin/bash", "-c", "cd ${Main.root.child("data").absolutePath()} && java -cp h2-1.4.200.jar org.h2.tools.Script -url jdbc:h2:../database.db -user sa -script script.sql")
                    Runtime.getRuntime().exec(cmd).waitFor()
                } else {
                    Runtime.getRuntime().exec("cmd /c cd /D ${Main.root.child("data").absolutePath()} && java -cp h2-1.4.200.jar org.h2.tools.Script -url jdbc:h2:../database.db -user sa -script script.sql").waitFor()
                }

                Main.root.child("database.db.mv.db").moveTo(Main.root.child("old-database.mv.db"))
                Config.database = Main.root.child("database").absolutePath()
                Config.save()
            }

            isRemote = !Config.database.equals(Main.root.child("database").absolutePath(), false)
            if(!isRemote) {
                try {
                    dbServer = Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9092", "-ifNotExists", "-key", "db", Config.database).start()
                    db = Database.connect("jdbc:h2:tcp://127.0.0.1:9092/db", "org.h2.Driver", "sa", "")
                } catch(e : Exception) {
                    db = Database.connect("jdbc:h2:tcp://127.0.0.1:9092/db", "org.h2.Driver", "sa", "")
                    Log.info(Bundle()["event.database.remote"])
                }
            } else {
                db = Database.connect("jdbc:h2:tcp://${Config.database}:9092/db", "org.h2.Driver", "sa", "")
            }

            if(Main.root.child("data/script.sql").exists()) {
                RunScript.main("-url", "jdbc:h2:${Config.database}", "-user", "sa", "-script", Main.root.child("data/script.sql").absolutePath(), "-options", "FROM_1X")
                Main.root.child("data/script.sql").moveTo(Main.root.child("data/script_backup.sql"))
            }

            transaction {
                if(!connection.isClosed) {
                    SchemaUtils.create(Player)
                    SchemaUtils.create(Data)
                    SchemaUtils.create(DB)

                    if(!isRemote) {
                        if(DB.selectAll().empty()) {
                            try {
                                exec("SELECT hud FROM Players")
                            } catch(e : SQLException) {
                                val s = listOf(
                                    "ALTER TABLE Player ALTER COLUMN IF EXISTS placecount RENAME TO \"blockPlaceCount\"",
                                    "ALTER TABLE Player ALTER COLUMN IF EXISTS breakcount RENAME TO \"blockBreakCount\"",
                                    "ALTER TABLE Player ALTER COLUMN IF EXISTS joincount RENAME TO \"totalJoinCount\"",
                                    "ALTER TABLE Player ALTER COLUMN IF EXISTS kickcount RENAME TO \"totalKickCount\"",
                                    "ALTER TABLE Player ALTER COLUMN IF EXISTS lastdate RENAME TO \"lastLoginTime\"",
                                    "ALTER TABLE Player ALTER COLUMN IF EXISTS \"joinDate\" RENAME TO \"firstPlayDate\"",
                                    "ALTER TABLE Player ALTER COLUMN IF EXISTS playtime RENAME TO \"totalPlayTime\"",
                                    "ALTER TABLE Player ALTER COLUMN IF EXISTS attackclear RENAME TO \"attackModeClear\"",
                                    "ALTER TABLE Player ALTER COLUMN IF EXISTS pvpwincount RENAME TO \"pvpVictoriesCount\"",
                                    "ALTER TABLE Player ALTER COLUMN IF EXISTS pvplosecount RENAME TO \"pvpDefeatCount\"",
                                    "ALTER TABLE Player ALTER COLUMN IF EXISTS colornick RENAME TO \"animatedName\"",
                                    "ALTER TABLE Player ALTER COLUMN IF EXISTS id RENAME TO \"accountID\"",
                                    "ALTER TABLE Player ALTER COLUMN IF EXISTS pw RENAME TO \"accountPW\"",
                                    "ALTER TABLE Player ADD COLUMN IF NOT EXISTS discord CHARACTER VARYING",
                                    "ALTER TABLE Player ADD COLUMN IF NOT EXISTS \"effectLevel\" INTEGER",
                                    "ALTER TABLE Player ADD COLUMN IF NOT EXISTS \"effectColor\" CHARACTER VARYING",
                                    "ALTER TABLE Player ADD COLUMN IF NOT EXISTS \"hideRanking\" BOOLEAN",
                                    "ALTER TABLE Player ADD COLUMN IF NOT EXISTS freeze BOOLEAN",
                                    "ALTER TABLE Player ADD COLUMN IF NOT EXISTS hud CHARACTER VARYING",
                                    "ALTER TABLE Player ADD COLUMN IF NOT EXISTS tpp CHARACTER VARYING",
                                    "ALTER TABLE Player ADD COLUMN IF NOT EXISTS \"tppTeam\" INTEGER",
                                    "ALTER TABLE Player ADD COLUMN IF NOT EXISTS log BOOLEAN",
                                    "ALTER TABLE Player ADD COLUMN IF NOT EXISTS \"oldUUID\" CHARACTER VARYING",
                                    "ALTER TABLE Player ADD COLUMN IF NOT EXISTS \"banTime\" CHARACTER VARYING",
                                    "ALTER TABLE Player ADD COLUMN IF NOT EXISTS \"duplicateName\" CHARACTER VARYING",
                                    "ALTER TABLE Player ADD COLUMN IF NOT EXISTS tracking BOOLEAN",
                                    "ALTER TABLE Player ADD COLUMN IF NOT EXISTS \"joinStacks\" CHARACTER VARYING",
                                    "ALTER TABLE Player ADD COLUMN IF NOT EXISTS \"lastLoginDate\" CHARACTER VARYING",
                                    "UPDATE player SET \"hideRanking\" = false, freeze = false, log = false, tracking = false, \"joinStacks\" = 0",
                                )
                                Log.info(Bundle()["event.plugin.db.version", 2])
                                Log.warn(Bundle()["event.plugin.db.warning"])
                                execInBatch(s)
                            }
                            exec("INSERT INTO DB VALUES 2")
                        } else {
                            when(DB.selectAll().first()[DB.version]) {
                                2 -> {
                                    // TODO DB 업데이트 명령줄
                                }
                            }
                        }

                        try {
                            getAll()
                        } catch(e : ParseException) {
                            Player.update {
                                it[status] = "{}"
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
                                        if(rs.getString("duplicateName") == "null") {
                                            data.duplicateName = data.name
                                        }
                                    } catch(e : ParseException) {
                                        if(data.duplicateName == null) {
                                            data.duplicateName = data.name
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

    object DB: Table() {
        val version = integer("version")
    }

    object Player: Table() {
        val name = text("name").index()
        val uuid = text("uuid")
        val languageTag = text("languageTag")
        val blockPlaceCount = integer("blockPlaceCount")
        val blockBreakCount = integer("blockBreakCount")
        val totalJoinCount = integer("totalJoinCount")
        val totalKickCount = integer("totalKickCount")
        val level = integer("level")
        val exp = integer("exp")
        val firstPlayDate = long("firstPlayDate")
        val lastLoginTime = long("lastLoginTime")
        val totalPlayTime = long("totalPlayTime")
        val attackModeClear = integer("attackModeClear")
        val pvpVictoriesCount = integer("pvpVictoriesCount")
        val pvpDefeatCount = integer("pvpDefeatCount")
        val animatedName = bool("animatedName")
        val permission = text("permission")
        val mute = bool("mute")
        val accountID = text("accountID")
        val accountPW = text("accountPW")
        val status = text("status")
        val discord = text("discord").nullable()
        val effectLevel = integer("effectLevel").nullable()
        val effectColor = text("effectColor").nullable()
        val hideRanking = bool("hideRanking")
        val freeze = bool("freeze")
        val hud = text("hud").nullable()
        val tpp = text("tpp").nullable()
        val tppTeam = integer("tppTeam").nullable()
        val log = bool("log")
        val oldUUID = text("oldUUID").nullable()
        val banTime = text("banTime").nullable()
        val duplicateName = text("duplicateName").nullable()
        val tracking = bool("tracking")
        val joinStacks = integer("joinStacks")
        val lastLoginDate = text("lastLoginDate").nullable()
    }

    class PlayerData {
        var name : String = "none"
        var uuid : String = "none"
        var languageTag : String = Locale.getDefault().toLanguageTag()
        var blockPlaceCount : Int = 0
        var blockBreakCount : Int = 0
        var totalJoinCount : Int = 0
        var totalKickCount : Int = 0
        var level : Int = 0
        var exp : Int = 0
        var firstPlayDate : Long = 0
        var lastLoginTime : Long = 0
        var totalPlayTime : Long = 0
        var attackModeClear : Int = 0
        var pvpVictoriesCount : Int = 0
        var pvpDefeatCount : Int = 0
        var animatedName : Boolean = false
        var permission : String = "visitor"
        var mute : Boolean = false
        var accountID : String = "none"
        var accountPW : String = "none"
        var status : ObjectMap<String, String> = ObjectMap()
        var discord : String? = null
        var effectLevel : Int? = null
        var effectColor : String? = null
        var hideRanking : Boolean = false
        var freeze : Boolean = false
        var hud : String? = null
        var tpp : String? = null
        var tppTeam : Int? = null
        var log : Boolean = false
        var oldUUID : String? = null
        var banTime : String? = null
        var duplicateName : String? = null
        var tracking : Boolean = false
        var joinStacks : Int = 0
        var lastLoginDate : LocalDate? = null
        var expMultiplier : Double = 1.0

        var afkTime : Int = 0
        var player : Playerc = mindustry.gen.Player.create()
        var entityid : Int = 0

        override fun toString() : String {
            return """
                name: $name
                uuid: $uuid
                languageTag: $languageTag
                blockPlaceCount: $blockPlaceCount
                blockBreakCount: $blockBreakCount
                totalJoinCount: $totalJoinCount
                totalKickCount: $totalKickCount
                level: $level
                exp: $exp
                firstPlayDate: $firstPlayDate
                lastLoginTime: $lastLoginTime
                totalPlayTime: $totalPlayTime
                attackModeClear: $attackModeClear
                pvpVictoriesCount: $pvpVictoriesCount
                pvpDefeatCount: $pvpDefeatCount
                animatedName: $animatedName
                permission: $permission
                mute: $mute
                accountID: $accountID
                accountPW: $accountPW
                status: $status
                discord: $discord
                effectLevel: $effectLevel
                effectColor: $effectColor
                hideRanking: $hideRanking
                freeze: $freeze
                hud: $hud
                tpp: $tpp
                tppTeam: $tppTeam
                log: $log
                oldUUID: $oldUUID
                banTime: $banTime
                duplicateName: $duplicateName
                tracking: $tracking
                joinStacks: $joinStacks
                afkTime: $afkTime
                entityid: $entityid
                lastLoginDate: ${if(lastLoginDate != null) lastLoginDate.toString() else "null"}
                expMultiplier: $expMultiplier
            """.trimIndent()
        }
    }

    fun createData(data : PlayerData) {
        transaction {
            Player.insert {
                it[name] = data.name
                it[uuid] = data.uuid
                it[languageTag] = data.languageTag
                it[blockPlaceCount] = data.blockPlaceCount
                it[blockBreakCount] = data.blockBreakCount
                it[totalJoinCount] = data.totalJoinCount
                it[totalKickCount] = data.totalKickCount
                it[level] = data.level
                it[exp] = data.exp
                it[firstPlayDate] = data.firstPlayDate
                it[lastLoginTime] = data.lastLoginTime
                it[totalPlayTime] = data.totalPlayTime
                it[attackModeClear] = data.attackModeClear
                it[pvpVictoriesCount] = data.pvpVictoriesCount
                it[pvpDefeatCount] = data.pvpDefeatCount
                it[animatedName] = data.animatedName
                it[permission] = data.permission
                it[mute] = data.mute
                it[accountID] = data.accountID
                it[accountPW] = data.accountPW
                it[status] = data.status.toString()
                it[discord] = null
                it[effectLevel] = -1
                it[effectColor] = null
                it[hideRanking] = data.hideRanking
                it[freeze] = data.freeze
                it[hud] = null
                it[tpp] = null
                it[tppTeam] = data.tppTeam
                it[log] = data.log
                it[oldUUID] = null
                it[banTime] = null
                it[duplicateName] = null
                it[tracking] = data.tracking
                it[joinStacks] = data.joinStacks
                it[lastLoginDate] = LocalDate.now().toString()
            }
        }
    }

    operator fun get(uuid : String) : PlayerData? {
        val d = transaction { Player.select { Player.uuid.eq(uuid) }.firstOrNull() }
        if(d != null) {
            val data = PlayerData()
            data.name = d[Player.name]
            data.uuid = d[Player.uuid]
            data.languageTag = d[Player.languageTag]
            data.blockPlaceCount = d[Player.blockPlaceCount]
            data.blockBreakCount = d[Player.blockBreakCount]
            data.totalJoinCount = d[Player.totalJoinCount]
            data.totalKickCount = d[Player.totalKickCount]
            data.level = d[Player.level]
            data.exp = d[Player.exp]
            data.firstPlayDate = d[Player.firstPlayDate]
            data.lastLoginTime = d[Player.lastLoginTime]
            data.totalPlayTime = d[Player.totalPlayTime]
            data.attackModeClear = d[Player.attackModeClear]
            data.pvpVictoriesCount = d[Player.pvpVictoriesCount]
            data.pvpDefeatCount = d[Player.pvpDefeatCount]
            data.animatedName = d[Player.animatedName]
            data.permission = d[Player.permission]
            data.mute = d[Player.mute]
            data.accountID = d[Player.accountID]
            data.accountPW = d[Player.accountPW]
            data.discord = d[Player.discord]
            data.effectLevel = d[Player.effectLevel]
            data.effectColor = d[Player.effectColor]
            data.hideRanking = d[Player.hideRanking]
            data.freeze = d[Player.freeze]
            data.hud = d[Player.hud]
            data.tpp = d[Player.tpp]
            data.tppTeam = d[Player.tppTeam]
            data.log = d[Player.log]
            data.oldUUID = d[Player.oldUUID]
            data.banTime = d[Player.banTime]
            data.duplicateName = d[Player.duplicateName]
            data.tracking = d[Player.tracking]
            data.joinStacks = d[Player.joinStacks]
            data.lastLoginDate = LocalDate.parse(d[Player.lastLoginDate])

            val obj = ObjectMap<String, String>()
            JsonObject.readHjson(d[Player.status]).asObject().forEach {
                obj.put(it.name, it.value.asString())
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
                data.blockPlaceCount = it[Player.blockPlaceCount]
                data.blockBreakCount = it[Player.blockBreakCount]
                data.totalJoinCount = it[Player.totalJoinCount]
                data.totalKickCount = it[Player.totalKickCount]
                data.level = it[Player.level]
                data.exp = it[Player.exp]
                data.firstPlayDate = it[Player.firstPlayDate]
                data.lastLoginTime = it[Player.lastLoginTime]
                data.totalPlayTime = it[Player.totalPlayTime]
                data.attackModeClear = it[Player.attackModeClear]
                data.pvpVictoriesCount = it[Player.pvpVictoriesCount]
                data.pvpDefeatCount = it[Player.pvpDefeatCount]
                data.animatedName = it[Player.animatedName]
                data.permission = it[Player.permission]
                data.mute = it[Player.mute]
                data.accountID = it[Player.accountID]
                data.accountPW = it[Player.accountPW]
                data.discord = it[Player.discord]
                data.effectLevel = it[Player.effectLevel]
                data.effectColor = it[Player.effectColor]
                data.hideRanking = it[Player.hideRanking]
                data.freeze = it[Player.freeze]
                data.hud = it[Player.hud]
                data.tpp = it[Player.tpp]
                data.tppTeam = it[Player.tppTeam]
                data.log = it[Player.log]
                data.oldUUID = it[Player.oldUUID]
                data.banTime = it[Player.banTime]
                data.duplicateName = it[Player.duplicateName]
                data.tracking = it[Player.tracking]
                data.joinStacks = it[Player.joinStacks]
                data.lastLoginDate = LocalDate.parse(it[Player.lastLoginDate])

                val obj = ObjectMap<String, String>()
                JsonObject.readHjson(it[Player.status]).asObject().forEach { member ->
                    obj.put(member.name, member.value.asString())
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
            Player.selectAll().orderBy(Player.exp, SortOrder.DESC).map {
                val data = PlayerData()
                data.name = it[Player.name]
                data.level = it[Player.level]
                data.exp = it[Player.exp]
                data.totalPlayTime = it[Player.totalPlayTime]
                data.attackModeClear = it[Player.attackModeClear]
                data.pvpVictoriesCount = it[Player.pvpVictoriesCount]
                data.pvpDefeatCount = it[Player.pvpDefeatCount]

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
            Player.update({ Player.uuid eq id }) { it ->
                it[name] = data.name
                it[uuid] = data.uuid
                it[languageTag] = data.languageTag
                it[blockPlaceCount] = data.blockPlaceCount
                it[blockBreakCount] = data.blockBreakCount
                it[totalJoinCount] = data.totalJoinCount
                it[totalKickCount] = data.totalKickCount
                it[level] = data.level
                it[exp] = data.exp
                it[firstPlayDate] = data.firstPlayDate
                it[lastLoginTime] = data.lastLoginTime
                it[totalPlayTime] = data.totalPlayTime
                it[attackModeClear] = data.attackModeClear
                it[pvpVictoriesCount] = data.pvpVictoriesCount
                it[pvpDefeatCount] = data.pvpDefeatCount
                it[animatedName] = data.animatedName
                it[permission] = data.permission
                it[mute] = data.mute
                it[accountID] = data.accountID
                it[accountPW] = data.accountPW
                it[discord] = data.discord!!
                it[effectLevel] = data.effectLevel!!
                it[effectColor] = data.effectColor!!
                it[hideRanking] = data.hideRanking
                it[freeze] = data.freeze
                it[hud] = data.hud!!
                it[tpp] = data.tpp!!
                it[tppTeam] = data.tppTeam
                it[log] = data.log
                it[oldUUID] = data.oldUUID!!
                it[banTime] = data.banTime!!
                it[duplicateName] = data.duplicateName!!
                it[tracking] = data.tracking
                it[joinStacks] = data.joinStacks
                it[lastLoginDate] = data.lastLoginDate.toString()

                val json = JsonObject()
                data.status.forEach {
                    json.add(it.key, it.value)
                }
                it[status] = json.toString()
            }
        }
    }

    fun search(id : String, pw : String) : PlayerData? {
        transaction { Player.select { Player.accountID eq id }.firstOrNull() }.run {
            if(this != null) {
                val data = PlayerData()
                data.name = this[Player.name]
                data.uuid = this[Player.uuid]
                data.languageTag = this[Player.languageTag]
                data.blockPlaceCount = this[Player.blockPlaceCount]
                data.blockBreakCount = this[Player.blockBreakCount]
                data.totalJoinCount = this[Player.totalJoinCount]
                data.totalKickCount = this[Player.totalKickCount]
                data.level = this[Player.level]
                data.exp = this[Player.exp]
                data.firstPlayDate = this[Player.firstPlayDate]
                data.lastLoginTime = this[Player.lastLoginTime]
                data.totalPlayTime = this[Player.totalPlayTime]
                data.attackModeClear = this[Player.attackModeClear]
                data.pvpVictoriesCount = this[Player.pvpVictoriesCount]
                data.pvpDefeatCount = this[Player.pvpDefeatCount]
                data.animatedName = this[Player.animatedName]
                data.permission = this[Player.permission]
                data.mute = this[Player.mute]
                data.accountID = this[Player.accountID]
                data.accountPW = this[Player.accountPW]
                data.discord = this[Player.discord]
                data.effectLevel = this[Player.effectLevel]
                data.effectColor = this[Player.effectColor]
                data.hideRanking = this[Player.hideRanking]
                data.freeze = this[Player.freeze]
                data.hud = this[Player.hud]
                data.tpp = this[Player.tpp]
                data.tppTeam = this[Player.tppTeam]
                data.log = this[Player.log]
                data.oldUUID = this[Player.oldUUID]
                data.banTime = this[Player.banTime]
                data.duplicateName = this[Player.duplicateName]
                data.tracking = this[Player.tracking]
                data.joinStacks = this[Player.joinStacks]
                data.lastLoginDate = LocalDate.parse(this[Player.lastLoginDate])

                val obj = ObjectMap<String, String>()
                JsonObject.readHjson(this[Player.status]).asObject().forEach {
                    obj.put(it.name, it.value.asString())
                }
                data.status = obj

                return if(data.accountID == data.accountPW) data else if(BCrypt.checkpw(pw, data.accountPW)) data else null
            } else {
                return null
            }
        }
    }

    fun close() {
        TransactionManager.closeAndUnregister(db)
    }
}