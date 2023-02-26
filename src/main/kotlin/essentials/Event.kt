package essentials

import arc.ApplicationListener
import arc.Core
import arc.Events
import arc.files.Fi
import arc.func.Cons2
import arc.graphics.Color
import arc.graphics.Colors
import arc.struct.ObjectMap
import arc.struct.ObjectSet
import arc.struct.Seq
import arc.util.Align
import arc.util.Log
import arc.util.Strings
import arc.util.Time
import com.github.pemistahl.lingua.api.IsoCode639_1
import com.github.pemistahl.lingua.api.Language
import com.github.pemistahl.lingua.api.LanguageDetector
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder
import essentials.Main.Companion.database
import mindustry.Vars.*
import mindustry.content.*
import mindustry.core.NetServer
import mindustry.entities.Damage
import mindustry.game.EventType
import mindustry.game.EventType.*
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.gen.Playerc
import mindustry.io.SaveIO
import mindustry.maps.Map
import mindustry.net.Administration.PlayerInfo
import mindustry.net.Packets
import mindustry.net.WorldReloader
import mindustry.world.Block
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.regex.Pattern
import kotlin.experimental.and
import kotlin.math.abs
import kotlin.math.floor

object Event {
    private var orignalBlockMultiplier = 1f
    private var orignalUnitMultiplier = 1f
    private var enemyBuildingDestroyed = 0

    var voting = false
    var voteType : String? = null
    var voteTarget : Playerc? = null
    var voteTargetUUID : String? = null
    var voteReason : String? = null
    var voteMap : Map? = null
    var voteWave : Int? = null
    var voteStarter : Playerc? = null
    var isPvP : Boolean = false
    var voteTeam : Team = state.rules.defaultTeam
    var voteCooltime : Int = 0
    private var voted = Seq<String>()
    private var lastVoted = LocalTime.now()
    private var isAdminVote = false
    private var isCanceled = false

    var worldHistory = Seq<TileLog>()
    var playerHistory = Seq<PlayerLog>()
    var voterCooltime = ObjectMap<String, Int>()

    private var random = Random()
    private var dateformat = SimpleDateFormat("HH:mm:ss")
    private var blockExp = ObjectMap<String, Int>()
    private var dosBlacklist = ObjectSet<String>()
    private var pvpCount = Config.pvpPeaceTime
    private var count = 60
    var pvpSpectors = Seq<String>()
    var pvpPlayer = Seq<String>()
    var isGlobalMute = false

    fun register() {
        Events.on(WithdrawEvent::class.java) {
            if(it.tile != null && it.player.unit().item() != null && it.player.name != null) {
                log(LogType.WithDraw, Bundle()["log.withdraw", it.player.plainName(), it.player.unit().item().name, it.amount, it.tile.block.name, it.tile.tileX(), it.tile.tileY()])
            }
        }

        Events.on(DepositEvent::class.java) {
            if(it.tile != null && it.player.unit().item() != null && it.player.name != null) {
                log(LogType.Deposit, Bundle()["log.deposit", it.player.plainName(), it.player.unit().item().name, it.amount, it.tile.block.name, it.tile.tileX(), it.tile.tileY()])
            }
        }

        Events.on(ConfigEvent::class.java) { // todo 전력 노드 오작동
            if(it.tile != null && it.tile.block() != null && it.player != null && it.value is Int) {
                if(Config.antiGrief) {
                    val entity = it.tile
                    val other = world.tile(it.value as Int)
                    val valid = other != null && entity.power != null && other.block().hasPower && other.block().outputsPayload
                    if(valid) {
                        val oldGraph = entity.power.graph
                        val newGraph = other.build.power.graph
                        val oldGraphCount = oldGraph.toString().substring(oldGraph.toString().indexOf("all=["), oldGraph.toString().indexOf("], graph")).replaceFirst("all=\\[".toRegex(), "").split(",").toTypedArray().size
                        val newGraphCount = newGraph.toString().substring(newGraph.toString().indexOf("all=["), newGraph.toString().indexOf("], graph")).replaceFirst("all=\\[".toRegex(), "").split(",").toTypedArray().size
                        if(abs(oldGraphCount - newGraphCount) > 10) {
                            database.players.forEach { a ->
                                a.player.sendMessage(Bundle(a.languageTag)["event.antigrief.node", it.player.name, oldGraphCount.coerceAtLeast(newGraphCount), oldGraphCount.coerceAtMost(newGraphCount), "${it.tile.x}, ${it.tile.y}"])
                            }
                        }
                    }
                }

                addLog(TileLog(System.currentTimeMillis(), it.player.name, "config", it.tile.tile.x, it.tile.tile.y, it.tile.block().name, it.tile.rotation, it.tile.team))
                addLog(PlayerLog(it.player.name, it.tile.block(), it.tile.tile.x, it.tile.tile.y, it.tile.tile.block().configurations))
            }
        }

        Events.on(TapEvent::class.java) {
            log(LogType.Tap, Bundle()["log.tap", it.player.plainName(), it.tile.block().name])
            addLog(TileLog(System.currentTimeMillis(), it.player.name, "tap", it.tile.x, it.tile.y, it.tile.block().name, if(it.tile.build != null) it.tile.build.rotation else 0, if(it.tile.build != null) it.tile.build.team else state.rules.defaultTeam))
            val data = findPlayerData(it.player.uuid())
            if(data != null) {
                for(a in PluginData.warpBlocks) {
                    if(it.tile.block().name == a.tileName && it.tile.build.tileX() == a.x && it.tile.build.tileY() == a.y) {
                        if(a.online) {
                            for(b in database.players) b.player.sendMessage(Bundle(b.languageTag)["event.tap.server", it.player.plainName(), a.description])
                            Log.info(Bundle()["log.warp.move.block", it.player.plainName(), Strings.stripColors(a.description), a.ip, a.port.toString()])
                            Call.connect(it.player.con(), a.ip, a.port)
                        }
                        break
                    }
                }

                for(a in PluginData.warpZones) {
                    if(it.tile.x > a.startTile.x && it.tile.x < a.finishTile.x && it.tile.y > a.startTile.y && it.tile.y < a.finishTile.y) {
                        Log.info(Bundle()["log.warp.move", it.player.plainName(), a.ip, a.port.toString()])
                        Call.connect(it.player.con(), a.ip, a.port)
                        break
                    }
                }

                if(data.status.containsKey("log")) {
                    val buf = Seq<TileLog>()
                    for(a in worldHistory) {
                        if(a.x == it.tile.x && a.y == it.tile.y) {
                            buf.add(a)
                        }
                    }
                    val str = StringBuilder()
                    val bundle = Bundle(data.languageTag)
                    val coreBundle = ResourceBundle.getBundle("bundle_block", try {
                        when(data.languageTag) {
                            "ko" -> Locale.KOREA
                            else -> Locale.ENGLISH
                        }
                    } catch(e : Exception) {
                        Locale.ENGLISH
                    })

                    for(a in buf) {
                        val action = when(a.action) {
                            "tap" -> "[royal]${bundle["event.log.tap"]}[]"
                            "break" -> "[scarlet]${bundle["event.log.break"]}[]"
                            "place" -> "[sky]${bundle["event.log.place"]}[]"
                            "config" -> "[cyan]${bundle["event.log.config"]}[]"
                            else -> ""
                        }

                        str.append(bundle["event.log.format", dateformat.format(a.time), a.player, coreBundle.getString("block.${a.tile}.name"), action]).append("\n")
                    }

                    Call.effect(it.player.con(), Fx.shockwave, it.tile.getX(), it.tile.getY(), 0f, Color.cyan)
                    val str2 = StringBuilder()
                    if(str.toString().lines().size > 10) {
                        val lines : List<String> = str.toString().split("\n").reversed()
                        for(i in 0 until 10) {
                            str2.append(lines[i]).append("\n")
                        }
                        it.player.sendMessage(str2.toString())
                    } else {
                        it.player.sendMessage(str.toString())
                    }
                }

                for(a in database.players) {
                    if(a.status.containsKey("tracking")) {
                        Call.effect(a.player.con(), Fx.bigShockwave, it.tile.getX(), it.tile.getY(), 0f, Color.cyan)
                    }
                }
            }
        }

        Events.on(PickupEvent::class.java) {

        }

        Events.on(UnitControlEvent::class.java) {

        }

        Events.on(WaveEvent::class.java) {
            if(Config.waveskip > 1) {
                var loop = 1
                while(Config.waveskip != loop) {
                    loop++
                    spawner.spawnEnemies()
                    state.wave++
                    state.wavetime = state.rules.waveSpacing
                }
            }
        }

        Events.on(ServerLoadEvent::class.java) {
            content.blocks().each {
                var buf = 0
                for(b in it.requirements) {
                    buf = +b.amount
                }
                blockExp.put(it.name, buf)
            }

            dosBlacklist = netServer.admins.dosBlacklist

            if(Config.countAllServers) {
                Core.settings.put("totalPlayers", 0)
                Core.settings.saveValues()
            }

            if(!Config.blockIP && Config.database != Main.root.child("database").absolutePath() && PluginData["iptablesFirst"] != null) {
                Log.warn(Bundle()["event.database.blockip.conflict"])

                val os = System.getProperty("os.name").lowercase(Locale.getDefault())
                if(os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                    Config.blockIP = true
                    Log.info(Bundle()["config.blockIP.enabled"])
                }
            } else if(!Config.blockIP && PluginData["iptablesFirst"] != null) {
                for(a in netServer.admins.banned) {
                    for(b in a.ips) {
                        val cmd = arrayOf("/bin/bash", "-c", "echo ${PluginData.sudoPassword}| sudo -S iptables -D INPUT -s $b -j DROP")
                        Runtime.getRuntime().exec(cmd)
                    }
                }
                PluginData.status.remove("iptablesFirst")
                Log.info(Bundle()["event.ban.iptables.remove"])
                PluginData.changed = true
            } else if(Config.blockIP && PluginData["iptablesFirst"] == null) {
                for(a in netServer.admins.banned) {
                    for(b in a.ips) {
                        val cmd = arrayOf("/bin/bash", "-c", "echo ${PluginData.sudoPassword}| sudo -S iptables -A INPUT -s $b -j DROP")
                        Runtime.getRuntime().exec(cmd)
                        Log.info(Bundle()["event.ban.iptables.exists", b, a.lastName])
                    }
                }
                PluginData.status.put("iptablesFirst", "none")
                PluginData.changed = true
            }

            netServer.chatFormatter = NetServer.ChatFormatter { player : Player, message : String ->
                var isMute = false
                if(Config.blockfooclient) {
                    if(message.takeLast(2).all { a -> (0xF80 until 0x107F).contains(a.code) }) {
                        Call.kick(player.con(), Bundle(if(findPlayers(player.plainName()) != null) findPlayers(player.plainName())!!.locale() else "")["event.antigrief.foo.detected", "2"])
                        Log.info(Bundle()["event.antigrief.foo.detected.log", player.plainName(), "2"])
                        for(a in database.players) {
                            a.player.sendMessage(Bundle(a.languageTag)["event.antigrief.foo", player.name, "2"])
                        }
                        isMute = true
                    }
                }

                if(!message.startsWith("/")) {
                    val data = findPlayerData(player.uuid())
                    if(data != null) {
                        log(LogType.Chat, "${data.name}: $message")

                        if(!data.mute) {
                            val isAdmin = Permission.check(player, "vote.pass")
                            if(voting && message.equals("y", true) && !voted.contains(player.uuid())) {
                                if(isAdmin) {
                                    isAdminVote = true
                                } else {
                                    if(state.rules.pvp && voteTeam == player.team()) {
                                        voted.add(player.uuid())
                                    } else if(!state.rules.pvp) {
                                        voted.add(player.uuid())
                                    }
                                }
                                player.sendMessage(Bundle(data.languageTag)["command.vote.voted"])
                            } else if(voting && message.equals("n", true) && isAdmin) {
                                isCanceled = true
                            }

                            if(Config.chatlimit) {
                                val configs = Config.chatlanguage.split(",")
                                val languages = ArrayList<Language>()
                                configs.forEach { a -> languages.add(Language.getByIsoCode639_1(IsoCode639_1.valueOf(a.uppercase()))) }

                                val d : LanguageDetector = LanguageDetectorBuilder.fromLanguages(*languages.toTypedArray()).build()
                                val e : Language = d.detectLanguageOf(message)

                                if(e.name == "UNKNOWN" && !message.substring(0, 1).matches(Regex("[!@#$%&*()_+=|<>?{}\\[\\]~-]")) && !(voting && message.equals("y", true) && !voted.contains(player.uuid()))) {
                                    player.sendMessage(Bundle(data.languageTag)["event.chat.language.not.allow"])
                                    isMute = true
                                }
                            }

                            if(Config.chatBlacklist) {
                                val file = Main.root.child("chat_blacklist.txt").readString("UTF-8").split("\r\n")
                                if(file.isNotEmpty()) {
                                    for(a in file) {
                                        if(Config.chatBlacklistRegex) {
                                            if(message.contains(Regex(a))) {
                                                player.sendMessage(Bundle(findPlayerData(player.uuid())!!.languageTag)["event.chat.blacklisted"])
                                                isMute = true
                                            }
                                        } else {
                                            if(message.contains(a)) {
                                                player.sendMessage(Bundle(findPlayerData(player.uuid())!!.languageTag)["event.chat.blacklisted"])
                                                isMute = true
                                            }
                                        }
                                    }
                                }
                            }
                            val format = Permission[player].chatFormat.replace("%1", "[#${player.color}]${data.name}").replace("%2", message)
                            return@ChatFormatter if(isGlobalMute && Permission.check(player, "chat.admin")) {
                                format
                            } else if(!isGlobalMute && !(voting && message.contains("y") && !isMute)) {
                                format
                            } else {
                                null
                            }
                        } else {
                            return@ChatFormatter null
                        }
                    } else {
                        return@ChatFormatter "[gray]${player.name} [orange] > [white]${message}"
                    }
                } else {
                    return@ChatFormatter null
                }
            }
        }

        Events.on(GameOverEvent::class.java) {
            if(voting) {
                database.players.forEach { a ->
                    if(voteTargetUUID != a.uuid) a.player.sendMessage(Bundle(a.languageTag)["command.vote.canceled"])
                }

                resetVote()
            }

            if(!state.rules.infiniteResources) {
                if(state.rules.pvp) {
                    for(a in database.players) {
                        if(a.player.team() == it.winner) {
                            a.pvpwincount++
                        }
                    }
                } else if(state.rules.attackMode) {
                    for(a in database.players) {
                        if(a.player.team() == it.winner) {
                            a.attackclear++
                        }
                    }
                }
                for(p in Groups.player) {
                    val target = findPlayerData(p.uuid())
                    if(target != null) earnEXP(it.winner, p, target)
                }
            }
            if(voting && voteType == "gg") resetVote()
            worldHistory = Seq<TileLog>()
            pvpSpectors = Seq<String>()
            pvpPlayer = Seq<String>()
        }

        Events.on(BlockBuildBeginEvent::class.java) {

        }

        Events.on(BlockBuildEndEvent::class.java) {
            val isDebug = Core.settings.getBool("debugMode")

            if(it.unit.isPlayer) {
                val player = it.unit.player
                val target = findPlayerData(player.uuid())

                if(!player.unit().isNull && target != null && it.tile.block() != null && player.unit().buildPlan() != null) {
                    val block = it.tile.block()
                    if(!it.breaking) {
                        log(LogType.Block, Bundle()["log.block.place", target.name, block.name, it.tile.x, it.tile.y])
                        addLog(TileLog(System.currentTimeMillis(), target.name, "place", it.tile.x, it.tile.y, it.tile.block().name, if(it.tile.build != null) it.tile.build.rotation else 0, if(it.tile.build != null) it.tile.build.team else state.rules.defaultTeam))
                        addLog(PlayerLog(target.name, it.tile.block(), it.tile.x, it.tile.y, null))

                        if(!state.rules.infiniteResources) {
                            target.placecount++
                            target.exp = target.exp + blockExp.get(block.name)
                        }

                        if(isDebug) {
                            Log.info("${player.name} placed ${it.tile.block().name} to ${it.tile.x},${it.tile.y}")
                        }
                    } else if(it.breaking) {
                        log(LogType.Block, Bundle()["log.block.break", target.name, block.name, it.tile.x, it.tile.y])
                        addLog(TileLog(System.currentTimeMillis(), target.name, "break", it.tile.x, it.tile.y, player.unit().buildPlan().block.name, if(it.tile.build != null) it.tile.build.rotation else 0, if(it.tile.build != null) it.tile.build.team else state.rules.defaultTeam))
                        addLog(PlayerLog(target.name, player.unit().buildPlan().block, it.tile.x, it.tile.y, null))

                        if(!state.rules.infiniteResources) {
                            target.breakcount++
                            target.exp = target.exp - blockExp.get(player.unit().buildPlan().block.name)
                        }
                    }
                }
            }
        }

        Events.on(BuildSelectEvent::class.java) {
            if(it.builder is Playerc && it.builder.buildPlan() != null && !Pattern.matches(".*build.*", it.builder.buildPlan().block.name) && it.tile.block() !== Blocks.air && it.breaking) {
                log(LogType.Block, Bundle()["log.block.remove", (it.builder as Playerc).plainName(), it.tile.block().name, it.tile.x, it.tile.y])
            }
        }

        Events.on(BlockDestroyEvent::class.java) {
            if(Config.destroyCore && state.rules.coreCapture) {
                Fx.spawnShockwave.at(it.tile.getX(), it.tile.getY(), state.rules.dropZoneRadius)
                Damage.damage(world.tile(it.tile.pos()).team(), it.tile.getX(), it.tile.getY(), state.rules.dropZoneRadius, 1.0E8f, true)
            }

            if(state.rules.attackMode && it.tile.team() != state.rules.defaultTeam) {
                enemyBuildingDestroyed++
            }
        }

        Events.on(UnitDestroyEvent::class.java) {

        }

        Events.on(UnitCreateEvent::class.java) { u ->
            if(Groups.unit.size() > Config.spawnLimit) {
                u.unit.health(0f)

                database.players.forEach {
                    it.player.sendMessage(Bundle(it.languageTag)["config.spawnlimit.reach", "[scarlet]${Groups.unit.size()}[white]/[sky]${Config.spawnLimit}"])
                }
            }
        }

        Events.on(UnitChangeEvent::class.java) {

        }

        fun checkHack(inp : Float, item : Byte) : Boolean {
            var bits = java.lang.Float.floatToIntBits(inp)
            val itembits = java.lang.Byte.toUnsignedInt(item)
            bits = bits shl 32 - 8
            var addend : Int = item.toInt()
            addend = addend shl 32 - 8

            return bits == addend || bits and itembits == itembits
        }

        Events.on(PlayerJoin::class.java) {
            log(LogType.Player, Bundle()["log.joined", it.player.plainName(), it.player.uuid(), it.player.con.address])
            it.player.admin(false)

            val data = database[it.player.uuid()]
            if(Config.authType == Config.AuthType.None) {
                if(data != null) {
                    Trigger.loadPlayer(it.player, data)
                } else if(Config.authType != Config.AuthType.None) {
                    it.player.sendMessage(Bundle(it.player.locale)["event.player.first.register"])
                } else if(Config.authType == Config.AuthType.None) {
                    Main.daemon.submit(Thread {
                        if(database.getAll().contains { a -> a.name == it.player.name() }) {
                            Core.app.post { it.player.con.kick(Bundle(it.player.locale)["event.player.name.duplicate"], 0L) }
                        } else {
                            Core.app.post { Trigger.createPlayer(it.player, null, null) }
                        }
                    })
                }
            }

            if(Config.blockfooclient) {
                Core.app.addListener(object: ApplicationListener {
                    var hackCount = 0
                    var timeout = 600

                    override fun update() {
                        if(it.player != null) {
                            if(!it.player.con.mobile) {
                                val x = it.player.mouseX()
                                val y = it.player.mouseY()

                                var fooUser = checkHack(x, 170.toByte()) && checkHack(y, 85.toByte()) || checkHack(y, 170.toByte())
                                var assistUser = (checkHack(x, 170.toByte())) || checkHack(x, 85.toByte()) && checkHack(y, 170.toByte())

                                var kicked = false

                                if(fooUser || assistUser) {
                                    hackCount++
                                    if(hackCount > 150) {
                                        kicked = true
                                        Call.kick(it.player.con(), Bundle(if(findPlayers(it.player.plainName()) != null) findPlayers(it.player.plainName())!!.locale() else "")["event.antigrief.foo.detected", "0"])
                                        Log.info(Bundle()["event.antigrief.foo.detected.log", it.player.plainName(), "0"])
                                        for(a in database.players) {
                                            a.player.sendMessage(Bundle(a.languageTag)["event.antigrief.foo", it.player.name, "0"])
                                        }
                                        Core.app.removeListener(this)
                                    }
                                }

                                if(!kicked) {
                                    val aimx = it.player.unit().aimX
                                    val aimy = it.player.unit().aimY

                                    fooUser = checkHack(aimx, 170.toByte()) && checkHack(aimy, 85.toByte()) || checkHack(aimy, 170.toByte())
                                    assistUser = (checkHack(aimx, 170.toByte())) || checkHack(aimx, 85.toByte()) && checkHack(aimy, 170.toByte())

                                    if(fooUser || assistUser) {
                                        hackCount++
                                        if(hackCount > 150) {
                                            Call.kick(it.player.con(), Bundle(if(findPlayers(it.player.plainName()) != null) findPlayers(it.player.plainName())!!.locale() else "")["event.antigrief.foo.detected", "1"])
                                            Log.info(Bundle()["event.antigrief.foo.detected.log", it.player.plainName(), "1"])
                                            for(a in database.players) {
                                                a.player.sendMessage(Bundle(a.languageTag)["event.antigrief.foo", it.player.name, "1"])
                                            }
                                            Core.app.removeListener(this)
                                        }
                                    }
                                }
                            }
                        } else {
                            Core.app.removeListener(this)
                        }

                        if(timeout != 0) {
                            timeout--
                        } else {
                            Core.app.removeListener(this)
                        }
                    }
                })
            }
        }

        Events.on(PlayerLeave::class.java) {
            log(LogType.Player, Bundle()["log.player.disconnect", it.player.plainName(), it.player.uuid(), it.player.con.address])
            val data = database.players.find { data -> data.uuid == it.player.uuid() }
            if(data != null) {
                database.queue(data)
            }
            database.players.remove(data)
        }

        Events.on(PlayerBanEvent::class.java) {
            if(Config.blockIP) {
                val os = System.getProperty("os.name").lowercase(Locale.getDefault())
                if(os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                    val ip = if(it.player != null) it.player.ip() else netServer.admins.getInfo(it.uuid).lastIP
                    Runtime.getRuntime().exec(arrayOf("/bin/bash", "-c", "echo ${PluginData.sudoPassword} | sudo -S iptables -D INPUT -s $ip -j DROP"))
                    Runtime.getRuntime().exec(arrayOf("/bin/bash", "-c", "echo ${PluginData.sudoPassword} | sudo -S iptables -A INPUT -s $ip -j DROP"))
                    Log.info(Bundle()["event.ban.iptables", ip])
                }
            }

            if(Config.banChannelToken.isNotEmpty()) {
                val name = if(it.player != null) {
                    netServer.admins.findByName(it.player.uuid()).first().names.toString(", ")
                } else {
                    netServer.admins.banned.find { a -> a.id.equals(it.uuid) }.names.toString(", ")
                }

                val id = if(it.player != null) {
                    netServer.admins.findByName(it.player.uuid()).first().id
                } else {
                    netServer.admins.banned.find { a -> a.id.equals(it.uuid) }.id
                }

                val msg = Bundle()["event.discord.banned", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")), name, id]
                Commands.Discord.catnip.rest().channel().createMessage(Config.banChannelToken, msg)
            }

            log(LogType.Player, Bundle()["log.player.banned", if(it.player == null) netServer.admins.getInfo(it.uuid).lastName else it.player.name, if(it.player == null) netServer.admins.getInfo(it.uuid).lastIP else it.player.ip()])
        }

        Events.on(PlayerIpUnbanEvent::class.java) {
            if(Config.banChannelToken.isNotEmpty()) {
                val msg = Bundle()["event.discord.ip.unbanned", netServer.admins.findByIP(it.ip).lastName]
                Commands.Discord.catnip.rest().channel().createMessage(Config.banChannelToken, msg)
            }
        }

        Events.on(AdminRequestEvent::class.java) {
            if(Config.banChannelToken.isNotEmpty() && it.action == Packets.AdminAction.ban) {
                val msg = Bundle()["event.discord.banned.admin", it.player.plainName()]
                Commands.Discord.catnip.rest().channel().createMessage(Config.banChannelToken, msg)
            }
        }

        Events.on(WorldLoadEvent::class.java) {
            PluginData.playtime = 0L
            if(saveDirectory.child("rollback.msav").exists()) saveDirectory.child("rollback.msav").delete()

            if(state.rules.pvp) {
                if(Config.pvpPeace) {
                    orignalBlockMultiplier = state.rules.blockDamageMultiplier
                    orignalUnitMultiplier = state.rules.unitDamageMultiplier
                    state.rules.blockDamageMultiplier = 0f
                    state.rules.unitDamageMultiplier = 0f
                    pvpCount = Config.pvpPeaceTime
                }
                pvpSpectors = Seq<String>()

                for(a in database.players) {
                    if(Permission.check(a.player, "pvp.spector")) {
                        a.player.team(Team.derelict)
                    }
                }
            }
        }

        Events.on(ConnectPacketEvent::class.java) { e ->
            log(LogType.Player, "${e.packet.name} (${e.packet.uuid}, ${e.connection.address}) connected.")

            if(Config.blockNewUser && netServer.admins.getInfo(e.packet.uuid) == null) {
                e.connection.kick(Bundle(e.packet.locale)["event.player.new.blocked"], 0L)
                return@on
            }

            if(!Config.allowMobile && e.connection.mobile) {
                e.connection.kick(Bundle(e.packet.locale)["event.player.not.allow.mobile"], 0L)
            }

            // 닉네임이 블랙리스트에 등록되어 있는지 확인
            for(s in PluginData.blacklist) {
                if(e.packet.name.matches(Regex(s))) e.connection.kick(Bundle(e.packet.locale)["event.player.name.blacklisted"], 0L)
            }

            if(Config.fixedName) {
                if(e.packet.name.length > 32) e.connection.kick(Bundle(e.packet.locale)["event.player.name.long"], 0L)
                if(e.packet.name.matches(Regex(".*\\[.*].*"))) e.connection.kick(Bundle(e.packet.locale)["event.player.name.parenthese"], 0L)
                if(e.packet.name.contains("　")) e.connection.kick(Bundle(e.packet.locale)["event.player.name.special"], 0L)
                if(e.packet.name.contains(" ")) e.connection.kick(Bundle(e.packet.locale)["event.player.name.not.allow"], 0L)
            }

            if(Config.minimalName && e.packet.name.length < 4) e.connection.kick(Bundle(e.packet.locale)["event.player.name.short"], 0L)

            if(Config.antiVPN) {
                for(a in PluginData.vpnList) {
                    val match = IpAddressMatcher(a)
                    if(match.matches(e.connection.address)) {
                        e.connection.kick(Bundle(e.packet.locale)["anti-grief.vpn"])
                    }
                }
            }

            if(netServer.admins.isIDBanned(e.packet.uuid)) {
                val msg = Bundle()["event.discord.connect.blocked", netServer.admins.findByName(e.packet.uuid).first().lastName]
                Commands.Discord.catnip.rest().channel().createMessage(Config.banChannelToken, msg)
            }
        }

        fun send(message : String, vararg parameter : Any) {
            database.players.forEach {
                if(voteTargetUUID != it.uuid) {
                    Core.app.post { it.player.sendMessage(Bundle(it.languageTag).get(message, *parameter)) }
                }
            }
        }

        fun check() : Int {
            return if(!isPvP) {
                when(database.players.size) {
                    1 -> 1
                    in 2..4 -> 2
                    in 5..6 -> 3
                    7 -> 4
                    in 8..9 -> 5
                    in 10..11 -> 6
                    12 -> 7
                    else -> 8
                }
            } else {
                when(database.players.count { a -> a.player.team() == voteTeam }) {
                    1 -> 1
                    in 2..4 -> 2
                    in 5..6 -> 3
                    7 -> 4
                    in 8..9 -> 5
                    in 10..11 -> 6
                    12 -> 7
                    else -> 8
                }
            }
        }

        fun back(map : Map?) {
            Core.app.post {
                val savePath : Fi = saveDirectory.child("rollback.msav")

                try {
                    val mode = state.rules.mode()
                    val reloader = WorldReloader()

                    reloader.begin()

                    if(map != null) {
                        world.loadMap(map, map.applyRules(mode))
                    } else {
                        SaveIO.load(savePath)
                    }

                    state.rules = state.map.applyRules(mode)

                    logic.play()
                    reloader.end()
                } catch(t : Exception) {
                    t.printStackTrace()
                }
                send("command.vote.back.done")
            }
        }

        var colorOffset = 0
        fun nickcolor(name : String, player : Playerc) {
            val stringBuilder = StringBuilder()
            val colors = arrayOfNulls<String>(11)
            colors[0] = "[#ff0000]"
            colors[1] = "[#ff7f00]"
            colors[2] = "[#ffff00]"
            colors[3] = "[#7fff00]"
            colors[4] = "[#00ff00]"
            colors[5] = "[#00ff7f]"
            colors[6] = "[#00ffff]"
            colors[7] = "[#007fff]"
            colors[8] = "[#0000ff]"
            colors[9] = "[#8000ff]"
            colors[10] = "[#ff00ff]"
            val newName = arrayOfNulls<String>(name.length)
            for(i in name.indices) {
                val c = name[i]
                var colorIndex = (i + colorOffset) % colors.size
                if(colorIndex < 0) {
                    colorIndex += colors.size
                }
                val newtext = colors[colorIndex] + c
                newName[i] = newtext
            }
            colorOffset--
            for(s in newName) {
                stringBuilder.append(s)
            }
            player.name(stringBuilder.toString())
        }

        var milsCount = 0
        var secondCount = 0
        var minuteCount = 0

        var rollbackCount = Config.rollbackTime
        var messageCount = Config.messageTime
        var messageOrder = 0

        Events.run(EventType.Trigger.update) {
            if(Config.unbreakableCore) {
                for(a in Groups.build) {
                    when(a.block) {
                        Blocks.coreAcropolis, Blocks.coreBastion, Blocks.coreCitadel, Blocks.coreFoundation, Blocks.coreNucleus, Blocks.coreShard -> {
                            a.health(1.0E8f)
                        }
                    }
                }
            }

            for(a in database.players) {
                if(state.rules.pvp) {
                    if(a.player.team().cores().isEmpty && a.player.team() != Team.derelict && pvpPlayer.contains { b -> b == a.uuid } && !Permission.check(a.player, "pvp.spector")) {
                        val data = findPlayerData(a.uuid)
                        if(data != null) {
                            data.pvplosecount++
                        }
                        a.player.team(Team.derelict)
                        pvpSpectors.add(a.uuid)
                    }
                }

                if(a.status.containsKey("freeze")) {
                    val d = findPlayerData(a.uuid)
                    if(d != null) {
                        val player = d.player
                        val split = a.status.get("freeze").toString().split("/")
                        player.set(split[0].toFloat(), split[1].toFloat())
                        Call.setPosition(player.con(), split[0].toFloat(), split[1].toFloat())
                        Call.setCameraPosition(player.con(), split[0].toFloat(), split[1].toFloat())
                        player.x(split[0].toFloat())
                        player.y(split[1].toFloat())
                    }
                }

                if(a.status.containsKey("tracking")) {
                    for(b in Groups.player) {
                        Call.label(a.player.con(), b.name, Time.delta / 2, b.mouseX, b.mouseY)
                    }
                }

                if(a.status.containsKey("tpp")) {
                    val data = a.status.get("tpp")
                    val target = Groups.player.find { p -> p.uuid() == data }
                    if(target != null) {
                        Call.setCameraPosition(a.player.con(), target.x, target.y)
                    } else {
                        a.status.remove("tpp")
                        Call.setCameraPosition(a.player.con(), a.player.x, a.player.y)
                    }
                }
            }

            if(Config.border) {
                for(a in Groups.unit) {
                    if(a.x > world.width() * 8 || a.x < 0 || a.y > world.height() * 8 || a.y < 0) {
                        a.health(0f)
                    }
                }
            }

            if(Config.moveEffects) {
                if(milsCount == 5) {
                    for(a in database.players) {
                        if(a.player.unit() != null && a.player.unit().health > 0f) {
                            val color = if(a.status.containsKey("effectColor")) {
                                if(Colors.get(a.status.get("effectColor")) != null) Colors.get(a.status.get("effectColor")) else Color.valueOf(a.status.get("effectColor"))
                            } else {
                                when(a.level) {
                                    in 10..19 -> Color.sky
                                    in 20..29 -> Color.orange
                                    in 30..39 -> Color.red
                                    in 40..49 -> Color.sky
                                    in 50..59 -> Color.sky
                                    in 60..69 -> Color.sky
                                    in 70..79 -> Color.orange
                                    in 80..89 -> Color.orange
                                    in 90..99 -> Color.orange
                                    in 100..Int.MAX_VALUE -> Color.orange
                                    else -> Color.orange
                                }
                            }

                            val x = a.player.x
                            val y = a.player.y
                            val rot = a.player.unit().rotation

                            when(if(a.status.containsKey("effectLevel")) a.status.get("effectLevel").toInt() else a.level) {
                                in 10..19 -> Call.effect(Fx.freezing, x, y, rot, color)
                                in 20..29 -> Call.effect(Fx.overdriven, x, y, rot, color)
                                in 30..39 -> {
                                    Call.effect(Fx.burning, x, y, rot, color)
                                    Call.effect(Fx.melting, x, y, rot, color)
                                }

                                in 40..49 -> Call.effect(Fx.steam, x, y, rot, color)
                                in 50..59 -> Call.effect(Fx.shootSmallSmoke, x, y, rot, color)
                                in 60..69 -> Call.effect(Fx.mine, x, y, rot, color)
                                in 70..79 -> Call.effect(Fx.explosion, x, y, rot, color)
                                in 80..89 -> Call.effect(Fx.hitLaser, x, y, rot, color)
                                in 90..99 -> Call.effect(Fx.crawlDust, x, y, rot, color)
                                in 100..Int.MAX_VALUE -> Call.effect(Fx.mineImpact, x, y, rot, color)
                                else -> {}
                            }
                        }
                    }
                    milsCount = 0
                } else {
                    milsCount++
                }
            }

            if(secondCount == 60) {
                PluginData.uptime++
                PluginData.playtime++

                if(voteCooltime > 0) voteCooltime--
                for(a in voterCooltime) {
                    voterCooltime.put(a.key, a.value--)
                    if(a.value == 0) voterCooltime.remove(a.key)
                }

                if(state.isPlaying) PluginData.save()

                for(a in database.players) {
                    a.playtime = a.playtime + 1

                    if(a.colornick) {
                        val name = a.name.replace("\\[(.*?)]".toRegex(), "")
                        nickcolor(name, a.player)
                    } else {
                        a.player.name(a.name)
                    }

                    // 잠수 플레이어 카운트
                    if(Config.afk && a.player.unit() != null && !a.player.unit().moving() && !a.player.unit().mining() && !Permission.check(a.player, "afk.admin")) {
                        a.afkTime++
                        if(a.afkTime == Config.afkTime) {
                            if(Config.afkServer.isEmpty()) {
                                a.player.kick(Bundle(a.languageTag)["event.player.afk"])
                                for(b in database.players) {
                                    b.player.sendMessage(Bundle(b.languageTag)["event.player.afk.other", a.player.plainName()])
                                }
                            } else {
                                val server = Config.afkServer.split(":")
                                val port = if(server.size == 1) {
                                    6567
                                } else {
                                    server[1].toInt()
                                }
                                Call.connect(a.player.con(), server[0], port)
                            }
                        }
                    } else {
                        a.afkTime = 0
                    }

                    a.exp = a.exp + random.nextInt(7)
                    Commands.Exp[a]

                    if(Config.expDisplay) {
                        val message = "${a.exp}/${floor(Commands.Exp.calculateFullTargetXp(a.level)).toInt()}"
                        Call.infoPopup(a.player.con(), message, Time.delta, Align.left, 0, 0, 300, 0)
                    }
                    database.queue(a)
                }

                if(voting) {
                    if(Groups.player.find { a -> a.uuid() == voteStarter!!.uuid() } == null) {
                        send("command.vote.canceled.leave")
                        resetVote()
                    } else {
                        if(count % 10 == 0) {
                            if(isPvP) {
                                for(a in Groups.player) {
                                    if(a.team() == voteTeam) {
                                        val data = findPlayerData(a.uuid())
                                        if(data != null) {
                                            if(voteTargetUUID != data.uuid) {
                                                val bundle = Bundle(data.languageTag)
                                                a.sendMessage(bundle["command.vote.count", count.toString(), check() - voted.size])
                                            }
                                        }
                                    }
                                }
                            } else {
                                send("command.vote.count", count.toString(), check() - voted.size)
                                if(voteType == "kick" && Groups.player.find { a -> a.uuid() == voteTargetUUID } == null) {
                                    send("command.vote.kick.target.leave")
                                }
                            }
                        }
                        count--
                        if((count == 0 && check() <= voted.size) || check() <= voted.size || isAdminVote) {
                            send("command.vote.success")

                            val onlinePlayers = StringBuilder()
                            for(a in database.players) onlinePlayers.append("${a.name}, ")
                            onlinePlayers.substring(0, onlinePlayers.length - 1)

                            voting = false

                            when(voteType) {
                                "kick" -> {
                                    val name = netServer.admins.getInfo(voteTargetUUID).lastName
                                    if(Groups.player.find { a -> a.uuid() == voteTargetUUID } == null) {
                                        netServer.admins.banPlayerID(voteTargetUUID)
                                        send("command.vote.kick.target.banned", name)
                                        if(Config.banChannelToken.isNotEmpty()) Commands.Discord.catnip.rest().channel().createMessage(Config.banChannelToken, Bundle()["event.vote.banned", name, voteReason!!, onlinePlayers.toString()])
                                    } else {
                                        voteTarget?.kick(Packets.KickReason.kick, 60 * 60 * 3000)
                                        send("command.vote.kick.target.kicked", name)
                                        if(Config.banChannelToken.isNotEmpty()) Commands.Discord.catnip.rest().channel().createMessage(Config.banChannelToken, Bundle()["event.vote.kicked", name, voteReason!!, onlinePlayers.toString()])
                                    }
                                }

                                "map" -> {
                                    for(a in database.players) earnEXP(state.rules.waveTeam, a.player, a)
                                    back(voteMap)
                                }

                                "gg" -> {
                                    if(voteStarter != null && !Permission.check(voteStarter!!, "vote.pass")) voterCooltime.put(voteStarter!!.uuid(), 180)
                                    if(isPvP) {
                                        for(a in world.tiles) {
                                            if(a.build != null && a.build.team != null && a.build.team == voteTeam) {
                                                Call.setTile(a, Blocks.air, voteTeam, 0)
                                            }
                                        }
                                    } else {
                                        Events.fire(GameOverEvent(state.rules.waveTeam))
                                    }
                                }

                                "skip" -> {
                                    if(voteStarter != null) voterCooltime.put(voteStarter!!.uuid(), 180)
                                    for(a in 0..voteWave!!) {
                                        spawner.spawnEnemies()
                                        state.wave++
                                        state.wavetime = state.rules.waveSpacing
                                    }
                                    send("command.vote.skip.done", voteWave!!.toString())
                                }

                                "back" -> {
                                    back(null)
                                }

                                "random" -> {
                                    if(lastVoted.plusMinutes(10).isBefore(LocalTime.now())) {
                                        send("command.vote.random.cool")
                                    } else {
                                        if(voteStarter != null) voterCooltime.put(voteStarter!!.uuid(), 420)
                                        lastVoted = LocalTime.now()
                                        send("command.vote.random.done")
                                        Thread {
                                            val map : Map
                                            val random = Random()
                                            send("command.vote.random.is")
                                            Thread.sleep(3000)
                                            when(random.nextInt(7)) {
                                                0 -> {
                                                    send("command.vote.random.unit")
                                                    Groups.unit.each {
                                                        if(voteStarter != null) {
                                                            if(it.team == voteStarter!!.team()) it.kill()
                                                        } else {
                                                            it.kill()
                                                        }
                                                    }
                                                    send("command.vote.random.unit.wave")
                                                    logic.runWave()
                                                }

                                                1 -> {
                                                    send("command.vote.random.wave")
                                                    for(a in 0..5) logic.runWave()
                                                }

                                                2 -> {
                                                    send("command.vote.random.health")
                                                    Groups.build.each {
                                                        if(voteStarter != null) {
                                                            if(it.team == voteStarter!!.team()) {
                                                                it.block.health = it.block.health / 2
                                                            }
                                                        } else {
                                                            it.block.health = it.block.health / 2
                                                        }
                                                    }
                                                    for(a in Groups.player) {
                                                        Call.worldDataBegin(a.con)
                                                        netServer.sendWorldData(a)
                                                    }
                                                }

                                                3 -> {
                                                    send("command.vote.random.fill.core")
                                                    if(voteStarter != null) {
                                                        for(item in content.items()) {
                                                            state.teams.cores(voteStarter!!.team()).first().items.add(item, Random(516).nextInt(500))
                                                        }
                                                    } else {
                                                        for(item in content.items()) {
                                                            state.teams.cores(Team.sharded).first().items.add(item, Random(516).nextInt(500))
                                                        }
                                                    }
                                                }

                                                4 -> {
                                                    send("command.vote.random.storm")
                                                    Thread.sleep(1000)
                                                    Call.createWeather(Weathers.rain, 10f, 60 * 60f, 50f, 10f)
                                                }

                                                5 -> {
                                                    send("command.vote.random.fire")
                                                    for(x in 0 until world.width()) {
                                                        for(y in 0 until world.height()) {
                                                            Call.effect(Fx.fire, (x * 8).toFloat(), (y * 8).toFloat(), 0f, Color.red)
                                                        }
                                                    }
                                                    var tick = 600
                                                    map = state.map

                                                    while(tick != 0 && map == state.map) {
                                                        Thread.sleep(1000)
                                                        tick--
                                                        Core.app.post {
                                                            Groups.unit.each {
                                                                it.health(it.health() - 10f)
                                                            }
                                                            Groups.build.each {
                                                                it.block.health = it.block.health / 30
                                                            }
                                                        }
                                                        if(tick == 300) {
                                                            send("command.vote.random.supply")
                                                            repeat(2) {
                                                                if(voteStarter != null) {
                                                                    UnitTypes.oct.spawn(voteStarter!!.team(), voteStarter!!.x, voteStarter!!.y)
                                                                } else {
                                                                    UnitTypes.oct.spawn(Team.sharded, state.teams.cores(Team.sharded).first().x, state.teams.cores(Team.sharded).first().y)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                else -> {
                                                    send("command.vote.random.nothing")
                                                }
                                            }
                                        }.start()
                                    }
                                }
                            }

                            resetVote()
                        } else if((count == 0 && check() > voted.size) || isCanceled) {
                            send("command.vote.failed")

                            resetVote()
                        }
                    }
                }

                if(Config.pvpPeace) {
                    if(pvpCount != 0) {
                        pvpCount--
                    } else {
                        state.rules.blockDamageMultiplier = orignalBlockMultiplier
                        state.rules.unitDamageMultiplier = orignalUnitMultiplier
                        send("event.pvp.peace.end")
                    }
                }

                if(Config.banChannelToken.isNotEmpty()) {
                    if(dosBlacklist != netServer.admins.dosBlacklist) {
                        val buffer = dosBlacklist
                        for(a in dosBlacklist) {
                            for(b in netServer.admins.dosBlacklist) {
                                if(a == b) {
                                    buffer.remove(a)
                                }
                            }
                        }
                        for(a in buffer) {
                            Commands.Discord.catnip.rest().channel().createMessage(Config.banChannelToken, Bundle()["event.discord.dos"])
                        }
                    }
                }

                secondCount = 0
            } else {
                secondCount++
            }

            if(minuteCount == 3600) {
                if(state.rules.pvp) {
                    for(a in database.players) {
                        if(!pvpPlayer.contains { b -> b == a.uuid }) {
                            pvpPlayer.add(a.uuid)
                        }
                    }
                }

                Main.daemon.submit(Thread {
                    val data = database.getAll()

                    for(a in data) {
                        if(a.status.containsKey("ban") && LocalDateTime.now().isAfter(LocalDateTime.parse(a.status.get("ban")))) {
                            Core.app.post { netServer.admins.unbanPlayerID(a.uuid) }
                            a.status.removeAll { b -> b.key == "ban" }
                            if(Config.banChannelToken.isNotEmpty()) {
                                Commands.Discord.catnip.rest().channel().createMessage(Config.banChannelToken, Bundle()["event.tempban.unbanned", a.name])
                            }
                        }
                    }
                })

                if(rollbackCount == 0) {
                    SaveIO.save(saveDirectory.child("rollback.msav"))
                    rollbackCount = Config.rollbackTime
                } else {
                    rollbackCount--
                }

                if(Config.message) {
                    if(messageCount == Config.messageTime) {
                        for(a in database.players) {
                            val message = if(Main.root.child("messages/${a.languageTag}.txt").exists()) {
                                Main.root.child("messages/${a.languageTag}.txt").readString()
                            } else {
                                val file = Main.root.child("messages/en.txt")
                                if(file.exists()) file.readString() else ""
                            }
                            val c = message.split(Regex("\r\n"))

                            if(c.size <= messageOrder) {
                                messageOrder = 0
                            }
                            a.player.sendMessage(c[messageOrder])

                        }
                        messageOrder++
                        messageCount = 0
                    } else {
                        messageCount++
                    }
                }
                minuteCount = 0
            } else {
                minuteCount++
            }
        }
    }

    fun log(type : LogType, text : String, vararg name : String) {
        val root : Fi = Core.settings.dataDirectory.child("mods/Essentials/")
        val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        if(type != LogType.Report) {
            val date = DateTimeFormatter.ofPattern("yyyy-MM-dd HH_mm_ss").format(LocalDateTime.now())
            val new = Paths.get(root.child("log/$type.log").path())
            val old = Paths.get(root.child("log/old/$type/$date.log").path())
            var main = root.child("log/$type.log")
            val folder = root.child("log")

            if(main != null && main.length() > 2048 * 256) {
                main.writeString("end of file. $date", true)
                try {
                    if(!root.child("log/old/$type").exists()) {
                        root.child("log/old/$type").mkdirs()
                    }
                    Files.move(new, old, StandardCopyOption.REPLACE_EXISTING)
                } catch(e : IOException) {
                    e.printStackTrace()
                }
                main = null
            }
            if(main == null) main = folder.child("$type.log")
            main!!.writeString("[$time] $text\n", true)
        } else {
            val main = root.child("log/report/$time $name.txt")
            main.writeString(text)
        }
    }

    enum class LogType {
        Player, Tap, WithDraw, Block, Deposit, Chat, Report
    }

    class IpAddressMatcher(ipAddress : String) {
        private var nMaskBits = 0
        private val requiredAddress : InetAddress
        fun matches(address : String) : Boolean {
            val remoteAddress = parseAddress(address)
            if(requiredAddress.javaClass != remoteAddress.javaClass) {
                return false
            }
            if(nMaskBits < 0) {
                return remoteAddress == requiredAddress
            }
            val remAddr = remoteAddress.address
            val reqAddr = requiredAddress.address
            val nMaskFullBytes = nMaskBits / 8
            val finalByte = (0xFF00 shr (nMaskBits and 0x07)).toByte()
            for(i in 0 until nMaskFullBytes) {
                if(remAddr[i] != reqAddr[i]) {
                    return false
                }
            }
            return if(finalByte.toInt() != 0) {
                remAddr[nMaskFullBytes] and finalByte == reqAddr[nMaskFullBytes] and finalByte
            } else true
        }

        private fun parseAddress(address : String) : InetAddress {
            return try {
                InetAddress.getByName(address)
            } catch(e : UnknownHostException) {
                throw IllegalArgumentException("Failed to parse address$address", e)
            }
        }

        init {
            var address = ipAddress
            if(address.indexOf('/') > 0) {
                val addressAndMask = address.split("/").toTypedArray()
                address = addressAndMask[0]
                nMaskBits = addressAndMask[1].toInt()
            } else {
                nMaskBits = -1
            }
            requiredAddress = parseAddress(address)
            assert(requiredAddress.address.size * 8 >= nMaskBits) {
                String.format("IP address %s is too short for bitmask of length %d", address, nMaskBits)
            }
        }
    }

    fun earnEXP(winner : Team, p : Playerc, target : DB.PlayerData) {
        val oldLevel = target.level
        val oldExp = target.exp
        val time = PluginData.playtime.toInt()
        var blockexp = 0

        for(a in state.stats.placedBlockCount) {
            blockexp += blockExp[a.key.name]
        }

        val bundle = Bundle(target.languageTag)
        var coreitem = 0
        for(a in state.stats.coreItemCount) coreitem += a.value

        val erekirAttack = if (state.planet == Planets.erekir) state.stats.enemyUnitsDestroyed + coreitem else 0
        val erekirPvP = if (state.planet == Planets.erekir) coreitem else 0

        if(winner == p.team()) {
            val score : Int = if(state.rules.attackMode) {
                (time + blockexp + enemyBuildingDestroyed + erekirAttack) - (state.stats.buildingsDeconstructed + state.stats.buildingsDestroyed)
            } else if(state.rules.pvp) {
                time + if(state.planet == Planets.erekir) coreitem else 0 + 5000
            } else {
                0
            }

            target.exp = target.exp + score
            p.sendMessage(bundle["event.exp.earn.victory", score])
        } else {
            val score : Int = if(state.rules.attackMode) {
                time - (state.stats.buildingsDeconstructed + state.stats.buildingsDestroyed)
            } else if(state.rules.waves) {
                state.wave * 150
            } else if(state.rules.pvp) {
                time + 5000
            } else {
                0
            }

            val message = if(state.rules.attackMode) {
                bundle["event.exp.earn.defeat", score, (time + blockexp + enemyBuildingDestroyed + erekirAttack) - (state.stats.buildingsDeconstructed + state.stats.buildingsDestroyed)]
            } else if(state.rules.waves) {
                bundle["event.exp.earn.wave", score, state.wave]
            } else if(state.rules.pvp) {
                bundle["event.exp.earn.defeat", score, (time + erekirPvP + 5000)]
            } else {
                ""
            }

            target.exp = target.exp + score
            p.sendMessage(message)

            if(score < 0) {
                p.sendMessage(bundle["event.exp.lost.reason"])
                p.sendMessage(bundle["event.exp.lost.result", time, blockexp, enemyBuildingDestroyed, (state.stats.buildingsDeconstructed + state.stats.buildingsDestroyed)])
            }
        }

        Commands.Exp[target]
        p.sendMessage(bundle["event.exp.current", target.exp, (if(target.exp > oldExp) "+" else "-") + (target.exp - oldExp), target.level, (if(target.level > oldLevel) "+" else if(target.level == oldLevel) "" else "-") + (target.level - oldLevel)])
    }

    fun findPlayerData(uuid : String) : DB.PlayerData? {
        return database.players.find { e -> e.uuid == uuid }
    }

    fun findPlayers(name : String) : Playerc? {
        if(name.toIntOrNull() != null) {
            for(a in database.players) {
                if(a.entityid == name.toInt()) {
                    return a.player
                }
            }
            return Groups.player.find { p -> p.plainName().contains(name, true) }
        } else {
            return Groups.player.find { p -> p.plainName().contains(name, true) }
        }
    }

    fun findPlayersByName(name : String) : PlayerInfo? {
        return if(!netServer.admins.findByName(name).isEmpty) {
            netServer.admins.findByName(name).first()
        } else {
            null
        }
    }

    private fun resetVote() {
        voting = false
        voteType = null
        voteTarget = null
        voteTargetUUID = null
        voteReason = null
        voteMap = null
        voteWave = null
        voteStarter = null
        isCanceled = false
        isAdminVote = false
        isPvP = false
        voteTeam = state.rules.defaultTeam
        voted = Seq<String>()
        count = 60
    }

    private fun addLog(log : TileLog) {
        worldHistory.add(log)
    }

    private fun addLog(log : PlayerLog) {
        playerHistory.add(log)
    }

    class TileLog(val time : Long, val player : String, val action : String, val x : Short, val y : Short, val tile : String, val rotate : Int, val team : Team)
    class PlayerLog(val player : String, val block : Block, val x : Short, val y : Short, val config : ObjectMap<Class<*>, Cons2<Any, Any>>?)
}