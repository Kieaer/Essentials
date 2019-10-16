package essentials;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import io.anuke.arc.Core;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.type.Player;
import org.json.JSONObject;

import static essentials.EssentialConfig.keyfile;
import static essentials.EssentialPlayer.getData;
import static io.anuke.mindustry.Vars.playerGroup;

class EssentialTR {
    static void main(Player player, String message) {
        if(Core.settings.getDataDirectory().child("plugins/Essentials/"+keyfile+".json").exists()){
            Thread t = new Thread(() -> {
                try{
                    Translate translate = TranslateOptions.newBuilder().setCredentials(ServiceAccountCredentials.fromStream(Core.settings.getDataDirectory().child("plugins/Essentials/"+keyfile+".json").read())).build().getService();
                    Translation translation = translate.translate(message);
                    if(playerGroup != null && playerGroup.size() > 0) {
                        for (int i = 0; i < playerGroup.size(); i++) {
                            Player p = playerGroup.all().get(i);
                            if(!Vars.state.teams.get(p.getTeam()).cores.isEmpty()){
                                JSONObject db = getData(p.uuid);
                                boolean value = db.getBoolean("translate");
                                if (value) {
                                    p.sendMessage("[orange]["+player.name.replaceAll("\\[(.*?)]", "")+"][white]: [#F5FF6B]"+translation.getTranslatedText());
                                }
                            }
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            });
            t.start();
        }
    }
}