package remake.core.plugin;

import mindustry.world.Tile;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static mindustry.Vars.world;
import static remake.Main.root;

public class PluginData {
    // 일회성 플러그인 데이터
    public ArrayList<nukeblock> nukeblock = new ArrayList<>();
    public ArrayList<eventservers> eventservers = new ArrayList<>();
    public ArrayList<powerblock> powerblock = new ArrayList<>();
    public ArrayList<messagemonitor> messagemonitor = new ArrayList<>();
    public ArrayList<messagejump> messagejump = new ArrayList<>();
    public ArrayList<Tile> scancore = new ArrayList<>();
    public ArrayList<Tile> nukedata = new ArrayList<>();
    public ArrayList<Tile> nukeposition = new ArrayList<>();
    public ArrayList<Process> process = new ArrayList<>();
    public ArrayList<maildata> emailauth = new ArrayList<>();

    // 종료시 저장되는 플러그인 데이터
    public ArrayList<jumpzone> jumpzone = new ArrayList<>();
    public ArrayList<jumpcount> jumpcount = new ArrayList<>();
    public ArrayList<jumptotal> jumptotal = new ArrayList<>();
    public ArrayList<String> blacklist = new ArrayList<>();
    public ArrayList<banned> banned = new ArrayList<>();

    public void saveall() throws Exception {
        Map<String, ArrayList<?>> map = new HashMap<>();
        map.put("jumpzone", jumpzone);
        map.put("jumpcount", jumpcount);
        map.put("jumptotal", jumptotal);
        map.put("blacklist", blacklist);
        map.put("banned", banned);

        FileOutputStream fos = new FileOutputStream(root.child("data/data.object").file());
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(map);
        oos.close();
    }

    @SuppressWarnings("unchecked") // 의도적인 작동임
    public void loadall() throws Exception {
        if (!root.child("data/data.object").exists()) {
            Map<String, ArrayList<Object>> map = new HashMap<>();
            FileOutputStream fos = new FileOutputStream(root.child("data/PluginData.object").file());
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            map.put("jumpzone", new ArrayList<>());
            map.put("jumpcount", new ArrayList<>());
            map.put("jumptotal", new ArrayList<>());
            map.put("blacklist", new ArrayList<>());
            map.put("banned", new ArrayList<>());
            oos.writeObject(map);
            oos.close();
        } else {
            FileInputStream fis = new FileInputStream(root.child("data/PluginData.object").file());
            ObjectInputStream ois = new ObjectInputStream(fis);
            Map<String, Object> map = (Map<String, Object>) ois.readObject();
            jumpzone = (ArrayList<jumpzone>) map.get("jumpzone");
            jumpcount = (ArrayList<jumpcount>) map.get("jumpcount");
            jumptotal = (ArrayList<jumptotal>) map.get("jumptotal");
            blacklist = (ArrayList<String>) map.get("blacklist");
            banned = (ArrayList<banned>) map.get("banned");
            ois.close();
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

    public static class maildata {
        public final String authkey;
        public final String uuid;
        public final String id;
        public final String pw;
        public final String email;

        public maildata(String uuid, String authkey, String id, String pw, String email) {
            this.authkey = authkey;
            this.uuid = uuid;
            this.id = id;
            this.pw = pw;
            this.email = email;
        }
    }

    public static class jumpzone implements Serializable {
        public final int startx;
        public final int starty;
        public final int finishx;
        public final int finishy;
        public final String ip;
        public final boolean touch;

        public jumpzone(Tile start, Tile finish, boolean touch, String ip) {
            this.startx = start.x;
            this.starty = start.y;
            this.finishx = finish.x;
            this.finishy = finish.y;
            this.ip = ip;
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
