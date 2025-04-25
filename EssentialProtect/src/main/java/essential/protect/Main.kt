package essential.protect

import arc.Events

class Main : Plugin() {
    public override fun init() {
        essential.protect.Main.Companion.bundle.setPrefix("[EssentialProtect]")

        Log.debug(essential.protect.Main.Companion.bundle.get("event.plugin.starting"))

        essential.protect.Main.Companion.conf = essential.core.Main.Companion.createAndReadConfig(
            "config_protect.yaml",
            java.util.Objects.requireNonNull<T?>(this.javaClass.getResourceAsStream("/config_protect.yaml")),
            essential.protect.Config::class.java
        )

        netServer.admins.addActionFilter({ action ->
            if (action.player == null) return@addActionFilter true
            val data: PlayerData? = findPlayerByUuid(action.player.uuid())
            if (data != null) {
                if (action.type === Administration.ActionType.commandUnits) {
                    data.setCurrentControlCount(data.getCurrentControlCount() + 1)
                }
                // 계정 기능이 켜져있는 경우
                if (essential.protect.Main.Companion.conf!!.account.enabled) {
                    // Discord 인증을 사용할 경우
                    if (java.util.Objects.requireNonNull<AuthType?>(essential.protect.Main.Companion.conf!!.account.getAuthType()) == AuthType.Discord) {
                        // 계정에 Discord 인증이 안되어 있는 경우
                        if (data.getDiscord() == null) {
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
        if (essential.protect.Main.Companion.conf!!.account.getAuthType() != AuthType.None) {
            Permission.INSTANCE.setDefault("user")
        } else {
            Permission.INSTANCE.setDefault("visitor")
        }

        // VPN 확인
        if (essential.protect.Main.Companion.conf!!.rules.vpn) {
            var isUpdate = false
            if (essential.core.Main.pluginData.get("vpnListDate") == null || java.util.Objects.requireNonNull<T?>(
                    essential.core.Main.pluginData.get("vpnListDate")
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
            essential.protect.Main.Companion.pluginData.vpnList =
                file.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        }

        // 이벤트 설정
        val event = essential.protect.Event()
        event.start()

        Log.debug(essential.protect.Main.Companion.bundle.get("event.plugin.loaded"))

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
        val commands = essential.protect.Commands()

        val methods = commands.javaClass.getDeclaredMethods()

        for (method in methods) {
            val annotation: ServerCommand? = method.getAnnotation<T?>(ServerCommand::class.java)
            if (annotation != null) {
                handler.register(annotation.name(), annotation.parameter(), annotation.description(), { args ->
                    if (args.length > 0) {
                        try {
                            method.invoke(commands, *kotlin.arrayOf<kotlin.Any>(args))
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        try {
                            method.invoke(commands, *kotlin.arrayOf<kotlin.Any>(kotlin.arrayOf<kotlin.String?>()))
                        } catch (e: java.lang.Exception) {
                            java.lang.System.err.println("arg size - " + args.length)
                            java.lang.System.err.println("command - " + annotation.name())
                            e.printStackTrace()
                        }
                    }
                })
            }
        }
    }

    public override fun registerClientCommands(handler: CommandHandler) {
        val commands = essential.protect.Commands()
        val methods = commands.javaClass.getDeclaredMethods()

        for (method in methods) {
            val annotation: ClientCommand? = method.getAnnotation<T?>(ClientCommand::class.java)
            if (annotation != null) {
                handler.< Player > register < Player ? > (annotation.name(), annotation.parameter(), annotation.description(), { args, player ->
                    var data: PlayerData? = findPlayerByUuid(player.uuid())
                    if (data == null) {
                        data = PlayerData()
                    }
                    if (Permission.INSTANCE.check(data, annotation.name())) {
                        try {
                            if (args.length > 0) {
                                method.invoke(commands, player, data, args)
                            } else {
                                method.invoke(commands, player, data, kotlin.arrayOf<kotlin.String?>())
                            }
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        data.send("command.permission.false")
                    }
                })
            }
        }
    }

    fun findPlayerByUuid(uuid: kotlin.String?): PlayerData {
        return database.getPlayers().stream().filter({ e -> e.getUuid().equals(uuid) }).findFirst().orElse(null)
    }

    companion object {
        var bundle: Bundle = Bundle()
        var conf: essential.protect.Config? = null
        var pluginData: essential.protect.PluginData = essential.protect.PluginData()
    }
}
