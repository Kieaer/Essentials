package essentials;

import io.anuke.arc.Core;
import io.anuke.arc.util.Log;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.world.Tile;
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
import static io.anuke.mindustry.Vars.world;

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

    public static void getcount(Tile tile, int count){
        String[] zero = {"0.0","0.1","0.2","0.3","0.4","1.4","2.4","2.3","2.2","2.1","2.0","1.0"};
        String[] one = {"0.0","0.1","0.2","0.3","0.4"};
        String[] two = {"0.4","1.4","2.4","2.3","2.2","1.2","0.2","0.1","0.0","1.0","2.0"};
        String[] three = {"0.4","1.4","2.4","2.3","0.2","1.2","2.2","2.1","0.0","1.0","2.0"};
        String[] four = {"0.4","0.3","0.2","1.2","2.4","2.3","2.2","2.1","2.0"};
        String[] five = {"0.4","1.4","2.4","0.3","0.2","1.2","2.2","2.1","0.0","1.0","2.0"};
        String[] six = {"0.0","0.1","0.2","0.3","0.4","1.0","2.0","1.2","2.2","2.1"};
        String[] seven = {"0.4","1.4","2.4","2.3","2.2","2.1","2.0"};
        String[] eight = {"0.0","0.1","0.2","0.3","0.4","1.0","2.0","2.1","1.2","2.2","2.3","1.4","2.4"};
        String[] nine = {"0.2","0.3","0.4","2.0","2.1","2.2","2.3","2.4","1.2","1.4"};
        switch(count) {
            case 0:
                for (String s : zero) {
                    String[] split = s.split("\\.");
                    int x = Integer.parseInt(split[0]);
                    int y = Integer.parseInt(split[1]);
                    Tile target = world.tile(tile.x + x, tile.y + y);
                    if (target.block() != Blocks.plastaniumWall) {
                        Call.onConstructFinish(world.tile(tile.x + x, tile.y + y), Blocks.plastaniumWall, 0, (byte) 0, Team.sharded, true);
                    }
                }
                break;
            case 1:
                for (String s : one) {
                    String[] split = s.split("\\.");
                    int x = Integer.parseInt(split[0]);
                    int y = Integer.parseInt(split[1]);
                    Tile target = world.tile(tile.x + x, tile.y + y);
                    if (target.block() != Blocks.plastaniumWall) {
                        Call.onConstructFinish(world.tile(tile.x + x, tile.y + y), Blocks.plastaniumWall, 0, (byte) 0, Team.sharded, true);
                    }
                }
                break;
            case 2:
                for (String s : two) {
                    String[] split = s.split("\\.");
                    int x = Integer.parseInt(split[0]);
                    int y = Integer.parseInt(split[1]);
                    Tile target = world.tile(tile.x + x, tile.y + y);
                    if (target.block() != Blocks.plastaniumWall) {
                        Call.onConstructFinish(world.tile(tile.x + x, tile.y + y), Blocks.plastaniumWall, 0, (byte) 0, Team.sharded, true);
                    }
                }
                break;
            case 3:
                for (String s : three) {
                    String[] split = s.split("\\.");
                    int x = Integer.parseInt(split[0]);
                    int y = Integer.parseInt(split[1]);
                    Tile target = world.tile(tile.x + x, tile.y + y);
                    if (target.block() != Blocks.plastaniumWall) {
                        Call.onConstructFinish(world.tile(tile.x + x, tile.y + y), Blocks.plastaniumWall, 0, (byte) 0, Team.sharded, true);
                    }
                }
                break;
            case 4:
                for (String s : four) {
                    String[] split = s.split("\\.");
                    int x = Integer.parseInt(split[0]);
                    int y = Integer.parseInt(split[1]);
                    Tile target = world.tile(tile.x + x, tile.y + y);
                    if (target.block() != Blocks.plastaniumWall) {
                        Call.onConstructFinish(world.tile(tile.x + x, tile.y + y), Blocks.plastaniumWall, 0, (byte) 0, Team.sharded, true);
                    }
                }
                break;
            case 5:
                for (String s : five) {
                    String[] split = s.split("\\.");
                    int x = Integer.parseInt(split[0]);
                    int y = Integer.parseInt(split[1]);
                    Tile target = world.tile(tile.x + x, tile.y + y);
                    if (target.block() != Blocks.plastaniumWall) {
                        Call.onConstructFinish(world.tile(tile.x + x, tile.y + y), Blocks.plastaniumWall, 0, (byte) 0, Team.sharded, true);
                    }
                }
                break;
            case 6:
                for (String s : six) {
                    String[] split = s.split("\\.");
                    int x = Integer.parseInt(split[0]);
                    int y = Integer.parseInt(split[1]);
                    Tile target = world.tile(tile.x + x, tile.y + y);
                    if (target.block() != Blocks.plastaniumWall) {
                        Call.onConstructFinish(world.tile(tile.x + x, tile.y + y), Blocks.plastaniumWall, 0, (byte) 0, Team.sharded, true);
                    }
                }
                break;
            case 7:
                for (String s : seven) {
                    String[] split = s.split("\\.");
                    int x = Integer.parseInt(split[0]);
                    int y = Integer.parseInt(split[1]);
                    Tile target = world.tile(tile.x + x, tile.y + y);
                    if (target.block() != Blocks.plastaniumWall) {
                        Call.onConstructFinish(world.tile(tile.x + x, tile.y + y), Blocks.plastaniumWall, 0, (byte) 0, Team.sharded, true);
                    }
                }
                break;
            case 8:
                for (String s : eight) {
                    String[] split = s.split("\\.");
                    int x = Integer.parseInt(split[0]);
                    int y = Integer.parseInt(split[1]);
                    Tile target = world.tile(tile.x + x, tile.y + y);
                    if (target.block() != Blocks.plastaniumWall) {
                        Call.onConstructFinish(world.tile(tile.x + x, tile.y + y), Blocks.plastaniumWall, 0, (byte) 0, Team.sharded, true);
                    }
                }
                break;
            case 9:
                for (String s : nine) {
                    String[] split = s.split("\\.");
                    int x = Integer.parseInt(split[0]);
                    int y = Integer.parseInt(split[1]);
                    Tile target = world.tile(tile.x + x, tile.y + y);
                    if (target.block() != Blocks.plastaniumWall) {
                        Call.onConstructFinish(world.tile(tile.x + x, tile.y + y), Blocks.plastaniumWall, 0, (byte) 0, Team.sharded, true);
                    }
                }
                break;
        }
    }

    public static boolean validcount(Tile tile, int count){
        String[] zero = {"0.0","0.1","0.2","0.3","0.4","1.4","2.4","2.3","2.2","2.1","2.0","1.0"};
        String[] one = {"0.0","0.1","0.2","0.3","0.4"};
        String[] two = {"0.4","1.4","2.4","2.3","2.2","1.2","0.2","0.1","0.0","1.0","2.0"};
        String[] three = {"0.4","1.4","2.4","2.3","0.2","1.2","2.2","2.1","0.0","1.0","2.0"};
        String[] four = {"0.4","0.3","0.2","1.2","2.4","2.3","2.2","2.1","2.0"};
        String[] five = {"0.4","1.4","2.4","0.3","0.2","1.2","2.2","2.1","0.0","1.0","2.0"};
        String[] six = {"0.0","0.1","0.2","0.3","0.4","1.0","2.0","1.2","2.2","2.1"};
        String[] seven = {"0.4","1.4","2.4","2.3","2.2","2.1","2.0"};
        String[] eight = {"0.0","0.1","0.2","0.3","0.4","1.0","2.0","2.1","1.2","2.2","2.3","1.4","2.4"};
        String[] nine = {"0.2","0.3","0.4","2.0","2.1","2.2","2.3","2.4","1.2","1.4"};
        boolean result = false;
        switch(count){
            case 0:
                for (String s : zero) {
                    String[] split = s.split("\\.");
                    int x = Integer.parseInt(split[0]);
                    int y = Integer.parseInt(split[1]);
                    Tile target = world.tile(tile.x + x, tile.y + y);
                    if (target.block() != Blocks.plastaniumWall) {
                        result = true;
                        break;
                    }
                }
                break;
            case 1:
                for (String s : one) {
                    String[] split = s.split("\\.");
                    int x = Integer.parseInt(split[0]);
                    int y = Integer.parseInt(split[1]);
                    Tile target = world.tile(tile.x + x, tile.y + y);
                    if (target.block() != Blocks.plastaniumWall) {
                        result = true;
                        break;
                    }
                }
                break;
            case 2:
                for (String s : two) {
                    String[] split = s.split("\\.");
                    int x = Integer.parseInt(split[0]);
                    int y = Integer.parseInt(split[1]);
                    Tile target = world.tile(tile.x + x, tile.y + y);
                    if (target.block() != Blocks.plastaniumWall) {
                        result = true;
                        break;
                    }
                }
                break;
            case 3:
                for (String s : three) {
                    String[] split = s.split("\\.");
                    int x = Integer.parseInt(split[0]);
                    int y = Integer.parseInt(split[1]);
                    Tile target = world.tile(tile.x + x, tile.y + y);
                    if (target.block() != Blocks.plastaniumWall) {
                        result = true;
                        break;
                    }
                }
                break;
            case 4:
                for (String s : four) {
                    String[] split = s.split("\\.");
                    int x = Integer.parseInt(split[0]);
                    int y = Integer.parseInt(split[1]);
                    Tile target = world.tile(tile.x + x, tile.y + y);
                    if (target.block() != Blocks.plastaniumWall) {
                        result = true;
                        break;
                    }
                }
                break;
            case 5:
                for (String s : five) {
                    String[] split = s.split("\\.");
                    int x = Integer.parseInt(split[0]);
                    int y = Integer.parseInt(split[1]);
                    Tile target = world.tile(tile.x + x, tile.y + y);
                    if (target.block() != Blocks.plastaniumWall) {
                        result = true;
                        break;
                    }
                }
                break;
            case 6:
                for (String s : six) {
                    String[] split = s.split("\\.");
                    int x = Integer.parseInt(split[0]);
                    int y = Integer.parseInt(split[1]);
                    Tile target = world.tile(tile.x + x, tile.y + y);
                    if (target.block() != Blocks.plastaniumWall) {
                        result = true;
                        break;
                    }
                }
                break;
            case 7:
                for (String s : seven) {
                    String[] split = s.split("\\.");
                    int x = Integer.parseInt(split[0]);
                    int y = Integer.parseInt(split[1]);
                    Tile target = world.tile(tile.x + x, tile.y + y);
                    if (target.block() != Blocks.plastaniumWall) {
                        result = true;
                        break;
                    }
                }
                break;
            case 8:
                for (String s : eight) {
                    String[] split = s.split("\\.");
                    int x = Integer.parseInt(split[0]);
                    int y = Integer.parseInt(split[1]);
                    Tile target = world.tile(tile.x + x, tile.y + y);
                    if (target.block() != Blocks.plastaniumWall) {
                        result = true;
                        break;
                    }
                }
                break;
            case 9:
                for (String s : nine) {
                    String[] split = s.split("\\.");
                    int x = Integer.parseInt(split[0]);
                    int y = Integer.parseInt(split[1]);
                    Tile target = world.tile(tile.x + x, tile.y + y);
                    if (target.block() != Blocks.plastaniumWall) {
                        result = true;
                        break;
                    }
                }
                break;
        }
        return result;
    }
}
