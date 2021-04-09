package essentials.data

import essentials.form.Service
import essentials.internal.CrashReport
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

object DB : Service(){
    lateinit var database: Connection

    override fun start() {
        database = DriverManager.getConnection("jdbc:h2:file:./config/mods/Essentials/data/player", "", "")
        val data = "CREATE TABLE IF NOT EXISTS players (" +
                "uuid TEXT NOT NULL," +
                "data TEXT NOT NULL," +
                "accountid TEXT NOT NULL," +
                "accountpw TEXT NOT NULL" +
                ")"
        try {
            PlayerCore.conn.prepareStatement(data).use { pstmt -> pstmt.execute() }
        } catch (e: SQLException) {
            CrashReport(e)
        }
    }

    override fun stop() {
        if(!database.isClosed) database.close()
    }
}