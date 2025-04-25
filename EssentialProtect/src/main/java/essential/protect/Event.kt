package essential.protect

import arc.Core

class Event {
    var pvpCount: kotlin.Int = 0
    var originalBlockMultiplier: kotlin.Float? = 0f
    var originalUnitMultiplier: kotlin.Float? = 0f
    var coldData: kotlin.Array<kotlin.String?>

    fun start() {
        Events.on(WorldLoadEndEvent::class.java, { e ->
            if (essential.protect.Main.Companion.conf.pvp.peace.enabled) {
                originalBlockMultiplier = Vars.state.rules.blockDamageMultiplier
                originalUnitMultiplier = Vars.state.rules.unitDamageMultiplier
                Vars.state.rules.blockDamageMultiplier = 0f
                Vars.state.rules.unitDamageMultiplier = 0f
                pvpCount = essential.protect.Main.Companion.conf.pvp.peace.time
            }
        })

        Events.run(EventType.Trigger.update, {
            if (essential.protect.Main.Companion.conf.pvp.peace.enabled) {
                if (pvpCount != 0) {
                    pvpCount--
                } else {
                    Vars.state.rules.blockDamageMultiplier = originalBlockMultiplier
                    Vars.state.rules.unitDamageMultiplier = originalUnitMultiplier
                    for (data in database.getPlayers()) {
                        data.send("event.pvp.peace.end")
                    }
                }
            }
            if (essential.protect.Main.Companion.conf.pvp.border.enabled) {
                Groups.unit.forEach({ unit ->
                    if (unit.x < 0 || unit.y < 0 || unit.x > (Vars.world.width() * 8) || unit.y > (Vars.world.height() * 8)) {
                        unit.kill()
                    }
                })
            }
            if (essential.protect.Main.Companion.conf.protect.unbreakableCore) {
                Vars.state.teams.active.forEach({ t -> t.cores.forEach({ c -> c.health(1.0E8f) }) })
            }
        })

        Events.on(ConfigEvent::class.java, { e ->
            if (essential.protect.Main.Companion.conf.protect.powerDetect && e.value is kotlin.Int) {
                val entity: Building = e.tile
                val other: Tile? = Vars.world.tile(e.value as kotlin.Int)
                val valid =
                    other != null && entity.power != null && other.block().hasPower && other.block().outputsPayload && other.block() !== Blocks.massDriver && other.block() === Blocks.payloadMassDriver && other.block() === Blocks.largePayloadMassDriver
                if (valid) {
                    val oldGraph: PowerGraph = entity.power.graph
                    val newGraph: PowerGraph = other.build.power.graph
                    val oldGraphCount: kotlin.Int = java.util.Arrays.stream(
                        oldGraph.toString()
                            .substring(oldGraph.toString().indexOf("all=["), oldGraph.toString().indexOf("], graph"))
                            .replaceFirst("all=\\[", "").split(",")
                    ).toArray().length
                    val newGraphCount: kotlin.Int = java.util.Arrays.stream(
                        newGraph.toString()
                            .substring(newGraph.toString().indexOf("all=["), newGraph.toString().indexOf("], graph"))
                            .replaceFirst("all=\\[", "").split(",")
                    ).toArray().length
                    if (kotlin.math.abs(oldGraphCount - newGraphCount) > 10) {
                        database.getPlayers().forEach({ a ->
                            a.send(
                                "event.antigrief.node",
                                e.player.name,
                                kotlin.math.max(oldGraphCount, newGraphCount),
                                kotlin.math.min(oldGraphCount, newGraphCount),
                                e.tile.x + ", " + e.tile.y
                            )
                        }
                        )
                    }
                }
            }
        })

        Events.on(PlayerJoin::class.java, { e ->
            val trigger: Trigger = Trigger()
            e.player.admin(false)

            val data: PlayerData? = database.get(e.player.uuid())
            if (essential.protect.Main.Companion.conf.account.getAuthType() == AuthType.None || !essential.protect.Main.Companion.conf.account.enabled) {
                if (data == null) {
                    daemon.submit({
                        if (!trigger.checkUserNameExists(e.player.name)) {
                            Core.app.post({ trigger.createPlayer(e.player, null, null) })
                        } else {
                            Core.app.post({
                                e.player.con.kick(
                                    Bundle(e.player.locale).get("event.player.name.duplicate"),
                                    0L
                                )
                            })
                        }
                    })
                } else {
                    trigger.loadPlayer(e.player, data, false)
                }
            } else {
                if (essential.protect.Main.Companion.conf.account.getAuthType() == AuthType.Discord) {
                    if (data == null) {
                        daemon.submit({
                            if (trigger.checkUserNameExists(e.player.name)) {
                                Core.app.post({
                                    e.player.con.kick(
                                        Bundle(e.player.locale).get("event.player.name.duplicate"),
                                        0L
                                    )
                                })
                            } else {
                                Core.app.post({ trigger.createPlayer(e.player, null, null) })
                            }
                        })
                    } else {
                        trigger.loadPlayer(e.player, data, false)
                    }
                } else {
                    e.player.sendMessage(Bundle(e.player.locale).get("event.player.first.register"))
                }
            }
        })

        Events.on(BlockDestroyEvent::class.java, { e ->
            if (Vars.state.rules.pvp && essential.protect.Main.Companion.conf.pvp.destroyCore && Vars.state.rules.coreCapture) {
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
        })

        Events.on(PlayerDataLoaded::class.java, { e ->
            if (essential.protect.Main.Companion.conf.rules.strict) {
                Groups.player.find({ p -> p.uuid() == e.getPlayerData().getUuid() }).name(e.getPlayerData().getName())
            }
        })

        Events.on(ServerLoadEvent::class.java, { e ->
            if (Vars.mods.getMod("essential-discord") == null) {
                Log.warn(Bundle().get("command.reg.plugin-enough"))
            }
        })

        if (essential.protect.Main.Companion.conf.rules.blockNewUser) {
            enableBlockNewUser()
        }

        Events.on(ConnectPacketEvent::class.java, { event ->
            var kickReason = ""
            if (!essential.protect.Main.Companion.conf.rules.mobile && event.connection.mobile) {
                event.connection.kick(Bundle(event.packet.locale).get("event.player.not.allow.mobile"), 0L)
                kickReason = "mobile"
            } else if (essential.protect.Main.Companion.conf.rules.minimalName.enabled && essential.protect.Main.Companion.conf.rules.minimalName.length > event.packet.name.length()) {
                event.connection.kick(Bundle(event.packet.locale).get("event.player.name.short"), 0L)
                kickReason = "name.short"
            } else if (essential.protect.Main.Companion.conf.rules.vpn) {
                for (ip in essential.protect.Main.Companion.pluginData.vpnList) {
                    val match = IpAddressMatcher(ip)
                    if (match.matches(event.connection.address)) {
                        event.connection.kick(Bundle(event.packet.locale).get("anti-grief.vpn"))
                        kickReason = "vpn"
                        break
                    }
                }
            } else if (essential.protect.Main.Companion.conf.rules.blockNewUser && !java.util.Arrays.asList<kotlin.String?>(
                    *coldData
                ).contains(event.packet.uuid)
            ) {
                event.connection.kick(Bundle(event.packet.locale).get("event.player.new.blocked"), 0L)
                kickReason = "newuser"
            } else if (database.isBanned(Vars.netServer.admins.getInfo(event.connection.uuid))) {
                event.connection.kick(Packets.KickReason.banned)
                kickReason = "banned"
            }
            if (!kickReason.isEmpty()) {
                val bundle: Bundle = Bundle()
                log(
                    essential.core.Event.LogType.Player,
                    bundle.get(
                        "event.player.kick",
                        event.packet.name,
                        event.packet.uuid,
                        event.connection.address,
                        bundle.get("event.player.kick.reason." + kickReason)
                    )
                )
                Events.fire(
                    PlayerConnectKicked(
                        event.packet.name,
                        bundle.get("event.player.kick.reason." + kickReason)
                    )
                )
            }
        })

        Events.on(ConfigFileModified::class.java, { e ->
            if (e.getKind() === java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY) {
                if (e.getPaths().equals("config_protect.yaml")) {
                    essential.protect.Main.Companion.conf = essential.core.Main.Companion.createAndReadConfig(
                        "config_protect.yaml",
                        java.util.Objects.requireNonNull<T?>(this.javaClass.getResourceAsStream("/config_protect.yaml")),
                        essential.protect.Config::class.java
                    )
                    Log.info(Bundle().get("config.reloaded"))
                }
            }
        })
    }

    fun loadPlayer(data: PlayerData) {
        if (essential.protect.Main.Companion.conf.account.getAuthType() == AuthType.Discord && data.getDiscord() == null) {
            data.getPlayer().sendMessage(Bundle(data.getLanguageTag()).get("event.discord.not.registered"))
        }
    }

    fun enableBlockNewUser() {
        val list: java.util.ArrayList<PlayerData> = database.getAll()
        coldData = kotlin.arrayOfNulls<kotlin.String>(list.size)

        var size = 0
        for (playerData in list) {
            coldData[size++] = playerData.getUuid()
        }
    }

    internal inner class IpAddressMatcher(ipAddress: kotlin.String) {
        private var nMaskBits = 0
        private val requiredAddress: java.net.InetAddress

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
            kotlin.require(requiredAddress.getAddress().size * 8 >= nMaskBits) {
                kotlin.String.format(
                    "IP address %s is too short for bitmask of length %d",
                    address,
                    nMaskBits
                )
            }
        }

        fun matches(address: kotlin.String?): kotlin.Boolean {
            val remoteAddress = parseAddress(address)
            if (requiredAddress.javaClass != remoteAddress.javaClass) {
                return false
            }
            if (nMaskBits < 0) {
                return remoteAddress == requiredAddress
            }
            val remAddr = remoteAddress.getAddress()
            val reqAddr = requiredAddress.getAddress()
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

        private fun parseAddress(address: kotlin.String?): java.net.InetAddress {
            try {
                return java.net.InetAddress.getByName(address)
            } catch (e: java.net.UnknownHostException) {
                throw java.lang.IllegalArgumentException("Failed to parse address " + address, e)
            }
        }
    }
}
