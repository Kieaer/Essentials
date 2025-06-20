package essential.web

import kotlinx.serialization.Serializable

@Serializable
data class WebConfig (
    val port: Int = 0,
    val uploadPath: String = "maps/uploads",
    val sessionSecret: String = "essentialWebSecret",
    val sessionDuration: Long = 3600, // Session duration in seconds
    val maxFileSize: Long = 10485760, // 10MB max file size
    val discordUrl: String = "https://discord.gg/yourserver", // Discord URL for users without Discord ID
    val enableWebSocket: Boolean = true
)
