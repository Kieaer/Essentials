package essential.bridge

import kotlinx.serialization.Serializable

@Serializable
data class BridgeConfig(
    var address: String = "127.0.0.1",
    var port: Int = (10000..65535).random(),
    var sharing: SharingConfig = SharingConfig()
)

@Serializable
data class SharingConfig(
    var ban: Boolean = false,
    var broadcast: Boolean = false
)