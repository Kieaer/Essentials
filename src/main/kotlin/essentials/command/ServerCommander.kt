package essentials.command

import arc.Core
import arc.util.CommandHandler
import essentials.Main
import essentials.PlayerData
import essentials.external.StringUtils
import essentials.internal.Bundle
import essentials.internal.CrashReport
import essentials.internal.Log
import essentials.network.Client
import mindustry.Vars
import mindustry.gen.Groups
import mindustry.gen.Playerc
import java.sql.SQLException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ServerCommander {
    lateinit var commands: CommandHandler

    fun register(handler: CommandHandler) {
        handler.register("gendocs", "Generate Essentials README.md", ::genDocs)
        handler.register("lobby", "Toggle lobby server features", ::lobby)
        handler.register("edit", "<uuid> <name> [value]", "Edit PlayerData directly", ::edit)
        handler.register("saveall", "Manually save all plugin data", ::saveall)
        handler.register("admin", "<name>", "Set admin status to player.", ::admin)
        handler.register("bansync", "Synchronize ban list with server", ::bansync)
        handler.register("info", "<player/uuid>", "Show player information", ::info)
        handler.register("setperm", "<player_name/uuid> <group>", "Set player permission", ::setperm)
        handler.register("reload", "Reload Essential plugin data", ::reload)

        commands = handler
    }

    private fun genDocs(arg: Array<String>) {
        val serverdoc = "## Server commands\n\n| Command | Parameter | Description |\n|:---|:---|:--- |\n"
        val clientdoc = "## Client commands\n\n| Command | Parameter | Description |\n|:---|:---|:--- |\n"
        val gentime = "README.md Generated time: ${DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now())}"
        Log.info("readme-generating")
        val header = """
                [![SonarCloud Coverage](https://sonarcloud.io/api/project_badges/measure?project=Kieaer_Essentials&metric=coverage)](https://sonarcloud.io/component_measures/metric/coverage/list?id=Kieaer_Essentials) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Kieaer_Essentials&metric=alert_status)](https://sonarcloud.io/dashboard?id=Kieaer_Essentials)
                # Essentials
                Add more commands to the server.
                
                I'm getting a lot of suggestions.<br>
                Please submit your idea to this repository issues or Mindustry official discord!
                
                ## Essentials 11 Plans
                - [ ] Fix bugs
                - [ ] Voting not working
                  - [ ] Sometimes an account system not working
                - [ ] Fix many typos
                - [ ] Features separation
                  - [ ] Features separation
                    - [x] Rest API [Plugin Link](https://github.com/Kieaer/Essential-REST_API) 
                        - [x] Information 
                        - [x] Add players detail information 
                        - [x] Add a gamemode 
                        - [x] Add other team core resource status 
                        - [x] Add a server map list
                  - [ ] Web server
                    - [ ] Fix a sometimes ranking site not loaded
                  - [x] Auto Rollback (Not remove) [Plugin Link](https://github.com/Kieaer/AutoRollback)
                - [ ] New features
                  - [ ] Web console
                    - [ ] Control plugin database
                    - [ ] Check world status
                      - [ ] Dynmap (idea from Minecraft)
                      - [ ] Rest API
                - [ ] Remove external API services
                  - [x] IP API (Due to traffic excess)
                  - [ ] Translate (Due to paid service)
                - [ ] Security patches
                - [ ] All code clean
                
                ### Recommend
                CPU: Ryzen 3 2200G or Intel i3 8100<br>
                RAM: 50MB<br>
                Disk: HDD capable of more than 5MB/s random read/write.
                
                ## Installation
                
                Put this plugin in the ``<server folder location>/config/mods`` folder.
                
                
                """.trimIndent()
        var tempbuild = StringBuilder()
        for (a in 0 until Vars.netServer.clientCommands.commandList.size) {
            val command = Vars.netServer.clientCommands.commandList[a]
            var dup = false
            for (b in ClientCommander.commands.commandList) {
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
        val tmp = """$header$clientdoc$tempbuild""".trimIndent()
        tempbuild = StringBuilder()
        for (command in commands.commandList) {
            val temp = """| ${command.text} | ${StringUtils.encodeHtml(command.paramText)} | ${command.description} |"""
            tempbuild.append(temp)
        }
        Main.pluginRoot.child("README.md").writeString(tmp + serverdoc + tempbuild.toString() + gentime)
        Log.info("success")
    }

    private fun lobby(arg: Array<String>) {
        Core.settings.put("isLobby", !Core.settings.getBool("isLobby"))
        Core.settings.saveValues()
        Log.info("success")
    }

    private fun edit(arg: Array<String>) {
        val sql = "UPDATE players SET " + arg[1] + "=? WHERE uuid=?"
        try {
            Main.playerCore.conn.prepareStatement(sql).use { pstmt ->
                pstmt.setString(1, arg[2])
                pstmt.setString(2, arg[0])
                val playerData = Main.playerCore[arg[0]]
                val player = Groups.player.find { p: Playerc -> p.uuid() == arg[0] }
                if (!playerData.error) {
                    Main.playerCore.save(playerData)
                    playerData.toData(playerData.toMap().set(arg[1], arg[2]))
                    Main.perm.user[playerData.uuid].asObject()[arg[1]] = arg[2]
                    Main.perm.saveAll()
                }
                val count = pstmt.executeUpdate()
                if (count < 1 && !playerData.error) {
                    Log.info("success")
                    Main.vars.removePlayerData(playerData)
                    Main.vars.players.remove(player)
                    Main.playerCore.playerLoad(player, null)
                    player.sendMessage(Bundle(playerData.locale)["player.reloaded"])
                } else {
                    Log.info("failed")
                }
            }
        } catch (e: SQLException) {
            CrashReport(e)
        }
    }

    private fun saveall(arg: Array<String>) {
        Main.pluginData.saveAll()
    }

    private fun admin(arg: Array<String>) {
        val player = Groups.player.find { p: Playerc -> p.name() == arg[0] }
        if (player == null) {
            Log.warn("player.not-found")
        } else {
            for (data in Main.perm.perm) {
                if (data.name == "newadmin") {
                    val p = Main.playerCore[player.uuid()]
                    p.permission = "newadmin"
                    player.admin(Main.perm.isAdmin(p))
                    Log.info("success")
                    break
                }
            }
            //Log.warn("use-setperm");
        }
    }

    private fun bansync(arg: Array<String>) {
        if (Main.client.activated) {
            Main.client.request(Client.Request.BanSync, null, null)
        } else {
            Log.client("client.disabled")
        }
    }

    private fun info(arg: Array<String>) {
        val players = Vars.netServer.admins.findByName(arg[0])
        if (players.size != 0) {
            for (p in players) {
                try {
                    Main.playerCore.conn.prepareStatement("SELECT * from players WHERE uuid=?").use { pstmt ->
                        pstmt.setString(1, p.id)
                        pstmt.executeQuery().use { rs ->
                            if (rs.next()) {
                                var datatext = "${rs.getString("name")} Player information\n" +
                                        "=====================================\n" +
                                        "name: ${rs.getString("name")}\n" +
                                        "uuid: ${rs.getString("uuid")}\n" +
                                        "country: ${rs.getString("country")}\n" +
                                        "country_code: ${rs.getString("country_code")}\n" +
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
                                        "firstdate: ${Main.tool.longToDateTime(rs.getLong("firstdate")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))}\n" +
                                        "lastdate: ${Main.tool.longToDateTime(rs.getLong("lastDate")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))}\n" +
                                        "lastplacename: ${rs.getString("lastplacename")}\n" +
                                        "lastbreakname: ${rs.getString("lastbreakname")}\n" +
                                        "lastchat: ${rs.getString("lastchat")}\n" +
                                        "playtime: ${Main.tool.longToTime(rs.getLong("playtime"))}\n" +
                                        "attackclear: ${rs.getInt("attackclear")}\n" +
                                        "pvpwincount: ${rs.getInt("pvpwincount")}\n" +
                                        "pvplosecount: ${rs.getInt("pvplosecount")}\n" +
                                        "pvpbreakout: ${rs.getInt("pvpbreakout")}\n" +
                                        "reactorcount: ${rs.getInt("reactorcount")}\n" +
                                        "bantime: ${rs.getString("bantime")}\n" +
                                        "translate: ${rs.getBoolean("translate")}\n" +
                                        "crosschat: ${rs.getBoolean("crosschat")}\n" +
                                        "colornick: ${rs.getBoolean("colornick")}\n" +
                                        "connected: ${rs.getBoolean("connected")}\n" +
                                        "connserver: ${rs.getString("connserver")}\n" +
                                        "permission: ${rs.getString("permission")}\n" +
                                        "mute: ${rs.getBoolean("mute")}\n" +
                                        "alert: ${rs.getBoolean("alert")}\n" +
                                        "udid: ${rs.getLong("udid")}\n" +
                                        "accountid: ${rs.getString("accountid")}"

                                val current = Main.playerCore[p.id]
                                if (!current.error) {
                                    datatext = "$datatext\n" +
                                            "== ${current.name} Player internal data ==\n" + " +" +
                                            "isLogin: ${current.login}\n" +
                                            "afk: ${Main.tool.longToTime(current.afk)}\n" +
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
    private fun setperm(arg: Array<String>) {
        val target = Groups.player.find { p: Playerc -> p.name() == arg[0] }
        val bundle = Bundle()
        val playerData: PlayerData
        if (target == null) {
            Log.warn(bundle["player.not-found"])
            return
        }
        for (p in Main.perm.perm) {
            if (p.name == arg[1]) {
                playerData = Main.playerCore[target.uuid()]
                playerData.permission = arg[1]
                Main.perm.user[playerData.uuid].asObject()["group"] = arg[1]
                Main.perm.update(true)
                Main.perm.reload(false)
                target.admin(Main.perm.isAdmin(playerData))
                Log.info(bundle["success"])
                target.sendMessage(Bundle(Main.playerCore[target.uuid()].locale).prefix("perm-changed"))
                return
            }
        }
        Log.warn(bundle["perm-group-not-found"])
    }

    private fun reload(arg: Array<String>) {
        Main.perm.reload(false)
        Main.perm.update(false)
        Log.info("plugin-reloaded")
    }
}