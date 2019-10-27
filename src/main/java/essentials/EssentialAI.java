package essentials;

import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.world.Tile;

import java.util.ArrayList;

public class EssentialAI {
    public Tile start;
    public Tile target;
    public Player player;

    public ArrayList<Tile> closed = new ArrayList<>();
    public ArrayList<Tile> opened = new ArrayList<>();

    public boolean success;

    public void main(){
        closed.add(start);
        for (int rot = 0; rot < 4; rot++) {
            opened.add(start.getNearby(rot));
        }
        while(!success){
            for(int a=0;a<opened.size();a++){
                if(opened.get(a) != target){
                    int rangex = opened.get(a).x - target.x;
                    int rangey = opened.get(a).y - target.y;
                    for (int rot = 0; rot < 4; rot++) {
                        if(opened.get(a).getNearby(rot) != target){
                            opened.add(opened.get(a).getNearby(rot));

                            int tmpx;
                            int tmpy;
                            if(!closed.contains(opened.get(a))){
                                tmpx = opened.get(a).getNearby(rot).x - target.x;
                                tmpy = opened.get(a).getNearby(rot).y - target.y;
                                if(tmpx > rangex || tmpy > rangey){
                                    closed.add(opened.get(a).getNearby(rot));
                                } else {
                                    Call.onConstructFinish(opened.get(a).getNearby(rot), Blocks.conveyor, 0, (byte) rot, Team.sharded, false);
                                }
                                try {
                                    Thread.sleep(10);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                } else {
                    Global.log("SUCCESS!");
                    success = true;
                    return;
                }
            }
        }
    }
}

/*
public class EssentialAI {
    public final int width, height;
    public Tile start;
    public Tile target;
    public boolean success;

    public ArrayList<Integer> tile = new ArrayList<>();
    public ArrayList<Tile> path1 = new ArrayList<>();
    public ArrayList<Tile> path2 = new ArrayList<>();
    public ArrayList<Tile> current = new ArrayList<>();
    public ArrayList<Tile> scan = new ArrayList<>();

    public EssentialAI(){
        width = world.width();
        height = world.height();
    }


    public void main() {
        if (start == target) return;

        int[] arr = {1,4,5,6,7,-1};

        int closestIndex = 0;
        int diff = Integer.MAX_VALUE;
        for (int i = 0; i < arr.length; ++i) {
            int abs = Math.abs(arr[i]);
            if (abs < diff) {
                closestIndex = i;
                diff = abs;
            } else if (abs == diff && arr[i] > 0 && arr[closestIndex] < 0) {
                //same distance to zero but positive
                closestIndex =i;
            }
        }
        System.out.println(arr[closestIndex]);

        Bresenham2 b = new Bresenham2();
        Array<Point2> as = b.line(start.x,start.y,target.x , target.y);
        for(int i=0;i<as.size;i++){
            for (int list = 0; list < current.size(); list++) {
                for (int rot = 0; rot < 4; rot++) {
                    if(world.tile(as.get(i).x, as.get(i).y).solid()){

                    }
                    Call.onConstructFinish(world.tile(as.get(i).x, as.get(i).y), Blocks.conveyor, 0, (byte) rot, Team.sharded, false);
                }
            }
            Global.log(String.valueOf(as.get(i)));
            Call.onConstructFinish(world.tile(as.get(i).x, as.get(i).y), Blocks.phaseWall, 0, (byte) 0, Team.sharded, false);
        }
        path1.clear();
        path2.clear();

        // Add start block
        current.add(start);
        scan.add(start);
        // Start search
        int tempx = start.x;
        int tempy = start.y;
        while (!success) {
            for (int list = 0; list < current.size(); list++) {
                // check nearby block is target block
                if (scan.get(list) != target) {
                    for (int rot = 0; rot < 4; rot++) {
                        if (current.get(list).getNearby(rot).block() == Blocks.air) {
                            int currentx = current.get(list).getNearby(rot).x;
                            int currenty = current.get(list).getNearby(rot).y;
                            if(start.x < target.x) {
                                if (currentx > tempx) {
                                    //current.add(current.get(list).getNearby(rot));
                                    tempx++;
                                    Call.onConstructFinish(current.get(list).getNearby(rot), Blocks.phaseWall, 0, (byte) rot, Team.sharded, false);
                                    current.add(current.get(list).getNearby(rot));
                                    path1.add(current.get(list).getNearby(rot));
                                }
                                if (currenty > tempy) {
                                    tempy++;
                                    Call.onConstructFinish(current.get(list).getNearby(rot), Blocks.phaseWall, 0, (byte) rot, Team.sharded, false);
                                    current.add(current.get(list).getNearby(rot));
                                    path1.add(current.get(list).getNearby(rot));
                                }
                            } else if(start.x > target.x){
                                if (currentx < tempx) {
                                    //current.add(current.get(list).getNearby(rot));
                                    tempx--;
                                    Call.onConstructFinish(current.get(list).getNearby(rot), Blocks.phaseWall, 0, (byte) rot, Team.sharded, false);
                                    current.add(current.get(list).getNearby(rot));
                                    path1.add(current.get(list).getNearby(rot));
                                }
                                if (currenty < tempy) {
                                    tempy--;
                                    Call.onConstructFinish(current.get(list).getNearby(rot), Blocks.phaseWall, 0, (byte) rot, Team.sharded, false);
                                    current.add(current.get(list).getNearby(rot));
                                    path1.add(current.get(list).getNearby(rot));
                                }
                            }
                            /*if(start.y < target.y) {
                                if (currenty > tempy) {
                                    tempy++;
                                    Call.onConstructFinish(current.get(list).getNearby(rot), Blocks.phaseWall, 0, (byte) rot, Team.sharded, false);
                                    current.add(current.get(list).getNearby(rot));
                                    path1.add(current.get(list).getNearby(rot));
                                }
                            }
                            if(start.x > target.x) {
                                if (currentx < tempx) {
                                    tempx--;
                                    Call.onConstructFinish(current.get(list).getNearby(rot), Blocks.phaseWall, 0, (byte) rot, Team.sharded, false);
                                    current.add(current.get(list).getNearby(rot));
                                    path1.add(current.get(list).getNearby(rot));
                                }
                            }
                            if(start.y > target.y) {
                                if (currenty < tempy) {
                                    tempy--;
                                    Call.onConstructFinish(current.get(list).getNearby(rot), Blocks.phaseWall, 0, (byte) rot, Team.sharded, false);
                                    current.add(current.get(list).getNearby(rot));
                                    path1.add(current.get(list).getNearby(rot));
                                }
                            }*/
                                /*if (currentx > tempx) {
                                    if (scan != current) {
                                        scan.add(current.get(list).getNearby(rot));
                                    }
                                    tempx = currentx;
                                    path1.add(scan.get(list));
                                } else {
                                    Call.onConstructFinish(current.get(list).getNearby(rot), Blocks.titaniumWall, 0, (byte) rot, Team.sharded, false);
                                }
                                if (currenty > tempy) {
                                    if (scan != current) {
                                        //Call.onConstructFinish(current.get(list).getNearby(rot), Blocks.conveyor, 0, (byte) rot, Team.sharded, false);
                                        scan.add(current.get(list).getNearby(rot));
                                    }
                                    tempy = currenty;
                                    path1.add(scan.get(list));
                                } else {
                                    Call.onConstructFinish(current.get(list).getNearby(rot), Blocks.titaniumWall, 0, (byte) rot, Team.sharded, false);
                                }*/
                           // } else if(start.x > target.x){
                            //    Call.onConstructFinish(current.get(list).getNearby(rot), Blocks.phaseWall, 0, (byte) rot, Team.sharded, false);
                                /*if (currentx < tempx) {
                                    if (scan != current) {
                                        scan.add(current.get(list).getNearby(rot));
                                    }
                                    tempx = currentx;
                                    path1.add(scan.get(list));
                                } else {
                                    Call.onConstructFinish(current.get(list).getNearby(rot), Blocks.titaniumWall, 0, (byte) rot, Team.sharded, false);
                                }
                                if (currenty < tempy) {
                                    if (scan != current) {
                                        //Call.onConstructFinish(current.get(list).getNearby(rot), Blocks.conveyor, 0, (byte) rot, Team.sharded, false);
                                        scan.add(current.get(list).getNearby(rot));
                                    }
                                    tempy = currenty;
                                    path1.add(scan.get(list));
                                } else {
                                    Call.onConstructFinish(current.get(list).getNearby(rot), Blocks.titaniumWall, 0, (byte) rot, Team.sharded, false);
                                }*/
                            //}
                            /*
                        }
                    }
                    scan.add(current.get(list));
                    // check if match
                    for (int rot = 0; rot < 4; rot++) {
                        if (scan.get(list).getNearby(rot) == target) {
                            success = true;
                            path2.add(scan.get(list).getNearby(rot));
                            for(int c=0;c<path1.size();c++){
                                Call.onConstructFinish(path1.get(c), Blocks.copperWall, 0, (byte) rot, Team.sharded, false);
                            }
                            Global.log("SUCCESS!");
                            return;
                        }
                    }
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    /*
    public final int width, height;
    private final int[] cardinals;
    public final int size;

    public Tile start;
    public Tile target;
    public int[] field;
    public boolean success;
    public Player player;

    public ArrayList<Vector3> path = new ArrayList<>();
    public ArrayList<Vector3> reverse = new ArrayList<>();

    public EssentialAI(){
        width = world.width();
        height = world.height();
        size = (width-1) * (height-1);
        cardinals = new int[]{-1, 1, -width, width};
        field = new int[size];
    }

    public void main(){
        path(Blocks.conveyor, player);
        if(!success) return;
        Block block = Blocks.titaniumConveyor;
        float x, y;
        int rot;
        final float blockOffset = block.offset();
        for(Vector3 t : path){
            x = t.x * tilesize + blockOffset;
            y = t.y * tilesize + blockOffset;
            rot = (int) t.z;
            Call.onConstructFinish(world.tileWorld(x, y), block, 0, (byte) rot, Team.sharded, false);
        }
    }

    public void path(Block type, Player player){
        if(start == target) return;
        path.clear();
        int[] flowField = flow();
        int i = xyToI(target.x, target.y);
        if(!validindex(i)) return;
        int dir = nexttile(flowField, i);
        int ni = dir + i;
        Tile t = target;
        path.add(new Vector3(t.x, t.y, cardinalToRot(dir)));

        int limit = 300;
        while(t != start && limit-- > 0){
            int[] xy = iToXY(ni);
            int rot = cardinalToRot(dir);
            if(!validindex(ni) || !Build.validPlace(player.getTeam(), xy[0], xy[1], type, rot)){
                success = false;
                return;
            }
            path.add(new Vector3(xy[0], xy[1], rot));
            dir = nexttile(flowField, ni);
            ni += dir;
            t = world.tile(ni);
        }
        success = t.equals(start);
        if(!success) return;
        path.add(new Vector3(t.x, t.y, cardinalToRot(dir)));
        reverse = new ArrayList<>();
        for(int q = path.size()-1;q>=0;q--) reverse.add(path.get(q));
        path = reverse;
    }

    public int nexttile(int[] flow, int i){
        int highestDir = -1;
        int highestVal = -1;
        for(int cardinal : cardinals){
            Integer neighbor = flow[i + cardinal];
            if(neighbor > highestVal){
                highestVal = neighbor;
                highestDir = cardinal;
            }
        }
        return highestDir;
    }

    private int[] iToXY(int i){
        return new int[]{i % width, i / width};
    }

    private byte cardinalToRot(int dir){
        if(dir == -1) return (byte) 0;
        if(dir == 1) return (byte) 2;
        if(dir < 0) return (byte) 1;
        return (byte) 3;
    }

    public boolean validindex(int i){
        return ((i > width) && (i < size - width) && (i % width > 0));
    }

    public boolean validtile(Tile tile, int i){
        return validindex(i) && (!tile.solid()) && (tile.block() == Blocks.air);
    }

    private Tile getTile(int i){
        for(int xi=0;xi<world.width();xi++){
            for(int x=0;x<world.height();x++){
                Call.onConstructFinish(world.tile(xi), Blocks.phaseWall, 0, (byte) 0, Team.sharded, false);
                if(validindex(i)){
                    int r = xi+x;
                    Global.log(world.tile(r).block().name+"/"+r);
                   // return world.tile(xi)
                }
            }
        }
        return (validindex(i) ? world.tile(i) : world.tile(0));
    }

    public int[] flow(){
        for(int i = 0; i < size; i++) field[i] = 0;
        IntArray current = new IntArray(), next = new IntArray();
        current.add(xyToI(start.x, start.y));
        while(current.size > 0){
            for(int index : current.items){
                for(int cardinal : cardinals){
                    int neg = index + cardinal;
                    Call.onConstructFinish(world.tile(neg), Blocks.phaseWall, 0, (byte) 0, Team.sharded, false);
                    if(!validindex(neg)) continue;
                    Tile t = getTile(neg);
                    if(!validtile(t, neg)) continue;
                    int p = field[index] - 1;
                    if(field[neg] < p){
                        next.add(neg);
                        field[neg] = p;
                    }
                }
            }
            IntArray swap = current;
            current = next;
            swap.clear();
            next = swap;
        }
        return field;
    }

    public int xyToI(int x, int y){
        return x + y * world.width();
        //return x * y;
    }
}

                             */
