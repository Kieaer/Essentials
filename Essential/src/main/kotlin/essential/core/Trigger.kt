package essential.core

import arc.Core
import arc.Events
import arc.func.Prov
import arc.graphics.Color
import arc.util.Align
import arc.util.Log
import arc.util.Time
import arc.util.Timer
import essential.common.bundle.Bundle
import essential.common.database.data.PluginData
import essential.common.database.data.cleanupExpiredRoutingPermissions
import essential.common.database.data.grantRoutingPermission
import essential.common.database.data.plugin.WarpCount
import essential.common.permission.Permission
import essential.common.players
import essential.common.pluginData
import essential.common.rootPath
import essential.common.systemTimezone
import essential.common.util.findPlayerData
import essential.core.Main.Companion.conf
import essential.core.Main.Companion.scope
import essential.core.service.effect.EffectSystem
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.toLocalDateTime
import mindustry.Vars
import mindustry.content.Blocks
import mindustry.content.Fx
import mindustry.game.EventType
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Playerc
import mindustry.io.SaveIO
import mindustry.net.Host
import mindustry.net.NetworkIO
import mindustry.world.Tile
import java.lang.Thread.currentThread
import java.lang.Thread.sleep
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.util.function.Consumer
import kotlin.math.floor
import kotlin.random.Random
import kotlin.time.Instant


class Trigger {
    companion object {
        fun pingHostImpl(address: String, port: Int, listener: Consumer<Host>) {
            val packetSupplier: Prov<DatagramPacket> = Prov<DatagramPacket> { DatagramPacket(ByteArray(512), 512) }

            try {
                DatagramSocket().use { socket ->
                    val s: Long = Time.millis()
                    socket.send(DatagramPacket(byteArrayOf(-2, 1), 2, InetAddress.getByName(address), port))
                    socket.soTimeout = 1000
                    val packet: DatagramPacket = packetSupplier.get()
                    socket.receive(packet)
                    val buffer = ByteBuffer.wrap(packet.data)
                    val host =
                        NetworkIO.readServerData(Time.timeSinceMillis(s).toInt(), packet.address.hostAddress, buffer)
                    host.port = port
                    listener.accept(host)
                }
            } catch (_: Exception) {
                listener.accept(Host(0, null, null, null, 0, 0, 0, null, null, 0, null, null))
            }
        }
    }

    class PingThread: Runnable {
        private var ping = 0.000

        private fun calculateCenter(startTile: Tile, endTile: Tile): Pair<Int, Int> {
            data class Point(val x: Int, val y: Int)

            data class Tile(val coordinates: Point, val areaValue: Float)

            fun calculateAreaValue(x: Int, y: Int): Double {
                return (x + y) / 2.0
            }

            fun findMedianCoordinates(
                startPoint: mindustry.world.Tile,
                endPoint: mindustry.world.Tile
            ): Pair<Int, Int> {
                val regionWidth =
                    if (endPoint.x > startPoint.x) endPoint.x - startPoint.x else startPoint.x - endPoint.x
                val regionHeight =
                    if (endPoint.y > startPoint.y) endPoint.y - startPoint.y else startPoint.y - endPoint.y
                val totalTiles = regionWidth * regionHeight
                val tiles = mutableListOf<Tile>()

                fun search(y: Int) {
                    if (endPoint.x > startPoint.x) {
                        for (x in startPoint.x until endPoint.x) {
                            val areaValue = calculateAreaValue(x, y)
                            val tile = Tile(Point(x, y), areaValue.toFloat())
                            tiles.add(tile)
                        }
                    } else {
                        for (x in endPoint.x until startPoint.x) {
                            val areaValue = calculateAreaValue(x, y)
                            val tile = Tile(Point(x, y), areaValue.toFloat())
                            tiles.add(tile)
                        }
                    }
                }

                if (endPoint.y > startPoint.y) {
                    for (y in startPoint.y until endPoint.y) {
                        search(y)
                    }
                } else {
                    for (y in endPoint.y until startPoint.y) {
                        search(y)
                    }
                }

                tiles.sortBy { it.areaValue }

                val medianIndex = totalTiles / 2
                val medianTile = tiles[medianIndex]

                return Pair(medianTile.coordinates.x, medianTile.coordinates.y)
            }

            return findMedianCoordinates(startTile, endTile)
        }

        override fun run() {
            var isNotTargetMap: Boolean
            try {
                while (currentThread().isInterrupted.not()) {
                    val data = pluginData.data

                    isNotTargetMap = false
                    if (data.warpCount.none { f -> f.mapName == Vars.state.map.name() } &&
                        data.warpTotal.none { f -> f.mapName == Vars.state.map.name() } &&
                        data.warpZone.none { f -> f.mapName == Vars.state.map.name() } &&
                        data.warpBlock.none { f -> f.mapName == Vars.state.map.name() }
                    ) {
                        isNotTargetMap = true
                    }

                    if (!isNotTargetMap) {
                        var total = 0
                        val serverInfo = getServerInfo(pluginData)
                        for (a in serverInfo) {
                            total += a.players
                        }

                        if (Vars.state.isPlaying) {
                            for (i in 0 until data.warpCount.size) {
                                if (Vars.state.map.name() == data.warpCount[i].mapName) {
                                    val value = data.warpCount[i]
                                    val info = serverInfo.find { a -> a.address == value.ip && a.port == value.port }
                                    if (info != null) {
                                        val str = info.players.toString()
                                        val digits = IntArray(str.length)
                                        for (a in str.indices) digits[a] = str[a] - '0'
                                        val tile = value.tile
                                        if (value.players != info.players) {
                                            Core.app.post {
                                                for (px in 0..2) {
                                                    for (py in 0..4) {
                                                        Vars.world.tile(tile.x + 4 + px, tile.y + py)
                                                            .setBlock(Blocks.air)
                                                    }
                                                }
                                            }
                                        }

                                        data.warpCount[i] = WarpCount(
                                            Vars.state.map.name(),
                                            value.tile.pos(),
                                            value.ip,
                                            value.port,
                                            info.players,
                                            digits.size
                                        )
                                    }
                                }
                            }

                            val memory = mutableListOf<Pair<Playerc, Triple<String, Float, Float>>>()
                            val iterator = data.warpBlock.iterator()
                            while (iterator.hasNext()) {
                                val value = iterator.next()
                                if (Vars.state.map.name() == value.mapName) {
                                    val tile = Vars.world.tile(value.x, value.y)
                                    if (tile.block() == Blocks.air) {
                                        iterator.remove()
                                    } else {
                                        var margin = 0f
                                        var isDup = false
                                        val x = tile.build.getX()

                                        when (value.size) {
                                            1 -> margin = 8f
                                            2 -> {
                                                margin = 16f
                                                isDup = true
                                            }

                                            3 -> margin = 16f
                                            4 -> {
                                                margin = 24f
                                                isDup = true
                                            }

                                            5 -> margin = 24f
                                            6 -> {
                                                margin = 32f
                                                isDup = true
                                            }

                                            7 -> margin = 32f
                                        }

                                        var y = tile.build.getY() + if (isDup) margin - 8 else margin

                                        var alive = false
                                        var alivePlayer = 0
                                        var currentMap = ""
                                        serverInfo.forEach {
                                            try {
                                                val address = InetAddress.getByName(value.ip).hostAddress
                                                if ((it.address == value.ip || it.address == address) && it.port == value.port) {
                                                    alive = true
                                                    alivePlayer = it.players
                                                    currentMap = it.mapname
                                                }
                                            } catch (_: UnknownHostException) {
                                                Log.warn("Could not find a matching address $value.ip:$value.port")
                                            } catch (_: Exception) {

                                            }
                                        }

                                        if (alive) {
                                            if (isDup) y += 4
                                            Groups.player.forEach { a ->
                                                memory.add(
                                                    a to Triple(
                                                        "$currentMap\n[white][yellow]$alivePlayer[] ${Bundle(a.locale)["event.server.warp.players"]}",
                                                        x,
                                                        y
                                                    )
                                                )
                                            }
                                            value.online = true
                                        } else {
                                            Groups.player.forEach { a ->
                                                memory.add(
                                                    a to Triple(
                                                        Bundle(a.locale)["event.server.warp.offline"],
                                                        x,
                                                        y
                                                    )
                                                )
                                            }
                                            value.online = false
                                        }

                                        if (isDup) margin -= 4
                                        Groups.player.forEach { a ->
                                            memory.add(a to Triple(value.description, x, tile.build.getY() - margin))
                                        }
                                    }
                                }
                            }

                            for (value in data.warpZone) {
                                if (Vars.state.map.name() == value.mapName) {
                                    val center = calculateCenter(value.startTile, value.finishTile)

                                    var alive = false
                                    var alivePlayer = 0
                                    serverInfo.forEach {
                                        if ((it.address == value.ip || it.address == InetAddress.getByName(value.ip).hostAddress) && it.port == value.port) {
                                            alive = true
                                            alivePlayer = it.players
                                        }
                                    }

                                    // todo 중앙 정렬 안됨
                                    if (alive) {
                                        for (a in Groups.player) {
                                            memory.add(
                                                a to Triple(
                                                    "[yellow]$alivePlayer[] ${Bundle(a.locale)["event.server.warp.players"]}",
                                                    (center.first * 8).toFloat(),
                                                    (center.second * 8).toFloat()
                                                )
                                            )
                                        }
                                    } else {
                                        for (a in Groups.player) {
                                            memory.add(
                                                a to Triple(
                                                    Bundle(a.locale)["event.server.warp.offline"],
                                                    (center.first * 8).toFloat(),
                                                    (center.second * 8).toFloat()
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            for (m in memory) {
                                Core.app.post {
                                    Call.label(
                                        m.first.con(),
                                        m.second.first,
                                        ping.toFloat() + 3f,
                                        m.second.second,
                                        m.second.third
                                    )
                                }
                            }

                            for (i in 0 until data.warpTotal.size) {
                                val value = data.warpTotal[i]
                                if (Vars.state.map.name() == value.mapName) {
                                    if (value.totalPlayers != total) {
                                        when (total) {
                                            0, 1, 2, 3, 4, 5, 6, 7, 8, 9 -> {
                                                for (px in 0..2) {
                                                    for (py in 0..4) {
                                                        Core.app.post {
                                                            Call.setTile(
                                                                Vars.world.tile(
                                                                    value.tile.x + px,
                                                                    value.tile.y + py
                                                                ), Blocks.air, Team.sharded, 0
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            else -> {
                                                for (px in 0..5) {
                                                    for (py in 0..4) {
                                                        Core.app.post {
                                                            Call.setTile(
                                                                Vars.world.tile(
                                                                    value.tile.x + 4 + px,
                                                                    value.tile.y + py
                                                                ), Blocks.air, Team.sharded, 0
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (conf.feature.count) {
                            Core.settings.put("totalPlayers", total + Groups.player.size())
                        }
                    }

                    ping = 0.000
                    sleep(3000)
                }
            } catch (_: InterruptedException) {
                currentThread().interrupt()
            } catch (e: Exception) {
                Log.err(e)
                Core.app.exit()
            }

            runBlocking {
                cleanupExpiredRoutingPermissions()
            }
        }

        private fun getServerInfo(pluginData: PluginData): MutableSet<Host> {
            val total = mutableSetOf<Host>()
            var buf = arrayOf<Pair<String, Int>>()

            for (it in pluginData.data.warpBlock) {
                buf += Pair(it.ip, it.port)
            }
            for (it in pluginData.data.warpCount) {
                buf += Pair(it.ip, it.port)
            }
            for (it in pluginData.data.warpZone) {
                buf += Pair(it.ip, it.port)
            }
            for (a in buf) {
                pingHostImpl(a.first, a.second) {
                    if (it.name != null) {
                        total.add(it)
                    }
                }
            }

            return total
        }
    }

    fun register() {
        var colorOffset = 0
        fun rainbow(name: String): String {
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
                val new = colors[colorIndex] + c
                newName[i] = new
            }
            colorOffset--
            newName.forEach {
                stringBuilder.append(it)
            }
            return stringBuilder.toString()
        }

        // 맵 백업 시간
        var rollbackCount = conf.command.rollback.time
        var messageCount = conf.feature.motd.time
        var messageOrder = 0
        var dpsBlockCalculateTick = 0

        Events.run(EventType.Trigger.update) {
            for (data in players) {
                if (Vars.state.rules.pvp && data.player.unit() != null && data.player.team()
                        .cores().isEmpty && data.player.team() != Team.derelict && pvpPlayer.containsKey(data.uuid)
                ) {
                    data.pvpLoseCount++
                    if (conf.feature.pvp.spector) {
                        data.player.team(Team.derelict)
                        pvpSpecters.add(data.uuid)
                    }
                    pvpPlayer.remove(data.uuid)

                    val time = data.currentPlayTime
                    val score = time + 5000

                    data.exp += ((score * data.expMultiplier).toInt())
                    data.send("event.exp.earn.defeat", data.currentExp + score)
                }

                if (data.status.containsKey("freeze")) {
                    val d = findPlayerData(data.uuid)
                    if (d != null) {
                        val player = d.player
                        val split = data.status["freeze"].toString().split("/")
                        player[split[0].toFloat()] = split[1].toFloat()
                        Call.setPosition(player.con(), split[0].toFloat(), split[1].toFloat())
                        Call.setCameraPosition(player.con(), split[0].toFloat(), split[1].toFloat())
                    }
                }

                if (data.status.containsKey("chars_text") && Time.globalTime.toInt() % 30 == 0) {
                    val text = Commands.charsPlacing[data.uuid]
                    if (text != null) {
                        val startX = (data.player.mouseX() / 8f).toInt()
                        val startY = (data.player.mouseY() / 8f).toInt()
                        var x = startX
                        var y = startY
                        for (line in text) {
                            for (char in line) {
                                if (char == '#' && Vars.world.tile(x, y) != null) {
                                    Call.effect(data.player.con(), mindustry.content.Fx.placeBlock, x * 8f + 4f, y * 8f + 4f, 1f, arc.graphics.Color.green)
                                }
                                x++
                            }
                            y--
                            x = startX
                        }
                    }
                }

                val weSelection = worldEditSelection[data.uuid]
                if (weSelection != null && Time.globalTime.toInt() % 15 == 0) {
                    if ((weSelection.selecting && weSelection.startX != -1 && weSelection.startY != -1) || weSelection.selectionComplete) {
                        val startX = weSelection.startX
                        val startY = weSelection.startY
                        val endX = if (weSelection.selecting) (data.player.mouseX() / 8f).toInt() else weSelection.endX
                        val endY = if (weSelection.selecting) (data.player.mouseY() / 8f).toInt() else weSelection.endY

                        val minX = minOf(startX, endX)
                        val maxX = maxOf(startX, endX)
                        val minY = minOf(startY, endY)
                        val maxY = maxOf(startY, endY)

                        // Top & Bottom edges
                        for (x in minX..maxX) {
                            Call.effect(data.player.con(), Fx.fire, x * 8f + 4f, minY * 8f + 4f, 0f, Color.orange)
                            Call.effect(data.player.con(), Fx.fire, x * 8f + 4f, maxY * 8f + 4f, 0f, Color.orange)
                        }
                        // Left & Right edges
                        for (y in minY..maxY) {
                            Call.effect(data.player.con(), Fx.fire, minX * 8f + 4f, y * 8f + 4f, 0f, Color.orange)
                            Call.effect(data.player.con(), Fx.fire, maxX * 8f + 4f, y * 8f + 4f, 0f, Color.orange)
                        }
                    }
                }

                if (data.mouseTracking) {
                    Groups.player.forEach { player ->
                        Call.label(
                            data.player.con(),
                            player.name,
                            Time.delta / 2,
                            player.mouseX,
                            player.mouseY
                        )
                    }
                }

                for (two in pluginData.data.warpZone) {
                    if (two.mapName == Vars.state.map.name() && !two.click && isUnitInside(
                            data.player.unit().tileOn(),
                            two.startTile,
                            two.finishTile
                        )
                    ) {
                        Log.info(Bundle()["log.warp.move", data.player.plainName(), two.ip, two.port.toString()])

                        val currentMapName = Vars.state.map.name()
                        val hubMapName = pluginData.hubMapName
                        if (hubMapName != null && currentMapName == hubMapName) {
                            scope.launch {
                                val targetServerName = "${two.ip}:${two.port}"
                                val hubConnectionTime = Instant.fromEpochMilliseconds(data.player.con().connectTime).toLocalDateTime(systemTimezone)
                                grantRoutingPermission(data.player.uuid(), hubMapName, targetServerName, two.port, hubConnectionTime)
                                Call.connect(data.player.con(), two.ip, two.port)
                            }
                        } else {
                            Call.connect(data.player.con(), two.ip, two.port)
                        }
                        break
                    }
                }
            }

            if (dpsTile != null) {
                if (dpsTile!!.build != null && dpsTile!!.block() != null) {
                    dpsBlocks += (100000000f - dpsTile!!.build.health)
                    dpsTile!!.build.maxHealth(100000000f)
                    dpsTile!!.build.health(100000000f)
                } else {
                    dpsTile = null
                }
            }

            if (dpsBlockCalculateTick == 60) {
                if (dpsTile != null) {
                    if (maxDps == null) {
                        maxDps = 0f
                    } else if (dpsBlocks > maxDps!!) {
                        maxDps = dpsBlocks
                    }
                    for (data in players) {
                        Call.label(
                            data.player.con(),
                            data.bundle["command.dps", maxDps!!, dpsBlocks],
                            1f,
                            dpsTile!!.worldx(),
                            dpsTile!!.worldy()
                        )
                    }
                } else {
                    maxDps = null
                }
                dpsBlocks = 0f
                dpsBlockCalculateTick = 0
            } else {
                dpsBlockCalculateTick += 1
            }
        }

        Timer.schedule({
            players.forEach {
                it.totalPlayed++
                it.currentPlayTime++

                if (it.animatedName) {
                    val name = it.name.replace("\\[(.*?)]".toRegex(), "")
                    it.player.name(rainbow(name))
                } else if (!it.status.containsKey("router")) {
                    it.player.name(it.name)
                }

                // 잠수 플레이어 카운트
                if (it.player.unit() != null &&
                    !it.player.unit().moving() &&
                    !it.player.unit().mining() &&
                    !Permission.check(it, "afk.admin") &&
                    it.mousePosition == it.player.mouseX() + it.player.mouseY()
                ) {
                    it.afkTime++
                    if (it.afkTime == conf.feature.afk.time.toUShort()) {
                        it.afk = true
                        if (conf.feature.afk.enabled) {
                            if (conf.feature.afk.server == null) {

                                it.player.kick(it.bundle["event.player.afk"])

                                players.forEach { data ->
                                    data.send("event.player.afk.other", it.player.plainName())
                                }
                            } else {
                                val server = conf.feature.afk.server!!.split(":")
                                val port = if (server.size == 1) {
                                    6567
                                } else {
                                    server[1].toInt()
                                }

                                val currentMapName = Vars.state.map.name()
                                val hubMapName = pluginData.hubMapName
                                if (hubMapName != null && currentMapName == hubMapName) {
                                    scope.launch {
                                        val targetServerName = if (server.size == 1) "${server[0]}:6567" else "${server[0]}:${server[1]}"
                                        val hubConnectionTime = Instant.fromEpochMilliseconds(it.player.con().connectTime).toLocalDateTime(systemTimezone)
                                        grantRoutingPermission(it.player.uuid(), hubMapName, targetServerName, port, hubConnectionTime)
                                        Log.debug("Granted routing permission for ${it.player.plainName()} to $targetServerName (AFK)")
                                        Call.connect(it.player.con(), server[0], port)
                                    }
                                } else {
                                    Call.connect(it.player.con(), server[0], port)
                                }
                            }
                        }
                    }
                } else {
                    it.afkTime = 0u
                    it.afk = false
                    it.mousePosition = it.player.mouseX() + it.player.mouseY()
                }

                val randomResult = (Random.nextInt(7) * it.expMultiplier)
                it.exp += randomResult.toInt()
                it.currentExp += randomResult.toInt()
                Commands.Exp[it]

                if (conf.feature.level.display) {
                    val message = "${it.exp}/${floor(Commands.Exp.calculateFullTargetXp(it.level)).toInt()}"
                    Call.infoPopup(it.player.con(), message, Time.delta, Align.left, 0, 0, 300, 0)
                }
            }
        }, 0f, 1f)

        Timer.schedule({
            if (Vars.state.rules.pvp) {
                players.forEach {
                    if (!pvpPlayer.containsKey(it.uuid) && it.player.team() != Team.derelict && it.player.unit() != null) {
                        pvpPlayer[it.uuid] = it.player.team()
                    }
                }
            }

            if (rollbackCount == 0) {
                val timestamp = System.currentTimeMillis()
                val backupFile = Vars.saveDirectory.child("rollback_$timestamp.msav")
                SaveIO.save(backupFile)

                val files = Vars.saveDirectory.findAll { f -> f.name().startsWith("rollback_") && f.name().endsWith(".msav") }
                val sortedFiles = files.sortedBy { it.lastModified() }
                if (sortedFiles.size > conf.command.rollback.limit) {
                    for (i in 0 until (sortedFiles.size - conf.command.rollback.limit)) {
                        sortedFiles[i].delete()
                    }
                }

                rollbackCount = conf.command.rollback.time
            } else {
                rollbackCount--
            }

            if (conf.feature.motd.enabled) {
                if (messageCount == conf.feature.motd.time) {
                    players.forEach {
                        val message = if (rootPath.child("messages/${it.player.locale()}.txt").exists()) {
                            rootPath.child("messages/${it.player.locale()}.txt").readString()
                        } else if (rootPath.child("messages").list().isNotEmpty()) {
                            val file = rootPath.child("messages/en.txt")
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
        }, 0f, 60f)

        Events.on(EventType.ServerLoadEvent::class.java) {
            if (conf.feature.level.effect.enabled) {
                Timer.schedule(EffectSystem(), 0f, 0.05f)
            }
            coreListeners.forEach {
                Core.app.addListener(it)
            }
        }
    }
}
