package essential.collect;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.util.Log;
import essential.core.Bundle;
import mindustry.game.EventType;
import mindustry.mod.Plugin;
import org.hjson.JsonArray;

public class Main extends Plugin {
    static Bundle bundle = new Bundle();
    private JsonArray playerActivities = new JsonArray();
    private Fi recordFile;


    @Override
    public void init() {
        bundle.setPrefix("[EssentialCollect]");

        Log.debug(bundle.get("event.plugin.starting"));

        recordFile = Core.settings.getDataDirectory().child("mods/Essentials/records.json");
        if (!recordFile.exists()) {
            recordFile.writeString("[]");
        } else {
            try {
                playerActivities = JsonArray.readJSON(recordFile.readString()).asArray();
            } catch (Exception e) {
                Log.err("Failed to read records.json", e);
                playerActivities = new JsonArray();
            }
        }

        setEvents();

        Log.debug(bundle.get("event.plugin.loaded"));
    }

    /**
     * Sets up event handlers to record player activities and save them to a JSON file.
     */
    private void setEvents() {
        // triggered when map loaded
        Events.on(EventType.PlayEvent.class, e -> {
            // Record map load event
            org.hjson.JsonObject mapLoad = new org.hjson.JsonObject();
            mapLoad.add("type", "map_load");
            mapLoad.add("map", mindustry.Vars.state.map.name());
            mapLoad.add("time", System.currentTimeMillis());
            playerActivities.add(mapLoad);

            // Log that we're recording player activities
            Log.info(bundle.get("event.recording.started"));
        });

        // if player joined
        Events.on(EventType.PlayerJoin.class, e -> {
            // Record player join event
            org.hjson.JsonObject playerJoin = new org.hjson.JsonObject();
            org.hjson.JsonObject player = new org.hjson.JsonObject();

            // Add player data
            player.add("name", e.player.name);
            if (e.player.unit() != null) {
                player.add("unit_x", e.player.unit().x);
                player.add("unit_y", e.player.unit().y);
                player.add("rotation", e.player.unit().rotation);
            }
            player.add("mouse_x", e.player.mouseX);
            player.add("mouse_y", e.player.mouseY);

            playerJoin.add("type", "player_join");
            playerJoin.add("player", player);
            playerJoin.add("time", System.currentTimeMillis());
            playerActivities.add(playerJoin);
        });

        // if player leaved
        Events.on(EventType.PlayerLeave.class, e -> {
            // Record player leave event
            org.hjson.JsonObject playerLeave = new org.hjson.JsonObject();
            org.hjson.JsonObject player = new org.hjson.JsonObject();

            // Add player data
            player.add("name", e.player.name);
            if (e.player.unit() != null) {
                player.add("unit_x", e.player.unit().x);
                player.add("unit_y", e.player.unit().y);
                player.add("rotation", e.player.unit().rotation);
            }
            player.add("mouse_x", e.player.mouseX);
            player.add("mouse_y", e.player.mouseY);

            playerLeave.add("type", "player_leave");
            playerLeave.add("player", player);
            playerLeave.add("time", System.currentTimeMillis());
            playerActivities.add(playerLeave);
        });

        // if player withdraw item from unit
        Events.on(EventType.WithdrawEvent.class, e -> {
            // Record withdraw event
            org.hjson.JsonObject withdraw = new org.hjson.JsonObject();
            org.hjson.JsonObject player = new org.hjson.JsonObject();

            // Add player data
            player.add("name", e.player.name);
            if (e.player.unit() != null) {
                player.add("unit_x", e.player.unit().x);
                player.add("unit_y", e.player.unit().y);
                player.add("rotation", e.player.unit().rotation);
            }
            player.add("mouse_x", e.player.mouseX);
            player.add("mouse_y", e.player.mouseY);

            withdraw.add("type", "withdraw");
            withdraw.add("player", player);
            withdraw.add("item", e.item.name);
            withdraw.add("block_name", e.tile.block().name);
            withdraw.add("amount", e.amount);
            withdraw.add("time", System.currentTimeMillis());
            playerActivities.add(withdraw);
        });

        // if player deposit item to unit
        Events.on(EventType.DepositEvent.class, e -> {
            // Record deposit event
            org.hjson.JsonObject deposit = new org.hjson.JsonObject();
            org.hjson.JsonObject player = new org.hjson.JsonObject();

            // Add player data
            player.add("name", e.player.name);
            if (e.player.unit() != null) {
                player.add("unit_x", e.player.unit().x);
                player.add("unit_y", e.player.unit().y);
                player.add("rotation", e.player.unit().rotation);
            }
            player.add("mouse_x", e.player.mouseX);
            player.add("mouse_y", e.player.mouseY);

            deposit.add("type", "deposit");
            deposit.add("player", player);
            deposit.add("item", e.item.name);
            deposit.add("amount", e.amount);
            deposit.add("block_name", e.tile.block().name);
            deposit.add("time", System.currentTimeMillis());
            playerActivities.add(deposit);
        });

        // if any unit destroyed
        Events.on(EventType.UnitDestroyEvent.class, e -> {
            // Record unit destroy event
            org.hjson.JsonObject unitDestroy = new org.hjson.JsonObject();
            unitDestroy.add("type", "unit_destroy");
            unitDestroy.add("unit", e.unit.type.name);
            unitDestroy.add("team", e.unit.team.toString());
            unitDestroy.add("time", System.currentTimeMillis());
            playerActivities.add(unitDestroy);
        });

        // if player configured block
        Events.on(EventType.ConfigEvent.class, e -> {
            // Record config event
            org.hjson.JsonObject config = new org.hjson.JsonObject();
            config.add("type", "config");

            if (e.player != null) {
                org.hjson.JsonObject player = new org.hjson.JsonObject();

                // Add player data
                player.add("name", e.player.name);
                if (e.player.unit() != null) {
                    player.add("unit_x", e.player.unit().x);
                    player.add("unit_y", e.player.unit().y);
                    player.add("rotation", e.player.unit().rotation);
                }
                player.add("mouse_x", e.player.mouseX);
                player.add("mouse_y", e.player.mouseY);

                config.add("player", player);
            }

            config.add("config", e.value.toString());
            config.add("block", e.tile.block().name);
            config.add("time", System.currentTimeMillis());
            playerActivities.add(config);
        });

        // if game over
        Events.on(EventType.GameOverEvent.class, e -> {
            // Save player activities to JSON file when game is over
            try {
                recordFile.writeString(playerActivities.toString());
                Log.info(bundle.get("event.records.saved"));
                // Reset player activities for next game
                playerActivities = new JsonArray();
            } catch (Exception ex) {
                Log.err("Failed to save records.json", ex);
            }
        });

        // if player tap block
        Events.on(EventType.TapEvent.class, e -> {
            // Record tap event
            org.hjson.JsonObject tap = new org.hjson.JsonObject();
            org.hjson.JsonObject player = new org.hjson.JsonObject();
            if (e.player.unit() != null) {
                player.add("unit_x", e.player.unit().x);
                player.add("unit_y", e.player.unit().y);
                player.add("rotation", e.player.unit().rotation);
                player.add("tile_on", e.player.tileOn().block().name);
                player.add("is_builder", e.player.isBuilder());
                player.add("is_dead", e.player.unit().dead);
                player.add("shooting", e.player.unit().isShooting);
            }
            player.add("mouse_x", e.player.mouseX);
            player.add("mouse_y", e.player.mouseY);
            player.add("boosting", e.player.boosting);

            tap.add("player", player);
            tap.add("type", "tap");
            tap.add("tile_x", e.tile.x);
            tap.add("tile_y", e.tile.y);
            tap.add("time", System.currentTimeMillis());
            playerActivities.add(tap);
        });

        // if player or unit picked other blocks or units
        Events.on(EventType.PickupEvent.class, e -> {
            // Record pickup event
            org.hjson.JsonObject pickup = new org.hjson.JsonObject();
            pickup.add("type", "pickup");

            if (e.carrier != null && e.carrier.isPlayer()) {
                org.hjson.JsonObject player = new org.hjson.JsonObject();
                mindustry.gen.Player p = e.carrier.getPlayer();

                // Add player data
                player.add("name", p.name);
                if (p.unit() != null) {
                    player.add("unit_x", e.unit.x);
                    player.add("unit_y", e.unit.y);
                    player.add("rotation", e.unit.rotation);
                    player.add("tile_on", e.unit.tileOn().block().name);
                    player.add("is_dead", e.unit.dead);
                    player.add("shooting", e.unit.isShooting);
                }
                player.add("mouse_x", p.mouseX);
                player.add("mouse_y", p.mouseY);

                pickup.add("player", player);
            }

            pickup.add("time", System.currentTimeMillis());
            playerActivities.add(pickup);
        });

        // if any unit created
        Events.on(EventType.UnitCreateEvent.class, e -> {
            // Record unit create event
            org.hjson.JsonObject unitCreate = new org.hjson.JsonObject();
            unitCreate.add("type", "unit_create");
            unitCreate.add("unit", e.unit.type.name);
            unitCreate.add("team", e.unit.team.toString());
            unitCreate.add("time", System.currentTimeMillis());
            playerActivities.add(unitCreate);
        });

        // if unit controlled by player or processor block
        Events.on(EventType.UnitControlEvent.class, e -> {
            // Record unit control event
            org.hjson.JsonObject unitControl = new org.hjson.JsonObject();
            unitControl.add("type", "unit_control");

            if (e.player != null) {
                org.hjson.JsonObject player = new org.hjson.JsonObject();

                // Add player data
                player.add("name", e.player.name);
                if (e.player.unit() != null) {
                    player.add("unit_x", e.player.unit().x);
                    player.add("unit_y", e.player.unit().y);
                    player.add("rotation", e.player.unit().rotation);
                }
                player.add("mouse_x", e.player.mouseX);
                player.add("mouse_y", e.player.mouseY);

                unitControl.add("player", player);
            }

            unitControl.add("unit", e.unit.type.name);
            unitControl.add("time", System.currentTimeMillis());
            playerActivities.add(unitControl);
        });

        // if player command building
        Events.on(EventType.BuildingCommandEvent.class, e -> {
            // Record building command event
            org.hjson.JsonObject buildingCommand = new org.hjson.JsonObject();
            buildingCommand.add("type", "building_command");

            if (e.player != null) {
                org.hjson.JsonObject player = new org.hjson.JsonObject();

                // Add player data
                player.add("name", e.player.name);
                if (e.player.unit() != null) {
                    player.add("unit_x", e.player.unit().x);
                    player.add("unit_y", e.player.unit().y);
                    player.add("rotation", e.player.unit().rotation);
                }
                player.add("mouse_x", e.player.mouseX);
                player.add("mouse_y", e.player.mouseY);

                buildingCommand.add("player", player);
            }

            buildingCommand.add("building", e.building.block.name);
            buildingCommand.add("time", System.currentTimeMillis());
            playerActivities.add(buildingCommand);
        });

        // if player or unit is building unit
        Events.on(EventType.BlockBuildBeginEvent.class, e -> {
            // Record block build begin event
            org.hjson.JsonObject blockBuildBegin = new org.hjson.JsonObject();
            blockBuildBegin.add("type", "block_build_begin");

            if (e.unit != null && e.unit.isPlayer()) {
                org.hjson.JsonObject player = new org.hjson.JsonObject();
                mindustry.gen.Player p = e.unit.getPlayer();

                // Add player data
                player.add("name", p.name);
                if (p.unit() != null) {
                    player.add("unit_x", p.unit().x);
                    player.add("unit_y", p.unit().y);
                    player.add("rotation", p.unit().rotation);
                }
                player.add("mouse_x", p.mouseX);
                player.add("mouse_y", p.mouseY);

                blockBuildBegin.add("player", player);
            }

            blockBuildBegin.add("block", e.tile.block().name);
            blockBuildBegin.add("tile_x", e.tile.x);
            blockBuildBegin.add("tile_y", e.tile.y);
            blockBuildBegin.add("time", System.currentTimeMillis());
            playerActivities.add(blockBuildBegin);
        });

        // if player or unit is block build end
        Events.on(EventType.BlockBuildEndEvent.class, e -> {
            // Record block build end event
            org.hjson.JsonObject blockBuildEnd = new org.hjson.JsonObject();
            blockBuildEnd.add("type", "block_build_end");

            if (e.unit != null && e.unit.isPlayer()) {
                org.hjson.JsonObject player = new org.hjson.JsonObject();
                mindustry.gen.Player p = e.unit.getPlayer();

                // Add player data
                player.add("name", p.name);
                if (p.unit() != null) {
                    player.add("unit_x", p.unit().x);
                    player.add("unit_y", p.unit().y);
                    player.add("rotation", p.unit().rotation);
                }
                player.add("mouse_x", p.mouseX);
                player.add("mouse_y", p.mouseY);

                blockBuildEnd.add("player", player);
            }

            blockBuildEnd.add("block", e.tile.block().name);
            blockBuildEnd.add("tile_x", e.tile.x);
            blockBuildEnd.add("tile_y", e.tile.y);
            blockBuildEnd.add("time", System.currentTimeMillis());
            playerActivities.add(blockBuildEnd);
        });

        // if player or unit rotated the target block
        Events.on(EventType.BuildRotateEvent.class, e -> {

        });

        // if player or unit is block remove or selected
        Events.on(EventType.BuildSelectEvent.class, e -> {

        });

        // if power generator exploded by pressure
        Events.on(EventType.GeneratorPressureExplodeEvent.class, e -> {

        });

        // if building destroyed by bullet
        Events.on(EventType.BuildingBulletDestroyEvent.class, e -> {

        });

        // if unit destroyed by bullet
        Events.on(EventType.UnitBulletDestroyEvent.class, e -> {

        });

        // if unit taken damage
        Events.on(EventType.UnitDamageEvent.class, e -> {

        });

        // if unit is drowned
        Events.on(EventType.UnitDrownEvent.class, e -> {

        });

        // if unit is unload from block
        Events.on(EventType.UnitUnloadEvent.class, e -> {

        });
    }
}
