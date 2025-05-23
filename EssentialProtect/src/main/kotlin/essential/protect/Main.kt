package essential.protect

import arc.util.CommandHandler
import arc.util.Http
import arc.util.Log
import essential.bundle.Bundle
import essential.config.Config
import essential.core.Main
import essential.database.data.PlayerDataEntity
import essential.permission.Permission
import essential.protect.generated.registerGeneratedClientCommands
import essential.protect.generated.registerGeneratedEventHandlers
import essential.rootPath
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
            val data: PlayerDataEntity? = findPlayerData(action.player.uuid())
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
            Permission.default = "user"
        } else {
            Permission.default = "visitor"
        }

        // VPN 확인
        if (conf.rules.vpn) {
            var isUpdate = false
            if (Main.pluginData.get("vpnListDate") == null || java.util.Objects.requireNonNull<T?>(
                    Main.pluginData.get("vpnListDate")
                ).toLong() + 8.64e7 < System.currentTimeMillis()
            ) {
                Main.pluginData.getStatus()
                    .add(kotlin.Pair<A?, B?>("vpnListDate", java.lang.System.currentTimeMillis().toString()))
                isUpdate = true
            }

            if (isUpdate) {
                Http.get("https://raw.githubusercontent.com/X4BNet/lists_vpn/main/output/datacenter/ipv4.txt")
                    .error({ e -> Log.err("Failed to get vpn list!") })
                    .block({ e ->
                        val text = kotlin.text.String(java.io.BufferedInputStream(e.resultAsStream).readAllBytes())
                        rootPath.child("data/ipv4.txt").writeString(text)
                    })
            }

            val file: String = rootPath.child("data/ipv4.txt").readString()
            pluginData.vpnList =
                file.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        }

        // 이벤트 설정
        registerGeneratedEventHandlers()

        Log.debug(bundle["event.plugin.loaded"])
    }


    override fun registerClientCommands(handler: CommandHandler) {
        registerGeneratedClientCommands(handler)
    }

    companion object {
        var bundle: Bundle = Bundle()
        lateinit var conf: ProtectConfig
        var pluginData: PluginData = PluginData()
    }
}
