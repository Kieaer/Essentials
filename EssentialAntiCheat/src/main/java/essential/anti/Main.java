package essential.anti;

import arc.files.Fi;
import arc.util.Http;
import arc.util.Log;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import essential.core.Bundle;
import essential.core.PluginData;
import kotlin.Pair;
import mindustry.mod.Plugin;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Objects;

import static essential.core.Main.root;

public class Main extends Plugin {
    static final String CONFIG_PATH = "config/config_anti.yaml";
    static final Bundle bundle = new Bundle();
    static Config conf;

    @Override
    public void init() {
        Thread.currentThread().setName("EssentialAntiCheat");
        bundle.setPrefix("[EssentialAntiCheat]");

        Log.debug(bundle.get("event.plugin.starting"));

        // 플러그인 설정
        if (!root.child(CONFIG_PATH).exists()) {
            root.child(CONFIG_PATH).write(this.getClass().getResourceAsStream("/config_anti.yaml"), false);
        }

        // 설정 파일 읽기
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            conf = mapper.readValue(root.child("config/config_protect.yaml").file(), Config.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // VPN 확인
        if (conf.isVpn()) {
            PluginData pluginData = PluginData.INSTANCE;
            boolean isUpdate = false;
            if (pluginData.get("vpnListDate") == null || Long.parseLong(Objects.requireNonNull(pluginData.get("vpnListDate"))) + 8.64e7 < System.currentTimeMillis()) {
                pluginData.getStatus().add(new Pair<>("vpnListDate", String.valueOf(System.currentTimeMillis())));
                isUpdate = true;
            }

            if (isUpdate) {
                Http.get("https://raw.githubusercontent.com/X4BNet/lists_vpn/main/output/datacenter/ipv4.txt")
                        .error(e -> Log.err("Failed to get vpn list!"))
                        .block(e -> {
                            String text = new String(new BufferedInputStream(e.getResultAsStream()).readAllBytes());
                            root.child("data/ipv4.txt").writeString(text);
                        });
            }

            root.child("data/ipv4.txt").readString().lines().forEach(e -> pluginData.getVpnList().add(e));
        }



        Log.info(bundle.get("event.plugin.loaded"));
    }
}
