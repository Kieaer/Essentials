package essentials.core;

import essentials.utils.Config;
import io.anuke.mindustry.entities.type.Player;
import org.json.JSONObject;

import static essentials.Global.bundle;
import static essentials.core.PlayerDB.writeData;
import static io.anuke.mindustry.Vars.playerGroup;

public class Exp {
    public static Config config = new Config();
    private static double BASE_XP = config.getBasexp();
    private static double EXPONENT = config.getExponent();

    public static void exp(String name, String uuid) {
        JSONObject db = PlayerDB.getData(uuid);

        int currentlevel = db.getInt("level");
        int max = (int) calculateFullTargetXp(currentlevel);

        int xp =  db.getInt("exp");
        int levelXp = max - xp;
        int level = calculateLevel(xp);
        int reqexp = (int)Math.floor(max);
        String reqtotalexp = xp+"("+(int) Math.floor(levelXp)+") / "+(int) Math.floor(max);

        writeData("UPDATE players SET reqexp = ?, level = ?, reqtotalexp = ? WHERE uuid = ?",reqexp,level,reqtotalexp,uuid);

        int curlevel = (int) db.get("level");
        if(curlevel < level && curlevel > config.getAlarmlevel() && config.isLevelupalarm()){
            for(int a=0;a<playerGroup.size();a++){
                Player player = playerGroup.all().get(a);
                player.sendMessage(bundle(player, "player-levelup", name, level));
            }
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

        int exp = db.getInt("exp");
        int joincount = db.getInt("joincount");

        int result = exp+joincount;

        writeData("UPDATE players SET exp = ? WHERE uuid = ?",result,uuid);
    }
}

