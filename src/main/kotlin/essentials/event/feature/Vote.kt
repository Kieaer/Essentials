package essentials.event.feature

import arc.Events
import arc.struct.Seq
import arc.util.Time
import essentials.PluginData
import essentials.eof.sendMessage
import essentials.event.feature.VoteType.Gameover
import essentials.event.feature.VoteType.Kick
import essentials.event.feature.VoteType.Map
import essentials.event.feature.VoteType.Rollback
import essentials.event.feature.VoteType.Skipwave
import essentials.internal.Bundle
import essentials.internal.CrashReport
import essentials.internal.Log
import essentials.internal.Log.LogType
import essentials.internal.Tool
import mindustry.Vars
import mindustry.core.GameState
import mindustry.game.EventType.GameOverEvent
import mindustry.game.Gamemode
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.gen.Playerc
import mindustry.io.SaveIO
import mindustry.net.Packets

class Vote(val player: Playerc, val type: VoteType, vararg val arg: String) {
    lateinit var target: Playerc

    var voting = false
    var require = 0
    var amount = 0
    var map: mindustry.maps.Map? = null

    val voted = Seq<String>()

    fun start() {
        val bundle = Bundle(PluginData[player.uuid()])

        if(!voting) {
            voting = true
            if(PluginData.playerData.size < 4) {
                player.sendMessage(bundle["vote.minimal"])
            } else {
                require = if(PluginData.playerData.size > 8) 6 else 2 + if(PluginData.playerData.size > 4) 1 else 0

                Tool.sendMessageAll("vote.suggester-name", player.name())
                when(type) {
                    Kick -> {
                        this.target = arg[0] as Playerc
                        Tool.sendMessageAll("vote.kick", target.name())
                    }
                    Gameover -> Tool.sendMessageAll("vote.gameover")
                    Skipwave -> {
                        amount = try {
                            arg[0].toInt()
                        } catch(ignored: NumberFormatException) {
                            3
                        }
                        Tool.sendMessageAll("vote.skipwave", amount.toString())
                    }
                    Rollback -> Tool.sendMessageAll("vote.rollback")
                    Map -> {
                        var map = Vars.maps.all().find { map: mindustry.maps.Map ->
                            map.name().equals(arg[1].replace('_', ' '), ignoreCase = true) || map.name().equals(arg[1], ignoreCase = true)
                        }
                        if(map == null) {
                            map = Vars.maps.all()[arg[1].toInt()]
                            if(map == null) {
                                sendMessage(player, bundle["vote.map.not-found"])
                                return
                            }
                        }

                        Tool.sendMessageAll("vote.map", map.name())
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

    fun interrupt() {
        alert.interrupt()
        counting.interrupt()
        voting = false
    }

    private var counting: Thread = Thread()
    private var alert: Thread = Thread()

    fun success(success: Boolean) { // TODO 투표 성공 메세지 bundle 추가
        if(success) {
            when(type) {
                Gameover -> {
                    Log.info("Vote gameover passed!")
                    Tool.sendMessageAll("vote.gameover.done")
                    Events.fire(GameOverEvent(Team.crux))
                }
                Skipwave -> {
                    Log.info("Vote skipwave passed!")
                    Tool.sendMessageAll("vote.skipwave.done")
                    var a = 0
                    while(a < amount) {
                        Vars.logic.runWave()
                        a++
                    }
                }
                Kick -> {
                    Log.info("Vote kick passed!")
                    PluginData[target.uuid()]!!.kickcount++
                    Tool.sendMessageAll("vote.kick.done", target.name())
                    target.info.lastKicked = Time.millis() + 30 * 60 * 1000
                    Call.kick(target.con(), Packets.KickReason.vote)
                    Log.write(LogType.Player, "log.player.kick")
                }
                Rollback -> {
                    Log.info("Vote rollback passed!")
                    Tool.sendMessageAll("vote.rollback.done")
                    worldLoad()
                }
                Map -> {
                    Log.info("Vote map passed!")
                    Tool.sendMessageAll("vote.map.done")
                    worldLoad(map)
                    Tool.sendMessageAll("vote.map.done")
                }
            }
        } else {
            when(type) {
                Gameover -> Tool.sendMessageAll("vote.gameover.fail")
                Skipwave -> Tool.sendMessageAll("vote.skipwave.fail")
                Kick -> Tool.sendMessageAll("vote.kick.fail", target.name())
                Rollback -> Tool.sendMessageAll("vote.rollback.fail")
                Map -> Tool.sendMessageAll("vote.map.fail")
            }
        }
    }

    fun set(uuid: String) {
        voted.add(uuid)
        for(others in Groups.player) {
            val p = PluginData[others.uuid()]
            if(p != null && require - voted.size != -1) {
                others.sendMessage(Bundle(p)["vote.current-voted", voted.size.toString(), (require - voted.size).toString()])
            }
        }
        if(voted.size >= require) {
            interrupt()
            success(voted.size >= require)
        }
    }

    private fun worldLoad() {
        val players = Seq<Player>()
        for(p in Groups.player) {
            players.add(p)
            p.dead()
        }
        Vars.logic.reset()
        Call.worldDataBegin()
        try {
            val file = Vars.saveDirectory.child("rollback.${Vars.saveExtension}")
            SaveIO.load(file)
            Vars.logic.play()
            for(p in players) {
                if(p.con() == null) continue
                p.reset()
                if(Vars.state.rules.pvp) {
                    p.team(Vars.netServer.assignTeam(p, Seq.SeqIterable(players)))
                }
                Vars.netServer.sendWorldData(p)
            }
        } catch(e: SaveIO.SaveException) {
            CrashReport(e)
        }
        Log.info("Map rollbacked.")
        if(Vars.state.`is`(GameState.State.playing)) Call.sendMessage("[green]Map rollbacked.")
    }

    private fun worldLoad(map: mindustry.maps.Map?) {
        val players = Seq<Player>()
        for(p in Groups.player) {
            players.add(p)
            p.dead()
        }
        Vars.logic.reset()
        Call.worldDataBegin()
        try {
            Vars.world.loadMap(map, map!!.applyRules(Gamemode.survival)) //SaveIO.load(map.file);
            Vars.logic.play()
            for(p in players) {
                if(p.con() == null) continue
                p.reset()
                if(Vars.state.rules.pvp) {
                    p.team(Vars.netServer.assignTeam(p, Seq.SeqIterable(players)))
                }
                Vars.netServer.sendWorldData(p)
            }
        } catch(e: SaveIO.SaveException) {
            CrashReport(e)
        }
    }
}