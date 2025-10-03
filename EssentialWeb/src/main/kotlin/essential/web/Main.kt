package essential.web
import arc.ApplicationListener
import arc.Core
import arc.util.Log
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.zaxxer.hikari.HikariDataSource
import essential.common.bundle.Bundle
import kotlinx.serialization.Serializable
import mindustry.mod.Plugin
import org.jetbrains.exposed.v1.jdbc.Database

class Main : Plugin() {
    companion object {
        var bundle: Bundle = Bundle()
        lateinit var conf: WebConfig
    }
    private lateinit var datasource: HikariDataSource

    override fun init() {
        bundle.prefix = "[EssentialWeb]"

        Log.debug(bundle["event.plugin.starting"])

        // Create a default config
        val defaultConfig = WebConfig()

        // Create a local instance of Yaml to avoid class loader conflicts
        val localYaml = Yaml(configuration = YamlConfiguration(strictMode = false))

        // Load the config manually without using the Essential module's Yaml instance
        val rp = EssentialLookup.getRootPath() ?: arc.Core.settings.dataDirectory.child("mods/Essentials/")
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
                Log.info(bundle["config.created", "config_web.yaml"])
                defaultConfig
            } catch (e: Exception) {
                Log.err(bundle["event.plugin.load.failed"], e)
                defaultConfig
            }
        }
        conf = config
        datasource = HikariDataSource().apply {
            // Create a simple data class to represent the config structure
            @Serializable
            data class DatabaseConfig(val url: String = "", val username: String = "", val password: String = "")
            
            @Serializable
            data class PluginConfig(val database: DatabaseConfig = DatabaseConfig())
            
            @Serializable
            data class RootConfig(val plugin: PluginConfig = PluginConfig())
            
            val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))
            val configContent = rp.child("config/config.yaml").readString()
            val config = yaml.decodeFromString(RootConfig.serializer(), configContent)
            
            jdbcUrl = config.plugin.database.url
            username = config.plugin.database.username
            password = config.plugin.database.password
            maximumPoolSize = 2
        }
        Database.connect(datasource)
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
