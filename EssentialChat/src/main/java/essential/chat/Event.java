package essential.chat;

import mindustry.Vars;
import mindustry.core.NetServer;
import mindustry.gen.Player;

import java.util.ArrayList;

public class Event {
    void load() {
        Vars.netServer.chatFormatter = new NetServer.ChatFormatter() {
            @Override
            public String format(Player player, String message) {
                if (Config.chatlimit) {
                    String[] configs = Config.chatlanguage.split(",");
                    ArrayList<Language> languages = new ArrayList<>();
                    for (String a : configs) {
                        languages.add(Language.getByIsoCode639_1(IsoCode639_1.valueOf(a.toUpperCase())));
                    }

                    LanguageDetector d = LanguageDetectorBuilder.fromLanguages(languages.toArray(new Language[0])).build();
                    Language e = d.detectLanguageOf(message);

                    if (e.getName().equals("UNKNOWN") && !specificTextRegex.matcher(message.substring(0, 1)).matches() && !(voting && message.equalsIgnoreCase("y") && !voted.contains(player.uuid()))) {
                        player.sendMessage(Bundle(findPlayerData(player.uuid()).languageTag)["event.chat.language.not.allow"]);
                        isMute = true;
                    }
                }

                if (Config.chatBlacklist) {
                    String[] file = root.child("chat_blacklist.txt").readString("UTF-8").split("\r\n");
                    for (String text : file) {
                        if (Config.chatBlacklistRegex) {
                            if (Pattern.compile(text).matcher(message).find()) {
                                player.sendMessage(Bundle(findPlayerData(player.uuid()).languageTag)["event.chat.blacklisted"]);
                                isMute = true;
                            }
                        } else {
                            if (message.contains(text)) {
                                player.sendMessage(Bundle(findPlayerData(player.uuid()).languageTag)["event.chat.blacklisted"]);
                                isMute = true;
                            }
                        }
                    }
                }

                val format = Permission[data].chatFormat.replace("%1", "[#${player.color}]${data.name}")
                        .replace("%2", message).replace("%3", "${data.level}")
                if (isGlobalMute && Permission.check(data, "chat.admin") && !isMute) {
                    format
                } else if (!isGlobalMute && !(voting && message.contains("y", true) && !isMute)) {
                    format
                } else {
                    null
                }
            }
        };
    }
}
