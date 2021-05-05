package essentials.data

import essentials.form.Service
import essentials.internal.CrashReport
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

object DB : Service() {
    lateinit var database: Connection

    override fun start() {
        Class.forName("org.h2.Driver")
        database = DriverManager.getConnection("jdbc:h2:file:./config/mods/Essentials/data/player", "", "")
        val data = "CREATE TABLE IF NOT EXISTS players (" + "uuid TEXT NOT NULL," + "json TEXT NOT NULL," + "accountid TEXT NOT NULL," + "accountpw TEXT NOT NULL" + ")"
        try {
            database.prepareStatement(data).use { pstmt -> pstmt.execute() }
        } catch(e: SQLException) {
            CrashReport(e)
        }
    }

    override fun stop() {
        if(!database.isClosed) database.close()
    }
}