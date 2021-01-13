package essentials.thread

import arc.graphics.Color
import arc.struct.Seq
import arc.util.async.Threads.sleep
import essentials.data.Config
import essentials.PluginData
import essentials.external.PingHost
import essentials.internal.Log
import mindustry.Vars
import mindustry.Vars.world
import mindustry.content.Fx
import mindustry.core.GameState
import mindustry.gen.Call
import mindustry.net.Host
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

object WarpBorder : Runnable {
    var length = 0
    var thread = Seq<Thread>()
    override fun run() {
        Thread.currentThread().name = "Essential server to server work thread"
        length = PluginData.warpzones.size
        start()
    }

    fun interrupt() {
        for (t in thread) {
            t.interrupt()
        }
    }

    val isInterrupted: Boolean
        get() {
            var interrupt = true
            for (t in thread) {
                if (!t.isInterrupted) {
                    interrupt = false
                    break
                }
            }
            return interrupt
        }

    fun start() {
        for (data in PluginData.warpzones) {
            val t = Thread {
                while (!Thread.currentThread().isInterrupted) {
                    val ip = data!!.ip
                    if (Vars.state.`is`(GameState.State.playing)) {
                        PingHost[ip, data.port, Consumer { result: Host ->
                            try {
                                if (result.name != null) {
                                    val size = data.finishTile.x - data.startTile.x
                                    for (x in 0 until size) {
                                        val tile = world.tile(data.startTile.x + x, data.startTile.y.toInt())
                                        Call.effect(Fx.placeBlock, tile.getX(), tile.getY(), 0f, Color.orange)
                                        sleep(96)
                                    }
                                    for (y in 0 until size) {
                                        val tile = world.tile(data.finishTile.x.toInt(), data.startTile.y + y)
                                        Call.effect(Fx.placeBlock, tile.getX(), tile.getY(), 0f, Color.orange)
                                        sleep(96)
                                    }
                                    for (x in 0 until size) {
                                        val tile = world.tile(data.finishTile.x - x, data.finishTile.y.toInt())
                                        Call.effect(Fx.placeBlock, tile.getX(), tile.getY(), 0f, Color.orange)
                                        sleep(96)
                                    }
                                    for (y in 0 until size) {
                                        val tile = world.tile(data.startTile.x.toInt(), data.finishTile.y - y)
                                        Call.effect(Fx.placeBlock, tile.getX(), tile.getY(), 0f, Color.orange)
                                        sleep(96)
                                    }
                                    if (size < 5) sleep(2000)
                                } else {
                                    if (Config.debug) Log.info("warp zone $ip offline! After 1 minute, try to connect again.")
                                    TimeUnit.MINUTES.sleep(1)
                                }
                            } catch (e: InterruptedException) {
                                Thread.currentThread().interrupt()
                            }
                        }]
                    } else {
                        try {
                            TimeUnit.SECONDS.sleep(1)
                        } catch (e: InterruptedException) {
                            Thread.currentThread().interrupt()
                        }
                    }
                }
            }
            thread.add(t)
            t.start()
        }
    }
}