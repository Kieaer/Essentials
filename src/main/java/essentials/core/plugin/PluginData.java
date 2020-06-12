package essentials.core.plugin;

import arc.struct.Array;
import arc.util.serialization.Json;
import essentials.internal.Log;
import mindustry.world.Tile;
import mindustry.world.blocks.logic.MessageBlock;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.io.*;
import java.time.LocalDateTime;

import static essentials.Main.root;
import static mindustry.Vars.world;

public class PluginData {
    private final Json json = new Json();

    // 일회성 플러그인 데이터
    public Array<nukeblock> nukeblock = new Array<>();
    public Array<eventservers> eventservers = new Array<>();
    public Array<powerblock> powerblock = new Array<>();
    public Array<messagemonitor> messagemonitor = new Array<>();
    public Array<messagewarp> messagewarp = new Array<>();
    public Array<Tile> scancore = new Array<>();
    public Array<Tile> nukedata = new Array<>();
    public Array<Tile> nukeposition = new Array<>();
    public Array<Process> process = new Array<>();

    // 종료시 저장되는 플러그인 데이터
    public Array<warpzone> warpzones = new Array<>();
    public Array<warpblock> warpblocks = new Array<>();
    public Array<warpcount> warpcounts = new Array<>();
    public Array<warptotal> warptotals = new Array<>();
    public Array<String> blacklist = new Array<>();
    public Array<banned> banned = new Array<>();

    public void saveAll() {
        JsonObject data = new JsonObject();
        JsonArray buffer = new JsonArray();
        for (int a = 0; a < warpzones.size; a++) {
            buffer.add(JsonValue.readHjson(json.prettyPrint(warpzones.get(a))));
        }
        data.add("warpzones", buffer);
        buffer = new JsonArray();
        for (int a = 0; a < warpblocks.size; a++) {
            buffer.add(JsonValue.readHjson(json.prettyPrint(warpblocks.get(a))));
        }
        data.add("warpblocks", buffer);
        buffer = new JsonArray();
        for (int a = 0; a < warpcounts.size; a++) {
            buffer.add(JsonValue.readHjson(json.prettyPrint(warpcounts.get(a))));
        }
        data.add("warpcounts", buffer);
        buffer = new JsonArray();
        for (int a = 0; a < warptotals.size; a++) {
            buffer.add(JsonValue.readHjson(json.prettyPrint(warptotals.get(a))));
        }
        data.add("warptotals", buffer);
        buffer = new JsonArray();
        for (int a = 0; a < blacklist.size; a++) {
            buffer.add(blacklist.get(a));
        }
        data.add("blacklist", buffer);
        buffer = new JsonArray();
        for (int a = 0; a < banned.size; a++) {
            buffer.add(JsonValue.readHjson(json.prettyPrint(banned.get(a))));
        }
        data.add("banned", buffer);
        root.child("data/PluginData.object").writeString(data.toString(), false);
    }

    public void loadall() {
        try {
            if (!root.child("data/PluginData.object").exists()) {
                saveAll();
            } else {
                JsonObject data = JsonValue.readJSON(root.child("data/PluginData.object").readString()).asObject();
                for (int a = 0; a < data.get("warpzones").asArray().size(); a++) {
                    JsonObject buffer = data.get("warpzones").asArray().get(a).asObject();
                    warpzones.add(new warpzone(
                            buffer.get("mapName").asString(),
                            buffer.get("startx").asInt(),
                            buffer.get("starty").asInt(),
                            buffer.get("finishx").asInt(),
                            buffer.get("finishy").asInt(),
                            buffer.get("touch").asBoolean(),
                            buffer.get("ip").asString(),
                            buffer.get("port").asInt()
                    ));
                }

                for (int a = 0; a < data.get("warpblocks").asArray().size(); a++) {
                    JsonObject buffer = data.get("warpblocks").asArray().get(a).asObject();
                    warpblocks.add(new warpblock(
                            buffer.get("mapName").asString(),
                            buffer.get("tilex").asInt(),
                            buffer.get("tiley").asInt(),
                            buffer.get("tileName").asString(),
                            buffer.get("size").asInt(),
                            buffer.get("ip").asString(),
                            buffer.get("port").asInt(),
                            buffer.get("description").asString()
                    ));
                }

                for (int a = 0; a < data.get("warpcounts").asArray().size(); a++) {
                    JsonObject buffer = data.get("warpcounts").asArray().get(a).asObject();
                    warpcounts.add(new warpcount(
                            buffer.get("mapName").asString(),
                            buffer.get("x").asInt(),
                            buffer.get("y").asInt(),
                            buffer.get("ip").asString(),
                            buffer.get("port").asInt(),
                            buffer.get("players").asInt(),
                            buffer.get("numbersize").asInt()
                    ));
                }

                for (int a = 0; a < data.get("warptotals").asArray().size(); a++) {
                    JsonObject buffer = data.get("warptotals").asArray().get(a).asObject();
                    warptotals.add(new warptotal(
                            buffer.get("mapName").asString(),
                            buffer.get("x").asInt(),
                            buffer.get("y").asInt(),
                            buffer.get("totalplayers").asInt(),
                            buffer.get("numbersize").asInt()
                    ));
                }

                for (int a = 0; a < data.get("blacklist").asArray().size(); a++) {
                    JsonArray buffer = data.get("warpzones").asArray().get(a).asArray();
                    blacklist.add(buffer.get(a).asString());
                }

                for (int a = 0; a < data.get("banned").asArray().size(); a++) {
                    JsonObject buffer = data.get("banned").asArray().get(a).asObject();
                    banned.add(new banned(
                            buffer.get("time").asString(),
                            buffer.get("name").asString(),
                            buffer.get("uuid").asString(),
                            buffer.get("reason").asString()
                    ));
                }
                Log.info("plugindata-loaded");
            }
        } catch (Exception i) {
            i.printStackTrace();
            root.child("data/PluginData.object").delete();
        }
    }

    private static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ObjectOutputStream o = new ObjectOutputStream(b);
        o.writeObject(obj);
        return b.toByteArray();
    }

    public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream b = new ByteArrayInputStream(bytes);
        ObjectInputStream o = new ObjectInputStream(b);
        return o.readObject();
    }

    public static class nukeblock {
        public final Tile tile;
        public final String name;

        public nukeblock(Tile tile, String name) {
            this.tile = tile;
            this.name = name;
        }
    }

    public static class eventservers {
        public final String roomname;
        public int port;

        public eventservers(String roomname, int port) {
            this.roomname = roomname;
            this.port = port;
        }
    }

    public static class powerblock {
        public final Tile messageblock;
        public final Tile tile;

        public powerblock(Tile messageblock, Tile tile) {
            this.messageblock = messageblock;
            this.tile = tile;
        }
    }

    public static class messagemonitor {
        public final MessageBlock.MessageBlockEntity entity;

        public messagemonitor(MessageBlock.MessageBlockEntity entity) {
            this.entity = entity;
        }
    }

    public static class messagewarp {
        public final Tile tile;
        public final String message;

        public messagewarp(Tile tile, String message) {
            this.tile = tile;
            this.message = message;
        }
    }

    public static class warpzone {
        public final String mapName;
        public final int startx;
        public final int starty;
        public final int finishx;
        public final int finishy;
        public final String ip;
        public final int port;
        public final boolean touch;

        public warpzone(String mapName, Tile start, Tile finish, boolean touch, String ip, int port) {
            this.mapName = mapName;
            this.startx = start.x;
            this.starty = start.y;
            this.finishx = finish.x;
            this.finishy = finish.y;
            this.ip = ip;
            this.port = port;
            this.touch = touch;
        }

        private warpzone(String mapName, int startx, int starty, int finishx, int finishy, boolean touch, String ip, int port) {
            this.mapName = mapName;
            this.startx = startx;
            this.starty = starty;
            this.finishx = finishx;
            this.finishy = finishy;
            this.ip = ip;
            this.port = port;
            this.touch = touch;
        }

        public Tile getStartTile() {
            return world.tile(startx, starty);
        }

        public Tile getFinishTile() {
            return world.tile(finishx, finishy);
        }
    }

    public static class warpblock {
        public final String mapName;
        public final int tilex;
        public final int tiley;
        public final String tileName;
        public final String ip;
        public final int port;
        public final String description;
        public final int size;

        public warpblock(String mapName, Tile tile, String ip, int port, String description) {
            this.mapName = mapName;

            this.ip = ip;
            this.port = port;
            this.description = description;

            this.tilex = tile.entity.tileX();
            this.tiley = tile.entity.tileY();
            this.tileName = tile.entity.block.name;
            this.size = tile.entity.block.size;
        }

        private warpblock(String mapName, int tilex, int tiley, String tileName, int size, String ip, int port, String description) {
            this.mapName = mapName;

            this.ip = ip;
            this.port = port;
            this.description = description;

            this.tilex = tilex;
            this.tiley = tiley;
            this.tileName = tileName;
            this.size = size;
        }
    }

    public static class warpcount {
        public final String mapName;
        public final int x;
        public final int y;
        public final String ip;
        public final int port;
        public int players;
        public int numbersize;

        public warpcount(String mapName, Tile tile, String ip, int port, int players, int numbersize) {
            this.mapName = mapName;
            this.x = tile.x;
            this.y = tile.y;
            this.ip = ip;
            this.port = port;
            this.players = players;
            this.numbersize = numbersize;
        }

        private warpcount(String mapName, int x, int y, String ip, int port, int players, int numbersize) {
            this.mapName = mapName;
            this.x = x;
            this.y = y;
            this.ip = ip;
            this.port = port;
            this.players = players;
            this.numbersize = numbersize;
        }

        public Tile getTile() {
            return world.tile(x, y);
        }
    }

    public static class warptotal {
        public final String mapName;
        public final int x;
        public final int y;
        public int totalplayers;
        public int numbersize;

        public warptotal(String mapName, Tile tile, int totalplayers, int numbersize) {
            this.mapName = mapName;
            this.x = tile.x;
            this.y = tile.y;
            this.totalplayers = totalplayers;
            this.numbersize = numbersize;
        }

        private warptotal(String mapName, int x, int y, int totalplayers, int numbersize) {
            this.mapName = mapName;
            this.x = x;
            this.y = y;
            this.totalplayers = totalplayers;
            this.numbersize = numbersize;
        }

        public Tile getTile() {
            return world.tile(x, y);
        }
    }

    public static class banned {
        public final String time;
        public final String name;
        public final String uuid;
        public final String reason;

        public banned(LocalDateTime time, String name, String uuid, String reason) {
            this.time = time.toString();
            this.name = name;
            this.uuid = uuid;
            this.reason = reason;
        }

        private banned(String time, String name, String uuid, String reason) {
            this.time = time;
            this.name = name;
            this.uuid = uuid;
            this.reason = reason;
        }

        public LocalDateTime getTime() {
            return LocalDateTime.parse(time);
        }
    }
}
