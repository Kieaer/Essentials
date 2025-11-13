package essential.core.service.discord

import kotlinx.serialization.Serializable

@Serializable
data class DiscordConfig(
    val url: String = ""
)