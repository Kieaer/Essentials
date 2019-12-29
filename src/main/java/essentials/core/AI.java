package essentials.core;

import arc.graphics.Color;
import mindustry.content.Blocks;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.world.Tile;

import java.util.ArrayList;

import static mindustry.Vars.world;

public class AI{
    int startx;
    int starty;
    int targetx;
    int targety;

    Tile current;
    Tile target;

    Node[][] map;

    Algorithm Alg = new Algorithm();

    boolean solving = false;
    int count = 0;

    public AI(Tile start, Tile target) {
        this.startx = start.x;
        this.starty = start.y;
        this.targetx = target.x;
        this.targety = target.y;
        this.current = start;
        this.target = target;

        map = new Node[world.width()][world.height()];
        for(int x=0;x<world.width();x++){
            for(int y=0;y<world.height();y++){
                if(!world.tile(x,y).solid() && world.tile(x,y).floor().drownTime <= 0f){
                    map[x][y] = new Node(3,x,y);
                } else {
                    map[x][y] = new Node(2,x,y);
                }
            }
        }
    }

    public void target(){
        solving = true;
        map[targetx][targety] = new Node(1,targetx,targety);
        Alg.AStar();
    }

    public void findore(){
        solving = true;
        boolean temp = false;
        for(int x=startx-25;x<startx+25;x++) {
            if(!temp) {
                for (int y = starty-25; y < starty+25; y++) {
                    if(!temp) {
                        if ((world.tile(x, y).overlayID() == 149 || world.tile(x, y).overlayID() == 150 || world.tile(x, y).overlayID() == 153 || world.tile(x, y).overlayID() == 154) && !world.tile(x,y).solid() && world.tile(x,y).passable()) {
                            map[x][y] = new Node(1, x, y);
                            target = world.tile(x, y);
                            temp = true;
                        }
                    }
                }
            }
        }
        Alg.AStar();
        Call.onConstructFinish(target,Blocks.blastDrill,0,(byte) 0,Team.sharded,false);
    }

    class Algorithm {
        public void AStar() {
            ArrayList<Node> priority = new ArrayList<Node>();
            priority.add(map[startx][starty]);
            while (solving) {
                if (priority.size() <= 0) {
                    solving = false;
                    break;
                }
                int hops = priority.get(0).getHops() + 1;
                ArrayList<Node> explored = exploreNeighbors(priority.get(0), hops);
                if (explored.size() > 0) {
                    priority.remove(0);
                    priority.addAll(explored);
                } else {
                    priority.remove(0);
                }
                sortQue(priority);
            }
        }

        public ArrayList<Node> sortQue(ArrayList<Node> sort) {
            int c = 0;
            while (c < sort.size()) {
                int sm = c;
                for (int i = c + 1; i < sort.size(); i++) {
                    if (sort.get(i).getEuclidDist() + sort.get(i).getHops() < sort.get(sm).getEuclidDist() + sort.get(sm).getHops())
                        sm = i;
                }
                if (c != sm) {
                    Node temp = sort.get(c);
                    sort.set(c, sort.get(sm));
                    sort.set(sm, temp);
                }
                c++;
            }
            return sort;
        }

        public ArrayList<Node> exploreNeighbors(Node current, int hops) {
            ArrayList<Node> explored = new ArrayList<Node>();
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    int xbound = current.getX() + a;
                    int ybound = current.getY() + b;
                    if ((xbound > -1 && xbound < world.width()) && (ybound > -1 && ybound < world.height())) {
                        Node neighbor = map[xbound][ybound];
                        Call.createLighting(0,Team.sharded, Color.white,0,xbound,ybound,0,1);
                        if ((neighbor.getHops() == -1 || neighbor.getHops() > hops) && neighbor.getType() != 2) {
                            explore(neighbor, current.getX(), current.getY(), hops);
                            explored.add(neighbor);
                        }
                    }
                }
            }
            return explored;
        }

        public void explore(Node current, int lastx, int lasty, int hops) {
            if (current.getType() != 0 && current.getType() != 1)
                current.setType(4);
            current.setLastNode(lastx, lasty);
            current.setHops(hops);
            if (current.getType() == 1) {
                backtrack(current.getLastX(), current.getLastY(), hops);
            }
        }

        public void backtrack(int lx, int ly, int hops) {
            ArrayList<String> tile = new ArrayList<>();
            while (hops > 1) {
                Node current = map[lx][ly];
                Call.onConstructFinish(world.tile(lx,ly), Blocks.armoredConveyor,0,(byte)0, Team.sharded,false);
                current.setType(5);
                tile.add(lx+","+ly);
                lx = current.getLastX();
                ly = current.getLastY();
                hops--;
            }
            if(count == 50){
                solving = false;
            } else {
                count++;
            }
        }
    }

    class Node {
        private int cellType = 0;
        private int hops;
        private int x;
        private int y;
        private int lastX;
        private int lastY;
        private double dToEnd = 0;

        public Node(int type, int x, int y) {
            cellType = type;
            this.x = x;
            this.y = y;
            hops = -1;
        }

        public double getEuclidDist() {
            int xdif = Math.abs(x-targetx);
            int ydif = Math.abs(y-targety);
            dToEnd = Math.sqrt((xdif*xdif)+(ydif*ydif));
            return dToEnd;
        }

        public int getX() {return x;}
        public int getY() {return y;}
        public int getLastX() {return lastX;}
        public int getLastY() {return lastY;}
        public int getType() {return cellType;}
        public int getHops() {return hops;}

        public void setType(int type) {cellType = type;}
        public void setLastNode(int x, int y) {lastX = x; lastY = y;}
        public void setHops(int hops) {this.hops = hops;}
    }
}