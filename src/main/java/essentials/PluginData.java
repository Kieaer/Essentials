package essentials;

import arc.Core;
import mindustry.world.Tile;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PluginData {
    // 일회성 플러그인 데이터
    public static ArrayList<nukeblock> nukeblock = new ArrayList<>();
    public static ArrayList<eventservers> eventservers = new ArrayList<>();
    public static ArrayList<powerblock> powerblock = new ArrayList<>();
    public static ArrayList<messagemonitor> messagemonitor = new ArrayList<>();
    public static ArrayList<messagejump> messagejump = new ArrayList<>();
    public static ArrayList<Tile> scancore = new ArrayList<>();
    public static ArrayList<Tile> nukedata = new ArrayList<>();
    public static ArrayList<Tile> nukeposition = new ArrayList<>();
    public static ArrayList<Process> process = new ArrayList<>();

    // 종료시 저장되는 플러그인 데이터
    public static ArrayList<jumpzone> jumpzone = new ArrayList<>();
    public static ArrayList<jumpcount> jumpcount = new ArrayList<>();
    public static ArrayList<jumptotal> jumptotal = new ArrayList<>();
    public static ArrayList<String> blacklist = new ArrayList<>();
    public static ArrayList<banned> banned = new ArrayList<>();

    public static class nukeblock{
        public final Tile tile;
        public final String name;

        nukeblock(Tile tile, String name){
            this.tile = tile;
            this.name = name;
        }
    }

    public static class eventservers{
        public final String roomname;
        public int port;

        eventservers(String roomname, int port){
            this.roomname = roomname;
            this.port = port;
        }
    }

    public static class powerblock{
        public final Tile messageblock;
        public final Tile tile;

        powerblock(Tile messageblock, Tile tile){
            this.messageblock = messageblock;
            this.tile = tile;
        }
    }

    public static class messagemonitor{
        public final Tile tile;

        messagemonitor(Tile tile){
            this.tile = tile;
        }
    }

    public static class messagejump{
        public final Tile tile;
        public final String message;

        messagejump(Tile tile, String message){
            this.tile = tile;
            this.message = message;
        }
    }

    public static class jumpzone implements Serializable{
        public final Tile start;
        public final Tile finish;
        public final String ip;

        jumpzone(Tile start, Tile finish, String ip){
            this.start = start;
            this.finish = finish;
            this.ip = ip;
        }
    }

    public static class jumpcount implements Serializable{
        public final Tile tile;
        public final String serverip;
        public int players;
        public int numbersize;

        jumpcount(Tile tile, String serverip, int players, int numbersize){
            this.tile = tile;
            this.serverip = serverip;
            this.players = players;
            this.numbersize = numbersize;
        }
    }

    public static class jumptotal implements Serializable{
        public final Tile tile;
        public int totalplayers;
        public int numbersize;

        jumptotal(Tile tile, int totalplayers, int numbersize){
            this.tile = tile;
            this.totalplayers = totalplayers;
            this.numbersize = numbersize;
        }
    }

    public static class banned implements Serializable{
        public final LocalDateTime time;
        public final String name;
        public final String uuid;

        banned(LocalDateTime time, String name, String uuid){
            this.time = time;
            this.name = name;
            this.uuid = uuid;
        }
    }

    public static void saveall(){
        Map<String, ArrayList<?>> map = new HashMap<>();
        map.put("jumpzone", jumpzone);
        map.put("jumpcount", jumpcount);
        map.put("jumptotal", jumptotal);
        map.put("blacklist", blacklist);
        map.put("banned",banned);

        try {
            FileOutputStream fos = new FileOutputStream(Core.settings.getDataDirectory().child("mods/Essentials/data/PluginData.object").file());
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(map);
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked") // 의도적인 작동임
    public static void loadall(){
        if(!Core.settings.getDataDirectory().child("mods/Essentials/data/PluginData.object").exists()){
            Map<String, ArrayList<Object>> map = new HashMap<>();
            try {
                FileOutputStream fos = new FileOutputStream(Core.settings.getDataDirectory().child("mods/Essentials/data/PluginData.object").file());
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(map);
                oos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                FileInputStream fis = new FileInputStream(Core.settings.getDataDirectory().child("mods/Essentials/data/PluginData.object").file());
                ObjectInputStream ois = new ObjectInputStream(fis);
                Map<String, ArrayList> map = (Map<String, ArrayList>) ois.readObject();
                jumpzone = map.get("jumpzone");
                jumpcount = map.get("jumpcount");
                jumptotal = map.get("jumptotal");
                blacklist = map.get("blacklist");
                banned = map.get("banned");
                ois.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
