package essentials.utils;

import java.util.Locale;
import java.util.ResourceBundle;

public class Bundle {
    public static String load(boolean lang, String value){
        if(lang){
            return "[green][Essentials][] "+ResourceBundle.getBundle("bundle.bundle").getString(value);
        } else {
            return "[green][Essentials][] "+ResourceBundle.getBundle("bundle.bundle", Locale.ENGLISH).getString(value);
        }
    }

    public static String nload(boolean lang, String value){
        if(lang){
            return ResourceBundle.getBundle("bundle.bundle").getString(value);
        } else {
            return ResourceBundle.getBundle("bundle.bundle", Locale.ENGLISH).getString(value);
        }
    }
}
