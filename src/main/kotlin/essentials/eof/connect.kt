package essentials.eof

import arc.Core
import mindustry.gen.Call
import mindustry.gen.Playerc

class connect(player: Playerc, ip: String, port: Int) {
    init {
        Core.app.post {
            Call.connect(player.con(), ip, port)
        }
    }
}