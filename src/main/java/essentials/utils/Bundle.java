package essentials.utils;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Bundle {
    private ResourceBundle RESOURCE_BUNDLE;
    public Bundle(Locale locale){
        try{
            RESOURCE_BUNDLE = ResourceBundle.getBundle("bundle.bundle", locale);
        }catch (Exception e){
            RESOURCE_BUNDLE = ResourceBundle.getBundle("bundle.bundle");
        }
    }

    public String getBundle(String key){
        try {
            return "[green][Essentials][] "+RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    public String getBundle(String key, Object... params){
        try {
            return "[green][Essentials][] "+MessageFormat.format(RESOURCE_BUNDLE.getString(key), params);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    public String getNormal(String key){
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    public String getNormal(String key, Object... params){
        try {
            return MessageFormat.format(RESOURCE_BUNDLE.getString(key), params);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }
}
