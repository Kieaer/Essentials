package essential.chat;

import arc.Events;
import arc.util.Log;
import com.github.pemistahl.lingua.api.IsoCode639_1;
import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import essential.core.Bundle;
import essential.core.DB;
import essential.core.Permission;
import mindustry.Vars;
import mindustry.gen.Player;
import mindustry.net.Administration;

import java.nio.file.StandardWatchEventKinds;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Pattern;

import static essential.chat.Main.conf;
import static essential.core.Main.*;

public class Event {
    LanguageDetector detector;

    void loadDetector() {
        String[] configs = conf.strict.language.split(",");
        ArrayList<Language> languages = new ArrayList<>();
        for (String a : configs) {
            languages.add(Language.getByIsoCode639_1(IsoCode639_1.valueOf(a.toUpperCase())));
        }

        detector = LanguageDetectorBuilder.fromLanguages(languages.toArray(new Language[0])).build();
    }

    void load() {
        if (conf.strict.enabled) {
            loadDetector();
        }

        Vars.netServer.admins.addChatFilter(new Administration.ChatFilter() {
            private final Pattern specificTextRegex = Pattern.compile("[!@#$%&*()_+=|<>?{}\\[\\]~-]");

            @Override
            public String filter(Player player, String message) {
                Bundle bundle = new Bundle(player.locale);
                if (conf.strict.enabled) {
                    Language e = detector.detectLanguageOf(message);

                    if (e == Language.UNKNOWN && !specificTextRegex.matcher(message.substring(0, 1)).matches() && !(pluginData.getVoting() && message.equalsIgnoreCase("y"))) {
                        player.sendMessage(bundle.get("event.chat.language.not.allow"));
                        return null;
                    }
                }

                if (conf.blacklist.enabled) {
                    String[] file = root.child("chat_blacklist.txt").readString("UTF-8").split("\r\n");
                    for (String text : file) {
                        if (conf.blacklist.regex) {
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
                }

                return message;
            }
        });

        Vars.netServer.chatFormatter = (player, message) -> {
            if (player != null) {
                DB.PlayerData data = findPlayerData(player.uuid());
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

        Events.on(essential.core.CustomEvents.ConfigFileModified.class, e -> {
            if (e.getKind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                if (e.getPaths().equals("config_chat.yaml")) {
                    Main.conf = essential.core.Main.Companion.createAndReadConfig(
                            "config_chat.yaml",
                            Objects.requireNonNull(this.getClass().getResourceAsStream("/config_chat.yaml")),
                            Config.class
                    );
                    Log.info(new Bundle().get("config.reloaded"));
                }
            }
        });
    }

    DB.PlayerData findPlayerData(String uuid) {
        for (DB.PlayerData data : database.getPlayers()) {
            if ((data.getOldUUID() != null && data.getOldUUID().equals(uuid)) || data.getUuid().equals(uuid)) {
                return data;
            }
        }
        return null;
    }
}
