package essentials;

import io.anuke.arc.Core;
import io.anuke.arc.util.Log;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.gen.Call;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimerTask;

import static essentials.EssentialConfig.antirushtime;
import static essentials.EssentialConfig.enableantirush;
import static essentials.EssentialPlayer.getData;
import static essentials.EssentialPlayer.writeData;
import static essentials.Global.printStackTrace;
import static io.anuke.mindustry.Vars.*;

public class EssentialTimer extends TimerTask implements Runnable{
    public static String playtime;
    public static String uptime;

    @Override
    public void run() {
        // Player playtime counting
        try{
            if(playerGroup.size() > 0){
                for(int i = 0; i < playerGroup.size(); i++){
                    Player player = playerGroup.all().get(i);
                    if(!Vars.state.teams.get(player.getTeam()).cores.isEmpty()){
                        JSONObject db = new JSONObject();
                        try {
                            db = getData(player.uuid);
                        }catch (Exception e){
                            printStackTrace(e);
                        }
                        String data;
                        if(db.has("playtime")){
                            data = db.getString("playtime");
                        } else {
                            return;
                        }
                        SimpleDateFormat format = new SimpleDateFormat("HH:mm.ss");
                        Date d1;
                        Calendar cal;
                        String newTime = null;
                        try {
                            d1 = format.parse(data);
                            cal = Calendar.getInstance();
                            cal.setTime(d1);
                            cal.add(Calendar.SECOND, 1);
                            newTime = format.format(cal.getTime());
                        } catch (ParseException e1) {
                            printStackTrace(e1);
                        }

                        // Exp caculating
                        int exp = db.getInt("exp");
                        int newexp = exp+(int)(Math.random()*5);

                        writeData("UPDATE players SET exp = '"+newexp+"', playtime = '"+newTime+"' WHERE uuid = '"+player.uuid+"'");

                        EssentialExp.exp(player.name, player.uuid);
                    }
                }
            }
        }catch (Exception ex){
            printStackTrace(ex);
        }

        // Temporarily ban players time counting
        try{
            String db = Core.settings.getDataDirectory().child("plugins/Essentials/banned.json").readString();
            JSONTokener parser = new JSONTokener(db);
            JSONArray object = new JSONArray(parser);

            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd a hh:mm.ss", Locale.ENGLISH);
            String myTime = now.format(dateTimeFormatter);

            for (int i = 0; i < object.length(); i++) {
                JSONObject value1 = object.getJSONObject(i);
                String date = (String) value1.get("date");
                String uuid = (String) value1.get("uuid");
                String name = (String) value1.get("name");

                if (date.equals(myTime)) {
                    Log.info(myTime);
                    object.remove(i);
                    Core.settings.getDataDirectory().child("plugins/Essentials/banned.json").writeString(String.valueOf(object));
                    netServer.admins.unbanPlayerID(uuid);
                    Global.log("[" + myTime + "] [Bantime]" + name + "/" + uuid + " player unbanned!");
                }
            }
        }catch (Exception ex){
            printStackTrace(ex);
        }

        // Map playtime counting
        if(playtime != null){
            try{
                Calendar cal1;
                SimpleDateFormat format = new SimpleDateFormat("HH:mm.ss");
                Date d2 = format.parse(playtime);
                cal1 = Calendar.getInstance();
                cal1.setTime(d2);
                cal1.add(Calendar.SECOND, 1);
                playtime = format.format(cal1.getTime());
                // Anti PvP rushing timer
                if(enableantirush && Vars.state.rules.pvp && cal1.equals(antirushtime)) {
                    Call.sendMessage("[green] Peace time is over!");
                    Call.sendMessage("[green] You can now attack other teams!");
                    for(int i = 0; i < playerGroup.size(); i++) {
                        Player player = playerGroup.all().get(i);
                        Call.onPlayerDeath(player);
                    }
                    state.rules.playerDamageMultiplier = 1f;
                    state.rules.playerHealthMultiplier = 1f;
                }
            }catch (Exception e){
                printStackTrace(e);
            }
        }

        // Server uptime counting
        if(uptime != null){
            try{
                Calendar cal1;
                SimpleDateFormat format = new SimpleDateFormat("HH:mm.ss");
                Date d2 = format.parse(uptime);
                cal1 = Calendar.getInstance();
                cal1.setTime(d2);
                cal1.add(Calendar.SECOND, 1);
                uptime = format.format(cal1.getTime());
            }catch (Exception e){
                printStackTrace(e);
            }
        }

        /*if (playerGroup.size() > 0) {
            Thread work = new Thread(() -> {
                for(int i=0;i<jumpzone.length();i++){
                    String jumpdata = jumpzone.getString(i);
                    String[] data = jumpdata.split("/");
                    int startx = Integer.parseInt(data[0]);
                    int starty = Integer.parseInt(data[1]);
                    int tilex = Integer.parseInt(data[2]);
                    int tiley = Integer.parseInt(data[3]);
                    String serverip = data[4];
                    int serverport = Integer.parseInt(data[5]);
                    int block = Integer.parseInt(data[6]);

                    Block target;
                    switch(block){
                        case 1:
                        default:
                            target = Blocks.metalFloor;
                            break;
                        case 2:
                            target = Blocks.metalFloor2;
                            break;
                        case 3:
                            target = Blocks.metalFloor3;
                            break;
                        case 4:
                            target = Blocks.metalFloor5;
                            break;
                        case 5:
                            target = Blocks.metalFloorDamaged;
                            break;
                    }

                    if(!world.tile(startx, starty).block().name.matches(".*metal.*")){
                        int size = tilex - startx;
                        for(int x=0;x<size;x++){
                            for(int y=0;y<size;y++){
                                Tile tile = world.tile(startx+x, starty+y);
                                Call.onConstructFinish(tile, target, 0, (byte) 0, Team.sharded, false);
                            }
                        }
                    }

                    for (int ix = 0; ix < playerGroup.size(); ix++) {
                        Player player = playerGroup.all().get(ix);
                        if(player.tileX()>startx && player.tileX()<tilex){
                            if(player.tileY()>starty && player.tileY()<tiley){
                                Call.onConnect(player.con, serverip, serverport);
                            }
                        }
                    }
                }
            });
            work.start();
        }*/
    }
}