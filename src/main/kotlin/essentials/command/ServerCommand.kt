package essentials.command

import arc.Core
import arc.util.CommandHandler
import essentials.Main
import essentials.PluginData
import essentials.data.DB
import essentials.event.feature.Permissions
import essentials.external.StringUtils
import essentials.internal.CrashReport
import essentials.internal.Log
import essentials.internal.Tool
import essentials.network.Client
import mindustry.Vars
import java.sql.SQLException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ServerCommand {
    lateinit var commands: CommandHandler

    fun register(handler: CommandHandler) {
        handler.register("gendocs", "Generate Essentials README.md") {
            val server = "## Server commands\n\n| Command | Parameter | Description |\n|:---|:---|:--- |\n"
            val client = "## Client commands\n\n| Command | Parameter | Description |\n|:---|:---|:--- |\n"
            val time = "README.md Generated time: ${
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now())
            }"
            Log.info("readme-generating")
            var result = StringBuilder()

            for(a in 0 until Vars.netServer.clientCommands.commandList.size) {
                val command = Vars.netServer.clientCommands.commandList[a]
                var duplicate = false
                for(b in ClientCommand.commands.commandList) {
                    if(command.text == b.text) {
                        duplicate = true
                        break
                    }
                }

                if(!duplicate) {
                    val temp = "| ${command.text} | ${StringUtils.encodeHtml(command.paramText)} | ${command.description} |"
                    result.append(temp)
                }
            }

            val tmp = "$client$result"

            result = StringBuilder()
            for(command in ServerCommand.commands.commandList) {
                val temp = "| ${command.text} | ${StringUtils.encodeHtml(command.paramText)} | ${command.description} |"
                result.append(temp)
            }

            Main.pluginRoot.child("README.md").writeString("$tmp$server$result$time")
            Log.info("readme-generated")
        }
        handler.register("lobby", "Toggle lobby server features") {
            Core.settings.put("isLobby", !Core.settings.getBool("isLobby"))
            Core.settings.saveValues()
            Log.info(if(Core.settings.getBool("isLobby")) "lobby-enabled" else "lobby-disabled")
        }
        handler.register("saveall", "Manually save all plugin data") {
            PluginData.saveAll()
            Log.info("system.saved")
        }
        handler.register("bansync", "Synchronize ban list with server") {
            if(Client.activated) {
                Client.request(Client.Request.BanSync, null, null)
            } else {
                Log.client("client.disabled")
            }
        }
        handler.register("info", "<player/uuid>", "Show player information") {
            val players = Vars.netServer.admins.findByName(it[0])
            if(players.size != 0) {
                for(p in players) {
                    try {
                        DB.database.prepareStatement("SELECT * from players WHERE uuid=?").use { pstmt ->
                            pstmt.setString(1, p.id)
                            pstmt.executeQuery().use { rs ->
                                if(rs.next()) {
                                    Log.info("${rs.getString("name")} Player information\n" + "=====================================\n" + "name: ${rs.getString("name")}\n" + "uuid: ${rs.getString("uuid")}\n" + "country: ${rs.getString("country")}\n" + "countryCode: ${rs.getString("countryCode")}\n" + "language: ${rs.getString("language")}\n" + "isAdmin: ${rs.getBoolean("isAdmin")}\n" + "placecount: ${rs.getInt("placecount")}\n" + "breakcount: ${rs.getInt("breakcount")}\n" + "killcount: ${rs.getInt("killcount")}\n" + "deathcount: ${rs.getInt("deathcount")}\n" + "joincount: ${rs.getInt("joincount")}\n" + "kickcount: ${rs.getInt("kickcount")}\n" + "level: ${rs.getInt("level")}\n" + "exp: ${rs.getInt("exp")}\n" + "reqexp: ${rs.getInt("reqexp")}\n" + "firstdate: ${
                                        Tool.longToDateTime(rs.getLong("firstdate")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
                                    }\n" + "lastdate: ${
                                        Tool.longToDateTime(rs.getLong("lastDate")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
                                    }\n" + "lastplacename: ${rs.getString("lastplacename")}\n" + "lastbreakname: ${rs.getString("lastbreakname")}\n" + "lastchat: ${rs.getString("lastchat")}\n" + "playtime: ${Tool.longToTime(rs.getLong("playtime"))}\n" + "attackclear: ${rs.getInt("attackclear")}\n" + "pvpwincount: ${rs.getInt("pvpwincount")}\n" + "pvplosecount: ${rs.getInt("pvplosecount")}\n" + "pvpbreakout: ${rs.getInt("pvpbreakout")}\n" + "reactorcount: ${rs.getInt("reactorcount")}\n" + "bantime: ${rs.getString("bantime")}\n" + "crosschat: ${rs.getBoolean("crosschat")}\n" + "colornick: ${rs.getBoolean("colornick")}\n" + "connected: ${rs.getBoolean("connected")}\n" + "connserver: ${rs.getString("connserver")}\n" + "permission: ${rs.getString("permission")}\n" + "mute: ${rs.getBoolean("mute")}\n" + "alert: ${rs.getBoolean("alert")}\n" + "udid: ${rs.getLong("udid")}\n" + "accountid: ${rs.getString("accountid")}")
                                } else {
                                    Log.info("player.not-found")
                                }
                            }
                        }
                    } catch(e: SQLException) {
                        CrashReport(e)
                    }
                }
            }
        }
        handler.register("reload", "Reload Essential plugin data") {
            Permissions.reload(false)
            Permissions.update(false)
            Log.info("plugin-reloaded")
        }
        handler.register("blacklist", "<add/remove> [name]") {

        }

        commands = handler
    }
}