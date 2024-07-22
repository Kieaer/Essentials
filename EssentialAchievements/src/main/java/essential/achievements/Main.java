package essential.achievements;

import arc.util.CommandHandler;
import arc.util.Log;
import essential.core.Bundle;
import mindustry.mod.Plugin;

public class Main extends Plugin {
    static Bundle bundle = new Bundle();
    static Config conf;

    @Override
    public void init() {
        bundle.setPrefix("[EssentialAchievements]");
        Log.debug(bundle.get("event.plugin.starting"));

        /*conf = essential.core.Main.Companion.createAndReadConfig(
                "config_achievements.yaml",
                Objects.requireNonNull(this.getClass().getResourceAsStream("/config_achievements.yaml")),
                Config.class
        );
*/
        new Event().start();

        Log.info(bundle.get("event.plugin.loaded"));
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {

    }

    @Override
    public void registerClientCommands(CommandHandler handler) {

    }
}
