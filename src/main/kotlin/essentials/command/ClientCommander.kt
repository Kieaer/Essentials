package essentials.command

import arc.Core
import arc.math.Mathf
import arc.struct.Seq
import arc.util.CommandHandler
import arc.util.Strings
import essentials.Config
import essentials.PlayerCore
import essentials.PluginData
import essentials.PluginVars
import essentials.features.ColorNickname
import essentials.features.Discord
import essentials.features.Permissions
import essentials.features.Vote
import essentials.internal.Bundle
import essentials.internal.CrashReport
import essentials.internal.Tool
import essentials.thread.WarpBorder
import mindustry.Vars.*
import mindustry.content.Blocks
import mindustry.content.UnitTypes
import mindustry.game.Gamemode
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Playerc
import mindustry.gen.Unit
import mindustry.io.SaveIO
import mindustry.maps.Map
import mindustry.net.Packets
import mindustry.type.UnitType
import mindustry.world.Tile
import org.hjson.JsonObject
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

object ClientCommander {
    lateinit var commands: CommandHandler

    fun register(handler: CommandHandler) {
        handler.removeCommand("votekick")
        handler.removeCommand("vote")
        handler.removeCommand("help")

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
        handler.register("players", "[page]", "Show players list", ::players)
        handler.register("save", "Auto rollback map early save", ::save)
        handler.register("r", "<player> [message]", "Send Direct message to target player", ::r)
        handler.register("reset", "<zone/count/total/block> [ip]", "Remove a server-to-server warp zone data.", ::reset)
        handler.register("router", "Router", ::router)
        handler.register("register", if (Config.passwordMethod.equals("password", ignoreCase = true)) "<accountid> <password>" else "", "Register account", ::reg)
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

        commands = handler
    }

    private fun alert(arg: Array<String>, player: Playerc) {
        if (!Permissions.check(player, "alert")) return
        val playerData = PlayerCore[player.uuid()]
        if (playerData.alert) {
            playerData.alert = false
            player.sendMessage(Bundle(playerData.locale).prefix("anti-grief.alert.disable"))
        } else {
            playerData.alert = true
            player.sendMessage(Bundle(playerData.locale).prefix("anti-grief.alert.enable"))
        }
    }

    private fun ch(arg: Array<String>, player: Playerc) {
        if (!Permissions.check(player, "ch")) return
        val playerData = PlayerCore[player.uuid()]
        playerData.crosschat = !playerData.crosschat
        player.sendMessage(Bundle(playerData.locale).prefix(if (playerData.crosschat) "player.crosschat.disable" else "player.crosschat.enabled"))
    }

    private fun changepw(arg: Array<String>, player: Playerc) {
        if (!Permissions.check(player, "changepw")) return
        val playerData = PlayerCore[player.uuid()]
        val bundle = Bundle(playerData.locale)
        if (!Tool.checkPassword(player, playerData.accountid, arg[0], arg[1])) {
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
        if (!Permissions.check(player, "chars")) return
        if (world != null) Tool.setTileText(world.tile(player.tileX(), player.tileY()), Blocks.copperWall, arg[0])
    }

    private fun color(arg: Array<String>, player: Playerc) {
        if (!Permissions.check(player, "color")) return
        val playerData = PlayerCore[player.uuid()]
        playerData.colornick = !playerData.colornick
        if (playerData.colornick) ColorNickname.targets.add(player)
        player.sendMessage(Bundle(playerData.locale).prefix(if (playerData.colornick) "feature.colornick.enable" else "feature.colornick.disable"))
    }

    private fun killall(arg: Array<String>, player: Playerc) {
        if (!Permissions.check(player, "killall")) return
        for (a in Team.all.indices) Groups.unit.each { u: Unit -> u.kill() }
        player.sendMessage(Bundle(PlayerCore[player.uuid()].locale).prefix("success"))
    }

    private fun help(arg: Array<String>, player: Playerc) {
        if (arg.isNotEmpty() && !Strings.canParseInt(arg[0])) {
            player.sendMessage(Bundle(PlayerCore[player.uuid()].locale).prefix("page-number"))
            return
        }
        val temp = Seq<String>()
        for (a in 0 until netServer.clientCommands.commandList.size) {
            val command = netServer.clientCommands.commandList[a]
            if (Permissions.check(player, command.text)) {
                temp.add("[orange] /${command.text} [white]${command.paramText} [lightgray]- ${command.description}\n")
            }
        }
        val result = StringBuilder()
        val perpage = 8
        var page = if (arg.isNotEmpty()) {
            abs(Strings.parseInt(arg[0]))
        } else 1
        val pages = Mathf.ceil(temp.size.toFloat() / perpage)
        page--

        if (page >= pages || page < 0) {
            player.sendMessage("[scarlet]'page' must be a number between[orange] 1[] and[orange] ${pages}[scarlet].")
            return
        }

        result.append(Strings.format("[orange]-- Commands Page[lightgray] ${page + 1}[gray]/[lightgray]${pages}[orange] --\n"))
        for (a in perpage * page until (perpage * (page + 1)).coerceAtMost(temp.size)) {
            result.append(temp[a])
        }
        player.sendMessage(result.toString().substring(0, result.length - 1))
    }

    private fun info(arg: Array<String>, player: Playerc) {
        if (!Permissions.check(player, "info")) return
        val playerData = PlayerCore[player.uuid()]
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
                [green]${bundle["player.firstdate"]}[] : ${Tool.longToDateTime(playerData.firstdate).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))}
                [green]${bundle["player.lastdate"]}[] : ${Tool.longToDateTime(playerData.lastdate).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))}
                [green]${bundle["player.playtime"]}[] : ${Tool.longToTime(playerData.playtime)}
                [green]${bundle["player.attackclear"]}[] : ${playerData.attackclear}
                [green]${bundle["player.pvpwincount"]}[] : ${playerData.pvpwincount}
                [green]${bundle["player.pvplosecount"]}[] : ${playerData.pvplosecount}
                [green]${bundle["player.pvpbreakout"]}[] : ${playerData.pvpbreakout}
                """.trimIndent()
        Call.infoMessage(player.con(), datatext)
    }

    private fun warp(arg: Array<String>, player: Playerc) {
        if (!Permissions.check(player, "warp")) return
        val playerData = PlayerCore[player.uuid()]
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
                        PluginData.warpzones.add(PluginData.WarpZone(name, world.tile(x, y).pos(), world.tile(x + size, y + size).pos(), clickable, ip, port))
                        WarpBorder.thread.clear()
                        WarpBorder.start()
                        player.sendMessage(bundle.prefix("system.warp.added"))
                    }
                "block" -> if (parameters.isEmpty()) {
                    player.sendMessage(bundle.prefix("system.warp.incorrect"))
                } else {
                    val t: Tile = world.tile(x, y)
                    PluginData.warpblocks.add(PluginData.WarpBlock(name, t.pos(), t.block().name, t.block().size, ip, port, arg[2]))
                    player.sendMessage(bundle.prefix("system.warp.added"))
                }
                "count" -> {
                    PluginData.warpcounts.add(PluginData.WarpCount(name, world.tile(x, y).pos(), ip, port, 0, 0))
                    player.sendMessage(bundle.prefix("system.warp.added"))
                }
                "total" -> {
                    PluginData.warptotals.add(PluginData.WarpTotal(name, world.tile(x, y).pos(), 0, 0))
                    player.sendMessage(bundle.prefix("system.warp.added"))
                }
                else -> player.sendMessage(bundle.prefix("command.invalid"))
            }
        }
    }

    private fun kickall(arg: Array<String>, player: Playerc) {
        if (!Permissions.check(player, "kickall")) return
        for (p in Groups.player) {
            if (player !== p) Call.kick(p.con, Packets.KickReason.kick)
        }
    }

    private fun kill(arg: Array<String>, player: Playerc) {
        if (!Permissions.check(player, "kill")) return
        if (arg.isEmpty()) {
            player.dead()
        } else {
            val other = Groups.player.find { p: Playerc -> p.name().equals(arg[0], ignoreCase = true) }
            if (other == null) {
                player.sendMessage(Bundle(PlayerCore[player.uuid()].locale).prefix("player.not-found"))
            } else {
                other.dead()
            }
        }
    }

    private fun login(arg: Array<String>, player: Playerc) {
        val playerData = PlayerCore[player.uuid()]
        if (Config.loginEnable) {
            if (playerData.error) {
                if (PlayerCore.login(arg[0], arg[1])) {
                    if (PlayerCore.playerLoad(player, arg[0])) {
                        player.sendMessage(Bundle(playerData.locale).prefix("system.login.success"))
                    }
                } else {
                    player.sendMessage("[green][EssentialPlayer] [scarlet]Login failed/로그인 실패!!")
                }
            } else {
                if (Config.passwordMethod == "mixed") {
                    if (PlayerCore.login(arg[0], arg[1])) Call.connect(player.con(), PluginVars.serverIP, 7060)
                } else {
                    player.sendMessage("[green][EssentialPlayer] [scarlet]You're already logged./이미 로그인한 상태입니다.")
                }
            }
        } else {
            player.sendMessage(Bundle(playerData.locale).prefix("system.login.disabled"))
        }
    }

    private fun maps(arg: Array<String>, player: Playerc) {
        if (!Permissions.check(player, "maps")) return
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
        if (!Permissions.check(player, "me")) return
        Call.sendMessage("[orange]*[] " + player.name() + "[white] : " + arg[0])
    }

    private fun motd(arg: Array<String>, player: Playerc) {
        if (!Permissions.check(player, "motd")) return
        val motd = Tool.getMotd(PlayerCore[player.uuid()].locale)
        val count = motd.split("\r\n|\r|\n").toTypedArray().size
        if (count > 10) {
            Call.infoMessage(player.con(), motd)
        } else {
            player.sendMessage(motd)
        }
    }

    private fun players(arg: Array<String>, player: Playerc) {
        if (!Permissions.check(player, "players")) return
        val build = StringBuilder()

        var page = if (arg.isNotEmpty()) {
            abs(Strings.parseInt(arg[0]))
        } else 1
        val pages = Mathf.ceil(Groups.player.size().toFloat() / 6)

        if (pages < page) {
            player.sendMessage("[scarlet]'page' must be a number between[orange] 1[] and[orange] $pages[scarlet].")
            return
        }
        page--
        build.append("[green]==[white] Players list page ").append(page).append("/").append(pages).append(" [green]==[white]\n")

        val buf: Seq<Playerc> = Seq<Playerc>()
        Groups.player.each {e: Playerc ->
            buf.add(e)
        }

        for (a in 6 * page until (6 * (page + 1)).coerceAtMost(Groups.player.size())) {
            build.append("[gray]").append(buf.get(a).id()).append("[] ").append(buf.get(a).name()).append("\n")
        }

        player.sendMessage(build.toString())
    }

    private fun save(arg: Array<String>, player: Playerc) {
        if (!Permissions.check(player, "save")) return
        val file = saveDirectory.child(Config.slotNumber.toString() + "." + saveExtension)
        SaveIO.save(file)
        player.sendMessage(Bundle(PlayerCore[player.uuid()].locale).prefix("system.map-saved"))
    }

    private fun r(arg: Array<String>, player: Playerc) {
        if (!Permissions.check(player, "r")) return
        val playerData = PlayerCore[player.uuid()]
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
        if (!Permissions.check(player, "reset")) return
        val playerData = PlayerCore[player.uuid()]
        val bundle = Bundle(playerData.locale)
        when (arg[0]) {
            "zone" -> {
                var a = 0
                while (a < PluginData.warpzones.size) {
                    if (arg.size != 2) {
                        player.sendMessage(bundle.prefix("no-parameter"))
                        return
                    }
                    if (arg[1] == PluginData.warpzones[a].ip) {
                        PluginData.warpzones.remove(a)
                        for (value in WarpBorder.thread) {
                            value.interrupt()
                        }
                        WarpBorder.thread.clear()
                        WarpBorder.start()
                        player.sendMessage(bundle.prefix("success"))
                        break
                    }
                    a++
                }
            }
            "count" -> {
                PluginData.warpcounts.clear()
                player.sendMessage(bundle.prefix("system.warp.reset", "count"))
            }
            "total" -> {
                PluginData.warptotals.clear()
                player.sendMessage(bundle.prefix("system.warp.reset", "total"))
            }
            "block" -> {
                PluginData.warpblocks.clear()
                player.sendMessage(bundle.prefix("system.warp.reset", "block"))
            }
            else -> player.sendMessage(bundle.prefix("command.invalid"))
        }
    }

    private fun router(arg: Array<String>, player: Playerc) {
        if (!Permissions.check(player, "router")) return
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

    private fun reg(arg: Array<String>, player: Playerc) {
        if (Config.loginEnable) {
            when (Config.passwordMethod) {
                "discord" -> {
                    player.sendMessage("""Join discord and use !register command!${Config.discordLink}""".trimIndent())
                    if (!Discord.pins.containsKey(player.name())) Discord.queue(player)
                }
                "password" -> {
                    val lc = Tool.getGeo(player)
                    val hash = BCrypt.hashpw(arg[1], BCrypt.gensalt(12))
                    val register = PlayerCore.register(player.name(), player.uuid(), lc.displayCountry, lc.toString(), lc.displayLanguage, PluginVars.serverIP, "default", 0L, arg[0], hash, false)
                    if (register) {
                        PlayerCore.playerLoad(player, null)
                        player.sendMessage(Bundle(PlayerCore[player.uuid()].locale).prefix("register-success"))
                    } else {
                        player.sendMessage("[green][Essentials] [scarlet]Register failed/계정 등록 실패!")
                    }
                }
                else -> {
                    val lc = Tool.getGeo(player)
                    val hash = BCrypt.hashpw(arg[1], BCrypt.gensalt(12))
                    val register = PlayerCore.register(player.name(), player.uuid(), lc.displayCountry, lc.toString(), lc.displayLanguage, PluginVars.serverIP, "default", 0L, arg[0], hash, false)
                    if (register) {
                        PlayerCore.playerLoad(player, null)
                        player.sendMessage(Bundle(PlayerCore[player.uuid()].locale).prefix("register-success"))
                    } else {
                        player.sendMessage("[green][Essentials] [scarlet]Register failed/계정 등록 실패!")
                    }
                }
            }
        } else {
            player.sendMessage(Bundle(Config.locale).prefix("system.login.disabled"))
        }
    }

    private fun spawn(arg: Array<String>, player: Playerc) {
        if (!Permissions.check(player, "spawn")) return
        val playerData = PlayerCore[player.uuid()]
        val bundle = Bundle(playerData.locale)
        val targetUnit = Tool.getUnitByName(arg[0])
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
        if (Config.spawnLimit == count) {
            player.sendMessage(bundle.prefix("spawn-limit"))
            return
        }
        var targetPlayer = if (arg.size > 3) Tool.findPlayer(arg[3]) else player
        if (targetPlayer == null) {
            player.sendMessage(bundle.prefix("player.not-found"))
            targetPlayer = player
        }
        var targetTeam = if (arg.size > 2) Tool.getTeamByName(arg[2]) else targetPlayer.team()
        if (targetTeam == null) {
            player.sendMessage(bundle.prefix("team-not-found"))
            targetTeam = targetPlayer.team()
        }
        var i = 0
        while (count > i) {
            val baseUnit = targetUnit.create(targetTeam)
            baseUnit[targetPlayer.x] = targetPlayer.y
            baseUnit.add()
            i++
        }
    }

    private fun setperm(arg: Array<String>, player: Playerc) {
        if (!Permissions.check(player, "setperm")) return
        val playerData = PlayerCore[player.uuid()]
        val bundle = Bundle(playerData.locale)
        val target = Groups.player.find { p: Playerc -> p.name() == arg[0] }
        if (target == null) {
            player.sendMessage(bundle.prefix("player.not-found"))
            return
        }
        for (permission in Permissions.perm) {
            if (permission.name == arg[1]) {
                val data = PlayerCore[target.uuid()]
                data.permission = arg[1]
                Permissions.user[data.uuid].asObject()["group"] = arg[1]
                Permissions.update(true)
                player.sendMessage(bundle.prefix("success"))
                target.sendMessage(Bundle(data.locale).prefix("perm-changed"))
                return
            }
        }
        player.sendMessage(Bundle(playerData.locale).prefix("perm-group-not-found"))
    }

    private fun spawncore(arg: Array<String>, player: Playerc) {
        if (!Permissions.check(player, "spawn-core")) return
        var core = Blocks.coreShard
        when (arg[0]) {
            "normal" -> core = Blocks.coreFoundation
            "big" -> core = Blocks.coreNucleus
        }
        if (player.tileOn().block().alwaysReplace) {
            //player.tileOn().setBlock(core, player.team())
            Call.constructFinish(player.tileOn(), core, player.unit(), 0.toByte(), player.team(), false)
        }
    }

    private fun setmech(arg: Array<String>, player: Playerc) {
        if (!Permissions.check(player, "setmech")) return
        val playerData = PlayerCore[player.uuid()]
        val bundle = Bundle(playerData.locale)
        var mech: UnitType = UnitTypes.mace
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
            "vela" -> mech = UnitTypes.vela
            "corvus" -> mech = UnitTypes.corvus
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
            "quad" -> mech = UnitTypes.quad
            "oct" -> mech = UnitTypes.oct
            "alpha" -> mech = UnitTypes.alpha
            "beta" -> mech = UnitTypes.beta
            "gamma" -> mech = UnitTypes.gamma
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
        if (!Permissions.check(player, "status")) return
        val playerData = PlayerCore[player.uuid()]
        val bundle = Bundle(playerData.locale)
        player.sendMessage(bundle.prefix("server.status"))
        player.sendMessage("[#2B60DE]========================================[]")
        val fps = Core.graphics.framesPerSecond
        val bans = netServer.admins.banned.size
        val ipbans = netServer.admins.bannedIPs.size
        val bancount = bans + ipbans
        val playtime = Tool.longToTime(PluginVars.playtime)
        val uptime = Tool.longToTime(PluginVars.uptime)
        player.sendMessage(bundle["server.status.result", fps, Groups.player.size(), bancount, bans, ipbans, playtime, uptime, PluginVars.pluginVersion])
        val result = JsonObject()
        for (p in PluginVars.playerData) {
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
        if (!Permissions.check(player, "suicide")) return
        player.unit().kill()
        if (Groups.player != null && Groups.player.size() > 0) {
            Tool.sendMessageAll("suicide", player.name())
        }
    }

    private fun team(arg: Array<String>, player: Playerc) {
        if (!Permissions.check(player, "team")) return
        val playerData = PlayerCore[player.uuid()]
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
        if (!Permissions.check(player, "tempban")) return
        val playerData = PlayerCore[player.uuid()]
        var other: Playerc? = null
        for (p in Groups.player) {
            val result = p.name.contains(arg[0])
            if (result) {
                other = p
            }
        }
        if (other != null) {
            val bantime = System.currentTimeMillis() + 1000 * 60 * (arg[1].toInt())
            PlayerCore.ban(other, bantime, arg[2])
            Call.kick(other.con(), "Temp kicked")
            for (p in Groups.player) {
                val target = PlayerCore[p.uuid()]
                p.sendMessage(Bundle(target.locale).prefix("account.ban.temp", other.name(), player.name()))
            }
        } else {
            player.sendMessage(Bundle(playerData.locale).prefix("player.not-found"))
        }
    }

    private fun time(arg: Array<String>, player: Playerc) {
        if (!Permissions.check(player, "time")) return
        val playerData = PlayerCore[player.uuid()]
        val now = LocalDateTime.now()
        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val nowString = now.format(dateTimeFormatter)
        player.sendMessage(Bundle(playerData.locale).prefix("servertime", nowString))
    }

    private fun tp(arg: Array<String>, player: Playerc) {
        if (!Permissions.check(player, "tp")) return
        val playerData = PlayerCore[player.uuid()]
        val bundle = Bundle(playerData.locale)
        /*// TODO 모바일 유저 확인
        if (player.isMobile) {
            player.sendMessage(bundle.prefix("tp-not-support"))
            return
        }*/
        var other: Playerc? = null
        for (p in Groups.player) {
            val result = p.name.contains(arg[0])
            if (result) {
                other = p
            }
        }
        if (other == null) {
            player.sendMessage(bundle.prefix("player.not-found"))
            return
        }
        player.set(other.x, other.y)
    }

    private fun tpp(arg: Array<String>, player: Playerc) {
        if (!Permissions.check(player, "tpp")) return
        val playerData = PlayerCore[player.uuid()]
        var other1: Playerc? = null
        var other2: Playerc? = null
        for (p in Groups.player) {
            val result1 = p.name.contains(arg[0])
            if (result1) {
                other1 = p
            }
            val result2 = p.name.contains(arg[1])
            if (result2) {
                other2 = p
            }
        }
        if (other1 == null || other2 == null) {
            player.sendMessage(Bundle(playerData.locale).prefix("player.not-found"))
            return
        }
        other1.set(other2.x, other2.y)

        /*// TODO 모바일 유저도 tp 되는지 확인
        if (!other1.isMobile || !other2.isMobile) {
            other1.set(other2.x, other2.y)
        } else {*/
        //player.sendMessage(Bundle(playerData.locale).prefix("tp-ismobile"))
        //}
    }

    private fun tppos(arg: Array<String>, player: Playerc) {
        if (!Permissions.check(player, "tppos")) return
        val playerData = PlayerCore[player.uuid()]
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
    private fun voteService(arg: Array<String>, player: Playerc) {
        if (!Permissions.check(player, "vote") || Core.settings.getBool("isLobby")) return
        val playerData = PlayerCore[player.uuid()]
        val bundle = Bundle(playerData.locale)

        if (Vote.voting) {
            player.sendMessage(bundle.prefix("vote.in-processing"))
            return
        }

        Vote.player = player
        when (arg[0]) {
            "kick" -> {
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
                }

                Vote.type = Vote.VoteType.Kick
                Vote.parameters = arrayOf(target, arg[1])
            }

            "map" -> {
                if (arg.size < 2) {
                    player.sendMessage(bundle["no-parameter"])
                    return
                }

                var world = maps.all().find { map: Map -> map.name().equals(arg[1].replace('_', ' '), ignoreCase = true) || map.name().equals(arg[1], ignoreCase = true) }
                if (world == null) {
                    try {
                        world = maps.all()[arg[1].toInt()]
                        if (world != null) {
                            Vote.type = Vote.VoteType.Map
                            Vote.parameters = arrayOf(world)
                        } else {
                            player.sendMessage(bundle.prefix("vote.map.not-found"))
                        }
                    } catch (ignored: NumberFormatException) {
                        player.sendMessage(bundle.prefix("vote.map.not-found"))
                    }
                } else {
                    Vote.type = Vote.VoteType.Map
                    Vote.parameters = arrayOf(world)
                }
            }

            "gameover" -> {
                Vote.type = Vote.VoteType.Gameover
                Vote.parameters = arrayOf()
            }

            "rollback" -> {
                if (Config.rollback) {
                    Vote.type = Vote.VoteType.Rollback
                    Vote.parameters = arrayOf()
                } else {
                    player.sendMessage(bundle["vote.rollback.disabled"])
                }
            }

            "gamemode" -> {
                if (arg.size < 2) {
                    player.sendMessage(bundle["no-parameter"])
                    return
                }
                try {
                    Vote.type = Vote.VoteType.Gamemode
                    Vote.parameters = arrayOf(Gamemode.valueOf(arg[1]))
                } catch (e: IllegalArgumentException) {
                    player.sendMessage(bundle.prefix("vote.wrong-gamemode"))
                }
            }

            "skipwave" -> {
                if (arg.size != 2) {
                    player.sendMessage(bundle["no-parameter"])
                    return
                }
                Vote.type = Vote.VoteType.SkipWave
                Vote.parameters = arrayOf(arg[1])
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

        Vote.start()
    }

    private fun weather(arg: Array<String>, player: Playerc) {
        if (!Permissions.check(player, "weather")) return
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
        Call.setRules(state.rules)
        player.sendMessage(Bundle(PlayerCore[player.uuid()].locale).prefix("success"))
    }

    private fun mute(arg: Array<String>, player: Playerc) {
        if (!Permissions.check(player, "mute")) return
        val other = Groups.player.find { p: Playerc -> p.name().equals(arg[0], ignoreCase = true) }
        val playerData = PlayerCore[player.uuid()]
        if (other == null) {
            player.sendMessage(Bundle(playerData.locale).prefix("player.not-found"))
        } else {
            val target = PlayerCore[other.uuid()]
            target.mute = !target.mute
            player.sendMessage(Bundle(target.locale).prefix(if (target.mute) "player.muted" else "player.unmute", target.name))
        }
    }
}
