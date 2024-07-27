package essential.achievements;

import arc.util.CommandHandler;
import arc.util.Log;
import essential.core.Bundle;
import essential.core.DB;
import essential.core.Permission;
import essential.core.annotation.ClientCommand;
import essential.core.annotation.ServerCommand;
import mindustry.Vars;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.mod.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static essential.core.Main.database;

public class Main extends Plugin {
    static Bundle bundle = new Bundle();
    static Config conf;

    @Override
    public void init() {
        bundle.setPrefix("[EssentialAchievements]");
        Log.debug(bundle.get("event.plugin.starting"));

        /*conf = essential.core.Main.Companion.createAndReadConfig(
                "config_achievements.yaml",
                Objects.requireNonNull(this.getClass().getResourceAsStream("/config_achievements.yaml")),
                Config.class
        );
*/
        // 이벤트 실행
        Event event = new Event();
        Method[] methods = event.getClass().getDeclaredMethods();
        for (Method method : methods) {
            essential.core.annotation.Event annotation = method.getAnnotation(essential.core.annotation.Event.class);
            if (annotation != null) {
                try {
                    method.invoke(event);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        Log.info(bundle.get("event.plugin.loaded"));
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        Commands commands = new Commands();

        Method[] methods = commands.getClass().getDeclaredMethods();

        for (Method method : methods) {
            ServerCommand annotation = method.getAnnotation(ServerCommand.class);
            if (annotation != null) {
                handler.register(annotation.name(), annotation.parameter(), annotation.description(), args -> {
                    if (args.length > 0) {
                        try {
                            method.invoke(commands, new Object[]{args});
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            method.invoke(commands, new Object[]{new String[]{}});
                        } catch (Exception e) {
                            System.err.println("arg size - " + args.length);
                            System.err.println("command - " + annotation.name());
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        Commands commands = new Commands();
        Method[] methods = commands.getClass().getDeclaredMethods();

        for (Method method : methods) {
            ClientCommand annotation = method.getAnnotation(ClientCommand.class);
            if (annotation != null) {
                handler.<Player>register(annotation.name(), annotation.parameter(), (args, player) -> {
                    DB.PlayerData data = findPlayerByUuid(player.uuid());
                    if (data == null) {
                        data = new DB.PlayerData();
                    }

                    if (Permission.INSTANCE.check(data, annotation.name())) {
                        try {
                            if (args.length > 0) {
                                method.invoke(commands, player, data, args);
                            } else {
                                method.invoke(commands, player, data, new String[]{});
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        if ("js".equals(annotation.name())) {
                            Call.kick(player.con(), new Bundle(player.locale()).get("command.js.no.permission"));
                        } else {
                            player.sendMessage(Vars.netServer.invalidHandler.handle(
                                    player.self(),
                                    new CommandHandler.CommandResponse(
                                            CommandHandler.ResponseType.unknownCommand,
                                            null,
                                            annotation.name()
                                    )
                            ));
                        }
                    }
                });
            }
        }
    }

    DB.PlayerData findPlayerByUuid(String uuid) {
        return database.getPlayers().stream().filter( e -> e.getUuid().equals(uuid)).findFirst().orElse(null);
    }
}
