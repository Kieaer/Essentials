package essentials;

import io.anuke.arc.Core;
import io.anuke.mindustry.gen.Call;
import org.json.JSONObject;
import org.json.JSONTokener;

public class EssentialExp {
    private static final double BASE_XP = 500;
    private static final double EXPONENT = 1.08f;

    public static void exp(String name, String uuid) {
        JSONObject db = EssentialPlayer.getData(uuid);

        int currentlevel = (int) db.get("level");
        int max = (int) calculateFullTargetXp(currentlevel);

        int xp = (int) db.get("exp");
        int levelXp = max - xp;
        int level = calculateLevel(xp);
        String reqtotalexp = xp+"("+(int) Math.floor(levelXp)+") / "+(int) Math.floor(max);

        String playerdb = Core.settings.getDataDirectory().child("plugins/Essentials/players/"+uuid+".json").readString();
        JSONTokener pparse = new JSONTokener(playerdb);
        JSONObject db2 = new JSONObject(pparse);

        db2.put("xp", xp);
        db2.put("reqexp", (int) Math.floor(max));
        db2.put("level", level);
        db2.put("reqtotalexp", reqtotalexp);
        int curlevel = (int) db.get("level");
        if(curlevel < level){
            Call.sendMessage("[green]Congratulations! "+name+"[green] achieved level "+level+"!");
        }
        Core.settings.getDataDirectory().child("plugins/Essentials/players/"+uuid+".json").writeString(String.valueOf(db2));
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

