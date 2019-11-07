package essentials;

import essentials.utils.Bundle;
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

import static essentials.core.PlayerDB.getData;
import static essentials.utils.Config.debug;
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
        if (db.getString("country_code").contains("KR")) {
            player.sendMessage(Bundle.load(true, value));
        } else {
            player.sendMessage(Bundle.load(false, value));
        }
    }

    public static void setcount(Tile tile, int count){
        String[] pos = {"0,4","1,4","2,4","0,3","1,3","2,3","0,2","1,2","2,2","0,1","1,1","2,1","0,0","1,0","2,0"};
        int[] zero = {1,1,1,1,0,1,1,0,1,1,0,1,1,1,1};
        int[] one = {0,1,0,1,1,0,0,1,0,0,1,0,1,1,1};
        int[] two = {1,1,1,0,0,1,1,1,1,1,0,0,1,1,1};
        int[] three = {1,1,1,0,0,1,1,1,1,0,0,1,1,1,1};
        int[] four = {1,0,1,1,0,1,1,1,1,0,0,1,0,0,1};
        int[] five = {1,1,1,1,0,0,1,1,1,0,0,1,1,1,1};
        int[] six = {1,1,1,1,0,0,1,1,1,1,0,1,1,1,1};
        int[] seven = {1,1,1,1,0,1,0,0,1,0,0,1,0,0,1};
        int[] eight = {1,1,1,1,0,1,1,1,1,1,0,1,1,1,1};
        int[] nine = {1,1,1,1,0,1,1,1,1,0,0,1,1,1,1};

        switch(count) {
            case 0:
                for(int a=0;a<15;a++){
                    String position = pos[a];
                    String[] data = position.split(",");
                    int x = Integer.parseInt(data[0]);
                    int y = Integer.parseInt(data[1]);
                    Tile target = world.tile(tile.x, tile.y);
                    if(zero[a] == 1) {
                        if(world.tile(target.x+x, target.y+y).block() != Blocks.plastaniumWall){
                            Call.onConstructFinish(world.tile(target.x+x, target.y+y), Blocks.plastaniumWall, 0, (byte) 0, Team.sharded, true);
                        }
                    } else if(zero[a] == 0){
                        if(world.tile(target.x+x, target.y+y).block() == Blocks.plastaniumWall){
                            Call.onDeconstructFinish(world.tile(target.x+x,target.y+y), Blocks.air, 0);
                        }
                    }
                }
                break;
            case 1:
                for(int a=0;a<15;a++){
                    String position = pos[a];
                    String[] data = position.split(",");
                    int x = Integer.parseInt(data[0]);
                    int y = Integer.parseInt(data[1]);
                    Tile target = world.tile(tile.x, tile.y);
                    if(one[a] == 1){
                        if(world.tile(target.x+x, target.y+y).block() != Blocks.plastaniumWall){
                            Call.onConstructFinish(world.tile(target.x+x, target.y+y), Blocks.plastaniumWall, 0, (byte) 0, Team.sharded, true);
                        }
                    } else if(one[a] == 0){
                        if(world.tile(target.x+x, target.y+y).block() == Blocks.plastaniumWall){
                            Call.onDeconstructFinish(world.tile(target.x+x,target.y+y), Blocks.air, 0);
                        }
                    }
                }
                break;
            case 2:
                for(int a=0;a<15;a++){
                    String position = pos[a];
                    String[] data = position.split(",");
                    int x = Integer.parseInt(data[0]);
                    int y = Integer.parseInt(data[1]);
                    Tile target = world.tile(tile.x, tile.y);
                    if(two[a] == 1){
                        if(world.tile(target.x+x, target.y+y).block() != Blocks.plastaniumWall){
                            Call.onConstructFinish(world.tile(target.x+x, target.y+y), Blocks.plastaniumWall, 0, (byte) 0, Team.sharded, true);
                        }
                    } else if(two[a] == 0){
                        if(world.tile(target.x+x, target.y+y).block() == Blocks.plastaniumWall){
                            Call.onDeconstructFinish(world.tile(target.x+x,target.y+y), Blocks.air, 0);
                        }
                    }
                }
                break;
            case 3:
                for(int a=0;a<15;a++){
                    String position = pos[a];
                    String[] data = position.split(",");
                    int x = Integer.parseInt(data[0]);
                    int y = Integer.parseInt(data[1]);
                    Tile target = world.tile(tile.x, tile.y);
                    if(three[a] == 1){
                        if(world.tile(target.x+x, target.y+y).block() != Blocks.plastaniumWall){
                            Call.onConstructFinish(world.tile(target.x+x, target.y+y), Blocks.plastaniumWall, 0, (byte) 0, Team.sharded, true);
                        }
                    } else if(three[a] == 0){
                        if(world.tile(target.x+x, target.y+y).block() == Blocks.plastaniumWall){
                            Call.onDeconstructFinish(world.tile(target.x+x,target.y+y), Blocks.air, 0);
                        }
                    }
                }
                break;
            case 4:
                for(int a=0;a<15;a++){
                    String position = pos[a];
                    String[] data = position.split(",");
                    int x = Integer.parseInt(data[0]);
                    int y = Integer.parseInt(data[1]);
                    Tile target = world.tile(tile.x, tile.y);
                    if(four[a] == 1){
                        if(world.tile(target.x+x, target.y+y).block() != Blocks.plastaniumWall){
                            Call.onConstructFinish(world.tile(target.x+x, target.y+y), Blocks.plastaniumWall, 0, (byte) 0, Team.sharded, true);
                        }
                    } else if(four[a] == 0){
                        if(world.tile(target.x+x, target.y+y).block() == Blocks.plastaniumWall){
                            Call.onDeconstructFinish(world.tile(target.x+x,target.y+y), Blocks.air, 0);
                        }
                    }
                }
                break;
            case 5:
                for(int a=0;a<15;a++){
                    String position = pos[a];
                    String[] data = position.split(",");
                    int x = Integer.parseInt(data[0]);
                    int y = Integer.parseInt(data[1]);
                    Tile target = world.tile(tile.x, tile.y);
                    if(five[a] == 1){
                        if(world.tile(target.x+x, target.y+y).block() != Blocks.plastaniumWall){
                            Call.onConstructFinish(world.tile(target.x+x, target.y+y), Blocks.plastaniumWall, 0, (byte) 0, Team.sharded, true);
                        }
                    } else if(five[a] == 0){
                        if(world.tile(target.x+x, target.y+y).block() == Blocks.plastaniumWall){
                            Call.onDeconstructFinish(world.tile(target.x+x,target.y+y), Blocks.air, 0);
                        }
                    }
                }
                break;
            case 6:
                for(int a=0;a<15;a++){
                    String position = pos[a];
                    String[] data = position.split(",");
                    int x = Integer.parseInt(data[0]);
                    int y = Integer.parseInt(data[1]);
                    Tile target = world.tile(tile.x, tile.y);
                    if(six[a] == 1){
                        if(world.tile(target.x+x, target.y+y).block() != Blocks.plastaniumWall){
                            Call.onConstructFinish(world.tile(target.x+x, target.y+y), Blocks.plastaniumWall, 0, (byte) 0, Team.sharded, true);
                        }
                    } else if(six[a] == 0){
                        if(world.tile(target.x+x, target.y+y).block() == Blocks.plastaniumWall){
                            Call.onDeconstructFinish(world.tile(target.x+x,target.y+y), Blocks.air, 0);
                        }
                    }
                }
                break;
            case 7:
                for(int a=0;a<15;a++){
                    String position = pos[a];
                    String[] data = position.split(",");
                    int x = Integer.parseInt(data[0]);
                    int y = Integer.parseInt(data[1]);
                    Tile target = world.tile(tile.x, tile.y);
                    if(seven[a] == 1){
                        if(world.tile(target.x+x, target.y+y).block() != Blocks.plastaniumWall){
                            Call.onConstructFinish(world.tile(target.x+x, target.y+y), Blocks.plastaniumWall, 0, (byte) 0, Team.sharded, true);
                        }
                    } else if(seven[a] == 0){
                        if(world.tile(target.x+x, target.y+y).block() == Blocks.plastaniumWall){
                            Call.onDeconstructFinish(world.tile(target.x+x,target.y+y), Blocks.air, 0);
                        }
                    }
                }
                break;
            case 8:
                for(int a=0;a<15;a++){
                    String position = pos[a];
                    String[] data = position.split(",");
                    int x = Integer.parseInt(data[0]);
                    int y = Integer.parseInt(data[1]);
                    Tile target = world.tile(tile.x, tile.y);
                    if(eight[a] == 1){
                        if(world.tile(target.x+x, target.y+y).block() != Blocks.plastaniumWall){
                            Call.onConstructFinish(world.tile(target.x+x, target.y+y), Blocks.plastaniumWall, 0, (byte) 0, Team.sharded, true);
                        }
                    } else if(eight[a] == 0){
                        if(world.tile(target.x+x, target.y+y).block() == Blocks.plastaniumWall){
                            Call.onDeconstructFinish(world.tile(target.x+x,target.y+y), Blocks.air, 0);
                        }
                    }
                }
                break;
            case 9:
                for(int a=0;a<15;a++){
                    String position = pos[a];
                    String[] data = position.split(",");
                    int x = Integer.parseInt(data[0]);
                    int y = Integer.parseInt(data[1]);
                    Tile target = world.tile(tile.x, tile.y);
                    if(nine[a] == 1){
                        if(world.tile(target.x+x, target.y+y).block() != Blocks.plastaniumWall){
                            Call.onConstructFinish(world.tile(target.x+x, target.y+y), Blocks.plastaniumWall, 0, (byte) 0, Team.sharded, true);
                        }
                    } else if(nine[a] == 0){
                        if(world.tile(target.x+x, target.y+y).block() == Blocks.plastaniumWall){
                            Call.onDeconstructFinish(world.tile(target.x+x,target.y+y), Blocks.air, 0);
                        }
                    }
                }
                break;
        }
    }
}
