package essential.bridge;

/**
 * Configuration class for the EssentialBridge plugin.
 * This class is used to store and manage the bridge configuration.
 */
public class BridgeConfig {
    private String address;
    private int port;
    private SharingConfig sharing;

    /**
     * Default constructor with default values.
     */
    public BridgeConfig() {
        this.address = "127.0.0.1";
        this.port = (int) (Math.random() * (65535 - 10000) + 10000); // Random port between 10000 and 65535
        this.sharing = new SharingConfig();
    }

    /**
     * Constructor with all parameters.
     *
     * @param address The server address
     * @param port The server port
     * @param sharing The sharing configuration
     */
    public BridgeConfig(String address, int port, SharingConfig sharing) {
        this.address = address;
        this.port = port;
        this.sharing = sharing;
    }

    /**
     * Get the server address.
     *
     * @return The server address
     */
    public String getAddress() {
        return address;
    }

    /**
     * Set the server address.
     *
     * @param address The server address
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Get the server port.
     *
     * @return The server port
     */
    public int getPort() {
        return port;
    }

    /**
     * Set the server port.
     *
     * @param port The server port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Get the sharing configuration.
     *
     * @return The sharing configuration
     */
    public SharingConfig getSharing() {
        return sharing;
    }

    /**
     * Set the sharing configuration.
     *
     * @param sharing The sharing configuration
     */
    public void setSharing(SharingConfig sharing) {
        this.sharing = sharing;
    }

    /**
     * Configuration class for sharing settings between servers.
     */
    public static class SharingConfig {
        private boolean ban;
        private boolean broadcast;

        /**
         * Default constructor with default values.
         */
        public SharingConfig() {
            this.ban = false;
            this.broadcast = false;
        }

        /**
         * Constructor with all parameters.
         *
         * @param ban Whether to share ban lists
         * @param broadcast Whether to share broadcast messages
         */
        public SharingConfig(boolean ban, boolean broadcast) {
            this.ban = ban;
            this.broadcast = broadcast;
        }

        /**
         * Get whether to share ban lists.
         *
         * @return Whether to share ban lists
         */
        public boolean isBan() {
            return ban;
        }

        /**
         * Set whether to share ban lists.
         *
         * @param ban Whether to share ban lists
         */
        public void setBan(boolean ban) {
            this.ban = ban;
        }

        /**
         * Get whether to share broadcast messages.
         *
         * @return Whether to share broadcast messages
         */
        public boolean isBroadcast() {
            return broadcast;
        }

        /**
         * Set whether to share broadcast messages.
         *
         * @param broadcast Whether to share broadcast messages
         */
        public void setBroadcast(boolean broadcast) {
            this.broadcast = broadcast;
        }
    }
}