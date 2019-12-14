package essentials.core;

import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.world.Tile;

import static io.anuke.mindustry.Vars.world;

public class AI {/*
    // Source from io.anuke.mindustry.input.Placement
    static Array<Point2> points = new Array<>();

    static IntFloatMap costs = new IntFloatMap();
    static IntIntMap parents = new IntIntMap();
    static IntSet closed = new IntSet();

    public static boolean astar(int startx, int starty, int targetx, int targety){
        Tile start = world.tile(startx, starty);
        Tile target = world.tile(targetx, targety);

        costs.clear();
        closed.clear();
        parents.clear();

        int limit = 100;
        int totalnodes = 0;

        PriorityQueue<Tile> queue = new PriorityQueue<>(10, (a, b) -> Float.compare(costs.get(a.pos(), 0f) + distanceHeuristic(a.x, a.y, target.x, target.y), costs.get(b.pos(), 0f) + distanceHeuristic(b.x, b.y, target.x, target.y)));
        queue.add(start);
        boolean found = false;
        while(!queue.isEmpty() && totalnodes++ < limit){
            Tile next = queue.poll();
            float baseCost = costs.get(next.pos(), 0f);
            if(next == target){
                found = true;
                break;
            }
            closed.add(Pos.get(next.x, next.y));
            for(Point2 point : Geometry.d4){
                int newx = next.x + point.x, newy = next.y + point.y;
                Tile child = world.tile(newx, newy);
                if(child != null && validNode(next, child)){
                    if(closed.add(child.pos())){
                        parents.put(child.pos(), next.pos());
                        costs.put(child.pos(), tileHeuristic(next, child) + baseCost);
                        queue.add(child);
                    }
                }
            }
        }

        if(!found) return false;
        int total = 0;

        points.add(Pools.obtain(Point2.class, Point2::new).set(targetx, targety));

        Tile current = target;
        while(current != start && total++ < limit){
            if(current == null) return false;
            int newPos = parents.get(current.pos(), Pos.invalid);

            if(newPos == Pos.invalid) return false;

            points.add(Pools.obtain(Point2.class, Point2::new).set(Pos.x(newPos),  Pos.y(newPos)));
            current = world.tile(newPos);
        }

        points.reverse();

        return true;
    }

    static float distanceHeuristic(int x1, int y1, int x2, int y2){
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    static boolean validNode(Tile tile, Tile other){
        Block block = control.input.block;
        if(block != null && block.canReplace(other.block())){
            return true;
        }else{
            return other.block().alwaysReplace;
        }
    }

    static float tileHeuristic(Tile tile, Tile other){
        Block block = control.input.block;

        if((!other.block().alwaysReplace && !(block != null && block.canReplace(other.block()))) || other.floor().isDeep()){
            return 20;
        }else{
            if(parents.containsKey(tile.pos())){
                Tile prev = world.tile(parents.get(tile.pos(), 0));
                if(tile.relativeTo(prev) != other.relativeTo(tile)){
                    return 8;
                }
            }
        }
        return 1;
    }
*/
    /*public Tile start;
    public Tile target;
    public Player player;

    static int packTile(Tile tile){
        return PathTile.get(tile.cost, tile.getTeamID(), (byte)0, !tile.solid() && tile.floor().drownTime <= 0f);
    }*/

    //public void main() {
        /*for(int x=0;x<world.width();x++){
            for(int y=0;y<world.height();y++){
                if(world.tile(x,y).solid()){
                    MAP[x][y] = packTile(world.rawTile(x, y));
                }
            }
        }*/
        /*GridCell[][] cells = new GridCell[world.width()][world.height()];
        NavigationGrid<GridCell> navGrid = new NavigationGrid(cells);
        AStarGridFinder<GridCell> finder = new AStarGridFinder(GridCell.class);
        Global.log(start.x+"/"+start.y+"/"+target.x+"/"+target.y);
        List<GridCell> pathToEnd = finder.findPath(start.x, start.y, target.x, target.y, navGrid);
        for(int i=0;i<pathToEnd.size();i++){
            Call.onConstructFinish(world.tileWorld(pathToEnd.get(i).x, pathToEnd.get(i).y), Blocks.conveyor, 0, (byte) 0, Team.sharded, false);
        }*/
    //}


    public Tile start;
    public Tile target;
    public Player player;

    public boolean success;

    public Tile getNearby(Tile tile, int rotate){
        if(rotate == 0){
            return world.tile(tile.x+1, tile.y);
        } else if(rotate == 1){
            return world.tile(tile.x+1, tile.y+1);
        } else if(rotate == 2){
            return world.tile(tile.x, tile.y+1);
        } else if(rotate == 3){
            return world.tile(tile.x-1, tile.y+1);
        } else if(rotate == 4){
            return world.tile(tile.x-1, tile.y);
        } else if(rotate == 5){
            return world.tile(tile.x-1, tile.y-1);
        } else if(rotate == 6){
            return world.tile(tile.x, tile.y-1);
        } else {
            return rotate == 7 ? world.tile(tile.x+1, tile.y-1) : null;
        }
    }

    public void auto(){
        /*
        closed.add(start);
        for (int rot = 0; rot < 4; rot++) {
            opened.add(start.getNearby(rot));
        }
        // 완료되기 전까지 무한반복
        while(!success){
            // 검색 가능한 블록 확인
            for(int a=0;a<opened.size();a++){
                // 목표 블록이 맞는지 확인
                if(opened.get(a) != target){
                    // 현재 블록과 목표간의 거리 확인
                    float rangex = Math.abs(opened.get(a).x - target.x);
                    float rangey = Math.abs(opened.get(a).y - target.y);

                    // 가까운 4면 블록 확인
                    for (int rot = 0; rot < 8; rot++) {
                        boolean match = false;
                        // 검색 가능한 블록이 맞는지 확인
                        for (Tile tile : closed) {
                            if (opened.get(a) == tile) {
                                match = true;
                                break;
                            }
                        }

                        // 검색 가능하고, 벽이 아닌지 확인
                        if(!match && !getNearby(opened.get(a), rot).block().solid) {
                            // 현재 검색블록 위치 표시
                            Call.onConstructFinish(getNearby(opened.get(a), rot), Blocks.conveyor, 0, (byte) rot, Team.sharded, false);

                            // 가까운 4면 블록중 목표 블럭이 맞는지 확인
                            if (getNearby(opened.get(a), rot) != target) {
                                // 현재 검색중인 블록 거리 확인
                                float tmpx = Math.abs(getNearby(opened.get(a), rot).x - target.x);
                                float tmpy = Math.abs(getNearby(opened.get(a), rot).y - target.y);

                                // 목표와의 거리 확인
                                if (tmpx > rangex || tmpy > rangey) {
                                    // 거리가 더 멀어질경우 검색 대상에서 제외
                                    closed.add(getNearby(opened.get(a), rot));

                                    // 거리가 더 멀어졌으니 검색 경로에서 제외
                                    path.remove(getNearby(opened.get(a), rot));
                                } else if (tmpx < rangex || tmpy < rangey) {
                                    // 거리가 가까워 졌으니 검색 경로에 등록
                                    if (getNearby(opened.get(a), rot).block().solid) {
                                        closed.add(getNearby(opened.get(a), rot));
                                        int rand = (int) (Math.random() * 4);
                                        if (!getNearby(opened.get(a), rot).getNearby(rand).block().solid) {
                                            opened.add(getNearby(opened.get(a), rot).getNearby(rand));
                                            path.add(getNearby(opened.get(a), rot).getNearby(rand));
                                        }
                                    } else {
                                        // 검색 가능한 블록으로 등록
                                        opened.add(getNearby(opened.get(a), rot));
                                    }
                                    path.add(getNearby(opened.get(a), rot));

                                    // 현재 검색된 블럭을 메타벽으로 확인
                                    Call.onConstructFinish(getNearby(opened.get(a), rot), Blocks.titaniumConveyor, 0, (byte) rot, Team.sharded, false);
                                } else {
                                    // 망한 것들은 검색 대상에서 제외
                                    closed.add(getNearby(opened.get(a), rot));
                                }

                                try {
                                    // 디버그를 위한 작업 지연
                                    Thread.sleep(1);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } else if(getNearby(opened.get(a), rot) == target){
                                // 목표 블럭이 맞을 때 구리 벽으로 확인
                                for (Tile tile : path) {
                                    Call.onConstructFinish(tile, Blocks.phaseConveyor, 0, (byte) rot, Team.sharded, false);
                                }
                                Call.onConstructFinish(getNearby(opened.get(a), rot), Blocks.armoredConveyor, 0, (byte) rot, Team.sharded, false);
                                success = true;
                                return;
                            }
                        }
                    }
                } else {
                    // 목표 블럭이 맞을때 처리
                    Call.onConstructFinish(target, Blocks.surgeWall, 0, (byte) 0, Team.sharded, false);
                    Global.log("SUCCESS!");
                    success = true;
                    return;
                }
            }
        }
         */
    }

    /* Source copied from io.anuke.mindustry.input.Placement start */
    //for pathfinding
    /*
    private static IntFloatMap costs = new IntFloatMap();
    private static IntIntMap parents = new IntIntMap();
    private static IntSet closed = new IntSet();
    private static Array<Point2> points = new Array<>();
    private static Bresenham2 bres = new Bresenham2();
    Block block = Blocks.conveyor;
    int rotation;
    boolean overrideLineRotation = false;
    protected PlaceLine line = new PlaceLine();

    public void main(){
        Array<Point2> points = Placement.pathfindLine(true, start.x, start.y, target.x, target.y);

        float angle = Angles.angle(start.x, start.y, target.x, target.y);
        int baseRotation = rotation;
        if(!overrideLineRotation){
            baseRotation = (start.x == target.x && start.y == target.y) ? rotation : ((int)((angle + 45) / 90f)) % 4;
        }

        Tmp.r3.set(-1, -1, 0, 0);

        for(int i = 0; i < points.size; i++){
            Point2 point = points.get(i);

            if(block != null && Tmp.r2.setSize(block.size * tilesize).setCenter(point.x * tilesize + block.offset(), point.y * tilesize + block.offset()).overlaps(Tmp.r3)){
                continue;
            }

            Point2 next = i == points.size - 1 ? null : points.get(i + 1);
            line.x = point.x;
            line.y = point.y;
            if(!overrideLineRotation){
                line.rotation = next != null ? Tile.relativeTo(point.x, point.y, next.x, next.y) : baseRotation;
            }else{
                line.rotation = rotation;
            }
            line.last = next == null;
            //cons.get(line);

            Global.log(line.x+"/"+line.y+"/"+line.rotation);
            Tmp.r3.setSize(block.size * tilesize).setCenter(point.x * tilesize + block.offset(), point.y * tilesize + block.offset());
        }
    }

    private static boolean astar(int startX, int startY, int endX, int endY){
        Tile start = world.tile(startX, startY);
        Tile end = world.tile(endX, endY);
        if(start == end || start == null || end == null) return false;

        costs.clear();
        closed.clear();
        parents.clear();

        int nodeLimit = 1000;
        int totalNodes = 0;

        PriorityQueue<Tile> queue = new PriorityQueue<>(10, (a, b) -> Float.compare(costs.get(a.pos(), 0f) + distanceHeuristic(a.x, a.y, end.x, end.y), costs.get(b.pos(), 0f) + distanceHeuristic(b.x, b.y, end.x, end.y)));
        queue.add(start);
        boolean found = false;
        while(!queue.isEmpty() && totalNodes++ < nodeLimit){
            Tile next = queue.poll();
            float baseCost = costs.get(next.pos(), 0f);
            if(next == end){
                found = true;
                break;
            }
            closed.add(Pos.get(next.x, next.y));
            for(Point2 point : Geometry.d4){
                int newx = next.x + point.x, newy = next.y + point.y;
                Tile child = world.tile(newx, newy);
                if(child != null && validNode(next, child)){
                    if(closed.add(child.pos())){
                        parents.put(child.pos(), next.pos());
                        costs.put(child.pos(), tileHeuristic(next, child) + baseCost);
                        queue.add(child);
                    }
                }
            }
        }

        if(!found) return false;
        int total = 0;

        points.add(Pools.obtain(Point2.class, Point2::new).set(endX, endY));

        Tile current = end;
        while(current != start && total++ < nodeLimit){
            if(current == null) return false;
            int newPos = parents.get(current.pos(), Pos.invalid);

            if(newPos == Pos.invalid) return false;

            points.add(Pools.obtain(Point2.class, Point2::new).set(Pos.x(newPos),  Pos.y(newPos)));
            current = world.tile(newPos);
        }

        points.reverse();

        return true;
    }

    private static float distanceHeuristic(int x1, int y1, int x2, int y2){
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    private static boolean validNode(Tile tile, Tile other){
        Block block = Blocks.conveyor;
        if(block != null && block.canReplace(other.block())){
            return true;
        }else{
            return other.block().alwaysReplace;
        }
    }

    private static float tileHeuristic(Tile tile, Tile other){
        Block block = Blocks.conveyor;

        if(!other.block().alwaysReplace && !(block != null && block.canReplace(other.block()))){
            return 20;
        }else{
            if(parents.containsKey(tile.pos())){
                Tile prev = world.tile(parents.get(tile.pos(), 0));
                if(tile.relativeTo(prev) != other.relativeTo(tile)){
                    return 8;
                }
            }
        }
        return 1;
    }

    public static Array<Point2> pathfindLine(boolean conveyors, int startX, int startY, int endX, int endY){
        Pools.freeAll(points);

        points.clear();
        if(astar(startX, startY, endX, endY)){
            return points;
        }else{
            return bres.lineNoDiagonal(startX, startY, endX, endY, Pools.get(Point2.class, Point2::new), points);
        }
    }

    class PlaceLine{
        public int x, y, rotation;
        public boolean last;
    }
*/
    /* source copied from io.anuke.mindustry.input.Placement end*/
}