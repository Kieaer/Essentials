package essential.core

import arc.Core
import arc.files.Fi
import arc.util.Log
import essential.core.Main.Companion.conf
import essential.core.Main.Companion.daemon
import essential.core.exception.DatabaseNotSupportedException
import mindustry.gen.Playerc
import mindustry.net.Administration.PlayerInfo
import org.hjson.JsonObject
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.UpdateBuilder
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
import java.time.format.DateTimeFormatter
import java.util.*


class DB {
    lateinit var db: Database
    val root: Fi = Core.settings.dataDirectory.child("mods/Essentials/")
    val players: MutableList<PlayerData> = mutableListOf()

    fun load() {
        val dir = root.child("drivers/")
        dir.mkdirs()
        val cacheDir = dir.file()

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
                Log.info("$fileName saved")
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

    fun createTable() {
        transaction {
            SchemaUtils.create(Data, Player, DBTable, Banned)
        }
    }

    object Banned : Table("banned") {
        val type = integer("type")
        val data = text("data")
    }

    object Data : Table("data") {
        val data = text("data")
    }

    object DBTable : Table("db") {
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
            val text = bundle().get(message, *parameters)
            player.sendMessage(text)
            lastSentMessage = text
        }

        /**
         * 외부 Bundle 파일에서 [message] 값을 플레이어에게 보냄
         */
        fun send(bundle: Bundle, message: String, vararg parameters: Any) {
            val text = bundle.get(message, *parameters)
            player.sendMessage(text)
            lastSentMessage = text
        }

        fun bundle() : Bundle {
            return if (status.containsKey("language")) {
                Bundle(status["language"]!!)
            } else {
                Bundle(player.locale())
            }
        }

        override fun toString(): String {
            return """PlayerData(name='$name', 
                    uuid='$uuid', 
                    languageTag='$languageTag', 
                    blockPlaceCount=$blockPlaceCount, 
                    blockBreakCount=$blockBreakCount, 
                    totalJoinCount=$totalJoinCount, 
                    totalKickCount=$totalKickCount, 
                    level=$level, 
                    exp=$exp, 
                    firstPlayDate=$firstPlayDate, 
                    lastLoginTime=$lastLoginTime, 
                    totalPlayTime=$totalPlayTime, 
                    attackModeClear=$attackModeClear, 
                    pvpVictoriesCount=$pvpVictoriesCount, 
                    pvpDefeatCount=$pvpDefeatCount, 
                    animatedName=$animatedName, 
                    permission='$permission', 
                    mute=$mute, 
                    accountID='$accountID', 
                    accountPW='$accountPW', 
                    status=$status, 
                    discord=$discord, 
                    effectLevel=$effectLevel, 
                    effectColor=$effectColor, 
                    hideRanking=$hideRanking, 
                    freeze=$freeze, 
                    hud=$hud, 
                    tpp=$tpp, 
                    tppTeam=$tppTeam, 
                    log=$log, 
                    oldUUID=$oldUUID, 
                    banTime=$banTime, 
                    duplicateName=$duplicateName, 
                    tracking=$tracking, 
                    joinStacks=$joinStacks, 
                    lastLoginDate=$lastLoginDate, 
                    lastLeaveDate=$lastLeaveDate, 
                    showLevelEffects=$showLevelEffects, 
                    currentPlayTime=$currentPlayTime, 
                    currentUnitDestroyedCount=$currentUnitDestroyedCount, 
                    currentBuildDestroyedCount=$currentBuildDestroyedCount, 
                    currentBuildAttackCount=$currentBuildAttackCount, 
                    apm=$apm, 
                    currentControlCount=$currentControlCount, 
                    currentBuildDeconstructedCount=$currentBuildDeconstructedCount, 
                    isConnected=$isConnected, 
                    lastPlayedWorldName=$lastPlayedWorldName, 
                    lastPlayedWorldMode=$lastPlayedWorldMode, 
                    lastPlayedWorldId=$lastPlayedWorldId, 
                    mvpTime=$mvpTime, 
                    pvpEliminationTeamCount=$pvpEliminationTeamCount, 
                    strict=$strict, 
                    expMultiplier=$expMultiplier, 
                    currentExp=$currentExp, 
                    afkTime=$afkTime, 
                    afk=$afk, 
                    previousMousePosition=$previousMousePosition, 
                    player=$player, 
                    entityid=$entityid, 
                    lastSentMessage='$lastSentMessage')
                    """.trimIndent()
        }
    }

    fun createData(data: PlayerData) {
        transaction {
            Player.insert {
                convertToQueue(it, data)
            }
        }
    }

    operator fun get(uuid: String): PlayerData? {
        return transaction {
            val data = Player.selectAll().where { Player.uuid.eq(uuid) }.firstOrNull()
            if (data != null) {
                convertToData(data)
            } else {
                null
            }
        }
    }

    fun getAll(): ArrayList<PlayerData> {
        val d = ArrayList<PlayerData>()

        transaction {
            Player.selectAll().forEach {
                d.add(convertToData(it))
            }
        }
        return d
    }

    fun getAllByExp(): Array<PlayerData> {
        val d = ArrayList<PlayerData>()

        transaction {
            Player.selectAll().orderBy(Player.exp, SortOrder.DESC).forEach {
                d.add(convertToData(it))
            }
        }
        return d.toTypedArray()
    }

    fun getByDiscord(discord: String): PlayerData? {
        return transaction {
            val data = Player.selectAll().where { Player.discord eq discord }.firstOrNull()
            if (data != null) {
                convertToData(data)
            } else {
                null
            }
        }
    }

    fun getByName(name: String): PlayerData? {
        return transaction {
            val data = Player.selectAll().where { Player.name eq name }.firstOrNull()
            if (data != null) {
                convertToData(data)
            } else {
                null
            }
        }
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
                convertToQueue(it, data)
            }
        }
    }

    fun search(id: String, pw: String): PlayerData? {
        return transaction { Player.selectAll().where { Player.accountID eq id }.firstOrNull() }.run {
            if (this != null) {
                val data = convertToData(this)
                if (data.accountID == data.accountPW) data else if (BCrypt.checkpw(pw, data.accountPW)) data else null
            } else {
                null
            }
        }
    }

    fun convertToData(it: ResultRow) : PlayerData {
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
        data.lastLoginDate = if (it[Player.lastLoginDate] == null) null else LocalDate.parse(it[Player.lastLoginDate], DateTimeFormatter.ISO_LOCAL_DATE)
        data.lastLeaveDate = if (it[Player.lastLeaveDate] == null) null else LocalDateTime.parse(it[Player.lastLeaveDate], DateTimeFormatter.ISO_LOCAL_DATE_TIME)
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
        return data
    }

    fun convertToQueue(it: UpdateBuilder<*>, data: PlayerData): UpdateBuilder<*> {
        it[Player.name] = data.name
        it[Player.uuid] = data.uuid
        it[Player.languageTag] = data.languageTag
        it[Player.blockPlaceCount] = data.blockPlaceCount
        it[Player.blockBreakCount] = data.blockBreakCount
        it[Player.totalJoinCount] = data.totalJoinCount
        it[Player.totalKickCount] = data.totalKickCount
        it[Player.level] = data.level
        it[Player.exp] = data.exp
        it[Player.firstPlayDate] = data.firstPlayDate
        it[Player.lastLoginTime] = data.lastLoginTime
        it[Player.totalPlayTime] = data.totalPlayTime
        it[Player.attackModeClear] = data.attackModeClear
        it[Player.pvpVictoriesCount] = data.pvpVictoriesCount
        it[Player.pvpDefeatCount] = data.pvpDefeatCount
        it[Player.animatedName] = data.animatedName
        it[Player.permission] = data.permission
        it[Player.mute] = data.mute
        it[Player.accountID] = data.accountID
        it[Player.accountPW] = data.accountPW
        it[Player.discord] = data.discord
        it[Player.effectLevel] = data.effectLevel
        it[Player.effectColor] = data.effectColor
        it[Player.hideRanking] = data.hideRanking
        it[Player.freeze] = data.freeze
        it[Player.hud] = data.hud
        it[Player.tpp] = data.tpp
        it[Player.tppTeam] = data.tppTeam
        it[Player.log] = data.log
        it[Player.oldUUID] = data.oldUUID
        it[Player.banTime] = data.banTime
        it[Player.duplicateName] = data.duplicateName
        it[Player.tracking] = data.tracking
        it[Player.joinStacks] = data.joinStacks
        it[Player.lastLoginDate] = if (data.lastLoginDate == null) null else data.lastLoginDate!!.format(DateTimeFormatter.ISO_LOCAL_DATE)
        it[Player.lastLeaveDate] = if (data.lastLeaveDate == null) null else data.lastLeaveDate!!.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        it[Player.showLevelEffects] = data.showLevelEffects
        it[Player.currentPlayTime] = data.currentPlayTime
        it[Player.isConnected] = data.isConnected
        it[Player.lastPlayedWorldName] = data.lastPlayedWorldName
        it[Player.lastPlayedWorldMode] = data.lastPlayedWorldMode
        it[Player.lastPlayedWorldId] = data.lastPlayedWorldId
        it[Player.mvpTime] = data.mvpTime
        it[Player.pvpEliminationTeamCount] = data.pvpEliminationTeamCount
        it[Player.strict] = data.strict

        val json = JsonObject()
        data.status.forEach { entry ->
            json.add(entry.key, entry.value)
        }
        it[Player.status] = json.toString()

        return it
    }

    fun addBan(info: PlayerInfo) {
        transaction {
            Banned.insert {
                it[type] = 0
                it[data] = info.id
            }
            info.ips.forEach { ip ->
                Banned.insert {
                    it[type] = 1
                    it[data] = ip
                }
            }
        }
    }

    fun removeBan(info: PlayerInfo) {
        val ips = info.ips
        transaction {
            Banned.deleteWhere { type eq 0 and (data eq info.id) }
            for (ip in ips) {
                Banned.deleteWhere { type eq 1 and (data eq ip) }
            }
        }
    }

    fun isBanned(info: PlayerInfo) : Boolean {
        transaction {
            return@transaction Banned.selectAll().where { Banned.type eq 0 and (Banned.data eq info.id) or (Banned.type eq 1 and (Banned.data eq info.lastIP)) }.fetchSize != 0
        }
        return false
    }

    fun close() {
        TransactionManager.closeAndUnregister(db)
    }
}