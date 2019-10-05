package essentials;

import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.util.Log;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.game.EventType;
import io.anuke.mindustry.game.Team;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.anuke.mindustry.Vars.world;

public class EssentialAI {

    float[] copperflow;
    float[] leadflow;
    float[] titaniumflow;
    float[] coalflow;
    float[] thoriumflow;
    float[] coreflow;
/*
    public boolean isPassable(int i) {
        int type = tilesInt(i);
        if ((type == AIR) || (type == JUNCTION)) return true;
        return false;
    }

    int tilesInt(int i){
        if ((i<0)||(i>sizeSq-1)) return 0;
        int []xy = iToXY(i);
        return (int)tiles[xy[0]][xy[1]];
    }
*/
    public static void main(){

//        if (!Core.settings.getDataDirectory().child("test.log").exists()) {
//            Core.settings.getDataDirectory().child("test.log").writeString("");
//            Global.log("test.log created.");
//        }
        // Thread t = new Thread(() -> {});
        String[] blockNames = {"air","spawn","deepwater","water","taintedWater","tar","stone","craters","charr","sand","darksand","ice","snow","darksandTaintedWater","holostone","rocks","sporerocks","icerocks","cliffs","sporePine","snowPine","pine","shrubs","whiteTree","whiteTreeDead","sporeCluster","iceSnow","sandWater","darksandWater","duneRocks","sandRocks","moss","sporeMoss","shale","shaleRocks","shaleBoulder","sandBoulder","grass","salt","metalFloor","metalFloorDamaged","metalFloor2","metalFloor3","metalFloor5","ignarock","magmarock","hotrock","snowrocks","rock","snowrock","saltRocks","darkPanel1","darkPanel2","darkPanel3","darkPanel4","darkPanel5","darkPanel6","darkMetal","pebbles","tendrils","oreCopper","oreLead","oreScrap","oreCoal","oreTitanium","oreThorium","siliconSmelter","kiln","graphitePress","plastaniumCompressor","multiPress","phaseWeaver","surgeSmelter","pyratiteMixer","blastMixer","cryofluidMixer","melter","separator","sporePress","pulverizer","incinerator","coalCentrifuge","powerVoid","powerSource","itemSource","liquidSource","itemVoid","message","scrapWall","scrapWallLarge","scrapWallHuge","scrapWallGigantic","thruster","copperWall","copperWallLarge","titaniumWall","titaniumWallLarge","thoriumWall","thoriumWallLarge","door","doorLarge","phaseWall","phaseWallLarge","surgeWall","surgeWallLarge","mender","mendProjector","overdriveProjector","forceProjector","shockMine","conveyor","titaniumConveyor","armoredConveyor","distributor","junction","itemBridge","phaseConveyor","sorter","router","overflowGate","massDriver","mechanicalPump","rotaryPump","thermalPump","conduit","pulseConduit","liquidRouter","liquidTank","liquidJunction","bridgeConduit","phaseConduit","combustionGenerator","thermalGenerator","turbineGenerator","differentialGenerator","rtgGenerator","solarPanel","largeSolarPanel","thoriumReactor","impactReactor","battery","batteryLarge","powerNode","powerNodeLarge","surgeTower","mechanicalDrill","pneumaticDrill","laserDrill","blastDrill","waterExtractor","oilExtractor","cultivator","coreShard","coreFoundation","coreNucleus","vault","container","unloader","launchPad","launchPadLarge","duo","scatter","scorch","hail","arc","wave","lancer","swarmer","salvo","fuse","ripple","cyclone","spectre","meltdown","commandCenter","draugFactory","spiritFactory","phantomFactory","wraithFactory","ghoulFactory","revenantFactory","daggerFactory","crawlerFactory","titanFactory","fortressFactory","repairPoint","dartPad","deltaPad","tauPad","omegaPad","javelinPad","tridentPad","glaivePad"};
        String[] ores = {"ore-copper","ore-lead","ore-titanium","ore-coal","ore-thorium"};



        List<String> copper = new ArrayList<>();
        Path test = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("test.log")));
        // Multi thread
        //ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        // Single thread
        ExecutorService pool = Executors.newFixedThreadPool(1);
        Events.on(EventType.WorldLoadEvent.class, () -> {
            Global.log("Map scan start!");
            Thread t1 = new Thread(() -> {
                for (int x = 0; x < world.width() * 8; x += 8) {
                    for (int y = 0; y < world.height() * 8; y += 8) {
                        int finalX = x;
                        int finalY = y;
                        Runnable t = () -> {
                            if (world.tileWorld(finalX, finalY).overlayID() != 0) {
                                if (world.tileWorld(finalX, finalY).blockID() == 0) {
                                    if (world.tileWorld(finalX, finalY).overlayID() == 149) {
                                        world.tileWorld(finalX, finalY).setBlock(Blocks.mechanicalDrill, Team.sharded, 0);
                                    }
                                }
                            }
                            if (world.tileWorld(finalX, finalY).overlayID() != 0) {
                                if (world.tileWorld(finalX, finalY).blockID() == 0) {
                                    if (world.tileWorld(finalX, finalY).overlayID() == 150) {
                                        world.tileWorld(finalX, finalY).setBlock(Blocks.mechanicalDrill, Team.sharded, 0);
                                    }
                                }
                            }
                            if (world.tileWorld(finalX, finalY).overlayID() != 0) {
                                if (world.tileWorld(finalX, finalY).blockID() == 0) {
                                    if (world.tileWorld(finalX, finalY).overlayID() == 154) {
                                        world.tileWorld(finalX, finalY).setBlock(Blocks.laserDrill, Team.sharded, 0);
                                    }
                                }
                            }
                            if (world.tileWorld(finalX, finalY).overlayID() != 0) {
                                if (world.tileWorld(finalX, finalY).blockID() == 0) {
                                    if (world.tileWorld(finalX, finalY).overlayID() == 153) {
                                        world.tileWorld(finalX, finalY).setBlock(Blocks.pneumaticDrill, Team.sharded, 0);
                                    }
                                }
                            }
                        };
                        pool.execute(t);
                    }
                }
                Log.info(copper);
//                for(int i=0;i<copper.size();i++){
//                    String x = copper.get(i);
//                    String[] t = x.split(".*/.*");
//                    int xl = Integer.parseInt(t[0]);
//                    int yl = Integer.parseInt(t[1]);
//                    world.tileWorld(xl, yl).setBlock(Blocks.copperWall, Team.sharded, 0);
//                    Log.info(xl+"/"+yl);
//                }

            });
            t1.start();
        });
    }
}
