package essential.protect

import java.util.*

class Config {
    var pvp: Pvp? = null
    var account: Account? = null
    var protect: Protect? = null
    var rules: Rules? = null

    class Pvp {
        var peace: Peace? = null
        var border: Border? = null
        var destroyCore: Boolean = false

        class Peace {
            var enabled: Boolean = false
            var time: Int = 0
        }

        class Border {
            var enabled: Boolean = false
        }
    }

    class Account {
        enum class AuthType {
            None, Password, Discord
        }

        var enabled: Boolean = false
        private val authType: String? = null
        var discordURL: String? = null

        fun getAuthType(): AuthType {
            return AuthType.valueOf(authType!!.substring(0, 1).uppercase(Locale.getDefault()) + authType.substring(1))
        }
    }

    class Protect {
        var unbreakableCore: Boolean = false
        var powerDetect: Boolean = false
    }

    class Rules {
        var vpn: Boolean = false
        var foo: Boolean = false
        var mobile: Boolean = false
        var steamOnly: Boolean = false
        var minimalName: MinimalNameConfig? = null
        var strict: Boolean? = null
        var blockNewUser: Boolean? = null

        internal class MinimalNameConfig {
            var enabled: Boolean? = null
            var length: Int = 0
        }
    }
}
