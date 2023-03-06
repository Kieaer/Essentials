package essentials

import arc.Core
import arc.Events
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
import com.mewna.catnip.Catnip
import com.mewna.catnip.entity.message.Message
import com.mewna.catnip.shard.DiscordEvent
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
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Playerc
import mindustry.gen.Unit
import mindustry.maps.Map
import mindustry.net.Administration
import mindustry.net.Packets
import mindustry.type.UnitType
import mindustry.world.Tile
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.SocketException
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
        if(isClient) {
            handler.removeCommand("help")
            if(Config.vote) {
                handler.removeCommand("vote")
                handler.register("vote", "<kick/map/gg/skip/back/random> [player/amount/world_name] [reason]", "Start voting") { a, p : Playerc -> Client(a, p).vote(p, a) }

                handler.removeCommand("votekick")
                if(Config.votekick) {
                    handler.register("votekick", "<name...>", "Start kick voting") { a, p : Playerc -> Client(a, p).votekick() }
                }
            }

            handler.register("broadcast", "<text...>", "Broadcast message to all servers") { a, p : Playerc -> Client(a, p).broadcast() }
            handler.register("changename", "<new_name> [player]", "Change player name.") { a, p : Playerc -> Client(a, p).changename() }
            handler.register("changepw", "<new_password> <password_repeat>", "Change account password.") { a, p : Playerc -> Client(a, p).changepw() }
            handler.register("chat", "<on/off>", "Mute all players without admins.") { a, p : Playerc -> Client(a, p).chat() }
            handler.register("chars", "<text...>", "Make pixel texts") { a, p : Playerc -> Client(a, p).chars(null) }
            handler.register("color", "Enable color nickname") { a, p : Playerc -> Client(a, p).color() }
            handler.register("discord", "Authenticate your Discord account to the server.") { a, p : Playerc -> Client(a, p).discord() }
            handler.register("effect", "<level> [color]", "Set the effect and color for each level.") { a, p : Playerc -> Client(a, p).effect() }
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
            handler.register("sync", "Match ban list with all connected servers.") { a -> Server(a).sync() }
            handler.register("tempban", "<player> <time> [reason]", "Ban the player for a certain period of time.") { a -> Server(a).tempban() }
            serverCommands = handler
        }
    }

    class Client(val arg : Array<String>, val player : Playerc) {
        private var bundle = Bundle()
        private var data = DB.PlayerData()

        init {
            val d = findPlayerData(player.uuid())
            if(d != null) data = d else DB.PlayerData()
            bundle = Bundle(data.languageTag)
        }

        fun send(msg : String, vararg parameters : Any) {
            player.sendMessage(MessageFormat.format(bundle.resource.getString(msg), *parameters))
        }

        fun changename() {
            if(!Permission.check(player, "changename")) return
            if(arg.size != 1) {
                val target = findPlayers(arg[1])
                if(target != null) {
                    val data = findPlayerData(target.uuid())
                    if(data != null) {
                        data.name = arg[0]
                        target.name(arg[0])
                        database.queue(data)
                    } else {
                        send("player.not.registered")
                    }
                } else {
                    send("player.not.found")
                }
            } else {
                data.name = arg[0]
                player.name(arg[0])
                database.queue(data)
            }
            send("command.changename.apply")
        }

        fun changepw() {
            if(!Permission.check(player, "changepw")) return
            if(arg.size != 2) {
                send("command.changepw.empty")
                return
            }

            if(arg[0] != arg[1]) {
                send("command.changepw.same")
                return
            }

            val password = BCrypt.hashpw(arg[0], BCrypt.gensalt())
            data.pw = password
            database.queue(data)
            send("command.changepw.apply")
        }

        fun chat() {
            if(!Permission.check(player, "chat")) return
            Event.isGlobalMute = arg[0].equals("on", true)
            if(Event.isGlobalMute) {
                send("command.chat.off")
            } else {
                send("command.chat.on")
            }
        }

        fun chars(tile : Tile?) {
            if(!Permission.check(player, "chars")) return
            if(world != null) {
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
                for(i in texts) {
                    val pos = Seq<IntArray>()
                    if(!letters.containsKey(i.uppercaseChar().toString())) continue
                    val target = letters[i.uppercaseChar().toString()]
                    var xv = 0
                    var yv = 0
                    when(target.size) {
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
                    for(y in 0 until yv) {
                        for(x in 0 until xv) {
                            pos.add(intArrayOf(y, -x))
                        }
                    }
                    for(a in 0 until pos.size) {
                        if(t != null) {
                            val tar = world.tile(t.x + pos[a][0], t.y + pos[a][1])
                            if(target[a] == 1) {
                                Call.setTile(tar, Blocks.scrapWall, Team.sharded, 0)
                            } else if(tar != null) {
                                Call.setTile(tar, tar.block(), Team.sharded, 0)
                            }
                        }
                    }
                    val left : Int = when(target.size) {
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

        fun color() {
            if(!Permission.check(player, "color")) return
            data.colornick = !data.colornick
        }

        fun broadcast() {
            if(!Permission.check(player, "broadcast")) return
            if(Main.connectType) {
                for(a in Trigger.clients) {
                    val b = BufferedWriter(OutputStreamWriter(a.getOutputStream()))
                    try {
                        b.write("message")
                        b.newLine()
                        b.flush()
                        b.write(arg[0])
                        b.newLine()
                        b.flush()
                    } catch(e : SocketException) {
                        a.close()
                        Trigger.clients.remove(a)
                    }
                }
            } else {
                Trigger.Client.message(arg[0])
            }
        }

        fun discord() {
            if(!Permission.check(player, "discord")) return
            Call.openURI(player.con(), Config.discordURL)
            if(Config.authType == Config.AuthType.Discord) {
                if(!data.status.containsKey("discord")) {
                    val number = if(Discord.pin.containsKey(player.uuid())) {
                        Discord.pin.get(player.uuid())
                    } else {
                        Discord.queue(player)
                    }
                    send("command.discord.pin", number)
                } else {
                    send("command.discord.already")
                }
            }
        }

        fun effect() {
            if(!Permission.check(player, "effect")) return
            if(arg[0].toIntOrNull() != null) {
                if(arg[0].toInt() <= data.level) {
                    data.status.put("effectLevel", arg[0])
                    if(arg.size == 2) {
                        try {
                            if(Colors.get(arg[1]) == null) {
                                Color.valueOf(arg[1])
                            }

                            data.status.put("effectColor", arg[1])
                            database.queue(data)
                        } catch(_ : IllegalArgumentException) {
                            send("command.effect.no.color")
                        } catch(_ : StringIndexOutOfBoundsException) {
                            send("command.effect.no.color")
                        }
                    }
                } else {
                    send("command.effect.level")
                }
            } else {
                send("command.effect.invalid")
            }
        }

        fun exp() {
            if(!Permission.check(player, "exp")) return
            when(arg[0]) {
                "set" -> {
                    if(!Permission.check(player, "exp.admin")) return
                    if(arg[1].toIntOrNull() != null) {
                        if(arg.size == 3) {
                            val target = findPlayers(arg[2])
                            if(target != null) {
                                val data = findPlayerData(target.uuid())
                                if(data != null) {
                                    val previous = data.exp
                                    data.exp = arg[1].toInt()
                                    send("command.exp.result", previous, data.exp)
                                }
                            } else {
                                val p = findPlayersByName(arg[2])
                                if(p != null) {
                                    val a = database[p.id]
                                    if(a != null) {
                                        val previous = a.exp
                                        a.exp = arg[1].toInt()
                                        database.queue(a)
                                        send("command.exp.result", previous, a.exp)
                                    } else {
                                        send("player.not.registered")
                                    }
                                } else {
                                    send("player.not.found")
                                }
                            }
                        } else {
                            val previous = data.exp
                            data.exp = arg[1].toInt()
                            send("command.exp.result", previous, data.exp)
                        }
                    } else {
                        send("command.exp.invalid")
                    }
                }

                "hide" -> {
                    if(!Permission.check(player, "exp.admin")) return
                    if(arg.size == 2) {
                        val target = findPlayers(arg[1])
                        if(target != null) {
                            val data = findPlayerData(target.uuid())
                            if(data != null) {
                                if(data.status.containsKey("hideRanking")) {
                                    data.status.remove("hideRanking")
                                    send("command.exp.ranking.unhide")
                                } else {
                                    data.status.put("hideRanking", "")
                                    send("command.exp.ranking.hide")
                                }
                            }
                        } else {
                            send("player.not.found")
                        }
                    } else {
                        if(data.status.containsKey("hideRanking")) {
                            data.status.remove("hideRanking")
                            send("command.exp.ranking.unhide")
                        } else {
                            data.status.put("hideRanking", "")
                            send("command.exp.ranking.hide")
                        }
                    }
                }

                "add" -> {
                    if(!Permission.check(player, "exp.admin")) return
                    if(arg[1].toIntOrNull() != null) {
                        if(arg.size == 3) {
                            val target = findPlayers(arg[2])
                            if(target != null) {
                                val data = findPlayerData(target.uuid())
                                if(data != null) {
                                    val previous = data.exp
                                    data.exp += arg[1].toInt()
                                    send("command.exp.result", previous, data.exp)
                                }
                            } else {
                                val p = findPlayersByName(arg[2])
                                if(p != null) {
                                    val a = database[p.id]
                                    if(a != null) {
                                        val previous = a.exp
                                        a.exp += arg[1].toInt()
                                        database.queue(a)
                                        send("command.exp.result", previous, a.exp)
                                    } else {
                                        send("player.not.registered")
                                    }
                                } else {
                                    send("player.not.found")
                                }
                            }
                        } else {
                            val previous = data.exp
                            data.exp += arg[1].toInt()
                            send("command.exp.result", previous, data.exp)
                        }
                    } else {
                        send("command.exp.invalid")
                    }
                }

                "remove" -> {
                    if(!Permission.check(player, "exp.admin")) return
                    if(arg[1].toIntOrNull() != null) {
                        if(arg.size == 3) {
                            val target = findPlayers(arg[2])
                            if(target != null) {
                                val data = findPlayerData(target.uuid())
                                if(data != null) {
                                    val previous = data.exp
                                    data.exp -= arg[1].toInt()
                                    send("command.exp.result", previous, data.exp)
                                }
                            } else {
                                val p = findPlayersByName(arg[2])
                                if(p != null) {
                                    val a = database[p.id]
                                    if(a != null) {
                                        val previous = a.exp
                                        a.exp -= arg[1].toInt()
                                        database.queue(a)
                                        send("command.exp.result", previous, a.exp)
                                    } else {
                                        send("player.not.registered")
                                    }
                                } else {
                                    send("player.not.found")
                                }
                            }
                        } else {
                            val previous = data.exp
                            data.exp -= arg[1].toInt()
                            send("command.exp.result", previous, data.exp)
                        }
                    } else {
                        send("command.exp.invalid")
                    }
                }

                else -> {
                    send("command.exp.invalid.command")
                }
            }
        }

        fun fillitems() {
            if(!Permission.check(player, "fillitems")) return
            val team = selectTeam(arg[0])

            if(state.teams.cores(team).isEmpty) {
                send("command.fillitems.core.empty")
            }

            for(item in content.items()) {
                state.teams.cores(team).first().items[item] = state.teams.cores(team).first().storageCapacity
            }

            send("command.fillitems.core.filled")
        }

        fun freeze() {
            if(!Permission.check(player, "freeze")) return
            if(arg.isEmpty()) {
                send("player.not.found")
            } else {
                val target = findPlayers(arg[0])
                if(target != null) {
                    val data = findPlayerData(target.uuid())
                    if(data != null) {
                        if(data.status.containsKey("freeze")) {
                            data.status.remove("freeze")
                            send("command.freeze.undo", target.plainName())
                        } else {
                            data.status.put("freeze", "${target.x}/${target.y}")
                            send("command.freeze.done", target.plainName())
                        }
                    } else {
                        send("player.not.registered")
                    }
                } else {
                    send("player.not.found")
                }
            }
        }

        fun gg() {
            if(!Permission.check(player, "gg")) return
            if(arg.isEmpty()) {
                Events.fire(EventType.GameOverEvent(state.rules.waveTeam))
            } else {
                Events.fire(EventType.GameOverEvent(selectTeam(arg[0])))
            }

            player.unit().buildSpeedMultiplier(100f)
        }

        fun god() {
            if(!Permission.check(player, "god")) return

            player.unit().health(1.0E8f)
            send("command.god")
        }

        fun help() {
            if(!Permission.check(player, "help")) return
            if(arg.isNotEmpty() && !Strings.canParseInt(arg[0])) {
                try {
                    send("command.help.${arg[0]}")
                } catch(e : MissingResourceException) {
                    send("command.help.not.exists")
                }
                return
            }

            val temp = Seq<String>()
            for(a in 0 until netServer.clientCommands.commandList.size) {
                val command = netServer.clientCommands.commandList[a]
                if(Permission.check(player, command.text)) {
                    temp.add("[orange] /${command.text} [white]${command.paramText} [lightgray]- ${bundle["command.description." + command.text]}\n")
                }
            }
            val result = StringBuilder()
            val per = 8
            var page = if(arg.isNotEmpty()) abs(Strings.parseInt(arg[0])) else 1
            val pages = Mathf.ceil(temp.size.toFloat() / per)
            page--

            if(page >= pages || page < 0) {
                send("command.page.range", pages)
                return
            }

            result.append("[orange]-- ${bundle["command.page"]}[lightgray] ${page + 1}[gray]/[lightgray]${pages}[orange] --\n")
            for(a in per * page until (per * (page + 1)).coerceAtMost(temp.size)) {
                result.append(temp[a])
            }
            player.sendMessage(result.toString().substring(0, result.length - 1))
        }

        fun hub() {
            if(!Permission.check(player, "hub")) return
            val type = arg[0]
            val x = player.tileX()
            val y = player.tileY()
            val name = state.map.name()
            val size : Int?
            val clickable : Boolean?
            var ip = ""
            var port = 6567
            if(arg.size > 1) {
                if(arg[1].contains(":")) {
                    val address = arg[1].split(":").toTypedArray()
                    ip = address[0]

                    if(address[1].toIntOrNull() == null) {
                        send("command.hub.address.port.invalid")
                        return
                    }
                    port = address[1].toInt()
                } else {
                    ip = arg[1]
                }
            }

            when(type) {
                "set" -> {
                    if(PluginData["hubMode"] == null) {
                        PluginData.status.put("hubMode", state.map.name())
                        send("command.hub.mode.on")
                    } else {
                        PluginData.status.remove("hubMode")
                        send("command.hub.mode.off")
                    }
                }

                "zone" -> if(arg.size != 4) {
                    send("command.hub.zone.help")
                } else {
                    size = arg[2].toIntOrNull()
                    clickable = arg[3].toBooleanStrictOrNull()

                    if(size == null) {
                        send("command.hub.size.invalid")
                    } else if(clickable == null) {
                        send("command.hub.clickable.invalid")
                    }

                    if(size != null && clickable != null) {
                        PluginData.warpZones.add(PluginData.WarpZone(name, world.tile(x, y).pos(), world.tile(x + size, y + size).pos(), clickable, ip, port))
                        send("command.hub.zone.added", "$x:$y", ip, if(clickable) bundle["command.hub.zone.clickable"] else bundle["command.hub.zone.enter"])
                    }
                }

                "block" -> if(arg.size < 3) {
                    send("command.hub.block.parameter")
                } else {
                    val t : Tile = player.tileOn()
                    PluginData.warpBlocks.add(PluginData.WarpBlock(name, t.build.tileX(), t.build.tileY(), t.block().name, t.block().size, ip, port, arg[2]))
                    send("command.hub.block.added", "$x:$y", ip)
                }

                "count" -> {
                    if(arg.size < 2) {
                        send("command.hub.count.parameter")
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
                    PluginData.warpTotals.removeAll { true }
                    PluginData.warpCounts.removeAll { true }
                }

                else -> send("command.hub.help")
            }
            PluginData.save(false)
            PluginData.changed = true
        }

        fun hud() {
            if(!Permission.check(player, "hud")) return
            when(arg[0]) {
                "health" -> {
                    data.status.put("hud", "health")
                }

                else -> {
                    send("command.hud.not.found")
                }
            }
        }

        fun info() {
            if(!Permission.check(player, "info")) return
            if(arg.isNotEmpty()) {
                if(!Permission.check(player, "info.other")) return
                val target = findPlayers(arg[0])
                if(target != null) {
                    val other = findPlayerData(target.uuid())
                    if(other != null) {
                        val texts = """
                        ${bundle["info.name"]}: ${other.name}[white]
                        ${bundle["info.placecount"]}: ${other.placecount}
                        ${bundle["info.breakcount"]}: ${other.breakcount}
                        ${bundle["info.level"]}: ${other.level}
                        ${bundle["info.exp"]}: ${Exp[other]}
                        ${bundle["info.joindate"]}: ${Timestamp(other.joinDate).toLocalDateTime().format(DateTimeFormatter.ofPattern("yy-MM-dd HH:mm"))}
                        ${bundle["info.playtime"]}: ${bundle["command.info.time", (other.playtime / 60 / 60 / 24) % 365, (other.playtime / 60 / 24) % 24, (other.playtime / 60) % 60, (other.playtime) % 60]}
                        ${bundle["info.attackclear"]}: ${other.attackclear}
                        ${bundle["info.pvpwincount"]}: ${other.pvpwincount}
                        ${bundle["info.pvplosecount"]}: ${other.pvplosecount}
                        """.trimIndent()
                        Call.infoMessage(player.con(), texts)
                    } else {
                        send("player.not.found")
                    }
                } else {
                    val p = findPlayersByName(arg[0])
                    if(p != null) {
                        val a = database[p.id]
                        if(a != null) {
                            val texts = """
                                    ${bundle["info.name"]}: ${a.name}[white]
                                    ${bundle["info.placecount"]}: ${a.placecount}
                                    ${bundle["info.breakcount"]}: ${a.breakcount}
                                    ${bundle["info.level"]}: ${a.level}
                                    ${bundle["info.exp"]}: ${Exp[a]}
                                    ${bundle["info.joindate"]}: ${Timestamp(a.joinDate).toLocalDateTime().format(DateTimeFormatter.ofPattern("yy-MM-dd HH:mm"))}
                                    ${bundle["info.playtime"]}: ${bundle["command.info.time", (a.playtime / 60 / 60 / 24) % 365, (a.playtime / 60 / 24) % 24, (a.playtime / 60) % 60, (a.playtime) % 60]}
                                    ${bundle["info.attackclear"]}: ${a.attackclear}
                                    ${bundle["info.pvpwincount"]}: ${a.pvpwincount}
                                    ${bundle["info.pvplosecount"]}: ${a.pvplosecount}
                                    """.trimIndent()
                            Call.infoMessage(player.con(), texts)
                        } else {
                            send("player.not.registered")
                        }
                    } else {
                        send("player.not.found")
                    }
                }
            } else {
                val texts = """
                ${bundle["info.name"]}: ${data.name}[white]
                ${bundle["info.placecount"]}: ${data.placecount}
                ${bundle["info.breakcount"]}: ${data.breakcount}
                ${bundle["info.level"]}: ${data.level}
                ${bundle["info.exp"]}: ${Exp[data]}
                ${bundle["info.joindate"]}: ${Timestamp(data.joinDate).toLocalDateTime().format(DateTimeFormatter.ofPattern("yy-MM-dd HH:mm"))}
                ${bundle["info.playtime"]}: ${bundle["command.info.time", (data.playtime / 60 / 60 / 24) % 365, (data.playtime / 60 / 24) % 24, (data.playtime / 60) % 60, (data.playtime) % 60]}
                ${bundle["info.attackclear"]}: ${data.attackclear}
                ${bundle["info.pvpwincount"]}: ${data.pvpwincount}
                ${bundle["info.pvplosecount"]}: ${data.pvplosecount}
            """.trimIndent()
                Call.infoMessage(player.con(), texts)
            }
        }

        fun js() {
            if(!Permission.check(player, "js")) {
                Call.kick(player.con(), bundle["command.js.no.permission"])
                return
            }
            if(arg.isEmpty()) {
                send("command.js.invalid")
            } else {
                val output = mods.scripts.runConsole(arg[0])
                try {
                    val errorName = output?.substring(0, output.indexOf(' ') - 1)
                    Class.forName("org.mozilla.javascript.$errorName")
                    player.sendMessage("> [#ff341c]$output")
                } catch(e : Throwable) {
                    player.sendMessage("[scarlet]> $output")
                }
            }
        }

        fun kickall() {
            if(!Permission.check(player, "kickall")) return
            for(a in Groups.player) {
                if(!a.admin) Call.kick(a.con, Packets.KickReason.kick)
            }
        }

        fun kill() {
            if(!Permission.check(player, "kill")) return
            if(arg.isEmpty()) {
                player.unit().kill()
            } else {
                val other = findPlayers(arg[0])
                if(other == null) send("player.not.found") else other.unit().kill()
            }
        }

        fun killall() {
            if(!Permission.check(player, "killall")) return
            if(arg.isEmpty()) {
                for(a in Team.all.indices) {
                    Groups.unit.each { u : Unit -> if(player.team() == u.team) u.kill() }
                }
            } else {
                val team = selectTeam(arg[0])
                Groups.unit.each { u -> if(u.team == team) u.kill() }
            }

        }

        fun killunit() {
            if(!Permission.check(player, "killunit")) return
            val unit = content.units().find { unitType : UnitType -> unitType.name == arg[0] }
            if(unit != null) {
                if(arg.size > 2) {
                    if(arg[1].toIntOrNull() != null) {
                        if(arg.size == 3) {
                            val team = selectTeam(arg[2])
                            if(Groups.unit.size() < arg[1].toInt() || arg[1].toInt() == 0) {
                                Groups.unit.forEach { if(it == unit && it.team == team) it.kill() }
                            } else {
                                var count = 0
                                Groups.unit.forEach {
                                    if(it == unit && it.team == team && count != arg[1].toInt()) {
                                        it.kill()
                                        count++
                                    }
                                }
                            }
                        } else {
                            if(Groups.unit.size() < arg[1].toInt() || arg[1].toInt() == 0) {
                                Groups.unit.forEach { if(it == unit && it.team == player.team()) it.kill() }
                            } else {
                                var count = 0
                                Groups.unit.forEach {
                                    if(it == unit && it.team == player.team() && count != arg[1].toInt()) {
                                        it.kill()
                                        count++
                                    }
                                }
                            }
                        }
                    } else {
                        send("command.killunit.invalid.number")
                    }
                } else {
                    Groups.unit.forEach { if(it == unit && it.team == player.team()) it.kill() }
                }
            } else {
                send("command.killunit.not.found")
            }
        }

        fun lang() {
            if(!Permission.check(player, "lang")) return
            if(arg.isEmpty()) {
                player.sendMessage("command.language.empty")
                return
            }
            data.languageTag = arg[0]
            database.queue(data)
            send("command.language.set", Locale(arg[0]).language)
            player.sendMessage(Bundle(arg[0])["command.language.preview", Locale(arg[0]).toLanguageTag()])
        }

        fun log() {
            if(!Permission.check(player, "log")) return
            if(data.status.containsKey("log")) {
                data.status.remove("log")
                send("command.log.disabled")
            } else {
                data.status.put("log", "true")
                send("command.log.enabled")
            }
        }

        fun login() {
            if(!Permission.check(player, "login")) return
            if(arg[0] == arg[1]) {
                player.sendMessage("command.login.same.password")
                return
            }

            val result = database.search(arg[0], arg[1])
            if(result != null) {
                if(result.id == result.pw) {
                    Bundle(player.locale())["command.login.default.password"]
                } else {
                    Trigger.loadPlayer(player, result)
                }
            } else {
                send("command.login.not.found")
            }
        }

        fun maps() {
            if(!Permission.check(player, "maps")) return
            val list = maps.all().sortedBy { a -> a.name() }
            val arr = ObjectMap<Map, Int>()
            for((order, a) in list.withIndex()) {
                arr.put(a, order)
            }
            val build = StringBuilder()

            val page = if(arg.isNotEmpty() && arg[0].toIntOrNull() != null) arg[0].toInt() else 0

            val buffer = Mathf.ceil(list.size.toFloat() / 6)
            val pages = if(buffer > 1.0) buffer - 1 else 0

            if(page > pages || page < 0) {
                send("command.page.range", pages)
                return
            }
            build.append("[green]==[white] ${bundle["command.page.server"]} $page/$pages [green]==[white]\n")
            for(a in 6 * page until (6 * (page + 1)).coerceAtMost(list.size)) {
                build.append("[gray]$a[] ${list[a].name()}[white]\n")
            }

            player.sendMessage(build.toString())
        }

        fun me() {
            if(!Permission.check(player, "me") || data.mute) return

            if(Config.chatBlacklist) {
                val file = root.child("chat_blacklist.txt").readString("UTF-8").split("\r\n")
                if(file.isNotEmpty()) {
                    for(a in file) {
                        if(Config.chatBlacklistRegex) {
                            if(arg[0].contains(Regex(a))) {
                                player.sendMessage(Bundle(findPlayerData(player.uuid())!!.languageTag)["event.chat.blacklisted"])
                                return
                            }
                        } else {
                            if(arg[0].contains(a)) {
                                player.sendMessage(Bundle(findPlayerData(player.uuid())!!.languageTag)["event.chat.blacklisted"])
                                return
                            }
                        }
                    }
                    Call.sendMessage("[brown]== [sky]${player.plainName()}[white] - [tan]${arg[0]}")
                }
            }
        }

        fun meme() {
            if(!Permission.check(player, "meme")) return
            when(arg[0]) {
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
                    if(data.status.containsKey("router")) {
                        data.status.remove("router")
                    } else {
                        Thread {
                            data.status.put("router", "true")
                            while(!player.isNull) {
                                for(d in loop) {
                                    player.name(d)
                                    sleep(500)
                                }
                                if(!data.status.containsKey("router")) break
                                sleep(5000)
                                for(i in loop.indices.reversed()) {
                                    player.name(loop[i])
                                    sleep(500)
                                }
                                for(d in zero) {
                                    player.name(d)
                                    sleep(500)
                                }
                            }
                        }.start()
                    }
                }
            }
        }

        fun motd() {
            if(!Permission.check(player, "motd")) return
            val motd = if(root.child("motd/${data.languageTag}.txt").exists()) {
                root.child("motd/${data.languageTag}.txt").readString()
            } else {
                val file = root.child("motd/en.txt")
                if(file.exists()) file.readString() else ""
            }
            val count = motd.split("\r\n|\r|\n").toTypedArray().size
            if(count > 10) Call.infoMessage(player.con(), motd) else player.sendMessage(motd)
        }

        fun mute() {
            if(!Permission.check(player, "mute")) return
            val other = findPlayers(arg[0])
            if(other != null) {
                val target = findPlayerData(other.uuid())
                if(target != null) {
                    target.mute = true
                    database.queue(target)
                    send("command.mute", target.name)
                } else {
                    send("player.not.found")
                }
            } else {
                val p = findPlayersByName(arg[0])
                if(p != null) {
                    val a = database[p.id]
                    if(a != null) {
                        a.mute = true
                        database.queue(a)
                        send("command.mute", a.name)
                    } else {
                        send("player.not.registered")
                    }
                } else {
                    send("player.not.found")
                }
            }
        }

        fun pause() {
            if(!Permission.check(player, "pause")) return
            if(state.isPaused) {
                state.set(GameState.State.playing)
                send("command.pause.unpaused")
            } else {
                state.set(GameState.State.paused)
                send("command.pause.paused")
            }
        }

        fun players() {
            if(!Permission.check(player, "players")) return
            val message = StringBuilder()
            val page = if(arg.isNotEmpty() && arg[0].toIntOrNull() != null) arg[0].toInt() else 0

            val buffer = Mathf.ceil(database.players.size.toFloat() / 6)
            val pages = if(buffer > 1.0) buffer - 1 else 0

            if(pages < page) {
                send("command.page.range", pages)
            } else {
                message.append("[green]==[white] ${bundle["command.page.players"]} [orange]$page[]/[orange]$pages\n")
                for(a in 6 * page until (6 * (page + 1)).coerceAtMost(database.players.size)) {
                    val name = database.players.get(a).name
                    val id = database.players.get(a).entityid
                    message.append("[gray]$id [white]$name\n")
                }
                player.sendMessage(message.toString().dropLast(1))
            }
        }

        fun pm() {
            if(!Permission.check(player, "pm") || data.mute) return
            val target = findPlayers(arg[0])
            if(target == null) {
                send("player.not.found")
            } else if(arg.size > 1) {
                player.sendMessage("[green][PM] ${target.plainName()}[yellow] => [white] ${arg[1]}")
                target.sendMessage("[blue][PM] [gray][${data.entityid}][]${player.plainName()}[yellow] => [white] ${arg[1]}")
                for(a in database.players) {
                    if(Permission.check(a.player, "pm.other") && a.uuid != player.uuid()) {
                        a.player.sendMessage("[sky]${player.plainName()}[][yellow] => [pink]${target.plainName()} [white]: ${arg[1]}")
                    }
                }
            } else {
                send("command.pm.message")
            }
        }

        fun ranking() {
            if(!Permission.check(player, "ranking")) return
            if(PluginData.isRankingWorking) {
                player.sendMessage(bundle["command.ranking.working"])
                return
            }
            Main.daemon.submit(Thread {
                PluginData.isRankingWorking = true
                try {
                    val firstMessage = when(arg[0].lowercase()) {
                        "time" -> "command.ranking.time"
                        "exp" -> "command.ranking.exp"
                        "attack" -> "command.ranking.attack"
                        "place" -> "command.ranking.place"
                        "break" -> "command.ranking.break"
                        "pvp" -> "command.ranking.pvp"
                        else -> null
                    }

                    if(firstMessage == null) {
                        send("command.ranking.wrong")
                        return@Thread
                    }

                    val all = database.getAll()
                    val time = mutableMapOf<ArrayMap<String, String>, Long>()
                    val exp = mutableMapOf<ArrayMap<String, String>, Int>()
                    val attack = mutableMapOf<ArrayMap<String, String>, Int>()
                    val placeBlock = mutableMapOf<ArrayMap<String, String>, Int>()
                    val breakBlock = mutableMapOf<ArrayMap<String, String>, Int>()
                    val pvp = mutableMapOf<ArrayMap<String, String>, ArrayMap<Int, Int>>()

                    for(a in all) {
                        if(!a.status.containsKey("hideRanking") && !netServer.admins.banned.contains { b -> b.id == a.uuid }) {
                            val info = ArrayMap<String, String>()
                            val pvpcount = ArrayMap<Int, Int>()
                            info.put(a.name, a.uuid)
                            pvpcount.put(a.pvpwincount, a.pvplosecount)

                            time[info] = a.playtime
                            exp[info] = a.exp
                            attack[info] = a.attackclear
                            placeBlock[info] = a.placecount
                            breakBlock[info] = a.breakcount
                            pvp[info] = pvpcount
                        }
                    }

                    val d = when(arg[0].lowercase()) {
                        "time" -> time.toList().sortedWith(compareBy { -it.second })
                        "exp" -> exp.toList().sortedWith(compareBy { -it.second })
                        "attack" -> attack.toList().sortedWith(compareBy { -it.second })
                        "place" -> placeBlock.toList().sortedWith(compareBy { -it.second })
                        "break" -> breakBlock.toList().sortedWith(compareBy { -it.second })
                        "pvp" -> pvp.toList().sortedWith(compareBy { -it.second.firstKey() })
                        else -> return@Thread
                    }

                    val string = StringBuilder()
                    val per = 8
                    var page = if(arg.size == 2) abs(Strings.parseInt(arg[1])) else 1
                    val pages = Mathf.ceil(d.size.toFloat() / per)
                    page--

                    if(page >= pages || page < 0) {
                        Core.app.post { send("command.page.range", pages) }
                        return@Thread
                    }
                    string.append(bundle[firstMessage, page + 1, pages] + "\n")

                    for(a in per * page until (per * (page + 1)).coerceAtMost(d.size)) {
                        if(d[a].second is ArrayMap<*, *>) {
                            val rank = d[a].second as ArrayMap<*, *>
                            val rate = round((rank.firstKey().toString().toFloat() / (rank.firstKey().toString().toFloat() + rank.firstValue().toString().toFloat())) * 100)
                            string.append("[white]$a[] ${d[a].first.firstKey()}[white] [yellow]-[] [green]${rank.firstKey()}${bundle["command.ranking.pvp.win"]}[] / [scarlet]${rank.firstValue()}${bundle["command.ranking.pvp.lose"]}[] ($rate%)\n")
                        } else {
                            val t = d[a].second.toString().toLong()
                            val timeMessage = bundle["command.info.time", (t / 60 / 60) / 24, (t / 60 / 60) % 24, (t / 60) % 60, t % 60]
                            string.append("[white]${a + 1}[] ${d[a].first.firstKey()}[white] [yellow]-[] ${if(arg[0].lowercase() == "time") timeMessage else d[a].second}\n")
                        }
                    }
                    string.substring(0, string.length - 1)
                    string.append("[purple]=======================================[]\n")
                    for(a in d.indices) {
                        if(d[a].first.firstValue() == player.uuid()) {
                            if(d[a].second is ArrayMap<*, *>) {
                                val rank = d[a].second as ArrayMap<*, *>
                                val rate = round((rank.firstKey().toString().toFloat() / (rank.firstKey().toString().toFloat() + rank.firstValue().toString().toFloat())) * 100)
                                string.append("[white]${a + 1}[] ${d[a].first.firstKey()}[white] [yellow]-[] [green]${rank.firstKey()}${bundle["command.ranking.pvp.win"]}[] / [scarlet]${rank.firstValue()}${bundle["command.ranking.pvp.lose"]}[] ($rate%)")
                            } else {
                                val t = d[a].second.toString().toLong()
                                val timeMessage = bundle["command.info.time", (t / 60 / 60) / 24, (t / 60 / 60) % 24, (t / 60) % 60, t % 60]
                                string.append("[white]${a + 1}[] ${d[a].first.firstKey()}[white] [yellow]-[] ${if(arg[0].lowercase() == "time") timeMessage else d[a].second}")
                            }
                        }
                    }

                    Core.app.post { player.sendMessage(string.toString()) }
                } catch(e : Exception) {
                    player.sendMessage("플러그인 오류! 서버장에게 보고하세요.")
                    e.printStackTrace()
                }
                PluginData.isRankingWorking = false
            })
        }

        fun register() {
            if(!Permission.check(player, "register")) return
            if(Config.authType != Config.AuthType.None) {
                if(arg.size != 3) {
                    send("command.reg.usage")
                } else if(arg[1] != arg[2]) {
                    send("command.reg.incorrect")
                } else {
                    if(transaction { DB.Player.select { DB.Player.accountid.eq(arg[0]) }.firstOrNull() } == null) {
                        Trigger.createPlayer(player, arg[0], arg[1])
                        Log.info(Bundle()["log.data_created", player.plainName()])
                    } else {
                        player.sendMessage("command.reg.exists")
                    }
                }
            } else {
                player.sendMessage("[scarlet]This server doesn't use authentication.")
            }
        }

        fun report() {
            if(!Permission.check(player, "report")) return
            val target = findPlayers(arg[0])
            if(target != null) {
                val reason = arg[1]
                val infos = netServer.admins.findByName(target.uuid()).first()
                val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                val text = Bundle()["command.report.texts", target.plainName(), player.plainName(), reason, infos.lastName, infos.names, infos.id, infos.lastIP, infos.ips]

                Event.log(Event.LogType.Report, date + text, target.plainName())
                Log.info(Bundle()["command.report.received", player.plainName(), target.plainName(), reason])
                send("command.report.done", target.plainName())
            } else {
                send("player.not.found")
            }
        }

        fun rollback() {
            // todo 메세지 내용이 되돌려지지 않음
            // todo 일부 기록이 복구되지 않음
            if(!Permission.check(player, "rollback")) return

            for(a in worldHistory) {
                val buf = Seq<Event.TileLog>()
                if(a.player.contains(arg[0])) {
                    for(b in worldHistory) {
                        if(b.x == a.x && b.y == a.y) {
                            buf.add(b)
                        }
                    }

                    val last = buf.last()
                    if(last.action == "place") {
                        Call.setTile(world.tile(last.x.toInt(), last.y.toInt()), Blocks.air, state.rules.defaultTeam, 0)
                    } else if(last.action == "break") {
                        Call.setTile(world.tile(last.x.toInt(), last.y.toInt()), content.block(last.tile), last.team, last.rotate)/*println(content.block(last.tile).name)
                        if (world.tile(last.x.toInt(), last.y.toInt()).block() == Blocks.message || world.tile(last.x.toInt(), last.y.toInt()).block() == Blocks.reinforcedMessage || world.tile(last.x.toInt(), last.y.toInt()).block() == Blocks.worldMessage) {
                            val t = world.tile(last.x.toInt(), last.y.toInt()).block() as MessageBlock
                            t.MessageBuild().message = StringBuilder().append(last.other)
                        }*/
                    }
                }
            }
            worldHistory.removeAll { a -> a.player.contains(arg[0]) }
        }

        fun search() {
            if(!Permission.check(player, "search")) return
            if(arg[0].isEmpty()) {
                player.sendMessage("player.not.found")
                return
            }

            val result = ArrayList<DB.PlayerData?>()
            val data = findPlayers(arg[0])

            if(data == null) {
                val e = netServer.admins.findByName(arg[0])
                if(e.size > 0) {
                    for(info : Administration.PlayerInfo in e) {
                        result.add(database[info.id])
                    }
                } else {
                    result.add(database[arg[0]])
                }
            } else {
                result.add(database[data.uuid()])
            }

            if(result.size > 0) {
                for(a in result) {
                    if(a != null) {
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
                send("command.search.total", result.size)
            }
        }

        fun setitem() {
            if(!Permission.check(player, "setitem")) return // <item> <amount> [team]
            val item = content.item(arg[0])
            if(item != null) {
                val amount = arg[1].toIntOrNull()
                if(amount != null) {
                    if(arg.size == 3) {
                        val team = Team.all.find { a -> a.name.equals(arg[2]) }
                        if(team != null) {
                            team.core().items.set(item, if(team.core().storageCapacity < arg[1].toInt()) team.core().storageCapacity else arg[1].toInt())
                        } else {
                            send("command.setitem.wrong.team")
                        }
                    } else {
                        player.core().items.set(item, if(player.core().storageCapacity < arg[1].toInt()) player.core().storageCapacity else arg[1].toInt())
                    }
                } else {
                    send("command.setitem.wrong.amount")
                }
            } else {
                send("command.setitem.item.not.exists")
            }
        }

        fun setperm() {
            if(!Permission.check(player, "setperm")) return
            val target = findPlayers(arg[0])
            if(target != null) {
                val data = findPlayerData(target.uuid())
                if(data != null) {
                    data.permission = arg[1]
                    send("command.setperm.success", data.name, arg[1])
                } else {
                    send("player.not.registered")
                }
            } else {
                val p = findPlayersByName(arg[1])
                if(p != null) {
                    val a = database[p.id]
                    if(a != null) {
                        a.permission = arg[1]
                        database.queue(a)
                        send("command.setperm.success", a.name, arg[1])
                    } else {
                        send("player.not.registered")
                    }
                } else {
                    send("player.not.found")
                }
            }
        }

        fun spawn() {
            if(!Permission.check(player, "spawn")) return
            val type = arg[0]
            val name = arg[1]
            val parameter = if(arg.size == 3) arg[2].toIntOrNull() else 1

            // todo 유닛이 8마리까지 밖에 스폰이 안됨
            when {
                type.equals("unit", true) -> {
                    val unit = content.units().find { unitType : UnitType -> unitType.name == name }
                    if(unit != null) {
                        if(parameter != null) {
                            if(!unit.hidden) {
                                for(a in 1..parameter) {
                                    unit.spawn(player.team(), player.x, player.y)
                                }
                            } else {
                                send("command.spawn.unit.invalid")
                            }
                        } else {
                            send("command.spawn.number")
                        }
                    } else {
                        send("command.spawn.invalid")
                    }
                }

                type.equals("block", true) -> {
                    if(content.blocks().find { a -> a.name == name } != null) {
                        Call.constructFinish(player.tileOn(), content.blocks().find { a -> a.name.equals(name, true) }, player.unit(), parameter?.toByte() ?: 0, player.team(), null)
                    } else {
                        send("command.spawn.invalid")
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

            if(!Permission.check(player, "status")) return
            val bans = netServer.admins.banned.size

            player.sendMessage("""
                [#DEA82A]${bundle["command.status.info"]}[]
                [#2B60DE]========================================[]
                TPS: ${Core.graphics.framesPerSecond}/60
                ${bundle["command.status.banned", bans]}
                ${bundle["command.status.playtime"]}: ${longToTime(PluginData.playtime)}
                ${bundle["command.status.uptime"]}: ${longToTime(PluginData.uptime)}
            """.trimIndent())
        }

        fun t() {
            if(!data.mute) {
                Groups.player.each({ p -> p.team() === player.team() }) { o ->
                    o.sendMessage("[#" + player.team().color.toString() + "]<T>[] ${player.coloredName()} [orange]>[white] ${arg[0]}")
                }
            }
        }

        fun team() {
            if(!Permission.check(player, "team")) return
            val team = selectTeam(arg[0])

            if(arg.size == 1) {
                player.team(team)
            } else if(Permission.check(player, "team.other")) {
                val other = findPlayers(arg[1])
                if(other != null) {
                    other.team(team)
                } else {
                    send("player.not.found")
                }
            }
        }

        fun tempban() {
            if(!Permission.check(player, "tempban")) return
            val other = findPlayers(arg[0])

            if(other == null) {
                send("player.not.found")
            } else {
                val d = findPlayerData(other.uuid())
                if(d == null) {
                    send("command.tempban.not.registered")
                    netServer.admins.banPlayer(other.uuid())
                    Call.kick(other.con(), Packets.KickReason.banned)
                } else {
                    val time = LocalDateTime.now()
                    val minute = arg[1].toLongOrNull()
                    val reason = arg[2]

                    if(minute != null) {
                        d.status.put("ban", time.plusMinutes(minute.toLong()).toString())
                        netServer.admins.banPlayer(other.uuid())
                        Call.kick(other.con(), reason)
                    } else {
                        send("command.tempban.not.number")
                    }
                }
            }
        }

        fun time() {
            if(!Permission.check(player, "time")) return
            val now = LocalDateTime.now()
            val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            player.sendMessage("${bundle["command.time"]}: ${now.format(dateTimeFormatter)}")
        }

        fun tp() {
            if(!Permission.check(player, "tp")) return
            val other = findPlayers(arg[0])

            if(other == null) {
                send("player.not.found")
            } else {
                player.unit().set(other.x, other.y)
                Call.setPosition(player.con(), other.x, other.y)
                Call.setCameraPosition(player.con(), other.x, other.y)
            }
        }

        fun tpp() {
            if(!Permission.check(player, "tp")) return

            if(arg.isEmpty() && data.status.containsKey("tpp")) {
                player.team(Team.get(data.status.get("tpp_team").toInt()))
                send("command.tpp.unfollowing")
                Call.setCameraPosition(player.con(), player.x, player.y)

                data.status.remove("tpp")
                data.status.remove("tpp_team")
            } else {
                val other = findPlayers(arg[0])
                if(other == null) {
                    send("player.not.found")
                } else {
                    data.status.put("tpp_team", player.team().id.toString())
                    data.status.put("tpp", other.uuid())
                    player.clearUnit()
                    player.team(Team.derelict)
                    send("command.tpp.following", other.plainName())
                }
            }
        }

        fun track() {
            if(!Permission.check(player, "tp")) return
            if(data.status.containsKey("tracking")) {
                data.status.remove("tracking")
                send("command.track.toggle.disabled")
            } else {
                data.status.put("tracking", "enabled")
                send("command.track.toggle")
            }
        }

        fun unban() {
            if(!Permission.check(player, "unban")) return
            val target = netServer.admins.findByName(arg[0])
            if(target != null) {
                netServer.admins.unbanPlayerID(arg[0])
                Main.daemon.submit {
                    if(Config.banChannelToken.isNotEmpty()) {
                        Discord.catnip.rest().channel().createMessage(Config.banChannelToken, Bundle()["command.unban", player.plainName(), target.first().lastName, LocalDateTime.now().format(DateTimeFormatter.ofPattern("YYYY-mm-dd HH:mm:ss"))])
                    }
                    if(Config.blockIP) {
                        for(a in target.first().ips) {
                            Runtime.getRuntime().exec(arrayOf("/bin/bash", "-c", "echo ${PluginData.sudoPassword} | sudo -S iptables -D INPUT -s $a -j DROP"))
                        }
                    }
                }
            } else {
                send("player.not.found")
            }
        }

        fun unmute() {
            if(!Permission.check(player, "unmute")) return
            val other = findPlayers(arg[0])
            if(other != null) {
                val target = findPlayerData(other.uuid())
                if(target != null) {
                    target.mute = false
                    database.queue(target)
                    send("command.unmute", target.name)
                } else {
                    send("player.not.found")
                }
            } else {
                val p = findPlayersByName(arg[0])
                if(p != null) {
                    val a = database[p.id]
                    if(a != null) {
                        a.mute = false
                        database.queue(a)
                        send("command.unmute", a.name)
                    } else {
                        send("player.not.registered")
                    }
                } else {
                    send("player.not.found")
                }
            }
        }

        fun url() {
            if(!Permission.check(player, "url")) return
            when(arg[0]) {
                "effect" -> {
                    try {
                        Call.openURI(player.con(), "https://github.com/Anuken/Mindustry/blob/master/core/src/mindustry/content/Fx.java")
                    } catch(e : NoSuchMethodError) {
                        send("command.not.support")
                    }
                }

                else -> {}
            }
        }

        fun weather() {
            if(!Permission.check(player, "weather")) return
            val weather = when(arg[0]) {
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
            } catch(e : NumberFormatException) {
                send("command.weather.not.number")
            }
        }

        fun vote(player : Playerc, arg : Array<out String>) {
            fun sendStart(message : String, vararg parameter : Any) {
                database.players.forEach {
                    if(Event.isPvP) {
                        if(Event.voteTeam == it.player.team()) {
                            val data = findPlayerData(it.uuid)
                            if(data != null) {
                                val bundle = Bundle(data.languageTag)
                                it.player.sendMessage(bundle["command.vote.starter", player.plainName()])
                                it.player.sendMessage(bundle.get(message, *parameter))
                                it.player.sendMessage(bundle["command.vote.how"])
                            }
                        }
                    } else {
                        val data = findPlayerData(it.uuid)
                        if(data != null) {
                            val bundle = Bundle(data.languageTag)
                            it.player.sendMessage(bundle["command.vote.starter", player.plainName()])
                            it.player.sendMessage(bundle.get(message, *parameter))
                            it.player.sendMessage(bundle["command.vote.how"])
                        }
                    }
                }
            }
            if(!Permission.check(player, "vote")) return
            if(arg.isEmpty()) {
                player.sendMessage("command.vote.arg.empty")
                return
            }
            if(Event.voterCooltime.containsKey(player.plainName())) {
                send("command.vote.cooltime")
                return
            }
            if(!Event.voting) {
                if(database.players.size <= 3 && !Permission.check(player, "vote.admin")) {
                    send("command.vote.enough")
                    return
                }
                when(arg[0]) {
                    "kick" -> {
                        if(!Permission.check(player, "vote.kick")) return
                        if(arg.size != 3) {
                            send("command.vote.no.reason")
                            return
                        }
                        val target = findPlayers(arg[1])
                        if(target != null) {
                            if(Permission.check(target, "kick.admin")) {
                                send("command.vote.kick.target.admin")
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
                            send("player.not.found")
                        }
                    }

                    // vote map <map name> <reason>
                    "map" -> {
                        if(!Permission.check(player, "vote.map")) return
                        if(arg.size == 1) {
                            send("command.vote.no.map")
                            return
                        }
                        if(arg.size == 2) {
                            send("command.vote.no.reason")
                            return
                        }
                        if(arg[1].toIntOrNull() != null) {
                            try {
                                var target = maps.all().find { e -> e.name().contains(arg[1]) }
                                if(target == null) {
                                    target = maps.all().sortedBy { a -> a.name() }[arg[1].toInt()]
                                }
                                Event.voteType = "map"
                                Event.voteMap = target
                                Event.voteReason = arg[2]
                                Event.voteStarter = player
                                Event.voting = true
                                sendStart("command.vote.map.start", target.name(), arg[2])
                            } catch(e : IndexOutOfBoundsException) {
                                send("command.vote.map.not.exists")
                            }
                        } else {
                            send("command.vote.map.not.exists")
                        }
                    }

                    // vote gg
                    "gg" -> {
                        if(!Permission.check(player, "vote.gg")) return
                        if(Event.voteCooltime == 0) {
                            Event.voteType = "gg"
                            Event.voteStarter = player
                            Event.voting = true
                            if(state.rules.pvp) {
                                Event.voteTeam = player.team()
                                Event.isPvP = true
                                Event.voteCooltime = 120
                                sendStart("command.vote.gg.pvp.team")
                            } else {
                                sendStart("command.vote.gg.start")
                            }
                        } else {
                            send("command.vote.cooltime")
                        }
                    }

                    // vote skip <count>
                    "skip" -> {
                        if(!Permission.check(player, "vote.skip")) return
                        if(arg.size == 1) {
                            send("command.vote.skip.wrong")
                        } else if(arg[1].toIntOrNull() != null) {
                            if(arg[1].toInt() > 3) {
                                send("command.vote.skip.toomany")
                            } else {
                                if(Event.voteCooltime == 0) {
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
                        if(!Permission.check(player, "vote.back")) return
                        if(!saveDirectory.child("rollback.msav").exists()) {
                            player.sendMessage("command.vote.back.no.file")
                            return
                        }
                        if(arg.size == 1) {
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
                        if(!Permission.check(player, "vote.random")) return
                        if(Event.voteCooltime == 0) {
                            Event.voteType = "random"
                            Event.voteStarter = player
                            Event.voting = true
                            Event.voteCooltime = 360
                            sendStart("command.vote.random.start")
                        } else {
                            send("command.vote.cooltime")
                        }
                    }

                    else -> {
                        send("command.vote.wrong")
                    }
                }
            }
        }

        fun votekick() {
            if(arg[0].contains("#")) {
                val f = Groups.player.find { p -> p.id() == arg[0].substring(1).toInt() }

                if(Permission.check(f, "kick.admin")) {
                    send("command.vote.kick.target.admin")
                } else {
                    val array = arrayOf("kick", f.name, "Kick")
                    vote(player, array)
                }
            }
        }

        fun selectTeam(arg : String) : Team {
            return if("derelict".first() == arg.first()) {
                Team.derelict
            } else if("sharded".first() == arg.first()) {
                Team.sharded
            } else if("crux".first() == arg.first()) {
                Team.crux
            } else if("green".first() == arg.first()) {
                Team.green
            } else if("malis".first() == arg.first()) {
                Team.malis
            } else if("blue".first() == arg.first()) {
                Team.blue
            } else if("derelict".contains(arg[0], true)) {
                Team.derelict
            } else if("sharded".contains(arg[0], true)) {
                Team.sharded
            } else if("crux".contains(arg[0], true)) {
                Team.crux
            } else if("green".contains(arg[0], true)) {
                Team.green
            } else if("malis".contains(arg[0], true)) {
                Team.malis
            } else if("blue".contains(arg[0], true)) {
                Team.blue
            } else {
                state.rules.defaultTeam
            }
        }
    }

    class Server(val arg : Array<String>) {
        fun genDocs() {
            if(System.getenv("DEBUG_KEY") != null) {
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
        }

        fun reload() {
            try {
                Permission.load()
                Log.info(Bundle()["config.permission.updated"])
                Config.load()
                Log.info(Bundle()["config.reloaded"])
            } catch(e : Exception) {
                e.printStackTrace()
            }
        }

        fun debug() {

            when(arg[0]) {
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
                    if(arg.isNotEmpty()) {
                        if(arg[0].toBoolean()) {
                            Core.settings.put("debugMode", true)
                        } else {
                            Core.settings.put("debugMode", false)
                        }
                    }
                }

                "sync" -> {
                    for(a in netServer.admins.banned) {
                        for(b in a.ips) {
                            Runtime.getRuntime().exec(arrayOf("/bin/bash", "-c", "echo ${PluginData.sudoPassword} | sudo -S iptables -D INPUT -s $b -j DROP"))
                            Runtime.getRuntime().exec(arrayOf("/bin/bash", "-c", "echo ${PluginData.sudoPassword} | sudo -S iptables -A INPUT -s $b -j DROP"))
                        }
                    }
                }
            }
        }

        fun setperm() {
            val target = findPlayers(arg[0])
            if(target != null) {
                val data = findPlayerData(target.uuid())
                if(data != null) {
                    data.permission = arg[1]
                    database.queue(data)
                } else {
                    Log.info(stripColors(bundle["player.not.registered"]))
                }
            } else {
                Log.info(stripColors(bundle["player.not.found"]))
            }
        }

        fun sync() {
            if(Main.connectType) {
                Log.info(bundle["command.sync.client.only"])
            } else {
                Trigger.Client.send("sync")
                Log.info(bundle["success"])
            }
        }

        fun tempban() {
            val other = findPlayers(arg[0])

            if(other == null) {
                Log.info(bundle["player.not.found"])
            } else {
                val d = findPlayerData(other.uuid())
                if(d == null) {
                    Log.info(stripColors(bundle["command.tempban.not.registered"]))
                    netServer.admins.banPlayer(other.uuid())
                    Call.kick(other.con(), Packets.KickReason.banned)
                } else {
                    val time = LocalDateTime.now()
                    val minute = arg[1].toLongOrNull()
                    val reason = arg[2]

                    if(minute != null) {
                        d.status.put("ban", time.plusMinutes(minute.toLong()).toString())
                        Call.kick(other.con(), reason)
                        if(Config.banChannelToken.isNotEmpty()) {
                            Discord.catnip.rest().channel().createMessage(Config.banChannelToken, Bundle()["command.tempban.banned", d.name, player.plainName(), time.plusMinutes(minute.toLong()).format(DateTimeFormatter.ofPattern("YYYY-mm-dd HH:mm:ss"))])
                        }
                    } else {
                        Log.info(stripColors(bundle["command.tempban.not.number"]))
                    }
                }
            }
        }

        fun stripColors(string : String) : String {
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
            for(i in 0..level) requiredXP += calcXpForLevel(i)
            return requiredXP
        }

        private fun calculateLevel(xp : Double) : Int {
            var level = 0
            var maxXp = calcXpForLevel(0)
            do maxXp += calcXpForLevel(++level) while(maxXp < xp)
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

    object Discord {
        val pin : ObjectMap<String, Int> = ObjectMap()
        lateinit var catnip : Catnip

        init {
            if(Config.botToken.isNotEmpty() && Config.channelToken.isNotEmpty()) {
                catnip = Catnip.catnip(Config.botToken)
            }
        }

        fun start() {
            catnip.observable(DiscordEvent.MESSAGE_CREATE).subscribe({
                if(it.channelIdAsLong().toString() == Config.channelToken && !it.author().bot()) {
                    if(it.content().toIntOrNull() != null) {
                        if(pin.findKey(it.content(), true) != null) {
                            val data = database[pin.findKey(it.content().toInt(), true)]
                            data?.status?.put("discord", it.author().id())
                            pin.remove(pin.findKey(it.content().toInt(), true))
                        }
                    } else {
                        with(it.content()) {
                            when {
                                equals("help", true) -> {
                                    val message = """
                                    ``!help`` ${Bundle()["event.discord.help.help"]}
                                    ``!ping`` ${Bundle()["event.discord.help.ping"]}
                                """.trimIndent()
                                    it.reply(message, true).subscribe { m : Message ->
                                        sleep(7000)
                                        m.delete()
                                    }
                                }

                                equals("ping", true) -> {
                                    val start = System.currentTimeMillis()
                                    it.reply("pong!", true).subscribe { ping : Message ->
                                        val end = System.currentTimeMillis()
                                        ping.edit("pong! (" + (end - start) + "ms).")
                                        sleep(5000)
                                        ping.delete()
                                    }
                                }

                                /*startsWith("!auth", true) -> {
                                    if (Config.authType == Config.AuthType.Discord) {
                                        val arg = it.content().replace("!auth ", "").split(" ")
                                        if (arg.size == 1) {
                                            try {
                                                var isMatch = false

                                                for (a in database.getAll()) {
                                                    if (a.status.containsKey("discord")) {
                                                        if (a.status.get("discord") != it.author().id()) {
                                                            isMatch = true
                                                        }
                                                    }
                                                }

                                                if (!isMatch) {
                                                    var data: DB.PlayerData? = null
                                                    for (a in pin) {
                                                        if (a.value == arg[0].toInt()) {
                                                            data = database.getAll().find { b -> b.name == a.key }
                                                        }
                                                    }

                                                    if (data != null) {
                                                        data.status.put("discord", it.author().id())

                                                        it.reply(Bundle()["event.discord.auth.success"], true).subscribe { m: Message ->
                                                            sleep(5000)
                                                            m.delete()
                                                        }
                                                        pin.removeAll { a -> a.value == arg[0].toInt() }
                                                    } else {
                                                        it.reply("등록되지 않은 계정입니다!", true).subscribe { m: Message ->
                                                            sleep(5000)
                                                            m.delete()
                                                        }
                                                    }
                                                } else {
                                                    it.reply("이미 등록된 계정입니다!", true).subscribe { m: Message ->
                                                        sleep(5000)
                                                        m.delete()
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                it.reply("올바른 PIN 번호가 아닙니다! 사용법: ``!auth <PIN 번호>``", true).subscribe { m: Message ->
                                                    sleep(5000)
                                                    m.delete()
                                                }
                                            }
                                        } else {
                                            it.reply("사용법: ``!auth <PIN 번호>``", true).subscribe { m: Message ->
                                                sleep(5000)
                                                m.delete()
                                            }
                                        }
                                    } else {
                                        it.reply("현재 서버에 Discord 인증이 활성화 되어 있지 않습니다!", true)
                                    }
                                }*/
                                else -> {}
                            }
                        }
                    }
                }
            }) { e : Throwable -> e.printStackTrace() }
        }

        fun queue(player : Playerc) : Int {
            val number = (Math.random() * 9999).toInt()
            pin.put(player.uuid(), number)
            return number
        }

        fun shutdownNow() {
            if(Discord::catnip.isInitialized) catnip.shutdown()
        }
    }

    object StringUtils {
        // Source from https://howtodoinjava.com/java/string/escape-html-encode-string/
        private val htmlEncodeChars = ObjectMap<Char, String>()
        fun encodeHtml(source : String?) : String? {
            return encode(source)
        }

        private fun encode(source : String?) : String? {
            if(null == source) return null
            var encode : StringBuilder? = null
            val encodeArray = source.toCharArray()
            var match = -1
            var difference : Int
            for(i in encodeArray.indices) {
                val charEncode = encodeArray[i]
                if(htmlEncodeChars.containsKey(charEncode)) {
                    if(null == encode) encode = StringBuilder(source.length)
                    difference = i - (match + 1)
                    if(difference > 0) encode.appendRange(encodeArray, match + 1, match + 1 + difference)
                    encode.append(htmlEncodeChars[charEncode])
                    match = i
                }
            }
            return if(null == encode) {
                source
            } else {
                difference = encodeArray.size - (match + 1)
                if(difference > 0) encode.appendRange(encodeArray, match + 1, match + 1 + difference)
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