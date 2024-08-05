package essential.protect;

import arc.Events;
import arc.net.Server;
import arc.net.ServerDiscoveryHandler;
import arc.util.CommandHandler;
import arc.util.Http;
import arc.util.Log;
import essential.core.Bundle;
import essential.core.DB;
import essential.core.Permission;
import essential.core.annotation.ClientCommand;
import essential.core.annotation.ServerCommand;
import kotlin.Pair;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Player;
import mindustry.mod.Plugin;
import mindustry.net.Administration;
import mindustry.net.ArcNetProvider;
import mindustry.net.NetworkIO;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.nio.ByteBuffer;
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

        netServer.admins.addActionFilter(action -> {
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
            pluginData.vpnList = file.split("\n");
        }

        // 이벤트 설정
        Event event = new Event();
        event.start();

        Log.debug(bundle.get("event.plugin.loaded"));

        Events.on(EventType.WorldLoadEndEvent.class, e -> setNetworkFilter());
    }

    void setNetworkFilter() {
        try {
            Class<?> inner = ((ArcNetProvider) Vars.platform.getNet()).getClass();
            Field field = inner.getDeclaredField("server");
            field.setAccessible(true);

            Object serverInstance = field.get(Vars.platform.getNet());

            Class<?> innerClass = field.get(Vars.platform.getNet()).getClass();
            Method method = innerClass.getMethod("setDiscoveryHandler", ServerDiscoveryHandler.class);

            ServerDiscoveryHandler handler = new ServerDiscoveryHandler() {
                @Override
                public void onDiscoverReceived(InetAddress inetAddress, ReponseHandler reponseHandler) throws IOException {
                    if (!Vars.netServer.admins.isIPBanned(inetAddress.getHostAddress())) {
                        ByteBuffer buffer = NetworkIO.writeServerData();
                        buffer.position(0);
                        reponseHandler.respond(buffer);
                    } else {
                        reponseHandler.respond(ByteBuffer.allocate(0));
                    }
                }
            };

            method.invoke(serverInstance, handler);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Server.ServerConnectFilter filter = s -> !Vars.netServer.admins.bannedIPs.contains(s);
        Vars.platform.getNet().setConnectFilter(filter);
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
                        data.send("command.permission.false");
                    }
                });
            }
        }
    }

    DB.PlayerData findPlayerByUuid(String uuid) {
        return database.getPlayers().stream().filter( e -> e.getUuid().equals(uuid)).findFirst().orElse(null);
    }
}
