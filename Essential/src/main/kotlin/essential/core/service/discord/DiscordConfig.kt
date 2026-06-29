package essential.core.service.discord

import com.charleskorn.kaml.YamlComment
import kotlinx.serialization.Serializable

@Serializable
data class DiscordConfig(
    @YamlComment("Set your discord server url")
    val url: String = ""
)
