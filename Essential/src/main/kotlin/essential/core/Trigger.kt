package essential.core

import arc.Core
import arc.Events
import arc.func.Prov
import arc.struct.Seq
import arc.util.Align
import arc.util.Log
import arc.util.Time
import arc.util.Timer
import essential.core.Event.apmRanking
import essential.core.Event.coreListeners
import essential.core.Event.dpsBlocks
import essential.core.Event.dpsTile
import essential.core.Event.findPlayerData
import essential.core.Event.isUnitInside
import essential.core.Event.maxdps
import essential.core.Event.pvpPlayer
import essential.core.Event.pvpSpecters
import essential.core.Event.unitLimitMessageCooldown
import essential.core.Main.Companion.conf
import essential.core.Main.Companion.database
import essential.core.Main.Companion.root
import essential.core.PluginData.entityOrder
import essential.core.PluginData.voteCooltime
import essential.core.PluginData.voterCooltime
import essential.core.service.effect.EffectSystem
import mindustry.Vars
import mindustry.content.Blocks
import mindustry.game.EventType
import mindustry.game.EventType.ServerLoadEvent
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.gen.Playerc
import mindustry.io.SaveIO
import mindustry.net.Host
import mindustry.net.NetworkIO
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
                    pvpPlayer.containsKey(data.uuid) -> {
                        player.team(pvpPlayer[data.uuid])
                    }

                    conf.feature.pvp.spector && pvpSpecters.contains(data.uuid) || Permission.check(data, "pvp.spector") -> {
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
                    for (data in database.players) {
                        Call.label(data.player.con(), data.bundle().get("command.dps", maxdps!!, dpsBlocks), 1f, dpsTile!!.worldx(), dpsTile!!.worldy())
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

        Events.on(EventType.Trigger.update::class.java) {
            for (data in database.players) {
                if (Vars.state.rules.pvp && data.player.unit() != null && data.player.team().cores().isEmpty && data.player.team() != Team.derelict && pvpPlayer.containsKey(data.uuid)) {
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

                for (two in PluginData.warpZones) {
                    if (two.mapName == Vars.state.map.name() && !two.click && isUnitInside(data.player.unit().tileOn(), two.startTile, two.finishTile)) {
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