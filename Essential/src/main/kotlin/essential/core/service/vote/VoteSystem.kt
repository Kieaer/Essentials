package essential.core.service.vote

import arc.Core
import arc.Events
import arc.files.Fi
import arc.func.Cons
import arc.graphics.Color
import arc.util.Time
import arc.util.Timer
import essential.common.*
import essential.common.database.data.PlayerData
import essential.common.event.CustomEvents
import essential.common.permission.Permission
import essential.common.util.findPlayerData
import essential.core.earnEXP
import mindustry.Vars
import mindustry.content.Blocks
import mindustry.content.Fx
import mindustry.content.UnitTypes
import mindustry.content.Weathers
import mindustry.game.EventType.GameOverEvent
import mindustry.game.EventType.WorldLoadEvent
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.io.SaveIO
import mindustry.net.Administration
import mindustry.net.Packets
import mindustry.net.WorldReloader
import java.util.*
import kotlin.time.Duration.Companion.minutes

class VoteSystem(val voteData: VoteData) : Timer.Task() {
    private var count = 60
    private var voted = ArrayList<String>()
    private var isAdminVote = false
    private var isCanceled = false
    private var isPvP = Vars.state.rules.pvp

    private var chatFilter: Administration.ChatFilter
    private var gameoverEvent: Cons<GameOverEvent>
    private var worldLoadEvent: Cons<WorldLoadEvent>

    init {
        fun sendMessage(playerData: PlayerData?) {
            if (playerData != null) {
                val bundle = playerData.bundle
                playerData.send("command.vote.starter", voteData.starter.player.plainName())
                playerData.player.sendMessage(
                    when (voteData.type) {
                        VoteType.Kick -> bundle["command.vote.kick.start", voteData.target!!.plainName(), voteData.reason!!]
                        VoteType.Map -> bundle["command.vote.map.start", voteData.map!!.name(), voteData.reason!!]
                        VoteType.GameOver -> {
                            if (!isPvP) {
                                bundle["command.vote.gg.start"]
                            } else {
                                bundle["command.vote.gg.pvp.team"]
                            }
                        }
                        VoteType.Skip -> bundle["command.vote.skip.start", voteData.wave!!]
                        VoteType.Back -> bundle["command.vote.back.start", voteData.reason!!]
                        VoteType.Random -> bundle["command.vote.random.start"]
                    }
                )
                playerData.send("command.vote.how")
            }
        }

        voted.add(voteData.starter.player.uuid())

        players.forEach {
            if (isPvP) {
                if (voteData.team == it.player.team()) {
                    sendMessage(findPlayerData(it.uuid))
                }
            } else {
                sendMessage(findPlayerData(it.uuid))
            }
        }

        chatFilter = Administration.ChatFilter { player, message ->
            if (!message.startsWith("/")) {
                val data = findPlayerData(player.uuid())
                if (data != null) {
                    val isAdmin = Permission.check(data, "vote.pass")
                    if (isVoting && message.equals("y", true) && !voted.contains(player.uuid())) {
                        if (voteData.starter != data) {
                            if (Vars.state.rules.pvp && voteData.team == player.team()) {
                                voted.add(player.uuid())
                            } else if (!Vars.state.rules.pvp) {
                                voted.add(player.uuid())
                            }
                        } else if (isAdmin) {
                            isAdminVote = true
                        }
                        data.send("command.vote.voted")
                    } else if (isVoting && message.equals("n", true) && isAdmin) {
                        isCanceled = true
                    }
                    if (isVoting && message.contains("y", true) && !voted.contains(player.uuid())) {
                        return@ChatFilter null
                    } else {
                        return@ChatFilter message
                    }
                } else {
                    return@ChatFilter message
                }
            } else {
                return@ChatFilter message
            }
        }

        gameoverEvent = Cons<GameOverEvent> {
            this.cancel()
        }

        worldLoadEvent = Cons<WorldLoadEvent> {
            this.cancel()
        }

        Vars.netServer.admins.addChatFilter(chatFilter)
        Events.on(GameOverEvent::class.java, gameoverEvent)
        Events.on(WorldLoadEvent::class.java, worldLoadEvent)
    }

    fun send(message: String, vararg parameter: Any) {
        players.forEach {
            if (voteData.targetUUID != it.uuid) {
                it.send(message, *parameter)
            }
        }
    }

    fun check(): Int {
        return if (!isPvP) {
            when (players.filterNot { it.afk }.size) {
                1 -> 1
                in 2..4 -> 2
                in 5..6 -> 3
                7 -> 4
                in 8..9 -> 5
                in 10..11 -> 6
                12 -> 7
                else -> 8
            }
        } else {
            when (players.count { a -> a.player.team() == voteData.team && !a.afk }) {
                1 -> 1
                in 2..4 -> 2
                in 5..6 -> 3
                7 -> 4
                in 8..9 -> 5
                in 10..11 -> 6
                12 -> 7
                else -> 8
            }
        }
    }

    override fun cancel() {
        isVoting = false
        Vars.netServer.admins.chatFilters.remove(chatFilter)
        Events.remove(GameOverEvent::class.java, gameoverEvent)
        Events.remove(WorldLoadEvent::class.java, worldLoadEvent)
        super.cancel()
    }

    override fun run() {
        if (isVoting) {
            if (Groups.player.find { a -> a.uuid() == voteData.starter.uuid } == null) {
                send("command.vote.canceled.leave")
                this.cancel()
            } else {
                if (count % 10 == 0) {
                    if (isPvP) {
                        Groups.player.forEach {
                            if (it.team() == voteData.team) {
                                val data = findPlayerData(it.uuid())
                                if (data != null && voteData.targetUUID != data.uuid) {
                                    data.send("command.vote.count", count.toString(), check() - voted.size)
                                }
                            }
                        }
                    } else {
                        send("command.vote.count", count.toString(), check() - voted.size)
                        if (voteData.type == VoteType.Kick && Groups.player.find { a -> a.uuid() == voteData.targetUUID } == null) {
                            send("command.vote.kick.target.leave")
                        }
                    }
                }
                count--
                if ((count == 0 && check() <= voted.size) || check() <= voted.size || isAdminVote) {
                    send("command.vote.success")

                    val onlinePlayers = StringBuilder()
                    players.forEach {
                        onlinePlayers.append("${it.name}, ")
                    }
                    onlinePlayers.substring(0, onlinePlayers.length - 2)

                    when (voteData.type) {
                        VoteType.Kick -> {
                            val name = Vars.netServer.admins.getInfo(voteData.targetUUID).lastName
                            if (Groups.player.find { a -> a.uuid() == voteData.targetUUID } == null) {
                                Vars.netServer.admins.banPlayerID(voteData.targetUUID)
                                send("command.vote.kick.target.banned", name)
                                Events.fire(
                                    CustomEvents.PlayerVoteBanned(
                                        voteData.starter.name,
                                        name,
                                        voteData.reason!!,
                                        onlinePlayers.toString()
                                    )
                                )
                            } else {
                                voteData.target?.kick(Packets.KickReason.kick, 60 * 60 * 3000)
                                send("command.vote.kick.target.kicked", name)
                                Events.fire(
                                    CustomEvents.PlayerVoteKicked(
                                        voteData.starter.name,
                                        name,
                                        voteData.reason!!,
                                        onlinePlayers.toString()
                                    )
                                )
                            }
                        }

                        VoteType.Map -> {
                            for (it in players) {
                                earnEXP(Vars.state.rules.waveTeam, it.player, it, true)
                            }
                            isSurrender = true
                            Vars.maps.setNextMapOverride(voteData.map)
                            Events.fire(GameOverEvent(Vars.state.rules.waveTeam))
                        }

                        VoteType.GameOver -> {
                            if (!Permission.check(voteData.starter, "vote.pass")) {
                                voterCooldown[voteData.starter.uuid] = timeSource.markNow().plus(3.minutes)
                            }
                            if (isPvP) {
                                Vars.world.tiles.forEach {
                                    if (it.build != null && it.build.team != null && it.build.team == voteData.team) {
                                        Call.setTile(it, Blocks.air, voteData.team, 0)
                                    }
                                }
                            } else {
                                isSurrender = true
                                Events.fire(GameOverEvent(Vars.state.rules.waveTeam))
                            }
                        }

                        VoteType.Skip -> {
                            voterCooldown[voteData.starter.uuid] = timeSource.markNow().plus(3.minutes)
                            for (a in 0..voteData.wave!!) {
                                Vars.spawner.spawnEnemies()
                                Vars.state.wave++
                                Vars.state.wavetime = Vars.state.rules.waveSpacing
                            }
                            send("command.vote.skip.done", voteData.wave!!.toString())
                        }

                        VoteType.Back -> {
                            isSurrender = true
                            val savePath: Fi = if (Core.settings.getBool("autosave")) {
                                Vars.saveDirectory.findAll { f: Fi ->
                                    f.name().startsWith("auto_")
                                }.min { obj: Fi -> obj.lastModified().toFloat() }
                            } else {
                                Vars.saveDirectory.child("rollback.msav")
                            }

                            try {
                                val mode = Vars.state.rules.mode()
                                val reloader = WorldReloader()

                                reloader.begin()
                                SaveIO.load(savePath)

                                Vars.state.rules = Vars.state.map.applyRules(mode)
                                Vars.logic.play()
                                reloader.end()

                                savePath.delete()
                            } catch (t: Exception) {
                                t.printStackTrace()
                            }
                            send("command.vote.back.done")
                        }

                        VoteType.Random -> {
                            if (nextVoteAvailable.hasPassedNow() && !Permission.check(voteData.starter, "vote.random.bypass")) {
                                send("command.vote.random.cool")
                            } else {
                                voterCooldown[voteData.starter.uuid] = timeSource.markNow().plus(7.minutes)
                                nextVoteAvailable = timeSource.markNow().plus(5.minutes)
                                send("command.vote.random.done")
                                send("command.vote.random.is")
                                Time.runTask(180f, object : Timer.Task() {
                                    override fun run() {
                                        when (kotlin.random.Random.nextInt(7)) {
                                            0 -> {
                                                send("command.vote.random.unit")
                                                Groups.unit.each {
                                                    if (it.team == voteData.starter.player.team()) it.kill()
                                                }
                                                send("command.vote.random.unit.wave")
                                                Vars.logic.runWave()
                                            }

                                            1 -> {
                                                send("command.vote.random.wave")
                                                for (a in 0..5) Vars.logic.runWave()
                                            }

                                            2 -> {
                                                send("command.vote.random.health")
                                                Groups.build.each {
                                                    it.health(it.health / 2)
                                                }
                                            }

                                            3 -> {
                                                send("command.vote.random.fill.core")
                                                Vars.content.items().forEach {
                                                    if (!it.isHidden) {
                                                        Vars.state.teams.cores(voteData.starter.player.team())
                                                            .first().items.add(
                                                                it,
                                                                kotlin.random.Random.nextInt(2000)
                                                            )
                                                    }
                                                }
                                            }

                                            4 -> {
                                                send("command.vote.random.storm")
                                                Call.createWeather(
                                                    Weathers.rain,
                                                    10f,
                                                    60 * 60f,
                                                    50f,
                                                    10f
                                                )
                                            }

                                            5 -> {
                                                send("command.vote.random.fire")
                                                for (x in 0 until Vars.world.width()) {
                                                    for (y in 0 until Vars.world.height()) {
                                                        Call.effect(
                                                            Fx.fire,
                                                            (x * 8).toFloat(),
                                                            (y * 8).toFloat(),
                                                            0f,
                                                            Color.red
                                                        )
                                                    }
                                                }

                                                Timer.schedule(object : TimerTask() {
                                                    var tick = 600
                                                    val listener: Cons<WorldLoadEvent>

                                                    init {
                                                        listener = Cons<WorldLoadEvent> {
                                                            this.cancel()
                                                        }

                                                        Events.on(WorldLoadEvent::class.java, listener)
                                                    }

                                                    override fun cancel(): Boolean {
                                                        Events.remove(WorldLoadEvent::class.java, listener)
                                                        return super.cancel()
                                                    }

                                                    override fun run() {
                                                        tick--
                                                        Groups.unit.each {
                                                            it.health(it.health() / 10)
                                                        }
                                                        Groups.build.each {
                                                            it.health(it.health() / 30)
                                                        }
                                                        if (tick == 300) {
                                                            send("command.vote.random.supply")
                                                            repeat(2) {
                                                                UnitTypes.oct.spawn(
                                                                    voteData.starter.player.team(),
                                                                    voteData.starter.player.x,
                                                                    voteData.starter.player.y
                                                                )
                                                            }
                                                        }
                                                    }

                                                }, 0f, 10f)

                                            }

                                            else -> {
                                                send("command.vote.random.nothing")
                                            }
                                        }
                                    }
                                })
                            }
                        }
                    }

                    this.cancel()
                } else if ((count == 0 && check() > voted.size) || isCanceled) {
                    if (isPvP) {
                        players.forEach {
                            if (it.player.team() == voteData.team) {
                                Core.app.post { it.send("command.vote.failed") }
                            }
                        }
                    } else {
                        send("command.vote.failed")
                    }
                    this.cancel()
                }
            }
        } else {
            this.cancel()
        }
    }
}