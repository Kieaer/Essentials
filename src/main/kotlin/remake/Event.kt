package remake

import arc.Core
import arc.Events
import arc.files.Fi
import arc.util.Log
import mindustry.game.EventType
import mindustry.game.EventType.ConfigEvent
import mindustry.gen.Call
import remake.Main.Companion.database
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Event {
    fun register(){
        Events.on(EventType.PlayerChatEvent::class.java){
            if (!it.message.startsWith("/")) {
                log(LogType.Chat, "${it.player.name}: ${it.message}")
                Log.info("<&y" + it.player.name + ": &lm" + it.message + "&lg>")

                // todo 채팅 포맷 변경
                Call.sendMessage(
                    Permission[it.player].chatFormat
                        .replace("%1", it.player.coloredName())
                        .replace("%2", it.message)
                )
            }
        }

        Events.on(EventType.WithdrawEvent::class.java){
            if (it.tile != null && it.player.unit().item() != null && it.player.name != null) {
                log(LogType.WithDraw, "${it.player.name} puts ${it.player.unit().item().name} ${it.amount.toString()} amount into ${it.tile.block().name}.")
            }
        }

        Events.on(EventType.DepositEvent::class.java){

        }

        Events.on(ConfigEvent::class.java){

        }

        Events.on(EventType.TapEvent::class.java){

        }

        Events.on(EventType.PickupEvent::class.java){

        }

        Events.on(EventType.UnitControlEvent::class.java){

        }

        Events.on(EventType.GameOverEvent::class.java){

        }

        Events.on(EventType.BlockBuildBeginEvent::class.java){

        }

        Events.on(EventType.BlockBuildEndEvent::class.java){
            if (it.unit.isPlayer){
                val player = database[it.unit.player.uuid()]
                if (player != null) {
                    if (!it.breaking) player.placecount++ else player.breakcount++
                }
            }
        }

        Events.on(EventType.BuildSelectEvent::class.java){

        }

        Events.on(EventType.BlockDestroyEvent::class.java){

        }

        Events.on(EventType.UnitDestroyEvent::class.java){

        }

        Events.on(EventType.UnitCreateEvent::class.java){

        }

        Events.on(EventType.UnitChangeEvent::class.java){

        }

        Events.on(EventType.PlayerJoin::class.java){
            val data = database[it.player.uuid()]
            if(data != null){
                Trigger.loadPlayer(it.player, data)
            } else {
                if (Config.authType == Config.AuthType.None){
                    Trigger.createPlayer(it.player, null)
                }
            }
        }

        Events.on(EventType.PlayerLeave::class.java){
            val data = database.players.find { data -> data.uuid == it.player.uuid() }
            if(data != null){
                database.update(it.player.uuid(), data)
            }
            database.players.remove(data)
        }

        Events.on(EventType.PlayerBanEvent::class.java){

        }

        Events.on(EventType.WorldLoadEvent::class.java){

        }

        Events.on(EventType.MenuOptionChooseEvent::class.java){

        }

        Events.run(EventType.Trigger.impactPower){

        }
    }

    fun log(type: LogType, text: String) {
        val root: Fi = Core.settings.dataDirectory.child("mods/Essentials/")

        val date = DateTimeFormatter.ofPattern("yyyy-MM-dd HH_mm_ss").format(LocalDateTime.now())
        val new = Paths.get(root.child("log/$type.log").path())
        val old = Paths.get(root.child("log/old/$type/$date.log").path())
        var main = root.child("log/$type.log")
        val folder = root.child("log")

        if(main != null && main.length() > 2048 * 256) {
            main.writeString("end of file. $date", true)
            try {
                if(!root.child("log/old/$type").exists()) {
                    root.child("log/old/$type").mkdirs()
                }
                Files.move(new, old, StandardCopyOption.REPLACE_EXISTING)
            } catch(e: IOException) {
                e.printStackTrace()
            }
            main = null
        }
        if(main == null) main = folder.child("$type.log")
        main!!.writeString("[${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))}] $text\n", true)
    }

    enum class LogType {
        Log, Warn, Error, Debug, Server, ServerWarn, ServerError, Client, ClientWarn, ClientError, Config, Player, PlayerWarn, PlayerError, Tap, WithDraw, Block, Deposit, Chat, Griefer, Web
    }
}