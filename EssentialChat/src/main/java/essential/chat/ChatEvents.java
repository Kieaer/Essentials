package essential.chat;

import arc.util.Log;
import com.github.pemistahl.lingua.api.IsoCode639_1;
import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import essential.PluginDataKt;
import essential.bundle.Bundle;
import essential.event.CustomEvents;
import essential.permission.Permission;
import ksp.event.Event;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Player;
import mindustry.net.Administration;

import java.util.ArrayList;
import java.util.regex.Pattern;

import static essential.chat.Main.conf;

/**
 * Event handlers for the EssentialChat plugin.
 */
public class ChatEvents {
    private static LanguageDetector detector;

    /**
     * Load the language detector based on the configuration.
     */
    public static void loadDetector() {
        String[] configs = conf.getStrict().getLanguage().split(",");
        ArrayList<Language> languages = new ArrayList<>();

        for (String a : configs) {
            languages.add(Language.getByIsoCode639_1(IsoCode639_1.valueOf(a.toUpperCase())));
        }

        detector = LanguageDetectorBuilder.fromLanguages(languages.toArray(new Language[0])).build();
    }

    /**
     * Load the chat filter and language detector.
     */
    public static void load() {
        if (conf.getStrict().isEnabled()) {
            loadDetector();
        }

        Vars.netServer.admins.addChatFilter(new Administration.ChatFilter() {
            private final Pattern specificTextRegex = Pattern.compile("[!@#$%&*()_+=|<>?{}\\[\\]~-]");

            @Override
            public String filter(Player player, String message) {
                Bundle bundle = new Bundle(player.locale());

                if (conf.getStrict().isEnabled()) {
                    Language detectedLanguage = detector.detectLanguageOf(message);

                    if (detectedLanguage == Language.UNKNOWN &&
                        !specificTextRegex.matcher(message.substring(0, 1)).matches() &&
                        !(PluginDataKt.isVoting() && message.equalsIgnoreCase("y"))) {
                        player.sendMessage(bundle.get("event.chat.language.not.allow"));
                        return null;
                    }
                }

                if (conf.getBlacklist().isEnabled()) {
                    try {
                        String[] blacklist = PluginDataKt.getRootPath().child("chat_blacklist.txt")
                            .readString("UTF-8").split("\r\n");

                        for (String text : blacklist) {
                            if (conf.getBlacklist().isRegex()) {
                                if (Pattern.compile(text).matcher(message).find()) {
                                    player.sendMessage(bundle.get("event.chat.blacklisted"));
                                    return null;
                                }
                            } else {
                                if (message.contains(text)) {
                                    player.sendMessage(bundle.get("event.chat.blacklisted"));
                                    return null;
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.err("Error reading blacklist file", e);
                    }
                }

                return message;
            }
        });
    }

    /**
     * Event handler for the ServerLoadEvent.
     * Sets up the chat formatter.
     *
     * @param event The server load event
     */
    @Event
    public static void serverLoaded(EventType.ServerLoadEvent event) {
        Vars.netServer.chatFormatter = (player, message) -> {
            if (player != null) {
                essential.database.data.PlayerData data = essential.util.UtilsKt.findPlayerData(player.uuid());
                if (message != null) {
                    String defaultFormat = "[coral][[" + player.coloredName() + "[coral]]:[white] " + message;
                    if (data != null) {
                        String chatFormat = Permission.INSTANCE.get(data).getChatFormat();

                        if (chatFormat.isEmpty()) {
                            return defaultFormat;
                        } else {
                            return chatFormat
                                .replace("%1", player.coloredName())
                                .replace("%2", message);
                        }
                    } else {
                        return defaultFormat;
                    }
                }
            }
            return null;
        };
    }

    /**
     * Event handler for the ConfigFileModified event.
     * Reloads the configuration when the config file is modified.
     *
     * @param event The config file modified event
     */
    @Event
    public static void configFileModified(CustomEvents.ConfigFileModified event) {
        if (event.getKind() == java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY) {
            if (event.getPaths().equals("config_chat.yaml")) {
                try {
                    // Use Jackson for YAML parsing in Java
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
                    arc.files.Fi configFile = PluginDataKt.getRootPath().child("config/config_chat.yaml");

                    if (!configFile.exists()) {
                        Log.err(new Bundle().get("event.plugin.load.failed"));
                        return;
                    }

                    ChatConfig config = mapper.readValue(configFile.file(), ChatConfig.class);

                    if (config == null) {
                        Log.err(new Bundle().get("event.plugin.load.failed"));
                        return;
                    }

                    conf = config;

                    Log.info(new Bundle().get("config.reloaded"));
                } catch (Exception e) {
                    Log.err("Failed to load config file", e);
                }
            }
        }
    }
}