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
import org.mindrot.jbcrypt.BCrypt
import java.sql.SQLException
import java.time.LocalDateTime
import java.util.*

object PlayerCore {
    var server: Server? = null

    fun playerLoad(p: Playerc, id: String?): Boolean {
        val playerData = load(p.uuid(), id)
        if (playerData != null){
            PluginData.playerData.add(playerData)
        } else {
            return false
        }

        if (LocalDateTime.now().isBefore(Tool.longToDateTime(playerData.bantime))) {
            Vars.netServer.admins.banPlayerID(p.uuid())
            Call.kick(p.con(), Packets.KickReason.banned)
            return false
        }

        if (Config.motd) {
            val motd = Tool.getMotd(Locale(playerData.countryCode))
            val count = motd.split("\r\n|\r|\n").toTypedArray().size
            if (count > 10) {
                Call.infoMessage(p.con(), motd)
            } else if (motd.isNotEmpty()) {
                p.sendMessage(motd)
            }
        }

        if (playerData.colornick) RainbowName.targets.add(p)

        val oldUUID = playerData.uuid

        playerData.uuid = p.uuid()
        playerData.lastdate = System.currentTimeMillis()
        playerData.joincount = playerData.joincount + 1
        playerData.exp = playerData.exp + playerData.joincount

        Permissions.setUserPerm(oldUUID, p.uuid())
        if (Permissions.user[p.uuid()] == null) {
            Permissions.create(playerData)
            Permissions.saveAll()
        } else {
            p.name(Permissions.user[playerData.uuid].asObject()["name"].asString())
        }
        p.admin(Permissions.isAdmin(playerData))
        return true
    }


    fun createData(player: Playerc?, name: String, uuid: String, id: String, pw: String): PlayerData {
        val country = Tool.getGeo(player)

        val json = JsonObject()
        json.add("name",name)
        json.add("uuid",uuid)
        json.add("countryCode",country.toLanguageTag())
        json.add("placecount",0)
        json.add("breakcount",0)
        json.add("joincount",0)
        json.add("kickcount",0)
        json.add("level",0)
        json.add("exp",0)
        json.add("firstdate",System.currentTimeMillis())
        json.add("lastdate",System.currentTimeMillis())
        json.add("playtime",0L)
        json.add("attackclear",0)
        json.add("pvpwincount",0)
        json.add("pvplosecount",0)
        json.add("pvpbreakout",0)
        json.add("bantime",0L)
        json.add("crosschat",false)
        json.add("colornick",false)
        json.add("permission","default")
        json.add("mute",false)
        json.add("alert",false)
        json.add("udid",0L)

        return PlayerData(uuid, json.toString(), id, pw)
    }

    fun login(id: String, pw: String): Boolean {
        try {
            database.prepareStatement("SELECT * from players WHERE accountid=?").use { pstmt ->
                pstmt.setString(1, id)
                pstmt.executeQuery().use { rs ->
                    return if (rs.next()) {
                        BCrypt.checkpw(pw, rs.getString("accountpw"))
                    } else {
                        false
                    }
                }
            }
        } catch (e: RuntimeException) {
            return false
        } catch (e: SQLException) {
            CrashReport(e)
            return false
        }
    }

    fun load(uuid: String, id: String?): PlayerData? {
        val sql = StringBuilder()
        sql.append("SELECT * FROM players WHERE uuid=?")
        if (id != null) sql.append(" OR accountid=?")
        try {
            database.prepareStatement(sql.toString()).use { pstmt ->
                pstmt.setString(1, uuid)
                if (id != null) pstmt.setString(2, id)
                pstmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        return PlayerData(
                            rs.getString("uuid"),
                            rs.getString("json"),
                            rs.getString("accountid"),
                            rs.getString("accountpw")
                        )
                    }
                }
            }
        } catch (e: SQLException) {
            CrashReport(e)
        }
        return null
    }

    fun save(playerData: PlayerData): Boolean {
        try {
            database.prepareStatement("UPDATE players SET json=?, accountid=?, accountpw=? WHERE uuid=?").use { p ->
                p.setString(1, playerData.json)
                p.setString(2, playerData.accountid)
                p.setString(3, playerData.accountpw)
                p.setString(4, playerData.uuid)
                return p.execute()
            }
        } catch (e: SQLException) {
            CrashReport(e)
            return false
        }
    }

    fun saveAll() {
        for (p in PluginData.playerData) save(p)
    }

    fun register(player: Playerc?, name: String, uuid: String, id: String, pw: String): Boolean {
        val sql = StringBuilder()
        sql.append("INSERT INTO players VALUES(?,?,?,?)")
        val new = createData(player, name, uuid, id, pw)
        try {
            database.prepareStatement(sql.toString()).use { p ->
                p.setString(1, new.uuid)
                p.setString(2, new.json)
                p.setString(3, new.accountid)
                p.setString(4, new.accountpw)
                val count = p.executeUpdate()
                return count > 0
            }
        } catch (e: SQLException) {
            CrashReport(e)
            return false
        }
    }
}