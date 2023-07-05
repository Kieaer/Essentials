package essentials

import arc.Core
import arc.Events
import arc.files.Fi
import arc.graphics.Color
import arc.graphics.Colors
import arc.math.Mathf
import arc.struct.ArrayMap
import arc.struct.ObjectMap
import arc.struct.Seq
import arc.util.CommandHandler
import arc.util.Log
import arc.util.Strings
import arc.util.Threads.sleep
import arc.util.Tmp
import essentials.Event.findPlayerData
import essentials.Event.findPlayers
import essentials.Event.findPlayersByName
import essentials.Event.worldHistory
import essentials.Main.Companion.database
import essentials.Main.Companion.root
import essentials.Permission.bundle
import mindustry.Vars.*
import mindustry.content.Blocks
import mindustry.content.Weathers
import mindustry.core.GameState
import mindustry.game.EventType
import mindustry.game.Gamemode
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Playerc
import mindustry.gen.Unit
import mindustry.maps.Map
import mindustry.net.Packets
import mindustry.net.WorldReloader
import mindustry.type.Item
import mindustry.type.UnitType
import mindustry.ui.Menus
import mindustry.world.Tile
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import org.hjson.JsonArray
import org.hjson.JsonObject
import org.hjson.Stringify
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.sql.Timestamp
import java.text.MessageFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.round

class Commands(handler : CommandHandler, isClient : Boolean) {
    companion object {
        var clientCommands = CommandHandler("/")
        var serverCommands = CommandHandler("")
    }

    init {
        if (isClient) {
            handler.removeCommand("help")
            if (Config.vote) {
                handler.removeCommand("vote")
                handler.register("vote", "<kick/map/gg/skip/back/random> [player/amount/world_name] [reason]", "Start voting") { a, p : Playerc -> Client(a, p).vote(p, a) }

                handler.removeCommand("votekick")
                if (Config.votekick) {
                    handler.register("votekick", "<name...>", "Start kick voting") { a, p : Playerc -> Client(a, p).votekick() }
                }
            }

            handler.register("broadcast", "<text...>", "Broadcast message to all servers") { a, p : Playerc -> Client(a, p).broadcast() }
            handler.register("changemap", "<name> [gamemode]", "Change the world or gamemode immediately.") { a, p : Playerc -> Client(a, p).changemap() }
            handler.register("changename", "<new_name> [player]", "Change player name.") { a, p : Playerc -> Client(a, p).changename() }
            handler.register("changepw", "<new_password> <password_repeat>", "Change account password.") { a, p : Playerc -> Client(a, p).changepw() }
            handler.register("chat", "<on/off>", "Mute all players without admins.") { a, p : Playerc -> Client(a, p).chat() }
            handler.register("chars", "<text...>", "Make pixel texts") { a, p : Playerc -> Client(a, p).chars(null) }
            handler.register("color", "Enable color nickname") { a, p : Playerc -> Client(a, p).color() }
            handler.register("discord", "Authenticate your Discord account to the server.") { a, p : Playerc -> Client(a, p).discord() }
            handler.register("dps", "Create damage per seconds meter block") { a, p : Playerc -> Client(a, p).dps() }
            handler.register("effect", "<on/off/level> [color]", "Turn other players' effects on or off, or set effects and colors for each level.") { a, p : Playerc -> Client(a, p).effect() }
            handler.register("exp", "<set/hide/add/remove> [values/player] [player]", "Edit account EXP values") { a, p : Playerc -> Client(a, p).exp() }
            handler.register("fillitems", "<team>", "Fill the core with items.") { a, p : Playerc -> Client(a, p).fillitems() }
            handler.register("freeze", "<player>", "Stop player unit movement") { a, p : Playerc -> Client(a, p).freeze() }
            handler.register("gg", "[team]", "Force gameover") { a, p : Playerc -> Client(a, p).gg() }
            handler.register("god", "[name]", "Set max player health") { a, p : Playerc -> Client(a, p).god() }
            handler.register("help", "[page]", "Show command lists") { a, p : Playerc -> Client(a, p).help() }
            handler.register("hub", "<set/zone/block/count/total/remove/reset> [ip] [parameters...]", "Create a server to server point.") { a, p : Playerc -> Client(a, p).hub() }
            handler.register("hud", "<health>", "Enable unit information.") { a, p : Playerc -> Client(a, p).hud() }
            handler.register("info", "[player]", "Show your information") { a, p : Playerc -> Client(a, p).info() }
            handler.register("js", "[code...]", "Execute JavaScript codes") { a, p : Playerc -> Client(a, p).js() }
            handler.register("kickall", "All users except yourself and the administrator will be kicked") { a, p : Playerc -> Client(a, p).kickall() }
            handler.register("kill", "[player]", "Kill player.") { a, p : Playerc -> Client(a, p).kill() }
            handler.register("killall", "[team]", "Kill all enemy units") { a, p : Playerc -> Client(a, p).killall() }
            handler.register("killunit", "<name> [amount] [team]", "Destroys specific units only.") { a, p : Playerc -> Client(a, p).killunit() }
            handler.register("lang", "<language_tag>", "Set the language for your account.") { a, p : Playerc -> Client(a, p).lang() }
            handler.register("log", "Enable block log") { a, p : Playerc -> Client(a, p).log() }
            handler.register("login", "<id> <password>", "Access your account") { a, p : Playerc -> Client(a, p).login() }
            handler.register("maps", "[page]", "Show server maps") { a, p : Playerc -> Client(a, p).maps() }
            handler.register("me", "<text...>", "broadcast * message") { a, p : Playerc -> Client(a, p).me() }
            handler.register("meme", "<type>", "Enjoy meme features!") { a, p : Playerc -> Client(a, p).meme() }
            handler.register("motd", "Show server motd.") { a, p : Playerc -> Client(a, p).motd() }
            handler.register("mute", "<player>", "Mute player") { a, p : Playerc -> Client(a, p).mute() }
            handler.register("pause", "Pause server") { a, p : Playerc -> Client(a, p).pause() }
            handler.register("players", "[page]", "Show players list") { a, p : Playerc -> Client(a, p).players() }
            handler.register("pm", "<player> [message...]", "Send private messgae") { a, p : Playerc -> Client(a, p).pm() }
            handler.register("ranking", "<time/exp/attack/place/break/pvp> [page]", "Show players ranking") { a, p : Playerc -> Client(a, p).ranking() }
            handler.register("reg", "<id> <password> <password_repeat>", "Register account") { a, p : Playerc -> Client(a, p).register() }
            handler.register("report", "<player> <reason...>", "Report player") { a, p : Playerc -> Client(a, p).report() }
            handler.register("rollback", "<player>", "Undo all actions taken by the player.") { a, p : Playerc -> Client(a, p).rollback() }
            handler.register("search", "[value]", "Search player data") { a, p : Playerc -> Client(a, p).search() }
            handler.register("setitem", "<item> <amount> [team]", "Set team core item amount") { a, p : Playerc -> Client(a, p).setitem() }
            handler.register("setperm", "<player> <group>", "Set the player's permission group.") { a, p : Playerc -> Client(a, p).setperm() }
            handler.register("skip", "Start n wave immediately.") { a, p : Playerc -> Client(a, p).skip() }
            handler.register("spawn", "<unit/block> <name> [amount/rotate]", "Spawns units at the player's location.") { a, p : Playerc -> Client(a, p).spawn() }
            handler.register("status", "Show server status") { a, p : Playerc -> Client(a, p).status() }
            handler.register("t", "<message...>", "Send a message only to your teammates.") { a, p : Playerc -> Client(a, p).t() }
            handler.register("team", "<team_name> [name]", "Change team") { a, p : Playerc -> Client(a, p).team() }
            handler.register("tempban", "<player> <time> [reason]", "Ban the player for a certain period of time.") { a, p : Playerc -> Client(a, p).tempban() }
            handler.register("time", "Show server time") { a, p : Playerc -> Client(a, p).time() }
            handler.register("tp", "<player>", "Teleport to other players") { a, p : Playerc -> Client(a, p).tp() }
            handler.register("tpp", "[player]", "Lock on camera the target player.") { a, p : Playerc -> Client(a, p).tpp() }
            handler.register("track", "Displays the mouse positions of players.") { a, p : Playerc -> Client(a, p).track() }
            handler.register("unban", "<uuid>", "Unban player") { a, p : Playerc -> Client(a, p).unban() }
            handler.register("unmute", "<player>", "Unmute player") { a, p : Playerc -> Client(a, p).unmute() }
            handler.register("url", "<command>", "Opens a URL contained in a specific command.") { a, p : Playerc -> Client(a, p).url() }
            handler.register("weather", "<rain/snow/sandstorm/sporestorm> <seconds>", "Adds a weather effect to the map.") { a, p : Playerc -> Client(a, p).weather() }
            clientCommands = handler
        } else {
            handler.register("debug", "[bool]", "Show plugin internal informations") { a -> Server(a).debug() }
            handler.register("gen", "Generate README.md texts") { a -> Server(a).genDocs() }
            handler.register("reload", "Reload permission and config files.") { a -> Server(a).reload() }
            handler.register("setperm", "<player> <group>", "Set the player's permission group.") { a -> Server(a).setperm() }
            handler.register("tempban", "<player> <time> [reason]", "Ban the player for a certain period of time.") { a -> Server(a).tempban() }
            serverCommands = handler
        }
    }

    class Client(val arg : Array<String>, private val player : Playerc) {
        private var bundle = Bundle()
        private var data = DB.PlayerData()

        init {
            val d = findPlayerData(player.uuid())
            if (d != null) {
                data = d
            } else {
                DB.PlayerData()
            }
            bundle = Bundle(data.languageTag)
        }

        private fun send(msg : String, vararg parameters : Any) {
            player.sendMessage(MessageFormat.format(bundle.resource.getString(msg), *parameters))
        }

        fun err(key : String, vararg parameters : Any) {
            player.sendMessage("[scarlet]" + MessageFormat.format(bundle.resource.getString(key), *parameters))
        }

        fun changemap() {
            if (!Permission.check(player, "changemap")) return
            var map = maps.all().find { e -> e.name().contains(arg[0]) }
            if (map == null) {
                val list = maps.all().sortedBy { a -> a.name() }
                val arr = ObjectMap<Map, Int>()
                list.forEachIndexed { index, m ->
                    arr.put(m, index)
                }
                arr.forEach {
                    if (it.value == arg[0].toInt()) {
                        map = it.key
                        return@forEach
                    }
                }
            }

            if (map != null) {
                try {
                    val mode = if (arg.size != 1) {
                        Gamemode.valueOf(arg[1])
                    } else {
                        state.rules.mode()
                    }

                    val reloader = WorldReloader()
                    reloader.begin()
                    world.loadMap(map, map.applyRules(mode))
                    state.rules = state.map.applyRules(mode)
                    logic.play()
                    reloader.end()
                } catch (_ : IllegalArgumentException) {
                    err("command.changemap.mode.not.found")
                }
            } else {
                err("command.changemap.map.not.found")
            }
        }

        fun changename() {
            if (!Permission.check(player, "changename")) return
            if (arg.size != 1) {
                val target = findPlayers(arg[1])
                if (target != null) {
                    val data = findPlayerData(target.uuid())
                    if (data != null) {
                        Events.fire(PlayerNameChanged(data.name, arg[0], data.uuid))
                        data.name = arg[0]
                        target.name(arg[0])
                        database.queue(data)
                    } else {
                        err("player.not.registered")
                    }
                } else {
                    err("player.not.found")
                }
            } else {
                data.name = arg[0]
                player.name(arg[0])
                database.queue(data)
            }
            send("command.changename.apply")
        }

        fun changepw() {
            if (!Permission.check(player, "changepw")) return
            if (arg.size != 2) {
                err("command.changepw.empty")
                return
            }

            if (arg[0] != arg[1]) {
                err("command.changepw.same")
                return
            }

            val password = BCrypt.hashpw(arg[0], BCrypt.gensalt())
            data.accountPW = password
            database.queue(data)
            send("command.changepw.apply")
        }

        fun chat() {
            if (!Permission.check(player, "chat")) return
            Event.isGlobalMute = arg[0].equals("on", true)
            if (Event.isGlobalMute) {
                send("command.chat.off")
            } else {
                send("command.chat.on")
            }
        }

        fun chars(tile : Tile?) {
            if (!Permission.check(player, "chars")) return
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
                texts.forEach {
                    val pos = Seq<IntArray>()
                    if (letters.containsKey(it.uppercaseChar().toString())) {
                        val target = letters[it.uppercaseChar().toString()]
                        var xv = 0
                        var yv = 0
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
                            if (t != null) {
                                val tar = world.tile(t.x + pos[a][0], t.y + pos[a][1])
                                if (target[a] == 1) {
                                    Call.setTile(tar, Blocks.scrapWall, Team.sharded, 0)
                                } else if (tar != null) {
                                    Call.setTile(tar, tar.block(), Team.sharded, 0)
                                }
                            }
                        }
                        val left : Int = when (target.size) {
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
        }

        fun color() {
            if (!Permission.check(player, "color")) return
            data.animatedName = !data.animatedName
        }

        fun broadcast() {
            if (!Permission.check(player, "broadcast")) return
            if (Main.connectType) {
                Trigger.Server.sendAll("message", arg[0])
            } else {
                Trigger.Client.message(arg[0])
            }
        }

        fun discord() {
            if (!Permission.check(player, "discord")) return
            if (Config.discordURL.isNotEmpty()) Call.openURI(player.con(), Config.discordURL)
            if (data.discord == null) {
                val number = if (Discord.pin.containsKey(player.uuid())) {
                    Discord.pin.get(player.uuid())
                } else {
                    Discord.queue(player)
                }
                send("command.discord.pin", number)
            } else {
                send("command.discord.already")
            }
        }

        fun dps() {
            if (!Permission.check(player, "dps")) return
            if (Event.dpsTile == null) {
                Call.constructFinish(player.tileOn(), Blocks.thoriumWallLarge, player.unit(), 0, state.rules.waveTeam, null)
                Event.dpsTile = player.tileOn()
                send("command.dps.created")
            } else {
                Call.deconstructFinish(Event.dpsTile, Blocks.air, player.unit())
                Event.dpsTile = null
                send("command.dps.deleted")
            }
        }

        fun effect() {
            if (!Permission.check(player, "effect")) return
            if (arg[0].toIntOrNull() != null) {
                if (arg[0].toInt() <= data.level) {
                    data.effectLevel = arg[0].toInt()
                    if (arg.size == 2) {
                        try {
                            if (Colors.get(arg[1]) == null) {
                                Color.valueOf(arg[1])
                            }

                            data.effectColor = arg[1]
                            database.queue(data)
                        } catch (_ : IllegalArgumentException) {
                            err("command.effect.no.color")
                        } catch (_ : StringIndexOutOfBoundsException) {
                            err("command.effect.no.color")
                        }
                    }
                } else {
                    err("command.effect.level")
                }
            } else if (arg[0].equals("off")) {
                data.showLevelEffects = false
                send("command.effect.off")
            } else if (arg[0].equals("on")) {
                data.showLevelEffects = true
                send("command.effect.on")
            } else {
                err("command.effect.invalid")
            }
        }

        fun exp() {
            if (!Permission.check(player, "exp")) return

            fun set(exp : Int?, type : String) {
                var b : DB.PlayerData = data
                if (exp != null) {
                    if (arg.size == 3) {
                        val target = findPlayers(arg[2])
                        if (target != null) {
                            val data = findPlayerData(target.uuid())
                            if (data != null) b = data
                        } else {
                            val p = findPlayersByName(arg[2])
                            if (p != null) {
                                val a = database[p.id]
                                if (a != null) {
                                    b = a
                                } else {
                                    err("player.not.registered")
                                }
                            } else {
                                err("player.not.found")
                            }
                        }
                    }

                    val previous = b.exp
                    when (type) {
                        "set" -> b.exp = arg[1].toInt()
                        "add" -> b.exp += arg[1].toInt()
                        "remove" -> b.exp -= arg[1].toInt()
                    }
                    database.queue(b)
                    send("command.exp.result", previous, data.exp)
                } else {
                    err("command.exp.invalid")
                }
            }
            when (arg[0]) {
                "set" -> {
                    if (!Permission.check(player, "exp.admin")) return
                    set(arg[1].toInt(), "set")
                }

                "hide" -> {
                    if (!Permission.check(player, "exp.admin")) return
                    var b = data

                    if (arg.size == 2) {
                        val target = findPlayers(arg[1])
                        if (target != null) {
                            val data = findPlayerData(target.uuid())
                            if (data != null) b = data
                        } else {
                            err("player.not.found")
                        }
                    }

                    b.hideRanking = !b.hideRanking
                    val msg = if (b.hideRanking) "unhide" else "hide"
                    send("command.exp.ranking.$msg")
                }

                "add" -> {
                    if (!Permission.check(player, "exp.admin")) return
                    set(arg[1].toInt(), "add")
                }

                "remove" -> {
                    if (!Permission.check(player, "exp.admin")) return
                    set(arg[1].toInt(), "remove")
                }

                else -> {
                    err("command.exp.invalid.command")
                }
            }
        }

        fun fillitems() {
            if (!Permission.check(player, "fillitems")) return
            val team = selectTeam(arg[0])

            if (state.teams.cores(team).isEmpty) {
                err("command.fillitems.core.empty")
            }

            content.items().forEach {
                state.teams.cores(team).first().items[it] = state.teams.cores(team).first().storageCapacity
            }

            send("command.fillitems.core.filled")
        }

        fun freeze() {
            if (!Permission.check(player, "freeze")) return
            if (arg.isEmpty()) {
                err("player.not.found")
            } else {
                val target = findPlayers(arg[0])
                if (target != null) {
                    val data = findPlayerData(target.uuid())
                    if (data != null) {
                        data.freeze = !data.freeze
                        val msg = if (data.freeze) {
                            data.status.put("freeze", "${target.x}/${target.y}")
                            "done"
                        } else {
                            data.status.remove("freeze")
                            "undo"
                        }
                        send("command.freeze.$msg", target.plainName())
                    } else {
                        err("player.not.registered")
                    }
                } else {
                    err("player.not.found")
                }
            }
        }

        fun gg() {
            if (!Permission.check(player, "gg")) return
            if (arg.isEmpty()) {
                Events.fire(EventType.GameOverEvent(state.rules.waveTeam))
            } else {
                Events.fire(EventType.GameOverEvent(selectTeam(arg[0])))
            }

            player.unit().buildSpeedMultiplier(100f)
        }

        fun god() {
            if (!Permission.check(player, "god")) return

            player.unit().health(1.0E8f)
            send("command.god")
        }

        fun help() {
            if (!Permission.check(player, "help")) return
            if (arg.isNotEmpty() && !Strings.canParseInt(arg[0])) {
                try {
                    send("command.help.${arg[0]}")
                } catch (e : MissingResourceException) {
                    err("command.help.not.exists")
                }
                return
            }

            val temp = Seq<String>()
            for (a in 0 until netServer.clientCommands.commandList.size) {
                val command = netServer.clientCommands.commandList[a]
                if (Permission.check(player, command.text)) {
                    temp.add("[orange] /${command.text} [white]${command.paramText} [lightgray]- ${bundle["command.description." + command.text]}\n")
                }
            }
            val result = StringBuilder()
            val per = 8
            var page = if (arg.isNotEmpty()) abs(Strings.parseInt(arg[0])) else 1
            val pages = Mathf.ceil(temp.size.toFloat() / per)
            page--

            if (page >= pages || page < 0) {
                err("command.page.range", pages)
                return
            }

            result.append("[orange]-- ${bundle["command.page"]}[lightgray] ${page + 1}[gray]/[lightgray]${pages}[orange] --\n")
            for (a in per * page until (per * (page + 1)).coerceAtMost(temp.size)) {
                result.append(temp[a])
            }
            player.sendMessage(result.toString().substring(0, result.length - 1))
        }

        fun hub() {
            if (!Permission.check(player, "hub")) return
            val type = arg[0]
            val x = player.tileX()
            val y = player.tileY()
            val name = state.map.name()
            var ip = ""
            var port = 6567
            if (arg.size > 1) {
                if (arg[1].contains(":")) {
                    val address = arg[1].split(":").toTypedArray()
                    ip = address[0]

                    if (address[1].toIntOrNull() == null) {
                        err("command.hub.address.port.invalid")
                        return
                    }
                    port = address[1].toInt()
                } else {
                    ip = arg[1]
                }
            }

            when (type) {
                "set" -> {
                    if (PluginData["hubMode"] == null) {
                        PluginData.status.put("hubMode", state.map.name())
                        send("command.hub.mode.on")
                    } else {
                        PluginData.status.remove("hubMode")
                        send("command.hub.mode.off")
                    }
                }

                "zone" -> {
                    if (!data.status.containsKey("hub_first") && !data.status.containsKey("hub_second")) {
                        data.status.put("hub_ip", ip)
                        data.status.put("hub_port", port.toString())
                        data.status.put("hub_first", "true")
                        send("command.hub.zone.first")
                    } else {
                        send("command.hub.zone.process")
                    }
                }

                "block" -> if (arg.size != 3) {
                    err("command.hub.block.parameter")
                } else {
                    val t : Tile = player.tileOn()
                    PluginData.warpBlocks.add(PluginData.WarpBlock(name, t.build.tileX(), t.build.tileY(), t.block().name, t.block().size, ip, port, arg[2]))
                    send("command.hub.block.added", "$x:$y", ip)
                }

                "count" -> {
                    if (arg.size < 2) {
                        err("command.hub.count.parameter")
                    } else {
                        PluginData.warpCounts.add(PluginData.WarpCount(name, world.tile(x, y).pos(), ip, port, 0, 1))
                        send("command.hub.count", "$x:$y", ip)
                    }
                }

                "total" -> {
                    PluginData.warpTotals.add(PluginData.WarpTotal(name, world.tile(x, y).pos(), 0, 1))
                    send("command.hub.total", "$x:$y")
                }

                "remove" -> {
                    PluginData.warpBlocks.removeAll { a -> a.ip == ip && a.port == port }
                    PluginData.warpZones.removeAll { a -> a.ip == ip && a.port == port }
                    send("command.hub.removed", ip, port)
                }

                "reset" -> {
                    PluginData.warpTotals.clear()
                    PluginData.warpCounts.clear()
                }

                else -> send("command.hub.help")
            }
            PluginData.save(false)
            PluginData.changed = true
        }

        fun hud() {
            if (!Permission.check(player, "hud")) return
            val status = if (data.hud != null) JsonObject.readJSON(data.hud).asArray() else JsonArray()
            when (arg[0]) {
                "health" -> {
                    if (status.contains("health")) {
                        var i = 0
                        while (i < status.size()) {
                            if (status.get(i).asString() == "health") {
                                status.remove(i)
                            } else {
                                i++
                            }
                        }
                    } else {
                        status.add("health")
                    }
                }

                else -> {
                    err("command.hud.not.found")
                }
            }

            data.hud = if (status.size() != 0) status.toString() else null
        }

        fun info() {
            if (!Permission.check(player, "info")) return

            fun timeFormat(seconds : Long, msg : String) : String {
                val days = seconds / (24 * 60 * 60)
                val hours = (seconds % (24 * 60 * 60)) / (60 * 60)
                val minutes = ((seconds % (24 * 60 * 60)) % (60 * 60)) / 60
                val remainingSeconds = ((seconds % (24 * 60 * 60)) % (60 * 60)) % 60

                return when (msg) {
                    "command.info.time" -> bundle["command.info.time", days, hours, minutes, remainingSeconds]
                    "command.info.time.minimal" -> bundle["command.info.time.minimal", hours, minutes, remainingSeconds]
                    else -> ""
                }
            }

            fun show(target : DB.PlayerData) : String {
                return """
                        ${bundle["command.info.name"]}: ${target.name}[white]
                        ${bundle["command.info.placecount"]}: ${target.blockPlaceCount}
                        ${bundle["command.info.breakcount"]}: ${target.blockBreakCount}
                        ${bundle["command.info.level"]}: ${target.level}
                        ${bundle["command.info.exp"]}: ${Exp[target]}
                        ${bundle["command.info.joindate"]}: ${Timestamp(target.firstPlayDate).toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd a HH:mm:ss"))}
                        ${bundle["command.info.playtime"]}: ${timeFormat(target.totalPlayTime, "command.info.time")}
                        ${bundle["command.info.playtime.current"]}: ${timeFormat(target.currentPlayTime, "command.info.time.minimal")}
                        ${bundle["command.info.attackclear"]}: ${target.attackModeClear}
                        ${bundle["command.info.pvpwinrate"]}: [green]${target.pvpVictoriesCount}[white]/[scarlet]${target.pvpDefeatCount}[white]([sky]${if (target.pvpVictoriesCount + target.pvpDefeatCount != 0) round(target.pvpVictoriesCount.toDouble() / (target.pvpVictoriesCount + target.pvpDefeatCount) * 100) else "0%"}%[white])
                        ${bundle["command.info.joinstacks"]}: ${target.joinStacks}
                        """.trimIndent()
            }

            val lineBreak = "\n"

            if (arg.isNotEmpty()) {
                if (!Permission.check(player, "info.admin")) return
                val target = findPlayers(arg[0])
                var targetData : DB.PlayerData? = null

                fun banPlayer(data : DB.PlayerData?) {
                    if (data != null) {
                        val name = data.name
                        val ip = netServer.admins.getInfo(data.uuid).lastIP

                        val ipBanList = JsonArray()
                        for (a in netServer.admins.getInfo(data.uuid).ips) {
                            ipBanList.add(a)
                        }

                        val json = JsonObject()
                        json.add("id", data.uuid)
                        json.add("ip", ipBanList)
                        json.add("name", netServer.admins.getInfo(data.uuid).names.toString())
                        Fi(Config.banList).writeString(JsonArray.readHjson(Fi(Config.banList).readString()).asArray().add(json).toString(Stringify.HJSON))

                        Event.log(Event.LogType.Player, Bundle()["log.player.banned", name, ip])
                    }
                }

                val controlMenus = arrayOf(
                    arrayOf(bundle["info.button.close"]),
                    arrayOf(bundle["info.button.ban"], bundle["info.button.kick"])
                )

                val banMenus = arrayOf(
                    arrayOf(bundle["info.button.tempban.10min"], bundle["info.button.tempban.1hour"], bundle["info.button.tempban.1day"]),
                    arrayOf(bundle["info.button.tempban.1week"], bundle["info.button.tempban.2week"], bundle["info.button.tempban.1month"]),
                    arrayOf(bundle["info.button.ban.permanently"]),
                    arrayOf(bundle["info.button.close"])
                )

                val mainMenu = Menus.registerMenu { player, select ->
                    if (select == 1) {
                        val innerMenu = Menus.registerMenu { _, s ->
                            val time = when (s) {
                                0 -> 10
                                1 -> 60
                                2 -> 1440
                                3 -> 10080
                                4 -> 20160
                                5 -> 43800
                                else -> 0
                            }

                            val timeText = bundle["info.button.tempban.${
                                when (s) {
                                    0 -> "10min"
                                    1 -> "1hour"
                                    2 -> "1day"
                                    3 -> "1week"
                                    4 -> "2week"
                                    5 -> "1month"
                                    else -> ""
                                }
                            }"]

                            if (s <= 5) {
                                val tempBanConfirmMenu = Menus.registerMenu { _, i ->
                                    if (i == 0) {
                                        data.banTime = time.toString()
                                        database.queue(data)
                                        banPlayer(data)
                                    }
                                }

                                Call.menu(player.con(), tempBanConfirmMenu, bundle["info.title.tempban"], bundle["info.tempban.comfirm", timeText] + lineBreak, arrayOf(arrayOf(bundle["info.button.ban"], bundle["info.button.cancel"])))
                            } else {
                                val banConfirmMenu = Menus.registerMenu { _, i ->
                                    if (i == 0) {
                                        banPlayer(targetData)
                                    }
                                }

                                Call.menu(player.con(), banConfirmMenu, bundle["info.title.ban.time"], bundle["info.ban.time"] + lineBreak, arrayOf(arrayOf(bundle["info.button.ban"], bundle["info.button.cancel"])))
                            }
                        }
                        Call.menu(player.con(), innerMenu, bundle["info.title.ban.time"], bundle["info.ban.comfirm"] + lineBreak, banMenus)
                    } else if (select == 2) {
                        if (target != null) {
                            Call.kick(target.con(), Packets.KickReason.kick)
                        }
                    }
                }

                if (target != null) {
                    val banned = "\n${bundle["info.banned"]}: ${(netServer.admins.isIDBanned(target.uuid()) || netServer.admins.isIPBanned(target.con().address))}"
                    val other = findPlayerData(target.uuid())
                    if (other != null) {
                        val menu = if (Permission.check(other.player, "info.admin")) arrayOf(arrayOf(bundle["info.button.close"])) else controlMenus
                        targetData = other
                        Call.menu(player.con(), mainMenu, bundle["info.title.admin"], show(other) + banned + lineBreak, menu)
                    } else {
                        err("player.not.found")
                    }
                } else {
                    val p = findPlayersByName(arg[0])
                    if (p != null) {
                        val banned = "\n${bundle["info.banned"]}: ${(netServer.admins.isIDBanned(p.id) || netServer.admins.isIPBanned(p.lastIP))}"
                        val other = database[p.id]
                        if (other != null) {
                            val menu = if (Permission.check(other.player, "info.admin")) arrayOf(arrayOf(bundle["info.button.close"])) else controlMenus
                            targetData = other
                            Call.menu(player.con(), mainMenu, bundle["info.title.admin"], show(other) + banned + lineBreak, menu)
                        } else {
                            err("player.not.registered")
                        }
                    } else {
                        err("player.not.found")
                    }
                }
            } else {
                Call.menu(player.con(), -1, bundle["info.title"], show(data) + lineBreak, arrayOf(arrayOf(bundle["info.button.close"])))
            }
        }

        fun js() {
            if (!Permission.check(player, "js")) {
                Call.kick(player.con(), bundle["command.js.no.permission"])
                return
            }
            if (arg.isEmpty()) {
                err("command.js.invalid")
            } else {
                val output = mods.scripts.runConsole(arg[0])
                try {
                    val errorName = output?.substring(0, output.indexOf(' ') - 1)
                    Class.forName("org.mozilla.javascript.$errorName")
                    player.sendMessage("> [#ff341c]$output")
                } catch (e : Throwable) {
                    player.sendMessage("[scarlet]> $output")
                }
            }
        }

        fun kickall() {
            if (!Permission.check(player, "kickall")) return
            Groups.player.forEach {
                if (!it.admin) Call.kick(it.con, Packets.KickReason.kick)
            }
        }

        fun kill() {
            if (!Permission.check(player, "kill")) return
            if (arg.isEmpty()) {
                player.unit().kill()
            } else {
                val other = findPlayers(arg[0])
                if (other == null) err("player.not.found") else other.unit().kill()
            }
        }

        fun killall() {
            if (!Permission.check(player, "killall")) return
            if (arg.isEmpty()) {
                repeat(Team.all.count()) {
                    Groups.unit.each { u : Unit -> if (player.team() == u.team) u.kill() }
                }
            } else {
                val team = selectTeam(arg[0])
                Groups.unit.each { u -> if (u.team == team) u.kill() }
            }
        }

        fun killunit() {
            if (!Permission.check(player, "killunit")) return
            val unit = content.units().find { unitType : UnitType -> unitType.name == arg[0] }

            fun destroy(team : Team) {
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
                        err("command.killunit.invalid.number")
                    }
                } else {
                    for (it in Groups.unit) {
                        if (it.type() == unit && it.team == player.team()) {
                            it.kill()
                        }
                    }
                }
            } else {
                err("command.killunit.not.found")
            }
        }

        fun lang() {
            if (!Permission.check(player, "lang")) return
            if (arg.isEmpty()) {
                err("command.language.empty")
                return
            }
            data.languageTag = arg[0]
            database.queue(data)
            send("command.language.set", Locale(arg[0]).language)
            player.sendMessage(Bundle(arg[0])["command.language.preview", Locale(arg[0]).toLanguageTag()])
        }

        fun log() {
            if (!Permission.check(player, "log")) return
            data.log = !data.log
            val msg = if (data.log) {
                "enabled"
            } else {
                "disabled"
            }
            send("command.log.$msg")
        }

        fun login() {
            if (!Permission.check(player, "login")) return
            if (arg[0] == arg[1]) {
                err("command.login.same.password")
                return
            }

            val result = database.search(arg[0], arg[1])
            if (result != null) {
                if (result.accountID == result.accountPW) {
                    Bundle(player.locale())["command.login.default.password"]
                } else if (result.isConnected) {
                    Bundle(player.locale())["command.login.already"]
                } else {
                    if (findPlayerData(result.uuid) == null) {
                        database.players.remove { a -> a.uuid == player.uuid() }
                        result.oldUUID = result.uuid
                        result.uuid = player.uuid()
                        Trigger.loadPlayer(player, result, true)
                    } else {
                        Bundle(player.locale())["command.login.already"]
                    }
                }
            } else {
                err("command.login.not.found")
            }
        }

        fun maps() {
            if (!Permission.check(player, "maps")) return
            val list = maps.all().sortedBy { a -> a.name() }
            val prebuilt = Seq<Pair<String, Array<Array<String>>>>()
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

            data.status.put("page", "0")

            var mainMenu = 0
            mainMenu = Menus.registerMenu { player, select ->
                var page = data.status.get("page").toInt()
                when (select) {
                    0 -> {
                        if (page != 0) page--
                        Call.menu(player.con(), mainMenu, title, prebuilt.get(page).first, prebuilt.get(page).second)
                    }

                    1 -> {
                        Call.menu(player.con(), mainMenu, title, prebuilt.get(page).first, prebuilt.get(page).second)
                    }

                    2 -> {
                        if (page != pages) page++
                        Call.menu(player.con(), mainMenu, title, prebuilt.get(page).first, prebuilt.get(page).second)
                    }

                    else -> {
                        data.status.remove("page")
                    }
                }
                data.status.put("page", page.toString())
            }
            Call.menu(player.con(), mainMenu, title, prebuilt.get(0).first, prebuilt.get(0).second)
        }

        fun me() {
            if (!Permission.check(player, "me") || data.mute) return
            if (Config.chatBlacklist) {
                val file = root.child("chat_blacklist.txt").readString("UTF-8").split("\r\n")
                if (file.isNotEmpty()) {
                    file.forEach {
                        if ((Config.chatBlacklistRegex && arg[0].contains(Regex(it))) || (!Config.chatBlacklistRegex && arg[0].contains(it))) {
                            err("event.chat.blacklisted")
                            return
                        }
                    }
                    Call.sendMessage("[brown]== [sky]${player.plainName()}[white] - [tan]${arg[0]}")
                }
            }
        }

        fun meme() {
            if (!Permission.check(player, "meme")) return
            when (arg[0]) {
                "router" -> {
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
                    if (data.status.containsKey("router")) {
                        data.status.remove("router")
                    } else {
                        Thread {
                            fun change(name : String) {
                                player.name(name)
                                sleep(500)
                            }

                            data.status.put("router", "true")
                            while (!player.isNull) {
                                loop.forEach {
                                    change(it)
                                }
                                if (!data.status.containsKey("router")) break
                                sleep(5000)
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

        fun motd() {
            if (!Permission.check(player, "motd")) return
            val motd = if (root.child("motd/${data.languageTag}.txt").exists()) {
                root.child("motd/${data.languageTag}.txt").readString()
            } else {
                val file = root.child("motd/en.txt")
                if (file.exists()) file.readString() else ""
            }
            val count = motd.split("\r\n|\r|\n").toTypedArray().size
            if (count > 10) Call.infoMessage(player.con(), motd) else player.sendMessage(motd)
        }

        fun mute() {
            if (!Permission.check(player, "mute")) return
            val other = findPlayers(arg[0])
            if (other != null) {
                val target = findPlayerData(other.uuid())
                if (target != null) {
                    target.mute = true
                    database.queue(target)
                    send("command.mute", target.name)
                } else {
                    err("player.not.found")
                }
            } else {
                val p = findPlayersByName(arg[0])
                if (p != null) {
                    val a = database[p.id]
                    if (a != null) {
                        a.mute = true
                        database.queue(a)
                        send("command.mute", a.name)
                    } else {
                        err("player.not.registered")
                    }
                } else {
                    err("player.not.found")
                }
            }
        }

        fun pause() {
            if (!Permission.check(player, "pause")) return
            if (state.isPaused) {
                state.set(GameState.State.playing)
                send("command.pause.unpaused")
            } else {
                state.set(GameState.State.paused)
                send("command.pause.paused")
            }
        }

        fun players() {
            if (!Permission.check(player, "players")) return
            val prebuilt = Seq<Pair<String, Array<Array<String>>>>()
            val buffer = Mathf.ceil(database.players.size.toFloat() / 6)
            val pages = if (buffer > 1.0) buffer - 1 else 0
            val title = bundle["command.page.server"]

            for (page in 0..pages) {
                val build = StringBuilder()
                for (a in 6 * page until (6 * (page + 1)).coerceAtMost(database.players.size)) {
                    build.append("[gray]${database.players.get(a).entityid} ${database.players.get(a).name}")
                }

                val options = arrayOf(
                    arrayOf("<-", bundle["command.players.page", page, pages], "->"),
                    arrayOf(bundle["command.players.close"])
                )

                prebuilt.add(Pair(build.toString(), options))
            }

            data.status.put("page", "0")

            var mainMenu = 0
            mainMenu = Menus.registerMenu { player, select ->
                var page = data.status.get("page").toInt()
                when (select) {
                    0 -> {
                        if (page != 0) page--
                        Call.menu(player.con(), mainMenu, title, prebuilt.get(page).first, prebuilt.get(page).second)
                    }

                    1 -> {
                        Call.menu(player.con(), mainMenu, title, prebuilt.get(page).first, prebuilt.get(page).second)
                    }

                    2 -> {
                        if (page != pages) page++
                        Call.menu(player.con(), mainMenu, title, prebuilt.get(page).first, prebuilt.get(page).second)
                    }

                    else -> {
                        data.status.remove("page")
                    }
                }
                data.status.put("page", page.toString())
            }
            Call.menu(player.con(), mainMenu, title, prebuilt.get(0).first, prebuilt.get(0).second)
        }

        fun pm() {
            if (!Permission.check(player, "pm") || data.mute) return
            val target = findPlayers(arg[0])
            if (target == null) {
                err("player.not.found")
            } else if (arg.size > 1) {
                player.sendMessage("[green][PM] ${target.plainName()}[yellow] => [white] ${arg[1]}")
                target.sendMessage("[blue][PM] [gray][${data.entityid}][]${player.plainName()}[yellow] => [white] ${arg[1]}")
                database.players.forEach {
                    if (Permission.check(it.player, "pm.other") && it.uuid != player.uuid() && target.uuid() != it.player.uuid()) {
                        it.player.sendMessage("[sky]${player.plainName()}[][yellow] => [pink]${target.plainName()} [white]: ${arg[1]}")
                    }
                }
            } else {
                err("command.pm.message")
            }
        }

        fun ranking() {
            if (!Permission.check(player, "ranking")) return
            if (PluginData.isRankingWorking) {
                err("command.ranking.working")
                return
            }
            Main.daemon.submit(Thread {
                try {
                    fun timeFormat(seconds : Long) : String {
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
                        err("command.ranking.wrong")
                        return@Thread
                    }

                    PluginData.isRankingWorking = true
                    Core.app.post { player.sendMessage(bundle["command.ranking.wait"]) }
                    val all = database.getAll()
                    val time = mutableMapOf<ArrayMap<String, String>, Long>()
                    val exp = mutableMapOf<ArrayMap<String, String>, Int>()
                    val attack = mutableMapOf<ArrayMap<String, String>, Int>()
                    val placeBlock = mutableMapOf<ArrayMap<String, String>, Int>()
                    val breakBlock = mutableMapOf<ArrayMap<String, String>, Int>()
                    val pvp = mutableMapOf<ArrayMap<String, String>, ArrayMap<Int, Int>>()

                    all.forEach {
                        if (!it.hideRanking && JsonArray.readHjson(Fi(Config.banList).readString()).asArray().find { a -> a.asObject().get("id").asString() == it.uuid } == null) {
                            val info = ArrayMap<String, String>()
                            val pvpCount = ArrayMap<Int, Int>()
                            info.put(it.name, it.uuid)
                            pvpCount.put(it.pvpVictoriesCount, it.pvpDefeatCount)

                            time[info] = it.totalPlayTime
                            exp[info] = it.exp
                            attack[info] = it.attackModeClear
                            placeBlock[info] = it.blockPlaceCount
                            breakBlock[info] = it.blockBreakCount
                            pvp[info] = pvpCount
                        }
                    }

                    val d = when (arg[0].lowercase()) {
                        "time" -> time.toList().sortedWith(compareBy { -it.second })
                        "exp" -> exp.toList().sortedWith(compareBy { -it.second })
                        "attack" -> attack.toList().sortedWith(compareBy { -it.second })
                        "place" -> placeBlock.toList().sortedWith(compareBy { -it.second })
                        "break" -> breakBlock.toList().sortedWith(compareBy { -it.second })
                        "pvp" -> pvp.toList().sortedWith(compareBy { -it.second.firstKey() })
                        else -> {
                            PluginData.isRankingWorking = true
                            return@Thread
                        }
                    }

                    val string = StringBuilder()
                    val per = 8
                    var page = if (arg.size == 2) abs(Strings.parseInt(arg[1])) else 1
                    val pages = Mathf.ceil(d.size.toFloat() / per)
                    page--

                    if (page >= pages || page < 0) {
                        Core.app.post { err("command.page.range", pages) }
                        PluginData.isRankingWorking = false
                        return@Thread
                    }
                    string.append(bundle[firstMessage, page + 1, pages] + "\n")

                    for (a in per * page until (per * (page + 1)).coerceAtMost(d.size)) {
                        if (d[a].second is ArrayMap<*, *>) {
                            val rank = d[a].second as ArrayMap<*, *>
                            val rate = round((rank.firstKey().toString().toFloat() / (rank.firstKey().toString().toFloat() + rank.firstValue().toString().toFloat())) * 100)
                            string.append("[white]$a[] ${d[a].first.firstKey()}[white] [yellow]-[] [green]${rank.firstKey()}${bundle["command.ranking.pvp.win"]}[] / [scarlet]${rank.firstValue()}${bundle["command.ranking.pvp.lose"]}[] ($rate%)\n")
                        } else {
                            string.append("[white]${a + 1}[] ${d[a].first.firstKey()}[white] [yellow]-[] ${if (arg[0].lowercase() == "time") timeFormat(d[a].second.toString().toLong()) else d[a].second}\n")
                        }
                    }
                    string.substring(0, string.length - 1)
                    string.append("[purple]=======================================[]\n")
                    for (a in d.indices) {
                        if (d[a].first.firstValue() == player.uuid()) {
                            if (d[a].second is ArrayMap<*, *>) {
                                val rank = d[a].second as ArrayMap<*, *>
                                val rate = round((rank.firstKey().toString().toFloat() / (rank.firstKey().toString().toFloat() + rank.firstValue().toString().toFloat())) * 100)
                                string.append("[white]${a + 1}[] ${d[a].first.firstKey()}[white] [yellow]-[] [green]${rank.firstKey()}${bundle["command.ranking.pvp.win"]}[] / [scarlet]${rank.firstValue()}${bundle["command.ranking.pvp.lose"]}[] ($rate%)")
                            } else {
                                string.append("[white]${a + 1}[] ${d[a].first.firstKey()}[white] [yellow]-[] ${if (arg[0].lowercase() == "time") timeFormat(d[a].second.toString().toLong()) else d[a].second}")
                            }
                        }
                    }

                    Core.app.post { player.sendMessage(string.toString()) }
                } catch (e : Exception) {
                    e.printStackTrace()
                    Core.app.exit()
                }
                PluginData.isRankingWorking = false
            })
        }

        fun register() {
            if (!Permission.check(player, "register")) return
            if (Config.authType != Config.AuthType.None) {
                if (arg.size != 3) {
                    send("command.reg.usage")
                } else if (arg[1] != arg[2]) {
                    err("command.reg.incorrect")
                } else {
                    if (transaction { DB.Player.select { DB.Player.accountID.eq(arg[0]) }.firstOrNull() } == null) {
                        Trigger.createPlayer(player, arg[0], arg[1])
                        Log.info(Bundle()["log.data_created", player.plainName()])
                    } else {
                        err("command.reg.exists")
                    }
                }
            } else {
                err("command.reg.unavailable")
            }
        }

        fun report() {
            if (!Permission.check(player, "report")) return
            val target = findPlayers(arg[0])
            if (target != null) {
                val reason = arg[1]
                val infos = netServer.admins.findByName(target.uuid()).first()
                val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                val text = Bundle()["command.report.texts", target.plainName(), player.plainName(), reason, infos.lastName, infos.names, infos.id, infos.lastIP, infos.ips]

                Event.log(Event.LogType.Report, date + text, target.plainName())
                Log.info(Bundle()["command.report.received", player.plainName(), target.plainName(), reason])
                send("command.report.done", target.plainName())
            } else {
                err("player.not.found")
            }
        }

        fun rollback() {
            if (!Permission.check(player, "rollback")) return

            state.set(GameState.State.paused)
            worldHistory.forEach {
                val buf = Seq<Event.TileLog>()
                if (it.player.contains(arg[0])) {
                    worldHistory.forEach { two ->
                        if (two.x == it.x && two.y == it.y) {
                            buf.add(two)
                        }
                    }

                    val last = buf.last()
                    if (last.action == "place") {
                        Call.removeTile(world.tile(last.x.toInt(), last.y.toInt()))
                    } else if (last.action == "break") {
                        Call.setTile(world.tile(last.x.toInt(), last.y.toInt()), content.block(last.tile), last.team, last.rotate)

                        run breaking@{
                            buf.reverse().forEach { tile ->
                                if (tile.value != null) {
                                    Call.tileConfig(null, world.tile(last.x.toInt(), last.y.toInt()).build, tile.value)
                                    return@breaking
                                }
                            }
                        }

                    }
                }
            }
            worldHistory.removeAll { a -> a.player.contains(arg[0]) }
            state.set(GameState.State.playing)
        }

        fun search() {
            if (!Permission.check(player, "search")) return
            if (arg[0].isEmpty()) {
                err("player.not.found")
                return
            }

            val result = ArrayList<DB.PlayerData?>()
            val data = findPlayers(arg[0])

            if (data == null) {
                val e = netServer.admins.findByName(arg[0])
                if (e.size > 0) {
                    e.forEach {
                        result.add(database[it.id])
                    }
                } else {
                    result.add(database[arg[0]])
                }
            } else {
                result.add(database[data.uuid()])
            }

            if (result.size > 0) {
                result.forEach {
                    if (it != null) {
                        val texts = """
                        ${bundle["command.info.name"]}: ${it.name}
                        ${bundle["command.info.uuid"]}: ${it.uuid}
                        ${bundle["command.info.languageTag"]}: ${it.languageTag}
                        ${bundle["command.info.placecount"]}: ${it.blockPlaceCount}
                        ${bundle["command.info.breakcount"]}: ${it.blockBreakCount}
                        ${bundle["command.info.joincount"]}: ${it.totalJoinCount}
                        ${bundle["command.info.kickcount"]}: ${it.totalKickCount}
                        ${bundle["command.info.level"]}: ${it.level}
                        ${bundle["command.info.exp"]}: ${it.exp}
                        ${bundle["command.info.joindate"]}: ${it.firstPlayDate}
                        ${bundle["command.info.lastdate"]}: ${it.lastLoginTime}
                        ${bundle["command.info.playtime"]}: ${it.totalPlayTime}
                        ${bundle["command.info.attackclear"]}: ${it.attackModeClear}
                        ${bundle["command.info.pvpwincount"]}: ${it.pvpVictoriesCount}
                        ${bundle["command.info.pvplosecount"]}: ${it.pvpDefeatCount}
                        ${bundle["command.info.colornick"]}: ${it.animatedName}
                        ${bundle["command.info.permission"]}: ${it.permission}
                        ${bundle["command.info.mute"]}: ${it.mute}
                        ${bundle["command.info.status"]}: ${it.status}
                        """.trimIndent()
                        player.sendMessage(texts)
                    }
                }
                send("command.search.total", result.size) // todo 메뉴 형식으로 바꾸기
            }
        }

        fun setitem() {
            if (!Permission.check(player, "setitem")) return

            fun set(item : Item) {
                val amount = arg[1].toIntOrNull()
                if (amount != null) {
                    if (arg.size == 3) {
                        val team = Team.all.find { a -> a.name.equals(arg[2]) }
                        if (team != null) {
                            team.core().items.set(item, if (team.core().storageCapacity < arg[1].toInt()) team.core().storageCapacity else arg[1].toInt())
                        } else {
                            err("command.setitem.wrong.team")
                        }
                    } else {
                        player.core().items.set(item, if (player.core().storageCapacity < arg[1].toInt()) player.core().storageCapacity else arg[1].toInt())
                    }
                } else {
                    err("command.setitem.wrong.amount")
                }
            }

            val item = content.item(arg[0])
            if (item != null) {
                set(item)
            } else if (!arg[0].equals("all", true)) {
                content.items().forEach {
                    set(it)
                }
            } else {
                err("command.setitem.item.not.exists")
            }
        }

        fun setperm() {
            if (!Permission.check(player, "setperm")) return
            val target = findPlayers(arg[0])
            if (target != null) {
                val data = findPlayerData(target.uuid())
                if (data != null) {
                    data.permission = arg[1]
                    send("command.setperm.success", data.name, arg[1])
                } else {
                    err("player.not.registered")
                }
            } else {
                val p = findPlayersByName(arg[1])
                if (p != null) {
                    val a = database[p.id]
                    if (a != null) {
                        a.permission = arg[1]
                        database.queue(a)
                        send("command.setperm.success", a.name, arg[1])
                    } else {
                        err("player.not.registered")
                    }
                } else {
                    err("player.not.found")
                }
            }
        }

        fun skip() {
            if (!Permission.check(player, "skip")) return
            val wave = arg[0].toIntOrNull()
            if (wave != null) {
                if (wave > 0) {
                    val previousWave = state.wave
                    var loop = 0
                    while (arg[0].toInt() != loop) {
                        loop++
                        spawner.spawnEnemies()
                        state.wave++
                        state.wavetime = state.rules.waveSpacing
                    }
                    send("command.skip.process", previousWave, state.wave)
                } else {
                    err("command.skip.number.low")
                }
            } else {
                err("command.skip.number.invalid")
            }
        }

        fun spawn() {
            if (!Permission.check(player, "spawn")) return
            val type = arg[0]
            val name = arg[1]
            val parameter = if (arg.size == 3) arg[2].toIntOrNull() else 1
            val spread = (tilesize * 1.5).toFloat()

            when {
                type.equals("unit", true) -> {
                    val unit = content.units().find { unitType : UnitType -> unitType.name == name }
                    if (unit != null) {
                        if (parameter != null) {
                            if (!unit.hidden) {
                                unit.useUnitCap = false
                                for (a in 1..parameter) {
                                    Tmp.v1.rnd(spread)
                                    unit.spawn(player.team(), player.x + Tmp.v1.x, player.y + Tmp.v1.y)
                                }
                            } else {
                                err("command.spawn.unit.invalid")
                            }
                        } else {
                            err("command.spawn.number")
                        }
                    } else {
                        err("command.spawn.invalid")
                    }
                }

                type.equals("block", true) -> {
                    if (content.blocks().find { a -> a.name == name } != null) {
                        Call.constructFinish(player.tileOn(), content.blocks().find { a -> a.name.equals(name, true) }, player.unit(), parameter?.toByte() ?: 0, player.team(), null)
                    } else {
                        err("command.spawn.invalid")
                    }
                }

                else -> {
                    return
                }
            }
        }

        fun status() {
            fun longToTime(seconds : Long) : String {
                val min = seconds / 60
                val hour = min / 60
                val days = hour / 24
                return String.format("%d:%02d:%02d:%02d", days % 365, hour % 24, min % 60, seconds % 60)
            }

            if (!Permission.check(player, "status")) return
            val bans = JsonArray.readHjson(Fi(Config.banList).readString()).asArray().size()

            val message = StringBuilder()
            message.append("""
                [#DEA82A]${bundle["command.status.info"]}[]
                [#2B60DE]========================================[]
                ${bundle["command.status.name"]}: ${state.map.name()}[white]
                TPS: ${Core.graphics.framesPerSecond}/60
                ${bundle["command.status.banned", bans]}
                ${bundle["command.status.playtime"]}: ${longToTime(PluginData.playtime)}
                ${bundle["command.status.uptime"]}: ${longToTime(PluginData.uptime)}
            """.trimIndent())

            if (state.rules.pvp) {
                message.appendLine()
                message.appendLine("""
                    [#2B60DE]========================================[]
                    [#DEA82A]${bundle["command.status.pvp"]}[]
                """.trimIndent())

                var teamStatus = arrayOf<Triple<Team, String, Int>>()
                val teams = ObjectMap<Team, Double>()
                database.players.forEach {
                    teamStatus += (Triple(it.player.team(), it.name, if (it.pvpVictoriesCount != 0) (it.pvpVictoriesCount / (it.pvpVictoriesCount + it.pvpDefeatCount)) * 100 else 0))
                }

                fun winPercentage(team : Team) : Double? {
                    val teamPlayers = teamStatus.filter { it.first == team }
                    val winPercentages = teamPlayers.map { it.third }
                    if (winPercentages.isEmpty()) {
                        return null
                    }
                    return winPercentages.average()
                }

                for (a in state.teams.active) {
                    if (winPercentage(a.team) != null) {
                        teams.put(a.team, winPercentage(a.team))
                    }
                }

                teams.forEach {
                    message.appendLine("${it.key.coloredName()} : ${round(it.value)}%")
                }

                player.sendMessage(message.toString().dropLast(1))
            } else {
                player.sendMessage(message.toString())
            }
        }

        fun t() {
            if (!data.mute) {
                Groups.player.each({ p -> p.team() === player.team() }) { o ->
                    o.sendMessage("[#" + player.team().color.toString() + "]<T>[] ${player.coloredName()} [orange]>[white] ${arg[0]}")
                }
            }
        }

        fun team() {
            if (!Permission.check(player, "team")) return
            val team = selectTeam(arg[0])

            if (arg.size == 1) {
                player.team(team)
            } else if (Permission.check(player, "team.other")) {
                val other = findPlayers(arg[1])
                if (other != null) {
                    other.team(team)
                } else {
                    err("player.not.found")
                }
            }
        }

        fun tempban() {
            if (!Permission.check(player, "tempban")) return
            val other = findPlayers(arg[0])

            if (other == null) {
                err("player.not.found")
            } else {
                val d = findPlayerData(other.uuid())
                if (d == null) {
                    send("command.tempban.not.registered")
                    netServer.admins.banPlayer(other.uuid())
                    Call.kick(other.con(), Packets.KickReason.banned)
                } else {
                    val time = LocalDateTime.now()
                    val minute = arg[1].toLongOrNull()
                    val reason = arg[2]

                    if (minute != null) { // todo d h m s 날짜 형식 지원
                        d.banTime = time.plusMinutes(minute.toLong()).toString()
                        netServer.admins.banPlayer(other.uuid())
                        Call.kick(other.con(), reason)
                    } else {
                        err("command.tempban.not.number")
                    }
                }
            }
        }

        fun time() {
            if (!Permission.check(player, "time")) return
            val now = LocalDateTime.now()
            val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd a HH:mm:ss").withLocale(Locale(data.languageTag))
            send("command.time", now.format(dateTimeFormatter))
        }

        fun tp() {
            if (!Permission.check(player, "tp")) return
            val other = findPlayers(arg[0])

            if (other == null) {
                err("player.not.found")
            } else {
                player.unit().set(other.x, other.y)
                Call.setPosition(player.con(), other.x, other.y)
                Call.setCameraPosition(player.con(), other.x, other.y)
            }
        }

        fun tpp() {
            if (!Permission.check(player, "tp")) return

            if (arg.isEmpty() && data.tpp != null && data.tppTeam != null) {
                player.team(Team.get(data.tppTeam!!))

                send("command.tpp.unfollowing")
                Call.setCameraPosition(player.con(), player.x, player.y)

                data.tppTeam = null
                data.tpp = null
            } else {
                val other = findPlayers(arg[0])
                if (other == null) {
                    err("player.not.found")
                } else {
                    data.tppTeam = player.team().id
                    data.tpp = other.uuid()
                    player.clearUnit()
                    player.team(Team.derelict)
                    send("command.tpp.following", other.plainName())
                }
            }

            if (arg.isEmpty() && data.tpp != null) {
                data.tpp = null
                data.tppTeam = 0
            }
        }

        fun track() {
            if (!Permission.check(player, "tp")) return
            data.tracking = !data.tracking
            val msg = if (data.tracking) ".disabled" else ""
            send("command.track.toggle$msg")
        }

        fun unban() {
            if (!Permission.check(player, "unban")) return
            val target = netServer.admins.findByName(arg[0])
            if (target != null) {
                val json = JsonArray.readHjson(Fi(Config.banList).readString()).asArray()
                json.removeAll { a -> a.asObject().get("id").asString() == target.first().id }
                Fi(Config.banList).writeString(json.toString(Stringify.HJSON))

                Events.fire(PlayerUnbanned(player.plainName(), target.first().lastName, LocalDateTime.now().format(DateTimeFormatter.ofPattern("YYYY-mm-dd a HH:mm:ss"))))
            } else {
                err("player.not.found")
            }
        }

        fun unmute() {
            if (!Permission.check(player, "unmute")) return
            val other = findPlayers(arg[0])
            if (other != null) {
                val target = findPlayerData(other.uuid())
                if (target != null) {
                    target.mute = false
                    database.queue(target)
                    send("command.unmute", target.name)
                } else {
                    err("player.not.found")
                }
            } else {
                val p = findPlayersByName(arg[0])
                if (p != null) {
                    val a = database[p.id]
                    if (a != null) {
                        a.mute = false
                        database.queue(a)
                        send("command.unmute", a.name)
                    } else {
                        err("player.not.registered")
                    }
                } else {
                    err("player.not.found")
                }
            }
        }

        fun url() {
            if (!Permission.check(player, "url")) return
            when (arg[0]) {
                "effect" -> {
                    Call.openURI(player.con(), "https://github.com/Anuken/Mindustry/blob/master/core/src/mindustry/content/Fx.java")
                }

                else -> {}
            }
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
            } catch (e : NumberFormatException) {
                err("command.weather.not.number")
            }
        }

        fun vote(player : Playerc, arg : Array<out String>) {
            fun sendStart(message : String, vararg parameter : Any) {
                Event.voted.add(player.uuid())
                database.players.forEach {
                    if (Event.isPvP) {
                        if (Event.voteTeam == it.player.team()) {
                            val data = findPlayerData(it.uuid)
                            if (data != null) {
                                val bundle = Bundle(data.languageTag)
                                it.player.sendMessage(bundle["command.vote.starter", player.plainName()])
                                it.player.sendMessage(bundle.get(message, *parameter))
                                it.player.sendMessage(bundle["command.vote.how"])
                            }
                        }
                    } else {
                        val data = findPlayerData(it.uuid)
                        if (data != null) {
                            val bundle = Bundle(data.languageTag)
                            it.player.sendMessage(bundle["command.vote.starter", player.plainName()])
                            it.player.sendMessage(bundle.get(message, *parameter))
                            it.player.sendMessage(bundle["command.vote.how"])
                        }
                    }
                }
            }
            if (!Permission.check(player, "vote")) return
            if (arg.isEmpty()) {
                err("command.vote.arg.empty")
                return
            }
            if (Event.voterCooltime.containsKey(player.plainName())) {
                err("command.vote.cooltime")
                return
            }
            if (!Event.voting) {
                if (database.players.size <= 3 && !Permission.check(player, "vote.admin")) {
                    err("command.vote.enough")
                    return
                }
                when (arg[0]) {
                    "kick" -> {
                        if (!Permission.check(player, "vote.kick")) return
                        if (arg.size != 3) {
                            err("command.vote.no.reason")
                            return
                        }
                        val target = findPlayers(arg[1])
                        if (target != null) {
                            if (Permission.check(target, "kick.admin")) {
                                err("command.vote.kick.target.admin")
                            } else {
                                Event.voteTarget = target
                                Event.voteTargetUUID = target.uuid()
                                Event.voteReason = arg[2]
                                Event.voteType = "kick"
                                Event.voteStarter = player
                                Event.voting = true
                                sendStart("command.vote.kick.start", target.plainName(), arg[2])
                            }
                        } else {
                            err("player.not.found")
                        }
                    }

                    // vote map <map name> <reason>
                    "map" -> {
                        if (!Permission.check(player, "vote.map")) return
                        if (arg.size == 1) {
                            err("command.vote.no.map")
                            return
                        }
                        if (arg.size == 2) {
                            err("command.vote.no.reason")
                            return
                        }
                        if (arg[1].toIntOrNull() != null) {
                            try {
                                var target = maps.all().find { e -> e.name().contains(arg[1]) }
                                if (target == null) {
                                    val list = maps.all().sortedBy { a -> a.name() }
                                    val arr = ObjectMap<Map, Int>()
                                    list.forEachIndexed { index, map ->
                                        arr.put(map, index)
                                    }
                                    arr.forEach {
                                        if (it.value == arg[1].toInt()) {
                                            target = it.key
                                            return@forEach
                                        }
                                    }
                                }

                                Event.voteType = "map"
                                Event.voteMap = target
                                Event.voteReason = arg[2]
                                Event.voteStarter = player
                                Event.voting = true
                                sendStart("command.vote.map.start", target.name(), arg[2])
                            } catch (e : IndexOutOfBoundsException) {
                                err("command.vote.map.not.exists")
                            }
                        } else {
                            err("command.vote.map.not.exists")
                        }
                    }

                    // vote gg
                    "gg" -> {
                        if (!Permission.check(player, "vote.gg")) return
                        if (Event.voteCooltime == 0) {
                            Event.voteType = "gg"
                            Event.voteStarter = player
                            Event.voting = true
                            if (state.rules.pvp) {
                                Event.voteTeam = player.team()
                                Event.isPvP = true
                                Event.voteCooltime = 120
                                sendStart("command.vote.gg.pvp.team")
                            } else {
                                sendStart("command.vote.gg.start")
                            }
                        } else {
                            err("command.vote.cooltime")
                        }
                    }

                    // vote skip <count>
                    "skip" -> {
                        if (!Permission.check(player, "vote.skip")) return
                        if (arg.size == 1) {
                            send("command.vote.skip.wrong")
                        } else if (arg[1].toIntOrNull() != null) {
                            if (arg[1].toInt() > 3) {
                                send("command.vote.skip.toomany")
                            } else {
                                if (Event.voteCooltime == 0) {
                                    Event.voteType = "skip"
                                    Event.voteWave = arg[1].toInt()
                                    Event.voteStarter = player
                                    Event.voting = true
                                    Event.voteCooltime = 120
                                    sendStart("command.vote.skip.start", arg[1])
                                } else {
                                    send("command.vote.cooltime")
                                }
                            }
                        }
                    }

                    // vote back <reason>
                    "back" -> {
                        if (!Permission.check(player, "vote.back")) return
                        if (!saveDirectory.child("rollback.msav").exists()) {
                            player.sendMessage("command.vote.back.no.file")
                            return
                        }
                        if (arg.size == 1) {
                            send("command.vote.no.reason")
                            return
                        }
                        Event.voteType = "back"
                        Event.voteReason = arg[1]
                        Event.voteStarter = player
                        Event.voting = true
                        sendStart("command.vote.back.start", arg[1])
                    }

                    // vote random
                    "random" -> {
                        if (!Permission.check(player, "vote.random")) return
                        if (Event.voteCooltime == 0) {
                            Event.voteType = "random"
                            Event.voteStarter = player
                            Event.voting = true
                            Event.voteCooltime = 360
                            sendStart("command.vote.random.start")
                        } else {
                            err("command.vote.cooltime")
                        }
                    }

                    else -> {
                        send("command.vote.wrong")
                    }
                }
            }
        }

        fun votekick() {
            if (arg[0].contains("#")) {
                val f = Groups.player.find { p -> p.id() == arg[0].substring(1).toInt() }

                if (Permission.check(f, "kick.admin")) {
                    err("command.vote.kick.target.admin")
                } else {
                    val array = arrayOf("kick", f.name, "Kick")
                    vote(player, array)
                }
            }
        }

        private fun selectTeam(arg : String) : Team {
            return if ("derelict".first() == arg.first()) {
                Team.derelict
            } else if ("sharded".first() == arg.first()) {
                Team.sharded
            } else if ("crux".first() == arg.first()) {
                Team.crux
            } else if ("green".first() == arg.first()) {
                Team.green
            } else if ("malis".first() == arg.first()) {
                Team.malis
            } else if ("blue".first() == arg.first()) {
                Team.blue
            } else if ("derelict".contains(arg[0], true)) {
                Team.derelict
            } else if ("sharded".contains(arg[0], true)) {
                Team.sharded
            } else if ("crux".contains(arg[0], true)) {
                Team.crux
            } else if ("green".contains(arg[0], true)) {
                Team.green
            } else if ("malis".contains(arg[0], true)) {
                Team.malis
            } else if ("blue".contains(arg[0], true)) {
                Team.blue
            } else {
                state.rules.defaultTeam
            }
        }
    }

    class Server(val arg : Array<String>) {
        private inner class StringUtils {
            // Source from https://howtodoinjava.com/java/string/escape-html-encode-string/
            private val htmlEncodeChars = ObjectMap<Char, String>()
            fun encodeHtml(source : String?) : String? {
                return encode(source)
            }

            private fun encode(source : String?) : String? {
                if (null == source) return null
                var encode : StringBuilder? = null
                val encodeArray = source.toCharArray()
                var match = -1
                var difference : Int
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

        fun genDocs() {
            if (System.getenv("DEBUG_KEY") != null) {
                val server = "## Server commands\n| Command | Parameter | Description |\n|:---|:---|:--- |\n"
                val client = "## Client commands\n| Command | Parameter | Description |\n|:---|:---|:--- |\n"
                val time = "README.md Generated time: ${DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now())}"

                val result = StringBuilder()

                clientCommands.commandList.forEach {
                    val temp = "| ${it.text} | ${StringUtils().encodeHtml(it.paramText)} | ${it.description} |\n"
                    result.append(temp)
                }

                val tmp = "$client$result\n\n"

                result.clear()
                serverCommands.commandList.forEach {
                    val temp = "| ${it.text} | ${StringUtils().encodeHtml(it.paramText)} | ${it.description} |\n"
                    result.append(temp)
                }

                println("$tmp$server$result\n\n\n$time")
            }
        }

        fun reload() {
            try {
                Permission.load()
                Log.info(Bundle()["config.permission.updated"])
                Config.load()
                Log.info(Bundle()["config.reloaded"])
            } catch (e : Exception) {
                e.printStackTrace()
            }
        }

        fun debug() {
            when (arg[0]) {
                "info" -> {
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
                    status: ${PluginData.status}
                    
                    == DB class
                    """.trimIndent())
                    database.players.forEach { println(it.toString()) }
                }

                "debug" -> {
                    if (arg.isNotEmpty()) {
                        if (arg[0].toBoolean()) {
                            Core.settings.put("debugMode", true)
                        } else {
                            Core.settings.put("debugMode", false)
                        }
                    }
                }
            }
        }

        fun setperm() {
            val target = findPlayers(arg[0])
            if (target != null) {
                val data = findPlayerData(target.uuid())
                if (data != null) {
                    data.permission = arg[1]
                    database.queue(data)
                } else {
                    Log.info(stripColors(bundle["player.not.registered"]))
                }
            } else {
                Log.info(stripColors(bundle["player.not.found"]))
            }
        }

        fun tempban() {
            val other = findPlayers(arg[0])

            if (other == null) {
                Log.info(bundle["player.not.found"])
            } else {
                val d = findPlayerData(other.uuid())
                if (d == null) {
                    Log.info(stripColors(bundle["command.tempban.not.registered"]))
                    netServer.admins.banPlayer(other.uuid())
                    Call.kick(other.con(), Packets.KickReason.banned)
                } else {
                    val time = LocalDateTime.now()
                    val minute = arg[1].toLongOrNull()
                    val reason = arg[2]

                    if (minute != null) {
                        d.banTime = time.plusMinutes(minute.toLong()).toString()
                        Call.kick(other.con(), reason)
                        Events.fire(PlayerTempBanned(d.name, player.plainName(), time.plusMinutes(minute.toLong()).format(DateTimeFormatter.ofPattern("YYYY-mm-dd HH:mm:ss"))))
                    } else {
                        Log.info(stripColors(bundle["command.tempban.not.number"]))
                    }
                }
            }
        }

        private fun stripColors(string : String) : String {
            return string.replace(" *\\(.+?\\)".toRegex(), "")
        }
    }

    object Exp {
        private const val baseXP = 1000
        private const val exponent = 1.12
        private fun calcXpForLevel(level : Int) : Double {
            return baseXP + baseXP * level.toDouble().pow(exponent)
        }

        fun calculateFullTargetXp(level : Int) : Double {
            var requiredXP = 0.0
            for (i in 0..level) requiredXP += calcXpForLevel(i)
            return requiredXP
        }

        private fun calculateLevel(xp : Double) : Int {
            var level = 0
            var maxXp = calcXpForLevel(0)
            do maxXp += calcXpForLevel(++level) while (maxXp < xp)
            return level
        }

        operator fun get(target : DB.PlayerData) : String {
            val currentlevel = target.level
            val max = calculateFullTargetXp(currentlevel).toInt()
            val xp = target.exp
            val levelXp = max - xp
            val level = calculateLevel(xp.toDouble())
            target.level = level
            return "$xp (${floor(levelXp.toDouble()).toInt()}) / ${floor(max.toDouble()).toInt()}"
        }
    }

    object Discord: ListenerAdapter() {
        val pin : ObjectMap<String, Int> = ObjectMap()
        var jda : JDA? = null

        fun start() {
            if (Config.botToken.isNotEmpty()) {
                jda = JDABuilder.createDefault(Config.botToken).enableIntents(GatewayIntent.MESSAGE_CONTENT).build()
                jda!!.awaitReady()
                jda!!.addEventListener(this)
            }
        }

        override fun onMessageReceived(event : MessageReceivedEvent) {
            if (event.channel.id == Config.channelToken && !event.author.isBot) {
                if (event.message.contentStripped.toIntOrNull() != null) {
                    if (pin.findKey(event.message.contentStripped, true) != null) {
                        val data = database[pin.findKey(event.message.contentStripped.toInt(), true)]
                        data?.discord = event.author.id
                        pin.remove(pin.findKey(event.message.contentStripped.toInt(), true))
                    }
                } else if (Core.settings.getInt("port") == port) {
                    with(event.message.contentStripped) {
                        when {
                            equals("!help", true) -> {
                                val message = """
                                    ``!help`` ${Bundle()["event.discord.help.help"]}
                                    ``!ping`` ${Bundle()["event.discord.help.ping"]}
                                """.trimIndent()
                                event.message.reply(message).queue {
                                    sleep(7000)
                                    it.delete()
                                }
                            }

                            equals("!ping", true) -> {
                                val start = System.currentTimeMillis()
                                event.message.reply("pong!").queue { ping ->
                                    val end = System.currentTimeMillis()
                                    ping.editMessage("pong! (" + (end - start) + "ms).")
                                    sleep(5000)
                                    ping.delete()
                                }
                            }

                            startsWith("!auth", true) -> {
                                val arg = event.message.contentStripped.replace("!auth ", "").split(" ")
                                if (arg.size == 1) {
                                    try {
                                        if (database.getAll().find { it.discord != null && it.discord == event.message.author.id } == null) {
                                            var data : DB.PlayerData? = null
                                            for (a in pin) {
                                                if (a.value == arg[0].toInt()) {
                                                    data = database.getAll().find { b -> b.name == a.key }
                                                }
                                            }

                                            if (data != null) {
                                                data.discord = event.message.author.id
                                                event.message.reply(Bundle()["event.discord.auth.success"]).queue {
                                                    sleep(5000)
                                                    it.delete()
                                                }
                                                pin.removeAll { a -> a.value == arg[0].toInt() }
                                            } else {
                                                event.message.reply(Bundle()["event.discord.auth.not-found"]).queue {
                                                    sleep(5000)
                                                    it.delete()
                                                }
                                            }
                                        } else {
                                            event.message.reply(Bundle()["event.discord.auth.already-exists"]).queue {
                                                sleep(5000)
                                                it.delete()
                                            }
                                        }
                                    } catch (e : Exception) {
                                        event.message.reply(Bundle()["event.discord.auth.pin.invalid"]).queue {
                                            sleep(5000)
                                            it.delete()
                                        }
                                    }
                                } else {
                                    event.message.reply(Bundle()["event.discord.auth.usage"]).queue {
                                        sleep(5000)
                                        it.delete()
                                    }
                                }
                            }

                            else -> {}
                        }
                    }
                }
            }
        }

        fun queue(player : Playerc) : Int {
            val number = (Math.random() * 9999).toInt()
            pin.put(player.uuid(), number)
            return number
        }

        fun shutdownNow() {
            jda?.shutdown()
        }
    }
}