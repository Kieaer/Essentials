package essentials.features

import essentials.Main
import essentials.PlayerData
import essentials.internal.Bundle
import mindustry.gen.Call

class Exp(target: PlayerData) {
    val baseXP = Main.configs.baseXp
    val exponent = Main.configs.exponent
    private fun calcXpForLevel(level: Int): Double {
        return baseXP + baseXP * Math.pow(level.toDouble(), exponent)
    }

    private fun calculateFullTargetXp(level: Int): Double {
        var requiredXP = 0.0
        for (i in 0..level) {
            requiredXP += calcXpForLevel(i)
        }
        return requiredXP
    }

    private fun calculateLevel(xp: Double): Int {
        var level = 0
        var maxXp = calcXpForLevel(0)
        do {
            maxXp += calcXpForLevel(++level)
        } while (maxXp < xp)
        return level
    }

    init {
        val currentlevel = target.level
        val max = calculateFullTargetXp(currentlevel).toInt()
        val xp = target.exp
        val levelXp = max - xp
        val level = calculateLevel(xp.toDouble())
        val reqexp = Math.floor(max.toDouble()).toInt()
        val reqtotalexp = xp.toString() + "(" + Math.floor(levelXp.toDouble()).toInt() + ") / " + Math.floor(max.toDouble()).toInt()
        target.reqexp = reqexp
        target.level = level
        target.reqtotalexp = reqtotalexp
        if (currentlevel < level && currentlevel > Main.configs.alarmLevel && Main.configs.levelUpAlarm) Call.onInfoToast(Bundle(target.locale)["player.levelup", target.name, level], 600f)
    }
}