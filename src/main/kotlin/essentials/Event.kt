package essentials

import arc.Core
import arc.Events
import arc.files.Fi
import arc.graphics.Color
import arc.struct.Seq
import arc.util.Log
import arc.util.Time
import com.cybozu.labs.langdetect.DetectorFactory
import com.cybozu.labs.langdetect.LangDetectException
import essentials.Main.Companion.database
import mindustry.Vars
import mindustry.Vars.netServer
import mindustry.Vars.state
import mindustry.content.Blocks
import mindustry.content.Fx
import mindustry.content.UnitTypes
import mindustry.content.Weathers
import mindustry.entities.Damage
import mindustry.game.EventType
import mindustry.game.EventType.*
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Playerc
import mindustry.io.SaveIO
import mindustry.maps.Map
import mindustry.net.Packets
import mindustry.net.WorldReloader
import org.hjson.JsonArray
import org.hjson.JsonObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.regex.Pattern
import kotlin.experimental.and
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt


object Event {
    val file = JsonObject.readHjson(Main::class.java.classLoader.getResourceAsStream("exp.hjson")!!.reader()).asObject()
    var order = 0
    val players = JsonArray()
    var orignalBlockMultiplier = 1f
    var orignalUnitMultiplier = 1f

    var voting = false
    var voteType: String? = null
    var voteTarget: Playerc? = null
    var voteTargetUUID: String? = null
    var voteReason: String? = null
    var voteMap: Map? = null
    var voteWave: Int? = null
    var voteStarter: Playerc? = null
    var voted = Seq<String>()
    var lastVoted = LocalTime.now()

    var destroyAll = false

    var enemyCores = 0
    var enemyCoresCounted = false

    init {
        val aa = arrayOf("af","ar","bg","bn","cs","da","de","el","en","es","et","fa","fi","fr","gu","he","hi","hr","hu","id","it","ja","kn","ko","lt","lv","mk","ml","mr","ne","nl","no","pa","pl","pt","ro","ru","sk","sl","so","sq","sv","sw","ta","te","th","tl","tr","uk","ur","vi","zh-cn","zh-tw")
        val bb = arrayListOf<String>()
        for (a in aa){
            bb.add(Main::class.java.classLoader.getResource("profiles/$a")!!.readText(Charset.forName("UTF-8")))
        }
        DetectorFactory.loadProfile(bb)
    }

    fun register() {
        Events.on(PlayerChatEvent::class.java) {
            if (!it.message.startsWith("/")) {
                if (findPlayerData(it.player.uuid()) != null) {
                    log(LogType.Chat, "${it.player.name}: ${it.message}")
                    Log.info("<&y" + it.player.name + ": &lm" + it.message + "&lg>")

                    val data = database.players.find { e -> e.uuid == it.player.uuid() }

                    if (data != null && !data.mute) {
                        if (voting && it.message.equals("y", true) && !voted.contains(it.player.uuid())) {
                            voted.add(it.player.uuid())
                            it.player.sendMessage(Bundle(data.languageTag)["command.vote.voted"])
                        }

                        if (Config.chatlimit) {
                            val d = DetectorFactory.create()
                            val languages = Config.chatlanguage.split(",")
                            d.append(it.message)
                            try {
                                if (!languages.contains(d.detect()) && (voting && it.message.equals("y", true) && !voted.contains(it.player.uuid()))) {
                                    it.player.sendMessage(Bundle(data.languageTag)["chat.language.not.allow"])
                                    return@on
                                }
                            } catch (_: LangDetectException) {}
                        }

                        if (Config.chatBlacklist) {
                            val file = Main.root.child("chat_blacklist.txt").readString("UTF-8").split("\r\n")
                            if (file.isNotEmpty()) {
                                for (a in file) {
                                    if (Config.chatBlacklistRegex) {
                                        if (it.message.contains(Regex(a))) {
                                            it.player.sendMessage(Bundle(findPlayerData(it.player.uuid())!!.languageTag)["chat.blacklisted"])
                                            return@on
                                        }
                                    } else {
                                        if (it.message.contains(a)) {
                                            it.player.sendMessage(Bundle(findPlayerData(it.player.uuid())!!.languageTag)["chat.blacklisted"])
                                            return@on
                                        }
                                    }
                                }
                            }
                        }
                        Call.sendMessage(Permission[it.player].chatFormat.replace("%1", it.player.coloredName()).replace("%2", it.message))
                    }
                } else {
                    Call.sendMessage("[gray]${it.player.name} [orange] > [white]${it.message}")
                }
            }
        }

        Events.on(WithdrawEvent::class.java) {
            if (it.tile != null && it.player.unit().item() != null && it.player.name != null) {
                log(LogType.WithDraw, "${it.player.name} puts ${it.player.unit().item().name} ${it.amount} amount into ${it.tile.block().name}.")
            }
        }

        Events.on(DepositEvent::class.java) {
            if (it.tile != null && it.player.unit().item() != null && it.player.name != null) {
                log(LogType.Deposit, "${it.player.name} puts ${it.player.unit().item().name} ${it.amount} amount into ${it.tile.block().name}.")
            }
        }

        Events.on(ConfigEvent::class.java) {
            if (it.tile != null && it.tile.block() != null && it.player != null && it.value is Int && Config.antiGrief) {
                val entity = it.tile
                val other = Vars.world.tile(it.value as Int)
                val valid = other != null && entity.power != null && other.block().hasPower
                if (valid) {
                    val oldGraph = entity.power.graph
                    val newGraph = other.build.power.graph
                    val oldGraphCount = oldGraph.toString().substring(oldGraph.toString().indexOf("all=["), oldGraph.toString().indexOf("], graph")).replaceFirst("all=\\[".toRegex(), "").split(",").toTypedArray().size
                    val newGraphCount = newGraph.toString().substring(newGraph.toString().indexOf("all=["), newGraph.toString().indexOf("], graph")).replaceFirst("all=\\[".toRegex(), "").split(",").toTypedArray().size
                    if (abs(oldGraphCount - newGraphCount) > 10) {
                        Groups.player.forEach { a ->
                            val data = findPlayerData(a.uuid())
                            if (data != null) {
                                val bundle = Bundle(data.languageTag)
                                a.sendMessage(bundle["event.antigrief.node", it.player.name, oldGraphCount.coerceAtLeast(newGraphCount), oldGraphCount.coerceAtMost(newGraphCount), "${it.tile.x}, ${it.tile.y}"])
                            }
                        }
                    }
                }
            }
        }

        Events.on(TapEvent::class.java) {
            log(LogType.Tap, "${it.player.name} clicks on ${it.tile.block().name}")
            val playerData = findPlayerData(it.player.uuid())
            if (playerData != null) {
                for (data in PluginData.warpBlocks) {
                    if (it.tile.x >= Vars.world.tile(data.pos).x && it.tile.x <= Vars.world.tile(data.pos).x && it.tile.y >= Vars.world.tile(data.pos).y && it.tile.y <= Vars.world.tile(data.pos).y) {
                        if (data.online) {
                            Log.info("${it.player.name} moves to server ${data.ip}:${data.port}")
                            Call.connect(it.player.con(), data.ip, data.port)
                        }
                        break
                    }
                }

                for (data in PluginData.warpZones) {
                    if (it.tile.x > data.startTile.x && it.tile.x < data.finishTile.x && it.tile.y > data.startTile.y && it.tile.y < data.finishTile.y) {
                        Log.info("${it.player.name} moves to server ${data.ip}:${data.port}")
                        Call.connect(it.player.con(), data.ip, data.port)
                        break
                    }
                }
            }
        }

        Events.on(PickupEvent::class.java) {

        }

        Events.on(UnitControlEvent::class.java) {

        }

        Events.on(GameOverEvent::class.java) {
            if (state.rules.pvp) {
                var index = 5
                for (a in 0..4) {
                    if (state.teams[Team.all[index]].cores.isEmpty) {
                        index--
                    }
                }
                if (index == 1) {
                    for (player in Groups.player) {
                        val target = findPlayerData(player.uuid())
                        if (target != null) {
                            if (player.team().name == it.winner.name) {
                                target.pvpwincount++
                            } else if (player.team().name != it.winner.name) {
                                target.pvplosecount++
                            }
                        }
                    }
                }
            } else if (state.rules.attackMode) {
                for (p in Groups.player) {
                    val target = findPlayerData(p.uuid())
                    if (target != null) {
                        if (it.winner == p.team()) {
                            val oldLevel = target.level
                            val oldExp = target.exp
                            val exp = (PluginData.playtime.toInt() * 2) * enemyCores
                            target.exp = target.exp + exp
                            target.attackclear++

                            val bundle = Bundle(target.languageTag)
                            p.sendMessage(bundle["exp.earn", exp])
                            p.sendMessage(bundle["exp.current", target.exp, target.exp - oldExp, target.level, target.level - oldLevel])
                            database.update(p.uuid(), target)
                            p.sendMessage(bundle["data.saved"])
                        }
                    }
                }
            }
        }

        Events.on(BlockBuildBeginEvent::class.java) {

        }

        Events.on(BlockBuildEndEvent::class.java) {
            if (it.unit.isPlayer) {
                val player = findPlayerData(it.unit.player.uuid())
                if (player != null) {
                    if (!it.breaking) player.placecount++ else player.breakcount++
                }
            }

            val isDebug = Core.settings.getBool("debugMode")

            if (it.unit.isPlayer) {
                val player = it.unit.player
                val target = findPlayerData(player.uuid())

                if (!player.unit().isNull && target != null && it.tile.block() != null && player.unit().buildPlan() != null) {
                    val block = it.tile.block()
                    val exp = block.buildCostMultiplier

                    if (!it.breaking) {
                        log(LogType.Block, "${player.name} placed ${block.name}")

                        target.placecount + 1
                        target.exp = target.exp + exp.roundToInt()

                        if (isDebug) {
                            Log.info("${player.name} placed ${it.tile.block().name} to ${it.tile.x},${it.tile.y}")
                        }
                    } else if (it.breaking) {
                        log(LogType.Block, "${player.name} break ${player.unit().buildPlan().block.name}")
                        target.breakcount + 1
                        target.exp = target.exp - exp.roundToInt()

                        if (isDebug) {
                            Log.info("${player.name} break ${it.tile.block().name} to ${it.tile.x},${it.tile.y}")
                        }
                    }
                }
            }
        }

        Events.on(BuildSelectEvent::class.java) {
            if (it.builder is Playerc && it.builder.buildPlan() != null && !Pattern.matches(".*build.*", it.builder.buildPlan().block.name) && it.tile.block() !== Blocks.air && it.breaking) {
                log(LogType.Block, "${(it.builder as Playerc).name()} remove ${it.tile.block().name} to ${it.tile.x},${it.tile.y}")
            }
        }

        Events.on(BlockDestroyEvent::class.java) {
            if (Config.destroyCore && state.rules.coreCapture) {
                Fx.spawnShockwave.at(it.tile.getX(), it.tile.getY(), state.rules.dropZoneRadius)
                Damage.damage(Vars.world.tile(it.tile.pos()).team(), it.tile.getX(), it.tile.getY(), state.rules.dropZoneRadius, 1.0E8f, true)
            }
        }

        Events.on(UnitDestroyEvent::class.java) {

        }

        Events.on(UnitCreateEvent::class.java) {
            if (Groups.unit.size() > Config.spawnLimit) {
                Groups.player.forEach {
                    val data = findPlayerData(it.uuid())
                    if (data != null) {
                        val bundle = Bundle(data.languageTag)
                        it.sendMessage(bundle["config.spawnlimit.reach", "[scarlet]${Groups.unit.size()}[white]/[sky]${Config.spawnLimit}"])
                    }
                }
            }
        }

        Events.on(UnitChangeEvent::class.java) {

        }

        Events.on(PlayerJoin::class.java) {
            log(LogType.Player, "${it.player.plainName()} (${it.player.uuid()}, ${it.player.con.address}) joined.")
            it.player.admin(false)

            if (!enemyCoresCounted && state.rules.attackMode) {
                enemyCores = max(state.teams.present.sum { t -> if (t.team !== it.player.team()) t.cores.size else 0 }, 1)
                enemyCoresCounted = true
            }

            if (Config.authType == Config.AuthType.None) {
                val data = database[it.player.uuid()]
                if (data != null) {
                    Trigger.loadPlayer(it.player, data)
                } else if (Config.authType != Config.AuthType.None) {
                    it.player.sendMessage("[green]To play the server, use the [scarlet]/reg[] command to register account.")
                } else if (Config.authType == Config.AuthType.None) {
                    Trigger.createPlayer(it.player, null, null)
                }
            }
        }

        Events.on(PlayerLeave::class.java) {
            for (a in 0..players.size()) {
                if (players.get(a).asObject().get("uuid").asString().equals(it.player.uuid())) {
                    players.remove(a)
                    break
                }
            }
            log(LogType.Player, "${it.player.plainName()} (${it.player.uuid()}, ${it.player.con.address}) disconnected.")
            val data = database.players.find { data -> data.uuid == it.player.uuid() }
            if (data != null) {
                database.update(it.player.uuid(), data)
            }
            database.players.remove(data)
        }

        Events.on(PlayerBanEvent::class.java) {

        }

        Events.on(WorldLoadEvent::class.java) {
            PluginData.playtime = 0L
            enemyCoresCounted = false
            if (state.rules.pvp && Config.pvpPeace) {
                orignalBlockMultiplier = state.rules.blockDamageMultiplier
                orignalUnitMultiplier = state.rules.unitDamageMultiplier
                state.rules.blockDamageMultiplier = 0f
                state.rules.unitDamageMultiplier = 0f
            }
        }

        Events.on(PlayerConnect::class.java) { e ->
            log(LogType.Player, "${e.player.plainName()} (${e.player.uuid()}, ${e.player.con.address}) connected.")

            // 닉네임이 블랙리스트에 등록되어 있는지 확인
            for (s in PluginData.blacklist) {
                if (e.player.name.matches(Regex(s))) Call.kick(e.player.con, "This name is blacklisted.")
            }

            if (Config.fixedName) {
                if (e.player.name.length > 32) Call.kick(e.player.con(), "Nickname too long!")
                if (e.player.name.matches(Regex(".*\\[.*].*"))) Call.kick(e.player.con(), "Color tags can't be used for nicknames on this Server.")
                if (e.player.name.contains("　")) Call.kick(e.player.con(), "Don't use blank speical charactor nickname!")
                if (e.player.name.contains(" ")) Call.kick(e.player.con(), "Nicknames can't be used on this server!")
                if (Pattern.matches(".*\\[.*.].*", e.player.name)) Call.kick(e.player.con(), "Can't use only color tags nickname in this Server.")
            }

            if (Config.minimalName) {
                if (e.player.name.length < 4) Call.kick(e.player.con(), "Nickname too short!")
            }

            if (Config.antiVPN) {
                val br = BufferedReader(InputStreamReader(Main::class.java.classLoader.getResourceAsStream("IP2LOCATION-LITE-DB1.BIN")!!))
                br.use { _ ->
                    var line: String
                    while (br.readLine().also { line = it } != null) {
                        val match = IpAddressMatcher(line)
                        if (match.matches(e.player.con.address)) {
                            Call.kick(e.player.con(), Bundle()["anti-grief.vpn"])
                        }
                    }
                }
            }

            if (Config.antiGrief) {
                val find = netServer.admins.findByName(e.player.name)
                if (find != null) {
                    if (find.first().lastIP != e.player.con.address) {
                        Call.kick(e.player.con(), "There's a player with the same name on the server!")
                    }
                }
            }
        }

        Events.on(MenuOptionChooseEvent::class.java) {
            if (it.menuId == 0) {
                if (it.option == 0) {
                    val d = findPlayerData(it.player.uuid())
                    if (d != null) {
                        d.languageTag = "ko"
                        it.player.sendMessage(Bundle(d.languageTag)["command.language.preview", Locale(d.languageTag).toLanguageTag()])
                    }
                }
            }
        }

        Events.run(EventType.Trigger.impactPower) {

        }

        fun send(message: String, vararg parameter: Any) {
            Groups.player.forEach {
                val data = findPlayerData(it.uuid())
                if (data != null) {
                    val bundle = Bundle(data.languageTag)
                    Core.app.post { it.sendMessage(bundle.get(message, *parameter)) }
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
            Core.app.post {
                val savePath: Fi = Vars.saveDirectory.child("rollback.msav")

                try {
                    val mode = state.rules.mode()
                    val reloader = WorldReloader()

                    reloader.begin()

                    if (map != null) {
                        Vars.world.loadMap(map, map.applyRules(mode))
                    } else {
                        SaveIO.load(savePath)
                    }

                    state.rules = state.map.applyRules(mode)

                    Vars.logic.play()
                    reloader.end()
                } catch (t: Exception) {
                    t.printStackTrace()
                }
                send("command.vote.back.done")
            }
        }

        var colorOffset = 0
        fun nickcolor(name: String, player: Playerc) {
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

        var secondCount = 0
        var minuteCount = 0
        var count = 60

        var rollbackCount = Config.rollbackTime
        var messageCount = Config.messageTime
        var messageOrder = 0

        Events.run(EventType.Trigger.update) {
            for (a in database.players) {
                if (a.status.containsKey("freeze")) {
                    val player = findPlayers(a.uuid)
                    if (player != null) {
                        val split = a.status.get("freeze").toString().split("/")
                        player.set(split[0].toFloat(), split[1].toFloat())
                        Call.setPosition(player.con(), split[0].toFloat(), split[1].toFloat())
                        Call.setCameraPosition(player.con(), split[0].toFloat(), split[1].toFloat())
                    }
                }

                if (a.status.containsKey("tracking")) {
                    for (b in Groups.player) {
                        Call.label(a.player.con(), b.name, Time.delta/2, b.mouseX, b.mouseY)
                    }
                }
            }

            if (destroyAll) {
                Call.gameOver(Team.derelict)
                for (a in 0..Vars.world.tiles.height) {
                    for (b in 0..Vars.world.tiles.width) {
                        Call.effect(Fx.pointHit, (a * 8).toFloat(), (b * 8).toFloat(), 0f, Color.red)
                        if (Vars.world.tile(a, b) != null) {
                            try {
                                Call.setFloor(Vars.world.tile(a, b), Blocks.space, Blocks.space)
                            } catch (e: Exception) {
                                Call.setFloor(Vars.world.tile(a, b), Blocks.space, Blocks.space)
                            }
                            try {
                                Call.removeTile(Vars.world.tile(a, b))
                            } catch (e: Exception) {
                                Call.removeTile(Vars.world.tile(a, b))
                            }
                        }
                    }
                }
                destroyAll = false
            }

            if (secondCount == 60) {
                PluginData.uptime++
                PluginData.playtime++

                for (a in database.players) {
                    a.playtime = a.playtime + 1

                    if (a.colornick) {
                        val name = a.name.replace("\\[(.*?)]".toRegex(), "")
                        nickcolor(name, a.player)
                    } else {
                        a.player.name(a.name)
                    }

                    // 잠수 플레이어 카운트
                    if (a.x == a.player.tileX() && a.y == a.player.tileY()) {
                        a.afkTime++
                        if (a.afkTime == Config.afkTime) {
                            a.player.kick("AFK")
                        }
                    } else {
                        a.afkTime = 0
                    }
                }

                if (voting) {
                    if (count % 10 == 0) {
                        send("command.vote.count", count.toString(), check() - voted.size)
                        if (voteType == "kick" && voteTarget == null) {
                            send("command.vote.kick.target.leave")
                        }
                    }
                    count--
                    if ((count == 0 && check() <= voted.size) || check() <= voted.size) {
                        send("command.vote.success")

                        when (voteType) {
                            "kick" -> {
                                val name = Vars.netServer.admins.getInfo(voteTargetUUID).lastName
                                if (voteTarget == null) {
                                    Vars.netServer.admins.banPlayer(voteTargetUUID)
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
                                destroyAll = true
                            }

                            "skip" -> {
                                for (a in 0..voteWave!!) Vars.logic.runWave()
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
                                        Thread.sleep(3000)
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
                                                        if (it.team == voteStarter!!.team()) {
                                                            it.block.health = it.block.health / 2
                                                        }
                                                    } else {
                                                        it.block.health = it.block.health / 2
                                                    }
                                                }
                                                for (a in Groups.player) {
                                                    Call.worldDataBegin(a.con)
                                                    Vars.netServer.sendWorldData(a)
                                                }
                                            }

                                            3 -> {
                                                send("command.vote.random.fill.core")
                                                if (voteStarter != null) {
                                                    for (item in Vars.content.items()) {
                                                        state.teams.cores(voteStarter!!.team()).first().items.add(item, Random(516).nextInt(500))
                                                    }
                                                } else {
                                                    for (item in Vars.content.items()) {
                                                        state.teams.cores(Team.sharded).first().items.add(item, Random(516).nextInt(500))
                                                    }
                                                }
                                            }

                                            4 -> {
                                                send("command.vote.random.storm")
                                                Thread.sleep(1000)
                                                Call.createWeather(Weathers.rain, 10f, 60 * 60f, 50f, 10f)
                                            }

                                            5 -> {
                                                send("command.vote.random.fire")
                                                for (x in 0 until Vars.world.width()) {
                                                    for (y in 0 until Vars.world.height()) {
                                                        Call.effect(Fx.fire, (x * 8).toFloat(), (y * 8).toFloat(), 0f, Color.red)
                                                    }
                                                }
                                                var tick = 600
                                                map = state.map

                                                while (tick != 0 && map == state.map) {
                                                    Thread.sleep(1000)
                                                    tick--
                                                    Core.app.post {
                                                        Groups.unit.each {
                                                            it.health(it.health() - 10f)
                                                        }
                                                        Groups.build.each {
                                                            it.block.health = it.block.health / 30
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
                        voteType = null
                        voteTarget = null
                        voteTargetUUID = null
                        voteReason = null
                        voteMap = null
                        voteWave = null
                        voteStarter = null
                        voted = Seq<String>()
                        count = 60
                    } else if (count == 0 && check() > voted.size) {
                        send("command.vote.failed")

                        voting = false
                        voteType = null
                        voteTarget = null
                        voteTargetUUID = null
                        voteReason = null
                        voteMap = null
                        voteWave = null
                        voteStarter = null
                        voted = Seq<String>()
                        count = 60
                    }
                }

                if (Config.pvpPeace) {
                    if (Trigger.pvpCount != 0) {
                        Trigger.pvpCount--
                    } else {
                        state.rules.blockDamageMultiplier = orignalBlockMultiplier
                        state.rules.unitDamageMultiplier = orignalUnitMultiplier
                        send("trigger.pvp.end")
                    }
                }

                secondCount = 0
            } else {
                secondCount++
            }

            if (minuteCount == 3600) {
                val data = database.getAll()

                for (a in data) {
                    if (a.status.containsKey("ban")) {
                        if (LocalDateTime.now().isAfter(LocalDateTime.parse(a.status.get("ban")))) {
                            Vars.netServer.admins.unbanPlayerID(a.uuid)
                        }
                    }
                }

                if (rollbackCount == 0) {
                    Core.app.post { SaveIO.save(Vars.saveDirectory.child("rollback.msav")) }
                    rollbackCount = Config.rollbackTime
                } else {
                    rollbackCount--
                }

                if (Config.message) {
                    if (messageCount == Config.messageTime) {
                        for (a in database.players) {
                            val message = if (Main.root.child("messages/${a.languageTag}.txt").exists()) {
                                Main.root.child("messages/${a.languageTag}.txt").readString()
                            } else {
                                val file = Main.root.child("messages/en.txt")
                                if (file.exists()) file.readString() else ""
                            }
                            val c = message.split(Regex("\r\n"))

                            if (c.size <= messageOrder) {
                                messageOrder = 0
                            }
                            a.player.sendMessage(c[messageOrder])

                        }
                        messageOrder++
                        messageCount = 0
                    } else {
                        messageCount++
                    }
                }
                minuteCount = 0
            } else {
                minuteCount++
            }
        }
    }

    fun log(type: LogType, text: String, vararg name: String) {
        val root: Fi = Core.settings.dataDirectory.child("mods/Essentials/")
        val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        if (type != LogType.Report) {
            val date = DateTimeFormatter.ofPattern("yyyy-MM-dd HH_mm_ss").format(LocalDateTime.now())
            val new = Paths.get(root.child("log/$type.log").path())
            val old = Paths.get(root.child("log/old/$type/$date.log").path())
            var main = root.child("log/$type.log")
            val folder = root.child("log")

            if (main != null && main.length() > 2048 * 256) {
                main.writeString("end of file. $date", true)
                try {
                    if (!root.child("log/old/$type").exists()) {
                        root.child("log/old/$type").mkdirs()
                    }
                    Files.move(new, old, StandardCopyOption.REPLACE_EXISTING)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                main = null
            }
            if (main == null) main = folder.child("$type.log")
            main!!.writeString("[$time] $text\n", true)
        } else {
            val main = root.child("log/report/$time $name.txt")
            main.writeString(text)
        }
    }

    enum class LogType {
        Player, Tap, WithDraw, Block, Deposit, Chat, Report
    }

    class IpAddressMatcher(ipAddress: String) {
        private var nMaskBits = 0
        private val requiredAddress: InetAddress
        fun matches(address: String): Boolean {
            val remoteAddress = parseAddress(address)
            if (requiredAddress.javaClass != remoteAddress.javaClass) {
                return false
            }
            if (nMaskBits < 0) {
                return remoteAddress == requiredAddress
            }
            val remAddr = remoteAddress.address
            val reqAddr = requiredAddress.address
            val nMaskFullBytes = nMaskBits / 8
            val finalByte = (0xFF00 shr (nMaskBits and 0x07)).toByte()
            for (i in 0 until nMaskFullBytes) {
                if (remAddr[i] != reqAddr[i]) {
                    return false
                }
            }
            return if (finalByte.toInt() != 0) {
                remAddr[nMaskFullBytes] and finalByte == reqAddr[nMaskFullBytes] and finalByte
            } else true
        }

        private fun parseAddress(address: String): InetAddress {
            return try {
                InetAddress.getByName(address)
            } catch (e: UnknownHostException) {
                throw IllegalArgumentException("Failed to parse address$address", e)
            }
        }

        init {
            var address = ipAddress
            if (address.indexOf('/') > 0) {
                val addressAndMask = address.split("/").toTypedArray()
                address = addressAndMask[0]
                nMaskBits = addressAndMask[1].toInt()
            } else {
                nMaskBits = -1
            }
            requiredAddress = parseAddress(address)
            assert(requiredAddress.address.size * 8 >= nMaskBits) {
                String.format("IP address %s is too short for bitmask of length %d", address, nMaskBits)
            }
        }
    }

    fun findPlayerData(uuid: String): DB.PlayerData? {
        return database.players.find { e -> e.uuid == uuid }
    }

    fun findPlayers(name: String): Playerc? {
        return if (name.toIntOrNull() != null) {
            val d = players.find { it.asObject().get("id").asInt() == name.toInt() }
            if (d != null) {
                Groups.player.find { p -> p.uuid() == d.asObject().get("uuid").asString() }
            } else {
                null
            }
        } else {
            Groups.player.find { p -> p.name.contains(name, true) }
        }
    }
}