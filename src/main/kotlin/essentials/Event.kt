package essentials

import arc.ApplicationListener
import arc.Core
import arc.Events
import arc.files.Fi
import arc.graphics.Color
import arc.graphics.Colors
import arc.struct.ObjectMap
import arc.struct.ObjectSet
import arc.struct.Seq
import arc.util.Align
import arc.util.Log
import arc.util.Strings
import arc.util.Time
import com.github.pemistahl.lingua.api.IsoCode639_1
import com.github.pemistahl.lingua.api.Language
import com.github.pemistahl.lingua.api.LanguageDetector
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder
import essentials.Main.Companion.database
import mindustry.Vars.*
import mindustry.content.*
import mindustry.core.NetServer
import mindustry.entities.Damage
import mindustry.game.EventType.*
import mindustry.game.Team
import mindustry.game.Teams.TeamData
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.gen.Playerc
import mindustry.io.SaveIO
import mindustry.maps.Map
import mindustry.net.Administration.PlayerInfo
import mindustry.net.Packets
import mindustry.net.WorldReloader
import mindustry.world.Tile
import org.hjson.JsonArray
import org.hjson.Stringify
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.function.Consumer
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.experimental.and
import kotlin.io.path.Path
import kotlin.math.abs
import kotlin.math.floor

object Event {
    var orignalBlockMultiplier = 1f
    var orignalUnitMultiplier = 1f
    var enemyBuildingDestroyed = 0

    var voting = false
    var voteType : String? = null
    var voteTarget : Playerc? = null
    var voteTargetUUID : String? = null
    var voteReason : String? = null
    var voteMap : Map? = null
    var voteWave : Int? = null
    var voteStarter : Playerc? = null
    var isPvP : Boolean = false
    var voteTeam : Team = state.rules.defaultTeam
    var voteCooltime : Int = 0
    var voted = Seq<String>()
    var lastVoted = LocalTime.now()
    var isAdminVote = false
    var isCanceled = false

    var worldHistory = Seq<TileLog>()
    var voterCooltime = ObjectMap<String, Int>()

    private var random = Random()
    private var dateformat = SimpleDateFormat("HH:mm:ss")
    var blockExp = ObjectMap<String, Int>()
    var dosBlacklist = ObjectSet<String>()
    var pvpCount = Config.pvpPeaceTime
    var count = 60
    var pvpSpectors = Seq<String>()
    var pvpPlayer = ObjectMap<String, Team>()
    var isGlobalMute = false
    var dpsBlocks = 0f
    var dpsTile : Tile? = null
    var maxdps = 0f
    var unitLimitMessageCooldown = 0
    val offlinePlayers = Seq<DB.PlayerData>()

    private val specificTextRegex : Pattern = Pattern.compile("[!@#\$%&*()_+=|<>?{}\\[\\]~-]")
    private val blockSelectRegex : Pattern = Pattern.compile(".*build.*")
    private val nameRegex : Pattern = Pattern.compile("(.*\\[.*].*)|　|^(.*\\s+.*)+\$")

    fun register() {
        Events.on(WithdrawEvent::class.java) {
            if (it.tile != null && it.player.unit().item() != null && it.player.name != null) {
                log(LogType.WithDraw, Bundle()["log.withdraw", it.player.plainName(), it.player.unit().item().name, it.amount, it.tile.block.name, it.tile.tileX(), it.tile.tileY()])
                addLog(TileLog(System.currentTimeMillis(), it.player.name, "withdraw", it.tile.tile.x, it.tile.tile.y, it.tile.block().name, it.tile.rotation, it.tile.team, it.tile.config()))
            }
        }

        Events.on(DepositEvent::class.java) {
            if (it.tile != null && it.player.unit().item() != null && it.player.name != null) {
                log(LogType.Deposit, Bundle()["log.deposit", it.player.plainName(), it.player.unit().item().name, it.amount, it.tile.block.name, it.tile.tileX(), it.tile.tileY()])
                addLog(TileLog(System.currentTimeMillis(), it.player.name, "deposit", it.tile.tile.x, it.tile.tile.y, it.tile.block().name, it.tile.rotation, it.tile.team, it.tile.config()))
            }
        }

        Events.on(ConfigEvent::class.java) {
            if (it.tile != null && it.tile.block() != null && it.player != null) {
                if (Config.antiGrief && it.value is Int) {
                    val entity = it.tile
                    val other = world.tile(it.value as Int)
                    val valid = other != null && entity.power != null && other.block().hasPower && other.block().outputsPayload && other.block() != Blocks.massDriver && other.block() == Blocks.payloadMassDriver && other.block() == Blocks.largePayloadMassDriver
                    if (valid) {
                        val oldGraph = entity.power.graph
                        val newGraph = other.build.power.graph
                        val oldGraphCount = oldGraph.toString().substring(oldGraph.toString().indexOf("all=["), oldGraph.toString().indexOf("], graph")).replaceFirst("all=\\[".toRegex(), "").split(",").toTypedArray().size
                        val newGraphCount = newGraph.toString().substring(newGraph.toString().indexOf("all=["), newGraph.toString().indexOf("], graph")).replaceFirst("all=\\[".toRegex(), "").split(",").toTypedArray().size
                        if (abs(oldGraphCount - newGraphCount) > 10) {
                            database.players.forEach { a ->
                                a.player.sendMessage(Bundle(a.languageTag)["event.antigrief.node", it.player.name, oldGraphCount.coerceAtLeast(newGraphCount), oldGraphCount.coerceAtMost(newGraphCount), "${it.tile.x}, ${it.tile.y}"])
                            }
                        }
                    }
                }

                addLog(TileLog(System.currentTimeMillis(), it.player.name, "config", it.tile.tile.x, it.tile.tile.y, it.tile.block().name, it.tile.rotation, it.tile.team, it.value))
            }
        }

        Events.on(TapEvent::class.java) {
            log(LogType.Tap, Bundle()["log.tap", it.player.plainName(), it.tile.block().name])
            addLog(TileLog(System.currentTimeMillis(), it.player.name, "tap", it.tile.x, it.tile.y, it.tile.block().name, if (it.tile.build != null) it.tile.build.rotation else 0, if (it.tile.build != null) it.tile.build.team else state.rules.defaultTeam, null))
            val data = findPlayerData(it.player.uuid())
            if (data != null) {
                PluginData.warpBlocks.forEach { two ->
                    if (it.tile.block().name == two.tileName && it.tile.build.tileX() == two.x && it.tile.build.tileY() == two.y) {
                        if (two.online) {
                            database.players.forEach { data ->
                                data.player.sendMessage(Bundle(data.languageTag)["event.tap.server", it.player.plainName(), two.description])
                            }
                            Log.info(Bundle()["log.warp.move.block", it.player.plainName(), Strings.stripColors(two.description), two.ip, two.port.toString()])
                            Call.connect(it.player.con(), two.ip, two.port)
                        }
                        return@forEach
                    }
                }

                PluginData.warpZones.forEach { two ->
                    if (it.tile.x > two.startTile.x && it.tile.x < two.finishTile.x && it.tile.y > two.startTile.y && it.tile.y < two.finishTile.y) {
                        Log.info(Bundle()["log.warp.move", it.player.plainName(), two.ip, two.port.toString()])
                        Call.connect(it.player.con(), two.ip, two.port)
                        return@forEach
                    }
                }

                if (data.log) {
                    val buf = Seq<TileLog>()
                    worldHistory.forEach { two ->
                        if (two.x == it.tile.x && two.y == it.tile.y) {
                            buf.add(two)
                        }
                    }
                    val str = StringBuilder()
                    val bundle = Bundle(data.languageTag)
                    val coreBundle = ResourceBundle.getBundle("bundle_block", try {
                        when (data.languageTag) {
                            "ko" -> Locale.KOREA
                            else -> Locale.ENGLISH
                        }
                    } catch (e : Exception) {
                        Locale.ENGLISH
                    })

                    buf.forEach { two ->
                        val action = when (two.action) {
                            "tap" -> "[royal]${bundle["event.log.tap"]}[]"
                            "break" -> "[scarlet]${bundle["event.log.break"]}[]"
                            "place" -> "[sky]${bundle["event.log.place"]}[]"
                            "config" -> "[cyan]${bundle["event.log.config"]}[]"
                            "withdraw" -> "[green]${bundle["event.log.withdraw"]}"
                            "deposit" -> "[brown]${bundle["event.log.deposit"]}"
                            else -> ""
                        }

                        str.append(bundle["event.log.format", dateformat.format(two.time), two.player, coreBundle.getString("block.${two.tile}.name"), action]).append("\n")
                    }

                    Call.effect(it.player.con(), Fx.shockwave, it.tile.getX(), it.tile.getY(), 0f, Color.cyan)
                    val str2 = StringBuilder()
                    if (str.toString().lines().size > 10) {
                        val lines : List<String> = str.toString().split("\n").reversed()
                        for (i in 0 until 10) {
                            str2.append(lines[i]).append("\n")
                        }
                        it.player.sendMessage(str2.toString())
                    } else {
                        it.player.sendMessage(str.toString())
                    }
                }

                database.players.forEach { two ->
                    if (two.tracking) {
                        Call.effect(two.player.con(), Fx.bigShockwave, it.tile.getX(), it.tile.getY(), 0f, Color.cyan)
                    }
                }
            }
        }

        Events.on(PickupEvent::class.java) {

        }

        Events.on(UnitControlEvent::class.java) {

        }

        Events.on(WaveEvent::class.java) {
            if (Config.waveskip > 1) {
                var loop = 1
                while (Config.waveskip != loop) {
                    loop++
                    spawner.spawnEnemies()
                    state.wave++
                    state.wavetime = state.rules.waveSpacing
                }
            }
        }

        Events.on(ServerLoadEvent::class.java) {
            content.blocks().each { two ->
                var buf = 0
                two.requirements.forEach { item ->
                    buf = +item.amount
                }
                blockExp.put(two.name, buf)
            }

            dosBlacklist = netServer.admins.dosBlacklist

            if (Config.countAllServers) {
                Core.settings.put("totalPlayers", 0)
                Core.settings.saveValues()
            }

            val os = System.getProperty("os.name").lowercase(Locale.getDefault())
            if (!Config.blockIP && Config.database != Main.root.child("database").absolutePath() && PluginData["iptablesFirst"] != null) {
                Log.warn(Bundle()["event.database.blockip.conflict"])

                if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                    Config.blockIP = true
                    Log.info(Bundle()["config.blockIP.enabled"])
                }
            } else if (!Config.blockIP && PluginData["iptablesFirst"] != null) {
                if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                    netServer.admins.banned.forEach { data ->
                        data.ips.forEach { ip ->
                            val cmd = arrayOf("/bin/bash", "-c", "echo ${PluginData.sudoPassword}| sudo -S iptables -D INPUT -s $ip -j DROP")
                            Runtime.getRuntime().exec(cmd)
                        }
                    }
                    PluginData.status.remove("iptablesFirst")
                    Log.info(Bundle()["event.ban.iptables.remove"])
                    PluginData.save(false)
                    PluginData.changed = true
                }
            } else if (Config.blockIP && PluginData["iptablesFirst"] == null) {
                if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                    netServer.admins.banned.forEach { data ->
                        data.ips.forEach { ip ->
                            val cmd = arrayOf("/bin/bash", "-c", "echo ${PluginData.sudoPassword}| sudo -S iptables -A INPUT -s $ip -j DROP")
                            Runtime.getRuntime().exec(cmd)
                            Log.info(Bundle()["event.ban.iptables.exists", ip, data.lastName])
                        }
                    }
                    PluginData.status.put("iptablesFirst", "none")
                    PluginData.save(false)
                    PluginData.changed = true
                }
            }

            netServer.chatFormatter = NetServer.ChatFormatter { player : Player, message : String ->
                var isMute = false

                if (!message.startsWith("/")) {
                    val data = findPlayerData(player.uuid())
                    if (data != null) {
                        log(LogType.Chat, "${data.name}: $message")

                        if (!data.mute) {
                            val isAdmin = Permission.check(player, "vote.pass") // todo 자신이 시작한 투표에 자기 자신이 y 를 쳐서 투표에 참여하면 무조건 투표 통과가 되는 문제 (확인 필요)
                            if (voting && message.equals("y", true) && voteStarter != player && !voted.contains(player.uuid())) {
                                if (isAdmin) {
                                    isAdminVote = true
                                } else {
                                    if (state.rules.pvp && voteTeam == player.team()) {
                                        voted.add(player.uuid())
                                    } else if (!state.rules.pvp) {
                                        voted.add(player.uuid())
                                    }
                                }
                                player.sendMessage(Bundle(data.languageTag)["command.vote.voted"])
                            } else if (voting && message.equals("n", true) && isAdmin) {
                                isCanceled = true
                            }

                            if (Config.chatlimit) {
                                val configs = Config.chatlanguage.split(",")
                                val languages = ArrayList<Language>()
                                configs.forEach { a -> languages.add(Language.getByIsoCode639_1(IsoCode639_1.valueOf(a.uppercase()))) }

                                val d : LanguageDetector = LanguageDetectorBuilder.fromLanguages(*languages.toTypedArray()).build()
                                val e : Language = d.detectLanguageOf(message)

                                if (e.name == "UNKNOWN" && !specificTextRegex.matcher(message.substring(0, 1)).matches() && !(voting && message.equals("y", true) && !voted.contains(player.uuid()))) {
                                    player.sendMessage(Bundle(data.languageTag)["event.chat.language.not.allow"])
                                    isMute = true
                                }
                            }

                            if (Config.chatBlacklist) {
                                val file = Main.root.child("chat_blacklist.txt").readString("UTF-8").split("\r\n")
                                if (file.isNotEmpty()) {
                                    file.forEach { text ->
                                        if (Config.chatBlacklistRegex) {
                                            if (message.contains(Regex(text))) {
                                                player.sendMessage(Bundle(findPlayerData(player.uuid())!!.languageTag)["event.chat.blacklisted"])
                                                isMute = true
                                            }
                                        } else {
                                            if (message.contains(text)) {
                                                player.sendMessage(Bundle(findPlayerData(player.uuid())!!.languageTag)["event.chat.blacklisted"])
                                                isMute = true
                                            }
                                        }
                                    }
                                }
                            }
                            val format = Permission[player].chatFormat.replace("%1", "[#${player.color}]${data.name}").replace("%2", message)
                            return@ChatFormatter if (isGlobalMute && Permission.check(player, "chat.admin")) {
                                format
                            } else if (!isGlobalMute && !(voting && message.contains("y") && !isMute)) {
                                format
                            } else {
                                null
                            }
                        } else {
                            return@ChatFormatter null
                        }
                    } else {
                        return@ChatFormatter "[gray]${player.name} [orange] > [white]${message}"
                    }
                } else {
                    return@ChatFormatter null
                }
            }
        }

        Events.on(GameOverEvent::class.java) {
            if (voting) {
                database.players.forEach { a ->
                    if (voteTargetUUID != a.uuid) a.player.sendMessage(Bundle(a.languageTag)["command.vote.canceled"])
                }

                resetVote()
            }

            if (!state.rules.infiniteResources) {
                if (state.rules.pvp) {
                    for (data in database.players) {
                        if (data.player.team() == it.winner) {
                            data.pvpVictoriesCount++
                        }
                    }
                } else if (state.rules.attackMode) {
                    for (data in database.players) {
                        if (data.player.team() == it.winner) {
                            data.attackModeClear++
                        }
                    }
                }
                for (data in database.players) {
                    earnEXP(it.winner, data.player, data, true)
                }
                for (data in offlinePlayers) {
                    earnEXP(it.winner, data.player, data, false)
                }
            }
            if (voting && voteType == "gg") resetVote()
            worldHistory = Seq()
            pvpSpectors = Seq()
            pvpPlayer = ObjectMap()
            dpsTile = null
        }

        Events.on(BlockBuildBeginEvent::class.java) {

        }

        Events.on(BlockBuildEndEvent::class.java) {
            val isDebug = Core.settings.getBool("debugMode")

            if (it.unit.isPlayer) {
                val player = it.unit.player
                val target = findPlayerData(player.uuid())

                if (!player.unit().isNull && target != null && it.tile.block() != null && player.unit().buildPlan() != null) {
                    val block = it.tile.block()
                    if (!it.breaking) {
                        log(LogType.Block, Bundle()["log.block.place", target.name, block.name, it.tile.x, it.tile.y])
                        addLog(TileLog(System.currentTimeMillis(), target.name, "place", it.tile.x, it.tile.y, it.tile.block().name, if (it.tile.build != null) it.tile.build.rotation else 0, if (it.tile.build != null) it.tile.build.team else state.rules.defaultTeam, it.config))

                        if (!state.rules.infiniteResources) {
                            target.blockPlaceCount++
                            target.exp = target.exp + blockExp.get(block.name)
                        }

                        if (isDebug) {
                            Log.info("${player.name} placed ${it.tile.block().name} to ${it.tile.x},${it.tile.y}")
                        }
                    } else if (it.breaking) {
                        log(LogType.Block, Bundle()["log.block.break", target.name, block.name, it.tile.x, it.tile.y])
                        addLog(TileLog(System.currentTimeMillis(), target.name, "break", it.tile.x, it.tile.y, player.unit().buildPlan().block.name, if (it.tile.build != null) it.tile.build.rotation else 0, if (it.tile.build != null) it.tile.build.team else state.rules.defaultTeam, it.config))

                        if (!state.rules.infiniteResources) {
                            target.blockBreakCount++
                            target.exp = target.exp - blockExp.get(player.unit().buildPlan().block.name)
                        }
                    }
                }
            }
        }

        Events.on(BuildSelectEvent::class.java) {
            if (it.builder is Playerc && it.builder.buildPlan() != null && !blockSelectRegex.matcher(it.builder.buildPlan().block.name).matches() && it.tile.block() !== Blocks.air && it.breaking) {
                log(LogType.Block, Bundle()["log.block.remove", (it.builder as Playerc).plainName(), it.tile.block().name, it.tile.x, it.tile.y])
            }
        }

        Events.on(BlockDestroyEvent::class.java) {
            if (Config.destroyCore && state.rules.coreCapture) {
                Fx.spawnShockwave.at(it.tile.getX(), it.tile.getY(), state.rules.dropZoneRadius)
                Damage.damage(world.tile(it.tile.pos()).team(), it.tile.getX(), it.tile.getY(), state.rules.dropZoneRadius, 1.0E8f, true)
            }

            if (state.rules.attackMode && it.tile.team() != state.rules.defaultTeam) {
                enemyBuildingDestroyed++
            }
        }

        Events.on(UnitDestroyEvent::class.java) {

        }

        Events.on(UnitCreateEvent::class.java) { u ->
            if (Groups.unit.size() > Config.spawnLimit) {
                u.unit.kill()

                if (unitLimitMessageCooldown == 0) {
                    database.players.forEach {
                        it.player.sendMessage(Bundle(it.languageTag)["config.spawnlimit.reach", "[scarlet]${Groups.unit.size()}[white]/[sky]${Config.spawnLimit}"])
                    }
                    unitLimitMessageCooldown = 60
                }
            }
        }

        Events.on(UnitChangeEvent::class.java) {

        }

        Events.on(PlayerJoin::class.java) {
            log(LogType.Player, Bundle()["log.joined", it.player.plainName(), it.player.uuid(), it.player.con.address])
            it.player.admin(false)

            val data = database[it.player.uuid()]
            if (Config.authType == Config.AuthType.None) {
                if (data != null) {
                    Trigger.loadPlayer(it.player, data, false)
                } else if (Config.authType != Config.AuthType.None) {
                    it.player.sendMessage(Bundle(it.player.locale)["event.player.first.register"])
                } else if (Config.authType == Config.AuthType.None) {
                    Main.daemon.submit(Thread {
                        if (database.getAll().contains { a -> a.name == it.player.name() }) {
                            Core.app.post { it.player.con.kick(Bundle(it.player.locale)["event.player.name.duplicate"], 0L) }
                        } else {
                            Core.app.post { Trigger.createPlayer(it.player, null, null) }
                        }
                    })
                }
            }
        }

        Events.on(PlayerLeave::class.java) {
            log(LogType.Player, Bundle()["log.player.disconnect", it.player.plainName(), it.player.uuid(), it.player.con.address])
            val data = database.players.find { data -> data.name == it.player.name }
            if (data != null) {
                data.lastPlayedWorldName = state.map.plainName()
                data.lastPlayedWorldMode = state.rules.modeName
                data.lastPlayedWorldId = port
                data.lastLeaveDate = LocalDateTime.now()
                data.isConnected = false

                if (data.oldUUID != null) {
                    data.uuid = data.oldUUID!!
                    data.oldUUID = null
                }

                database.queue(data)
                offlinePlayers.add(data)
                database.players.remove(data)
            }
        }

        Events.on(PlayerBanEvent::class.java) {
            if (Config.blockIP) {
                val os = System.getProperty("os.name").lowercase(Locale.getDefault())
                if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                    val ip = if (it.player != null) it.player.ip() else netServer.admins.getInfo(it.uuid).lastIP
                    Runtime.getRuntime().exec(arrayOf("/bin/bash", "-c", "echo ${PluginData.sudoPassword} | sudo -S iptables -D INPUT -s $ip -j DROP"))
                    Runtime.getRuntime().exec(arrayOf("/bin/bash", "-c", "echo ${PluginData.sudoPassword} | sudo -S iptables -A INPUT -s $ip -j DROP"))
                    Log.info(Bundle()["event.ban.iptables", ip])
                }
            }

            val name = if (it.player == null) {
                netServer.admins.getInfo(it.uuid).lastName
            } else {
                it.player.name
            }
            val ip = if (it.player == null) {
                netServer.admins.getInfo(it.uuid).lastIP
            } else {
                it.player.ip()
            }

            val ipBanList = JsonArray.readHjson(Config.ipBanList.readString()).asArray()
            for (a in netServer.admins.getInfo(it.uuid).ips) {
                ipBanList.add(a)
            }
            val idBanList = JsonArray.readHjson(Config.idBanList.readString()).asArray()
            idBanList.add(it.uuid)

            Config.idBanList.writeString(idBanList.toString(Stringify.HJSON))
            Config.ipBanList.writeString(ipBanList.toString(Stringify.HJSON))

            netServer.admins.playerInfo.values().forEach(Consumer { info : PlayerInfo -> info.banned = false })
            netServer.admins.save()

            log(LogType.Player, Bundle()["log.player.banned", name, ip])
        }

        Events.on(PlayerUnbanEvent::class.java) {
            if (Config.blockIP) {
                val os = System.getProperty("os.name").lowercase(Locale.getDefault())
                if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                    val ip = if (it.player != null) it.player.ip() else netServer.admins.getInfo(it.uuid).lastIP
                    Runtime.getRuntime().exec(arrayOf("/bin/bash", "-c", "echo ${PluginData.sudoPassword} | sudo -S iptables -D INPUT -s $ip -j DROP"))
                }
            }

            val ipBanList = JsonArray.readHjson(Config.ipBanList.readString()).asArray()
            for (a in netServer.admins.getInfo(it.uuid).ips) {
                ipBanList.removeAll { b -> b.asString() == a }
            }
            val idBanList = JsonArray.readHjson(Config.idBanList.readString()).asArray()
            idBanList.removeAll { a -> a.asString() == it.uuid }

            Config.idBanList.writeString(idBanList.toString(Stringify.HJSON))
            Config.ipBanList.writeString(ipBanList.toString(Stringify.HJSON))
        }

        Events.on(WorldLoadEvent::class.java) {
            PluginData.playtime = 0L
            dpsTile = null
            if (saveDirectory.child("rollback.msav").exists()) saveDirectory.child("rollback.msav").delete()

            if (state.rules.pvp) {
                if (Config.pvpPeace) {
                    orignalBlockMultiplier = state.rules.blockDamageMultiplier
                    orignalUnitMultiplier = state.rules.unitDamageMultiplier
                    state.rules.blockDamageMultiplier = 0f
                    state.rules.unitDamageMultiplier = 0f
                    pvpCount = Config.pvpPeaceTime
                }
                pvpSpectors = Seq<String>()

                for (data in database.players) {
                    if (Permission.check(data.player, "pvp.spector")) {
                        data.player.team(Team.derelict)
                    }
                }
            }

            for (data in database.players) {
                data.currentPlayTime = 0
            }
        }

        Events.on(ConnectPacketEvent::class.java) {
            val isIPbanned = JsonArray.readHjson(Config.ipBanList.readString()).asArray().contains(it.connection.address)
            val isIDbanned = JsonArray.readHjson(Config.idBanList.readString()).asArray().contains(it.packet.uuid)
            if (isIPbanned || isIDbanned) {
                Call.kick(it.connection, Packets.KickReason.banned)
                Log.info(Bundle()["event.player.banned", it.packet.name, if(isIPbanned) "IP (${it.connection.address})" else "UUID (${it.packet.uuid})"])
                return@on
            }

            log(LogType.Player, "${it.packet.name} (${it.packet.uuid}, ${it.connection.address}) connected.")

            if (Config.blockNewUser && netServer.admins.getInfo(it.packet.uuid) == null) {
                it.connection.kick(Bundle(it.packet.locale)["event.player.new.blocked"], 0L)
                return@on
            }

            if (!Config.allowMobile && it.connection.mobile) {
                it.connection.kick(Bundle(it.packet.locale)["event.player.not.allow.mobile"], 0L)
            }

            // 닉네임이 블랙리스트에 등록되어 있는지 확인
            PluginData.blacklist.forEach { pattern ->
                if (pattern.matcher(it.packet.name).matches()) it.connection.kick(Bundle(it.packet.locale)["event.player.name.blacklisted"], 0L)
            }

            if (Config.fixedName) {
                if (it.packet.name.length > 32) it.connection.kick(Bundle(it.packet.locale)["event.player.name.long"], 0L)
                if (nameRegex.matcher(it.packet.name).matches()) it.connection.kick(Bundle(it.packet.locale)["event.player.name.not.allow"], 0L)
            }

            if (Config.minimalName && it.packet.name.length < 4) it.connection.kick(Bundle(it.packet.locale)["event.player.name.short"], 0L)

            if (Config.antiVPN) {
                PluginData.vpnList.forEach { text ->
                    val match = IpAddressMatcher(text)
                    if (match.matches(it.connection.address)) {
                        it.connection.kick(Bundle(it.packet.locale)["anti-grief.vpn"])
                    }
                }
            }
        }

        Events.on(BuildingBulletDestroyEvent::class.java) {
            val cores = listOf(Blocks.coreAcropolis, Blocks.coreBastion, Blocks.coreCitadel, Blocks.coreFoundation, Blocks.coreAcropolis, Blocks.coreNucleus, Blocks.coreShard)
            if (state.rules.pvp) {
                if (it.build.closestCore() == null && cores.contains(it.build.block())) {
                    for (data in database.players) {
                        if (data.player.team() == it.bullet.team) {
                            data.pvpEliminationTeamCount++
                        }
                        data.player.sendMessage(Bundle(data.languageTag)["event.bullet.kill", it.bullet.team.coloredName(), it.build.team.coloredName()])
                    }
                    if(netServer.isWaitingForPlayers) {
                        for (t in state.teams.getActive()) {
                            if (Groups.player.count { p : Player -> p.team() === t.team } > 0) {
                                Events.fire(GameOverEvent(t.team))
                                return@on
                            }
                        }
                    }
                }
            }
        }

        fun send(message : String, vararg parameter : Any) {
            database.players.forEach {
                if (voteTargetUUID != it.uuid) {
                    Core.app.post { it.player.sendMessage(Bundle(it.languageTag).get(message, *parameter)) }
                }
            }
        }

        fun check() : Int {
            return if (!isPvP) {
                when (database.players.size) {
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
                when (database.players.count { a -> a.player.team() == voteTeam }) {
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

        fun back(map : Map?) {
            Core.app.post {
                val savePath : Fi = saveDirectory.child("rollback.msav")

                try {
                    val mode = state.rules.mode()
                    val reloader = WorldReloader()

                    reloader.begin()

                    if (map != null) {
                        world.loadMap(map, map.applyRules(mode))
                    } else {
                        SaveIO.load(savePath)
                    }

                    state.rules = state.map.applyRules(mode)

                    logic.play()
                    reloader.end()
                } catch (t : Exception) {
                    t.printStackTrace()
                }
                if (map == null) send("command.vote.back.done")
            }
        }

        var colorOffset = 0
        fun nickcolor(name : String, player : Playerc) {
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
            newName.forEach {
                stringBuilder.append(it)
            }
            player.name(stringBuilder.toString())
        }

        var milsCount = 0
        var secondCount = 0
        var minuteCount = 0

        var rollbackCount = Config.rollbackTime
        var messageCount = Config.messageTime
        var messageOrder = 0

        data class effectData(val x : Float, val y : Float, val rotate : Float, val effectLevel : Int?, val level : Int, val color : Color)

        Core.app.addListener(object: ApplicationListener {
            override fun update() {
                if (Config.unbreakableCore) {
                    state.rules.defaultTeam.cores().forEach {
                        it.health(1.0E8f)
                    }
                }

                for (it in database.players) {
                    if (state.rules.pvp) {
                        if (it.player.unit() != null && it.player.team().cores().isEmpty && it.player.team() != Team.derelict && pvpPlayer.containsKey(it.uuid)) {
                            it.pvpDefeatCount++
                            it.player.team(Team.derelict)
                            pvpSpectors.add(it.uuid)
                            pvpPlayer.remove(it.uuid)
                        }
                    }

                    if (it.status.containsKey("freeze")) {
                        val d = findPlayerData(it.uuid)
                        if (d != null) {
                            val player = d.player
                            val split = it.status.get("freeze").toString().split("/")
                            player.set(split[0].toFloat(), split[1].toFloat())
                            Call.setPosition(player.con(), split[0].toFloat(), split[1].toFloat())
                            Call.setCameraPosition(player.con(), split[0].toFloat(), split[1].toFloat())
                            player.x(split[0].toFloat())
                            player.y(split[1].toFloat())
                        }
                    }

                    if (it.tracking) {
                        Groups.player.forEach { player ->
                            Call.label(it.player.con(), player.name, Time.delta / 2, player.mouseX, player.mouseY)
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
                }

                if (Config.border) {
                    Groups.unit.forEach {
                        if (it.x < 0 || it.y < 0 || it.x > (world.width() * 8) || it.y > (world.height() * 8)) {
                            it.kill()
                        }
                    }
                }

                if (Config.moveEffects) {
                    if (milsCount == 5) {
                        val effectList = Seq<effectData>()

                        database.players.forEach {
                            if (it.player.unit() != null && it.player.unit().health > 0f) {
                                val color = if (it.effectColor != null) {
                                    if (Colors.get(it.effectColor) != null) Colors.get(it.effectColor) else Color.valueOf(it.effectColor)
                                } else {
                                    when (it.level) {
                                        in 10..19 -> Color.sky
                                        in 20..29 -> Color.orange
                                        in 30..39 -> Color.red
                                        in 40..49 -> Color.sky
                                        in 50..59 -> Color.sky
                                        in 60..69 -> Color.sky
                                        in 70..79 -> Color.orange
                                        in 80..89 -> Color.orange
                                        in 90..99 -> Color.orange
                                        in 100..Int.MAX_VALUE -> Color.orange
                                        else -> Color.orange
                                    }
                                }

                                effectList.add(effectData(it.player.x, it.player.y, it.player.unit().rotation, it.effectLevel, it.level, color))
                            }
                        }

                        database.players.forEach { data ->
                            if (data.showLevelEffects) {
                                effectList.forEach {
                                    when (it.effectLevel ?: it.level) {
                                        in 10..19 -> Call.effect(data.player.con(), Fx.freezing, it.x, it.y, it.rotate, it.color)
                                        in 20..29 -> Call.effect(data.player.con(), Fx.overdriven, it.x, it.y, it.rotate, it.color)
                                        in 30..39 -> {
                                            Call.effect(data.player.con(), Fx.burning, it.x, it.y, it.rotate, it.color)
                                            Call.effect(data.player.con(), Fx.melting, it.x, it.y, it.rotate, it.color)
                                        }

                                        in 40..49 -> Call.effect(data.player.con(), Fx.steam, it.x, it.y, it.rotate, it.color)
                                        in 50..59 -> Call.effect(data.player.con(), Fx.shootSmallSmoke, it.x, it.y, it.rotate, it.color)
                                        in 60..69 -> Call.effect(data.player.con(), Fx.mine, it.x, it.y, it.rotate, it.color)
                                        in 70..79 -> Call.effect(data.player.con(), Fx.explosion, it.x, it.y, it.rotate, it.color)
                                        in 80..89 -> Call.effect(data.player.con(), Fx.hitLaser, it.x, it.y, it.rotate, it.color)
                                        in 90..99 -> Call.effect(data.player.con(), Fx.crawlDust, it.x, it.y, it.rotate, it.color)
                                        in 100..Int.MAX_VALUE -> Call.effect(data.player.con(), Fx.mineImpact, it.x, it.y, it.rotate, it.color)
                                        else -> {}
                                    }
                                }
                            }
                        }
                        milsCount = 0
                    } else {
                        milsCount++
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

                if (secondCount == 60) {
                    PluginData.uptime++
                    PluginData.playtime++

                    if (voteCooltime > 0) voteCooltime--
                    voterCooltime.forEach {
                        voterCooltime.put(it.key, it.value--)
                        if (it.value == 0) voterCooltime.remove(it.key)
                    }

                    if (dpsTile != null) {
                        if (dpsBlocks > maxdps) maxdps = dpsBlocks
                        val message = "Max DPS: $maxdps/min\nDPS: ${dpsBlocks}/s"
                        Call.label(message, 1f, dpsTile!!.worldx(), dpsTile!!.worldy())
                    } else {
                        maxdps = 0f
                    }
                    dpsBlocks = 0f

                    for (it in database.players) {
                        it.totalPlayTime++
                        it.currentPlayTime++

                        if (it.animatedName) {
                            val name = it.name.replace("\\[(.*?)]".toRegex(), "")
                            nickcolor(name, it.player)
                        } else {
                            it.player.name(it.name)
                        }

                        // 잠수 플레이어 카운트
                        if (Config.afk && it.player.unit() != null && !it.player.unit().moving() && !it.player.unit().mining() && !Permission.check(it.player, "afk.admin")) {
                            it.afkTime++
                            if (it.afkTime == Config.afkTime) {
                                if (Config.afkServer.isEmpty()) {
                                    it.player.kick(Bundle(it.languageTag)["event.player.afk"])
                                    database.players.forEach { data ->
                                        data.player.sendMessage(Bundle(data.languageTag)["event.player.afk.other", it.player.plainName()])
                                    }
                                } else {
                                    val server = Config.afkServer.split(":")
                                    val port = if (server.size == 1) {
                                        6567
                                    } else {
                                        server[1].toInt()
                                    }
                                    Call.connect(it.player.con(), server[0], port)
                                }
                            }
                        } else {
                            it.afkTime = 0
                        }

                        it.exp = it.exp + ((random.nextInt(7) * it.expMultiplier).toInt())
                        Commands.Exp[it]

                        if (Config.expDisplay) {
                            val message = "${it.exp}/${floor(Commands.Exp.calculateFullTargetXp(it.level)).toInt()}"
                            Call.infoPopup(it.player.con(), message, Time.delta, Align.left, 0, 0, 300, 0)
                        }

                        if (it.hud != null) {
                            val array = JsonArray.readJSON(it.hud).asArray()

                            fun color(current : Float, max : Float) : String {
                                return when (current / max * 100.0) {
                                    in 50.0..100.0 -> "[green]"
                                    in 20.0..49.9 -> "[yellow]"
                                    else -> "[scarlet]"
                                }
                            }

                            fun shieldColor(current : Float, max : Float) : String {
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
                                            if(unit.shield > 0) {
                                                val shield = shieldColor(unit.health, unit.maxHealth)
                                                msg.appendLine("$shield${floor(unit.shield.toDouble())}")
                                            }
                                            msg.append("$color${floor(unit.health.toDouble())}")

                                            if (unit.team != it.player.team() && Permission.check(it.player, "hud.enemy")) {
                                                Call.label(it.player.con(), msg.toString(), Time.delta, unit.getX(), unit.getY())
                                            } else if (unit.team == it.player.team()) {
                                                Call.label(it.player.con(), msg.toString(), Time.delta, unit.getX(), unit.getY())
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (voting) {
                        if (Groups.player.find { a -> a.uuid() == voteStarter!!.uuid() } == null) {
                            send("command.vote.canceled.leave")
                            resetVote()
                        } else {
                            if (count % 10 == 0) {
                                if (isPvP) {
                                    Groups.player.forEach {
                                        if (it.team() == voteTeam) {
                                            val data = findPlayerData(it.uuid())
                                            if (data != null) {
                                                if (voteTargetUUID != data.uuid) {
                                                    val bundle = Bundle(data.languageTag)
                                                    it.sendMessage(bundle["command.vote.count", count.toString(), check() - voted.size])
                                                }
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
                                        val name = netServer.admins.getInfo(voteTargetUUID).lastName
                                        if (Groups.player.find { a -> a.uuid() == voteTargetUUID } == null) {
                                            netServer.admins.banPlayerID(voteTargetUUID)
                                            send("command.vote.kick.target.banned", name)
                                            Events.fire(PlayerVoteBanned(name, voteReason!!, onlinePlayers.toString()))
                                        } else {
                                            voteTarget?.kick(Packets.KickReason.kick, 60 * 60 * 3000)
                                            send("command.vote.kick.target.kicked", name)
                                            Events.fire(PlayerVoteKicked(name, voteReason!!, onlinePlayers.toString()))
                                        }
                                    }

                                    "map" -> {
                                        for (it in database.players) {
                                            earnEXP(state.rules.waveTeam, it.player, it, true)
                                        }
                                        back(voteMap)
                                    }

                                    "gg" -> {
                                        if (voteStarter != null && !Permission.check(voteStarter!!, "vote.pass")) voterCooltime.put(voteStarter!!.uuid(), 180)
                                        if (isPvP) {
                                            world.tiles.forEach {
                                                if (it.build != null && it.build.team != null && it.build.team == voteTeam) {
                                                    Call.setTile(it, Blocks.air, voteTeam, 0)
                                                }
                                            }
                                        } else {
                                            Events.fire(GameOverEvent(state.rules.waveTeam))
                                        }
                                    }

                                    "skip" -> {
                                        if (voteStarter != null) voterCooltime.put(voteStarter!!.uuid(), 180)
                                        for (a in 0..voteWave!!) {
                                            spawner.spawnEnemies()
                                            state.wave++
                                            state.wavetime = state.rules.waveSpacing
                                        }
                                        send("command.vote.skip.done", voteWave!!.toString())
                                    }

                                    "back" -> {
                                        back(null)
                                    }

                                    "random" -> {
                                        if (lastVoted.plusMinutes(10).isBefore(LocalTime.now())) {
                                            send("command.vote.random.cool")
                                        } else {
                                            if (voteStarter != null) voterCooltime.put(voteStarter!!.uuid(), 420)
                                            lastVoted = LocalTime.now()
                                            send("command.vote.random.done")
                                            Thread {
                                                val map : Map
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
                                                        logic.runWave()
                                                    }

                                                    1 -> {
                                                        send("command.vote.random.wave")
                                                        for (a in 0..5) logic.runWave()
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
                                                        Groups.player.forEach {
                                                            Call.worldDataBegin(it.con)
                                                            netServer.sendWorldData(it)
                                                        }
                                                    }

                                                    3 -> {
                                                        send("command.vote.random.fill.core")
                                                        if (voteStarter != null) {
                                                            content.items().forEach {
                                                                state.teams.cores(voteStarter!!.team()).first().items.add(it, Random(516).nextInt(500))
                                                            }
                                                        } else {
                                                            content.items().forEach {
                                                                state.teams.cores(Team.sharded).first().items.add(it, Random(516).nextInt(500))
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
                                                        for (x in 0 until world.width()) {
                                                            for (y in 0 until world.height()) {
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

                                resetVote()
                            } else if ((count == 0 && check() > voted.size) || isCanceled) {
                                if (isPvP) {
                                    database.players.forEach {
                                        if (it.player.team() == voteTeam) {
                                            Core.app.post { it.player.sendMessage(Bundle(it.languageTag)["command.vote.failed"]) }
                                        }
                                    }
                                } else {
                                    send("command.vote.failed")
                                }
                                resetVote()
                            }
                        }
                    }

                    if (Config.pvpPeace) {
                        if (pvpCount != 0) {
                            pvpCount--
                        } else {
                            state.rules.blockDamageMultiplier = orignalBlockMultiplier
                            state.rules.unitDamageMultiplier = orignalUnitMultiplier
                            send("event.pvp.peace.end")
                        }
                    }

                    if (unitLimitMessageCooldown > 0) {
                        unitLimitMessageCooldown = unitLimitMessageCooldown--
                    }

                    secondCount = 0
                } else {
                    secondCount++
                }

                if (minuteCount == 3600) {
                    if (state.rules.pvp) {
                        database.players.forEach {
                            if (!pvpPlayer.containsKey(it.uuid) && it.player.team() != Team.derelict) {
                                pvpPlayer.put(it.uuid, it.player.team())
                            }
                        }
                    }

                    Main.daemon.submit(Thread {
                        val data = database.getAll()

                        data.forEach {
                            if (it.banTime != null && LocalDateTime.now().isAfter(LocalDateTime.parse(it.banTime))) {
                                Core.app.post { netServer.admins.unbanPlayerID(it.uuid) }
                                it.banTime = null
                                database.update(it.uuid, it)
                                Events.fire(PlayerTempUnbanned(it.name))
                            }
                        }
                    })

                    if (rollbackCount == 0) {
                        SaveIO.save(saveDirectory.child("rollback.msav"))
                        rollbackCount = Config.rollbackTime
                    } else {
                        rollbackCount--
                    }

                    if (Config.message) {
                        if (messageCount == Config.messageTime) {
                            database.players.forEach {
                                val message = if (Main.root.child("messages/${it.languageTag}.txt").exists()) {
                                    Main.root.child("messages/${it.languageTag}.txt").readString()
                                } else if (Main.root.child("messages").list().isNotEmpty()) {
                                    val file = Main.root.child("messages/en.txt")
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

                    maxdps = 0f
                    minuteCount = 0
                } else {
                    minuteCount++
                }
            }
        })
    }

    fun log(type : LogType, text : String, vararg name : String) {
        val maxLogFile = 20
        val root : Fi = Core.settings.dataDirectory.child("mods/Essentials/")
        val time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH_mm_ss").format(LocalDateTime.now())

        if (type != LogType.Report) {
            val new = Paths.get(root.child("log/$type.log").path())
            val old = Paths.get(root.child("log/old/$type/$time.log").path())
            var main = root.child("log/$type.log")
            val folder = root.child("log")

            if (main != null && main.length() > 2048 * 256) {
                main.writeString("end of file. $time", true)
                try {
                    if (!root.child("log/old/$type").exists()) {
                        root.child("log/old/$type").mkdirs()
                    }
                    Files.move(new, old, StandardCopyOption.REPLACE_EXISTING)
                    val logFiles = root.child("log/old/$type").file().listFiles { file -> file.name.endsWith(".log") }

                    if (logFiles != null) {
                        if (logFiles.size >= maxLogFile) {
                            val zipFileName = "$time.zip"
                            val zipOutputStream = ZipOutputStream(FileOutputStream(zipFileName))

                            Thread {
                                for (logFile in logFiles) {
                                    val entryName = logFile.name
                                    val zipEntry = ZipEntry(entryName)
                                    zipOutputStream.putNextEntry(zipEntry)

                                    val fileInputStream = FileInputStream(logFile)
                                    val buffer = ByteArray(1024)
                                    var length : Int
                                    while (fileInputStream.read(buffer).also { length = it } > 0) {
                                        zipOutputStream.write(buffer, 0, length)
                                    }

                                    fileInputStream.close()
                                    zipOutputStream.closeEntry()
                                }

                                zipOutputStream.close()

                                logFiles.forEach {
                                    it.delete()
                                }

                                Files.move(Path(Core.files.external(zipFileName).absolutePath()), Path(root.child("log/old/$type/$zipFileName").absolutePath()))
                            }.start()
                        }
                    }
                } catch (e : Exception) {
                    e.printStackTrace()
                }
                main = null
            }
            if (main == null) main = folder.child("$type.log")
            main!!.writeString("[$time] $text\n", true)
        } else {
            val main = root.child("log/report/$time-${name[0]}.txt")
            main.writeString(text)
        }
    }

    enum class LogType {
        Player, Tap, WithDraw, Block, Deposit, Chat, Report
    }

    private class IpAddressMatcher(ipAddress : String) {
        private var nMaskBits = 0
        private val requiredAddress : InetAddress
        fun matches(address : String) : Boolean {
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

        private fun parseAddress(address : String) : InetAddress {
            return try {
                InetAddress.getByName(address)
            } catch (e : UnknownHostException) {
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

    private fun earnEXP(winner : Team, p : Playerc, target : DB.PlayerData, isConnected : Boolean) {
        val oldLevel = target.level
        val oldExp = target.exp
        val time = target.currentPlayTime.toInt()
        var blockexp = 0

        state.stats.placedBlockCount.forEach {
            blockexp += blockExp[it.key.name]
        }

        val bundle = Bundle(target.languageTag)
        var coreitem = 0
        state.stats.coreItemCount.forEach {
            coreitem += it.value
        }
        val erekirAttack = if (state.planet == Planets.erekir) state.stats.enemyUnitsDestroyed else 0
        val erekirPvP = if (state.planet == Planets.erekir) 5000 else 0

        if (winner == p.team()) {
            val score : Int = if (state.rules.attackMode) {
                (time + blockexp + enemyBuildingDestroyed + erekirAttack) - (state.stats.buildingsDeconstructed + state.stats.buildingsDestroyed)
            } else if (state.rules.pvp) {
                time + erekirPvP + 5000
            } else {
                0
            }

            target.exp = target.exp + ((score * target.expMultiplier).toInt())
            if (isConnected) p.sendMessage(bundle["event.exp.earn.victory", score])
        } else {
            val score : Int = if (state.rules.attackMode) {
                time - (state.stats.buildingsDeconstructed + state.stats.buildingsDestroyed)
            } else if (state.rules.waves) {
                state.wave * 150
            } else if (state.rules.pvp) {
                time + 5000
            } else {
                0
            }

            val message = if (state.rules.attackMode) {
                bundle["event.exp.earn.defeat", score, (time + blockexp + enemyBuildingDestroyed + erekirAttack) - state.stats.buildingsDeconstructed]
            } else if (state.rules.waves) {
                bundle["event.exp.earn.wave", score, state.wave]
            } else if (state.rules.pvp) {
                bundle["event.exp.earn.defeat", score, (time + 5000)]
            } else {
                ""
            }

            target.exp = target.exp + ((score * target.expMultiplier).toInt())
            if (isConnected) {
                p.sendMessage(message)

                if (score < 0) {
                    p.sendMessage(bundle["event.exp.lost.reason"])
                    p.sendMessage(bundle["event.exp.lost.result", time, blockexp, enemyBuildingDestroyed, state.stats.buildingsDeconstructed])
                }
            }
        }

        Commands.Exp[target]
        if (!isConnected) {
            if (target.oldUUID != null) {
                target.uuid = target.oldUUID!!
                target.oldUUID = null
                database.queue(target)
            }
        }
        if (isConnected) p.sendMessage(bundle["event.exp.current", target.exp, target.exp - oldExp, target.level, target.level - oldLevel])
    }

    fun findPlayerData(uuid : String) : DB.PlayerData? {
        return database.players.find { data -> (data.oldUUID != null && data.oldUUID == uuid) || data.uuid == uuid }
    }

    fun findPlayers(name : String) : Playerc? {
        if (name.toIntOrNull() != null) {
            database.players.forEach {
                if (it.entityid == name.toInt()) {
                    return it.player
                }
            }
            return Groups.player.find { p -> p.plainName().contains(name, true) }
        } else {
            return Groups.player.find { p -> p.plainName().contains(name, true) }
        }
    }

    fun findPlayersByName(name : String) : PlayerInfo? {
        return if (!netServer.admins.findByName(name).isEmpty) {
            netServer.admins.findByName(name).first()
        } else {
            null
        }
    }

    private fun resetVote() {
        voting = false
        voteType = null
        voteTarget = null
        voteTargetUUID = null
        voteReason = null
        voteMap = null
        voteWave = null
        voteStarter = null
        isCanceled = false
        isAdminVote = false
        isPvP = false
        voteTeam = state.rules.defaultTeam
        voted = Seq<String>()
        count = 60
    }

    private fun addLog(log : TileLog) {
        worldHistory.add(log)
    }

    class TileLog(val time : Long, val player : String, val action : String, val x : Short, val y : Short, val tile : String, val rotate : Int, val team : Team, val value : Any?)
}