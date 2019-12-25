package essentials.core;

import essentials.Global;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.world.Tile;

import java.util.ArrayList;

import static io.anuke.mindustry.Vars.world;
import static java.lang.Thread.sleep;

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

    public ArrayList<Tile> as() {
        closed.add(start);
        for (int rot = 0; rot < 4; rot++) {
            opened.add(start.getNearby(rot));
        }
        // 완료되기 전까지 무한반복
        boolean success = false;
        while(!success){
            // 검색 가능한 블록 확인
            for(int a=0;a<opened.size();a++){
                // 목표 블록이 맞는지 확인
                if(opened.get(a) != target){
                    // 현재 블록과 목표간의 거리 확인
                    float rangex = Math.abs(opened.get(a).x - target.x);
                    float rangey = Math.abs(opened.get(a).y - target.y);

                    // 가까운 4면 블록 확인
                    for (int rot = 0; rot < 4; rot++) {
                        boolean match = false;
                        // 검색 가능한 블록이 맞는지 확인
                        for (Tile tile : closed) {
                            if (opened.get(a) == tile) {
                                match = true;
                                break;
                            }
                        }

                        // 검색 가능하고, 벽이 아닌지 확인
                        if(!match && !opened.get(a).getNearby(rot).block().solid) {
                            // 현재 검색블록 위치 표시
                            Call.onConstructFinish(opened.get(a).getNearby(rot), Blocks.conveyor, 0, (byte) rot, Team.sharded, false);

                            // 가까운 4면 블록중 목표 블럭이 맞는지 확인
                            if (opened.get(a).getNearby(rot) != target) {
                                // 현재 검색중인 블록 거리 확인
                                float tmpx = Math.abs(opened.get(a).getNearby(rot).x - target.x);
                                float tmpy = Math.abs(opened.get(a).getNearby(rot).y - target.y);

                                // 목표와의 거리 확인
                                if (tmpx > rangex || tmpy > rangey) {
                                    // 거리가 더 멀어질경우 검색 대상에서 제외
                                    closed.add(opened.get(a).getNearby(rot));

                                    // 거리가 더 멀어졌으니 검색 경로에서 제외
                                    //path.remove(opened.get(a).getNearby(rot));
                                } else if (tmpx < rangex || tmpy < rangey) {
                                    // 거리가 가까워 졌으니 검색 경로에 등록
                                    if (opened.get(a).getNearby(rot).block().solid) {
                                        closed.add(opened.get(a).getNearby(rot));
                                        int rand = (int) (Math.random() * 4);
                                        if (!opened.get(a).getNearby(rot).getNearby(rand).block().solid) {
                                            opened.add(opened.get(a).getNearby(rot).getNearby(rand));
                                            //camefrom.add(opened.get(a).getNearby(b).x + "/" + opened.get(a).getNearby(b).y + "/" + opened.get(a).x + "/" + opened.get(a).y + "/" + b);
                                        }
                                    } else {
                                        // 검색 가능한 블록으로 등록
                                        opened.add(opened.get(a).getNearby(rot));
                                    }
                                    //path.add(opened.get(a).getNearby(rot));

                                    // 현재 검색된 블럭을 메타벽으로 확인
                                    Call.onConstructFinish(opened.get(a).getNearby(rot), Blocks.titaniumConveyor, 0, (byte) rot, Team.sharded, false);
                                } else {
                                    // 망한 것들은 검색 대상에서 제외
                                    closed.add(opened.get(a).getNearby(rot));
                                }
                            } else if(opened.get(a).getNearby(rot) == target){
                                ArrayList<Tile> path = new ArrayList<>();
                                Tile current = opened.get(a);
                                //Tile next;
                                byte rota;
                                for (int c = 0; c < camefrom.size(); c++) {
                                    String[] data = camefrom.get(c).split("/");
                                    int startx = Integer.parseInt(data[0]);
                                    int starty = Integer.parseInt(data[1]);
                                    int targetx = Integer.parseInt(data[2]);
                                    int targety = Integer.parseInt(data[3]);
                                    rota = Byte.parseByte(data[4]);
                                    Tile curr = world.tile(startx, starty);
                                    Tile next = world.tile(targetx, targety);
                                    if (current != start) {
                                        if (current == curr) {
                                            path.add(next);
                                        }
                                    } else {
                                        result = path;
                                    }
                                }

                                // 목표 블럭이 맞을 때 구리 벽으로 확인
                                for (Tile tile : result) {
                                    Call.onConstructFinish(tile, Blocks.phaseConveyor, 0, (byte) rot, Team.sharded, false);
                                }
                                Call.onConstructFinish(opened.get(a).getNearby(rot), Blocks.armoredConveyor, 0, (byte) rot, Team.sharded, false);
                                success = true;
                            }
                        }
                    }
                } else {
                    // 목표 블럭이 맞을때 처리
                    Call.onConstructFinish(target, Blocks.surgeWall, 0, (byte) 0, Team.sharded, false);
                    Global.log("SUCCESS!");
                    success = true;
                }
            }
        }
        return result;
    }

    public ArrayList<Tile> auto() throws InterruptedException {
        for(int a=0;a<rot;a++){
            opened.add(start.getNearby(a));
        }
        boolean success = false;

        while(!success) {
            for(int a=0;a<opened.size();a++){
                for(int b=0;b<rot;b++){
                    Call.onConstructFinish(opened.get(a), Blocks.copperWall, 0, (byte) b, Team.sharded, false);
                    if(opened.get(a).getNearby(b) != target && opened.get(a).getNearby(b) != start) {
                        for(int c=0;c<closed.size();c++){
                            if(closed.get(c) != opened.get(a).getNearby(b)){
                                Call.onConstructFinish(opened.get(a).getNearby(b), Blocks.conveyor, 0, (byte) b, Team.sharded, false);
                                camefrom.add(opened.get(a).getNearby(b).x + "/" + opened.get(a).getNearby(b).y + "/" + opened.get(a).x + "/" + opened.get(a).y + "/" + b);
                                opened.add(opened.get(a).getNearby(b));
                            }
                        }

                        opened.remove(opened.get(a));
                        closed.add(opened.get(a));
                        sleep(70);
                    } else {
                        ArrayList<Tile> path = new ArrayList<>();
                        Tile current = opened.get(a);
                        //Tile next;
                        byte rot;
                        for (int c = 0; c < camefrom.size(); c++) {
                            String[] data = camefrom.get(c).split("/");
                            int startx = Integer.parseInt(data[0]);
                            int starty = Integer.parseInt(data[1]);
                            int targetx = Integer.parseInt(data[2]);
                            int targety = Integer.parseInt(data[3]);
                            rot = Byte.parseByte(data[4]);
                            Tile curr = world.tile(startx, starty);
                            Tile next = world.tile(targetx, targety);
                            if (current != start) {
                                if (current == curr) {
                                    path.add(next);
                                }
                            } else {
                                result = path;
                                success = true;
                            }
                        }
                    }
                }
            }
            sleep(70);
        }

        return result;
    }
}