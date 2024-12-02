package essential.chat;

public class Config {
    public String chatFormat;
    public StrictConfig strict;
    public BlacklistConfig blacklist;

    static class StrictConfig {
        public Boolean enabled;
        public String language;
    }

    static class BlacklistConfig {
        public Boolean enabled;
        public Boolean regex;
    }
}
