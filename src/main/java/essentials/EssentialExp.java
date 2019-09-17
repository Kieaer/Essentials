package essentials;

import io.anuke.arc.Core;
import io.anuke.mindustry.gen.Call;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class EssentialExp {
    private static final double BASE_XP = 500;
    private static final double EXPONENT = 1.08f;
    static String url = "jdbc:sqlite:"+Core.settings.getDataDirectory().child("plugins/Essentials/player.sqlite3");

    public static void exp(String name, String uuid) {
        JSONObject db = EssentialPlayer.getData(uuid);

        int currentlevel = (int) db.get("level");
        int max = (int) calculateFullTargetXp(currentlevel);

        int xp = (int) db.get("exp");
        int levelXp = max - xp;
        int level = calculateLevel(xp);
        String reqtotalexp = xp+"("+(int) Math.floor(levelXp)+") / "+(int) Math.floor(max);

        String sql = "UPDATE players SET exp = ?, reqexp = ?, level = ?, reqtotalexp = ? WHERE uuid = ?";

        try{
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection(url);

            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, xp);
            pstmt.setInt(2, (int) Math.floor(max));
            pstmt.setInt(3, level);
            pstmt.setString(4, reqtotalexp);
            pstmt.setString(5, uuid);
            pstmt.executeUpdate();
            pstmt.close();
            conn.close();
        } catch (Exception e){
            e.printStackTrace();
        }

        int curlevel = (int) db.get("level");
        if(curlevel < level){
            Call.sendMessage("[yellow]Congratulations![white] "+name+"[white] achieved level [green]"+level+"!");
        }
    }

    private static double calcXpForLevel(int level) {
        return BASE_XP + (BASE_XP * Math.pow(level, EXPONENT));
    }

    private static double calculateFullTargetXp(int level) {
        double requiredXP = 0;
        for (int i = 0; i <= level; i++) {
            requiredXP += calcXpForLevel(i);
        }
        return requiredXP;
    }

    private static int calculateLevel(double xp) {
        int level = 0;
        double maxXp = calcXpForLevel(0);
        do {
            maxXp += calcXpForLevel(++level);
        } while (maxXp < xp);
        return level;
    }

    public static void joinexp(String uuid){
        JSONObject db = EssentialPlayer.getData(uuid);

        int exp = (int) db.get("exp");
        int joincount = (int) db.get("joincount");

        db.put("exp", exp+joincount);

        String json = db.toString();
        Core.settings.getDataDirectory().child("plugins/Essentials/players/"+uuid+".json").writeString(json);
    };
}

