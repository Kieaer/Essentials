package essentials.thread

import arc.Core
import arc.Events
import arc.struct.ArrayMap
import arc.struct.ObjectMap
import arc.util.Strings
import essentials.Main.Companion.configs
import essentials.Main.Companion.playerCore
import essentials.Main.Companion.pluginData
import essentials.Main.Companion.pluginRoot
import essentials.Main.Companion.pluginVars
import essentials.Main.Companion.tool
import essentials.PluginData
import essentials.features.Exp
import essentials.internal.Bundle
import essentials.internal.CrashReport
import essentials.internal.Log
import mindustry.Vars.*
import mindustry.content.Blocks
import mindustry.core.GameState
import mindustry.core.NetClient
import mindustry.game.EventType.ServerLoadEvent
import mindustry.game.EventType.Trigger.update
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.type.Item
import mindustry.world.Tile
import org.hjson.JsonObject
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class TickTrigger {
    private val ores = ArrayMap<Item, Int?>()
    private val random = SecureRandom()
    var tick = 0
    val resources = ObjectMap<String, Int?>()

    fun writeOreStatus(item: Item, orignal: Int): String? {
        val `val`: Int
        val color: String
        val player = if (Groups.player.size() > 0) random.nextInt(Groups.player.size()) else 0
        if (Groups.player.size() > 0 && !state.teams[Groups.player.getByID(player).team()].cores.isEmpty) {
            if (state.teams[Groups.player.getByID(player).team()].cores.first().items.has(item) && ores[item] != null) {
                `val` = orignal - ores[item]!!
                color = when {
                    `val` > 0 -> {
                        "[green]+"
                    }
                    `val` < 0 -> {
                        "[red]-"
                    }
                    else -> {
                        "[yellow]"
                    }
                }
                ores.put(item, orignal)
                return """
                            []${item.name}: $color$`val`/s
                            
                            """.trimIndent()
            }
        }
        return null
    }


    init {
        Events.on(ServerLoadEvent::class.java) {
            for (item in content.items()) {
                if (item.type == ItemType.material) {
                    ores.put(item, 0)
                }
            }
        }
        Events.on(update::class.java) {
                if (tick < 86400) {
                    tick++
                } else {
                    tick = 0
                }
                if (state.`is`(GameState.State.playing)) {
                    if (configs.border) {
                        for (p in Groups.player) {
                            if (p.x > world.width() * 8 || p.x < 0 || p.y > world.height() * 8 || p.y < 0) Call.onPlayerDeath(p)
                        }
                    }
                }

                // 1초마다 실행
                if (tick % 60 == 0) {
                    // 서버 켜진시간 카운트
                    pluginVars.uptime = pluginVars.uptime + 1

                    // 데이터 저장
                    val json = JsonObject()
                    json.add("servername", Core.settings.getString("servername"))
                    pluginRoot.child("data/data.json").writeString(json.toString())

                    // 현재 서버 이름에다가 클라이언트 서버에 대한 인원 새기기
                    // new changename().start();

                    // 임시로 밴당한 유저 감시
                    for (a in 0 until pluginData.banned.size) {
                        val time = LocalDateTime.now()
                        if (time.isAfter(tool.longToDateTime(pluginData.banned[a].time))) {
                            pluginData.banned.remove(a)
                            netServer.admins.unbanPlayerID(pluginData.banned[a]!!.uuid)
                            Log.info("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + pluginData.banned[a]!!.name + "/" + pluginData.banned[a]!!.uuid + " player unbanned!")
                            break
                        }
                    }

                    // 맵이 돌아가고 있을 때
                    if (state.`is`(GameState.State.playing)) {
                        // 서버간 이동 패드에 플레이어가 있는지 확인
                        // new jumpzone().start();

                        // 맵 플탐 카운트
                        pluginVars.playtime = pluginVars.playtime + 1

                        // PvP 평화시간 카운트
                        if (configs.antiRush && state.rules.pvp && pluginVars.playtime < configs.antiRushtime && pluginVars.isPvPPeace) {
                            state.rules.playerDamageMultiplier = 0.66f
                            state.rules.playerHealthMultiplier = 0.8f
                            NetClient.onSetRules(state.rules)
                            for (p in Groups.player) {
                                player.sendMessage(Bundle(playerCore[p.uuid()].locale)["pvp-peacetime"])
                                player.kill()
                            }
                            pluginVars.isPvPPeace = false
                        }

                        // 모든 클라이언트 서버에 대한 인원 총합 카운트
                        for (a in 0 until pluginData.warptotals.size) {
                            var result = 0
                            for (value in pluginData.warpcounts) result += value!!.players
                            val str = result.toString()
                            // TODO 인원 카운트 다시 만들기
                            val digits = IntArray(str.length)
                            for (b in str.indices) digits[b] = str[b] - '0'
                            val tile = pluginData.warptotals[a].tile
                            if (pluginData.warptotals[a]!!.totalplayers != result) {
                                if (pluginData.warptotals[a]!!.numbersize != digits.size) {
                                    for (px in 0..2) {
                                        for (py in 0..4) {
                                            Call.deconstructFinish(world.tile(tile.x + 4 + px, tile.y + py), Blocks.air, 0)
                                        }
                                    }
                                }
                            }
                            tool.setTileText(tile, Blocks.copperWall, result.toString())
                            pluginData.warptotals[a] = PluginData.WarpTotal(state.map.name(), tile.pos(), result, digits.size)
                        }

                        // 플레이어 플탐 카운트 및 잠수확인
                        for (p in Groups.player) {
                            val target = playerCore[p.uuid()]
                            var kick = false
                            if (target.login) {
                                // Exp 계산
                                target.exp = target.exp + random.nextInt(50)

                                // 잠수 및 플레이 시간 계산
                                target.playtime = target.playtime + 1
                                if (target.x == p.tileX() && target.y == p.tileY()) {
                                    target.afk = target.afk + 1
                                    if (configs.afktime != 0L && configs.afktime < target.afk) {
                                        kick = true
                                    }
                                } else {
                                    target.afk = 0
                                }
                                target.x = p.tileX()
                                target.y = p.tileY()
                                if (!state.rules.editor) Exp(target)
                                if (kick) Call.onKick(p.con, "AFK")
                            }
                        }

                        // 메세지 블럭 감시
                        for (data in pluginData.messagemonitors) {
                            val tile: Tile = world.tile(data.pos)

                            if (tile.block() !== Blocks.message && tile.entity !is MessageBlockEntity) {
                                pluginData.messagemonitors.remove(data)
                                return
                            }
                            val entity = tile.entity as MessageBlockEntity
                            val msg = entity.message
                            if (msg == "powerblock") {
                                for (rot in 0..3) {
                                    if (entity.tile.link().getNearby(rot).entity != null) {
                                        pluginData.powerblocks.add(PluginData.PowerBlock(entity.tile, entity.tile.getNearby(rot).link().pos(), rot))
                                        break
                                    }
                                }
                                pluginData.messagemonitors.remove(data)
                                break
                            } else if (msg.contains("warp")) {
                                pluginData.messagewarps.add(PluginData.MessageWarp(data.pos, msg))
                                pluginData.messagemonitors.remove(data)
                                break
                            } else if (msg == "scancore") {
                                pluginData.scancore.add(tile)
                                pluginData.messagemonitors.remove(data)
                                break
                            }
                        }

                        // 서버간 이동 영역에 플레이어가 있는지 확인
                        for (value in pluginData.warpzones) {
                            if (!value!!.touch) {
                                for (ix in 0 until Groups.player.size()) {
                                    val player = Groups.player[ix]
                                    if (player.tileX() > value.startTile.x && player.tileX() < value.finishTile.x) {
                                        if (player.tileY() > value.startTile.y && player.tileY() < value.finishTile.y) {
                                            var resultIP = value.ip
                                            var port = 6567
                                            if (resultIP.contains(":") && Strings.canParsePostiveInt(resultIP.split(":").toTypedArray()[1])) {
                                                val temp = resultIP.split(":").toTypedArray()
                                                resultIP = temp[0]
                                                port = temp[1].toInt()
                                            }
                                            Log.info("player.warped", player.name, "$resultIP:$port")
                                            Call.connect(player.con, resultIP, port)
                                        }
                                    }
                                }
                            }
                        }

                        // 메세지 블럭에 있는 자원 소모량 감시
                        if (Groups.player.size() > 0) {
                            val items = StringBuilder()
                            for (item in content.items()) {
                                if (item.type == ItemType.material) {
                                    val player = Groups.player.getByID(random.nextInt(Groups.player.size()))
                                    var team: Team?
                                    team = if (player != null && !state.teams[player.team].cores.isEmpty) {
                                        player.team
                                    } else {
                                        return
                                    }
                                    val amount = state.teams[team].cores.first().items[item]
                                    if (state.teams[team].cores.first().items.has(item)) items.append(writeOreStatus(item, amount))
                                }
                            }
                            for (data in pluginData.scancore) {
                                if (data!!.block() !== Blocks.message) {
                                    data!!.remove()
                                    break
                                }
                                Call.setMessageBlockText(null, data, items.toString())
                            }
                        }

                        // 메세지 블럭에 있는 근처 전력 계산
                        for (data in pluginData.powerblocks) {
                            if (data!!.messageblock.block() !== Blocks.message) {
                                pluginData.powerblocks.remove(data)
                                return@on
                            }

                            val tile: Tile = world.tile(data.pos)

                            val arrow: String = when (data!!.rotate) {
                                0 -> "⇨"
                                1 -> "⇧"
                                2 -> "⇦"
                                3 -> "⇩"
                                else -> "null"
                            }
                            var current: Float
                            var product: Float
                            var using: Float
                            try {
                                current = tile.link().entity.power.graph.powerBalance * 60
                                using = tile.link().entity.power.graph.powerNeeded * 60
                                product = tile.link().entity.power.graph.powerProduced * 60
                            } catch (e: Exception) {
                                pluginData.powerblocks.remove(data)
                                Call.setMessageBlockText(null, data.messageblock, "$arrow Tile doesn't have powers!")
                                return
                            }
                            val text = """
                                [accent]$arrow[] Power status [accent]$arrow[]
                                Current: [sky]${current.roundToInt()}/s[]
                                Using: [red]${using.roundToInt()}[]/s
                                Production: [green]${product.roundToInt()}/s[]
                                """.trimIndent()
                            Call.setMessageBlockText(null, data.messageblock, text)
                        }
                    }
                }

                // 1.5초마다 실행
                if (tick % 90 == 0) {
                    if (state.`is`(GameState.State.playing) && configs.scanResource && state.rules.waves && Groups.player.size() > 0) {
                        for (item in content.items()) {
                            if (item.type == ItemType.material) {
                                val player = Groups.player.getByID(random.nextInt(Groups.player.size()))
                                val team : Team ?
                                team = if (player != null && state.teams[player.team].cores.isEmpty) {
                                    return player.team
                                } else {
                                    return
                                }
                                if (state.teams[team].cores.first().items.has(item)) {
                                    val cur = state.teams[team].cores.first().items[item]
                                    if (resources[item.name] != null) {
                                        if (cur - resources[item.name]!! <= -55) {
                                            val using = StringBuilder()
                                            for (p in Groups.player) {
                                                if (p.buildRequest() != null) {
                                                    for (c in p.buildRequest().block.requirements.indices) {
                                                        if (p.buildRequest().block.requirements[c].item.name == item.name) {
                                                            using.append(p.name).append(", ")
                                                        }
                                                    }
                                                }
                                            }
                                            if (using.length > 2) tool.sendMessageAll("resource-fast-use", item.name, using)
                                        }
                                    } else {
                                        resources.put(item.name, cur)
                                    }
                                }
                            }
                        }
                    }
                }

                // 3초마다
                if (tick % 180 == 0) {
                    try {
                        playerCore.saveAll()
                        pluginData.saveAll()
                    } catch (e: Exception) {
                        CrashReport(e)
                    }
                }

                // 1분마다
                if (tick % 3600 == 0) {
                    for (p in Groups.player) {
                        val playerData = playerCore[p.uuid()]
                        if (playerData.error) {
                            val message: String? = if (configs.passwordMethod == "discord") {
                                """
                                ${Bundle(playerData.locale)["system.login.require.discord"]}
                                ${configs.discordLink}
                                """.trimIndent()
                            } else {
                                Bundle(playerData.locale)["system.login.require.password"]
                            }
                            p.sendMessage(message)
                        }
                    }
                }
        }
    }
}