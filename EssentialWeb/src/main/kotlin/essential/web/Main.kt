package essential.web

import arc.ApplicationListener
import arc.Core
import arc.util.Log
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import essential.bundle.Bundle
import mindustry.mod.Plugin

class Main : Plugin() {
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

        val webServer = WebServer()
        webServer.start(conf.jdbcUrl, conf.username, conf.password)

        Core.app.addListener(object : ApplicationListener {
            override fun dispose() {
                webServer.stop()
            }
        })

        Log.debug(bundle["event.plugin.loaded"])
    }

    companion object {
        var bundle: Bundle = Bundle()
        lateinit var conf: WebConfig
    }
}
