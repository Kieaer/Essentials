package remake.external;

import java.util.Locale;

public class Tools {
    public Locale TextToLocale(String data) {
        Locale locale = new Locale(data);
        String lc = data;
        if (data.contains(",")) lc = lc.split(",")[0];
        if (lc.split("_").length > 1) {
            String[] array = lc.split("_");
            locale = new Locale(array[0], array[1]);
        }
        return locale;
    }
}
