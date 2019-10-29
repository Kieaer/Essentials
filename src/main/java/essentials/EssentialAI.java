package essentials;

public class EssentialAI {

    /*public Tile start;
    public Tile target;
    public Player player;

    static int packTile(Tile tile){
        return PathTile.get(tile.cost, tile.getTeamID(), (byte)0, !tile.solid() && tile.floor().drownTime <= 0f);
    }*/

    public void main() {
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
    }

    /*
    public Tile start;
    public Tile target;
    public Player player;

    public ArrayList<Tile> closed = new ArrayList<>();
    public ArrayList<Tile> opened = new ArrayList<>();
    public ArrayList<Tile> path = new ArrayList<>();

    public boolean success;

    public Tile getNearby(Tile tile, int rotate){
        if(rotate == 0){
            return Vars.world.tile(tile.x+1, tile.y);
        } else if(rotate == 1){
            return Vars.world.tile(tile.x+1, tile.y+1);
        } else if(rotate == 2){
            return Vars.world.tile(tile.x, tile.y+1);
        } else if(rotate == 3){
            return Vars.world.tile(tile.x-1, tile.y+1);
        } else if(rotate == 4){
            return Vars.world.tile(tile.x-1, tile.y);
        } else if(rotate == 5){
            return Vars.world.tile(tile.x-1, tile.y-1);
        } else if(rotate == 6){
            return Vars.world.tile(tile.x, tile.y-1);
        } else {
            return rotate == 7 ? Vars.world.tile(tile.x+1, tile.y-1) : null;
        }
    }

    public void tracking(){

    }

    public void auto(){
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
    }
    */
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

    public EssentialAI() {
        width = world.width();
        height = world.height();
        size = (width - 1) * (height - 1);
        cardinals = new int[]{-1, 1, -width, width};
        field = new int[size];
    }

    public void main() {
        path(Blocks.conveyor, player);
        if (!success) return;
        Block block = Blocks.titaniumConveyor;
        float x, y;
        int rot;
        final float blockOffset = block.offset();
        for (Vector3 t : path) {
            x = t.x * tilesize + blockOffset;
            y = t.y * tilesize + blockOffset;
            rot = (int) t.z;
            Call.onConstructFinish(world.tileWorld(x, y), block, 0, (byte) rot, Team.sharded, false);
        }
    }

    public void path(Block type, Player player) {
        if (start == target) return;
        path.clear();
        int[] flowField = flow();
        int i = xyToI(target.x, target.y);
        if (!validindex(i)) return;
        int dir = nexttile(flowField, i);
        int ni = dir + i;
        Tile t = target;
        path.add(new Vector3(t.x, t.y, cardinalToRot(dir)));

        int limit = 300;
        while (t != start && limit-- > 0) {
            int[] xy = iToXY(ni);
            int rot = cardinalToRot(dir);
            if (!validindex(ni) || !Build.validPlace(player.getTeam(), xy[0], xy[1], type, rot)) {
                success = false;
                return;
            }
            path.add(new Vector3(xy[0], xy[1], rot));
            dir = nexttile(flowField, ni);
            ni += dir;
            t = world.tile(ni);
        }
        success = t.equals(start);
        if (!success) return;
        path.add(new Vector3(t.x, t.y, cardinalToRot(dir)));
        reverse = new ArrayList<>();
        for (int q = path.size() - 1; q >= 0; q--) reverse.add(path.get(q));
        path = reverse;
    }

    public int nexttile(int[] flow, int i) {
        int highestDir = -1;
        int highestVal = -1;
        for (int cardinal : cardinals) {
            Integer neighbor = flow[i + cardinal];
            if (neighbor > highestVal) {
                highestVal = neighbor;
                highestDir = cardinal;
            }
        }
        return highestDir;
    }

    private int[] iToXY(int i) {
        return new int[]{i % width, i / width};
    }

    private byte cardinalToRot(int dir) {
        if (dir == -1) return (byte) 0;
        if (dir == 1) return (byte) 2;
        if (dir < 0) return (byte) 1;
        return (byte) 3;
    }

    public boolean validindex(int i) {
        return ((i > width) && (i < size - width) && (i % width > 0));
    }

    public boolean validtile(Tile tile, int i) {
        return validindex(i) && (!tile.solid()) && (tile.block() == Blocks.air);
    }

    private Tile getTile(int i) {
        return (validindex(i) ? world.tile(i) : world.tile(0));
    }

    public int[] flow() {
        for (int i = 0; i < size; i++) field[i] = 0;
        IntArray current = new IntArray(), next = new IntArray();
        current.add(xyToI(start.x, start.y));
        while (current.size > 0) {
            for (int index : current.items) {
                for (int cardinal : cardinals) {
                    int neg = index + cardinal;
                    Call.onConstructFinish(world.tile(neg), Blocks.phaseWall, 0, (byte) 0, Team.sharded, false);
                    if (!validindex(neg)) continue;
                    Tile t = getTile(neg);
                    if (!validtile(t, neg)) continue;
                    int p = field[index] - 1;
                    if (field[neg] < p) {
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

    public int xyToI(int x, int y) {
        return x + y * world.width();
        //return x * y;
    }
     */
}