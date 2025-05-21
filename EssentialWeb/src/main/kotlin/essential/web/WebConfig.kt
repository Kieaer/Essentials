package essential.web

import kotlinx.serialization.Serializable

@Serializable
data class WebConfig (
    val port: Int = 0
)
