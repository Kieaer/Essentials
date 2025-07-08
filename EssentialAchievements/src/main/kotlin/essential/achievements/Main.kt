package essential.achievements

import arc.util.CommandHandler
import arc.util.Log
import essential.achievements.generated.registerGeneratedClientCommands
import essential.achievements.generated.registerGeneratedEventHandlers
import essential.bundle.Bundle
import essential.database.data.PlayerData
import essential.players
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import mindustry.mod.Plugin

class Main : Plugin() {
    companion object {
        internal var bundle: Bundle = Bundle()
        internal var conf: AchievementConfig? = null

        internal val scope = CoroutineScope(Dispatchers.IO)
    }

    override fun init() {
        bundle.prefix = "[EssentialAchievements]"

        Log.debug(bundle["event.plugin.starting"])

        /*conf = essential.core.Main.Companion.createAndReadConfig(
                "config_achievements.yaml",
                Objects.requireNonNull(this.getClass().getResourceAsStream("/config_achievements.yaml")),
                Config.class
        );
*/
        // 이벤트 실행
        registerGeneratedEventHandlers()

        // Initialize APMTracker
        APMTracker()

        Log.debug(bundle["event.plugin.loaded"])
    }

    override fun registerClientCommands(handler: CommandHandler) {
        registerGeneratedClientCommands(handler)
    }
}
