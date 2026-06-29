package essential.core.service.web

import com.charleskorn.kaml.YamlComment
import kotlinx.serialization.Serializable

@Serializable
data class WebConfig (
    @YamlComment("Port number for the web server")
    val port: Int = 32000,
    @YamlComment("Directory path where uploaded map files are stored")
    val uploadPath: String = "config/maps",
    @YamlComment("Secret key used for encrypting session cookies")
    val sessionSecret: String = "essentialWebSecret",
    @YamlComment("Session validity duration in seconds (1 hour = 3600 seconds)")
    val sessionDuration: Long = 3600,
    @YamlComment("Maximum file upload size in bytes (10 MB = 10485760 bytes)")
    val maxFileSize: Long = 10485760,
    @YamlComment("Discord server invitation URL shown to users who need to link their account")
    val discordUrl: String = "https://discord.gg/yourserver",
    @YamlComment("Enable WebSocket for real-time communication between web server and clients")
    val enableWebSocket: Boolean = true
)
