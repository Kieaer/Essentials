package essential.anti;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Config {
    private boolean iptables;
    private boolean vpn;
    private boolean foo;
    @JsonProperty("fixed-name")
    private boolean fixedName;
    private boolean mobile;
    @JsonProperty("steam-only")
    private boolean steamOnly;

    public boolean isIptables() {
        return iptables;
    }

    public boolean isVpn() {
        return vpn;
    }

    public boolean isFoo() {
        return foo;
    }

    public boolean isFixedName() {
        return fixedName;
    }

    public boolean isMobile() {
        return mobile;
    }

    public boolean isSteamOnly() {
        return steamOnly;
    }
}
