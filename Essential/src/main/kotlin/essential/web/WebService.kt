package essential.web
import arc.ApplicationListener
import arc.Core
import arc.util.Log
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import essential.common.bundle.Bundle
import io.r2dbc.spi.ConnectionFactoryOptions
import kotlinx.serialization.Serializable
import mindustry.mod.Plugin
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase

class WebService : Plugin() {
    companion object {
        var bundle: Bundle = Bundle()
        lateinit var conf: WebConfig
    }

    override fun init() {
        bundle.prefix = "[EssentialWeb]"

        Log.debug(bundle["event.plugin.starting"])

        // Create a default config
        val defaultConfig = WebConfig()

        // Create a local instance of Yaml to avoid class loader conflicts
        val localYaml = Yaml(configuration = YamlConfiguration(strictMode = false))

        // Load the config manually without using the Essential module's Yaml instance
        val rp = Core.settings.dataDirectory.child("mods/Essentials/")
        val configFile = rp.child("config/config_web.yaml")
        val config = if (configFile.exists()) {
            try {
                val content = configFile.readString()
                localYaml.decodeFromString(WebConfig.serializer(), content)
            } catch (e: Exception) {
                Log.err(bundle["event.plugin.load.failed"], e)
                defaultConfig
            }
        } else {
            // Save the default config
            try {
                rp.child("config").mkdirs()
                val content = localYaml.encodeToString(WebConfig.serializer(), defaultConfig)
                configFile.writeString(content)
                Log.info(bundle["config.created", "configs/config_web.yaml"])
                defaultConfig
            } catch (e: Exception) {
                Log.err(bundle["event.plugin.load.failed"], e)
                defaultConfig
            }
        }
        conf = config
        R2dbcDatabase.connect {
            @Serializable
            data class DatabaseConfig(val url: String = "", val username: String = "", val password: String = "")
            
            @Serializable
            data class PluginConfig(val database: DatabaseConfig = DatabaseConfig())
            
            @Serializable
            data class RootConfig(val plugin: PluginConfig = PluginConfig())
            
            val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))
            val configContent = rp.child("config/config.yaml").readString()
            val config = yaml.decodeFromString(RootConfig.serializer(), configContent)

            fun normalizeUrl(url: String): String {
                if (url.startsWith("r2dbc:h2:")) return url
                if (url.startsWith("r2dbc:sqlite:")) {
                    val path = url.removePrefix("r2dbc:sqlite:").replace('\\','/')
                    return "r2dbc:h2:file:///" + path
                }
                if (url.startsWith("jdbc:sqlite:")) {
                    val path = url.removePrefix("jdbc:sqlite:").replace('\\','/')
                    return "r2dbc:h2:file:///" + path
                }
                if (url.startsWith("sqlite:")) {
                    val path = url.removePrefix("sqlite:").replace('\\','/')
                    return "r2dbc:h2:file:///" + path
                }
                if (url.startsWith("jdbc:h2:")) return "r2dbc:h2:" + url.removePrefix("jdbc:h2:")
                if (url.startsWith("h2:")) return "r2dbc:h2:" + url.removePrefix("h2:")
                if (url.startsWith("r2dbc:")) return url
                return if (url.startsWith("jdbc:")) "r2dbc:" + url.removePrefix("jdbc:") else url
            }

            setUrl(normalizeUrl(config.plugin.database.url))
            connectionFactoryOptions {
                option(ConnectionFactoryOptions.USER, config.plugin.database.username)
                option(ConnectionFactoryOptions.PASSWORD, config.plugin.database.password)
            }
        }

        val webServer = WebServer()
        webServer.start()
        Core.app.addListener(object : ApplicationListener {
            override fun dispose() {
                webServer.stop()
            }
        })
        Log.debug(bundle["event.plugin.loaded"])
    }
}
