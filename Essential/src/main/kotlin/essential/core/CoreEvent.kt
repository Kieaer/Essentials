package essential.core

import arc.ApplicationListener
import arc.Core
import arc.Events
import arc.func.Cons
import arc.graphics.Color
import arc.util.Log
import arc.util.Strings
import com.charleskorn.kaml.Yaml
import com.fasterxml.jackson.databind.ObjectMapper
import essential.*
import essential.bundle.Bundle
import essential.core.Main.Companion.conf
import essential.core.Main.Companion.pluginData
import essential.core.Main.Companion.scope
import essential.database.data.*
import essential.database.data.plugin.WarpZone
import essential.database.table.PlayerTable
import essential.event.CustomEvents
import essential.permission.Permission
import essential.util.currentTime
import essential.util.findPlayerData
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.toLocalDateTime
import ksp.event.Event
import mindustry.Vars
import mindustry.content.Blocks
import mindustry.content.Fx
import mindustry.content.Planets
import mindustry.game.EventType.*
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.gen.Playerc
import mindustry.maps.Map
import mindustry.net.Administration
import mindustry.ui.Menus
import mindustry.world.Tile
import mindustry.world.blocks.ConstructBlock
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.StandardWatchEventKinds
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.minutes

/** 월드 기록 */
internal var worldHistory = ArrayList<TileLog>()
private var dateformat = SimpleDateFormat("HH:mm:ss")
private var blockExp = mutableMapOf<String, Int>()

/** 맵 투표 목록 (UUID, 맵) */
val mapVotes = HashMap<String, Map>()

/** 맵 평가 목록 (UUID, Boolean) - true for upvote, false for downvote */
val mapRatings = HashMap<String, Boolean>()

/** PvP 관전 플레이어 목록 */
internal var pvpSpecters = mutableListOf<String>()

/** PvP 플레이어 팀 데이터 목록 */
internal var pvpPlayer = mutableMapOf<String, Team>()

/** 전체 채팅 차단 유무 */
internal var isGlobalMute = false
private var unitLimitMessageCooldown = 0
var offlinePlayers = mutableListOf<PlayerData>()

val eventListeners: HashMap<Class<*>, Cons<*>> = hashMapOf()
val coreListeners: ArrayList<ApplicationListener> = arrayListOf()
lateinit var actionFilter: Administration.ActionFilter

private val blockSelectRegex: Pattern = Pattern.compile("^build\\d{1,2}$")
private val logFiles = HashMap<LogType, FileAppender>()

fun init() {
    for (type in LogType.entries) {
        logFiles[type] = FileAppender(rootPath.child("log/$type.log").file())
    }
}

@Event
internal fun withdraw(event: WithdrawEvent) {
    if (event.tile != null && event.player.unit().item() != null && event.player.name != null) {
        log(
            LogType.WithDraw,
            Bundle()["log.withdraw", event.player.plainName(), event.player.unit()
                .item().name, event.amount, event.tile.block.name, event.tile.tileX(), event.tile.tileY()]
        )
        addLog(
            TileLog(
                System.currentTimeMillis(),
                event.player.name,
                "withdraw",
                event.tile.tile.x,
                event.tile.tile.y,
                checkValidBlock(event.tile.tile),
                event.tile.rotation,
                event.tile.team,
                event.tile.config()
            )
        )
    }
}

@Event
internal fun deposit(event: DepositEvent) {
    if (event.tile != null && event.player.unit().item() != null && event.player.name != null) {
        log(
            LogType.Deposit,
            Bundle()["log.deposit", event.player.plainName(), event.player.unit()
                .item().name, event.amount, checkValidBlock(event.tile.tile), event.tile.tileX(), event.tile.tileY()]
        )
        addLog(
            TileLog(
                System.currentTimeMillis(),
                event.player.name,
                "deposit",
                event.tile.tile.x,
                event.tile.tile.y,
                checkValidBlock(event.tile.tile),
                event.tile.rotation,
                event.tile.team,
                event.tile.config()
            )
        )
    }
}

@Event
internal fun config(event: ConfigEvent) {
    if (event.tile != null && event.tile.block != null && event.player != null) {
        addLog(
            TileLog(
                System.currentTimeMillis(),
                event.player.name,
                "config",
                event.tile.tile.x,
                event.tile.tile.y,
                checkValidBlock(event.tile.tile),
                event.tile.rotation,
                event.tile.team,
                event.value
            )
        )
        if (checkValidBlock(event.tile.tile).contains("message", true)) {
            addLog(
                TileLog(
                    System.currentTimeMillis(),
                    event.player.name,
                    "message",
                    event.tile.tile.x,
                    event.tile.tile.y,
                    checkValidBlock(event.tile.tile),
                    event.tile.rotation,
                    event.tile.team,
                    event.value
                )
            )
        }
    }
}

@Event
internal fun tap(event: TapEvent) {
    log(LogType.Tap, Bundle()["log.tap", event.player.plainName(), checkValidBlock(event.tile)])
    addLog(
        TileLog(
            System.currentTimeMillis(),
            event.player.name,
            "tap",
            event.tile.x,
            event.tile.y,
            checkValidBlock(event.tile),
            if (event.tile.build != null) event.tile.build.rotation else 0,
            if (event.tile.build != null) event.tile.build.team else Vars.state.rules.defaultTeam,
            null
        )
    )
    val data = findPlayerData(event.player.uuid())
    if (data != null) {
        pluginData.data.warpBlock.forEach { two ->
            if (two.mapName == Vars.state.map.name() && event.tile.block().name == two.tileName && event.tile.build.tileX() == two.x && event.tile.build.tileY() == two.y) {
                if (two.online) {
                    players.forEach { data ->
                        data.send("event.tap.server", event.player.plainName(), two.description)
                    }
                    // why?
                    val format = NumberFormat.getNumberInstance(Locale.US)
                    format.isGroupingUsed = false

                    Log.info(
                        Bundle()["log.warp.move.block", event.player.plainName(), Strings.stripColors(two.description), two.ip, format.format(
                            two.port
                        )]
                    )
                    Call.connect(event.player.con(), two.ip, two.port)
                }
                return@forEach
            }
        }

        for (two in pluginData.data.warpZone) {
            if (two.mapName == Vars.state.map.name() && two.click && isUnitInside(
                    event.tile,
                    two.startTile,
                    two.finishTile
                )
            ) {
                Log.info(Bundle()["log.warp.move", event.player.plainName(), two.ip, two.port.toString()])
                Call.connect(event.player.con(), two.ip, two.port)
                continue
            }
        }

        if (data.viewHistoryMode) {
            val buf = ArrayList<TileLog>()
            worldHistory.forEach { two ->
                if (two.x == event.tile.x && two.y == event.tile.y) {
                    buf.add(two)
                }
            }
            val str = StringBuilder()
            val bundle = data.bundle
            // todo 이거 파일 없음
            val coreBundle =
                Bundle(ResourceBundle.getBundle("mindustry/bundle", Locale(data.player.locale())))

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
                        bundle["event.log.format.message", dateformat.format(two.time), two.player, coreBundle["block.${two.tile}.name"], two.value as String]
                    ).append("\n")
                } else {
                    str.append(
                        bundle["event.log.format", dateformat.format(two.time), two.player, coreBundle["block.${two.tile}.name"], action]
                    ).append("\n")
                }
            }

            Call.effect(event.player.con(), Fx.shockwave, event.tile.getX(), event.tile.getY(), 0f, Color.cyan)
            if (str.toString().lines().size > 10) {
                str.append(bundle["event.log.position", event.tile.x, event.tile.y] + "\n")
                val lines: List<String> = str.toString().split("\n").reversed()
                for (i in 0 until 10) {
                    str.append(lines[i]).append("\n")
                }
                event.player.sendMessage(str.toString().trim())
            } else {
                event.player.sendMessage(
                    bundle["event.log.position", event.tile.x, event.tile.y] + "\n" + str.toString().trim()
                )
            }
        }

        if (data.status.containsKey("hub_first") && !data.status.containsKey("hub_second")) {
            data.status["hub_first"] = "${event.tile.x},${event.tile.y}"
            data.status["hub_second"] = "true"
            data.send("command.hub.zone.next", "${event.tile.x},${event.tile.y}")
        } else if (data.status.containsKey("hub_first") && data.status.containsKey("hub_second")) {
            val x = data.status["hub_first"]!!.split(",")[0].toInt()
            val y = data.status["hub_first"]!!.split(",")[1].toInt()
            val ip = data.status["hub_ip"]!!
            val port = data.status["hub_port"]!!.toInt()

            val bundle = if (data.status.containsKey("language")) {
                Bundle(data.status["language"]!!)
            } else {
                Bundle(event.player.locale())
            }
            val options = arrayOf(arrayOf(bundle["command.hub.zone.yes"], bundle["command.hub.zone.no"]))
            val menu = Menus.registerMenu { player, option ->
                val touch = when (option) {
                    0 -> true
                    else -> false
                }
                pluginData.data.warpZone.add(
                    WarpZone(
                        Vars.state.map.plainName(),
                        Vars.world.tile(x, y).pos(),
                        event.tile.pos(),
                        touch,
                        ip,
                        port
                    )
                )
                player.sendMessage(bundle["command.hub.zone.added", "$x:$y", ip, if (touch) bundle["command.hub.zone.clickable"] else bundle["command.hub.zone.enter"]])
                scope.launch {
                    pluginData.update()
                }
            }

            Call.menu(
                data.player.con(),
                menu,
                bundle["command.hub.zone.title"],
                bundle["command.hub.zone.message"],
                options
            )

            data.status.remove("hub_first")
            data.status.remove("hub_second")
            data.status.remove("hub_ip")
            data.status.remove("hub_port")
        }

        players.forEach { two ->
            if (two.mouseTracking) {
                Call.effect(two.player.con(), Fx.bigShockwave, event.tile.getX(), event.tile.getY(), 0f, Color.cyan)
            }
        }
    }
}

@Event
internal fun wave(event: WaveEvent) {
    for (data in players) {
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
}

@Event
internal fun serverLoad(event: ServerLoadEvent) {
    Vars.content.blocks().each { two ->
        var buf = 0
        two.requirements.forEach { item ->
            buf += item.amount
        }
        blockExp.put(two.name, buf)
    }

    Vars.netServer.admins.addChatFilter(Administration.ChatFilter { player, message ->
        log(LogType.Chat, "${player.plainName()}: $message")
        return@ChatFilter if (!message.startsWith("/")) {
            val data = findPlayerData(player.uuid())
            if (data != null) {
                if (!data.chatMuted) {
                    if (isGlobalMute) {
                        if (Permission.check(data, "chat.admin")) {
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
                message
            }
        } else {
            null
        }
    })

    if (!Vars.mods.list().contains { mod -> mod.name == "essential-protect" }) {
        Events.on(PlayerJoin::class.java, Cons<PlayerJoin> {
            it.player.admin(false)
            val data = runBlocking { getPlayerData(it.player.uuid()) }

            val trigger = Trigger()
            if (data == null) {
                if (transaction {
                    PlayerTable.select(PlayerTable.name).where { PlayerTable.name eq it.player.name }.empty()
                    }) {
                    val data = runBlocking { createPlayerData(it.player) }
                    trigger.loadPlayer(data)
                } else {
                    Call.kick(it.player.con, Bundle(it.player.locale)["event.player.name.duplicate"])
                }
            } else {
                trigger.loadPlayer(data)
            }
        }.also { listener -> eventListeners[PlayerJoin::class.java] = listener })
    }
}

@Event
internal fun gameOver(event: GameOverEvent) {
    if (mapVotes.isNotEmpty()) {
        val voteCount = HashMap<Map, Int>()
        mapVotes.values.forEach { map ->
            voteCount[map] = voteCount.getOrDefault(map, 0) + 1
        }

        val mostVotedMap = voteCount.maxByOrNull { it.value }?.key

        if (mostVotedMap != null) {
            Call.sendMessage(Bundle()["command.nextmap.vote.result", mostVotedMap.plainName()])
        }

        mapVotes.clear()
    }

    // Show map rating menu to players if 5 minutes have passed since game start
    val fiveMinutesPassed = (timeSource.markNow() - mapStartTime).inWholeMinutes >= 5
    if (fiveMinutesPassed) {
        val currentMap = Vars.state.map
        val mapName = currentMap.plainName()

        for (data in players) {
            // Only show the menu if the player hasn't already voted
            if (!mapRatings.containsKey(data.uuid)) {
                val rateMapMenu = Menus.registerMenu { player, select ->
                    if (select == 0) {
                        // Upvote
                        mapRatings[data.uuid] = true

                        // Save to persistent storage
                        val mapRatingsForCurrentMap = pluginData.data.mapRatings.getOrPut(mapName) { HashMap() }
                        mapRatingsForCurrentMap[data.uuid] = true

                        data.send("command.map.rate.upvote", mapName)
                    } else if (select == 1) {
                        // Downvote
                        mapRatings[data.uuid] = false

                        // Save to persistent storage
                        val mapRatingsForCurrentMap = pluginData.data.mapRatings.getOrPut(mapName) { HashMap() }
                        mapRatingsForCurrentMap[data.uuid] = false

                        data.send("command.map.rate.downvote", mapName)
                    }
                    // If select == 2, it's "Cancel" so do nothing
                }

                Call.menu(
                    data.player.con(),
                    rateMapMenu,
                    Bundle(data.player.locale())["command.map.rate.title"],
                    Bundle(data.player.locale())["command.map.rate.text", mapName],
                    arrayOf(
                        arrayOf(
                            Bundle(data.player.locale())["command.map.rate.upvote.button"],
                            Bundle(data.player.locale())["command.map.rate.downvote.button"],
                            Bundle(data.player.locale())["command.map.rate.cancel"]
                        )
                    )
                )
            }
        }
    }

    if (!Vars.state.rules.infiniteResources) {
        if (Vars.state.rules.pvp) {
            for (data in players) {
                if (data.player.team() == event.winner) {
                    data.pvpWinCount++
                }
            }
        } else if (Vars.state.rules.attackMode) {
            for (data in players) {
                if (data.player.team() == event.winner) {
                    data.attackClear++
                }
            }
        }
        for (data in players) {
            earnEXP(event.winner, data.player, data, true)
        }
        for (data in offlinePlayers) {
            earnEXP(event.winner, data.player, data, false)
        }
    }
    offlinePlayers.clear()
    worldHistory.clear()
    pvpSpecters.clear()
    pvpPlayer.clear()
}

@Event
internal fun blockBuildBegin(event: BlockBuildBeginEvent) {
    Events.on(BlockBuildBeginEvent::class.java, Cons<BlockBuildBeginEvent> {

    }.also { listener -> eventListeners[BlockBuildEndEvent::class.java] = listener })
}

@Event
internal fun blockBuildEnd(event: BlockBuildEndEvent) {
    val isDebug = Core.settings.getBool("debugMode")

    if (event.unit != null && event.unit.isPlayer) {
        val player = event.unit.player
        val target = findPlayerData(player.uuid())

        if (player.unit() != null && target != null && event.tile.block() != null && player.unit()
                .buildPlan() != null
        ) {
            val block = event.tile.block()
            if (!event.breaking) {
                log(
                    LogType.Block,
                    Bundle()["log.block.place", target.name, checkValidBlock(event.tile), event.tile.x, event.tile.y]
                )

                val buf = ArrayList<TileLog>()
                worldHistory.forEach { two ->
                    if (two.x == event.tile.x && two.y == event.tile.y) {
                        buf.add(two)
                    }
                }

                if (!Vars.state.rules.infiniteResources && event.tile != null && event.tile.build != null && event.tile.build.maxHealth() == event.tile.block().health.toFloat() && (!buf.isEmpty() && buf.last().tile != event.tile.block().name)) {
                    target.blockPlaceCount++
                    target.exp += blockExp[block.name]!!
                    target.currentExp += blockExp[block.name]!!
                }

                addLog(
                    TileLog(
                        System.currentTimeMillis(),
                        target.name,
                        "place",
                        event.tile.x,
                        event.tile.y,
                        checkValidBlock(event.tile),
                        if (event.tile.build != null) event.tile.build.rotation else 0,
                        if (event.tile.build != null) event.tile.build.team else Vars.state.rules.defaultTeam,
                        event.config
                    )
                )

                if (isDebug) {
                    Log.info("${player.name} placed ${event.tile.block().name} to ${event.tile.x},${event.tile.y}")
                }
            } else {
                log(
                    LogType.Block,
                    Bundle()["log.block.break", target.name, checkValidBlock(event.tile), event.tile.x, event.tile.y]
                )
                addLog(
                    TileLog(
                        System.currentTimeMillis(),
                        target.name,
                        "break",
                        event.tile.x,
                        event.tile.y,
                        checkValidBlock(player.unit().buildPlan().tile()),
                        if (event.tile.build != null) event.tile.build.rotation else 0,
                        if (event.tile.build != null) event.tile.build.team else Vars.state.rules.defaultTeam,
                        event.config
                    )
                )

                if (!Vars.state.rules.infiniteResources) {
                    target.blockBreakCount++
                    target.exp -= blockExp[player.unit().buildPlan().block.name]!!
                    target.currentExp -= blockExp[player.unit().buildPlan().block.name]!!
                }
            }
        }
    }
}

@Event
internal fun buildSelect(event: BuildSelectEvent) {
    if (event.builder is Playerc && event.builder.buildPlan() != null && event.tile.block() !== Blocks.air && event.breaking) {
        log(
            LogType.Block,
            Bundle()["log.block.remove", (event.builder as Playerc).plainName(), checkValidBlock(event.tile), event.tile.x, event.tile.y]
        )
        addLog(
            TileLog(
                System.currentTimeMillis(),
                (event.builder as Playerc).plainName(),
                "select",
                event.tile.x,
                event.tile.y,
                checkValidBlock(Vars.player.unit().buildPlan().tile()),
                if (event.tile.build != null) event.tile.build.rotation else 0,
                if (event.tile.build != null) event.tile.build.team else Vars.state.rules.defaultTeam,
                event.tile.build.config()
            )
        )
    }
}

@Event
internal fun blockDestroy(event: BlockDestroyEvent) {
    if (Vars.state.rules.attackMode) {
        for (a in players) {
            if (event.tile.team() != Vars.state.rules.defaultTeam) {
                a.currentBuildAttackCount++
            } else {
                a.currentBuildDestroyedCount++
            }
        }
    }
}

@Event
internal fun unitDestroy(event: UnitDestroyEvent) {
    if (!Vars.state.rules.pvp) {
        for (a in players) {
            if (event.unit.team() != a.player.team()) {
                a.currentUnitDestroyedCount++
            }
        }
    }
}

@Event
internal fun unitCreate(event: UnitCreateEvent) {
    if (conf.feature.unit.enabled && Groups.unit.size() > conf.feature.unit.limit) {
        event.unit.kill()

        if (unitLimitMessageCooldown == 0) {
            players.forEach {
                it.send(
                    "config.spawnLimit.reach",
                    "[scarlet]${Groups.unit.size()}[white]/[sky]${conf.feature.unit.limit}"
                )
            }
            unitLimitMessageCooldown = 60
        }
    }
}

@Event
internal fun playerJoin(event: PlayerJoin) {
    log(LogType.Player, Bundle()["log.joined", event.player.plainName(), event.player.uuid(), event.player.con.address])
}

@Event
internal fun playerLeave(event: PlayerLeave) {
    log(
        LogType.Player,
        Bundle()["log.player.disconnect", event.player.plainName(), event.player.uuid(), event.player.con.address]
    )
    val data = players.find { e -> e.uuid == event.player.uuid() }
    if (data != null) {
        data.lastPlayedWorldName = Vars.state.map.plainName()
        data.lastPlayedWorldMode = Vars.state.rules.modeName
        data.lastLogoutDate = Clock.System.now().toLocalDateTime(systemTimezone)
        data.isConnected = false

        offlinePlayers.add(data)

        if (Vars.state.rules.pvp) {
            val b = Groups.player.copy()
            b.remove(event.player)
            val s: HashMap<Team, Playerc> = hashMapOf()
            b.forEach { p ->
                if (p.team() != Team.derelict) {
                    s[p.team()] = p
                }
            }
            if (s.keys.size == 1) {
                Events.fire(GameOverEvent(b.first().team()))
            }
        } else if (players.isEmpty() && pvpSpecters.isNotEmpty()) {
            Events.fire(GameOverEvent(data.player.team()))
        }
        players.removeAll { e -> e.uuid == data.uuid }
    }
}

@Event
internal fun playerBan(event: PlayerBanEvent) {
    log(
        LogType.Player,
        Bundle()["log.player.banned", Vars.netServer.admins.getInfo(event.uuid).ips.first(), Vars.netServer.admins.getInfo(
            event.uuid
        ).names.first()]
    )
    scope.launch {
        createBanInfo(Vars.netServer.admins.getInfo(event.uuid), null)
    }
}

@Event
internal fun playerUnban(event: PlayerUnbanEvent) {
    Events.fire(CustomEvents.PlayerUnbanned(Vars.netServer.admins.getInfo(event.uuid).lastName, currentTime()))
    scope.launch {
        removeBanInfoByUUID(event.uuid)
    }
}

@Event
internal fun playerIpUnban(eent: PlayerIpUnbanEvent) {
    Events.fire(CustomEvents.PlayerUnbanned(Vars.netServer.admins.findByIP(eent.ip).lastName, currentTime()))
    scope.launch {
        removeBanInfoByIP(eent.ip)
    }
}

@Event
internal fun worldLoad(event: WorldLoadEvent) {
    mapStartTime = timeSource.markNow()
    isSurrender = false
    isCheated = false
    mapRatings.clear()

    // Load map ratings for the current map from persistent storage
    val currentMapName = Vars.state.map.plainName()
    val savedRatings = pluginData.data.mapRatings[currentMapName]
    if (savedRatings != null) {
        // Copy the saved ratings to the in-memory map
        mapRatings.putAll(savedRatings)
    }

    if (Vars.saveDirectory.child("rollback.msav").exists()) Vars.saveDirectory.child("rollback.msav").delete()

    if (Vars.state.rules.pvp) {
        pvpSpecters.clear()

        for (data in players) {
            if (Permission.check(data, "pvp.spector")) {
                data.player.team(Team.derelict)
            }
        }
    }

    for (data in players) {
        data.currentPlayTime = 0
        data.viewHistoryMode = false
    }
}

@Event
internal fun connectPacket(event: ConnectPacketEvent) {
    if (conf.feature.blacklist.enabled) {
        pluginData.data.blacklistedNames.forEach { text ->
            val pattern = Regex(text)
            if ((conf.feature.blacklist.regex && pattern.matches(event.packet.name)) ||
                !conf.feature.blacklist.regex && event.packet.name.contains(text)
            ) {
                event.connection.kick(Bundle(event.packet.locale)["event.player.name.blacklisted"], 0L)
                log(
                    LogType.Player,
                    Bundle()["event.player.kick", event.packet.name, event.packet.uuid, event.connection.address, Bundle()["event.player.kick.reason.blacklisted"]]
                )
                Events.fire(
                    CustomEvents.PlayerConnectKicked(
                        event.packet.name,
                        Bundle()["event.player.kick.reason.blacklisted"]
                    )
                )
                return@forEach
            }
        }
    }
}

@Event
internal fun playerConnect(event: PlayerConnect) {
    log(
        LogType.Player,
        Bundle()["event.player.connected", event.player.plainName(), event.player.uuid(), event.player.con.address]
    )
}

@Event
internal fun buildingBulletDestroy(event: BuildingBulletDestroyEvent) {
    val cores = listOf(
        Blocks.coreAcropolis,
        Blocks.coreBastion,
        Blocks.coreCitadel,
        Blocks.coreFoundation,
        Blocks.coreAcropolis,
        Blocks.coreNucleus,
        Blocks.coreShard
    )
    if (Vars.state.rules.pvp && event.build.closestCore() == null && cores.contains(event.build.block)) {
        for (data in players) {
            if (data.player.team() == event.bullet.team) {
                data.pvpEliminatedCount++
            }
            data.send("event.bullet.kill", event.bullet.team.coloredName(), event.build.team.coloredName())
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
}

@Event
internal fun configFileModified(event: CustomEvents.ConfigFileModified) {
    if (event.kind == StandardWatchEventKinds.ENTRY_MODIFY) {
        when (event.paths) {
            "permission_user.yaml", "permission.yaml" -> {
                try {
                    Permission.load()
                    Log.info(Bundle()["config.permission.updated"])
                } catch (e: ParseException) {
                    Log.err(e)
                }
            }

            "config.yaml" -> {
                conf = Yaml.default.decodeFromString(
                    CoreConfig.serializer(),
                    rootPath.child(Main.CONFIG_PATH).readString()
                )
                Log.info(Bundle()["config.reloaded"])
            }
        }
    }
}

fun log(type: LogType, text: String, vararg name: String) {
    val maxLogFile = 20
    val time = DateTimeFormatter.ofPattern("YYYY-MM-dd HH_mm_ss").format(LocalDateTime.now())

    if (!rootPath.child("log/old/$type").exists()) {
        rootPath.child("log/old/$type").mkdirs()
    }
    if (!rootPath.child("log/$type.log").exists()) {
        rootPath.child("log/$type.log").writeString("")
    }

    if (type != LogType.Report) {
        val new = Paths.get(rootPath.child("log/$type.log").path())
        val old = Paths.get(rootPath.child("log/old/$type/$time.log").path())
        var main = logFiles[type]
        val folder = rootPath.child("log")

        if (main != null && main.length() > 2048 * 1024) {
            main.write("end of file. $time")
            main.close()
            try {
                if (!rootPath.child("log/old/$type").exists()) {
                    rootPath.child("log/old/$type").mkdirs()
                }
                Files.move(new, old, StandardCopyOption.REPLACE_EXISTING)
                val logFiles =
                    rootPath.child("log/old/$type").file().listFiles { file -> file.name.endsWith(".log") }

                if (logFiles != null && logFiles.size >= maxLogFile) {
                    val zipFileName = "$time.zip"
                    val zipOutputStream = ZipOutputStream(FileOutputStream(zipFileName))

                    scope.launch {
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
                            Path(rootPath.child("log/old/$type/$zipFileName").absolutePath())
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            main = null
        }
        if (main == null) {
            logFiles[type] = FileAppender(folder.child("$type.log").file())
        } else {
            main.write("[$time] $text")
        }
    } else {
        val main = rootPath.child("log/report/$time-${name[0]}.txt")
        main.writeString(text)
    }
}

enum class LogType {
    Player, Tap, WithDraw, Block, Deposit, Chat, Report
}

fun earnEXP(winner: Team, p: Playerc, target: PlayerData, isConnected: Boolean) {
    val oldLevel = target.level
    var result: Int = target.currentExp
    val time = target.currentPlayTime

    if (mapStartTime.plus(5.minutes).hasPassedNow()) {
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
                time - target.currentBuildDestroyedCount
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

        if (!rootPath.child("data/exp.json").exists()) {
            rootPath.child("data/exp.json").writeString("[]")
        }
        val objectMapper = ObjectMapper()
        val resultArray = objectMapper.readTree(rootPath.child("data/exp.json").readString("UTF-8"))
        val resultJson = mutableMapOf<String, Any>()
        resultJson["name"] = target.name
        resultJson["uuid"] = target.uuid
        resultJson["date"] = currentTime()
        resultJson["erekirAttack"] = erekirAttack
        resultJson["erekirPvP"] = erekirPvP
        resultJson["time"] = time
        resultJson["enemyBuildingDestroyed"] = target.currentUnitDestroyedCount
        resultJson["buildingsDestroyed"] = target.currentBuildDestroyedCount
        resultJson["wave"] = Vars.state.wave
        resultJson["multiplier"] = target.expMultiplier
        resultJson["score"] = score
        resultJson["totalScore"] = score * target.expMultiplier

        objectMapper.writeValue(rootPath.child("data/exp.json").file(), resultArray.plus(resultJson))
        rootPath.child("data/exp.json").writeString(resultArray.toString())
    }

    if (isConnected && conf.feature.level.levelNotify) target.send(
        "event.exp.current",
        target.exp,
        result,
        target.level,
        target.level - oldLevel
    )
}

class FileAppender(private val file: File) {
    private val raf: RandomAccessFile

    init {
        if (!file.exists()) {
            file.writeText("")
        }
        raf = RandomAccessFile(file, "rw")
    }

    fun write(text: String) {
        raf.write(("\n$text").toByteArray(StandardCharsets.UTF_8))
    }

    fun length(): Long {
        return file.length()
    }

    fun close() {
        raf.close()
    }
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

fun isUnitInside(target: Tile, first: Tile, second: Tile): Boolean {
    val minX = minOf(first.getX(), second.getX())
    val maxX = maxOf(first.getX(), second.getX())
    val minY = minOf(first.getY(), second.getY())
    val maxY = maxOf(first.getY(), second.getY())

    return target.getX() in minX..maxX && target.getY() in minY..maxY
}

private fun checkValidBlock(tile: Tile): String {
    return if (tile.build != null && blockSelectRegex.matcher(tile.block().name).matches()) {
        (tile.build as ConstructBlock.ConstructBuild).current.name
    } else {
        tile.block().name
    }
}
