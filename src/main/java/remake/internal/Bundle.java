package remake.internal;

import essentials.special.UTF8Control;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import static remake.Main.locale;
import static remake.Main.root;

public class Bundle {
    ResourceBundle RESOURCE_BUNDLE;

    public Bundle() {
        try {
            RESOURCE_BUNDLE = root.child("bundle.properties").exists()
                    ? new PropertyResourceBundle(Files.newInputStream(Paths.get(root.child("bundle.properties").path())))
                    : ResourceBundle.getBundle("bundle.bundle", locale, new UTF8Control());
        } catch (Exception e) {
            RESOURCE_BUNDLE = ResourceBundle.getBundle("bundle.bundle", new Locale("en", "US"), new UTF8Control());
        }
    }

    public Bundle(Locale locale) {
        try {
            RESOURCE_BUNDLE = root.child("bundle.properties").exists()
                    ? new PropertyResourceBundle(Files.newInputStream(Paths.get(root.child("bundle.properties").path())))
                    : ResourceBundle.getBundle("bundle.bundle", locale, new UTF8Control());
        } catch (Exception e) {
            RESOURCE_BUNDLE = ResourceBundle.getBundle("bundle.bundle", new Locale("en", "US"), new UTF8Control());
        }
    }

    public String get(String key, Object... params) {
        try {
            return MessageFormat.format(RESOURCE_BUNDLE.getString(key), params);
        } catch (MissingResourceException e) {
            return null;
        }
    }
}
