package essential.discord;

/**
 * Configuration class for the EssentialDiscord plugin.
 * This class is used to store and manage the Discord configuration.
 */
public class DiscordConfig {
    private String url;

    /**
     * Default constructor with default values.
     */
    public DiscordConfig() {
        this.url = "";
    }

    /**
     * Constructor with all parameters.
     *
     * @param url The Discord server URL
     */
    public DiscordConfig(String url) {
        this.url = url;
    }

    /**
     * Get the Discord server URL.
     *
     * @return The Discord server URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Set the Discord server URL.
     *
     * @param url The Discord server URL
     */
    public void setUrl(String url) {
        this.url = url;
    }
}