package essential.core.service.protect

import arc.util.CommandHandler
import arc.util.Log
import essential.common.bundle.Bundle
import essential.common.config.Config
import essential.common.database.data.PlayerData
import essential.common.permission.Permission
import essential.common.util.findPlayerData
import essential.core.service.protect.generated.registerGeneratedClientCommands
import essential.core.service.protect.generated.registerGeneratedEventHandlers
import mindustry.Vars.netServer
import mindustry.mod.Plugin
import java.net.URI
import java.util.Objects.requireNonNull


class ProtectService : Plugin() {
    companion object {
        var bundle: Bundle = Bundle()
        lateinit var conf: ProtectConfig
        var pluginData: PluginData = PluginData()
    }

    override fun init() {
        bundle.prefix = "[EssentialProtect]"

        val config = Config.load("config_protect", ProtectConfig.serializer(), ProtectConfig())
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
                    if (requireNonNull(conf.account.getAuthType()) == ProtectConfig.AuthType.Discord) {
                        // 계정에 Discord 인증이 안되어 있는 경우
                        if (data.discordID == null) {
                            action.player.sendMessage(Bundle(action.player.locale)["event.discord.not.registered"])
                            return@addActionFilter false
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
            val list = URI("https://raw.githubusercontent.com/X4BNet/lists_vpn/main/output/vpn/ipv4.txt").toURL().readText()
            pluginData.vpnList = list.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        }

        // 이벤트 설정
        registerGeneratedEventHandlers()
    }


    override fun registerClientCommands(handler: CommandHandler) {
        registerGeneratedClientCommands(handler)
    }
}
