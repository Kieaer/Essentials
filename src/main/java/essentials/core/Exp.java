package essentials.core;

import io.anuke.mindustry.gen.Call;
import org.json.JSONObject;

import static essentials.core.PlayerDB.writeData;
import static essentials.utils.Config.*;

public class Exp {
    private static final double BASE_XP = basexp;
    private static final double EXPONENT = exponent;

    public static void exp(String name, String uuid) {
        JSONObject db = PlayerDB.getData(uuid);

        int currentlevel = db.getInt("level");
        int max = (int) calculateFullTargetXp(currentlevel);

        int xp =  db.getInt("exp");
        int levelXp = max - xp;
        int level = calculateLevel(xp);
        int reqexp = (int)Math.floor(max);
        String reqtotalexp = xp+"("+(int) Math.floor(levelXp)+") / "+(int) Math.floor(max);

        writeData("UPDATE players SET reqexp = '"+reqexp+"', level = '"+level+"', reqtotalexp = '"+reqtotalexp+"' WHERE uuid = '"+uuid+"'");

        int curlevel = (int) db.get("level");
        if(curlevel < level && curlevel > 20 && levelupalarm){
            Call.sendMessage("[yellow]Congratulations![white] "+name+"[white] achieved level [green]"+level+"!");
        }
    }

    private static double calcXpForLevel(int level) {
        return BASE_XP+(BASE_XP * Math.pow(level, EXPONENT));
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
        JSONObject db = PlayerDB.getData(uuid);

        int exp = (int) db.get("exp");
        int joincount = (int) db.get("joincount");

        int result = exp+joincount;

        writeData("UPDATE players SET exp = '"+result+"' WHERE uuid = '"+uuid+"'");
    }
}

