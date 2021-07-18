package essentials.data

import arc.struct.Seq
import com.neovisionaries.i18n.CountryCode
import essentials.Main.Companion.pluginRoot
import essentials.PlayerData
import essentials.form.Service
import essentials.internal.Tool
import org.hjson.JsonObject
import org.hjson.JsonType
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException

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
                JsonType.BOOLEAN -> sql.append("BOOLEAN ")
                JsonType.NUMBER -> sql.append("BIGINT ")
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

    // KR-Plugin 호환
    fun backword() {
        try {
            val db = database.createStatement()
            db.execute("ALTER TABLE players ALTER COLUMN IF EXISTS firstDate RENAME TO joinDate")
            db.execute("ALTER TABLE players ALTER COLUMN joindate SET DATA TYPE BIGINT")
            db.execute("ALTER TABLE players ALTER COLUMN lastdate SET DATA TYPE BIGINT")
            db.execute("ALTER TABLE players ALTER COLUMN playtime SET DATA TYPE BIGINT")
            db.close()
        } catch (e: SQLException){
            e.printStackTrace()
        }

        if(pluginRoot.child("kr.mv.db").exists()){
            val db = DriverManager.getConnection("jdbc:h2:file:./config/mods/Essentials/kr", "", "")
            val buffer = Seq<PlayerData>()

            val db2 = db.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE)
            var result = db2.executeQuery("SELECT * FROM players")

            var total = 0
            var count = 0
            if(result.last()) {
                total = result.row
                result.close()
            }

            result = db2.executeQuery("SELECT * FROM players")
            while(result.next()){
                count++
                print("\r변환중... $count/$total")
                val data = PlayerData(
                    name = result.getString("name"),
                    uuid = result.getString("uuid"),
                    countryCode = "null",
                    placecount = result.getInt("placeCount"),
                    breakcount = result.getInt("breakCount"),
                    joincount = result.getInt("joinCount"),
                    kickcount = result.getInt("kickCount"),
                    level = result.getInt("level"),
                    exp = result.getInt("exp"),
                    joinDate = result.getLong("joinDate"),
                    lastdate = result.getLong("lastDate"),
                    playtime = result.getLong("playTime"),
                    attackclear = result.getInt("attackWinner"),
                    pvpwincount = result.getInt("pvpWinner"),
                    pvplosecount = result.getInt("pvpLoser"),
                    bantime = 0L,
                    crosschat = false,
                    colornick = false,
                    permission = result.getString("rank"),
                    mute = false,
                    udid = 0L,
                    json = JsonObject.readJSON(result.getString("json")).asObject(),
                    accountid = result.getString("id"),
                    accountpw = result.getString("pw")
                )

                buffer.add(data)
                PlayerCore.register(data)
            }

            print("\n")
            result.close()
            db.close()
            db2.close()

            count = 0
            buffer.forEach {
                count++
                print("\r검사중... $count/$total")
                val r = PlayerCore.load(it.uuid, null)
                if (r == null){
                    println("\n검사 실패.")
                    return
                }
            }

            println("\n성공!")
        }
    }
}