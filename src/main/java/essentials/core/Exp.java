package essentials.core;

import mindustry.entities.type.Player;

import static essentials.Global.bundle;
import static essentials.Global.config;
import static essentials.core.PlayerDB.PlayerData;
import static essentials.core.PlayerDB.PlayerDataSet;
import static mindustry.Vars.playerGroup;

public class Exp {
    private static double BASE_XP = config.getBasexp();
    private static double EXPONENT = config.getExponent();

    public static void exp(String name, String uuid) {
        PlayerData target = PlayerData(uuid);

        int currentlevel = target.level;
        int max = (int) calculateFullTargetXp(currentlevel);

        int xp =  target.exp;
        int levelXp = max - xp;
        int level = calculateLevel(xp);
        int reqexp = (int)Math.floor(max);
        String reqtotalexp = xp+"("+(int) Math.floor(levelXp)+") / "+(int) Math.floor(max);

        target.reqexp = reqexp;
        target.level = level;
        target.reqtotalexp = reqtotalexp;

        if(currentlevel < level && currentlevel > config.getAlarmlevel() && config.isLevelupalarm()){
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
        PlayerData target = PlayerData(uuid);
        target.exp = target.exp+target.joincount;
        PlayerDataSet(uuid,target);
    }
}

