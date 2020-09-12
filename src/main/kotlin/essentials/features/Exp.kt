package essentials.features

import essentials.Config
import essentials.Main
import essentials.PlayerData
import essentials.internal.Bundle
import mindustry.gen.Call
import kotlin.math.floor
import kotlin.math.pow

class Exp(target: PlayerData) {
    val baseXP = Config.baseXp
    val exponent = Config.exponent
    private fun calcXpForLevel(level: Int): Double {
        return baseXP + baseXP * level.toDouble().pow(exponent)
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
        val reqexp = floor(max.toDouble()).toInt()
        val reqtotalexp = xp.toString() + "(" + floor(levelXp.toDouble()).toInt() + ") / " + floor(max.toDouble()).toInt()
        target.reqexp = reqexp
        target.level = level
        target.reqtotalexp = reqtotalexp
        if (currentlevel < level && currentlevel > Config.alarmLevel && Config.levelUpAlarm) Call.infoToast(Bundle(target.locale)["player.levelup", target.name, level], 600f)
    }
}