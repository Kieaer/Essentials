package essential.protect

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class ProtectConfig(
    var pvp: Pvp = Pvp(),
    var account: Account = Account(),
    var protect: Protect = Protect(),
    var rules: Rules = Rules()
) {
    @Serializable
    data class Pvp(
        var peace: Peace = Peace(),
        var border: Border = Border(),
        var destroyCore: Boolean = false
    ) {
        @Serializable
        data class Peace(
            var enabled: Boolean = false,
            var time: Int = 0
        )

        @Serializable
        data class Border(
            var enabled: Boolean = false
        )
    }

    enum class AuthType {
        None, Password, Discord
    }

    @Serializable
    data class Account(
        var enabled: Boolean = false,
        private val authType: String = AuthType.None.name,
        var discordURL: String = ""
    ) {
        fun getAuthType(): AuthType {
            return AuthType.valueOf(authType.take(1).uppercase(Locale.getDefault()) + authType.substring(1))
        }
    }

    @Serializable
    data class Protect(
        var unbreakableCore: Boolean = false,
        var powerDetect: Boolean = false
    )

    @Serializable
    data class Rules(
        var vpn: Boolean = false,
        var foo: Boolean = false,
        var mobile: Boolean = false,
        var steamOnly: Boolean = false,
        var minimalName: MinimalNameConfig = MinimalNameConfig(),
        var strict: Boolean = false,
        var blockNewUser: Boolean = false
    ) {
        @Serializable
        data class MinimalNameConfig (
            var enabled: Boolean = false,
            var length: Int = 0
        )
    }
}
