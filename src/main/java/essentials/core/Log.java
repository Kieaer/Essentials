package essentials.core;

import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.files.FileHandle;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.game.EventType.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static essentials.Global.config;
import static essentials.Global.gettime;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class Log{
    public void main() {
        // No error, griefer, non-block, withdraw event
        Events.on(PlayerChatEvent.class, e -> {
            writelog("chat", gettime() + e.player.name + ": " + e.message);
        });

        /*Events.on(WorldLoadEvent.class, e -> {
            Path total = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/Logs/Total.log")));
            try {
                String text = gettime() + "World loaded!\n";
                byte[] result = text.getBytes();
                Files.write(total, result, StandardOpenOption.APPEND);
            } catch (IOException error) {
                printStackTrace(error);
            }
        });*/

        /*Events.on(TapEvent.class, e-> {
            if (e.tile.entity != null && e.tile.entity.block != null && e.tile.entity() != null && e.player != null && e.player.name != null) {
                if (config.getLanguage().equals("ko")) {
                    writelog("tap", gettime() + e.player.name + " 플레이어가 " + e.tile.entity.block.name + " 블럭을 건드렸습니다.");
                } else {
                    writelog("tap", gettime() + "Player "+e.player.name + " has touched " + e.tile.entity.block.name + ".");
                }
            }
        });

        Events.on(TapConfigEvent.class, e-> {
            if (e.tile.entity != null && e.tile.entity.block != null && e.tile.entity() != null && e.player != null && e.player.name != null) {
                if (config.getLanguage().equals("ko")) {
                    writelog("tap", gettime() + e.player.name + " 플레이어가 " + e.tile.entity.block.name + " 블럭의 설정을 변경했습니다.");
                } else {
                    writelog("tap", gettime() + "Player "+e.player.name + " edited " + e.tile.entity.block.name + "'s block setting.");
                }
            }
        });*/

        Events.on(BlockBuildEndEvent.class, e -> {
            if(!e.breaking && e.tile.entity() != null && e.player != null && e.tile.entity.block != null && e.player.name != null) {
                if (config.getLanguage().equals("ko")) {
                    writelog("block", gettime() + e.player.name + " 플레이어가 " + e.tile.entity.block.name + " 블럭을 만들었습니다.");
                } else {
                    writelog("block", gettime() + "Player "+e.player.name + " has created block " + e.tile.entity.block.name + ".");
                }
            }
        });

        Events.on(BuildSelectEvent.class, e -> {
            if(e.breaking && e.builder instanceof Player && e.builder.buildRequest() != null && !e.builder.buildRequest().block.name.matches(".*build.*")) {
                if (config.getLanguage().equals("ko")) {
                    writelog("block", gettime() + ((Player) e.builder).name + " 플레이어가 " + e.builder.buildRequest().block.name + " 블럭을 파괴했습니다.");
                } else {
                    writelog("block", gettime() + "Player "+((Player) e.builder).name + " has destroyed block " + e.builder.buildRequest().block.name + ".");
                }
            }
        });

        Events.on(MechChangeEvent.class, e -> {
            if(config.getLanguage().equals("ko")) {
                writelog("player", gettime() + e.player.name + " 플레이어가 기체를 " + e.mech.name + " 으로 변경했습니다.");
            } else {
                writelog("player", gettime() + "Player "+e.player.name + " has change mech to " + e.mech.name + ".");
            }
        });

        Events.on(PlayerJoin.class, e -> {
            String ip = Vars.netServer.admins.getInfo(e.player.uuid).lastIP;

            if(config.getLanguage().equals("ko")) {
                writelog("player", gettime() + e.player.name + "/" + e.player.uuid + "/" + ip + " 플레이어가 서버에 입장했습니다.");
            } else {
                writelog("player", gettime() + "Player "+e.player.name + "/" + e.player.uuid + "/" + ip + " joined.");
            }
        });

        Events.on(PlayerConnect.class, e -> {
            String ip = Vars.netServer.admins.getInfo(e.player.uuid).lastIP;

            if(config.getLanguage().equals("ko")) {
                writelog("player", gettime() + e.player.name + "/" + e.player.uuid + "/" + ip + " 플레이어가 서버에 연결했습니다.");
            } else {
                writelog("player", gettime() + "Player "+e.player.name + "/" + e.player.uuid + "/" + ip + " connected.");
            }
        });

        Events.on(PlayerLeave.class, e -> {
            String ip = Vars.netServer.admins.getInfo(e.player.uuid).lastIP;

            if(config.getLanguage().equals("ko")) {
                writelog("player", gettime() + e.player.name + "/" + e.player.uuid + "/" + ip + " 플레이어가 서버에서 나갔습니다.");
            } else {
                writelog("player", gettime() + "Player "+ e.player.name + "/" + e.player.uuid + "/" + ip + " disconnected.");
            }
        });

        Events.on(DepositEvent.class, e -> {
            if(config.getLanguage().equals("ko")) {
                writelog("deposit", gettime() + e.player.name + " 플레이어가 " + e.player.item().item.name + " 아이템을 " + e.tile.block().name + " 으로 직접 옮겼습니다.");
            } else {
                writelog("deposit", gettime() + "Player "+ e.player.name + " has moved item " + e.player.item().item.name + " to " + e.tile.block().name + ".");
            }
        });
    }

    public void writelog(String type, String text){
        String date = DateTimeFormatter.ofPattern("yyyy-MM-dd HH_mm_ss").format(LocalDateTime.now());
        Path newlog = Paths.get(Core.settings.getDataDirectory().child("mods/Essentials/log/"+type+".log").path());
        Path oldlog = Paths.get(Core.settings.getDataDirectory().child("mods/Essentials/log/old/"+type+"/"+date+".log").path());
        FileHandle mainlog = Core.settings.getDataDirectory().child("mods/Essentials/log/"+type+".log");
        FileHandle logfolder = Core.settings.getDataDirectory().child("mods/Essentials/log");

        if(mainlog != null && mainlog.length() > 1024 * 512){
            if(config.getLanguage().equals("ko")){
                mainlog.writeString("== 이 파일의 끝입니다. 날짜: " + date, true);
            } else {
                mainlog.writeString("== End of log file. Date: " + date, true);
            }

            try {
                Files.move(newlog, oldlog, REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mainlog = null;
        }

        if(mainlog == null){
            mainlog = logfolder.child(type+".log");
        }

        mainlog.writeString(text + "\n", true);
    }
}
