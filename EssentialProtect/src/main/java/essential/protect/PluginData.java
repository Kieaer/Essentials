package essential.protect;

/**
 * Plugin data class for the EssentialProtect plugin.
 * This class is used to store plugin-specific data.
 */
public class PluginData {
    private String[] vpnList;

    /**
     * Default constructor with default values.
     */
    public PluginData() {
        this.vpnList = new String[0];
    }

    /**
     * Get the VPN list.
     *
     * @return The VPN list
     */
    public String[] getVpnList() {
        return vpnList;
    }

    /**
     * Set the VPN list.
     *
     * @param vpnList The VPN list
     */
    public void setVpnList(String[] vpnList) {
        this.vpnList = vpnList;
    }
}