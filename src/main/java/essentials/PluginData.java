package essentials;

import mindustry.world.Tile;

import java.util.ArrayList;

public class PluginData {
    public static ArrayList<nukeblock> nukeblock = new ArrayList<>();
    public static ArrayList<eventservers> eventservers = new ArrayList<>();
    public static ArrayList<powerblock> powerblock = new ArrayList<>();
    public static ArrayList<messagemonitor> messagemonitor = new ArrayList<>();
    public static ArrayList<messagejump> messagejump = new ArrayList<>();
    public static ArrayList<Tile> scancore = new ArrayList<>();
    public static ArrayList<Tile> nukedata = new ArrayList<>();
    public static ArrayList<Tile> nukeposition = new ArrayList<>();
    public static ArrayList<Process> process = new ArrayList<>();

    public static class nukeblock{
        public Tile tile;
        public String name;

        nukeblock(Tile tile, String name){
            this.tile = tile;
            this.name = name;
        }
    }

    public static class eventservers{
        public String roomname;
        public int port;

        eventservers(String roomname, int port){
            this.roomname = roomname;
            this.port = port;
        }
    }

    public static class powerblock{
        public Tile messageblock;
        public Tile tile;

        powerblock(Tile messageblock, Tile tile){
            this.messageblock = messageblock;
            this.tile = tile;
        }
    }

    public static class messagemonitor{
        public Tile tile;

        messagemonitor(Tile tile){
            this.tile = tile;
        }
    }

    public static class messagejump{
        public Tile tile;
        public String message;

        messagejump(Tile tile, String message){
            this.tile = tile;
            this.message = message;
        }
    }
}
