package essential.core.service.protect

import com.charleskorn.kaml.YamlComment
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class ProtectConfig(
    @YamlComment("PvP mode settings")
    var pvp: Pvp = Pvp(),
    @YamlComment("Account system settings (requires player authentication)")
    var account: Account = Account(),
    @YamlComment("Core protection settings")
    var protect: Protect = Protect(),
    @YamlComment("Player rule enforcement settings")
    var rules: Rules = Rules()
) {
    @Serializable
    data class Pvp(
        @YamlComment("Peace mode settings (disables all player vs player combat)")
        var peace: Peace = Peace(),
        @YamlComment("Border mode settings (kills players outside the map boundaries)")
        var border: Border = Border(),
        @YamlComment("Allow players to destroy cores in PvP mode")
        var destroyCore: Boolean = false
    ) {
        @Serializable
        data class Peace(
            @YamlComment("Enable peace mode (no damage between players)")
            var enabled: Boolean = false,
            @YamlComment("Duration of peace mode in seconds (0 = permanent peace mode)")
            var time: Int = 0
        )

        @Serializable
        data class Border(
            @YamlComment("Enable border protection (kills players who go outside the map)")
            var enabled: Boolean = false
        )
    }

    enum class AuthType {
        None, Password, Discord
    }

    @Serializable
    data class Account(
        @YamlComment("Enable account authentication system")
        var enabled: Boolean = false,
        @YamlComment("Authentication method: None, Password, or Discord")
        private val authType: String = AuthType.None.name,
        @YamlComment("Discord OAuth URL for Discord-based authentication")
        var discordURL: String = ""
    ) {
        fun getAuthType(): AuthType {
            return AuthType.valueOf(authType.take(1).uppercase(Locale.getDefault()) + authType.substring(1))
        }
    }

    @Serializable
    data class Protect(
        @YamlComment("Make all cores indestructible (health set to 100 million)")
        var unbreakableCore: Boolean = false,
        @YamlComment("Detect and prevent power graph manipulation exploits")
        var powerDetect: Boolean = false
    )

    @Serializable
    data class Rules(
        @YamlComment("Block players using VPN connections")
        var vpn: Boolean = false,
        @YamlComment("Block players with \"foo\" in their name (deprecated - not implemented)")
        var foo: Boolean = false,
        @YamlComment("Block mobile players from joining")
        var mobile: Boolean = false,
        @YamlComment("Block non-Steam players (deprecated - not implemented)")
        var steamOnly: Boolean = false,
        @YamlComment("Enforce minimum username length")
        var minimalName: MinimalNameConfig = MinimalNameConfig(),
        @YamlComment("Strict mode - forces player name to match registered name")
        var strict: Boolean = false,
        @YamlComment("Block completely new players (only allow returning players)")
        var blockNewUser: Boolean = false
    ) {
        @Serializable
        data class MinimalNameConfig (
            @YamlComment("Enable username length check")
            var enabled: Boolean = false,
            @YamlComment("Minimum required character length for usernames")
            var length: Int = 0
        )
    }
}
