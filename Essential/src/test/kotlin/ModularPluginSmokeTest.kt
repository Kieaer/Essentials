import essential.core.Main
import kotlin.test.Test
import kotlin.test.assertNotNull

/** Boots the plugin with the exact main source set used by a modular artifact. */
class ModularPluginSmokeTest {
    @Test
    fun bootsWithoutExcludedServices() {
        PluginTest.loadGame(loadPlugin = true)
        try {
            assertNotNull(Main.conf)
        } finally {
            PluginTest.stopPlugin()
        }
    }
}
