package essentials.internal;

import essentials.external.UTF8Control;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import static essentials.Main.config;
import static essentials.Main.root;

public class Bundle {
    ResourceBundle RESOURCE_BUNDLE;

    public Bundle() {
        try {
            RESOURCE_BUNDLE = root.child("bundle.properties").exists()
                    ? new PropertyResourceBundle(Files.newInputStream(Paths.get(root.child("bundle.properties").path())))
                    : ResourceBundle.getBundle("bundle.bundle", config.locale, new UTF8Control());
        } catch (Exception e) {
            RESOURCE_BUNDLE = ResourceBundle.getBundle("bundle.bundle", Locale.US, new UTF8Control());
        }
    }

    public Bundle(Locale locale) {
        try {
            RESOURCE_BUNDLE = root.child("bundle.properties").exists()
                    ? new PropertyResourceBundle(Files.newInputStream(Paths.get(root.child("bundle.properties").path())))
                    : ResourceBundle.getBundle("bundle.bundle", locale, new UTF8Control());
        } catch (Exception e) {
            RESOURCE_BUNDLE = ResourceBundle.getBundle("bundle.bundle", Locale.US, new UTF8Control());
        }
    }

    public String get(String key, Object... params) {
        try {
            return MessageFormat.format(RESOURCE_BUNDLE.getString(key), params);
        } catch (MissingResourceException e) {
            return null;
        }
    }

    public String get(boolean NotNull, String key, Object... params) {
        try {
            return MessageFormat.format(RESOURCE_BUNDLE.getString(key), params);
        } catch (MissingResourceException e) {
            if (NotNull) {
                ResourceBundle bundle = ResourceBundle.getBundle("bundle.bundle", Locale.US, new UTF8Control());
                return MessageFormat.format(bundle.getString(key), params);
            } else {
                return null;
            }
        }
    }

    public String prefix(String key, Object... params) {
        try {
            return MessageFormat.format(config.prefix() + RESOURCE_BUNDLE.getString(key), params);
        } catch (MissingResourceException e) {
            return null;
        }
    }

    public String prefix(boolean NotNull, String key, Object... params) {
        try {
            return MessageFormat.format(config.prefix() + RESOURCE_BUNDLE.getString(key), params);
        } catch (MissingResourceException e) {
            if (NotNull) {
                ResourceBundle bundle = ResourceBundle.getBundle("bundle.bundle", Locale.US, new UTF8Control());
                return MessageFormat.format(bundle.getString(key), params);
            } else {
                return null;
            }
        }
    }
}
