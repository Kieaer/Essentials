package remake

import mindustry.gen.Playerc

class Trigger {
    fun load(){

    }

    fun loadPlayer(player: Playerc, data: DB.PlayerData){
        player.name(data.name)
    }
}