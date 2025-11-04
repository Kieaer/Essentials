package essential.common.database

import arc.util.Log
import essential.common.DATABASE_VERSION
import essential.common.bundle
import essential.common.database.data.getPluginData
import essential.common.database.table.*
import essential.common.rootPath
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
import org.jetbrains.exposed.v1.core.Schema
import org.jetbrains.exposed.v1.core.vendors.*
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.mariadb.r2dbc.MariadbConnectionConfiguration
import org.mariadb.r2dbc.MariadbConnectionFactory
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.time.Duration

var worldHistoryDatabase: R2dbcDatabase? = null
var defaultDatabase: R2dbcDatabase? = null

/** Initial database setup */
suspend fun databaseInit(r2dbcUrl: String, user: String, pass: String) {
    val h2 = h2("worldHistory")

    rootPath.mkdirs()
    val dataDir = rootPath.child("data")
    if (!dataDir.exists()) dataDir.mkdirs()

    worldHistoryDatabase = R2dbcDatabase.connect(
        connectionFactory = h2.first,
        databaseConfig = R2dbcDatabaseConfig {
            defaultMaxAttempts = 1
            defaultMinRetryDelay = 0
            defaultMaxRetryDelay = 0
            explicitDialect = h2.second
        }
    )

    worldHistoryDatabase?.let { db ->
        suspendTransaction(db = db) {
            SchemaUtils.createSchema(Schema("public"))
            SchemaUtils.create(WorldHistoryTable)
        }
    }


    val selected = h2("main")

    val poolConfig = ConnectionPoolConfiguration.builder(selected.first)
        .maxSize(2)
        .initialSize(1)
        .maxIdleTime(Duration.ofMinutes(10))
        .maxAcquireTime(Duration.ofSeconds(10))
        .maxCreateConnectionTime(Duration.ofSeconds(1))
        .maxLifeTime(Duration.ofMinutes(5))
        .validationQuery("SELECT 1")
        .validationDepth(ValidationDepth.REMOTE)
        .build()

    defaultDatabase = R2dbcDatabase.connect(
        connectionFactory = ConnectionPool(poolConfig),
        databaseConfig = R2dbcDatabaseConfig {
            defaultMaxAttempts = 1
            defaultMinRetryDelay = 0
            defaultMaxRetryDelay = 0
            explicitDialect = selected.second
        }
    )

    TransactionManager.defaultDatabase = defaultDatabase!!

    suspendTransaction {
        SchemaUtils.create(
            PlayerTable,
            PluginTable,
            PlayerBannedTable,
            AchievementTable,
            MapRatingTable,
            ServerRoutingTable
        )
    }

    upgradeDatabase()
}

/**
 * Update database schema when necessary.
 * Compares the current database version with the plugin's database version and runs SQL scripts.
 */
private suspend fun upgradeDatabase() {
    val pluginData = getPluginData()
    val currentVersion = pluginData?.databaseVersion ?: DATABASE_VERSION

    if (currentVersion < DATABASE_VERSION) {
        Log.info(bundle["database.upgrade.start", currentVersion, DATABASE_VERSION])

        for (version in (currentVersion.toUInt() + 1u).toUByte()..DATABASE_VERSION) {
            val fileName = "v${version}.sql"
            val sqlFilePath = "sql/$fileName"
            val inputStream: InputStream? = Thread.currentThread().contextClassLoader.getResourceAsStream(sqlFilePath)

            if (inputStream != null) {
                val sqlScript = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                Log.info(bundle["database.upgrade.execute", fileName])

                suspendTransaction {
                    sqlScript.split(";").filter { it.trim().isNotEmpty() }.forEach { statement ->
                        exec(statement)
                    }
                }
            } else {
                Log.warn(bundle["database.upgrade.notFound", fileName])
            }
        }

        Log.info(bundle["database.upgrade.end"])
    } else {
        Log.info(bundle["database.upgrade.upToDate", DATABASE_VERSION])
    }
}

fun postgresql(): Pair<ConnectionFactory, DatabaseDialect> =
    Pair(
        PostgresqlConnectionFactory(
            PostgresqlConnectionConfiguration.builder()
                .host("localhost")
                .port(5432)
                .database("essential")
                .username("postgres")
                .password("postgres")
                .schema("public")
                .build()
        ), PostgreSQLDialect()
    )


fun h2(name: String): Pair<ConnectionFactory, DatabaseDialect> = Pair(
    H2ConnectionFactory(
        H2ConnectionConfiguration.builder()
            .url("./config/mods/Essentials/data/$name")
            .property(H2ConnectionOption.DB_CLOSE_DELAY, "-1")
            .property(H2ConnectionOption.DB_CLOSE_ON_EXIT, "FALSE")
            .property(H2ConnectionOption.MODE, "PostgreSQL")
            .property("DATABASE_TO_LOWER", "TRUE")
            .property("DEFAULT_NULL_ORDERING", "HIGH")
            .build()
    ),
    H2Dialect()
)

fun mysql(): Pair<ConnectionFactory, DatabaseDialect> = Pair(
    MySqlConnectionFactory.from(
        MySqlConnectionConfiguration.builder()
            .host("localhost")
            .port(3306)
            .database("essential")
            .username("root")
            .password("1234")
            .build()
    ),
    MysqlDialect()
)

fun mariadb(): Pair<ConnectionFactory, DatabaseDialect> = Pair(
    MariadbConnectionFactory.from(
        MariadbConnectionConfiguration.builder()
            .host("localhost")
            .port(3306)
            .database("essential")
            .username("root")
            .password("1234")
            .build()
    ),
    MariaDBDialect()
)