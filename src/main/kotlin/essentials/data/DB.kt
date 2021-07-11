package essentials.data

import essentials.PlayerData
import essentials.form.Service
import essentials.internal.CrashReport
import org.hjson.JsonObject
import org.hjson.JsonType
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.function.Consumer

object DB : Service() {
    lateinit var database: Connection

    override fun start() {
        Class.forName("org.h2.Driver")
        database = DriverManager.getConnection("jdbc:h2:file:./config/mods/Essentials/data/player", "", "")
        val js = PlayerData().toJson()
        val sql = StringBuilder().append("CREATE TABLE IF NOT EXISTS players (")
        js.forEach{
            sql.append("${it.name} ")
            when (it.value.type){
                JsonType.STRING -> sql.append("TEXT ")
                JsonType.BOOLEAN -> sql.append("TINYINT(4) ")
                JsonType.NUMBER -> sql.append("INT(11) ")
                else -> sql.append("TEXT ")
            }
            sql.append("NOT NULL,")
        }
        sql.deleteCharAt(sql.length - 1).append(")")
        database.prepareStatement(sql.toString()).execute()
    }

    override fun stop() {
        if(!database.isClosed) database.close()
    }
}