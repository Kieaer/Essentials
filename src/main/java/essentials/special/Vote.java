package essentials.special;

import essentials.EssentialBundle;
import essentials.EssentialPlayer;
import essentials.Global;
import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.game.EventType;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.Call;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import static essentials.EssentialPlayer.getData;
import static essentials.Global.bundle;
import static essentials.Global.printStackTrace;
import static io.anuke.mindustry.Vars.*;

public class Vote{
    private Player player;
    private Player target;
    public String mode;
    public static boolean isvoting;
    public static ArrayList<String> list = new ArrayList<>();
    public static int require = (int) Math.ceil(0.4 * playerGroup.size());

    public void main(Player player, String type, String target){
        this.player = player;
        if(target != null){
            Player other = Vars.playerGroup.find(p -> p.name.equalsIgnoreCase(target));
            if(other != null){
                this.target = other;
            }
        }
        if(playerGroup.size() < 3){
            bundle(player, "vote-min");
            return;
        }

        switch(type) {
            case "gameover":
                if(!isvoting){
                    isvoting = true;
                    list.add(player.uuid);
                    for (int i = 0; i < playerGroup.size(); i++) {
                        Player others = playerGroup.all().get(i);
                        bundle(others, "vote-gameover");
                    }
                    gameover.start();
                } else {
                    bundle(player, "vote-in-processing");
                }
                break;
            case "skipwave":
                if(!isvoting){
                    isvoting = true;
                    list.add(player.uuid);
                    for (int i = 0; i < playerGroup.size(); i++) {
                        Player others = playerGroup.all().get(i);
                        bundle(others, "vote-skipwave");
                    }
                    skipwave.start();
                } else {
                    bundle(player, "vote-in-processing");
                }
                break;
            case "kick":
                if(!isvoting){
                    isvoting = true;
                    list.add(player.uuid);
                    for (int i = 0; i < playerGroup.size(); i++) {
                        Player others = playerGroup.all().get(i);
                        bundle(others, "vote-kick");
                    }
                    kick.start();
                } else {
                    bundle(player, "vote-in-processing");
                }
                break;
            default:
                break;
        }
    }

    public static final Thread counting = new Thread(() -> {
        if (playerGroup != null && playerGroup.size() > 0) {
            Thread loop = new Thread(() -> {
                for (int i = 0; i < playerGroup.size(); i++) {
                    int finalI = i;
                    Thread work = new Thread(() -> {
                        Player others = playerGroup.all().get(finalI);
                        JSONObject db1 = getData(others.uuid);
                        if (db1.get("country_code") == "KR") {
                            try {
                                Thread.sleep(10000);
                                others.sendMessage(EssentialBundle.load(true, "vote-50sec"));
                                Thread.sleep(10000);
                                others.sendMessage(EssentialBundle.load(true, "vote-40sec"));
                                Thread.sleep(10000);
                                others.sendMessage(EssentialBundle.load(true, "vote-30sec"));
                                Thread.sleep(10000);
                                others.sendMessage(EssentialBundle.load(true, "vote-20sec"));
                                Thread.sleep(10000);
                                others.sendMessage(EssentialBundle.load(true, "vote-10sec"));
                                Thread.sleep(10000);
                            } catch (Exception e) {
                                printStackTrace(e);
                            }
                        } else {
                            try {
                                Thread.sleep(10000);
                                others.sendMessage(EssentialBundle.load(false, "vote-50sec"));
                                Thread.sleep(10000);
                                others.sendMessage(EssentialBundle.load(false, "vote-40sec"));
                                Thread.sleep(10000);
                                others.sendMessage(EssentialBundle.load(false, "vote-30sec"));
                                Thread.sleep(10000);
                                others.sendMessage(EssentialBundle.load(false, "vote-20sec"));
                                Thread.sleep(10000);
                                others.sendMessage(EssentialBundle.load(false, "vote-10sec"));
                                Thread.sleep(10000);
                            } catch (Exception e) {
                                printStackTrace(e);
                            }
                        }
                    });
                    work.start();
                }
            });
            loop.start();
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                loop.interrupt();
            }
        }
    });

    private final Thread gameover = new Thread(() -> {
        try {
            Call.sendMessage("[green][Essentials] Require [scarlet]" + require + "[green] players.");
            counting.start();
            counting.join();
        } catch (InterruptedException ignored) {
        } finally {
            if (list.size() >= require) {
                Call.sendMessage("[green][Essentials] Gameover vote passed!");
                Events.fire(new EventType.GameOverEvent(Team.sharded));
            } else {
                Call.sendMessage("[green][Essentials] [red]Gameover vote failed.");
            }
            list = new ArrayList<>();
            isvoting = false;
        }
    });

    private final Thread skipwave = new Thread(() -> {
        try {
            Call.sendMessage("[green][Essentials] Require [scarlet]" + require + "[green] players.");
            counting.start();
            counting.join();
        } catch (InterruptedException ignored) {
        } finally {
            if (list.size() >= require) {
                Call.sendMessage("[green][Essentials] Skip 10 wave vote passed!");
                for (int i = 0; i < 10; i++) {
                    logic.runWave();
                }
            } else {
                Call.sendMessage("[green][Essentials] [red]Skip 10 wave vote failed.");
            }
            list = new ArrayList<>();
            isvoting = false;
        }
    });

    private final Thread kick = new Thread(() -> {
        if(target != null){
            try {
                Call.sendMessage("[green][Essentials] Require [scarlet]" + require + "[green] players.");
                counting.start();
                counting.join();
            } catch (InterruptedException ignored) {
            } finally {
                if (list.size() >= require) {
                    Call.sendMessage("[green][Essentials] Player kick vote success!");
                    EssentialPlayer.addtimeban(target.name, target.uuid, 4);
                    Global.log(target.name + " / " + target.uuid + " Player has banned due to voting. " + list.size() + "/" + require);

                    Path path = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/Logs/Player.log")));
                    Path total = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/Logs/Total.log")));
                    try {
                        JSONObject other = getData(target.uuid);
                        String text = other.get("name") + " / " + target.uuid + " Player has banned due to voting. " + list.size() + "/" + require + "\n";
                        byte[] result = text.getBytes();
                        Files.write(path, result, StandardOpenOption.APPEND);
                        Files.write(total, result, StandardOpenOption.APPEND);
                    } catch (IOException error) {
                        printStackTrace(error);
                    }

                    netServer.admins.banPlayer(target.uuid);
                    Call.onKick(target.con, "You're banned.");
                } else {
                    for (int i = 0; i < playerGroup.size(); i++) {
                        Player others = playerGroup.all().get(i);
                        bundle(others, "vote-failed");
                    }
                }
                list = new ArrayList<>();
                isvoting = false;
            }
        } else {
            bundle(player, "player-not-found");
        }
    });
}