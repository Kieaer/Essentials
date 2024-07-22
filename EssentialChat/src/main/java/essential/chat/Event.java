package essential.chat;

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

import java.util.ArrayList;
import java.util.regex.Pattern;

import static essential.chat.Main.conf;
import static essential.core.Event.INSTANCE;
import static essential.core.Main.database;
import static essential.core.Main.root;

public class Event {
    LanguageDetector detector;

    void loadDetector() {
        String[] configs = conf.getStrict().getLanguage().split(",");
        ArrayList<Language> languages = new ArrayList<>();
        for (String a : configs) {
            languages.add(Language.getByIsoCode639_1(IsoCode639_1.valueOf(a.toUpperCase())));
        }

        detector = LanguageDetectorBuilder.fromLanguages(languages.toArray(new Language[0])).build();
    }

    void load() {
        if (conf.getStrict().getEnabled()) {
            loadDetector();
        }

        Vars.netServer.admins.addChatFilter(new Administration.ChatFilter() {
            private final Pattern specificTextRegex = Pattern.compile("[!@#$%&*()_+=|<>?{}\\[\\]~-]");

            @Override
            public String filter(Player player, String message) {
                Bundle bundle = new Bundle(player.locale);
                if (conf.getStrict().getEnabled()) {
                    Language e = detector.detectLanguageOf(message);

                    if (e == Language.UNKNOWN && !specificTextRegex.matcher(message.substring(0, 1)).matches() && !(INSTANCE.getVoting() && message.equalsIgnoreCase("y") && !INSTANCE.getVoted().contains(player.uuid()))) {
                        player.sendMessage(bundle.get("event.chat.language.not.allow"));
                        return null;
                    }
                }

                if (conf.getBlacklist().getEnabled()) {
                    String[] file = root.child("chat_blacklist.txt").readString("UTF-8").split("\r\n");
                    for (String text : file) {
                        if (conf.getBlacklist().getRegex()) {
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
                DB.PlayerData data = database.get(player.uuid());
                if (message != null) {
                    String defaultFormat = "[coral][[" + player.coloredName() + "[coral]]:[white] " + message;
                    if (data != null) {
                        String chatFormat = Permission.INSTANCE.get(data).getChatFormat();
                        if (chatFormat.isEmpty()) {
                            return defaultFormat;
                        } else {
                            return chatFormat
                                    .replace("%1", "[${player.color}]${data.name}")
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
}
