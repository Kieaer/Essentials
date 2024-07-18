package essential.bridge;

import arc.ApplicationListener;
import arc.Core;
import arc.util.CommandHandler;
import arc.util.Log;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import essential.core.Bundle;
import mindustry.mod.Plugin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static essential.core.Main.root;

public class Main extends Plugin {
    static String CONFIG_PATH = "config/config_bridge.yaml";
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
        if (!root.child(CONFIG_PATH).exists()) {
            root.child(CONFIG_PATH).write(this.getClass().getResourceAsStream("/config_bridge.yaml"), false);
        }

        //todo 서버 포트별로 이름을 설정할 수 있고, 설정에서 서버 그룹별로 해당 설정을 덮어쓰기 할 수 있게 만들기
        // 설정 파일 읽기
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        try {
            conf = mapper.readValue(root.child(CONFIG_PATH).file(), Config.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (conf.isCountAllServers()) {
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

        Log.info(bundle.get("event.plugin.loaded"));
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {

    }

    @Override
    public void registerClientCommands(CommandHandler handler) {

    }
}
