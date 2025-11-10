package essential.feature.achievements

import arc.util.CommandHandler
import arc.util.Log
import essential.achievements.generated.registerGeneratedClientCommands
import essential.achievements.generated.registerGeneratedEventHandlers
import essential.common.bundle.Bundle
import mindustry.mod.Plugin

class AchievementService : Plugin() {
    companion object {
        internal var bundle: Bundle = Bundle()
        internal var conf: AchievementConfig? = null
    }

    override fun init() {
        bundle.prefix = "[EssentialAchievements]"

        Log.debug(bundle["event.plugin.starting"])

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
