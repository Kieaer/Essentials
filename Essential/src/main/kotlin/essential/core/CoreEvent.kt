package essential.core

import arc.ApplicationListener
import arc.Core
import arc.Events
import arc.func.Cons
import arc.graphics.Color
import arc.util.Log
import arc.util.Strings
import com.charleskorn.kaml.Yaml
import essential.*
import essential.bundle.Bundle
import essential.core.Main.Companion.conf
import essential.core.Main.Companion.scope
import essential.database.data.*
import essential.database.data.plugin.WarpZone
import essential.database.table.PlayerTable
import essential.event.CustomEvents
import essential.log.LogType
import essential.log.writeLog
import essential.permission.Permission
import essential.util.currentTime
import essential.util.findPlayerData
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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
import java.io.IOException
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.StandardWatchEventKinds
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.minutes

/** 월드 기록 - Now stored in the database */
@Deprecated("Use database functions instead", ReplaceWith("getAllWorldHistory()"))
internal var worldHistory = ArrayList<TileLog>()
private var dateformat = SimpleDateFormat("HH:mm:ss")
private var blockExp = mutableMapOf<String, Int>()

/** 맵 투표 목록 (UUID, 맵) */
val mapVotes = HashMap<String, Map>()

/** 맵 평가 목록 (UUID, Boolean) - true for upvote, false for downvote */
// This is now stored in the database using MapRating class
val mapRatings = HashMap<String, Boolean>()

/** PvP 관전 플레이어 목록 */
internal var pvpSpecters = mutableListOf<String>()

/** PvP 플레이어 팀 데이터 목록 */
internal var pvpPlayer = mutableMapOf<String, Team>()

/** 전체 채팅 차단 유무 */
internal var isGlobalMute = false
private var unitLimitMessageCooldown = 0

val eventListeners: HashMap<Class<*>, Cons<*>> = hashMapOf()
val coreListeners: ArrayList<ApplicationListener> = arrayListOf()
lateinit var actionFilter: Administration.ActionFilter

private val blockSelectRegex: Pattern = Pattern.compile("^build\\d{1,2}$")

@Event
internal fun withdraw(event: WithdrawEvent) {
    if (event.tile != null && event.player.unit().item() != null && event.player.name != null) {
        writeLog(
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
        writeLog(
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
    writeLog(LogType.Tap, Bundle()["log.tap", event.player.plainName(), checkValidBlock(event.tile)])
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
            // Use both the ArrayList (for backward compatibility) and the database
            val buf = ArrayList<TileLog>()

            // Add entries from the ArrayList
            worldHistory.forEach { two ->
                if (two.x == event.tile.x && two.y == event.tile.y) {
                    buf.add(two)
                }
            }

            // Launch a coroutine to get entries from the database
            scope.launch {
                try {
                    // Get entries from the database
                    val dbEntries = getWorldHistoryByCoordinates(event.tile.x.toShort(), event.tile.y.toShort())

                    // Convert database entries to TileLog objects
                    val dbTileLogs = dbEntries.map { entry ->
                        TileLog(
                            time = entry.time,
                            player = entry.player,
                            action = entry.action,
                            x = entry.x,
                            y = entry.y,
                            tile = entry.tile,
                            rotate = entry.rotate,
                            team = Team.all.find { it.name == entry.team } ?: Team.derelict,
                            value = entry.value
                        )
                    }

                    // Add database entries to the buffer if they're not already in the ArrayList
                    dbTileLogs.forEach { dbLog ->
                        if (!buf.any { it.time == dbLog.time && it.player == dbLog.player && it.action == dbLog.action }) {
                            buf.add(dbLog)
                        }
                    }

                    // Sort the buffer by time
                    buf.sortBy { it.time }

                    // Process the combined results
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
                } catch (e: Exception) {
                    Log.err("Error retrieving world history from database", e)

                    // Fallback to using only the ArrayList if there's an error
                    val str = StringBuilder()
                    val bundle = data.bundle
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
        writeLog(LogType.Chat, "${player.plainName()}: $message")
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

    Events.on(PlayerJoin::class.java, Cons<PlayerJoin> {
        it.player.admin(false)

        scope.launch {
            val data = getPlayerData(it.player.uuid())

            if (data == null) {
                if (transaction {
                        PlayerTable.select(PlayerTable.name).where { PlayerTable.name eq it.player.name }.empty()
                    }) {
                    val data = createPlayerData(it.player)
                    data.permission = "user"
                    data.player = it.player
                    Events.fire(CustomEvents.PlayerDataLoad(data))
                } else {
                    Call.kick(it.player.con, Bundle(it.player.locale)["event.player.name.duplicate"])
                }
            } else {
                data.player = it.player
                Events.fire(CustomEvents.PlayerDataLoad(data))
            }
        }
    }.also { listener -> eventListeners[PlayerJoin::class.java] = listener })
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
            val hasVoted = runBlocking { getMapRating(data.uuid, mapName) != null }
            if (!hasVoted) {
                val rateMapMenu = Menus.registerMenu { player, select ->
                    if (select == 0) {
                        // Upvote
                        runBlocking {
                            // Get the map hash (MD5 hash of the map file)
                            val mapHash = calculateMapMD5Hash(currentMap)

                            // Save to database
                            updateOrCreateMapRating(mapName, mapHash, data.uuid, true)

                            // Keep in-memory cache for current session
                            mapRatings[data.uuid] = true
                        }

                        data.send("command.map.rate.upvote", mapName)
                    } else if (select == 1) {
                        // Downvote
                        runBlocking {
                            // Get the map hash (MD5 hash of the map file)
                            val mapHash = calculateMapMD5Hash(currentMap)

                            // Save to database
                            updateOrCreateMapRating(mapName, mapHash, data.uuid, false)

                            // Keep in-memory cache for current session
                            mapRatings[data.uuid] = false
                        }

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

    // Clear the world history database
    scope.launch {
        clearWorldHistory()
    }

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
                writeLog(
                    LogType.Block,
                    Bundle()["log.block.place", target.name, checkValidBlock(event.tile), event.tile.x, event.tile.y]
                )

                // Use both the ArrayList (for backward compatibility) and the database
                val buf = ArrayList<TileLog>()

                // Add entries from the ArrayList
                worldHistory.forEach { two ->
                    if (two.x == event.tile.x && two.y == event.tile.y) {
                        buf.add(two)
                    }
                }

                // Launch a coroutine to get entries from the database
                runBlocking {
                    try {
                        // Get entries from the database
                        val dbEntries = getWorldHistoryByCoordinates(event.tile.x.toShort(), event.tile.y.toShort())

                        // Convert database entries to TileLog objects
                        val dbTileLogs = dbEntries.map { entry ->
                            TileLog(
                                time = entry.time,
                                player = entry.player,
                                action = entry.action,
                                x = entry.x,
                                y = entry.y,
                                tile = entry.tile,
                                rotate = entry.rotate,
                                team = Team.all.find { it.name == entry.team } ?: Team.derelict,
                                value = entry.value
                            )
                        }

                        // Add database entries to the buffer if they're not already in the ArrayList
                        dbTileLogs.forEach { dbLog ->
                            if (!buf.any { it.time == dbLog.time && it.player == dbLog.player && it.action == dbLog.action }) {
                                buf.add(dbLog)
                            }
                        }

                        // Sort the buffer by time
                        buf.sortBy { it.time }
                    } catch (e: Exception) {
                        Log.err("Error retrieving world history from database", e)
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
                writeLog(
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
        writeLog(
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
    writeLog(LogType.Player, Bundle()["log.joined", event.player.plainName(), event.player.uuid(), event.player.con.address])
}

@Event
internal fun playerLeave(event: PlayerLeave) {
    writeLog(
        LogType.Player,
        Bundle()["log.player.disconnect", event.player.plainName(), event.player.uuid(), event.player.con.address]
    )
    val data = players.find { e -> e.uuid == event.player.uuid() }
    if (data != null) {
        data.lastPlayedWorldName = Vars.state.map.plainName()
        data.lastPlayedWorldMode = Vars.state.rules.modeName
        data.lastLogoutDate = Clock.System.now().toLocalDateTime(systemTimezone)
        data.isConnected = false
        scope.launch { data.update() }

        offlinePlayers.add(data)

        if (Administration.Config.autoPause.bool()) {
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
        }
        players.removeAll { e -> e.uuid == data.uuid }
    }
}

@Event
internal fun playerBan(event: PlayerBanEvent) {
    writeLog(
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

    // Load map ratings for the current map from the database
    val currentMapName = Vars.state.map.plainName()

    // Load ratings into in-memory cache for quick access during the current session
    runBlocking {
        val ratings = getMapRatings(currentMapName)
        for (rating in ratings) {
            mapRatings[rating.playerUuid] = rating.isUpvote
        }
    }

    // Migrate ratings from the old storage system if needed
    val savedRatings = pluginData.data.mapRatings[currentMapName]
    if (savedRatings != null && savedRatings.isNotEmpty()) {
        runBlocking {
            for ((uuid, isUpvote) in savedRatings) {
                // Only migrate if not already in the database
                if (getMapRating(uuid, currentMapName) == null) {
                    val mapHash = calculateMapMD5Hash(Vars.state.map)
                    updateOrCreateMapRating(currentMapName, mapHash, uuid, isUpvote)
                }
            }
        }
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
                writeLog(
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
    writeLog(
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

@Event
internal fun playerDataLoad(event: CustomEvents.PlayerDataLoad) {
    val playerData = event.playerData
    val player = playerData.player
    val message = StringBuilder()

    // 로그인 날짜 기록
    val currentTime = Clock.System.now().toLocalDateTime(systemTimezone)
    val isDayPassed = playerData.lastLoginDate.date.daysUntil(currentTime.date)
    if (isDayPassed >= 1) playerData.attendanceDays += 1
    playerData.lastLoginDate = currentTime

    // 권한 설정에 의한 닉네임 및 admin 권한 설정
    val permission = Permission[playerData]
    if (permission.name.isNotEmpty()) {
        playerData.player.name(Permission[playerData].name)
    }
    playerData.player.admin(Permission[playerData].admin)

    // 언어에 따라 각 언어별 motd 불러오기
    val motd = if (rootPath.child("motd/${player.locale()}.txt").exists()) {
        rootPath.child("motd/${player.locale()}.txt").readString()
    } else if (rootPath.child("motd").list().isNotEmpty()) {
        val file = rootPath.child("motd/en.txt")
        if (file.exists()) file.readString() else null
    } else {
        null
    }

    // 오늘의 메세지가 10줄 이상 넘어갈 경우 전체 화면으로 출력
    if (motd != null) {
        val count = motd.split("\r\n|\r|\n").toTypedArray().size
        if (count > 10) Call.infoMessage(player.con(), motd) else message.appendLine(motd)
    }

    // 입장 할 때 특정 메세지가 출력 되도록 설정 되어 있는 경우
    if (permission.isAlert) {
        if (permission.alertMessage.isEmpty()) {
            players.forEach { data ->
                data.send("event.player.joined", player.con())
            }
        } else {
            Call.sendMessage(permission.alertMessage)
        }
    }

    // 현재 PvP 모드일 경우
    if (Vars.state.rules.pvp) {
        when {
            // 이 플레이어가 이전에 참여한 팀이 있을 경우, 해당 팀에 다시 투입
            pvpPlayer.containsKey(playerData.uuid) -> {
                player.team(pvpPlayer[playerData.uuid])
            }

            // PvP 관전 기능이 켜져 있고, 관전 플레이어 이거나, 해당 플레이어의 권한 목록에 관전 권한이 있는 경우 관전 팀으로 설정
            conf.feature.pvp.spector && pvpSpecters.contains(playerData.uuid) || Permission.check(
                playerData,
                "pvp.spector"
            ) -> {
                player.team(Team.derelict)
            }


            conf.feature.pvp.autoTeam -> {
                val teamRate = mutableMapOf<Team, Double>()
                var teams = arrayOf<Pair<Team, Int>>()
                for (team in Vars.state.teams.active) {
                    var list = arrayOf<Pair<Team, Double>>()

                    players.forEach {
                        val rate =
                            it.pvpWinCount.toDouble() / (it.pvpWinCount + it.pvpLoseCount).toDouble()
                        list += Pair(it.player.team(), if (rate.isNaN()) 0.0 else rate)
                    }

                    teamRate[team.team] = list.filter { it.first == team }.map { it.second }.average()
                    teams += Pair(team.team, team.players.size)
                }

                val teamSorted = teams.toList().sortedByDescending { it.second }
                val rate = teamRate.toList().sortedWith(compareBy { it.second })
                if ((teamSorted.first().second - teamSorted.last().second) >= 2) {
                    player.team(teamSorted.last().first)
                } else {
                    player.team(rate.last().first)
                }
            }
        }
    }



    if (playerData.expMultiplier != 1.0) {
        message.appendLine(playerData.bundle["event.player.expboost", playerData.attendanceDays, playerData.expMultiplier])
    }

    playerData.isConnected = true
    players.add(playerData)
    playerNumber++
    player.sendMessage(message.toString())

    runBlocking {
        val data = getPlayerAchievements(playerData)
        data.forEach {
            playerData.achievementStatus.add(it.achievementName)
        }
    }

    Log.debug("${playerData.name} data loaded.")
    Events.fire(CustomEvents.PlayerDataLoadEnd(playerData))
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
        
        // Define a data class for the exp record
        @Serializable
        data class ExpRecord(
            val name: String,
            val uuid: String,
            val date: String,
            val erekirAttack: Int,
            val erekirPvP: Int,
            val time: Int,
            val enemyBuildingDestroyed: Int,
            val buildingsDestroyed: Int,
            val wave: Int,
            val multiplier: Double,
            val score: Int,
            val totalScore: Double
        )
        
        // Read existing JSON array
        val json = Json { 
            prettyPrint = true
            ignoreUnknownKeys = true
            isLenient = true
        }
        
        val jsonString = rootPath.child("data/exp.json").readString("UTF-8")
        val existingRecords = if (jsonString.isBlank() || jsonString == "[]") {
            listOf()
        } else {
            try {
                json.decodeFromString<List<ExpRecord>>(jsonString)
            } catch (e: Exception) {
                Log.err("Error parsing exp.json, creating new file", e)
                listOf()
            }
        }
        
        // Create new record
        val newRecord = ExpRecord(
            name = target.name,
            uuid = target.uuid,
            date = currentTime(),
            erekirAttack = erekirAttack,
            erekirPvP = erekirPvP,
            time = time,
            enemyBuildingDestroyed = target.currentUnitDestroyedCount,
            buildingsDestroyed = target.currentBuildDestroyedCount,
            wave = Vars.state.wave,
            multiplier = target.expMultiplier,
            score = score,
            totalScore = score * target.expMultiplier
        )
        
        // Add new record to list and write back to file
        val updatedRecords = existingRecords + newRecord
        rootPath.child("data/exp.json").writeString(json.encodeToString(updatedRecords))
    }

    if (isConnected && conf.feature.level.levelNotify) target.send(
        "event.exp.current",
        target.exp,
        result,
        target.level,
        target.level - oldLevel
    )
}

private fun addLog(log: TileLog) {
    // Add to the ArrayList for backward compatibility
    worldHistory.add(log)

    // Add to the database in a coroutine I/O scope
    scope.launch {
        createWorldHistory(
            time = log.time,
            player = log.player,
            action = log.action,
            x = log.x,
            y = log.y,
            tile = log.tile,
            rotate = log.rotate,
            team = log.team.name,
            value = log.value?.toString()
        )
    }
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

/**
 * Calculate the MD5 hash of a map file
 * @param map The map to calculate the hash for
 * @return The MD5 hash of the map file as a hexadecimal string
 */
private fun calculateMapMD5Hash(map: Map): String {
    try {
        val data = Files.readAllBytes(map.file.file().toPath())
        val hash = MessageDigest.getInstance("MD5").digest(data)
        return BigInteger(1, hash).toString(16)
    } catch (e: NoSuchAlgorithmException) {
        Log.err("Failed to calculate MD5 hash: ${e.message}")
        return ""
    } catch (e: IOException) {
        Log.err("Failed to read map file: ${e.message}")
        return ""
    }
}

private fun checkValidBlock(tile: Tile): String {
    return if (tile.build != null && blockSelectRegex.matcher(tile.block().name).matches()) {
        (tile.build as ConstructBlock.ConstructBuild).current.name
    } else {
        tile.block().name
    }
}
