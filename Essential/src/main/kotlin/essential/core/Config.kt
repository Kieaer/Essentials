package essential.core

import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val plugin: PluginConfig
)

@Serializable
data class PluginConfig(
    /** plugin language */
    val lang: String,
    /** plugin auto update */
    val autoUpdate: Boolean,
    /** plugin database */
    val database: DatabaseConfig
)


@Serializable
data class DatabaseConfig(
    /** database jdbc url */
    val url: String?,
    /** database username */
    val username: String?,
    /** database password */
    val password: String?
)

