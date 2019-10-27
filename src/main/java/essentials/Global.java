package essentials;

import io.anuke.arc.Core;
import io.anuke.arc.util.Log;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.game.Team;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static essentials.EssentialConfig.debug;
import static essentials.EssentialPlayer.getData;

public class Global {
    public static void log(String msg){
        Log.info("[Essential] "+msg);
    }

    public static void log(float msg){
        Log.info("[Essential] "+msg);
    }

    public static void log(int msg){
        Log.info("[Essential] "+msg);
    }

    public static void logw(String msg){
        Log.warn("[Essential] "+msg);
    }

    public static void loge(String msg){
        Log.err("[Essential] "+msg);
    }

    public static void logs(String msg){
        Log.info("[EssentialServer] "+msg);
    }

    public static void logc(String msg){
        Log.info("[EssentialClient] "+msg);
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

    public static void printStackTrace(Throwable e) {
        if(!debug){
            StringBuilder sb = new StringBuilder();
            try {
                sb.append(e.toString());
                sb.append("\n");
                StackTraceElement[] element = e.getStackTrace();
                for (StackTraceElement stackTraceElement : element) {
                    sb.append("\tat ");
                    sb.append(stackTraceElement.toString());
                    sb.append("\n");
                }
                sb.append("=================================================\n");
                String text = sb.toString();

                Path path = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/Logs/error.log")));
                byte[] result = text.getBytes();
                Files.write(path, result, StandardOpenOption.APPEND);
                Global.loge("Internal error! - "+e.getMessage());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            e.printStackTrace();
        }
    }

    public static String gettime(){
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm.ss", Locale.ENGLISH);
        return "[" + now.format(dateTimeFormatter) + "] ";
    }

    public static void bundle(Player player, String value){
        JSONObject db = getData(player.uuid);
        if (db.get("country_code") == "KR") {
            player.sendMessage(EssentialBundle.load(true, value));
        } else {
            player.sendMessage(EssentialBundle.load(false, value));
        }
    }
}
