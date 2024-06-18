package essential.chat;

public class Config {
    private StrictConfig strict;
    private BlacklistConfig blacklist;

    public StrictConfig getStrict() {
        return strict;
    }

    public BlacklistConfig getBlacklist() {
        return blacklist;
    }

    static class StrictConfig {
        private Boolean enabled;
        private String language;

        public Boolean getEnabled() {
            return enabled;
        }

        public String getLanguage() {
            return language;
        }
    }

    static class BlacklistConfig {
        private Boolean enabled;
        private Boolean regex;

        public Boolean getEnabled() {
            return enabled;
        }

        public Boolean getRegex() {
            return regex;
        }
    }
}
