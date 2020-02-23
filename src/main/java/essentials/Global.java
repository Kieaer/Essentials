package essentials;

import arc.util.Log;
import essentials.special.UTF8Control;
import essentials.utils.Bundle;
import essentials.utils.Config;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.core.Version;
import mindustry.entities.type.Player;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.world.Tile;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.hjson.Stringify;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static essentials.Main.root;
import static essentials.core.Log.writeLog;
import static essentials.core.PlayerDB.PlayerData;
import static essentials.core.PlayerDB.conn;
import static essentials.utils.Permission.permission;
import static mindustry.Vars.*;

public class Global {
    public static Config config = new Config();
    public static String plugin_version;
    public static String hostip = getip();
    public static Locale locale = new Locale(System.getProperty("user.language"), System.getProperty("user.country"));

    final static String tag = "[Essential] ";
    final static String servertag = "[EssentialServer] ";
    final static String clienttag = "[EssentialClient] ";
    final static String playertag = "[EssentialPlayer] ";
    final static String configtag = "[EssentialConfig] ";

    public final static String[] intpos = {"0,4","1,4","2,4","0,3","1,3","2,3","0,2","1,2","2,2","0,1","1,1","2,1","0,0","1,0","2,0"};

    // 로그
    public enum LogType{
        log, warn, error, debug, server, serverwarn, servererr, client, clientwarn, clienterr, config, player, playerwarn, playererr,
        tap, withdraw, block, deposit, chat, griefer, web
    }
    public static void log(LogType type, String value, Object... parameter){
        switch(type){
            case log:
                Log.info(parameter.length == 0 ? tag+nbundle(value) : tag+nbundle(value,parameter));
                break;
            case warn:
                Log.warn(parameter.length == 0 ? tag+nbundle(value) : tag+nbundle(value,parameter));
                break;
            case error:
                Log.err(parameter.length == 0 ? tag+nbundle(value) : tag+nbundle(value,parameter));
                break;
            case debug:
                Log.debug(parameter.length == 0 ? tag+nbundle(value) : tag+nbundle(value,parameter));
                break;
            case server:
                Log.info(parameter.length == 0 ? servertag+nbundle(value) : servertag+nbundle(value,parameter));
                break;
            case serverwarn:
                Log.warn(parameter.length == 0 ? servertag+nbundle(value) : servertag+nbundle(value,parameter));
                break;
            case servererr:
                Log.err(parameter.length == 0 ? servertag+nbundle(value) : servertag+nbundle(value,parameter));
                break;
            case client:
                Log.info(parameter.length == 0 ? clienttag+nbundle(value) : clienttag+nbundle(value,parameter));
                break;
            case clientwarn:
                Log.warn(parameter.length == 0 ? clienttag+nbundle(value) : clienttag+nbundle(value,parameter));
                break;
            case clienterr:
                Log.err(parameter.length == 0 ? clienttag+nbundle(value) : clienttag+nbundle(value,parameter));
                break;
            case config:
                Log.info(parameter.length == 0 ? configtag+nbundle(value) : configtag+nbundle(value,parameter));
                break;
            case player:
                Log.info(parameter.length == 0 ? playertag+nbundle(value) : playertag+nbundle(value,parameter));
                break;
            case playerwarn:
                Log.warn(parameter.length == 0 ? playertag+nbundle(value) : playertag+nbundle(value,parameter));
                break;
            case playererr:
                Log.err(parameter.length == 0 ? playertag+nbundle(value) : playertag+nbundle(value,parameter));
                break;
        }
    }

    public static void nlog(LogType type, String value){
        switch(type){
            case log:
                Log.info(value);
                break;
            case warn:
                Log.warn(value);
                break;
            case error:
                Log.err(value);
                break;
            case debug:
                Log.debug(value);
                break;
            case player:
                Log.info(playertag+value);
                break;
            case client:
                Log.info(clienttag+value);
                break;
        }
    }

    // 오류 메세지를 파일로 복사하거나 즉시 출력
    public static void printError(Throwable e) {
        if(!config.isDebug()){
            StringBuilder sb = new StringBuilder();
            try {
                sb.append(e.toString())
                .append("\n");
                StackTraceElement[] element = e.getStackTrace();
                for (StackTraceElement error : element) {
                    sb.append("\tat ")
                    .append(error.toString())
                    .append("\n");
                }
                sb.append("=================================================\n");
                String text = sb.toString();

                writeLog(LogType.error, text);
                nlog(LogType.error,"Plugin internal error! - "+e.getMessage());
                if(config.isCrashReport()){
                    InetAddress address = InetAddress.getByName("mindustry.kr");
                    Socket socket = new Socket(address, 6560);
                    BufferedReader is = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    DataOutputStream os = new DataOutputStream(socket.getOutputStream());
                    os.writeBytes(e.toString()+"\n");
                    sb = new StringBuilder();
                    sb.append(e.toString()).append("\n");
                    for (StackTraceElement error : element) {
                        sb.append("at ")
                                .append(error.toString())
                                .append("\n");
                    }
                    StringBuilder plugins = new StringBuilder();
                    for(int a=0;a<mods.list().size;a++){
                        plugins.append(mods.list().get(a).name).append(", ");
                    }

                    String logs = "플러그인 버전: " + plugin_version + "\n" +
                            "서버 버전: "+ Version.build + "\n" +
                            "OS: " + System.getProperty("os.name") + "\n" +
                            "플러그인 목록: " + plugins.toString().substring(0,plugins.length()-2) + "\n" +
                            "== 설정파일 ==\n" + JsonValue.readHjson(root.child("config.hjson").readString()).toString(Stringify.HJSON) + "\n" +
                            "== Stacktrace ==\n" + sb.toString() + "\n!exit!\n";

                    os.write(logs.getBytes(StandardCharsets.UTF_8));

                    String data = is.readLine();
                    if(data != null){
                        nlog(LogType.log, "crash reported");
                    } else {
                        nlog(LogType.log, "receive failed");
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            e.printStackTrace();
        }
    }

    // 현재 시간출력
    public static String getTime(){
        return DateTimeFormatter.ofPattern("yy-MM-dd HH:mm.ss").format(LocalDateTime.now());
    }

    // Bundle 파일에서 Essentials 문구를 포함시켜 출력
    public static String bundle(Object data, Object... parameter) {
        Locale locale = null;
        if(data instanceof Player){
            locale = PlayerData(((Player) data).uuid).locale;
        } else if (data instanceof Locale){
            locale = (Locale) data;
        } else if (data instanceof String){
            locale = config.getLanguage();
            Bundle bundle = new Bundle(locale);
            return parameter.length != 0 ? bundle.getBundle((String) data, parameter) : bundle.getBundle((String) data);
        }
        String value = (String) parameter[0];
        parameter = removeElement(0,parameter);
        Bundle bundle = new Bundle(locale);
        return parameter.length != 0 ? bundle.getBundle(value, parameter) : bundle.getBundle(value);
    }

    // Bundle 파일에서 Essentials 문구 없이 출력
    public static String nbundle(Object data, Object... parameter) {
        Locale locale = null;
        if(data instanceof Player){
            locale = PlayerData(((Player) data).uuid).locale;
        } else if (data instanceof Locale){
            locale = (Locale) data;
        } else if (data instanceof String){
            locale = config.getLanguage();
            Bundle bundle = new Bundle(locale);
            return parameter.length != 0 ? bundle.getNormal((String) data, parameter) : bundle.getNormal((String) data);
        }
        String value = (String) parameter[0];
        parameter = removeElement(0,parameter);
        Bundle bundle = new Bundle(locale);
        return parameter.length != 0 ? bundle.getNormal(value, parameter) : bundle.getNormal(value);
    }

    // 숫자 카운트
    public static void setcount(Tile tile, int count){
        int[] zero = {1,1,1,1,0,1,1,0,1,1,0,1,1,1,1};
        int[] one = {0,1,0,1,1,0,0,1,0,0,1,0,1,1,1};
        int[] two = {1,1,1,0,0,1,1,1,1,1,0,0,1,1,1};
        int[] three = {1,1,1,0,0,1,1,1,1,0,0,1,1,1,1};
        int[] four = {1,0,1,1,0,1,1,1,1,0,0,1,0,0,1};
        int[] five = {1,1,1,1,0,0,1,1,1,0,0,1,1,1,1};
        int[] six = {1,1,1,1,0,0,1,1,1,1,0,1,1,1,1};
        int[] seven = {1,1,1,1,0,1,0,0,1,0,0,1,0,0,1};
        int[] eight = {1,1,1,1,0,1,1,1,1,1,0,1,1,1,1};
        int[] nine = {1,1,1,1,0,1,1,1,1,0,0,1,1,1,1};

        switch(count) {
            case 0:
                for(int a=0;a<15;a++){
                    String position = intpos[a];
                    String[] data = position.split(",");
                    int x = Integer.parseInt(data[0]);
                    int y = Integer.parseInt(data[1]);
                    Tile target = world.tile(tile.x, tile.y);
                    if(zero[a] == 1) {
                        if(world.tile(target.x+x, target.y+y).block() != Blocks.plastaniumWall){
                            Call.onConstructFinish(world.tile(target.x+x, target.y+y), Blocks.plastaniumWall, 0, (byte) 0, Team.sharded, true);
                        }
                    } else if(zero[a] == 0){
                        if(world.tile(target.x+x, target.y+y).block() == Blocks.plastaniumWall){
                            Call.onDeconstructFinish(world.tile(target.x+x,target.y+y), Blocks.air, 0);
                        }
                    }
                }
                break;
            case 1:
                for(int a=0;a<15;a++){
                    String position = intpos[a];
                    String[] data = position.split(",");
                    int x = Integer.parseInt(data[0]);
                    int y = Integer.parseInt(data[1]);
                    Tile target = world.tile(tile.x, tile.y);
                    if(one[a] == 1){
                        if(world.tile(target.x+x, target.y+y).block() != Blocks.plastaniumWall){
                            Call.onConstructFinish(world.tile(target.x+x, target.y+y), Blocks.plastaniumWall, 0, (byte) 0, Team.sharded, true);
                        }
                    } else if(one[a] == 0){
                        if(world.tile(target.x+x, target.y+y).block() == Blocks.plastaniumWall){
                            Call.onDeconstructFinish(world.tile(target.x+x,target.y+y), Blocks.air, 0);
                        }
                    }
                }
                break;
            case 2:
                for(int a=0;a<15;a++){
                    String position = intpos[a];
                    String[] data = position.split(",");
                    int x = Integer.parseInt(data[0]);
                    int y = Integer.parseInt(data[1]);
                    Tile target = world.tile(tile.x, tile.y);
                    if(two[a] == 1){
                        if(world.tile(target.x+x, target.y+y).block() != Blocks.plastaniumWall){
                            Call.onConstructFinish(world.tile(target.x+x, target.y+y), Blocks.plastaniumWall, 0, (byte) 0, Team.sharded, true);
                        }
                    } else if(two[a] == 0){
                        if(world.tile(target.x+x, target.y+y).block() == Blocks.plastaniumWall){
                            Call.onDeconstructFinish(world.tile(target.x+x,target.y+y), Blocks.air, 0);
                        }
                    }
                }
                break;
            case 3:
                for(int a=0;a<15;a++){
                    String position = intpos[a];
                    String[] data = position.split(",");
                    int x = Integer.parseInt(data[0]);
                    int y = Integer.parseInt(data[1]);
                    Tile target = world.tile(tile.x, tile.y);
                    if(three[a] == 1){
                        if(world.tile(target.x+x, target.y+y).block() != Blocks.plastaniumWall){
                            Call.onConstructFinish(world.tile(target.x+x, target.y+y), Blocks.plastaniumWall, 0, (byte) 0, Team.sharded, true);
                        }
                    } else if(three[a] == 0){
                        if(world.tile(target.x+x, target.y+y).block() == Blocks.plastaniumWall){
                            Call.onDeconstructFinish(world.tile(target.x+x,target.y+y), Blocks.air, 0);
                        }
                    }
                }
                break;
            case 4:
                for(int a=0;a<15;a++){
                    String position = intpos[a];
                    String[] data = position.split(",");
                    int x = Integer.parseInt(data[0]);
                    int y = Integer.parseInt(data[1]);
                    Tile target = world.tile(tile.x, tile.y);
                    if(four[a] == 1){
                        if(world.tile(target.x+x, target.y+y).block() != Blocks.plastaniumWall){
                            Call.onConstructFinish(world.tile(target.x+x, target.y+y), Blocks.plastaniumWall, 0, (byte) 0, Team.sharded, true);
                        }
                    } else if(four[a] == 0){
                        if(world.tile(target.x+x, target.y+y).block() == Blocks.plastaniumWall){
                            Call.onDeconstructFinish(world.tile(target.x+x,target.y+y), Blocks.air, 0);
                        }
                    }
                }
                break;
            case 5:
                for(int a=0;a<15;a++){
                    String position = intpos[a];
                    String[] data = position.split(",");
                    int x = Integer.parseInt(data[0]);
                    int y = Integer.parseInt(data[1]);
                    Tile target = world.tile(tile.x, tile.y);
                    if(five[a] == 1){
                        if(world.tile(target.x+x, target.y+y).block() != Blocks.plastaniumWall){
                            Call.onConstructFinish(world.tile(target.x+x, target.y+y), Blocks.plastaniumWall, 0, (byte) 0, Team.sharded, true);
                        }
                    } else if(five[a] == 0){
                        if(world.tile(target.x+x, target.y+y).block() == Blocks.plastaniumWall){
                            Call.onDeconstructFinish(world.tile(target.x+x,target.y+y), Blocks.air, 0);
                        }
                    }
                }
                break;
            case 6:
                for(int a=0;a<15;a++){
                    String position = intpos[a];
                    String[] data = position.split(",");
                    int x = Integer.parseInt(data[0]);
                    int y = Integer.parseInt(data[1]);
                    Tile target = world.tile(tile.x, tile.y);
                    if(six[a] == 1){
                        if(world.tile(target.x+x, target.y+y).block() != Blocks.plastaniumWall){
                            Call.onConstructFinish(world.tile(target.x+x, target.y+y), Blocks.plastaniumWall, 0, (byte) 0, Team.sharded, true);
                        }
                    } else if(six[a] == 0){
                        if(world.tile(target.x+x, target.y+y).block() == Blocks.plastaniumWall){
                            Call.onDeconstructFinish(world.tile(target.x+x,target.y+y), Blocks.air, 0);
                        }
                    }
                }
                break;
            case 7:
                for(int a=0;a<15;a++){
                    String position = intpos[a];
                    String[] data = position.split(",");
                    int x = Integer.parseInt(data[0]);
                    int y = Integer.parseInt(data[1]);
                    Tile target = world.tile(tile.x, tile.y);
                    if(seven[a] == 1){
                        if(world.tile(target.x+x, target.y+y).block() != Blocks.plastaniumWall){
                            Call.onConstructFinish(world.tile(target.x+x, target.y+y), Blocks.plastaniumWall, 0, (byte) 0, Team.sharded, true);
                        }
                    } else if(seven[a] == 0){
                        if(world.tile(target.x+x, target.y+y).block() == Blocks.plastaniumWall){
                            Call.onDeconstructFinish(world.tile(target.x+x,target.y+y), Blocks.air, 0);
                        }
                    }
                }
                break;
            case 8:
                for(int a=0;a<15;a++){
                    String position = intpos[a];
                    String[] data = position.split(",");
                    int x = Integer.parseInt(data[0]);
                    int y = Integer.parseInt(data[1]);
                    Tile target = world.tile(tile.x, tile.y);
                    if(eight[a] == 1){
                        if(world.tile(target.x+x, target.y+y).block() != Blocks.plastaniumWall){
                            Call.onConstructFinish(world.tile(target.x+x, target.y+y), Blocks.plastaniumWall, 0, (byte) 0, Team.sharded, true);
                        }
                    } else if(eight[a] == 0){
                        if(world.tile(target.x+x, target.y+y).block() == Blocks.plastaniumWall){
                            Call.onDeconstructFinish(world.tile(target.x+x,target.y+y), Blocks.air, 0);
                        }
                    }
                }
                break;
            case 9:
                for(int a=0;a<15;a++){
                    String position = intpos[a];
                    String[] data = position.split(",");
                    int x = Integer.parseInt(data[0]);
                    int y = Integer.parseInt(data[1]);
                    Tile target = world.tile(tile.x, tile.y);
                    if(nine[a] == 1){
                        if(world.tile(target.x+x, target.y+y).block() != Blocks.plastaniumWall){
                            Call.onConstructFinish(world.tile(target.x+x, target.y+y), Blocks.plastaniumWall, 0, (byte) 0, Team.sharded, true);
                        }
                    } else if(nine[a] == 0){
                        if(world.tile(target.x+x, target.y+y).block() == Blocks.plastaniumWall){
                            Call.onDeconstructFinish(world.tile(target.x+x,target.y+y), Blocks.air, 0);
                        }
                    }
                }
                break;
        }
    }

    // 각 언어별 motd
    public static String getmotd(Player player){
        PlayerData p = PlayerData(player.uuid);
        if(root.child("motd/motd_"+p.language+".txt").exists()){
            return root.child("motd/motd_"+p.language+".txt").readString();
        } else {
            return root.child("motd/motd_en.txt").readString();
        }
    }

    // No 글자 표시
    public static void setno(Tile tile, boolean duplicate){
        int[] n = {1,1,1,1,0,1,1,0,1,1,0,1,1,0,1};
        int[] o = {1,1,1,1,0,1,1,0,1,1,0,1,1,1,1};
        if(duplicate){
            for(int x=0;x<7;x++){
                for(int y=0;y<5;y++){
                    Call.onDeconstructFinish(world.tile(tile.x+x,tile.y+y), Blocks.air, 0);
                }
            }
        }

        Tile target = world.tile(tile.x, tile.y);
        for(int a=0;a<15;a++) {
            String position = intpos[a];
            String[] data = position.split(",");
            int x = Integer.parseInt(data[0]);
            int y = Integer.parseInt(data[1]);
            if(n[a]==1) {
                if (world.tile(target.x+x, target.y+y).block() != Blocks.scrapWall) {
                    Call.onConstructFinish(world.tile(target.x+x, target.y+y), Blocks.scrapWall, 0, (byte) 0, Team.sharded, true);
                }
            } else {
                if(world.tile(target.x+x, target.y+y).block().solid){
                    Call.onDeconstructFinish(world.tile(target.x+x,target.y+y), Blocks.air, 0);
                }
            }
        }

        target = world.tile(tile.x+4,tile.y);

        for(int a=0;a<15;a++) {
            String position = intpos[a];
            String[] data = position.split(",");
            int x = Integer.parseInt(data[0]);
            int y = Integer.parseInt(data[1]);
            if(o[a]==1) {
                if (world.tile(target.x+x, target.y+y).block() != Blocks.scrapWall) {
                    Call.onConstructFinish(world.tile(target.x+x, target.y+y), Blocks.scrapWall, 0, (byte) 0, Team.sharded, true);
                }
            } else {
                if(world.tile(target.x+x, target.y+y).block().solid){
                    Call.onDeconstructFinish(world.tile(target.x+x,target.y+y), Blocks.air, 0);
                }
            }
        }
    }

    // 모든 플레이어에게 메세지 표시
    public static void allsendMessage(String name){
        Thread t = new Thread(() -> {
            for (int i = 0; i < playerGroup.size(); i++) {
                Player other = playerGroup.all().get(i);
                other.sendMessage(bundle(other, name));
            }
        });
        t.start();
    }

    public static void allsendMessage(String name, Object... parameter){
        Thread t = new Thread(() -> {
            for (int i = 0; i < playerGroup.size(); i++) {
                Player other = playerGroup.all().get(i);
                if(other == null) return;
                other.sendMessage(bundle(PlayerData(other.uuid).locale, name, parameter));
            }
        });
        t.start();
    }

    // 본인의 코어가 있는지 없는지 확인
    public static boolean isNocore(Player player){
        return Vars.state.teams.get(player.getTeam()).cores.isEmpty();
    }

    // 플레이어 지역 위치 확인
    public static Locale geolocation(Object data) {
        String ip = data instanceof Player ? Vars.netServer.admins.getInfo(((Player)data).uuid).lastIP : (String) data;
        try {
            String json = Jsoup.connect("http://ipapi.co/"+ip+"/json").ignoreContentType(true).execute().body();
            JsonObject result = JsonValue.readJSON(json).asObject();

            if (result.get("reserved") != null) {
                return locale;
            } else {
                Locale loc = locale;
                String lc = result.get("country_code").asString().split(",")[0];
                if(lc.split("_").length == 2){
                    String[] array = lc.split("_");
                    loc = new Locale(array[0], array[1]);
                }
                try {
                    ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("bundle.bundle", loc, new UTF8Control());
                    RESOURCE_BUNDLE.getString("success");
                }catch (Exception e){
                    for(int a=0;a<result.get("country_code").asString().split(",").length;a++){
                        try{
                            lc = result.get("country_code").asString().split(",")[a];
                            if(lc.split("_").length == 2){
                                String[] array = lc.split("_");
                                loc = new Locale(array[0], array[1]);
                            }
                            ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("bundle.bundle", loc, new UTF8Control());
                            RESOURCE_BUNDLE.getString("success");
                            return loc;
                        }catch (Exception ignored){}
                    }
                }
            }
        } catch (Exception e) {
            printError(e);
            return locale;
        }

        return locale;
    }

    // 로그인 유무 확인 (DB)
    public static boolean isLogin(Player player){
        return PlayerData(player.uuid).isLogin;
    }

    // 비 로그인 유저 확인 (코어)
    public static boolean checklogin(Player player){
        if (Vars.state.teams.get(player.getTeam()).cores.isEmpty()) {
            player.sendMessage(bundle("not-login"));
            return false;
        } else {
            return true;
        }
    }

    // 권한 확인
    public static boolean checkperm(Player player, String command){
        if(isLogin(player) && !isNocore(player)){
            PlayerData p = PlayerData(player.uuid);
            int size = permission.get(p.permission).asObject().get("permission").asArray().size();
            for(int a=0;a<size;a++){
                String permlevel = permission.get(p.permission).asObject().get("permission").asArray().get(a).asString();
                if(permlevel.equals(command) || permlevel.equals("ALL")){
                    return true;
                }
            }
        }
        return false;
    }

    // 로그인 시간 확인
    public static boolean isLoginold(String date){
        try {
            // 플레이어 시간
            SimpleDateFormat format = new SimpleDateFormat("yy-MM-dd HH:mm.ss", Locale.ENGLISH);
            Calendar cal1 = Calendar.getInstance();
            Date d = format.parse(String.valueOf(date));
            cal1.setTime(d);
            // 로그인 만료시간 설정 (3시간)
            cal1.add(Calendar.HOUR, 3);

            // 서버 시간
            LocalDateTime now = LocalDateTime.now();
            Calendar cal2 = Calendar.getInstance();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm.ss", Locale.ENGLISH);
            Date d1 = format.parse(now.format(dateTimeFormatter));
            cal2.setTime(d1);

            return cal1.after(cal2);
        } catch (ParseException e) {
            printError(e);
            return true;
        }
    }

    // 스레드 이름 설정
    public static class threadname implements ThreadFactory {
        String name;
        int count = 0;
        public threadname(String name){
            this.name = name;
        }

        @Override
        public Thread newThread(@NotNull Runnable r) {
            return new Thread(r, name+"-" + ++count);
        }
    }

    // 패킷 암호화
    public static byte[] encrypt(String data, SecretKeySpec spec, Cipher cipher) throws Exception {
        cipher.init(Cipher.ENCRYPT_MODE, spec);
        return cipher.doFinal(data.getBytes());
    }

    // 패킷 복호화
    public static byte[] decrypt(byte[] data, SecretKeySpec spec, Cipher cipher) throws Exception {
        cipher.init(Cipher.DECRYPT_MODE, spec);
        return cipher.doFinal(data);
    }

    public static boolean isduplicate(Player player){
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT uuid FROM players WHERE uuid = ?");
            stmt.setString(1, player.uuid);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }catch (SQLException e){
            printError(e);
            return true;
        }
    }

    public static boolean isduplicate(String uuid){
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM players WHERE uuid = ?");
            stmt.setString(1, uuid);
            ResultSet rs = stmt.executeQuery();
            if(rs.next()){
                nlog(LogType.debug, rs.getString("name"));
                return true;
            } else {
                nlog(LogType.debug, "not duplicate this uuid");
                return false;
            }
        }catch (SQLException e){
            printError(e);
            return true;
        }
    }

    public static boolean isduplicateid(String id){
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM players WHERE accountid = ?");
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }catch (SQLException e){
            printError(e);
            return true;
        }
    }

    public static boolean isduplicatename(String name){
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM players WHERE name = ?");
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }catch (SQLException e){
            printError(e);
            return true;
        }
    }

    public static void printProgress(long startTime, int total, int remain) {
        long eta = remain == 0 ? 0 :
                (total - remain) * (System.currentTimeMillis() - startTime) / remain;

        String etaHms = total == 0 ? "N/A" :
                String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(eta),
                        TimeUnit.MILLISECONDS.toMinutes(eta) % TimeUnit.HOURS.toMinutes(1),
                        TimeUnit.MILLISECONDS.toSeconds(eta) % TimeUnit.MINUTES.toSeconds(1));

        if (remain > total) {
            throw new IllegalArgumentException();
        }
        int maxBareSize = 20;
        int remainProcent = ((20 * remain) / total);
        char defaultChar = '-';
        String icon = "*";
        String bare = new String(new char[maxBareSize]).replace('\0', defaultChar) + "]";
        StringBuilder bareDone = new StringBuilder();
        bareDone.append("[");
        for (int i = 0; i < remainProcent; i++) {
            bareDone.append(icon);
        }
        String bareRemain = bare.substring(remainProcent);
        System.out.print("\r" + bareDone + bareRemain + " " + remainProcent * 5 + "%, ETA: "+etaHms);
        if (remain == total) {
            System.out.print("\n");
        }
    }

    public static String getip(){
        try{
            URL whatismyip = new URL("http://checkip.amazonaws.com");
            BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
            return in.readLine();
        }catch (Exception e){
            return "127.0.0.1";
        }
    }

    public static void URLDownload(URL URL, File savepath, String start_message, String finish_message, String error_message){
        try{
            BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(savepath));
            URLConnection urlConnection = URL.openConnection();
            InputStream is = urlConnection.getInputStream();
            int size = urlConnection.getContentLength();
            byte[] buf = new byte[512];
            int byteRead;
            int byteWritten = 0;
            long startTime = System.currentTimeMillis();
            if(start_message != null) System.out.println(start_message);
            while ((byteRead = is.read(buf)) != -1) {
                outputStream.write(buf, 0, byteRead);
                byteWritten += byteRead;

                printProgress(startTime, size, byteWritten);
            }
            if(finish_message != null) System.out.println("\n"+finish_message);
            is.close();
            outputStream.close();
        }catch (Exception e){
            if(error_message != null) System.out.println("\n"+error_message);
            printError(e);
        }
    }

    public static Object[] removeElement(int index, Object[] array) {
        Object[] newArray = new Object[array.length - 1];
        for (int i = 0; i < array.length; i++) {
            if (index > i) {
                newArray[i] = array[i];
            } else if(index < i) {
                newArray[i - 1] = array[i];
            }
        }
        return newArray;
    }

    public static Locale TextToLocale(String data){
        Locale locale = new Locale(data);
        String lc = data;
        if(data.contains(",")) lc = lc.split(",")[0];
        if (lc.split("_").length > 1) {
            String[] array = lc.split("_");
            locale = new Locale(array[0], array[1]);
        }
        return locale;
    }
}
