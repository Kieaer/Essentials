package essentials.command

import arc.Core
import essentials.Main.Companion.pluginRoot
import essentials.PluginData
import essentials.command.ServerCommand.Command.*
import essentials.data.PlayerCore
import essentials.event.feature.Permissions
import essentials.external.StringUtils
import essentials.internal.CrashReport
import essentials.internal.Log
import essentials.internal.Tool
import essentials.network.Client
import mindustry.Vars.netServer
import java.sql.SQLException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ServerCommandThread(private val type: ServerCommand.Command, private val arg: Array<String>) : Thread() {
    override fun run() {
        try {
            when (type) {
                Gendocs -> {
                    val server = "## Server commands\n\n| Command | Parameter | Description |\n|:---|:---|:--- |\n"
                    val client = "## Client commands\n\n| Command | Parameter | Description |\n|:---|:---|:--- |\n"
                    val time = "README.md Generated time: ${
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(
                            LocalDateTime.now()
                        )
                    }"
                    Log.info("readme-generating")
                    var result = StringBuilder()

                    for (a in 0 until netServer.clientCommands.commandList.size) {
                        val command = netServer.clientCommands.commandList[a]
                        var duplicate = false
                        for (b in ClientCommand.commands.commandList) {
                            if (command.text == b.text) {
                                duplicate = true
                                break
                            }
                        }

                        if (!duplicate) {
                            val temp =
                                "| ${command.text} | ${StringUtils.encodeHtml(command.paramText)} | ${command.description} |"
                            result.append(temp)
                        }
                    }

                    val tmp = "$client$result"

                    result = StringBuilder()
                    for (command in ServerCommand.commands.commandList) {
                        val temp =
                            "| ${command.text} | ${StringUtils.encodeHtml(command.paramText)} | ${command.description} |"
                        result.append(temp)
                    }

                    pluginRoot.child("README.md").writeString("$tmp$server$result$time")
                    Log.info("readme-generated")
                }
                Lobby -> {
                    Core.settings.put("isLobby", !Core.settings.getBool("isLobby"))
                    Core.settings.saveValues()
                    Log.info(if (Core.settings.getBool("isLobby")) "lobby-enabled" else "lobby-disabled")
                }
                Saveall -> {
                    PluginData.saveAll()
                    Log.info("system.saved")
                }
                BanSync -> {
                    if (Client.activated) {
                        Client.request(Client.Request.BanSync, null, null)
                    } else {
                        Log.client("client.disabled")
                    }
                }
                Info -> {
                    val players = netServer.admins.findByName(arg[0])
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
                                                    "firstdate: ${
                                                        Tool.longToDateTime(rs.getLong("firstdate"))
                                                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
                                                    }\n" +
                                                    "lastdate: ${
                                                        Tool.longToDateTime(rs.getLong("lastDate"))
                                                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
                                                    }\n" +
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
                                            Log.info("player.not-found")
                                        }
                                    }
                                }
                            } catch (e: SQLException) {
                                CrashReport(e)
                            }
                        }
                    }
                }
                Reload -> {
                    Permissions.reload(false)
                    Permissions.update(false)
                    Log.info("plugin-reloaded")
                }
            }
        } catch (e: Exception){
            e.printStackTrace()
        }
    }
}