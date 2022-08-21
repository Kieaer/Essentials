package remake

import arc.Core
import arc.Events
import arc.files.Fi
import arc.struct.ArrayMap
import arc.util.Log
import mindustry.Vars
import mindustry.Vars.netServer
import mindustry.content.Blocks
import mindustry.game.EventType
import mindustry.game.EventType.*
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Playerc
import org.hjson.JsonObject
import remake.Main.Companion.database
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import kotlin.experimental.and
import kotlin.math.abs

object Event {
    val file = JsonObject.readHjson(Main::class.java.classLoader.getResourceAsStream("exp.hjson").reader()).asObject()
    var order = 0
    val players = ArrayMap<Playerc, Int>()

    fun register() {
        Events.on(PlayerChatEvent::class.java) {
            if (!it.message.startsWith("/")) {
                log(LogType.Chat, "${it.player.name}: ${it.message}")
                Log.info("<&y" + it.player.name + ": &lm" + it.message + "&lg>")

                // todo 채팅 포맷 변경
                Call.sendMessage(Permission[it.player].chatFormat.replace("%1", it.player.coloredName()).replace("%2", it.message))

                if (database.players.find { e -> e.uuid == it.player.uuid() } != null && Trigger.voting && it.message.equals("y", true) && !Trigger.voted.contains(it.player.uuid())){
                    Trigger.voted.add(it.player.uuid())
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
            if (it.tile != null && it.tile.block() != null && it.player != null && it.value is Int) { // Source by BasedUser(router)
                val entity = it.tile
                val other = Vars.world.tile(it.value as Int)
                val valid = other != null && entity.power != null && other.block().hasPower
                if (valid) {
                    val oldGraph = entity.power.graph
                    val newGraph = other.build.power.graph
                    val oldGraphCount = oldGraph.toString()
                        .substring(oldGraph.toString().indexOf("all=["), oldGraph.toString().indexOf("], l"))
                        .replaceFirst("all=\\[".toRegex(), "").split(",").toTypedArray().size
                    val newGraphCount = newGraph.toString()
                        .substring(newGraph.toString().indexOf("all=["), newGraph.toString().indexOf("], l"))
                        .replaceFirst("all=\\[".toRegex(), "").split(",").toTypedArray().size
                    if (abs(oldGraphCount - newGraphCount) > 10) {
                        Call.sendMessage("${it.player.name} [white]player has [scarlet]unlinked[] the [yellow]power node[]. Number of connected buildings: [green] ${oldGraphCount.coerceAtLeast(newGraphCount)} [cyan]->[scarlet] ${oldGraphCount.coerceAtMost(newGraphCount)} [white](" + it.tile.x + ", " + it.tile.y + ")")
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
            if (Vars.state.rules.pvp) {
                var index = 5
                for (a in 0..4) {
                    if (Vars.state.teams[Team.all[index]].cores.isEmpty) {
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
            } else if (Vars.state.rules.attackMode) {
                for (p in Groups.player) {
                    val target = findPlayerData(p.uuid())
                    if (target != null) target.attackclear++
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
                    val name = it.tile.block().name
                    if (!it.breaking) {
                        log(LogType.Block, "${player.name} placed ${it.tile.block().name}")
                        val exp = PluginData.expData.getInt(name, 0)
                        target.placecount++
                        target.exp = target.exp + exp

                        if (isDebug) {
                            Log.info("${player.name} placed ${it.tile.block().name} to ${it.tile.x},${it.tile.y}")
                        }
                    } else if (it.breaking) {
                        log(LogType.Block, "${player.name} break ${player.unit().buildPlan().block.name}")
                        val exp = PluginData.expData.getInt(player.unit().buildPlan().block.name, 0)
                        target.breakcount++
                        target.exp = target.exp + exp

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

        }

        Events.on(UnitDestroyEvent::class.java) {

        }

        Events.on(UnitCreateEvent::class.java) {

        }

        Events.on(UnitChangeEvent::class.java) {

        }

        Events.on(PlayerJoin::class.java) {
            players.put(it.player, order)
            log(LogType.Player, "${it.player.plainName()}(${it.player.uuid()}, ${it.player.con.address} joined.")
            it.player.admin(false)

            if (Config.authType == Config.AuthType.None) {
                val data = database[it.player.uuid()]
                if (data != null) {
                    Trigger.loadPlayer(it.player, data)
                } else if (Config.authType == Config.AuthType.Password){
                    it.player.sendMessage("[green]To play the server, use the [scarlet]/reg[] command to register account.")
                } else {
                    Trigger.createPlayer(it.player, null, null)
                }
            }
        }

        Events.on(PlayerLeave::class.java) {
            players.removeKey(it.player)
            log(LogType.Player, "${it.player.plainName()}(${it.player.uuid()}, ${it.player.con.address} disconnected.")
            val data = database.players.find { data -> data.uuid == it.player.uuid() }
            if (data != null) {
                database.update(it.player.uuid(), data)
            } else {
                Log.info("Data is null!")
            }
            Log.info(data.permission)
            database.players.remove(data)
        }

        Events.on(PlayerBanEvent::class.java) {

        }

        Events.on(WorldLoadEvent::class.java) {
            PluginData.playtime = 0L
        }

        Events.on(PlayerConnect::class.java) { e ->
            log(LogType.Player, "${e.player.plainName()}(${e.player.uuid()}, ${e.player.con.address} connected.")

            val data = database[e.player.uuid()]
            if (data != null){
                if (data.status.containsKey("ban")){
                    if (LocalDateTime.now().isAfter(LocalDateTime.parse(data.status.get("ban")))){
                        netServer.admins.unbanPlayerID(e.player.uuid())
                    }
                }
            }

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

            if (Config.antiVPN) {
                val br = BufferedReader(InputStreamReader(javaClass.getResourceAsStream("/ipv4.txt")))
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
        }

        Events.on(MenuOptionChooseEvent::class.java) {

        }

        Events.run(EventType.Trigger.impactPower) {

        }
    }

    fun log(type: LogType, text: String) {
        val root: Fi = Core.settings.dataDirectory.child("mods/Essentials/")

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
        main!!.writeString("[${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))}] $text\n", true)
    }

    enum class LogType {
        Log, Warn, Error, Debug, Server, ServerWarn, ServerError, Client, ClientWarn, ClientError, Config, Player, PlayerWarn, PlayerError, Tap, WithDraw, Block, Deposit, Chat, Griefer, Web
    }

    class IpAddressMatcher(ipAddress: String) {
        private var nMaskBits = 0
        private val requiredAddress: InetAddress
        fun matches(address: String): Boolean {
            val remoteAddress = parseAddress(address)
            if(requiredAddress.javaClass != remoteAddress.javaClass) {
                return false
            }
            if(nMaskBits < 0) {
                return remoteAddress == requiredAddress
            }
            val remAddr = remoteAddress.address
            val reqAddr = requiredAddress.address
            val nMaskFullBytes = nMaskBits / 8
            val finalByte = (0xFF00 shr (nMaskBits and 0x07)).toByte()
            for(i in 0 until nMaskFullBytes) {
                if(remAddr[i] != reqAddr[i]) {
                    return false
                }
            }
            return if(finalByte.toInt() != 0) {
                remAddr[nMaskFullBytes] and finalByte == reqAddr[nMaskFullBytes] and finalByte
            } else true
        }

        private fun parseAddress(address: String): InetAddress {
            return try {
                InetAddress.getByName(address)
            } catch(e: UnknownHostException) {
                throw IllegalArgumentException("Failed to parse address$address", e)
            }
        }

        /**
         * Takes a specific IP address or a range specified using the IP/Netmask (e.g.
         * 192.168.1.0/24 or 202.24.0.0/14).
         *
         * @param ipAddress the address or range of addresses from which the request must
         * come.
         */
        init {
            var address = ipAddress
            if(address.indexOf('/') > 0) {
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

    fun findPlayerData(uuid: String) : DB.PlayerData?{
        return database.players.find { e -> e.uuid == uuid }
    }

    fun findPlayers(any: Any) : Playerc? {
        return if(any.toString().toIntOrNull() == null) {
            Groups.player.find { e -> e.name.contains(any.toString(), true) }
        } else {
            players.getKeyAt(any.toString().toInt())
        }
    }
}