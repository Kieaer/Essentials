package essential.common.database

import arc.util.Log
import com.zaxxer.hikari.HikariDataSource
import essential.common.DATABASE_VERSION
import essential.common.bundle
import essential.common.database.data.getPluginData
import essential.common.database.table.*
import essential.common.rootPath
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URI
import java.net.URL
import java.net.URLClassLoader
import java.nio.channels.Channels
import java.nio.charset.StandardCharsets
import java.sql.*
import java.util.*
import java.util.logging.Logger

// Shim to register JDBC drivers loaded via a custom ClassLoader with DriverManager
private class DriverShim(private val driver: Driver) : Driver {
    override fun connect(url: String?, info: Properties?): Connection? = driver.connect(url, info)
    override fun acceptsURL(url: String?): Boolean = driver.acceptsURL(url)
    override fun getPropertyInfo(url: String?, info: Properties?): Array<DriverPropertyInfo> = driver.getPropertyInfo(url, info)
    override fun getMajorVersion(): Int = driver.majorVersion
    override fun getMinorVersion(): Int = driver.minorVersion
    override fun jdbcCompliant(): Boolean = driver.jdbcCompliant()
    override fun getParentLogger(): Logger = try {
        driver.parentLogger
    } catch (_: SQLFeatureNotSupportedException) {
        Logger.getLogger("global")
    }
}

var datasource = HikariDataSource()

// Dedicated datasource/database for local-only world_history storage
var worldHistoryDatasource: HikariDataSource? = null
var worldHistoryDatabase: Database? = null

// ClassLoaders for dynamically downloaded JDBC drivers
private val driverClassLoaders = mutableMapOf<String, URLClassLoader>()

private fun <T> withDriverClassLoader(dbType: String, block: () -> T): T {
    val cl = driverClassLoaders[dbType]
    if (cl == null) return block()
    val thread = Thread.currentThread()
    val prev = thread.contextClassLoader
    return try {
        thread.contextClassLoader = cl
        block()
    } finally {
        thread.contextClassLoader = prev
    }
}

/** DB 초기 설정 */
fun databaseInit(jdbcUrl: String, user: String, pass: String) {
    val dbType = extractDatabaseType(jdbcUrl)
    loadJdbcDriver(dbType)

    try {
        loadJdbcDriver("sqlite")
        rootPath.mkdirs()
        val dataDir = rootPath.child("data")
        if (!dataDir.exists()) dataDir.mkdirs()
        val sqlitePath = rootPath.child("data/world_history.db").file().absolutePath
        val sqliteDriver = getDriverMap()["sqlite"]?.third ?: "org.sqlite.JDBC"
        withDriverClassLoader("sqlite") {
            worldHistoryDatasource = HikariDataSource().apply {
                this.jdbcUrl = "jdbc:sqlite:$sqlitePath"
                this.driverClassName = sqliteDriver
                this.maximumPoolSize = 1
            }
            worldHistoryDatabase = worldHistoryDatasource?.let { Database.connect(it) }
            worldHistoryDatabase?.let { db ->
                transaction(db) {
                    SchemaUtils.create(WorldHistoryTable)
                }
            }
        }
    } catch (e: Throwable) {
        Log.err("[database] Failed to initialize local SQLite for world_history: @", e)
    }

    val mainDriver = getDriverMap()[dbType]?.third
    if (dbType == "sqlite") ensureSqliteFileParentExists(jdbcUrl)
    withDriverClassLoader(dbType) {
        datasource = HikariDataSource().apply {
            this.jdbcUrl = jdbcUrl
            this.username = user
            this.password = pass
            this.maximumPoolSize = 2
            if (mainDriver != null) this.driverClassName = mainDriver
        }

        Database.connect(datasource)

        transaction {
            SchemaUtils.create(PlayerTable, PluginTable, PlayerBannedTable, AchievementTable, MapRatingTable, ServerRoutingTable)
        }
    }

    upgradeDatabaseBlocking()
}

/** DB 연결 종료 */
fun databaseClose() {
    datasource.close()
    worldHistoryDatasource?.close()
}

private fun extractDatabaseType(jdbcUrl: String): String {
    val pattern = "jdbc:([^:]+):.*".toRegex()
    val matchResult = pattern.find(jdbcUrl)

    return matchResult?.groupValues?.get(1) ?: "sqlite"
}

/** Load the JDBC driver for the specified database type */
private fun loadJdbcDriver(dbType: String) {
    val driverMap = getDriverMap()
    val driverInfo = driverMap[dbType]

    if (driverInfo == null) {
        Log.warn(bundle["database.driver.unknown", dbType])
        return
    }

    val (_, _, driverClass) = driverInfo

    // 1) Try to load with the current classpath
    try {
        Class.forName(driverClass)
        Log.info(bundle["database.driver.loaded", dbType, driverClass])
        return
    } catch (_: ClassNotFoundException) {
        // proceed to dynamic loading
    }

    // 2) Attempt to use a previously created URLClassLoader for this dbType
    driverClassLoaders[dbType]?.let { cl ->
        try {
            Thread.currentThread().contextClassLoader = cl
            Log.info(bundle["database.driver.loaded.after.download", dbType, driverClass])
            return
        } catch (_: Throwable) {
            // we'll re-create it below
        }
    }

    // 3) Ensure the driver jar exists (download if necessary), then create a URLClassLoader and load the class
    val jarFile = rootPath.child("drivers").child("${dbType}-driver.jar").file()
    if (!jarFile.exists()) {
        try {
            downloadSpecificDriver(dbType)
        } catch (e: Exception) {
            Log.err(bundle["database.download.driver.failed", dbType], e)
        }
    }

    if (jarFile.exists()) {
        try {
            val cl = URLClassLoader(arrayOf(jarFile.toURI().toURL()), Database::class.java.classLoader)
            driverClassLoaders[dbType] = cl
            Thread.currentThread().contextClassLoader = cl

            // Ensure the driver class is initialized using the dedicated classloader
            val drvClass = Class.forName(driverClass, true, cl)
            val drvInstance = drvClass.getDeclaredConstructor().newInstance() as Driver

            // Register the driver with DriverManager via a shim to avoid classloader issues
            try {
                DriverManager.registerDriver(DriverShim(drvInstance))
            } catch (_: SQLException) {
                // ignore duplicate registration or similar problems
            }

            Log.info(bundle["database.driver.loaded.after.download", dbType, driverClass])
            return
        } catch (e: Throwable) {
            Log.err(bundle["database.driver.load.failed.after.download", dbType, driverClass], e)
        }
    } else {
        Log.err(bundle["database.driver.load.failed", dbType, driverClass])
    }
}

/** JDBC 드라이버 목록 */
private fun getDriverMap(): Map<String, Triple<String, String, String>> {
    return mapOf(
        "mysql" to Triple("com.mysql", "mysql-connector-j", "com.mysql.cj.jdbc.Driver"),
        "mariadb" to Triple("org.mariadb.jdbc", "mariadb-java-client", "org.mariadb.jdbc.MariaDbDriver"),
        "postgresql" to Triple("org.postgresql", "postgresql", "org.postgresql.Driver"),
        "h2" to Triple("com.h2database", "h2", "org.h2.Driver"),
        "oracle" to Triple("com.oracle.ojdbc", "ojdbc", "com.oracle.jdbc.OracleDriver"),
        "mssql" to Triple("com.microsoft.sqlserver", "mssql-jdbc", "com.microsoft.sqlserver.jdbc.SQLServerDriver"),
        "sqlite" to Triple("org.xerial", "sqlite-jdbc", "org.sqlite.JDBC")
    )
}

/** JDBC 드라이버 다운로드 */
fun downloadSpecificDriver(dbType: String) {
    val driverMap = getDriverMap()
    val driverInfo = driverMap[dbType] ?: return

    val (groupId, artifactId, _) = driverInfo

    val driversDir = rootPath.child("drivers")
    if (!driversDir.exists()) {
        driversDir.mkdirs()
    }

    try {
        val groupPath = groupId.replace('.', '/')

        // 1) Try central.sonatype.com search API to resolve latest version
        var latestVersion: String? = null
        try {
            val centralUrl = URI("https://central.sonatype.com/api/search?q=g:$groupId+AND+a:$artifactId&rows=1&wt=json").toURL()
            Log.info("https://central.sonatype.com/api/search?q=g:$groupId+AND+a:$artifactId&rows=1&wt=json")
            val centralConn = centralUrl.openConnection()
            centralConn.getInputStream().bufferedReader().use { reader ->
                val resp = reader.readText()
                val regexes = listOf(
                    """"latestVersion"\s*:\s*"([^"]+)"""".toRegex(),
                    """"version"\s*:\s*"([^"]+)"""".toRegex()
                )
                for (re in regexes) {
                    val m = re.find(resp)
                    if (m != null) {
                        latestVersion = m.groupValues[1]
                        break
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore, we'll fallback to maven-metadata.xml
        }

        // 2) Fallback: fetch maven-metadata.xml from Maven Central repository
        if (latestVersion == null) {
            val metadataUrl = URI("https://repo1.maven.org/maven2/$groupPath/$artifactId/maven-metadata.xml").toURL()
            Log.info("https://repo1.maven.org/maven2/$groupPath/$artifactId/maven-metadata.xml")
            val metadataConn = metadataUrl.openConnection()
            val metadata = metadataConn.getInputStream().bufferedReader().use { it.readText() }

            val releaseRegex = "<release>([^<]+)</release>".toRegex()
            val latestRegex = "<latest>([^<]+)</latest>".toRegex()
            val versionsRegex = "<version>([^<]+)</version>".toRegex()

            latestVersion = releaseRegex.find(metadata)?.groupValues?.get(1)
                ?: latestRegex.find(metadata)?.groupValues?.get(1)
                ?: versionsRegex.findAll(metadata).map { it.groupValues[1] }.toList().lastOrNull()
        }

        if (latestVersion != null) {
            val filePath = "$groupPath/$artifactId/$latestVersion/$artifactId-$latestVersion.jar"
            val downloadUrl = URI("https://repo1.maven.org/maven2/$filePath").toURL()
            val outputFile = rootPath.child("drivers").child("$dbType-driver.jar").file()
            downloadFile(downloadUrl, outputFile)

            Log.info(bundle["database.download.driver", dbType, artifactId, latestVersion!!])
        } else {
            Log.warn(bundle["database.download.driver.failed", dbType])
        }
    } catch (e: Exception) {
        Log.err(bundle["database.download.driver.failed", dbType], e)
    }
}

/** URL 으로부터 파일 다운로드 */
private fun downloadFile(url: URL, outputFile: File) {
    url.openStream().use { inputStream ->
        val readableByteChannel = Channels.newChannel(inputStream)
        FileOutputStream(outputFile).use { fileOutputStream ->
            fileOutputStream.channel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE)
        }
    }
}

/**
 * 필요한 경우 database schema 업데이트
 * 현재 database 버전과 플러그인의 database 버전을 비교하고 SQL 를 실행 합니다.
 */
suspend fun upgradeDatabase() {
    val pluginData = getPluginData()
    val currentVersion = pluginData?.databaseVersion ?: DATABASE_VERSION

    if (currentVersion < DATABASE_VERSION) {
        Log.info(bundle["database.upgrade.start", currentVersion, DATABASE_VERSION])

        for (version in (currentVersion.toUInt() + 1u).toUByte()..DATABASE_VERSION) {
            val sqlFileName = "v${version}.sql"
            executeSqlScript(sqlFileName)
        }

        Log.info(bundle["database.upgrade.end"])
    } else {
        Log.info(bundle["database.upgrade.upToDate", DATABASE_VERSION])
    }
}

/**
 * resources/sql 폴더에서 SQL 파일을 실행 합니다.
 * @param fileName 실행할 SQL 파일 이름
 */
private fun executeSqlScript(fileName: String) {
    val sqlFilePath = "sql/$fileName"
    val inputStream: InputStream? = Thread.currentThread().contextClassLoader.getResourceAsStream(sqlFilePath)

    if (inputStream != null) {
        val sqlScript = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        Log.info(bundle["database.upgrade.execute", fileName])

        transaction {
            sqlScript.split(";").filter { it.trim().isNotEmpty() }.forEach { statement ->
                exec(statement)
            }
        }
    } else {
        Log.warn(bundle["database.upgrade.notFound", fileName])
    }
}

/** DB 업데이트 */
fun upgradeDatabaseBlocking() {
    runBlocking {
        upgradeDatabase()
    }
}


// Ensure parent directory exists for SQLite file paths in jdbc:sqlite: URLs
private fun ensureSqliteFileParentExists(jdbcUrl: String) {
    val prefix = "jdbc:sqlite:"
    if (!jdbcUrl.startsWith(prefix)) return
    val path = jdbcUrl.substring(prefix.length)
    // Skip special paths like :memory:
    if (path.startsWith(":")) return
    try {
        val file = File(path)
        file.parentFile?.let { parent ->
            if (!parent.exists()) parent.mkdirs()
        }
    } catch (_: Throwable) {
        // Ignore; connection will fail and be logged by Hikari/Exposed if the path is invalid
    }
}
