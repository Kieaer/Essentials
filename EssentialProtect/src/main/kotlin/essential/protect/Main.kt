package essential.protect

import arc.Events
import arc.util.CommandHandler
import arc.util.Log
import essential.bundle.Bundle
import essential.config.Config
import essential.core.Main
import essential.core.generated.registerGeneratedServerCommands
import essential.database.data.PlayerData
import essential.protect.generated.registerGeneratedClientCommands
import essential.util.findPlayerData
import mindustry.Vars.netServer
import mindustry.mod.Plugin
import java.util.Objects.requireNonNull

class Main : Plugin() {
    override fun init() {
        bundle.prefix = "[EssentialProtect]"

        Log.debug(bundle["event.plugin.starting"])

        val config = Config.load("config_protect.yaml", ProtectConfig.serializer(), true, ProtectConfig())
        require(config != null) {
            Log.err(bundle["event.plugin.load.failed"])
            return
        }

        conf = config

        netServer.admins.addActionFilter({ action ->
            if (action.player == null) return@addActionFilter true
            val data: PlayerData? = findPlayerData(action.player.uuid())
            if (data != null) {
                // 계정 기능이 켜져있는 경우
                if (conf.account.enabled) {
                    // Discord 인증을 사용할 경우
                    if (requireNonNull<ProtectConfig.AuthType>(conf.account.getAuthType()) == ProtectConfig.AuthType.Discord) {
                        // 계정에 Discord 인증이 안되어 있는 경우
                        if (data.discordID == null) {
                            action.player.sendMessage(Bundle(action.player.locale).get("event.discord.not.registered"))
                            return@addActionFilter false
                        } else {
                            return@addActionFilter true
                        }
                    } else {
                        return@addActionFilter true
                    }
                }
                return@addActionFilter true
            } else {
                return@addActionFilter false
            }
        })

        // 계정 설정 유무에 따라 기본 권한 변경
        if (conf.account.getAuthType() != ProtectConfig.AuthType.None) {
            Permission.INSTANCE.setDefault("user")
        } else {
            Permission.INSTANCE.setDefault("visitor")
        }

        // VPN 확인
        if (conf!!.rules.vpn) {
            var isUpdate = false
            if (Main.pluginData.get("vpnListDate") == null || java.util.Objects.requireNonNull<T?>(
                    Main.pluginData.get("vpnListDate")
                ).toLong() + 8.64e7 < java.lang.System.currentTimeMillis()
            ) {
                essential.core.Main.pluginData.getStatus()
                    .add(kotlin.Pair<A?, B?>("vpnListDate", java.lang.System.currentTimeMillis().toString()))
                isUpdate = true
            }

            if (isUpdate) {
                Http.get("https://raw.githubusercontent.com/X4BNet/lists_vpn/main/output/datacenter/ipv4.txt")
                    .error({ e -> Log.err("Failed to get vpn list!") })
                    .block({ e ->
                        val text = kotlin.text.String(java.io.BufferedInputStream(e.getResultAsStream()).readAllBytes())
                        root.child("data/ipv4.txt").writeString(text)
                    })
            }

            val file: kotlin.String = root.child("data/ipv4.txt").readString()
            pluginData.vpnList =
                file.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        }

        // 이벤트 설정
        val event = ProtectEvent()
        event.start()

        Log.debug(bundle.get("event.plugin.loaded"))

        Events.on(WorldLoadEndEvent::class.java, { e -> setNetworkFilter() })
    }

    fun setNetworkFilter() {
        try {
            val inner: java.lang.Class<*> = (Vars.platform.getNet() as ArcNetProvider).getClass()
            val field = inner.getDeclaredField("server")
            field.setAccessible(true)

            val serverInstance = field.get(Vars.platform.getNet())

            val innerClass: java.lang.Class<*> = field.get(Vars.platform.getNet()).javaClass
            val method = innerClass.getMethod("setDiscoveryHandler", ServerDiscoveryHandler::class.java)

            val handler: ServerDiscoveryHandler = object : ServerDiscoveryHandler() {
                @kotlin.Throws(java.io.IOException::class)
                public override fun onDiscoverReceived(
                    inetAddress: java.net.InetAddress,
                    reponseHandler: ReponseHandler
                ) {
                    if (!Vars.netServer.admins.isIPBanned(inetAddress.getHostAddress())) {
                        val buffer: java.nio.ByteBuffer = NetworkIO.writeServerData()
                        buffer.position(0)
                        reponseHandler.respond(buffer)
                    } else {
                        reponseHandler.respond(java.nio.ByteBuffer.allocate(0))
                    }
                }
            }

            method.invoke(serverInstance, handler)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        val filter: ServerConnectFilter = ServerConnectFilter { s -> !Vars.netServer.admins.bannedIPs.contains(s) }
        Vars.platform.getNet().setConnectFilter(filter)
    }

    public override fun registerServerCommands(handler: CommandHandler) {
        registerGeneratedServerCommands(handler)
    }

    public override fun registerClientCommands(handler: CommandHandler) {
        registerGeneratedClientCommands(handler)
    }

    companion object {
        var bundle: Bundle = Bundle()
        lateinit var conf: ProtectConfig
        var pluginData: PluginData = PluginData()
    }
}
