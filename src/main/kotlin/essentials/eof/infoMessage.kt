package essentials.eof

import arc.Core
import mindustry.gen.Call
import mindustry.gen.Playerc

class infoMessage(val player: Playerc, val msg: String) {
    init {
        Core.app.post { Call.infoMessage(msg) }
    }
}