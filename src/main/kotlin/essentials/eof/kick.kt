package essentials.eof

import arc.Core
import mindustry.gen.Playerc
import mindustry.net.Packets

class kick {
    constructor(player: Playerc, reason: Packets.KickReason){
        Core.app.post{
            player.kick(reason)
        }
    }

    constructor(player: Playerc, reason: String){
        Core.app.post{
            player.kick(reason)
        }
    }
}