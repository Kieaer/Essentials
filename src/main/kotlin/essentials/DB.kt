package essentials

import arc.Core
import arc.struct.ObjectMap
import arc.struct.Seq
import arc.util.Log
import mindustry.gen.Playerc
import org.h2.jdbc.JdbcSQLNonTransientConnectionException
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit

class DB {
    val players : Seq<PlayerData> = Seq()
    private var isRemote : Boolean = false
    lateinit var db : Database
    var dbServer : Server? = null
    var dbVersion = 2

    fun backup() {
        if (Config.database.equals(Main.root.child("database").absolutePath(), false)) {
            if (Main.root.child("backup").list().size > 20) {
                Main.root.child("backup").list().first().delete()
            }
            Main.root.child("database.mv.db").copyTo(Main.root.child("backup/database-stable-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss"))}.mv.db"))
        }
    }

    fun open() {
        try {
            fun updateLibrary(version : String) {
                if (!Main.root.child("data/$version.jar").exists()) {
                    Log.info(Bundle()["event.plugin.db.downloading", version])
                    Main.root.child("data").mkdirs()
                    URL("https://repo1.maven.org/maven2/com/h2database/h2/$version/h2-$version.jar").openStream().use { b ->
                        BufferedInputStream(b).use { bis ->
                            FileOutputStream(Main.root.child("data/h2-$version.jar").absolutePath()).use { fos ->
                                val data = ByteArray(1024)
                                var count : Int
                                while (bis.read(data, 0, 1024).also { count = it } != -1) {
                                    fos.write(data, 0, count)
                                }
                            }
                        }
                    }
                }

                Log.info(Bundle()["event.plugin.db.export"])
                val os = System.getProperty("os.name").lowercase(Locale.getDefault())
                if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                    val cmd = arrayOf("/bin/bash", "-c", "cd ${Main.root.child("data").absolutePath()} && java -cp h2-$version.jar org.h2.tools.Script -url jdbc:h2:../database.db -user sa -script script.sql")
                    Runtime.getRuntime().exec(cmd).waitFor()
                } else {
                    val cmd = arrayOf("cmd", "/c", "cd /D ${Main.root.child("data").absolutePath()} && java -cp h2-$version.jar org.h2.tools.Script -url jdbc:h2:../database -user sa -script script.sql")
                    Runtime.getRuntime().exec(cmd).waitFor()
                }
            }

            fun connectServer() {
                isRemote = !Config.database.equals(Main.root.child("database").absolutePath(), false)
                if (!isRemote) {
                    try {
                        db = Database.connect("jdbc:h2:${Config.database}", "org.h2.Driver", "sa", "")
                        dbServer = Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9092", "-ifNotExists", "-key", "db", Config.database).start()
                    } catch (e : Exception) {
                        db = Database.connect("jdbc:h2:tcp://127.0.0.1:9092/db", "org.h2.Driver", "sa", "")
                        Log.info(Bundle()["event.database.remote"])
                    }
                } else {
                    db = Database.connect("jdbc:h2:tcp://${Config.database}:9092/db", "org.h2.Driver", "sa", "")
                }
            }

            backup()
            connectServer()

            if (!isRemote) {
                var migrateVersion = if (Main.root.child("data/migrateVersion.txt").exists()) {
                    Main.root.child("data/migrateVersion.txt").readString().toInt()
                } else {
                    0
                }

                if (Main.root.child("database.db.mv.db").exists()) {
                    updateLibrary("1.4.200")

                    Main.root.child("database.db.mv.db").moveTo(Main.root.child("old-database.mv.db"))
                    migrateVersion = 0
                }

                Main.daemon.submit {
                    while (!Thread.currentThread().isInterrupted) {
                        TimeUnit.DAYS.sleep(1)
                        transaction {
                            exec("BACKUP TO ${Main.root.child("backup/database-online-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss"))}.mv.db").absolutePath()}'")
                        }
                    }
                }


                try {
                    transaction {
                        connection.commit()
                    }
                } catch (e : JdbcSQLNonTransientConnectionException) {
                    close()
                    updateLibrary("2.1.214")

                    Main.root.child("database.mv.db").moveTo(Main.root.child("old-database-v2.mv.db"))
                    migrateVersion = 1
                    connectServer()
                }

                if (Main.root.child("data/script.sql").exists()) {
                    Log.info(Bundle()["event.plugin.db.updating"])
                    when (migrateVersion) {
                        0 -> RunScript.main("-url", "jdbc:h2:${Config.database}", "-user", "sa", "-script", Main.root.child("data/script.sql").absolutePath(), "-options", "FROM_1X")
                        1 -> RunScript.main("-url", "jdbc:h2:${Config.database}", "-user", "sa", "-script", Main.root.child("data/script.sql").absolutePath())
                    }

                    Main.root.child("data/script_backup.sql").delete()
                    Main.root.child("data/script.sql").moveTo(Main.root.child("data/script_backup.sql"))

                    close()
                    connectServer()
                }

                Main.root.child("data/migrateVersion.txt").writeString("1")

                transaction {
                    if (!connection.isClosed) {
                        SchemaUtils.create(Player)
                        SchemaUtils.create(Data)
                        SchemaUtils.create(DB)

                        if (DB.selectAll().empty()) {
                            val query = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='PLAYER'"
                            val list = mutableListOf<String>()
                            exec(query) {
                                val id = it.findColumn("COLUMN_NAME")
                                while (it.next()) {
                                    list.add(it.getString(id))
                                }
                            }

                            DB.insert {
                                val ver = if (!list.contains("blockPlaceCount")) {
                                    0
                                } else if (!list.contains("pvpEliminationTeamCount")) {
                                    1
                                } else {
                                    dbVersion
                                }
                                it[version] = ver
                            }
                        }

                        if (!isRemote) {
                            fun upgrade() {
                                val command = when (DB.selectAll().first()[DB.version]) {
                                    0 -> {
                                        listOf(
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
                                            "ALTER TABLE Player ADD COLUMN IF NOT EXISTS \"joinStacks\" INTEGER",
                                            "ALTER TABLE Player ADD COLUMN IF NOT EXISTS \"lastLoginDate\" CHARACTER VARYING",
                                            "ALTER TABLE Player ADD COLUMN IF NOT EXISTS \"lastLeaveDate\" CHARACTER VARYING",
                                            "ALTER TABLE Player ADD COLUMN IF NOT EXISTS \"showLevelEffects\" CHARACTER VARYING",
                                            "ALTER TABLE Player ADD COLUMN IF NOT EXISTS \"currentPlayTime\" INTEGER",
                                            "ALTER TABLE Player ADD COLUMN IF NOT EXISTS \"isConnected\" BOOLEAN",
                                            "ALTER TABLE Player ADD COLUMN IF NOT EXISTS \"lastPlayedWorldName\" CHARACTER VARYING",
                                            "ALTER TABLE Player ADD COLUMN IF NOT EXISTS \"lastPlayedWorldMode\" CHARACTER VARYING",
                                            "ALTER TABLE Player ADD COLUMN IF NOT EXISTS \"lastPlayedWorldId\" INTEGER",
                                            "ALTER TABLE Player ADD COLUMN IF NOT EXISTS \"mvpTime\" INTEGER",
                                            "UPDATE player SET \"hideRanking\" = false, freeze = false, log = false, tracking = false,\"joinStacks\" = 0, \"showLevelEffects\" = true, \"isConnected\" = false, \"currentPlayTime\" = 0, \"mvpTime\" = 0",
                                            "UPDATE db SET version = 1"
                                        )
                                    }

                                    1 -> {
                                        listOf(
                                            "ALTER TABLE Player ADD COLUMN IF NOT EXISTS \"pvpEliminationTeamCount\" INTEGER",
                                            "UPDATE player SET \"pvpEliminationTeamCount\" = 0",
                                            "UPDATE db SET version = 2"
                                        )
                                    }

                                    else -> listOf()
                                }

                                execInBatch(command)
                            }

                            var isUpgraded = false
                            while (DB.selectAll().first()[DB.version] != dbVersion) {
                                isUpgraded = true
                                upgrade()
                            }

                            if (isUpgraded) {
                                Log.info(Bundle()["event.plugin.db.version", dbVersion])
                                Log.warn(Bundle()["event.plugin.db.warning"])
                            }

                            try {
                                getAll()
                            } catch (e : ParseException) {
                                Player.update {
                                    it[status] = "{}"
                                }
                            }

                            if (!Main.root.child("data/isDuplicateNameChecked.txt").exists()) {
                                val sql = """
                                UPDATE player SET "duplicateName" = null;
                                SELECT * FROM player t1 WHERE EXISTS (
                                    SELECT 1
                                    FROM player t2
                                    WHERE t2.name = t1.name
                                    GROUP BY t2.name
                                    HAVING COUNT(*) > 1
                                    AND MIN(t2."firstPlayDate") <> t1."firstPlayDate"
                                )
                            """.trimIndent()

                                exec(sql) { rs ->
                                    while (rs.next()) {
                                        val data = get(rs.getString("uuid"))
                                        if (data != null) {
                                            try {
                                                if (rs.getString("duplicateName") == "null") {
                                                    data.duplicateName = data.name
                                                }
                                            } catch (e : ParseException) {
                                                if (data.duplicateName == null) {
                                                    data.duplicateName = data.name
                                                }
                                            }
                                            update(data.uuid, data)
                                        }
                                    }
                                }

                                Main.root.child("data/isDuplicateNameChecked.txt").writeString("Yes! No more time spent checking player name duplicates on plugin startup lol")
                            }
                        }
                    } else {
                        Log.err(Bundle()["event.plugin.db.wrong"])
                        dbServer?.stop()
                        Core.app.exit()
                    }
                }
            }
        } catch (e : Exception) {
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
        val lastLeaveDate = text("lastLeaveDate").nullable()
        val showLevelEffects = bool("showLevelEffects")
        val currentPlayTime = long("currentPlayTime")
        val isConnected = bool("isConnected")
        val lastPlayedWorldName = text("lastPlayedWorldName").nullable()
        val lastPlayedWorldMode = text("lastPlayedWorldMode").nullable()
        val lastPlayedWorldId = integer("lastPlayedWorldId").nullable()
        val mvpTime = integer("mvpTime")
        val pvpEliminationTeamCount = integer("pvpEliminationTeamCount")
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
        var lastLeaveDate : LocalDateTime? = null
        var showLevelEffects : Boolean = true
        var currentPlayTime : Long = 0L
        var isConnected : Boolean = false
        var lastPlayedWorldName : String? = null
        var lastPlayedWorldMode : String? = null
        var lastPlayedWorldId : Int? = null
        var mvpTime : Int = 0
        var pvpEliminationTeamCount : Int = 0

        var expMultiplier : Double = 1.0
        var currentExp : Int = 0

        var afkTime : Int = 0
        var previousMousePosition : Float = 0F
        var player : Playerc = mindustry.gen.Player.create()
        var entityid : Int = 0

        // Use plugin test only
        var lastSentMessage : String = ""
        override fun toString(): String {
            return "PlayerData(name='$name', uuid='$uuid', languageTag='$languageTag', blockPlaceCount=$blockPlaceCount, blockBreakCount=$blockBreakCount, totalJoinCount=$totalJoinCount, totalKickCount=$totalKickCount, level=$level, exp=$exp, firstPlayDate=$firstPlayDate, lastLoginTime=$lastLoginTime, totalPlayTime=$totalPlayTime, attackModeClear=$attackModeClear, pvpVictoriesCount=$pvpVictoriesCount, pvpDefeatCount=$pvpDefeatCount, animatedName=$animatedName, permission='$permission', mute=$mute, accountID='$accountID', accountPW='$accountPW', status=$status, discord=$discord, effectLevel=$effectLevel, effectColor=$effectColor, hideRanking=$hideRanking, freeze=$freeze, hud=$hud, tpp=$tpp, tppTeam=$tppTeam, log=$log, oldUUID=$oldUUID, banTime=$banTime, duplicateName=$duplicateName, tracking=$tracking, joinStacks=$joinStacks, lastLoginDate=$lastLoginDate, lastLeaveDate=$lastLeaveDate, showLevelEffects=$showLevelEffects, currentPlayTime=$currentPlayTime, isConnected=$isConnected, lastPlayedWorldName=$lastPlayedWorldName, lastPlayedWorldMode=$lastPlayedWorldMode, lastPlayedWorldId=$lastPlayedWorldId, mvpTime=$mvpTime, pvpEliminationTeamCount=$pvpEliminationTeamCount, expMultiplier=$expMultiplier, currentExp=$currentExp, afkTime=$afkTime, previousMousePosition=$previousMousePosition, player=$player, entityid=$entityid, lastSentMessage='$lastSentMessage')"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PlayerData

            return uuid == other.uuid
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + uuid.hashCode()
            return result
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
                it[lastLeaveDate] = null
                it[showLevelEffects] = data.showLevelEffects
                it[currentPlayTime] = data.currentPlayTime
                it[isConnected] = data.isConnected
                it[lastPlayedWorldName] = data.lastPlayedWorldName
                it[lastPlayedWorldMode] = data.lastPlayedWorldMode
                it[lastPlayedWorldId] = data.lastPlayedWorldId
                it[mvpTime] = data.mvpTime
                it[pvpEliminationTeamCount] = data.pvpEliminationTeamCount
            }
        }
    }

    operator fun get(uuid : String) : PlayerData? {
        val it = transaction { Player.select { Player.uuid.eq(uuid) }.firstOrNull() }
        if (it != null) {
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
            data.lastLoginDate = if (it[Player.lastLoginDate] == null) null else LocalDate.parse(it[Player.lastLoginDate])
            data.lastLeaveDate = if (it[Player.lastLeaveDate] == null) null else LocalDateTime.parse(it[Player.lastLeaveDate])
            data.showLevelEffects = it[Player.showLevelEffects]
            data.currentPlayTime = it[Player.currentPlayTime]
            data.isConnected = it[Player.isConnected]
            data.lastPlayedWorldName = it[Player.lastPlayedWorldName]
            data.lastPlayedWorldMode = it[Player.lastPlayedWorldMode]
            data.lastPlayedWorldId = it[Player.lastPlayedWorldId]
            data.mvpTime = it[Player.mvpTime]
            data.pvpEliminationTeamCount = it[Player.pvpEliminationTeamCount]

            val obj = ObjectMap<String, String>()
            JsonObject.readHjson(it[Player.status]).asObject().forEach {
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
                data.lastLoginDate = if (it[Player.lastLoginDate] == null) null else LocalDate.parse(it[Player.lastLoginDate])
                data.lastLeaveDate = if (it[Player.lastLeaveDate] == null) null else LocalDateTime.parse(it[Player.lastLeaveDate])
                data.showLevelEffects = it[Player.showLevelEffects]
                data.currentPlayTime = it[Player.currentPlayTime]
                data.isConnected = it[Player.isConnected]
                data.lastPlayedWorldName = it[Player.lastPlayedWorldName]
                data.lastPlayedWorldMode = it[Player.lastPlayedWorldMode]
                data.lastPlayedWorldId = it[Player.lastPlayedWorldId]
                data.mvpTime = it[Player.mvpTime]
                data.pvpEliminationTeamCount = it[Player.pvpEliminationTeamCount]

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
                it[discord] = data.discord
                it[effectLevel] = data.effectLevel
                it[effectColor] = data.effectColor
                it[hideRanking] = data.hideRanking
                it[freeze] = data.freeze
                it[hud] = data.hud
                it[tpp] = data.tpp
                it[tppTeam] = data.tppTeam
                it[log] = data.log
                it[oldUUID] = data.oldUUID
                it[banTime] = data.banTime
                it[duplicateName] = data.duplicateName
                it[tracking] = data.tracking
                it[joinStacks] = data.joinStacks
                it[lastLoginDate] = if (data.lastLoginDate == null) null else data.lastLoginDate.toString()
                it[lastLeaveDate] = if (data.lastLeaveDate == null) null else data.lastLeaveDate.toString()
                it[showLevelEffects] = data.showLevelEffects
                it[currentPlayTime] = data.currentPlayTime
                it[isConnected] = data.isConnected
                it[lastPlayedWorldName] = data.lastPlayedWorldName
                it[lastPlayedWorldMode] = data.lastPlayedWorldMode
                it[lastPlayedWorldId] = data.lastPlayedWorldId
                it[mvpTime] = data.mvpTime
                it[pvpEliminationTeamCount] = data.pvpEliminationTeamCount

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
            if (this != null) {
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
                data.lastLoginDate = if (data.lastLoginDate == null) null else LocalDate.parse(this[Player.lastLoginDate])
                data.lastLeaveDate = if (data.lastLeaveDate == null) null else LocalDateTime.parse(this[Player.lastLeaveDate])
                data.showLevelEffects = this[Player.showLevelEffects]
                data.currentPlayTime = this[Player.currentPlayTime]
                data.isConnected = this[Player.isConnected]
                data.lastPlayedWorldName = this[Player.lastPlayedWorldName]
                data.lastPlayedWorldMode = this[Player.lastPlayedWorldMode]
                data.lastPlayedWorldId = this[Player.lastPlayedWorldId]
                data.mvpTime = this[Player.mvpTime]
                data.pvpEliminationTeamCount = this[Player.pvpEliminationTeamCount]

                val obj = ObjectMap<String, String>()
                JsonObject.readHjson(this[Player.status]).asObject().forEach {
                    obj.put(it.name, it.value.asString())
                }
                data.status = obj

                return if (data.accountID == data.accountPW) data else if (BCrypt.checkpw(pw, data.accountPW)) data else null
            } else {
                return null
            }
        }
    }

    fun close() {
        dbServer?.stop()
        TransactionManager.closeAndUnregister(db)
    }


}