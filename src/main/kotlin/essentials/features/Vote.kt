package essentials.features

import arc.Core
import arc.Events
import arc.struct.Seq
import arc.util.Time
import essentials.Main
import essentials.Main.Companion.playerCore
import essentials.Main.Companion.pluginVars
import essentials.Main.Companion.tool
import essentials.internal.Bundle
import essentials.internal.Log
import essentials.internal.Log.LogType
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

class Vote : Thread() {
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
                println("** LOOP **")
                if (pluginVars.playerData.size < 4) {
                    player!!.sendMessage(Bundle(playerCore[player!!.uuid()].locale)["vote.minimal"])
                } else {
                    require = if (pluginVars.playerData.size > 8) 6 else 2 + if (pluginVars.playerData.size > 4) 1 else 0
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

    inner class Service {
        private var target: Playerc? = null
        private var map: Map? = null
        private var amount = 0
        var process = false
        var voted = Seq<String>()

        fun init() {
            process = true
            tool.sendMessageAll("vote.suggester-name", player!!.name())
            when (type) {
                VoteType.kick -> {
                    this.target = parameters[0] as Playerc
                    tool.sendMessageAll("vote.kick", target!!.name())
                }
                VoteType.gameover -> tool.sendMessageAll("vote.gameover")
                VoteType.skipwave -> {
                    amount = try {
                        parameters[0] as Int
                    } catch (ignored: NumberFormatException) {
                        3
                    }
                    tool.sendMessageAll("vote.skipwave", amount)
                }
                VoteType.rollback -> tool.sendMessageAll("vote.rollback")
                VoteType.gamemode -> if (parameters[0] is Gamemode) {
                    val gamemode = parameters[0] as Gamemode
                    tool.sendMessageAll("vote-gamemode", gamemode.name)
                } else {
                    player!!.sendMessage("vote.wrong-gamemode")
                    return
                }
                VoteType.map -> if (parameters[0] is Map) {
                    map = parameters[0] as Map
                    tool.sendMessageAll("vote.map", map!!.name())
                }
            }
            counting.start()
            alert.start()
        }

        var counting = Thread {
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
        var alert = Thread {
            currentThread().name = "Vote alert timertask"
            var time = 0
            while (!currentThread().isInterrupted) {
                val bundles = arrayOf("vote.count.50", "vote.count.40", "vote.count.30", "vote.count.20", "vote.count.10")
                if (time <= 4) {
                    if (Groups.player.size() > 0) {
                        tool.sendMessageAll(bundles[time])
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
                    VoteType.gameover -> {
                        Log.info("Vote gameover passed!")
                        tool.sendMessageAll("vote.gameover.done")
                        Events.fire(GameOverEvent(Team.crux))
                    }
                    VoteType.skipwave -> {
                        Log.info("Vote skipwave passed!")
                        tool.sendMessageAll("vote.skipwave.done")
                        var a = 0
                        while (a < amount) {
                            Vars.logic.runWave()
                            a++
                        }
                    }
                    VoteType.kick -> {
                        Log.info("Vote kick passed!")
                        playerCore[target!!.uuid()].kickcount = playerCore[target!!.uuid()].kickcount + 1
                        tool.sendMessageAll("vote.kick.done", target!!.name())
                        Core.app.post { target!!.info.lastKicked = Time.millis() + 30 * 60 * 1000 }
                        Call.kick(target!!.con(), Packets.KickReason.vote)
                        Log.write(LogType.player, "log.player.kick")
                    }
                    VoteType.rollback -> {
                        Log.info("Vote rollback passed!")
                        tool.sendMessageAll("vote.rollback.done")
                        Main.rollback.load()
                    }
                    VoteType.gamemode -> {
                        /*val m = Vars.world.map
                        val rules = Vars.world.map.rules()
                        if (rules.attackMode) rules.attackMode = false
                        Vars.world.loadMap(Vars.world.map, rules)*/
                    }
                    VoteType.map -> {
                        Log.info("Vote map passed!")
                        tool.sendMessageAll("vote.map.done")
                        Main.rollback.load(map)
                        tool.sendMessageAll("vote.map.done")
                    }
                }
            } else {
                when (type) {
                    VoteType.gameover -> tool.sendMessageAll("vote.gameover.fail")
                    VoteType.skipwave -> tool.sendMessageAll("vote.skipwave.fail")
                    VoteType.kick -> tool.sendMessageAll("vote.kick.fail", target!!.name())
                    VoteType.rollback -> tool.sendMessageAll("vote.rollback.fail")
                    VoteType.gamemode -> tool.sendMessageAll("vote.gamemode.fail")
                    VoteType.map -> tool.sendMessageAll("vote.map.fail")
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
                val p = playerCore[others.uuid()]
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
        gameover, skipwave, kick, rollback, gamemode, map
    }
}