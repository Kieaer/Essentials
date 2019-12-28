package essentials.core;

import mindustry.content.Blocks;
import mindustry.entities.type.Player;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.world.Tile;

import java.util.ArrayList;

import static java.lang.Thread.sleep;
import static mindustry.Vars.world;

public class AI {
    Tile start;
    Tile target;
    Player player;

    final int rot = 4;

    ArrayList<Tile> open = new ArrayList<>();
    ArrayList<Tile> close = new ArrayList<>();
    ArrayList<String> from = new ArrayList<>();

    public AI(Tile start, Tile target, Player player) {
        this.start = start;
        this.target = target;
        this.player = player;
    }

    public ArrayList<Tile> auto() {
        Tile current = start;
        close.add(start);
        for (int a = 0; a < rot; a++) open.add(start.getNearby(a));
        while(current != target) {
            for (int a = 0; a < open.size(); a++) {
                current = open.get(a);
                Call.onConstructFinish(open.get(a), Blocks.metalFloor,0, (byte) 0,Team.sharded,false);
                for (int rotate = 0; rotate < rot; rotate++) {
                    if (current.getNearby(rotate).passable()) {
                        boolean closed = false;
                        for (int b = 0; b < close.size(); b++) {
                            if (close.get(b) == current.getNearby(rotate)) {
                                closed = true;
                                break;
                            }
                        }
                        if (!closed) {
                            Call.onConstructFinish(open.get(a).getNearby(rotate), Blocks.metalFloor2,0, (byte) 0,Team.sharded,false);
                            from.add(current.x + "/" + current.y + "/" + current.getNearby(rotate).x + "/" + current.getNearby(rotate).y + "/" + rotate);
                            open.add(current.getNearby(rotate));
                        }
                    }
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                open.remove(current);
            }
            try {
                sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        ArrayList<Tile> path = new ArrayList<>();
        current = target;
        while(target != start){
            for(int a=0;a<from.size();a++){
                String[] data = from.get(a).split("/");
                int targetx = Integer.parseInt(data[0]);
                int targety = Integer.parseInt(data[1]);
                int startx = Integer.parseInt(data[2]);
                int starty = Integer.parseInt(data[3]);
                int rot = Integer.parseInt(data[4]);
                Tile target = world.tile(targetx, targety);
                Tile start = world.tile(startx, starty);
                if(start == current){
                    path.add(target);
                    current = target;
                }
            }
        }
        return path;
    }
}