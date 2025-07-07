package essential.achievements;

import arc.Events;
import essential.database.data.PlayerData;
import mindustry.Vars;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static essential.database.data.AchievementDataKt.hasAchievement;
import static essential.database.data.AchievementDataKt.setAchievement;

public enum Achievement {
    // int array values are current value and target value
    CrawlerBlockDestroyer {
        @Override
        public int value() {
            return 5;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.crawler.block.destroy", "0"));
        }
    },

    SoloMapClear {
        @Override
        public int value() {
            return 1;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.map.clear.solo", "0"));
        }
    },

    LongPlayNoAfk {
        @Override
        public int value() {
            return 18000; // 5 hours in seconds
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.time.noafk", "0"));
        }
    },

    NoMiningClear {
        @Override
        public int value() {
            return 1;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.map.clear.nomining", "0"));
        }
    },

    SerpuloPvPWin {
        @Override
        public int value() {
            return 5;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.pvp.win.serpulo", "0"));
        }
    },

    ErekirPvPWin {
        @Override
        public int value() {
            return 5;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.pvp.win.erekir", "0"));
        }
    },

    BothPlanetsPvPWin {
        @Override
        public int value() {
            return 10;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.pvp.win.both", "0"));
        }
    },

    LeaveAndLosePvP {
        @Override
        public int value() {
            return 10;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.pvp.leave.lose", "0"));
        }
    },

    PvPWinMaster {
        @Override
        public int value() {
            return 300;
        }

        @Override
        public int current(PlayerData data) {
            return data.getPvpWinCount();
        }
    },

    MapClearMaster {
        @Override
        public int value() {
            return 10;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.map.clear.count", "0"));
        }
    },

    TurretMultiKill {
        @Override
        public int value() {
            return 5;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.turret.multikill", "0"));
        }
    },

    QuillKiller {
        @Override
        public int value() {
            return 5;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.turret.quill.kill", "0"));
        }
    },

    ZenithKiller {
        @Override
        public int value() {
            return 30;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.turret.zenith.kill", "0"));
        }
    },

    OmuraHorizonKiller {
        @Override
        public int value() {
            return 5;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.omura.horizon.kill", "0"));
        }
    },

    PvPWinStreak {
        @Override
        public int value() {
            return 5;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.pvp.win.streak", "0"));
        }
    },

    PvPWinRate {
        @Override
        public int value() {
            return 20;
        }

        @Override
        public int current(PlayerData data) {
            int result;
            try {
                int total = data.getPvpWinCount() + data.getPvpLoseCount();
                if (total < 10) { // Require at least 10 games for win rate calculation
                    result = 0;
                } else {
                    result = data.getPvpWinCount() * 100 / total;
                }
            } catch (ArithmeticException e) {
                result = 0;
            }
            return result;
        }
    },

    PvPUnderdog {
        @Override
        public int value() {
            return 1;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.pvp.underdog", "0"));
        }
    },

    WarpServerDisconnect {
        @Override
        public int value() {
            return 1;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.warp.disconnect", "0"));
        }
    },

    ExplosionKiller {
        @Override
        public int value() {
            return 10;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.explosion.kill", "0"));
        }
    },

    LowPowerClear {
        @Override
        public int value() {
            return 1;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.map.clear.lowpower", "0"));
        }
    },

    PvPDefeatStreak {
        @Override
        public int value() {
            return 5;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.pvp.defeat.streak", "0"));
        }
    },

    NoPowerClear {
        @Override
        public int value() {
            return 1;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.map.clear.nopower", "0"));
        }
    },

    NoTurretsClear {
        @Override
        public int value() {
            return 1;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.map.clear.noturrets", "0"));
        }
    },

    DuoTurretSurvival {
        @Override
        public int value() {
            return 100;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.wave.duo", "0"));
        }
    },

    FlareOnlyClear {
        @Override
        public int value() {
            return 1;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.map.clear.flareonly", "0"));
        }
    },

    VotingBan {
        @Override
        public int value() {
            return 1;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.voting.ban", "0"));
        }
    },

    PvPContribution {
        @Override
        public int value() {
            return 1;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.pvp.contribution", "0"));
        }
    },

    APM50 {
        @Override
        public int value() {
            return 50;
        }

        @Override
        public int current(PlayerData data) {
            return data.getApm();
        }
    },

    APM100 {
        @Override
        public int value() {
            return 100;
        }

        @Override
        public int current(PlayerData data) {
            return data.getApm();
        }
    },

    APM200 {
        @Override
        public int value() {
            return 200;
        }

        @Override
        public int current(PlayerData data) {
            return data.getApm();
        }
    },

    MapProvider {
        @Override
        public int value() {
            return 1;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.map.provider", "0"));
        }
    },

    FeedbackProvider {
        @Override
        public int value() {
            return 1;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.feedback.provider", "0"));
        }
    },

    Builder {
        @Override
        public int value() {
            return 100000;
        }

        @Override
        public int current(PlayerData data) {
            return data.getBlockPlaceCount();
        }
    },

    Deconstructor {
        @Override
        public int value() {
            return 100000;
        }

        @Override
        public int current(PlayerData data) {
            return data.getBlockBreakCount();
        }
    },

    Creator {
        @Override
        public int value() {
            return 360000;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.time.sandbox", "0"));
        }
    },

    Eliminator {
        @Override
        public int value() {
            return 100;
        }

        @Override
        public int current(PlayerData data) {
            return data.getPvpWinCount();
        }
    },

    Defender {
        @Override
        public int value() {
            return 10000;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.wave", "0"));
        }
    },

    Aggressor {
        @Override
        public int value() {
            return 50;
        }

        @Override
        public int current(PlayerData data) {
            return data.getAttackClear();
        }
    },

    Serpulo {
        @Override
        public int value() {
            return 360000;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.time.serpulo", "0"));
        }
    },

    Erekir {
        @Override
        public int value() {
            return 360000;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.time.erekir", "0"));
        }
    },

    TurbidWater {
        @Override
        public int value() {
            return 360000;
        }

        @Override
        public int current(PlayerData data) {
            return data.getTotalPlayed();
        }
    },

    BlackWater {
        @Override
        public int value() {
            return 720000;
        }

        @Override
        public int current(PlayerData data) {
            return data.getTotalPlayed();
        }
    },

    Oil {
        @Override
        public int value() {
            return 1080000;
        }

        @Override
        public int current(PlayerData data) {
            return data.getTotalPlayed();
        }
    },

    SerpuloQuad {
        @Override
        public int value() {
            return 1;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.unit.serpulo.quad", "0"));
        }
    },

    Lord {
        @Override
        public int value() {
            return 70;
        }

        @Override
        public int current(PlayerData data) {
            int result;
            try {
                int total = data.getPvpWinCount() + data.getPvpLoseCount();
                if (total < 50) {
                    result = 0;
                } else {
                    result = data.getPvpWinCount() * 100 / total;
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
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.time.chat", "0"));
        }
    },

    // ??
    MeetOwner {
        @Override
        public int value() {
            return 1;
        }

        private final boolean isHidden = true;

        @Override
        public boolean isHidden() {
            return isHidden;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.time.meetowner", "0"));
        }
    },

    // Specific map clear achievements
    Asteroids {
        @Override
        public int value() {
            return 1;
        }

        private final boolean isHidden = true;

        @Override
        public boolean isHidden() {
            return isHidden;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.map.clear.asteroids", "0"));
        }

        @Override
        public boolean success(PlayerData data) {
            String mapHash = "7b032cc7815022be644d00a877ae0388";
            if (Achievement.getMapHash().equals(mapHash)) {
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

        private final boolean isHidden = true;

        @Override
        public boolean isHidden() {
            return isHidden;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.map.clear.transcendence", "0"));
        }

        @Override
        public boolean success(PlayerData data) {
            String mapHash = "f355b3d91d5d8215e557ff045b3864ef";
            if (Achievement.getMapHash().equals(mapHash)) {
                data.getStatus().put("record.map.clear.transcendence", "1");
                return true;
            } else {
                return false;
            }
        }
    },

    NewYear {
        @Override
        public int value() {
            return 1;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.chat.newyear", "0"));
        }
    },

    Loyal {
        @Override
        public int value() {
            return 1;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.login.loyal", "0"));
        }
    },

    WaterExtractor {
        @Override
        public int value() {
            return 10;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.build.waterextractor", "0"));
        }
    },

    Attendance {
        @Override
        public int value() {
            return 100;
        }

        @Override
        public int current(PlayerData data) {
            return data.getAttendanceDays();
        }
    },

    LoyalSixMonths {
        @Override
        public int value() {
            return 1;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.login.loyal.sixmonths", "0"));
        }
    },

    LoyalOneYearSixMonths {
        @Override
        public int value() {
            return 1;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.login.loyal.oneyearsixmonths", "0"));
        }
    },

    AllMaps {
        @Override
        public int value() {
            return 1;
        }

        @Override
        public int current(PlayerData data) {
            return Integer.parseInt(data.getStatus().getOrDefault("record.map.clear.all", "0"));
        }
    },

    DiscordAuth {
        @Override
        public int value() {
            return 1;
        }

        @Override
        public int current(PlayerData data) {
            return data.getDiscordID() != null && !data.getDiscordID().isEmpty() ? 1 : 0;
        }
    };

    public abstract int value();

    public boolean isHidden() {
        return false;
    }

    public abstract int current(PlayerData data);

    public boolean success(PlayerData data) {
        // Prevent achievements from being cleared in sandbox mode
        if (Vars.state.rules.infiniteResources) {
            return false;
        }

        String achievementName = this.toString().toLowerCase(Locale.getDefault());

        if (hasAchievement(data, achievementName)) {
            return false;
        } else {
            return current(data) >= value();
        }
    }

    public void set(PlayerData data) {
        String achievementName = this.toString().toLowerCase(Locale.getDefault());

        // Store in status for temporary use during this session
        if (!data.getStatus().containsKey("achievement." + achievementName)) {
            data.getStatus().put(
                "achievement." + achievementName,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );

            setAchievement(data, achievementName);

            Events.fire(new CustomEvents.AchievementClear(this, data));
        }
    }

    private static String getMapHash() {
        try {
            byte[] data = Files.readAllBytes(Vars.state.map.file.file().toPath());
            byte[] hash = MessageDigest.getInstance("MD5").digest(data);
            return new BigInteger(1, hash).toString(16);
        } catch (NoSuchAlgorithmException | IOException e) {
            return "";
        }
    }
}
