package essentials

import arc.Core
import arc.func.Prov
import arc.struct.ArrayMap
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
import mindustry.net.NetworkIO.readServerData
import org.hjson.JsonArray
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.mindrot.jbcrypt.BCrypt
import java.io.*
import java.lang.Thread.currentThread
import java.lang.Thread.sleep
import java.net.*
import java.nio.ByteBuffer
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import kotlin.concurrent.thread


object Trigger {
    var order = 0
    val servers = Seq<Server.Handler>()

    fun loadPlayer(player : Playerc, data : DB.PlayerData) {
        if(data.status.containsKey("duplicateName") && data.status.get("duplicateName") == player.name()) {
            player.kick(Bundle(player.locale())["event.player.duplicate.name"])
        } else {
            if(data.status.containsKey("duplicateName") && data.status.get("duplicateName") != player.name()) {
                data.name = player.name()
                data.status.remove("duplicateName")
                database.queue(data)
            }
            if(Config.fixedName) player.name(data.name)
            data.lastdate = System.currentTimeMillis()
            data.joincount = data.joincount++
            data.player = player

            val perm = Permission[player]
            if(perm.name.isNotEmpty()) player.name(Permission[player].name)
            player.admin(Permission[player].admin)
            player.sendMessage(Bundle(data.languageTag)["event.player.loaded"])

            database.players.add(data)

            data.entityid = order
            order++

            val motd = if(root.child("motd/${data.languageTag}.txt").exists()) {
                root.child("motd/${data.languageTag}.txt").readString()
            } else {
                val file = root.child("motd/en.txt")
                if(file.exists()) file.readString() else ""
            }
            val count = motd.split("\r\n|\r|\n").toTypedArray().size
            if(count > 10) Call.infoMessage(player.con(), motd) else player.sendMessage(motd)

            if(perm.isAlert) {
                if(perm.alertMessage.isEmpty()) {
                    for(a in database.players) {
                        a.player.sendMessage(Bundle(a.languageTag)["event.player.joined", player.plainName()])
                    }
                } else {
                    Call.sendMessage(perm.alertMessage)
                }
            }

            if(state.rules.pvp) {
                if(Permission.check(player, "pvp.spector")) {
                    player.team(Team.derelict)
                } else if(Event.pvpSpectors.contains(player.uuid())) {
                    player.team(Team.derelict)
                }
            }

            if(Event.voting) {
                val bundle = Bundle(data.languageTag)
                player.sendMessage(bundle["command.vote.starter", player.plainName()])
                player.sendMessage(when(Event.voteType) {
                    "kick"   -> bundle["command.vote.kick.start", Event.voteTarget!!.plainName(), Event.voteReason!!]
                    "map"    -> bundle["command.vote.map.start", Event.voteMap!!.name(), Event.voteReason!!]
                    "gg"     -> bundle["command.vote.gg.start"]
                    "skip"   -> bundle["command.vote.skip.start", Event.voteWave!!]
                    "back"   -> bundle["command.vote.back.start", Event.voteReason!!]
                    "random" -> bundle["command.vote.random.start"]
                    else     -> ""
                })
                player.sendMessage(bundle["command.vote.how"])
            }
        }
    }

    fun createPlayer(player : Playerc, id : String?, password : String?) {
        val data = DB.PlayerData()
        data.name = player.name()
        data.uuid = player.uuid()
        data.joinDate = System.currentTimeMillis()
        data.id = id ?: player.plainName()
        data.pw = if(password == null) player.plainName() else BCrypt.hashpw(password, BCrypt.gensalt())
        data.permission = "user"
        data.languageTag = player.locale()

        database.createData(data)
        Permission.apply()

        player.sendMessage(Bundle(player.locale())["event.player.data.registered"])
        loadPlayer(player, data)
    }

    class Thread: Runnable {
        private var ping = 0.000
        private var servers = ArrayMap<String, Int>()
        private val dummy = Player.create()

        override fun run() {
            while(!currentThread().isInterrupted) {
                try {
                    try {
                        transaction {
                            if(PluginData.changed) {
                                DB.Data.update {
                                    it[this.data] = PluginData.lastMemory
                                    PluginData.changed = false
                                }
                            } else {
                                PluginData.load()
                            }
                        }
                    } catch(e : Exception) {
                        e.printStackTrace()
                    }

                    if(state.isPlaying) {
                        servers = ArrayMap<String, Int>()
                        for(i in 0 until PluginData.warpCounts.size) {
                            if(state.map.name() == PluginData.warpCounts[i].mapName) {
                                val value = PluginData.warpCounts[i]
                                pingHostImpl(value.ip, value.port) { r : Host ->
                                    if(r.name != null) {
                                        ping += ("0." + r.ping).toDouble()
                                        val str = r.players.toString()
                                        val digits = IntArray(str.length)
                                        for(a in str.indices) digits[a] = str[a] - '0'
                                        val tile = value.tile
                                        if(value.players != r.players) {
                                            for(px in 0..2) {
                                                for(py in 0..4) {
                                                    Call.deconstructFinish(world.tile(tile.x + 4 + px, tile.y + py), Blocks.air, dummy.unit())
                                                }
                                            }
                                        }
                                        dummy.x = tile.getX()
                                        dummy.y = tile.getY()

                                        Commands.Client(arrayOf(str), dummy).chars(tile)
                                        PluginData.warpCounts[i] = PluginData.WarpCount(state.map.name(), value.tile.pos(), value.ip, value.port, r.players, digits.size)
                                        addPlayers(value.ip, value.port, r.players)
                                    } else {
                                        ping += 1.000

                                        dummy.x = value.tile.getX()
                                        dummy.y = value.tile.getY()
                                        Commands.Client(arrayOf("no"), dummy).chars(value.tile)
                                    }
                                }
                            }
                        }

                        val memory = mutableListOf<Pair<Playerc, String>>()
                        for(value in PluginData.warpBlocks) {
                            if(state.map.name() == value.mapName) {
                                val tile = world.tile(value.x, value.y)
                                if(tile.block() == Blocks.air) {
                                    PluginData.warpBlocks.remove(value)
                                } else {
                                    var margin = 0f
                                    var isDup = false
                                    val x = tile.build.getX()

                                    when(value.size) {
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

                                    var y = tile.build.getY() + if(isDup) margin - 8 else margin
                                    var players = 0

                                    try {
                                        pingHostImpl(value.ip, value.port) { r : Host ->
                                            ping += ("0." + r.ping).toDouble()
                                            if(isDup) y += 4
                                            for(a in Groups.player) {
                                                memory.add(a to "[yellow]${r.players}[] ${Bundle(a.locale)["event.server.warp.players"]}///$x///$y")
                                            }
                                            value.online = true
                                            players = r.players
                                        }
                                    } catch(e : IOException) {
                                        ping += 1.000
                                        for(a in Groups.player) {
                                            memory.add(a to "${Bundle(a.locale)["event.server.warp.offline"]}///$x///$y")
                                        }
                                        value.online = false
                                    }

                                    if(isDup) margin -= 4
                                    for(a in Groups.player) {
                                        memory.add(a to "${value.description}///$x///${tile.build.getY() - margin}")
                                    }
                                    addPlayers(value.ip, value.port, players)
                                }
                            }
                        }
                        for(m in memory) {
                            val a = m.second.split("///").toTypedArray()
                            Core.app.post { Call.label(m.first.con(), a[0], ping.toFloat() + 3f, a[1].toFloat(), a[2].toFloat()) }
                        }

                        for(i in 0 until PluginData.warpTotals.size) {
                            val value = PluginData.warpTotals[i]
                            if(state.map.name() == value.mapName) {
                                if(value.totalplayers != totalPlayers()) {
                                    when(totalPlayers()) {
                                        0, 1, 2, 3, 4, 5, 6, 7, 8, 9 -> {
                                            for(px in 0..2) {
                                                for(py in 0..4) {
                                                    Core.app.post {
                                                        Call.setTile(world.tile(value.tile.x + px, value.tile.y + py), Blocks.air, Team.sharded, 0)
                                                    }
                                                }
                                            }
                                        }

                                        else                         -> {
                                            for(px in 0..5) {
                                                for(py in 0..4) {
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
                                Commands.Client(arrayOf(totalPlayers().toString()), dummy).chars(value.tile)
                            }
                        }

                        if(Config.countAllServers) {
                            Core.settings.put("totalPlayers", totalPlayers() + Groups.player.size())
                            Core.settings.saveValues()
                        }
                        ping = 0.000
                    }
                    TimeUnit.SECONDS.sleep(3)
                } catch(e : Exception) {
                    currentThread().interrupt()
                }
            }
        }

        @Throws(IOException::class, SocketException::class)
        private fun pingHostImpl(address : String, port : Int, listener : Consumer<Host>) {
            val packetSupplier : Prov<DatagramPacket> = Prov<DatagramPacket> { DatagramPacket(ByteArray(512), 512) }

            DatagramSocket().use { socket ->
                val seconds : Long = Time.millis()
                socket.send(DatagramPacket(byteArrayOf(-2, 1), 2, InetAddress.getByName(address), port))
                socket.soTimeout = 2000
                val packet : DatagramPacket = packetSupplier.get()
                socket.receive(packet)
                val buffer = ByteBuffer.wrap(packet.data)
                val host = readServerData(Time.timeSinceMillis(seconds).toInt(), packet.address.hostAddress, buffer)
                host.port = port
                listener.accept(host)
            }
        }


        private fun addPlayers(ip : String?, port : Int, players : Int) {
            val mip = "$ip:$port"
            if(!servers.containsKey(mip)) {
                servers.put(mip, players)
            }
        }

        private fun totalPlayers() : Int {
            var total = 0
            for(v in servers) {
                total += v.value
            }
            return total
        }
    }

    object UpdateThread: Runnable {
        val queue = Seq<DB.PlayerData>()

        override fun run() {
            while(!currentThread().isInterrupted) {
                for(a in queue) {
                    database.update(a.uuid, a)
                    queue.removeAll { b -> b.uuid == a.uuid }
                }
                sleep(100)
            }
        }
    }

    object Server: Runnable {
        lateinit var server : ServerSocket

        override fun run() {
            server = ServerSocket(6000)
            while(!currentThread().isInterrupted) {
                val client = server.accept()
                Log.info(Bundle()["network.server.connected", client.inetAddress.hostAddress])
                val handler = Handler(client)
                servers.add(handler)
                thread { Handler(client).run() }
            }
        }

        fun shutdown() {
            currentThread().interrupt()
            server.close()
        }

        class Handler(val client : Socket) {
            val reader = BufferedReader(InputStreamReader(client.getInputStream()))
            val writer = BufferedWriter(OutputStreamWriter(client.getOutputStream()))
            var run = false

            fun message(message : String) {
                write("message")
                write(message)
            }

            fun requestList() {
                write("sendServer")
            }

            fun run() {
                run = true

                while(run) {
                    try {
                        when(reader.readLine()) {
                            "send"           -> {
                                write("send")
                                val data = reader.readLine()
                                val json = JsonArray.readJSON(data).asArray()
                                for(a in json) {
                                    netServer.admins.banPlayerID(a.asString())
                                }
                                write("done")
                            }

                            "receive"        -> {
                                val json = JsonArray()
                                for(a in netServer.admins.banned) json.add(a.id)
                                write(json.toString())
                            }

                            "messageRequest" -> {
                                val message = reader.readLine()
                                Call.sendMessage(message)
                                for(a in servers) {
                                    a.message(message)
                                }
                            }

                            "crash"          -> {
                                write("ok")
                                val stacktrace = StringBuffer()
                                while(reader.readLine() != "null") {
                                    stacktrace.append(reader.readLine() + "\n")
                                }
                                root.child("report/${LocalDateTime.now().withNano(0)}.txt")
                                write("done")
                                Log.info("Crash log received from ${client.inetAddress.hostAddress}")
                            }

                            "exit"           -> {
                                shutdown()
                                Log.info(Bundle()["network.server.disconnected", client.inetAddress.hostAddress])
                            }
                        }
                    } catch(e : Exception) {
                        run = false
                        shutdown()
                    }
                }
            }

            private fun write(msg : String) {
                try {
                    writer.write(msg)
                    writer.newLine()
                    writer.flush()
                } catch(e : SocketException) {
                    run = false
                    currentThread().interrupt()
                    client.close()
                    servers.removeAll { a -> a == this }
                }
            }

            fun shutdown() {
                try {
                    write("exit")
                    run = false
                    writer.close()
                    reader.close()
                    client.close()
                } catch(e : Exception) {
                    e.printStackTrace()
                }
                servers.removeAll { a -> a == this }
            }
        }
    }

    object Client: Runnable {
        val address = Config.shareBanListServer
        val port = 6000
        val socket = Socket()
        lateinit var reader : BufferedReader
        lateinit var writer : BufferedWriter

        override fun run() {
            try {
                socket.connect(InetSocketAddress(address, port), 5000)
                Log.info(Bundle()["network.client.connected", "$address:$port"])
            } catch(e : SocketTimeoutException) {
                Log.info(Bundle()["network.client.timeout"])
                return
            }

            reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))

            while(!currentThread().isInterrupted) {
                try {
                    when(reader.readLine()) {
                        "send"       -> {
                            val json = JsonArray()
                            for(a in netServer.admins.banned) json.add(a.id)
                            write(json.toString())
                            reader.readLine()
                            write("receive")
                            for(a in JsonArray.readJSON(reader.readLine()).asArray()) {
                                netServer.admins.banPlayerID(a.asString())
                            }
                        }

                        "sendServer" -> {
                            write("send")
                        }

                        "message"    -> {
                            Call.sendMessage(reader.readLine())
                        }

                        "exit"       -> {
                            currentThread().interrupt()
                        }
                    }
                } catch(e : Exception) {
                    currentThread().interrupt()
                }
            }
        }

        fun write(msg : String) {
            writer.write(msg)
            writer.newLine()
            writer.flush()
        }

        fun message(message : String) {
            write("messageRequest")
            write(message)
        }

        fun send(command : String, vararg parameter : String) {
            when(command) {
                "send"    -> {
                    write("send")
                }

                "receive" -> {
                    write("receive")
                    val data = reader.readLine()
                    val json = JsonArray.readJSON(data).asArray()
                    for(a in json) {
                        netServer.admins.banPlayerID(a.asString())
                    }
                }

                "crash"   -> {
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
                    } catch(e : SocketTimeoutException) {
                        Log.info("Connection timed out. crash report server may be closed.")
                    }
                }

                "exit"    -> {
                    if(::reader.isInitialized) {
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