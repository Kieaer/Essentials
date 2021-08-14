package essentials

import arc.Core
import arc.struct.ArrayMap
import arc.struct.Seq
import essentials.external.PingHost
import essentials.internal.Bundle
import essentials.internal.CrashReport
import essentials.internal.Tool
import mindustry.Vars.state
import mindustry.Vars.world
import mindustry.content.Blocks
import mindustry.core.GameState
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.net.Host
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

object Threads : Runnable {
    private var ping = 0.000
    private val servers = ArrayMap<String, Int>()

    override fun run() {
        Thread.currentThread().name = "Essential main thread"
        while(!Thread.currentThread().isInterrupted) {
            try {
                if(state.`is`(GameState.State.playing)) {
                    // 서버 인원 확인
                    for(i in 0 until PluginData.warpcounts.size) {
                        val value = PluginData.warpcounts[i]
                        PingHost[value!!.ip, value.port, Consumer { result: Host ->
                            if(result.name != null) {
                                ping += ("0." + result.ping).toDouble()
                                val str = result.players.toString()
                                val digits = IntArray(str.length)
                                for(a in str.indices) digits[a] = str[a] - '0'
                                val tile = value.tile
                                if(value.players != result.players && value.numbersize != digits.size) {
                                    for(px in 0..2) {
                                        for(py in 0..4) {
                                            Call.deconstructFinish(
                                                world.tile(tile.x + 4 + px, tile.y + py),
                                                Blocks.air,
                                                null
                                            )
                                        }
                                    }
                                }
                                Tool.setTileText(tile, Blocks.copperWall, str) // i 번째 server ip, 포트, x좌표, y좌표, 플레이어 인원, 플레이어 인원 길이
                                PluginData.warpcounts[i] = PluginData.WarpCount(state.map.name(), value.tile.pos(), value.ip, value.port, result.players, digits.size)
                                addPlayers(value.ip, value.port, result.players)
                            } else {
                                ping += 1.000
                                Tool.setTileText(value.tile, Blocks.copperWall, "no")
                            }
                        }]
                    }

                    val memory = Seq<String>()
                    for(value in PluginData.warpblocks) {
                        val tile = world.tile(value.pos)
                        if(tile.block() === Blocks.air) {
                            PluginData.warpblocks.remove(value)
                        } else {
                            PingHost[value.ip, value.port, Consumer { result: Host ->
                                var margin = 0f
                                var isDup = false
                                var x = tile.drawx()
                                when(value.size) {
                                    1 -> margin = 8f
                                    2 -> {
                                        margin = 16f
                                        x = tile.drawx() - 4f
                                        isDup = true
                                    }
                                    3 -> margin = 16f
                                    4 -> {
                                        x = tile.drawx() - 4f
                                        margin = 24f
                                        isDup = true
                                    }
                                }
                                val y = tile.drawy() + if(isDup) margin - 8 else margin
                                if(result.name != null) {
                                    ping += ("0." + result.ping).toDouble()
                                    memory.add("[yellow]" + result.players + "[] Players///" + x + "///" + y)
                                    value.online = true
                                } else {
                                    ping += 1.000
                                    memory.add("[scarlet]Offline///$x///$y")
                                    value.online = false
                                }
                                memory.add(value.description + "///" + x + "///" + (tile.drawy() - margin))
                                addPlayers(value.ip, value.port, result.players)
                            }]
                        }
                    }

                    for(m in memory) {
                        val a = m.split("///").toTypedArray()
                        Call.label(a[0], ping.toFloat() + 3f, a[1].toFloat(), a[2].toFloat())
                    }

                    if(Core.settings.getBool("isLobby")) {
                        if (state.`is`(GameState.State.playing)){
                            world.tiles.forEach {
                                if(it.build != null){
                                    it.build.health(it.build.health)
                                }
                            }
                        }
                        Core.settings.put("totalPlayers", totalPlayers + Groups.player.size())
                        Core.settings.saveValues()
                    }
                    ping = 0.000
                }

                TimeUnit.SECONDS.sleep(3)
            } catch(ignored: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch(e: Exception) {
                for(p in Groups.player) {
                    if(PluginData[p.uuid()] != null) {
                        Call.kick(p.con(), Bundle(Locale(PluginData[p.uuid()]!!.countryCode))["plugin-error-kick"])
                    } else {
                        Call.kick(p.con(), Bundle(Locale.ENGLISH)["plugin-error-kick"])
                    }
                }
                CrashReport(e)
                Core.app.dispose()
                Core.app.exit()
            }
        }
    }

    private fun addPlayers(ip: String?, port: Int, players: Int) {
        val mip = "$ip:$port"
        if(!servers.containsKey(mip)) {
            servers.put(mip, players)
        }
    }

    private val totalPlayers: Int
        get() {
            var total = 0
            for(v in servers) {
                total += v.value
            }
            return total
        }
}