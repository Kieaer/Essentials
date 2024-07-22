package essential.core

import arc.ApplicationListener
import arc.Core
import arc.Events
import arc.files.Fi
import arc.func.Cons
import arc.graphics.Color
import arc.graphics.Colors
import arc.util.*
import essential.core.Main.Companion.conf
import essential.core.Main.Companion.currentTime
import essential.core.Main.Companion.database
import essential.core.Main.Companion.root
import mindustry.Vars
import mindustry.content.*
import mindustry.entities.Effect
import mindustry.game.EventType.*
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.gen.Playerc
import mindustry.io.SaveIO
import mindustry.maps.Map
import mindustry.net.Administration
import mindustry.net.Packets
import mindustry.net.WorldReloader
import mindustry.ui.Menus
import mindustry.world.Tile
import mindustry.world.blocks.ConstructBlock
import org.hjson.JsonArray
import org.hjson.JsonObject
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.random.RandomGenerator
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.Path
import kotlin.math.floor
import kotlin.random.Random


object Event {
    var originalBlockMultiplier = 1f
    var originalUnitMultiplier = 1f

    var voting = false
    var voteType: String? = null
    var voteTarget: Playerc? = null
    var voteTargetUUID: String? = null
    var voteReason: String? = null
    var voteMap: Map? = null
    var voteWave: Int? = null
    var voteStarter: DB.PlayerData? = null
    var isPvP: Boolean = false
    var voteTeam: Team = Vars.state.rules.defaultTeam
    var voteCooltime: Int = 0
    var voted = ArrayList<String>()
    var lastVoted: LocalTime? = LocalTime.now()
    var isAdminVote = false
    var isCanceled = false

    var worldHistory = ArrayList<TileLog>()
    var voterCooltime = HashMap<String, Int>()

    private var random = RandomGenerator.of("Random")
    private var dateformat = SimpleDateFormat("HH:mm:ss")
    var blockExp = HashMap<String, Int>()
    var dosBlacklist : List<String> = listOf()
    var count = 60
    var pvpSpecters = ArrayList<String>()
    var pvpPlayer = HashMap<String, Team>()
    var isGlobalMute = false
    var dpsBlocks = 0f
    var dpsTile: Tile? = null
    var maxdps: Float? = null
    var unitLimitMessageCooldown = 0
    var offlinePlayers = ArrayList<DB.PlayerData>()
    var apmRanking = ""

    val eventListeners: HashMap<Class<*>, Cons<*>> = hashMapOf()
    val coreListeners: ArrayList<ApplicationListener> = arrayListOf()
    lateinit var actionFilter: Administration.ActionFilter

    private val blockSelectRegex: Pattern = Pattern.compile("^build\\d{1,2}\$")

    fun register() {
        fun checkValidBlock(tile: Tile): String {
            return if (tile.build != null && blockSelectRegex.matcher(tile.block().name).matches()) {
                (tile.build as ConstructBlock.ConstructBuild).current.name
            } else {
                tile.block().name
            }
        }

        Events.on(WithdrawEvent::class.java, Cons<WithdrawEvent> {
            if (it.tile != null && it.player.unit().item() != null && it.player.name != null) {
                log(
                    LogType.WithDraw,
                    Bundle()["log.withdraw", it.player.plainName(), it.player.unit()
                        .item().name, it.amount, it.tile.block.name, it.tile.tileX(), it.tile.tileY()]
                )
                addLog(
                    TileLog(
                        System.currentTimeMillis(),
                        it.player.name,
                        "withdraw",
                        it.tile.tile.x,
                        it.tile.tile.y,
                        checkValidBlock(it.tile.tile),
                        it.tile.rotation,
                        it.tile.team,
                        it.tile.config()
                    )
                )
                val p = findPlayerData(it.player.uuid())
                if (p != null) {
                    p.currentControlCount++
                }
            }
        }.also { listener -> eventListeners[WithdrawEvent::class.java] = listener })

        Events.on(DepositEvent::class.java, Cons<DepositEvent> {
            if (it.tile != null && it.player.unit().item() != null && it.player.name != null) {
                log(
                    LogType.Deposit,
                    Bundle()["log.deposit", it.player.plainName(), it.player.unit()
                        .item().name, it.amount, checkValidBlock(it.tile.tile), it.tile.tileX(), it.tile.tileY()]
                )
                addLog(
                    TileLog(
                        System.currentTimeMillis(),
                        it.player.name,
                        "deposit",
                        it.tile.tile.x,
                        it.tile.tile.y,
                        checkValidBlock(it.tile.tile),
                        it.tile.rotation,
                        it.tile.team,
                        it.tile.config()
                    )
                )
                val p = findPlayerData(it.player.uuid())
                if (p != null) {
                    p.currentControlCount++
                }
            }
        }.also { listener -> eventListeners[DepositEvent::class.java] = listener })

        Events.on(ConfigEvent::class.java, Cons<ConfigEvent> {
            if (it.tile != null && it.tile.block() != null && it.player != null) {
                addLog(
                    TileLog(
                        System.currentTimeMillis(),
                        it.player.name,
                        "config",
                        it.tile.tile.x,
                        it.tile.tile.y,
                        checkValidBlock(it.tile.tile),
                        it.tile.rotation,
                        it.tile.team,
                        it.value
                    )
                )
                if (checkValidBlock(it.tile.tile).contains("message", true)) {
                    addLog(
                        TileLog(
                            System.currentTimeMillis(),
                            it.player.name,
                            "message",
                            it.tile.tile.x,
                            it.tile.tile.y,
                            checkValidBlock(it.tile.tile),
                            it.tile.rotation,
                            it.tile.team,
                            it.value
                        )
                    )
                }

                val p = findPlayerData(it.player.uuid())
                if (p != null) {
                    p.currentControlCount++
                }
            }
        }.also { listener -> eventListeners[ConfigEvent::class.java] = listener })

        Events.on(TapEvent::class.java, Cons<TapEvent> {
            log(LogType.Tap, Bundle()["log.tap", it.player.plainName(), checkValidBlock(it.tile)])
            addLog(
                TileLog(
                    System.currentTimeMillis(),
                    it.player.name,
                    "tap",
                    it.tile.x,
                    it.tile.y,
                    checkValidBlock(it.tile),
                    if (it.tile.build != null) it.tile.build.rotation else 0,
                    if (it.tile.build != null) it.tile.build.team else Vars.state.rules.defaultTeam,
                    null
                )
            )
            val data = findPlayerData(it.player.uuid())
            if (data != null) {
                PluginData.warpBlocks.forEach { two ->
                    if (two.mapName == Vars.state.map.name() && it.tile.block().name == two.tileName && it.tile.build.tileX() == two.x && it.tile.build.tileY() == two.y) {
                        if (two.online) {
                            database.players.forEach { data ->
                                data.send("event.tap.server", it.player.plainName(), two.description)
                            }
                            // why?
                            val format = NumberFormat.getNumberInstance(Locale.US)
                            format.isGroupingUsed = false

                            Log.info(
                                Bundle()["log.warp.move.block", it.player.plainName(), Strings.stripColors(two.description), two.ip, format.format(
                                    two.port
                                )]
                            )
                            Call.connect(it.player.con(), two.ip, two.port)
                        }
                        return@forEach
                    }
                }

                for (two in PluginData.warpZones) {
                    if (two.mapName == Vars.state.map.name() && two.click && isUnitInside(it.tile, two.startTile, two.finishTile)) {
                        Log.info(Bundle()["log.warp.move", it.player.plainName(), two.ip, two.port.toString()])
                        Call.connect(it.player.con(), two.ip, two.port)
                        continue
                    }
                }

                if (data.log) {
                    val buf = ArrayList<TileLog>()
                    worldHistory.forEach { two ->
                        if (two.x == it.tile.x && two.y == it.tile.y) {
                            buf.add(two)
                        }
                    }
                    val str = StringBuilder()
                    val bundle = if (data.status.containsKey("language")) {
                        Bundle(data.status["language"]!!)
                    } else {
                        Bundle(it.player.locale())
                    }
                    val handle = Core.files.internal("bundles/bundle")
                    val coreBundle = I18NBundle.createBundle(handle, Locale(data.languageTag))

                    buf.forEach { two ->
                        val action = when (two.action) {
                            "tap" -> "[royal]${bundle["event.log.tap"]}[]"
                            "break" -> "[scarlet]${bundle["event.log.break"]}[]"
                            "place" -> "[sky]${bundle["event.log.place"]}[]"
                            "config" -> "[cyan]${bundle["event.log.config"]}[]"
                            "withdraw" -> "[green]${bundle["event.log.withdraw"]}[]"
                            "deposit" -> "[brown]${bundle["event.log.deposit"]}[]"
                            "message" -> "[orange]${bundle["event.log.message"]}"
                            else -> ""
                        }

                        if (two.action == "message") {
                            str.append(
                                bundle["event.log.format.message", dateformat.format(two.time), two.player, coreBundle.get(
                                    "block.${two.tile}.name"
                                ), two.value as String]
                            ).append("\n")
                        } else {
                            str.append(
                                bundle["event.log.format", dateformat.format(two.time), two.player, coreBundle.get(
                                    "block.${two.tile}.name"
                                ), action]
                            ).append("\n")
                        }
                    }

                    Call.effect(it.player.con(), Fx.shockwave, it.tile.getX(), it.tile.getY(), 0f, Color.cyan)
                    val str2 = StringBuilder()
                    if (str.toString().lines().size > 10) {
                        val lines: List<String> = str.toString().split("\n").reversed()
                        for (i in 0 until 10) {
                            str2.append(lines[i]).append("\n")
                        }
                        it.player.sendMessage(str2.toString())
                    } else {
                        it.player.sendMessage(str.toString())
                    }
                }

                if (data.status.containsKey("hub_first") && !data.status.containsKey("hub_second")) {
                    data.status["hub_first"] = "${it.tile.x},${it.tile.y}"
                    data.status["hub_second"] = "true"
                    data.player.sendMessage(Bundle(data.languageTag)["command.sb.zone.next", "${it.tile.x},${it.tile.y}"])
                } else if (data.status.containsKey("hub_first") && data.status.containsKey("hub_second")) {
                    val x = data.status["hub_first"]!!.split(",")[0].toInt()
                    val y = data.status["hub_first"]!!.split(",")[1].toInt()
                    val ip = data.status["hub_ip"]!!
                    val port = data.status["hub_port"]!!.toInt()

                    val bundle = if (data.status.containsKey("language")) {
                        Bundle(data.status["language"]!!)
                    } else {
                        Bundle(it.player.locale())
                    }
                    val options = arrayOf(arrayOf(bundle["command.sb.zone.yes"], bundle["command.sb.zone.no"]))
                    val menu = Menus.registerMenu { player, option ->
                        val touch = when (option) {
                            0 -> true
                            else -> false
                        }
                        PluginData.warpZones.add(
                            PluginData.WarpZone(
                                Vars.state.map.plainName(),
                                Vars.world.tile(x, y).pos(),
                                it.tile.pos(),
                                touch,
                                ip,
                                port
                            )
                        )
                        player.sendMessage(bundle["command.sb.zone.added", "$x:$y", ip, if (touch) bundle["command.sb.zone.clickable"] else bundle["command.sb.zone.enter"]])
                        PluginData.save(false)
                    }

                    Call.menu(
                        data.player.con(),
                        menu,
                        bundle["command.sb.zone.title"],
                        bundle["command.sb.zone.message"],
                        options
                    )

                    data.status.remove("hub_first")
                    data.status.remove("hub_second")
                    data.status.remove("hub_ip")
                    data.status.remove("hub_port")

                }

                database.players.forEach { two ->
                    if (two.tracking) {
                        Call.effect(two.player.con(), Fx.bigShockwave, it.tile.getX(), it.tile.getY(), 0f, Color.cyan)
                    }
                }

                data.currentControlCount++
            }
        }.also { listener -> eventListeners[TapEvent::class.java] = listener })

        Events.on(PickupEvent::class.java, Cons<PickupEvent> {
            if (it.unit != null && it.unit.isPlayer) {
                val p = findPlayerData(it.unit.player.uuid())
                if (p != null) {
                    p.currentControlCount++
                }
            }
        }.also { listener -> eventListeners[PickupEvent::class.java] = listener })

        Events.on(UnitControlEvent::class.java, Cons<UnitControlEvent> {
            val p = findPlayerData(it.player.uuid())
            if (p != null) {
                p.currentControlCount++
            }
        }.also { listener -> eventListeners[UnitControlEvent::class.java] = listener })

        Events.on(BuildingCommandEvent::class.java, Cons<BuildingCommandEvent> {
            val p = findPlayerData(it.player.uuid())
            if (p != null) {
                p.currentControlCount++
            }
        }.also { listener -> eventListeners[BuildingCommandEvent::class.java] = listener })

        Events.on(WaveEvent::class.java, Cons<WaveEvent> {
            for (data in database.players) {
                data.exp += 500
            }

            if (conf.feature.game.wave.autoSkip > 1) {
                var loop = 1
                while (conf.feature.game.wave.autoSkip != loop) {
                    loop++
                    Vars.spawner.spawnEnemies()
                    Vars.state.wave++
                    Vars.state.wavetime = Vars.state.rules.waveSpacing
                }
            }
        }.also { listener -> eventListeners[WaveEvent::class.java] = listener })

        Events.on(ServerLoadEvent::class.java, Cons<ServerLoadEvent> {
            Vars.content.blocks().each { two ->
                var buf = 0
                two.requirements.forEach { item ->
                    buf += item.amount
                }
                blockExp.put(two.name, buf)
            }

            dosBlacklist = Vars.netServer.admins.dosBlacklist.toList()

            Vars.netServer.admins.addChatFilter(Administration.ChatFilter { player, message ->
                log(LogType.Chat, "${player.plainName()}: $message")
                return@ChatFilter if (!message.startsWith("/")) {
                    val data = findPlayerData(player.uuid())
                    if (data != null) {
                        if (!data.mute) {
                            val isAdmin = Permission.check(data, "vote.pass")
                            if (voting && message.equals("y", true) && !voted.contains(player.uuid())) {
                                if (voteStarter != data) {
                                    if (Vars.state.rules.pvp && voteTeam == player.team()) {
                                        voted.add(player.uuid())
                                    } else if (!Vars.state.rules.pvp) {
                                        voted.add(player.uuid())
                                    }
                                } else if (isAdmin) {
                                    isAdminVote = true
                                }
                                data.send("command.vote.voted")
                            } else if (voting && message.equals("n", true) && isAdmin) {
                                isCanceled = true
                            }

                            if (isGlobalMute && Permission.check(data, "chat.admin")) {
                                message
                            } else if (!isGlobalMute && !(voting && message.contains("y", true))) {
                                message
                            } else {
                                null
                            }
                        } else {
                            message
                        }
                    } else {
                        message
                    }
                } else {
                    null
                }
            })

            if (!Vars.mods.list().contains { mod -> mod.name == "essential-protect" }) {
                Events.on(PlayerJoin::class.java, Cons<PlayerJoin> {
                    it.player.admin(false)

                    val data = database[it.player.uuid()]
                    val trigger = Trigger()
                    if (data == null) {
                        Main.daemon.submit(Thread {
                            transaction {
                                if (DB.Player.select(DB.Player.name).where { DB.Player.name eq it.player.name }
                                        .empty()) {
                                    Core.app.post { trigger.createPlayer(it.player, null, null) }
                                } else {
                                    Core.app.post {
                                        it.player.con.kick(
                                            Bundle(it.player.locale)["event.player.name.duplicate"],
                                            0L
                                        )
                                    }
                                }
                            }
                        })
                    } else {
                        trigger.loadPlayer(it.player, data, false)
                    }
                }.also { listener -> eventListeners[PlayerJoin::class.java] = listener })
            }
        }.also { listener -> eventListeners[ServerLoadEvent::class.java] = listener })

        Events.on(PlayerChatEvent::class.java, Cons<PlayerChatEvent> {

        }.also { listener -> eventListeners[PlayerChatEvent::class.java] = listener })

        Events.on(GameOverEvent::class.java, Cons<GameOverEvent> {
            if (voting) {
                database.players.forEach { a ->
                    if (voteTargetUUID != a.uuid) a.player.sendMessage(Bundle(a.languageTag)["command.vote.canceled"])
                }
                resetVote()
            }

            if (!Vars.state.rules.infiniteResources) {
                if (Vars.state.rules.pvp) {
                    for (data in database.players) {
                        if (data.player.team() == it.winner) {
                            data.pvpVictoriesCount++
                        }
                    }
                } else if (Vars.state.rules.attackMode) {
                    for (data in database.players) {
                        if (data.player.team() == it.winner) {
                            data.attackModeClear++
                        }
                    }
                }
                for (data in database.players) {
                    earnEXP(it.winner, data.player, data, true)
                }
                for (data in offlinePlayers) {
                    earnEXP(it.winner, data.player, data, false)
                }
            }
            if (voting) resetVote()
            offlinePlayers.clear()
            worldHistory.clear()
            pvpSpecters.clear()
            pvpPlayer.clear()
            dpsTile = null
        }.also { listener -> eventListeners[GameOverEvent::class.java] = listener })

        Events.on(BlockBuildBeginEvent::class.java, Cons<BlockBuildBeginEvent> {

        }.also { listener -> eventListeners[BlockBuildEndEvent::class.java] = listener })

        Events.on(BlockBuildEndEvent::class.java, Cons<BlockBuildEndEvent> {
            val isDebug = Core.settings.getBool("debugMode")

            if (it.unit.isPlayer) {
                val player = it.unit.player
                val target = findPlayerData(player.uuid())

                if (!player.unit().isNull && target != null && it.tile.block() != null && player.unit()
                        .buildPlan() != null
                ) {
                    val block = it.tile.block()
                    if (!it.breaking) {
                        log(
                            LogType.Block,
                            Bundle()["log.block.place", target.name, checkValidBlock(it.tile), it.tile.x, it.tile.y]
                        )

                        val buf = ArrayList<TileLog>()
                        worldHistory.forEach { two ->
                            if (two.x == it.tile.x && two.y == it.tile.y) {
                                buf.add(two)
                            }
                        }

                        if (!Vars.state.rules.infiniteResources && it.tile != null && it.tile.build != null && it.tile.build.maxHealth() == it.tile.block().health.toFloat() && (!buf.isEmpty() && buf.last().tile != it.tile.block().name)) {
                            target.blockPlaceCount++
                            target.exp += blockExp[block.name]!!
                            target.currentExp += blockExp[block.name]!!
                        }

                        addLog(
                            TileLog(
                                System.currentTimeMillis(),
                                target.name,
                                "place",
                                it.tile.x,
                                it.tile.y,
                                checkValidBlock(it.tile),
                                if (it.tile.build != null) it.tile.build.rotation else 0,
                                if (it.tile.build != null) it.tile.build.team else Vars.state.rules.defaultTeam,
                                it.config
                            )
                        )

                        if (isDebug) {
                            Log.info("${player.name} placed ${it.tile.block().name} to ${it.tile.x},${it.tile.y}")
                        }
                    } else if (it.breaking) {
                        log(
                            LogType.Block,
                            Bundle()["log.block.break", target.name, checkValidBlock(it.tile), it.tile.x, it.tile.y]
                        )
                        addLog(
                            TileLog(
                                System.currentTimeMillis(),
                                target.name,
                                "break",
                                it.tile.x,
                                it.tile.y,
                                checkValidBlock(player.unit().buildPlan().tile()),
                                if (it.tile.build != null) it.tile.build.rotation else 0,
                                if (it.tile.build != null) it.tile.build.team else Vars.state.rules.defaultTeam,
                                it.config
                            )
                        )

                        if (!Vars.state.rules.infiniteResources) {
                            target.blockBreakCount++
                            target.exp -= blockExp[player.unit().buildPlan().block.name]!!
                            target.currentExp -= blockExp[player.unit().buildPlan().block.name]!!
                        }
                    }

                    target.currentControlCount++
                }
            }
        }.also { listener -> eventListeners[BlockBuildEndEvent::class.java] = listener })

        Events.on(BuildSelectEvent::class.java, Cons<BuildSelectEvent> {
            if (it.builder is Playerc && it.builder.buildPlan() != null && it.tile.block() !== Blocks.air && it.breaking) {
                log(
                    LogType.Block,
                    Bundle()["log.block.remove", (it.builder as Playerc).plainName(), checkValidBlock(it.tile), it.tile.x, it.tile.y]
                )
                addLog(
                    TileLog(
                        System.currentTimeMillis(),
                        (it.builder as Playerc).plainName(),
                        "select",
                        it.tile.x,
                        it.tile.y,
                        checkValidBlock(Vars.player.unit().buildPlan().tile()),
                        if (it.tile.build != null) it.tile.build.rotation else 0,
                        if (it.tile.build != null) it.tile.build.team else Vars.state.rules.defaultTeam,
                        it.tile.build.config()
                    )
                )
                val p = findPlayerData((it.builder as Playerc).uuid())
                if (p != null) {
                    p.currentControlCount++
                }
            }
        }.also { listener -> eventListeners[BuildSelectEvent::class.java] = listener })

        Events.on(BlockDestroyEvent::class.java, Cons<BlockDestroyEvent> {
            if (Vars.state.rules.attackMode) {
                for (a in database.players) {
                    if (it.tile.team() != Vars.state.rules.defaultTeam) {
                        a.currentBuildAttackCount++
                    } else {
                        a.currentBuildDestroyedCount++
                    }
                }
            }
        }.also { listener -> eventListeners[BlockDestroyEvent::class.java] = listener })

        Events.on(UnitDestroyEvent::class.java, Cons<UnitDestroyEvent> {
            if (!Vars.state.rules.pvp) {
                for (a in database.players) {
                    if (it.unit.team() != a.player.team()) {
                        a.currentUnitDestroyedCount++
                    }
                }
            }
        }.also { listener -> eventListeners[UnitDestroyEvent::class.java] = listener })

        Events.on(UnitCreateEvent::class.java, Cons<UnitCreateEvent> { u ->
            if (conf.feature.unit.enabled && Groups.unit.size() > conf.feature.unit.limit) {
                u.unit.kill()

                if (unitLimitMessageCooldown == 0) {
                    database.players.forEach {
                        it.send("config.spawnlimit.reach", "[scarlet]${Groups.unit.size()}[white]/[sky]${conf.feature.unit.limit}")
                    }
                    unitLimitMessageCooldown = 60
                }
            }
        }.also { listener -> eventListeners[UnitCreateEvent::class.java] = listener })

        Events.on(UnitChangeEvent::class.java, Cons<UnitChangeEvent> {
            val p = findPlayerData(it.player.uuid())
            if (p != null) {
                p.currentControlCount++
            }
        }.also { listener -> eventListeners[UnitChangeEvent::class.java] = listener })

        Events.on(PlayerJoin::class.java, Cons<PlayerJoin> {
            log(LogType.Player, Bundle()["log.joined", it.player.plainName(), it.player.uuid(), it.player.con.address])
        }.also { listener -> eventListeners[PlayerJoin::class.java] = listener })

        Events.on(PlayerLeave::class.java, Cons<PlayerLeave> {
            log(
                LogType.Player,
                Bundle()["log.player.disconnect", it.player.plainName(), it.player.uuid(), it.player.con.address]
            )
            val data = database.players.find { e -> e.uuid == it.player.uuid() }
            if (data != null) {
                data.lastPlayedWorldName = Vars.state.map.plainName()
                data.lastPlayedWorldMode = Vars.state.rules.modeName
                data.lastPlayedWorldId = Vars.port
                data.lastLeaveDate = LocalDateTime.now()
                data.isConnected = false

                if (data.oldUUID != null) {
                    data.uuid = data.oldUUID!!
                    data.oldUUID = null
                }

                database.queue(data)
                offlinePlayers.add(data)

                if (database.players.size == 0 && pvpSpecters.isNotEmpty()) {
                    Events.fire(GameOverEvent(data.player.team()))
                }

                database.players.removeAll { e -> e.uuid == data.uuid }
            }
        }.also { listener -> eventListeners[PlayerLeave::class.java] = listener })

        Events.on(PlayerBanEvent::class.java, Cons<PlayerBanEvent> {
            log(
                LogType.Player,
                Bundle()["log.player.banned", Vars.netServer.admins.getInfo(it.uuid).ips.first(), Vars.netServer.admins.getInfo(
                    it.uuid
                ).names.first()]
            )
        }.also { listener -> eventListeners[PlayerBanEvent::class.java] = listener })

        Events.on(PlayerUnbanEvent::class.java, Cons<PlayerUnbanEvent> {
            Events.fire(CustomEvents.PlayerUnbanned(Vars.netServer.admins.getInfo(it.uuid).lastName, currentTime()))
        }.also { listener -> eventListeners[PlayerUnbanEvent::class.java] = listener })

        Events.on(PlayerIpUnbanEvent::class.java, Cons<PlayerIpUnbanEvent> {
            Events.fire(CustomEvents.PlayerUnbanned(Vars.netServer.admins.findByIP(it.ip).lastName, currentTime()))
        }.also { listener -> eventListeners[PlayerIpUnbanEvent::class.java] = listener })

        Events.on(WorldLoadEvent::class.java, Cons<WorldLoadEvent> {
            PluginData.playtime = 0L
            PluginData.isSurrender = false
            PluginData.isCheated = false
            dpsTile = null
            if (Vars.saveDirectory.child("rollback.msav").exists()) Vars.saveDirectory.child("rollback.msav").delete()

            if (Vars.state.rules.pvp) {
                pvpSpecters.clear()

                for (data in database.players) {
                    if (Permission.check(data, "pvp.spector")) {
                        data.player.team(Team.derelict)
                    }
                }
            }

            for (data in database.players) {
                data.currentPlayTime = 0
                data.currentUnitDestroyedCount = 0
                data.currentBuildDestroyedCount = 0
                data.currentBuildAttackCount = 0
                data.currentControlCount = 0
                data.currentBuildDeconstructedCount = 0
            }

            resetVote()
        }.also { listener -> eventListeners[WorldLoadEvent::class.java] = listener })

        Events.on(ConnectPacketEvent::class.java, Cons<ConnectPacketEvent> {
            if (conf.feature.blacklist.enabled) {
                PluginData.blacklist.forEach { text ->
                    val pattern = Regex(text)
                    if ((conf.feature.blacklist.regex && pattern.matches(it.packet.name)) ||
                        !conf.feature.blacklist.regex && it.packet.name.contains(text)
                    ) {
                        it.connection.kick(Bundle(it.packet.locale)["event.player.name.blacklisted"], 0L)
                        log(
                            LogType.Player,
                            Bundle()["event.player.kick", it.packet.name, it.packet.uuid, it.connection.address, Bundle()["event.player.kick.reason.blacklisted"]]
                        )
                        Events.fire(
                            CustomEvents.PlayerConnectKicked(
                                it.packet.name,
                                Bundle()["event.player.kick.reason.blacklisted"]
                            )
                        )
                        return@forEach
                    }
                }
            }
        }.also { listener -> eventListeners[ConnectPacketEvent::class.java] = listener })

        Events.on(PlayerConnect::class.java, Cons<PlayerConnect> {
            log(
                LogType.Player,
                Bundle()["event.player.connected", it.player.plainName(), it.player.uuid(), it.player.con.address]
            )
        }.also { listener -> eventListeners[PlayerConnect::class.java] = listener })

        Events.on(BuildingBulletDestroyEvent::class.java, Cons<BuildingBulletDestroyEvent> {
            val cores = listOf(
                Blocks.coreAcropolis,
                Blocks.coreBastion,
                Blocks.coreCitadel,
                Blocks.coreFoundation,
                Blocks.coreAcropolis,
                Blocks.coreNucleus,
                Blocks.coreShard
            )
            if (Vars.state.rules.pvp && it.build.closestCore() == null && cores.contains(it.build.block())) {
                for (data in database.players) {
                    if (data.player.team() == it.bullet.team) {
                        data.pvpEliminationTeamCount++
                    }
                    data.send("event.bullet.kill", it.bullet.team.coloredName(), it.build.team.coloredName())
                }
                if (Vars.netServer.isWaitingForPlayers) {
                    for (t in Vars.state.teams.getActive()) {
                        if (Groups.player.count { p: Player -> p.team() === t.team } > 0) {
                            Events.fire(GameOverEvent(t.team))
                            break
                        }
                    }
                }
            }
        }.also { listener -> eventListeners[BuildingBulletDestroyEvent::class.java] = listener })

        fun send(message: String, vararg parameter: Any) {
            database.players.forEach {
                if (voteTargetUUID != it.uuid) {
                    Core.app.post { it.send(message, *parameter) }
                }
            }
        }

        fun check(): Int {
            return if (!isPvP) {
                when (database.players.filterNot { it.afk }.size) {
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
                when (database.players.count { a -> a.player.team() == voteTeam && !a.afk }) {
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

        fun back(map: Map?) {
            Core.app.post {
                val savePath: Fi = if (Core.settings.getBool("autosave")) {
                    Vars.saveDirectory.findAll { f: Fi ->
                        f.name().startsWith("auto_")
                    }.min { obj: Fi -> obj.lastModified().toFloat() }
                } else {
                    Vars.saveDirectory.child("rollback.msav")
                }

                try {
                    val mode = Vars.state.rules.mode()
                    val reloader = WorldReloader()

                    reloader.begin()

                    if (map != null) {
                        Vars.world.loadMap(map, map.applyRules(mode))
                    } else {
                        SaveIO.load(savePath)
                    }

                    Vars.state.rules = Vars.state.map.applyRules(mode)

                    Vars.logic.play()
                    reloader.end()

                    savePath.delete()
                } catch (t: Exception) {
                    t.printStackTrace()
                }
                if (map == null) send("command.vote.back.done")
            }
        }

        var colorOffset = 0
        fun nickcolor(name: String): String {
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
            for (i in name.indices) {
                val c = name[i]
                var colorIndex = (i + colorOffset) % colors.size
                if (colorIndex < 0) {
                    colorIndex += colors.size
                }
                val newtext = colors[colorIndex] + c
                newName[i] = newtext
            }
            colorOffset--
            newName.forEach {
                stringBuilder.append(it)
            }
            return stringBuilder.toString()
        }

        var milsCount = 0
        var secondCount = 0
        var minuteCount = 0

        // 맵 백업 시간
        var rollbackCount = conf.command.rollback.time
        var messageCount = conf.feature.motd.time
        var messageOrder = 0

        val coreListener = object : ApplicationListener {
            override fun update() {
                try {
                    if (Vars.state.isPlaying) {
                        for (it in database.players) {
                            if (Vars.state.rules.pvp && it.player.unit() != null && it.player.team()
                                    .cores().isEmpty && it.player.team() != Team.derelict && pvpPlayer.containsKey(it.uuid)
                            ) {
                                it.pvpDefeatCount++
                                if (conf.feature.pvp.spector) {
                                    it.player.team(Team.derelict)
                                    pvpSpecters.add(it.uuid)
                                }
                                pvpPlayer.remove(it.uuid)

                                val time = it.currentPlayTime
                                val score = time + 5000

                                it.exp += ((score * it.expMultiplier).toInt())
                                it.send("event.exp.earn.defeat", it.currentExp + score)
                            }

                            if (it.status.containsKey("freeze")) {
                                val d = findPlayerData(it.uuid)
                                if (d != null) {
                                    val player = d.player
                                    val split = it.status["freeze"].toString().split("/")
                                    player[split[0].toFloat()] = split[1].toFloat()
                                    Call.setPosition(player.con(), split[0].toFloat(), split[1].toFloat())
                                    Call.setCameraPosition(player.con(), split[0].toFloat(), split[1].toFloat())
                                    player.x(split[0].toFloat())
                                    player.y(split[1].toFloat())
                                }
                            }

                            if (it.tracking) {
                                Groups.player.forEach { player ->
                                    Call.label(
                                        it.player.con(),
                                        player.name,
                                        Time.delta / 2,
                                        player.mouseX,
                                        player.mouseY
                                    )
                                }
                            }

                            if (it.tpp != null) {
                                val target = Groups.player.find { p -> p.uuid() == it.tpp }
                                if (target != null) {
                                    Call.setCameraPosition(it.player.con(), target.x, target.y)
                                } else {
                                    it.tpp = null
                                    Call.setCameraPosition(it.player.con(), it.player.x, it.player.y)
                                }
                            }

                            for (two in PluginData.warpZones) {
                                if (two.mapName == Vars.state.map.name() && !two.click && isUnitInside(it.player.unit().tileOn(), two.startTile, two.finishTile)) {
                                    Log.info(Bundle()["log.warp.move", it.player.plainName(), two.ip, two.port.toString()])
                                    Call.connect(it.player.con(), two.ip, two.port)
                                    continue
                                }
                            }
                        }

                        if (conf.feature.level.effect.enabled && conf.feature.level.effect.moving) {
                            if (milsCount == 5) {
                                database.players.forEach {
                                    if (it.showLevelEffects && it.player.unit() != null && it.player.unit().health > 0f) {
                                        for (e in database.players) {
                                            if (e.player.unit().moving()) {
                                                val color = if (e.effectColor != null) {
                                                    if (Colors.get(e.effectColor) != null) Colors.get(e.effectColor) else Color.valueOf(
                                                        e.effectColor
                                                    )
                                                } else {
                                                    e.player.color()
                                                }

                                                fun runEffect(effect: Effect) {
                                                    if (e.player.unit() != null) Call.effect(
                                                        it.player.con(),
                                                        effect,
                                                        e.player.unit().x,
                                                        e.player.unit().y,
                                                        0f,
                                                        color
                                                    )
                                                }

                                                fun runEffect(effect: Effect, size: Float) {
                                                    if (e.player.unit() != null) Call.effect(
                                                        it.player.con(),
                                                        effect,
                                                        e.player.unit().x,
                                                        e.player.unit().y,
                                                        size,
                                                        color
                                                    )
                                                }

                                                fun runEffectRandom(effect: Effect, range: IntRange) {
                                                    if (e.player.unit() != null) Call.effect(
                                                        it.player.con(),
                                                        effect,
                                                        e.player.unit().x + range.random(),
                                                        e.player.unit().y + range.random(),
                                                        0f,
                                                        color
                                                    )
                                                }
                                                when (e.effectLevel ?: e.level) {
                                                    in 10..19 -> runEffect(Fx.freezing)
                                                    in 20..29 -> runEffect(Fx.overdriven)
                                                    in 30..39 -> {
                                                        runEffect(Fx.burning)
                                                        runEffect(Fx.melting)
                                                    }

                                                    in 40..49 -> runEffect(Fx.steam)
                                                    in 50..59 -> runEffect(Fx.shootSmallSmoke)
                                                    in 60..69 -> runEffect(Fx.mine)
                                                    in 70..79 -> runEffect(Fx.explosion)
                                                    in 80..89 -> runEffect(Fx.hitLaser)
                                                    in 90..99 -> runEffect(Fx.crawlDust)
                                                    in 100..109 -> runEffect(Fx.mineImpact)
                                                    in 110..119 -> {
                                                        runEffect(Fx.vapor)
                                                        runEffect(Fx.hitBulletColor)
                                                    }

                                                    in 120..129 -> {
                                                        runEffect(Fx.vapor)
                                                        runEffect(Fx.hitBulletColor)
                                                        runEffect(Fx.hitSquaresColor)
                                                    }

                                                    in 130..139 -> {
                                                        runEffect(Fx.vapor)
                                                        runEffect(Fx.hitLaserBlast)
                                                    }

                                                    in 140..149 -> {
                                                        runEffect(Fx.smokePuff)
                                                        runEffect(Fx.hitBulletColor)
                                                    }

                                                    in 150..159 -> {
                                                        runEffect(Fx.smokePuff)
                                                        runEffect(Fx.hitBulletColor)
                                                        runEffect(Fx.hitSquaresColor)
                                                    }

                                                    in 160..169 -> {
                                                        runEffect(Fx.smokePuff)
                                                        runEffect(Fx.hitLaserBlast)
                                                    }

                                                    in 170..179 -> {
                                                        runEffect(Fx.placeBlock, 1.8f)
                                                        runEffect(Fx.spawn)
                                                    }

                                                    in 180..189 -> {
                                                        runEffect(Fx.placeBlock, 1.8f)
                                                        runEffect(Fx.spawn)
                                                        runEffect(Fx.hitLaserBlast)
                                                    }

                                                    in 190..199 -> {
                                                        runEffect(Fx.placeBlock, 1.8f)
                                                        runEffect(Fx.spawn)
                                                        runEffect(Fx.circleColorSpark)
                                                    }

                                                    in 200..209 -> {
                                                        val f = Fx.dynamicWave
                                                        runEffect(f, 0.5f)
                                                        runEffect(f, 3f)
                                                        runEffect(f, 7f)
                                                        runEffect(f, 5f)
                                                        runEffect(f, 9f)
                                                        val xRandom = (-16..16).random()
                                                        val yRandom = (-16..16).random()
                                                        if (e.player.unit() != null) {
                                                            Call.effect(
                                                                it.player.con(),
                                                                Fx.hitLaserBlast,
                                                                e.player.unit().x + xRandom,
                                                                e.player.unit().y + yRandom,
                                                                0f,
                                                                color
                                                            )
                                                            Call.effect(
                                                                it.player.con(),
                                                                Fx.hitSquaresColor,
                                                                e.player.unit().x + xRandom,
                                                                e.player.unit().y + yRandom,
                                                                0f,
                                                                color
                                                            )
                                                        }
                                                        runEffectRandom(Fx.vapor, (-4..4))
                                                    }

                                                    in 210..219 -> {
                                                        runEffect(Fx.dynamicSpikes, 7f)
                                                        runEffectRandom(Fx.hitSquaresColor, (-4..4))
                                                        runEffectRandom(Fx.vapor, (-4..4))
                                                    }

                                                    in 220..229 -> {
                                                        runEffect(Fx.dynamicSpikes, 7f)
                                                        runEffectRandom(Fx.circleColorSpark, (-4..4))
                                                        runEffectRandom(Fx.vapor, (-4..4))
                                                    }

                                                    in 230..239 -> {
                                                        runEffect(Fx.dynamicSpikes, 7f)
                                                        runEffectRandom(Fx.circleColorSpark, (-4..4))
                                                        runEffectRandom(Fx.hitLaserBlast, (-4..4))
                                                        runEffectRandom(Fx.smokePuff, (-4..4))
                                                    }

                                                    in 240..249 -> {
                                                        runEffect(Fx.dynamicExplosion, 0.8f)
                                                        runEffectRandom(Fx.hitLaserBlast, (-16..16))
                                                        runEffectRandom(Fx.vapor, (-16..16))
                                                    }

                                                    in 250..259 -> {
                                                        runEffect(Fx.dynamicExplosion, 0.8f)
                                                        runEffectRandom(Fx.hitLaserBlast, (-4..4))
                                                        runEffectRandom(Fx.smokePuff, (-4..4))
                                                    }

                                                    in 260..269 -> {
                                                        runEffect(Fx.dynamicExplosion, 0.8f)
                                                        runEffectRandom(Fx.hitLaserBlast, (-4..4))
                                                        runEffectRandom(Fx.hitLaserBlast, (-4..4))
                                                        runEffectRandom(Fx.smokePuff, (-4..4))
                                                        Call.effect(
                                                            it.player.con(),
                                                            Fx.shootSmokeSquareBig,
                                                            e.player.unit().x + (-1..1).random(),
                                                            e.player.unit().y + (-1..1).random(),
                                                            listOf(0f, 90f, 180f, 270f).random(),
                                                            Color.HSVtoRGB(252f, 164f, 0f, 0.22f)
                                                        )
                                                    }

                                                    else -> {}
                                                }
                                            }
                                        }
                                    }
                                }
                                milsCount = 0
                            } else {
                                database.players.forEach {
                                    if (it.showLevelEffects && it.player.unit() != null && it.player.unit().health > 0f) {
                                        for (e in database.players) {
                                            if (e.player.unit().moving()) {
                                                val color = if (e.effectColor != null) {
                                                    if (Colors.get(e.effectColor) != null) Colors.get(e.effectColor) else Color.valueOf(
                                                        e.effectColor
                                                    )
                                                } else {
                                                    e.player.color()
                                                }

                                                fun runEffect(effect: Effect) {
                                                    if (e.player.unit() != null) Call.effect(
                                                        it.player.con(),
                                                        effect,
                                                        e.player.unit().x,
                                                        e.player.unit().y,
                                                        0f,
                                                        color
                                                    )
                                                }

                                                fun runEffect(effect: Effect, size: Float) {
                                                    if (e.player.unit() != null) Call.effect(
                                                        it.player.con(),
                                                        effect,
                                                        e.player.unit().x,
                                                        e.player.unit().y,
                                                        size,
                                                        color
                                                    )
                                                }

                                                fun runEffectRandomRotate(effect: Effect) {
                                                    if (e.player.unit() != null) Call.effect(
                                                        it.player.con(),
                                                        effect,
                                                        e.player.unit().x,
                                                        e.player.unit().y,
                                                        Random.nextFloat() * 360f,
                                                        color
                                                    )
                                                }

                                                fun runEffectAtRotate(effect: Effect, rotate: Float) {
                                                    if (e.player.unit() != null) Call.effect(
                                                        it.player.con(),
                                                        effect,
                                                        e.player.unit().x,
                                                        e.player.unit().y,
                                                        rotate,
                                                        color
                                                    )
                                                }

                                                fun runEffectAtRotateAndColor(
                                                    effect: Effect,
                                                    rotate: Float,
                                                    customColor: Color
                                                ) {
                                                    if (e.player.unit() != null) Call.effect(
                                                        it.player.con(),
                                                        effect,
                                                        e.player.unit().x,
                                                        e.player.unit().y,
                                                        rotate,
                                                        customColor
                                                    )
                                                }
                                                when (e.effectLevel ?: e.level) {
                                                    in 270..279 -> {
                                                        runEffectRandomRotate(Fx.shootSmokeSquare)
                                                        runEffect(Fx.hitLaserBlast)
                                                        runEffect(Fx.colorTrail, 4f)
                                                    }

                                                    in 280..289 -> {
                                                        runEffectRandomRotate(Fx.shootSmokeSquare)
                                                        runEffect(Fx.hitLaserBlast)
                                                        runEffect(Fx.dynamicWave, 2f)
                                                    }

                                                    in 290..299 -> {
                                                        runEffectAtRotate(Fx.shootSmokeSquare, 0f)
                                                        runEffectAtRotate(Fx.shootSmokeSquare, 45f)
                                                        runEffectAtRotate(Fx.shootSmokeSquare, 90f)
                                                        runEffectAtRotate(Fx.shootSmokeSquare, 135f)
                                                        runEffectAtRotate(Fx.shootSmokeSquare, 180f)
                                                        runEffectAtRotate(Fx.shootSmokeSquare, 225f)
                                                        runEffectAtRotate(Fx.shootSmokeSquare, 270f)
                                                        runEffectAtRotate(Fx.shootSmokeSquare, 315f)
                                                        runEffect(Fx.breakProp)
                                                        runEffect(Fx.vapor)
                                                    }

                                                    in 300..Int.MAX_VALUE -> {
                                                        var rot = e.player.unit().rotation
                                                        val customColor = Color.HSVtoRGB(252f, 164f, 0f, 0.22f)
                                                        rot += 180f
                                                        runEffectAtRotateAndColor(
                                                            Fx.shootSmokeSquareBig,
                                                            rot,
                                                            customColor
                                                        )
                                                        rot += 40f
                                                        runEffectAtRotateAndColor(Fx.shootTitan, rot, customColor)
                                                        rot += 25f
                                                        runEffectAtRotateAndColor(Fx.colorSpark, rot, customColor)
                                                        rot -= 105f
                                                        runEffectAtRotateAndColor(Fx.shootTitan, rot, customColor)
                                                        rot -= 25f
                                                        runEffectAtRotateAndColor(Fx.colorSpark, rot, customColor)
                                                        runEffect(Fx.mineHuge)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                milsCount++
                            }
                        }

                        if (dpsTile != null) {
                            if (dpsTile!!.build != null && dpsTile!!.block() != null) {
                                dpsBlocks += (100000000f - dpsTile!!.build.health)
                                dpsTile!!.build.health(100000000f)
                            } else {
                                dpsTile = null
                            }
                        }

                        if (secondCount == 60) {
                            PluginData.uptime++
                            PluginData.playtime++

                            if (voteCooltime > 0) voteCooltime--
                            voterCooltime.forEach {
                                voterCooltime[it.key] = it.value - 1
                                if (it.value == 0) voterCooltime.remove(it.key)
                            }

                            if (dpsTile != null) {
                                if (maxdps == null) {
                                    maxdps = 0f
                                } else if (dpsBlocks > maxdps!!) {
                                    maxdps = dpsBlocks
                                }
                                val message = "Max DPS: $maxdps/min\nDPS: ${dpsBlocks}/s"
                                Call.label(message, 1f, dpsTile!!.worldx(), dpsTile!!.worldy())
                            } else {
                                maxdps = null
                            }
                            dpsBlocks = 0f

                            for (it in database.players) {
                                it.totalPlayTime++
                                it.currentPlayTime++

                                if (it.apm.size >= 60) {
                                    it.apm.poll()
                                }
                                it.apm.add(it.currentControlCount)
                                it.currentControlCount = 0

                                if (it.animatedName) {
                                    val name = it.name.replace("\\[(.*?)]".toRegex(), "")
                                    it.player.name(nickcolor(name))
                                } else if (!it.status.containsKey("router")) {
                                    it.player.name(it.name)
                                }

                                // 잠수 플레이어 카운트
                                if (it.player.unit() != null && !it.player.unit().moving() && !it.player.unit()
                                        .mining() && !Permission.check(
                                        it,
                                        "afk.admin"
                                    ) && it.previousMousePosition == it.player.mouseX() + it.player.mouseY()
                                ) {
                                    it.afkTime++
                                    if (it.afkTime == conf.feature.afk.time) {
                                        it.afk = true
                                        if (conf.feature.afk.enabled) {
                                            if (conf.feature.afk.server.isEmpty()) {
                                                val bundle = if (it.status.containsKey("language")) {
                                                    Bundle(it.status["language"]!!)
                                                } else {
                                                    Bundle(it.player.locale())
                                                }

                                                it.player.kick(bundle["event.player.afk"])
                                                database.players.forEach { data ->
                                                    data.send("event.player.afk.other", it.player.plainName())
                                                }
                                            } else {
                                                val server = conf.feature.afk.server.split(":")
                                                val port = if (server.size == 1) {
                                                    6567
                                                } else {
                                                    server[1].toInt()
                                                }
                                                Call.connect(it.player.con(), server[0], port)
                                            }
                                        }
                                    }
                                } else {
                                    it.afkTime = 0
                                    it.afk = false
                                    it.previousMousePosition = it.player.mouseX() + it.player.mouseY()
                                }

                                val randomResult = (random.nextInt(7) * it.expMultiplier).toInt()
                                it.exp += randomResult
                                it.currentExp += randomResult
                                Commands.Exp[it]

                                if (conf.feature.level.display) {
                                    val message =
                                        "${it.exp}/${floor(Commands.Exp.calculateFullTargetXp(it.level)).toInt()}"
                                    Call.infoPopup(it.player.con(), message, Time.delta, Align.left, 0, 0, 300, 0)
                                }

                                if (it.hud != null) {
                                    val array = JsonArray.readJSON(it.hud).asArray()

                                    fun color(current: Float, max: Float): String {
                                        return when (current / max * 100.0) {
                                            in 50.0..100.0 -> "[green]"
                                            in 20.0..49.9 -> "[yellow]"
                                            else -> "[scarlet]"
                                        }
                                    }

                                    fun shieldColor(current: Float, max: Float): String {
                                        return when (current / max * 100.0) {
                                            in 50.0..100.0 -> "[#ffffe0]"
                                            in 20.0..49.9 -> "[orange]"
                                            else -> "[#CC5500]"
                                        }
                                    }

                                    array.forEach { value ->
                                        when (value.asString()) {
                                            "health" -> {
                                                Groups.unit.forEach { unit ->
                                                    val msg = StringBuilder()
                                                    val color = color(unit.health, unit.maxHealth)
                                                    if (unit.shield > 0) {
                                                        val shield = shieldColor(unit.health, unit.maxHealth)
                                                        msg.appendLine("$shield${floor(unit.shield.toDouble())}")
                                                    }
                                                    msg.append("$color${floor(unit.health.toDouble())}")

                                                    if (unit.team != it.player.team() && Permission.check(
                                                            it,
                                                            "hud.enemy"
                                                        )
                                                    ) {
                                                        Call.label(
                                                            it.player.con(),
                                                            msg.toString(),
                                                            Time.delta,
                                                            unit.getX(),
                                                            unit.getY()
                                                        )
                                                    } else if (unit.team == it.player.team()) {
                                                        Call.label(
                                                            it.player.con(),
                                                            msg.toString(),
                                                            Time.delta,
                                                            unit.getX(),
                                                            unit.getY()
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (voting) {
                                if (Groups.player.find { a -> a.uuid() == voteStarter!!.uuid } == null) {
                                    send("command.vote.canceled.leave")
                                    resetVote()
                                } else {
                                    if (count % 10 == 0) {
                                        if (isPvP) {
                                            Groups.player.forEach {
                                                if (it.team() == voteTeam) {
                                                    val data = findPlayerData(it.uuid())
                                                    if (data != null && voteTargetUUID != data.uuid) {
                                                        data.send("command.vote.count", count.toString(), check() - voted.size)
                                                    }
                                                }
                                            }
                                        } else {
                                            send("command.vote.count", count.toString(), check() - voted.size)
                                            if (voteType == "kick" && Groups.player.find { a -> a.uuid() == voteTargetUUID } == null) {
                                                send("command.vote.kick.target.leave")
                                            }
                                        }
                                    }
                                    count--
                                    if ((count == 0 && check() <= voted.size) || check() <= voted.size || isAdminVote) {
                                        send("command.vote.success")

                                        val onlinePlayers = StringBuilder()
                                        database.players.forEach {
                                            onlinePlayers.append("${it.name}, ")
                                        }
                                        onlinePlayers.substring(0, onlinePlayers.length - 2)

                                        voting = false
                                        when (voteType) {
                                            "kick" -> {
                                                val name = Vars.netServer.admins.getInfo(voteTargetUUID).lastName
                                                if (Groups.player.find { a -> a.uuid() == voteTargetUUID } == null) {
                                                    Vars.netServer.admins.banPlayerID(voteTargetUUID)
                                                    send("command.vote.kick.target.banned", name)
                                                    Events.fire(
                                                        CustomEvents.PlayerVoteBanned(
                                                            voteStarter!!.name,
                                                            name,
                                                            voteReason!!,
                                                            onlinePlayers.toString()
                                                        )
                                                    )
                                                } else {
                                                    voteTarget?.kick(Packets.KickReason.kick, 60 * 60 * 3000)
                                                    send("command.vote.kick.target.kicked", name)
                                                    Events.fire(
                                                        CustomEvents.PlayerVoteKicked(
                                                            voteStarter!!.name,
                                                            name,
                                                            voteReason!!,
                                                            onlinePlayers.toString()
                                                        )
                                                    )
                                                }
                                            }

                                            "map" -> {
                                                for (it in database.players) {
                                                    earnEXP(Vars.state.rules.waveTeam, it.player, it, true)
                                                }
                                                PluginData.isSurrender = true
                                                Vars.maps.setNextMapOverride(voteMap)
                                                Events.fire(GameOverEvent(Vars.state.rules.waveTeam))
                                            }

                                            "gg" -> {
                                                if (voteStarter != null && !Permission.check(voteStarter!!, "vote.pass")) {
                                                    voterCooltime[voteStarter!!.uuid] = 180
                                                }
                                                if (isPvP) {
                                                    Vars.world.tiles.forEach {
                                                        if (it.build != null && it.build.team != null && it.build.team == voteTeam) {
                                                            Call.setTile(it, Blocks.air, voteTeam, 0)
                                                        }
                                                    }
                                                } else {
                                                    PluginData.isSurrender = true
                                                    Events.fire(GameOverEvent(Vars.state.rules.waveTeam))
                                                }
                                            }

                                            "skip" -> {
                                                if (voteStarter != null) voterCooltime.put(voteStarter!!.uuid, 180)
                                                for (a in 0..voteWave!!) {
                                                    Vars.spawner.spawnEnemies()
                                                    Vars.state.wave++
                                                    Vars.state.wavetime = Vars.state.rules.waveSpacing
                                                }
                                                send("command.vote.skip.done", voteWave!!.toString())
                                            }

                                            "back" -> {
                                                PluginData.isSurrender = true
                                                back(null)
                                            }

                                            "random" -> {
                                                if (LocalTime.now()
                                                        .isAfter(lastVoted!!.plusMinutes(10L)) && Permission.check(
                                                        voteStarter!!,
                                                        "vote.random.bypass"
                                                    )
                                                ) {
                                                    send("command.vote.random.cool")
                                                } else {
                                                    if (voteStarter != null) voterCooltime.put(voteStarter!!.uuid, 420)
                                                    lastVoted = LocalTime.now()
                                                    send("command.vote.random.done")
                                                    Thread {
                                                        var map: Map
                                                        send("command.vote.random.is")
                                                        Thread.sleep(3000)
                                                        when (random.nextInt(7)) {
                                                            0 -> {
                                                                send("command.vote.random.unit")
                                                                Groups.unit.each {
                                                                    if (voteStarter != null) {
                                                                        if (it.team == voteStarter!!.player.team()) it.kill()
                                                                    } else {
                                                                        it.kill()
                                                                    }
                                                                }
                                                                send("command.vote.random.unit.wave")
                                                                Vars.logic.runWave()
                                                            }

                                                            1 -> {
                                                                send("command.vote.random.wave")
                                                                for (a in 0..5) Vars.logic.runWave()
                                                            }

                                                            2 -> {
                                                                send("command.vote.random.health")
                                                                Groups.build.each {
                                                                    if (voteStarter != null) {
                                                                        if (it.team == voteStarter!!.player.team()) {
                                                                            it.block.health /= 2
                                                                        }
                                                                    } else {
                                                                        it.block.health /= 2
                                                                    }
                                                                }
                                                                Groups.player.forEach {
                                                                    Call.worldDataBegin(it.con)
                                                                    Vars.netServer.sendWorldData(it)
                                                                }
                                                            }

                                                            3 -> {
                                                                send("command.vote.random.fill.core")
                                                                if (voteStarter != null) {
                                                                    Vars.content.items().forEach {
                                                                        if (!it.isHidden) {
                                                                            Vars.state.teams.cores(voteStarter!!.player.team())
                                                                                .first().items.add(
                                                                                    it,
                                                                                    random.nextInt(2000)
                                                                                )
                                                                        }
                                                                    }
                                                                } else {
                                                                    Vars.content.items().forEach {
                                                                        if (!it.isHidden) {
                                                                            Vars.state.teams.cores(Team.sharded)
                                                                                .first().items.add(
                                                                                    it,
                                                                                    random.nextInt(2000)
                                                                                )
                                                                        }
                                                                    }
                                                                }
                                                            }

                                                            4 -> {
                                                                send("command.vote.random.storm")
                                                                Thread.sleep(1000)
                                                                Call.createWeather(
                                                                    Weathers.rain,
                                                                    10f,
                                                                    60 * 60f,
                                                                    50f,
                                                                    10f
                                                                )
                                                            }

                                                            5 -> {
                                                                send("command.vote.random.fire")
                                                                for (x in 0 until Vars.world.width()) {
                                                                    for (y in 0 until Vars.world.height()) {
                                                                        Call.effect(
                                                                            Fx.fire,
                                                                            (x * 8).toFloat(),
                                                                            (y * 8).toFloat(),
                                                                            0f,
                                                                            Color.red
                                                                        )
                                                                    }
                                                                }
                                                                var tick = 600
                                                                map = Vars.state.map

                                                                while (tick != 0 && map == Vars.state.map) {
                                                                    Thread.sleep(1000)
                                                                    tick--
                                                                    Core.app.post {
                                                                        Groups.unit.each {
                                                                            it.health(it.health() - 10f)
                                                                        }
                                                                        Groups.build.each {
                                                                            it.block.health /= 30
                                                                        }
                                                                    }
                                                                    if (tick == 300) {
                                                                        send("command.vote.random.supply")
                                                                        repeat(2) {
                                                                            if (voteStarter != null) {
                                                                                UnitTypes.oct.spawn(
                                                                                    voteStarter!!.player.team(),
                                                                                    voteStarter!!.player.x,
                                                                                    voteStarter!!.player.y
                                                                                )
                                                                            } else {
                                                                                UnitTypes.oct.spawn(
                                                                                    Team.sharded,
                                                                                    Vars.state.teams.cores(
                                                                                        Team.sharded
                                                                                    ).first().x,
                                                                                    Vars.state.teams.cores(
                                                                                        Team.sharded
                                                                                    ).first().y
                                                                                )
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
                                    } else if ((count == 0 && check() > voted.size) || isCanceled) {
                                        if (isPvP) {
                                            database.players.forEach {
                                                if (it.player.team() == voteTeam) {
                                                    Core.app.post { it.send("command.vote.failed") }
                                                }
                                            }
                                        } else {
                                            send("command.vote.failed")
                                        }
                                        resetVote()
                                    }
                                }
                            }

                            if (unitLimitMessageCooldown > 0) {
                                unitLimitMessageCooldown--
                            }

                            apmRanking = "APM\n"
                            val color = arrayOf("[scarlet]", "[orange]", "[yellow]", "[green]", "[white]", "[gray]")
                            val list = LinkedHashMap<String, Int>()
                            database.players.forEach {
                                val total = it.apm.max()
                                list[it.player.plainName()] = total
                            }
                            list.toList().sortedBy { (key, _) -> key }.forEach {
                                val colored = when (it.second) {
                                    in 41..Int.MAX_VALUE -> color[0]
                                    in 21..40 -> color[1]
                                    in 11..20 -> color[2]
                                    in 6..10 -> color[3]
                                    in 1..5 -> color[4]
                                    else -> color[5]
                                }
                                val coloredName = if (it.second >= 41) nickcolor(it.first) else it.first
                                apmRanking += "$coloredName[orange] > $colored${it.second}[white]\n"
                            }
                            apmRanking = apmRanking.substring(0, apmRanking.length - 1)
                            database.players.forEach {
                                if (it.hud != null) {
                                    val array = JsonArray.readJSON(it.hud).asArray()
                                    array.forEach { value ->
                                        if (value.asString() == "apm") {
                                            Call.infoPopup(
                                                it.player.con(),
                                                apmRanking,
                                                Time.delta,
                                                Align.left,
                                                0,
                                                0,
                                                0,
                                                0
                                            )
                                        }
                                    }
                                }
                            }

                            secondCount = 0
                        } else {
                            secondCount++
                        }

                        if (minuteCount == 3600) {
                            if (Vars.state.rules.pvp) {
                                database.players.forEach {
                                    if (!pvpPlayer.containsKey(it.uuid) && it.player.team() != Team.derelict) {
                                        pvpPlayer.put(it.uuid, it.player.team())
                                    }
                                }
                            }

                            Main.daemon.submit(Thread {
                                transaction {
                                    DB.Player.selectAll().where { DB.Player.banTime neq null }.forEach { data ->
                                        val banTime = data[DB.Player.banTime]
                                        val uuid = data[DB.Player.uuid]
                                        val name = data[DB.Player.name]

                                        // todo datetime 을 모두 zoneddatetime 으로 바꾸기
                                        if (ZonedDateTime.now().isAfter(ZonedDateTime.parse(banTime))) {
                                            DB.Player.update({ DB.Player.uuid eq uuid }) {
                                                it[DB.Player.banTime] = null
                                            }
                                            Events.fire(CustomEvents.PlayerTempUnbanned(name))
                                        }
                                    }
                                }
                            })

                            if (rollbackCount == 0) {
                                SaveIO.save(Vars.saveDirectory.child("rollback.msav"))
                                rollbackCount = conf.command.rollback.time
                            } else {
                                rollbackCount--
                            }

                            if (conf.feature.motd.enabled) {
                                if (messageCount == conf.feature.motd.time) {
                                    database.players.forEach {
                                        val message = if (root.child("messages/${it.languageTag}.txt").exists()) {
                                            root.child("messages/${it.languageTag}.txt").readString()
                                        } else if (root.child("messages").list().isNotEmpty()) {
                                            val file = root.child("messages/en.txt")
                                            if (file.exists()) file.readString() else null
                                        } else {
                                            null
                                        }
                                        if (message != null) {
                                            val c = message.split(Regex("\r\n"))

                                            if (c.size <= messageOrder) {
                                                messageOrder = 0
                                            }
                                            it.player.sendMessage(c[messageOrder])
                                        }
                                    }
                                    messageOrder++
                                    messageCount = 0
                                } else {
                                    messageCount++
                                }
                            }

                            maxdps = null
                            minuteCount = 0
                        } else {
                            minuteCount++
                        }
                    } else {
                        if (secondCount == 60) {
                            milsCount = 0
                            secondCount = 0
                            minuteCount = 0
                            rollbackCount = conf.command.rollback.time
                            messageCount = conf.feature.motd.time
                            messageOrder = 0
                            maxdps = null
                            resetVote()
                        } else {
                            secondCount++
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        Core.app.addListener(coreListener)
        coreListeners.add(coreListener)
    }

    @JvmStatic
    fun log(type: LogType, text: String, vararg name: String) {
        val maxLogFile = 20
        val root: Fi = Core.settings.dataDirectory.child("mods/Essentials/")
        val time = DateTimeFormatter.ofPattern("YYYY-MM-dd HH_mm_ss").format(LocalDateTime.now())

        if (type != LogType.Report) {
            val new = Paths.get(root.child("log/$type.log").path())
            val old = Paths.get(root.child("log/old/$type/$time.log").path())
            var main = root.child("log/$type.log")
            val folder = root.child("log")

            if (main != null && main.length() > 2048 * 1024) {
                RandomAccessFile(main.file(), "rw").use { raf ->
                    raf.seek(main.file().length())
                    raf.writeBytes("\nend of file. $time")
                }
                try {
                    if (!root.child("log/old/$type").exists()) {
                        root.child("log/old/$type").mkdirs()
                    }
                    Files.move(new, old, StandardCopyOption.REPLACE_EXISTING)
                    val logFiles = root.child("log/old/$type").file().listFiles { file -> file.name.endsWith(".log") }

                    if (logFiles != null && logFiles.size >= maxLogFile) {
                        val zipFileName = "$time.zip"
                        val zipOutputStream = ZipOutputStream(FileOutputStream(zipFileName))

                        Thread {
                            for (logFile in logFiles) {
                                val entryName = logFile.name
                                val zipEntry = ZipEntry(entryName)
                                zipOutputStream.putNextEntry(zipEntry)

                                val fileInputStream = FileInputStream(logFile)
                                val buffer = ByteArray(1024)
                                var length: Int
                                while (fileInputStream.read(buffer).also { length = it } > 0) {
                                    zipOutputStream.write(buffer, 0, length)
                                }

                                fileInputStream.close()
                                zipOutputStream.closeEntry()
                            }

                            zipOutputStream.close()

                            logFiles.forEach {
                                it.delete()
                            }

                            Files.move(
                                Path(Core.files.external(zipFileName).absolutePath()),
                                Path(root.child("log/old/$type/$zipFileName").absolutePath())
                            )
                        }.start()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                main = null
            }
            if (main == null) main = folder.child("$type.log")
            RandomAccessFile(main.file(), "rw").use { raf ->
                raf.seek(main.file().length())
                raf.writeBytes("\n[$time] $text")
            }
        } else {
            val main = root.child("log/report/$time-${name[0]}.txt")
            main.writeString(text)
        }
    }

    enum class LogType {
        Player, Tap, WithDraw, Block, Deposit, Chat, Report
    }

    private fun earnEXP(winner: Team, p: Playerc, target: DB.PlayerData, isConnected: Boolean) {
        val oldLevel = target.level
        var result: Int = target.currentExp
        val time = target.currentPlayTime.toInt()

        if (PluginData.playtime > 300L) {
            val erekirAttack = if (Vars.state.planet == Planets.erekir) target.currentUnitDestroyedCount else 0
            val erekirPvP = if (Vars.state.planet == Planets.erekir) 5000 else 0

            val score = if (winner == p.team()) {
                if (Vars.state.rules.attackMode) {
                    time + (target.currentBuildAttackCount + erekirAttack)
                } else if (Vars.state.rules.pvp) {
                    time + erekirPvP + 5000
                } else {
                    0
                }
            } else if (p.team() != Team.derelict) {
                if (Vars.state.rules.attackMode) {
                    time - (target.currentBuildDeconstructedCount + target.currentBuildDestroyedCount)
                } else if (Vars.state.rules.pvp) {
                    time + 5000
                } else {
                    0
                }
            } else {
                0
            }

            target.exp += ((score * target.expMultiplier).toInt())
            result = target.currentExp + score

            Commands.Exp[target]
            target.currentExp = 0

            if (!isConnected && target.oldUUID != null) {
                target.uuid = target.oldUUID!!
                target.oldUUID = null
                database.queue(target)
            }

            if (!root.child("data/exp.json").exists()) {
                root.child("data/exp.json").writeString("[]")
            }
            val resultArray = JsonArray.readJSON(root.child("data/exp.json").readString("UTF-8")).asArray()
            val resultJson = JsonObject()
            resultJson.add("name", target.name)
            resultJson.add("uuid", target.uuid)
            resultJson.add("date", currentTime())
            resultJson.add("erekirAttack", erekirAttack)
            resultJson.add("erekirPvP", erekirPvP)
            resultJson.add("time", time)
            resultJson.add("enemyBuildingDestroyed", target.currentUnitDestroyedCount)
            resultJson.add("buildingsDeconstructed", target.currentBuildDeconstructedCount)
            resultJson.add("buildingsDestroyed", target.currentBuildDestroyedCount)
            resultJson.add("wave", Vars.state.wave)
            resultJson.add("multiplier", target.expMultiplier)
            resultJson.add("score", score)
            resultJson.add("totalScore", score * target.expMultiplier)

            resultArray.add(resultJson)
            root.child("data/exp.json").writeString(resultArray.toString())
        }

        if (isConnected && conf.feature.level.levelNotify) target.send("event.exp.current", target.exp, result, target.level, target.level - oldLevel)
    }

    fun findPlayerData(uuid: String): DB.PlayerData? {
        return database.players.find { data -> (data.oldUUID != null && data.oldUUID == uuid) || data.uuid == uuid }
    }

    @JvmStatic
    fun findPlayers(name: String): Playerc? {
        return if (name.toIntOrNull() != null) {
            database.players.forEach {
                if (it.entityid == name.toInt()) {
                    it.player
                }
            }
            Groups.player.find { p -> p.id == name.toInt() }
        } else {
            Groups.player.find { p -> p.plainName().contains(name, true) }
        }
    }

    fun findPlayersByName(name: String): Administration.PlayerInfo? {
        return if (!Vars.netServer.admins.findByName(name).isEmpty) {
            Vars.netServer.admins.findByName(name).first()
        } else {
            null
        }
    }

    fun resetVote() {
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
        voteTeam = Vars.state.rules.defaultTeam
        voted.clear()
        count = 60
    }

    private fun addLog(log: TileLog) {
        worldHistory.add(log)
    }

    class TileLog(
        val time: Long,
        val player: String,
        val action: String,
        val x: Short,
        val y: Short,
        val tile: String,
        val rotate: Int,
        val team: Team,
        val value: Any?
    )

    fun isUnitInside(target: Tile, first: Tile, second: Tile) : Boolean {
        val minX = minOf(first.getX(), second.getX())
        val maxX = maxOf(first.getX(), second.getX())
        val minY = minOf(first.getY(), second.getY())
        val maxY = maxOf(first.getY(), second.getY())

        return target.getX() in minX..maxX && target.getY() in minY..maxY
    }
}