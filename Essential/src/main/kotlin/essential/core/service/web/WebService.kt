package essential.core.service.web
import arc.ApplicationListener
import arc.Core
import arc.util.Log
import essential.common.bundle.Bundle
import essential.common.config.Config
import kotlinx.coroutines.runBlocking
import mindustry.mod.Plugin
import java.util.*

class WebService : Plugin() {
    companion object {
        var bundle: Bundle = Bundle(ResourceBundle.getBundle("bundles/web/web"))
        var conf: WebConfig = reloadConf()

        fun reloadConf() : WebConfig {
            return runBlocking {
                val config = Config.load("config_web", WebConfig.serializer(), WebConfig())
                require(config != null) {
                    Log.err(bundle["event.plugin.load.failed"])
                }
                config
            }
        }
    }

    override fun init() {
        bundle.prefix = "[EssentialWeb]"

        Log.debug(bundle["event.plugin.starting"])

        val webServer = WebServer()
        webServer.start()
        Core.app.addListener(object : ApplicationListener {
            override fun dispose() {
                webServer.stop()
            }
        })
    }
}
