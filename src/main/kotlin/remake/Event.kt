package remake

import arc.Events
import mindustry.Vars.netServer
import mindustry.game.EventType
import mindustry.game.EventType.ConfigEvent
import mindustry.gen.Call

object Event {
    fun register(){
        Events.on(EventType.PlayerChatEvent::class.java){
            if (netServer.chatFormatter.format(it.player, it.player.name) != null) {
                Call.sendMessage(
                    Permission.data[it.player.id].asObject()["chatFormat"].asString()
                        .replace("%1", netServer.chatFormatter.format(it.player, it.player.name))
                        .replace("%2", it.message)
                )
            }
        }

        Events.on(EventType.WithdrawEvent::class.java){

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
                val player = DB[it.unit.player.uuid()]
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
            val data = DB[it.player.uuid()]
            if(data != null){
                Trigger.loadPlayer(it.player, data)
            } else {
                if (Config.authType == Config.AuthType.None){
                    Trigger.createPlayer(it.player, null)
                }
            }
        }

        Events.on(EventType.PlayerLeave::class.java){

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
}