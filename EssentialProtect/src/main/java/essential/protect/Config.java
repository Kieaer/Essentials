package essential.protect;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Config {
    private Pvp pvp;
    private Account account;

    public Pvp getPvp() {
        return pvp;
    }

    public Account getAccount() {
        return account;
    }

    public static class Pvp {
        private Peace peace;
        private Border border;

        @JsonProperty("destroy-core")
        private boolean destroyCore;

        @JsonProperty("unbreakable-core")
        private boolean unbreakableCore;

        public Peace getPeace() {
            return peace;
        }

        public Border getBorder() {
            return border;
        }

        public boolean isDestroyCore() {
            return destroyCore;
        }

        public boolean isUnbreakableCore() {
            return unbreakableCore;
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
        public static enum AuthType {
            None, Password, Discord
        }

        private boolean enabled;

        @JsonProperty("auth-type")
        private AuthType authType;

        public boolean isEnabled() {
            return enabled;
        }

        public AuthType getAuthType() {
            return authType;
        }
    }
}
