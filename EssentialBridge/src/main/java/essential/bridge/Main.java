package essential.bridge;

import arc.ApplicationListener;
import arc.Core;
import arc.util.CommandHandler;
import arc.util.Log;
import essential.core.Bundle;
import essential.core.Permission;
import essential.core.annotation.ClientCommand;
import essential.core.annotation.ServerCommand;
import mindustry.gen.Player;
import mindustry.mod.Plugin;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static essential.core.Main.database;

public class Main extends Plugin {
    static Bundle bundle = new Bundle();
    static Boolean isServerMode = false;
    ExecutorService daemon = Executors.newSingleThreadExecutor();
    static Config conf;
    Server server = new Server();
    Client client = new Client();

    @Override
    public void init() {
        bundle.setPrefix("[EssentialBridge]");

        Log.debug(bundle.get("event.plugin.starting"));

        // 플러그인 설정
        conf = essential.core.Main.Companion.createAndReadConfig(
                "config_bridge.yaml",
                Objects.requireNonNull(this.getClass().getResourceAsStream("/config_bridge.yaml")),
                Config.class
        );

        // 서버간 연결할 포트 생성
        try (ServerSocket serverSocket = new ServerSocket(conf.port)) {
            isServerMode = true;
            daemon.submit(server);
        } catch (IOException e) {
            isServerMode = false;
            daemon.submit(client);
        }

        Core.app.addListener(new ApplicationListener() {
            @Override
            public void dispose() {
                if (isServerMode) {
                    for (Socket socket : server.clients) {
                        try {
                            socket.close();
                        } catch (IOException ignored) {

                        }
                    }
                    server.shutdown();
                } else {
                    try {
                        client.send("exit");
                    } catch (IOException e) {
                        try {
                            client.socket.close();
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }
                daemon.shutdown();
            }
        });

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

        Log.debug(bundle.get("event.plugin.loaded"));
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
                handler.<Player>register(annotation.name(), annotation.parameter(), annotation.description(), (args, player) -> {
                    essential.core.DB.PlayerData data = findPlayerByUuid(player.uuid());
                    if (data == null) {
                        data = new essential.core.DB.PlayerData();
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
                        data.send("command.permission.false");
                    }
                });
            }
        }
    }

    essential.core.DB.PlayerData findPlayerByUuid(String uuid) {
        return database.getPlayers().stream().filter( e -> e.getUuid().equals(uuid)).findFirst().orElse(null);
    }
}
