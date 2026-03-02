package essential.common.database

import arc.util.Log
import essential.common.DATABASE_VERSION
import essential.common.bundle
import essential.common.database.data.getPluginData
import essential.common.database.data.update
import essential.common.database.table.*
import essential.common.rootPath
import essential.core.Main
import io.asyncer.r2dbc.mysql.MySqlConnectionConfiguration
import io.asyncer.r2dbc.mysql.MySqlConnectionFactory
import io.r2dbc.h2.H2ConnectionConfiguration
import io.r2dbc.h2.H2ConnectionFactory
import io.r2dbc.h2.H2ConnectionOption
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ValidationDepth
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.vendors.*
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.mariadb.r2dbc.MariadbConnectionConfiguration
import org.mariadb.r2dbc.MariadbConnectionFactory
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.time.Duration

var worldHistoryDatabase: R2dbcDatabase? = null
var defaultDatabase: R2dbcDatabase? = null

/** Initial database setup */
suspend fun databaseInit(r2dbcUrl: String, user: String, pass: String) {
    // Parse database URL to determine the database type
    val databaseType = when {
        r2dbcUrl.startsWith("h2:") -> "h2"
        r2dbcUrl.startsWith("postgresql:") -> "postgresql"
        r2dbcUrl.startsWith("mysql:") -> "mysql"
        r2dbcUrl.startsWith("mariadb:") -> "mariadb"
        else -> "h2" // Default to H2
    }

    val h2History = h2("worldHistory")

    worldHistoryDatabase = connectDatabase(h2History.first, h2History.second)

    rootPath.child("data").mkdirs()

    worldHistoryDatabase?.let { db ->
        suspendTransaction(db = db) {
            SchemaUtils.create(WorldHistoryTable)
        }
    }

    val (connectionFactory, dialect) = when (databaseType) {
        "postgresql" -> postgresql(r2dbcUrl, user, pass)
        "mysql" -> mysql(r2dbcUrl, user, pass)
        "mariadb" -> mariadb(r2dbcUrl, user, pass)
        else -> h2("database")
    }

    val poolConfig = ConnectionPoolConfiguration.builder(connectionFactory)
        .maxSize(2)
        .initialSize(1)
        .maxIdleTime(Duration.ofMinutes(10))
        .maxAcquireTime(Duration.ofSeconds(10))
        .maxCreateConnectionTime(Duration.ofSeconds(1))
        .maxLifeTime(Duration.ofMinutes(5))
        .validationQuery("SELECT 1")
        .validationDepth(ValidationDepth.REMOTE)
        .build()

    defaultDatabase = connectDatabase(ConnectionPool(poolConfig), dialect)

    TransactionManager.defaultDatabase = defaultDatabase!!

    upgradeDatabase()

    suspendTransaction {
        val tablesToCreate = listOf(
            PlayerTable,
            PluginTable,
            PlayerBannedTable,
            AchievementTable,
            MapRatingTable,
            ServerRoutingTable
        )

        SchemaUtils.create(*tablesToCreate.toTypedArray())
    }
}

/**
 * Migration for player achievements from status column to player_achievements table
 */
private suspend fun migrateStatusToAchievements() {
    val playerTable = object : org.jetbrains.exposed.v1.core.Table("players") {
        val id = uinteger("id")
        val status = text("status")
    }

    val achievementTable = object : org.jetbrains.exposed.v1.core.Table("player_achievements") {
        val id = uinteger("id").autoIncrement()
        val playerId = uinteger("player_id")
        val achievementName = varchar("achievement_name", 100)
        val completedAt = datetime("completed_at")
        override val primaryKey = PrimaryKey(id)
    }

    try {
        suspendTransaction {
            playerTable.selectAll().map { row ->
                row[playerTable.id] to row[playerTable.status]
            }.toList().forEach { (playerId, statusStr) ->
                if (statusStr.isNotEmpty() && statusStr != "{}") {
                    try {
                        val json = Json.parseToJsonElement(statusStr).jsonObject
                        val achievements = json.filter { it.key.startsWith("achievement.") }

                        if (achievements.isNotEmpty()) {
                            achievements.forEach { (key, value) ->
                                val name = key.removePrefix("achievement.")
                                val dateStr = value.jsonPrimitive.content

                                val exists = achievementTable.selectAll()
                                    .where { (achievementTable.playerId eq playerId) and (achievementTable.achievementName eq name) }
                                    .firstOrNull() != null

                                if (!exists) {
                                    achievementTable.insert {
                                        it[achievementTable.playerId] = playerId
                                        it[achievementTable.achievementName] = name
                                        try {
                                            it[achievementTable.completedAt] = kotlinx.datetime.LocalDateTime.parse(dateStr)
                                        } catch (_: Exception) {
                                            // Keep default (CurrentDateTime)
                                        }
                                    }
                                }
                            }

                            val newStatus = json.filter { !it.key.startsWith("achievement.") }
                            playerTable.update({ playerTable.id eq playerId }) {
                                it[playerTable.status] = JsonObject(newStatus).toString()
                            }
                        }
                    } catch (e: Exception) {
                        Log.warn("Failed to parse status for player $playerId: ${e.message}")
                    }
                }
            }
        }
    } catch (e: Exception) {
        Log.err("Achievement migration failed: ${e.message}")
        e.printStackTrace()
    }
}

private fun connectDatabase(factory: ConnectionFactory, dialect: DatabaseDialect): R2dbcDatabase {
    return R2dbcDatabase.connect(
        connectionFactory = factory,
        databaseConfig = R2dbcDatabaseConfig {
            defaultMaxAttempts = 1
            defaultMinRetryDelay = 0
            defaultMaxRetryDelay = 0
            explicitDialect = dialect
        }
    )
}

private suspend fun updatePluginVersion(version: UByte) {
    val data = getPluginData()
    if (data != null) {
        data.databaseVersion = version
        data.update()
    }
}

private suspend fun upgradeDatabase() {
    try {
        var currentVersion: UByte? = null

        val candidates = listOf(
            Paths.get("config/mods/Essentials/data/database.mv.db").toFile(),
            rootPath.child("data/database.mv.db").file(),
        )
        val found = candidates.firstOrNull { it.exists() }
        currentVersion = try {
            getPluginData()?.databaseVersion
        } catch (_: Throwable) {
            val dbExists = try {
                suspendTransaction {
                    exec("SELECT 1 FROM db LIMIT 1")
                    true
                }
            } catch (_: Throwable) {
                false
            }
            if (found != null || dbExists) 3u else null
        }

        if (currentVersion == null) {
            Log.info(bundle["database.upgrade.upToDate", DATABASE_VERSION])
            return
        }

        if (currentVersion < DATABASE_VERSION) {
            Log.info(bundle["database.upgrade.start", currentVersion, DATABASE_VERSION])

            for (v in (currentVersion.toUInt() + 1u)..DATABASE_VERSION.toUInt()) {
                val version = v.toUByte()
                val dialectSuffix = when (defaultDatabase!!.config.explicitDialect) {
                    is H2Dialect -> "_h2"
                    is PostgreSQLDialect -> "_postgres"
                    is MariaDBDialect -> "_mariadb"
                    is MysqlDialect -> "_mysql"
                    else -> ""
                }
                val sqlFiles = listOf("v${version}${dialectSuffix}.sql", "v${version}_h2.sql", "v${version}.sql")

                for (sqlFile in sqlFiles) {
                    val inputStream = Main::class.java.classLoader.getResourceAsStream("sql/$sqlFile")
                    if (inputStream != null) {
                        try {
                            val sqlScript = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                            Log.info(bundle["database.upgrade.execute", sqlFile])

                             suspendTransaction {
                                 sqlScript.split(";").map { it.trim() }.filter { it.isNotEmpty() }.forEach { statement ->
                                     try {
                                         exec(statement)
                                     } catch (e: Throwable) {
                                         val isCritical = statement.contains("plugin_data", true) ||
                                             statement.contains("players", true)
                                         if (isCritical) {
                                             throw IllegalStateException("Critical statement failed: $statement", e)
                                         }
                                         Log.warn("Failed to execute statement: $statement. Reason: ${e.message}")
                                     }
                                 }

                                 updatePluginVersion(version)
                                 
                                 if (version == 4u.toUByte()) {
                                     migrateStatusToAchievements()
                                 }
                             }
                             break
                         } catch (e: Throwable) {
                            e.printStackTrace()
                            throw e
                        }
                    }
                }
            }

            // Final version update
            updatePluginVersion(DATABASE_VERSION)

            Log.info(bundle["database.upgrade.end"])
        } else {
            Log.info(bundle["database.upgrade.upToDate", DATABASE_VERSION])
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun parseR2dbcUrl(r2dbcUrl: String, prefix: String, defaultPort: String): Triple<String, Int, String> {
    val url = r2dbcUrl.removePrefix(prefix)
    val host = url.substringBefore(":").substringBefore("/")
    val port = url.substringAfter(":", defaultPort).substringBefore("/")
    val database = url.substringAfter("/")
    return Triple(host, port.toInt(), database)
}

fun postgresql(r2dbcUrl: String, user: String, pass: String): Pair<ConnectionFactory, DatabaseDialect> {
    val (host, port, database) = parseR2dbcUrl(r2dbcUrl, "postgresql://", "5432")

    return Pair(
        PostgresqlConnectionFactory(
            PostgresqlConnectionConfiguration.builder()
                .host(host)
                .port(port)
                .database(database)
                .username(user)
                .password(pass)
                .schema("public")
                .build()
        ), PostgreSQLDialect()
    )
}

fun h2(name: String): Pair<ConnectionFactory, DatabaseDialect> = Pair(
    H2ConnectionFactory(
        H2ConnectionConfiguration.builder()
            .url("./config/mods/Essentials/data/$name")
            .property(H2ConnectionOption.DB_CLOSE_DELAY, "-1")
            .property(H2ConnectionOption.DB_CLOSE_ON_EXIT, "FALSE")
            .property("DEFAULT_NULL_ORDERING", "HIGH")
            .username("sa")
            .password("123")
            .build()
    ),
    H2Dialect()
)

fun mysql(r2dbcUrl: String, user: String, pass: String): Pair<ConnectionFactory, DatabaseDialect> {
    val (host, port, database) = parseR2dbcUrl(r2dbcUrl, "mysql://", "3306")

    return Pair(
        MySqlConnectionFactory.from(
            MySqlConnectionConfiguration.builder()
                .host(host)
                .port(port)
                .database(database)
                .username(user)
                .password(pass)
                .build()
        ),
        MysqlDialect()
    )
}

fun mariadb(r2dbcUrl: String, user: String, pass: String): Pair<ConnectionFactory, DatabaseDialect> {
    val (host, port, database) = parseR2dbcUrl(r2dbcUrl, "mariadb://", "3306")

    return Pair(
        MariadbConnectionFactory.from(
            MariadbConnectionConfiguration.builder()
                .host(host)
                .port(port)
                .database(database)
                .username(user)
                .password(pass)
                .build()
        ),
        MariaDBDialect()
    )
}
