package essentials.features

import arc.Core
import arc.Events
import arc.struct.Seq
import arc.util.Time
import essentials.PlayerCore
import essentials.PluginVars
import essentials.features.Vote.VoteType.*
import essentials.internal.Bundle
import essentials.internal.Log
import essentials.internal.Log.LogType
import essentials.internal.Tool
import essentials.thread.AutoRollback
import mindustry.Vars
import mindustry.game.EventType.GameOverEvent
import mindustry.game.Gamemode
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Nulls
import mindustry.gen.Playerc
import mindustry.maps.Map
import mindustry.net.Packets
import java.lang.Thread.currentThread
import java.util.concurrent.TimeUnit

object Vote {
    lateinit var player: Playerc
    lateinit var type: VoteType
    lateinit var parameters: Array<Any>
    lateinit var target: Playerc

    var voting = false
    var require = 0
    var amount = 0
    var map : Map? = null

    val voted = Seq<String>()

    fun start() {
        val bundle = Bundle(PlayerCore[player.uuid()].locale)

        if(!voting) {
            voting = true
            if (PluginVars.playerData.size < 4) {
                player.sendMessage(bundle["vote.minimal"])
            } else {
                require = if (PluginVars.playerData.size > 8) 6 else 2 + if (PluginVars.playerData.size > 4) 1 else 0

                Tool.sendMessageAll("vote.suggester-name", player.name())
                when (type) {
                    Kick -> {
                        this.target = parameters[0] as Playerc
                        Tool.sendMessageAll("vote.kick", target.name())
                    }
                    Gameover -> Tool.sendMessageAll("vote.gameover")
                    SkipWave -> {
                        println(parameters[0])
                        amount = try {
                            parameters[0].toString().toInt()
                        } catch (ignored: NumberFormatException) {
                            3
                        }
                        Tool.sendMessageAll("vote.skipwave", amount.toString())
                    }
                    Rollback -> Tool.sendMessageAll("vote.rollback")
                    Gamemode -> if (parameters[0] is Gamemode) {
                        val gamemode = parameters[0] as Gamemode
                        Tool.sendMessageAll("vote-gamemode", gamemode.name)
                    } else {
                        player.sendMessage("vote.wrong-gamemode")
                        interrupt()
                    }
                    Maps -> if (parameters[0] is Map) {
                        map = parameters[0] as Map
                        Tool.sendMessageAll("vote.map", map!!.name())
                    }
                }
                if(voting) {
                    counting.start()
                    alert.start()
                }
            }
        } else {
            player.sendMessage(bundle["vote.in-processing"])
        }
    }

    fun interrupt(){
        alert.interrupt()
        counting.interrupt()
        voting = false
    }

    fun reset(){
        player = Nulls.player
        type = Gameover
        parameters = arrayOf()
        target = Nulls.player
        voting = false
        require = 0
        amount = 0
        map = null

        counting = Thread {
            var time = 0
            while (!currentThread().isInterrupted) {
                time++
                if (time >= 60) {
                    interrupt()
                } else {
                    try {
                        TimeUnit.SECONDS.sleep(1)
                    } catch (ignored: InterruptedException) {
                        interrupt()
                        success(voted.size >= require)
                    }
                }
            }
        }

        alert = Thread {
            currentThread().name = "Vote alert timertask"
            var time = 0
            while (!currentThread().isInterrupted) {
                val bundles = arrayOf("vote.count.50", "vote.count.40", "vote.count.30", "vote.count.20", "vote.count.10")
                if (time <= 4) {
                    if (Groups.player.size() > 0) {
                        Tool.sendMessageAll(bundles[time])
                    }
                    time++
                }
                try {
                    TimeUnit.SECONDS.sleep(10)
                } catch (ignored: InterruptedException) {
                    interrupt()
                }
            }
        }
    }

    private var counting : Thread = Thread()
    private var alert : Thread = Thread()

    fun success(success: Boolean) {
        // TODO 투표 성공 메세지 bundle 추가
        if (success) {
            when (type) {
                Gameover -> {
                    Log.info("Vote gameover passed!")
                    Tool.sendMessageAll("vote.gameover.done")
                    Events.fire(GameOverEvent(Team.crux))
                }
                SkipWave -> {
                    Log.info("Vote skipwave passed!")
                    Tool.sendMessageAll("vote.skipwave.done")
                    var a = 0
                    while (a < amount) {
                        Vars.logic.runWave()
                        a++
                    }
                }
                Kick -> {
                    Log.info("Vote kick passed!")
                    PlayerCore[target.uuid()].kickcount = PlayerCore[target.uuid()].kickcount + 1
                    Tool.sendMessageAll("vote.kick.done", target.name())
                    Core.app.post { target.info.lastKicked = Time.millis() + 30 * 60 * 1000 }
                    Call.kick(target.con(), Packets.KickReason.vote)
                    Log.write(LogType.Player, "log.player.kick")
                }
                Rollback -> {
                    Log.info("Vote rollback passed!")
                    Tool.sendMessageAll("vote.rollback.done")
                    AutoRollback.load()
                }
                Gamemode -> {
                    /*val m = Vars.world.map
                    val rules = Vars.world.map.rules()
                    if (rules.attackMode) rules.attackMode = false
                    Vars.world.loadMap(Vars.world.map, rules)*/
                }
                Maps -> {
                    Log.info("Vote map passed!")
                    Tool.sendMessageAll("vote.map.done")
                    AutoRollback.load(map)
                    Tool.sendMessageAll("vote.map.done")
                }
            }
        } else {
            when (type) {
                Gameover -> Tool.sendMessageAll("vote.gameover.fail")
                SkipWave -> Tool.sendMessageAll("vote.skipwave.fail")
                Kick -> Tool.sendMessageAll("vote.kick.fail", target.name())
                Rollback -> Tool.sendMessageAll("vote.rollback.fail")
                Gamemode -> Tool.sendMessageAll("vote.gamemode.fail")
                Maps -> Tool.sendMessageAll("vote.map.fail")
            }
        }
        reset()
    }

    fun set(uuid: String) {
        voted.add(uuid)
        for (others in Groups.player) {
            val p = PlayerCore[others.uuid()]
            if (!p.error && require - voted.size != -1) {
                others.sendMessage(Bundle(p.locale).prefix("vote.current-voted", voted.size.toString(), (require - voted.size).toString()))
            }
        }
        if (voted.size >= require) {
            interrupt()
            success(voted.size >= require)
        }
    }

    enum class VoteType {
        Gameover, SkipWave, Kick, Rollback, Gamemode, Maps
    }
}