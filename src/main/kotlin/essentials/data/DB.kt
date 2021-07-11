package essentials.data

import arc.struct.Seq
import essentials.Main.Companion.pluginRoot
import essentials.PlayerData
import essentials.form.Service
import org.hjson.JsonObject
import org.hjson.JsonType
import java.sql.Connection
import java.sql.DriverManager

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

    // KR-Plugin 호환
    private fun backword() {
        if(pluginRoot.child("kr.mv.db").exists()){
            val db = DriverManager.getConnection("jdbc:h2:file:./config/mods/Essentials/kr", "", "")
            val buffer = Seq<PlayerData>()

            val result = db.prepareStatement("SELECT * FROM players").executeQuery()

            var total = 0
            var count = 0
            if(result.last()) {
                total = result.row
                result.beforeFirst()
            }

            while(result.next()){
                count++
                print("\r변환중... $count/$total")
                val data = PlayerData(
                    name = result.getString("name"),
                    uuid = result.getString("uuid"),
                    countryCode = result.getString("country"),
                    placecount = result.getInt("placeCount"),
                    breakcount = result.getInt("breakCount"),
                    joincount = result.getInt("joinCount"),
                    kickcount = result.getInt("kickCount"),
                    level = result.getInt("level"),
                    exp = result.getInt("exp"),
                    firstdate = result.getLong("joinDate"),
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

                PlayerCore.save(data)
            }

            print("\n")
            result.close()
            db.close()

            count = 0

            buffer.forEach {
                count++
                print("\r검사중... $count/$total")
                val r = PlayerCore.load(it.uuid, it.accountid)
                if (r == null){
                    println("검사 실패.")
                    return
                }
            }

            println("\n성공!")
        }
    }
}