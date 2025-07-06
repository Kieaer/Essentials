package essential.chat;

import java.util.Locale;

/**
 * Configuration class for the EssentialChat plugin.
 * This class is used to store and manage the chat configuration.
 */
public class ChatConfig {
    private String chatFormat;
    private StrictConfig strict;
    private BlacklistConfig blacklist;

    /**
     * Default constructor with default values.
     */
    public ChatConfig() {
        this.chatFormat = "";
        this.strict = new StrictConfig();
        this.blacklist = new BlacklistConfig();
    }

    /**
     * Constructor with all parameters.
     *
     * @param chatFormat The chat format
     * @param strict The strict configuration
     * @param blacklist The blacklist configuration
     */
    public ChatConfig(String chatFormat, StrictConfig strict, BlacklistConfig blacklist) {
        this.chatFormat = chatFormat;
        this.strict = strict;
        this.blacklist = blacklist;
    }

    /**
     * Get the chat format.
     *
     * @return The chat format
     */
    public String getChatFormat() {
        return chatFormat;
    }

    /**
     * Set the chat format.
     *
     * @param chatFormat The chat format
     */
    public void setChatFormat(String chatFormat) {
        this.chatFormat = chatFormat;
    }

    /**
     * Get the strict configuration.
     *
     * @return The strict configuration
     */
    public StrictConfig getStrict() {
        return strict;
    }

    /**
     * Set the strict configuration.
     *
     * @param strict The strict configuration
     */
    public void setStrict(StrictConfig strict) {
        this.strict = strict;
    }

    /**
     * Get the blacklist configuration.
     *
     * @return The blacklist configuration
     */
    public BlacklistConfig getBlacklist() {
        return blacklist;
    }

    /**
     * Set the blacklist configuration.
     *
     * @param blacklist The blacklist configuration
     */
    public void setBlacklist(BlacklistConfig blacklist) {
        this.blacklist = blacklist;
    }

    /**
     * Configuration class for strict mode.
     */
    public static class StrictConfig {
        private boolean enabled;
        private String language;

        /**
         * Default constructor with default values.
         */
        public StrictConfig() {
            this.enabled = false;
            this.language = Locale.getDefault().toLanguageTag();
        }

        /**
         * Constructor with all parameters.
         *
         * @param enabled Whether strict mode is enabled
         * @param language The language for strict mode
         */
        public StrictConfig(boolean enabled, String language) {
            this.enabled = enabled;
            this.language = language;
        }

        /**
         * Get whether strict mode is enabled.
         *
         * @return Whether strict mode is enabled
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Set whether strict mode is enabled.
         *
         * @param enabled Whether strict mode is enabled
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Get the language for strict mode.
         *
         * @return The language for strict mode
         */
        public String getLanguage() {
            return language;
        }

        /**
         * Set the language for strict mode.
         *
         * @param language The language for strict mode
         */
        public void setLanguage(String language) {
            this.language = language;
        }
    }

    /**
     * Configuration class for blacklist.
     */
    public static class BlacklistConfig {
        private boolean enabled;
        private boolean regex;

        /**
         * Default constructor with default values.
         */
        public BlacklistConfig() {
            this.enabled = false;
            this.regex = false;
        }

        /**
         * Constructor with all parameters.
         *
         * @param enabled Whether blacklist is enabled
         * @param regex Whether to use regex for blacklist
         */
        public BlacklistConfig(boolean enabled, boolean regex) {
            this.enabled = enabled;
            this.regex = regex;
        }

        /**
         * Get whether blacklist is enabled.
         *
         * @return Whether blacklist is enabled
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Set whether blacklist is enabled.
         *
         * @param enabled Whether blacklist is enabled
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Get whether to use regex for blacklist.
         *
         * @return Whether to use regex for blacklist
         */
        public boolean isRegex() {
            return regex;
        }

        /**
         * Set whether to use regex for blacklist.
         *
         * @param regex Whether to use regex for blacklist
         */
        public void setRegex(boolean regex) {
            this.regex = regex;
        }
    }
}