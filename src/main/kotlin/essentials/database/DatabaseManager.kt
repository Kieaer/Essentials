package essentials.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import essentials.Manager
import essentials.PluginVariables.playerList
import essentials.data.PlayerData
import essentials.data.PlayerTable
import essentials.data.PluginTable
import kotlinx.datetime.toJavaLocalDateTime
import mindustry.gen.Player
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.vendors.SQLiteDialect

class DatabaseManager : Manager {
    override fun initialize() {
        val setting = HikariDataSource(HikariConfig().apply {
            jdbcUrl = "jdbc:sqlite:file:test"
            driverClassName = "org.sqlite.JDBC"

            validate()
        })
        val database = Database.connect(setting)
        transaction {
            if (database.dialect is SQLiteDialect) {
                // SQLite Write Ahead Logging
                exec("PRAGMA journal_mode=WAL")
            }

            SchemaUtils.create(PlayerTable, PluginTable, inBatch = true)
        }
    }

    override fun terminate() {
        TODO("Not yet implemented")
    }

    fun getPlayerData(uuid: String) : List<PlayerData> {
        val data = playerList.filter { it.uuid == uuid }
        return data.ifEmpty {
            transaction {
                PlayerTable.selectAll().where { PlayerTable.uuid eq uuid }.map {
                    PlayerData(
                        name = it[PlayerTable.name],
                        uuid = it[PlayerTable.uuid],
                        uuidList = it[PlayerTable.uuidList],
                        id = it[PlayerTable.id],
                        password = it[PlayerTable.password],
                        blockPlaceCount = it[PlayerTable.blockPlaceCount],
                        blockBreakCount = it[PlayerTable.blockBreakCount],
                        totalJoinCount = it[PlayerTable.totalJoinCount],
                        totalKickCount = it[PlayerTable.totalKickCount],
                        level = it[PlayerTable.level],
                        exp = it[PlayerTable.exp],
                        firstPlayDate = it[PlayerTable.firstPlayDate].toJavaLocalDateTime(),
                        lastLoginDate = it[PlayerTable.lastLoginDate].toJavaLocalDateTime(),
                        lastLeaveDate = it[PlayerTable.lastLeaveDate].toJavaLocalDateTime(),
                        totalPlayTime = it[PlayerTable.totalPlayTime],
                        attackModeClear = it[PlayerTable.attackModeClear],
                        pvpVictories = it[PlayerTable.pvpVictories],
                        pvpDefeats = it[PlayerTable.pvpDefeats],
                        pvpEliminationTeamCount = it[PlayerTable.pvpEliminationTeamCount],
                        hideRanking = it[PlayerTable.hideRanking],
                        joinStacks = it[PlayerTable.joinStacks],
                        strict = it[PlayerTable.strict],
                        isConnected = it[PlayerTable.isConnected],
                        animatedName = it[PlayerTable.animatedName],
                        permission = it[PlayerTable.permission],
                        mute = it[PlayerTable.mute],
                        discord = it[PlayerTable.discord],
                        effectLevel = it[PlayerTable.effectLevel],
                        effectColor = it[PlayerTable.effectColor],
                        freeze = it[PlayerTable.freeze],
                        hud = it[PlayerTable.hud],
                        tpp = it[PlayerTable.tpp],
                        log = it[PlayerTable.log],
                        banTime = it[PlayerTable.banTime].toJavaLocalDateTime(),
                        tracking = it[PlayerTable.tracking],
                        showLevelEffects = it[PlayerTable.showLevelEffects],
                        lastPlayedWorldName = it[PlayerTable.lastPlayedWorldName],
                        lastPlayedWorldMode = it[PlayerTable.lastPlayedWorldMode],
                        mvpTime = it[PlayerTable.mvpTime],
                        currentPlayTime = 0u,
                        afkTime = 0u,
                        afk = false,
                        player = Player.create(),
                        entityId = 0
                    )
                }
            }.toList()
        }
    }
}