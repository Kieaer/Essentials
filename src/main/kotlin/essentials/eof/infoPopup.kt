package essentials.eof

import arc.Core
import mindustry.gen.Call
import mindustry.gen.Playerc

class infoPopup {
    constructor(message: String, duration: Float, align: Int, top: Int, left: Int, bottom: Int, right: Int){
        Core.app.post{
            Call.infoPopup(message, duration, align, top, left, bottom, right)
        }
    }

    constructor(player: Playerc, message: String, duration: Float, align: Int, top: Int, left: Int, bottom: Int, right: Int){
        Core.app.post{
            Call.infoPopup(player.con(), message, duration, align, top, left, bottom, right)
        }
    }
}