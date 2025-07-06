package essential.achievements;

import arc.util.CommandHandler;
import arc.util.Log;
import essential.achievements.generated.ClientCommandsGeneratedKt;
import essential.achievements.generated.EventHandlersGenerated;
import essential.bundle.Bundle;
import mindustry.mod.Plugin;

public class Main extends Plugin {
    // Static field equivalent to companion object
    public static Bundle bundle = new Bundle();

    @Override
    public void init() {
        bundle.setPrefix("[EssentialAchievements]");

        Log.debug(bundle.get("event.plugin.starting"));

        // Register event handlers
        EventHandlersGenerated.registerGeneratedEventHandlers();

        Log.debug(bundle.get("event.plugin.loaded"));
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        ClientCommandsGeneratedKt.registerGeneratedClientCommands(handler);
    }
}
