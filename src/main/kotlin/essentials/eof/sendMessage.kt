package essentials.eof

import arc.Core
import mindustry.gen.Call
import mindustry.gen.Playerc

class sendMessage {
    constructor(player: Playerc, msg: String) {
        Core.app.post {
            player.sendMessage(msg)
        }
    }

    constructor(msg: String){
        Core.app.post{
            Call.sendMessage(msg)
        }
    }
}