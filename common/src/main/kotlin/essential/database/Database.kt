package essential.database

import arc.util.Log
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import essential.DATABASE_VERSION
import essential.bundle
import essential.database.data.getPluginData
import essential.database.table.PlayerBannedTable
import essential.database.table.PlayerTable
import essential.database.table.PluginTable
import essential.rootPath
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URI
import java.net.URL
import java.nio.channels.Channels
import java.nio.charset.StandardCharsets

val datasource = HikariDataSource()

/** DB 초기 설정 */
fun databaseInit(jdbcUrl: String, user: String, pass: String) {
    val dbType = extractDatabaseType(jdbcUrl)
    loadJdbcDriver(dbType)

    datasource.dataSource = HikariDataSource(HikariConfig().apply {
        this.jdbcUrl = jdbcUrl
        username = user
        password = pass
        maximumPoolSize = 2
    })

    Database.connect(datasource)

    transaction {
        SchemaUtils.create(PlayerTable, PluginTable, PlayerBannedTable)
    }

    upgradeDatabaseBlocking()
}

/** DB 연결 종료 */
fun databaseClose() {
    datasource.close()
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

    if (driverInfo != null) {
        val (_, _, driverClass) = driverInfo
        try {
            Class.forName(driverClass)
            Log.info(bundle["database.driver.loaded", dbType, driverClass])
        } catch (e: ClassNotFoundException) {
            Log.err(bundle["database.driver.load.failed", dbType, driverClass], e)

            try {
                downloadSpecificDriver(dbType)
                Class.forName(driverClass)
                Log.info(bundle["database.driver.loaded.after.download", dbType, driverClass])
            } catch (e: Exception) {
                Log.err(bundle["database.driver.load.failed.after.download", dbType, driverClass], e)
            }
        }
    } else {
        Log.warn(bundle["database.driver.unknown", dbType])
    }
}

/** JDBC 드라이버 목록 */
private fun getDriverMap(): Map<String, Triple<String, String, String>> {
    return mapOf(
        "mysql" to Triple("com.mysql.cj", "mariadb-connector-j", "com.mysql.cj.jdbc.Driver"),
        "mariadb" to Triple("org.mariadb.jdbc", "mariadb-java-client", "org.mariadb.jdbc.MariaDbDriver"),
        "postgresql" to Triple("org.postgresql", "postgresql", "org.postgresql.Driver"),
        "h2" to Triple("com.h2database", "h2", "org.h2.Driver"),
        "oracle" to Triple("com.oracle.ojdbc", "ojdbc", "com.oracle.jdbc.OracleDriver"), // 되나?
        "mssql" to Triple("com.microsoft.sqlserver", "mssql-jdbc", "com.microsoft.sqlserver.jdbc.SQLServerDriver"),
        "sqlite" to Triple("org.xerial", "sqlitejdbc", "org.sqlite.JDBC")
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

        val metadataUrl =
            URI("https://search.maven.org/solrsearch/select?q=g:$groupId+AND+a:$artifactId&rows=1&wt=json").toURL()
        val metadataConnection = metadataUrl.openConnection()
        val metadataReader = metadataConnection.getInputStream().bufferedReader()
        val metadataResponse = metadataReader.readText()

        val versionRegex = """"latestVersion":"([^"]+)"""".toRegex()
        val versionMatch = versionRegex.find(metadataResponse)

        if (versionMatch != null) {
            val latestVersion = versionMatch.groupValues[1]
            val filePath = "$groupPath/$artifactId/$latestVersion/$artifactId-$latestVersion.jar"
            val downloadUrl = URI("https://search.maven.org/remotecontent?filepath=$filePath").toURL()
            val outputFile = rootPath.child("drivers").child("$dbType-driver.jar").file()
            downloadFile(downloadUrl, outputFile)

            Log.info(bundle["database.download.driver", dbType, artifactId, latestVersion])
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
    val currentVersion = pluginData?.databaseVersion ?: 0u

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
