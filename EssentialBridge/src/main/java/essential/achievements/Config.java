package essential.achievements;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Config {
    private String address;
    private Integer port;
    @JsonProperty("count-all-servers")
    private boolean countAllServers;
    private SharingConfig sharing;

    public String getAddress() {
        return address;
    }

    public Integer getPort() {
        return port;
    }

    public boolean isCountAllServers() {
        return countAllServers;
    }

    public SharingConfig getSharing() {
        return sharing;
    }

    static class SharingConfig {
        private boolean ban;
        private boolean broadcast;

        public boolean isBan() {
            return ban;
        }

        public boolean isBroadcast() {
            return broadcast;
        }
    }
}
