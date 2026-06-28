package essential.core.service.contribution

import arc.util.CommandHandler
import arc.util.Log
import essential.common.bundle.Bundle
import essential.common.config.Config
import essential.core.service.contribution.generated.registerGeneratedClientCommands
import essential.core.service.contribution.generated.registerGeneratedEventHandlers
import kotlinx.coroutines.runBlocking
import mindustry.mod.Plugin

/**
 * Per-game contribution scoring service.
 *
 * Tracks mining, factory builds, item production, power generation, building damage
 * and unit losses to produce a per-game contribution score, persisted per game into
 * [essential.common.database.table.ContributionTable] and averaged across games.
 *
 * See ContributionEvents.kt for the event handlers and the per-second polling loop.
 */
class ContributionService : Plugin() {
    companion object {
        var bundle: Bundle = Bundle()
        var conf: ContributionConfig = reloadConf()

        fun reloadConf(): ContributionConfig {
            return runBlocking {
                val config = Config.load("config_contribution", ContributionConfig.serializer(), ContributionConfig())
                require(config != null) {
                    Log.err(bundle["event.plugin.load.failed"])
                }
                config
            }
        }
    }

    override fun init() {
        bundle.prefix = "[EssentialContribution]"
        registerGeneratedEventHandlers()
    }

    override fun registerClientCommands(handler: CommandHandler) {
        registerGeneratedClientCommands(handler)
    }
}
