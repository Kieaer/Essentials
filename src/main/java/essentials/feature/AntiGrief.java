package essentials.feature;

import arc.math.Mathf;
import mindustry.entities.type.Unit;
import mindustry.world.Tile;

public class AntiGrief {
    public float getDistanceToCore(Unit unit, Tile tile) {
        Tile nearestCore = unit.getClosestCore().getTile();
        return Mathf.dst(tile.x, tile.y, nearestCore.x, nearestCore.y);
    }
}