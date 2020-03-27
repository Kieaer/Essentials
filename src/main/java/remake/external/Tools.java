package remake.external;

import mindustry.entities.type.Player;
import remake.internal.Bundle;

import java.util.Locale;

import static mindustry.Vars.playerGroup;
import static remake.Main.playerDB;

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

    public void sendMessageAll(String value, Object... parameter) {
        for (Player p : playerGroup.all()) p.sendMessage(new Bundle(playerDB.get(p.uuid).locale).get(value, parameter));
    }
}
