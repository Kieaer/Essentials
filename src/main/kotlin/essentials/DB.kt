package essentials

import arc.struct.ObjectMap
import arc.struct.Seq
import arc.util.Log
import mindustry.gen.Playerc
import org.hjson.JsonObject
import org.hjson.Stringify
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import org.postgresql.util.PSQLException
import java.sql.DriverManager
import java.sql.SQLException
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.system.exitProcess


class DB {
    val players : Seq<PlayerData> = Seq()
    lateinit var db : Database
    var dbVersion = 3


    fun open() {
        var migrateData = Seq<PlayerData>()

        try {
            if (Main.root.child("database.mv.db").exists()) {
                val new = "postgresql://127.0.0.1:5432/essentials"
                try {
                    DriverManager.getConnection("jdbc:h2:${Config.database}", "sa", "").use { conn ->
                        conn.createStatement().use { stmt ->
                            stmt.execute("ALTER TABLE PLAYER ADD strict bool default false")
                            stmt.executeUpdate("UPDATE player SET \"lastLoginDate\" = NULL WHERE \"lastLoginDate\" = 'null'")
                            stmt.executeUpdate("UPDATE player SET \"lastLeaveDate\" = NULL WHERE \"lastLeaveDate\" = 'null'")
                            stmt.executeUpdate("UPDATE player SET \"duplicateName\" = NULL WHERE \"duplicateName\" = 'null'")
                            stmt.executeUpdate("UPDATE player SET status='{}'")

                            val old = Database.connect("jdbc:h2:${Config.database}", "org.h2.Driver", "sa", "")
                            migrateData = getAll()
                            TransactionManager.closeAndUnregister(old)
                            Config.database = new
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            try {
                db = Database.connect(
                    "jdbc:${Config.database}",
                    "org.postgresql.Driver",
                    Config.databaseID,
                    Config.databasePW
                )
                transaction { connection.isClosed }
            } catch (e: PSQLException) {
                TransactionManager.closeAndUnregister(db)
                try {
                    DriverManager.getConnection("jdbc:${Config.database.replace("essentials","")}", Config.databaseID, Config.databasePW).use { conn ->
                        conn.createStatement().use { stmt ->
                            val sql = "CREATE DATABASE essentials"
                            stmt.executeUpdate(sql)
                        }
                    }
                } catch (e: SQLException) {
                    e.printStackTrace()
                }

                db = Database.connect(
                    "jdbc:${Config.database}",
                    "org.postgresql.Driver",
                    Config.databaseID,
                    Config.databasePW
                )
            }

            transaction {
                if (!connection.isClosed) {
                    SchemaUtils.create(Player)
                    SchemaUtils.create(Data)
                    SchemaUtils.create(DB)

                    fun upgrade() {
                        when (DB.selectAll().first()[DB.version]) {
                            2 -> {
                                val total = migrateData.size
                                for ((progress, data) in migrateData.withIndex()) {
                                    createData(data)
                                    print("\r$progress/$total")
                                }
                                println()
                                DB.update {
                                    it[version] = 3
                                }
                            }

                            else -> listOf("")
                        }
                    }

                    var isUpgraded = false
                    if (DB.selectAll().empty() && migrateData.isEmpty) {
                        DB.insert {
                            it[version] = dbVersion
                        }
                    } else {
                        if(!migrateData.isEmpty) {
                            DB.insert {
                                it[version] = 2
                            }
                        }

                        while (DB.selectAll().first()[DB.version] != dbVersion) {
                            isUpgraded = true
                            upgrade()
                        }
                    }

                    if (isUpgraded) {
                        Log.info(Bundle()["event.plugin.db.version", dbVersion])
                        Log.warn(Bundle()["event.plugin.db.warning"])
                    }

                    if (!Data.selectAll().empty() && JsonObject.readJSON(String(Base64.getDecoder().decode(Data.selectAll().first()[Data.data]))).asObject().getBoolean("isDuplicateNameChecked", false)) {
                        val duplicate = Player.slice(Player.name, Player.duplicateName).selectAll().groupBy(Player.name).having { Player.name.count() greater 1 }.map { it[Player.name] }
                        for (value in duplicate) {
                            Player.update({ Player.name eq value }) {
                                it[duplicateName] = value
                            }
                        }
                        PluginData.save(true)
                    }
                } else {
                    Log.err(Bundle()["event.plugin.db.wrong"])
                    exitProcess(1)
                }
            }
        } catch (e : PSQLException) {
            if (Config.databasePW.isEmpty()) {
                Log.warn(Bundle()["event.plugin.db.account.empty"])
            } else {
                e.printStackTrace()
                Log.err(Bundle()["event.plugin.db.account.wrong"])
            }
            exitProcess(1)
        } catch (e : Exception) {
            e.printStackTrace()
            exitProcess(1)
        }
    }

    object Data : Table("public.data") {
        val data = text("data")
    }

    object DB : Table("public.db") {
        val version = integer("version")
    }

    object Player : Table("public.player") {
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
        var effectLevel : Int? = -1
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
        var strict : Boolean = false

        var expMultiplier : Double = 1.0
        var currentExp : Int = 0

        var afkTime : Int = 0
        var afk : Boolean = false
        var previousMousePosition : Float = 0F
        var player : Playerc = mindustry.gen.Player.create()
        var entityid : Int = 0

        // Use plugin test only
        var lastSentMessage : String = ""
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
            data.strict = it[Player.strict]

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
                data.strict = it[Player.strict]

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
                it[strict] = data.strict

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
                data.strict = this[Player.strict]

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
        TransactionManager.closeAndUnregister(db)
    }
}