package essentials.event.feature

import essentials.PlayerData
import essentials.data.Config
import kotlin.math.floor
import kotlin.math.pow

object Exp {
    private val baseXP = Config.baseXp
    private val exponent = Config.exponent
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

    operator fun get(target: PlayerData): String {
        val currentlevel = target.level
        val max = calculateFullTargetXp(currentlevel).toInt()
        val xp = target.exp
        val levelXp = max - xp
        val level = calculateLevel(xp.toDouble())
        //val reqexp = floor(max.toDouble()).toInt()
        target.level = level
        return xp.toString() + "(" + floor(levelXp.toDouble()).toInt() + ") / " + floor(max.toDouble()).toInt()
    }
}