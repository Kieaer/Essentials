package essentials.eof

import arc.Core
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Unit
import mindustry.world.Block
import mindustry.world.Tile

class constructFinish(tile: Tile, block: Block, builder: Unit, rotation: Byte, team: Team, config: Boolean?) {
    init {
        Core.app.post { Call.constructFinish(tile, block, builder, rotation, team, config) }
    }
}