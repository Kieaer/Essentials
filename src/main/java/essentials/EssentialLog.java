package essentials;

import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.game.EventType;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static essentials.EssentialPlayer.getData;

public class EssentialLog {
    public static void main(){
        if (!Core.settings.getDataDirectory().child("plugins/Essentials/Logs/Block.log").exists()) {
            Core.settings.getDataDirectory().child("plugins/Essentials/Logs/Block.log").writeString("");
            Global.log("Block.log created.");
        }
        if (!Core.settings.getDataDirectory().child("plugins/Essentials/Logs/Player.log").exists()) {
            Core.settings.getDataDirectory().child("plugins/Essentials/Logs/Player.log").writeString("");
            Global.log("Player.log created.");
        }
        if (!Core.settings.getDataDirectory().child("plugins/Essentials/Logs/Total.log").exists()) {
            Core.settings.getDataDirectory().child("plugins/Essentials/Logs/Total.log").writeString("");
            Global.log("Total.log created.");
        }
        if (!Core.settings.getDataDirectory().child("plugins/Essentials/Logs/Griefer.log").exists()) {
            Core.settings.getDataDirectory().child("plugins/Essentials/Logs/Griefer.log").writeString("");
            Global.log("Griefer.log created.");
        }

        Events.on(EventType.PlayerChatEvent.class, e -> {
            Thread t = new Thread(() -> {
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm.ss", Locale.ENGLISH);
                String nowString = "["+now.format(dateTimeFormatter)+"] ";

                Path path = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("plugins/Essentials/Logs/Player.log")));
                Path total = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("plugins/Essentials/Logs/Total.log")));
                try {
                    JSONObject db = getData(e.player.uuid);
                    String text = nowString+db.get("name")+": "+e.message+"\n";
                    byte[] result = text.getBytes();
                    Files.write(path, result, StandardOpenOption.APPEND);
                    Files.write(total, result, StandardOpenOption.APPEND);
                }catch (IOException error) {
                    error.printStackTrace();
                }
            });
            t.start();
        });

        Events.on(EventType.WorldLoadEvent.class, e -> {
            Thread t = new Thread(() -> {
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm.ss", Locale.ENGLISH);
                String nowString = "["+now.format(dateTimeFormatter)+"] ";
                Path total = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("plugins/Essentials/Logs/Total.log")));
                try {
                    String text = nowString+"World loaded!\n";
                    byte[] result = text.getBytes();
                    Files.write(total, result, StandardOpenOption.APPEND);
                }catch (IOException error) {
                    error.printStackTrace();
                }
            });
            t.start();
        });

        Events.on(EventType.BlockBuildEndEvent.class, e -> {
            Thread t = new Thread(() -> {
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm.ss", Locale.ENGLISH);
                String nowString = "["+now.format(dateTimeFormatter)+"] ";

                Path block = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("plugins/Essentials/Logs/Block.log")));
                Path total = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("plugins/Essentials/Logs/Total.log")));
                try {
                    String text;
                    if(!e.breaking && e.tile.entity() != null){
                        text = nowString+e.player.name+" Player break " +e.tile.entity().block.name+".\n";
                    } else if(e.tile.entity() != null){
                        text = nowString+e.player.name+" Player made " + e.tile.entity().block.name+".\n";
                    } else {
                        return;
                    }
                    byte[] result = text.getBytes();
                    Files.write(block, result, StandardOpenOption.APPEND);
                    Files.write(total, result, StandardOpenOption.APPEND);
                }catch (IOException error) {
                    error.printStackTrace();
                }
            });
            t.start();
        });

        Events.on(EventType.MechChangeEvent.class, e -> {
            Thread t = new Thread(() -> {
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm.ss", Locale.ENGLISH);
                String nowString = "["+now.format(dateTimeFormatter)+"] ";

                Path path = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("plugins/Essentials/Logs/Player.log")));
                Path total = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("plugins/Essentials/Logs/Total.log")));
                try {
                    String text = nowString+e.player.name+" has change mech to "+e.mech.name+".\n";
                    byte[] result = text.getBytes();
                    Files.write(path, result, StandardOpenOption.APPEND);
                    Files.write(total, result, StandardOpenOption.APPEND);
                }catch (IOException error) {
                    error.printStackTrace();
                }
            });
            t.start();
        });

        Events.on(EventType.PlayerJoin.class, e -> {
            Thread t = new Thread(() -> {
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm.ss", Locale.ENGLISH);
                String nowString = "["+now.format(dateTimeFormatter)+"] ";

                Path path = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("plugins/Essentials/Logs/Player.log")));
                Path total = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("plugins/Essentials/Logs/Total.log")));
                JSONObject db = getData(e.player.uuid);

                try {
                    String ip = Vars.netServer.admins.getInfo(e.player.uuid).lastIP;
                    String text = nowString+e.player.name+" player joined.\n" +
                            "========================================\n" +
                            "Name: "+e.player.name+"\n" +
                            "UUID: "+e.player.uuid+"\n" +
                            "Mobile: "+e.player.isMobile+"\n" +
                            "IP: "+ip+"\n" +
                            "Country: "+db.get("country")+"\n" +
                            "Block place: "+db.get("placecount")+"\n" +
                            "Block break: "+db.get("breakcount")+"\n" +
                            "Kill units: "+db.get("killcount")+"\n" +
                            "Death count: "+db.get("deathcount")+"\n" +
                            "Join count: "+db.get("joincount")+"\n" +
                            "Kick count: "+db.get("kickcount")+"\n" +
                            "Level: "+db.get("level")+"\n" +
                            "XP: "+db.get("reqtotalexp")+"\n" +
                            "First join: "+db.get("firstdate")+"\n" +
                            "Last join: "+db.get("lastdate")+"\n" +
                            "Playtime: "+db.get("playtime")+"\n" +
                            "Attack clear: "+db.get("attackclear")+"\n" +
                            "PvP Win: "+db.get("pvpwincount")+"\n" +
                            "PvP Lose: "+db.get("pvplosecount")+"\n" +
                            "PvP Surrender: "+db.get("pvpbreakout")+"\n" +
                            "========================================\n";
                    byte[] result = text.getBytes();
                    Files.write(path, result, StandardOpenOption.APPEND);
                    Files.write(total, result, StandardOpenOption.APPEND);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            t.start();
        });

        Events.on(EventType.PlayerConnect.class, e -> {
            Thread t = new Thread(() -> {
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm.ss", Locale.ENGLISH);
                String nowString = "["+now.format(dateTimeFormatter)+"] ";

                Path path = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("plugins/Essentials/Logs/Player.log")));
                Path total = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("plugins/Essentials/Logs/Total.log")));
                try {
                    String ip = Vars.netServer.admins.getInfo(e.player.uuid).lastIP;
                    String text = nowString+e.player.name+"/"+e.player.uuid+"/"+ip+" Player connected.\n";
                    byte[] result = text.getBytes();
                    Files.write(path, result, StandardOpenOption.APPEND);
                    Files.write(total, result, StandardOpenOption.APPEND);
                }catch (IOException error) {
                    error.printStackTrace();
                }
            });
            t.start();
        });

        Events.on(EventType.PlayerLeave.class, e -> {
            Thread t = new Thread(() -> {
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm.ss", Locale.ENGLISH);
                String nowString = "["+now.format(dateTimeFormatter)+"] ";

                Path path = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("plugins/Essentials/Logs/Player.log")));
                Path total = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("plugins/Essentials/Logs/Total.log")));
                try {
                    String ip = Vars.netServer.admins.getInfo(e.player.uuid).lastIP;
                    String text = nowString+e.player.name+"/"+e.player.uuid+"/"+ip+" Player disconnected.\n";
                    byte[] result = text.getBytes();
                    Files.write(path, result, StandardOpenOption.APPEND);
                    Files.write(total, result, StandardOpenOption.APPEND);
                }catch (IOException error) {
                    error.printStackTrace();
                }
            });
            t.start();
        });
    }
}
