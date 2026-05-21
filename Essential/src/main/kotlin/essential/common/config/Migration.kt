package essential.common.config

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import essential.common.rootPath
import essential.core.service.bridge.BridgeConfig
import essential.core.service.bridge.SharingConfig
import essential.core.service.chat.BlacklistConfig
import essential.core.service.chat.ChatConfig
import essential.core.service.chat.StrictConfig
import essential.core.service.protect.ProtectConfig
import essential.core.service.web.WebConfig
import kotlinx.serialization.Serializable
import java.nio.file.Files
import java.nio.file.Path

object Migration {
    private val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))
    private var isMigrated = false

    fun migrateConfigs() {
        if (isMigrated) return

        try {
            exportConfigsFromResources()
            migrateOldConfigsToNewLocation()
            isMigrated = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun reset() {
        isMigrated = false
    }
    
    private fun exportConfigsFromResources() {
        val configDir = rootPath.child("config")
        configDir.mkdirs()

        val configsToExport = listOf(
            "config.yaml",
            "config_bridge.yaml",
            "config_chat.yaml",
            "config_discord.yaml",
            "config_protect.yaml",
            "config_web.yaml",
            "config_achievements.yaml"
        )

        for (configName in configsToExport) {
            val configPath = configDir.child(configName)
            if (!configPath.exists()) {
                try {
                    val resourceStream = Migration::class.java.getResourceAsStream("/configs/$configName")
                    if (resourceStream != null) {
                        val content = resourceStream.readAllBytes().decodeToString()
                        configPath.writeString(content, false)
                        resourceStream.close()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    private fun migrateOldConfigsToNewLocation() {
        val oldConfigNames = listOf(
            "config_bridge.yaml",
            "config_chat.yaml",
            "config_discord.yaml",
            "config_protect.yaml",
            "config_web.yaml",
            "config_achievements.yaml"
        )

        val configDir = rootPath.child("config")
        for (configName in oldConfigNames) {
            val oldConfigPath = rootPath.child(configName)
            if (oldConfigPath.exists()) {
                try {
                    migrateConfig(configName, oldConfigPath.file().toPath(), configDir)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    private fun migrateConfig(configName: String, oldPath: Path, newDir: arc.files.Fi) {
        when (configName) {
            "config_bridge.yaml" -> migrateBridgeConfig(oldPath, newDir)
            "config_chat.yaml" -> migrateChatConfig(oldPath, newDir)
            "config_discord.yaml" -> migrateDiscordConfig(oldPath, newDir)
            "config_protect.yaml" -> migrateProtectConfig(oldPath, newDir)
            "config_web.yaml" -> migrateWebConfig(oldPath, newDir)
            "config_achievements.yaml" -> migrateAchievementsConfig(oldPath, newDir)
        }
    }
    
    private fun migrateBridgeConfig(oldPath: Path, newDir: arc.files.Fi) {
        val oldContent = Files.readString(oldPath)
        val oldConfig = yaml.decodeFromString(BridgeConfigOld.serializer(), oldContent)
        
        val newConfig = BridgeConfig(
            address = oldConfig.address,
            port = oldConfig.port,
            sharing = SharingConfig(
                ban = oldConfig.sharing.ban,
                broadcast = oldConfig.sharing.broadcast
            )
        )
        
        val newConfigPath = newDir.child("config_bridge.yaml")
        val content = yaml.encodeToString(BridgeConfig.serializer(), newConfig)
        newConfigPath.writeString(content, false)
        
        oldPath.toFile().delete()
        
    }
    
    private fun migrateChatConfig(oldPath: Path, newDir: arc.files.Fi) {
        val oldContent = Files.readString(oldPath)
        val oldConfig = yaml.decodeFromString(ChatConfigOld.serializer(), oldContent)
        
        val newConfig = ChatConfig(
            chatFormat = oldConfig.chatFormat,
            strict = StrictConfig(
                enabled = oldConfig.strict.enabled,
                language = oldConfig.strict.language
            ),
            blacklist = BlacklistConfig(
                enabled = oldConfig.blacklist.enabled,
                regex = oldConfig.blacklist.regex
            )
        )
        
        val newConfigPath = newDir.child("config_chat.yaml")
        val content = yaml.encodeToString(ChatConfig.serializer(), newConfig)
        newConfigPath.writeString(content, false)
        
        oldPath.toFile().delete()
        
    }
    
    private fun migrateDiscordConfig(oldPath: Path, newDir: arc.files.Fi) {
        val content = Files.readString(oldPath)
        val newConfigPath = newDir.child("config_discord.yaml")
        newConfigPath.writeString(content, false)
        
        oldPath.toFile().delete()
        
    }
    
    private fun migrateProtectConfig(oldPath: Path, newDir: arc.files.Fi) {
        val oldContent = Files.readString(oldPath)
        val oldConfig = yaml.decodeFromString(ProtectConfigOld.serializer(), oldContent)
        
        val newConfig = ProtectConfig(
            pvp = ProtectConfig.Pvp(
                peace = ProtectConfig.Pvp.Peace(
                    enabled = oldConfig.pvp.peace.enabled,
                    time = oldConfig.pvp.peace.time
                ),
                border = ProtectConfig.Pvp.Border(
                    enabled = oldConfig.pvp.border.enabled
                ),
                destroyCore = oldConfig.pvp.destroyCore
            ),
            account = ProtectConfig.Account(
                enabled = oldConfig.account.enabled,
                authType = oldConfig.account.authType,
                discordURL = oldConfig.account.discordURL
            ),
            protect = ProtectConfig.Protect(
                unbreakableCore = oldConfig.protect.unbreakableCore,
                powerDetect = oldConfig.protect.powerDetect
            ),
            rules = ProtectConfig.Rules(
                vpn = oldConfig.rules.vpn,
                foo = oldConfig.rules.foo,
                mobile = oldConfig.rules.mobile,
                steamOnly = oldConfig.rules.steamOnly,
                minimalName = ProtectConfig.Rules.MinimalNameConfig(
                    enabled = oldConfig.rules.minimalName.enabled,
                    length = oldConfig.rules.minimalName.length
                ),
                strict = oldConfig.rules.strict,
                blockNewUser = oldConfig.rules.blockNewUser
            )
        )
        
        val newConfigPath = newDir.child("config_protect.yaml")
        val content = yaml.encodeToString(ProtectConfig.serializer(), newConfig)
        newConfigPath.writeString(content, false)
        
        oldPath.toFile().delete()
    }
    
    private fun migrateWebConfig(oldPath: Path, newDir: arc.files.Fi) {
        val oldContent = Files.readString(oldPath)
        val oldConfig = yaml.decodeFromString(WebConfigOld.serializer(), oldContent)
        
        val newConfig = WebConfig(
            port = oldConfig.port,
            uploadPath = "config/maps",
            sessionSecret = "essentialWebSecret",
            sessionDuration = 3600,
            maxFileSize = 10485760,
            discordUrl = "https://discord.gg/yourserver",
            enableWebSocket = true
        )
        
        val newConfigPath = newDir.child("config_web.yaml")
        val content = yaml.encodeToString(WebConfig.serializer(), newConfig)
        newConfigPath.writeString(content, false)
        
        oldPath.toFile().delete()
    }
    
    private fun migrateAchievementsConfig(oldPath: Path, newDir: arc.files.Fi) {
        val content = Files.readString(oldPath)
        val newConfigPath = newDir.child("config_achievements.yaml")
        newConfigPath.writeString(content, false)
        
        oldPath.toFile().delete()
    }
}

/**
 * Old config structures for migration
 */

@Suppress("Unused")
@Serializable
data class BridgeConfigOld(
    val address: String = "127.0.0.1",
    val port: Int = 42600,
    val sharing: SharingConfigOld = SharingConfigOld()
)

@Suppress("Unused")
@Serializable
data class SharingConfigOld(
    val ban: Boolean = false,
    val broadcast: Boolean = false
)

@Suppress("Unused")
@Serializable
data class ChatConfigOld(
    val chatFormat: String = "%player.name[orange] >[white] %chat",
    val strict: StrictConfigOld = StrictConfigOld(),
    val blacklist: BlacklistConfigOld = BlacklistConfigOld()
)

@Suppress("Unused")
@Serializable
data class StrictConfigOld(
    val enabled: Boolean = false,
    val language: String = "ko,en"
)

@Suppress("Unused")
@Serializable
data class BlacklistConfigOld(
    val enabled: Boolean = true,
    val regex: Boolean = false
)

@Suppress("Unused")
@Serializable
data class ProtectConfigOld(
    val pvp: PvpOld = PvpOld(),
    val account: AccountOld = AccountOld(),
    val protect: ProtectOld = ProtectOld(),
    val rules: RulesOld = RulesOld()
) {
    @Suppress("Unused")
    @Serializable
    data class PvpOld(
        val peace: PeaceOld = PeaceOld(),
        val border: BorderOld = BorderOld(),
        val destroyCore: Boolean = false
    ) {
        @Suppress("Unused")
        @Serializable
        data class PeaceOld(
            val enabled: Boolean = false,
            val time: Int = 300
        )

        @Suppress("Unused")
        @Serializable
        data class BorderOld(
            val enabled: Boolean = false
        )
    }

    @Suppress("Unused")
    @Serializable
    data class AccountOld(
        val enabled: Boolean = false,
        val authType: String = "none",
        val discordURL: String = ""
    )

    @Suppress("Unused")
    @Serializable
    data class ProtectOld(
        val unbreakableCore: Boolean = false,
        val powerDetect: Boolean = true
    )

    @Suppress("Unused")
    @Serializable
    data class RulesOld(
        val vpn: Boolean = true,
        val foo: Boolean = false,
        val mobile: Boolean = true,
        val steamOnly: Boolean = false,
        val minimalName: MinimalNameConfigOld = MinimalNameConfigOld(),
        val strict: Boolean = true,
        val blockNewUser: Boolean = false
    ) {
        @Suppress("Unused")
        @Serializable
        data class MinimalNameConfigOld(
            val enabled: Boolean = false,
            val length: Int = 4
        )
    }
}

@Suppress("Unused")
@Serializable
data class WebConfigOld(
    val port: Int = 32148
)

