package essentials.core;

import mindustry.entities.type.Player;
import mindustry.world.Tile;

import java.util.ArrayList;

public class AI {
    Tile start;
    Tile target;
    Player player;

    ArrayList<Tile> opened = new ArrayList<>();
    ArrayList<Tile> closed = new ArrayList<>();
    ArrayList<String> camefrom = new ArrayList<>();
    ArrayList<Tile> result;

    final int rot = 4;

    public AI(Tile start, Tile target, Player player) {
        this.start = start;
        this.target = target;
        this.player = player;
    }

}