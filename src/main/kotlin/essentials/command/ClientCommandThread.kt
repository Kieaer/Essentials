package essentials.command

import arc.Core
import arc.math.Mathf
import arc.struct.Seq
import arc.util.Strings
import arc.util.Tmp
import arc.util.async.Threads
import essentials.PluginData
import essentials.command.ClientCommand.Command.*
import essentials.command.ClientCommand.Command.Vote
import essentials.data.Config
import essentials.data.PlayerCore
import essentials.eof.constructFinish
import essentials.eof.infoMessage
import essentials.eof.sendMessage
import essentials.eof.setPosition
import essentials.event.feature.*
import essentials.internal.Bundle
import essentials.internal.CrashReport
import essentials.internal.Tool
import essentials.thread.WarpBorder
import exceptions.ClientCommandError
import mindustry.Vars
import mindustry.Vars.netServer
import mindustry.content.Blocks
import mindustry.content.Weathers
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Playerc
import mindustry.gen.Unit
import mindustry.io.SaveIO
import mindustry.type.UnitType
import mindustry.world.Tile
import org.hjson.JsonObject
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.abs

class ClientCommandThread(private val type: ClientCommand.Command, private val arg: Array<String>, private val player: Playerc) {
    fun run() {
        val data = PluginData[player.uuid()]
        val locale = if(data != null) Locale(data.countryCode) else Config.locale
        val bundle = if(data != null) Bundle(Locale(data.countryCode)) else Bundle()
        if(!Permissions.check(player, type.name.toLowerCase())) return
        val sendMessage = sendMessage(player, bundle)

        try {
            when(type) {
                Vote -> {
                    if(Core.settings.getBool("isLobby")) return
                    if(PluginData.isVoting) {
                        sendMessage["vote.in-processing"]
                        return
                    }

                    PluginData.votingPlayer = player
                    when(arg[0]) {
                        "kick" -> {
                            if(arg.size < 2) {
                                sendMessage["no-parameter"]
                                return
                            }

                            var target = Groups.player.find { p: Playerc -> p.name().equals(arg[1], ignoreCase = true) }
                            try {
                                if(target == null) target = Groups.player.find { p: Playerc -> p.id() == arg[1].toInt() }
                            } catch(e: NumberFormatException) {
                                sendMessage["player.not-found"]
                                return
                            }

                            when {
                                target == null -> {
                                    sendMessage["player.not-found"]
                                    return
                                }
                                target.admin -> {
                                    sendMessage["vote.target-admin"]
                                    return
                                }
                                target === player -> {
                                    sendMessage["vote.target-own"]
                                    return
                                }
                                else -> {
                                    PluginData.votingType = VoteType.Kick
                                }
                            }
                        }

                        "map" -> {
                            if(arg.size < 2) {
                                sendMessage["no-parameter"]
                                return
                            }

                            PluginData.votingType = VoteType.Map
                        }

                        "gameover" -> {
                            PluginData.votingType = VoteType.Gameover
                        }

                        "rollback" -> {
                            PluginData.votingType = VoteType.Rollback
                        }

                        "skipwave" -> {
                            if(arg.size != 2) {
                                sendMessage["no-parameter"]
                                return
                            }
                            PluginData.votingType = VoteType.Skipwave
                        }

                        else -> {
                            when(arg[0]) {
                                "gamemode" -> sendMessage["vote.list.gamemode"]
                                "map" -> sendMessage["vote.map.not-found"]
                                "kick" -> sendMessage["vote.kick.parameter"]
                                else -> sendMessage["vote.list"]
                            }
                            return
                        }
                    }

                    PluginData.votingClass = essentials.event.feature.Vote(player, PluginData.votingType, if(arg.size == 2) arg[1] else "")
                    PluginData.votingClass!!.start()
                }
                Ch -> {
                    data!!.crosschat = !data.crosschat
                    sendMessage[if(data.crosschat) "player.crosschat.disable" else "player.crosschat.enabled"]
                }
                Changepw -> {
                    if(!Tool.checkPassword(player, data!!.accountid, arg[0], arg[1])) {
                        sendMessage["system.account.need-new-password"]
                        return
                    }
                    try {
                        Class.forName("org.mindrot.jbcrypt.BCrypt")
                        data.accountpw = BCrypt.hashpw(arg[0], BCrypt.gensalt(12))
                        sendMessage["success"]
                    } catch(e: ClassNotFoundException) {
                        CrashReport(e)
                    }
                }
                Chars -> {
                    if(Vars.world != null) Tool.setTileText(Vars.world.tile(player.tileX(), player.tileY()), Blocks.copperWall, arg[0])
                }
                Color -> {
                    data!!.colornick = !data.colornick
                    if(data.colornick) RainbowName.targets.add(player)
                    sendMessage[if(data.colornick) "feature.colornick.enable" else "feature.colornick.disable"]
                }
                KillAll -> {
                    for(a in mindustry.game.Team.all.indices) {
                        Groups.unit.each { u: Unit -> if(player.team() != u.team) u.kill() }
                    }
                    sendMessage["success"]
                }
                Help -> {
                    if(arg.isNotEmpty() && !Strings.canParseInt(arg[0])) {
                        sendMessage["page-number"]
                        return
                    }
                    val temp = Seq<String>()
                    for(a in 0 until netServer.clientCommands.commandList.size) {
                        val command = netServer.clientCommands.commandList[a]
                        if(Permissions.check(player, command.text)) {
                            temp.add("[orange] /${command.text} [white]${command.paramText} [lightgray]- ${command.description}\n")
                        }
                    }
                    val result = StringBuilder()
                    val per = 8
                    var page = if(arg.isNotEmpty()) abs(Strings.parseInt(arg[0])) else 1
                    val pages = Mathf.ceil(temp.size.toFloat() / per)
                    page--

                    if(page >= pages || page < 0) {
                        sendMessage["[scarlet]'page' must be a number between[orange] 1[] and[orange] ${pages}[scarlet]."]
                        return
                    }

                    result.append(Strings.format("[orange]-- Commands Page[lightgray] ${page + 1}[gray]/[lightgray]${pages}[orange] --\n"))
                    for(a in per * page until (per * (page + 1)).coerceAtMost(temp.size)) {
                        result.append(temp[a])
                    }
                    sendMessage[result.toString().substring(0, result.length - 1)]
                }
                Info -> {
                    val datatext = """
                [#DEA82A]${Bundle(data)["player.info"]}[]
                [#2B60DE]====================================[]
                [green]${bundle["player.name"]}[] : ${player.name()}[white]
                [green]${bundle["player.uuid"]}[] : ${data!!.uuid}[white]
                [green]${bundle["player.country"]}[] : ${locale.getDisplayCountry(locale)}
                [green]${bundle["player.placecount"]}[] : ${data.placecount}
                [green]${bundle["player.breakcount"]}[] : ${data.breakcount}
                [green]${bundle["player.joincount"]}[] : ${data.joincount}
                [green]${bundle["player.kickcount"]}[] : ${data.kickcount}
                [green]${bundle["player.level"]}[] : ${data.level}
                [green]${bundle["player.reqtotalexp"]}[] : ${Exp[data]}
                [green]${bundle["player.firstdate"]}[] : ${
                        Tool.longToDateTime(data.firstdate).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
                    }
                [green]${bundle["player.lastdate"]}[] : ${
                        Tool.longToDateTime(data.lastdate).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
                    }
                [green]${bundle["player.playtime"]}[] : ${Tool.longToTime(data.playtime)}
                [green]${bundle["player.attackclear"]}[] : ${data.attackclear}
                [green]${bundle["player.pvpwincount"]}[] : ${data.pvpwincount}
                [green]${bundle["player.pvplosecount"]}[] : ${data.pvplosecount}
                """.trimIndent()
                    infoMessage(player, datatext)
                }
                Warp -> {
                    val types = arrayOf("zone", "block", "count", "total")
                    if(!listOf(*types).contains(arg[0])) {
                        sendMessage["system.warp.info"]
                    } else {
                        val type = arg[0]
                        val x = player.tileX()
                        val y = player.tileY()
                        val name = Vars.state.map.name()
                        val size: Int
                        val clickable: Boolean
                        var ip = ""
                        var port = 6567
                        if(arg.size > 1) {
                            if(arg[1].contains(":")) {
                                val address = arg[1].split(":").toTypedArray()
                                ip = address[0]
                                port = address[1].toInt()
                            } else {
                                ip = arg[1]
                            }
                        }
                        val parameters: Array<String> = if(arg.size == 3) {
                            arg[2].split(" ").toTypedArray()
                        } else {
                            arrayOf()
                        }
                        when(type) {
                            "zone" -> //ip size clickable
                                if(parameters.size <= 1) {
                                    sendMessage["system.warp.incorrect"]
                                } else {
                                    try {
                                        size = parameters[0].toInt()
                                        clickable = java.lang.Boolean.parseBoolean(parameters[1])
                                    } catch(ignored: NumberFormatException) {
                                        sendMessage["system.warp.not-int"]
                                        return
                                    }
                                    PluginData.warpzones.add(PluginData.WarpZone(name, Vars.world.tile(x, y).pos(), Vars.world.tile(x + size, y + size).pos(), clickable, ip, port))
                                    WarpBorder.thread.clear()
                                    WarpBorder.start()
                                    sendMessage["system.warp.added"]
                                }
                            "block" -> if(parameters.isEmpty()) {
                                sendMessage["system.warp.incorrect"]
                            } else {
                                val t: Tile = Vars.world.tile(x, y)
                                PluginData.warpblocks.add(PluginData.WarpBlock(name, t.pos(), t.block().name, t.block().size, ip, port, arg[2]))
                                sendMessage["system.warp.added"]
                            }
                            "count" -> {
                                PluginData.warpcounts.add(PluginData.WarpCount(name, Vars.world.tile(x, y).pos(), ip, port, 0, 0))
                                sendMessage["system.warp.added"]
                            }
                            "total" -> {
                                PluginData.warptotals.add(PluginData.WarpTotal(name, Vars.world.tile(x, y).pos(), 0, 0))
                                sendMessage["system.warp.added"]
                            }
                            else -> sendMessage["command.invalid"]
                        }
                    }
                }
                Kill -> {
                    if(arg.isEmpty()) {
                        player.unit().kill()
                    } else {
                        val other = Groups.player.find { p: Playerc -> p.name().equals(arg[0], ignoreCase = true) }
                        if(other == null) sendMessage["player.not-found"] else other.unit().kill()
                    }
                }
                Login -> {
                    if(Config.authType != Config.AuthType.None) {
                        if(data != null) {
                            if(PlayerCore.login(arg[0], arg[1])) {
                                if(PlayerCore.playerLoad(player, arg[0])) {
                                    sendMessage["system.login.success"]
                                }
                            } else {
                                sendMessage["[scarlet]Login failed!"]
                            }
                        } else {
                            sendMessage["You're already logged."]
                        }
                    } else {
                        sendMessage["system.login.disabled"]
                    }
                }
                Me -> {
                    sendMessage("[orange]*[] " + player.name() + "[white] : " + arg[0])
                }
                Motd -> {
                    val motd = Tool.getMotd(locale)
                    val count = motd.split("\r\n|\r|\n").toTypedArray().size
                    if(count > 10) infoMessage(player, motd) else sendMessage(player, motd)
                }
                Players -> {
                    val message = StringBuilder()
                    val page = if(arg.isNotEmpty()) arg[0].toInt() else 0

                    val buffer = Mathf.ceil(Groups.player.size().toFloat() / 6)
                    val pages = if(buffer > 1.0) buffer - 1 else 0

                    if(pages < page) {
                        sendMessage["[scarlet]페이지 쪽수는 최대 [orange]$pages[] 까지 있습니다"]
                    } else {
                        message.append("[green]==[white] 현재 서버 플레이어 목록. [sky]페이지 [orange]$page[]/[orange]$pages\n")

                        val players: Seq<Playerc> = Seq<Playerc>()
                        Groups.player.each { e: Playerc ->
                            players.add(e)
                        }

                        for(a in 6 * page until (6 * (page + 1)).coerceAtMost(Groups.player.size())) {
                            message.append("[gray]${players.get(a).id()}[white] ${
                                players.get(a).name()
                            }\n")
                        }

                        sendMessage[message.toString().dropLast(2)]
                    }
                }
                Save -> {
                    val file = Vars.saveDirectory.child("rollback." + Vars.saveExtension)
                    SaveIO.save(file)
                    sendMessage["system.map-saved"]
                }/*Reset -> {
                    when (arg[0]) {
                        "zone" -> {
                            var a = 0
                            while (a < PluginData.warpzones.size) {
                                if (arg.size != 2) {
                                    sendMessage["no-parameter"]
                                    return
                                }
                                if (arg[1] == PluginData.warpzones[a].ip) {
                                    PluginData.warpzones.remove(a)
                                    for (value in WarpBorder.thread) {
                                        value.interrupt()
                                    }
                                    WarpBorder.thread.clear()
                                    WarpBorder.start()
                                    sendMessage["success"]
                                    break
                                }
                                a++
                            }
                        }
                        "count" -> {
                            PluginData.warpcounts.clear()
                            sendMessage["system.warp.reset", "count"]
                        }
                        "total" -> {
                            PluginData.warptotals.clear()
                            sendMessage["system.warp.reset", "total"]
                        }
                        "block" -> {
                            PluginData.warpblocks.clear()
                            sendMessage["system.warp.reset", "block"]
                        }
                        else -> sendMessage["command.invalid"]
                    }
                }*/
                Router -> {
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
                            """, """
                            [stat][#404040][]
                            [stat][#404040]
                            [stat][#404040][]
                            [#404040][stat]
                            [stat][#404040][]
                            [stat][#404040][]
                            [stat][#404040]
                            [stat][#404040][][#404040][]
                            """, """
                            [stat][#404040][][#404040]
                            [stat][#404040][]
                            [#404040][stat]
                            [stat][#404040][]
                            [stat][#404040][]
                            [stat][#404040]
                            [stat][#404040][]
                            [#404040][stat][][stat]
                            """, """
                            [stat][#404040][][#404040][]
                            [#404040][stat]
                            [stat][#404040][]
                            [stat][#404040][]
                            [stat][#404040]
                            [stat][#404040][]
                            [#404040][stat]
                            [stat][#404040][]
                            """, """
                            [#404040][stat][][stat]
                            [stat][#404040][]
                            [stat][#404040][]
                            [stat][#404040]
                            [stat][#404040][]
                            [#404040][stat]
                            [stat][#404040][]
                            [stat][#404040][]
                            """)
                        val loop = arrayOf("""
                            [#6B6B6B][stat][#6B6B6B]
                            [stat][#404040][]
                            [stat][#404040]
                            [stat][#404040][]
                            [#404040][]
                            [stat][#404040][]
                            [stat][#404040][]
                            [#6B6B6B][stat][#404040][][#6B6B6B]
                            """, """
                            [#6B6B6B][stat][#6B6B6B]
                            [#6B6B6B][stat][#404040][][#6B6B6B]
                            [stat][#404040][]
                            [#404040][]
                            [stat][#404040][]
                            [stat][#404040][]
                            [#6B6B6B][stat][#404040][][#6B6B6B]
                            [#6B6B6B][stat][#6B6B6B]
                            """, """
                            [#6B6B6B][#585858][stat][][#6B6B6B]
                            [#6B6B6B][#828282][stat][#404040][][][#6B6B6B]
                            [#585858][stat][#404040][][#585858]
                            [stat][#404040][]
                            [stat][#404040][]
                            [#585858][stat][#404040][][#585858]
                            [#6B6B6B][stat][#404040][][#828282][#6B6B6B]
                            [#6B6B6B][#585858][stat][][#6B6B6B]
                            """, """
                            [#6B6B6B][#585858][#6B6B6B]
                            [#6B6B6B][#828282][stat][][#6B6B6B]
                            [#585858][#6B6B6B][stat][#404040][][#828282][#585858]
                            [#585858][stat][#404040][][#585858]
                            [#585858][stat][#404040][][#585858]
                            [#585858][#6B6B6B][stat][#404040][][#828282][#585858]
                            [#6B6B6B][stat][][#828282][#6B6B6B]
                            [#6B6B6B][#585858][#6B6B6B]
                            """, """
                            [#6B6B6B][#585858][#6B6B6B]
                            [#6B6B6B][#828282][#6B6B6B]
                            [#585858][#6B6B6B][stat][][#828282][#585858]
                            [#585858][#6B6B6B][stat][#404040][][#828282][#585858]
                            [#585858][#6B6B6B][stat][#404040][][#828282][#585858]
                            [#585858][#6B6B6B][stat][][#828282][#585858]
                            [#6B6B6B][#828282][#6B6B6B]
                            [#6B6B6B][#585858][#6B6B6B]
                            """, """
                            [#6B6B6B][#585858][#6B6B6B]
                            [#6B6B6B][#828282][#6B6B6B]
                            [#585858][#6B6B6B][#828282][#585858]
                            [#585858][#6B6B6B][stat][#6B6B6B][#828282][#585858]
                            [#585858][#6B6B6B][stat][#6B6B6B][#828282][#585858]
                            [#585858][#6B6B6B][#828282][#585858]
                            [#6B6B6B][#828282][#6B6B6B]
                            [#6B6B6B][#585858][#6B6B6B]
                            """, """
                            [#6B6B6B][#585858][#6B6B6B]
                            [#6B6B6B][#828282][#6B6B6B]
                            [#585858][#6B6B6B][#828282][#585858]
                            [#585858][#6B6B6B][#828282][#6B6B6B][#828282][#585858]
                            [#585858][#6B6B6B][#828282][#6B6B6B][#828282][#585858]
                            [#585858][#6B6B6B][#828282][#585858]
                            [#6B6B6B][#828282][#6B6B6B]
                            [#6B6B6B][#585858][#6B6B6B]
                            """)
                        try {
                            while(!player.isNull) {
                                for(d in loop) {
                                    player.name(d)
                                    Threads.sleep(500)
                                }
                                Threads.sleep(5000)
                                for(i in loop.indices.reversed()) {
                                    player.name(loop[i])
                                    Threads.sleep(500)
                                }
                                for(d in zero) {
                                    player.name(d)
                                    Threads.sleep(500)
                                }
                            }
                        } catch(e: InterruptedException) {

                        }
                    }.start()
                }
                Register -> {
                    if(Config.authType != Config.AuthType.None) {
                        when(Config.authType.name.toLowerCase()) {
                            "discord" -> {
                                sendMessage["Join discord and use !register command!"]
                                if(!Discord.pins.containsKey(player.name())) Discord.queue(player)
                            }
                            "password" -> {
                                val hash = BCrypt.hashpw(arg[1], BCrypt.gensalt(12))
                                val register = PlayerCore.register(player, player.name(), player.uuid(), arg[0], hash)
                                if(register) {
                                    PlayerCore.playerLoad(player, null)
                                    sendMessage["register-success"]
                                } else {
                                    sendMessage["[scarlet]Register failed"]
                                }
                            }
                            else -> {
                                val hash = BCrypt.hashpw(arg[1], BCrypt.gensalt(12))
                                val register = PlayerCore.register(player, player.name(), player.uuid(), arg[0], hash)
                                if(register) {
                                    PlayerCore.playerLoad(player, null)
                                    sendMessage["register-success"]
                                } else {
                                    sendMessage["[scarlet]Register failed"]
                                }
                            }
                        }
                    } else {
                        sendMessage["system.login.disabled"]
                    }
                }
                Spawn -> {
                    val type = arg[0]
                    val name = arg[1]
                    val parameter = if(arg.size == 3) arg[2].toIntOrNull() else 1

                    when {
                        type.equals("unit", true) -> {
                            val unit = Vars.content.units().find { unitType: UnitType -> unitType.name == name }
                            if(unit != null) {
                                if(parameter != null) {
                                    if(name != "block") {
                                        for(a in 1..parameter) {
                                            val baseUnit = unit.create(player.team())
                                            baseUnit.set(player.x, player.y)
                                            baseUnit.add()
                                        }
                                    } else { // TODO bundle
                                        sendMessage["Block isn't unit. don't spawn it."]
                                    }
                                } else {
                                    sendMessage["system.mob.not-number"]
                                }
                            } else {
                                val names = StringBuilder()
                                Vars.content.units().each {
                                    names.append("${it.name}, ")
                                } // TODO bundle
                                sendMessage["Avaliable unit names: ${names.dropLast(2)}"]
                            }
                        }
                        type.equals("block", true) -> {
                            constructFinish(tile = player.tileOn(), block = Vars.content.blocks().find { it.name == name }, builder = player.unit(), rotation = parameter?.toByte()
                                    ?: 0, team = player.team(), config = null)
                        }
                        else -> { // TODO 명령어 예외 만들기
                            return
                        }
                    }
                }
                Status -> {
                    sendMessage["server.status"]
                    sendMessage(player, "[#2B60DE]========================================[]")
                    val fps = Core.graphics.framesPerSecond
                    val bans = netServer.admins.banned.size
                    val ipbans = netServer.admins.bannedIPs.size
                    val bancount = bans + ipbans
                    val playtime = Tool.longToTime(PluginData.playtime)
                    val uptime = Tool.longToTime(PluginData.uptime)
                    sendMessage["server.status.result", fps.toString(), Groups.player.size().toString(), bancount.toString(), bans.toString(), ipbans.toString(), playtime, uptime, PluginData.pluginVersion.toString()]
                    val result = JsonObject()
                    for(p in PluginData.playerData) {
                        val loc = Locale(p.countryCode)
                        if(result[loc.getDisplayCountry(locale)] == null) {
                            result.add(loc.getDisplayCountry(locale), 1)
                        } else {
                            result[loc.getDisplayCountry(locale)] = result[loc.getDisplayCountry(locale)].asInt() + 1
                        }
                    }/*val s = StringBuilder()
                    for (m in result) {
                        val d = "${m.name}: ${m.value}"
                        s.append(d)
                    }
                    sendMessage[if (s.isNotEmpty() && s.last() == (',')) s.substring(0, s.length - 1) else s.toString()]*/
                }
                Team -> {
                    when(arg[0]) {
                        "derelict" -> player.team(mindustry.game.Team.derelict)
                        "sharded" -> player.team(mindustry.game.Team.sharded)
                        "crux" -> player.team(mindustry.game.Team.crux)
                        "green" -> player.team(mindustry.game.Team.green)
                        "purple" -> player.team(mindustry.game.Team.purple)
                        "blue" -> player.team(mindustry.game.Team.blue)
                        else -> sendMessage(player, Bundle(data).prefix("command.team"))
                    }
                }
                Time -> {
                    val now = LocalDateTime.now()
                    val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    val nowString = now.format(dateTimeFormatter)
                    sendMessage(player, Bundle(data).prefix("servertime", nowString))
                }
                Tp -> {
                    var other: Playerc? = null
                    for(p in Groups.player) {
                        val result = p.name.contains(arg[0])
                        if(result) {
                            other = p
                        }
                    }
                    if(other == null) {
                        sendMessage["player.not-found"]
                        return
                    }
                    setPosition(player, other.x, other.y)
                }
                Weather -> {
                    if(arg.isNullOrEmpty() || arg[0].toIntOrNull() !is Int) {
                        sendMessage["command-invalid"]
                        return
                    }

                    val time = arg[0].toFloat() * 60f
                    Tmp.v1.setToRandomDirection()
                    when(arg[0]) {
                        "rain" -> Call.createWeather(Weathers.rain, 1f, time, Tmp.v1.x, Tmp.v1.y)
                        "snow" -> Call.createWeather(Weathers.snow, 1f, time, Tmp.v1.x, Tmp.v1.y)
                        "sandstorm" -> Call.createWeather(Weathers.sandstorm, 1f, time, Tmp.v1.x, Tmp.v1.y)
                        "sporestorm" -> Call.createWeather(Weathers.sporestorm, 1f, time, Tmp.v1.x, Tmp.v1.y)
                        else -> return
                    }
                    sendMessage["success"]
                }
                Mute -> {
                    val other = Groups.player.find { p: Playerc -> p.name().equals(arg[0], ignoreCase = true) }
                    if(other == null) {
                        sendMessage(player, Bundle(data).prefix("player.not-found"))
                    } else {
                        val target = PluginData[other.uuid()]
                        target!!.mute = !target.mute
                        sendMessage[if(target.mute) "player.muted" else "player.unmute", target.name]
                    }
                }
                Maps -> {
                    val list = Vars.maps.all()
                    val build = StringBuilder()
                    var page = if(arg.isNotEmpty()) Strings.parseInt(arg[0]) else 1
                    val pages = Mathf.ceil(list.size.toFloat() / 6)
                    page--
                    if(page > pages || page < 0) {
                        sendMessage["[scarlet]'page' must be a number between[orange] 1[] and[orange] $pages[scarlet]."]
                        return
                    }
                    build.append("[green]==[white] Server maps page ").append(page).append("/").append(pages).append(" [green]==[white]\n")
                    for(a in 6 * page until (6 * (page + 1)).coerceAtMost(list.size)) {
                        build.append("[gray]").append(a).append("[] ").append(list[a].name()).append("\n")
                    }
                    sendMessage[build.toString()]
                }
            }
        } catch(e: ClientCommandError) {
            CrashReport(e)
        }
    }
}