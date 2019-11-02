package essentials;

import essentials.special.Vote;
import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.util.Log;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.game.EventType.BlockBuildEndEvent;
import io.anuke.mindustry.game.EventType.BuildSelectEvent;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

import static essentials.EssentialConfig.*;
import static essentials.EssentialPlayer.getData;
import static essentials.EssentialPlayer.writeData;
import static essentials.Global.printStackTrace;
import static essentials.Global.setcount;
import static essentials.special.Vote.isvoting;
import static io.anuke.mindustry.Vars.*;

public class EssentialTimer extends TimerTask implements Runnable{
    public static String playtime;
    public static String uptime;

    @Override
    public void run() {
        // Player playtime counting
        Thread playtime = new playtime();
        playtime.start();

        // Temporarily ban players time counting
        Thread bantime = new bantime();
        bantime.start();

        // Map playtime counting
        Thread maptime = new maptime();
        maptime.start();

        // Server uptime counting
        Thread uptime = new uptime();
        uptime.start();

        // Vote monitoring
        Thread checkvote = new checkvote();
        checkvote.start();

        // If world loaded
        if(state.is(GameState.State.playing)) {
            // server to server monitoring
            Thread jumpzone = new jumpzone();
            jumpzone.start();

            // client players counting
            Thread jumpcheck = new jumpcheck();
            jumpcheck.start();

            // all client players counting
            Thread jumpall = new jumpall();
            jumpall.start();
        }
    }

    static class playtime extends Thread{
        @Override
        public void run(){
            try{
                if(playerGroup.size() > 0){
                    for(int i = 0; i < playerGroup.size(); i++){
                        Player player = playerGroup.all().get(i);
                        if(!Vars.state.teams.get(player.getTeam()).cores.isEmpty()){
                            JSONObject db = new JSONObject();
                            try {
                                db = getData(player.uuid);
                            }catch (Exception e){
                                printStackTrace(e);
                            }
                            String data;
                            if(db.has("playtime")){
                                data = db.getString("playtime");
                            } else {
                                return;
                            }
                            SimpleDateFormat format = new SimpleDateFormat("HH:mm.ss");
                            Date d1;
                            Calendar cal;
                            String newTime = null;
                            try {
                                d1 = format.parse(data);
                                cal = Calendar.getInstance();
                                cal.setTime(d1);
                                cal.add(Calendar.SECOND, 1);
                                newTime = format.format(cal.getTime());
                            } catch (ParseException e1) {
                                printStackTrace(e1);
                            }

                            // Exp caculating
                            int exp = db.getInt("exp");
                            int newexp = exp+(int)(Math.random()*5);

                            writeData("UPDATE players SET exp = '"+newexp+"', playtime = '"+newTime+"' WHERE uuid = '"+player.uuid+"'");

                            EssentialExp.exp(player.name, player.uuid);
                        }
                    }
                }
            }catch (Exception ex){
                printStackTrace(ex);
            }
        }
    }
    static class bantime extends Thread{
        @Override
        public void run(){
            try{
                String db = Core.settings.getDataDirectory().child("mods/Essentials/banned.json").readString();
                JSONTokener parser = new JSONTokener(db);
                JSONArray object = new JSONArray(parser);

                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd a hh:mm.ss", Locale.ENGLISH);
                String myTime = now.format(dateTimeFormatter);

                for(int i = 0; i < object.length(); i++) {
                    JSONObject value1 = object.getJSONObject(i);
                    String date = (String) value1.get("date");
                    String uuid = (String) value1.get("uuid");
                    String name = (String) value1.get("name");

                    if (date.equals(myTime)) {
                        Log.info(myTime);
                        object.remove(i);
                        Core.settings.getDataDirectory().child("mods/Essentials/banned.json").writeString(String.valueOf(object));
                        netServer.admins.unbanPlayerID(uuid);
                        Global.log("["+myTime+"] [Bantime]"+name+"/"+uuid+" player unbanned!");
                    }
                }
            }catch (Exception ex){
                printStackTrace(ex);
            }
        }
    }
    static class maptime extends Thread{
        @Override
        public void run(){
            if(playtime != null){
                try{
                    Calendar cal1;
                    SimpleDateFormat format = new SimpleDateFormat("HH:mm.ss");
                    Date d2 = format.parse(playtime);
                    cal1 = Calendar.getInstance();
                    cal1.setTime(d2);
                    cal1.add(Calendar.SECOND, 1);
                    playtime = format.format(cal1.getTime());
                    // Anti PvP rushing timer
                    if(enableantirush && Vars.state.rules.pvp && cal1.equals(antirushtime)) {
                        Call.sendMessage("[scarlet]== NOTICE ==");
                        Call.sendMessage("[green]Peace time is over!");
                        Call.sendMessage("[green]You can now attack other teams using your own mechs!");

                        state.rules.playerDamageMultiplier = 1f;
                        state.rules.playerHealthMultiplier = 1f;
                        for(int i = 0; i < playerGroup.size(); i++) {
                            Player player = playerGroup.all().get(i);
                            Call.onPlayerDeath(player);
                        }
                    }
                }catch (Exception e){
                    printStackTrace(e);
                }
            }
        }
    }
    static class uptime extends Thread{
        @Override
        public void run(){
            if(uptime != null){
                try{
                    Calendar cal1;
                    SimpleDateFormat format = new SimpleDateFormat("HH:mm.ss");
                    Date d2 = format.parse(uptime);
                    cal1 = Calendar.getInstance();
                    cal1.setTime(d2);
                    cal1.add(Calendar.SECOND, 1);
                    uptime = format.format(cal1.getTime());
                }catch (Exception e){
                    printStackTrace(e);
                }
            }
        }
    }
    static class jumpzone extends Thread{
        @Override
        public void run(){
            if (playerGroup.size() > 0) {
                for (int i=0;i<jumpzone.length();i++) {
                    String jumpdata = jumpzone.getString(i);
                    if (jumpdata.equals("")) return;
                    String[] data = jumpdata.split("/");
                    int startx = Integer.parseInt(data[0]);
                    int starty = Integer.parseInt(data[1]);
                    int tilex = Integer.parseInt(data[2]);
                    int tiley = Integer.parseInt(data[3]);
                    String serverip = data[4];
                    int serverport = Integer.parseInt(data[5]);
                    int block = Integer.parseInt(data[6]);

                    Block target;
                    switch (block) {
                        case 1:
                        default:
                            target = Blocks.metalFloor;
                            break;
                        case 2:
                            target = Blocks.metalFloor2;
                            break;
                        case 3:
                            target = Blocks.metalFloor3;
                            break;
                        case 4:
                            target = Blocks.metalFloor5;
                            break;
                        case 5:
                            target = Blocks.metalFloorDamaged;
                            break;
                    }

                    if (!world.tile(startx, starty).block().name.matches(".*metal.*")) {
                        int size = tilex - startx;
                        for(int x = 0; x < size; x++) {
                            for(int y = 0; y < size; y++) {
                                Tile tile = world.tile(startx+x, starty+y);
                                Call.onConstructFinish(tile, target, 0, (byte) 0, Team.sharded, false);
                            }
                        }
                    }

                    for(int ix = 0; ix < playerGroup.size(); ix++) {
                        Player player = playerGroup.all().get(ix);
                        if (player.tileX() > startx && player.tileX() < tilex) {
                            if (player.tileY() > starty && player.tileY() < tiley) {
                                Call.onConnect(player.con, serverip, serverport);
                            }
                        }
                    }
                }
            }
        }
    }
    static class checkgrief extends Thread{
        Player player;
        int routercount;
        int breakcount;
        int conveyorcount;
        ArrayList<Block> impblock = new ArrayList<>();
        ArrayList<Block> block = new ArrayList<>();

        checkgrief(Player player){
            this.player = player;
        }

        @Override
        public void run(){
            // Important blocks
            impblock.add(Blocks.thoriumReactor);
            impblock.add(Blocks.impactReactor);
            impblock.add(Blocks.blastDrill);
            impblock.add(Blocks.siliconSmelter);
            impblock.add(Blocks.cryofluidMixer);
            impblock.add(Blocks.battery);
            impblock.add(Blocks.batteryLarge);
            impblock.add(Blocks.oilExtractor);
            impblock.add(Blocks.spectre);
            impblock.add(Blocks.meltdown);
            impblock.add(Blocks.turbineGenerator);

            // Normal blocks
            block.add(Blocks.phaseConduit);

            routercount = 0;
            breakcount = 0;
            conveyorcount = 0;

            // Break count
            Events.on(BuildSelectEvent.class, e -> {
                if(e.builder instanceof Player && e.builder.buildRequest() != null && !e.builder.buildRequest().block.name.matches(".*build.*")) {
                    if (e.breaking) {
                        Block block = e.builder.buildRequest().block;
                        if(block == Blocks.thoriumReactor){
                            breakcount++;
                        }
                    }
                }
            });

            // Place count
            Events.on(BlockBuildEndEvent.class, e -> {
                if (!e.breaking && e.player != null && e.player.buildRequest() != null && !state.teams.get(e.player.getTeam()).cores.isEmpty()) {
                    if(e.player.buildRequest().block == Blocks.router){
                        routercount++;
                        if(routercount > 20){
                            Call.sendMessage("[scarlet]ALERT! "+e.player.name+"[white] player is spamming [gray]router[]!");
                        }
                        for (Block value : impblock) {
                            if (e.player.buildRequest().block == value) {
                                breakcount++;
                                if (breakcount > 15) {
                                    Call.sendMessage("[scarlet]ALERT! "+e.player.name+"[white] player is destroying an [green]important building[]!");
                                }
                            }
                        }
                        for (Block value : block) {
                            if (e.player.buildRequest().block == value) {
                                conveyorcount++;
                                if (conveyorcount > 30) {
                                    Call.sendMessage("[scarlet]ALERT! "+e.player.name+"[white] player is destroying an many [green]conveyors[]!");
                                }
                            }
                        }
                    }
                }
            });

            Thread reset = new Thread(() -> {
                try {
                    Thread.sleep(20000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                routercount = 0;
                breakcount = 0;
                conveyorcount = 0;
            });
            reset.start();
        }

    }
    static class login extends TimerTask{
        @Override
        public void run() {
            Thread.currentThread().setName("Login alert thread");
            if (playerGroup.size() > 0) {
                for(int i = 0; i < playerGroup.size(); i++) {
                    Player player = playerGroup.all().get(i);
                    if (Vars.state.teams.get(player.getTeam()).cores.isEmpty()) {
                        String message1 = "You will need to login with [accent]/login <username> <password>[] to get access to the server.\n" +
                                "If you don't have an account, use the command [accent]/register <username> <password> <password repeat>[].";
                        String message2 = "서버를 플레이 할려면 [accent]/login <사용자 이름> <비밀번호>[] 를 입력해야 합니다.\n" +
                                "만약 계정이 없다면 [accent]/register <사용자 이름> <비밀번호> <비밀번호 재입력>[]를 입력해야 합니다.";
                        player.sendMessage(message1);
                        player.sendMessage(message2);
                    }
                }
            }
        }
    }
    static class checkvote extends Thread{
        @Override
        public void run() {
            if(!isvoting){
                Vote.counting.interrupt();
                /*Vote.gameover.interrupt();
                Vote.skipwave.interrupt();
                Vote.kick.interrupt();*/
            }
        }
    }
    static class jumpcheck extends Thread {
        // Source from Anuken/CoreBot
        @Override
        public void run() {
            for (int i=0;i<jumpcount.length();i++) {
                String jumpdata = jumpcount.getString(i);
                String[] data = jumpdata.split("/");
                String serverip = data[0];
                int port = Integer.parseInt(data[1]);
                int x = Integer.parseInt(data[2]);
                int y = Integer.parseInt(data[3]);
                String count = data[4];
                int length = Integer.parseInt(data[5]);

                int finalI = i;
                pingServer(serverip, port, result -> {
                    if (result.valid) {
                        String str = result.players;
                        int[] digits = new int[str.length()];
                        for(int a = 0; a < str.length(); a++) digits[a] = str.charAt(a) - '0';

                        Tile tile = world.tile(x, y);
                        if(!count.equals(result.players)) {
                            if(length != digits.length){
                                for(int px=0;px<3;px++){
                                    for(int py=0;py<5;py++){
                                        Call.onDeconstructFinish(world.tile(tile.x+4+px,tile.y+py), Blocks.air, 0);
                                    }
                                }
                            }
                            for (int digit : digits) {
                                setcount(tile, digit);
                                tile = world.tile(tile.x+4, tile.y);
                            }
                        } else {
                            for(int l=0;l<length;l++) {
                                setcount(tile, digits[l]);
                                tile = world.tile(x + 4, y);
                            }
                        }
                        // i 번째 server ip, 포트, x좌표, y좌표, 플레이어 인원, 플레이어 인원 길이
                        jumpcount.put(finalI, serverip + "/" + port + "/" + x + "/" + y + "/" + result.players + "/" + digits.length);
                    }
                });
            }
        }

        void pingServer(String ip, int port, Consumer<PingResult> listener) {
            try {
                DatagramSocket socket = new DatagramSocket();
                socket.send(new DatagramPacket(new byte[]{-2, 1}, 2, InetAddress.getByName(ip), port));

                socket.setSoTimeout(2000);

                DatagramPacket packet = new DatagramPacket(new byte[256], 256);

                long start = System.currentTimeMillis();
                socket.receive(packet);

                ByteBuffer buffer = ByteBuffer.wrap(packet.getData());
                listener.accept(readServerData(buffer, ip, System.currentTimeMillis() - start));
                socket.disconnect();
            } catch (Exception ignored) {}
        }

        private static PingResult readServerData(ByteBuffer buffer, String ip, long ping){
            byte hlength = buffer.get();
            byte[] hb = new byte[hlength];
            buffer.get(hb);

            byte mlength = buffer.get();
            byte[] mb = new byte[mlength];
            buffer.get(mb);

            String host = new String(hb);
            String map = new String(mb);

            int players = buffer.getInt();
            int wave = buffer.getInt();
            int version = buffer.getInt();

            return new PingResult(ip, ping, players+"", host, map, wave+"", version == -1 ? "Custom Build" : (""+version));
        }

        static class PingResult{
            boolean valid;
            String players;
            String host;
            String error;
            String wave;
            String map;
            String ip;
            String version;
            long ping;

            public PingResult(String ip, String error){
                this.valid = false;
                this.error = error;
                this.ip = ip;
            }

            public PingResult(String error){
                this.valid = false;
                this.error = error;
            }

            PingResult(String ip, long ping, String players, String host, String map, String wave, String version){
                this.ping = ping;
                this.ip = ip;
                this.valid = true;
                this.players = players;
                this.host = host;
                this.map = map;
                this.wave = wave;
                this.version = version;
            }
        }
    }
    static class jumpall extends Thread{
        @Override
        public void run() {
            for (int i=0;i<jumpall.length();i++) {
                String jumpdata = jumpall.getString(i);
                String[] data = jumpdata.split("/");
                int x = Integer.parseInt(data[0]);
                int y = Integer.parseInt(data[1]);
                int count = Integer.parseInt(data[2]);
                int length = Integer.parseInt(data[3]);

                int result = 0;
                for (int l=0;l<jumpcount.length();l++) {
                    String dat = jumpcount.getString(l);
                    String[] re = dat.split("/");
                    result += Integer.parseInt(re[4]);
                }

                String str = String.valueOf(result);
                int[] digits = new int[str.length()];
                for(int a = 0; a < str.length(); a++) digits[a] = str.charAt(a) - '0';

                Tile tile = world.tile(x, y);
                if(count != result) {
                    if(length != digits.length){
                        for(int px=0;px<3;px++){
                            for(int py=0;py<5;py++){
                                Call.onDeconstructFinish(world.tile(tile.x+4+px,tile.y+py), Blocks.air, 0);
                            }
                        }
                    }
                    for (int digit : digits) {
                        setcount(tile, digit);
                        tile = world.tile(tile.x+4, tile.y);
                    }
                } else {
                    for(int l=0;l<length;l++) {
                        setcount(tile, digits[l]);
                        tile = world.tile(x+4, y);
                    }
                }
                jumpall.put(i, x+"/"+y+"/"+result+"/"+digits.length);
            }
        }
    }
}