package essential.core.service.chat

import com.charleskorn.kaml.YamlComment
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class ChatConfig(
    @YamlComment(
        "Global default chat format (leave empty to use permission group formats)",
        "Placeholders: {player} for player name, {message} for chat message",
    )
    var chatFormat: String = "",
    @YamlComment("Strict chat filtering (deprecated - not implemented)")
    var strict: StrictConfig = StrictConfig(),
    @YamlComment("Chat blacklist settings")
    var blacklist: BlacklistConfig = BlacklistConfig()
)

@Serializable
data class StrictConfig(
    @YamlComment("Enable strict filtering (currently unused)")
    var enabled: Boolean = false,
    @YamlComment("Language code for filtering (ko-KR: Korean, en-US: English, etc.)")
    var language: String = Locale.getDefault().toLanguageTag()
)

@Serializable
data class BlacklistConfig(
    @YamlComment("Enable blacklist filtering to block prohibited words in chat")
    var enabled: Boolean = false,
    @YamlComment("Use regex patterns for blacklist entries instead of plain text")
    var regex: Boolean = false
)
