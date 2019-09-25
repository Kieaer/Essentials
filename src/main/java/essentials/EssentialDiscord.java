package essentials;

import com.mrpowergamerbr.temmiewebhook.DiscordMessage;
import com.mrpowergamerbr.temmiewebhook.TemmieWebhook;
import io.anuke.arc.Events;
import io.anuke.mindustry.game.EventType;
import org.json.JSONObject;

import static essentials.EssentialConfig.discordurl;
import static essentials.EssentialConfig.webhookenable;

public class EssentialDiscord {
    public static void main(){
        Events.on(EventType.PlayerChatEvent.class, e -> {
            if(!e.message.equals("/") && webhookenable) {
                JSONObject db = EssentialPlayer.getData(e.player.uuid);
                TemmieWebhook temmie = new TemmieWebhook(discordurl);
                DiscordMessage dm = new DiscordMessage(db.getString("name"), e.message, "http://img06.deviantart.net/a35d/i/2016/056/c/3/temmie___undertale_by_tartifondue-d9t3h1h.png");
                temmie.sendMessage(dm);
            }
        });
    }
}
