package essentials.thread

import arc.Core
import arc.Events
import arc.struct.ArrayMap
import arc.struct.ObjectMap
import arc.util.Strings
import essentials.Config
import essentials.PlayerCore
import essentials.Main.Companion.pluginRoot
import essentials.PluginData
import essentials.PluginVars
import essentials.features.Exp
import essentials.internal.Bundle
import essentials.internal.CrashReport
import essentials.internal.Log
import essentials.internal.Tool
import mindustry.Vars.*
import mindustry.content.Blocks
import mindustry.core.GameState
import mindustry.core.NetClient
import mindustry.ctype.ContentType
import mindustry.game.EventType.ServerLoadEvent
import mindustry.game.EventType.Trigger.update
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Nulls
import mindustry.type.Item
import mindustry.world.Tile
import mindustry.world.blocks.logic.MessageBlock
import org.hjson.JsonObject
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

object TriggerThread : Runnable {
    private val random = SecureRandom()

    override fun run() {
        Events.on(update::class.java) {
            if (state.`is`(GameState.State.playing)) {
                if (Config.border) {
                    for (p in Groups.player) {
                        if (p.x > world.width() * 8 || p.x < 0 || p.y > world.height() * 8 || p.y < 0) p.dead()
                    }
                }

                // 서버간 이동 영역에 플레이어가 있는지 확인
                for (value in PluginData.warpzones) {
                    if (!value!!.touch) {
                        for (ix in 0 until Groups.player.size()) {
                            val player = Groups.player.getByID(ix)
                            if (player.tileX() > value.startTile.x && player.tileX() < value.finishTile.x) {
                                if (player.tileY() > value.startTile.y && player.tileY() < value.finishTile.y) {
                                    var resultIP = value.ip
                                    var port = 6567
                                    if (resultIP.contains(":") && Strings.canParsePositiveInt(resultIP.split(":").toTypedArray()[1])) {
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
            }
        }

        while(!Thread.currentThread().isInterrupted){
            // 서버 켜진시간 카운트
            PluginVars.uptime = PluginVars.uptime + 1

            // 데이터 저장
            val json = JsonObject()
            json.add("servername", Core.settings.getString("servername"))
            pluginRoot.child("data/data.json").writeString(json.toString())

            // 현재 서버 이름에다가 클라이언트 서버에 대한 인원 새기기
            // new changename().start();

            // 임시로 밴당한 유저 감시
            for (a in 0 until PluginData.banned.size) {
                val time = LocalDateTime.now()
                if (time.isAfter(Tool.longToDateTime(PluginData.banned[a].time))) {
                    PluginData.banned.remove(a)
                    netServer.admins.unbanPlayerID(PluginData.banned[a]!!.uuid)
                    Log.info("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + PluginData.banned[a]!!.name + "/" + PluginData.banned[a]!!.uuid + " player unbanned!")
                    break
                }
            }

            // 맵이 돌아가고 있을 때
            if (state.`is`(GameState.State.playing)) {
                // 서버간 이동 패드에 플레이어가 있는지 확인
                // new jumpzone().start();

                // 맵 플탐 카운트
                PluginVars.playtime = PluginVars.playtime + 1

                // 모든 클라이언트 서버에 대한 인원 총합 카운트
                for (a in 0 until PluginData.warptotals.size) {
                    var result = 0
                    for (value in PluginData.warpcounts) result += value!!.players
                    val str = result.toString()
                    // TODO 인원 카운트 다시 만들기
                    val digits = IntArray(str.length)
                    for (b in str.indices) digits[b] = str[b] - '0'
                    val tile = PluginData.warptotals[a].tile
                    if (PluginData.warptotals[a]!!.totalplayers != result) {
                        if (PluginData.warptotals[a]!!.numbersize != digits.size) {
                            for (px in 0..2) {
                                for (py in 0..4) {
                                    Call.deconstructFinish(world.tile(tile.x + 4 + px, tile.y + py), Blocks.air, Nulls.unit)
                                }
                            }
                        }
                    }
                    Tool.setTileText(tile, Blocks.copperWall, result.toString())
                    PluginData.warptotals[a] = PluginData.WarpTotal(state.map.name(), tile.pos(), result, digits.size)
                }

                // 플레이어 플탐 카운트 및 잠수확인
                for (p in Groups.player) {
                    val target = PlayerCore[p.uuid()]
                    var kick = false
                    if (target.login) {
                        // Exp 계산
                        target.exp = target.exp + random.nextInt(50)

                        // 잠수 및 플레이 시간 계산
                        target.playtime = target.playtime + 1
                        if (target.x == p.tileX() && target.y == p.tileY()) {
                            target.afk = target.afk + 1
                            if (Config.afktime != 0L && Config.afktime < target.afk) {
                                kick = true
                            }
                        } else {
                            target.afk = 0
                        }
                        target.x = p.tileX()
                        target.y = p.tileY()
                        if (!state.rules.editor) Exp(target)
                        if (kick) Call.kick(p.con, "AFK")
                    }
                }
            }

            // 데이터 저장
            try {
                PlayerCore.saveAll()
                PluginData.saveAll()
            } catch (e: Exception) {
                CrashReport(e)
            }
        }
    }
}