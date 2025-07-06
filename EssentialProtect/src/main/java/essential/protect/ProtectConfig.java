package essential.protect;

import java.util.Locale;

/**
 * Configuration class for the EssentialProtect plugin.
 * This class is used to store and manage the protection configuration.
 */
public class ProtectConfig {
    private Pvp pvp;
    private Account account;
    private Protect protect;
    private Rules rules;

    /**
     * Default constructor with default values.
     */
    public ProtectConfig() {
        this.pvp = new Pvp();
        this.account = new Account();
        this.protect = new Protect();
        this.rules = new Rules();
    }

    /**
     * Constructor with all parameters.
     *
     * @param pvp The PVP configuration
     * @param account The account configuration
     * @param protect The protection configuration
     * @param rules The rules configuration
     */
    public ProtectConfig(Pvp pvp, Account account, Protect protect, Rules rules) {
        this.pvp = pvp;
        this.account = account;
        this.protect = protect;
        this.rules = rules;
    }

    /**
     * Get the PVP configuration.
     *
     * @return The PVP configuration
     */
    public Pvp getPvp() {
        return pvp;
    }

    /**
     * Set the PVP configuration.
     *
     * @param pvp The PVP configuration
     */
    public void setPvp(Pvp pvp) {
        this.pvp = pvp;
    }

    /**
     * Get the account configuration.
     *
     * @return The account configuration
     */
    public Account getAccount() {
        return account;
    }

    /**
     * Set the account configuration.
     *
     * @param account The account configuration
     */
    public void setAccount(Account account) {
        this.account = account;
    }

    /**
     * Get the protection configuration.
     *
     * @return The protection configuration
     */
    public Protect getProtect() {
        return protect;
    }

    /**
     * Set the protection configuration.
     *
     * @param protect The protection configuration
     */
    public void setProtect(Protect protect) {
        this.protect = protect;
    }

    /**
     * Get the rules configuration.
     *
     * @return The rules configuration
     */
    public Rules getRules() {
        return rules;
    }

    /**
     * Set the rules configuration.
     *
     * @param rules The rules configuration
     */
    public void setRules(Rules rules) {
        this.rules = rules;
    }

    /**
     * PVP configuration class.
     */
    public static class Pvp {
        private Peace peace;
        private Border border;
        private boolean destroyCore;

        /**
         * Default constructor with default values.
         */
        public Pvp() {
            this.peace = new Peace();
            this.border = new Border();
            this.destroyCore = false;
        }

        /**
         * Constructor with all parameters.
         *
         * @param peace The peace configuration
         * @param border The border configuration
         * @param destroyCore Whether to destroy the core
         */
        public Pvp(Peace peace, Border border, boolean destroyCore) {
            this.peace = peace;
            this.border = border;
            this.destroyCore = destroyCore;
        }

        /**
         * Get the peace configuration.
         *
         * @return The peace configuration
         */
        public Peace getPeace() {
            return peace;
        }

        /**
         * Set the peace configuration.
         *
         * @param peace The peace configuration
         */
        public void setPeace(Peace peace) {
            this.peace = peace;
        }

        /**
         * Get the border configuration.
         *
         * @return The border configuration
         */
        public Border getBorder() {
            return border;
        }

        /**
         * Set the border configuration.
         *
         * @param border The border configuration
         */
        public void setBorder(Border border) {
            this.border = border;
        }

        /**
         * Get whether to destroy the core.
         *
         * @return Whether to destroy the core
         */
        public boolean isDestroyCore() {
            return destroyCore;
        }

        /**
         * Set whether to destroy the core.
         *
         * @param destroyCore Whether to destroy the core
         */
        public void setDestroyCore(boolean destroyCore) {
            this.destroyCore = destroyCore;
        }

        /**
         * Peace configuration class.
         */
        public static class Peace {
            private boolean enabled;
            private int time;

            /**
             * Default constructor with default values.
             */
            public Peace() {
                this.enabled = false;
                this.time = 0;
            }

            /**
             * Constructor with all parameters.
             *
             * @param enabled Whether peace is enabled
             * @param time The peace time
             */
            public Peace(boolean enabled, int time) {
                this.enabled = enabled;
                this.time = time;
            }

            /**
             * Get whether peace is enabled.
             *
             * @return Whether peace is enabled
             */
            public boolean isEnabled() {
                return enabled;
            }

            /**
             * Set whether peace is enabled.
             *
             * @param enabled Whether peace is enabled
             */
            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            /**
             * Get the peace time.
             *
             * @return The peace time
             */
            public int getTime() {
                return time;
            }

            /**
             * Set the peace time.
             *
             * @param time The peace time
             */
            public void setTime(int time) {
                this.time = time;
            }
        }

        /**
         * Border configuration class.
         */
        public static class Border {
            private boolean enabled;

            /**
             * Default constructor with default values.
             */
            public Border() {
                this.enabled = false;
            }

            /**
             * Constructor with all parameters.
             *
             * @param enabled Whether the border is enabled
             */
            public Border(boolean enabled) {
                this.enabled = enabled;
            }

            /**
             * Get whether the border is enabled.
             *
             * @return Whether the border is enabled
             */
            public boolean isEnabled() {
                return enabled;
            }

            /**
             * Set whether the border is enabled.
             *
             * @param enabled Whether the border is enabled
             */
            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
        }
    }

    /**
     * Authentication type enum.
     */
    public enum AuthType {
        None, Password, Discord;
    }

    /**
     * Account configuration class.
     */
    public static class Account {
        private boolean enabled;
        private String authType;
        private String discordURL;

        /**
         * Default constructor with default values.
         */
        public Account() {
            this.enabled = false;
            this.authType = AuthType.None.name();
            this.discordURL = "";
        }

        /**
         * Constructor with all parameters.
         *
         * @param enabled Whether the account is enabled
         * @param authType The authentication type
         * @param discordURL The Discord URL
         */
        public Account(boolean enabled, String authType, String discordURL) {
            this.enabled = enabled;
            this.authType = authType;
            this.discordURL = discordURL;
        }

        /**
         * Get whether the account is enabled.
         *
         * @return Whether the account is enabled
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Set whether the account is enabled.
         *
         * @param enabled Whether the account is enabled
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Get the authentication type string.
         *
         * @return The authentication type string
         */
        public String getAuthTypeString() {
            return authType;
        }

        /**
         * Set the authentication type string.
         *
         * @param authType The authentication type string
         */
        public void setAuthTypeString(String authType) {
            this.authType = authType;
        }

        /**
         * Get the authentication type.
         *
         * @return The authentication type
         */
        public AuthType getAuthType() {
            return AuthType.valueOf(authType.substring(0, 1).toUpperCase(Locale.getDefault()) + authType.substring(1));
        }

        /**
         * Get the Discord URL.
         *
         * @return The Discord URL
         */
        public String getDiscordURL() {
            return discordURL;
        }

        /**
         * Set the Discord URL.
         *
         * @param discordURL The Discord URL
         */
        public void setDiscordURL(String discordURL) {
            this.discordURL = discordURL;
        }
    }

    /**
     * Protection configuration class.
     */
    public static class Protect {
        private boolean unbreakableCore;
        private boolean powerDetect;

        /**
         * Default constructor with default values.
         */
        public Protect() {
            this.unbreakableCore = false;
            this.powerDetect = false;
        }

        /**
         * Constructor with all parameters.
         *
         * @param unbreakableCore Whether the core is unbreakable
         * @param powerDetect Whether to detect power
         */
        public Protect(boolean unbreakableCore, boolean powerDetect) {
            this.unbreakableCore = unbreakableCore;
            this.powerDetect = powerDetect;
        }

        /**
         * Get whether the core is unbreakable.
         *
         * @return Whether the core is unbreakable
         */
        public boolean isUnbreakableCore() {
            return unbreakableCore;
        }

        /**
         * Set whether the core is unbreakable.
         *
         * @param unbreakableCore Whether the core is unbreakable
         */
        public void setUnbreakableCore(boolean unbreakableCore) {
            this.unbreakableCore = unbreakableCore;
        }

        /**
         * Get whether to detect power.
         *
         * @return Whether to detect power
         */
        public boolean isPowerDetect() {
            return powerDetect;
        }

        /**
         * Set whether to detect power.
         *
         * @param powerDetect Whether to detect power
         */
        public void setPowerDetect(boolean powerDetect) {
            this.powerDetect = powerDetect;
        }
    }

    /**
     * Rules configuration class.
     */
    public static class Rules {
        private boolean vpn;
        private boolean foo;
        private boolean mobile;
        private boolean steamOnly;
        private MinimalNameConfig minimalName;
        private boolean strict;
        private boolean blockNewUser;

        /**
         * Default constructor with default values.
         */
        public Rules() {
            this.vpn = false;
            this.foo = false;
            this.mobile = false;
            this.steamOnly = false;
            this.minimalName = new MinimalNameConfig();
            this.strict = false;
            this.blockNewUser = false;
        }

        /**
         * Constructor with all parameters.
         *
         * @param vpn Whether to check for VPNs
         * @param foo Whether to check for foo
         * @param mobile Whether to check for mobile
         * @param steamOnly Whether to only allow Steam users
         * @param minimalName The minimal name configuration
         * @param strict Whether to use strict mode
         * @param blockNewUser Whether to block new users
         */
        public Rules(boolean vpn, boolean foo, boolean mobile, boolean steamOnly, MinimalNameConfig minimalName, boolean strict, boolean blockNewUser) {
            this.vpn = vpn;
            this.foo = foo;
            this.mobile = mobile;
            this.steamOnly = steamOnly;
            this.minimalName = minimalName;
            this.strict = strict;
            this.blockNewUser = blockNewUser;
        }

        /**
         * Get whether to check for VPNs.
         *
         * @return Whether to check for VPNs
         */
        public boolean isVpn() {
            return vpn;
        }

        /**
         * Set whether to check for VPNs.
         *
         * @param vpn Whether to check for VPNs
         */
        public void setVpn(boolean vpn) {
            this.vpn = vpn;
        }

        /**
         * Get whether to check for foo.
         *
         * @return Whether to check for foo
         */
        public boolean isFoo() {
            return foo;
        }

        /**
         * Set whether to check for foo.
         *
         * @param foo Whether to check for foo
         */
        public void setFoo(boolean foo) {
            this.foo = foo;
        }

        /**
         * Get whether to check for mobile.
         *
         * @return Whether to check for mobile
         */
        public boolean isMobile() {
            return mobile;
        }

        /**
         * Set whether to check for mobile.
         *
         * @param mobile Whether to check for mobile
         */
        public void setMobile(boolean mobile) {
            this.mobile = mobile;
        }

        /**
         * Get whether to only allow Steam users.
         *
         * @return Whether to only allow Steam users
         */
        public boolean isSteamOnly() {
            return steamOnly;
        }

        /**
         * Set whether to only allow Steam users.
         *
         * @param steamOnly Whether to only allow Steam users
         */
        public void setSteamOnly(boolean steamOnly) {
            this.steamOnly = steamOnly;
        }

        /**
         * Get the minimal name configuration.
         *
         * @return The minimal name configuration
         */
        public MinimalNameConfig getMinimalName() {
            return minimalName;
        }

        /**
         * Set the minimal name configuration.
         *
         * @param minimalName The minimal name configuration
         */
        public void setMinimalName(MinimalNameConfig minimalName) {
            this.minimalName = minimalName;
        }

        /**
         * Get whether to use strict mode.
         *
         * @return Whether to use strict mode
         */
        public boolean isStrict() {
            return strict;
        }

        /**
         * Set whether to use strict mode.
         *
         * @param strict Whether to use strict mode
         */
        public void setStrict(boolean strict) {
            this.strict = strict;
        }

        /**
         * Get whether to block new users.
         *
         * @return Whether to block new users
         */
        public boolean isBlockNewUser() {
            return blockNewUser;
        }

        /**
         * Set whether to block new users.
         *
         * @param blockNewUser Whether to block new users
         */
        public void setBlockNewUser(boolean blockNewUser) {
            this.blockNewUser = blockNewUser;
        }

        /**
         * Minimal name configuration class.
         */
        public static class MinimalNameConfig {
            private boolean enabled;
            private int length;

            /**
             * Default constructor with default values.
             */
            public MinimalNameConfig() {
                this.enabled = false;
                this.length = 0;
            }

            /**
             * Constructor with all parameters.
             *
             * @param enabled Whether minimal name checking is enabled
             * @param length The minimal name length
             */
            public MinimalNameConfig(boolean enabled, int length) {
                this.enabled = enabled;
                this.length = length;
            }

            /**
             * Get whether minimal name checking is enabled.
             *
             * @return Whether minimal name checking is enabled
             */
            public boolean isEnabled() {
                return enabled;
            }

            /**
             * Set whether minimal name checking is enabled.
             *
             * @param enabled Whether minimal name checking is enabled
             */
            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            /**
             * Get the minimal name length.
             *
             * @return The minimal name length
             */
            public int getLength() {
                return length;
            }

            /**
             * Set the minimal name length.
             *
             * @param length The minimal name length
             */
            public void setLength(int length) {
                this.length = length;
            }
        }
    }
}