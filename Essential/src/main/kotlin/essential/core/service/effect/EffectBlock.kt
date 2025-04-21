package essential.core.service.effect

import arc.struct.Seq
import essential.core.Main.Companion.database
import mindustry.Vars
import mindustry.content.Blocks
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.world.Tile
import mindustry.world.blocks.logic.LogicBlock

class EffectBlock {
    var effectBlock : Tile? = null

    fun config() : ByteArray {
        val buffer = StringBuilder()
        database.players.forEach { playerData ->
            // playerData.player.team().data().players.indexOf(
            //                            playerData.player
            //                        )
            val uuid = playerData.uuid
            val config = """
                    fetch player $uuid @${playerData.player.team().name} ${
                playerData.player.team().data().players.indexOf(playerData.player)
            } @conveyor
                    sensor ${uuid}_x $uuid @x
                    sensor ${uuid}_y $uuid @y
                """.trimIndent()
            val color = playerData.effectColor ?: "ffaaff"
            buffer.appendLine(config)

            val next = when (playerData.effectLevel ?: playerData.level) {
                in 110..119 -> {
                    """
                        effect vapor ${uuid}_x ${uuid}_y 2 %$color
                        effect hit ${uuid}_x ${uuid}_y 2 %$color
                    """.trimIndent()
                }

                in 120..129 -> {
                    """
                        effect vapor ${uuid}_x ${uuid}_y 2 %$color
                        effect hit ${uuid}_x ${uuid}_y 2 %$color
                        effect hitSquare ${uuid}_x ${uuid}_y 2 %$color
                    """.trimIndent()
                }

                in 130..139 -> {
                    """
                        effect vapor ${uuid}_x ${uuid}_y 2 %$color
                        effect spark ${uuid}_x ${uuid}_y 2 %$color
                    """.trimIndent()
                }

                in 140..149 -> {
                    """
                        effect smokePuff ${uuid}_x ${uuid}_y 2 %$color
                        effect hit ${uuid}_x ${uuid}_y 2 %$color
                    """.trimIndent()
                }

                in 150..159 -> {
                    """
                        effect smokePuff ${uuid}_x ${uuid}_y 2 %$color
                        effect hit ${uuid}_x ${uuid}_y 2 %$color
                        effect hitSquare ${uuid}_x ${uuid}_y 2 %$color
                    """.trimIndent()
                }

                in 160..169 -> {
                    """
                        effect smokePuff ${uuid}_x ${uuid}_y 2 %$color 
                        effect spark ${uuid}_x ${uuid}_y 2 %$color 
                    """.trimIndent()
                }

                in 170..179 -> {
                    """
                        effect placeBlock ${uuid}_x ${uuid}_y 1.8 %$color
                        effect spawn ${uuid}_x ${uuid}_y 1 %$color
                    """.trimIndent()
                }

                in 180..189 -> {
                    """
                        effect placeBlock ${uuid}_x ${uuid}_y 1.8 %$color 
                        effect spawn ${uuid}_x ${uuid}_y 1 %$color 
                        effect spark ${uuid}_x ${uuid}_y 1 %$color 
                    """.trimIndent()
                }

                in 190..199 -> {
                    """
                        effect placeBlock ${uuid}_x ${uuid}_y 1.8 %$color 
                        effect spawn ${uuid}_x ${uuid}_y 1 %$color 
                        effect sparkBig ${uuid}_x ${uuid}_y 1 %$color 
                    """.trimIndent()
                }

                in 210..219 -> {
                    """
                        effect crossExplosion ${uuid}_x ${uuid}_y 7 %$color @duo
                        op rand ${uuid}_r1 2 b
                        op rand ${uuid}_r2 2 b
                        op sub ${uuid}_r1 ${uuid}_r1 1
                        op sub ${uuid}_r2 ${uuid}_r2 1
                        op add ${uuid}_x ${uuid}_x ${uuid}_r1
                        op add ${uuid}_y ${uuid}_y ${uuid}_r2
                        effect spark ${uuid}_x ${uuid}_y 0.5 %$color @duo
                        effect vapor ${uuid}_x ${uuid}_y 2 %$color @duo
                    """.trimIndent()
                }

                in 220..229 -> {
                    """
                        effect crossExplosion ${uuid}_x ${uuid}_y 7 %$color @duo
                        op rand ${uuid}_r1 2 b
                        op rand ${uuid}_r2 2 b
                        op sub ${uuid}_r1 ${uuid}_r1 1
                        op sub ${uuid}_r2 ${uuid}_r2 1
                        op add ${uuid}_x ${uuid}_x ${uuid}_r1
                        op add ${uuid}_y ${uuid}_y ${uuid}_r2
                        effect sparkBig ${uuid}_x ${uuid}_y 0.5 %$color @duo
                        effect vapor ${uuid}_x ${uuid}_y 2 %$color @duo
                    """.trimIndent()
                }

                in 230..239 -> {
                    """
                        effect crossExplosion ${uuid}_x ${uuid}_y 7 %$color @duo
                        op rand ${uuid}_r1 2 b
                        op rand ${uuid}_r2 2 b
                        op sub ${uuid}_r1 ${uuid}_r1 1
                        op sub ${uuid}_r2 ${uuid}_r2 1
                        op add ${uuid}_x ${uuid}_x ${uuid}_r1
                        op add ${uuid}_y ${uuid}_y ${uuid}_r2
                        effect sparkBig ${uuid}_x ${uuid}_y 0.5 %$color @duo
                        effect spark ${uuid}_x ${uuid}_y 0.5 %$color @duo
                        effect smokePuff ${uuid}_x ${uuid}_y 2 %$color @duo
                    """.trimIndent()
                }

                in 240..249 -> {
                    """
                        effect explosion ${uuid}_x ${uuid}_y 0.8 %$color @duo
                        op rand ${uuid}_r1 4 b
                        op rand ${uuid}_r2 4 b
                        op sub ${uuid}_r1 ${uuid}_r1 2
                        op sub ${uuid}_r2 ${uuid}_r2 2
                        op add ${uuid}_x ${uuid}_x ${uuid}_r1
                        op add ${uuid}_y ${uuid}_y ${uuid}_r2
                        effect spark ${uuid}_x ${uuid}_y 0.5 %$color @duo
                        effect vapor ${uuid}_x ${uuid}_y 2 %$color @duo
                    """.trimIndent()
                }

                in 250..259 -> {
                    """
                        effect explosion ${uuid}_x ${uuid}_y 0.8 %$color @duo                        
                        op rand ${uuid}_r1 1 b
                        op rand ${uuid}_r2 1 b
                        op sub ${uuid}_r1 ${uuid}_r1 0.5
                        op sub ${uuid}_r2 ${uuid}_r2 0.5
                        op add ${uuid}_x ${uuid}_x ${uuid}_r1
                        op add ${uuid}_y ${uuid}_y ${uuid}_r2
                        effect spark ${uuid}_x ${uuid}_y 0.5 %$color @duo
                        effect smokePuff ${uuid}_x ${uuid}_y 2 %$color @duo
                    """.trimIndent()
                }

                in 260..269 -> {
                    """
                        effect explosion ${uuid}_x ${uuid}_y 0.8 %$color @duo                        
                        op rand ${uuid}_r1 1 b
                        op rand ${uuid}_r2 1 b
                        op sub ${uuid}_r1 ${uuid}_r1 0.5
                        op sub ${uuid}_r2 ${uuid}_r2 0.5
                        op add ${uuid}_x ${uuid}_x ${uuid}_r1
                        op add ${uuid}_y ${uuid}_y ${uuid}_r2
                        effect spark ${uuid}_x ${uuid}_y 0.5 %$color @duo
                        effect spark ${uuid}_x ${uuid}_y 0.5 %$color @duo
                        effect smokePuff ${uuid}_x ${uuid}_y 2 %$color @duo
                        op rand ${uuid}_rotation 4 b
                        op ceil ${uuid}_rotation rotation b
                        op mul ${uuid}_rotation rotation 90
                        effect smokeSquareBig ${uuid}_x ${uuid}_y ${uuid}_rotation %fca40038 @duo
                    """.trimIndent()
                }

                in 270..279 -> {
                    """
                        op rand ${uuid}_rotation 360 b
                        effect smokeSquare ${uuid}_x ${uuid}_y ${uuid}_rotation %$color
                        effect spark ${uuid}_x ${uuid}_y ${uuid}_rotation %$color
                        effect trail ${uuid}_x ${uuid}_y 4 %$color
                    """.trimIndent()
                }

                in 280..289 -> {
                    """
                        op rand ${uuid}_rotation 360 b
                        effect smokeSquare ${uuid}_x ${uuid}_y ${uuid}_rotation %$color
                        effect spark ${uuid}_x ${uuid}_y ${uuid}_rotation %$color
                        effect wave ${uuid}_x ${uuid}_y 2 %$color
                    """.trimIndent()
                }

                in 290..299 -> {
                    """
                        effect smokeSquare ${uuid}_x ${uuid}_y 0 %$color
                        effect smokeSquare ${uuid}_x ${uuid}_y 45 %$color
                        effect smokeSquare ${uuid}_x ${uuid}_y 90 %$color
                        effect smokeSquare ${uuid}_x ${uuid}_y 135 %$color
                        effect smokeSquare ${uuid}_x ${uuid}_y 180 %$color
                        effect smokeSquare ${uuid}_x ${uuid}_y 225 %$color
                        effect smokeSquare ${uuid}_x ${uuid}_y 270 %$color
                        effect smokeSquare ${uuid}_x ${uuid}_y 315 %$color
                        effect breakProp ${uuid}_x ${uuid}_y rotation %$color
                        effect vapor ${uuid}_x ${uuid}_y rotation %$color
                    """.trimIndent()
                }

                in 300..Int.MAX_VALUE -> {
                    """
                        sensor ${uuid}_rotation $uuid @rotation
                        op add ${uuid}_rotation ${uuid}_rotation 180
                        effect smokeSquareBig ${uuid}_x ${uuid}_y ${uuid}_rotation %fca40038 
                        op add ${uuid}_rotation ${uuid}_rotation 40
                        effect shootBig ${uuid}_x ${uuid}_y ${uuid}_rotation %fca40038 
                        op add ${uuid}_rotation ${uuid}_rotation 25
                        effect sparkShoot ${uuid}_x ${uuid}_y ${uuid}_rotation %fca40038 
                        op sub ${uuid}_rotation ${uuid}_rotation 105
                        effect shootBig ${uuid}_x ${uuid}_y ${uuid}_rotation %fca40038 
                        op sub ${uuid}_rotation ${uuid}_rotation 25
                        effect sparkShoot ${uuid}_x ${uuid}_y ${uuid}_rotation %fca40038 
                        effect drillBig ${uuid}_x ${uuid}_y 2 %$color 
                    """.trimIndent()
                }

                else -> {
                    ""
                }
            }

            buffer.appendLine(next)

            if ((playerData.effectLevel ?: playerData.level) in 200..209) {
                buffer.appendLine(
                    """
                            op rand ${uuid}_r1 1.5 b
                            op ceil ${uuid}_r1 ${uuid}_r1 b
                            op rand ${uuid}_r2 1.5 b
                            op ceil ${uuid}_r2 ${uuid}_r2 b
                            op rand ${uuid}_r 4 b
                            op ceil ${uuid}_r ${uuid}_r b
                    """.trimIndent()
                )

                val order = buffer.lines().size
                buffer.appendLine(
                    """
                        jump ${order + 3} notEqual ${uuid}_r 1
                        op add ${uuid}_x ${uuid}_x ${uuid}_r2
                        op add ${uuid}_y ${uuid}_y ${uuid}_r1
                        jump ${order + 6} notEqual ${uuid}_r 2
                        op sub ${uuid}_x ${uuid}_x ${uuid}_r2
                        op add ${uuid}_y ${uuid}_y ${uuid}_r1
                        jump ${order + 9} notEqual ${uuid}_r 3
                        op sub ${uuid}_x ${uuid}_x ${uuid}_r2
                        op sub ${uuid}_y ${uuid}_y ${uuid}_r1
                        jump ${order + 12} notEqual ${uuid}_r 4
                        op add ${uuid}_x ${uuid}_x ${uuid}_r2
                        op sub ${uuid}_y ${uuid}_y ${uuid}_r1
                    """.trimIndent()
                )

                buffer.append(
                    """
                        effect wave ${uuid}_x ${uuid}_y 0.5 %$color @duo
                        effect wave ${uuid}_x ${uuid}_y 3 %$color @duo
                        effect wave ${uuid}_x ${uuid}_y 7 %$color @duo
                        effect wave ${uuid}_x ${uuid}_y 5 %$color @duo
                        effect wave ${uuid}_x ${uuid}_y 9 %$color @duo
                        effect hitSquare ${uuid}_x ${uuid}_y 0.5 %$color @duo
                        effect vapor ${uuid}_x ${uuid}_y 2 %$color @duo
                    """.trimIndent()
                )
            }
        }

        buffer.appendLine("wait 0.05")
        return LogicBlock.compress(buffer.toString(), Seq())
    }

    fun apply() {
        for (it in Vars.world.tiles) {
            if (it.block() !is LogicBlock || effectBlock == null || effectBlock == it) {
                /*Call.constructFinish(
                    it,
                    Blocks.worldProcessor,
                    Nulls.unit,
                    0,
                    Team.sharded,
                    LogicBlock.compress(config(), Seq())
                )*/
                effectBlock = it
                break
            }
        }
    }
}