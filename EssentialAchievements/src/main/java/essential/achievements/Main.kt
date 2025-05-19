package essential.achievements

import arc.util.CommandHandler
import arc.util.Log
import essential.achievements.generated.registerGeneratedClientCommands
import essential.bundle.Bundle
import essential.database.data.PlayerData
import essential.players
import mindustry.mod.Plugin

class Main : Plugin() {
    public override fun init() {
        bundle.prefix = "[EssentialAchievements]"

        Log.debug(bundle["event.plugin.starting"])

        /*conf = essential.core.Main.Companion.createAndReadConfig(
                "config_achievements.yaml",
                Objects.requireNonNull(this.getClass().getResourceAsStream("/config_achievements.yaml")),
                Config.class
        );
*/
        // 이벤트 실행
        val event = Events()
        val methods = event.javaClass.getDeclaredMethods()
        for (method in methods) {
            val annotation: essential.core.annotation.Event? =
                method.getAnnotation<T?>(essential.core.annotation.Event::class.java)
            if (annotation != null) {
                try {
                    method.invoke(event)
                } catch (e: IllegalAccessException) {
                    throw java.lang.RuntimeException(e)
                } catch (e: java.lang.reflect.InvocationTargetException) {
                    throw java.lang.RuntimeException(e)
                }
            }
        }

        Log.debug(bundle["event.plugin.loaded"])
    }

    override fun registerClientCommands(handler: CommandHandler) {
        registerGeneratedClientCommands(handler)
    }

    fun findPlayerByUuid(uuid: kotlin.String?): PlayerData {
        return players.stream().filter({ e -> e.uuid == uuid }).findFirst().orElse(null)
    }

    companion object {
        var bundle: Bundle = Bundle()
        var conf: Config? = null
    }
}
