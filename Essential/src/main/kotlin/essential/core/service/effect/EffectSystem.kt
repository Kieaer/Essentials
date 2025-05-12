package essential.core.service.effect

import arc.graphics.Color
import arc.graphics.Colors
import arc.util.Timer
import essential.core.Main.Companion.conf
import essential.database.data.PlayerData
import essential.players
import mindustry.Vars
import mindustry.content.Fx
import mindustry.entities.Effect
import mindustry.gen.Call
import mindustry.gen.Playerc
import kotlin.random.Random

class EffectSystem : Timer.Task() {
    inner class EffectPos(
        val player: Playerc,
        val effect: Effect,
        val rotate: Float,
        val color: Color,
        vararg val random: IntRange
    )

    var buffer = ArrayList<EffectPos>()

    fun effect(data: PlayerData) {
        val color = if (data.effectColor != null) {
            if (Colors.get(data.effectColor) != null) Colors.get(data.effectColor) else Color.valueOf(
                data.effectColor
            )
        } else {
            data.player.color()
        }

        fun runEffect(effect: Effect) {
            buffer.add(EffectPos(data.player, effect, 0f, color))
        }

        fun runEffect(effect: Effect, size: Float) {
            buffer.add(EffectPos(data.player, effect, size, color))
        }

        fun runEffectAtRotate(effect: Effect, rotate: Float) {
            buffer.add(EffectPos(data.player, effect, rotate, color))
        }

        fun runEffectRandom(effect: Effect, range: IntRange) {
            buffer.add(EffectPos(data.player, effect, 0f, color, range))
        }

        fun runEffectRandomRotate(effect: Effect) {
            buffer.add(
                EffectPos(
                    data.player,
                    effect,
                    Random.nextFloat() * 360f,
                    color
                )
            )
        }

        fun runEffectAtRotateAndColor(
            effect: Effect,
            rotate: Float,
            customColor: Color
        ) {
            buffer.add(EffectPos(data.player, effect, rotate, customColor))
        }

        when (data.effectLevel ?: data.level) {
            in 10..19 -> runEffect(Fx.freezing)
            in 20..29 -> runEffect(Fx.overdriven)
            in 30..39 -> {
                runEffect(Fx.burning)
                runEffect(Fx.melting)
            }

            in 40..49 -> runEffect(Fx.steam)
            in 50..59 -> runEffect(Fx.shootSmallSmoke)
            in 60..69 -> runEffect(Fx.mine)
            in 70..79 -> runEffect(Fx.explosion)
            in 80..89 -> runEffect(Fx.hitLaser)
            in 90..99 -> runEffect(Fx.crawlDust)
            in 100..109 -> runEffect(Fx.mineImpact)
            in 110..119 -> {
                runEffect(Fx.vapor)
                runEffect(Fx.hitBulletColor)
            }

            in 120..129 -> {
                runEffect(Fx.vapor)
                runEffect(Fx.hitBulletColor)
                runEffect(Fx.hitSquaresColor)
            }

            in 130..139 -> {
                runEffect(Fx.vapor)
                runEffect(Fx.hitLaserBlast)
            }

            in 140..149 -> {
                runEffect(Fx.smokePuff)
                runEffect(Fx.hitBulletColor)
            }

            in 150..159 -> {
                runEffect(Fx.smokePuff)
                runEffect(Fx.hitBulletColor)
                runEffect(Fx.hitSquaresColor)
            }

            in 160..169 -> {
                runEffect(Fx.smokePuff)
                runEffect(Fx.hitLaserBlast)
            }

            in 170..179 -> {
                runEffect(Fx.placeBlock, 1.8f)
                runEffect(Fx.spawn)
            }

            in 180..189 -> {
                runEffect(Fx.placeBlock, 1.8f)
                runEffect(Fx.spawn)
                runEffect(Fx.hitLaserBlast)
            }

            in 190..199 -> {
                runEffect(Fx.placeBlock, 1.8f)
                runEffect(Fx.spawn)
                runEffect(Fx.circleColorSpark)
            }

            in 200..209 -> {
                val f = Fx.dynamicWave
                runEffect(f, 0.5f)
                runEffect(f, 3f)
                runEffect(f, 7f)
                runEffect(f, 5f)
                runEffect(f, 9f)
                runEffectRandom(Fx.hitLaserBlast, (-16..16))
                runEffectRandom(Fx.hitSquaresColor, (-16..16))
                runEffectRandom(Fx.vapor, (-4..4))
            }

            in 210..219 -> {
                runEffect(Fx.dynamicSpikes, 7f)
                runEffectRandom(Fx.hitSquaresColor, (-4..4))
                runEffectRandom(Fx.vapor, (-4..4))
            }

            in 220..229 -> {
                runEffect(Fx.dynamicSpikes, 7f)
                runEffectRandom(Fx.circleColorSpark, (-4..4))
                runEffectRandom(Fx.vapor, (-4..4))
            }

            in 230..239 -> {
                runEffect(Fx.dynamicSpikes, 7f)
                runEffectRandom(Fx.circleColorSpark, (-4..4))
                runEffectRandom(Fx.hitLaserBlast, (-4..4))
                runEffectRandom(Fx.smokePuff, (-4..4))
            }

            in 240..249 -> {
                runEffect(Fx.dynamicExplosion, 0.8f)
                runEffectRandom(Fx.hitLaserBlast, (-16..16))
                runEffectRandom(Fx.vapor, (-16..16))
            }

            in 250..259 -> {
                runEffect(Fx.dynamicExplosion, 0.8f)
                runEffectRandom(Fx.hitLaserBlast, (-4..4))
                runEffectRandom(Fx.smokePuff, (-4..4))
            }

            in 260..269 -> {
                runEffect(Fx.dynamicExplosion, 0.8f)
                runEffectRandom(Fx.hitLaserBlast, (-4..4))
                runEffectRandom(Fx.hitLaserBlast, (-4..4))
                runEffectRandom(Fx.smokePuff, (-4..4))
                buffer.add(
                    EffectPos(
                        data.player,
                        Fx.shootSmokeSquareBig,
                        listOf(0f, 90f, 180f, 270f).random(),
                        Color.HSVtoRGB(252f, 164f, 0f, 0.22f),
                        (-1..1)
                    )
                )
            }

            in 270..279 -> {
                runEffectRandomRotate(Fx.shootSmokeSquare)
                runEffect(Fx.hitLaserBlast)
                runEffect(Fx.colorTrail, 4f)
            }

            in 280..289 -> {
                runEffectRandomRotate(Fx.shootSmokeSquare)
                runEffect(Fx.hitLaserBlast)
                runEffect(Fx.dynamicWave, 2f)
            }

            in 290..299 -> {
                runEffectAtRotate(Fx.shootSmokeSquare, 0f)
                runEffectAtRotate(Fx.shootSmokeSquare, 45f)
                runEffectAtRotate(Fx.shootSmokeSquare, 90f)
                runEffectAtRotate(Fx.shootSmokeSquare, 135f)
                runEffectAtRotate(Fx.shootSmokeSquare, 180f)
                runEffectAtRotate(Fx.shootSmokeSquare, 225f)
                runEffectAtRotate(Fx.shootSmokeSquare, 270f)
                runEffectAtRotate(Fx.shootSmokeSquare, 315f)
                runEffect(Fx.breakProp)
                runEffect(Fx.vapor)
            }

            in 300..Int.MAX_VALUE -> {
                var rot = data.player.unit().rotation
                val customColor = Color.HSVtoRGB(252f, 164f, 0f, 0.22f)
                rot += 180f
                runEffectAtRotateAndColor(
                    Fx.shootSmokeSquareBig,
                    rot,
                    customColor
                )
                rot += 40f
                runEffectAtRotateAndColor(Fx.shootTitan, rot, customColor)
                rot += 25f
                runEffectAtRotateAndColor(Fx.colorSpark, rot, customColor)
                rot -= 105f
                runEffectAtRotateAndColor(Fx.shootTitan, rot, customColor)
                rot -= 25f
                runEffectAtRotateAndColor(Fx.colorSpark, rot, customColor)
                runEffect(Fx.mineHuge)
            }
        }
    }

    override fun run() {
        if (Vars.state.isPlaying) {
            if (conf.feature.level.effect.enabled) {
                val target = ArrayList<Playerc>()
                players.forEach {
                    if (it.effectVisibility) {
                        effect(it)
                        if (it.player.unit() != null && it.player.unit().health > 0f) {
                            if (conf.feature.level.effect.moving && it.player.unit().moving()) {
                                target.add(it.player)
                            } else if (!conf.feature.level.effect.moving) {
                                target.add(it.player)
                            }
                        }
                    }
                }

                buffer.forEach {
                    target.forEach { p ->
                        val x = if (it.random.isNotEmpty()) it.player.x + it.random[0].random() else it.player.x
                        val y = if (it.random.isNotEmpty()) it.player.y + it.random[0].random() else it.player.y
                        Call.effect(p.con(), it.effect, x, y, it.rotate, it.color)
                    }
                }
                buffer = ArrayList()
            } else {
                this.cancel()
            }
        }
    }
}