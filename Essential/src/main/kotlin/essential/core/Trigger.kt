package essential.core

import arc.ApplicationListener
import arc.Core
import arc.Events
import arc.files.Fi
import arc.func.Prov
import arc.graphics.Color
import arc.graphics.Colors
import arc.struct.Seq
import arc.util.Align
import arc.util.Log
import arc.util.Time
import essential.core.Event.apmRanking
import essential.core.Event.coreListeners
import essential.core.Event.count
import essential.core.Event.dpsBlocks
import essential.core.Event.dpsTile
import essential.core.Event.earnEXP
import essential.core.Event.findPlayerData
import essential.core.Event.isAdminVote
import essential.core.Event.isCanceled
import essential.core.Event.isPvP
import essential.core.Event.isUnitInside
import essential.core.Event.lastVoted
import essential.core.Event.maxdps
import essential.core.Event.pvpPlayer
import essential.core.Event.pvpSpecters
import essential.core.Event.resetVote
import essential.core.Event.unitLimitMessageCooldown
import essential.core.Event.voteCooltime
import essential.core.Event.voteMap
import essential.core.Event.voteReason
import essential.core.Event.voteStarter
import essential.core.Event.voteTarget
import essential.core.Event.voteTargetUUID
import essential.core.Event.voteTeam
import essential.core.Event.voteType
import essential.core.Event.voteWave
import essential.core.Event.voted
import essential.core.Event.voterCooltime
import essential.core.Event.voting
import essential.core.Main.Companion.conf
import essential.core.Main.Companion.database
import essential.core.Main.Companion.root
import essential.core.PluginData.entityOrder
import mindustry.Vars
import mindustry.content.Blocks
import mindustry.content.Fx
import mindustry.content.UnitTypes
import mindustry.content.Weathers
import mindustry.entities.Effect
import mindustry.game.EventType.GameOverEvent
import mindustry.game.EventType.ServerLoadEvent
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.gen.Playerc
import mindustry.io.SaveIO
import mindustry.maps.Map
import mindustry.net.Host
import mindustry.net.NetworkIO
import mindustry.net.Packets
import mindustry.net.WorldReloader
import mindustry.world.Tile
import org.hjson.JsonArray
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.mindrot.jbcrypt.BCrypt
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import java.nio.ByteBuffer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import kotlin.math.floor
import kotlin.random.Random

class Trigger {
    fun loadPlayer(player: Playerc, data: DB.PlayerData, login: Boolean) {
        if (data.duplicateName != null && data.duplicateName == player.name()) {
            player.kick(Bundle(player.locale())["event.player.duplicate.name"])
        } else {
            if (data.duplicateName != null && data.duplicateName != player.name()) {
                data.name = player.name()
                data.duplicateName = null
            }

            if (data.lastLoginDate != null) {
                val passed = Period.between(data.lastLoginDate, LocalDate.now()).days
                if (passed == 1) {
                    data.joinStacks += 1
                    when {
                        data.joinStacks >= 15 -> data.expMultiplier = 5.0
                        data.joinStacks >= 7 -> data.expMultiplier = 2.5
                        data.joinStacks >= 3 -> data.expMultiplier = 1.5
                    }
                } else if (passed > 1) {
                    data.joinStacks = 0
                }
            }
            data.lastLoginDate = LocalDate.now()

            if (data.lastLeaveDate != null && data.lastLeaveDate!!.plusMinutes(30).isBefore(LocalDateTime.now())) {
                if (data.lastPlayedWorldId == Vars.port && (data.lastPlayedWorldName != Vars.state.map.name() || data.lastPlayedWorldMode != Vars.state.rules.modeName)) {
                    data.currentPlayTime = 0L
                }
            } else {
                data.currentPlayTime = 0L
            }

            val bundle = if (data.status.containsKey("language")) {
                Bundle(data.status["language"]!!)
            } else {
                Bundle(player.locale())
            }

            Events.fire(CustomEvents.PlayerDataLoaded(data))
            data.lastLoginTime = System.currentTimeMillis()
            data.totalJoinCount++
            data.player = player

            val message = StringBuilder()

            val perm = Permission[data]
            if (perm.name.isNotEmpty()) player.name(Permission[data].name)
            player.admin(Permission[data].admin)
            message.appendLine(bundle[if (login) "event.player.logged" else "event.player.loaded"])

            data.entityid = entityOrder
            entityOrder += 1

            if (!login) {
                val motd = if (root.child("motd/${data.languageTag}.txt").exists()) {
                    root.child("motd/${data.languageTag}.txt").readString()
                } else if (root.child("motd").list().isNotEmpty()) {
                    val file = root.child("motd/en.txt")
                    if (file.exists()) file.readString() else null
                } else {
                    null
                }
                if (motd != null) {
                    val count = motd.split("\r\n|\r|\n").toTypedArray().size
                    if (count > 10) Call.infoMessage(player.con(), motd) else message.appendLine(motd)
                }
            }

            if (perm.isAlert) {
                if (perm.alertMessage.isEmpty()) {
                    for (a in database.players) {
                        a.player.sendMessage(Bundle(a.languageTag)["event.player.joined", player.plainName()])
                    }
                } else {
                    Call.sendMessage(perm.alertMessage)
                }
            }

            if (Vars.state.rules.pvp) {
                when {
                    Event.pvpPlayer.containsKey(data.uuid) -> {
                        player.team(Event.pvpPlayer[data.uuid])
                    }

                    conf.feature.pvp.spector && Event.pvpSpecters.contains(data.uuid) || Permission.check(data, "pvp.spector") -> {
                        player.team(Team.derelict)
                    }

                    conf.feature.pvp.autoTeam -> {
                        fun winPercentage(team: Team): Double {
                            var players = arrayOf<Pair<Team, Double>>()
                            database.players.forEach {
                                val rate =
                                    it.pvpVictoriesCount.toDouble() / (it.pvpVictoriesCount + it.pvpDefeatCount).toDouble()
                                players += Pair(it.player.team(), if (rate.isNaN()) 0.0 else rate)
                            }

                            val targetTeam = players.filter { it.first == team }
                            val rate = targetTeam.map { it.second }
                            return rate.average()
                        }

                        val teamRate = mutableMapOf<Team, Double>()
                        var teams = arrayOf<Pair<Team, Int>>()
                        for (a in Vars.state.teams.active) {
                            val rate = winPercentage(a.team)
                            teamRate[a.team] = rate
                            teams += Pair(a.team, a.players.size)
                        }

                        val teamSorted = teams.toList().sortedByDescending { it.second }
                        val rateSorted = teamRate.toList().sortedWith(compareBy { it.second })
                        if ((teamSorted.first().second - teamSorted.last().second) >= 2) {
                            player.team(teamSorted.last().first)
                        } else {
                            player.team(rateSorted.last().first)
                        }
                    }
                }
            }

            if (Event.voting) {
                if (Event.voteStarter != null) message.appendLine(bundle["command.vote.starter", Event.voteStarter!!.player.plainName()])
                message.appendLine(
                    when (Event.voteType) {
                        "kick" -> bundle["command.vote.kick.start", Event.voteTarget!!.plainName(), Event.voteReason!!]
                        "map" -> bundle["command.vote.map.start", Event.voteMap!!.name(), Event.voteReason!!]
                        "gg" -> bundle["command.vote.gg.start"]
                        "skip" -> bundle["command.vote.skip.start", Event.voteWave!!]
                        "back" -> bundle["command.vote.back.start", Event.voteReason!!]
                        "random" -> bundle["command.vote.random.start"]
                        else -> ""
                    }
                )
                message.appendLine(bundle["command.vote.how"])
            }

            if (data.expMultiplier != 1.0) {
                message.appendLine(bundle["event.player.expboost", data.joinStacks, data.expMultiplier])
            }

            data.isConnected = true
            database.players.add(data)
            player.sendMessage(message.toString())
        }
    }

    fun createPlayer(player: Playerc, id: String?, password: String?) {
        val data = DB.PlayerData()
        data.name = player.name()
        data.uuid = player.uuid()
        data.firstPlayDate = System.currentTimeMillis()
        data.lastLoginDate = LocalDate.now()
        data.accountID = id ?: player.plainName()
        data.accountPW = if (password == null) player.plainName() else BCrypt.hashpw(password, BCrypt.gensalt())
        data.permission = "user"
        data.languageTag = player.locale()

        database.createData(data)
        Permission.apply()

        player.sendMessage(Bundle(player.locale())["event.player.data.registered"])
        loadPlayer(player, data, false)
    }

    fun checkUserExistsInDatabase(name: String, uuid: String): Boolean {
        return transaction {
            DB.Player.select(DB.Player.name).where {
                DB.Player.accountID.eq(name).and(DB.Player.uuid.eq(uuid))
                    .and(DB.Player.oldUUID.eq(uuid))
            }.firstOrNull() != null
        }
    }

    fun checkUserNameExists(name: String): Boolean {
        return transaction {
            DB.Player.select(DB.Player.name).where { DB.Player.name.eq(name) }.firstOrNull()
        } != null
    }

    class Thread : Runnable {
        private var ping = 0.000
        private val dummy = Player.create()

        private fun caculateCenter(startTile: Tile, endTile: Tile): Pair<Int, Int> {
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
            while (!java.lang.Thread.currentThread().isInterrupted) {
                try {
                    PluginData.load()

                    var total = 0
                    val serverInfo = getServerInfo()
                    for (a in serverInfo) {
                        total += a.players
                    }

                    if (Vars.state.isPlaying) {
                        for (i in 0 until PluginData.warpCounts.size) {
                            if (Vars.state.map.name() == PluginData.warpCounts[i].mapName) {
                                val value = PluginData.warpCounts[i]
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
                                                    Call.deconstructFinish(
                                                        Vars.world.tile(
                                                            tile.x + 4 + px,
                                                            tile.y + py
                                                        ), Blocks.air, dummy.unit()
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    dummy.x = tile.getX()
                                    dummy.y = tile.getY()

                                    //Core.app.post { Commands.Client(arrayOf(str), dummy).chars(tile) }
                                    PluginData.warpCounts[i] = PluginData.WarpCount(
                                        Vars.state.map.name(),
                                        value.tile.pos(),
                                        value.ip,
                                        value.port,
                                        info.players,
                                        digits.size
                                    )
                                } else {
                                    dummy.x = value.tile.getX()
                                    dummy.y = value.tile.getY()
                                    Core.app.post {
                                        //Commands.Client(arrayOf("no"), dummy).chars(value.tile)
                                    }
                                }
                            }
                        }

                        val memory = mutableListOf<Pair<Playerc, Triple<String, Float, Float>>>()
                        for (value in PluginData.warpBlocks) {
                            if (Vars.state.map.name() == value.mapName) {
                                val tile = Vars.world.tile(value.x, value.y)
                                if (tile.block() == Blocks.air) {
                                    PluginData.warpBlocks.remove(value)
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
                                    serverInfo.forEach {
                                        if ((it.address == value.ip || it.address == InetAddress.getByName(value.ip).hostAddress) && it.port == value.port) {
                                            alive = true
                                            alivePlayer = it.players
                                        }
                                    }

                                    if (alive) {
                                        if (isDup) y += 4
                                        Groups.player.forEach { a ->
                                            memory.add(
                                                a to Triple(
                                                    "[yellow]$alivePlayer[] ${Bundle(a.locale)["event.server.warp.players"]}",
                                                    x,
                                                    y
                                                )
                                            )
                                        }
                                        value.online = true
                                    } else {
                                        Groups.player.forEach { a ->
                                            memory.add(a to Triple(Bundle(a.locale)["event.server.warp.offline"], x, y))
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

                        for (value in PluginData.warpZones) {
                            if (Vars.state.map.name() == value.mapName) {
                                val center = caculateCenter(value.startTile, value.finishTile)

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

                        for (i in 0 until PluginData.warpTotals.size) {
                            val value = PluginData.warpTotals[i]
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

                                dummy.x = value.tile.getX()
                                dummy.y = value.tile.getY()
                                Core.app.post {
                                    //Commands.Client(arrayOf(getServerInfo().toString()), dummy).chars(value.tile)
                                }
                            }
                        }
                    }

                    /*if (Config.countAllServers) {
                        Core.settings.put("totalPlayers", total + Groups.player.size())
                        Core.settings.saveValues()
                    }*/

                    ping = 0.000
                    TimeUnit.SECONDS.sleep(3)
                } catch (e: InterruptedException) {
                    java.lang.Thread.currentThread().interrupt()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Core.app.exit()
                }
            }
        }

        @Throws(IOException::class, SocketException::class)
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

        private fun getServerInfo(): Seq<Host> {
            val total = Seq<Host>()
            var buf = arrayOf<Pair<String, Int>>()

            for (it in PluginData.warpBlocks) {
                buf += Pair(it.ip, it.port)
            }
            for (it in PluginData.warpCounts) {
                buf += Pair(it.ip, it.port)
            }
            for (it in PluginData.warpZones) {
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

    class UpdateThread : Runnable {
        private val queue = Seq<DB.PlayerData>()

        override fun run() {
            while (!java.lang.Thread.currentThread().isInterrupted) {
                for (a in queue) {
                    database.update(a.uuid, a)
                    queue.remove(a)
                }
                java.lang.Thread.sleep(200)
            }
        }
    }

    fun register() {
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

        val every = object : ApplicationListener {
            // var nextTime = System.nanoTime()

            override fun update() {
                /*val nextNanoTime = System.nanoTime()
                if (((nextNanoTime - nextTime) / 1_000_000) > 20) {
                    PluginData.effectLocal = true

                    println("effect local enabled")
                }
                nextTime = nextNanoTime*/

                for (it in database.players) {
                    if (Vars.state.rules.pvp && it.player.unit() != null && it.player.team().cores().isEmpty && it.player.team() != Team.derelict && pvpPlayer.containsKey(it.uuid)) {
                        it.pvpDefeatCount += 1
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
        }

        val second = object : ApplicationListener {
            private var random = Random
            private var tick = 0

            override fun update() {
                if (tick == 60) {
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
                        val message = "Max DPS: $maxdps/min\nDPS: $dpsBlocks/s"
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
                            it.player.name(rainbow(name))
                        } else if (!it.status.containsKey("router")) {
                            it.player.name(it.name)
                        }

                        // 잠수 플레이어 카운트
                        if (it.player.unit() != null && !it.player.unit().moving() && !it.player.unit().mining() && !Permission.check(it, "afk.admin") && it.previousMousePosition == it.player.mouseX() + it.player.mouseY()) {
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
                                            val color = color(unit.health, unit.maxHealth)
                                            if (unit.shield > 0) {
                                                val shield = shieldColor(unit.health, unit.maxHealth)
                                                msg.appendLine("$shield${floor(unit.shield.toDouble())}")
                                            }
                                            msg.append("$color${floor(unit.health.toDouble())}")

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
                                            SaveIO.load(savePath)

                                            Vars.state.rules = Vars.state.map.applyRules(mode)
                                            Vars.logic.play()
                                            reloader.end()

                                            savePath.delete()
                                        } catch (t: Exception) {
                                            t.printStackTrace()
                                        }
                                        send("command.vote.back.done")
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
                                                val map: Map
                                                send("command.vote.random.is")
                                                java.lang.Thread.sleep(3000)
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
                                                        java.lang.Thread.sleep(1000)
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
                                                            java.lang.Thread.sleep(1000)
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
                        val coloredName = if (it.second >= 41) rainbow(it.first) else it.first
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
                    tick = 0
                } else {
                    tick++
                }
            }
        }

        val minute = object : ApplicationListener {
            var tick = 0

            override fun update() {
                if (tick == 3600) {
                    if (Vars.state.rules.pvp) {
                        database.players.forEach {
                            if (!pvpPlayer.containsKey(it.uuid) && it.player.team() != Team.derelict) {
                                pvpPlayer[it.uuid] = it.player.team()
                            }
                        }
                    }

                    Main.daemon.submit(Thread {
                        transaction {
                            DB.Player.selectAll().where { DB.Player.banTime neq null }.forEach { data ->
                                val banTime = data[DB.Player.banTime]
                                val uuid = data[DB.Player.uuid]
                                val name = data[DB.Player.name]

                                if (LocalDateTime.now().isAfter(LocalDateTime.parse(banTime))) {
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
                    tick++
                } else {
                    tick += 1
                }
            }
        }

        val effect = object : ApplicationListener {
            inner class EffectPos(val player: Playerc, val effect: Effect, val rotate: Float, val color: Color, vararg val random: IntRange)
            private var tick = 0

            val buffer = ArrayList<EffectPos>()

            fun effect(data: DB.PlayerData) {
                val color = if (data.effectColor != null) {
                    if (Colors.get(data.effectColor) != null) Colors.get(data.effectColor) else Color.valueOf(
                        data.effectColor
                    )
                } else {
                    data.player.color()
                }

                fun runEffect(effect: Effect) {
                    buffer.add(EffectPos(data.player, effect, 0f, color))
                }

                fun runEffect(effect: Effect, size: Float) {
                    buffer.add(EffectPos(data.player, effect, size, color))
                }

                fun runEffectAtRotate(effect: Effect, rotate: Float) {
                    buffer.add(EffectPos(data.player, effect, rotate, color))
                }

                fun runEffectRandom(effect: Effect, range: IntRange) {
                    buffer.add(EffectPos(data.player, effect, 0f, color, range))
                }

                fun runEffectRandomRotate(effect: Effect) {
                    buffer.add(
                        EffectPos(
                            data.player,
                            effect,
                            Random.nextFloat() * 360f,
                            color
                        )
                    )
                }

                fun runEffectAtRotateAndColor(
                    effect: Effect,
                    rotate: Float,
                    customColor: Color
                ) {
                    buffer.add(EffectPos(data.player, effect, rotate, customColor))
                }

                when (data.effectLevel ?: data.level) {
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
                        runEffectRandom(Fx.hitLaserBlast, (-16..16))
                        runEffectRandom(Fx.hitSquaresColor, (-16..16))
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
                        buffer.add(
                            EffectPos(
                                data.player,
                                Fx.shootSmokeSquareBig,
                                listOf(0f, 90f, 180f, 270f).random(),
                                Color.HSVtoRGB(252f, 164f, 0f, 0.22f),
                                (-1..1)
                            )
                        )
                    }

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
                        var rot = data.player.unit().rotation
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

                    else -> {}
                }
            }

            override fun update() {
                if (!PluginData.effectLocal) {
                    if (tick == 5) {
                        val target = ArrayList<Playerc>()
                        database.players.asSequence().forEach {
                            if (it.showLevelEffects) {
                                target.add(it.player)
                            }

                            if (it.player.unit() != null && it.player.unit().health > 0f) {
                                effect(it)
                            }
                        }

                        buffer.forEach {
                            target.forEach { p ->
                                val x = if (it.random.isNotEmpty()) it.player.x + it.random[0].random() else it.player.x
                                val y = if (it.random.isNotEmpty()) it.player.y + it.random[0].random() else it.player.y
                                Call.effect(p.con(), it.effect, x, y, it.rotate, it.color)
                            }
                        }
                        buffer.clear()
                        tick = 0
                    }
                    tick++
                }
            }
        }

        Events.on(ServerLoadEvent::class.java) {
            coreListeners.add(every)
            coreListeners.add(second)
            coreListeners.add(minute)
            if (conf.feature.level.effect.enabled) {
                coreListeners.add(effect)
            }
            coreListeners.forEach {
                Core.app.addListener(it)
            }
        }
    }
}