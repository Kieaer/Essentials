package essential.achievements;

/*
public enum Achievement {
    Builder, Deconstructor,
    // Specific
    Creator, Eliminator, Defender, Aggressor, Serpulo, Erekir,
    // Time
    TurbidWater, BlackWater, Oil,
    // PvP
    Lord,
    */
/*Iron, Bronze, Silver, Gold, Platinum, Master, GrandMaster*//*

    // Wave
    // Attack
}*/

import arc.Events;
import essential.core.DB;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public enum Achievement {
    // int 배열값은 현재 값과 목표 값
    Builder {
        @Override
        public int value() {
            return 100000;
        }

        @Override
        public int current(DB.PlayerData data) {
            return data.getBlockPlaceCount();
        }
    },
    Deconstructor {
        @Override
        public int value() {
            return 100000;
        }

        @Override
        public int current(DB.PlayerData data) {
            return data.getBlockBreakCount();
        }
    },

    Creator {
        @Override
        public int value() {
            return 360000;
        }

        @Override
        public int current(DB.PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.time.sandbox", "0"));
        }
    },
    Eliminator {
        @Override
        public int value() {
            return 100;
        }

        @Override
        public int current(DB.PlayerData data) {
            return data.getPvpVictoriesCount();
        }
    },
    Defender {
        @Override
        public int value() {
            return 10000;
        }

        @Override
        public int current(DB.PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.wave", "0"));
        }
    },
    Aggressor {
        @Override
        public int value() {
            return 50;
        }

        @Override
        public int current(DB.PlayerData data) {
            return data.getAttackModeClear();
        }
    },
    Serpulo {
        @Override
        public int value() {
            return 360000;
        }

        @Override
        public int current(DB.PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.time.serpulo", "0"));
        }
    },
    Erekir {
        @Override
        public int value() {
            return 360000;
        }

        @Override
        public int current(DB.PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.time.erekir", "0"));
        }
    },

    TurbidWater {
        @Override
        public int value() {
            return 360000;
        }

        @Override
        public int current(DB.PlayerData data) {
            return (int) data.getTotalPlayTime();
        }
    },
    BlackWater {
        @Override
        public int value() {
            return 720000;
        }

        @Override
        public int current(DB.PlayerData data) {
            return (int) data.getTotalPlayTime();
        }
    },
    Oil {
        @Override
        public int value() {
            return 1080000;
        }

        @Override
        public int current(DB.PlayerData data) {
            return (int) data.getTotalPlayTime();
        }
    },

    Lord {
        @Override
        public int value() {
            return 70;
        }

        @Override
        public int current(DB.PlayerData data) {
            int result;
            try {
                result = (data.getPvpVictoriesCount() + data.getPvpDefeatCount()) / (data.getPvpVictoriesCount() + data.getPvpDefeatCount()) * 100;
            } catch (ArithmeticException e) {
                result = 0;
            }
            return result;
        }
    };

    public abstract int value();
    public abstract int current(DB.PlayerData data);
    public boolean success(DB.PlayerData data) {
        return current(data) >= value();
    }
    public void set(DB.PlayerData data) {
        if (!data.getStatus().containsKey("achievement." + this.toString().toLowerCase())) {
            data.getStatus().put("achievement." + this.toString().toLowerCase(), LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            Events.fire(new CustomEvents.AchievementClear(this, data));
        }
    }
}