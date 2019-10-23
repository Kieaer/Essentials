package essentials;

import io.anuke.arc.Core;
import io.anuke.arc.util.Log;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.game.Team;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

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

    public static void printStackTrace(Throwable e) {
        if(!e.getMessage().equals("Connection refused: connect")){
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
        }
    }

    public static String gettime(){
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm.ss", Locale.ENGLISH);
        return "[" + now.format(dateTimeFormatter) + "] ";
    }

    public static boolean ipmatches(String ip, String subnet) {
        IpAddressMatcher ipAddressMatcher = new IpAddressMatcher(subnet);
        return ipAddressMatcher.matches(ip);
    }
}
