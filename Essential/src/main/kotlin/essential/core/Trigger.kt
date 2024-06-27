package essential.core

import arc.Core
import arc.Events
import arc.func.Prov
import arc.struct.Seq
import arc.util.Time
import essential.core.Main.Companion.conf
import essential.core.Main.Companion.database
import essential.core.Main.Companion.root
import essential.core.PluginData.entityOrder
import mindustry.Vars
import mindustry.content.Blocks
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.gen.Playerc
import mindustry.net.Host
import mindustry.net.NetworkIO
import mindustry.world.Tile
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import java.nio.ByteBuffer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

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
                if ((LocalDate.now().toEpochDay() - data.lastLoginDate!!.toEpochDay()) == 1L) {
                    data.joinStacks += 1
                    when {
                        data.joinStacks >= 15 -> data.expMultiplier = 5.0
                        data.joinStacks >= 7 -> data.expMultiplier = 2.5
                        data.joinStacks >= 3 -> data.expMultiplier = 1.5
                    }
                } else if ((LocalDate.now().toEpochDay() - data.lastLoginDate!!.toEpochDay()) >= 2L) {
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

            val bundle = Bundle(data.languageTag)
            Events.fire(CustomEvents.PlayerDataLoaded(data.name, player.name(), player.uuid()))
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

                    conf.feature.pvp.spector && Event.pvpSpectors.contains(data.uuid) || Permission.check(
                        data,
                        "pvp.spector"
                    ) -> {
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

    object UpdateThread : Runnable {
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
}