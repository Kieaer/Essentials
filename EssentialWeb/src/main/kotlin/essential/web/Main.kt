package essential.web

import arc.ApplicationListener
import arc.Core
import arc.util.Log
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.zaxxer.hikari.HikariDataSource
import essential.bundle.Bundle
import essential.rootPath
import mindustry.mod.Plugin
import org.jetbrains.exposed.sql.Database

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
        val configFile = essential.rootPath.child("config/config_web.yaml")
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
                essential.rootPath.child("config").mkdirs()
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
            val root = ObjectMapper(YAMLFactory()).readTree(rootPath.child("config/config.yaml").file())
            val db = root.path("plugin").path("database")

            jdbcUrl = db.path("url").asText()
            username = db.path("username").asText()
            password = db.path("password").asText()
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
