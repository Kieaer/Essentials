package essentials.thread

import essentials.PluginData
import essentials.data.Config
import essentials.data.PlayerCore
import essentials.event.feature.Exp
import essentials.internal.CrashReport
import essentials.internal.Log
import essentials.internal.Tool
import mindustry.Vars.*
import mindustry.content.Blocks
import mindustry.core.GameState
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Nulls
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

object TriggerThread : TimerTask() {
    override fun run() {
        PluginData.uptime = PluginData.uptime + 1

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
            // 맵 플탐 카운트
            PluginData.playtime = PluginData.playtime + 1

            // 모든 클라이언트 서버에 대한 인원 총합 카운트
            for (a in 0 until PluginData.warptotals.size) {
                var result = 0
                for (value in PluginData.warpcounts) result += value!!.players
                val str = result.toString()
                val digits = IntArray(str.length)
                for (b in str.indices) digits[b] = str[b] - '0'
                val tile = PluginData.warptotals[a].tile
                if (PluginData.warptotals[a]!!.totalplayers != result && PluginData.warptotals[a]!!.numbersize != digits.size) {
                    for (px in 0..2) {
                        for (py in 0..4) {
                            Call.deconstructFinish(world.tile(tile.x + 4 + px, tile.y + py), Blocks.air, Nulls.unit)
                        }
                    }
                }
                Tool.setTileText(tile, Blocks.copperWall, result.toString())
                PluginData.warptotals[a] = PluginData.WarpTotal(state.map.name(), tile.pos(), result, digits.size)
            }

            // 플레이어 플탐 카운트 및 잠수확인
            for (p in Groups.player) {
                val target = PluginData[p.uuid()]
                if (target != null) {
                    var kick = false

                    // Exp 계산
                    target.exp = target.exp++

                    // 잠수 및 플레이 시간 계산
                    target.playtime = target.playtime + 1
                    if (target.x == p.tileX() && target.y == p.tileY()) {
                        target.afk = target.afk + 1
                        if (Config.afktime != 0 && Config.afktime < target.afk) {
                            kick = true
                        }
                    } else {
                        target.afk = 0
                    }
                    target.x = p.tileX()
                    target.y = p.tileY()
                    if (!state.rules.editor) Exp[target]
                    if (kick) Call.kick(p.con(), "AFK")
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