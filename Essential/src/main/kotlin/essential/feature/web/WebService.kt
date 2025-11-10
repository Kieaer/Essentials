package essential.feature.web
import arc.ApplicationListener
import arc.Core
import arc.util.Log
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import essential.common.bundle.Bundle
import mindustry.mod.Plugin

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
