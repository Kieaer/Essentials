package remake.external;

import mindustry.Vars;
import mindustry.entities.type.Player;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.jsoup.Jsoup;
import remake.internal.Bundle;
import remake.internal.CrashReport;
import remake.internal.Log;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static mindustry.Vars.playerGroup;
import static remake.Main.*;

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

    public byte[] encrypt(String data, SecretKeySpec spec, Cipher cipher) throws Exception {
        cipher.init(Cipher.ENCRYPT_MODE, spec);
        return cipher.doFinal(data.getBytes());
    }

    public byte[] decrypt(byte[] data, SecretKeySpec spec, Cipher cipher) throws Exception {
        cipher.init(Cipher.DECRYPT_MODE, spec);
        return cipher.doFinal(data);
    }

    public Locale getGeo(Object data) {
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

    public static String getHostIP() {
        try {
            URL whatismyip = new URL("http://checkip.amazonaws.com");
            BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
            return in.readLine();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    public static String getMotd(Locale loc) {
        if (root.child("motd/motd_" + loc.toString() + ".txt").exists()) {
            return root.child("motd/motd_" + loc.toString() + ".txt").readString();
        } else {
            return root.child("motd/motd_" + locale.toString() + ".txt").readString();
        }
    }

    // TODO discord/in-game 합치기
    public boolean checkPassword(Player player, String id, String password, String password_repeat) {
        // 영문(소문자), 숫자, 7~20자리
        String pwPattern = "^(?=.*\\d)(?=.*[a-z]).{7,20}$";
        Matcher matcher = Pattern.compile(pwPattern).matcher(password);

        // 같은 문자 4개이상 사용 불가
        pwPattern = "(.)\\1\\1\\1";
        Matcher matcher2 = Pattern.compile(pwPattern).matcher(password);

        if (!password.equals(password_repeat)) {
            // 비밀번호가 비밀번호 재확인 문자열과 똑같지 않을경우
            player.sendMessage("[green][Essentials] [sky]The password isn't the same.\n" +
                    "[green][Essentials] [sky]비밀번호가 똑같지 않습니다.");
            return false;
        } else if (!matcher.matches()) {
            // 정규식에 맞지 않을경우
            player.sendMessage("[green][Essentials] [sky]The password should be 7 ~ 20 letters long and contain alphanumeric characters and special characters!\n" +
                    "[green][Essentials] [sky]비밀번호는 7~20자 내외로 설정해야 하며, 영문과 숫자를 포함해야 합니다!");
            Log.player("password-match-regex", player.name);
            return false;
        } else if (matcher2.find()) {
            // 비밀번호에 ID에 사용된 같은 문자가 4개 이상일경우
            player.sendMessage("[green][Essentials] [sky]Passwords should not be similar to nicknames!\n" +
                    "[green][Essentials] [sky]비밀번호는 닉네임과 비슷하면 안됩니다!");
            Log.player("password-match-name", player.name);
            return false;
        } else if (password.contains(id)) {
            // 비밀번호와 ID가 완전히 같은경우
            player.sendMessage("[green][Essentials] [sky]Password shouldn't be the same as your nickname.\n" +
                    "[green][Essentials] [sky]비밀번호는 ID와 비슷하게 설정할 수 없습니다!");
            return false;
        } else if (password.contains(" ")) {
            // 비밀번호에 공백이 있을경우
            player.sendMessage("[green][Essentials] [sky]Password must not contain spaces!\n" +
                    "[green][Essentials] [sky]비밀번호에는 공백이 있으면 안됩니다!");
            Log.player("password-match-blank", player.name);
            return false;
        } else if (password.matches("<(.*?)>")) {
            // 비밀번호 형식이 "<비밀번호>" 일경우
            player.sendMessage("[green][Essentials] [green]<[sky]password[green]>[sky] format isn't allowed!\n" +
                    "[green][Essentials] [sky]Use /register password\n" +
                    "[green][Essentials] [green]<[sky]비밀번호[green]>[sky] 형식은 허용되지 않습니다!\n" +
                    "[green][Essentials] [sky]/register password 형식으로 사용하세요.");
            Log.player("password-match-invalid", player.name);
            return false;
        }
        return true;
    }
}
