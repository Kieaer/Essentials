package essential.anti;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Config {
    private boolean vpn;
    private boolean foo;
    private boolean mobile;
    @JsonProperty("steam-only")
    private boolean steamOnly;
    @JsonProperty("minimalName")
    private MinimalNameConfig minimalNameConfig;
    private Boolean strict;
    @JsonProperty("block-new-user")
    private Boolean blockNewUser;

    public boolean isVpn() {
        return vpn;
    }

    public boolean isFoo() {
        return foo;
    }

    public boolean isMobile() {
        return mobile;
    }

    public boolean isSteamOnly() {
        return steamOnly;
    }

    public Boolean getStrict() {
        return strict;
    }

    public Boolean getBlockNewUser() {
        return blockNewUser;
    }

    public MinimalNameConfig getMinimalNameConfig() {
        return minimalNameConfig;
    }

    static class MinimalNameConfig {
        private Boolean enabled;
        private int length;

        public Boolean getEnabled() {
            return enabled;
        }

        public int getLength() {
            return length;
        }
    }
}
