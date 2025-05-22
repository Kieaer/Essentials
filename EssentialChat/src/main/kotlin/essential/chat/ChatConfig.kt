package essential.chat

import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class ChatConfig(
    var chatFormat: String = "",
    var strict: StrictConfig = StrictConfig(),
    var blacklist: BlacklistConfig = BlacklistConfig()
)

@Serializable
data class StrictConfig(
    var enabled: Boolean = false,
    var language: String = Locale.getDefault().toLanguageTag()
)

@Serializable
data class BlacklistConfig(
    var enabled: Boolean = false,
    var regex: Boolean = false
)
