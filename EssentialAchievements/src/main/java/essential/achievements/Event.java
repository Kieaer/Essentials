package essential.achievements;

import arc.Events;
import essential.core.Bundle;
import essential.core.DB;
import mindustry.content.Planets;
import mindustry.game.EventType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

import static essential.core.Main.database;
import static mindustry.Vars.state;

public class Event {
    int tick = 0;

    void set(Achievement achievement, DB.PlayerData data) {
        if (has(achievement, data)) {
            data.getStatus().put("achievement." + achievement.toString().toLowerCase(), LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            Events.fire(new CustomEvents.AchievementClear(achievement, data));
        }
    }

    boolean has(Achievement achievement, DB.PlayerData data) {
        return !data.getStatus().containsKey("achievement." + achievement.toString().toLowerCase());
    }

    void start() {
        Events.on(EventType.BlockBuildEndEvent.class, e -> {
            if (e.unit.isPlayer()) {
                DB.PlayerData data = findPlayerByUuid(e.unit.getPlayer().uuid());
                if (data != null) {
                    if (data.getBlockPlaceCount() >= 100000) {
                        set(Achievement.Builder, data);
                    }
                    if (data.getBlockBreakCount() >= 100000) {
                        set(Achievement.Deconstructor, data);
                    }
                }
            }
        });

        Events.on(EventType.GameOverEvent.class, e -> {
            database.getPlayers().forEach(data -> {
                if (data.getPvpVictoriesCount() >= 100) {
                    set(Achievement.Eliminator, data);
                }

                if (has(Achievement.Lord, data)) {
                    Integer victories = data.getPvpVictoriesCount();
                    Integer defeats = data.getPvpDefeatCount();
                    double percent = (victories + defeats) / 100.0;
                    if ((victories + defeats) >= 50 && percent >= 70.0) {
                        set(Achievement.Lord, data);
                    }
                }

                if (data.getAttackModeClear() >= 50) {
                    set(Achievement.Aggressor, data);
                }
            });
        });

        Events.on(EventType.WaveEvent.class, e -> {
            database.getPlayers().forEach(data -> {
                data.getStatus().put("record.wave", String.valueOf(Integer.parseInt(data.getStatus().get("record.wave")) + 1));
                if (Integer.parseInt(data.getStatus().get("record.wave")) >= 10000) {
                    set(Achievement.Defender, data);
                }
            });
        });

        Events.on(EventType.Trigger.class, e -> {
            tick++;
            if (tick >= 60) {
                database.getPlayers().forEach(data -> {
                    if (state.rules.planet == Planets.serpulo) {
                        data.getStatus().put("record.time.serpulo", String.valueOf(Integer.parseInt(data.getStatus().get("record.time.serpulo")) + 1));
                        if (Integer.parseInt(data.getStatus().get("record.time.serpulo")) >= 360000) {
                            set(Achievement.Serpulo, data);
                        }
                    } else if (state.rules.planet == Planets.erekir) {
                        data.getStatus().put("record.time.erekir", String.valueOf(Integer.parseInt(data.getStatus().get("record.time.erekir")) + 1));
                        if (Integer.parseInt(data.getStatus().get("record.time.erekir")) >= 360000) {
                            set(Achievement.Erekir, data);
                        }
                    } else if (state.rules.infiniteResources) {
                        data.getStatus().put("record.time.sandbox", String.valueOf(Integer.parseInt(data.getStatus().get("record.time.sandbox")) + 1));
                        if (Integer.parseInt(data.getStatus().get("record.time.sandbox")) >= 360000) {
                            set(Achievement.Creator, data);
                        }
                    }
                    if (data.getTotalPlayTime() >= 360000) {
                        set(Achievement.TurbidWater, data);
                    }
                    if (data.getTotalPlayTime() >= 720000) {
                        set(Achievement.BlackWater, data);
                    }
                    if (data.getTotalPlayTime() >= 1080000) {
                        set(Achievement.Oil, data);
                    }
                });
            } else {
                tick = 0;
            }
        });

        Events.on(CustomEvents.AchievementClear.class, e -> {
            Locale locale;
            if (e.playerData.getStatus().containsKey("language")) {
                locale = new Locale(e.playerData.getStatus().get("language"));
            } else {
                locale = new Locale(e.playerData.getPlayer().locale());
            }

            Bundle bundle = new Bundle(ResourceBundle.getBundle("bundle", locale));

            e.playerData.send(bundle, "event.achievement.success", e.achievement.toString().toLowerCase());
        });
    }

    DB.PlayerData findPlayerByUuid(String uuid) {
        return database.getPlayers().stream().filter( e -> e.getUuid().equals(uuid)).findFirst().orElse(null);
    }
}
