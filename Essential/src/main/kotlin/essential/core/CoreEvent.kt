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
import ksp.event.Event
import essential.permission.Permission
import essential.util.currentTime
import essential.util.findPlayerData
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.toLocalDateTime
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
import mindustry.net.Administration
import mindustry.ui.Menus
import mindustry.world.Tile
import mindustry.world.blocks.ConstructBlock
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
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


var originalBlockMultiplier = 1f
var originalUnitMultiplier = 1f

internal var worldHistory = ArrayList<TileLog>()
private var dateformat = SimpleDateFormat("HH:mm:ss")
private var blockExp = mutableMapOf<String, Int>()

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
internal fun withdraw(it: WithdrawEvent) {
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
    }
}

@Event
internal fun deposit(it: DepositEvent) {
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
    }
}

@Event
internal fun config(it: ConfigEvent) {
    if (it.tile != null && it.tile.block != null && it.player != null) {
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
    }
}

@Event
internal fun tap(it: TapEvent) {
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
        pluginData.data.warpBlock.forEach { two ->
            if (two.mapName == Vars.state.map.name() && it.tile.block().name == two.tileName && it.tile.build.tileX() == two.x && it.tile.build.tileY() == two.y) {
                if (two.online) {
                    players.forEach { data ->
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

        for (two in pluginData.data.warpZone) {
            if (two.mapName == Vars.state.map.name() && two.click && isUnitInside(
                    it.tile,
                    two.startTile,
                    two.finishTile
                )
            ) {
                Log.info(Bundle()["log.warp.move", it.player.plainName(), two.ip, two.port.toString()])
                Call.connect(it.player.con(), two.ip, two.port)
                continue
            }
        }

        if (data.viewHistoryMode) {
            val buf = ArrayList<TileLog>()
            worldHistory.forEach { two ->
                if (two.x == it.tile.x && two.y == it.tile.y) {
                    buf.add(two)
                }
            }
            val str = StringBuilder()
            val bundle = data.bundle
            // todo 이거 파일 없음
            val coreBundle =
                Bundle(ResourceBundle.getBundle("mindustry/bundle", Locale.of(data.player.locale())))

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

            Call.effect(it.player.con(), Fx.shockwave, it.tile.getX(), it.tile.getY(), 0f, Color.cyan)
            if (str.toString().lines().size > 10) {
                str.append(bundle["event.log.position", it.tile.x, it.tile.y] + "\n")
                val lines: List<String> = str.toString().split("\n").reversed()
                for (i in 0 until 10) {
                    str.append(lines[i]).append("\n")
                }
                it.player.sendMessage(str.toString().trim())
            } else {
                it.player.sendMessage(
                    bundle["event.log.position", it.tile.x, it.tile.y] + "\n" + str.toString().trim()
                )
            }
        }

        if (data.status.containsKey("hub_first") && !data.status.containsKey("hub_second")) {
            data.status["hub_first"] = "${it.tile.x},${it.tile.y}"
            data.status["hub_second"] = "true"
            data.send("command.hub.zone.next", "${it.tile.x},${it.tile.y}")
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
                        it.tile.pos(),
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
                Call.effect(two.player.con(), Fx.bigShockwave, it.tile.getX(), it.tile.getY(), 0f, Color.cyan)
            }
        }
    }
}

@Event
internal fun wave(it: WaveEvent) {
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
internal fun serverLoad(it: ServerLoadEvent) {
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
            scope.launch {
                it.player.admin(false)
                val data = getPlayerData(it.player.uuid())

                val trigger = Trigger()
                if (data == null) {
                    newSuspendedTransaction {
                        if (PlayerTable.select(PlayerTable.name).where { PlayerTable.name eq it.player.name }
                                .empty()) {
                            createPlayerData(it.player)
                        } else {
                            Call.kick(it.player.con, Bundle(it.player.locale)["event.player.name.duplicate"])
                        }
                    }
                } else {
                    trigger.loadPlayer(data)
                }
            }
        }.also { listener -> eventListeners[PlayerJoin::class.java] = listener })
    }
}

@Event
internal fun playerChat(it: PlayerChatEvent) {

}

@Event
internal fun gameOver(it: GameOverEvent) {
    if (!Vars.state.rules.infiniteResources) {
        if (Vars.state.rules.pvp) {
            for (data in players) {
                if (data.player.team() == it.winner) {
                    data.pvpWinCount++
                }
            }
        } else if (Vars.state.rules.attackMode) {
            for (data in players) {
                if (data.player.team() == it.winner) {
                    data.attackClear++
                }
            }
        }
        for (data in players) {
            earnEXP(it.winner, data.player, data, true)
        }
        for (data in offlinePlayers) {
            earnEXP(it.winner, data.player, data, false)
        }
    }
    offlinePlayers.clear()
    worldHistory.clear()
    pvpSpecters.clear()
    pvpPlayer.clear()
}

@Event
internal fun blockBuildBegin(it: BlockBuildBeginEvent) {
    Events.on(BlockBuildBeginEvent::class.java, Cons<BlockBuildBeginEvent> {

    }.also { listener -> eventListeners[BlockBuildEndEvent::class.java] = listener })
}

@Event
internal fun blockBuildEnd(it: BlockBuildEndEvent) {
    val isDebug = Core.settings.getBool("debugMode")

    if (it.unit != null && it.unit.isPlayer) {
        val player = it.unit.player
        val target = findPlayerData(player.uuid())

        if (player.unit() != null && target != null && it.tile.block() != null && player.unit()
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
        }
    }
}

@Event
internal fun buildSelect(it: BuildSelectEvent) {
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
    }
}

@Event
internal fun blockDestroy(it: BlockDestroyEvent) {
    if (Vars.state.rules.attackMode) {
        for (a in players) {
            if (it.tile.team() != Vars.state.rules.defaultTeam) {
                a.currentBuildAttackCount++
            } else {
                a.currentBuildDestroyedCount++
            }
        }
    }
}

@Event
internal fun unitDestroy(it: UnitDestroyEvent) {
    if (!Vars.state.rules.pvp) {
        for (a in players) {
            if (it.unit.team() != a.player.team()) {
                a.currentUnitDestroyedCount++
            }
        }
    }
}

@Event
internal fun unitCreate(it: UnitCreateEvent) {
    if (conf.feature.unit.enabled && Groups.unit.size() > conf.feature.unit.limit) {
        it.unit.kill()

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
internal fun unitChange(it: UnitChangeEvent) {

}

@Event
internal fun playerJoin(it: PlayerJoin) {
    log(LogType.Player, Bundle()["log.joined", it.player.plainName(), it.player.uuid(), it.player.con.address])
}

@Event
internal fun playerLeave(it: PlayerLeave) {
    log(
        LogType.Player,
        Bundle()["log.player.disconnect", it.player.plainName(), it.player.uuid(), it.player.con.address]
    )
    val data = players.find { e -> e.uuid == it.player.uuid() }
    if (data != null) {
        data.lastPlayedWorldName = Vars.state.map.plainName()
        data.lastPlayedWorldMode = Vars.state.rules.modeName
        data.lastLogoutDate = Clock.System.now().toLocalDateTime(systemTimezone)
        data.isConnected = false
        scope.launch {
            data.update()
        }
        offlinePlayers.add(data)

        if (Vars.state.rules.pvp) {
            val b = Groups.player.copy()
            b.remove(it.player)
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
internal fun playerBan(it: PlayerBanEvent) {
    log(
        LogType.Player,
        Bundle()["log.player.banned", Vars.netServer.admins.getInfo(it.uuid).ips.first(), Vars.netServer.admins.getInfo(
            it.uuid
        ).names.first()]
    )
    scope.launch {
        createBanInfo(Vars.netServer.admins.getInfo(it.uuid), null)
    }
}

@Event
internal fun playerUnban(it: PlayerUnbanEvent) {
    Events.fire(CustomEvents.PlayerUnbanned(Vars.netServer.admins.getInfo(it.uuid).lastName, currentTime()))
    scope.launch {
        removeBanInfoByUUID(it.uuid)
    }
}

@Event
internal fun playerIpUnban(it: PlayerIpUnbanEvent) {
    Events.fire(CustomEvents.PlayerUnbanned(Vars.netServer.admins.findByIP(it.ip).lastName, currentTime()))
    scope.launch {
        removeBanInfoByIP(it.ip)
    }
}

@Event
internal fun worldLoad(it: WorldLoadEvent) {
    mapStartTime = timeSource.markNow()
    isSurrender = false
    isCheated = false
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
internal fun connectPacket(it: ConnectPacketEvent) {
    if (conf.feature.blacklist.enabled) {
        pluginData.data.blacklistedNames.forEach { text ->
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
}

@Event
internal fun playerConnect(it: PlayerConnect) {
    log(
        LogType.Player,
        Bundle()["event.player.connected", it.player.plainName(), it.player.uuid(), it.player.con.address]
    )
}

@Event
internal fun buildingBulletDestroy(it: BuildingBulletDestroyEvent) {
    val cores = listOf(
        Blocks.coreAcropolis,
        Blocks.coreBastion,
        Blocks.coreCitadel,
        Blocks.coreFoundation,
        Blocks.coreAcropolis,
        Blocks.coreNucleus,
        Blocks.coreShard
    )
    if (Vars.state.rules.pvp && it.build.closestCore() == null && cores.contains(it.build.block)) {
        for (data in players) {
            if (data.player.team() == it.bullet.team) {
                data.pvpEliminatedCount++
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
}

@Event
internal fun configFileModified(it: CustomEvents.ConfigFileModified) {
    if (it.kind == StandardWatchEventKinds.ENTRY_MODIFY) {
        when (it.paths) {
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