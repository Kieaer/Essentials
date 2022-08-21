package essentials.command

import arc.Core
import arc.util.CommandHandler
import com.neovisionaries.i18n.CountryCode
import essentials.Main
import essentials.PluginData
import essentials.data.DB
import essentials.data.DB.database
import essentials.data.PlayerCore
import essentials.event.feature.Exp
import essentials.event.feature.Permissions
import essentials.external.StringUtils
import essentials.internal.Bundle
import essentials.internal.Log
import essentials.internal.Tool
import essentials.network.Client
import mindustry.Vars
import mindustry.gen.Groups
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ServerCommand {
    lateinit var commands: CommandHandler

    fun register(handler: CommandHandler) {
        handler.register("gendocs", "Generate Essentials README.md") {
            val server = "## Server commands\n| Command | Parameter | Description |\n|:---|:---|:--- |\n"
            val client = "## Client commands\n| Command | Parameter | Description |\n|:---|:---|:--- |\n"
            val time = "README.md Generated time: ${
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now())
            }"
            Log.info("readme-generating")
            val result = StringBuilder()

            for(b in ClientCommand.commands.commandList) {
                val temp = "| ${b.text} | ${StringUtils.encodeHtml(b.paramText)} | ${b.description} |\n"
                result.append(temp)
            }

            val tmp = "$client$result\n\n"

            result.clear()
            for(c in commands.commandList) {
                val temp = "| ${c.text} | ${StringUtils.encodeHtml(c.paramText)} | ${c.description} |\n"
                result.append(temp)
            }

            Main.pluginRoot.child("README.md").writeString("$tmp$server$result\n\n\n$time")
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
                    val data = PlayerCore.load(p.id, null)
                    val bundle = Bundle()
                    val text: String
                    if (data != null){
                        text = """
                        ====================================
                        ${bundle["player.name"]}: ${data.name}
                        ${bundle["player.uuid"]}: ${data.uuid}
                        ${bundle["player.country"]}: ${if(CountryCode.getByAlpha3Code(data.countryCode) != null) CountryCode.getByAlpha3Code(data.countryCode).toLocale().displayCountry else "Data not found"}
                        ${bundle["player.placecount"]}: ${data.placecount}
                        ${bundle["player.breakcount"]}: ${data.breakcount}
                        ${bundle["player.joincount"]}: ${data.joincount}
                        ${bundle["player.kickcount"]}: ${data.kickcount}
                        ${bundle["player.level"]}: ${data.level}
                        ${bundle["player.reqtotalexp"]}: ${Exp[data]}
                        ${bundle["player.joindate"]}: ${
                            Tool.longToDateTime(data.joinDate).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))}
                        ${bundle["player.lastdate"]}: ${
                            Tool.longToDateTime(data.lastdate).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))}
                        ${bundle["player.playtime"]}: ${Tool.longToTime(data.playtime)}
                        ${bundle["player.attackclear"]}: ${data.attackclear}
                        ${bundle["player.pvpwincount"]}: ${data.pvpwincount}
                        ${bundle["player.pvplosecount"]}: ${data.pvplosecount}
                        """.trimIndent()

                        val result = text.lines()
                        for(a in result) Log.info(a)
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
        handler.register("debug", "<code>", "Debug message. plugin developer only.") {
            if(it[0] == "db"){
                for(p in Groups.player){
                    p.con.close()
                }
                database.prepareStatement("DELETE FROM players").execute()
                Log.info("success")
            } else if (it[0] == "import"){
                if(Main.pluginRoot.child("kr.mv.db").exists()) {
                    DB.backword()
                } else {
                    Log.info("파일 없음")
                }
            } else if (it[0] == "backword"){
                DB.backword()
            }
        }

        commands = handler
    }
}