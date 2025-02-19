package essential.core

import arc.Core
import arc.files.Fi
import arc.util.Log
import essential.core.Main.Companion.conf
import essential.core.Main.Companion.daemon
import essential.core.Main.Companion.pluginData
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
                        .getDeclaredConstructor().newInstance() as Driver
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
                        "jdbc:${conf.plugin.database.url}", conf.plugin.database.username, conf.plugin.database.password
                    )
                })
            } else {
                connectDefaultDatabase()
            }
        }
    }

    fun createTable() {
        transaction {
            SchemaUtils.create(ServiceTable, PlayerTable, BannedTable, inBatch = true)
        }
    }

    fun updateDatabase() {
        transaction {
            if (pluginData.databaseVersion == 3) {
                // Some linux DBMS doesn't support uppercase column name by default
                // example) postgresql
                val sql = """
                ALTER TABLE player RENAME TO player_data;
                ALTER TABLE data RENAME TO service_data;
                ALTER TABLE banned RENAME TO banned_data;
                
                ALTER TABLE player_data RENAME COLUMN languageTag to language_tag;
                ALTER TABLE player_data RENAME COLUMN blockPlaceCount to block_place_count;
                ALTER TABLE player_data RENAME COLUMN blockBreakCount to block_break_count;
                ALTER TABLE player_data RENAME COLUMN totalJoinCount to total_join_count;
                ALTER TABLE player_data RENAME COLUMN firstPlayDate to first_play_date;
                ALTER TABLE player_data RENAME COLUMN lastLoginTime to last_login_time;
                ALTER TABLE player_data RENAME COLUMN totalPlayTime to total_play_time;
                ALTER TABLE player_data RENAME COLUMN attackModeClear to attack_mode_clear;
                ALTER TABLE player_data RENAME COLUMN pvpVictoriesCount to pvp_victories_count;
                ALTER TABLE player_data RENAME COLUMN pvpDefeatCount to pvp_defeat_count;
                ALTER TABLE player_data RENAME COLUMN animatedName to animated_name;
                ALTER TABLE player_data RENAME COLUMN accountID to account_id;
                ALTER TABLE player_data RENAME COLUMN accountPW to account_id;
                ALTER TABLE player_data RENAME COLUMN effectLevel to effect_level;
                ALTER TABLE player_data RENAME COLUMN effectColor to effect_color;
                ALTER TABLE player_data RENAME COLUMN hideRanking to hide_ranking;
                ALTER TABLE player_data RENAME COLUMN tppTeam to tpp_team;
                ALTER TABLE player_data RENAME COLUMN oldUUID to old_uuid;
                ALTER TABLE player_data RENAME COLUMN banTime to ban_time;
                ALTER TABLE player_data RENAME COLUMN duplicateName to duplicate_name;
                ALTER TABLE player_data RENAME COLUMN joinStacks to join_stacks;
                ALTER TABLE player_data RENAME COLUMN lastLoginDate to last_login_date;
                ALTER TABLE player_data RENAME COLUMN lastLeaveDate to last_leave_date;
                ALTER TABLE player_data RENAME COLUMN showLevelEffects to show_level_effects;
                ALTER TABLE player_data RENAME COLUMN currentPlayTime to current_play_time;
                ALTER TABLE player_data RENAME COLUMN isConnected to is_connected;
                ALTER TABLE player_data RENAME COLUMN lastPlayedWorldName to last_played_world_name;
                ALTER TABLE player_data RENAME COLUMN lastPlayedWorldMode to last_played_world_mode;
                ALTER TABLE player_data RENAME COLUMN lastPlayedWorldId to last_played_world_id;
                ALTER TABLE player_data RENAME COLUMN mvpTime to mvp_time;
                ALTER TABLE player_data RENAME COLUMN pvpEliminationTeamCount to pvp_elimination_team_count;
                
                DROP TABLE db;
            """.trimIndent()
                exec(sql)
            }
        }
    }

    object BannedTable : Table("banned_data") {
        val type = integer("type")
        val data = text("data")
    }

    object ServiceTable : Table("service_data") {
        val data = text("data")
    }

    object PlayerTable : Table("player_data") {
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

        fun bundle(): Bundle {
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
            PlayerTable.insert {
                convertToQueue(it, data)
            }
        }
    }

    operator fun get(uuid: String): PlayerData? {
        return transaction {
            val data = PlayerTable.selectAll().where { PlayerTable.uuid.eq(uuid) }.firstOrNull()
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
            PlayerTable.selectAll().forEach {
                d.add(convertToData(it))
            }
        }
        return d
    }

    fun getAllByExp(): Array<PlayerData> {
        val d = ArrayList<PlayerData>()

        transaction {
            PlayerTable.selectAll().orderBy(PlayerTable.exp, SortOrder.DESC).forEach {
                d.add(convertToData(it))
            }
        }
        return d.toTypedArray()
    }

    fun getByDiscord(discord: String): PlayerData? {
        return transaction {
            val data = PlayerTable.selectAll().where { PlayerTable.discord eq discord }.firstOrNull()
            if (data != null) {
                convertToData(data)
            } else {
                null
            }
        }
    }

    fun getByName(name: String): PlayerData? {
        return transaction {
            val data = PlayerTable.selectAll().where { PlayerTable.name eq name }.firstOrNull()
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
            PlayerTable.update({ PlayerTable.uuid eq id }) {
                convertToQueue(it, data)
            }
        }
    }

    fun search(id: String, pw: String): PlayerData? {
        return transaction { PlayerTable.selectAll().where { PlayerTable.accountID eq id }.firstOrNull() }.run {
            if (this != null) {
                val data = convertToData(this)
                if (data.accountID == data.accountPW) data else if (BCrypt.checkpw(pw, data.accountPW)) data else null
            } else {
                null
            }
        }
    }

    fun convertToData(it: ResultRow): PlayerData {
        val data = PlayerData()
        data.name = it[PlayerTable.name]
        data.uuid = it[PlayerTable.uuid]
        data.languageTag = it[PlayerTable.languageTag]
        data.blockPlaceCount = it[PlayerTable.blockPlaceCount]
        data.blockBreakCount = it[PlayerTable.blockBreakCount]
        data.totalJoinCount = it[PlayerTable.totalJoinCount]
        data.totalKickCount = it[PlayerTable.totalKickCount]
        data.level = it[PlayerTable.level]
        data.exp = it[PlayerTable.exp]
        data.firstPlayDate = it[PlayerTable.firstPlayDate]
        data.lastLoginTime = it[PlayerTable.lastLoginTime]
        data.totalPlayTime = it[PlayerTable.totalPlayTime]
        data.attackModeClear = it[PlayerTable.attackModeClear]
        data.pvpVictoriesCount = it[PlayerTable.pvpVictoriesCount]
        data.pvpDefeatCount = it[PlayerTable.pvpDefeatCount]
        data.animatedName = it[PlayerTable.animatedName]
        data.permission = it[PlayerTable.permission]
        data.mute = it[PlayerTable.mute]
        data.accountID = it[PlayerTable.accountID]
        data.accountPW = it[PlayerTable.accountPW]
        data.discord = it[PlayerTable.discord]
        data.effectLevel = it[PlayerTable.effectLevel]
        data.effectColor = it[PlayerTable.effectColor]
        data.hideRanking = it[PlayerTable.hideRanking]
        data.freeze = it[PlayerTable.freeze]
        data.hud = it[PlayerTable.hud]
        data.tpp = it[PlayerTable.tpp]
        data.tppTeam = it[PlayerTable.tppTeam]
        data.log = it[PlayerTable.log]
        data.oldUUID = it[PlayerTable.oldUUID]
        data.banTime = it[PlayerTable.banTime]
        data.duplicateName = it[PlayerTable.duplicateName]
        data.tracking = it[PlayerTable.tracking]
        data.joinStacks = it[PlayerTable.joinStacks]
        data.lastLoginDate = if (it[PlayerTable.lastLoginDate] == null) null else LocalDate.parse(
            it[PlayerTable.lastLoginDate],
            DateTimeFormatter.ISO_LOCAL_DATE
        )
        data.lastLeaveDate = if (it[PlayerTable.lastLeaveDate] == null) null else LocalDateTime.parse(
            it[PlayerTable.lastLeaveDate],
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
        )
        data.showLevelEffects = it[PlayerTable.showLevelEffects]
        data.currentPlayTime = it[PlayerTable.currentPlayTime]
        data.isConnected = it[PlayerTable.isConnected]
        data.lastPlayedWorldName = it[PlayerTable.lastPlayedWorldName]
        data.lastPlayedWorldMode = it[PlayerTable.lastPlayedWorldMode]
        data.lastPlayedWorldId = it[PlayerTable.lastPlayedWorldId]
        data.mvpTime = it[PlayerTable.mvpTime]
        data.pvpEliminationTeamCount = it[PlayerTable.pvpEliminationTeamCount]
        data.strict = it[PlayerTable.strict]

        val obj = HashMap<String, String>()
        JsonObject.readHjson(it[PlayerTable.status]).asObject().forEach { member ->
            obj[member.name] = member.value.asString()
        }
        data.status = obj
        return data
    }

    fun convertToQueue(it: UpdateBuilder<*>, data: PlayerData): UpdateBuilder<*> {
        it[PlayerTable.name] = data.name
        it[PlayerTable.uuid] = data.uuid
        it[PlayerTable.languageTag] = data.languageTag
        it[PlayerTable.blockPlaceCount] = data.blockPlaceCount
        it[PlayerTable.blockBreakCount] = data.blockBreakCount
        it[PlayerTable.totalJoinCount] = data.totalJoinCount
        it[PlayerTable.totalKickCount] = data.totalKickCount
        it[PlayerTable.level] = data.level
        it[PlayerTable.exp] = data.exp
        it[PlayerTable.firstPlayDate] = data.firstPlayDate
        it[PlayerTable.lastLoginTime] = data.lastLoginTime
        it[PlayerTable.totalPlayTime] = data.totalPlayTime
        it[PlayerTable.attackModeClear] = data.attackModeClear
        it[PlayerTable.pvpVictoriesCount] = data.pvpVictoriesCount
        it[PlayerTable.pvpDefeatCount] = data.pvpDefeatCount
        it[PlayerTable.animatedName] = data.animatedName
        it[PlayerTable.permission] = data.permission
        it[PlayerTable.mute] = data.mute
        it[PlayerTable.accountID] = data.accountID
        it[PlayerTable.accountPW] = data.accountPW
        it[PlayerTable.discord] = data.discord
        it[PlayerTable.effectLevel] = data.effectLevel
        it[PlayerTable.effectColor] = data.effectColor
        it[PlayerTable.hideRanking] = data.hideRanking
        it[PlayerTable.freeze] = data.freeze
        it[PlayerTable.hud] = data.hud
        it[PlayerTable.tpp] = data.tpp
        it[PlayerTable.tppTeam] = data.tppTeam
        it[PlayerTable.log] = data.log
        it[PlayerTable.oldUUID] = data.oldUUID
        it[PlayerTable.banTime] = data.banTime
        it[PlayerTable.duplicateName] = data.duplicateName
        it[PlayerTable.tracking] = data.tracking
        it[PlayerTable.joinStacks] = data.joinStacks
        it[PlayerTable.lastLoginDate] =
            if (data.lastLoginDate == null) null else data.lastLoginDate!!.format(DateTimeFormatter.ISO_LOCAL_DATE)
        it[PlayerTable.lastLeaveDate] =
            if (data.lastLeaveDate == null) null else data.lastLeaveDate!!.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        it[PlayerTable.showLevelEffects] = data.showLevelEffects
        it[PlayerTable.currentPlayTime] = data.currentPlayTime
        it[PlayerTable.isConnected] = data.isConnected
        it[PlayerTable.lastPlayedWorldName] = data.lastPlayedWorldName
        it[PlayerTable.lastPlayedWorldMode] = data.lastPlayedWorldMode
        it[PlayerTable.lastPlayedWorldId] = data.lastPlayedWorldId
        it[PlayerTable.mvpTime] = data.mvpTime
        it[PlayerTable.pvpEliminationTeamCount] = data.pvpEliminationTeamCount
        it[PlayerTable.strict] = data.strict

        val json = JsonObject()
        data.status.forEach { entry ->
            json.add(entry.key, entry.value)
        }
        it[PlayerTable.status] = json.toString()

        return it
    }

    fun addBan(info: PlayerInfo) {
        transaction {
            BannedTable.insert {
                it[type] = 0
                it[data] = info.id
            }
            info.ips.forEach { ip ->
                BannedTable.insert {
                    it[type] = 1
                    it[data] = ip
                }
            }
        }
    }

    fun removeBan(info: PlayerInfo) {
        val ips = info.ips
        transaction {
            BannedTable.deleteWhere { type eq 0 and (data eq info.id) }
            for (ip in ips) {
                BannedTable.deleteWhere { type eq 1 and (data eq ip) }
            }
        }
    }

    fun isBanned(info: PlayerInfo): Boolean {
        transaction {
            return@transaction BannedTable.selectAll()
                .where { BannedTable.type eq 0 and (BannedTable.data eq info.id) or (BannedTable.type eq 1 and (BannedTable.data eq info.lastIP)) }.fetchSize != 0
        }
        return false
    }

    fun close() {
        TransactionManager.closeAndUnregister(db)
    }
}