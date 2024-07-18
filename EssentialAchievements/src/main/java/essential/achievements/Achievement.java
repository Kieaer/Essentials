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

import essential.core.DB;

public enum Achievement {
    // int 배열값은 현재 값과 목표 값
    Builder {
        @Override
        public int[] get(DB.PlayerData data) {
            return new int[]{data.getBlockPlaceCount(), 100000};
        }
    },
    Deconstructor {
        @Override
        public int[] get(DB.PlayerData data) {
            return new int[]{data.getBlockBreakCount(), 100000};
        }
    },

    Creator {
        @Override
        public int[] get(DB.PlayerData data) {
            return new int[]{Integer.parseInt(data.getStatus().get("record.time.sandbox")), 360000};
        }
    },
    Eliminator {
        @Override
        public int[] get(DB.PlayerData data) {
            return new int[]{data.getPvpVictoriesCount(), 100};
        }
    },
    Defender {
        @Override
        public int[] get(DB.PlayerData data) {
            return new int[]{Integer.parseInt(data.getStatus().get("record.wave")), 10000};
        }
    },
    Aggressor {
        @Override
        public int[] get(DB.PlayerData data) {
            return new int[]{data.getAttackModeClear(), 50};
        }
    },
    Serpulo {
        @Override
        public int[] get(DB.PlayerData data) {
            return new int[]{Integer.parseInt(data.getStatus().get("record.time.serpulo")), 360000};
        }
    },
    Erekir {
        @Override
        public int[] get(DB.PlayerData data) {
            return new int[]{Integer.parseInt(data.getStatus().get("record.time.erekir")), 360000};
        }
    },

    TurbidWater {
        @Override
        public int[] get(DB.PlayerData data) {
            return new int[]{(int) data.getTotalPlayTime(), 360000};
        }
    },
    BlackWater {
        @Override
        public int[] get(DB.PlayerData data) {
            return new int[]{(int) data.getTotalPlayTime(), 720000};
        }
    },
    Oil {
        @Override
        public int[] get(DB.PlayerData data) {
            return new int[]{(int) data.getTotalPlayTime(), 1080000};

        }
    },

    Lord {
        @Override
        public int[] get(DB.PlayerData data) {
            return new int[]{
                (data.getPvpVictoriesCount() + data.getPvpDefeatCount()) / (data.getPvpVictoriesCount() + data.getPvpDefeatCount()) * 100, 70
            };
        }
    };

    public abstract int[] get(DB.PlayerData data);
}