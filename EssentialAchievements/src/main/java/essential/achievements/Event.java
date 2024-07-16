package essential.achievements;

import arc.Events;
import essential.core.DB;
import mindustry.content.Planets;
import mindustry.game.EventType;
import mindustry.type.Planet;

import static essential.core.Main.database;
import static mindustry.Vars.state;

public class Event {
    int tick = 0;

    void start() {
        Events.on(EventType.BlockBuildEndEvent.class, e -> {
            if (e.unit.isPlayer()) {
                DB.PlayerData data = findPlayerByUuid(e.unit.getPlayer().uuid());
                if (data != null) {
                    if (!data.getStatus().containsKey("achievement.builder") && data.getBlockPlaceCount() >= 100000) {
                        data.getStatus().put("achievement.builder", "true");
                    }
                    if (!data.getStatus().containsKey("achievement.destroyer") && data.getBlockBreakCount() >= 100000) {
                        data.getStatus().put("achievement.destroyer", "true");
                    }
                }
            }
        });

        Events.on(EventType.GameOverEvent.class, e -> {
            database.getPlayers().forEach(data -> {
                if (!data.getStatus().containsKey("achievement.pvp_master") && data.getPvpVictoriesCount() >= 100) {
                    data.getStatus().put("achievement.pvp_master", "true");
                }

                if (!data.getStatus().containsKey("achievement.champion")) {
                    Integer victories = data.getPvpVictoriesCount();
                    Integer defeats = data.getPvpDefeatCount();
                    double percent = (victories + defeats) / 100.0;
                    if ((victories + defeats) >= 50 && percent >= 70.0) {
                        data.getStatus().put("achievement.champion", "true");
                    }
                }

                if (!data.getStatus().containsKey("achievement.conqueror") && data.getAttackModeClear() >= 50) {
                    data.getStatus().put("achievement.conqueror", "true");
                }
            });
        });

        Events.on(EventType.WaveEvent.class, e -> {
            database.getPlayers().forEach(data -> {
                data.getStatus().put("record.wave", String.valueOf(Integer.parseInt(data.getStatus().get("record.wave")) + 1));

                if (!data.getStatus().containsKey("achievement.defence_master") && Integer.parseInt(data.getStatus().get("record.wave")) >= 3000) {
                    data.getStatus().put("achievement.defence_master", "true");
                }
            });
        });

        Events.on(EventType.Trigger.class, e -> {
            tick++;
            if (tick >= 60) {
                database.getPlayers().forEach(data -> {
                    if (state.rules.planet == Planets.serpulo) {
                        data.getStatus().put("record.time.serpulo", String.valueOf(Integer.parseInt(data.getStatus().get("record.time.serpulo")) + 1));
                        if (!data.getStatus().containsKey("achievement.serpulo_master") && Integer.parseInt(data.getStatus().get("record.time.serpulo")) >= 360000) {
                            data.getStatus().put("achievement.serpulo_master", "true");
                        }
                    } else if (state.rules.planet == Planets.erekir) {
                        data.getStatus().put("record.time.erekir", String.valueOf(Integer.parseInt(data.getStatus().get("record.time.erekir")) + 1));
                        if (!data.getStatus().containsKey("achievement.erekir_master") && Integer.parseInt(data.getStatus().get("record.time.erekir")) >= 360000) {
                            data.getStatus().put("achievement.erekir_master", "true");
                        }
                    }
                    if (!data.getStatus().containsKey("achievement.veteran") && data.getTotalPlayTime() >= 360000) {
                        data.getStatus().put("achievement.veteran", "true");
                    }
                    if (!data.getStatus().containsKey("achievement.master") && data.getTotalPlayTime() >= 720000) {
                        data.getStatus().put("achievement.master", "true");
                    }
                    if (!data.getStatus().containsKey("achievement.challenger") && data.getTotalPlayTime() >= 1080000) {
                        data.getStatus().put("achievement.challenger", "true");
                    }
                });
            } else {
                tick = 0;
            }
        });
    }

    DB.PlayerData findPlayerByUuid(String uuid) {
        return database.getPlayers().stream().filter( e -> e.getUuid().equals(uuid)).findFirst().orElse(null);
    }
}
