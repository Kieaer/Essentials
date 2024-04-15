package essentials

import arc.Core
import arc.func.Prov
import arc.struct.Seq
import arc.util.Log
import arc.util.Time
import essentials.Main.Companion.database
import essentials.Main.Companion.root
import mindustry.Vars.*
import mindustry.content.Blocks
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.gen.Playerc
import mindustry.net.Host
import mindustry.net.NetworkIO
import mindustry.world.Tile
import org.hjson.JsonArray
import org.mindrot.jbcrypt.BCrypt
import java.io.*
import java.lang.Thread.currentThread
import java.lang.Thread.sleep
import java.net.*
import java.nio.ByteBuffer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

object Trigger {
    private var order = 0
    val clients = Seq<Socket>()

    fun loadPlayer(player : Playerc, data : DB.PlayerData, login : Boolean) {
        if (data.duplicateName != null && data.duplicateName == player.name()) {
            player.kick(Bundle(player.locale())["event.player.duplicate.name"])
        } else {
            if (data.duplicateName != null && data.duplicateName != player.name()) {
                data.name = player.name()
                data.duplicateName = null
            }

            if (data.lastLoginDate != null) {
                if ((LocalDate.now(ZoneOffset.UTC).toEpochDay() - data.lastLoginDate!!.toEpochDay()) == 1L) {
                    data.joinStacks++
                    when {
                        data.joinStacks >= 15 -> data.expMultiplier = 5.0
                        data.joinStacks >= 7 -> data.expMultiplier = 2.5
                        data.joinStacks >= 3 -> data.expMultiplier = 1.5
                    }
                } else if ((LocalDate.now(ZoneOffset.UTC).toEpochDay() - data.lastLoginDate!!.toEpochDay()) >= 2L) {
                    data.joinStacks = 0
                }
            }
            data.lastLoginDate = LocalDate.now()

            if (data.lastLeaveDate != null && data.lastLeaveDate!!.plusMinutes(30).isBefore(LocalDateTime.now())) {
                if (data.lastPlayedWorldId == port && (data.lastPlayedWorldName != state.map.name() || data.lastPlayedWorldMode != state.rules.modeName)) {
                    data.currentPlayTime = 0L
                }
            } else {
                data.currentPlayTime = 0L
            }

            val bundle = Bundle(data.languageTag)
            if (Config.fixedName) {
                if (player.name() != data.name) {
                    database.players.forEach {
                        it.player.sendMessage(Bundle(it.languageTag)["event.player.name.changed", player.plainName(), data.name])
                    }
                }
                player.name(data.name)
            }
            data.lastLoginTime = System.currentTimeMillis()
            data.totalJoinCount++
            data.player = player

            val message = StringBuilder()

            val perm = Permission[data]
            if (perm.name.isNotEmpty()) player.name(Permission[data].name)
            player.admin(Permission[data].admin)
            message.appendLine(bundle[if (login) "event.player.logged" else "event.player.loaded"])

            data.entityid = order
            order++

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

            if (state.rules.pvp) {
                when {
                    Event.pvpPlayer.containsKey(data.uuid) -> {
                        player.team(Event.pvpPlayer[data.uuid])
                    }
                    Config.pvpSpector && Event.pvpSpectors.contains(data.uuid) || Permission.check(data, "pvp.spector") -> {
                        player.team(Team.derelict)
                    }
                    Config.pvpAutoTeam -> {
                        fun winPercentage(team : Team) : Double {
                            var players = arrayOf<Pair<Team, Double>>()
                            database.players.forEach {
                                val rate = it.pvpVictoriesCount.toDouble() / (it.pvpVictoriesCount + it.pvpDefeatCount).toDouble()
                                players += Pair(it.player.team(), if (rate.isNaN()) 0.0 else rate)
                            }

                            val targetTeam = players.filter { it.first == team }
                            val rate = targetTeam.map { it.second }
                            return rate.average()
                        }

                        val teamRate = mutableMapOf<Team, Double>()
                        var teams = arrayOf<Pair<Team, Int>>()
                        for (a in state.teams.active) {
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
                message.appendLine(when (Event.voteType) {
                    "kick" -> bundle["command.vote.kick.start", Event.voteTarget!!.plainName(), Event.voteReason!!]
                    "map" -> bundle["command.vote.map.start", Event.voteMap!!.name(), Event.voteReason!!]
                    "gg" -> bundle["command.vote.gg.start"]
                    "skip" -> bundle["command.vote.skip.start", Event.voteWave!!]
                    "back" -> bundle["command.vote.back.start", Event.voteReason!!]
                    "random" -> bundle["command.vote.random.start"]
                    else -> ""
                })
                message.appendLine(bundle["command.vote.how"])
            }

            if (data.expMultiplier != 1.0) {
                message.appendLine(bundle["event.player.expboost", data.joinStacks, data.expMultiplier])
            }

            data.isConnected = true
            database.players.add(data)
            player.sendMessage(message.toString())

            if (Config.authType == Config.AuthType.Discord && data.discord.isNullOrEmpty()) {
                player.sendMessage(bundle["event.discord.not.registered"])
            }
        }
    }

    fun createPlayer(player : Playerc, id : String?, password : String?) {
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

    class Thread: Runnable {
        private var ping = 0.000
        private val dummy = Player.create()

        fun caculateCenter(startTile : Tile, endTile : Tile) : Pair<Int, Int> {
            data class Point(val x : Int, val y : Int)

            data class Tile(val coordinates : Point, val areaValue : Float)

            fun calculateAreaValue(x : Int, y : Int) : Double {
                return (x + y) / 2.0
            }

            fun findMedianCoordinates(startPoint : mindustry.world.Tile, endPoint : mindustry.world.Tile) : Pair<Int, Int> {
                val regionWidth = if (endPoint.x > startPoint.x) endPoint.x - startPoint.x else startPoint.x - endPoint.x
                val regionHeight = if (endPoint.y > startPoint.y) endPoint.y - startPoint.y else startPoint.y - endPoint.y
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
            while (!currentThread().isInterrupted) {
                try {
                    PluginData.load()

                    var total = 0
                    val serverInfo = getServerInfo()
                    for (a in serverInfo) {
                        total += a.players
                    }

                    if (state.isPlaying) {
                        for (i in 0 until PluginData.warpCounts.size) {
                            if (state.map.name() == PluginData.warpCounts[i].mapName) {
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
                                                    Call.deconstructFinish(world.tile(tile.x + 4 + px, tile.y + py), Blocks.air, dummy.unit())
                                                }
                                            }
                                        }
                                    }
                                    dummy.x = tile.getX()
                                    dummy.y = tile.getY()

                                    Core.app.post { Commands.Client(arrayOf(str), dummy).chars(tile) }
                                    PluginData.warpCounts[i] = PluginData.WarpCount(state.map.name(), value.tile.pos(), value.ip, value.port, info.players, digits.size)
                                } else {
                                    dummy.x = value.tile.getX()
                                    dummy.y = value.tile.getY()
                                    Core.app.post {
                                        Commands.Client(arrayOf("no"), dummy).chars(value.tile)
                                    }
                                }
                            }
                        }

                        val memory = mutableListOf<Pair<Playerc, Triple<String, Float, Float>>>()
                        for (value in PluginData.warpBlocks) {
                            if (state.map.name() == value.mapName) {
                                val tile = world.tile(value.x, value.y)
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
                                        for (a in Groups.player) {
                                            memory.add(a to Triple("[yellow]$alivePlayer[] ${Bundle(a.locale)["event.server.warp.players"]}", x, y))
                                        }
                                        value.online = true
                                    } else {
                                        for (a in Groups.player) {
                                            memory.add(a to Triple(Bundle(a.locale)["event.server.warp.offline"], x, y))
                                        }
                                        value.online = false
                                    }

                                    if (isDup) margin -= 4
                                    for (a in Groups.player) {
                                        memory.add(a to Triple(value.description, x, tile.build.getY() - margin))
                                    }
                                }
                            }
                        }

                        for (value in PluginData.warpZones) {
                            if (state.map.name() == value.mapName) {
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
                                        memory.add(a to Triple("[yellow]$alivePlayer[] ${Bundle(a.locale)["event.server.warp.players"]}", (center.first * 8).toFloat(), (center.second * 8).toFloat()))
                                    }
                                } else {
                                    for (a in Groups.player) {
                                        memory.add(a to Triple(Bundle(a.locale)["event.server.warp.offline"], (center.first * 8).toFloat(), (center.second * 8).toFloat()))
                                    }
                                }
                            }
                        }
                        for (m in memory) {
                            Core.app.post {
                                Call.label(m.first.con(), m.second.first, ping.toFloat() + 3f, m.second.second, m.second.third)
                            }
                        }

                        for (i in 0 until PluginData.warpTotals.size) {
                            val value = PluginData.warpTotals[i]
                            if (state.map.name() == value.mapName) {
                                if (value.totalplayers != total) {
                                    when (total) {
                                        0, 1, 2, 3, 4, 5, 6, 7, 8, 9 -> {
                                            for (px in 0..2) {
                                                for (py in 0..4) {
                                                    Core.app.post {
                                                        Call.setTile(world.tile(value.tile.x + px, value.tile.y + py), Blocks.air, Team.sharded, 0)
                                                    }
                                                }
                                            }
                                        }

                                        else -> {
                                            for (px in 0..5) {
                                                for (py in 0..4) {
                                                    Core.app.post {
                                                        Call.setTile(world.tile(value.tile.x + 4 + px, value.tile.y + py), Blocks.air, Team.sharded, 0)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                dummy.x = value.tile.getX()
                                dummy.y = value.tile.getY()
                                Core.app.post {
                                    Commands.Client(arrayOf(getServerInfo().toString()), dummy).chars(value.tile)
                                }
                            }
                        }
                    }

                    if (Config.countAllServers) {
                        Core.settings.put("totalPlayers", total + Groups.player.size())
                        Core.settings.saveValues()
                    }

                    ping = 0.000
                    TimeUnit.SECONDS.sleep(3)
                } catch (e : InterruptedException) {
                    currentThread().interrupt()
                } catch (e : Exception) {
                    e.printStackTrace()
                    Core.app.exit()
                }
            }
        }

        @Throws(IOException::class, SocketException::class)
        private fun pingHostImpl(address : String, port : Int, listener : Consumer<Host>) {
            val packetSupplier : Prov<DatagramPacket> = Prov<DatagramPacket> { DatagramPacket(ByteArray(512), 512) }

            try {
                DatagramSocket().use { socket ->
                    val s : Long = Time.millis()
                    socket.send(DatagramPacket(byteArrayOf(-2, 1), 2, InetAddress.getByName(address), port))
                    socket.soTimeout = 1000
                    val packet : DatagramPacket = packetSupplier.get()
                    socket.receive(packet)
                    val buffer = ByteBuffer.wrap(packet.data)
                    val host = NetworkIO.readServerData(Time.timeSinceMillis(s).toInt(), packet.address.hostAddress, buffer)
                    host.port = port
                    listener.accept(host)
                }
            } catch (e : Exception) {
                listener.accept(Host(0, null, null, null, 0, 0, 0, null, null, 0, null, null))
            }
        }

        private fun getServerInfo() : Seq<Host> {
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

    object UpdateThread: Runnable {
        val queue = Seq<DB.PlayerData>()

        override fun run() {
            while (!currentThread().isInterrupted) {
                for (a in queue) {
                    database.update(a.uuid, a)
                    queue.remove(a)
                }
                sleep(200)
            }
        }
    }

    object Server: Runnable {
        lateinit var server : ServerSocket
        var lastSentMessage = ""

        override fun run() {
            try {
                server = ServerSocket(6000)
                server.use { s ->
                    while (!currentThread().isInterrupted) {
                        val socket = s.accept()
                        Log.info(Bundle()["network.server.connected", socket.inetAddress.hostAddress])
                        clients.add(socket)
                        val handler = Handler(socket)
                        handler.start()
                    }
                }
            } catch (_ : SocketException) {
            } catch (e : Exception) {
                e.printStackTrace()
            }
        }

        fun shutdown() {
            currentThread().interrupt()
            server.close()
        }

        fun sendAll(type : String, msg : String) {
            for (a in clients) {
                val b = BufferedWriter(OutputStreamWriter(a.getOutputStream()))
                try {
                    b.write(type)
                    b.newLine()
                    b.flush()
                    b.write(msg)
                    b.newLine()
                    b.flush()
                } catch (e : SocketException) {
                    a.close()
                    clients.remove(a)
                }
            }
        }

        class Handler(private val socket : Socket): java.lang.Thread() {
            override fun run() {
                try {
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                    val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))

                    while (!currentThread().isInterrupted) {
                        val d = reader.readLine()
                        if (d == null) interrupt()
                        when (d) {
                            "exit" -> interrupt()

                            "message" -> {
                                val msg = reader.readLine()
                                sendAll("message", msg)
                            }

                            "crash" -> {
                                val stacktrace = StringBuilder()
                                while (reader.readLine() !== "null") {
                                    stacktrace.append(reader.readLine() + "\n")
                                }
                                root.child("report/${LocalDateTime.now().withNano(0)}.txt").writeString(stacktrace.toString())
                                Log.info("Crash log received from ${socket.inetAddress.hostAddress}")
                            }
                        }
                    }
                } catch (_ : SocketException) {
                } catch (e : Exception) {
                    e.printStackTrace()
                }
                clients.remove(socket)
                Log.info(Bundle()["network.server.disconnected", socket.inetAddress.hostAddress])
            }
        }
    }

    object Client: Runnable {
        private val address = Config.shareBanListServer
        private val port = 6000
        private val socket = Socket()
        private lateinit var reader : BufferedReader
        private lateinit var writer : BufferedWriter
        var lastReceivedMessage = ""

        override fun run() {
            try {
                socket.connect(InetSocketAddress(address, port), 5000)
                Log.info(Bundle()["network.client.connected", "$address:$port"])

                reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))

                while (!currentThread().isInterrupted) {
                    try {
                        when (val d = reader.readLine()) {
                            "message" -> {
                                val msg = reader.readLine()
                                lastReceivedMessage = msg
                                Call.sendMessage(msg)
                            }

                            "exit" -> {
                                writer.close()
                                reader.close()
                                socket.close()
                                currentThread().interrupt()
                            }

                            else -> {
                                try {
                                    for (a in JsonArray.readJSON(d).asArray()) {
                                        netServer.admins.getInfo(a.asString()).banned = true
                                        netServer.admins.save()
                                    }
                                    val json = JsonArray()
                                    for (a in netServer.admins.banned) json.add(a.id)
                                    write(json.toString())
                                } catch (_ : Exception) {

                                }
                            }
                        }
                    } catch (e : Exception) {
                        currentThread().interrupt()
                    }
                }
            } catch (_ : SocketTimeoutException) {
                Log.info(Bundle()["network.client.timeout"])
            } catch (e : java.lang.Exception) {
                e.printStackTrace()
            }
        }

        private fun write(msg : String) {
            writer.write(msg)
            writer.newLine()
            writer.flush()
        }

        fun message(message : String) {
            write("message")
            write(message)
        }

        fun send(command : String, vararg parameter : String) {
            when (command) {
                "crash" -> {
                    try {
                        Socket("mindustry.kr", 6000).use {
                            it.soTimeout = 5000
                            BufferedWriter(OutputStreamWriter(socket.getOutputStream())).use { out ->
                                out.write("crash\n")
                                Scanner(socket.getInputStream()).use { sc ->
                                    sc.nextLine() // ok
                                    out.write("${parameter[0]}\n")
                                    out.write("null")
                                    sc.nextLine()
                                    Log.info("Crash log reported!")
                                }
                            }
                        }
                    } catch (e : SocketTimeoutException) {
                        Log.info("Connection timed out. crash report server may be closed.")
                    }
                }

                "exit" -> {
                    if (::reader.isInitialized) {
                        write("exit")
                        writer.close()
                        reader.close()
                        socket.close()
                    }
                }
            }
        }
    }
}