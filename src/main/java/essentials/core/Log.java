package essentials.core;

import arc.Events;
import arc.files.Fi;
import mindustry.Vars;
import mindustry.entities.type.Player;
import mindustry.game.EventType.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static essentials.Global.*;
import static essentials.Main.root;
import static essentials.utils.Config.singleService;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class Log{
    public Log() {
        Events.on(PlayerChatEvent.class, e -> writelog("chat", e.player.name + ": " + e.message));

        Events.on(TapEvent.class, e-> {
            if (e.tile.entity != null && e.tile.entity.block != null && e.player != null && e.player.name != null) {
                writelog("tap", nbundle("log-tap", e.player.name, e.tile.entity.block.name));
            }
        });

        Events.on(TapConfigEvent.class, e-> {
            if (e.tile.entity != null && e.tile.entity.block != null && e.player != null && e.player.name != null) {
                writelog("tap", nbundle("log-tap-config", e.player.name, e.tile.entity.block.name));
            }
        });

        Events.on(WithdrawEvent.class, e-> {
            if (e.tile.entity != null && e.tile.entity.block != null && e.player != null && e.player.name != null) {
                writelog("withdraw", nbundle("log-withdraw", e.player.name, e.tile.entity.block.name, e.amount, e.tile.block().name));
            }
        });

        Events.on(BlockBuildEndEvent.class, e -> {
            if(!e.breaking && e.player != null && e.tile.entity.block != null && e.player.name != null) {
                writelog("block", nbundle("log-block-place", e.player.name, e.tile.entity.block.name));
            }
        });

        Events.on(BuildSelectEvent.class, e -> {
            if(e.breaking && e.builder instanceof Player && e.builder.buildRequest() != null && !e.builder.buildRequest().block.name.matches(".*build.*")) {
                writelog("block", nbundle("log-block-remove", ((Player) e.builder).name, e.builder.buildRequest().block.name));
            }
        });

        Events.on(MechChangeEvent.class, e -> writelog("player", nbundle("log-mech-change", e.player.name, e.mech.name)));
        Events.on(PlayerJoin.class, e -> writelog("player", nbundle("log-player-join", e.player.name, e.player.uuid, Vars.netServer.admins.getInfo(e.player.uuid).lastIP)));
        Events.on(PlayerConnect.class, e -> writelog("player", nbundle("log-player-connect", e.player.name, e.player.uuid, Vars.netServer.admins.getInfo(e.player.uuid).lastIP)));
        Events.on(PlayerLeave.class, e -> writelog("player", nbundle("log-player-leave", e.player.name, e.player.uuid, Vars.netServer.admins.getInfo(e.player.uuid).lastIP)));
        Events.on(DepositEvent.class, e -> writelog("deposit", nbundle("log-deposit", e.player.name, e.player.item().item.name, e.tile.block().name)));
    }

    public static void writelog(String type, String text){
        Thread t = new Thread(() -> {
            String date = DateTimeFormatter.ofPattern("yyyy-MM-dd HH_mm_ss").format(LocalDateTime.now());
            Path newlog = Paths.get(root.child("log/" + type + ".log").path());
            Path oldlog = Paths.get(root.child("log/old/" + type + "/" + date + ".log").path());
            Fi mainlog = root.child("log/" + type + ".log");
            Fi logfolder = root.child("log");

            if (mainlog != null && mainlog.length() > 1024 * 512) {
                mainlog.writeString(nbundle("log-file-end", (Object) date), true);
                try {
                    Files.move(newlog, oldlog, REPLACE_EXISTING);
                } catch (IOException e) {
                    printError(e);
                }
                mainlog = null;
            }

            if (mainlog == null) {
                mainlog = logfolder.child(type + ".log");
            }

            mainlog.writeString("["+getTime()+"]" + text + "\n", true);
        });
        singleService.submit(t);
    }
}
