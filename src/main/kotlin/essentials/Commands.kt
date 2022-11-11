package essentials

import arc.Core
import arc.graphics.Color
import arc.math.Mathf
import arc.struct.ObjectMap
import arc.struct.Seq
import arc.util.CommandHandler
import arc.util.Log
import arc.util.Strings
import arc.util.Threads.sleep
import com.mewna.catnip.Catnip
import com.mewna.catnip.shard.DiscordEvent
import essentials.Event.findPlayerData
import essentials.Event.findPlayers
import essentials.Main.Companion.database
import essentials.Main.Companion.root
import essentials.Permission.bundle
import mindustry.Vars.*
import mindustry.content.Blocks
import mindustry.content.Fx
import mindustry.content.Weathers
import mindustry.core.GameState
import mindustry.game.Team
import mindustry.gen.*
import mindustry.gen.Unit
import mindustry.net.Administration
import mindustry.net.Packets
import mindustry.type.UnitType
import mindustry.world.Tile
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
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
            if (Config.vote) {
                handler.removeCommand("votekick")
            } else {
                handler.register("vote", "<kick/map/gg/skip/back/random> [player/amount/world_name] [reason]", "Start voting") { a, p: Playerc -> Client(a, p).vote() }
            }

            handler.register("chars", "<text...>", "Make pixel texts") { a, p: Playerc -> Client(a, p).chars(null) }
            handler.register("color", "Enable color nickname") { a, p: Playerc -> Client(a, p).color() }
            handler.register("discord", "Authenticate your Discord account to the server.") { a, p: Playerc -> Client(a, p).discord() }
            handler.register("effect", "[effect] [x] [y] [rotate] [color]", "effects") { a, p: Playerc -> Client(a, p).effect() }
            handler.register("fillitems", "<team>", "Fill the core with items.") { a, p: Playerc -> Client(a, p).fillitems() }
            handler.register("freeze", "<player>", "Stop player unit movement") { a, p: Playerc -> Client(a, p).freeze() }
            handler.register("gg", "Force gameover") { a, p: Playerc -> Client(a, p).gg() }
            handler.register("god", "[name]", "Set max player health") { a, p: Playerc -> Client(a, p).god() }
            handler.register("help", "[page]", "Show command lists") { a, p: Playerc -> Client(a, p).help() }
            handler.register("hub", "<zone/block/count/total> [ip] [parameters...]", "Create a server to server point.") { a, p: Playerc -> Client(a, p).hub() }
            handler.register("info", "[player]", "Show your information") { a, p: Playerc -> Client(a, p).info() }
            handler.register("js", "[code]", "Execute JavaScript codes") { a, p: Playerc -> Client(a, p).js() }
            handler.register("kickall", "All users except yourself and the administrator will be kicked") { a, p: Playerc -> Client(a, p).kickall() }
            handler.register("kill", "[player]", "Kill player.") { a, p: Playerc -> Client(a, p).kill() }
            handler.register("killall", "[team]", "Kill all enemy units") { a, p: Playerc -> Client(a, p).killall() }
            handler.register("lang", "<language_tag>", "Set the language for your account.") { a, p: Playerc -> Client(a, p).language() }
            handler.register("login", "<id> <password>", "Access your account") { a, p: Playerc -> Client(a, p).login() }
            handler.register("maps", "[page]", "Show server maps") { a, p: Playerc -> Client(a, p).maps() }
            handler.register("me", "<text...>", "broadcast * message") { a, p: Playerc -> Client(a, p).me() }
            handler.register("meme", "<type>", "Enjoy meme features!") { a, p: Playerc -> Client(a, p).meme() }
            handler.register("motd", "Show server motd.") { a, p: Playerc -> Client(a, p).motd() }
            handler.register("mute", "<player>", "Mute player") { a, p: Playerc -> Client(a, p).mute() }
            handler.register("pause", "Pause server") { a, p: Playerc -> Client(a, p).pause() }
            handler.register("players", "[page]", "Show players list") { a, p: Playerc -> Client(a, p).players() }
            handler.register("reg", "<id> <password> <password_repeat>", "Register account") { a, p: Playerc -> Client(a, p).register() }
            handler.register("report", "<player> <reason...>", "Report player") { a, p: Playerc -> Client(a, p).report() }
            handler.register("search", "[value]", "Search player data") { a, p: Playerc -> Client(a, p).search() }
            handler.register("setperm", "<player> <group>", "Set the player's permission group.") { a, p: Playerc -> Client(a, p).setperm() }
            handler.register("spawn", "<unit/block> <name> [amount/rotate]", "Spawns units at the player's location.") { a, p: Playerc -> Client(a, p).spawn() }
            handler.register("status", "Show server status") { a, p: Playerc -> Client(a, p).status() }
            handler.register("team", "<team_name> [name]", "Change team") { a, p: Playerc -> Client(a, p).team() }
            handler.register("tempban", "<player> <time> [reason]", "Ban the player for a certain period of time.") { a, p: Playerc -> Client(a, p).tempban() }
            handler.register("time", "Show server time") { a, p: Playerc -> Client(a, p).time() }
            handler.register("tp", "<player>", "Teleport to other players") { a, p: Playerc -> Client(a, p).tp() }
            handler.register("unmute", "<player>", "Unmute player") { a, p: Playerc -> Client(a, p).unmute() }
            handler.register("url", "<command>", "Opens a URL contained in a specific command.") { a, p: Playerc -> Client(a, p).url() }
            handler.register("weather", "<rain/snow/sandstorm/sporestorm> <seconds>", "Adds a weather effect to the map.") { a, p: Playerc -> Client(a, p).weather() }
            clientCommands = handler
        } else {
            handler.register("debug", "[bool]", "Show plugin internal informations") { a -> Server(a).debug() }
            handler.register("gen", "Generate README.md texts") { a -> Server(a).genDocs() }
            handler.register("setperm", "<player> <group>", "Set the player's permission group.") { a -> Server(a).setperm() }
            handler.register("tempban", "<player> <time> [reason]", "Ban the player for a certain period of time.") { a -> Server(a).tempban() }
            serverCommands = handler
        }
    }

    class Client(val arg: Array<String>, val player: Playerc) {
        var bundle = Bundle()
        val data: DB.PlayerData? = findPlayerData(player.uuid())

        init {
            if (data != null) bundle = Bundle(data.languageTag)
        }

        fun chars(tile: Tile?) {
            if (player.unit() != Nulls.unit) {
                if (!Permission.check(player, "chars")) return
            }
            if (world != null) {
                var t = tile ?: world.tile(player.tileX(), player.tileY())
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
                letters.put("Z", intArrayOf(1, 0, 0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 1, 0, 1, 1, 1, 0, 0, 1, 1, 0, 0, 0, 1))
                letters.put("0", intArrayOf(1, 1, 1, 1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1))
                letters.put("1", intArrayOf(0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 1))
                letters.put("2", intArrayOf(1, 0, 1, 1, 1, 1, 0, 1, 0, 1, 1, 1, 1, 0, 1))
                letters.put("3", intArrayOf(1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1, 1))
                letters.put("4", intArrayOf(1, 1, 1, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1))
                letters.put("5", intArrayOf(1, 1, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 1, 1, 1))
                letters.put("6", intArrayOf(1, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 0, 1, 1, 1))
                letters.put("7", intArrayOf(1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 1, 1, 1))
                letters.put("8", intArrayOf(1, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1, 1))
                letters.put("9", intArrayOf(1, 1, 1, 0, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1, 1))
                letters.put("!", intArrayOf(1, 1, 1, 0, 1))
                letters.put("?", intArrayOf(0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 1, 1, 0, 0, 0))
                letters.put(" ", intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0))
                letters.put("#", intArrayOf(0, 1, 0, 1, 0, 1, 1, 1, 1, 1, 0, 1, 0, 1, 0, 1, 1, 1, 1, 1, 0, 1, 0, 1, 0))
                letters.put("%", intArrayOf(1, 1, 0, 0, 1, 1, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 1, 1, 0, 0, 1, 1))
                letters.put("^", intArrayOf(0, 1, 1, 0, 0, 1))
                letters.put("&", intArrayOf(0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1))
                letters.put("*", intArrayOf(0, 1, 0, 1, 0, 1, 0, 1, 0))
                letters.put("(", intArrayOf(0, 1, 1, 1, 0, 1, 0, 0, 0, 1))
                letters.put(")", intArrayOf(1, 0, 0, 0, 1, 0, 1, 1, 1, 0))
                letters.put(";", intArrayOf(1, 0, 1, 1))
                letters.put(":", intArrayOf(0, 1, 0, 1, 0))
                letters.put("'", intArrayOf(1, 1))
                letters.put("[", intArrayOf(1, 1, 1, 1, 1, 1, 0, 0, 0, 1))
                letters.put("]", intArrayOf(1, 0, 0, 0, 1, 1, 1, 1, 1, 1))
                letters.put("\"", intArrayOf(1, 1, 0, 0, 1, 1))

                val texts = arg[0].toCharArray()
                for (i in texts) {
                    val pos = Seq<IntArray>()
                    if (!letters.containsKey(i.uppercaseChar().toString())) continue
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
                        val tar = world.tile(t.x + pos[a][0], t.y + pos[a][1])
                        if (target[a] == 1) {
                            Call.setTile(tar, Blocks.scrapWall, Team.sharded, 0)
                        } else if (tar != null) {
                            Call.setTile(tar, tar.block(), Team.sharded, 0)
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
                    t = world.tile(t.x + left, t.y.toInt())
                }
            }
        }

        fun killall() {
            if (!Permission.check(player, "killall")) return
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
            if (arg.isNotEmpty() && !Strings.canParseInt(arg[0])) {
                try {
                    player.sendMessage(bundle["command.help.${arg[0]}"])
                } catch (e: MissingResourceException) {
                    player.sendMessage(bundle["command.help.not.exists"])
                }
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
            if (arg.isNotEmpty()) {
                if (!Permission.check(player, "info.other")) return
                val target = findPlayers(arg[0])
                if (target != null) {
                    val other = findPlayerData(target.uuid())
                    if (other != null) {
                        val texts = """
                        ${bundle["name"]}: ${other.name}
                        ${bundle["placecount"]}: ${other.placecount}
                        ${bundle["breakcount"]}: ${other.breakcount}
                        ${bundle["level"]}: ${other.level}
                        ${bundle["exp"]}: ${Exp[other]}
                        ${bundle["joindate"]}: ${Timestamp(other.joinDate).toLocalDateTime().format(DateTimeFormatter.ofPattern("yy-MM-dd HH:mm"))}
                        ${bundle["playtime"]}: ${String.format("%d:%02d:%02d:%02d", (other.playtime / 60 / 60 / 24) % 365, (other.playtime / 60 / 24) % 24, (other.playtime / 60) % 60, (other.playtime) % 60)}
                        ${bundle["attackclear"]}: ${other.attackclear}
                        ${bundle["pvpwincount"]}: ${other.pvpwincount}
                        ${bundle["pvplosecount"]}: ${other.pvplosecount}
                        """.trimIndent()
                        Call.infoMessage(player.con(), texts)
                    } else {
                        player.sendMessage(bundle["player.not.registered"])
                    }
                } else {
                    player.sendMessage(bundle["player.not.found"])
                }
            } else {
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
        }

        fun color() {
            if (!Permission.check(player, "color")) return
            if (data != null) data.colornick = !data.colornick
        }

        fun language() {
            if (!Permission.check(player, "language")) return
            if (arg.isEmpty()) {
                player.sendMessage("command.language.empty")
                return
            }
            if (data != null) {
                data.languageTag = arg[0]
                player.sendMessage(bundle["command.language.set", Locale(arg[0]).language])
                player.sendMessage(bundle["command.language.preview", Bundle(Locale(arg[0]).toLanguageTag())])
            }
        }

        fun login() {
            if (!Permission.check(player, "login")) return
            val result = database.search(arg[0], arg[1])
            if (result != null) {
                Trigger.loadPlayer(player, result)
            } else {
                player.sendMessage(bundle["account-not-match"])
            }
        }

        fun register() {
            if (!Permission.check(player, "register")) return
            if (Config.authType != Config.AuthType.None) {
                if (arg.size != 3) {
                    player.sendMessage(bundle["command.reg.usage"])
                } else if (arg[1] != arg[2]) {
                    player.sendMessage(bundle["command.reg.incorrect"])
                } else {
                    if (transaction { DB.Player.select { DB.Player.accountid.eq(arg[0]) }.firstOrNull() } == null) {
                        Trigger.createPlayer(player, arg[0], arg[1])
                        Log.info(Bundle()["log.data_created", player.name()])
                    } else {
                        player.sendMessage("command.reg.exists")
                    }
                }
            } else {
                player.sendMessage("[scarlet]This server doesn't use authentication.")
            }
        }

        fun report(){
            if (!Permission.check(player, "report")) return
            if (arg.isEmpty()) {
                player.sendMessage(bundle["command.report.arg.empty"])
            } else if (arg.size == 1) {
                player.sendMessage(bundle["command.report.no.reason"])
            } else if (arg.size > 2) {
                val target = findPlayers(arg[0])
                if (target != null) {
                    val reason = arg[2]
                    val infos = netServer.admins.findByIP(target.con().address)
                    // TODO 보고서 번역
                    val text = """
                        == ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}
                        Target player: ${target.name()}
                        Reporter: ${player.name()}
                        Reason: $reason
                        
                        == Target player information
                        Last name: ${infos.lastName}
                        Names: ${infos.names}
                        uuid: ${infos.id}
                        Last IP: ${infos.lastIP}
                        IP: ${infos.ips}
                    """.trimIndent()
                    Event.log(Event.LogType.Report, text, target.plainName())
                    Log.info(Bundle()["command.report.received", player.plainName(), target.plainName(), reason])
                    player.sendMessage(bundle["command.report.done", target.plainName()])
                } else {
                    player.sendMessage(bundle["player.not.found"])
                }
            }
        }

        fun maps() {
            if (!Permission.check(player, "maps")) return
            val list = maps.all()
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
            Call.sendMessage("[brown]== [sky]${player.name()}[white] - [tan]${arg[0]}")
        }

        fun motd() {
            if (!Permission.check(player, "motd")) return
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
            val message = StringBuilder()
            val page = if (arg.isNotEmpty() && arg[0].toIntOrNull() != null) arg[0].toInt() else 0

            val buffer = Mathf.ceil(Event.players.size.toFloat() / 6)
            val pages = if (buffer > 1.0) buffer - 1 else 0

            if (pages < page) {
                player.sendMessage(bundle["command.page.range", pages])
            } else {
                message.append("[green]==[white] ${bundle["command.page.players"]} [orange]$page[]/[orange]$pages\n")
                for (a in 6 * page until (6 * (page + 1)).coerceAtMost(Event.players.size)) {
                    message.append("[gray]${Event.players.get(a).keys().first()} [white]${Event.players.get(a).values().first()}\n")
                }
                player.sendMessage(message.toString().dropLast(1))
            }
        }

        fun spawn() {
            if (!Permission.check(player, "spawn")) return
            val type = arg[0]
            val name = arg[1]
            val parameter = if (arg.size == 3) arg[2].toIntOrNull() else 1

            // todo 유닛이 8마리까지 밖에 스폰이 안됨
            when {
                type.equals("unit", true) -> {
                    val unit = content.units().find { unitType: UnitType -> unitType.name == name }
                    if (unit != null) {
                        if (parameter != null) {
                            if (name != "block" && name != "turret-unit-build-tower") {
                                for (a in 1..parameter) {
                                    unit.spawn(player.team(), player.x, player.y)
                                }
                            } else {
                                player.sendMessage(bundle["command.spawn.block"])
                            }
                        } else {
                            player.sendMessage(bundle["command.spawn.number"])
                        }
                    } else {
                        val names = StringBuilder()
                        content.units().each {
                            names.append("${it.name}, ")
                        }
                        player.sendMessage("${bundle["command.spawn.units"]}: ${names.dropLast(2)}")
                    }
                }

                type.equals("block", true) -> {
                    if (content.blocks().find { a -> a.name == name } != null) {
                        Call.constructFinish(player.tileOn(), content.blocks().find { a -> a.name.equals(name, true) }, player.unit(), parameter?.toByte() ?: 0, player.team(), null)
                    } else {
                        val names = StringBuilder()
                        content.blocks().each {
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

        fun setperm() {
            if (!Permission.check(player, "setperm")) return
            val target = findPlayers(arg[0])
            if (target != null) {
                val data = findPlayerData(target.uuid())
                if (data != null) {
                    data.permission = arg[1]
                } else {
                    player.sendMessage(bundle["player.not.registered"])
                }
            } else {
                player.sendMessage(bundle["player.not.found"])
            }
        }

        fun status() {
            fun longToTime(seconds: Long): String {
                val min = seconds / 60
                val hour = min / 60
                val days = hour / 24
                return String.format("%d:%02d:%02d:%02d", days % 365, hour % 24, min % 60, seconds % 60)
            }

            if (!Permission.check(player, "status")) return
            val bans = netServer.admins.banned.size

            player.sendMessage(
                """
                [#DEA82A]${bundle["command.status.info"]}[]
                [#2B60DE]========================================[]
                TPS: ${Core.graphics.framesPerSecond}/60
                ${bundle["command.status.banned", bans]}
                ${bundle["command.status.playtime"]}: ${longToTime(PluginData.playtime)}
                ${bundle["command.status.uptime"]}: ${longToTime(PluginData.uptime)}
            """.trimIndent()
            )
        }

        fun team() {
            if (!Permission.check(player, "team")) return
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
                } else {
                    player.sendMessage(bundle["player.not.found"])
                }
            }
        }

        fun time() {
            if (!Permission.check(player, "time")) return
            val now = LocalDateTime.now()
            val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            player.sendMessage("${bundle["command.time"]}: ${now.format(dateTimeFormatter)}")
        }

        fun weather() {
            if (!Permission.check(player, "weather")) return
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
                Call.createWeather(weather, (Math.random() * 100).toFloat(), (duration * 8).toFloat(), 10f, 10f)
            } catch (e: NumberFormatException) {
                player.sendMessage(bundle["command.weather.not.number"])
            }
        }

        fun mute() {
            if (!Permission.check(player, "mute")) return
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
            val other = findPlayers(arg[0])
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
            if (arg.isEmpty()) {
                player.sendMessage(bundle["command.effect.arg.empty"])
                return
            } else if (arg.size != 5) {
                player.sendMessage(bundle["command.effect.arg.need"])
                return
            }
            val effect = when (arg[0]) {
                "blockCrash" -> Fx.blockCrash
                "trailFade" -> Fx.trailFade
                "unitSpawn" -> Fx.unitSpawn
                "unitCapKill" -> Fx.unitCapKill
                "unitEnvKill" -> Fx.unitEnvKill
                "unitControl" -> Fx.unitControl
                "unitDespawn" -> Fx.unitDespawn
                "unitSpirit" -> Fx.unitSpirit
                "itemTransfer" -> Fx.itemTransfer
                "pointBeam" -> Fx.pointBeam
                "pointHit" -> Fx.pointHit
                "lightning" -> Fx.lightning
                "coreBuildShockwave" -> Fx.coreBuildShockwave
                "coreBuildBlock" -> Fx.coreBuildBlock
                "pointShockwave" -> Fx.pointShockwave
                "moveCommand" -> Fx.moveCommand
                "attackCommand" -> Fx.attackCommand
                "commandSend" -> Fx.commandSend
                "upgradeCore" -> Fx.upgradeCore
                "upgradeCoreBloom" -> Fx.upgradeCoreBloom
                "placeBlock" -> Fx.placeBlock
                "coreLaunchConstruct" -> Fx.coreLaunchConstruct
                "tapBlock" -> Fx.tapBlock
                "breakBlock" -> Fx.breakBlock
                "payloadDeposit" -> Fx.payloadDeposit
                "select" -> Fx.select
                "smoke" -> Fx.smoke
                "fallSmoke" -> Fx.fallSmoke
                "unitWreck" -> Fx.unitWreck
                "rocketSmoke" -> Fx.rocketSmoke
                "rocketSmokeLarge" -> Fx.rocketSmokeLarge
                "magmasmoke" -> Fx.magmasmoke
                "spawn" -> Fx.spawn
                "unitAssemble" -> Fx.unitAssemble
                "padlaunch" -> Fx.padlaunch
                "breakProp" -> Fx.breakProp
                "unitDrop" -> Fx.unitDrop
                "unitLand" -> Fx.unitLand
                "unitDust" -> Fx.unitDust
                "unitLandSmall" -> Fx.unitLandSmall
                "unitPickup" -> Fx.unitPickup
                "crawlDust" -> Fx.crawlDust
                "landShock" -> Fx.landShock
                "pickup" -> Fx.pickup
                "titanExplosion" -> Fx.titanExplosion
                "titanSmoke" -> Fx.titanSmoke
                "missileTrailSmoke" -> Fx.missileTrailSmoke
                "neoplasmSplat" -> Fx.neoplasmSplat
                "scatheExplosion" -> Fx.scatheExplosion
                "scatheLight" -> Fx.scatheLight
                "dynamicSpikes" -> Fx.dynamicSpikes
                "greenBomb" -> Fx.greenBomb
                "greenLaserCharge" -> Fx.greenLaserCharge
                "greenLaserChargeSmall" -> Fx.greenLaserChargeSmall
                "greenCloud" -> Fx.greenCloud
                "healWaveDynamic" -> Fx.healWaveDynamic
                "healWave" -> Fx.healWave
                "heal" -> Fx.heal
                "shieldWave" -> Fx.shieldWave
                "shieldApply" -> Fx.shieldApply
                "disperseTrail" -> Fx.disperseTrail
                "hitBulletSmall" -> Fx.hitBulletSmall
                "hitBulletColor" -> Fx.hitBulletColor
                "hitSquaresColor" -> Fx.hitSquaresColor
                "hitFuse" -> Fx.hitFuse
                "hitBulletBig" -> Fx.hitBulletBig
                "hitFlameSmall" -> Fx.hitFlameSmall
                "hitFlamePlasma" -> Fx.hitFlamePlasma
                "hitLiquid" -> Fx.hitLiquid
                "hitLaserBlast" -> Fx.hitLaserBlast
                "hitEmpSpark" -> Fx.hitEmpSpark
                "hitLancer" -> Fx.hitLancer
                "hitBeam" -> Fx.hitBeam
                "hitFlameBeam" -> Fx.hitFlameBeam
                "hitMeltdown" -> Fx.hitMeltdown
                "hitMeltHeal" -> Fx.hitMeltHeal
                "instBomb" -> Fx.instBomb
                "instTrail" -> Fx.instTrail
                "instShoot" -> Fx.instShoot
                "instHit" -> Fx.instHit
                "hitLaser" -> Fx.hitLaser
                "hitLaserColor" -> Fx.hitLaserColor
                "despawn" -> Fx.despawn
                "airBubble" -> Fx.airBubble
                "plasticExplosion" -> Fx.plasticExplosion
                "plasticExplosionFlak" -> Fx.plasticExplosionFlak
                "blastExplosion" -> Fx.blastExplosion
                "sapExplosion" -> Fx.sapExplosion
                "massiveExplosion" -> Fx.massiveExplosion
                "artilleryTrail" -> Fx.artilleryTrail
                "incendTrail" -> Fx.incendTrail
                "missileTrail" -> Fx.missileTrail
                "absorb" -> Fx.absorb
                "forceShrink" -> Fx.forceShrink
                "burning" -> Fx.burning
                "fireRemove" -> Fx.fireRemove
                "fire" -> Fx.fire
                "fireHit" -> Fx.fireHit
                "fireSmoke" -> Fx.fireSmoke
                "neoplasmHeal" -> Fx.neoplasmHeal
                "ventSteam" -> Fx.ventSteam
                "vaporSmall" -> Fx.vaporSmall
                "fireballsmoke" -> Fx.fireballsmoke
                "ballfire" -> Fx.ballfire
                "freezing" -> Fx.freezing
                "melting" -> Fx.melting
                "wet" -> Fx.wet
                "muddy" -> Fx.muddy
                "sapped" -> Fx.sapped
                "electrified" -> Fx.electrified
                "sporeSlowed" -> Fx.sporeSlowed
                "oily" -> Fx.oily
                "overdriven" -> Fx.overdriven
                "overclocked" -> Fx.overclocked
                "dropItem" -> Fx.dropItem
                "shockwave" -> Fx.shockwave
                "bigShockwave" -> Fx.bigShockwave
                "spawnShockwave" -> Fx.spawnShockwave
                "explosion" -> Fx.explosion
                "dynamicExplosion" -> Fx.dynamicExplosion
                "reactorExplosion" -> Fx.reactorExplosion
                "impactReactorExplosion" -> Fx.impactReactorExplosion
                "blockExplosionSmoke" -> Fx.blockExplosionSmoke
                "shootSmall" -> Fx.shootSmall
                "shootSmallColor" -> Fx.shootSmallColor
                "shootHeal" -> Fx.shootHeal
                "shootHealYellow" -> Fx.shootHealYellow
                "shootSmallSmoke" -> Fx.shootSmallSmoke
                "shootBig" -> Fx.shootBig
                "shootBig2" -> Fx.shootBig2
                "shootBigColor" -> Fx.shootBigColor
                "shootTitan" -> Fx.shootTitan
                "shootBigSmoke" -> Fx.shootBigSmoke
                "shootBigSmoke2" -> Fx.shootBigSmoke2
                "shootSmokeDisperse" -> Fx.shootSmokeDisperse
                "shootSmokeSquare" -> Fx.shootSmokeSquare
                "shootSmokeSquareSparse" -> Fx.shootSmokeSquareSparse
                "shootSmokeSquareBig" -> Fx.shootSmokeSquareBig
                "shootSmokeTitan" -> Fx.shootSmokeTitan
                "shootSmokeSmite" -> Fx.shootSmokeSmite
                "shootSmokeMissile" -> Fx.shootSmokeMissile
                "regenParticle" -> Fx.regenParticle
                "regenSuppressParticle" -> Fx.regenSuppressParticle
                "regenSuppressSeek" -> Fx.regenSuppressSeek
                "neoplasiaSmoke" -> Fx.neoplasiaSmoke
                "heatReactorSmoke" -> Fx.heatReactorSmoke
                "circleColorSpark" -> Fx.circleColorSpark
                "colorSpark" -> Fx.colorSpark
                "colorSparkBig" -> Fx.colorSparkBig
                "randLifeSpark" -> Fx.randLifeSpark
                "shootPayloadDriver" -> Fx.shootPayloadDriver
                "shootSmallFlame" -> Fx.shootSmallFlame
                "shootPyraFlame" -> Fx.shootPyraFlame
                "shootLiquid" -> Fx.shootLiquid
                "casing1" -> Fx.casing1
                "railTrail" -> Fx.railTrail
                "railHit" -> Fx.railHit
                "lancerLaserShoot" -> Fx.lancerLaserShoot
                "lancerLaserShootSmoke" -> Fx.lancerLaserShootSmoke
                "lancerLaserCharge" -> Fx.lancerLaserCharge
                "lancerLaserChargeBegin" -> Fx.lancerLaserChargeBegin
                "lightningCharge" -> Fx.lightningCharge
                "sparkShoot" -> Fx.sparkShoot
                "lightningShoot" -> Fx.lightningShoot
                "thoriumShoot" -> Fx.thoriumShoot
                "reactorsmoke" -> Fx.reactorsmoke
                "redgeneratespark" -> Fx.redgeneratespark
                "fuelburn" -> Fx.fuelburn
                "incinerateSlag" -> Fx.incinerateSlag
                "coreBurn" -> Fx.coreBurn
                "plasticburn" -> Fx.plasticburn
                "conveyorPoof" -> Fx.conveyorPoof
                "pulverize" -> Fx.pulverize
                "pulverizeRed" -> Fx.pulverizeRed
                "pulverizeSmall" -> Fx.pulverizeSmall
                "pulverizeMedium" -> Fx.pulverizeMedium
                "producesmoke" -> Fx.producesmoke
                "artilleryTrailSmoke" -> Fx.artilleryTrailSmoke
                "smokeCloud" -> Fx.smokeCloud
                "smeltsmoke" -> Fx.smeltsmoke
                "coalSmeltsmoke" -> Fx.coalSmeltsmoke
                "formsmoke" -> Fx.formsmoke
                "blastsmoke" -> Fx.blastsmoke
                "lava" -> Fx.lava
                "dooropen" -> Fx.dooropen
                "doorclose" -> Fx.doorclose
                "dooropenlarge" -> Fx.dooropenlarge
                "doorcloselarge" -> Fx.doorcloselarge
                "generate" -> Fx.generate
                "mineWallSmall" -> Fx.mineWallSmall
                "mineSmall" -> Fx.mineSmall
                "mine" -> Fx.mine
                "mineBig" -> Fx.mineBig
                "mineHuge" -> Fx.mineHuge
                "mineImpact" -> Fx.mineImpact
                "mineImpactWave" -> Fx.mineImpactWave
                "payloadReceive" -> Fx.payloadReceive
                "teleportActivate" -> Fx.teleportActivate
                "teleport" -> Fx.teleport
                "teleportOut" -> Fx.teleportOut
                "ripple" -> Fx.ripple
                "launch" -> Fx.launch
                "launchPod" -> Fx.launchPod
                "healWaveMend" -> Fx.healWaveMend
                "overdriveWave" -> Fx.overdriveWave
                "healBlock" -> Fx.healBlock
                "healBlockFull" -> Fx.healBlockFull
                "rotateBlock" -> Fx.rotateBlock
                "lightBlock" -> Fx.lightBlock
                "overdriveBlockFull" -> Fx.overdriveBlockFull
                "shieldBreak" -> Fx.shieldBreak
                "chainLightning" -> Fx.chainLightning
                "chainEmp" -> Fx.chainEmp
                "legDestroy" -> Fx.legDestroy
                else -> Fx.none
            }

            try {
                val x = arg[1].toIntOrNull()
                val y = arg[2].toIntOrNull()

                if (x == null || y == null) {
                    player.sendMessage(bundle["command.effect.int.invalid"])
                } else {
                    val rot = arg[3].toFloatOrNull()
                    if (rot != null && rot > 360) {
                        val color = Color.valueOf(arg[4])
                        val tile = world.tile(x, y)
                        Call.effect(effect, tile.getX(), tile.getY(), rot, color)
                    } else {
                        player.sendMessage(bundle["command.effect.rotate.invalid"])
                    }
                }
            } catch (e: IllegalArgumentException) {
                player.sendMessage(bundle["command.effect.color.invalid"])
            }
        }

        fun fillitems() {
            if (!Permission.check(player, "fillitems")) return
            val team = when (arg[0].lowercase()) {
                "derelict" -> Team.derelict
                "sharded" -> Team.sharded
                "crux" -> Team.crux
                "green" -> Team.green
                "malis" -> Team.malis
                "blue" -> Team.blue
                else -> {
                    null
                }
            }

            if (team == null) {
                player.sendMessage(bundle["command.fillitems.team"])
                return
            }

            if (state.teams.cores(team).isEmpty) {
                player.sendMessage(bundle["command.fillitems.core.empty"])
            }

            for (item in content.items()) {
                state.teams.cores(team).first().items[item] = state.teams.cores(team).first().storageCapacity
            }

            player.sendMessage(bundle["command.fillitems.core.filled"])
        }

        fun freeze(){
            if (!Permission.check(player, "freeze")) return
            if (arg.isEmpty()){
                player.sendMessage(bundle["player.not.found"])
            } else {
                val target = findPlayers(arg[0])
                if (target != null) {
                    val data = findPlayerData(target.uuid())
                    if (data != null) {
                        if (data.status.containsKey("freeze")) {
                            data.status.remove("freeze")
                            player.sendMessage(bundle["command.freeze.undo", target.plainName()])
                        } else {
                            data.status.put("freeze", "${target.x}/${target.y}")
                            player.sendMessage(bundle["command.freeze.done", target.plainName()])
                        }
                    } else {
                        player.sendMessage(bundle["player.not.registered"])
                    }
                } else {
                    player.sendMessage(bundle["player.not.found"])
                }
            }
        }

        fun god() {
            if (!Permission.check(player, "god")) return

            player.unit().health(1.0E8f)
            player.sendMessage(bundle["command.god"])
        }

        fun pause() {
            if (!Permission.check(player, "pause")) return
            if (state.isPaused) {
                state.set(GameState.State.playing)
                player.sendMessage(bundle["command.pause.unpaused"])
            } else {
                state.set(GameState.State.paused)
                player.sendMessage(bundle["command.pause.paused"])
            }
        }

        fun js() {
            if (!Permission.check(player, "js")) {
                Call.kick(player.con(), bundle["command.js.no.permission"])
                return
            }
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

        fun kickall() {
            if (!Permission.check(player, "kickall")) return
            for (a in Groups.player) {
                if (!a.admin) Call.kick(a.con, Packets.KickReason.kick)
            }
        }

        fun hub() {
            if (!Permission.check(player, "hub")) return
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
                "zone" -> if (parameters.size != 2) {
                    player.sendMessage(bundle["command.hub.zone.help"])
                } else {
                    try {
                        size = parameters[0].toInt()
                        clickable = java.lang.Boolean.parseBoolean(parameters[1])
                    } catch (ignored: NumberFormatException) {
                        player.sendMessage(bundle["command.hub.size.invalid"])
                        return
                    }
                    PluginData.warpZones.add(PluginData.WarpZone(name, world.tile(x, y).pos(), world.tile(x + size, y + size).pos(), clickable, ip, port))

                    player.sendMessage(bundle["command.hub.zone.added", "$x:$y", ip, if (clickable) bundle["command.hub.zone.clickable"] else bundle["command.hub.zone.enter"]])
                }

                "block" -> if (parameters.isEmpty()) {
                    player.sendMessage(bundle["command.hub.block.parameter"])
                } else {
                    val t: Tile = player.tileOn()
                    PluginData.warpBlocks.add(PluginData.WarpBlock(name, t.pos(), t.block().name, t.block().size, ip, port, arg[2]))
                    player.sendMessage(bundle["command.hub.block.added", "$x:$y", ip])
                }

                "count" -> {
                    if (parameters.isNotEmpty()) {
                        player.sendMessage(bundle["command.hub.count.parameter"])
                    } else {
                        PluginData.warpCounts.add(PluginData.WarpCount(name, world.tile(x, y).pos(), ip, port, 0, 1))
                        player.sendMessage(bundle["command.hub.count", "$x:$y", ip])
                    }
                }

                "total" -> {
                    PluginData.warpTotals.add(PluginData.WarpTotal(name, world.tile(x, y).pos(), 0, 1))
                    player.sendMessage(bundle["command.hub.total", "$x:$y"])
                }

                else -> player.sendMessage(bundle["command.hub.help"])
            }
        }

        fun gg() {
            if (!Permission.check(player, "gg")) return
            for (a in 0..world.tiles.height) {
                for (b in 0..world.tiles.width) {
                    Call.effect(Fx.pointHit, (a * 8).toFloat(), (b * 8).toFloat(), 0f, Color.red)
                    if (world.tile(a, b) != null) {
                        try {
                            Call.setFloor(world.tile(a, b), Blocks.space, Blocks.space)
                        } catch (e: Exception) {
                            Call.setFloor(world.tile(a, b), Blocks.space, Blocks.space)
                        }
                        try {
                            Call.removeTile(world.tile(a, b))
                        } catch (e: Exception) {
                            Call.removeTile(world.tile(a, b))
                        }
                    }
                }
            }
        }

        fun kill() {
            if (!Permission.check(player, "kill")) return
            if (arg.isEmpty()) {
                player.unit().kill()
            } else {
                val other = findPlayers(arg[0])
                if (other == null) player.sendMessage(bundle["player.not.found"]) else other.unit().kill()
            }
        }

        fun meme() {
            if (!Permission.check(player, "meme")) return
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
                            data.status.put("router", "true")
                            while (!player.isNull) {
                                for (d in loop) {
                                    player.name(d)
                                    sleep(500)
                                }
                                if (!data.status.containsKey("router")) break
                                sleep(5000)
                                for (i in loop.indices.reversed()) {
                                    player.name(loop[i])
                                    sleep(500)
                                }
                                for (d in zero) {
                                    player.name(d)
                                    sleep(500)
                                }
                            }
                        }.start()
                    }
                }
            }
        }

        fun tp() {
            if (!Permission.check(player, "tp")) return
            val other = findPlayers(arg[0])

            if (other == null) {
                player.sendMessage(bundle["player.not.found"])
            } else {
                Call.setPosition(player.con(), other.x, other.y)
            }
        }

        fun tempban() {
            if (!Permission.check(player, "tempban")) return
            val other = findPlayers(arg[0])

            if (other == null) {
                player.sendMessage(bundle["player.not.found"])
            } else {
                val d = findPlayerData(other.uuid())
                if (d == null) {
                    player.sendMessage(bundle["command.tempban.not.registered"])
                    netServer.admins.banPlayer(other.uuid())
                    Call.kick(other.con(), Packets.KickReason.banned)
                } else {
                    val time = LocalDateTime.now()
                    val minute = arg[1].toLongOrNull()
                    val reason = arg[2]

                    if (minute != null) {
                        d.status.put("ban", time.plusMinutes(minute.toLong()).toString())
                        netServer.admins.banPlayer(other.uuid())
                        Call.kick(other.con(), reason)
                    } else {
                        player.sendMessage(bundle["command.tempban.not.number"])
                    }
                }
            }
        }

        fun search() {
            if (!Permission.check(player, "search")) return
            if (arg[0].isEmpty()) {
                player.sendMessage("player.not.found")
                return
            }

            val result = ArrayList<DB.PlayerData?>()
            val data = findPlayers(arg[0])

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
                        player.sendMessage(texts)
                    }
                }
                player.sendMessage(bundle["command.search.total", result.size])
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

        fun url() {
            if (!Permission.check(player, "url")) return
            when (arg[0]) {
                "effect" -> {
                    try {
                        Call.openURI(player.con(), "https://github.com/Anuken/Mindustry/blob/master/core/src/mindustry/content/Fx.java")
                    } catch (e: NoSuchMethodError) {
                        player.sendMessage(bundle["command.not.support"])
                    }
                }

                else -> {}
            }
        }

        fun vote() {
            fun sendStart(message: String, vararg parameter: Any) {
                Groups.player.forEach {
                    val data = findPlayerData(it.uuid())
                    if (data != null) {
                        val bundle = Bundle(data.languageTag)
                        it.sendMessage(bundle["command.vote.starter", player.name()])
                        it.sendMessage(bundle.get(message, *parameter))
                    }
                }
            }
            if (!Permission.check(player, "vote")) return
            if (arg.isEmpty()) {
                player.sendMessage("command.vote.arg.empty")
                return
            }
            if (!Trigger.voting) {
                when (arg[0]) {
                    "kick" -> {
                        if (arg.size == 2) {
                            player.sendMessage(bundle["command.vote.no.reason"])
                            return
                        }
                        val target = findPlayers(arg[1])
                        if (target != null) {
                            Trigger.voteTarget = target
                            Trigger.voteTargetUUID = target.uuid()
                            Trigger.voteReason = arg[2]
                            Trigger.voteType = "kick"
                            Trigger.voteStarter = player
                            Trigger.voting = true
                            sendStart("command.vote.kick.start", target.name(), arg[2])
                        } else {
                            player.sendMessage(bundle["player.not.found"])
                        }
                    }

                    "map" -> {
                        if (arg.size == 2) {
                            player.sendMessage(bundle["command.vote.no.reason"])
                            return
                        }
                        if (arg[1].toIntOrNull() != null) {
                            try {
                                var target = maps.all().find { e -> e.name().contains(arg[1]) }
                                if (target == null) {
                                    target = maps.all().get(arg[1].toInt())
                                }
                                Trigger.voteType = "map"
                                Trigger.voteMap = target
                                Trigger.voteReason = arg[2]
                                Trigger.voteStarter = player
                                Trigger.voting = true
                                sendStart("command.vote.map.start", target.name(), arg[2])
                            } catch (e: IndexOutOfBoundsException) {
                                player.sendMessage(bundle["command.vote.map.not.exists"])
                            }
                        } else {
                            player.sendMessage(bundle["command.vote.map.not.exists"])
                        }
                    }

                    "gg" -> {
                        Trigger.voteType = "gg"
                        Trigger.voteStarter = player
                        Trigger.voting = true
                        sendStart("command.vote.gg.start")
                    }

                    "skip" -> {
                        if (arg[1].toIntOrNull() != null) {
                            Trigger.voteType = "skip"
                            Trigger.voteWave = arg[1].toInt()
                            Trigger.voteStarter = player
                            Trigger.voting = true
                            sendStart("command.vote.skip.start", arg[1])
                        } else {
                            player.sendMessage(bundle["command.vote.skip.wrong"])
                        }
                    }

                    "back" -> {
                        if (!saveDirectory.child("rollback.msav").exists()) {
                            player.sendMessage("command.vote.back.no.file")
                            return
                        }
                        if (arg.size == 1) {
                            player.sendMessage(bundle["command.vote.no.reason"])
                            return
                        }
                        Trigger.voteType = "back"
                        Trigger.voteReason = arg[1]
                        Trigger.voteStarter = player
                        Trigger.voting = true
                        sendStart("command.vote.back.start", arg[1])
                    }

                    "random" -> {
                        Trigger.voteType = "random"
                        Trigger.voteStarter = player
                        Trigger.voting = true
                        sendStart("command.vote.random.start")
                    }

                    else -> {
                        player.sendMessage(bundle["command.vote.wrong"])
                    }
                }
            }
        }
    }

    class Server(val arg: Array<String>) {
        fun genDocs() {
            if (System.getenv("DEBUG_KEY") != null) {
                val server = "## Server commands\n| Command | Parameter | Description |\n|:---|:---|:--- |\n"
                val client = "## Client commands\n| Command | Parameter | Description |\n|:---|:---|:--- |\n"
                val time = "README.md Generated time: ${DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now())}"

                val result = StringBuilder()

                for (b in clientCommands.commandList) {
                    val temp = "| ${b.text} | ${StringUtils.encodeHtml(b.paramText)} | ${b.description} |\n"
                    result.append(temp)
                }

                val tmp = "$client$result\n\n"

                result.clear()
                for (c in serverCommands.commandList) {
                    val temp = "| ${c.text} | ${StringUtils.encodeHtml(c.paramText)} | ${c.description} |\n"
                    result.append(temp)
                }

                println("$tmp$server$result\n\n\n$time")
            }
        }

        fun debug() {
            if (System.getenv("DEBUG_KEY") != null) {
                if (arg.isNotEmpty()) {
                    if (arg[0].toBoolean()) {
                        Core.settings.put("debugMode", true)
                    } else {
                        Core.settings.put("debugMode", false)
                    }
                }
                println(
                    """
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
                """.trimIndent()
                )
                database.players.forEach { println(it.toString()) }
            }
        }

        fun setperm() {
            val target = Groups.player.find { a -> a.name == arg[0] }
            if (target != null) {
                val data = findPlayerData(target.uuid())
                if (data != null) {
                    data.permission = arg[2]
                } else {
                    Log.info(bundle["player.not.registered"])
                }
            } else {
                Log.info(bundle["player.not.found"])
            }
        }

        fun tempban() {
            val other = if (arg[0].toIntOrNull() != null) {
                Groups.player.find { e -> e.id == arg[0].toInt() }
            } else {
                Groups.player.find { e -> e.name().contains(arg[0]) }
            }

            if (other == null) {
                Log.info(bundle["player.not.found"])
            } else {
                val d = findPlayerData(other.uuid())
                if (d == null) {
                    Log.info(bundle["command.tempban.not.registered"])
                    netServer.admins.banPlayer(other.uuid())
                    Call.kick(other.con, Packets.KickReason.banned)
                } else {
                    val time = LocalDateTime.now()
                    val minute = arg[1].toLongOrNull()
                    val reason = arg[2]

                    if (minute != null) {
                        d.status.put("ban", time.plusMinutes(minute.toLong()).toString())
                        Call.kick(other.con, reason)
                    } else {
                        Log.info(bundle["command.tempban.not.number"])
                    }
                }
            }
        }

        fun longToDateTime(mils: Long): LocalDateTime {
            return Timestamp(mils).toLocalDateTime()
        }
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
            if (null == source) return null
            var encode: StringBuffer? = null
            val encodeArray = source.toCharArray()
            var match = -1
            var difference: Int
            for (i in encodeArray.indices) {
                val charEncode = encodeArray[i]
                if (htmlEncodeChars.containsKey(charEncode)) {
                    if (null == encode) encode = StringBuffer(source.length)
                    difference = i - (match + 1)
                    if (difference > 0) encode.append(encodeArray, match + 1, difference)
                    encode.append(htmlEncodeChars[charEncode])
                    match = i
                }
            }
            return if (null == encode) {
                source
            } else {
                difference = encodeArray.size - (match + 1)
                if (difference > 0) encode.append(encodeArray, match + 1, difference)
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