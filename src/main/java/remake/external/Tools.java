package remake.external;

import mindustry.Vars;
import mindustry.entities.type.Player;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.jsoup.Jsoup;
import remake.internal.Bundle;
import remake.internal.CrashReport;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

import static mindustry.Vars.playerGroup;
import static remake.Main.locale;
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

    public String getTime() {
        return DateTimeFormatter.ofPattern("yy-MM-dd HH:mm.ss").format(LocalDateTime.now());
    }

    public static byte[] encrypt(String data, SecretKeySpec spec, Cipher cipher) throws Exception {
        cipher.init(Cipher.ENCRYPT_MODE, spec);
        return cipher.doFinal(data.getBytes());
    }

    public static byte[] decrypt(byte[] data, SecretKeySpec spec, Cipher cipher) throws Exception {
        cipher.init(Cipher.DECRYPT_MODE, spec);
        return cipher.doFinal(data);
    }

    public static Locale getGeo(Object data) {
        String ip = data instanceof Player ? Vars.netServer.admins.getInfo(((Player) data).uuid).lastIP : (String) data;
        try {
            String json = Jsoup.connect("http://ipapi.co/" + ip + "/json").ignoreContentType(true).execute().body();
            JsonObject result = JsonValue.readJSON(json).asObject();

            if (result.get("reserved") != null) {
                return locale;
            } else {
                Locale loc = locale;
                String lc = result.get("country_code").asString().split(",")[0];
                if (lc.split("_").length == 2) {
                    String[] array = lc.split("_");
                    loc = new Locale(array[0], array[1]);
                }
                try {
                    ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("bundle.bundle", loc, new UTF8Control());
                    RESOURCE_BUNDLE.getString("success");
                } catch (Exception e) {
                    for (int a = 0; a < result.get("country_code").asString().split(",").length; a++) {
                        try {
                            lc = result.get("country_code").asString().split(",")[a];
                            if (lc.split("_").length == 2) {
                                String[] array = lc.split("_");
                                loc = new Locale(array[0], array[1]);
                            }
                            ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("bundle.bundle", loc, new UTF8Control());
                            RESOURCE_BUNDLE.getString("success");
                            return loc;
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        } catch (Exception e) {
            new CrashReport(e);
        }
        return locale;
    }
}
