package essential.achievements;

import arc.Events;
import arc.util.Timer;
import essential.core.Bundle;
import essential.core.DB;
import mindustry.content.Planets;
import mindustry.game.EventType;

import java.util.Locale;
import java.util.ResourceBundle;

import static essential.core.Main.database;
import static mindustry.Vars.state;

public class Event {
    int tick = 0;

    @essential.core.annotation.Event
    void blockBuildEnd() {
        Events.on(EventType.BlockBuildEndEvent.class, e -> {
            if (e.unit.isPlayer()) {
                DB.PlayerData data = findPlayerByUuid(e.unit.getPlayer().uuid());
                if (data != null) {
                    if (Achievement.Builder.success(data)) {
                        Achievement.Builder.set(data);
                    }
                    if (Achievement.Deconstructor.success(data)) {
                        Achievement.Deconstructor.set(data);
                    }
                }
            }
        });
    }

    @essential.core.annotation.Event
    void gameover() {
        Events.on(EventType.GameOverEvent.class, e -> {
            database.getPlayers().forEach(data -> {
                if (Achievement.Eliminator.success(data)) {
                    Achievement.Eliminator.set(data);
                }

                if (Achievement.Lord.success(data)) {
                    Achievement.Lord.set(data);
                }

                if (Achievement.Aggressor.success(data)) {
                    Achievement.Aggressor.set(data);
                }

                if (e.winner == data.getPlayer().team()) {
                    if (Achievement.Asteroids.success(data)) {
                        Achievement.Asteroids.set(data);
                    }
                }
            });
        });
    }

    @essential.core.annotation.Event
    void wave() {
        Events.on(EventType.WaveEvent.class, e -> {
            database.getPlayers().forEach(data -> {
                int value = Integer.parseInt(data.getStatus().getOrDefault("record.wave", "0")) + 1;
                data.getStatus().put("record.wave", Integer.toString(value));
                if (Achievement.Defender.success(data)) {
                    Achievement.Defender.set(data);
                }
            });
        });
    }

    @essential.core.annotation.Event
    void achievementClear() {
        Events.on(CustomEvents.AchievementClear.class, e -> {
            Locale locale;
            if (e.playerData.getStatus().containsKey("language")) {
                locale = new Locale(e.playerData.getStatus().get("language"));
            } else {
                locale = new Locale(e.playerData.getPlayer().locale());
            }

            Bundle bundle = new Bundle(ResourceBundle.getBundle("bundle", locale));

            e.playerData.send(bundle, "event.achievement.success", e.achievement.toString().toLowerCase());
            database.getPlayers().forEach(data -> {
                Bundle b = new Bundle(ResourceBundle.getBundle("bundle", new Locale(data.getPlayer().locale())));
                data.send(b, "event.achievement.success.other", e.playerData.getName(), b.get("achievement." + e.achievement.toString().toLowerCase()));
            });
        });
    }

    @essential.core.annotation.Event
    void playerChat() {
        Events.on(EventType.PlayerChatEvent.class, e -> {
            if (!e.message.startsWith("/")) {
                DB.PlayerData data = findPlayerByUuid(e.player.uuid());
                if (data != null) {
                    int value = Integer.parseInt(data.getStatus().getOrDefault("record.time.chat", "0")) + 1;
                    data.getStatus().put("record.time.chat", Integer.toString(value));
                    if (Achievement.Chatter.success(data)) {
                        Achievement.Chatter.set(data);
                    }
                }
            }
        });
    }

    @essential.core.annotation.Event
    void updateSecond() {
        Timer.schedule(() -> {
            for (DB.PlayerData data : database.getPlayers()) {
                if (state.rules.planet == Planets.serpulo) {
                    int value = Integer.parseInt(data.getStatus().getOrDefault("record.time.serpulo", "0")) + 1;
                    data.getStatus().put("record.time.serpulo", Integer.toString(value));
                    if (Achievement.Serpulo.success(data)) {
                        Achievement.Serpulo.set(data);
                    }
                } else if (state.rules.planet == Planets.erekir) {
                    int value = Integer.parseInt(data.getStatus().getOrDefault("record.time.erekir", "0")) + 1;
                    data.getStatus().put("record.time.erekir", Integer.toString(value));
                    if (Achievement.Erekir.success(data)) {
                        Achievement.Erekir.set(data);
                    }
                } else if (state.rules.infiniteResources) {
                    int value = Integer.parseInt(data.getStatus().getOrDefault("record.time.sandbox", "0")) + 1;
                    data.getStatus().put("record.time.sandbox", Integer.toString(value));
                    if (Achievement.Creator.success(data)) {
                        Achievement.Creator.set(data);
                    }
                }
            }

            boolean isOwnerMeet = false;

            for (DB.PlayerData data : database.getPlayers()) {
                if(data.getPermission().equals("owner")) {
                    isOwnerMeet = true;
                    break;
                }
            }

            if(isOwnerMeet) {
                for (DB.PlayerData data : database.getPlayers()) {
                    int value = Integer.parseInt(data.getStatus().getOrDefault("record.time.meetowner", "0")) + 1;
                    data.getStatus().put("record.time.meetowner", Integer.toString(value));
                    if (Achievement.MeetOwner.success(data)) {
                        Achievement.MeetOwner.set(data);
                    }
                }
            }
        }, 0, 1);
    }

    DB.PlayerData findPlayerByUuid(String uuid) {
        return database.getPlayers().stream().filter( e -> e.getUuid().equals(uuid)).findFirst().orElse(null);
    }
}
