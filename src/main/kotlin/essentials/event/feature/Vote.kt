package essentials.event.feature

import arc.Core
import arc.Events
import arc.struct.Seq
import arc.util.Time
import essentials.PluginData
import essentials.data.PlayerCore
import essentials.eof.sendMessage
import essentials.event.feature.VoteType.*
import essentials.event.feature.VoteType.Map
import essentials.internal.Bundle
import essentials.internal.Log
import essentials.internal.Log.LogType
import essentials.internal.Tool
import mindustry.Vars
import mindustry.game.EventType.GameOverEvent
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Playerc
import mindustry.net.Packets

class Vote(val player: Playerc, val type: VoteType, vararg val arg: String) {
    lateinit var target: Playerc

    var voting = false
    var require = 0
    var amount = 0
    var map : mindustry.maps.Map? = null

    val voted = Seq<String>()

    fun start() {
        val bundle = Bundle(PlayerCore[player.uuid()].locale)

        if(!voting) {
            voting = true
            if (PluginData.playerData.size < 4) {
                player.sendMessage(bundle["vote.minimal"])
            } else {
                require = if (PluginData.playerData.size > 8) 6 else 2 + if (PluginData.playerData.size > 4) 1 else 0

                Tool.sendMessageAll("vote.suggester-name", player.name())
                when (type) {
                    Kick -> {
                        this.target = arg[0] as Playerc
                        Tool.sendMessageAll("vote.kick", target.name())
                    }
                    Gameover -> Tool.sendMessageAll("vote.gameover")
                    Skipwave -> {
                        println(arg[0])
                        amount = try {
                            arg[0].toInt()
                        } catch (ignored: NumberFormatException) {
                            3
                        }
                        Tool.sendMessageAll("vote.skipwave", amount.toString())
                    }
                    Rollback -> Tool.sendMessageAll("vote.rollback")
                    Map -> {
                        var map = Vars.maps.all().find { map: mindustry.maps.Map ->
                            map.name().equals(arg[1].replace('_', ' '), ignoreCase = true) || map.name()
                                .equals(arg[1], ignoreCase = true)
                        }
                        if (map == null) {
                            map = Vars.maps.all()[arg[1].toInt()]
                            if (map == null) {
                                sendMessage(player, bundle.prefix("vote.map.not-found"))
                                return
                            }
                        }

                        Tool.sendMessageAll("vote.map", map.name())
                    }
                    None -> {}
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
                Skipwave -> {
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
                Map -> {
                    Log.info("Vote map passed!")
                    Tool.sendMessageAll("vote.map.done")
                    AutoRollback.load(map)
                    Tool.sendMessageAll("vote.map.done")
                }
                None -> {}
            }
        } else {
            when (type) {
                Gameover -> Tool.sendMessageAll("vote.gameover.fail")
                Skipwave -> Tool.sendMessageAll("vote.skipwave.fail")
                Kick -> Tool.sendMessageAll("vote.kick.fail", target.name())
                Rollback -> Tool.sendMessageAll("vote.rollback.fail")
                Map -> Tool.sendMessageAll("vote.map.fail")
                None -> {}
            }
        }
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
}