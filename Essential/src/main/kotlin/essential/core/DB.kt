package essential.core

import arc.Core
import arc.files.Fi
import arc.util.Log
import essential.core.Main.Companion.conf
import essential.core.Main.Companion.daemon
import essential.core.exception.DatabaseNotSupportedException
import mindustry.gen.Playerc
import org.hjson.JsonObject
import org.hjson.Stringify
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.net.URLClassLoader
import java.sql.Driver
import java.sql.DriverManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList


class DB {
    lateinit var db: Database
    val root: Fi = Core.settings.dataDirectory.child("mods/Essentials/")
    val players: MutableList<PlayerData> = mutableListOf()

    fun load() {
        val cacheDir = File(System.getProperty("java.io.tmpdir"))

        // DB 라이브러리 다운로드
        val mavenRepository = "https://repo1.maven.org/maven2"
        val postgresql = "$mavenRepository/org/postgresql/postgresql/42.7.3/postgresql-42.7.3.jar"
        val mysql = "$mavenRepository/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar"
        val mariadb = "$mavenRepository/org/mariadb/jdbc/mariadb-java-client/3.4.0/mariadb-java-client-3.4.0.jar"
        val sqlite = "$mavenRepository/org/xerial/sqlite-jdbc/3.46.0.0/sqlite-jdbc-3.46.0.0.jar"
        val h2 = "$mavenRepository/com/h2database/h2/2.2.224/h2-2.2.224.jar"
        val sqlserver = "$mavenRepository/com/microsoft/sqlserver/mssql-jdbc/12.6.2.jre11/mssql-jdbc-12.6.2.jre11.jar"
        val drivers = arrayOf(
            Triple(postgresql, "postgresql", "org.postgresql.Driver"),
            Triple(mysql, "mysql", "com.mysql.cj.jdbc.Driver"),
            Triple(mariadb, "mariadb", "org.mariadb.jdbc.Driver"),
            Triple(sqlite, "sqlite", "org.sqlite.JDBC"),
            Triple(h2, "h2", "org.h2.Driver"),
            Triple(sqlserver, "sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver")
        )
        drivers.forEach { driver ->
            val fileName = driver.first.substring(driver.first.lastIndexOf('/') + 1)
            val targetPath = File(cacheDir, fileName)
            if (!targetPath.exists()) {
                Log.info("Downloading $fileName...")
                FileOutputStream(targetPath).write(URL(driver.first).readBytes())
                Log.info("$fileName saved to ${targetPath.absolutePath}")
            }
        }

        LoggerFactory.getILoggerFactory()

        // DB 드라이버 불러오기
        try {
            fun load(type: String) {
                val driver = drivers.find { driver -> driver.second == type }
                if (driver != null) {
                    val f = File(cacheDir, driver.first.substring(driver.first.lastIndexOf('/') + 1)).toURI().toURL()
                    val d = Class.forName(driver.third, true, URLClassLoader(arrayOf(f), this.javaClass.classLoader))
                        .getDeclaredConstructor()
                        .newInstance() as Driver
                    DriverManager.registerDriver(DriverLoader(d))
                } else {
                    throw DatabaseNotSupportedException("$type doesn't supported!")
                }
            }

            val type = "^[^:]*".toRegex().find(conf.plugin.database.url)
            if (type != null) {
                load(type.value)
            } else {
                load("sqlite")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun connect() {
        fun connectDefaultDatabase(): Database {
            return Database.connect({
                DriverManager.getConnection("jdbc:sqlite:config/mods/Essentials/data/database.db", "sa", "123")
            })
        }

        db = run {
            val type = "^[^:]*".toRegex().find(conf.plugin.database.url)
            if (type != null) {
                Database.connect({
                    DriverManager.getConnection(
                        "jdbc:${conf.plugin.database.url}",
                        conf.plugin.database.username,
                        conf.plugin.database.password
                    )
                })
            } else {
                connectDefaultDatabase()
            }
        }
    }

    fun create() {
        transaction {
            if (!Player.exists()) SchemaUtils.create(Player)
            if (!Data.exists()) SchemaUtils.create(Data)
            if (!DB.exists()) SchemaUtils.create(DB)
        }
    }

    object Data : Table("data") {
        val data = text("data")
    }

    object DB : Table("db") {
        val version = integer("version")
    }

    object Player : Table("player") {
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
        val lastLoginDate = date("lastLoginDate").nullable()
        val lastLeaveDate = datetime("lastLeaveDate").nullable()
        val showLevelEffects = bool("showLevelEffects")
        val currentPlayTime = long("currentPlayTime")
        val isConnected = bool("isConnected")
        val lastPlayedWorldName = text("lastPlayedWorldName").nullable()
        val lastPlayedWorldMode = text("lastPlayedWorldMode").nullable()
        val lastPlayedWorldId = integer("lastPlayedWorldId").nullable()
        val mvpTime = integer("mvpTime")
        val pvpEliminationTeamCount = integer("pvpEliminationTeamCount")
        val strict = bool("strict")
    }


    class PlayerData {
        var name: String = "none"
        var uuid: String = "none"
        var languageTag: String = Locale.getDefault().toLanguageTag()
        var blockPlaceCount: Int = 0
        var blockBreakCount: Int = 0
        var totalJoinCount: Int = 0
        var totalKickCount: Int = 0
        var level: Int = 0
        var exp: Int = 0
        var firstPlayDate: Long = 0
        var lastLoginTime: Long = 0
        var totalPlayTime: Long = 0
        var attackModeClear: Int = 0
        var pvpVictoriesCount: Int = 0
        var pvpDefeatCount: Int = 0
        var animatedName: Boolean = false
        var permission: String = "visitor"
        var mute: Boolean = false
        var accountID: String = "none"
        var accountPW: String = "none"
        var status = HashMap<String, String>()
        var discord: String? = null
        var effectLevel: Int? = -1
        var effectColor: String? = null
        var hideRanking: Boolean = false
        var freeze: Boolean = false
        var hud: String? = null
        var tpp: String? = null
        var tppTeam: Int? = null
        var log: Boolean = false
        var oldUUID: String? = null
        var banTime: String? = null
        var duplicateName: String? = null
        var tracking: Boolean = false
        var joinStacks: Int = 0
        var lastLoginDate: LocalDate? = null
        var lastLeaveDate: LocalDateTime? = null
        var showLevelEffects: Boolean = true
        var currentPlayTime: Long = 0L
        var currentUnitDestroyedCount: Int = 0
        var currentBuildDestroyedCount: Int = 0
        var currentBuildAttackCount: Int = 0
        var apm = LinkedList<Int>()
        var currentControlCount: Int = 0
        var currentBuildDeconstructedCount: Int = 0
        var isConnected: Boolean = false
        var lastPlayedWorldName: String? = null
        var lastPlayedWorldMode: String? = null
        var lastPlayedWorldId: Int? = null
        var mvpTime: Int = 0
        var pvpEliminationTeamCount: Int = 0
        var strict: Boolean = false

        var expMultiplier: Double = 1.0
        var currentExp: Int = 0

        var afkTime: Int = 0
        var afk: Boolean = false
        var previousMousePosition: Float = 0F
        var player: Playerc = mindustry.gen.Player.create()
        var entityid: Int = 0

        // Use plugin test only
        var lastSentMessage: String = ""

        /**
         * Bundle 파일에서 [message] 값을 경고 메세지로 플레이어에게 보냄
         */
        fun err(message: String, vararg parameters: Any) {
            val text = "[scarlet]" + Bundle(languageTag).get(message, *parameters)
            player.sendMessage(text)
            lastSentMessage = text
        }

        /**
         * Bundle 파일에서 [message] 값을 플레이어에게 보냄
         */
        fun send(message: String, vararg parameters: Any) {
            val text = Bundle(languageTag).get(message, *parameters)
            player.sendMessage(text)
            lastSentMessage = text
        }
    }

    fun createData(data: PlayerData) {
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
                it[status] = JsonObject().toString(Stringify.HJSON)
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
                it[lastLoginDate] = null
                it[lastLeaveDate] = null
                it[showLevelEffects] = data.showLevelEffects
                it[currentPlayTime] = data.currentPlayTime
                it[isConnected] = data.isConnected
                it[lastPlayedWorldName] = data.lastPlayedWorldName
                it[lastPlayedWorldMode] = data.lastPlayedWorldMode
                it[lastPlayedWorldId] = data.lastPlayedWorldId
                it[mvpTime] = data.mvpTime
                it[pvpEliminationTeamCount] = data.pvpEliminationTeamCount
                it[strict] = data.strict
            }
        }
    }

    operator fun get(uuid: String): PlayerData? {
        val it = transaction { Player.selectAll().where { Player.uuid.eq(uuid) }.firstOrNull() }
        return if (it != null) {
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
            data.lastLoginDate =
                if (it[Player.lastLoginDate] == null) null else it[Player.lastLoginDate]
            data.lastLeaveDate =
                if (it[Player.lastLeaveDate] == null) null else it[Player.lastLeaveDate]
            data.showLevelEffects = it[Player.showLevelEffects]
            data.currentPlayTime = it[Player.currentPlayTime]
            data.isConnected = it[Player.isConnected]
            data.lastPlayedWorldName = it[Player.lastPlayedWorldName]
            data.lastPlayedWorldMode = it[Player.lastPlayedWorldMode]
            data.lastPlayedWorldId = it[Player.lastPlayedWorldId]
            data.mvpTime = it[Player.mvpTime]
            data.pvpEliminationTeamCount = it[Player.pvpEliminationTeamCount]
            data.strict = it[Player.strict]

            val obj = HashMap<String, String>()
            JsonObject.readHjson(it[Player.status]).asObject().forEach {
                obj[it.name] = it.value.asString()
            }
            data.status = obj
            data
        } else {
            null
        }
    }

    fun getAll(): ArrayList<PlayerData> {
        val d = ArrayList<PlayerData>()

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
                data.lastLoginDate = if (it[Player.lastLoginDate] == null) null else it[Player.lastLoginDate]
                data.lastLeaveDate = if (it[Player.lastLeaveDate] == null) null else it[Player.lastLeaveDate]
                data.showLevelEffects = it[Player.showLevelEffects]
                data.currentPlayTime = it[Player.currentPlayTime]
                data.isConnected = it[Player.isConnected]
                data.lastPlayedWorldName = it[Player.lastPlayedWorldName]
                data.lastPlayedWorldMode = it[Player.lastPlayedWorldMode]
                data.lastPlayedWorldId = it[Player.lastPlayedWorldId]
                data.mvpTime = it[Player.mvpTime]
                data.pvpEliminationTeamCount = it[Player.pvpEliminationTeamCount]
                data.strict = it[Player.strict]

                val obj = HashMap<String, String>()
                JsonObject.readHjson(it[Player.status]).asObject().forEach { member ->
                    obj[member.name] = member.value.asString()
                }
                data.status = obj
                d.add(data)
            }
        }
        return d
    }

    fun getAllByExp(): Array<PlayerData> {
        val d = ArrayList<PlayerData>()

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

        return d.toTypedArray()
    }

    fun queue(data: PlayerData) {
        daemon.submit {
            transaction {
                update(data.uuid, data)
            }
        }
    }

    fun update(id: String, data: PlayerData) {
        transaction {
            Player.update({ Player.uuid eq id }) {
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
                it[lastLoginDate] = if (data.lastLoginDate == null) null else data.lastLoginDate
                it[lastLeaveDate] = if (data.lastLeaveDate == null) null else data.lastLeaveDate
                it[showLevelEffects] = data.showLevelEffects
                it[currentPlayTime] = data.currentPlayTime
                it[isConnected] = data.isConnected
                it[lastPlayedWorldName] = data.lastPlayedWorldName
                it[lastPlayedWorldMode] = data.lastPlayedWorldMode
                it[lastPlayedWorldId] = data.lastPlayedWorldId
                it[mvpTime] = data.mvpTime
                it[pvpEliminationTeamCount] = data.pvpEliminationTeamCount
                it[strict] = data.strict

                val json = JsonObject()
                data.status.forEach { entry ->
                    json.add(entry.key, entry.value)
                }
                it[status] = json.toString()
            }
        }
    }

    fun search(id: String, pw: String): PlayerData? {
        transaction { Player.selectAll().where { Player.accountID eq id }.firstOrNull() }.run {
            return if (this != null) {
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
                data.lastLoginDate = if (data.lastLoginDate == null) null else this[Player.lastLoginDate]
                data.lastLeaveDate = if (data.lastLeaveDate == null) null else this[Player.lastLeaveDate]
                data.showLevelEffects = this[Player.showLevelEffects]
                data.currentPlayTime = this[Player.currentPlayTime]
                data.isConnected = this[Player.isConnected]
                data.lastPlayedWorldName = this[Player.lastPlayedWorldName]
                data.lastPlayedWorldMode = this[Player.lastPlayedWorldMode]
                data.lastPlayedWorldId = this[Player.lastPlayedWorldId]
                data.mvpTime = this[Player.mvpTime]
                data.pvpEliminationTeamCount = this[Player.pvpEliminationTeamCount]
                data.strict = this[Player.strict]

                val obj = HashMap<String, String>()
                JsonObject.readHjson(this[Player.status]).asObject().forEach {
                    obj[it.name] = it.value.asString()
                }
                data.status = obj

                if (data.accountID == data.accountPW) data else if (BCrypt.checkpw(pw, data.accountPW)) data else null
            } else {
                null
            }
        }
    }

    fun <T> executeInTransaction(block: () -> T): T {
        return transaction {
            block()
        }
    }

    fun close() {
        TransactionManager.closeAndUnregister(db)
    }
}