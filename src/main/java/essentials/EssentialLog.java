package essentials;

import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.game.EventType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.ResultSet;
import java.sql.Statement;

import static essentials.EssentialPlayer.conn;
import static essentials.Global.gettime;
import static essentials.Global.printStackTrace;

public class EssentialLog implements Runnable{
    @Override
    public void run() {
        Thread.currentThread().setName("Logging thread");

        if (!Core.settings.getDataDirectory().child("mods/Essentials/Logs/Block.log").exists()) {
            Core.settings.getDataDirectory().child("mods/Essentials/Logs/Block.log").writeString("");
            Global.log("Block.log created.");
        }
        if (!Core.settings.getDataDirectory().child("mods/Essentials/Logs/Player.log").exists()) {
            Core.settings.getDataDirectory().child("mods/Essentials/Logs/Player.log").writeString("");
            Global.log("Player.log created.");
        }
        if (!Core.settings.getDataDirectory().child("mods/Essentials/Logs/Total.log").exists()) {
            Core.settings.getDataDirectory().child("mods/Essentials/Logs/Total.log").writeString("");
            Global.log("Total.log created.");
        }
        if (!Core.settings.getDataDirectory().child("mods/Essentials/Logs/Griefer.log").exists()) {
            Core.settings.getDataDirectory().child("mods/Essentials/Logs/Griefer.log").writeString("");
            Global.log("Griefer.log created.");
        }

        Events.on(EventType.PlayerChatEvent.class, e -> {
            Path path = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/Logs/Player.log")));
            Path total = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/Logs/Total.log")));
            try {
                String text = gettime() + e.player.name + ": " + e.message + "\n";
                byte[] result = text.getBytes();
                Files.write(path, result, StandardOpenOption.APPEND);
                Files.write(total, result, StandardOpenOption.APPEND);
            } catch (IOException error) {
                printStackTrace(error);
            }
        });

        Events.on(EventType.WorldLoadEvent.class, e -> {
            Path total = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/Logs/Total.log")));
            try {
                String text = gettime() + "World loaded!\n";
                byte[] result = text.getBytes();
                Files.write(total, result, StandardOpenOption.APPEND);
            } catch (IOException error) {
                printStackTrace(error);
            }
        });

        Events.on(EventType.BlockBuildEndEvent.class, e -> {
            if(!e.breaking && e.tile.entity() != null && e.player != null){
                if(e.tile.entity.block != null && e.player.name != null){
                    //Thread t = new Thread(() -> {
                        Path block = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/Logs/Block.log")));
                        Path total = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/Logs/Total.log")));
                        try {
                            String text = gettime()+e.player.name+" Player place " +e.tile.entity.block.name+".\n";
                            byte[] result = text.getBytes();
                            Files.write(block, result, StandardOpenOption.APPEND);
                            Files.write(total, result, StandardOpenOption.APPEND);
                        }catch (IOException error) {
                            printStackTrace(error);
                        }
                    //});
                    //t.start();
                }
            }
        });

        Events.on(EventType.BuildSelectEvent.class, e -> {
            try{
                if(e.breaking && e.builder != null && ((Player) e.builder).name != null && e.builder.buildRequest() != null && e.builder.buildRequest() != null && e.builder.buildRequest().block.name != null && !e.builder.buildRequest().block.name.matches(".*build.*")){
                    Thread t = new Thread(() -> {
                        Path block = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/Logs/Block.log")));
                        Path total = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/Logs/Total.log")));
                        try {
                            String text = gettime()+((Player)e.builder).name+" Player break " +e.builder.buildRequest().block.name+".\n";
                            byte[] result = text.getBytes();
                            Files.write(block, result, StandardOpenOption.APPEND);
                            Files.write(total, result, StandardOpenOption.APPEND);
                        }catch (IOException error) {
                            printStackTrace(error);
                        }
                    });
                    t.start();
                }
            }catch (Exception error){
                printStackTrace(error);
            }
        });

        Events.on(EventType.MechChangeEvent.class, e -> {
            Path path = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/Logs/Player.log")));
            Path total = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/Logs/Total.log")));
            try {
                String text = gettime() + e.player.name + " has change mech to " + e.mech.name + ".\n";
                byte[] result = text.getBytes();
                Files.write(path, result, StandardOpenOption.APPEND);
                Files.write(total, result, StandardOpenOption.APPEND);
            } catch (IOException error) {
                printStackTrace(error);
            }
        });

        Events.on(EventType.PlayerJoin.class, e -> {
            Path path = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/Logs/Player.log")));
            Path total = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/Logs/Total.log")));
            try {
                String ip = Vars.netServer.admins.getInfo(e.player.uuid).lastIP;
                String sql = "SELECT * FROM players WHERE name='" + e.player.uuid + "'";

                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    String text = gettime()+"\nPlayer Information\n" +
                            "========================================\n" +
                            "Name: " + rs.getString("name") + "\n" +
                            "UUID: " + rs.getString("uuid") + "\n" +
                            "IP: " + ip + "\n" +
                            "Country: " + rs.getString("country") + "\n" +
                            "Block place: " + rs.getInt("placecount") + "\n" +
                            "Block break: " + rs.getInt("breakcount") + "\n" +
                            "Kill units: " + rs.getInt("killcount") + "\n" +
                            "Death count: " + rs.getInt("deathcount") + "\n" +
                            "Join count: " + rs.getInt("joincount") + "\n" +
                            "Kick count: " + rs.getInt("kickcount") + "\n" +
                            "Level: " + rs.getInt("level") + "\n" +
                            "XP: " + rs.getString("reqtotalexp") + "\n" +
                            "First join: " + rs.getString("firstdate") + "\n" +
                            "Last join: " + rs.getString("lastdate") + "\n" +
                            "Playtime: " + rs.getString("playtime") + "\n" +
                            "Attack clear: " + rs.getInt("attackclear") + "\n" +
                            "PvP Win: " + rs.getInt("pvpwincount") + "\n" +
                            "PvP Lose: " + rs.getInt("pvplosecount") + "\n" +
                            "PvP Surrender: " + rs.getInt("pvpbreakout");
                    byte[] result = text.getBytes();
                    Files.write(path, result, StandardOpenOption.APPEND);
                    Files.write(total, result, StandardOpenOption.APPEND);
                }
            } catch (Exception ex) {
                printStackTrace(ex);
            }
        });

        Events.on(EventType.PlayerConnect.class, e -> {
            Path path = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/Logs/Player.log")));
            Path total = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/Logs/Total.log")));
            try {
                String ip = Vars.netServer.admins.getInfo(e.player.uuid).lastIP;
                String text = gettime() + e.player.name + "/" + e.player.uuid + "/" + ip + " Player connected.\n";
                byte[] result = text.getBytes();
                Files.write(path, result, StandardOpenOption.APPEND);
                Files.write(total, result, StandardOpenOption.APPEND);
            } catch (IOException error) {
                printStackTrace(error);
            }
        });

        Events.on(EventType.PlayerLeave.class, e -> {
            Path path = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/Logs/Player.log")));
            Path total = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/Logs/Total.log")));
            try {
                String ip = Vars.netServer.admins.getInfo(e.player.uuid).lastIP;
                String text = gettime() + e.player.name + "/" + e.player.uuid + "/" + ip + " Player disconnected.\n";
                byte[] result = text.getBytes();
                Files.write(path, result, StandardOpenOption.APPEND);
                Files.write(total, result, StandardOpenOption.APPEND);
            } catch (IOException error) {
                printStackTrace(error);
            }
        });

        /*Events.on(EventType.DepositEvent.class, e -> {
            Path path = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/Logs/Player.log")));
            Path total = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/Logs/Total.log")));
            try {
                String text = gettime() + e.player.name+" Player has moved item "+e.player.item().item.name+" to "+e.tile.block().name+".\n";
                byte[] result = text.getBytes();
                Files.write(path, result, StandardOpenOption.APPEND);
                Files.write(total, result, StandardOpenOption.APPEND);
            } catch (IOException error) {
                printStackTrace(error);
            }
        });*/
    }
}
