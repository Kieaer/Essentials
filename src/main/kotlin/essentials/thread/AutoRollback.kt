package essentials.internal.thread

import arc.struct.Array
import arc.struct.Array.ArrayIterable
import essentials.Main.Companion.configs
import essentials.internal.CrashReport
import essentials.internal.Log
import mindustry.Vars
import mindustry.core.GameState
import mindustry.entities.type.Player
import mindustry.game.Gamemode
import mindustry.gen.Call
import mindustry.io.SaveIO
import mindustry.io.SaveIO.SaveException
import mindustry.maps.Map
import java.util.*

class AutoRollback : TimerTask() {
    fun save() {
        try {
            val file = Vars.saveDirectory.child(configs.slotNumber.toString() + "." + Vars.saveExtension)
            if (Vars.state.`is`(GameState.State.playing)) SaveIO.save(file)
        } catch (e: Exception) {
            CrashReport(e)
        }
    }

    fun load() {
        val players = Array<Player>()
        for (p in Vars.playerGroup.all()) {
            players.add(p)
            p.isDead = true
        }
        Vars.logic.reset()
        Call.onWorldDataBegin()
        try {
            val file = Vars.saveDirectory.child(configs.slotNumber.toString() + "." + Vars.saveExtension)
            SaveIO.load(file)
            Vars.logic.play()
            for (p in players) {
                if (p.con == null) continue
                p.reset()
                if (Vars.state.rules.pvp) {
                    p.team = Vars.netServer.assignTeam(p, ArrayIterable(players))
                }
                Vars.netServer.sendWorldData(p)
            }
        } catch (e: SaveException) {
            CrashReport(e)
        }
        Log.info("Map rollbacked.")
        Thread(Runnable {
            try {
                val orignal = Vars.state.rules.respawnTime
                Vars.state.rules.respawnTime = 0f
                Call.onSetRules(Vars.state.rules)
                Thread.sleep(3000)
                Vars.state.rules.respawnTime = orignal
                Call.onSetRules(Vars.state.rules)
            } catch (ignored: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }).start()
        if (Vars.state.`is`(GameState.State.playing)) Call.sendMessage("[green]Map rollbacked.")
    }

    fun load(map: Map?) {
        val players = Array<Player>()
        for (p in Vars.playerGroup.all()) {
            players.add(p)
            p.isDead = true
        }
        Vars.logic.reset()
        Call.onWorldDataBegin()
        try {
            Vars.world.loadMap(map, map!!.applyRules(Gamemode.survival))
            //SaveIO.load(map.file);
            Vars.logic.play()
            for (p in players) {
                if (p.con == null) continue
                p.reset()
                if (Vars.state.rules.pvp) {
                    p.team = Vars.netServer.assignTeam(p, ArrayIterable(players))
                }
                Vars.netServer.sendWorldData(p)
            }
        } catch (e: SaveException) {
            CrashReport(e)
        }
    }

    override fun run() {
        Thread.currentThread().name = "Essential Auto rollback thread"
        save()
    }
}