package essential.common.database

import arc.util.Log
import essential.common.DATABASE_VERSION
import essential.common.bundle
import essential.common.database.data.getPluginData
import essential.common.database.data.update
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
import java.nio.file.Paths
import java.time.Duration

var worldHistoryDatabase: R2dbcDatabase? = null
var defaultDatabase: R2dbcDatabase? = null
private var upgradeDialectIsH2: Boolean = false

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

    worldHistoryDatabase = R2dbcDatabase.connect(
        connectionFactory = h2History.first,
        databaseConfig = R2dbcDatabaseConfig {
            defaultMaxAttempts = 1
            defaultMinRetryDelay = 0
            defaultMaxRetryDelay = 0
            explicitDialect = h2History.second
        }
    )

    rootPath.child("data").mkdirs()

    worldHistoryDatabase?.let { db ->
        suspendTransaction(db = db) {
            SchemaUtils.createSchema(Schema("public"))
            SchemaUtils.create(WorldHistoryTable)
        }
    }

    val (connectionFactory, dialect) = when (databaseType) {
        "postgresql" -> postgresql()
        "mysql" -> mysql()
        "mariadb" -> mariadb()
        else -> h2("database")
    }

    upgradeDialectIsH2 = dialect is H2Dialect

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

    defaultDatabase = R2dbcDatabase.connect(
        connectionFactory = ConnectionPool(poolConfig),
        databaseConfig = R2dbcDatabaseConfig {
            defaultMaxAttempts = 1
            defaultMinRetryDelay = 0
            defaultMaxRetryDelay = 0
            explicitDialect = dialect
        }
    )

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

        tablesToCreate.forEach { table ->
            try {
                exec("SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME='${table.tableName}'")
            } catch (_: Throwable) {
                SchemaUtils.create(table)
            }
        }
    }
}

private fun migrateSqliteToH2IfNeeded() {
    try {
        val dir = arc.Core.settings.dataDirectory.child("mods/Essentials/data")
        val h2File = dir.child("database.mv.db").file()
        if (h2File.exists()) {
            Log.info("H2 database already present. Skipping SQLite migration.")
            return
        }

        Class.forName("org.sqlite.JDBC")

        // write here
    } catch (_: Throwable) {
    }
}

private suspend fun upgradeDatabase() {
    try {
        migrateSqliteToH2IfNeeded()

        var currentVersion: UByte? = null

        val candidates = listOf(
            Paths.get("config/mods/Essentials/data/database.mv.db").toFile(),
            rootPath.child("data/database.mv.db").file(),
        )
        val found = candidates.firstOrNull { it.exists() }
        if (found != null) {
            currentVersion = 3u
        }

        if (currentVersion == null) {
            currentVersion = getPluginData()?.databaseVersion
        }

        if (currentVersion == null) {
            suspendTransaction {
                exec("SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME='DATA'")
                    ?.let { currentVersion = 3u }
                    ?: exec("SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME='PLAYER'")
                        ?.let { currentVersion = 3u }
                    ?: exec("SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME='BANNED'")
                        ?.let { currentVersion = 3u }
            }
        }

        if (currentVersion == null) {
            Log.info(bundle["database.upgrade.upToDate", DATABASE_VERSION])
            return
        }

        if (currentVersion < DATABASE_VERSION) {
            Log.info(bundle["database.upgrade.start", currentVersion, DATABASE_VERSION])

            for (v in (currentVersion.toUInt() + 1u)..DATABASE_VERSION.toUInt()) {
                val version = v.toUByte()
                val sqlFiles = listOf("v${version}_h2.sql", "v${version}.sql")

                for (sqlFile in sqlFiles) {
                    val inputStream = Thread.currentThread().contextClassLoader.getResourceAsStream("sql/$sqlFile")
                    if (inputStream != null) {
                        try {
                            val sqlScript = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                            Log.info(bundle["database.upgrade.execute", sqlFile])

                            suspendTransaction {
                                exec(sqlScript)
                            }

                            val data = getPluginData()
                            if (data != null) {
                                data.databaseVersion = version
                                data.update()
                            }
                            break
                        } catch (e: Throwable) {
                            e.printStackTrace()
                            continue
                        }
                    }
                }
            }

            // Final version update
            val data = getPluginData()
            if (data != null) {
                data.databaseVersion = DATABASE_VERSION
                data.update()
            }

            Log.info(bundle["database.upgrade.end"])
        } else {
            Log.info(bundle["database.upgrade.upToDate", DATABASE_VERSION])
        }
        } catch (e: Exception) {
            Log.warn("Database upgrade failed: ${e.message}")
            e.printStackTrace()
        }
}

fun postgresql(): Pair<ConnectionFactory, DatabaseDialect> =
    Pair(
        PostgresqlConnectionFactory(
            PostgresqlConnectionConfiguration.builder()
                .host("localhost")
                .port(5432)
                .database("essential")
                .username("plugins")
                .password("plugind")
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
            .property("DEFAULT_NULL_ORDERING", "HIGH")
            .username("sa")
            .password("123")
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