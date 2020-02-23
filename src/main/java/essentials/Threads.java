package essentials;

import arc.ApplicationListener;
import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.struct.Array;
import arc.util.Strings;
import arc.util.Time;
import essentials.core.PlayerDB;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.core.GameState;
import mindustry.entities.type.Player;
import mindustry.game.EventType;
import mindustry.game.Gamemode;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.io.SaveIO;
import mindustry.maps.Map;
import mindustry.net.Packets;
import mindustry.type.Item;
import mindustry.type.ItemType;
import mindustry.world.Tile;
import mindustry.world.blocks.logic.MessageBlock;
import org.codehaus.plexus.util.FileUtils;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static essentials.Global.*;
import static essentials.Main.root;
import static essentials.PluginData.*;
import static essentials.core.Exp.exp;
import static essentials.core.Log.writeLog;
import static essentials.core.PlayerDB.PlayerData;
import static essentials.special.PingServer.pingServer;
import static essentials.utils.Config.PluginConfig;
import static mindustry.Vars.*;
import static mindustry.core.NetClient.onSetRules;

public class Threads extends TimerTask{
    public static LocalTime playtime = LocalTime.of(0,0,0);
    public static LocalTime uptime = LocalTime.of(0,0,0);
    public static boolean isvoting;

    @Override
    public void run() {
        // 서버 켜진시간 카운트
        uptime = uptime.plusSeconds(1);

        // 데이터 저장
        JsonObject data = new JsonObject();
        data.add("servername", Core.settings.getString("servername"));
        root.child("data/data.json").writeString(data.toString());

        // 현재 서버 이름에다가 클라이언트 서버에 대한 인원 새기기
        // new changename().start();

        // 임시로 밴당한 유저 감시
        for (int a = 0; a < banned.size(); a++) {
            LocalDateTime time = LocalDateTime.now();
            if (time.isAfter(banned.get(a).getTime())) {
                banned.remove(a);
                PluginConfig.get("banned").asArray().remove(a);
                netServer.admins.unbanPlayerID(banned.get(a).uuid);
                nlog(LogType.log,"[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + banned.get(a).name + "/" + banned.get(a).uuid + " player unbanned!");
                break;
            }
        }

        // 맵이 돌아가고 있을 때
        if(state.is(GameState.State.playing)) {
            // 서버간 이동 패드에 플레이어가 있는지 확인
            // new jumpzone().start();

            // 맵 플탐 카운트
            playtime = playtime.plusSeconds(1);

            // PvP 평화시간 카운트
            if (config.isEnableantirush() && state.rules.pvp && playtime.isAfter(config.getAntirushtime())) {
                state.rules.playerDamageMultiplier = 0.66f;
                state.rules.playerHealthMultiplier = 0.8f;
                onSetRules(state.rules);
                for (int i = 0; i < playerGroup.size(); i++) {
                    Player player = playerGroup.all().get(i);
                    player.sendMessage(bundle("pvp-peacetime"));
                    Call.onPlayerDeath(player);
                }
            }

            // 모든 클라이언트 서버에 대한 인원 총합 카운트
            for (int a = 0; a < jumptotal.size(); a++) {
                int result = 0;
                for (jumpcount value : jumpcount) result = result + value.players;

                String str = String.valueOf(result);
                int[] digits = new int[str.length()];
                for (int b = 0; b < str.length(); b++) digits[b] = str.charAt(b) - '0';

                Tile tile = jumptotal.get(a).getTile();
                if (jumptotal.get(a).totalplayers != result) {
                    if (jumptotal.get(a).numbersize != digits.length) {
                        for (int px = 0; px < 3; px++) {
                            for (int py = 0; py < 5; py++) {
                                Call.onDeconstructFinish(world.tile(tile.x + 4 + px, tile.y + py), Blocks.air, 0);
                            }
                        }
                    }
                    for (int digit : digits) {
                        setcount(tile, digit);
                        tile = world.tile(tile.x + 4, tile.y);
                    }
                } else {
                    for (int l = 0; l < jumptotal.get(a).numbersize; l++) {
                        setcount(tile, digits[l]);
                        tile = world.tile(tile.x + 4, tile.y);
                    }
                }
                jumptotal.set(a, new jumptotal(tile, result, digits.length));
            }

            // 플레이어 플탐 카운트 및 잠수확인
            if(playerGroup.size() > 0){
                for(int i = 0; i < playerGroup.size(); i++) {
                    Player player = playerGroup.all().get(i);
                    PlayerData target = PlayerData(player.uuid);
                    boolean kick = false;

                    if (target.isLogin) {
                        // Exp 계산
                        target.exp = target.exp + (int) (Math.random() * 5);

                        // 잠수 및 플레이 시간 계산
                        target.playtime = LocalTime.parse(target.playtime, DateTimeFormatter.ofPattern("HH:mm.ss")).plusSeconds(1).format(DateTimeFormatter.ofPattern("HH:mm.ss"));
                        if (target.afk_tilex == player.tileX() && target.afk_tiley == player.tileY()) {
                            target.afk = target.afk.plusSeconds(1);
                            if (target.afk == LocalTime.of(0, 5, 0)) {
                                kick = true;
                            }
                        } else {
                            target.afk = LocalTime.of(0, 0, 0);
                        }
                        target.afk_tilex = player.tileX();
                        target.afk_tiley = player.tileY();

                        if (!state.rules.editor) exp(player);
                        if (kick) Call.onKick(player.con, "AFK");
                    }
                    if (target.grief_destory_count > 0) target.grief_destory_count--;
                    if (target.grief_build_count > 0) target.grief_build_count--;
                }
            }

            // 메세지 블럭 감시
            for(int a=0;a<messagemonitor.size();a++) {
                String msg;
                MessageBlock.MessageBlockEntity entity;
                try {
                    entity = (MessageBlock.MessageBlockEntity) messagemonitor.get(a).tile.entity;
                    msg = entity.message;
                }catch (NullPointerException e){
                    messagemonitor.remove(a);
                    return;
                }

                if (msg.equals("powerblock")) {
                    Tile target;

                    if (entity.tile.getNearby(0).entity != null) {
                        target = entity.tile.getNearby(0);
                    } else if (entity.tile.getNearby(1).entity != null) {
                        target = entity.tile.getNearby(1);
                    } else if (entity.tile.getNearby(2).entity != null) {
                        target = entity.tile.getNearby(2);
                    } else if (entity.tile.getNearby(3).entity != null) {
                        target = entity.tile.getNearby(3);
                    } else {
                        return;
                    }
                    powerblock.add(new powerblock(entity.tile,target));
                    messagemonitor.remove(a);
                    break;
                } else if (msg.contains("jump")) {
                    messagejump.add(new messagejump(messagemonitor.get(a).tile,msg));
                    messagemonitor.remove(a);
                    break;
                } else if (msg.equals("scancore")) {
                    scancore.add(messagemonitor.get(a).tile);
                    messagemonitor.remove(a);
                    break;
                }
            }

            // 서버 인원 확인
            for (int i = 0; i < jumpcount.size(); i++) {
                int i2 = i;
                jumpcount value = jumpcount.get(i);

                pingServer(jumpcount.get(i).serverip, result -> {
                    if (result.name != null) {
                        String str = String.valueOf(result.players);
                        int[] digits = new int[str.length()];
                        for (int a = 0; a < str.length(); a++) digits[a] = str.charAt(a) - '0';

                        Tile tile = value.getTile();
                        if (value.players != result.players) {
                            if (value.numbersize != digits.length) {
                                for (int px = 0; px < 3; px++) {
                                    for (int py = 0; py < 5; py++) {
                                        Call.onDeconstructFinish(world.tile(tile.x + 4 + px, tile.y + py), Blocks.air, 0);
                                    }
                                }
                            }
                            for (int digit : digits) {
                                setcount(tile, digit);
                                tile = world.tile(tile.x + 4, tile.y);
                            }
                        } else {
                            for (int l = 0; l < value.numbersize; l++) {
                                setcount(tile, digits[l]);
                                tile = world.tile(value.getTile().x + 4, value.getTile().y);
                            }
                        }
                        // i 번째 server ip, 포트, x좌표, y좌표, 플레이어 인원, 플레이어 인원 길이
                        jumpcount.set(i2,new jumpcount(value.getTile(),value.serverip,result.players,digits.length));
                    } else {
                        setno(value.getTile(), true);
                    }
                });
            }

            // 서버간 이동 영역에 플레이어가 있는지 확인
            for (jumpzone value : jumpzone) {
                if(!value.touch) {
                    for (int ix = 0; ix < playerGroup.size(); ix++) {
                        Player player = playerGroup.all().get(ix);
                        if (player.tileX() > value.startx && player.tileX() < value.finishx) {
                            if (player.tileY() > value.starty && player.tileY() < value.finishy) {
                                String resultIP = value.ip;
                                int port = 6567;
                                if (resultIP.contains(":") && Strings.canParsePostiveInt(resultIP.split(":")[1])) {
                                    String[] temp = resultIP.split(":");
                                    resultIP = temp[0];
                                    port = Integer.parseInt(temp[1]);
                                }
                                log(LogType.log, "player-jumped", player.name, resultIP + ":" + port);
                                Call.onConnect(player.con, resultIP, port);
                            }
                        }
                    }
                }
            }
        }
    }
    static class login extends TimerTask{
        @Override
        public void run() {
            Thread.currentThread().setName("Login alert thread");
            if (playerGroup.size() > 0) {
                for(int i = 0; i < playerGroup.size(); i++) {
                    Player player = playerGroup.all().get(i);
                    if (isNocore(player)) {
                        try {
                            String message;
                            String json = Jsoup.connect("http://ipapi.co/" + Vars.netServer.admins.getInfo(player.uuid).lastIP + "/json").ignoreContentType(true).execute().body();
                            JsonObject result = JsonValue.readJSON(json).asObject();
                            String language = result.getString("languages", "en");
                            if (config.getPasswordmethod().equals("discord")) {
                                message = nbundle(language, "login-require-discord") + "\n" + config.getDiscordLink();
                            } else {
                                message = nbundle(language, "login-require-password");
                            }
                            player.sendMessage(message);
                        }catch (Exception e){
                            printError(e);
                        }
                    }
                }
            }
        }
    }
    static class changename extends Thread {
        @Override
        public void run(){
            if(jumpcount.size() > 1){
                int result = 0;
                for (jumpcount value : jumpcount) result = result + value.players;
                Core.settings.put("servername", config.getServername()+", "+result+" players");
            }
        }
    }
    public static class AutoRollback extends TimerTask {
        private void save() {
            try {
                Fi file = saveDirectory.child(config.getSlotnumber() + "." + saveExtension);
                if(state.is(GameState.State.playing)) SaveIO.save(file);
            } catch (Exception e) {
                printError(e);
            }
        }

        void load() {
            Array<Player> all = Vars.playerGroup.all();
            Array<Player> players = new Array<>();
            players.addAll(all);

            try {
                Fi file = saveDirectory.child(config.getSlotnumber() + "." + saveExtension);
                SaveIO.load(file);
            } catch (SaveIO.SaveException e) {
                printError(e);
            }

            Call.onWorldDataBegin();

            for (Player p : players) {
                Vars.netServer.sendWorldData(p);
                p.reset();

                if (Vars.state.rules.pvp) {
                    p.setTeam(Vars.netServer.assignTeam(p, new Array.ArrayIterable<>(players)));
                }
            }
            nlog(LogType.log,"Map rollbacked.");
            Call.sendMessage("[green]Map rollbacked.");
        }

        @Override
        public void run() {
            save();
        }
    }
    static class eventserver extends Thread {
        String roomname;
        String map;
        String gamemode;
        int customport;

        eventserver(String roomname, String map, String gamemode, int customport){
            this.gamemode = gamemode;
            this.map = map;
            this.roomname = roomname;
            this.customport = customport;
        }

        @Override
        public void run() {
            try {
                FileUtils.copyURLToFile(new URL("https://github.com/Anuken/Mindustry/releases/download/v102/server-release.jar"), new File(Paths.get("").toAbsolutePath().toString()+"/config/mods/Essentials/temp/"+roomname+"/server.jar"));
                Service service = new Service(roomname, map, gamemode, customport);
                service.start();
                Thread.sleep(10000);
            } catch (Exception e) {
                printError(e);
            }
        }

        public static class Service extends Thread {
            String roomname;
            String map;
            String gamemode;
            int customport;
            int disablecount;

            Service(String roomname, String map, String gamemode, int customport) {
                this.gamemode = gamemode;
                this.map = map;
                this.roomname = roomname;
                this.customport = customport;
            }

            @Override
            public void run(){
                try {
                    Process p;
                    ProcessBuilder pb;
                    if(gamemode.equals("wave")){
                        pb = new ProcessBuilder("java", "-jar", Paths.get("").toAbsolutePath().toString() + "/config/mods/Essentials/temp/" + roomname + "/server.jar", "config port "+customport+",host "+map);
                    } else {
                        pb = new ProcessBuilder("java", "-jar", Paths.get("").toAbsolutePath().toString() + "/config/mods/Essentials/temp/" + roomname + "/server.jar", "config port "+customport+",host "+map+" "+gamemode);
                    }
                    pb.directory(new File(Paths.get("").toAbsolutePath().toString() + "/config/mods/Essentials/temp/" + roomname));
                    pb.inheritIO().redirectOutput(Core.settings.getDataDirectory().child("test.txt").file());
                    p = pb.start();
                    process.add(p);
                    if(p.isAlive()) nlog(LogType.log,"online");
                    Process finalP = p;
                    TimerTask t = new TimerTask() {
                        @Override
                        public void run() {
                            pingServer("localhost", result -> {
                                if (disablecount > 300) {
                                    try {
                                        JsonObject settings = JsonValue.readJSON(root.child("data/data.json").reader()).asObject();
                                        for (int a = 0; a < settings.get("servers").asArray().size(); a++) {
                                            if (settings.get("servers").asArray().get(a).asObject().getInt("port",0) == customport) {
                                                settings.get("servers").asArray().remove(a);
                                                root.child("data/data.json").writeString(settings.toString());
                                                break;
                                            }
                                        }

                                        finalP.destroy();
                                        process.remove(finalP);
                                        this.cancel();
                                    } catch (IOException e) {
                                        printError(e);
                                    }
                                } else if (result.players == 0) {
                                    disablecount++;
                                }
                            });
                        }
                    };
                    Timer timer = new Timer(true);
                    timer.scheduleAtFixedRate(t, 1000, 1000);

                    Core.app.addListener(new ApplicationListener(){
                        @Override
                        public void dispose(){
                            timer.cancel();
                        }
                    });
                }catch (Exception e){
                    printError(e);
                }
            }
        }
    }
    public static class ColorNick implements Runnable{
        private static int colorOffset = 0;
        private static long updateIntervalMs = config.getCupdatei();

        final Player player;

        public ColorNick(Player player){
            this.player = player;
        }

        @Override
        public void run() {
            Thread.currentThread().setName(player.name+" color nickname thread");
            PlayerData db = PlayerData(player.uuid);
            while (db.connected) {
                String name = db.name.replaceAll("\\[(.*?)]", "");
                try {
                    Thread.sleep(updateIntervalMs);
                    nickcolor(name, player);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        private void nickcolor(String name, Player player) {
            StringBuilder stringBuilder = new StringBuilder();

            String[] colors = new String[11];
            colors[0] = "[#ff0000]";
            colors[1] = "[#ff7f00]";
            colors[2] = "[#ffff00]";
            colors[3] = "[#7fff00]";
            colors[4] = "[#00ff00]";
            colors[5] = "[#00ff7f]";
            colors[6] = "[#00ffff]";
            colors[7] = "[#007fff]";
            colors[8] = "[#0000ff]";
            colors[9] = "[#8000ff]";
            colors[10] = "[#ff00ff]";

            String[] newnick = new String[name.length()];
            for (int i = 0; i<name.length(); i++) {
                char c = name.charAt(i);
                int colorIndex = (i+colorOffset)%colors.length;
                if (colorIndex < 0) {
                    colorIndex += colors.length;
                }
                String newtext = colors[colorIndex]+c;
                newnick[i]=newtext;
            }
            colorOffset--;
            for (String s : newnick) {
                stringBuilder.append(s);
            }
            player.name = stringBuilder.toString();
        }
    }
    static class monitorresource extends Thread {
        Array<Integer> pre = new Array<>();
        Array<Integer> cur = new Array<>();
        Array<Item> name = new Array<>();

        @Override
        public void run(){
            Thread.currentThread().setName("Resource monitoring thread");
            while(Thread.currentThread().isInterrupted()) {
                if(state.is(GameState.State.playing)) {
                    for (Item item : content.items()) {
                        if (item.type == ItemType.material) {
                            pre.add(state.teams.get(Team.sharded).cores.first().items.get(item));
                        }
                    }

                    for (Item item : content.items()) {
                        if (item.type == ItemType.material) {
                            name.add(item);
                        }
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }

                    int a = 0;
                    for (Item item : content.items()) {
                        if (item.type == ItemType.material) {
                            int resource;
                            if (state.teams.get(Team.sharded).cores.isEmpty()) return;
                            if (state.teams.get(Team.sharded).cores.first().items.has(item)) {
                                resource = state.teams.get(Team.sharded).cores.first().items.get(item);
                            } else {
                                return;
                            }
                            int temp = resource - pre.get(a);
                            if (temp <= -55) {
                                StringBuilder using = new StringBuilder();
                                if(Vars.state.is(GameState.State.playing)) {
                                    for (int b = 0; b < playerGroup.size(); b++) {
                                        Player p = playerGroup.all().get(b);
                                        if (p.buildRequest().block == null) return;
                                        for (int c = 0; c < p.buildRequest().block.requirements.length; c++) {
                                            Item ad = p.buildRequest().block.requirements[c].item;
                                            if (ad == name.get(a)) {
                                                using.append(p.name).append(", ");
                                            }
                                        }
                                    }
                                    allsendMessage("resource-fast", name.get(a).name);
                                    allsendMessage("resource-fast-use", name.get(a).name, using.substring(0, using.length() - 2));
                                }
                            }
                            cur.add(a, state.teams.get(Team.sharded).cores.first().items.get(item));
                            a++;
                        }
                    }

                    for (Item item : content.items()) {
                        if (item.type == ItemType.material) {
                            pre.add(state.teams.get(Team.sharded).cores.first().items.get(item));
                        }
                    }
                } else {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        }
    }
    public static class Vote{
        private static Player player;
        private static Player target;
        private static Map map;
        private static String type;
        private static Timer votetimer = new Timer();
        private static Timer bundletimer = new Timer();

        private static int time = 0;
        private static int bundletime = 0;

        static ArrayList<String> list = new ArrayList<>();
        static int require;

        Vote(Player player, String type, Player target){
            Vote.player = player;
            Vote.type = type;
            Vote.target = target;
            command();
        }

        Vote(Player player, String type, Map map){
            Vote.player = player;
            Vote.type = type;
            Vote.map = map;
            command();
        }

        Vote(Player player, String type){
            Vote.player = player;
            Vote.type = type;
            command();
        }

        // 1초마다 실행됨
        TimerTask counting = new TimerTask() {
            @Override
            public void run() {
                Thread.currentThread().setName("Vote counting timertask");
                time++;
                if(time >= 60){
                    Vote.cancel();
                }
            }
        };

        // 10초마다 실행됨
        TimerTask alert = new TimerTask() {
            @Override
            public void run() {
                Thread.currentThread().setName("Vote alert timertask");
                String[] bundlename = {"vote-50sec", "vote-40sec", "vote-30sec", "vote-20sec", "vote-10sec"};

                if(bundletime <= 4){
                    if (playerGroup != null && playerGroup.size() > 0) {
                        allsendMessage(bundlename[bundletime]);
                    }
                    bundletime++;
                }
            }
        };

        static void cancel() {
            isvoting = false;

            votetimer.cancel();
            votetimer = new Timer();
            time = 0;

            bundletimer.cancel();
            bundletimer = new Timer();
            bundletime = 0;

            switch (type) {
                case "gameover":
                    if (list.size() >= require) {
                        allsendMessage("vote-gameover-done");
                        Events.fire(new EventType.GameOverEvent(Team.crux));
                    } else {
                        allsendMessage("vote-gameover-fail");
                    }
                    break;
                case "skipwave":
                    if (list.size() >= require) {
                        allsendMessage("vote-skipwave-done");
                        for (int i = 0; i < 5; i++) {
                            logic.runWave();
                        }
                    } else {
                        allsendMessage("vote-skipwave-fail");
                    }
                    break;
                case "kick":
                    if (list.size() >= require) {
                        allsendMessage("vote-kick-done", target.name);
                        PlayerDB.addtimeban(target.name, target.uuid, 4);

                        writeLog(LogType.player,nbundle("log-player-kick",target.name,require));

                        target.getInfo().lastKicked = Time.millis() + (15 * 60)*1000;
                        playerGroup.all().each(p -> p.uuid != null && p.uuid.equals(target.uuid), p -> p.con.kick(Packets.KickReason.vote));
                    } else {
                        allsendMessage("vote-kick-fail");
                    }
                    break;
                case "rollback":
                    if (list.size() >= require) {
                        allsendMessage("vote-rollback-done");
                        Threads.AutoRollback rl = new Threads.AutoRollback();
                        rl.load();
                    } else {
                        allsendMessage("vote-rollback-fail");
                    }
                    break;
                case "map":
                    if (list.size() >= require) {
                        Array<Player> all = Vars.playerGroup.all();
                        Array<Player> players = new Array<>();
                        players.addAll(all);

                        Gamemode current = Gamemode.survival;
                        if(state.rules.attackMode){
                            current = Gamemode.attack;
                        } else if(state.rules.pvp){
                            current = Gamemode.pvp;
                        } else if(state.rules.editor){
                            current = Gamemode.editor;
                        }

                        world.loadMap(map, map.applyRules(current));

                        Call.onWorldDataBegin();

                        for (Player p : players) {
                            Vars.netServer.sendWorldData(p);
                            p.reset();

                            if (Vars.state.rules.pvp) {
                                p.setTeam(Vars.netServer.assignTeam(p, new Array.ArrayIterable<>(players)));
                            }
                        }
                        nlog(LogType.log,"Map rollbacked.");
                        allsendMessage("vote-map-done");
                    } else {
                        allsendMessage("vote-map-fail");
                    }
                    break;
            }
            list.clear();
        }

        void command(){
            if(playerGroup.size() == 1){
                player.sendMessage(bundle(player, "vote-min"));
                return;
            } else if(playerGroup.size() <= 3){
                require = 2;
            } else {
                require = (int) Math.ceil((double) playerGroup.size() / 2);
            }

            if(!isvoting){
                switch (type){
                    case "gameover":
                        allsendMessage("vote-gameover");
                        break;
                    case "skipwave":
                        allsendMessage("vote-skipwave");
                        break;
                    case "kick":
                        allsendMessage("vote-kick", target.name);
                        break;
                    case "rollback":
                        if(config.isEnableRollback()) {
                            allsendMessage("vote-rollback");
                            break;
                        } else {
                            player.sendMessage(bundle(player,"vote-rollback-disabled"));
                            return;
                        }
                    case "map":
                        allsendMessage("vote-map");
                        break;
                    default:
                        // 모드가 잘못되었을 때
                        player.sendMessage("wrong mode");
                        return;
                }
                isvoting = true;
                votetimer.schedule(counting, 0, 1000);
                bundletimer.schedule(alert, 10000, 10000);
            } else {
                player.sendMessage(bundle("vote-in-processing"));
            }
        }
    }
    public static class jumpdata extends Thread{
        @Override
        public void run() {
            while(true) {
                for (int a = 0; a < messagejump.size(); a++) {
                    if(messagejump.get(a).tile.entity.block != Blocks.message){
                        messagejump.remove(a);
                        break;
                    }
                    Call.setMessageBlockText(null, messagejump.get(a).tile, "[green]Working...");

                    String[] arr = messagejump.get(a).message.split(" ");
                    String ip = arr[1];

                    int fa = a;
                    pingServer(ip, result -> {
                        if (result.name != null){
                            Call.setMessageBlockText(null, messagejump.get(fa).tile, "[green]"+result.players + " Players in this server.");
                        } else {
                            Call.setMessageBlockText(null, messagejump.get(fa).tile, "[scarlet]Server offline");
                        }
                    });
                }
                try {
                    sleep(2500);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }
    public static class visualjump extends Thread{
        public static int length = 0;
        public static ArrayList<Thread> thread = new ArrayList<>();

        @Override
        public void run() {
            main();
        }

        public static void main(){
            length = jumpzone.size();

            for (jumpzone data : jumpzone) {
                Thread t = new Thread(() -> {
                    while (true) {
                        String ip = data.ip;
                        if(state.is(GameState.State.playing)) {
                            pingServer(ip, result -> {
                                try {
                                    if (result.name != null) {
                                        int size = data.getFinishTile().x - data.getStartTile().x;

                                        for (int x = 0; x < size; x++) {
                                            Tile tile = world.tile(data.getStartTile().x + x, data.getStartTile().y);
                                            Call.onConstructFinish(tile, Blocks.air, 0, (byte) 0, Team.sharded, true);
                                            sleep(96);
                                        }
                                        for (int y = 0; y < size; y++) {
                                            Tile tile = world.tile(data.getFinishTile().x, data.getStartTile().y + y);
                                            Call.onConstructFinish(tile, Blocks.air, 0, (byte) 0, Team.sharded, true);
                                            sleep(96);
                                        }
                                        for (int x = 0; x < size; x++) {
                                            Tile tile = world.tile(data.getFinishTile().x - x, data.getFinishTile().y);
                                            Call.onConstructFinish(tile, Blocks.air, 0, (byte) 0, Team.sharded, true);
                                            sleep(96);
                                        }
                                        for (int y = 0; y < size; y++) {
                                            Tile tile = world.tile(data.getStartTile().x, data.getFinishTile().y - y);
                                            Call.onConstructFinish(tile, Blocks.air, 0, (byte) 0, Team.sharded, true);
                                            sleep(96);
                                        }
                                        if (size < 5) sleep(2000);
                                    } else {
                                        log(LogType.debug, "jump zone " + ip + " offline! After 30 seconds, try to connect again.");
                                        sleep(30000);
                                    }
                                } catch (InterruptedException ignored) {
                                }
                            });
                        } else {
                            try {
                                sleep(1000);
                            } catch (InterruptedException ignored) {
                                return;
                            }
                        }
                    }
                });
                thread.add(t);
                t.start();
            }
        }
    }
}