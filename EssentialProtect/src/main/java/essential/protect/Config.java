package essential.protect;

public class Config {
    public Pvp pvp;
    public Account account;
    public Protect protect;
    public Rules rules;

    public static class Pvp {
        public Peace peace;
        public Border border;
        public boolean destroyCore;

        public static class Peace {
            public boolean enabled;
            public int time;
        }

        public static class Border {
            public boolean enabled;
        }
    }

    public static class Account {
        public enum AuthType {
            None, Password, Discord
        }
        public boolean enabled;
        private String authType;
        public String discordURL;
        public Boolean strict;

        public AuthType getAuthType() {
            return AuthType.valueOf(authType.substring(0, 1).toUpperCase() + authType.substring(1));
        }
    }

    public static class Protect {
        public boolean unbreakableCore;
        public boolean powerDetect;
    }

    public static class Rules {
        public boolean vpn;
        public boolean foo;
        public boolean mobile;
        public boolean steamOnly;
        public MinimalNameConfig minimalName;
        public Boolean strict;
        public Boolean blockNewUser;

        static class MinimalNameConfig {
            public Boolean enabled;
            public int length;
        }
    }
}
