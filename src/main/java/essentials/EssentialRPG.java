package essentials;

import io.anuke.arc.Events;
import io.anuke.mindustry.game.EventType;
import org.json.JSONObject;

public class EssentialRPG {
    public EssentialRPG(){
        Events.on(EventType.PlayerJoin.class, e -> {
            
        });
    }
}
