package essentials;

import io.anuke.arc.util.Log;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.game.Team;

public class Global {
    public static void log(String msg){
        Log.info("[Essentials] "+msg);
    }

    public static void log(float msg){
        Log.info("[Essentials] "+msg);
    }

    public static void log(int msg){
        Log.info("[Essentials] "+msg);
    }

    public static void logw(String msg){
        Log.warn("[Essentials] "+msg);
    }

    public static void loge(String msg){
        Log.err("[Essentials] "+msg);
    }

    public static void banc(String msg){
        Log.info("[EssentialsBanClient] "+msg);
    }

    public static void bans(String msg){
        Log.info("[EssentialsBanServer] "+msg);
    }

    public static void bansw(String msg){
        Log.warn("[EssentialsBanServer] "+msg);
    }

    public static void chatc(String msg){
        Log.info("[EssentialsChatClient] "+msg);
    }

    public static void chats(String msg){
        Log.info("[EssentialsChatServer] "+msg);
    }

    public static void chatsw(String msg){
        Log.warn("[EssentialsBanServer] "+msg);
    }

    public static void logn(String msg) { Log.info(msg); }

    public static Team getTeamNoCore(Player player){
        int index = player.getTeam().ordinal()+1;
        while (index != player.getTeam().ordinal()){
            if (index >= Team.all.length){
                index = 0;
            }
            if (Vars.state.teams.get(Team.all[index]).cores.isEmpty()){
                return Team.all[index]; //return a team without a core
            }
            index++;
        }
        return player.getTeam();
    }
}
