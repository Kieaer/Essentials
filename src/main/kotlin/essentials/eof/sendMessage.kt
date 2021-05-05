package essentials.eof

import arc.Core
import essentials.internal.Bundle
import mindustry.gen.Call
import mindustry.gen.Nulls
import mindustry.gen.Playerc

class sendMessage {
    var player: Playerc = Nulls.player
    var bundle: Bundle = Bundle()

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

    constructor(player: Playerc, bundle: Bundle){
        this.player = player
        this.bundle = bundle
    }

    operator fun get(msg: String, vararg parameter: String){
        Core.app.post {
            player.sendMessage(bundle.get(msg, *parameter))
        }
    }
}