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
import mindustry.Vars;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
                int total = data.getPvpVictoriesCount() + data.getPvpDefeatCount();
                if (total < 50) {
                    result = 0;
                } else {
                    result = data.getPvpVictoriesCount() * 100 / total;
                }
            } catch (ArithmeticException e) {
                result = 0;
            }
            return result;
        }
    },

    Chatter {
        @Override
        public int value() {
            return 10000;
        }

        @Override
        public int current(DB.PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.time.chat", "0"));
        }
    },

    // ??
    MeetOwner {
        @Override
        public int value() {
            return 1;
        }

        @Override
        public boolean isHidden() {
            return true;
        }

        @Override
        public int current(DB.PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.time.meetowner", "0"));
        }
    },

    // Specific map clear achievements
    Asteroids {
        @Override
        public int value() {
            return 1;
        }

        @Override
        public boolean isHidden() {
            return true;
        }

        @Override
        public int current(DB.PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.map.clear.asteroids", "0"));
        }

        @Override
        public boolean success(DB.PlayerData data) {
            String mapHash = "7b032cc7815022be644d00a877ae0388";
            if (getMapHash().equals(mapHash)) {
                data.getStatus().put("record.map.clear.asteroids", "1");
                return true;
            } else {
                return false;
            }
        }
    },

    Transcendence {
        @Override
        public int value() {
            return 1;
        }

        @Override
        public boolean isHidden() {
            return true;
        }

        @Override
        public int current(DB.PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.map.clear.transcendence", "0"));
        }

        @Override
        public boolean success(DB.PlayerData data) {
            String mapHash = "f355b3d91d5d8215e557ff045b3864ef";
            if (getMapHash().equals(mapHash)) {
                data.getStatus().put("record.map.clear.transcendence", "1");
                return true;
            } else {
                return false;
            }
        }
    }
    ;

    public abstract int value();
    public boolean isHidden() {
        return false;
    }
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

    private static String getMapHash() {
        try {
            byte[] data = Files.readAllBytes(Vars.state.map.file.file().toPath());
            byte[] hash = MessageDigest.getInstance("MD5").digest(data);
            return new BigInteger(1, hash).toString(16);
        } catch (NoSuchAlgorithmException | IOException e){
            return "";
        }
    }
}