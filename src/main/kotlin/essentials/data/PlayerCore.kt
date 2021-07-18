package essentials.data

import essentials.PlayerData
import essentials.PluginData
import essentials.data.DB.database
import essentials.event.feature.Permissions
import essentials.event.feature.RainbowName
import essentials.internal.CrashReport
import essentials.internal.Tool
import mindustry.Vars
import mindustry.gen.Call
import mindustry.gen.Playerc
import mindustry.net.Packets
import org.h2.tools.Server
import org.hjson.JsonObject
import org.hjson.JsonType
import org.mindrot.jbcrypt.BCrypt
import java.sql.SQLException
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*
import java.util.function.Consumer

object PlayerCore {
    var server: Server? = null

    fun playerLoad(p: Playerc, id: String?): Boolean {
        val playerData = load(p.uuid(), id)
        if(playerData != null) {
            PluginData.playerData.add(playerData)
        } else {
            return false
        }

        if(LocalDateTime.now().isBefore(Tool.longToDateTime(playerData.bantime))) {
            Vars.netServer.admins.banPlayerID(p.uuid())
            Call.kick(p.con(), Packets.KickReason.banned)
            return false
        }

        if(Config.motd) {
            val motd = Tool.getMotd(Locale(playerData.countryCode))
            val count = motd.split("\r\n|\r|\n").toTypedArray().size
            if(count > 10) {
                Call.infoMessage(p.con(), motd)
            } else if(motd.isNotEmpty()) {
                p.sendMessage(motd)
            }
        }

        if(playerData.colornick) RainbowName.targets.add(p)

        val oldUUID = playerData.uuid

        playerData.uuid = p.uuid()
        playerData.lastdate = System.currentTimeMillis()
        playerData.joincount = playerData.joincount++
        playerData.exp = playerData.exp + playerData.joincount

        Permissions.setUserPerm(oldUUID, p.uuid())
        if(Permissions.user[p.uuid()] == null) {
            Permissions.create(playerData)
            Permissions.saveAll()
        } else {
            p.name(Permissions.user[playerData.uuid].asObject()["name"].asString())
        }
        p.admin(Permissions.isAdmin(playerData))
        return true
    }

    fun createData(player: Playerc, name: String, uuid: String, id: String, pw: String): PlayerData {
        val country = Tool.getGeo(player)
        return PlayerData(name, uuid, country.isO3Country, System.currentTimeMillis(), id, pw, "default")
    }

    fun login(id: String, pw: String): Boolean {
        try {
            database.prepareStatement("SELECT * from players WHERE accountid=?").use { pstmt ->
                pstmt.setString(1, id)
                pstmt.executeQuery().use { rs ->
                    return if(rs.next()) {
                        BCrypt.checkpw(pw, rs.getString("accountpw"))
                    } else {
                        false
                    }
                }
            }
        } catch(e: RuntimeException) {
            return false
        } catch(e: SQLException) {
            CrashReport(e)
            return false
        }
    }

    fun load(uuid: String, id: String?): PlayerData? {
        val sql = StringBuilder()
        sql.append("SELECT * FROM players WHERE uuid=?")
        if(id != null) sql.append(" OR accountid=?")
        try {
            database.prepareStatement(sql.toString()).use { pstmt ->
                pstmt.setString(1, uuid)
                if(id != null) pstmt.setString(2, id)
                pstmt.executeQuery().use { rs ->
                    if(rs.next()) {
                        return PlayerData(
                            name = rs.getString("name"),
                            uuid = rs.getString("uuid"),
                            countryCode = rs.getString("countryCode"),
                            placecount = rs.getInt("placecount"),
                            breakcount = rs.getInt("breakcount"),
                            joincount = rs.getInt("joincount"),
                            kickcount = rs.getInt("kickcount"),
                            level = rs.getInt("level"),
                            exp = rs.getInt("exp"),
                            joinDate = rs.getLong("joindate"),
                            lastdate = rs.getLong("lastdate"),
                            playtime = rs.getLong("playtime"),
                            attackclear = rs.getInt("attackclear"),
                            pvpwincount = rs.getInt("pvpwincount"),
                            pvplosecount = rs.getInt("pvplosecount"),
                            bantime = rs.getLong("bantime"),
                            crosschat = rs.getBoolean("crosschat"),
                            colornick = rs.getBoolean("colornick"),
                            permission = rs.getString("permission"),
                            mute = rs.getBoolean("mute"),
                            udid = rs.getLong("udid"),
                            json = JsonObject.readJSON(rs.getString("json")).asObject(),
                            accountid = rs.getString("accountid"),
                            accountpw = rs.getString("accountpw")
                        )
                    }
                }
            }
        } catch(e: SQLException) {
            CrashReport(e)
        }
        return null
    }

    fun save(playerData: PlayerData): Boolean {
        val sql = StringBuilder()
        val js = playerData.toJson()
        sql.append("UPDATE players SET ")
        js.forEach(Consumer { s: JsonObject.Member ->
            val buf = s.name.lowercase(Locale.getDefault()) + "=?, "
            sql.append(buf)
        })
        sql.deleteCharAt(sql.length - 2)
        sql.append(" WHERE uuid=?")
        try {
            database.prepareStatement(sql.toString()).use { p ->
                js.forEach(object : Consumer<JsonObject.Member> {
                    var index = 1
                    override fun accept(o: JsonObject.Member) {
                        when(o.value.type) {
                            JsonType.STRING -> p.setString(index, o.value.asString())
                            JsonType.BOOLEAN -> p.setBoolean(index, o.value.asBoolean())
                            JsonType.NUMBER -> p.setLong(index, o.value.asLong())
                            else -> p.setString(index, o.value.toString())
                        }
                        index++
                    }
                })
                p.setString(js.size() + 1, playerData.uuid)
                return p.execute()
            }
        } catch (e: SQLException) {
            CrashReport(e)
            return false
        }
    }

    fun saveAll() {
        for(p in PluginData.playerData) save(p)
    }

    fun register(name: String, uuid: String, countryCode: String, id: String, pw: String, permission: String): Boolean {
        val sql = StringBuilder()
        sql.append("INSERT INTO players VALUES(")
        val newdata = PlayerData(name, uuid, countryCode, System.currentTimeMillis(), id, pw, permission)
        val js = newdata.toJson()
        js.forEach(Consumer { sql.append("?,") })
        sql.deleteCharAt(sql.length - 1)
        sql.append(")")
        try {
            database.prepareStatement(sql.toString()).use { p ->
                js.forEach(object : Consumer<JsonObject.Member> {
                    var index = 1
                    override fun accept(o: JsonObject.Member) {
                        when(o.value.type) {
                            JsonType.STRING -> p.setString(index, o.value.asString())
                            JsonType.BOOLEAN -> p.setBoolean(index, o.value.asBoolean())
                            JsonType.NUMBER -> p.setLong(index, o.value.asLong())
                            else -> p.setString(index, o.value.toString())
                        }
                        index++
                    }
                })
                val count = p.executeUpdate()
                return count > 0
            }
        } catch (e: SQLException) {
            CrashReport(e)
            return false
        }
    }
}