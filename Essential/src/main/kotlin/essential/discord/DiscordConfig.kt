package essential.discord

import kotlinx.serialization.Serializable

@Serializable
data class DiscordConfig(
    val url: String = ""
)
