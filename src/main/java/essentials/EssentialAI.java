package essentials;

import io.anuke.arc.Events;
import io.anuke.mindustry.game.EventType;

public class EssentialAI {
    public static void main(){
        Events.on(EventType.PlayerJoin.class, e -> {
            Thread t = new Thread(() -> {

            });
            t.start();
        });
    }
}
