package essential.web

import arc.util.Log
import essential.core.Bundle
import mindustry.mod.Plugin
import java.util.*

class Main : Plugin() {
    public override fun init() {
        bundle.setPrefix("[EssentialWeb]")

        Log.debug(bundle.get("event.plugin.starting"))

        conf = essential.core.Main.Companion.createAndReadConfig(
            "config_web.yaml",
            Objects.requireNonNull<T?>(this.javaClass.getResourceAsStream("/config_web.yaml")),
            Config::class.java
        )

        Log.debug(bundle.get("event.plugin.loaded"))
    }

    companion object {
        var bundle: Bundle = Bundle()
        var conf: Config? = null
    }
}
