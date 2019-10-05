package essentials;

import io.anuke.arc.Core;
import io.anuke.arc.util.Log;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.gen.Call;
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
import static io.anuke.mindustry.Vars.*;

public class EssentialTimer extends TimerTask {
    public static String playtime;

    public void run() {
        // Player playtime counting
        if(playerGroup.size() > 0){
            for(int i = 0; i < playerGroup.size(); i++){
                Player player = playerGroup.all().get(i);
                JSONObject db = new JSONObject();
                try {
                    db = getData(player.uuid);
                }catch (Exception ignored){}

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
                    e1.printStackTrace();
                }

                // Exp caculating
                int exp = (int) db.get("exp");
                int newexp = exp+(int)(Math.random()*5);

                writeData("UPDATE players SET exp = '"+newexp+"', playtime = '"+newTime+"' WHERE uuid = '"+player.uuid+"'");

                EssentialExp.exp(player.name, player.uuid);
            }
        }

        // Temporarily ban players time counting
        String db = Core.settings.getDataDirectory().child("plugins/Essentials/banned.json").readString();
        JSONTokener parser = new JSONTokener(db);
        JSONObject object = new JSONObject(parser);

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd a hh:mm.ss", Locale.ENGLISH);
        String myTime = now.format(dateTimeFormatter);

        for (int i = 0; i < object.length(); i++) {
            JSONObject value1 = (JSONObject) object.get(String.valueOf(i));
            String date = (String) value1.get("date");
            String uuid = (String) value1.get("uuid");
            String name = (String) value1.get("name");

            if (date.equals(myTime)) {
                Log.info(myTime);
                object.remove(String.valueOf(i));
                Core.settings.getDataDirectory().child("plugins/Essentials/banned.json").writeString(String.valueOf(object));
                netServer.admins.unbanPlayerID(uuid);
                Global.log(name + "/" + uuid + " player unbanned!");
            }
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
                    Call.sendMessage("Peace time is over!");
                    state.rules.playerDamageMultiplier = 1f;
                    state.rules.playerHealthMultiplier = 1f;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
