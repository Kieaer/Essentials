package essential.protect;

import arc.util.Log;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import essential.core.Bundle;
import essential.core.DB;
import mindustry.mod.Plugin;
import mindustry.net.Administration;

import java.io.IOException;
import java.util.Objects;

import static essential.core.Main.findPlayerByUuid;
import static essential.core.Main.root;
import static mindustry.Vars.netServer;

public class Main extends Plugin {
    static final String CONFIG_PATH = "config/config_protect.yaml";
    static final Bundle bundle = new Bundle();
    static Config conf;

    @Override
    public void init() {
        bundle.setPrefix("[EssentialProtect]");

        Log.debug(bundle.get("event.plugin.starting"));

        if (!root.child(CONFIG_PATH).exists()) {
            root.child(CONFIG_PATH).write(this.getClass().getResourceAsStream("/config_protect.yaml"), false);
        }

        // 설정 파일 읽기
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            conf = mapper.readValue(root.child("config/config_protect.yaml").file(), Config.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        netServer.admins.actionFilters.add(action -> {
            if (action.player == null) return true;

            DB.PlayerData data = findPlayerByUuid(action.player.uuid());
            if (data != null) {
                if (action.type == Administration.ActionType.commandUnits) {
                    data.setCurrentControlCount(data.getCurrentControlCount() + 1);
                }
                // 계정 기능이 켜져있는 경우
                if (conf.getAccount().isEnabled()) {
                    // Discord 인증을 사용할 경우
                    if (Objects.requireNonNull(conf.getAccount().getAuthType()) == Config.Account.AuthType.Discord) {
                        // 계정에 Discord 인증이 안되어 있는 경우
                        if (data.getDiscord() == null) {
                            action.player.sendMessage(new Bundle(action.player.locale).get("event.discord.not.registered"));
                            return false;
                        } else {
                            return true;
                        }
                    }
                }
                return true;
            } else {
                return false;
            }
        });

        Log.info(bundle.get("event.plugin.loaded"));
    }
}
