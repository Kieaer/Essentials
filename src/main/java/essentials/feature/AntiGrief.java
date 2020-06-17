package essentials.feature;

import arc.math.Mathf;
import mindustry.gen.Unitc;
import mindustry.world.Tile;

public class AntiGrief {
    public float getDistanceToCore(Unitc unit, Tile tile) {
        Tile nearestCore = unit.closestCore().tile();
        return Mathf.dst(tile.x, tile.y, nearestCore.x, nearestCore.y);
    }
}