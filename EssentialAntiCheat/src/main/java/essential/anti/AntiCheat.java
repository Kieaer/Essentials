package essential.anti;

import arc.util.CommandHandler;
import arc.util.Log;
import essential.core.Bundle;
import mindustry.mod.Plugin;

public class AntiCheat extends Plugin {
    final static String CONFIG_PATH = "config/config_anti.yaml";
    final static Bundle bundle = new Bundle();

    AntiCheat() {
        // JVM Fake
        new EssentialAntiCheat();

        Thread.currentThread().setName("EssentialAntiCheat");
        bundle.setPrefix("[EssentialAntiCheat]");

        Log.info(bundle.get("event.plugin.starting"));
        Log.info(bundle.get("event.plugin.loaded"));
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        super.registerServerCommands(handler);
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        super.registerClientCommands(handler);
    }
}
