package essentials;

import io.anuke.arc.Core;
import io.anuke.arc.util.Log;
import io.anuke.mindustry.entities.type.Player;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static essentials.EssentialPlayer.getData;
import static io.anuke.mindustry.Vars.netServer;
import static io.anuke.mindustry.Vars.playerGroup;

public class EssentialTimer {
    static String url = "jdbc:sqlite:"+Core.settings.getDataDirectory().child("plugins/Essentials/player.sqlite3");
    static void main(){
        // Player playtime counting
        if(playerGroup.size() > 0){
            for(int i = 0; i < playerGroup.size(); i++){
                Player p = playerGroup.all().get(i);
                JSONObject db = getData(p.uuid);

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
                int newexp = exp+(int)(Math.random()*5)+(int)db.get("level");

                String sql = "UPDATE players SET exp = ?, playtime = ? WHERE uuid = ?";
                try{
                    Class.forName("org.sqlite.JDBC");
                    Connection conn = DriverManager.getConnection(url);

                    PreparedStatement pstmt = conn.prepareStatement(sql);
                    pstmt.setInt(1, newexp);
                    pstmt.setString(2, newTime);
                    pstmt.setString(3, p.uuid);
                    pstmt.executeUpdate();
                    pstmt.close();
                    conn.close();
                } catch (Exception e){
                    e.printStackTrace();
                }

                EssentialExp.exp(p.name, p.uuid);
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
                Log.info("[Essentials] " + name + "/" + uuid + " player unbanned!");
            }
        }

        // todo make dynmap for mindustry (extreme hard work)
        //Dynmap.takeMapScreenshot();
    }
}
