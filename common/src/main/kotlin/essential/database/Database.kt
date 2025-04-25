package essential.database

import arc.util.serialization.Json
import arc.util.serialization.JsonReader
import arc.util.serialization.JsonValue
import com.fasterxml.jackson.databind.util.JSONPObject
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import essential.DATABASE_VERSION
import essential.database.data.DisplayData
import essential.database.data.PlayerData
import essential.database.data.PluginData
import essential.database.data.updatePluginData
import essential.database.table.PlayerBannedTable
import essential.database.table.PlayerTable
import essential.database.table.PlayerTable.locale
import essential.database.table.PlayerTable.name
import essential.database.table.PluginTable
import essential.rootPath
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import mindustry.Vars
import mindustry.gen.Playerc
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.json.JSONObject
import org.json.JSONTokener
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Paths

class Database {
    val datasource = HikariDataSource()

    fun init() {
        datasource.dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = "jdbc:sqlite:identifier.sqlite"
            username = "sa"
            password = ""
            maximumPoolSize = 2
        })

        Database.connect(datasource)

        transaction {
            SchemaUtils.create(PlayerTable, PluginTable, PlayerBannedTable)
        }
    }

    fun downloadDriver() {
        val driverMap = mapOf(
            "mysql" to Triple("com.mysql.cj", "mariadb-connector-j", "com.mysql.cj.jdbc.Driver"),
            "mariadb" to Triple("org.mariadb.jdbc", "mariadb-java-client", "org.mariadb.jdbc.MariaDbDriver"),
            "postgresql" to Triple("org.postgresql", "postgresql", "org.postgresql.Driver"),
            "h2" to Triple("com.h2database", "h2", "org.h2.Driver"),
            "oracle" to Triple("com.oracle.ojdbc", "ojdbc", "com.oracle.jdbc.OracleDriver"), // 되나?
            "mssql" to Triple("com.microsoft.sqlserver", "mssql-jdbc", "com.microsoft.sqlserver.jdbc.SQLServerDriver"),
            "sqlite" to Triple("org.xerial", "sqlitejdbc", "org.sqlite.JDBC")
        )

        //https://search.maven.org/remotecontent?filepath=org/xerial/sqlite-jdbc/3.49.1.0/sqlite-jdbc-3.49.1.0.jar

//        val url = URL("https://search.maven.org/solrsearch/select?q=g:\"$groupId\" AND a:\"$artifactId\"&rows=1&wt=json")

        val url = URL("")

        val mavenData = url.readText()
        JsonReader().parse(mavenData)["response"]["docs"][0].getString("latestVersion")

        url.openStream().use {
            Files.copy(it, rootPath.child("drivers/${}"))
        }
    }
}