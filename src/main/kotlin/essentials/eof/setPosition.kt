package essentials.eof

import arc.Core
import mindustry.gen.Call
import mindustry.gen.Playerc

class setPosition(player: Playerc, x: Float, y: Float) {
    init{
        Core.app.post{
            Call.setPosition(player.con(), x, y)
            player.unit().set(x, y)
            player.snapSync()
        }
    }
}