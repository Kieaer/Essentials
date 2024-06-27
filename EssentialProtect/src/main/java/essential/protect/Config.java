package essential.protect;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Config {
    private Pvp pvp;
    private Account account;
    private Protect protect;

    public Pvp getPvp() {
        return pvp;
    }

    public Account getAccount() {
        return account;
    }

    public Protect getProtect() {
        return protect;
    }

    public static class Pvp {
        private Peace peace;
        private Border border;

        @JsonProperty("destroy-core")
        private boolean destroyCore;

        public Peace getPeace() {
            return peace;
        }

        public Border getBorder() {
            return border;
        }

        public boolean isDestroyCore() {
            return destroyCore;
        }

        public static class Peace {
            private boolean enabled;
            private int time;

            public boolean isEnabled() {
                return enabled;
            }

            public int getTime() {
                return time;
            }
        }

        public static class Border {
            private boolean enabled;

            public boolean isEnabled() {
                return enabled;
            }
        }
    }

    public static class Account {
        public enum AuthType {
            None, Password, Discord
        }

        private boolean enabled;

        @JsonProperty("auth-type")
        private String authType;

        @JsonProperty("discord-url")
        private String discordURL;

        private Boolean strict;

        public boolean isEnabled() {
            return enabled;
        }

        public AuthType getAuthType() {
            return AuthType.valueOf(authType.substring(0, 1).toUpperCase() + authType.substring(1));
        }

        public String getDiscordURL() {
            return discordURL;
        }

        public Boolean getStrict() {
            return strict;
        }
    }

    public static class Protect {
        @JsonProperty("unbreakable-core")
        private boolean unbreakableCore;

        @JsonProperty("power-detect")
        private boolean powerDetect;

        public boolean isUnbreakableCore() {
            return unbreakableCore;
        }

        public boolean isPowerDetect() {
            return powerDetect;
        }
    }
}
