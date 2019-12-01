package essentials;

import essentials.utils.Bundle;
import essentials.utils.Config;
import io.anuke.arc.Core;
import io.anuke.arc.util.Log;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.world.Tile;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ThreadFactory;

import static essentials.core.PlayerDB.getData;
import static io.anuke.mindustry.Vars.playerGroup;
import static io.anuke.mindustry.Vars.world;

public class Global {
    public static Config config = new Config();

    // 일반 기록
    public static void log(String msg){
        Log.info("[Essential] "+msg);
    }

    public static void log(float msg){
        Log.info("[Essential] "+msg);
    }

    public static void log(int msg){
        Log.info("[Essential] "+msg);
    }

    public static void log(boolean msg){
        Log.info("[Essential] "+msg);
    }

    // 경고
    public static void logw(String msg){
        Log.warn("[Essential] "+msg);
    }

    // 오류
    public static void loge(String msg){
        Log.err("[Essential] "+msg);
    }

    // 서버
    public static void logs(String msg){
        Log.info("[EssentialServer] "+msg);
    }

    // 클라이언트
    public static void logc(String msg){
        Log.info("[EssentialClient] "+msg);
    }

    // 그냥 출력
    public static void logn(String msg) { Log.info(msg); }

    // 설정
    public static void logco(String msg){
        Log.info("[EssentialConfig] "+msg);
    }

    // PlayerDB
    public static void logp(String msg){
        Log.info("[EssentialPlayer] "+msg);
    }

    // PlayerDB 경고
    public static void logpw(String msg){
        Log.warn("[EssentialPlayer] "+msg);
    }

    // PlayerDB 오류
    public static void logpe(String msg){
        Log.err("[EssentialPlayer] "+msg);
    }

    // 코어가 없는 팀 찾기
    public static Team getTeamNoCore(Player player){
        int index = player.getTeam().ordinal()+1;
        while (index != player.getTeam().ordinal()){
            if (index >= Team.all.length){
                index = 0;
            }
            if (Vars.state.teams.get(Team.all[index]).cores.isEmpty()){
                return Team.all[index]; //return a team without a core
            }
            index++;
        }
        return player.getTeam();
    }

    // 오류 메세지를 파일로 복사하거나 즉시 출력
    public static void printStackTrace(Throwable e) {
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

                essentials.core.Log log = new essentials.core.Log();
                log.writelog("error", text);
                Global.loge("Internal error! - "+e.getMessage());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            e.printStackTrace();
        }
    }

    // 현재 시간출력
    public static String getTime(){
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm.ss", Locale.ENGLISH);
        return "[" + now.format(dateTimeFormatter) + "] ";
    }

    public static String getnTime(){
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm.ss", Locale.ENGLISH);
        return now.format(dateTimeFormatter);
    }

    // Bundle 파일에서 Essentials 문구를 포함시켜 출력
    public static String bundle(Player player, String value, Object... parameter) {
        if(isLogin(player)){
            JSONObject db = getData(player.uuid);
            Locale locale = new Locale(db.getString("language"));
            Bundle bundle = new Bundle(locale);
            return bundle.getBundle(value, parameter);
        } else {
            return "";
        }
    }

    public static String bundle(Player player, String value) {
        if(isLogin(player)){
            JSONObject db = getData(player.uuid);
            Locale locale = new Locale(db.getString("language"));
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
    public static String nbundle(Player player, String value, Object... paramter) {
        JSONObject db = getData(player.uuid);
        if(isLogin(player)){
            Locale locale = new Locale(db.getString("language"));
            Bundle bundle = new Bundle(locale);
            return bundle.getNormal(value, paramter);
        } else {
            return "";
        }
    }

    public static String nbundle(Player player, String value) {
        if(isLogin(player)){
            JSONObject db = getData(player.uuid);
            Locale locale = new Locale(db.getString("language"));
            Bundle bundle = new Bundle(locale);
            return bundle.getNormal(value);
        } else {
            return "";
        }
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
        JSONObject db = getData(player.uuid);
        Locale locale = new Locale(db.getString("language"));
        if(Core.settings.getDataDirectory().child("mods/Essentials/motd/motd_"+db.getString("language")+".txt").exists()){
            return Core.settings.getDataDirectory().child("mods/Essentials/motd/motd_"+db.getString("language")+".txt").readString();
        } else {
            return Core.settings.getDataDirectory().child("mods/Essentials/motd/motd_en.txt").readString();
        }
    }

    // No 글자 표시
    public static void setno(Tile tile){
        String[] pos = {"0,4","1,4","2,4","0,3","1,3","2,3","0,2","1,2","2,2","0,1","1,1","2,1","0,0","1,0","2,0"};
        int[] n = {1,1,1,1,0,1,1,0,1,1,0,1,1,0,1};
        int[] o = {1,1,1,1,0,1,1,0,1,1,0,1,1,1,1};

        for(int a=0;a<15;a++) {
            String position = pos[a];
            String[] data = position.split(",");
            int x = Integer.parseInt(data[0]);
            int y = Integer.parseInt(data[1]);
            Tile target = world.tile(tile.x, tile.y);
            if(n[a] == 1) {
                if (world.tile(target.x + x, target.y + y).block() != Blocks.titaniumWall) {
                    Call.onConstructFinish(world.tile(target.x + x, target.y + y), Blocks.scrapWall, 0, (byte) 0, Team.sharded, true);
                }
            } else if(n[a] == 0){
                if(world.tile(target.x+x, target.y+y).block().solid){
                    Call.onDeconstructFinish(world.tile(target.x+x,target.y+y), Blocks.air, 0);
                }
            }
        }

        for(int a=0;a<15;a++) {
            String position = pos[a];
            String[] data = position.split(",");
            int x = Integer.parseInt(data[0]);
            int y = Integer.parseInt(data[1]);
            Tile target = world.tile(tile.x, tile.y);
            if(o[a] == 1) {
                if (world.tile(target.x + x, target.y + y).block() != Blocks.titaniumWall) {
                    Call.onConstructFinish(world.tile(target.x+4+x, target.y+y), Blocks.scrapWall, 0, (byte) 0, Team.sharded, true);
                }
            } else if(o[a] == 0){
                if(world.tile(target.x+x, target.y+y).block().solid){
                    Call.onDeconstructFinish(world.tile(target.x+4+x,target.y+y), Blocks.air, 0);
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
    public static JSONObject geolocation(Player player) {
        String ip = Vars.netServer.admins.getInfo(player.uuid).lastIP;
        JSONObject list = new JSONObject();

        try {
            String apiURL = "http://ipapi.co/" + ip + "/json";
            URL url = new URL(apiURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setReadTimeout(5000);
            con.setRequestMethod("POST");

            boolean redirect = false;

            int status = con.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER)
                    redirect = true;
            }

            if (redirect) {
                String newUrl = con.getHeaderField("Location");
                String cookies = con.getHeaderField("Set-Cookie");

                con = (HttpURLConnection) new URL(newUrl).openConnection();
                con.setRequestProperty("Cookie", cookies);
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
            br.close();
            JSONTokener parser = new JSONTokener(response.toString());
            JSONObject result = new JSONObject(parser);

            if (result.has("reserved")) {
                list.put("geo", "Local IP");
                list.put("geocode", "LC");
                list.put("lang", "en");
            } else {
                list.put("geo", result.getString("country_name"));
                list.put("geocode", result.getString("country"));
                list.put("lang", result.getString("languages").substring(0, 1));
            }
        } catch (IOException e) {
            printStackTrace(e);
            list.put("geo", "invalid");
            list.put("geocode", "invalid");
            list.put("lang", "en");
        }

        return list;
    }

    // 로그인 유무 확인
    public static boolean isLogin(Player player){
        JSONObject db = getData(player.uuid);
        if(db.toString().equals("{}") || player.uuid == null) return false;
        return db.getBoolean("connected");
    }

    // 비 로그인 유저 확인
    public static boolean checklogin(Player player){
        if (Vars.state.teams.get(player.getTeam()).cores.isEmpty()) {
            player.sendMessage(bundle("not-login"));
            return true;
        } else {
            return false;
        }
    }

    // 권한 확인
    public static boolean checkperm(Player player){
        if(isLogin(player)){
            JSONObject db = getData(player.uuid);
            String perm = db.getString("permission");

        }
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
            printStackTrace(e);
            return true;
        }
    }

    // Thread name
    public static class threadname implements ThreadFactory {
        String name;
        int count = 0;
        public threadname(String name){
            this.name = name;
        }

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, name+"-" + ++count);
        }
    }
}
