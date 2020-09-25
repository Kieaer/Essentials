package essentials.features

import arc.Core
import arc.Events
import arc.struct.Seq
import arc.util.Time
import essentials.PlayerCore
import essentials.PluginVars
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
import mindustry.gen.Playerc
import mindustry.maps.Map
import mindustry.net.Packets
import java.util.concurrent.TimeUnit

object Vote : Thread() {
    var player: Playerc? = null
    var type: VoteType? = null
    lateinit var parameters: Array<Any>
    var pause = true
    var require = 0
    var service = Service()

    override fun run() {
        try {
            while (!currentThread().isInterrupted) {
                while (!currentThread().isInterrupted && pause) {
                    sleep(6)
                }
                if (PluginVars.playerData.size < 4) {
                    player!!.sendMessage(Bundle(PlayerCore[player!!.uuid()].locale)["vote.minimal"])
                } else {
                    require = if (PluginVars.playerData.size > 8) 6 else 2 + if (PluginVars.playerData.size > 4) 1 else 0
                    service.init()
                    println("Type: " + type.toString())
                }
                pause = true
            }
        } catch (ignored: InterruptedException) {
            service.interrupt()
            currentThread().interrupt()
        }
    }

    class Service {
        private var target: Playerc? = null
        private var map: Map? = null
        private var amount = 0
        var process = false
        var voted = Seq<String>()

        fun init() {
            process = true
            Tool.sendMessageAll("vote.suggester-name", player!!.name())
            when (type) {
                VoteType.Kick -> {
                    this.target = parameters[0] as Playerc
                    Tool.sendMessageAll("vote.kick", target!!.name())
                }
                VoteType.Gameover -> Tool.sendMessageAll("vote.gameover")
                VoteType.SkipWave -> {
                    amount = try {
                        parameters[0] as Int
                    } catch (ignored: NumberFormatException) {
                        3
                    }
                    Tool.sendMessageAll("vote.skipwave", amount)
                }
                VoteType.Rollback -> Tool.sendMessageAll("vote.rollback")
                VoteType.Gamemode -> if (parameters[0] is Gamemode) {
                    val gamemode = parameters[0] as Gamemode
                    Tool.sendMessageAll("vote-gamemode", gamemode.name)
                } else {
                    player!!.sendMessage("vote.wrong-gamemode")
                    return
                }
                VoteType.Map -> if (parameters[0] is Map) {
                    map = parameters[0] as Map
                    Tool.sendMessageAll("vote.map", map!!.name())
                }
            }
            counting.start()
            alert.start()
        }

        private var counting = Thread {
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
        private var alert = Thread {
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

        fun success(success: Boolean) {
            // TODO 투표 성공 메세지 bundle 추가
            if (success) {
                when (type) {
                    VoteType.Gameover -> {
                        Log.info("Vote gameover passed!")
                        Tool.sendMessageAll("vote.gameover.done")
                        Events.fire(GameOverEvent(Team.crux))
                    }
                    VoteType.SkipWave -> {
                        Log.info("Vote skipwave passed!")
                        Tool.sendMessageAll("vote.skipwave.done")
                        var a = 0
                        while (a < amount) {
                            Vars.logic.runWave()
                            a++
                        }
                    }
                    VoteType.Kick -> {
                        Log.info("Vote kick passed!")
                        PlayerCore[target!!.uuid()].kickcount = PlayerCore[target!!.uuid()].kickcount + 1
                        Tool.sendMessageAll("vote.kick.done", target!!.name())
                        Core.app.post { target!!.info.lastKicked = Time.millis() + 30 * 60 * 1000 }
                        Call.kick(target!!.con(), Packets.KickReason.vote)
                        Log.write(LogType.Player, "log.player.kick")
                    }
                    VoteType.Rollback -> {
                        Log.info("Vote rollback passed!")
                        Tool.sendMessageAll("vote.rollback.done")
                        AutoRollback.load()
                    }
                    VoteType.Gamemode -> {
                        /*val m = Vars.world.map
                        val rules = Vars.world.map.rules()
                        if (rules.attackMode) rules.attackMode = false
                        Vars.world.loadMap(Vars.world.map, rules)*/
                    }
                    VoteType.Map -> {
                        Log.info("Vote map passed!")
                        Tool.sendMessageAll("vote.map.done")
                        AutoRollback.load(map)
                        Tool.sendMessageAll("vote.map.done")
                    }
                }
            } else {
                when (type) {
                    VoteType.Gameover -> Tool.sendMessageAll("vote.gameover.fail")
                    VoteType.SkipWave -> Tool.sendMessageAll("vote.skipwave.fail")
                    VoteType.Kick -> Tool.sendMessageAll("vote.kick.fail", target!!.name())
                    VoteType.Rollback -> Tool.sendMessageAll("vote.rollback.fail")
                    VoteType.Gamemode -> Tool.sendMessageAll("vote.gamemode.fail")
                    VoteType.Map -> Tool.sendMessageAll("vote.map.fail")
                }
            }

            target = null
            map = null
            amount = 0
            process = false
            voted = Seq()
            process = false
        }

        fun interrupt() {
            alert.interrupt()
            counting.interrupt()
            process = false
        }

        fun set(uuid: String) {
            voted.add(uuid)
            for (others in Groups.player) {
                val p = PlayerCore[others.uuid()]
                if (!p.error && require - voted.size != -1) {
                    others.sendMessage(Bundle(p.locale).prefix("vote.current-voted", voted.size, require - voted.size))
                }
            }
            println("투표 인원: " + voted.size + "/ $require /" + (voted.size >= require))
            if (voted.size >= require) {
                interrupt()
                success(voted.size >= require)
            }
        }
    }

    enum class VoteType {
        Gameover, SkipWave, Kick, Rollback, Gamemode, Map
    }
}