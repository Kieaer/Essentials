package essentials

import arc.Core
import arc.files.Fi
import arc.func.Prov
import arc.graphics.Color
import arc.struct.ArrayMap
import arc.struct.Seq
import arc.util.Log
import arc.util.Threads
import arc.util.Time
import com.ip2location.IP2Location
import com.neovisionaries.i18n.CountryCode
import essentials.Event.findPlayerData
import essentials.Main.Companion.database
import essentials.Main.Companion.root
import mindustry.Vars.*
import mindustry.content.Blocks
import mindustry.content.Fx
import mindustry.content.UnitTypes
import mindustry.content.Weathers
import mindustry.core.GameState
import mindustry.entities.Damage
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.gen.Playerc
import mindustry.io.SaveIO
import mindustry.maps.Map
import mindustry.net.Host
import mindustry.net.NetworkIO.readServerData
import mindustry.net.Packets
import org.mindrot.jbcrypt.BCrypt
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.Thread.sleep
import java.net.*
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import kotlin.concurrent.thread


object Trigger {
    val ip2location = IP2Location()
    var voting = false
    var voteType: String? = null
    var voteTarget: Playerc? = null
    var voteTargetUUID: String? = null
    var voteReason: String? = null
    var voteMap: Map? = null
    var voteWave: Int? = null
    var voteStarter: Playerc? = null
    val voted = Seq<String>()
    var lastVoted = LocalTime.now()
    var pvpCount = Config.pvpPeaceTime

    init {
        Main::class.java.classLoader.getResourceAsStream("IP2LOCATION-LITE-DB1.BIN").run {
            ip2location.Open(this.readBytes())
        }
    }

    fun loadPlayer(player: Playerc, data: DB.PlayerData) {
        player.name(data.name)
        data.lastdate = System.currentTimeMillis()
        data.joincount = data.joincount++

        val perm = Permission[player]
        if (perm.name.isNotEmpty()) player.name(Permission[player].name)
        player.admin(Permission[player].admin)
        player.sendMessage(Bundle(data.languageTag)["event.player.loaded"])

        database.players.add(data)
    }

    fun createPlayer(player: Playerc, id: String?, password: String?) {
        val data = DB.PlayerData()

        data.name = player.name()
        data.uuid = player.uuid()
        data.joinDate = System.currentTimeMillis()
        data.id = id ?: player.name()
        data.pw = if (password == null) player.name() else BCrypt.hashpw(password, BCrypt.gensalt())
        data.permission = "user"

        database.createData(data)
        Permission.apply()

        player.sendMessage("Player data registered!")
        loadPlayer(player, data)

        Thread {
            val ip = player.ip()
            val isLocal = try {
                val address = InetAddress.getByName(ip)
                if (address.isAnyLocalAddress || address.isLoopbackAddress) {
                    true
                } else {
                    NetworkInterface.getByInetAddress(address) != null
                }
            } catch (e: SocketException) {
                false
            } catch (e: UnknownHostException) {
                false
            }

            val res = if (isLocal) {
                val add = BufferedReader(InputStreamReader(URL("http://checkip.amazonaws.com").openStream())).readLine()
                ip2location.IPQuery(add).countryShort
            } else {
                ip2location.IPQuery(player.ip()).countryShort
            }

            val locale = if (CountryCode.getByCode(res) == null) {
                Locale.ENGLISH
            } else {
                CountryCode.getByCode(res).toLocale()
            }

            data.languageTag = locale.toLanguageTag()
        }.start()
    }

    // 1초마다 작동함
    class Seconds : TimerTask() {
        private var colorOffset = 0
        var count = 60

        fun send(message: String, vararg parameter: Any) {
            Groups.player.forEach {
                val data = findPlayerData(it.uuid())
                if (data != null) {
                    val bundle = Bundle(data.languageTag)
                    it.sendMessage(bundle.get(message, *parameter))
                }
            }
        }

        fun check(): Int {
            return when (database.players.size) {
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

        fun back(map: Map?) {
            val savePath: Fi = saveDirectory.child("rollback.msav")

            val players = Seq<Player>()
            for (p in Groups.player) {
                players.add(p)
                p.dead()
            }
            if (map == null) {
                state.serverPaused = true
            }

            Core.app.post {
                Call.worldDataBegin()
                logic.reset()
                val mode = state.rules.mode()

                try {
                    if (map != null) {
                        world.loadMap(map, map.applyRules(mode))
                        state.rules = map.applyRules(mode)
                        state.serverPaused = false
                        state.set(GameState.State.playing)
                        logic.play()
                    } else {
                        SaveIO.load(savePath)
                    }

                    for (p in players) {
                        if (p.con() == null) continue
                        p.reset()
                        if (state.rules.pvp) {
                            p.team(netServer.assignTeam(p, Seq.SeqIterable(players)))
                        }
                        netServer.sendWorldData(p)
                        val data = findPlayerData(p.uuid())
                        if (data != null) {
                            p.sendMessage(Bundle(data.languageTag)["command.vote.back.wait"])
                        } else {
                            p.sendMessage(Bundle()["command.vote.back.wait"])
                        }
                    }
                } catch (t: Exception) {
                    t.printStackTrace()
                }
                send("command.vote.back.done")
            }
        }

        override fun run() {
            database.players.forEach {
                it.playtime = it.playtime + 1
            }
            PluginData.uptime++
            PluginData.playtime++

            for (player in Groups.player) {
                if (!player.isNull) {
                    val p = database.players.find { a -> a.uuid == player.uuid() }

                    if (p != null) { // 무지개 닉네임
                        if (p.colornick) {
                            val name = p.name.replace("\\[(.*?)]".toRegex(), "")
                            nickcolor(name, player)
                        } else {
                            player.name(p.name)
                        }

                        // 잠수 플레이어 카운트
                        if (p.x == player.tileX() && p.y == player.tileY()) {
                            p.afkTime++
                            if (p.afkTime == Config.afkTime) {
                                player.kick("AFK")
                            }
                        } else {
                            p.afkTime = 0
                        }
                    }
                }
            }

            if (voting) {
                if (count % 10 == 0) {
                    send("command.vote.count", count.toString(), check().toString())
                    if (voteType == "kick" && voteTarget == null) {
                        send("command.vote.kick.target.leave")
                    }
                }
                count--
                if (count == 0 || check() <= voted.size) {
                    send("command.vote.success")

                    when (voteType) {
                        "kick" -> {
                            val name = netServer.admins.getInfo(voteTargetUUID).lastName
                            if (voteTarget == null) {
                                netServer.admins.banPlayer(voteTargetUUID)
                                send("command.vote.kick.target.banned", name)
                            } else {
                                voteTarget?.kick(Packets.KickReason.kick, 60 * 60 * 1000)
                                send("command.vote.kick.target.kicked", name)
                            }
                        }

                        "map" -> {
                            back(voteMap)
                        }

                        "gg" -> {
                            Thread {
                                for (a in 0..world.tiles.height) {
                                    for (b in 0..world.tiles.width) {
                                        Call.effect(Fx.pointHit, (a * 8).toFloat(), (b * 8).toFloat(), 0f, Color.red)
                                        if (world.tile(a, b) != null) {
                                            try {
                                                Call.setFloor(world.tile(a, b), Blocks.space, Blocks.space)
                                            } catch (e: Exception) {
                                                Call.setFloor(world.tile(a, b), Blocks.space, Blocks.space)
                                            }
                                            try {
                                                Call.removeTile(world.tile(a, b))
                                            } catch (e: Exception) {
                                                Call.removeTile(world.tile(a, b))
                                            }
                                        }
                                    }
                                    Threads.sleep(8)
                                }
                            }.start()
                        }

                        "skip" -> {
                            for (a in 0..voteWave!!) logic.runWave()
                            send("command.vote.skip.done", voteWave!!.toString())
                        }

                        "back" -> {
                            back(null)
                        }

                        "random" -> {
                            if (lastVoted.plusMinutes(10).isBefore(LocalTime.now())) {
                                send("command.vote.random.cool")
                            } else {
                                lastVoted = LocalTime.now()
                                send("command.vote.random.done")
                                Thread {
                                    val map: Map
                                    val random = Random()
                                    send("command.vote.random.is")
                                    sleep(3000)
                                    when (random.nextInt(7)) {
                                        0 -> {
                                            send("command.vote.random.unit")
                                            Groups.unit.each {
                                                if (voteStarter != null) {
                                                    if (it.team == voteStarter!!.team()) it.kill()
                                                } else {
                                                    it.kill()
                                                }
                                            }
                                            send("command.vote.random.unit.wave")
                                            logic.runWave()
                                        }

                                        1 -> {
                                            send("command.vote.random.wave")
                                            for (a in 0..5) logic.runWave()
                                        }

                                        2 -> {
                                            send("command.vote.random.health")
                                            Groups.build.each {
                                                if (voteStarter != null){
                                                    if (it.team == voteStarter!!.team()) {
                                                        Core.app.post { Damage.tileDamage(voteStarter!!.team(), it.tileX(), it.tileY(), 1f, 50f) }
                                                    }
                                                } else {
                                                    Core.app.post { Damage.tileDamage(it.team(), it.tileX(), it.tileY(), 1f, 50f) }
                                                }
                                            }
                                            for (a in Groups.player) {
                                                Call.worldDataBegin(a.con)
                                                netServer.sendWorldData(a)
                                            }
                                        }

                                        3 -> {
                                            send("command.vote.random.fill.core")
                                            if (voteStarter != null){
                                                for (item in content.items()) {
                                                    state.teams.cores(voteStarter!!.team()).first().items.add(item, Random(516).nextInt(500))
                                                }
                                            } else {
                                                for (item in content.items()) {
                                                    state.teams.cores(Team.sharded).first().items.add(item, Random(516).nextInt(500))
                                                }
                                            }
                                        }

                                        4 -> {
                                            send("command.vote.random.storm")
                                            sleep(1000)
                                            Call.createWeather(Weathers.rain, 10f, 60 * 60f, 50f, 10f)
                                        }

                                        5 -> {
                                            send("command.vote.random.fire")
                                            for (x in 0 until world.width()) {
                                                for (y in 0 until world.height()) {
                                                    Core.app.post { Call.effect(Fx.fire, (x * 8).toFloat(), (y * 8).toFloat(), 0f, Color.red) }
                                                }
                                            }
                                            var tick = 600
                                            map = state.map

                                            while (tick != 0 && map == state.map) {
                                                sleep(1000)
                                                tick--
                                                Core.app.post {
                                                    Groups.unit.each {
                                                        it.health(it.health() - 10f)
                                                    }
                                                    Groups.build.each {
                                                        Damage.tileDamage(it.team(), it.tileX(), it.tileY(), 1f, 2f)
                                                    }
                                                }
                                                if (tick == 300) {
                                                    send("command.vote.random.supply")
                                                    repeat(2) {
                                                        if (voteStarter != null) {
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
                    voting = false
                } else if (count == 0 && check() > voted.size) {
                    send("command.vote.failed")
                }

                if (!voting) {
                    voteType = null
                    voteTarget = null
                    voteTargetUUID = null
                    voteReason = null
                    voteMap = null
                    voteWave = null
                    voteStarter = null
                    voted.clear()
                    count = 60
                }
            }

            if (Config.pvpPeace) {
                if (pvpCount != 0) {
                    pvpCount--
                } else {
                    state.rules.blockDamageMultiplier = Event.orignalBlockMultiplier
                    state.rules.unitDamageMultiplier = Event.orignalUnitMultiplier
                    send("trigger.pvp.end")
                }
            }
        }

        private fun nickcolor(name: String, player: Playerc) {
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
            for (s in newName) {
                stringBuilder.append(s)
            }
            player.name(stringBuilder.toString())
        }
    }

    class Minutes : TimerTask() {
        var count = Config.rollbackTime

        override fun run() {
            val data = database.getAll()

            for (a in data) {
                if (a.status.containsKey("ban")) {
                    if (LocalDateTime.now().isAfter(LocalDateTime.parse(a.status.get("ban")))) {
                        netServer.admins.unbanPlayerID(a.uuid)
                    }
                }
            }

            if (count == 0) {
                SaveIO.save(saveDirectory.child("rollback.msav"))
                count = Config.rollbackTime
            } else {
                count--
            }
        }
    }


    class Thread : Runnable {
        private var ping = 0.000
        private val servers = ArrayMap<String, Int>()
        override fun run() {
            while (!java.lang.Thread.currentThread().isInterrupted) {
                try {
                    for (i in 0 until PluginData.warpCounts.size) {
                        val value = PluginData.warpCounts[i]
                        pingHostImpl(value.ip, value.port) { r: Host ->
                            if (r.name != null) {
                                ping += ("0." + r.ping).toDouble()
                                val str = r.players.toString()
                                val digits = IntArray(str.length)
                                for (a in str.indices) digits[a] = str[a] - '0'
                                val tile = value.tile
                                if (value.players != r.players && value.numbersize != digits.size) {
                                    for (px in 0..2) {
                                        for (py in 0..4) {
                                            Call.deconstructFinish(
                                                world.tile(tile.x + 4 + px, tile.y + py), Blocks.air, null
                                            )
                                        }
                                    }
                                }
                                Commands.Client(arrayOf(str), Player.create()).chars(tile) // i 번째 server ip, 포트, x좌표, y좌표, 플레이어 인원, 플레이어 인원 길이
                                PluginData.warpCounts[i] = PluginData.WarpCount(state.map.name(), value.tile.pos(), value.ip, value.port, r.players, digits.size)
                                addPlayers(value.ip, value.port, r.players)
                            } else {
                                ping += 1.000
                                Commands.Client(arrayOf("no"), Player.create()).chars(value.tile)
                            }
                        }
                    }

                    val memory = Seq<String>()
                    for (value in PluginData.warpBlocks) {
                        val tile = world.tile(value.pos)
                        if (tile.block() === Blocks.air) {
                            PluginData.warpBlocks.remove(value)
                        } else {
                            pingHostImpl(value.ip, value.port) { r: Host ->
                                var margin = 0f
                                var isDup = false
                                var x = tile.drawx()
                                when (value.size) {
                                    1 -> margin = 8f
                                    2 -> {
                                        margin = 16f
                                        x = tile.drawx()
                                        isDup = true
                                    }

                                    3 -> margin = 16f
                                    4 -> {
                                        x = tile.drawx()
                                        margin = 24f
                                        isDup = true
                                    }
                                }
                                Call.effect(Fx.pointHit, tile.block().offset, tile.block().offset, 0f, Color.forest)
                                Call.effect(Fx.pointHit, tile.drawx(), tile.drawy(), 0f, Color.cyan)
                                val y = tile.drawy() + if (isDup) margin - 8 else margin
                                if (r.name != null) {
                                    ping += ("0." + r.ping).toDouble()
                                    memory.add("[yellow]" + r.players + "[] Players///" + x + "///" + y)
                                    value.online = true
                                } else {
                                    ping += 1.000
                                    memory.add("[scarlet]Offline///$x///$y")
                                    value.online = false
                                }
                                memory.add(value.description + "///" + x + "///" + (tile.drawy() - margin))
                                addPlayers(value.ip, value.port, r.players)
                            }
                        }
                    }

                    for (m in memory) {
                        val a = m.split("///").toTypedArray()
                        Call.label(a[0], ping.toFloat() + 3f, a[1].toFloat(), a[2].toFloat())
                    }

                    if (Core.settings.getBool("isLobby")) {
                        if (state.`is`(GameState.State.playing)) {
                            world.tiles.forEach {
                                if (it.build != null) {
                                    it.build.health(it.build.health)
                                }
                            }
                        }
                        Core.settings.put("totalPlayers", totalPlayers() + Groups.player.size())
                        Core.settings.saveValues()
                    }
                    ping = 0.000

                    TimeUnit.SECONDS.sleep(3)
                } catch (e: InterruptedException) {
                    java.lang.Thread.currentThread().interrupt()
                }
            }
        }

        @Throws(IOException::class)
        private fun pingHostImpl(address: String, port: Int, listener: Consumer<Host>) {
            val packetSupplier: Prov<DatagramPacket> = Prov<DatagramPacket> { DatagramPacket(ByteArray(512), 512) }

            DatagramSocket().use { socket ->
                val seconds: Long = Time.millis()
                socket.send(DatagramPacket(byteArrayOf(-2, 1), 2, InetAddress.getByName(address), port))
                socket.soTimeout = 2000
                val packet: DatagramPacket = packetSupplier.get()
                socket.receive(packet)
                val buffer = ByteBuffer.wrap(packet.data)
                val host = readServerData(Time.timeSinceMillis(seconds).toInt(), packet.address.hostAddress, buffer)
                host.port = port
                listener.accept(host)
            }
        }


        private fun addPlayers(ip: String?, port: Int, players: Int) {
            val mip = "$ip:$port"
            if (!servers.containsKey(mip)) {
                servers.put(mip, players)
            }
        }

        private fun totalPlayers(): Int {
            var total = 0
            for (v in servers) {
                total += v.value
            }
            return total
        }
    }

    class Server {
        val server = ServerSocket(6000)

        init {
            while (true) {
                val client = server.accept()
                thread { Handler(client).run() }
            }
        }

        class Handler(val client: Socket) {
            val reader = Scanner(client.getInputStream())
            val writer = client.getOutputStream()
            var run = false

            fun run() {
                run = true

                while (run) {
                    try {
                        when (reader.nextLine()) { // Client 에게 데이터 전달 준비
                            "send" -> {
                                write("ok")
                                val data = reader.nextLine()
                                println("[SERVER] data received: message is $data")
                            } // Client 에게서 오는 데이터 수신
                            "receive" -> { //val data = netServer.admins.banned.toString("&&")
                                write("send dummy data")
                                println("[SERVER] dummy data send.")
                            }

                            "crash" -> {
                                if (System.getenv("DEBUG_KEY") != null) {
                                    write("ok")
                                    val stacktrace = StringBuffer()
                                    while (reader.hasNextLine()) {
                                        stacktrace.append(reader.nextLine() + "\n")
                                    }
                                    root.child("report/${LocalDateTime.now().withNano(0)}.txt")
                                    write("done")
                                    Log.info("Crash log received from ${client.inetAddress.hostAddress}")
                                }
                            }

                            "exit" -> {
                                shutdown()
                            }
                        }
                    } catch (e: NoSuchElementException) {
                        shutdown()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        shutdown()
                    }
                }
            }

            private fun write(msg: String) {
                writer.write((msg + '\n').toByteArray(Charset.forName("UTF-8")))
            }

            private fun shutdown() {
                run = false
                client.close()
            }
        }
    }

    class Client {
        val address = Config.shareBanListServer
        val port = 6000

        val client = Handler(address, port)

        class Handler(address: String, port: Int) {
            val socket = Socket()
            var connected = false

            init {
                try {
                    socket.connect(InetSocketAddress(address, port), 5000)
                    connected = true
                    Log.info("You're connected to server.")
                } catch (e: SocketTimeoutException) {
                    Log.info("Connection timed out.")
                }
            }

            val reader = Scanner(socket.getInputStream())
            val writer = socket.getOutputStream()

            fun send(command: String, vararg parameter: String) {
                if (connected) {
                    when (command) {
                        "send" -> {
                            write("send")
                            reader.nextLine()
                            write("client sent data to server")
                            println("[CLIENT] send data to server")
                        }

                        "receive" -> {
                            write("receive")
                            val data = reader.nextLine()
                            println("[CLIENT] $data")
                        }

                        "crash" -> {
                            try {
                                Socket("mindustry.kr", 6000).use {
                                    it.soTimeout = 5000
                                    socket.getOutputStream().use { out ->
                                        out.write("crash\n".toByteArray(Charset.forName("UTF-8")))
                                        Scanner(socket.getInputStream()).use { sc ->
                                            sc.nextLine() // ok
                                            out.write("${parameter[0]}\n".toByteArray(Charset.forName("UTF-8")))
                                            sc.nextLine()
                                            Log.info("Crash log reported!")
                                        }
                                    }
                                }
                            } catch (e: SocketTimeoutException) {
                                Log.info("Connection timed out. crash report server may be closed.")
                            }
                        }

                        "exit" -> {
                            write("exit")
                            reader.close()
                            socket.close()
                        }
                    }
                }
            }

            private fun write(msg: String) {
                writer.write((msg + '\n').toByteArray(Charset.forName("UTF-8")))
            }
        }
    }
}