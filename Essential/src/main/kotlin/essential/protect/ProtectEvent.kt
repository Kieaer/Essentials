package essential.protect

import arc.Events
import arc.net.Server
import arc.net.ServerDiscoveryHandler
import arc.util.Log
import arc.util.Timer
import essential.common.bundle.Bundle
import essential.common.config.Config
import essential.common.database.data.*
import essential.common.database.table.PlayerTable
import essential.common.event.CustomEvents
import essential.common.log.LogType
import essential.common.log.writeLog
import essential.common.players
import essential.core.Main.Companion.scope
import essential.protect.ProtectService.Companion.conf
import essential.protect.ProtectService.Companion.pluginData
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import ksp.event.Event
import mindustry.Vars
import mindustry.content.Blocks
import mindustry.content.Fx
import mindustry.entities.Damage
import mindustry.game.EventType
import mindustry.gen.Building
import mindustry.gen.Groups
import mindustry.net.ArcNetProvider
import mindustry.net.NetworkIO
import mindustry.net.Packets
import mindustry.world.Tile
import mindustry.world.blocks.power.PowerGraph
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.nio.file.StandardWatchEventKinds
import kotlin.math.max
import kotlin.math.min

internal var pvpCount: Int = 0
internal var originalBlockMultiplier: Float = 0f
internal var originalUnitMultiplier: Float = 0f
internal var coldData: Array<String> = arrayOf()

@Event
fun worldLoadEnd(event: EventType.WorldLoadEndEvent) {
    try {
        val inner: Class<*> = (Vars.platform.net as ArcNetProvider).javaClass
        val field = inner.getDeclaredField("server")
        field.setAccessible(true)

        val serverInstance = field[Vars.platform.net]

        val innerClass: Class<*> = field[Vars.platform.net].javaClass
        val method = innerClass.getMethod("setDiscoveryHandler", ServerDiscoveryHandler::class.java)

        val handler = ServerDiscoveryHandler { inetAddress, responseHandler ->
            if (!Vars.netServer.admins.isIPBanned(inetAddress.hostAddress)) {
                val buffer: ByteBuffer = NetworkIO.writeServerData()
                buffer.position(0)
                responseHandler.respond(buffer)
            } else {
                responseHandler.respond(ByteBuffer.allocate(0))
            }
        }

        method.invoke(serverInstance, handler)
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }

    val filter: Server.ServerConnectFilter =
        Server.ServerConnectFilter { s -> !Vars.netServer.admins.bannedIPs.contains(s) }
    Vars.platform.net.connectFilter = filter

    if (conf.pvp.peace.enabled) {
        originalBlockMultiplier = Vars.state.rules.blockDamageMultiplier
        originalUnitMultiplier = Vars.state.rules.unitDamageMultiplier
        Vars.state.rules.blockDamageMultiplier = 0f
        Vars.state.rules.unitDamageMultiplier = 0f
        pvpCount = conf.pvp.peace.time
    }
}

@Event
fun runEverySecond() {
    Timer.schedule({
        if (conf.pvp.peace.enabled && Vars.state.rules.pvp && Vars.state.isPlaying) {
            if (pvpCount > 0) {
                pvpCount--
            } else if (pvpCount == 0) {
                Vars.state.rules.blockDamageMultiplier = originalBlockMultiplier
                Vars.state.rules.unitDamageMultiplier = originalUnitMultiplier
                players.forEach {
                    it.send("event.pvp.peace.end")
                }
            }
        }
    }, 0f, 1f)
}

@Event
fun update() {
    if (conf.pvp.border.enabled) {
        Groups.unit.forEach { unit ->
            if (unit.x < 0 || unit.y < 0 || unit.x > (Vars.world.width() * 8) || unit.y > (Vars.world.height() * 8)) {
                unit.kill()
            }
        }
    }
    if (conf.protect.unbreakableCore) {
        Vars.state.teams.active.forEach { t -> t.cores.forEach { c -> c.health(1.0E8f) } }
    }
}

@Event
fun config(e: EventType.ConfigEvent) {
    if (conf.protect.powerDetect && e.value is Int) {
        val entity: Building = e.tile
        val other: Tile? = Vars.world.tile(e.value as Int)
        val valid =
            other != null && entity.power != null && other.block().hasPower && other.block().outputsPayload && other.block() !== Blocks.massDriver && other.block() === Blocks.payloadMassDriver && other.block() === Blocks.largePayloadMassDriver
        if (valid) {
            val oldGraph: PowerGraph = entity.power.graph
            val newGraph: PowerGraph = other.build.power.graph
            val oldGraphCount: Int = 0
            val newGraphCount: Int = 0

            players.forEach { a ->
                a.send(
                    "event.antiGrief.node",
                    e.player.name,
                    max(oldGraphCount, newGraphCount),
                    min(oldGraphCount, newGraphCount),
                    "${e.tile.x},${e.tile.y}"
                )
            }
        }
    }
}

@Event
fun playerJoin(e: EventType.PlayerJoin) {
    e.player.admin(false)
    scope.launch {
        val data: PlayerData? = getPlayerData(e.player.uuid())
        if (conf.account.getAuthType() == ProtectConfig.AuthType.None || !conf.account.enabled) {
            if (data == null) {
                if (suspendTransaction {
                        PlayerTable
                            .select(PlayerTable.name)
                            .where { PlayerTable.name eq e.player.plainName() }
                            .empty()
                    }) {

                    val data = createPlayerData(e.player)
                    data.permission = "user"
                    data.update()
                    Events.fire(CustomEvents.PlayerDataLoad(data))
                } else {
                    e.player.con.kick(
                        Bundle(e.player.locale)["event.player.name.duplicate"],
                        0L
                    )
                }
            } else {
                Events.fire(CustomEvents.PlayerDataLoad(data))
            }
        } else if (conf.account.getAuthType() == ProtectConfig.AuthType.Discord) {
            if (data == null) {
                if (suspendTransaction() {
                        PlayerTable
                            .select(PlayerTable.name)
                            .where { PlayerTable.name eq e.player.plainName() }
                            .empty()
                    }) {
                    //data.send("event.discord.not.registered")
                    // TODO discord 로그인 추가
                } else {
                    e.player.con.kick(
                        Bundle(e.player.locale)["event.player.name.duplicate"],
                        0L
                    )
                }
            } else {
                Events.fire(CustomEvents.PlayerDataLoad(data))
            }
        } else {
            e.player.sendMessage(Bundle(e.player.locale)["event.player.first.register"])
        }
    }
}

@Event
fun blockDestroy(e: EventType.BlockDestroyEvent) {
    if (Vars.state.rules.pvp && conf.pvp.destroyCore && Vars.state.rules.coreCapture) {
        Fx.spawnShockwave.at(e.tile.getX(), e.tile.getY(), Vars.state.rules.dropZoneRadius)
        Damage.damage(
            Vars.world.tile(e.tile.pos()).team(),
            e.tile.getX(),
            e.tile.getY(),
            Vars.state.rules.dropZoneRadius,
            1.0E8f,
            true
        )
    }
}

@Event
fun playerDataLoaded(e: CustomEvents.PlayerDataLoadEnd) {
    if (conf.rules.strict) {
        Groups.player.find { p -> p.uuid() == e.playerData.uuid }.name(e.playerData.name)
    }
}

@Event
fun connectPacket(event: EventType.ConnectPacketEvent) {
    var kickReason = ""
    if (!conf.rules.mobile && event.connection.mobile) {
        event.connection.kick(Bundle(event.packet.locale)["event.player.not.allow.mobile"], 0L)
        kickReason = "mobile"
    } else if (conf.rules.minimalName.enabled && conf.rules.minimalName.length > event.packet.name.length) {
        event.connection.kick(Bundle(event.packet.locale)["event.player.name.short"], 0L)
        kickReason = "name.short"
    } else if (conf.rules.vpn) {
        for (ip in pluginData.vpnList) {
            val match = IpAddressMatcher(ip)
            if (match.matches(event.connection.address)) {
                event.connection.kick(Bundle(event.packet.locale)["anti-grief.vpn"])
                kickReason = "vpn"
                break
            }
        }
    } else if (conf.rules.blockNewUser && !listOf<String?>(*coldData).contains(event.packet.uuid)) {
        event.connection.kick(Bundle(event.packet.locale)["event.player.new.blocked"], 0L)
        kickReason = "newuser"
    } else {
        scope.launch {
            try {
                if (checkPlayerBanned(event.packet.name, event.packet.uuid, event.connection.address)) {
                    event.connection.kick(Packets.KickReason.banned)
                    kickReason = "banned"
                }
            } catch (e: Exception) {
                Log.err("Failed to check if player is banned", e)
            }
        }
    }

    if (!kickReason.isEmpty()) {
        val bundle = Bundle()
        writeLog(
            LogType.Player,
            bundle["event.player.kick", event.packet.name, event.packet.uuid, event.connection.address, bundle["event.player.kick.reason.$kickReason"]]
        )
        Events.fire(
            CustomEvents.PlayerConnectKicked(
                event.packet.name,
                bundle["event.player.kick.reason.$kickReason"]
            )
        )
    }
}

@Event
fun configFileModified(e: CustomEvents.ConfigFileModified) {
    if (e.kind === StandardWatchEventKinds.ENTRY_MODIFY) {
        if (e.paths == "config_protect.yaml") {
            val config = Config.load("config_protect", ProtectConfig.serializer(), ProtectConfig())
            require(config != null) {
                Log.err(Bundle()["config.invalid.path"])
                return
            }

            conf = config
            Log.info(Bundle()["config.reloaded"])
        }
    }
}

fun start() {
    if (conf.rules.blockNewUser) {
        enableBlockNewUser()
    }
}

fun enableBlockNewUser() {
    scope.launch {
        try {
            suspendTransaction {
                val list = PlayerTable.select(PlayerTable.uuid).toList()

                var size = 0
                for (playerData in list) {
                    coldData[size++] = playerData[PlayerTable.uuid]
                }
            }
        } catch (e: Exception) {
            Log.err("Failed to load player UUIDs for new user blocking", e)
            coldData = arrayOf()
        }
    }
}

internal class IpAddressMatcher(ipAddress: String) {
    private var nMaskBits = 0
    private val requiredAddress: InetAddress

    init {
        var address = ipAddress
        if (address.indexOf('/') > 0) {
            val addressAndMask = address.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            address = addressAndMask[0]
            nMaskBits = addressAndMask[1].toInt()
        } else {
            nMaskBits = -1
        }
        requiredAddress = parseAddress(address)
        require(requiredAddress.address.size * 8 >= nMaskBits) {
            String.format(
                "IP address %s is too short for bitmask of length %d",
                address,
                nMaskBits
            )
        }
    }

    fun matches(address: String?): Boolean {
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
        for (i in 0..<nMaskFullBytes) {
            if (remAddr[i] != reqAddr[i]) {
                return false
            }
        }
        if (finalByte.toInt() != 0) {
            return (remAddr[nMaskFullBytes].toInt() and finalByte.toInt()) == (reqAddr[nMaskFullBytes].toInt() and finalByte.toInt())
        } else {
            return true
        }
    }

    private fun parseAddress(address: String?): InetAddress {
        try {
            return InetAddress.getByName(address)
        } catch (e: UnknownHostException) {
            throw IllegalArgumentException("Failed to parse address $address", e)
        }
    }
}
