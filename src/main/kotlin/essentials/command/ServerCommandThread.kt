package essentials.command

import arc.Core
import essentials.*
import essentials.command.ServerCommand.Command.*
import essentials.data.PlayerCore
import essentials.external.StringUtils
import essentials.event.feature.Permissions
import essentials.internal.Bundle
import essentials.internal.CrashReport
import essentials.internal.Log
import essentials.internal.Tool
import essentials.network.Client
import mindustry.Vars
import mindustry.gen.Groups
import mindustry.gen.Playerc
import java.sql.SQLException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ServerCommandThread(private val type: ServerCommand.Command, private val arg: Array<String>) : Thread() {
    override fun run() {
        when (type){
            Gendocs -> {
                val serverdoc = "## Server commands\n\n| Command | Parameter | Description |\n|:---|:---|:--- |\n"
                val clientdoc = "## Client commands\n\n| Command | Parameter | Description |\n|:---|:---|:--- |\n"
                val gentime = "README.md Generated time: ${
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(
                        LocalDateTime.now())}"
                Log.info("readme-generating")
                var tempbuild = StringBuilder()
                for (a in 0 until Vars.netServer.clientCommands.commandList.size) {
                    val command = Vars.netServer.clientCommands.commandList[a]
                    var dup = false
                    for (b in ClientCommand.commands.commandList) {
                        if (command.text == b.text) {
                            dup = true
                            break
                        }
                    }
                    if (!dup) {
                        val temp = """| ${command.text} | ${StringUtils.encodeHtml(command.paramText)} | ${command.description} |"""
                        tempbuild.append(temp)
                    }
                }
                val tmp = """$clientdoc$tempbuild""".trimIndent()
                tempbuild = StringBuilder()
                for (command in ServerCommand.commands.commandList) {
                    val temp = """| ${command.text} | ${StringUtils.encodeHtml(command.paramText)} | ${command.description} |"""
                    tempbuild.append(temp)
                }
                Main.pluginRoot.child("README.md").writeString(tmp + serverdoc + tempbuild.toString() + gentime)
                Log.info("success")
            }
            Lobby -> {
                Core.settings.put("isLobby", !Core.settings.getBool("isLobby"))
                Core.settings.saveValues()
                Log.info("success")
            }
            Edit -> {
                val sql = "UPDATE players SET " + arg[1] + "=? WHERE uuid=?"
                try {
                    PlayerCore.conn.prepareStatement(sql).use { pstmt ->
                        pstmt.setString(1, arg[2])
                        pstmt.setString(2, arg[0])
                        val playerData = PlayerCore[arg[0]]
                        val player = Groups.player.find { p: Playerc -> p.uuid() == arg[0] }
                        if (!playerData.error) {
                            PlayerCore.save(playerData)
                            playerData.toData(playerData.toMap().set(arg[1], arg[2]))
                            Permissions.user[playerData.uuid].asObject()[arg[1]] = arg[2]
                            Permissions.saveAll()
                        }
                        val count = pstmt.executeUpdate()
                        if (count < 1 && !playerData.error) {
                            Log.info("success")
                            PluginData.removePlayerData(playerData)
                            PluginData.players.remove(player)
                            PlayerCore.playerLoad(player, null)
                            player.sendMessage(Bundle(playerData.locale)["player.reloaded"])
                        } else {
                            Log.info("failed")
                        }
                    }
                } catch (e: SQLException) {
                    CrashReport(e)
                }
            }
            Saveall -> {
                PluginData.saveAll()
            }
            Admin -> {
                val player = Groups.player.find { p: Playerc -> p.name() == arg[0] }
                if (player == null) {
                    Log.warn("player.not-found")
                } else {
                    for (data in Permissions.perm) {
                        if (data.name == "newadmin") {
                            val p = PlayerCore[player.uuid()]
                            p.permission = "newadmin"
                            player.admin(Permissions.isAdmin(p))
                            Log.info("success")
                            break
                        }
                    }
                    //Log.warn("use-setperm");
                }
            }
            BanSync -> {
                if (Client.activated) {
                    Client.request(Client.Request.BanSync, null, null)
                } else {
                    Log.client("client.disabled")
                }
            }
            Info -> {
                val players = Vars.netServer.admins.findByName(arg[0])
                if (players.size != 0) {
                    for (p in players) {
                        try {
                            PlayerCore.conn.prepareStatement("SELECT * from players WHERE uuid=?").use { pstmt ->
                                pstmt.setString(1, p.id)
                                pstmt.executeQuery().use { rs ->
                                    if (rs.next()) {
                                        var datatext = "${rs.getString("name")} Player information\n" +
                                                "=====================================\n" +
                                                "name: ${rs.getString("name")}\n" +
                                                "uuid: ${rs.getString("uuid")}\n" +
                                                "country: ${rs.getString("country")}\n" +
                                                "countryCode: ${rs.getString("countryCode")}\n" +
                                                "language: ${rs.getString("language")}\n" +
                                                "isAdmin: ${rs.getBoolean("isAdmin")}\n" +
                                                "placecount: ${rs.getInt("placecount")}\n" +
                                                "breakcount: ${rs.getInt("breakcount")}\n" +
                                                "killcount: ${rs.getInt("killcount")}\n" +
                                                "deathcount: ${rs.getInt("deathcount")}\n" +
                                                "joincount: ${rs.getInt("joincount")}\n" +
                                                "kickcount: ${rs.getInt("kickcount")}\n" +
                                                "level: ${rs.getInt("level")}\n" +
                                                "exp: ${rs.getInt("exp")}\n" +
                                                "reqexp: ${rs.getInt("reqexp")}\n" +
                                                "firstdate: ${Tool.longToDateTime(rs.getLong("firstdate")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))}\n" +
                                                "lastdate: ${Tool.longToDateTime(rs.getLong("lastDate")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))}\n" +
                                                "lastplacename: ${rs.getString("lastplacename")}\n" +
                                                "lastbreakname: ${rs.getString("lastbreakname")}\n" +
                                                "lastchat: ${rs.getString("lastchat")}\n" +
                                                "playtime: ${Tool.longToTime(rs.getLong("playtime"))}\n" +
                                                "attackclear: ${rs.getInt("attackclear")}\n" +
                                                "pvpwincount: ${rs.getInt("pvpwincount")}\n" +
                                                "pvplosecount: ${rs.getInt("pvplosecount")}\n" +
                                                "pvpbreakout: ${rs.getInt("pvpbreakout")}\n" +
                                                "reactorcount: ${rs.getInt("reactorcount")}\n" +
                                                "bantime: ${rs.getString("bantime")}\n" +
                                                "crosschat: ${rs.getBoolean("crosschat")}\n" +
                                                "colornick: ${rs.getBoolean("colornick")}\n" +
                                                "connected: ${rs.getBoolean("connected")}\n" +
                                                "connserver: ${rs.getString("connserver")}\n" +
                                                "permission: ${rs.getString("permission")}\n" +
                                                "mute: ${rs.getBoolean("mute")}\n" +
                                                "alert: ${rs.getBoolean("alert")}\n" +
                                                "udid: ${rs.getLong("udid")}\n" +
                                                "accountid: ${rs.getString("accountid")}"

                                        val current = PlayerCore[p.id]
                                        if (!current.error) {
                                            datatext = "$datatext\n" +
                                                    "== ${current.name} Player internal data ==\n" + " +" +
                                                    "isLogin: ${current.login}\n" +
                                                    "afk: ${Tool.longToTime(current.afk)}\n" +
                                                    "afk_x: ${current.x}\n" + " +" +
                                                    "afk_y: ${current.y}"
                                        }
                                        Log.info(datatext)
                                    } else {
                                        Log.info("Player not found!")
                                    }
                                }
                            }
                        } catch (e: SQLException) {
                            CrashReport(e)
                        }
                    }
                }
            }
            // TODO 모든 권한 그룹 변경 만들기
            Setperm -> {
                val target = Groups.player.find { p: Playerc -> p.name() == arg[0] }
                val bundle = Bundle()
                val playerData: PlayerData
                if (target == null) {
                    Log.warn(bundle["player.not-found"])
                    return
                }
                for (p in Permissions.perm) {
                    if (p.name == arg[1]) {
                        playerData = PlayerCore[target.uuid()]
                        playerData.permission = arg[1]
                        Permissions.user[playerData.uuid].asObject()["group"] = arg[1]
                        Permissions.update(true)
                        Permissions.reload(false)
                        target.admin(Permissions.isAdmin(playerData))
                        Log.info(bundle["success"])
                        target.sendMessage(Bundle(PlayerCore[target.uuid()].locale).prefix("perm-changed"))
                        return
                    }
                }
                Log.warn(bundle["perm-group-not-found"])
            }
            Reload -> {
                Permissions.reload(false)
                Permissions.update(false)
                Log.info("plugin-reloaded")
            }
        }
    }
}