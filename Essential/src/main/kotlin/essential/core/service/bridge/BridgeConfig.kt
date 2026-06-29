package essential.core.service.bridge

import com.charleskorn.kaml.YamlComment
import kotlinx.serialization.Serializable

@Serializable
data class BridgeConfig(
    @YamlComment("Bridge server address (for client mode connection)")
    var address: String = "127.0.0.1",
    @YamlComment("Bridge server port number")
    var port: Int = (10000..65535).random(),
    @YamlComment("Settings for sharing data with bridge server")
    var sharing: SharingConfig = SharingConfig()
)

@Serializable
data class SharingConfig(
    @YamlComment("Share ban list with bridge server")
    var ban: Boolean = false,
    @YamlComment("Share broadcast messages with bridge server")
    var broadcast: Boolean = false
)
