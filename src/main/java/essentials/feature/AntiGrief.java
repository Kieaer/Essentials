package essentials.feature;

public class AntiGrief {
    /*public final Array<Array<worldTileInfo>> worldInfos = new Array<>();

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
    }*/
}