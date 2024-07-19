package essential.protect;

import arc.util.Http;
import arc.util.Log;
import essential.core.Bundle;
import essential.core.DB;
import essential.core.Permission;
import kotlin.Pair;
import mindustry.mod.Plugin;
import mindustry.net.Administration;

import java.io.BufferedInputStream;
import java.util.Objects;

import static essential.core.Main.database;
import static essential.core.Main.root;
import static mindustry.Vars.netServer;

public class Main extends Plugin {
    static Bundle bundle = new Bundle();
    static Config conf;
    static PluginData pluginData = new PluginData();

    @Override
    public void init() {
        bundle.setPrefix("[EssentialProtect]");

        Log.debug(bundle.get("event.plugin.starting"));

        conf = essential.core.Main.Companion.createAndReadConfig(
                "config_protect.yaml",
                Objects.requireNonNull(this.getClass().getResourceAsStream("/config_protect.yaml")),
                Config.class
        );

        netServer.admins.actionFilters.add(action -> {
            if (action.player == null) return true;

            DB.PlayerData data = findPlayerByUuid(action.player.uuid());
            if (data != null) {
                if (action.type == Administration.ActionType.commandUnits) {
                    data.setCurrentControlCount(data.getCurrentControlCount() + 1);
                }
                // 계정 기능이 켜져있는 경우
                if (conf.account.enabled) {
                    // Discord 인증을 사용할 경우
                    if (Objects.requireNonNull(conf.account.getAuthType()) == Config.Account.AuthType.Discord) {
                        // 계정에 Discord 인증이 안되어 있는 경우
                        if (data.getDiscord() == null) {
                            action.player.sendMessage(new Bundle(action.player.locale).get("event.discord.not.registered"));
                            return false;
                        } else {
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
                return true;
            } else {
                return false;
            }
        });

        // 계정 설정 유무에 따라 기본 권한 변경
        if (conf.account.getAuthType() != Config.Account.AuthType.None) {
            Permission.INSTANCE.setDefault("user");
        } else {
            Permission.INSTANCE.setDefault("visitor");
        }

        // VPN 확인
        if (conf.rules.vpn) {
            boolean isUpdate = false;
            if (essential.core.PluginData.INSTANCE.get("vpnListDate") == null || Long.parseLong(Objects.requireNonNull(essential.core.PluginData.INSTANCE.get("vpnListDate"))) + 8.64e7 < System.currentTimeMillis()) {
                essential.core.PluginData.INSTANCE.getStatus().add(new Pair<>("vpnListDate", String.valueOf(System.currentTimeMillis())));
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

            String file = root.child("data/ipv4.txt").readString();
            pluginData.vpnList = new String[(int) file.lines().count()];
        }

        // 이벤트 설정
        Event event = new Event();
        event.start();

        Log.info(bundle.get("event.plugin.loaded"));
    }

    DB.PlayerData findPlayerByUuid(String uuid) {
        return database.getPlayers().stream().filter( e -> e.getUuid().equals(uuid)).findFirst().orElse(null);
    }
}
