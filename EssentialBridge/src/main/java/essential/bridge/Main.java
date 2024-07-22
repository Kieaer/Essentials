package essential.bridge;

import arc.ApplicationListener;
import arc.Core;
import arc.util.CommandHandler;
import arc.util.Log;
import essential.core.Bundle;
import mindustry.mod.Plugin;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

        // 서버간 연결할 포트 생성
        try (ServerSocket serverSocket = new ServerSocket(57293)) {
            isServerMode = true;
            daemon.submit(server);
        } catch (IOException e) {
            isServerMode = false;
            daemon.submit(client);
        }

        // 플러그인 설정
        conf = essential.core.Main.Companion.createAndReadConfig(
                "config_bridge.yaml",
                Objects.requireNonNull(this.getClass().getResourceAsStream("/config_bridge.yaml")),
                Config.class
        );

        if (conf.count) {
            Core.settings.put("totalPlayers", 0);
            Core.settings.saveValues();
        }

        Core.app.addListener(new ApplicationListener() {
            @Override
            public void dispose() {
                if (isServerMode) {
                    for (Socket socket : server.clients) {
                        try {
                            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                            writer.write("exit");
                            writer.newLine();
                            writer.flush();
                            socket.close();
                        } catch (IOException e) {
                            try {
                                socket.close();
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    }
                    server.shutdown();
                } else {
                    try {
                        client.send("exit");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });

        // test
        new DB().load();

        Log.info(bundle.get("event.plugin.loaded"));
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {

    }

    @Override
    public void registerClientCommands(CommandHandler handler) {

    }
}
