package essentials;

import arc.Core;
import arc.util.Log;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import essentials.special.UTF8Control;
import essentials.utils.Bundle;
import essentials.utils.Config;
import essentials.utils.Permission;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.entities.type.Player;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.world.Tile;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static essentials.core.Log.writelog;
import static essentials.core.PlayerDB.PlayerData;
import static essentials.core.PlayerDB.conn;
import static mindustry.Vars.playerGroup;
import static mindustry.Vars.world;

public class Global {
    public static Config config = new Config(); // 설정 파일들
    public static String version;

    final static String tag = "[Essential] ";
    final static String servertag = "[EssentialServer] ";
    final static String clienttag = "[EssentialClient] ";
    final static String playertag = "[EssentialPlayer] ";
    final static String configtag = "[EssentialConfig] ";

    // 로그
    public static void log(String type, String value, Object... parameter){
        switch(type){
            case "log":
                if(parameter.length == 0){
                    Log.info(tag+nbundle(value));
                } else {
                    Log.info(tag+nbundle(value,parameter));
                }
                break;
            case "warn":
                if(parameter.length == 0){
                    Log.warn(tag+nbundle(value));
                } else {
                    Log.warn(tag+nbundle(value,parameter));
                }
                break;
            case "err":
                if(parameter.length == 0){
                    Log.err(tag+nbundle(value));
                } else {
                    Log.err(tag+nbundle(value,parameter));
                }
                break;
            case "debug":
                if(parameter.length == 0){
                    Log.info("[DEBUG]"+tag+nbundle(value));
                } else {
                    Log.info("[DEBUG]"+tag+nbundle(value,parameter));
                }
                break;
            case "server":
                if(parameter.length == 0){
                    Log.info(servertag+nbundle(value));
                } else {
                    Log.info(servertag+nbundle(value,parameter));
                }
                break;
            case "serverwarn":
                if(parameter.length == 0){
                    Log.warn(servertag+nbundle(value));
                } else {
                    Log.warn(servertag+nbundle(value,parameter));
                }
                break;
            case "servererr":
                if(parameter.length == 0){
                    Log.err(servertag+nbundle(value));
                } else {
                    Log.err(servertag+nbundle(value,parameter));
                }
                break;
            case "client":
                if(parameter.length == 0){
                    Log.info(clienttag+nbundle(value));
                } else {
                    Log.info(clienttag+nbundle(value,parameter));
                }
                break;
            case "clientwarn":
                if(parameter.length == 0){
                    Log.warn(clienttag+nbundle(value));
                } else {
                    Log.warn(clienttag+nbundle(value,parameter));
                }
                break;
            case "clienterr":
                if(parameter.length == 0){
                    Log.err(clienttag+nbundle(value));
                } else {
                    Log.err(clienttag+nbundle(value,parameter));
                }
                break;
            case "config":
                if(parameter.length == 0){
                    Log.info(configtag+nbundle(value));
                } else {
                    Log.info(configtag+nbundle(value,parameter));
                }
                break;
            case "player":
                if(parameter.length == 0){
                    Log.info(playertag+nbundle(value));
                } else {
                    Log.info(playertag+nbundle(value,parameter));
                }
                break;
            case "playererror":
                if(parameter.length == 0){
                    Log.err(playertag+nbundle(value));
                } else {
                    Log.err(playertag+nbundle(value,parameter));
                }
                break;
            case "playerwarn":
                if(parameter.length == 0){
                    Log.warn(playertag+nbundle(value));
                } else {
                    Log.warn(playertag+nbundle(value,parameter));
                }
                break;
        }
    }

    public static void nlog(String type, String value){
        switch(type){
            case "log":
                Log.info(value);
                break;
            case "warn":
                Log.warn(value);
                break;
            case "err":
                Log.err(value);
                break;
            case "debug":
                if(config.isDebug()) Log.info("[DEBUG] "+value);
                break;
            case "player":
                Log.info(playertag+value);
                break;
            case "client":
                Log.info(clienttag+value);
                break;
        }
    }

    // 오류 메세지를 파일로 복사하거나 즉시 출력
    public static void printError(Throwable e) {
        if(!config.isDebug()){
            StringBuilder sb = new StringBuilder();
            try {
                sb.append(e.toString());
                sb.append("\n");
                StackTraceElement[] element = e.getStackTrace();
                for (StackTraceElement stackTraceElement : element) {
                    sb.append("\tat ");
                    sb.append(stackTraceElement.toString());
                    sb.append("\n");
                }
                sb.append("=================================================\n");
                String text = sb.toString();

                writelog("error", text);
                nlog("err","Internal error! - "+e.getMessage());
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
    public static String bundle(Player player, String value, Object... parameter) {
        PlayerData p = PlayerData(player.uuid);
        if(p.isLogin){
            Locale locale = new Locale(p.language);
            Bundle bundle = new Bundle(locale);
            return bundle.getBundle(value, parameter);
        } else {
            return "";
        }
    }

    public static String bundle(Player player, String value) {
        PlayerData p = PlayerData(player.uuid);
        if(p.isLogin){
            Locale locale = new Locale(p.language);
            Bundle bundle = new Bundle(locale);
            return bundle.getBundle(value);
        } else {
            return "";
        }
    }

    public static String bundle(String value, Object... paramter){
        Locale locale = new Locale(config.getLanguage());
        Bundle bundle = new Bundle(locale);
        return bundle.getBundle(value, paramter);
    }

    public static String bundle(String value){
        Locale locale = new Locale(config.getLanguage());
        Bundle bundle = new Bundle(locale);
        return bundle.getBundle(value);
    }

    // Bundle 파일에서 Essentials 문구 없이 출력
    public static String nbundle(Player player, String value, Object... parameter) {
        PlayerData p = PlayerData(player.uuid);
        if(p.isLogin){
            Locale locale = new Locale(p.language);
            Bundle bundle = new Bundle(locale);
            return bundle.getNormal(value, parameter);
        } else {
            return "";
        }
    }

    public static String nbundle(Player player, String value) {
        PlayerData p = PlayerData(player.uuid);
        if(p.isLogin){
            Locale locale = new Locale(p.language);
            Bundle bundle = new Bundle(locale);
            return bundle.getNormal(value);
        } else {
            return "";
        }
    }

    public static String nbundle(String language, String value) {
        Locale locale = new Locale(language);
        Bundle bundle = new Bundle(locale);
        return bundle.getNormal(value);
    }

    public static String nbundle(String value, Object... paramter){
        Locale locale = new Locale(config.getLanguage());
        Bundle bundle = new Bundle(locale);
        return bundle.getNormal(value, paramter);
    }

    public static String nbundle(String value){
        Locale locale = new Locale(config.getLanguage());
        Bundle bundle = new Bundle(locale);
        return bundle.getNormal(value);
    }

    // 숫자 카운트
    public static void setcount(Tile tile, int count){
        String[] pos = {"0,4","1,4","2,4","0,3","1,3","2,3","0,2","1,2","2,2","0,1","1,1","2,1","0,0","1,0","2,0"};
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
                    String position = pos[a];
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
                    String position = pos[a];
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
                    String position = pos[a];
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
                    String position = pos[a];
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
                    String position = pos[a];
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
                    String position = pos[a];
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
                    String position = pos[a];
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
                    String position = pos[a];
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
                    String position = pos[a];
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
                    String position = pos[a];
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
        if(Core.settings.getDataDirectory().child("mods/Essentials/motd/motd_"+p.language+".txt").exists()){
            return Core.settings.getDataDirectory().child("mods/Essentials/motd/motd_"+p.language+".txt").readString();
        } else {
            return Core.settings.getDataDirectory().child("mods/Essentials/motd/motd_en.txt").readString();
        }
    }

    // No 글자 표시
    public static void setno(Tile tile){
        // todo 원래 숫자와 겹치는 버그 수정
        String[] pos = {"0,4","1,4","2,4","0,3","1,3","2,3","0,2","1,2","2,2","0,1","1,1","2,1","0,0","1,0","2,0"};
        int[] n = {1,1,1,1,0,1,1,0,1,1,0,1,1,0,1};
        int[] o = {1,1,1,1,0,1,1,0,1,1,0,1,1,1,1};

        Tile target = world.tile(tile.x, tile.y);
        for(int a=0;a<15;a++) {
            String position = pos[a];
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
            String position = pos[a];
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
                other.sendMessage(bundle(other, name, parameter));
            }
        });
        t.start();
    }

    // 본인의 코어가 있는지 없는지 확인
    public static boolean isNocore(Player player){
        return Vars.state.teams.get(player.getTeam()).cores.isEmpty();
    }

    // 플레이어 지역 위치 확인
    public static HashMap<String, String> geolocation(Player player) {
        String ip = Vars.netServer.admins.getInfo(player.uuid).lastIP;
        HashMap <String, String> data = new HashMap<>();
        try {
            String json = Jsoup.connect("http://ipapi.co/"+ip+"/json").ignoreContentType(true).execute().body();
            JsonObject result = JsonParser.object().from(json);

            if (result.has("reserved")) {
                data.put("country", "Local IP");
                data.put("country_code", "LC");
                data.put("languages", "en");
            } else {
                Locale locale;
                while(true){
                    try {
                        locale = new Locale(result.getString("languages"));
                        ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("bundle.bundle", locale, new UTF8Control());
                        RESOURCE_BUNDLE.getString("success");
                        break;
                    }catch (Exception e){
                        String[] array = result.getString("languages").split(",");
                        for(int a=0;a<array.length;a++){
                            try{
                                locale = new Locale(result.getString("languages"));
                                ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("bundle.bundle", locale, new UTF8Control());
                                RESOURCE_BUNDLE.getString("success");
                                break;
                            }catch (Exception ignored){}
                        }
                    }
                }
                data.put("country", result.getString("country_name"));
                data.put("country_code", result.getString("country"));
                data.put("languages", locale.getLanguage());
            }
        } catch (Exception e) {
            printError(e);
            data.put("country", "invalid");
            data.put("country_code", "invalid");
            data.put("languages", "en");
        }

        return data;
    }

    public static String geolocation(String ip){
        try {
            String json = Jsoup.connect("http://ipapi.co/" + ip + "/json").ignoreContentType(true).execute().body();
            JsonObject result = JsonParser.object().from(json);
            return result.getString("languages") == null ? "en" : result.getString("languages");
        } catch (Exception e){
            printError(e);
            return "en";
        }
    }

    // 로그인 유무 확인 (DB)
    public static boolean isLogin(Player player){
        PlayerData target = PlayerData(player.uuid);
        if(!target.isLogin) return false;
        return target.connected;
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
            int size = Permission.permission.getObject(p.permission).getArray("permission").size();
            for(int a=0;a<size;a++){
                String permlevel = Permission.permission.getObject(p.permission).getArray("permission").getString(a);
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
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM players WHERE uuid = ?");
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
                System.out.println(rs.getString("id"));
                return true;
            } else {
                System.out.println("false");
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
            e.printStackTrace();
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
}
