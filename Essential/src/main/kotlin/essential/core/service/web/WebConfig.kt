package essential.core.service.web

import com.charleskorn.kaml.YamlComment
import kotlinx.serialization.Serializable

@Serializable
data class WebConfig (
    @YamlComment("Port number for the web server")
    val port: Int = 32000,
    @YamlComment("Directory path where uploaded map files are stored")
    val uploadPath: String = "config/maps",
    @YamlComment("At least 32 characters of secret material used to encrypt and sign session cookies")
    val sessionSecret: String = "",
    @YamlComment("Only send session cookies over HTTPS")
    val secureCookie: Boolean = true,
    @YamlComment("Session validity duration in seconds (1 hour = 3600 seconds)")
    val sessionDuration: Long = 3600,
    @YamlComment("Maximum file upload size in bytes (10 MB = 10485760 bytes)")
    val maxFileSize: Long = 10485760,
    @YamlComment("Discord server invitation URL shown to users who need to link their account")
    val discordUrl: String = "https://discord.gg/yourserver",
    @YamlComment("Enable WebSocket for real-time communication between web server and clients")
    val enableWebSocket: Boolean = true
)
