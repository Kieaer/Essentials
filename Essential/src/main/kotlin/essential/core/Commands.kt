package essential.core

import arc.Core
import arc.Events
import arc.func.Cons
import arc.graphics.Color
import arc.graphics.Colors
import arc.math.Mathf
import arc.util.*
import arc.util.Timer
import com.charleskorn.kaml.Yaml
import com.github.lalyos.jfiglet.FigletFont
import essential.bundle.Bundle
import essential.command.ClientCommand
import essential.command.ServerCommand
import essential.core.Event.actionFilter
import essential.core.Event.findPlayerData
import essential.core.Event.findPlayers
import essential.core.Event.findPlayersByName
import essential.core.Event.worldHistory
import essential.core.Main.Companion.conf
import essential.core.Main.Companion.pluginData
import essential.core.Main.Companion.scope
import essential.core.service.vote.VoteData
import essential.core.service.vote.VoteSystem
import essential.core.service.vote.VoteType
import essential.database.data.PlayerData
import essential.database.data.getPlayerData
import essential.database.data.plugin.WarpBlock
import essential.database.data.plugin.WarpCount
import essential.database.data.plugin.WarpTotal
import essential.database.data.update
import essential.database.databaseClose
import essential.database.table.PlayerTable
import essential.event.CustomEvents
import essential.isCheated
import essential.isSurrender
import essential.isVoting
import essential.nextVoteAvailable
import essential.permission.Permission
import essential.playTime
import essential.players
import essential.rootPath
import essential.systemTimezone
import essential.timeSource
import essential.uptime
import essential.util.currentTime
import essential.voterCooldown
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import mindustry.Vars
import mindustry.content.Blocks
import mindustry.content.Weathers
import mindustry.core.GameState
import mindustry.game.EventType.GameOverEvent
import mindustry.game.Gamemode
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Unit
import mindustry.maps.Map
import mindustry.net.Packets
import mindustry.net.WorldReloader
import mindustry.server.ServerControl.instance
import mindustry.type.Item
import mindustry.type.UnitType
import mindustry.ui.Menus
import mindustry.world.Tile
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.util.MissingResourceException
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.round
import kotlin.random.Random
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.time.Duration.Companion.minutes


internal class Commands {
    companion object {
        // 다중 사용 함수
        const val PLAYER_NOT_FOUND = "player.not.found"
        const val PLAYER_NOT_REGISTERED = "player.not.registered"

        /**
         * Calculate the Levenshtein distance between two strings
         */
        private fun levenshteinDistance(s1: String, s2: String): Int {
            val m = s1.length
            val n = s2.length
            val dp = Array(m + 1) { IntArray(n + 1) }

            for (i in 0..m) {
                dp[i][0] = i
            }

            for (j in 0..n) {
                dp[0][j] = j
            }

            for (i in 1..m) {
                for (j in 1..n) {
                    dp[i][j] = if (s1[i - 1] == s2[j - 1]) {
                        dp[i - 1][j - 1]
                    } else {
                        minOf(dp[i - 1][j - 1], minOf(dp[i][j - 1], dp[i - 1][j])) + 1
                    }
                }
            }

            return dp[m][n]
        }
    }

    @ClientCommand("changemap", "<name> [gamemode]", "Change the world or game mode immediately.")
    fun changeMap(playerData: PlayerData, arg: Array<out String>) {
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
                playerData.err("command.changeMap.mode.not.found", arg[1])
            }
        } else {
            playerData.err("command.changeMap.map.not.found", arg[0])
        }
    }

    @ClientCommand("changename", "<target> <new_name>", "Change player name")
    fun changeName(playerData: PlayerData, arg: Array<out String>) {
        suspend fun change(data: PlayerData) {
            newSuspendedTransaction {
                val exists = PlayerData.find { PlayerTable.name eq arg[1] }.firstOrNull()
                if (exists != null) {
                    data.err("command.changeName.exists", arg[1])
                } else {
                    Events.fire(CustomEvents.PlayerNameChanged(data.name, arg[1], data.uuid))
                    if (data.uuid == playerData.uuid) {
                        playerData.send("command.changeName.apply")
                    } else {
                        data.send("command.changeName.apply.other", data.name, arg[1])
                    }
                    data.name = arg[1]
                    data.player.name(arg[1])
                    data.update()
                }
            }
        }

        scope.launch {
            val target = findPlayers(arg[0])
            if (target != null) {
                val data = findPlayerData(target.uuid())
                if (data != null) {
                    change(data)
                } else {
                    playerData.err(PLAYER_NOT_REGISTERED)
                }
            } else {
                val offline = getPlayerData(arg[0])
                if (offline != null) {
                    change(offline)
                } else {
                    playerData.err(PLAYER_NOT_FOUND)
                }
            }
        }
    }

    @ClientCommand("changepw", "<new_password> <password_repeat>", "Change account password.")
    fun changePassword(playerData: PlayerData, arg: Array<out String>) {
        if (arg[0] != arg[1]) {
            playerData.err("command.changePw.same")
            return
        }

        scope.launch {
            val password = BCrypt.hashpw(arg[0], BCrypt.gensalt())
            playerData.accountPW = password
            playerData.update()
            Core.app.post { playerData.send("command.changePw.apply") }
        }
    }

    @ClientCommand("chat", "<on/off>", "Mute all players without admins")
    fun chat(playerData: PlayerData, arg: Array<out String>) {
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
    fun chars(playerData: PlayerData, arg: Array<out String>) {
        if (Vars.world != null) {
            fun convert(text: String): Array<String>? {
                return try {
                    val art =
                        FigletFont.convertOneLine(Main::class.java.classLoader.getResourceAsStream("6x10.flf"), text)
                    art.split("\n").toTypedArray()
                } catch (_: ArrayIndexOutOfBoundsException) {
                    null
                }
            }

            var x = playerData.player.tileX()
            var y = playerData.player.tileY()
            val text = convert(arg[0])
            if (text != null) {
                for (line in text) {
                    for (char in line) {
                        if (char == '#' && Vars.world.tile(x, y).block() != null && Vars.world.tile(x, y)
                                .block() == Blocks.air
                        ) {
                            Call.setTile(Vars.world.tile(x, y), Blocks.scrapWall, playerData.player.team(), 0)
                        }
                        x++
                    }
                    y--
                    x = playerData.player.tileX()
                }
            } else {
                playerData.err("command.char.unsupported")
            }
        }
    }

    @ClientCommand(name = "color", description = "Enable color nickname")
    fun color(playerData: PlayerData, arg: Array<out String>) {
        playerData.animatedName = !playerData.animatedName
    }

    @ClientCommand(
        "effect",
        "<on/off/level> [color]",
        "Turn other players' effects on or off, or set effects and colors for each level."
    )
    fun effect(playerData: PlayerData, arg: Array<out String>) {
        scope.launch {
            when {
                arg[0].toUShortOrNull() != null -> {
                    if (arg[0].toInt() <= playerData.level) {
                        playerData.effectLevel = arg[0].toShortOrNull()
                        if (arg.size == 2) {
                            try {
                                if (Colors.get(arg[1]) == null) {
                                    Color.valueOf(arg[1])
                                }

                                playerData.effectColor = arg[1]
                            } catch (_: IllegalArgumentException) {
                                playerData.err("command.effect.no.color")
                            } catch (_: StringIndexOutOfBoundsException) {
                                playerData.err("command.effect.no.color")
                            }
                        }
                        playerData.update()
                    } else {
                        playerData.err("command.effect.level")
                    }
                }

                arg[0] == "off" -> {
                    playerData.effectVisibility = false
                    playerData.update()
                    playerData.send("command.effect.off")
                }

                arg[0] == "on" -> {
                    playerData.effectVisibility = true
                    playerData.update()
                    playerData.send("command.effect.on")
                }

                else -> {
                    playerData.err("command.effect.invalid")
                }
            }
        }
    }

    @ClientCommand("exp", "<set/hide/add/remove> [values/player] [player]", "Edit account exp values")
    fun exp(playerData: PlayerData, arg: Array<out String>) {
        scope.launch {
            suspend fun set(exp: Int?, type: String) {
                suspend fun set(data: PlayerData) {
                    val previous = data.exp
                    when (type) {
                        "set" -> data.exp = arg[1].toInt()
                        "add" -> data.exp += arg[1].toInt()
                        "remove" -> data.exp -= arg[1].toInt()
                    }
                    playerData.update()
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
                                val a = getPlayerData(p.id)
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
                    if (arg.size >= 2) {
                        set(arg[1].toIntOrNull(), "set")
                    } else {
                        playerData.err("command.exp.invalid")
                    }
                }

                "hide" -> {
                    if (arg.size == 2) {
                        val target = findPlayers(arg[1])
                        if (target != null) {
                            val other = findPlayerData(target.uuid())
                            if (other != null) {
                                other.hideRanking = !other.hideRanking
                                scope.launch { other.update() }
                                val msg = if (other.hideRanking) "hide" else "unhide"
                                playerData.send("command.exp.ranking.$msg")
                                return@launch
                            }
                        } else {
                            playerData.err(PLAYER_NOT_FOUND)
                            return@launch
                        }
                    }

                    playerData.hideRanking = !playerData.hideRanking
                    playerData.update()
                    val msg = if (playerData.hideRanking) "hide" else "unhide"
                    playerData.send("command.exp.ranking.$msg")
                }

                "add" -> {
                    if (arg.size >= 2) {
                        set(arg[1].toIntOrNull(), "add")
                    } else {
                        playerData.err("command.exp.invalid")
                    }
                }

                "remove" -> {
                    if (arg.size >= 2) {
                        set(arg[1].toIntOrNull(), "remove")
                    } else {
                        playerData.err("command.exp.invalid")
                    }
                }

                else -> {
                    playerData.err("command.exp.invalid.command")
                }
            }
        }
    }

    @ClientCommand("fillitems", "[team]", "Fill the core with items.")
    fun fillItems(playerData: PlayerData, arg: Array<out String>) {
        if (arg.isEmpty()) {
            if (Vars.state.teams.cores(playerData.player.team()).isEmpty) {
                playerData.err("command.fillItems.core.empty")
                return
            }

            Vars.content.items().forEach {
                Vars.state.teams.cores(playerData.player.team()).first().items[it] =
                    Vars.state.teams.cores(playerData.player.team()).first().storageCapacity
            }
            playerData.send("command.fillItems.core.filled", playerData.player.team().coloredName())
        } else {
            val team = selectTeam(arg[0])
            if (Vars.state.teams.cores(team).isEmpty) {
                playerData.err("command.fillItems.core.empty")
                return
            }

            Vars.content.items().forEach {
                Vars.state.teams.cores(team).forEach { core ->
                    core.items[it] = core.storageCapacity
                }
            }

            playerData.send("command.fillItems.core.filled", team.coloredName())
        }
    }

    @ClientCommand("gg", "[team]", "Make game over immediately.")
    fun gg(playerData: PlayerData, arg: Array<out String>) {
        if (arg.isEmpty()) {
            Events.fire(GameOverEvent(Vars.state.rules.waveTeam))
        } else {
            Events.fire(GameOverEvent(selectTeam(arg[0])))
        }
    }

    @ClientCommand("god", "[player]", "Set max player health")
    fun god(playerData: PlayerData, arg: Array<out String>) {
        playerData.player.unit().health(1.0E8f)
        playerData.send("command.god")
    }

    @ClientCommand("help", "[page]", "Show command lists")
    fun help(playerData: PlayerData, arg: Array<out String>) {
        if (arg.isNotEmpty() && !Strings.canParseInt(arg[0])) {
            try {
                playerData.send("command.help.${arg[0]}")
            } catch (e: MissingResourceException) {
                playerData.err("command.help.not.exists")
            }
            return
        }

        val temp = ArrayList<String>()
        for (a in 0 until Vars.netServer.clientCommands.commandList.size) {
            val command = Vars.netServer.clientCommands.commandList[a]
            if (Permission.check(playerData, command.text)) {
                val description = try {
                    playerData.bundle["command.description." + command.text.lowercase()]
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

        result.append("[orange]-- ${playerData.bundle["command.page"]}[lightgray] ${page + 1}[gray]/[lightgray]${pages}[orange] --\n")
        for (a in per * page until (per * (page + 1)).coerceAtMost(temp.size)) {
            result.append(temp[a])
        }

        val msg = result.toString().substring(0, result.length - 1)
        playerData.player.sendMessage(msg)
    }

    @ClientCommand("info", "[player...]", "Show player info")
    fun info(playerData: PlayerData, arg: Array<out String>) {
        val bundle = playerData.bundle
        val timeBundleFormat = "command.info.time"

        fun timeFormat(seconds: Int, msg: String): String {
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
        fun show(target: PlayerData): String {
            return """
                ${bundle["command.info.name"]}: ${target.name}[white]
                ${bundle["command.info.placeCount"]}: ${target.blockPlaceCount}
                ${bundle["command.info.breakCount"]}: ${target.blockBreakCount}
                ${bundle["command.info.level"]}: ${target.level}
                ${bundle["command.info.exp"]}: ${Exp[target]}
                ${bundle["command.info.joinDate"]}: ${
                target.firstPlayed.format(LocalDateTime.Formats.ISO)
            }
                ${bundle["command.info.playtime"]}: ${timeFormat(target.totalPlayed, timeBundleFormat)}
                ${bundle["command.info.playtime.current"]}: ${
                timeFormat(
                    target.currentPlayTime,
                    "$timeBundleFormat.minimal"
                )
            }
                ${bundle["command.info.attackClear"]}: ${target.attackClear}
                ${bundle["command.info.pvpWinRate"]}: [green]${target.pvpWinCount}[white]/[scarlet]${target.pvpLoseCount}[white]([sky]${
                if (target.pvpWinCount + target.pvpLoseCount != 0) round(
                    target.pvpWinCount.toDouble() / (target.pvpWinCount + target.pvpLoseCount) * 100
                ) else 0
            }%[white])
                ${bundle["command.info.joinStacks"]}: ${target.attendanceDays}
                Discord: ${if (target.discordID != null) target.discordID else "none"}
                """.trimIndent()
        }

        val lineBreak = "\n"
        val close = "info.button.close"
        val ban = "info.button.ban"
        val cancel = "info.button.cancel"

        if (arg.isNotEmpty() && Permission.check(playerData, "info.other")) {
            val target = findPlayers(arg[0])
            var targetData: PlayerData? = null
            var isBanned = false

            fun banPlayer(data: PlayerData?) {
                if (data != null) {
                    val ip = Vars.netServer.admins.getInfo(data.uuid).lastIP
                    Vars.netServer.admins.banPlayer(data.uuid)

                    Event.log(Event.LogType.Player, Bundle()["log.player.banned", data.name, ip])
                    players.forEach {
                        it.send("info.banned.message", data.player.plainName(), data.name)
                    }
                }
            }

            fun unbanPlayer(data: PlayerData?) {
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
                    bundle["info.button.tempBan.10min"],
                    bundle["info.button.tempBan.1hour"],
                    bundle["info.button.tempBan.1day"]
                ),
                arrayOf(
                    bundle["info.button.tempBan.1week"],
                    bundle["info.button.tempBan.2week"],
                    bundle["info.button.tempBan.1month"]
                ),
                arrayOf(bundle["info.button.tempBan.permanent"]),
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
                                            require (targetData != null) {
                                                "DB error?"
                                            }
                                            targetData!!.banExpireDate = Clock.System.now().plus(time.minutes).toLocalDateTime(systemTimezone)
                                            scope.launch { targetData!!.update() }
                                            Events.fire(
                                                CustomEvents.PlayerTempBanned(
                                                    targetData!!.name,
                                                    p.plainName(),
                                                    Clock.System.now().plus(time.minutes).toString()
                                                )
                                            )
                                            banPlayer(targetData)
                                        }
                                    }
                                    Call.menu(
                                        p.con(),
                                        tempBanConfirmMenu,
                                        bundle["info.tempBan.title"],
                                        bundle["info.tempBan.confirm", timeText] + lineBreak,
                                        arrayOf(arrayOf(bundle[ban], bundle[cancel]))
                                    )
                                } else if (s == 6) {
                                    val banConfirmMenu = Menus.registerMenu { _, i ->
                                        if (i == 0) {
                                            if (targetData!!.player.con() != null) {
                                                targetData!!.player.kick(Packets.KickReason.banned)
                                            }
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
                            bundle["info.tempBan.title"],
                            bundle["info.tempBan.confirm"] + lineBreak,
                            banMenus
                        )
                    }

                    select == 1 -> {
                        val unbanConfirmMenu = Menus.registerMenu { _, i ->
                            if (i == 0) {
                                targetData!!.banExpireDate = null
                                scope.launch { targetData!!.update() }
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
                            targetData!!.player.kick(Packets.KickReason.kick)
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
                        playerData.player.con(),
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
                    scope.launch {
                        isBanned =
                            (Vars.netServer.admins.isIDBanned(p.id) || Vars.netServer.admins.isIPBanned(p.lastIP))
                        val banned = "\n${bundle["info.banned"]}: $isBanned"
                        val other = getPlayerData(p.id)
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
                                playerData.player.con(),
                                mainMenu,
                                bundle["info.admin.title"],
                                show(other) + banned + lineBreak,
                                menu
                            )
                        } else {
                            playerData.err(PLAYER_NOT_REGISTERED)
                        }
                    }
                } else {
                    playerData.err(PLAYER_NOT_FOUND)
                }
            }
        } else {
            Call.menu(
                playerData.player.con(),
                -1,
                bundle["info.title"],
                show(playerData) + lineBreak,
                arrayOf(arrayOf(bundle[close]))
            )
        }
    }

    @ClientCommand("js", "[code...]", "Execute JavaScript code")
    fun js(playerData: PlayerData, arg: Array<out String>) {
        if (arg.isEmpty()) {
            playerData.err("command.js.invalid")
        } else {
            Vars.mods.scripts.runConsole(arg[0]).also { result ->
                try {
                    val errorName: String = result.substring(0, result.indexOf(' ') - 1)
                    Class.forName("org.mozilla.javascript.$errorName")
                    playerData.player.sendMessage("[scarlet]> $result")
                } catch (e: Throwable) {
                    playerData.player.sendMessage("> $result")
                }
            }
        }
    }

    @ClientCommand("kickall", description = "Kick all players without you.")
    fun kickAll(playerData: PlayerData, arg: Array<out String>) {
        Groups.player.forEach {
            if (!it.admin) it.kick(Packets.KickReason.kick)
        }
        if (playerData.player.unit() != null) {
            playerData.send("command.kickAll.done")
        }
    }

    @ServerCommand("kickall", description = "Kick all players.")
    fun kickAll(arg: Array<out String>) {
        Groups.player.forEach {
            if (!it.admin) it.kick(Packets.KickReason.kick)
        }
        Log.info(Bundle()["command.kickAll.done"])
    }

    @ClientCommand("kill", "[player]", "Kill player's unit.")
    fun kill(playerData: PlayerData, arg: Array<out String>) {
        if (arg.isEmpty()) {
            playerData.player.unit().kill()
            playerData.send("command.kill.self")
        } else {
            if (Permission.check(playerData, "kill.other")) {
                val other = findPlayers(arg[0])
                if (other == null) {
                    playerData.err(PLAYER_NOT_FOUND)
                } else {
                    other.unit().kill()
                    playerData.send("command.kill.done", other.plainName())
                }
            } else {
                playerData.send("command.permission.false")
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
    fun killAll(playerData: PlayerData, arg: Array<out String>) {
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
    fun killUnit(playerData: PlayerData, arg: Array<out String>) {
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
                        destroy(playerData.player.team())
                    }
                } else {
                    playerData.err("command.killUnit.invalid.number")
                }
            } else {
                for (it in Groups.unit) {
                    if (it.type() == unit && it.team == playerData.player.team()) {
                        it.kill()
                    }
                }
            }
        } else {
            playerData.err("command.killUnit.not.found")
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
                    Log.err(bundle["command.killUnit.invalid.number"])
                }
            } else {
                for (it in Groups.unit) {
                    if (it.type() == unit) {
                        it.kill()
                    }
                }
            }
        } else {
            Log.err(bundle["command.killUnit.not.found"])
        }
    }

    @ClientCommand("log", description = "Enable block history view mode")
    fun log(playerData: PlayerData, arg: Array<out String>) {
        playerData.viewHistoryMode = !playerData.viewHistoryMode
        val msg = if (playerData.viewHistoryMode) {
            "enabled"
        } else {
            "disabled"
        }
        playerData.send("command.log.$msg")
    }

    @ClientCommand("maps", "[page]", "Show server map lists")
    fun maps(playerData: PlayerData, arg: Array<out String>) {
        val list = Vars.maps.all().sortedBy { a -> a.name() }
        val bundle = playerData.bundle
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
        Call.menu(playerData.player.con(), mainMenu, title, prebuilt[0].first, prebuilt[0].second)
    }

    @ClientCommand("meme", "<type>", "Enjoy mindustry meme features!")
    fun meme(playerData: PlayerData, arg: Array<out String>) {
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
                            """.trimIndent(), """
                            [stat][#404040][]
                            [stat][#404040]
                            [stat][#404040][]
                            [#404040][stat]
                            [stat][#404040][]
                            [stat][#404040][]
                            [stat][#404040]
                            [stat][#404040][][#404040][]
                            """.trimIndent(), """
                            [stat][#404040][][#404040]
                            [stat][#404040][]
                            [#404040][stat]
                            [stat][#404040][]
                            [stat][#404040][]
                            [stat][#404040]
                            [stat][#404040][]
                            [#404040][stat][][stat]
                            """.trimIndent(), """
                            [stat][#404040][][#404040][]
                            [#404040][stat]
                            [stat][#404040][]
                            [stat][#404040][]
                            [stat][#404040]
                            [stat][#404040][]
                            [#404040][stat]
                            [stat][#404040][]
                            """.trimIndent(), """
                            [#404040][stat][][stat]
                            [stat][#404040][]
                            [stat][#404040][]
                            [stat][#404040]
                            [stat][#404040][]
                            [#404040][stat]
                            [stat][#404040][]
                            [stat][#404040][]
                            """.trimIndent()
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
                            """.trimIndent(), """
                            [#6B6B6B][stat][#6B6B6B]
                            [#6B6B6B][stat][#404040][][#6B6B6B]
                            [stat][#404040][]
                            [#404040][]
                            [stat][#404040][]
                            [stat][#404040][]
                            [#6B6B6B][stat][#404040][][#6B6B6B]
                            [#6B6B6B][stat][#6B6B6B]
                            """.trimIndent(), """
                            [#6B6B6B][#585858][stat][][#6B6B6B]
                            [#6B6B6B][#828282][stat][#404040][][][#6B6B6B]
                            [#585858][stat][#404040][][#585858]
                            [stat][#404040][]
                            [stat][#404040][]
                            [#585858][stat][#404040][][#585858]
                            [#6B6B6B][stat][#404040][][#828282][#6B6B6B]
                            [#6B6B6B][#585858][stat][][#6B6B6B]
                            """.trimIndent(), """
                            [#6B6B6B][#585858][#6B6B6B]
                            [#6B6B6B][#828282][stat][][#6B6B6B]
                            [#585858][#6B6B6B][stat][#404040][][#828282][#585858]
                            [#585858][stat][#404040][][#585858]
                            [#585858][stat][#404040][][#585858]
                            [#585858][#6B6B6B][stat][#404040][][#828282][#585858]
                            [#6B6B6B][stat][][#828282][#6B6B6B]
                            [#6B6B6B][#585858][#6B6B6B]
                            """.trimIndent(), """
                            [#6B6B6B][#585858][#6B6B6B]
                            [#6B6B6B][#828282][#6B6B6B]
                            [#585858][#6B6B6B][stat][][#828282][#585858]
                            [#585858][#6B6B6B][stat][#404040][][#828282][#585858]
                            [#585858][#6B6B6B][stat][#404040][][#828282][#585858]
                            [#585858][#6B6B6B][stat][][#828282][#585858]
                            [#6B6B6B][#828282][#6B6B6B]
                            [#6B6B6B][#585858][#6B6B6B]
                            """.trimIndent(), """
                            [#6B6B6B][#585858][#6B6B6B]
                            [#6B6B6B][#828282][#6B6B6B]
                            [#585858][#6B6B6B][#828282][#585858]
                            [#585858][#6B6B6B][stat][#6B6B6B][#828282][#585858]
                            [#585858][#6B6B6B][stat][#6B6B6B][#828282][#585858]
                            [#585858][#6B6B6B][#828282][#585858]
                            [#6B6B6B][#828282][#6B6B6B]
                            [#6B6B6B][#585858][#6B6B6B]
                            """.trimIndent(), """
                            [#6B6B6B][#585858][#6B6B6B]
                            [#6B6B6B][#828282][#6B6B6B]
                            [#585858][#6B6B6B][#828282][#585858]
                            [#585858][#6B6B6B][#828282][#6B6B6B][#828282][#585858]
                            [#585858][#6B6B6B][#828282][#6B6B6B][#828282][#585858]
                            [#585858][#6B6B6B][#828282][#585858]
                            [#6B6B6B][#828282][#6B6B6B]
                            [#6B6B6B][#585858][#6B6B6B]
                            """.trimIndent()
                )
                if (playerData.status.containsKey("router")) {
                    playerData.status.remove("router")
                } else {
                    // todo thread 개선
                    scope.launch {
                        fun change(name: String) {
                            playerData.player.name(name)
                            Threads.sleep(500)
                        }

                        playerData.status["router"] = "true"
                        while (!playerData.player.unit().dead) {
                            loop.forEach {
                                change(it)
                            }
                            if (!playerData.status.containsKey("router")) break
                            delay(5000)
                            loop.reversed().forEach {
                                change(it)
                            }
                            zero.forEach {
                                change(it)
                            }
                        }
                    }
                }
            }
        }
    }

    @ClientCommand("motd", description = "Show server's message of the day")
    fun motd(playerData: PlayerData, arg: Array<out String>) {
        val motd = if (rootPath.child("motd/${playerData.player.locale()}.txt").exists()) {
            rootPath.child("motd/${playerData.player.locale()}.txt").readString()
        } else {
            val file = rootPath.child("motd/en.txt")
            if (file.exists()) file.readString() else ""
        }
        if (motd.isNotEmpty()) {
            val count = motd.split("\r\n|\r|\n").toTypedArray().size
            if (count > 10) Call.infoMessage(playerData.player.con(), motd) else playerData.player.sendMessage(motd)
        } else {
            playerData.send("command.motd.not-found")
        }
    }

    @ClientCommand("mute", "<player>", "Mute player")
    fun mute(playerData: PlayerData, arg: Array<out String>) {
        val other = findPlayers(arg[0])
        scope.launch {
            if (other != null) {
                val target = findPlayerData(other.uuid())
                if (target != null) {
                    target.chatMuted = true
                    target.update()
                    playerData.send("command.mute", target.name)
                } else {
                    playerData.err(PLAYER_NOT_FOUND)
                }
            } else {
                val p = findPlayersByName(arg[0])
                if (p != null) {
                    val a = getPlayerData(p.id)
                    if (a != null) {
                        a.chatMuted = true
                        a.update()
                        playerData.send("command.mute", a.name)
                    } else {
                        playerData.err(PLAYER_NOT_REGISTERED)
                    }
                } else {
                    playerData.err(PLAYER_NOT_FOUND)
                }
            }
        }
    }

    @ServerCommand("mute", "<player>", "Mute player")
    fun mute(arg: Array<out String>) {
        val other = findPlayers(arg[0])
        val bundle = Bundle()
        scope.launch {
            if (other != null) {
                val target = findPlayerData(other.uuid())
                if (target != null) {
                    target.chatMuted = true
                    target.update()
                    Log.info(bundle["command.mute", target.name])
                } else {
                    Log.err(bundle[PLAYER_NOT_FOUND])
                }
            } else {
                val p = findPlayersByName(arg[0])
                if (p != null) {
                    val a = getPlayerData(p.id)
                    if (a != null) {
                        a.chatMuted = true
                        a.update()
                        Log.info(bundle["command.mute", a.name])
                    } else {
                        Log.err(bundle[PLAYER_NOT_REGISTERED])
                    }
                } else {
                    Log.err(bundle[PLAYER_NOT_FOUND])
                }
            }
        }
    }

    @ClientCommand("pause", description = "Pause or Unpause map")
    fun pause(playerData: PlayerData, arg: Array<out String>) {
        if (Vars.state.isPaused) {
            Vars.state.set(GameState.State.playing)
            playerData.send("command.pause.unpaused")
        } else {
            Vars.state.set(GameState.State.paused)
            playerData.send("command.pause.paused")
        }
    }

    @ClientCommand("players", "[page]", "Show current players list")
    fun players(playerData: PlayerData, arg: Array<out String>) {
        val bundle = playerData.bundle
        val prebuilt = ArrayList<Pair<String, Array<Array<String>>>>()
        val buffer = Mathf.ceil(players.size.toFloat() / 6)
        val pages = if (buffer > 1.0) buffer - 1 else 0
        val title = bundle["command.page.server"]

        for (page in 0..pages) {
            val build = StringBuilder()
            for (a in 6 * page until (6 * (page + 1)).coerceAtMost(players.size)) {
                build.append("ID: [gray]${players[a].entityId} ${players[a].player.coloredName()}\n")
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
        Call.menu(playerData.player.con(), mainMenu, title, prebuilt[0].first, prebuilt[0].second)
    }

    @ClientCommand("ranking", "<time/exp/attack/place/break/pvp> [page]", "Show player ranking")
    fun ranking(playerData: PlayerData, arg: Array<out String>) {
        val bundle = playerData.bundle
        scope.launch {
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
                    return@launch
                }

                Core.app.post { playerData.player.sendMessage(bundle["command.ranking.wait"]) }
                val time = mutableMapOf<Pair<String, String>, Int>()
                val exp = mutableMapOf<Pair<String, String>, Int>()
                val attack = mutableMapOf<Pair<String, String>, Int>()
                val placeBlock = mutableMapOf<Pair<String, String>, Int>()
                val breakBlock = mutableMapOf<Pair<String, String>, Int>()
                val pvp = mutableMapOf<Pair<String, String>, Triple<Short, Short, Short>>()

                transaction {
                    if (arg[0].lowercase() == "pvp") {
                        PlayerTable.select(
                            PlayerTable.name,
                            PlayerTable.uuid,
                            PlayerTable.hideRanking,
                            PlayerTable.pvpWinCount,
                            PlayerTable.pvpLoseCount,
                            PlayerTable.pvpEliminatedCount
                        ).map {
                            if (!it[PlayerTable.hideRanking]) {
                                pvp[Pair(it[PlayerTable.name], it[PlayerTable.uuid])] = Triple(
                                    it[PlayerTable.pvpWinCount],
                                    it[PlayerTable.pvpLoseCount],
                                    it[PlayerTable.pvpEliminatedCount]
                                )
                            }
                        }
                    } else {
                        val type = when (arg[0].lowercase()) {
                            "time" -> PlayerTable.totalPlayed
                            "exp" -> PlayerTable.exp
                            "attack" -> PlayerTable.attackClear
                            "place" -> PlayerTable.blockPlaceCount
                            "break" -> PlayerTable.blockBreakCount
                            else -> PlayerTable.uuid // dummy
                        }
                        PlayerTable.select(PlayerTable.name, PlayerTable.uuid, PlayerTable.hideRanking, type).map {
                            if (!it[PlayerTable.hideRanking]) {
                                when (arg[0].lowercase()) {
                                    "time" -> time[Pair(it[PlayerTable.name], it[PlayerTable.uuid])] =
                                        it[PlayerTable.totalPlayed]

                                    "exp" -> exp[Pair(it[PlayerTable.name], it[PlayerTable.uuid])] = it[PlayerTable.exp]
                                    "attack" -> attack[Pair(it[PlayerTable.name], it[PlayerTable.uuid])] =
                                        it[PlayerTable.attackClear]

                                    "place" -> placeBlock[Pair(it[PlayerTable.name], it[PlayerTable.uuid])] =
                                        it[PlayerTable.blockPlaceCount]

                                    "break" -> breakBlock[Pair(it[PlayerTable.name], it[PlayerTable.uuid])] =
                                        it[PlayerTable.blockBreakCount]
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
                        return@launch
                    }
                }

                val string = StringBuilder()
                val per = 8
                var page = if (arg.size == 2) abs(Strings.parseInt(arg[1])) else 1
                val pages = Mathf.ceil(d.size.toFloat() / per)
                page--

                if (page >= pages || page < 0) {
                    Core.app.post { playerData.err("command.page.range", pages) }
                    return@launch
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
                        if (d[a].first.second == playerData.player.uuid()) {
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
                    playerData.player.sendMessage(string.toString())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Core.app.exit()
            }
        }
    }

    @ClientCommand("rollback", "<player>", "Undo all actions taken by the player.")
    fun rollback(playerData: PlayerData, arg: Array<out String>) {
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
            }
        }

        for (p in Groups.player) {
            Call.worldDataBegin(p.con)
            Vars.netServer.sendWorldData(p)
        }
    }

    @ClientCommand("hub", "<parameter> [ip] [parameters...]", "Create a server to server point.")
    fun hub(playerData: PlayerData, arg: Array<out String>) {
        val type = arg[0]
        val x = playerData.player.tileX()
        val y = playerData.player.tileY()
        val name = Vars.state.map.name()
        var ip = ""
        var port = 6567
        if (arg.size > 1) {
            if (arg[1].contains(":")) {
                val address = arg[1].split(":").toTypedArray()
                ip = address[0]

                if (address[1].toIntOrNull() == null) {
                    playerData.err("command.hub.address.port.invalid")
                    return
                }
                port = address[1].toInt()
            } else {
                ip = arg[1]
            }
        } else if (type != "set" && type != "reset" && ip.isBlank()) {
            playerData.err("command.hub.address.invalid")
            return
        }

        scope.launch {
            when (type) {
                "set" -> {
                    if (pluginData.hubMapName == null) {
                        pluginData.hubMapName = Vars.state.map.name()
                        playerData.send("command.hub.mode.on")
                    } else if (pluginData.hubMapName != null && pluginData.hubMapName != Vars.state.map.name()) {
                        playerData.send("command.hub.mode.exists")
                    } else {
                        pluginData.hubMapName = null
                        playerData.send("command.hub.mode.off")
                    }
                    pluginData.update()
                }

                "zone" -> {
                    if (!playerData.status.containsKey("hub_first") && !playerData.status.containsKey("hub_second")) {
                        playerData.status["hub_ip"] = ip
                        playerData.status["hub_port"] = port.toString()
                        playerData.status["hub_first"] = "true"
                        playerData.send("command.hub.zone.first")
                    } else {
                        playerData.send("command.hub.zone.process")
                    }
                }

                "block" -> if (arg.size != 3) {
                    playerData.err("command.hub.block.parameter")
                } else {
                    val t: Tile = playerData.player.tileOn()
                    pluginData.data.warpBlock.add(
                        WarpBlock(
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
                    playerData.send("command.hub.block.added", "$x:$y", arg[1])
                    pluginData.update()
                }

                "count" -> {
                    if (arg.size < 2) {
                        playerData.err("command.hub.count.parameter")
                    } else {
                        pluginData.data.warpCount.add(WarpCount(name, Vars.world.tile(x, y).pos(), ip, port, 0, 1))
                        playerData.send("command.hub.count", "$x:$y", arg[1])
                        pluginData.update()
                    }
                }

                "total" -> {
                    pluginData.data.warpTotal.add(WarpTotal(name, Vars.world.tile(x, y).pos(), 0, 1))
                    playerData.send("command.hub.total", "$x:$y")
                    pluginData.update()
                }

                "remove" -> {
                    pluginData.data.warpBlock.removeAll { a -> a.ip == ip && a.port == port }
                    pluginData.data.warpZone.removeAll { a -> a.ip == ip && a.port == port }
                    playerData.send("command.hub.removed", arg[1])
                    pluginData.update()
                }

                "reset" -> {
                    pluginData.data.warpTotal.clear()
                    pluginData.data.warpCount.clear()
                    pluginData.update()
                }

                else -> playerData.send("command.hub.help")
            }
        }
    }

    @ClientCommand("setitem", "<item> <amount> [team]", "Set item to team core")
    fun setItem(playerData: PlayerData, arg: Array<out String>) {
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
                        playerData.err("command.setItem.wrong.team")
                    }
                } else {
                    s(playerData.player.team())
                }
            } else {
                playerData.err("command.setItem.wrong.amount")
            }
        }

        val item = Vars.content.item(arg[0])
        if (item != null) {
            set(item)
        } else if (arg[0].equals("all", false)) {
            Vars.content.items().forEach {
                set(it)
            }
        } else {
            playerData.err("command.setItem.item.not.exists")
        }
    }

    @ClientCommand("setperm", "<player> <group>", "Set the player's permission group.")
    fun setPerm(playerData: PlayerData, arg: Array<out String>) {
        // todo permission.yml 같이 수정
        val target = findPlayers(arg[0])
        if (target != null) {
            val data = findPlayerData(target.uuid())
            if (data != null) {
                data.permission = arg[1]
                playerData.send("command.setPerm.success", data.name, arg[1])
            } else {
                playerData.err(PLAYER_NOT_REGISTERED)
            }
        } else {
            val p = findPlayersByName(arg[1])
            if (p != null) {
                scope.launch {
                    val a = getPlayerData(p.id)
                    if (a != null) {
                        a.permission = arg[1]
                        a.update()
                        playerData.send("command.setPerm.success", a.name, arg[1])
                    } else {
                        playerData.err(PLAYER_NOT_REGISTERED)
                    }
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
                Log.info(bundle["command.setPerm.success", data.name, arg[1]])
            } else {
                Log.warn(bundle[PLAYER_NOT_REGISTERED])
            }
        } else {
            val p = findPlayersByName(arg[1])
            if (p != null) {
                scope.launch {
                    val a = getPlayerData(p.id)
                    if (a != null) {
                        a.permission = arg[1]
                        a.update()
                        Log.info(bundle["command.setPerm.success", a.name, arg[1]])
                    } else {
                        Log.warn(bundle[PLAYER_NOT_REGISTERED])
                    }
                }
            } else {
                Log.warn(bundle[PLAYER_NOT_FOUND])
            }
        }
    }

    @ClientCommand("skip", "<wave>", "Start n wave immediately")
    fun skip(playerData: PlayerData, arg: Array<out String>) {
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
        "<unit/block> <name> [amount(rotate)/block_team] [unit_team]",
        "Spawn units or block at the player's current location."
    )
    fun spawn(playerData: PlayerData, arg: Array<out String>) {
        val type = arg[0]
        val name = arg[1]
        val parameter = if (arg.size == 3) {
            arg[2].toIntOrNull() ?: selectTeam(arg[2])
        } else {
            1
        }
        val team = if (arg.size == 4) selectTeam(arg[3]) else playerData.player.team()
        val spread = (Vars.tilesize * 1.5).toFloat()

        when {
            type.equals("unit", true) -> {
                val unit = Vars.content.units().find { unitType: UnitType -> unitType.name == name }
                if (unit != null) {
                    if (parameter is Int) {
                        if (!unit.hidden) {
                            unit.useUnitCap = false
                            isCheated = true
                            for (a in 1..parameter) {
                                Tmp.v1.rnd(spread)
                                unit.spawn(team, playerData.player.x + Tmp.v1.x, playerData.player.y + Tmp.v1.y)
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
                    isCheated = true
                    Call.constructFinish(
                        playerData.player.tileOn(),
                        Vars.content.blocks().find { a -> a.name.equals(name, true) },
                        playerData.player.unit(),
                        0,
                        team,
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
    fun status(playerData: PlayerData, arg: Array<out String>) {
        val bundle = playerData.bundle

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
                ${bundle["command.status.playtime"]}: $playTime
                ${bundle["command.status.uptime"]}: $uptime
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
                var player = arrayOf<Pair<Team, Double>>()
                players.forEach {
                    val rate = it.pvpWinCount.toDouble() / (it.pvpWinCount + it.pvpLoseCount).toDouble()
                    player += Pair(it.player.team(), if (rate.isNaN()) 0.0 else rate)
                }

                val targetTeam = player.filter { it.first == team }
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

            playerData.player.sendMessage(message.toString().dropLast(1))
        } else {
            playerData.player.sendMessage(message.toString())
        }
    }

    @ClientCommand("strict", "<player>", "Set whether the target player can build or not.")
    fun strict(playerData: PlayerData, arg: Array<out String>) {
        val other = findPlayers(arg[0])
        if (other != null) {
            val target = findPlayerData(other.uuid())
            if (target != null) {
                target.strictMode = !target.strictMode
                scope.launch { target.update() }
                val undo = if (target.strictMode) ".undo" else ""
                playerData.send("command.strict$undo", target.name)
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
                target.strictMode = !target.strictMode
                scope.launch { target.update() }
                val undo = if (target.strictMode) ".undo" else ""
                Log.info(bundle["command.strict$undo", target.name])
            } else {
                Log.err(bundle[PLAYER_NOT_FOUND])
            }
        }
    }

    @ClientCommand("t", "<message...>", "Send a meaage only to your teammates.")
    fun t(playerData: PlayerData, arg: Array<out String>) {
        if (!playerData.chatMuted) {
            Groups.player.each({ p -> p.team() === playerData.player.team() }) { o ->
                o.sendMessage("[#" + playerData.player.team().color.toString() + "]<T>[] ${playerData.player.coloredName()} [orange]>[white] ${arg[0]}")
            }
        }
    }

    @ClientCommand("team", "<team> [name]", "Set player team")
    fun team(playerData: PlayerData, arg: Array<out String>) {
        val team = selectTeam(arg[0])

        if (arg.size == 1) {
            playerData.player.team(team)
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
                Log.info(bundle["command.tempBan.not.registered"])
                Vars.netServer.admins.banPlayer(other.uuid())
                other.kick(Packets.KickReason.banned)
            } else {
                val minute = arg[1].toIntOrNull()
                val reason = arg[2]

                if (minute != null) {
                    d.banExpireDate = Clock.System.now().plus(minute.minutes).toLocalDateTime(systemTimezone)
                    Vars.netServer.admins.banPlayer(other.uuid())
                    other.kick(reason)
                } else {
                    Log.err(bundle["command.tempBan.not.number"])
                }
            }
        }
    }

    @ClientCommand("time", description = "Show current server time")
    fun time(playerData: PlayerData, arg: Array<out String>) {
        val now = Clock.System.now().toString()
        playerData.send("command.time", now)
    }

    @ClientCommand("tp", "<player>", "Teleport to other players")
    fun tp(playerData: PlayerData, arg: Array<out String>) {
        val other = findPlayers(arg[0])

        if (other == null) {
            playerData.err(PLAYER_NOT_FOUND)
        } else {
            playerData.player.unit()[other.x] = other.y
            Call.setPosition(playerData.player.con(), other.x, other.y)
            Call.setCameraPosition(playerData.player.con(), other.x, other.y)
        }
    }

    @ClientCommand("track", description = "Display the mouse positions of players.")
    fun track(playerData: PlayerData, arg: Array<out String>) {
        playerData.mouseTracking = !playerData.mouseTracking
        val msg = if (!playerData.mouseTracking) ".disabled" else ""
        playerData.send("command.track.toggle$msg")
    }

    @ClientCommand("unban", "<player>", "Unban player")
    fun unban(playerData: PlayerData, arg: Array<out String>) {
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
    fun unmute(playerData: PlayerData, arg: Array<out String>) {
        val other = findPlayers(arg[0])
        scope.launch {
            if (other != null) {
                val target = findPlayerData(other.uuid())
                if (target != null) {
                    target.chatMuted = false
                    target.update()
                    playerData.send("command.unmute", target.name)
                } else {
                    playerData.err(PLAYER_NOT_FOUND)
                }
            } else {
                val p = findPlayersByName(arg[0])
                if (p != null) {
                    val a = getPlayerData(p.id)
                    if (a != null) {
                        a.chatMuted = false
                        a.update()
                        playerData.send("command.unmute", a.name)
                    } else {
                        playerData.err(PLAYER_NOT_REGISTERED)
                    }
                } else {
                    playerData.err(PLAYER_NOT_FOUND)
                }
            }
        }
    }

    @ServerCommand("unmute", "<player>", "Unmute player")
    fun unmute(arg: Array<out String>) {
        val bundle = Bundle()
        val other = findPlayers(arg[0])
        scope.launch {
            if (other != null) {
                val target = findPlayerData(other.uuid())
                if (target != null) {
                    target.chatMuted = false
                    target.update()
                    Log.info(bundle["command.unmute", target.name])
                } else {
                    Log.warn(bundle[PLAYER_NOT_FOUND])
                }
            } else {
                val p = findPlayersByName(arg[0])
                if (p != null) {
                    val a = getPlayerData(p.id)
                    if (a != null) {
                        a.chatMuted = false
                        a.update()
                        Log.info(bundle["command.unmute", a.name])
                    } else {
                        Log.warn(bundle[PLAYER_NOT_REGISTERED])
                    }
                } else {
                    Log.warn(bundle[PLAYER_NOT_FOUND])
                }
            }
        }
    }

    @ClientCommand("url", "<command>", "Opens a URL contained in a specific command.")
    fun url(playerData: PlayerData, arg: Array<out String>) {
        // todo url 목록을 읽고 추가하는 기능 만들기
        when (arg[0]) {
            "effect" -> {
                Call.openURI(
                    playerData.player.con(),
                    "https://github.com/Anuken/Mindustry/blob/master/core/src/mindustry/content/Fx.java"
                )
            }

            else -> {}
        }
    }

    @ClientCommand("weather", "<weather> <seconds>", "Adds a weather effect to the map.")
    fun weather(playerData: PlayerData, arg: Array<out String>) {
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
                (Random.nextDouble() * 100).toFloat(),
                (duration * 8).toFloat(),
                10f,
                10f
            )
        } catch (e: NumberFormatException) {
            playerData.err("command.weather.not.number")
        }
    }

    @ClientCommand("vote", "<kick/map/gg/skip/back/random> [player/amount/world] [reason]", "Start voting")
    fun vote(playerData: PlayerData, arg: Array<out String>) {
        val coolTime = "command.vote.coolTime"
        val noReason = "command.vote.no.reason"
        val mapNotFound = "command.vote.map.not.exists"

        fun start(voteData: VoteData) {
            if (!isVoting) {
                isVoting = true
                Timer.schedule(VoteSystem(voteData), 0f, 1f, 60)
            } else {
                playerData.err("command.vote.process")
            }
        }

        if (arg.isEmpty()) {
            playerData.err("command.vote.arg.empty")
            return
        }

        if (voterCooldown.containsKey(playerData.player.plainName())) {
            playerData.err(coolTime)
            return
        }

        val solo = players.size == 1 && arg[0] == "map"
        if (!solo && players.filter { !it.afk }.size <= 3 && !Permission.check(playerData, "vote.admin")) {
            playerData.err("command.vote.enough")
            return
        }

        when (arg[0]) {
            "kick" -> {
                if (!Permission.check(playerData, "vote.kick")) return
                if (arg.size != 3) {
                    playerData.err(noReason)
                    return
                }
                val target = findPlayers(arg[1])
                if (target != null) {
                    if (Permission.check(playerData, "kick.admin")) {
                        playerData.err("command.vote.kick.target.admin")
                    } else {
                        val voteData = VoteData(
                            target = target,
                            targetUUID = target.uuid(),
                            reason = arg[2],
                            type = VoteType.Kick,
                            starter = playerData
                        )
                        start(voteData)
                    }
                } else {
                    playerData.err(PLAYER_NOT_FOUND)
                }
            }

            // vote map <map name> <reason>
            "map" -> {
                if (!Permission.check(playerData, "vote.map")) return
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
                            target = Vars.maps.all().find { e -> e.plainName().contains(arg[1]) }
                        }

                        if (target != null) {
                            if (players.size != 1) {
                                val voteData = VoteData(
                                    type = VoteType.Map,
                                    map = target,
                                    reason = arg[2],
                                    starter = playerData
                                )
                                start(voteData)
                            } else {
                                isSurrender = true
                                Vars.maps.setNextMapOverride(target)
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
                if (!Permission.check(playerData, "vote.gg")) return
                if (nextVoteAvailable.hasPassedNow()) {
                    val voteData = VoteData(
                        type = VoteType.GameOver,
                        starter = playerData,
                    )
                    if (Vars.state.rules.pvp) {
                        voteData.team = playerData.player.team()
                        nextVoteAvailable = timeSource.markNow().plus(2.minutes)
                    }
                    start(voteData)
                } else {
                    playerData.err(coolTime)
                }
            }

            // vote skip <count>
            "skip" -> {
                if (!Permission.check(playerData, "vote.skip")) return
                if (arg.size == 1) {
                    playerData.send("command.vote.skip.wrong")
                } else if (arg[1].toIntOrNull() != null) {
                    if (arg[1].toInt() > conf.command.skip.limit) {
                        playerData.send("command.vote.skip.tooMany")
                    } else {
                        if (nextVoteAvailable.hasPassedNow()) {
                            val voteData = VoteData(
                                type = VoteType.Skip,
                                wave = arg[1].toInt(),
                                starter = playerData
                            )
                            nextVoteAvailable = timeSource.markNow().plus(2.minutes)
                            start(voteData)
                        } else {
                            playerData.send(coolTime)
                        }
                    }
                }
            }

            // vote back <reason>
            "back" -> {
                if (!Permission.check(playerData, "vote.back")) return
                if (!Vars.saveDirectory.child("rollback.msav").exists()) {
                    playerData.err("command.vote.back.no.file")
                    return
                }
                if (arg.size == 1) {
                    playerData.send(noReason)
                    return
                }
                val voteData = VoteData(
                    type = VoteType.Back,
                    reason = arg[1],
                    starter = playerData
                )
                start(voteData)
            }

            // vote random
            "random" -> {
                if (!Permission.check(playerData, "vote.random")) return
                if (nextVoteAvailable.hasPassedNow() || Permission.check(playerData, "vote.random.bypass")) {
                    val voteData = VoteData(
                        type = VoteType.Random,
                        starter = playerData
                    )
                    nextVoteAvailable = timeSource.markNow().plus(6.minutes)
                    start(voteData)
                } else {
                    playerData.err(coolTime)
                }
            }

            "reset" -> {
                if (!Permission.check(playerData, "vote.reset")) return
                isVoting = false
                nextVoteAvailable = timeSource.markNow()
                voterCooldown.clear()
                playerData.send("command.vote.reset")
            }

            else -> {
                playerData.send("command.help.vote")
            }
        }
    }

    @ClientCommand("votekick", "<player>", "Start kick voting")
    fun votekick(playerData: PlayerData, arg: Array<out String>) {
        if (arg[0].contains("#")) {
            val target = players.find { e ->
                e.uuid == Groups.player.find { p -> p.id() == arg[0].substring(1).toInt() }.uuid()
            }
            if (target != null) {
                if (Permission.check(target, "kick.admin")) {
                    playerData.err("command.vote.kick.target.admin")
                } else {
                    val array = arrayOf("kick", target.name, "Kick")
                    vote(playerData, array)
                }
            }
        }
    }

    @ClientCommand("fuck", "[command]", "Corrects and executes a command with typos")
    fun fuck(playerData: PlayerData, arg: Array<out String>) {
        if (arg.isEmpty()) {
            playerData.err("command.fuck.no.command")
            return
        }

        val inputCommand = arg.joinToString(" ")

        val commandParts = inputCommand.split(" ", limit = 2)
        val commandName = commandParts[0]

        val availableCommands = Vars.netServer.clientCommands.commandList.map { it.text }

        val closestCommand = availableCommands.minByOrNull { levenshteinDistance(commandName, it) }
        if (closestCommand != null) {
            val args = if (commandParts.size > 1) " ${commandParts[1]}" else ""
            val fullCommand = "/$closestCommand$args"
            Vars.netServer.clientCommands.handleMessage(fullCommand, playerData.player)
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
                Clock.System.now().toLocalDateTime(systemTimezone)
                    .format(LocalDateTime.Formats.ISO)
            }"

            val result = StringBuilder()

            for (functions in this::class.declaredFunctions) {
                val annotation = functions.findAnnotation<ClientCommand>()
                if (annotation != null) {
                    val temp =
                        "| ${annotation.name} | ${StringUtils().encodeHtml(annotation.parameter)} | ${annotation.description} |\n"
                    result.append(temp)
                }
            }

            val tmp = "$client$result\n\n"

            result.clear()
            for (functions in this::class.declaredFunctions) {
                val annotation = functions.findAnnotation<ServerCommand>()
                if (annotation != null) {
                    val temp =
                        "| ${annotation.name} | ${StringUtils().encodeHtml(annotation.parameter)} | ${annotation.description} |\n"
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
            conf = Yaml.default.decodeFromString(CoreConfig.serializer(), rootPath.child(Main.CONFIG_PATH).readString())
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
            scope.cancel()

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
            databaseClose()

            Vars.mods.getMod("essential").dispose()
            Vars.mods.list().remove(Vars.mods.getMod("essential"))

            // 메모리 정리
            System.gc()
            Log.info("unloaded")
        }
    }

    @ServerCommand("debug", "[parameter...]", "Debug any commands")
    fun debug(arg: Array<out String>) {
        println(pluginData.toString())
        for (a in players) {
            println(a.toString())
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

        operator fun get(target: PlayerData): String {
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
