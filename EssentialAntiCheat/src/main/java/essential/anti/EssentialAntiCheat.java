package essential.anti;

import arc.Events;
import mindustry.game.EventType;
import mindustry.mod.Plugin;

public class EssentialAntiCheat extends Plugin {
    @Override
    public void init() {
        Events.on(EventType.ServerLoadEvent.class, e -> new AntiCheat());
    }
}
