package essential.core

import arc.Core
import arc.Events
import arc.func.Cons
import arc.graphics.Color
import arc.graphics.Colors
import arc.math.Mathf
import arc.util.Log
import arc.util.Strings
import arc.util.Threads
import arc.util.Tmp
import com.charleskorn.kaml.Yaml
import com.github.lalyos.jfiglet.FigletFont
import essential.core.Event.actionFilter
import essential.core.Event.findPlayerData
import essential.core.Event.findPlayers
import essential.core.Event.findPlayersByName
import essential.core.Event.resetVote
import essential.core.Event.voteMap
import essential.core.Event.worldHistory
import essential.core.Main.Companion.conf
import essential.core.Main.Companion.currentTime
import essential.core.Main.Companion.daemon
import essential.core.Main.Companion.database
import essential.core.Main.Companion.root
import essential.core.annotation.ClientCommand
import essential.core.annotation.ServerCommand
import mindustry.Vars
import mindustry.content.Blocks
import mindustry.content.Weathers
import mindustry.core.GameState
import mindustry.game.EventType
import mindustry.game.EventType.GameOverEvent
import mindustry.game.Gamemode
import mindustry.game.Team
import mindustry.gen.*
import mindustry.gen.Unit
import mindustry.maps.Map
import mindustry.net.Packets
import mindustry.net.WorldReloader
import mindustry.server.ServerControl.instance
import mindustry.type.Item
import mindustry.type.UnitType
import mindustry.ui.Menus
import mindustry.world.Tile
import org.hjson.JsonArray
import org.hjson.JsonObject
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.random.RandomGenerator
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.round
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation

class Commands {
    companion object {
        // 다중 사용 함수
        const val PLAYER_NOT_FOUND = "player.not.found"
        const val PLAYER_NOT_REGISTERED = "player.not.registered"
        const val STANDARD_DATE = "YYYY-MM-dd HH:mm:ss"
    }

    @ClientCommand("changemap", "<name> [gamemode]", "Change the world or gamemode immediately.")
    fun changemap(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        val arr = HashMap<Int, Map>()
        Vars.maps.all().sortedBy { a -> a.name() }.forEachIndexed { index, map ->
            arr[index] = map
        }

        val map: Map? = if (arg[0].toIntOrNull() != null) {
            arr[arg[0].toInt()]
        } else {
            Vars.maps.all().find { e -> e.name().contains(arg[0], true) }
        }

        if (map != null) {
            try {
                val mode = if (arg.size != 1) {
                    Gamemode.valueOf(arg[1])
                } else {
                    Vars.state.rules.mode()
                }

                val reloader = WorldReloader()
                reloader.begin()
                Vars.world.loadMap(map, map.applyRules(mode))
                Vars.state.rules = Vars.state.map.applyRules(mode)
                Vars.logic.play()
                reloader.end()
            } catch (_: IllegalArgumentException) {
                playerData.err("command.changemap.mode.not.found", arg[1])
            }
        } else {
            playerData.err("command.changemap.map.not.found", arg[0])
        }
    }

    @ClientCommand("changename", "<target> <new_name>", "Change player name")
    fun changeName(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        fun change(data: DB.PlayerData) {
            transaction {
                if (DB.Player.select(DB.Player.name).where(DB.Player.name eq (arg[1])).toList().isNotEmpty()) {
                    data.err("command.changename.exists", arg[1])
                } else {
                    Events.fire(CustomEvents.PlayerNameChanged(data.name, arg[1], data.uuid))
                    data.send("command.changename.apply.other", data.name, arg[1])
                    data.name = arg[0]
                    if (data.player.unit() != Nulls.unit) data.player.name(arg[1])
                    database.queue(data)
                }
            }
            return
        }

        if (arg.size > 1) {
            val target = findPlayers(arg[0])
            if (target != null) {
                val data = findPlayerData(target.uuid())
                if (data != null) {
                    change(data)
                } else {
                    playerData.err(PLAYER_NOT_REGISTERED)
                }
            } else {
                val offline = database[arg[0]]
                if (offline != null) {
                    change(offline)
                } else {
                    playerData.err(PLAYER_NOT_FOUND)
                }
            }
        } else {
            playerData.name = arg[0]
            player.name(arg[0])
            database.queue(playerData)
            playerData.send("command.changename.apply")
        }
    }

    @ClientCommand("changepw", "<new_password> <password_repeat>", "Change account password.")
    fun changePassword(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        if (arg[0] != arg[1]) {
            playerData.err("command.changepw.same")
            return
        }

        val password = BCrypt.hashpw(arg[0], BCrypt.gensalt())
        playerData.accountPW = password
        database.queue(playerData)
        playerData.send("command.changepw.apply")
    }

    @ClientCommand("chat", "<on/off>", "Mute all players without admins")
    fun chat(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        Event.isGlobalMute = arg[0].equals("off", true)
        if (Event.isGlobalMute) {
            playerData.send("command.chat.off")
        } else {
            playerData.send("command.chat.on")
        }
    }

    @ServerCommand("chat", "<on/off>", "Mute all players without admins")
    fun chat(arg: Array<out String>) {
        Event.isGlobalMute = arg[0].equals("off", true)
        val bundle = Bundle()
        if (Event.isGlobalMute) {
            Log.info(bundle["command.chat.off"])
        } else {
            Log.info(bundle["command.chat.on"])
        }
    }

    @ClientCommand("chars", "<text...>", "Make pixel texts on ground.")
    fun chars(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        if (Vars.world != null) {
            fun convert(text: String): Array<String>? {
                return try {
                    val art =
                        FigletFont.convertOneLine(Main::class.java.classLoader.getResourceAsStream("6x10.flf"), text)
                    art.split("\n").toTypedArray()
                } catch (e: ArrayIndexOutOfBoundsException) {
                    null
                }
            }

            var x = player.tileX()
            var y = player.tileY()
            val text = convert(arg[0])
            if (text != null) {
                for (line in text) {
                    for (char in line) {
                        if (char == '#' && Vars.world.tile(x, y).block() != null && Vars.world.tile(x, y)
                                .block() == Blocks.air
                        ) {
                            Call.setTile(Vars.world.tile(x, y), Blocks.scrapWall, player.team(), 0)
                        }
                        x++
                    }
                    y--
                    x = player.tileX()
                }
            } else {
                playerData.err("command.char.unsupported")
            }
        }
    }

    @ClientCommand(name = "color", description = "Enable color nickname")
    fun color(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        playerData.animatedName = !playerData.animatedName
    }

    @ClientCommand("dps", description = "Create damage per seconds meter block")
    fun dps(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        if (Event.dpsTile == null) {
            Call.constructFinish(
                player.tileOn(),
                Blocks.thoriumWallLarge,
                player.unit(),
                0,
                Vars.state.rules.waveTeam,
                null
            )
            Event.dpsTile = player.tileOn()
            playerData.send("command.dps.created")
        } else {
            Call.deconstructFinish(Event.dpsTile, Blocks.air, player.unit())
            Event.dpsTile = null
            playerData.send("command.dps.deleted")
        }
    }

    @ClientCommand(
        "effect",
        "<on/off/level> [color]",
        "Turn other players' effects on or off, or set effects and colors for each level."
    )
    fun effect(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        when {
            arg[0].toIntOrNull() != null -> {
                if (arg[0].toInt() <= playerData.level) {
                    playerData.effectLevel = arg[0].toInt()
                    if (arg.size == 2) {
                        try {
                            if (Colors.get(arg[1]) == null) {
                                Color.valueOf(arg[1])
                            }

                            playerData.effectColor = arg[1]
                            database.queue(playerData)
                        } catch (_: IllegalArgumentException) {
                            playerData.err("command.effect.no.color")
                        } catch (_: StringIndexOutOfBoundsException) {
                            playerData.err("command.effect.no.color")
                        }
                    }
                } else {
                    playerData.err("command.effect.level")
                }
            }

            arg[0] == "off" -> {
                playerData.showLevelEffects = false
                playerData.send("command.effect.off")
            }

            arg[0] == "on" -> {
                playerData.showLevelEffects = true
                playerData.send("command.effect.on")
            }

            else -> {
                playerData.err("command.effect.invalid")
            }
        }
    }

    @ClientCommand("exp", "<set/hide/add/remove> [values/player] [player]", "Edit account exp values")
    fun exp(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        fun set(exp: Int?, type: String) {
            fun set(data: DB.PlayerData) {
                val previous = data.exp
                when (type) {
                    "set" -> data.exp = arg[1].toInt()
                    "add" -> data.exp += arg[1].toInt()
                    "remove" -> data.exp -= arg[1].toInt()
                }
                database.queue(data)
                playerData.send("command.exp.result", previous, data.exp)
            }

            if (exp != null) {
                if (arg.size == 3) {
                    val target = findPlayers(arg[2])
                    if (target != null) {
                        val data = findPlayerData(target.uuid())
                        if (data != null) {
                            set(data)
                        } else {
                            playerData.err(PLAYER_NOT_REGISTERED)
                            return
                        }
                    } else {
                        val p = findPlayersByName(arg[2])
                        if (p != null) {
                            val a = database[p.id]
                            if (a != null) {
                                set(a)
                            }
                        } else {
                            playerData.err(PLAYER_NOT_FOUND)
                            return
                        }
                    }
                } else {
                    set(playerData)
                }
            } else {
                playerData.err("command.exp.invalid")
            }
        }

        when (arg[0]) {
            "set" -> {
                set(arg[1].toIntOrNull(), "set")
            }

            "hide" -> {
                if (arg.size == 2) {
                    val target = findPlayers(arg[1])
                    if (target != null) {
                        val other = findPlayerData(target.uuid())
                        if (other != null) {
                            other.hideRanking = !other.hideRanking
                            database.queue(other)
                            val msg = if (other.hideRanking) "hide" else "unhide"
                            playerData.send("command.exp.ranking.$msg")
                            return
                        }
                    } else {
                        playerData.err(PLAYER_NOT_FOUND)
                        return
                    }
                }

                playerData.hideRanking = !playerData.hideRanking
                database.queue(playerData)
                val msg = if (playerData.hideRanking) "hide" else "unhide"
                playerData.send("command.exp.ranking.$msg")
            }

            "add" -> {
                set(arg[1].toIntOrNull(), "add")
            }

            "remove" -> {
                set(arg[1].toIntOrNull(), "remove")
            }

            else -> {
                playerData.err("command.exp.invalid.command")
            }
        }
    }

    @ClientCommand("fillitems", "[team]", "Fill the core with items.")
    fun fillItems(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        if (arg.isEmpty()) {
            if (Vars.state.teams.cores(player.team()).isEmpty) {
                playerData.err("command.fillitems.core.empty")
                return
            }

            Vars.content.items().forEach {
                Vars.state.teams.cores(player.team()).first().items[it] =
                    Vars.state.teams.cores(player.team()).first().storageCapacity
            }
            playerData.send("command.fillitems.core.filled", player.team().coloredName())
        } else {
            val team = selectTeam(arg[0])
            if (Vars.state.teams.cores(team).isEmpty) {
                playerData.err("command.fillitems.core.empty")
                return
            }

            Vars.content.items().forEach {
                Vars.state.teams.cores(team).first().items[it] = Vars.state.teams.cores(team).first().storageCapacity
            }

            playerData.send("command.fillitems.core.filled", team.coloredName())
        }
    }

    @ClientCommand("freeze", "<player>", "Stop player unit movement")
    fun freezeClient(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        val target = findPlayers(arg[0])
        if (target != null) {
            val data = findPlayerData(target.uuid())
            if (data != null) {
                data.freeze = !data.freeze
                val msg = if (data.freeze) {
                    data.status["freeze"] = "${target.x}/${target.y}"
                    "done"
                } else {
                    data.status.remove("freeze")
                    "undo"
                }
                playerData.send("command.freeze.$msg", target.plainName())
            } else {
                playerData.err(PLAYER_NOT_REGISTERED)
            }
        } else {
            playerData.err(PLAYER_NOT_FOUND)
        }
    }

    @ServerCommand("freeze", "<player>", "Stop player unit movement")
    fun freezeServer(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        val bundle = Bundle()
        val target = findPlayers(arg[0])
        if (target != null) {
            val data = findPlayerData(target.uuid())
            if (data != null) {
                data.freeze = !data.freeze
                val msg = if (data.freeze) {
                    data.status["freeze"] = "${target.x}/${target.y}"
                    "done"
                } else {
                    data.status.remove("freeze")
                    "undo"
                }
                Log.info(bundle["command.freeze.$msg", target.plainName()])
            } else {
                Log.err(bundle[PLAYER_NOT_REGISTERED])
            }
        } else {
            Log.err(bundle[PLAYER_NOT_FOUND])
        }
    }

    @ClientCommand("gg", "[team]", "Make game over immediately.")
    fun gg(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        if (arg.isEmpty()) {
            Events.fire(EventType.GameOverEvent(Vars.state.rules.waveTeam))
        } else {
            Events.fire(EventType.GameOverEvent(selectTeam(arg[0])))
        }
    }

    @ClientCommand("god", "[player]", "Set max player health")
    fun god(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        player.unit().health(1.0E8f)
        playerData.send("command.god")
    }

    @ClientCommand("help", "[page]", "Show command lists")
    fun help(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        if (arg.isNotEmpty() && !Strings.canParseInt(arg[0])) {
            try {
                playerData.send("command.help.${arg[0]}")
            } catch (e: MissingResourceException) {
                playerData.err("command.help.not.exists")
            }
            return
        }

        val temp = ArrayList<String>()
        val bundle = Bundle(playerData.languageTag)
        for (a in 0 until Vars.netServer.clientCommands.commandList.size) {
            val command = Vars.netServer.clientCommands.commandList[a]
            if (Permission.check(playerData, command.text)) {
                val description = try {
                    bundle["command.description." + command.text]
                } catch (_: MissingResourceException) {
                    command.description
                }
                temp.add("[orange] /${command.text} [white]${command.paramText} [lightgray]- $description\n")
            }
        }
        val result = StringBuilder()
        val per = 8
        var page = if (arg.isNotEmpty()) abs(Strings.parseInt(arg[0])) else 1
        val pages = Mathf.ceil(temp.size.toFloat() / per)
        page--

        if (page >= pages || page < 0) {
            playerData.err("command.page.range", pages)
            return
        }

        result.append("[orange]-- ${bundle["command.page"]}[lightgray] ${page + 1}[gray]/[lightgray]${pages}[orange] --\n")
        for (a in per * page until (per * (page + 1)).coerceAtMost(temp.size)) {
            result.append(temp[a])
        }

        val msg = result.toString().substring(0, result.length - 1)
        playerData.lastSentMessage = msg
        player.sendMessage(msg)
    }

    @ClientCommand("hud", "<health/apm>", "Enable information on screen")
    fun hud(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        val status = if (playerData.hud != null) JsonObject.readJSON(playerData.hud).asArray() else JsonArray()

        fun remove(text: String) {
            var i = 0
            while (i < status.size()) {
                if (status[i].asString() == text) {
                    status.remove(i)
                    break
                } else {
                    i++
                }
            }
        }

        when (arg[0]) {
            "health" -> {
                if (status.contains("health")) {
                    remove("health")
                    playerData.send("command.hud.health.disabled")
                } else {
                    status.add("health")
                    playerData.send("command.hud.health.enabled")
                }
            }

            "apm" -> {
                if (status.contains("apm")) {
                    remove("apm")
                    playerData.send("command.hud.apm.disabled")
                } else {
                    status.add("apm")
                    playerData.send("command.hud.apm.enabled")
                }
            }

            else -> {
                playerData.err("command.hud.not.found")
            }
        }

        playerData.hud = if (status.size() != 0) status.toString() else null
    }

    @ClientCommand("info", "[player...]", "Show player info")
    fun info(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        val bundle = Bundle(playerData.languageTag)
        val timeBundleFormat = "command.info.time"

        fun timeFormat(seconds: Long, msg: String): String {
            val days = seconds / (24 * 60 * 60)
            val hours = (seconds % (24 * 60 * 60)) / (60 * 60)
            val minutes = ((seconds % (24 * 60 * 60)) % (60 * 60)) / 60
            val remainingSeconds = ((seconds % (24 * 60 * 60)) % (60 * 60)) % 60

            return when (msg) {
                timeBundleFormat -> bundle[timeBundleFormat, days, hours, minutes, remainingSeconds]
                "$timeBundleFormat.minimal" -> bundle["$timeBundleFormat.minimal", hours, minutes, remainingSeconds]
                else -> ""
            }
        }

        // todo 코드 정리
        fun show(target: DB.PlayerData): String {
            return """
                ${bundle["command.info.name"]}: ${target.name}[white]
                ${bundle["command.info.placecount"]}: ${target.blockPlaceCount}
                ${bundle["command.info.breakcount"]}: ${target.blockBreakCount}
                ${bundle["command.info.level"]}: ${target.level}
                ${bundle["command.info.exp"]}: ${Exp[target]}
                ${bundle["command.info.joindate"]}: ${
                Timestamp(target.firstPlayDate).toLocalDateTime().format(
                    DateTimeFormatter.ofPattern("YYYY-MM-dd a HH:mm:ss")
                )}
                ${bundle["command.info.playtime"]}: ${timeFormat(target.totalPlayTime, timeBundleFormat)}
                ${bundle["command.info.playtime.current"]}: ${
                timeFormat(
                    target.currentPlayTime,
                    "$timeBundleFormat.minimal"
                )}
                ${bundle["command.info.attackclear"]}: ${target.attackModeClear}
                ${bundle["command.info.pvpwinrate"]}: [green]${target.pvpVictoriesCount}[white]/[scarlet]${target.pvpDefeatCount}[white]([sky]${
                if (target.pvpVictoriesCount + target.pvpDefeatCount != 0) round(
                    target.pvpVictoriesCount.toDouble() / (target.pvpVictoriesCount + target.pvpDefeatCount) * 100
                ) else 0}%[white])
                ${bundle["command.info.joinstacks"]}: ${target.joinStacks}
                """.trimIndent()
        }

        val lineBreak = "\n"
        val close = "info.button.close"
        val ban = "info.button.ban"
        val cancel = "info.button.cancel"

        if (arg.isNotEmpty()) {

            val target = findPlayers(arg[0])
            var targetData: DB.PlayerData? = null
            var isBanned = false

            fun banPlayer(data: DB.PlayerData?) {
                if (data != null) {
                    val ip = Vars.netServer.admins.getInfo(data.uuid).lastIP
                    Vars.netServer.admins.banPlayer(data.uuid)

                    Event.log(Event.LogType.Player, Bundle()["log.player.banned", data.name, ip])
                    database.players.forEach {
                        it.player.sendMessage(Bundle(it.languageTag)["info.banned.message", player.plainName(), data.name])
                    }
                }
            }

            fun unbanPlayer(data: DB.PlayerData?) {
                if (data != null) {
                    val name = data.name
                    val ip = Vars.netServer.admins.getInfo(data.uuid).lastIP

                    if (!Vars.netServer.admins.unbanPlayerID(data.uuid)) {
                        if (!Vars.netServer.admins.unbanPlayerIP(ip)) {
                            playerData.err(PLAYER_NOT_FOUND)
                        } else {
                            playerData.send("command.unban.ip", ip)
                        }
                    } else {
                        playerData.send("command.unban.id", data.uuid)
                    }

                    Event.log(Event.LogType.Player, Bundle()["log.player.unbanned", name, ip])
                }
            }

            val controlMenus = arrayOf(
                arrayOf(bundle[close]),
                arrayOf(bundle[ban], bundle["info.button.kick"])
            )

            val unbanControlMenus = arrayOf(
                arrayOf(bundle[close]),
                arrayOf(bundle[ban], bundle["info.button.kick"])
            )

            val banMenus = arrayOf(
                arrayOf(
                    bundle["info.button.tempban.10min"],
                    bundle["info.button.tempban.1hour"],
                    bundle["info.button.tempban.1day"]
                ),
                arrayOf(
                    bundle["info.button.tempban.1week"],
                    bundle["info.button.tempban.2week"],
                    bundle["info.button.tempban.1month"]
                ),
                arrayOf(bundle["info.button.tempban.permanent"]),
                arrayOf(bundle[close])
            )

            val mainMenu = Menus.registerMenu { p, select ->
                when {
                    select == 1 && !isBanned -> {
                        val innerMenu = Menus.registerMenu { _, s ->
                            val time: Int = when (s) {
                                0 -> 10
                                1 -> 60
                                2 -> 1440
                                3 -> 10080
                                4 -> 20160
                                5 -> 43800
                                6 -> -1
                                else -> 0
                            }

                            try {
                                val timeText = bundle["info.button.tempban.${
                                    when (s) {
                                        0 -> "10min"
                                        1 -> "1hour"
                                        2 -> "1day"
                                        3 -> "1week"
                                        4 -> "2week"
                                        5 -> "1month"
                                        6 -> "permanent"
                                        else -> ""
                                    }
                                }"]

                                if (s <= 5) {
                                    val tempBanConfirmMenu = Menus.registerMenu { _, i ->
                                        if (i == 0) {
                                            targetData!!.banTime = time.toString()
                                            database.queue(targetData!!)
                                            Events.fire(
                                                CustomEvents.PlayerTempBanned(
                                                    targetData!!.name,
                                                    p.plainName(),
                                                    LocalDateTime.now().plusMinutes(time.toLong())
                                                        .format(DateTimeFormatter.ofPattern(STANDARD_DATE))
                                                )
                                            )
                                            banPlayer(targetData)
                                        }
                                    } // 임시 차단
                                    Call.menu(
                                        p.con(),
                                        tempBanConfirmMenu,
                                        bundle["info.tempban.title"],
                                        bundle["info.tempban.confirm", timeText] + lineBreak,
                                        arrayOf(arrayOf(bundle[ban], bundle[cancel]))
                                    )
                                } else if (s == 6) {
                                    val banConfirmMenu = Menus.registerMenu { _, i ->
                                        if (i == 0) {
                                            if (targetData!!.player.con() != null) Call.kick(
                                                targetData!!.player.con(),
                                                Packets.KickReason.banned
                                            )
                                            Events.fire(
                                                CustomEvents.PlayerBanned(
                                                    targetData!!.name,
                                                    targetData!!.uuid,
                                                    currentTime(),
                                                    bundle["info.banned.reason.admin"]
                                                )
                                            )
                                            banPlayer(targetData)
                                        }
                                    } // 영구 차단
                                    Call.menu(
                                        p.con(),
                                        banConfirmMenu,
                                        bundle["info.ban.title"],
                                        bundle["info.ban.confirm"] + lineBreak,
                                        arrayOf(arrayOf(bundle[ban], bundle[cancel]))
                                    )
                                }
                            } catch (_: MissingResourceException) {
                            }
                        }
                        Call.menu(
                            p.con(),
                            innerMenu,
                            bundle["info.tempban.title"],
                            bundle["info.tempban.confirm"] + lineBreak,
                            banMenus
                        )
                    }

                    select == 1 -> {
                        val unbanConfirmMenu = Menus.registerMenu { _, i ->
                            if (i == 0) {
                                targetData!!.banTime = null
                                database.queue(targetData!!)
                                unbanPlayer(targetData)
                                Events.fire(CustomEvents.PlayerUnbanned(targetData!!.name, currentTime()))
                                playerData.send("log.player.unbanned", targetData!!.name, targetData!!.uuid)
                            }
                        }
                        Call.menu(
                            p.con(),
                            unbanConfirmMenu,
                            bundle["info.unban.title"],
                            bundle["info.unban.confirm", targetData!!.name] + lineBreak,
                            arrayOf(arrayOf(bundle["info.button.unban"], bundle[cancel]))
                        )
                    }

                    select == 2 -> {
                        if (targetData != null) {
                            Call.kick(targetData!!.player.con(), Packets.KickReason.kick)
                        }
                    }
                }
            }

            // todo 특정 플레이어 조회 안됨
            if (target != null) {
                isBanned =
                    (Vars.netServer.admins.isIDBanned(target.uuid()) || Vars.netServer.admins.isIPBanned(target.con().address))
                val banned = "\n${bundle["info.banned"]}: $isBanned"
                val other = findPlayerData(target.uuid())
                if (other != null) {
                    val menu = if (Permission.check(other, "info.other")) {
                        arrayOf(arrayOf(bundle[close]))
                    } else if (!isBanned) {
                        controlMenus
                    } else {
                        unbanControlMenus
                    }
                    targetData = other
                    Call.menu(
                        player.con(),
                        mainMenu,
                        bundle["info.admin.title"],
                        show(other) + banned + lineBreak,
                        menu
                    )
                } else {
                    playerData.err(PLAYER_NOT_FOUND)
                }
            } else {
                val p = findPlayersByName(arg[0])
                if (p != null) {
                    isBanned = (Vars.netServer.admins.isIDBanned(p.id) || Vars.netServer.admins.isIPBanned(p.lastIP))
                    val banned = "\n${bundle["info.banned"]}: $isBanned"
                    val other = database[p.id]
                    if (other != null) {
                        val menu = if (Permission.check(other, "info.other")) {
                            arrayOf(arrayOf(bundle[close]))
                        } else if (!isBanned) {
                            controlMenus
                        } else {
                            unbanControlMenus
                        }
                        targetData = other
                        Call.menu(
                            player.con(),
                            mainMenu,
                            bundle["info.admin.title"],
                            show(other) + banned + lineBreak,
                            menu
                        )
                    } else {
                        playerData.err(PLAYER_NOT_REGISTERED)
                    }
                } else {
                    playerData.err(PLAYER_NOT_FOUND)
                }
            }
        } else {
            Call.menu(
                player.con(),
                -1,
                bundle["info.title"],
                show(playerData) + lineBreak,
                arrayOf(arrayOf(bundle[close]))
            )
        }
    }

    @ClientCommand("js", "[code...]", "Execute JavaScript code")
    fun js(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        if (arg.isEmpty()) {
            playerData.err("command.js.invalid")
        } else {
            val output = Vars.mods.scripts.runConsole(arg[0])
            try {
                val errorName = output?.substring(0, output.indexOf(' ') - 1)
                Class.forName("org.mozilla.javascript.$errorName")
                player.sendMessage("> [#ff341c]$output")
            } catch (e: Throwable) {
                player.sendMessage("[scarlet]> $output")
            }
        }
    }

    @ClientCommand("kickall", description = "Kick all players without you.")
    fun kickAll(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        Groups.player.forEach {
            if (!it.admin) Call.kick(it.con, Packets.KickReason.kick)
        }
    }

    @ServerCommand("kickall", description = "Kick all players.")
    fun kickAll(arg: Array<out String>) {
        Groups.player.forEach {
            if (!it.admin) Call.kick(it.con, Packets.KickReason.kick)
        }
        Log.info(Bundle()["command.kickall.done"])
    }

    @ClientCommand("kill", "[player]", "Kill player's unit.")
    fun kill(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        if (arg.isEmpty()) {
            player.unit().kill()
            playerData.send("command.kill.self")
        } else {
            val other = findPlayers(arg[0])
            if (other == null) {
                playerData.err(PLAYER_NOT_FOUND)
            } else {
                other.unit().kill()
                playerData.send("command.kill.done", other.plainName())
            }
        }
    }

    @ServerCommand("kill", "<player>", "Kill player's unit")
    fun kill(arg: Array<out String>) {
        val other = findPlayers(arg[0])
        if (other == null) {
            Log.err(Bundle()[PLAYER_NOT_FOUND])
        } else {
            other.unit().kill()
            Log.info(Bundle()["command.kill.done", other.plainName()])
        }
    }

    @ClientCommand("killall", "[team]", "Kill all enemy units")
    fun killAll(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        val count: Int
        if (arg.isEmpty()) {
            count = Groups.unit.size()
            repeat(Team.all.count()) {
                Groups.unit.each { u: Unit -> if (player.team() == u.team) u.kill() }
            }
        } else {
            val team = selectTeam(arg[0])
            count = Groups.unit.filter { u -> u.team == team }.size
            Groups.unit.each { u -> if (u.team == team) u.kill() }
        }
        playerData.send("command.killall.count", count)
    }

    @ServerCommand("killall", "[team]", "Kill all units")
    fun killAll(arg: Array<out String>) {
        val count: Int
        if (arg.isEmpty()) {
            count = Groups.unit.size()
            repeat(Team.all.count()) {
                Groups.unit.each { u: Unit -> u.kill() }
            }
        } else {
            val team = selectTeam(arg[0])
            count = Groups.unit.filter { u -> u.team == team }.size
            Groups.unit.each { u -> if (u.team == team) u.kill() }
        }
        Log.info(Bundle()["command.killall.count", count])
    }

    @ClientCommand("killunit", "<name> [amount] [team]", "Destroy specific units")
    fun killUnit(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        val unit = Vars.content.units().find { unitType: UnitType -> unitType.name == arg[0] }

        fun destroy(team: Team) {
            if (Groups.unit.size() < arg[1].toInt() || arg[1].toInt() == 0) {
                Groups.unit.forEach { if (it.type() == unit && it.team == team) it.kill() }
            } else {
                var count = 0
                Groups.unit.forEach {
                    if (it.type() == unit && it.team == team && count != arg[1].toInt()) {
                        it.kill()
                        count++
                    }
                }
            }
        }

        if (unit != null) {
            if (arg.size > 1) {
                if (arg[1].toIntOrNull() != null) {
                    if (arg.size == 3) {
                        val team = selectTeam(arg[2])
                        destroy(team)
                    } else {
                        destroy(player.team())
                    }
                } else {
                    playerData.err("command.killunit.invalid.number")
                }
            } else {
                for (it in Groups.unit) {
                    if (it.type() == unit && it.team == player.team()) {
                        it.kill()
                    }
                }
            }
        } else {
            playerData.err("command.killunit.not.found")
        }
    }

    @ServerCommand("killunit", "<name> [amount] [team]", "Destroy specific units")
    fun killUnit(arg: Array<out String>) {
        val unit = Vars.content.units().find { unitType: UnitType -> unitType.name == arg[0] }
        val bundle = Bundle()

        fun destroy(team: Team?) {
            if (Groups.unit.size() < arg[1].toInt() || arg[1].toInt() == 0 && team != null) {
                Groups.unit.forEach { if (it.type() == unit && it.team == team) it.kill() }
            } else {
                // todo 완료시 count 출력
                var count = 0
                Groups.unit.forEach {
                    if (it.type() == unit && count != arg[1].toInt()) {
                        it.kill()
                        count++
                    }
                }
            }
        }

        if (unit != null) {
            if (arg.size > 1) {
                if (arg[1].toIntOrNull() != null) {
                    if (arg.size == 3) {
                        val team = selectTeam(arg[2])
                        destroy(team)
                    } else {
                        destroy(null)
                    }
                } else {
                    Log.err(bundle["command.killunit.invalid.number"])
                }
            } else {
                for (it in Groups.unit) {
                    if (it.type() == unit) {
                        it.kill()
                    }
                }
            }
        } else {
            Log.err(bundle["command.killunit.not.found"])
        }
    }

    @ClientCommand("lang", "<language_tag>", "Set the language for current account")
    fun lang(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        if (arg.isEmpty()) {
            playerData.err("command.language.empty")
            return
        }
        // todo languageTag 를 플레이어 게임 언어로 player.locale()
        playerData.languageTag = arg[0]
        database.queue(playerData)
        playerData.send("command.language.set", Locale(arg[0]).language)
        player.sendMessage(Bundle(arg[0])["command.language.preview", Locale(arg[0]).toLanguageTag()])
    }

    @ClientCommand("log", description = "Enable block history view mode")
    fun log(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        playerData.log = !playerData.log
        val msg = if (playerData.log) {
            "enabled"
        } else {
            "disabled"
        }
        playerData.send("command.log.$msg")
    }

    @ClientCommand("maps", "[page]", "Show server map lists")
    fun maps(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        val list = Vars.maps.all().sortedBy { a -> a.name() }
        val bundle = Bundle(player.locale())
        val prebuilt = ArrayList<Pair<String, Array<Array<String>>>>()
        val buffer = Mathf.ceil(list.size.toFloat() / 6)
        val pages = if (buffer > 1.0) buffer - 1 else 0
        val title = bundle["command.page.server"]

        for (page in 0..pages) {
            val build = StringBuilder()
            for (a in 6 * page until (6 * (page + 1)).coerceAtMost(list.size)) {
                build.append("${list[a].name()}\n[orange]${bundle["command.maps.author"]} ${list[a].author()}[white]\n[gray]ID: $a[green]   ${list[a].width}x${list[a].height}[white]\n\n")
            }

            val options = arrayOf(
                arrayOf("<-", bundle["command.maps.page", page, pages], "->"),
                arrayOf(bundle["command.maps.close"])
            )

            prebuilt.add(Pair(build.toString(), options))
        }

        playerData.status["page"] = "0"

        var mainMenu = 0
        mainMenu = Menus.registerMenu { p, select ->
            var page = playerData.status["page"]!!.toInt()
            when (select) {
                0 -> {
                    if (page != 0) page--
                    Call.menu(p.con(), mainMenu, title, prebuilt[page].first, prebuilt[page].second)
                }

                1 -> {
                    Call.menu(p.con(), mainMenu, title, prebuilt[page].first, prebuilt[page].second)
                }

                2 -> {
                    if (page != pages) page++
                    Call.menu(p.con(), mainMenu, title, prebuilt[page].first, prebuilt[page].second)
                }

                else -> {
                    playerData.status.remove("page")
                }
            }
            playerData.status["page"] = page.toString()
        }
        Call.menu(player.con(), mainMenu, title, prebuilt[0].first, prebuilt[0].second)
    }


    @ClientCommand("meme", "<type>", "Enjoy mindustry meme features!")
    fun meme(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        when (arg[0]) {
            "router" -> {
                val zero = arrayOf(
                    """
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
                            """
                )
                val loop = arrayOf(
                    """
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
                            """
                )
                if (playerData.status.containsKey("router")) {
                    playerData.status.remove("router")
                } else {
                    // todo thread 개선
                    Thread {
                        fun change(name: String) {
                            player.name(name)
                            Threads.sleep(500)
                        }

                        playerData.status["router"] = "true"
                        while (!player.isNull) {
                            loop.forEach {
                                change(it)
                            }
                            if (!playerData.status.containsKey("router")) break
                            Threads.sleep(5000)
                            loop.reversed().forEach {
                                change(it)
                            }
                            zero.forEach {
                                change(it)
                            }
                        }
                    }.start()
                }
            }
        }
    }

    @ClientCommand("motd", description = "Show server's message of the day")
    fun motd(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        val motd = if (root.child("motd/${player.locale()}.txt").exists()) {
            root.child("motd/${player.locale()}.txt").readString()
        } else {
            val file = root.child("motd/en.txt")
            if (file.exists()) file.readString() else ""
        }
        val count = motd.split("\r\n|\r|\n").toTypedArray().size
        if (count > 10) Call.infoMessage(player.con(), motd) else player.sendMessage(motd)
    }

    @ClientCommand("mute", "<player>", "Mute player")
    fun mute(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        val other = findPlayers(arg[0])
        if (other != null) {
            val target = findPlayerData(other.uuid())
            if (target != null) {
                target.mute = true
                database.queue(target)
                playerData.send("command.mute", target.name)
            } else {
                playerData.err(PLAYER_NOT_FOUND)
            }
        } else {
            val p = findPlayersByName(arg[0])
            if (p != null) {
                val a = database[p.id]
                if (a != null) {
                    a.mute = true
                    database.queue(a)
                    playerData.send("command.mute", a.name)
                } else {
                    playerData.err(PLAYER_NOT_REGISTERED)
                }
            } else {
                playerData.err(PLAYER_NOT_FOUND)
            }
        }
    }

    @ServerCommand("mute", "<player>", "Mute player")
    fun mute(arg: Array<out String>) {
        val other = findPlayers(arg[0])
        val bundle = Bundle()
        if (other != null) {
            val target = findPlayerData(other.uuid())
            if (target != null) {
                target.mute = true
                database.queue(target)
                Log.info(bundle["command.mute", target.name])
            } else {
                Log.err(bundle[PLAYER_NOT_FOUND])
            }
        } else {
            val p = findPlayersByName(arg[0])
            if (p != null) {
                val a = database[p.id]
                if (a != null) {
                    a.mute = true
                    database.queue(a)
                    Log.info(bundle["command.mute", a.name])
                } else {
                    Log.err(bundle[PLAYER_NOT_REGISTERED])
                }
            } else {
                Log.err(bundle[PLAYER_NOT_FOUND])
            }
        }
    }

    @ClientCommand("pause", description = "Pause or Unpause map")
    fun pause(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        if (Vars.state.isPaused) {
            Vars.state.set(GameState.State.playing)
            playerData.send("command.pause.unpaused")
        } else {
            Vars.state.set(GameState.State.paused)
            playerData.send("command.pause.paused")
        }
    }

    @ClientCommand("players", "[page]", "Show current players list")
    fun players(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        val bundle = Bundle(player.locale())
        val prebuilt = ArrayList<Pair<String, Array<Array<String>>>>()
        val buffer = Mathf.ceil(database.players.size.toFloat() / 6)
        val pages = if (buffer > 1.0) buffer - 1 else 0
        val title = bundle["command.page.server"]

        for (page in 0..pages) {
            val build = StringBuilder()
            for (a in 6 * page until (6 * (page + 1)).coerceAtMost(database.players.size)) {
                build.append("ID: [gray]${database.players[a].entityid} ${database.players[a].player.coloredName()}\n")
            }

            val options = arrayOf(
                arrayOf("<-", bundle["command.players.page", page, pages], "->"),
                arrayOf(bundle["command.players.close"])
            )

            prebuilt.add(Pair(build.toString(), options))
        }

        playerData.status["page"] = "0"

        var mainMenu = 0
        mainMenu = Menus.registerMenu { p, select ->
            var page = playerData.status["page"]!!.toInt()
            when (select) {
                0 -> {
                    if (page != 0) page--
                    Call.menu(p.con(), mainMenu, title, prebuilt[page].first, prebuilt[page].second)
                }

                1 -> {
                    Call.menu(p.con(), mainMenu, title, prebuilt[page].first, prebuilt[page].second)
                }

                2 -> {
                    if (page != pages) page++
                    Call.menu(p.con(), mainMenu, title, prebuilt[page].first, prebuilt[page].second)
                }

                else -> {
                    playerData.status.remove("page")
                }
            }
            playerData.status["page"] = page.toString()
        }
        Call.menu(player.con(), mainMenu, title, prebuilt[0].first, prebuilt[0].second)
    }

    @ClientCommand("ranking", "<time/exp/attack/place/break/pvp> [page]", "Show player ranking")
    fun ranking(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        val bundle = Bundle(player.locale())
        if (PluginData.isRankingWorking) {
            playerData.err("command.ranking.working")
            return
        }
        Core.app.post {

        }
        daemon.submit(Thread {
            try {
                fun timeFormat(seconds: Long): String {
                    val days = seconds / (24 * 60 * 60)
                    val hours = (seconds % (24 * 60 * 60)) / (60 * 60)
                    val minutes = ((seconds % (24 * 60 * 60)) % (60 * 60)) / 60
                    val remainingSeconds = ((seconds % (24 * 60 * 60)) % (60 * 60)) % 60

                    return bundle["command.info.time", days, hours, minutes, remainingSeconds]
                }

                val firstMessage = when (arg[0].lowercase()) {
                    "time" -> "command.ranking.time"
                    "exp" -> "command.ranking.exp"
                    "attack" -> "command.ranking.attack"
                    "place" -> "command.ranking.place"
                    "break" -> "command.ranking.break"
                    "pvp" -> "command.ranking.pvp"
                    else -> null
                }

                if (firstMessage == null) {
                    playerData.err("command.ranking.wrong")
                    return@Thread
                }

                PluginData.isRankingWorking = true
                Core.app.post { player.sendMessage(bundle["command.ranking.wait"]) }
                val time = mutableMapOf<Pair<String, String>, Long>()
                val exp = mutableMapOf<Pair<String, String>, Int>()
                val attack = mutableMapOf<Pair<String, String>, Int>()
                val placeBlock = mutableMapOf<Pair<String, String>, Int>()
                val breakBlock = mutableMapOf<Pair<String, String>, Int>()
                val pvp = mutableMapOf<Pair<String, String>, Triple<Int, Int, Int>>()

                transaction {
                    if (arg[0].lowercase() == "pvp") {
                        DB.Player.select(
                            DB.Player.name,
                            DB.Player.uuid,
                            DB.Player.hideRanking,
                            DB.Player.pvpVictoriesCount,
                            DB.Player.pvpDefeatCount,
                            DB.Player.pvpEliminationTeamCount
                        ).map {
                            if (!it[DB.Player.hideRanking]) {
                                pvp[Pair(it[DB.Player.name], it[DB.Player.uuid])] = Triple(
                                    it[DB.Player.pvpVictoriesCount],
                                    it[DB.Player.pvpDefeatCount],
                                    it[DB.Player.pvpEliminationTeamCount]
                                )
                            }
                        }
                    } else {
                        val type = when (arg[0].lowercase()) {
                            "time" -> DB.Player.totalPlayTime
                            "exp" -> DB.Player.exp
                            "attack" -> DB.Player.attackModeClear
                            "place" -> DB.Player.blockPlaceCount
                            "break" -> DB.Player.blockBreakCount
                            else -> DB.Player.uuid // dummy
                        }
                        DB.Player.select(DB.Player.name, DB.Player.uuid, DB.Player.hideRanking, type).map {
                            if (!it[DB.Player.hideRanking]) {
                                when (arg[0].lowercase()) {
                                    "time" -> time[Pair(it[DB.Player.name], it[DB.Player.uuid])] =
                                        it[DB.Player.totalPlayTime]

                                    "exp" -> exp[Pair(it[DB.Player.name], it[DB.Player.uuid])] = it[DB.Player.exp]
                                    "attack" -> attack[Pair(it[DB.Player.name], it[DB.Player.uuid])] =
                                        it[DB.Player.attackModeClear]

                                    "place" -> placeBlock[Pair(it[DB.Player.name], it[DB.Player.uuid])] =
                                        it[DB.Player.blockPlaceCount]

                                    "break" -> breakBlock[Pair(it[DB.Player.name], it[DB.Player.uuid])] =
                                        it[DB.Player.blockBreakCount]
                                }
                            }
                        }
                    }
                }

                val d = when (arg[0].lowercase()) {
                    "time" -> time.toList().sortedWith(compareBy { -it.second })
                    "exp" -> exp.toList().sortedWith(compareBy { -it.second })
                    "attack" -> attack.toList().sortedWith(compareBy { -it.second })
                    "place" -> placeBlock.toList().sortedWith(compareBy { -it.second })
                    "break" -> breakBlock.toList().sortedWith(compareBy { -it.second })
                    "pvp" -> pvp.toList().sortedWith(compareBy { -it.second.first })
                    else -> {
                        PluginData.isRankingWorking = false
                        return@Thread
                    }
                }

                val string = StringBuilder()
                val per = 8
                var page = if (arg.size == 2) abs(Strings.parseInt(arg[1])) else 1
                val pages = Mathf.ceil(d.size.toFloat() / per)
                page--

                if (page >= pages || page < 0) {
                    Core.app.post { playerData.err("command.page.range", pages) }
                    PluginData.isRankingWorking = false
                    return@Thread
                }
                string.append(bundle[firstMessage, page + 1, pages] + "\n")

                for (a in per * page until (per * (page + 1)).coerceAtMost(d.size)) {
                    if (arg[0].lowercase() == "pvp") {
                        val rank = d[a].second as Triple<*, *, *>
                        val win = rank.first as Int
                        val defeat = rank.second as Int
                        val elimination = rank.third as Int
                        val rate = round((win.toFloat() / (defeat.toFloat() + elimination.toFloat())) * 100)
                        string.append("[white]$a[] ${d[a].first.first}[white] [yellow]-[] [green]$win${bundle["command.ranking.pvp.win"]}[] / [scarlet]$defeat${bundle["command.ranking.pvp.lose"]}[] ($rate%)\n")
                    } else {
                        val text = if (arg[0].lowercase() == "time") {
                            timeFormat(d[a].second.toString().toLong())
                        } else if (arg[0].lowercase() == "exp") {
                            "Lv.${Exp.calculateLevel(d[a].second as Int)} - ${d[a].second}"
                        } else {
                            d[a].second
                        }
                        string.append("[white]${a + 1}[] ${d[a].first.first}[white] [yellow]-[] $text\n")
                    }
                }
                string.substring(0, string.length - 1)
                if (!playerData.hideRanking) {
                    string.append("[purple]=======================================[]\n")
                    for (a in d.indices) {
                        if (d[a].first.second == player.uuid()) {
                            if (d[a].second is HashMap<*, *>) {
                                val rank = d[a].second as HashMap<*, *>
                                val rate = round(
                                    (rank.keys.first().toString().toFloat() / (rank.keys.first().toString()
                                        .toFloat() + rank.keys.first().toString().toFloat())) * 100
                                )
                                string.append("[white]${a + 1}[] ${d[a].first.first}[white] [yellow]-[] [green]${rank.keys.first()}${bundle["command.ranking.pvp.win"]}[] / [scarlet]${rank.values.first()}${bundle["command.ranking.pvp.lose"]}[] ($rate%)")
                            } else {
                                val text = if (arg[0].lowercase() == "time") {
                                    timeFormat(d[a].second.toString().toLong())
                                } else if (arg[0].lowercase() == "exp") {
                                    "Lv.${Exp.calculateLevel(d[a].second as Int)} - ${d[a].second}"
                                } else {
                                    d[a].second
                                }
                                string.append("[white]${a + 1}[] ${d[a].first.first}[white] [yellow]-[] $text")
                            }
                        }
                    }
                }

                Core.app.post {
                    playerData.lastSentMessage = string.toString()
                    player.sendMessage(string.toString())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Core.app.exit()
            }
            PluginData.isRankingWorking = false
        })
    }

    @ClientCommand("rollback", "<player>", "Undo all actions taken by the player.")
    fun rollback(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        val buffer = worldHistory.toTypedArray()

        buffer.forEach {
            val buf = ArrayList<Event.TileLog>()
            if (it.player.contains(arg[0])) {
                buffer.forEach { two ->
                    if (two.x == it.x && two.y == it.y) {
                        buf.add(two)
                    }
                }

                val last = buf.last()
                val targetTile = Vars.world.tile(last.x.toInt(), last.y.toInt())
                if (last.action == "place") {
                    targetTile.remove()
                } else if (last.action == "break") {
                    targetTile.setBlock(Vars.content.block(last.tile), last.team, last.rotate)

                    for (tile in buf.reversed()) {
                        if (tile.value != null) {
                            targetTile.build.configure(tile.value)
                            break
                        }
                    }
                }

                for (p in Groups.player) {
                    Call.worldDataBegin(p.con)
                    Vars.netServer.sendWorldData(p)
                }
            }
        }
    }

    @ClientCommand("sb", "<parameter> [ip] [parameters...]", "Create a server to server point.")
    fun sb(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        val type = arg[0]
        val x = player.tileX()
        val y = player.tileY()
        val name = Vars.state.map.name()
        var ip = ""
        var port = 6567
        if (arg.size > 1) {
            if (arg[1].contains(":")) {
                val address = arg[1].split(":").toTypedArray()
                ip = address[0]

                if (address[1].toIntOrNull() == null) {
                    playerData.err("command.sb.address.port.invalid")
                    return
                }
                port = address[1].toInt()
            } else {
                ip = arg[1]
            }
        } else if (type != "set" && type != "reset" && ip.isBlank()) {
            playerData.err("command.sb.address.invalid")
            return
        }

        when (type) {
            "set" -> {
                if (PluginData["hubMode"] == null) {
                    PluginData.status.add(Pair("hubMode", Vars.state.map.name()))
                    playerData.send("command.sb.mode.on")
                } else if (PluginData["hubMode"] != null && PluginData["hubMode"] != Vars.state.map.name()) {
                    playerData.send("command.sb.mode.exists")
                } else {
                    PluginData.status.removeIf { p -> p.first == "hubMode" }
                    playerData.send("command.sb.mode.off")
                }
                PluginData.save(false)
            }

            "zone" -> {
                if (!playerData.status.containsKey("hub_first") && !playerData.status.containsKey("hub_second")) {
                    playerData.status["hub_ip"] = ip
                    playerData.status["hub_port"] = port.toString()
                    playerData.status["hub_first"] = "true"
                    playerData.send("command.sb.zone.first")
                } else {
                    playerData.send("command.sb.zone.process")
                }
            }

            "block" -> if (arg.size != 3) {
                playerData.err("command.sb.block.parameter")
            } else {
                val t: Tile = player.tileOn()
                PluginData.warpBlocks.add(
                    PluginData.WarpBlock(
                        name,
                        t.build.tileX(),
                        t.build.tileY(),
                        t.block().name,
                        t.block().size,
                        ip,
                        port,
                        arg[2]
                    )
                )
                playerData.send("command.sb.block.added", "$x:$y", arg[1])
                PluginData.save(false)
            }

            "count" -> {
                if (arg.size < 2) {
                    playerData.err("command.sb.count.parameter")
                } else {
                    PluginData.warpCounts.add(PluginData.WarpCount(name, Vars.world.tile(x, y).pos(), ip, port, 0, 1))
                    playerData.send("command.sb.count", "$x:$y", arg[1])
                    PluginData.save(false)
                }
            }

            "total" -> {
                PluginData.warpTotals.add(PluginData.WarpTotal(name, Vars.world.tile(x, y).pos(), 0, 1))
                playerData.send("command.sb.total", "$x:$y")
                PluginData.save(false)
            }

            "remove" -> {
                PluginData.warpBlocks.removeAll { a -> a.ip == ip && a.port == port }
                PluginData.warpZones.removeAll { a -> a.ip == ip && a.port == port }
                playerData.send("command.sb.removed", arg[1])
                PluginData.save(false)
            }

            "reset" -> {
                PluginData.warpTotals.clear()
                PluginData.warpCounts.clear()
                PluginData.save(false)
            }

            else -> playerData.send("command.sb.help")
        }
    }

    @ClientCommand("setitem", "<item> <amount> [team]", "Set item to team core")
    fun setItem(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        fun set(item: Item) {
            fun s(team: Team) {
                team.core().items[item] =
                    if (team.core().storageCapacity < arg[1].toInt()) team.core().storageCapacity else arg[1].toInt()
            }

            val amount = arg[1].toIntOrNull()
            if (amount != null) {
                if (arg.size == 3) {
                    val team = Team.all.find { a -> a.name == arg[2] }
                    if (team != null) {
                        s(team)
                    } else {
                        playerData.err("command.setitem.wrong.team")
                    }
                } else {
                    s(player.team())
                }
            } else {
                playerData.err("command.setitem.wrong.amount")
            }
        }

        val item = Vars.content.item(arg[0])
        if (item != null) {
            set(item)
        } else if (!arg[0].equals("all", true)) {
            Vars.content.items().forEach {
                set(it)
            }
        } else {
            playerData.err("command.setitem.item.not.exists")
        }
    }

    @ClientCommand("setperm", "<player> <group>", "Set the player's permission group.")
    fun setPerm(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        // todo permission.yml 같이 수정
        val target = findPlayers(arg[0])
        if (target != null) {
            val data = findPlayerData(target.uuid())
            if (data != null) {
                data.permission = arg[1]
                playerData.send("command.setperm.success", data.name, arg[1])
            } else {
                playerData.err(PLAYER_NOT_REGISTERED)
            }
        } else {
            val p = findPlayersByName(arg[1])
            if (p != null) {
                val a = database[p.id]
                if (a != null) {
                    a.permission = arg[1]
                    database.queue(a)
                    playerData.send("command.setperm.success", a.name, arg[1])
                } else {
                    playerData.err(PLAYER_NOT_REGISTERED)
                }
            } else {
                playerData.err(PLAYER_NOT_FOUND)
            }
        }
    }

    @ServerCommand("setperm", "<player> <group>", "Set the player's permission group.")
    fun setPerm(arg: Array<out String>) {
        // todo permission.yml 같이 수정
        val target = findPlayers(arg[0])
        val bundle = Bundle()
        if (target != null) {
            val data = findPlayerData(target.uuid())
            if (data != null) {
                data.permission = arg[1]
                Log.info(bundle["command.setperm.success", data.name, arg[1]])
            } else {
                Log.warn(bundle[PLAYER_NOT_REGISTERED])
            }
        } else {
            val p = findPlayersByName(arg[1])
            if (p != null) {
                val a = database[p.id]
                if (a != null) {
                    a.permission = arg[1]
                    database.queue(a)
                    Log.info(bundle["command.setperm.success", a.name, arg[1]])
                } else {
                    Log.warn(bundle[PLAYER_NOT_REGISTERED])
                }
            } else {
                Log.warn(bundle[PLAYER_NOT_FOUND])
            }
        }
    }

    @ClientCommand("skip", "<wave>", "Start n wave immediately")
    fun skip(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        val wave = arg[0].toIntOrNull()
        if (wave != null) {
            if (wave > 0) {
                val previousWave = Vars.state.wave
                var loop = 0
                while (arg[0].toInt() != loop) {
                    loop++
                    Vars.spawner.spawnEnemies()
                    Vars.state.wave++
                    Vars.state.wavetime = Vars.state.rules.waveSpacing
                }
                playerData.send("command.skip.process", previousWave, Vars.state.wave)
            } else {
                playerData.err("command.skip.number.low")
            }
        } else {
            playerData.err("command.skip.number.invalid")
        }
    }

    @ClientCommand(
        "spawn",
        "<unit/block> <name> [amount/rotate]",
        "Spawn units or block at the player's current location."
    )
    fun spawn(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        val type = arg[0]
        val name = arg[1]
        val parameter = if (arg.size == 3) arg[2].toIntOrNull() else 1
        val spread = (Vars.tilesize * 1.5).toFloat()

        when {
            type.equals("unit", true) -> {
                val unit = Vars.content.units().find { unitType: UnitType -> unitType.name == name }
                if (unit != null) {
                    if (parameter != null) {
                        if (!unit.hidden) {
                            unit.useUnitCap = false
                            PluginData.isCheated = true
                            for (a in 1..parameter) {
                                Tmp.v1.rnd(spread)
                                unit.spawn(player.team(), player.x + Tmp.v1.x, player.y + Tmp.v1.y)
                            }
                        } else {
                            playerData.err("command.spawn.unit.invalid")
                        }
                    } else {
                        playerData.err("command.spawn.number")
                    }
                } else {
                    playerData.err("command.spawn.invalid")
                }
            }

            type.equals("block", true) -> {
                if (Vars.content.blocks().find { a -> a.name == name } != null) {
                    PluginData.isCheated = true
                    Call.constructFinish(
                        player.tileOn(),
                        Vars.content.blocks().find { a -> a.name.equals(name, true) },
                        player.unit(),
                        parameter?.toByte() ?: 0,
                        player.team(),
                        null
                    )
                } else {
                    playerData.err("command.spawn.invalid")
                }
            }

            else -> {
                return
            }
        }
    }

    @ClientCommand("status", description = "Show current server status")
    fun status(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        val bundle = Bundle(player.locale())
        fun longToTime(seconds: Long): String {
            val min = seconds / 60
            val hour = min / 60
            val days = hour / 24
            return String.format("%d:%02d:%02d:%02d", days % 365, hour % 24, min % 60, seconds % 60)
        }

        val message = StringBuilder()
        message.append(
            """
                [#DEA82A]${bundle["command.status.info"]}[]
                [#2B60DE]========================================[]
                ${bundle["command.status.name"]}: ${Vars.state.map.name()}[white]
                ${bundle["command.status.creator"]}: ${Vars.state.map.author()}[white]
                TPS: ${Core.graphics.framesPerSecond}/60
                ${bundle["command.status.banned", Vars.netServer.admins.banned.size]}
                ${bundle["command.status.playtime"]}: ${longToTime(PluginData.playtime)}
                ${bundle["command.status.uptime"]}: ${longToTime(PluginData.uptime)}
            """.trimIndent()
        )

        if (Vars.state.rules.pvp) {
            message.appendLine()
            message.appendLine(
                """
                    [#2B60DE]========================================[]
                    [#DEA82A]${bundle["command.status.pvp"]}[]
                """.trimIndent()
            )

            fun winPercentage(team: Team): Double {
                var players = arrayOf<Pair<Team, Double>>()
                database.players.forEach {
                    val rate = it.pvpVictoriesCount.toDouble() / (it.pvpVictoriesCount + it.pvpDefeatCount).toDouble()
                    players += Pair(it.player.team(), if (rate.isNaN()) 0.0 else rate)
                }

                val targetTeam = players.filter { it.first == team }
                val rate = targetTeam.map { it.second }
                return rate.average()
            }

            val teamRate = mutableMapOf<Team, Double>()
            var teams = arrayOf<Pair<Team, Int>>()
            for (a in Vars.state.teams.active) {
                val rate: Double = winPercentage(a.team)
                teamRate[a.team] = rate
                teams += Pair(a.team, a.players.size)
            }

            teamRate.forEach {
                message.appendLine("${it.key.coloredName()} : ${round(it.value * 100).toInt()}%")
            }

            playerData.lastSentMessage = message.toString().dropLast(1)
            player.sendMessage(message.toString().dropLast(1))
        } else {
            playerData.lastSentMessage = message.toString()
            player.sendMessage(message.toString())
        }
    }

    @ClientCommand("strict", "<player>", "Set whether the target player can build or not.")
    fun strict(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        val other = findPlayers(arg[0])
        if (other != null) {
            val target = findPlayerData(other.uuid())
            if (target != null) {
                if (!target.strict) {
                    target.strict = true
                    database.queue(target)
                    playerData.send("command.strict", target.name)
                } else {
                    target.strict = false
                    database.queue(target)
                    playerData.send("command.strict.undo", target.name)
                }
            } else {
                playerData.err(PLAYER_NOT_FOUND)
            }
        }
    }

    @ServerCommand("strict", "<player>", "Set whether the target player can build or not.")
    fun strict(arg: Array<out String>) {
        val bundle = Bundle()
        val other = findPlayers(arg[0])
        if (other != null) {
            val target = findPlayerData(other.uuid())
            if (target != null) {
                if (!target.strict) {
                    target.strict = true
                    database.queue(target)
                    Log.info(bundle["command.strict", target.name])
                } else {
                    target.strict = false
                    database.queue(target)
                    Log.info(bundle["command.strict.undo", target.name])
                }
            } else {
                Log.err(bundle[PLAYER_NOT_FOUND])
            }
        }
    }

    @ClientCommand("t", "<message...>", "Send a meaage only to your teammates.")
    fun t(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        if (!playerData.mute) {
            Groups.player.each({ p -> p.team() === player.team() }) { o ->
                o.sendMessage("[#" + player.team().color.toString() + "]<T>[] ${player.coloredName()} [orange]>[white] ${arg[0]}")
            }
        }
    }

    @ClientCommand("team", "<team> [name]", "Set player team")
    fun team(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        val team = selectTeam(arg[0])

        if (arg.size == 1) {
            player.team(team)
        } else if (Permission.check(playerData, "team.other")) {
            val other = findPlayers(arg[1])
            if (other != null) {
                other.team(team)
            } else {
                playerData.err(PLAYER_NOT_FOUND)
            }
        }
    }

    @ServerCommand("team", "<team> <name>", "Set player team")
    fun team(arg: Array<out String>) {
        val team = selectTeam(arg[0])
        val other = findPlayers(arg[1])
        if (other != null) {
            other.team(team)
        } else {
            Log.err(Bundle()[PLAYER_NOT_FOUND])
        }
    }

    // todo tempban client -> server
    @ServerCommand("tempban", "<player> <time> [reason]", "Ban the player for aa certain peroid of time")
    fun tempBan(arg: Array<out String>) {
        val bundle = Bundle()
        val other = findPlayers(arg[0])

        if (other == null) {
            Log.err(bundle[PLAYER_NOT_FOUND])
        } else {
            val d = findPlayerData(other.uuid())
            if (d == null) {
                Log.info(bundle["command.tempban.not.registered"])
                Vars.netServer.admins.banPlayer(other.uuid())
                Call.kick(other.con(), Packets.KickReason.banned)
            } else {
                val time = LocalDateTime.now()
                val minute = arg[1].toLongOrNull()
                val reason = arg[2]

                if (minute != null) { // todo d h m s 날짜 형식 지원
                    d.banTime = time.plusMinutes(minute.toLong()).toString()
                    Vars.netServer.admins.banPlayer(other.uuid())
                    Call.kick(other.con(), reason)
                } else {
                    Log.err(bundle["command.tempban.not.number"])
                }
            }
        }
    }

    @ClientCommand("time", description = "Show current server time")
    fun time(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        val now = LocalDateTime.now()
        // todo time format 통합
        val dateTimeFormatter =
            DateTimeFormatter.ofPattern("YYYY-MM-dd a HH:mm:ss").withLocale(Locale(playerData.languageTag))
        playerData.send("command.time", now.format(dateTimeFormatter))
    }

    @ClientCommand("tp", "<player>", "Teleport to other players")
    fun tp(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        val other = findPlayers(arg[0])

        if (other == null) {
            playerData.err(PLAYER_NOT_FOUND)
        } else {
            player.unit()[other.x] = other.y
            Call.setPosition(player.con(), other.x, other.y)
            Call.setCameraPosition(player.con(), other.x, other.y)
        }
    }

    @ClientCommand("tpp", "[player]", "Lock on camera the target player")
    fun tpp(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        if (arg.isEmpty() && playerData.tpp != null && playerData.tppTeam != null) {
            player.team(Team.get(playerData.tppTeam!!))

            playerData.send("command.tpp.unfollowing")
            Call.setCameraPosition(player.con(), player.x, player.y)

            playerData.tppTeam = null
            playerData.tpp = null
        } else {
            val other = findPlayers(arg[0])
            if (other == null) {
                playerData.err(PLAYER_NOT_FOUND)
            } else {
                playerData.tppTeam = player.team().id
                playerData.tpp = other.uuid()
                player.clearUnit()
                player.team(Team.derelict)
                playerData.send("command.tpp.following", other.plainName())
            }
        }

        if (arg.isEmpty() && playerData.tpp != null) {
            playerData.tpp = null
            playerData.tppTeam = 0
        }
    }

    @ClientCommand("track", description = "Display the mouse positions of players.")
    fun track(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        playerData.tracking = !playerData.tracking
        val msg = if (!playerData.tracking) ".disabled" else ""
        playerData.send("command.track.toggle$msg")
    }

    @ClientCommand("unban", "<player>", "Unban player")
    fun unban(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        if (!Vars.netServer.admins.unbanPlayerID(arg[0])) {
            if (!Vars.netServer.admins.unbanPlayerIP(arg[0])) {
                playerData.err(PLAYER_NOT_FOUND)
            } else {
                playerData.send("command.unban.ip", arg[0])
            }
        } else {
            playerData.send("command.unban.id", arg[0])
        }
    }

    @ClientCommand("unmute", "<player>", "Unmute player")
    fun unmute(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        val other = findPlayers(arg[0])
        if (other != null) {
            val target = findPlayerData(other.uuid())
            if (target != null) {
                target.mute = false
                database.queue(target)
                playerData.send("command.unmute", target.name)
            } else {
                playerData.err(PLAYER_NOT_FOUND)
            }
        } else {
            val p = findPlayersByName(arg[0])
            if (p != null) {
                val a = database[p.id]
                if (a != null) {
                    a.mute = false
                    database.queue(a)
                    playerData.send("command.unmute", a.name)
                } else {
                    playerData.err(PLAYER_NOT_REGISTERED)
                }
            } else {
                playerData.err(PLAYER_NOT_FOUND)
            }
        }
    }

    @ServerCommand("unmute", "<player>", "Unmute player")
    fun unmute(arg: Array<out String>) {
        val bundle = Bundle()
        val other = findPlayers(arg[0])
        if (other != null) {
            val target = findPlayerData(other.uuid())
            if (target != null) {
                target.mute = false
                database.queue(target)
                Log.info(bundle["command.unmute", target.name])
            } else {
                Log.warn(bundle[PLAYER_NOT_FOUND])
            }
        } else {
            val p = findPlayersByName(arg[0])
            if (p != null) {
                val a = database[p.id]
                if (a != null) {
                    a.mute = false
                    database.queue(a)
                    Log.info(bundle["command.unmute", a.name])
                } else {
                    Log.warn(bundle[PLAYER_NOT_REGISTERED])
                }
            } else {
                Log.warn(bundle[PLAYER_NOT_FOUND])
            }
        }
    }

    @ClientCommand("url", "<command>", "Opens a URL contained in a specific command.")
    fun url(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        // todo url 목록을 읽고 추가하는 기능 만들기
        when (arg[0]) {
            "effect" -> {
                Call.openURI(
                    player.con(),
                    "https://github.com/Anuken/Mindustry/blob/master/core/src/mindustry/content/Fx.java"
                )
            }

            else -> {}
        }
    }

    @ClientCommand("weather", "<weather> <seconds>", "Adds a weather effect to the map.")
    fun weather(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        val weather = when (arg[0]) {
            "snow" -> Weathers.snow
            "sandstorm" -> Weathers.sandstorm
            "sporestorm" -> Weathers.sporestorm
            "fog" -> Weathers.fog
            "suspendParticles" -> Weathers.suspendParticles
            else -> Weathers.rain
        }
        try {
            val duration = arg[1].toInt()
            Call.createWeather(
                weather,
                (RandomGenerator.of("random").nextDouble() * 100).toFloat(),
                (duration * 8).toFloat(),
                10f,
                10f
            )
        } catch (e: NumberFormatException) {
            playerData.err("command.weather.not.number")
        }
    }

    @ClientCommand("vote", "<kick/map/gg/skip/back/random> [player/amount/world] [reason]", "Start voting")
    fun vote(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        val cooltime = "command.vote.cooltime"
        val noReason = "command.vote.no.reason"
        val mapNotFound = "command.vote.map.not.exists"

        fun sendStart(message: String, vararg parameter: Any) {
            fun sendMessage(data: DB.PlayerData?) {
                if (data != null) {
                    val bundle = Bundle(data.languageTag)
                    data.player.sendMessage(bundle["command.vote.starter", player.plainName()])
                    data.player.sendMessage(bundle.get(message, *parameter))
                    data.player.sendMessage(bundle["command.vote.how"])
                }
            }

            Event.voted.add(player.uuid())
            database.players.forEach {
                if (Event.isPvP) {
                    if (Event.voteTeam == it.player.team()) {
                        sendMessage(findPlayerData(it.uuid))
                    }
                } else {
                    sendMessage(findPlayerData(it.uuid))
                }
            }
        }

        if (arg.isEmpty()) {
            playerData.err("command.vote.arg.empty")
            return
        } else {
            when (arg[0]) {

            }
        }

        if (Event.voterCooltime.containsKey(player.plainName())) {
            playerData.err(cooltime)
            return
        }

        if (!Event.voting) {
            if ((database.players.filterNot { it.afk }.size <= 3 && !Permission.check(playerData, "vote.admin")) ||
                (database.players.size == 1 && arg[0] != "map")) {
                playerData.err("command.vote.enough")
                return
            }
            when (arg[0]) {
                "kick" -> {
                    if (arg.size != 3) {
                        playerData.err(noReason)
                        return
                    }
                    val target = findPlayers(arg[1])
                    if (target != null) {
                        if (Permission.check(playerData, "kick.admin")) {
                            playerData.err("command.vote.kick.target.admin")
                        } else {
                            Event.voteTarget = target
                            Event.voteTargetUUID = target.uuid()
                            Event.voteReason = arg[2]
                            Event.voteType = "kick"
                            Event.voteStarter = playerData
                            Event.voting = true
                            sendStart("command.vote.kick.start", target.plainName(), arg[2])
                        }
                    } else {
                        playerData.err(PLAYER_NOT_FOUND)
                    }
                }

                // vote map <map name> <reason>
                "map" -> {
                    if (arg.size == 1) {
                        playerData.err("command.vote.no.map")
                        return
                    }
                    if (arg.size == 2) {
                        playerData.err(noReason)
                        return
                    }
                    if (arg[1].toIntOrNull() != null) {
                        try {
                            var target: Map? = null
                            val list = Vars.maps.all().sortedBy { a -> a.name() }
                            val arr = HashMap<Map, Int>()
                            list.forEachIndexed { index, map ->
                                arr[map] = index
                            }
                            arr.forEach {
                                if (it.value == arg[1].toInt()) {
                                    target = it.key
                                    return@forEach
                                }
                            }

                            if (target == null) {
                                target = Vars.maps.all().find { e -> e.name().contains(arg[1]) }
                            }

                            if (target != null) {
                                if (database.players.size != 1) {
                                    Event.voteType = "map"
                                    Event.voteMap = target
                                    Event.voteReason = arg[2]
                                    Event.voteStarter = playerData
                                    Event.voting = true
                                    sendStart("command.vote.map.start", target!!.name(), arg[2])
                                } else {
                                    PluginData.isSurrender = true
                                    Vars.maps.setNextMapOverride(voteMap)
                                    Events.fire(GameOverEvent(Vars.state.rules.waveTeam))
                                }
                            } else {
                                playerData.err(mapNotFound)
                            }
                        } catch (e: IndexOutOfBoundsException) {
                            playerData.err(mapNotFound)
                        }
                    } else {
                        playerData.err(mapNotFound)
                    }
                }

                // vote gg
                "gg" -> {
                    if (Event.voteCooltime == 0) {
                        Event.voteType = "gg"
                        Event.voteStarter = playerData
                        Event.voting = true
                        if (Vars.state.rules.pvp) {
                            Event.voteTeam = player.team()
                            Event.isPvP = true
                            Event.voteCooltime = 120
                            sendStart("command.vote.gg.pvp.team")
                        } else {
                            sendStart("command.vote.gg.start")
                        }
                    } else {
                        playerData.err(cooltime)
                    }
                }

                // vote skip <count>
                "skip" -> {
                    if (arg.size == 1) {
                        playerData.send("command.vote.skip.wrong")
                    } else if (arg[1].toIntOrNull() != null) {
                        if (arg[1].toInt() > conf.command.skip.limit) {
                            playerData.send("command.vote.skip.toomany")
                        } else {
                            if (Event.voteCooltime == 0) {
                                Event.voteType = "skip"
                                Event.voteWave = arg[1].toInt()
                                Event.voteStarter = playerData
                                Event.voting = true
                                Event.voteCooltime = 120
                                sendStart("command.vote.skip.start", arg[1])
                            } else {
                                playerData.send(cooltime)
                            }
                        }
                    }
                }

                // vote back <reason>
                "back" -> {
                    if (!Vars.saveDirectory.child("rollback.msav").exists()) {
                        playerData.err("command.vote.back.no.file")
                        return
                    }
                    if (arg.size == 1) {
                        playerData.send(noReason)
                        return
                    }
                    Event.voteType = "back"
                    Event.voteReason = arg[1]
                    Event.voteStarter = playerData
                    Event.voting = true
                    sendStart("command.vote.back.start", arg[1])
                }

                // vote random
                "random" -> {
                    if (Event.voteCooltime == 0) {
                        Event.voteType = "random"
                        Event.voteStarter = playerData
                        Event.voting = true
                        Event.voteCooltime = 360
                        sendStart("command.vote.random.start")
                    } else {
                        playerData.err(cooltime)
                    }
                }

                "reset" -> {
                    resetVote()
                    playerData.send("command.vote.reset")
                }

                else -> {
                    playerData.send("command.help.vote")
                }
            }
        }
    }

    @ClientCommand("votekick", "<player>", "Start kick voting")
    fun votekick(player: Playerc, playerData: DB.PlayerData, arg: Array<out String>) {
        if (arg[0].contains("#")) {
            val target = database.players.find { e ->
                e.uuid == Groups.player.find { p -> p.id() == arg[0].substring(1).toInt() }.uuid()
            }
            if (target != null) {
                if (Permission.check(target, "kick.admin")) {
                    playerData.err("command.vote.kick.target.admin")
                } else {
                    val array = arrayOf("kick", target.name, "Kick")
                    vote(player, playerData, array)
                }
            }
        }
    }

    @ServerCommand("gen", description = "Generate wiki docs")
    fun genDocs(arg: Array<out String>) {
        if (System.getenv("DEBUG_KEY") != null) {
            class StringUtils {
                // Source from https://howtodoinjava.com/java/string/escape-html-encode-string/
                private val htmlEncodeChars = HashMap<Char, String>()
                fun encodeHtml(source: String?): String? {
                    return encode(source)
                }

                private fun encode(source: String?): String? {
                    if (null == source) return null
                    var encode: StringBuilder? = null
                    val encodeArray = source.toCharArray()
                    var match = -1
                    var difference: Int
                    for (i in encodeArray.indices) {
                        val charEncode = encodeArray[i]
                        if (htmlEncodeChars.containsKey(charEncode)) {
                            if (null == encode) encode = StringBuilder(source.length)
                            difference = i - (match + 1)
                            if (difference > 0) encode.appendRange(encodeArray, match + 1, match + 1 + difference)
                            encode.append(htmlEncodeChars[charEncode])
                            match = i
                        }
                    }
                    return if (null == encode) {
                        source
                    } else {
                        difference = encodeArray.size - (match + 1)
                        if (difference > 0) encode.appendRange(encodeArray, match + 1, match + 1 + difference)
                        encode.toString()
                    }
                }

                init {
                    htmlEncodeChars.put('\u0026', "&amp;")
                    htmlEncodeChars.put('\u003C', "&lt;")
                    htmlEncodeChars.put('\u003E', "&gt;")
                    htmlEncodeChars.put('\u0022', "&quot;")
                    htmlEncodeChars.put('\u00A0', "&nbsp;")
                }
            }


            val server = "## Server commands\n| Command | Parameter | Description |\n|:---|:---|:--- |\n"
            val client = "## Client commands\n| Command | Parameter | Description |\n|:---|:---|:--- |\n"
            val time = "README.md Generated time: ${
                DateTimeFormatter.ofPattern(STANDARD_DATE).format(LocalDateTime.now())
            }"

            val result = StringBuilder()

            for (functions in this::class.declaredFunctions) {
                val annotation = functions.findAnnotation<ClientCommand>()
                if (annotation != null) {
                    val temp = "| ${annotation.name} | ${StringUtils().encodeHtml(annotation.parameter)} | ${annotation.description} |\n"
                    result.append(temp)
                }
            }

            val tmp = "$client$result\n\n"

            result.clear()
            for (functions in this::class.declaredFunctions) {
                val annotation = functions.findAnnotation<ServerCommand>()
                if (annotation != null) {
                    val temp = "| ${annotation.name} | ${StringUtils().encodeHtml(annotation.parameter)} | ${annotation.description} |\n"
                    result.append(temp)
                }
            }

            println("$tmp$server$result\n\n\n$time")
        }
    }

    @ServerCommand("reload", description = "Reload essential plugin configs.")
    fun reload(arg: Array<out String>) {
        try {
            Permission.load()
            Log.info(Bundle()["config.permission.updated"])
            conf = Yaml.default.decodeFromString(Config.serializer(), root.child(Main.CONFIG_PATH).readString())
            Log.info(Bundle()["config.reloaded"])
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @ServerCommand("unload", description = "unload essential plugin (experimental)")
    fun unload(arg: Array<out String>) {
        // todo unload 만들기
        Core.app.post {
            Log.info("unloading")
            // 스레드 종료
            daemon.shutdownNow()

            val commands = Commands()

            // 명령어 삭제
            for (functions in commands::class.declaredFunctions) {
                val clients = functions.findAnnotation<ClientCommand>()
                if (clients != null) {
                    Vars.netServer.clientCommands.removeCommand(clients.name)
                } else {
                    val servers = functions.findAnnotation<ServerCommand>()
                    if (servers != null) {
                        instance.handler.removeCommand(servers.name)
                    }
                }
            }

            // 이벤트 삭제
            Event.eventListeners.forEach { (t, u) ->
                // 성공??
                @Suppress("UNCHECKED_CAST")
                Events.remove(t as Class<Any>, u as Cons<Any>)
            }

            Event.coreListeners.forEach {
                Core.app.removeListener(it)
            }

            Vars.netServer.admins.actionFilters.remove(actionFilter)

            // DB 연결 해제
            database.close()

            Vars.mods.getMod("essential").dispose()
            Vars.mods.list().remove(Vars.mods.getMod("essential"))

            // 메모리 정리
            System.gc()
            Log.info("unloaded")
        }
    }

    private fun selectTeam(arg: String): Team {
        return when {
            "derelict".first() == arg.first() -> Team.derelict
            "sharded".first() == arg.first() -> Team.sharded
            "crux".first() == arg.first() -> Team.crux
            "green".first() == arg.first() -> Team.green
            "malis".first() == arg.first() -> Team.malis
            "blue".first() == arg.first() -> Team.blue
            "derelict".contains(arg[0], true) -> Team.derelict
            "sharded".contains(arg[0], true) -> Team.sharded
            "crux".contains(arg[0], true) -> Team.crux
            "green".contains(arg[0], true) -> Team.green
            "malis".contains(arg[0], true) -> Team.malis
            "blue".contains(arg[0], true) -> Team.blue
            else -> Vars.state.rules.defaultTeam
        }
    }

    object Exp {
        private const val BASE_XP = 750
        private const val EXPONENT = 1.06
        private fun calcXpForLevel(level: Int): Double {
            return BASE_XP + BASE_XP * level.toDouble().pow(EXPONENT)
        }

        fun calculateFullTargetXp(level: Int): Double {
            var requiredXP = 0.0
            for (i in 0..level) requiredXP += calcXpForLevel(i)
            return requiredXP
        }

        fun calculateLevel(xp: Int): Int {
            var level = 0
            var maxXp = calcXpForLevel(0)
            do maxXp += calcXpForLevel(++level) while (maxXp < xp)
            return level
        }

        operator fun get(target: DB.PlayerData): String {
            val currentlevel = target.level
            val max = calculateFullTargetXp(currentlevel).toInt()
            val xp = target.exp
            val levelXp = max - xp
            val level = calculateLevel(xp)
            target.level = level
            return "$xp (${floor(levelXp.toDouble()).toInt()}) / ${floor(max.toDouble()).toInt()}"
        }
    }
}