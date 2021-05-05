package essentials.eof

import arc.Core
import mindustry.gen.Call
import mindustry.gen.Playerc

class label {
    constructor(player: Playerc, message: String, duration: Float, x: Float, y: Float) {
        Core.app.post {
            Call.label(player.con(), message, duration, x, y)
        }
    }

    constructor(message: String, duration: Float, x: Float, y: Float) {
        Core.app.post {
            Call.label(message, duration, x, y)
        }
    }
}