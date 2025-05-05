package essential.core

import arc.Core
import arc.Events
import arc.func.Prov
import arc.util.Log
import arc.util.Time
import essential.bundle
import essential.bundle.Bundle
import essential.core.Event.pvpPlayer
import essential.core.Event.pvpSpecters
import essential.core.Main.Companion.conf
import essential.database.data.PlayerData
import essential.database.data.PluginData
import essential.database.data.getPluginData
import essential.database.data.plugin.WarpCount
import essential.event.CustomEvents
import essential.permission.Permission
import essential.players
import essential.rootPath
import essential.systemTimezone
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.datetime.Clock
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import mindustry.Vars
import mindustry.content.Blocks
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Playerc
import mindustry.net.Host
import mindustry.net.NetworkIO
import mindustry.world.Tile
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.function.Consumer
import kotlin.coroutines.coroutineContext


class Trigger {
    fun loadPlayer(playerData: PlayerData) {
        val player = playerData.player
        val message = StringBuilder()

        // 로그인 날짜 기록
        val currentTime = Clock.System.now().toLocalDateTime(systemTimezone)
        val isDayPassed = playerData.lastLoginDate.daysUntil(currentTime.date)
        if (isDayPassed >= 1) playerData.attendanceDays += 1u
        playerData.lastLoginDate = currentTime.date

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
            message.appendLine(playerData.bundle()["event.player.expboost", playerData.attendanceDays, playerData.expMultiplier])
        }

        playerData.isConnected = true
        players.add(playerData)
        player.sendMessage(message.toString())

        playerData.send("event.player.loaded")
        Events.fire(CustomEvents.PlayerDataLoaded(playerData))
    }

    class Thread {
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

        suspend fun init() {
            var isNotTargetMap = false
            while (coroutineContext.isActive) {
                try {
                    val pluginData = getPluginData()

                    // 플러그인 데이터는 항상 존재 해야 합니다.
                    require(pluginData != null) {
                        bundle["plugin.data.null"]
                    }

                    val data = pluginData.data

                    isNotTargetMap = false
                    if (data.warpCount.none { f -> f.mapName == Vars.state.map.name() } &&
                        data.warpTotal.none { f -> f.mapName == Vars.state.map.name() } &&
                        data.warpZone.none { f -> f.mapName == Vars.state.map.name() } &&
                        data.warpBlock.none { f -> f.mapName == Vars.state.map.name() }) {
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
                                                        Vars.world.tile(tile.x + 4 + px, tile.y + py).setBlock(Blocks.air)
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
                            for (value in data.warpBlock) {
                                if (Vars.state.map.name() == value.mapName) {
                                    val tile = Vars.world.tile(value.x, value.y)
                                    if (tile.block() == Blocks.air) {
                                        data.warpBlock.remove(value)
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
                                            if ((it.address == value.ip || it.address == InetAddress.getByName(value.ip).hostAddress) && it.port == value.port) {
                                                alive = true
                                                alivePlayer = it.players
                                                currentMap = it.mapname
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
                                    if (value.totalplayers != total) {
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
                    delay(3000)
                } catch (e: Exception) {
                    Log.err(e)
                    Core.app.exit()
                }
            }
        }

        private fun pingHostImpl(address: String, port: Int, listener: Consumer<Host>) {
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
            } catch (e: Exception) {
                listener.accept(Host(0, null, null, null, 0, 0, 0, null, null, 0, null, null))
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
        Timer.schedule(object : Timer.Task() {
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

            override fun run() {
                pluginData.uptime++
                pluginData.playtime++

                if (pluginData.voteCooltime > 0) pluginData.voteCooltime--

                for ((key, value) in pluginData.voterCooltime) {
                    pluginData.voterCooltime[key] = value - 1
                }

                val voterCooltimeIterator = pluginData.voterCooltime.iterator()
                while (voterCooltimeIterator.hasNext()) {
                    if (voterCooltimeIterator.next().value == 0) {
                        voterCooltimeIterator.remove()
                    }
                }

                if (dpsTile != null) {
                    if (maxdps == null) {
                        maxdps = 0f
                    } else if (dpsBlocks > maxdps!!) {
                        maxdps = dpsBlocks
                    }
                    for (data in database.players) {
                        Call.label(
                            data.player.con(),
                            data.bundle().get("command.dps", maxdps!!, dpsBlocks),
                            1f,
                            dpsTile!!.worldx(),
                            dpsTile!!.worldy()
                        )
                    }
                } else {
                    maxdps = null
                }
                dpsBlocks = 0f

                apmRanking = "APM\n"
                val color = arrayOf("[scarlet]", "[orange]", "[yellow]", "[green]", "[white]", "[gray]")
                val list = LinkedHashMap<String, Int>()
                database.players.forEach {
                    val total = if (it.apm.size != 0) it.apm.max() else 0
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
                    val coloredName = if (it.second >= 41) rainbow(it.first) else it.first
                    apmRanking += "$coloredName[orange] > $colored${it.second}[white]\n"
                }
                apmRanking = apmRanking.substring(0, apmRanking.length - 1)

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
                        it.player.name(rainbow(name))
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
                                if (conf.feature.afk.server == null) {
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
                                    val server = conf.feature.afk.server!!.split(":")
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

                    val randomResult = (Random.nextInt(7) * it.expMultiplier).toInt()
                    it.exp += randomResult
                    it.currentExp += randomResult
                    Commands.Exp[it]

                    if (conf.feature.level.display) {
                        val message = "${it.exp}/${floor(Commands.Exp.calculateFullTargetXp(it.level)).toInt()}"
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
                                        val c = color(unit.health, unit.maxHealth)
                                        if (unit.shield > 0) {
                                            val shield = shieldColor(unit.health, unit.maxHealth)
                                            msg.appendLine("$shield${floor(unit.shield.toDouble())}")
                                        }
                                        msg.append("$c${floor(unit.health.toDouble())}")

                                        if (unit.team != it.player.team() && Permission.check(it, "hud.enemy")) {
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

                                "apm" -> {
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
                }

                if (unitLimitMessageCooldown > 0) {
                    unitLimitMessageCooldown--
                }
            }
        }, 0f, 1f)

        // 맵 백업 시간
        var rollbackCount = conf.command.rollback.time
        var messageCount = conf.feature.motd.time
        var messageOrder = 0

        Events.run(EventType.Trigger.update) {
            for (data in database.players) {
                if (Vars.state.rules.pvp && data.player.unit() != null && data.player.team()
                        .cores().isEmpty && data.player.team() != Team.derelict && pvpPlayer.containsKey(data.uuid)
                ) {
                    data.pvpDefeatCount += 1
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

                if (data.tracking) {
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

                if (data.tpp != null) {
                    val target = Groups.player.find { p -> p.uuid() == data.tpp }
                    if (target != null) {
                        Call.setCameraPosition(data.player.con(), target.x, target.y)
                    } else {
                        data.tpp = null
                        Call.setCameraPosition(data.player.con(), data.player.x, data.player.y)
                    }
                }

                for (two in pluginData.warpZones) {
                    if (two.mapName == Vars.state.map.name() && !two.click && isUnitInside(
                            data.player.unit().tileOn(),
                            two.startTile,
                            two.finishTile
                        )
                    ) {
                        Log.info(Bundle()["log.warp.move", data.player.plainName(), two.ip, two.port.toString()])
                        Call.connect(data.player.con(), two.ip, two.port)
                        break
                    }
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
        }

        Timer.schedule(object : Timer.Task() {
            override fun run() {
                if (Vars.state.rules.pvp) {
                    database.players.forEach {
                        if (!pvpPlayer.containsKey(it.uuid) && it.player.team() != Team.derelict && it.player.unit() != null) {
                            pvpPlayer[it.uuid] = it.player.team()
                        }
                    }
                }

                Main.daemon.submit(Thread {
                    transaction {
                        DB.PlayerTable.selectAll().where { DB.PlayerTable.banTime neq null }.forEach { data ->
                            val banTime = data[DB.PlayerTable.banTime]
                            val uuid = data[DB.PlayerTable.uuid]
                            val name = data[DB.PlayerTable.name]

                            if (LocalDateTime.now().isAfter(LocalDateTime.parse(banTime))) {
                                DB.PlayerTable.update({ DB.PlayerTable.uuid eq uuid }) {
                                    it[DB.PlayerTable.banTime] = null
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
            }
        }, 0f, 60f)

        Events.on(ServerLoadEvent::class.java) {
            if (conf.feature.level.effect.enabled) {
                Timer.schedule(EffectSystem(), 0f, 0.05f)
            }
            coreListeners.forEach {
                Core.app.addListener(it)
            }
        }
    }
}