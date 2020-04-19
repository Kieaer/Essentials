package essentials.core.plugin;

import arc.struct.Array;
import essentials.internal.CrashReport;
import essentials.internal.Log;
import mindustry.world.Tile;

import java.io.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static essentials.Main.root;
import static mindustry.Vars.world;

public class PluginData {
    // 일회성 플러그인 데이터
    public Array<nukeblock> nukeblock = new Array<>();
    public Array<eventservers> eventservers = new Array<>();
    public Array<powerblock> powerblock = new Array<>();
    public Array<messagemonitor> messagemonitor = new Array<>();
    public Array<messagejump> messagejump = new Array<>();
    public Array<Tile> scancore = new Array<>();
    public Array<Tile> nukedata = new Array<>();
    public Array<Tile> nukeposition = new Array<>();
    public Array<Process> process = new Array<>();

    // 종료시 저장되는 플러그인 데이터
    public Array<jumpzone> jumpzone = new Array<>();
    public Array<jumpcount> jumpcount = new Array<>();
    public Array<jumptotal> jumptotal = new Array<>();
    public Array<String> blacklist = new Array<>();
    public Array<banned> banned = new Array<>();

    public void saveAll() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("jumpzone", jumpzone.toArray());
        map.put("jumpcount", jumpcount.toArray());
        map.put("jumptotal", jumptotal.toArray());
        map.put("blacklist", blacklist.toArray());
        map.put("banned", banned.toArray());

        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ObjectOutputStream o = new ObjectOutputStream(b);
        o.writeObject(map);
        root.child("data/PluginData.object").writeBytes(b.toByteArray(), false);
        o.close();
        b.close();
    }

    @SuppressWarnings("unchecked") // 의도적인 작동임
    public void loadall() {
        ByteArrayOutputStream fos = null;
        ObjectOutputStream oos = null;
        ByteArrayInputStream fis = null;
        ObjectInputStream ois = null;
        try {
            if (!root.child("data/PluginData.object").exists()) {
                Map<String, Array<Object>> map = new HashMap<>();
                map.put("jumpzone", new Array<>());
                map.put("jumpcount", new Array<>());
                map.put("jumptotal", new Array<>());
                map.put("blacklist", new Array<>());
                map.put("banned", new Array<>());

                fos = new ByteArrayOutputStream();
                oos = new ObjectOutputStream(fos);
                oos.writeObject(map);

                root.child("data/PluginData.object").writeBytes(fos.toByteArray(), false);
                oos.close();
                fos.close();

            } else {
                fis = new ByteArrayInputStream(root.child("data/PluginData.object").readBytes());
                ois = new ObjectInputStream(fis);
                Map<String, Object> map = (HashMap<String, Object>) ois.readObject();
                jumpzone = (Array<jumpzone>) map.get("jumpzone");
                jumpcount = (Array<jumpcount>) map.get("jumpcount");
                jumptotal = (Array<jumptotal>) map.get("jumptotal");
                blacklist = (Array<String>) map.get("blacklist");
                banned = (Array<banned>) map.get("banned");
                ois.close();
                fis.close();
                Log.info("plugindata-loaded");
            }
        } catch (Exception i) {
            try {
                if (fos != null) fos.close();
                if (oos != null) oos.close();
                if (fis != null) fis.close();
                if (ois != null) ois.close();
                root.child("data/PluginData.object").delete();
            } catch (Exception e) {
                new CrashReport(e);
            }
        }
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
        public final Tile tile;

        public messagemonitor(Tile tile) {
            this.tile = tile;
        }
    }

    public static class messagejump {
        public final Tile tile;
        public final String message;

        public messagejump(Tile tile, String message) {
            this.tile = tile;
            this.message = message;
        }
    }

    public static class jumpzone implements Serializable {
        public final int startx;
        public final int starty;
        public final int finishx;
        public final int finishy;
        public final String ip;
        public final int port;
        public final boolean touch;

        public jumpzone(Tile start, Tile finish, boolean touch, String ip, int port) {
            this.startx = start.x;
            this.starty = start.y;
            this.finishx = finish.x;
            this.finishy = finish.y;
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

    public static class jumpcount implements Serializable {
        public final int x;
        public final int y;
        public final String ip;
        public final int port;
        public int players;
        public int numbersize;

        public jumpcount(Tile tile, String ip, int port, int players, int numbersize) {
            this.x = tile.x;
            this.y = tile.y;
            this.ip = ip;
            this.port = port;
            this.players = players;
            this.numbersize = numbersize;
        }

        public Tile getTile() {
            return world.tile(x, y);
        }
    }

    public static class jumptotal implements Serializable {
        public final int x;
        public final int y;
        public int totalplayers;
        public int numbersize;

        public jumptotal(Tile tile, int totalplayers, int numbersize) {
            this.x = tile.x;
            this.y = tile.y;
            this.totalplayers = totalplayers;
            this.numbersize = numbersize;
        }

        public Tile getTile() {
            return world.tile(x, y);
        }
    }

    public static class banned implements Serializable {
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

        public LocalDateTime getTime() {
            return LocalDateTime.parse(time);
        }
    }
}
