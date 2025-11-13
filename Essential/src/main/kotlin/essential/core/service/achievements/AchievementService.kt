package essential.core.service.achievements

import arc.util.CommandHandler
import essential.common.bundle.Bundle
import essential.core.service.achievements.generated.registerGeneratedClientCommands
import essential.core.service.achievements.generated.registerGeneratedEventHandlers
import essential.core.service.achievements.generated.registerGeneratedServerCommands
import mindustry.mod.Plugin

class AchievementService : Plugin() {
    companion object {
        var bundle: Bundle = Bundle()
    }

    override fun init() {
        bundle.prefix = "[EssentialAchievements]"
        // 이벤트 실행
        registerGeneratedEventHandlers()

        // Initialize APMTracker
        APMTracker()
    }

    override fun registerServerCommands(handler: CommandHandler) {
        registerGeneratedServerCommands(handler)
    }

    override fun registerClientCommands(handler: CommandHandler) {
        registerGeneratedClientCommands(handler)
    }
}
