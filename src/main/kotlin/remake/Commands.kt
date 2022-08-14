package remake

import arc.Core
import arc.math.Mathf
import arc.struct.ObjectMap
import arc.struct.Seq
import arc.util.CommandHandler
import arc.util.Log
import arc.util.Strings
import arc.util.Threads
import com.mewna.catnip.Catnip
import com.mewna.catnip.shard.DiscordEvent
import mindustry.Vars
import mindustry.Vars.mods
import mindustry.Vars.netServer
import mindustry.content.Blocks
import mindustry.game.Team
import mindustry.gen.*
import mindustry.gen.Unit
import mindustry.net.Administration
import mindustry.type.UnitType
import mindustry.world.Tile
import remake.Main.Companion.database
import remake.Main.Companion.root
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow


class Commands(handler: CommandHandler, isClient: Boolean) {
    companion object {
        var clientCommands = CommandHandler("/")
        var serverCommands = CommandHandler("")
    }

    init {
        if (isClient) {
            handler.removeCommand("help")

            handler.register("chars", "<text...>", "Make pixel texts") { a, p: Playerc -> Client(a, p).chars(null) }
            handler.register("color", "Enable color nickname") { a, p: Playerc -> Client(a, p).color() }
            handler.register("effect", "[effects]", "effects") { a, p: Playerc -> Client(a, p).effect() }
            handler.register("gg", "[delay]", "Force gameover") { a, p: Playerc -> Client(a, p).gg() }
            handler.register("god", "[name]", "Set max player health") { a, p: Playerc -> Client(a, p).god() }
            handler.register("help", "[page]", "Show command lists") { a, p: Playerc -> Client(a, p).help() }
            handler.register("hub", "<zone/block/count/total> [ip] [parameters...]", "Create a server to server point.") { a, p: Playerc -> Client(a, p).hub() }
            handler.register("info", "Show your information") { a, p: Playerc -> Client(a, p).info() }
            handler.register("js", "[code]", "Execute JavaScript codes") { a, p: Playerc -> Client(a, p).js() }
            handler.register("kill", "[player]", "Kill player.") { a, p: Playerc -> Client(a, p).kill() }
            handler.register("killall", "[team]", "Kill all enemy units") { a, p: Playerc -> Client(a, p).killall() }
            handler.register("login", "<id> <password>", "Access your account") { a, p: Playerc -> Client(a, p).login() }
            handler.register("maps", "[page]", "Show server maps") { a, p: Playerc -> Client(a, p).maps() }
            handler.register("me", "<text...>", "broadcast * message") { a, p: Playerc -> Client(a, p).me() }
            handler.register("meme", "<type>", "Router") { a, p: Playerc -> Client(a, p).meme() }
            handler.register("motd", "Show server motd.") { a, p: Playerc -> Client(a, p).motd() }
            handler.register("mute", "<name>", "Mute player") { a, p: Playerc -> Client(a, p).mute() }
            handler.register("pause", "Pause server") { a, p: Playerc -> Client(a, p).pause() }
            handler.register("players", "[page]", "Show players list") { a, p: Playerc -> Client(a, p).players() }
            handler.register("random", "", "Random events") { a, p: Playerc -> Client(a, p).random() }
            handler.register("reg", "<id> <password> <password_repeat>", "Register account") { a, p: Playerc -> Client(a, p).register() }
            handler.register("search", "[value]", "Search player data") { a, p: Playerc -> Client(a, p).search() }
            handler.register("spawn", "<unit/block> <name> [amount/rotate]", "Spawn mob in player position") { a, p: Playerc -> Client(a, p).spawn() }
            handler.register("status", "Show server status") { a, p: Playerc -> Client(a, p).status() }
            handler.register("team", "<team_name> [name]", "Change team") { a, p: Playerc -> Client(a, p).team() }
            handler.register("time", "Show server time") { a, p: Playerc -> Client(a, p).time() }
            handler.register("tp", "<player>", "Teleport to other players") { a, p: Playerc -> Client(a, p).tp() }
            handler.register("unmute", "<name>", "Unmute player") { a, p: Playerc -> Client(a, p).unmute() }
            handler.register("weather", "<rain/snow/sandstorm/sporestorm> <seconds>", "Change map light") { a, p: Playerc -> Client(a, p).weather() }

            clientCommands = handler
        } else {
            handler.register("gen", "Generate README.md texts") { a -> Server(a).genDocs() }
            handler.register("debug", "[bool]","Show plugin internal informations") { a -> Server(a).debug() }

            serverCommands = handler
        }
    }

    class Client(val arg: Array<String>, val player: Playerc) {
        var bundle = Bundle()
        val data: DB.PlayerData? = findPlayers(player.uuid())

        init {
            if (data != null) bundle = Bundle(data.languageTag)
        }

        fun chars(tile: Tile?) {
            if (tile != null) {
                if (!Permission.check(player, "chars")) return
            }
            // 블록으로 글자 표시
            // arg: <글자>
            if (Vars.world != null) {
                var t = tile ?: Vars.world.tile(player.tileX(), player.tileY())
                val letters = ObjectMap<String, IntArray>()
                letters.put("A", intArrayOf(0, 1, 1, 1, 1, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 1, 1))
                letters.put("B", intArrayOf(1, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0))
                letters.put("C", intArrayOf(0, 1, 1, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1))
                letters.put("D", intArrayOf(1, 1, 1, 1, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 1, 1, 1, 0))
                letters.put("E", intArrayOf(1, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1))
                letters.put("F", intArrayOf(1, 1, 1, 1, 1, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0))
                letters.put("G", intArrayOf(0, 1, 1, 1, 0, 1, 0, 0, 0, 1, 1, 0, 1, 0, 1, 0, 0, 1, 1, 1))
                letters.put("H", intArrayOf(1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1))
                letters.put("I", intArrayOf(1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1))
                letters.put("J", intArrayOf(1, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 1, 1, 1, 0, 1, 0, 0, 0, 0))
                letters.put("K", intArrayOf(1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1))
                letters.put("L", intArrayOf(1, 1, 1, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1))
                letters.put("M", intArrayOf(1, 1, 1, 1, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 1, 1, 1, 1))
                letters.put("N", intArrayOf(1, 1, 1, 1, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1))
                letters.put("O", intArrayOf(0, 1, 1, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 1, 1, 1, 0))
                letters.put("P", intArrayOf(1, 1, 1, 1, 1, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0))
                letters.put("Q", intArrayOf(0, 1, 1, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 1, 1, 1, 0, 0, 0, 0, 0, 1))
                letters.put("R", intArrayOf(1, 1, 1, 1, 1, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 1))
                letters.put("S", intArrayOf(1, 1, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 1, 1, 1))
                letters.put("T", intArrayOf(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0))
                letters.put("U", intArrayOf(1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0))
                letters.put("V", intArrayOf(1, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 1, 1, 0, 0))
                letters.put("W", intArrayOf(1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0))
                letters.put("X", intArrayOf(1, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1))
                letters.put("Y", intArrayOf(1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0))
                letters.put("Z", intArrayOf(1,0,0,0,1,1,0,0,1,1,1,0,1,0,1,1,1,0,0,1,1,0,0,0,1))
                letters.put("0", intArrayOf(1,1,1,1,1,1,0,0,0,1,1,1,1,1,1))
                letters.put("1", intArrayOf(0,1,0,0,1,1,1,1,1,1,0,0,0,0,1))
                letters.put("2", intArrayOf(1,0,1,1,1,1,0,1,0,1,1,1,1,0,1))
                letters.put("3", intArrayOf(1,0,1,0,1,1,0,1,0,1,1,1,1,1,1))
                letters.put("4", intArrayOf(1,1,1,0,0,0,0,1,0,0,1,1,1,1,1))
                letters.put("5", intArrayOf(1,1,1,0,1,1,0,1,0,1,1,0,1,1,1))
                letters.put("6", intArrayOf(1,1,1,1,1,1,0,1,0,1,1,0,1,1,1))
                letters.put("7", intArrayOf(1,1,0,0,0,1,0,0,0,0,1,1,1,1,1))
                letters.put("8", intArrayOf(1,1,1,1,1,1,0,1,0,1,1,1,1,1,1))
                letters.put("9", intArrayOf(1,1,1,0,1,1,0,1,0,1,1,1,1,1,1))
                letters.put("!", intArrayOf(1,1,1,0,1))
                letters.put("?", intArrayOf(0,1,0,0,0,0,1,0,0,1,0,1,0,1,1,0,0,0))
                letters.put(" ", intArrayOf(0,0,0,0,0,0,0,0,0,0))
                letters.put("#", intArrayOf(0,1,0,1,0,1,1,1,1,1,0,1,0,1,0,1,1,1,1,1,0,1,0,1,0))
                letters.put("%", intArrayOf(1,1,0,0,1,1,1,0,1,0,0,0,1,0,0,0,1,0,1,1,1,0,0,1,1))
                letters.put("^", intArrayOf(0,1,1,0,0,1))
                letters.put("&", intArrayOf(0,1,0,1,0,1,0,1,0,1,1,0,1,0,1,0,1,0,1,0,0,0,1,0,1))
                letters.put("*", intArrayOf(0,1,0,1,0,1,0,1,0))
                letters.put("(", intArrayOf(0,1,1,1,0,1,0,0,0,1))
                letters.put(")", intArrayOf(1,0,0,0,1,0,1,1,1,0))
                letters.put(";", intArrayOf(1,0,1,1))
                letters.put(":", intArrayOf(0,1,0,1,0))
                letters.put("'", intArrayOf(1,1))
                letters.put("[", intArrayOf(1,1,1,1,1,1,0,0,0,1))
                letters.put("]", intArrayOf(1,0,0,0,1,1,1,1,1,1))
                letters.put("\"", intArrayOf(1,1,0,0,1,1))

                // TODO 숫자 1~9, ? 가 올바르게 작동 안됨
                val texts = arg[0].toCharArray()
                for (i in texts) {
                    val pos = Seq<IntArray>()
                    val target = letters[i.uppercaseChar().toString()]
                    var xv = 0 // 세로 크기
                    var yv = 0 // 가로 크기
                    // 배열 크기
                    when (target.size) {
                        25 -> {
                            xv = 5
                            yv = 5
                        }
                        20 -> {
                            xv = 5
                            yv = 4
                        }
                        18 -> {
                            xv = 6
                            yv = 3
                        }
                        15 -> {
                            xv = 5
                            yv = 3
                        }
                        10 -> {
                            xv = 5
                            yv = 2
                        }
                        9 -> {
                            xv = 3
                            yv = 3
                        }
                        6 -> {
                            xv = 2
                            yv = 3
                        }
                        4 -> {
                            xv = 4
                            yv = 1
                        }
                        5 -> {
                            xv = 5
                            yv = 1
                        }
                        2 -> {
                            xv = 2
                            yv = 1
                        }
                    }
                    for (y in 0 until yv) {
                        for (x in 0 until xv) {
                            pos.add(intArrayOf(y, -x))
                        }
                    }
                    for (a in 0 until pos.size) {
                        val tar = Vars.world.tile(t.x + pos[a][0], t.y + pos[a][1])
                        if (target[a] == 1) {
                            Call.constructFinish(tar, Blocks.copperWall, Nulls.unit, 0.toByte(), Team.sharded, false)
                        } else if (tar != null) {
                            Call.deconstructFinish(tar, Blocks.air, Nulls.unit)
                        }
                    }
                    val left: Int = when (target.size) {
                        20 -> {
                            xv + 1
                        }
                        15, 18 -> {
                            xv
                        }
                        5 -> {
                            xv - 2
                        }
                        25 -> {
                            xv + 2
                        }
                        else -> {
                            xv - 1
                        }
                    }
                    t = Vars.world.tile(t.x + left, t.y.toInt())
                }
            }
        }

        fun killall() {
            if (!Permission.check(player, "killall")) return
            // 모든 또는 특정 유닛 파괴
            // arg: [팀]
            if (arg.isEmpty()) {
                for (a in Team.all.indices) {
                    Groups.unit.each { u: Unit -> if (player.team() == u.team) u.kill() }
                }
            } else {
                when (arg[0].lowercase()) {
                    "derelict" -> Groups.unit.each { u: Unit -> if (Team.derelict == u.team) u.kill() }
                    "sharded" -> Groups.unit.each { u: Unit -> if (Team.sharded == u.team) u.kill() }
                    "crux" -> Groups.unit.each { u: Unit -> if (Team.crux == u.team) u.kill() }
                    "green" -> Groups.unit.each { u: Unit -> if (Team.green == u.team) u.kill() }
                    "malis" -> Groups.unit.each { u: Unit -> if (Team.malis == u.team) u.kill() }
                    "blue" -> Groups.unit.each { u: Unit -> if (Team.blue == u.team) u.kill() }
                    else -> {
                        player.sendMessage(bundle["command.team.invalid"])
                    }
                }
            }

        }

        fun help() {
            if (!Permission.check(player, "help")) return
            // 명령어 도움말 표시
            if (arg.isNotEmpty() && !Strings.canParseInt(arg[0])) {
                player.sendMessage(bundle["command.page.number"])
                return
            }
            val temp = Seq<String>()
            for (a in 0 until netServer.clientCommands.commandList.size) {
                val command = netServer.clientCommands.commandList[a]
                if (Permission.check(player, command.text)) {
                    temp.add("[orange] /${command.text} [white]${command.paramText} [lightgray]- ${command.description}\n")
                }
            }
            val result = StringBuilder()
            val per = 8
            var page = if (arg.isNotEmpty()) abs(Strings.parseInt(arg[0])) else 1
            val pages = Mathf.ceil(temp.size.toFloat() / per)
            page--

            if (page >= pages || page < 0) {
                player.sendMessage(bundle["command.page.range", pages])
                return
            }

            result.append("[orange]-- ${bundle["command.page"]}[lightgray] ${page + 1}[gray]/[lightgray]${pages}[orange] --\n")
            for (a in per * page until (per * (page + 1)).coerceAtMost(temp.size)) {
                result.append(temp[a])
            }
            player.sendMessage(result.toString().substring(0, result.length - 1))
        }

        fun info() {
            if (!Permission.check(player, "info")) return
            if (data != null) {
                val texts = """
                    ${bundle["name"]}: ${data.name}
                    ${bundle["placecount"]}: ${data.placecount}
                    ${bundle["breakcount"]}: ${data.breakcount}
                    ${bundle["level"]}: ${data.level}
                    ${bundle["exp"]}: ${Exp[data]}
                    ${bundle["joindate"]}: ${Timestamp(data.joinDate).toLocalDateTime().format(DateTimeFormatter.ofPattern("yy-MM-dd HH:mm"))}
                    ${bundle["playtime"]}: ${String.format("%d:%02d:%02d:%02d", (data.playtime / 60 / 60 / 24) % 365, (data.playtime / 60 / 24) % 24, (data.playtime / 60) % 60, (data.playtime) % 60)}
                    ${bundle["attackclear"]}: ${data.attackclear}
                    ${bundle["pvpwincount"]}: ${data.pvpwincount}
                    ${bundle["pvplosecount"]}: ${data.pvplosecount}
                """.trimIndent()
                Call.infoMessage(player.con(), texts)
            }
        }

        fun color() {
            if (!Permission.check(player, "color")) return
            if (data != null) data.colornick = !data.colornick
        }

        fun login() {
            if (!Permission.check(player, "login")) return
            // 계정에 로그인 할 때 사용
            // login <id> <pw>
            val result = database.search(arg[0], arg[1])
            if (result != null) {
                Trigger.loadPlayer(player, result)
            } else {
                player.sendMessage(bundle["account-not-match"])
            }
        }

        fun register() {
            if (!Permission.check(player, "register")) return
            // 계정을 등록할 때 사용
            // reg <id> <pw> <pw_repeat>
            if (arg.size != 3) {
                player.sendMessage(bundle["command.reg.usage"])
            } else if (arg[1] != arg[2]) {
                player.sendMessage(bundle["command.reg.incorrect"])
            } else {
                Trigger.createPlayer(player, arg[0], arg[1])
                Log.info(bundle["log.data_created", player.name()])
            }
        }

        fun maps() {
            if (!Permission.check(player, "maps")) return
            // 서버에 있는 맵 기능 표시
            val list = Vars.maps.all()
            val build = StringBuilder()

            val page = if (arg.isNotEmpty()) arg[0].toInt() else 0

            val buffer = Mathf.ceil(list.size.toFloat() / 6)
            val pages = if (buffer > 1.0) buffer - 1 else 0

            if (page > pages || page < 0) {
                player.sendMessage(bundle["command.page.range", pages])
                return
            }
            build.append("[green]==[white] ${bundle["command.page.server"]} $page/$pages [green]==[white]\n")
            for (a in 6 * page until (6 * (page + 1)).coerceAtMost(list.size)) {
                build.append("[gray]$a[] ${list[a].name()}\n")
            }

            player.sendMessage(build.toString())
        }

        fun me() {
            if (!Permission.check(player, "me")) return
            // 특수 포맷으로 채팅 표시
            Call.sendMessage("*${player.name()}: ${arg[0]}")
        }

        fun motd() {
            if (!Permission.check(player, "motd")) return
            // 서버 motd 표시
            // todo countryCode
            val motd = if (root.child("motd/${data!!.languageTag}.txt").exists()) {
                root.child("motd/${data.languageTag}.txt").readString()
            } else {
                val file = root.child("motd/en.txt")
                if (file.exists()) file.readString() else ""
            }
            val count = motd.split("\r\n|\r|\n").toTypedArray().size
            if (count > 10) Call.infoMessage(player.con(), motd) else player.sendMessage(motd)

        }

        fun players() {
            if (!Permission.check(player, "players")) return
            // 서버에 있는 플레이어 목록 표시
            val message = StringBuilder()
            val page = if (arg.isNotEmpty()) arg[0].toInt() else 0

            val buffer = Mathf.ceil(Groups.player.size().toFloat() / 6)
            val pages = if (buffer > 1.0) buffer - 1 else 0

            if (pages < page) {
                player.sendMessage(bundle["command.page.range", pages])
            } else {
                message.append("[green]==[white] ${bundle["command.page.players"]} [orange]$page[]/[orange]$pages\n")

                val players: Seq<Playerc> = Seq<Playerc>()
                Groups.player.each { e: Playerc -> players.add(e) }

                for (a in 6 * page until (6 * (page + 1)).coerceAtMost(Groups.player.size())) {
                    message.append("[gray]${players.get(a).id()}[white] ${players.get(a).name()}\n")
                }

                player.sendMessage(message.toString().dropLast(1))
            }
        }

        fun spawn() {
            if (!Permission.check(player, "spawn")) return
            // 몹 스폰
            val type = arg[0]
            val name = arg[1]
            val parameter = if (arg.size == 3) arg[2].toIntOrNull() else 1

            when {
                type.equals("unit", true) -> {
                    val unit = Vars.content.units().find { unitType: UnitType -> unitType.name == name }
                    if (unit != null) {
                        if (parameter != null) {
                            if (name != "block") {
                                for (a in 1..parameter) {
                                    val baseUnit = unit.create(player.team())
                                    baseUnit.set(player.x, player.y)
                                    baseUnit.add()
                                }
                            } else {
                                player.sendMessage(bundle["command.spawn.block"])
                            }
                        } else {
                            player.sendMessage(bundle["command.spawn.number"])
                        }
                    } else {
                        val names = StringBuilder()
                        Vars.content.units().each {
                            names.append("${it.name}, ")
                        }
                        player.sendMessage("${bundle["command.spawn.units"]}: ${names.dropLast(2)}")
                    }
                }

                type.equals("block", true) -> {
                    if (Vars.content.blocks().find { a -> a.name == name } != null) {
                        Call.constructFinish(player.tileOn(), Vars.content.blocks().find { a -> a.name.equals(name, true) }, player.unit(), parameter?.toByte() ?: 0, player.team(), null)
                    } else {
                        val names = StringBuilder()
                        Vars.content.blocks().each {
                            names.append("${it.name}, ")
                        }
                        player.sendMessage("${bundle["command.spawn.blocks"]}: ${names.dropLast(2)}")
                    }
                }

                else -> {
                    return
                }
            }
        }

        fun status() {
            if (!Permission.check(player, "status")) return
            // 서버 상태 표시
            val bans = netServer.admins.banned.size

            player.sendMessage(
                """
                [#DEA82A]${bundle["command.status.info"]}[]
                [#2B60DE]========================================[]
                TPS: ${Core.graphics.framesPerSecond}/20
                ${bundle["command.status.banned", bans]}
                ${bundle["command.status.playtime"]}: ${PluginData.playtime}
                ${bundle["command.status.uptime"]}: ${PluginData.uptime}
            """.trimIndent()
            )
        }

        fun team() {
            if (!Permission.check(player, "team")) return
            // 팀 변경
            when (arg[0]) {
                "derelict" -> player.team(Team.derelict)
                "sharded" -> player.team(Team.sharded)
                "crux" -> player.team(Team.crux)
                "green" -> player.team(Team.green)
                "malis" -> player.team(Team.malis)
                "blue" -> player.team(Team.blue)
                else -> {
                    player.sendMessage(bundle["command.team.invalid"])
                }
            }
            if (!Permission.check(player, "team.other") && arg.size > 1) {
                val other = if (arg[1].toIntOrNull() != null) {
                    Groups.player.find { e -> e.id == arg[1].toInt() }
                } else {
                    Groups.player.find { e -> e.name().contains(arg[1]) }
                }
                if (other != null) {
                    when (arg[0]) {
                        "derelict" -> other.team(Team.derelict)
                        "sharded" -> other.team(Team.sharded)
                        "crux" -> other.team(Team.crux)
                        "green" -> other.team(Team.green)
                        "malis" -> other.team(Team.malis)
                        "blue" -> other.team(Team.blue)
                        else -> {
                            player.sendMessage(bundle["command.team.invalid"])
                        }
                    }
                }
            }
        }

        fun time() {
            if (!Permission.check(player, "time")) return
            // 서버 시간 표시
            val now = LocalDateTime.now()
            val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            player.sendMessage("${bundle["command.time"]}: ${now.format(dateTimeFormatter)}")
        }

        fun weather() {
            if (!Permission.check(player, "weather")) return
            // 날씨 기능
        }

        fun mute() {
            if (!Permission.check(player, "mute")) return
            // 특정 플레이어 채팅 금지 기능
            val other = Groups.player.find { p: Playerc -> p.name().equals(arg[0], ignoreCase = true) }
            if (other == null) {
                player.sendMessage(bundle["player.not.found"])
            } else {
                val target = database[other.uuid()]
                if (target != null) {
                    target.mute = true
                    player.sendMessage(bundle["command.mute", target.name])
                } else {
                    player.sendMessage(bundle["player.not.registered"])
                }
            }
        }

        fun unmute() {
            if (!Permission.check(player, "unmute")) return
            // 특정 플레이어 채팅 금지 해제
            val other = Groups.player.find { p: Playerc -> p.name().equals(arg[0], ignoreCase = true) }
            if (other == null) {
                player.sendMessage(bundle["player.not.found"])
            } else {
                val target = database[other.uuid()]
                if (target != null) {
                    target.mute = false
                    player.sendMessage(bundle["command.unmute", target.name])
                } else {
                    player.sendMessage(bundle["player.not.registered"])
                }
            }
        }

        fun effect() {
            if (!Permission.check(player, "effect")) return
            // 효과 표시 기능
            //Effect effect, float x, float y, float rotation, Color color
            //Call.effect()
        }

        fun god() {
            if (!Permission.check(player, "god")) return
            // 무적 기능

            player.unit().health(1.0E8f)
            player.sendMessage(bundle["command.god"])
        }

        fun random() {
            if (!Permission.check(player, "random")) return
            // 무작위 이벤트 기능
        }

        fun pause() {
            if (!Permission.check(player, "pause")) return
            // 서버 일시정지 기능
            Vars.state.serverPaused = !Vars.state.serverPaused
            player.sendMessage(if (Vars.state.serverPaused) bundle["command.pause.paused"] else bundle["command.pause.unpaused"])
        }

        fun js() {
            if (!Permission.check(player, "js")) return
            if (arg.isEmpty()) {
                player.sendMessage(bundle["command.js.invalid"])
            } else {
                val output = mods.scripts.runConsole(arg[0])
                try {
                    val errorName = output?.substring(0, output.indexOf(' ') - 1)
                    Class.forName("org.mozilla.javascript.$errorName")
                    player.sendMessage("> [#ff341c]$output")
                } catch (e: Throwable) {
                    player.sendMessage("[scarlet]> $output")
                }
            }
        }

        fun hub() {
            if (!Permission.check(player, "hub")) return // 서버간 이동 기능
            // <zone/block/count/total> [ip] [parameters...]
            val type = arg[0]
            val x = player.tileX()
            val y = player.tileY()
            val name = Vars.state.map.name()
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
                "zone" -> //ip size clickable
                    if (parameters.size != 2) {
                        player.sendMessage(bundle["command.hub.zone.help"])
                    } else {
                        try {
                            size = parameters[0].toInt()
                            clickable = java.lang.Boolean.parseBoolean(parameters[1])
                        } catch (ignored: NumberFormatException) {
                            player.sendMessage(bundle["command.hub.size.invalid"])
                            return
                        }
                        PluginData.warpZones.add(PluginData.WarpZone(name, Vars.world.tile(x, y).pos(), Vars.world.tile(x + size, y + size).pos(), clickable, ip, port))

                        player.sendMessage(bundle["command.hub.zone.added", "$x:$y", ip, if (clickable) bundle["command.hub.zone.clickable"] else bundle["command.hub.zone.enter"]])
                    }

                "block" -> if (parameters.isEmpty()) {
                    player.sendMessage(bundle["command.hub.block.parameter"])
                } else {
                    val t: Tile = Vars.world.tile(x, y)
                    PluginData.warpBlocks.add(PluginData.WarpBlock(name, t.pos(), t.block().name, t.block().size, ip, port, arg[2]))
                    player.sendMessage(bundle["command.hub.block.added", "$x:$y", ip])
                }

                "count" -> {
                    PluginData.warpCounts.add(PluginData.WarpCount(name, Vars.world.tile(x, y).pos(), ip, port, 0, 0))
                    player.sendMessage(bundle["command.hub.count", "$x:$y", ip])
                }

                "total" -> {
                    PluginData.warpTotals.add(PluginData.WarpTotal(name, Vars.world.tile(x, y).pos(), 0, 0))
                    player.sendMessage(bundle["command.hub.total", "$x:$y"])
                }

                else -> player.sendMessage(bundle["command.hub.help"])
            }
        }

        fun gg() {
            if (!Permission.check(player, "gg")) return
            // 강제 게임오버 기능
            Call.gameOver(Team.crux)
        }

        fun kill() {
            if (!Permission.check(player, "kill")) return
            // 자신 또는 특정 플레이어 유닛을 파괴하는 기능
            if (arg.isEmpty()) {
                player.unit().kill()
            } else {
                val other = Groups.player.find { p: Playerc -> p.name().equals(arg[0], ignoreCase = true) }
                if (other == null) player.sendMessage(bundle["player.not.found"]) else other.unit().kill()
            }

        }

        fun meme() {
            if (!Permission.check(player, "meme")) return
            // 밈
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
                    if (data!!.status.containsKey("router")) {
                        data.status.remove("router")
                    } else {
                        Thread {
                            data.status.put("router", true)
                            while (!player.isNull) {
                                for (d in loop) {
                                    player.name(d)
                                    Threads.sleep(500)
                                }
                                if (!data.status.containsKey("router")) break
                                Threads.sleep(5000)
                                for (i in loop.indices.reversed()) {
                                    player.name(loop[i])
                                    Threads.sleep(500)
                                }
                                for (d in zero) {
                                    player.name(d)
                                    Threads.sleep(500)
                                }
                            }
                        }.start()
                    }
                }

                "music" -> {
                    // Router 의 밈
                }
            }
        }

        fun tp() {
            if (!Permission.check(player, "tp")) return
            // 플레이어에게 이동하는 기능
            val other = if (arg[0].toIntOrNull() != null) {
                Groups.player.find { e -> e.id == arg[0].toInt() }
            } else {
                Groups.player.find { e -> e.name().contains(arg[0]) }
            }

            if (other == null) {
                player.sendMessage(bundle["player.not.found"])
                return
            }
            Call.setPosition(player.con(), other.x, other.y)
        }

        fun search() {
            if (!Permission.check(player, "search")) return
            // 플레이어 데이터 검색 기능
            // arg[0] 이름 또는 uuid 값
            val result = ArrayList<DB.PlayerData?>()

            val data = if (arg[0].toIntOrNull() != null) {
                Groups.player.find { e -> e.id == arg[0].toInt() }
            } else {
                Groups.player.find { e -> e.name().contains(arg[0]) }
            }

            if (data == null) {
                val e = netServer.admins.findByName(arg[0])
                if (e.size > 0) {
                    for (info: Administration.PlayerInfo in e) {
                        result.add(database[info.id])
                    }
                } else {
                    result.add(database[arg[0]])
                }
            } else {
                result.add(database[data.uuid()])
            }

            if (result.size > 0) {
                for (a in result) {
                    if (a != null) {
                        val texts = """
                        name: ${a.name}
                        uuid: ${a.uuid}
                        languageTag: ${a.languageTag}
                        placecount: ${a.placecount}
                        breakcount: ${a.breakcount}
                        joincount: ${a.joincount}
                        kickcount: ${a.kickcount}
                        level: ${a.level}
                        exp: ${a.exp}
                        joinDate: ${a.joinDate}
                        lastdate: ${a.lastdate}
                        playtime: ${a.playtime}
                        attackclear: ${a.attackclear}
                        pvpwincount: ${a.pvpwincount}
                        pvplosecount: ${a.pvplosecount}
                        colornick: ${a.colornick}
                        permission: ${a.permission}
                        mute: ${a.mute}
                        status: ${a.status}
                        """.trimIndent()
                        Log.info(texts)
                    }
                }
            }
        }

        fun discord() {
            if (!Permission.check(player, "discord")) return
            if (data != null) {
                if (!data.status.containsKey("discord")) {
                    val number = if (Discord.pin.containsKey(player.uuid())) {
                        Discord.pin.get(player.uuid())
                    } else {
                        Discord.queue(player)
                    }
                    player.sendMessage(bundle["command.discord.pin", number])
                } else {
                    player.sendMessage(bundle["command.discord.already"])
                }
            }
        }

        fun findPlayers(uuid: String) : DB.PlayerData?{
            return database.players.find { e -> e.uuid == uuid }
        }
    }

    class Server(val arg: Array<String>) {
        fun genDocs(){
            val server = "## Server commands\n| Command | Parameter | Description |\n|:---|:---|:--- |\n"
            val client = "## Client commands\n| Command | Parameter | Description |\n|:---|:---|:--- |\n"
            val time = "README.md Generated time: ${DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now())}"

            val result = StringBuilder()

            for(b in clientCommands.commandList) {
                val temp = "| ${b.text} | ${StringUtils.encodeHtml(b.paramText)} | ${b.description} |\n"
                result.append(temp)
            }

            val tmp = "$client$result\n\n"

            result.clear()
            for(c in serverCommands.commandList) {
                val temp = "| ${c.text} | ${StringUtils.encodeHtml(c.paramText)} | ${c.description} |\n"
                result.append(temp)
            }

            println("$tmp$server$result\n\n\n$time")
        }

        fun debug(){
            if (arg.isNotEmpty()) {
                if (arg[0].toBoolean()) {
                    Core.settings.put("debugMode", true)
                } else {
                    Core.settings.put("debugMode", false)
                }
            }
            println("""
                == PluginData class
                uptime: ${PluginData.uptime}
                playtime: ${PluginData.playtime}
                pluginVersion: ${PluginData.pluginVersion}
                
                warpZones: ${PluginData.warpZones}
                warpBlocks: ${PluginData.warpBlocks}
                warpCounts: ${PluginData.warpCounts}
                warpTotals: ${PluginData.warpTotals}
                blacklist: ${PluginData.blacklist}
                banned: ${PluginData.banned}
                
                == DB class
            """.trimIndent())
            database.players.forEach { println(it.toString()) }
        }
    }

    fun longToDateTime(mils: Long): LocalDateTime {
        return Timestamp(mils).toLocalDateTime()
    }

    fun longToTime(seconds: Long): String {
        val min = seconds / 60
        val hour = min / 60
        val days = hour / 24
        return String.format("%d:%02d:%02d:%02d", days % 365, hour % 24, min % 60, seconds % 60)
    }

    object Exp {
        private const val baseXP = 500
        private const val exponent = 1.12
        private fun calcXpForLevel(level: Int): Double {
            return baseXP + baseXP * level.toDouble().pow(exponent)
        }

        private fun calculateFullTargetXp(level: Int): Double {
            var requiredXP = 0.0
            for (i in 0..level) requiredXP += calcXpForLevel(i)
            return requiredXP
        }

        private fun calculateLevel(xp: Double): Int {
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
            val level = calculateLevel(xp.toDouble())
            target.level = level
            return "$xp (${floor(levelXp.toDouble()).toInt()}) / ${floor(max.toDouble()).toInt()}"
        }
    }

    object Discord {
        val pin: ObjectMap<String, Int> = ObjectMap()
        private lateinit var catnip: Catnip

        init {
            if (Config.botToken.isNotEmpty() && Config.channelToken.isNotEmpty()) {
                catnip = Catnip.catnip(Config.botToken)
            }
        }

        fun start() {
            catnip.observable(DiscordEvent.MESSAGE_CREATE).subscribe({
                if (it.channelIdAsLong().toString() == Config.channelToken && !it.author().bot()) {
                    if (it.content().toIntOrNull() != null) {
                        if (pin.findKey(it.content(), true) != null) {
                            val data = database[pin.findKey(it.content().toInt(), true)]
                            data?.status?.put("discord", it.author().id())
                            pin.remove(pin.findKey(it.content().toInt(), true))
                        }
                    } else {
                        when (it.content()) {
                            "help" -> {

                            }
                        }
                    }
                }
            }) { e: Throwable -> e.printStackTrace() }
        }

        fun queue(player: Playerc): Int {
            val number = (Math.random() * 9999).toInt()
            pin.put(player.uuid(), number)
            return number
        }

        fun shutdownNow() {
            if (Discord::catnip.isInitialized) catnip.shutdown()
        }
    }

    object StringUtils {
        // Source from https://howtodoinjava.com/java/string/escape-html-encode-string/
        private val htmlEncodeChars = ObjectMap<Char, String>()
        fun encodeHtml(source: String?): String? {
            return encode(source)
        }

        private fun encode(source: String?): String? {
            if(null == source) return null
            var encode: StringBuffer? = null
            val encodeArray = source.toCharArray()
            var match = -1
            var difference: Int
            for(i in encodeArray.indices) {
                val charEncode = encodeArray[i]
                if(htmlEncodeChars.containsKey(charEncode)) {
                    if(null == encode) encode = StringBuffer(source.length)
                    difference = i - (match + 1)
                    if(difference > 0) encode.append(encodeArray, match + 1, difference)
                    encode.append(htmlEncodeChars[charEncode])
                    match = i
                }
            }
            return if(null == encode) {
                source
            } else {
                difference = encodeArray.size - (match + 1)
                if(difference > 0) encode.append(encodeArray, match + 1, difference)
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
}