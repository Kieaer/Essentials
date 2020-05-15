package essentials.feature;

import arc.Events;
import arc.math.Mathf;
import arc.struct.Array;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.entities.type.Unit;
import mindustry.game.EventType;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.game.EventType.PlayerChatEvent;
import mindustry.gen.Call;
import mindustry.world.Block;
import mindustry.world.Tile;

import static mindustry.Vars.world;

public class AntiGrief {
    public final Array<Array<worldTileInfo>> worldInfos = new Array<>();

    public AntiGrief() {
        Events.on(PlayerChatEvent.class, e -> {
            if (e.message.length() > Vars.maxTextLength || e.message.contains("Nexity#2671")) {
                Call.onKick(e.player.con, "Hacked client detected");
            }
        });

        Events.on(BlockBuildEndEvent.class, e -> {
            if (getDistanceToCore(e.player, e.tile) < 30) {
                Call.onDeconstructFinish(e.tile, Blocks.air, e.player.id);
            }
        });

        Events.on(EventType.Trigger.update.getClass(), e -> {
            Array<worldTileInfo> tileInfos = new Array<>();
            for (int x = 0; x < world.width(); x++) {
                for (int y = 0; y < world.height(); y++) {
                    tileInfos.add(new worldTileInfo(world.tile(x, y)));
                }
            }
            worldInfos.add(tileInfos);
            if (worldInfos.size > 500) {
                worldInfos.removeRange(0, 100);
            }

            // TODO check nexity packet
        });
    }

    public float getDistanceToCore(Unit unit, Tile tile) {
        Tile nearestCore = unit.getClosestCore().getTile();
        return Mathf.dst(tile.x, tile.y, nearestCore.x, nearestCore.y);
    }
}

class worldTileInfo {
    public final int x;
    public final int y;
    public final Block block;

    worldTileInfo(Tile tile) {
        this.x = tile.x;
        this.y = tile.y;
        this.block = tile.block();
    }
}

class playerInfo {
    public final String name;
    public final Array<blockInfo> blocks = new Array<blockInfo>();

    public playerInfo(String name) {
        this.name = name;
    }
}

class blockInfo {
    public final int x;
    public final int y;
    public final Block block;

    public blockInfo(Tile tile) {
        this.x = tile.x;
        this.y = tile.y;
        this.block = tile.block();
    }
}