package essential.web

import arc.ApplicationListener
import arc.Core
import arc.util.Log
import essential.bundle.Bundle
import mindustry.mod.Plugin

class Main : Plugin() {
    override fun init() {
        bundle.prefix = "[EssentialWeb]"

        Log.debug(bundle["event.plugin.starting"])

        val config = essential.config.Config.load("config_web.yaml", WebConfig.serializer(), true, WebConfig())
        require(config != null) {
            Log.err(bundle["event.plugin.load.failed"])
            return
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

    companion object {
        var bundle: Bundle = Bundle()
        lateinit var conf: WebConfig
    }
}
