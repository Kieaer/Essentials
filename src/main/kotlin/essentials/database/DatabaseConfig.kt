package essentials.database

import arc.files.Fi
import essentials.database.table.BannedTable
import essentials.database.table.PlayerDataTable
import essentials.database.table.PluginData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.sqlite.SQLiteConfig

class DatabaseConfig(val rootPath: Fi) {
    fun connect() {
        Database.connect(
            "jdbc:sqlite:file:${rootPath.absolutePath()}/database",
            driver = "org.sqlite.JDBC",
            setupConnection = { conn ->
                val sqliteConfig = SQLiteConfig().apply {
                    setJournalMode(SQLiteConfig.JournalMode.WAL)
                }
                sqliteConfig.apply(conn)
            })

        transaction {
            SchemaUtils.createMissingTablesAndColumns(PlayerDataTable)
            SchemaUtils.createMissingTablesAndColumns(BannedTable)
            SchemaUtils.createMissingTablesAndColumns(PluginData)
        }
    }

    fun write() = dbQuery {

    }

    fun read() {

    }

    private fun <T> dbQuery(block: suspend () -> T) = runBlocking(Dispatchers.IO) {
        launch {
            newSuspendedTransaction {
                block()
            }
        }
    }
}