package remake

import arc.Core
import arc.func.Prov
import arc.struct.ArrayMap
import arc.struct.Seq
import arc.util.Log
import arc.util.Time
import com.ip2location.IP2Location
import com.neovisionaries.i18n.CountryCode
import mindustry.Vars.state
import mindustry.Vars.world
import mindustry.content.Blocks
import mindustry.core.GameState
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.gen.Playerc
import mindustry.net.Host
import mindustry.net.NetworkIO.readServerData
import remake.Main.Companion.database
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.*
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import kotlin.concurrent.thread


object Trigger {
    val ip2location = IP2Location()

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

        database.players.add(data)

        Runnable{
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
            database.update(player.uuid(), data)
        }.run()
    }

    fun createPlayer(player: Playerc, id: String?, password: String?) {
        val data = DB.PlayerData

        data.name = player.name()
        data.uuid = player.uuid()
        data.joinDate = System.currentTimeMillis()
        data.id = id ?: player.name()
        data.pw = password ?: player.name()
        data.permission = "user"

        database.createData(data)

        player.sendMessage("Player data registered!")
        loadPlayer(player, data)
    }

    // 1초마다 작동함
    class Seconds : TimerTask() {
        private var colorOffset = 0

        override fun run() {
            database.players.forEach {
                it.playtime = it.playtime + 1
            }

            for (player in Groups.player) {
                if (!player.isNull) {
                    val p = database.players.find { a -> a.uuid == player.uuid() }

                    if (p != null) {
                        // 무지개 닉네임
                        if (p.colornick) {
                            val name = p.name.replace("\\[(.*?)]".toRegex(), "")
                            nickcolor(name, player)
                        } else {
                            player.name(p.name)
                        }

                        // 잠수 플레이어 카운트

                    }
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


    class Thread : Runnable{
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
                                        x = tile.drawx() - 4f
                                        isDup = true
                                    }

                                    3 -> margin = 16f
                                    4 -> {
                                        x = tile.drawx() - 4f
                                        margin = 24f
                                        isDup = true
                                    }
                                }
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
                } catch (e: InterruptedException){
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
            if(!servers.containsKey(mip)) {
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
        val server = ServerSocket(9999)

        init {
            while(true){
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

                while(run) {
                    try {
                        when (reader.nextLine()) {
                            // Client 에게 데이터 전달 준비
                            "send" -> {
                                write("ok")
                                val data = reader.nextLine()
                                println("[SERVER] data received: message is $data")
                            }
                            // Client 에게서 오는 데이터 수신
                            "receive" -> {
                                //val data = netServer.admins.banned.toString("&&")
                                write("send dummy data")
                                println("[SERVER] dummy data send.")
                            }
                            "exit" -> {
                                shutdown()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        shutdown()
                    }
                }
            }

            private fun write(msg: String) {
                writer.write((msg + '\n').toByteArray(Charset.defaultCharset()))
            }

            private fun shutdown() {
                run = false
                client.close()
            }
        }
    }

    class Client{
        val address = "127.0.0.1"
        val port = 9999

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

            fun send(command: String){
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

                        "exit" -> {
                            write("exit")
                            reader.close()
                            socket.close()
                        }
                    }
                }
            }

            private fun write(msg: String) {
                writer.write((msg + '\n').toByteArray(Charset.defaultCharset()))
            }
        }
    }
}