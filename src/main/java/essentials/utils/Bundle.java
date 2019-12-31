package essentials.utils;

import essentials.special.UTF8Control;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import static essentials.Global.config;
import static essentials.Global.nlog;
import static essentials.core.Log.writelog;

public class Bundle {
    private ResourceBundle RESOURCE_BUNDLE;
    public Bundle(Locale locale){
        try{
            RESOURCE_BUNDLE = ResourceBundle.getBundle("bundle.bundle", locale, new UTF8Control());
        }catch (Exception e){
            RESOURCE_BUNDLE = ResourceBundle.getBundle("bundle.bundle", Locale.ENGLISH, new UTF8Control());
        }
    }

    public String getBundle(String key){
        try {
            return config.getPrefix()+RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            nlog("warn","BUNDLE KEY NOT FOUND - " + key);
            writelog("error", Arrays.toString(e.getStackTrace()));
            return "BUNDLE KEY NOT FOUND - " + key;
        }
    }

    public String getBundle(String key, Object... params){
        try {
            return config.getPrefix()+MessageFormat.format(RESOURCE_BUNDLE.getString(key), params);
        } catch (MissingResourceException e) {
            nlog("warn","BUNDLE KEY NOT FOUND - " + key);
            writelog("error", Arrays.toString(e.getStackTrace()));
            return "BUNDLE KEY NOT FOUND - " + key;
        }
    }

    public String getNormal(String key){
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            nlog("warn","BUNDLE KEY NOT FOUND - " + key);
            writelog("error", Arrays.toString(e.getStackTrace()));
            return "BUNDLE KEY NOT FOUND - " + key;
        }
    }

    public String getNormal(String key, Object... params){
        try {
            return MessageFormat.format(RESOURCE_BUNDLE.getString(key), params);
        } catch (MissingResourceException e) {
            nlog("warn","BUNDLE KEY NOT FOUND - " + key);
            writelog("error", Arrays.toString(e.getStackTrace()));
            return "BUNDLE KEY NOT FOUND - " + key;
        }
    }
}
