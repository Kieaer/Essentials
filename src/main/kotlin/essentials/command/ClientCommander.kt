package essentials.command

import arc.Core
import arc.math.Mathf
import arc.struct.Seq
import arc.util.CommandHandler
import arc.util.Strings
import essentials.Main.Companion.colorNickname
import essentials.Main.Companion.configs
import essentials.Main.Companion.discord
import essentials.Main.Companion.perm
import essentials.Main.Companion.playerCore
import essentials.Main.Companion.pluginData
import essentials.Main.Companion.pluginVars
import essentials.Main.Companion.tool
import essentials.Main.Companion.vars
import essentials.Main.Companion.vote
import essentials.Main.Companion.warpBorder
import essentials.PluginData
import essentials.features.Vote
import essentials.internal.Bundle
import essentials.internal.CrashReport
import mindustry.Vars
import mindustry.Vars.*
import mindustry.content.Blocks
import mindustry.content.UnitTypes
import mindustry.game.Gamemode
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Playerc
import mindustry.io.SaveIO
import mindustry.net.Packets
import mindustry.type.UnitType
import mindustry.world.Tile
import org.hjson.JsonObject
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.collections.Map as Map1

object ClientCommander {
    fun register(handler: CommandHandler){
        handler.register("alert", "Turn on/off alerts", ::alert)
        handler.register("ch", "Send chat to another server.", ::ch)
        handler.register("changepw", "<new_password> <new_password_repeat>", "Change account password", ::changepw)
        handler.register("chars", "<Text...>", "Make pixel texts", ::chars)
        handler.register("color", "Enable color nickname", ::color)
        handler.register("killall", "Kill all enemy units", ::killall)
        handler.register("help", "[page]", "Show command lists", ::help)
        handler.register("info", "Show your information", ::info)
        handler.register("warp", "<zone/block/count/total> [ip] [parameters...]", "Create a server-to-server warp zone.", ::warp)
        handler.register("kickall", "Kick all players", ::kickall)
        handler.register("kill", "[player]", "Kill player.", ::kill)
        handler.register("login", "<id> <password>", "Access your account", ::login)
        handler.register("maps", "[page]", "Show server maps", ::maps)
        handler.register("me", "<text...>", "broadcast * message", ::me)
        handler.register("motd", "Show server motd.", ::motd)
        handler.register("players", "Show players list", ::players)
        handler.register("save", "Auto rollback map early save", ::save)
        handler.register("r", "<player> [message]", "Send Direct message to target player", ::r)
        handler.register("reset", "<zone/count/total/block> [ip]", "Remove a server-to-server warp zone data.", ::reset)
        handler.register("router", "Router", ::router)
        handler.register("register", if (configs.passwordMethod.equals("password", ignoreCase = true)) "<accountid> <password>" else "", "Register account", ::register)
        handler.register("spawn", "<mob_name> <count> [team] [playerName]", "Spawn mob in player position", ::spawn)
        handler.register("setperm", "<player_name> <group>", "Set player permission", ::setperm)
        handler.register("spawn-core", "<small/normal/big>", "Make new core", ::spawncore)
        handler.register("setmech", "<Mech> [player]", "Set player mech", ::setmech)
        handler.register("status", "Show server status", ::status)
        handler.register("suicide", "Kill yourself.", ::suicide)
        handler.register("team", "<team_name>", "Change team", ::team)
        handler.register("tempban", "<player> <time> <reason>", "Temporarily ban player. time unit: 1 minute", ::tempban)
        handler.register("time", "Show server time", ::time)
        handler.register("tp", "<player>", "Teleport to other players", ::tp)
        handler.register("tpp", "<source> <target>", "Teleport to other players", ::tpp)
        handler.register("tppos", "<x> <y>", "Teleport to coordinates", ::tppos)
        handler.register("vote", "<mode> [parameter...]", "Voting system (Use /vote to check detail commands)", ::voteService)
        handler.register("weather", "<day/eday/night/enight>", "Change map light", ::weather)
        handler.register("mute", "<Player_name>", "Mute/unmute player", ::mute)
    }

    private fun alert(arg: Array<String>, player: Playerc) {
        if (!perm.check(player, "alert")) return
        val playerData = playerCore[player.uuid()]
        if (playerData.alert) {
            playerData.alert = false
            player.sendMessage(Bundle(playerData.locale).prefix("anti-grief.alert.disable"))
        } else {
            playerData.alert = true
            player.sendMessage(Bundle(playerData.locale).prefix("anti-grief.alert.enable"))
        }
    }
    private fun ch(arg: Array<String>, player: Playerc) {
        if (!perm.check(player, "ch")) return
        val playerData = playerCore[player.uuid()]
        playerData.crosschat = !playerData.crosschat
        player.sendMessage(Bundle(playerData.locale).prefix(if (playerData.crosschat) "player.crosschat.disable" else "player.crosschat.enabled"))
    }
    private fun changepw(arg: Array<String>, player: Playerc) {
        if (!perm.check(player, "changepw")) return
        val playerData = playerCore[player.uuid()]
        val bundle = Bundle(playerData.locale)
        if (!tool.checkPassword(player, playerData.accountid, arg[0], arg[1])) {
            player.sendMessage(bundle.prefix("system.account.need-new-password"))
            return
        }
        try {
            Class.forName("org.mindrot.jbcrypt.BCrypt")
            playerData.accountpw = BCrypt.hashpw(arg[0], BCrypt.gensalt(12))
            player.sendMessage(bundle.prefix("success"))
        } catch (e: ClassNotFoundException) {
            CrashReport(e)
        }
    }
    private fun chars(arg: Array<String>, player: Playerc) {
        if (!perm.check(player, "chars")) return
        if (world != null) tool.setTileText(world.tile(player.tileX(), player.tileY()), Blocks.copperWall, arg[0])
    }
    private fun color(arg: Array<String>, player: Playerc) {
        if (!perm.check(player, "color")) return
        val playerData = playerCore[player.uuid()]
        playerData.colornick = !playerData.colornick
        if (playerData.colornick) colorNickname.targets.add(player)
        player.sendMessage(Bundle(playerData.locale).prefix(if (playerData.colornick) "feature.colornick.enable" else "feature.colornick.disable"))
    }
    private fun killall(arg: Array<String>, player: Playerc) {
        if (!perm.check(player, "killall")) return
        for (a in Team.all.indices) Groups.unit.each { u: Unit -> u.kill() }
        player.sendMessage(Bundle(playerCore[player.uuid()].locale).prefix("success"))
    }
    private fun help(arg: Array<String>, player: Playerc) {
        if (arg.isNotEmpty() && !Strings.canParseInt(arg[0])) {
            player.sendMessage(Bundle(playerCore[player.uuid()].locale).prefix("page-number"))
            return
        }
        val temp = Seq<String>()
        for (a in 0 until Vars.netServer.clientCommands.commandList.size) {
            val command = Vars.netServer.clientCommands.commandList[a]
            if (perm.check(player, command.text) || command.text == "t" || command.text == "sync") {
                temp.add("[orange] /${command.text} [white]${command.paramText} [lightgray]- ${command.description}")
            }
        }
        val result = StringBuilder()
        val perpage = 8
        var page = if (arg.isNotEmpty()) Strings.parseInt(arg[0]) else 1
        val pages = Mathf.ceil(temp.size.toFloat() / perpage)
        page--
        if (page > pages || page < 0) {
            player.sendMessage("[scarlet]'page' must be a number between[orange] 1[] and[orange] $pages[scarlet].")
            return
        }
        result.append(Strings.format("[orange]-- Commands Page[lightgray] {0}[gray]/[lightgray]{1}[orange] --\n", page + 1, pages))
        for (a in perpage * page until (perpage * (page + 1)).coerceAtMost(temp.size)) {
            result.append(temp[a])
        }
        player.sendMessage(result.toString().substring(0, result.length - 1))
    }
    private fun info(arg: Array<String>, player: Playerc) {
        if (!perm.check(player, "info")) return
        val playerData = playerCore[player.uuid()]
        val bundle = Bundle(playerData.locale)
        val datatext = """
                [#DEA82A]${Bundle(playerData.locale)["player.info"]}[]
                [#2B60DE]====================================[]
                [green]${bundle["player.name"]}[] : ${player.name()}[white]
                [green]${bundle["player.uuid"]}[] : ${playerData.uuid}[white]
                [green]${bundle["player.country"]}[] : ${playerData.locale.getDisplayCountry(playerData.locale)}
                [green]${bundle["player.placecount"]}[] : ${playerData.placecount}
                [green]${bundle["player.breakcount"]}[] : ${playerData.breakcount}
                [green]${bundle["player.killcount"]}[] : ${playerData.killcount}
                [green]${bundle["player.deathcount"]}[] : ${playerData.deathcount}
                [green]${bundle["player.joincount"]}[] : ${playerData.joincount}
                [green]${bundle["player.kickcount"]}[] : ${playerData.kickcount}
                [green]${bundle["player.level"]}[] : ${playerData.level}
                [green]${bundle["player.reqtotalexp"]}[] : ${playerData.reqtotalexp}
                [green]${bundle["player.firstdate"]}[] : ${tool.longToDateTime(playerData.firstdate).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))}
                [green]${bundle["player.lastdate"]}[] : ${tool.longToDateTime(playerData.lastdate).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))}
                [green]${bundle["player.playtime"]}[] : ${tool.longToTime(playerData.playtime)}
                [green]${bundle["player.attackclear"]}[] : ${playerData.attackclear}
                [green]${bundle["player.pvpwincount"]}[] : ${playerData.pvpwincount}
                [green]${bundle["player.pvplosecount"]}[] : ${playerData.pvplosecount}
                [green]${bundle["player.pvpbreakout"]}[] : ${playerData.pvpbreakout}
                """.trimIndent()
        Call.infoMessage(player.con(), datatext)
    }
    private fun warp(arg: Array<String>, player: Playerc) {
        if (!perm.check(player, "warp")) return
        val playerData = playerCore[player.uuid()]
        val bundle = Bundle(playerData.locale)
        val types = arrayOf("zone", "block", "count", "total")
        if (!listOf(*types).contains(arg[0])) {
            player.sendMessage(bundle["system.warp.info"])
        } else {
            val type = arg[0]
            val x = player.tileX()
            val y = player.tileY()
            val name = state.map.name()
            val size: Int
            val clickable: Boolean
            var ip = ""
            var port = 6567
            if (arg.size > 1) {
                if (arg[1].contains(":")) {
                    val address = arg[1].split(":").toTypedArray()
                    ip = address[0]
                    port = address[1].toInt()
                } else {
                    ip = arg[1]
                }
            }
            val parameters: Array<String> = if (arg.size == 3) {
                arg[2].split(" ").toTypedArray()
            } else {
                arrayOf()
            }
            when (type) {
                "zone" ->                         //ip size clickable
                    if (parameters.size <= 1) {
                        player.sendMessage(bundle.prefix("system.warp.incorrect"))
                    } else {
                        try {
                            size = parameters[0].toInt()
                            clickable = java.lang.Boolean.parseBoolean(parameters[1])
                        } catch (ignored: NumberFormatException) {
                            player.sendMessage(bundle.prefix("system.warp.not-int"))
                            return
                        }
                        pluginData.warpzones.add(PluginData.WarpZone(name, world.tile(x, y).pos(), world.tile(x + size, y + size).pos(), clickable, ip, port))
                        warpBorder.thread.clear()
                        warpBorder.start()
                        player.sendMessage(bundle.prefix("system.warp.added"))
                    }
                "block" -> if (parameters.isEmpty()) {
                    player.sendMessage(bundle.prefix("system.warp.incorrect"))
                } else {
                    val t: Tile = world.tile(x, y)
                    pluginData.warpblocks.add(PluginData.WarpBlock(name, t.pos(), t.block().name, t.block().size, ip, port, arg[2]))
                    player.sendMessage(bundle.prefix("system.warp.added"))
                }
                "count" -> {
                    pluginData.warpcounts.add(PluginData.WarpCount(name, world.tile(x, y).pos(), ip, port, 0, 0))
                    player.sendMessage(bundle.prefix("system.warp.added"))
                }
                "total" -> {
                    pluginData.warptotals.add(PluginData.WarpTotal(name, world.tile(x, y).pos(), 0, 0))
                    player.sendMessage(bundle.prefix("system.warp.added"))
                }
                else -> player.sendMessage(bundle.prefix("command.invalid"))
            }
        }
    }
    private fun kickall(arg: Array<String>, player: Playerc) {
        if (!perm.check(player, "kickall")) return
        for (p in Groups.player) {
            if (player !== p) Call.kick(p.con, Packets.KickReason.kick)
        }
    }
    private fun kill(arg: Array<String>, player: Playerc) {
        if (!perm.check(player, "kill")) return
        if (arg.isEmpty()) {
            player.dead()
        } else {
            val other = Groups.player.find { p: Playerc -> p.name().equals(arg[0], ignoreCase = true) }
            if (other == null) {
                player.sendMessage(Bundle(playerCore[player.uuid()].locale).prefix("player.not-found"))
            } else {
                other.dead()
            }
        }
    }
    private fun login(arg: Array<String>, player: Playerc) {
        val playerData = playerCore[player.uuid()]
        if (configs.loginEnable) {
            if (playerData.error) {
                if (playerCore.login(arg[0], arg[1])) {
                    if (playerCore.playerLoad(player, arg[0])) {
                        player.sendMessage(Bundle(playerData.locale).prefix("system.login.success"))
                    }
                } else {
                    player.sendMessage("[green][EssentialPlayer] [scarlet]Login failed/로그인 실패!!")
                }
            } else {
                if (configs.passwordMethod == "mixed") {
                    if (playerCore.login(arg[0], arg[1])) Call.connect(player.con(), vars.serverIP, 7060)
                } else {
                    player.sendMessage("[green][EssentialPlayer] [scarlet]You're already logged./이미 로그인한 상태입니다.")
                }
            }
        } else {
            player.sendMessage(Bundle(playerData.locale).prefix("system.login.disabled"))
        }
    }
    private fun maps(arg: Array<String>, player: Playerc) {
        if (!perm.check(player, "maps")) return
        val maplist = maps.all()
        val build = StringBuilder()
        var page = if (arg.isNotEmpty()) Strings.parseInt(arg[0]) else 1
        val pages = Mathf.ceil(maplist.size.toFloat() / 6)
        page--
        if (page > pages || page < 0) {
            player.sendMessage("[scarlet]'page' must be a number between[orange] 1[] and[orange] $pages[scarlet].")
            return
        }
        build.append("[green]==[white] Server maps page ").append(page).append("/").append(pages).append(" [green]==[white]\n")
        for (a in 6 * page until (6 * (page + 1)).coerceAtMost(maplist.size)) {
            build.append("[gray]").append(a).append("[] ").append(maplist[a].name()).append("\n")
        }
        player.sendMessage(build.toString())
    }
    private fun me(arg: Array<String>, player: Playerc) {
        if (!perm.check(player, "me")) return
        Call.sendMessage("[orange]*[] " + player.name() + "[white] : " + arg[0])
    }
    private fun motd(arg: Array<String>, player: Playerc) {
        if (!perm.check(player, "motd")) return
        val motd = tool.getMotd(playerCore[player.uuid()].locale)
        val count = motd.split("\r\n|\r|\n").toTypedArray().size
        if (count > 10) {
            Call.infoMessage(player.con(), motd)
        } else {
            player.sendMessage(motd)
        }
    }
    private fun players(arg: Array<String>, player: Playerc) {
        if (!perm.check(player, "players")) return
        val build = StringBuilder()
        var page = if (arg.isNotEmpty()) Strings.parseInt(arg[0]) else 1
        val pages = Mathf.ceil(Groups.player.size().toFloat() / 6)
        page--
        if (page > pages || page < 0) {
            player.sendMessage("[scarlet]'page' must be a number between[orange] 1[] and[orange] $pages[scarlet].")
            return
        }
        build.append("[green]==[white] Players list page ").append(page).append("/").append(pages).append(" [green]==[white]\n")
        for (a in 6 * page until (6 * (page + 1)).coerceAtMost(Groups.player.size())) {
            build.append("[gray]").append(Groups.player.getByID(a).id).append("[] ").append(Groups.player.getByID(a).name).append("\n")
        }
        player.sendMessage(build.toString())
    }
    private fun save(arg: Array<String>, player: Playerc) {
        if (!perm.check(player, "save")) return
        val file = Vars.saveDirectory.child(configs.slotNumber.toString() + "." + Vars.saveExtension)
        SaveIO.save(file)
        player.sendMessage(Bundle(playerCore[player.uuid()].locale).prefix("system.map-saved"))
    }
    private fun r(arg: Array<String>, player: Playerc) {
        if (!perm.check(player, "r")) return
        val playerData = playerCore[player.uuid()]
        val bundle = Bundle(playerData.locale)
        val target = Groups.player.find { p: Playerc -> p.name().contains(arg[0]) }
        if (target != null) {
            target.sendMessage("[orange]DM [sky]" + playerData.name + " [green]>> [white]" + arg[1])
            player.sendMessage("[cyan]DM [sky]" + target.name + " [green]>> [white]" + arg[1])
        } else {
            player.sendMessage(bundle["player.not-found"])
        }
    }
    private fun reset(arg: Array<String>, player: Playerc) {
        if (!perm.check(player, "reset")) return
        val playerData = playerCore[player.uuid()]
        val bundle = Bundle(playerData.locale)
        when (arg[0]) {
            "zone" -> {
                var a = 0
                while (a < pluginData.warpzones.size) {
                    if (arg.size != 2) {
                        player.sendMessage(bundle.prefix("no-parameter"))
                        return
                    }
                    if (arg[1] == pluginData.warpzones[a].ip) {
                        pluginData.warpzones.remove(a)
                        for (value in warpBorder.thread) {
                            value.interrupt()
                        }
                        warpBorder.thread.clear()
                        warpBorder.start()
                        player.sendMessage(bundle.prefix("success"))
                        break
                    }
                    a++
                }
            }
            "count" -> {
                pluginData.warpcounts.clear()
                player.sendMessage(bundle.prefix("system.warp.reset", "count"))
            }
            "total" -> {
                pluginData.warptotals.clear()
                player.sendMessage(bundle.prefix("system.warp.reset", "total"))
            }
            "block" -> {
                pluginData.warpblocks.clear()
                player.sendMessage(bundle.prefix("system.warp.reset", "block"))
            }
            else -> player.sendMessage(bundle.prefix("command.invalid"))
        }
    }
    private fun router(arg: Array<String>, player: Playerc) {
        if (!perm.check(player, "router")) return
        Thread {
            val zero = arrayOf("""
    [stat][#404040][]
    [stat][#404040][]
    [stat][#404040]
    [stat][#404040][]
    [#404040][stat]
    [stat][#404040][]
    [stat][#404040][]
    [stat][#404040][][#404040]
    """.trimIndent(),
                    """
                            [stat][#404040][]
                            [stat][#404040]
                            [stat][#404040][]
                            [#404040][stat]
                            [stat][#404040][]
                            [stat][#404040][]
                            [stat][#404040]
                            [stat][#404040][][#404040][]
                            """.trimIndent(),
                    """
                            [stat][#404040][][#404040]
                            [stat][#404040][]
                            [#404040][stat]
                            [stat][#404040][]
                            [stat][#404040][]
                            [stat][#404040]
                            [stat][#404040][]
                            [#404040][stat][][stat]
                            """.trimIndent(),
                    """
                            [stat][#404040][][#404040][]
                            [#404040][stat]
                            [stat][#404040][]
                            [stat][#404040][]
                            [stat][#404040]
                            [stat][#404040][]
                            [#404040][stat]
                            [stat][#404040][]
                            """.trimIndent(),
                    """
                            [#404040][stat][][stat]
                            [stat][#404040][]
                            [stat][#404040][]
                            [stat][#404040]
                            [stat][#404040][]
                            [#404040][stat]
                            [stat][#404040][]
                            [stat][#404040][]
                            
                            """.trimIndent())
            val loop = arrayOf("""
    [#6B6B6B][stat][#6B6B6B]
    [stat][#404040][]
    [stat][#404040]
    [stat][#404040][]
    [#404040][]
    [stat][#404040][]
    [stat][#404040][]
    [#6B6B6B][stat][#404040][][#6B6B6B]
    
    """.trimIndent(),
                    """
                            [#6B6B6B][stat][#6B6B6B]
                            [#6B6B6B][stat][#404040][][#6B6B6B]
                            [stat][#404040][]
                            [#404040][]
                            [stat][#404040][]
                            [stat][#404040][]
                            [#6B6B6B][stat][#404040][][#6B6B6B]
                            [#6B6B6B][stat][#6B6B6B]
                            """.trimIndent(),
                    """
                            [#6B6B6B][#585858][stat][][#6B6B6B]
                            [#6B6B6B][#828282][stat][#404040][][][#6B6B6B]
                            [#585858][stat][#404040][][#585858]
                            [stat][#404040][]
                            [stat][#404040][]
                            [#585858][stat][#404040][][#585858]
                            [#6B6B6B][stat][#404040][][#828282][#6B6B6B]
                            [#6B6B6B][#585858][stat][][#6B6B6B]
                            """.trimIndent(),
                    """
                            [#6B6B6B][#585858][#6B6B6B]
                            [#6B6B6B][#828282][stat][][#6B6B6B]
                            [#585858][#6B6B6B][stat][#404040][][#828282][#585858]
                            [#585858][stat][#404040][][#585858]
                            [#585858][stat][#404040][][#585858]
                            [#585858][#6B6B6B][stat][#404040][][#828282][#585858]
                            [#6B6B6B][stat][][#828282][#6B6B6B]
                            [#6B6B6B][#585858][#6B6B6B]
                            """.trimIndent(),
                    """
                            [#6B6B6B][#585858][#6B6B6B]
                            [#6B6B6B][#828282][#6B6B6B]
                            [#585858][#6B6B6B][stat][][#828282][#585858]
                            [#585858][#6B6B6B][stat][#404040][][#828282][#585858]
                            [#585858][#6B6B6B][stat][#404040][][#828282][#585858]
                            [#585858][#6B6B6B][stat][][#828282][#585858]
                            [#6B6B6B][#828282][#6B6B6B]
                            [#6B6B6B][#585858][#6B6B6B]
                            """.trimIndent(),
                    """
                            [#6B6B6B][#585858][#6B6B6B]
                            [#6B6B6B][#828282][#6B6B6B]
                            [#585858][#6B6B6B][#828282][#585858]
                            [#585858][#6B6B6B][stat][#6B6B6B][#828282][#585858]
                            [#585858][#6B6B6B][stat][#6B6B6B][#828282][#585858]
                            [#585858][#6B6B6B][#828282][#585858]
                            [#6B6B6B][#828282][#6B6B6B]
                            [#6B6B6B][#585858][#6B6B6B]
                            """.trimIndent(),
                    """
                            [#6B6B6B][#585858][#6B6B6B]
                            [#6B6B6B][#828282][#6B6B6B]
                            [#585858][#6B6B6B][#828282][#585858]
                            [#585858][#6B6B6B][#828282][#6B6B6B][#828282][#585858]
                            [#585858][#6B6B6B][#828282][#6B6B6B][#828282][#585858]
                            [#585858][#6B6B6B][#828282][#585858]
                            [#6B6B6B][#828282][#6B6B6B]
                            [#6B6B6B][#585858][#6B6B6B]
                            """.trimIndent())
            try {
                while (!player.isNull) {
                    for (d in loop) {
                        player.name(d)
                        Thread.sleep(500)
                    }
                    Thread.sleep(5000)
                    for (i in loop.indices.reversed()) {
                        player.name(loop[i])
                        Thread.sleep(500)
                    }
                    for (d in zero) {
                        player.name(d)
                        Thread.sleep(500)
                    }
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }.start()
    }
    private fun register(arg: Array<String>, player: Playerc) {
        if (configs.loginEnable) {
            when (configs.passwordMethod) {
                "discord" -> {
                    player.sendMessage("""Join discord and use !register command!${configs.discordLink}""".trimIndent())
                    if (!discord.pins.containsKey(player.name())) discord.queue(player)
                }
                "password" -> {
                    val lc = tool.getGeo(player)
                    val hash = BCrypt.hashpw(arg[1], BCrypt.gensalt(12))
                    val register = playerCore.register(player.name(), player.uuid(), lc.displayCountry, lc.toString(), lc.displayLanguage, true, pluginVars.serverIP, "default", 0L, arg[0], hash, false)
                    if (register) {
                        playerCore.playerLoad(player, null)
                        player.sendMessage(Bundle(playerCore[player.uuid()].locale).prefix("register-success"))
                    } else {
                        player.sendMessage("[green][Essentials] [scarlet]Register failed/계정 등록 실패!")
                    }
                }
                else -> {
                    val lc = tool.getGeo(player)
                    val hash = BCrypt.hashpw(arg[1], BCrypt.gensalt(12))
                    val register = playerCore.register(player.name(), player.uuid(), lc.displayCountry, lc.toString(), lc.displayLanguage, true, pluginVars.serverIP, "default", 0L, arg[0], hash, false)
                    if (register) {
                        playerCore.playerLoad(player, null)
                        player.sendMessage(Bundle(playerCore[player.uuid()].locale).prefix("register-success"))
                    } else {
                        player.sendMessage("[green][Essentials] [scarlet]Register failed/계정 등록 실패!")
                    }
                }
            }
        } else {
            player.sendMessage(Bundle(configs.locale).prefix("system.login.disabled"))
        }
    }
    private fun spawn(arg: Array<String>, player: Playerc) {
        if (!perm.check(player, "spawn")) return
        val playerData = playerCore[player.uuid()]
        val bundle = Bundle(playerData.locale)
        val targetUnit = tool.getUnitByName(arg[0])
        if (targetUnit == null) {
            player.sendMessage(bundle.prefix("system.mob.not-found"))
            return
        }
        val count: Int
        count = try {
            arg[1].toInt()
        } catch (e: NumberFormatException) {
            player.sendMessage(bundle.prefix("system.mob.not-number"))
            return
        }
        if (configs.spawnLimit == count) {
            player.sendMessage(bundle.prefix("spawn-limit"))
            return
        }
        var targetPlayer = if (arg.size > 3) tool.findPlayer(arg[3]) else player
        if (targetPlayer == null) {
            player.sendMessage(bundle.prefix("player.not-found"))
            targetPlayer = player
        }
        var targetTeam = if (arg.size > 2) tool.getTeamByName(arg[2]) else targetPlayer.team()
        if (targetTeam == null) {
            player.sendMessage(bundle.prefix("team-not-found"))
            targetTeam = targetPlayer.team()
        }
        var i = 0
        while (count > i) {
            val baseUnit = targetUnit.create(targetTeam)
            baseUnit[targetPlayer.getX()] = targetPlayer.getY()
            baseUnit.add()
            i++
        }
    }
    private fun setperm(arg: Array<String>, player: Playerc) {
        if (!perm.check(player, "setperm")) return
        val playerData = playerCore[player.uuid()]
        val bundle = Bundle(playerData.locale)
        val target = Groups.player.find { p: Playerc -> p.name() == arg[0] }
        if (target == null) {
            player.sendMessage(bundle.prefix("player.not-found"))
            return
        }
        for (permission in perm.perm) {
            if (permission.name == arg[1]) {
                val data = playerCore[target.uuid()]
                data.permission = arg[1]
                perm.user[data.uuid].asObject()["group"] = arg[1]
                perm.update(true)
                player.sendMessage(bundle.prefix("success"))
                target.sendMessage(Bundle(data.locale).prefix("perm-changed"))
                return
            }
        }
        player.sendMessage(Bundle(playerData.locale).prefix("perm-group-not-found"))
    }
    private fun spawncore(arg: Array<String>, player: Playerc) {
        if (!perm.check(player, "spawn-core")) return
        var core = Blocks.coreShard
        when (arg[0]) {
            "normal" -> core = Blocks.coreFoundation
            "big" -> core = Blocks.coreNucleus
        }
        if(player.tileOn().breakable()) {
            player.tileOn().setBlock(core, player.team())
            Call.constructFinish(player.tileOn(), core, player.unit(), 0.toByte(), player.team(), false)
        }
    }
    private fun setmech(arg: Array<String>, player: Playerc) {
        if (!perm.check(player, "setmech")) return
        val playerData = playerCore[player.uuid()]
        val bundle = Bundle(playerData.locale)
        var mech : UnitType = UnitTypes.mace
        when (arg[0]) {
            "mace" -> mech = UnitTypes.mace
            "dagger" -> mech = UnitTypes.dagger
            "crawler" -> mech = UnitTypes.crawler
            "fortress" -> mech = UnitTypes.fortress
            "scepter" -> mech = UnitTypes.scepter
            "reign" -> mech = UnitTypes.reign
            "nova" -> mech = UnitTypes.nova
            "pulsar" -> mech = UnitTypes.pulsar
            "quasar" -> mech = UnitTypes.quasar
            "atrax" -> mech = UnitTypes.atrax
            "spiroct" -> mech = UnitTypes.spiroct
            "arkyid" -> mech = UnitTypes.arkyid
            "toxopid" -> mech = UnitTypes.toxopid
            "flare" -> mech = UnitTypes.flare
            "eclipse" -> mech = UnitTypes.eclipse
            "horizon" -> mech = UnitTypes.horizon
            "zenith" -> mech = UnitTypes.zenith
            "antumbra" -> mech = UnitTypes.antumbra
            "mono" -> mech = UnitTypes.mono
            "poly" -> mech = UnitTypes.poly
            "mega" -> mech = UnitTypes.mega
            "alpha" -> mech = UnitTypes.alpha
            "beta" -> mech = UnitTypes.beta
            "gamma" -> mech = UnitTypes.gamma
            "risso" -> mech = UnitTypes.risso
            "minke" -> mech = UnitTypes.minke
            "bryde" -> mech = UnitTypes.bryde
            "block" -> mech = UnitTypes.block
        }
        if (arg.size == 1) {
            for (p in Groups.player) {
                p.unit(mech.create(p.team()))
            }
        } else {
            val target = Groups.player.find { p: Playerc -> p.name() == arg[1] }
            if (target == null) {
                player.sendMessage(bundle.prefix("player.not-found"))
                return
            }
            target.unit(mech.create(target.team()))
        }
        player.sendMessage(bundle.prefix("success"))
    }
    private fun status(arg: Array<String>, player: Playerc) {
        if (!perm.check(player, "status")) return
        val playerData = playerCore[player.uuid()]
        val bundle = Bundle(playerData.locale)
        player.sendMessage(bundle.prefix("server.status"))
        player.sendMessage("[#2B60DE]========================================[]")
        val fps = Core.graphics.framesPerSecond
        val bans = netServer.admins.banned.size
        val ipbans = netServer.admins.bannedIPs.size
        val bancount = bans + ipbans
        val playtime = tool.longToTime(vars.playtime)
        val uptime = tool.longToTime(vars.uptime)
        player.sendMessage(bundle["server.status.result", fps, Groups.player.size(), bancount, bans, ipbans, playtime, uptime, vars.pluginVersion])
        val result = JsonObject()
        for (p in vars.playerData) {
            if (result[p.locale.getDisplayCountry(playerData.locale)] == null) {
                result.add(p.locale.getDisplayCountry(playerData.locale), 1)
            } else {
                result[p.locale.getDisplayCountry(playerData.locale)] = result[p.locale.getDisplayCountry(playerData.locale)].asInt() + 1
            }
        }
        val s = StringBuilder()
        for (m in result) {
            val d = """
                    ${m.name}: ${m.value}
                    
                    """.trimIndent()
            s.append(d)
        }
        player.sendMessage(if (s.isNotEmpty() && s.last() == (',')) s.substring(0, s.length - 1) else s.toString())
    }
    private fun suicide(arg: Array<String>, player: Playerc) {
        if (!perm.check(player, "suicide")) return
        player.dead()
        if (Groups.player != null && Groups.player.size() > 0) {
            tool.sendMessageAll("suicide", player.name())
        }
    }
    private fun team(arg: Array<String>, player: Playerc) {
        if (!perm.check(player, "team")) return
        val playerData = playerCore[player.uuid()]
        when (arg[0]) {
            "derelict" -> player.team(Team.derelict)
            "sharded" -> player.team(Team.sharded)
            "crux" -> player.team(Team.crux)
            "green" -> player.team(Team.green)
            "purple" -> player.team(Team.purple)
            "blue" -> player.team(Team.blue)
            else -> player.sendMessage(Bundle(playerData.locale).prefix("command.team"))
        }
    }
    private fun tempban(arg: Array<String>, player: Playerc) {
        if (!perm.check(player, "tempban")) return
        val playerData = playerCore[player.uuid()]
        var other: Playerc? = null
        for (p in Groups.player) {
            val result = p.name.contains(arg[0])
            if (result) {
                other = p
            }
        }
        if (other != null) {
            val bantime = System.currentTimeMillis() + 1000 * 60 * (arg[1].toInt())
            playerCore.ban(other, bantime, arg[2])
            Call.kick(other.con(), "Temp kicked")
            for (a in 0 until Groups.player.size()) {
                val current = Groups.player.getByID(a)
                val target = playerCore[current.uuid()]
                current.sendMessage(Bundle(target.locale).prefix("account.ban.temp", other.name(), player.name()))
            }
        } else {
            player.sendMessage(Bundle(playerData.locale).prefix("player.not-found"))
        }
    }
    private fun time(arg: Array<String>, player: Playerc) {
        if (!perm.check(player, "time")) return
        val playerData = playerCore[player.uuid()]
        val now = LocalDateTime.now()
        val dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss")
        val nowString = now.format(dateTimeFormatter)
        player.sendMessage(Bundle(playerData.locale).prefix("servertime", nowString))
    }
    private fun tp(arg: Array<String>, player: Playerc) {
        if (!perm.check(player, "tp")) return
        val playerData = playerCore[player.uuid()]
        val bundle = Bundle(playerData.locale)
        /*// TODO 모바일 유저 확인
        if (player.isMobile) {
            player.sendMessage(bundle.prefix("tp-not-support"))
            return
        }*/
        var other: Playerc? = null
        for (p in Groups.player) {
            val result = p.name.contains(arg[0]!!)
            if (result) {
                other = p
            }
        }
        if (other == null) {
            player.sendMessage(bundle.prefix("player.not-found"))
            return
        }
        player.set(other!!.getX(), other!!.getY())
    }
    private fun tpp(arg: Array<String>, player: Playerc) {
        if (!perm.check(player, "tpp")) return
        val playerData = playerCore[player.uuid()]
        var other1: Playerc? = null
        var other2: Playerc? = null
        for (p in Groups.player) {
            val result1 = p.name.contains(arg[0]!!)
            if (result1) {
                other1 = p
            }
            val result2 = p.name.contains(arg[1]!!)
            if (result2) {
                other2 = p
            }
        }
        if (other1 == null || other2 == null) {
            player.sendMessage(Bundle(playerData.locale).prefix("player.not-found"))
            return
        }
        /*// TODO 모바일 유저도 tp 되는지 확인
        if (!other1.isMobile || !other2.isMobile) {
            other1.set(other2.x, other2.y)
        } else {*/
        player.sendMessage(Bundle(playerData.locale).prefix("tp-ismobile"))
        //}
    }
    private fun tppos(arg: Array<String>, player: Playerc) {
        if (!perm.check(player, "tppos")) return
        val playerData = playerCore[player.uuid()]
        val x: Int
        val y: Int
        try {
            x = arg[0].toInt()
            y = arg[1].toInt()
        } catch (ignored: Exception) {
            player.sendMessage(Bundle(playerData.locale).prefix("tp-not-int"))
            return
        }
        player.set(x.toFloat(), y.toFloat())
    }
    /*handler.<Player>register("tr", "Enable/disable Translate all chat", (arg, player) -> {
        if (!perm.check(player, "tr")) return;
        PlayerData playerData = playerCore.get(player.uuid);
        playerCore.get(player.uuid).translate(!playerData.translate());
        player.sendMessage(new Bundle(playerData.locale).prefix(playerData.translate() ? "translate" : "translate-disable", player.name));
    });*/
        private fun voteService(arg: Array<String>, player: Playerc) {
            if (!perm.check(player, "vote") || Core.settings.getBool("isLobby")) return
            val playerData = playerCore[player.uuid()]
            val bundle = Bundle(playerData.locale)
            if (vote.service.process) {
                player.sendMessage(bundle.prefix("vote.in-processing"))
                return
            }
            vote.player = player
            when (arg[0]) {
                "kick" -> {
                    // vote kick <player name>
                    if (arg.size < 2) {
                        player.sendMessage(bundle["no-parameter"])
                        return
                    }
                    var target = Groups.player.find { p: Playerc -> p.name().equals(arg[1], ignoreCase = true) }
                    try {
                        if (target == null) target = Groups.player.find { p: Playerc -> p.id() == arg[1].toInt() }
                    } catch (e: NumberFormatException) {
                        player.sendMessage(bundle.prefix("player.not-found"))
                        return
                    }
                    when {
                        target == null -> {
                            player.sendMessage(bundle.prefix("player.not-found"))
                            return
                        }
                        target.admin -> {
                            player.sendMessage(bundle.prefix("vote.target-admin"))
                            return
                        }
                        target === player -> {
                            player.sendMessage(bundle.prefix("vote.target-own"))
                            return
                        }


                        // 강퇴 투표
                        else -> {
                            vote.type = Vote.VoteType.kick
                            vote.parameters = arrayOf(target, arg[1])
                        }
                    }

                }
                "map" -> {
                    // vote map <map name>
                    if (arg.size < 2) {
                        player.sendMessage(bundle["no-parameter"])
                        return
                    }

                    // 맵 투표
                    var world = maps.all().find { map: Map1 -> map.name().equals(arg[1].replace('_', ' '), ignoreCase = true) || map.name().equals(arg[1], ignoreCase = true) }
                    if (world == null) {
                        try {
                            world = maps.all()[arg[1].toInt()]
                            if (world != null) {
                                vote.type = Vote.VoteType.map
                                vote.parameters = arrayOf(world)
                            } else {
                                player.sendMessage(bundle.prefix("vote.map.not-found"))
                            }
                        } catch (ignored: NumberFormatException) {
                            player.sendMessage(bundle.prefix("vote.map.not-found"))
                        }
                    } else {
                        vote.type = Vote.VoteType.map
                        vote.parameters = arrayOf(world)
                    }
                }
                "gameover" -> {
                    // vote gameover
                    vote.type = Vote.VoteType.gameover
                    vote.parameters = arrayOf()
                }
                "rollback" ->                         // vote rollback
                    if (configs.rollback) {
                        vote.type = Vote.VoteType.rollback
                        vote.parameters = arrayOf()
                    } else {
                        player.sendMessage(bundle["vote.rollback.disabled"])
                    }
                "gamemode" -> {
                    // vote gamemode <gamemode>
                    if (arg.size < 2) {
                        player.sendMessage(bundle["no-parameter"])
                        return
                    }
                    try {
                        vote.type = Vote.VoteType.gamemode
                        vote.parameters = arrayOf(Gamemode.valueOf(arg[1]))
                    } catch (e: IllegalArgumentException) {
                        player.sendMessage(bundle.prefix("vote.wrong-gamemode"))
                    }
                }
                "skipwave" -> {
                    // vote skipwave <wave>
                    if (arg.size != 2) {
                        player.sendMessage(bundle["no-parameter"])
                        return
                    }
                    vote.type = Vote.VoteType.skipwave
                    vote.parameters = arrayOf(arg[1])
                }
                else -> {
                    when (arg[0]) {
                        "gamemode" -> player.sendMessage(bundle.prefix("vote.list.gamemode"))
                        "map" -> player.sendMessage(bundle.prefix("vote.map.not-found"))
                        "kick" -> player.sendMessage(bundle.prefix("vote.kick.parameter"))
                        else -> player.sendMessage(bundle.prefix("vote.list"))
                    }
                    return
                }
            }
            vote.pause = false
        }
    }
    private fun weather(arg: Array<String>, player: Playerc) {
        if (!perm.check(player, "weather")) return
        // Command idea from Minecraft EssentialsX and Quezler's plugin!
        // Useful with the Quezler's plugin.
        state.rules.lighting = true
        when (arg[0]) {
            "day" -> state.rules.ambientLight.a = 0f
            "eday" -> state.rules.ambientLight.a = 0.3f
            "night" -> state.rules.ambientLight.a = 0.7f
            "enight" -> state.rules.ambientLight.a = 0.85f
            else -> return
        }
        Call.setRules(Vars.state.rules)
        player.sendMessage(Bundle(playerCore[player.uuid()].locale).prefix("success"))
    }
    private fun mute(arg: Array<String>, player: Playerc) {
        if (!perm.check(player, "mute")) return
        val other = Groups.player.find { p: Playerc -> p.name().equals(arg[0], ignoreCase = true) }
        val playerData = playerCore[player.uuid()]
        if (other == null) {
            player.sendMessage(Bundle(playerData.locale).prefix("player.not-found"))
        } else {
            val target = playerCore[other.uuid()]
            target.mute = !target.mute
            player.sendMessage(Bundle(target.locale).prefix(if (target.mute) "player.muted" else "player.unmute", target.name))
        }
    }
}