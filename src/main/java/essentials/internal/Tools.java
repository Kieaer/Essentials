package essentials.internal;

import arc.files.Fi;
import arc.struct.Array;
import arc.struct.ObjectMap;
import essentials.core.player.PlayerData;
import mindustry.content.Blocks;
import mindustry.entities.type.Player;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import org.hjson.JsonObject;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static essentials.Main.*;
import static mindustry.Vars.*;
import static org.hjson.JsonValue.readJSON;

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
        for (Player p : playerGroup.all()) {
            PlayerData playerData = playerDB.get(p.uuid);
            if (!playerData.error()) {
                p.sendMessage(new Bundle(playerData.locale()).prefix(value, parameter));
            }
        }
    }

    public String getTime() {
        return DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss").format(LocalDateTime.now());
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
        String ip = data instanceof Player ? netServer.admins.getInfo(((Player) data).uuid).lastIP : (String) data;
        Locale loc = Locale.US;
        JsonObject result = readJSON(getWebContent("https://ipapi.co/" + ip + "/json")).asObject();

        if (result.get("reserved") != null) {
            return config.locale;
        } else {
            String lc = result.get("languages").asString().split(",")[0];

            if (lc.split("-").length == 2) {
                String[] array = lc.split("-");
                loc = new Locale(array[0], array[1]);

                if (array[0].equals("zh")) {
                    return Locale.SIMPLIFIED_CHINESE;
                }
            }

            // TODO Bundle 검증 다시 만들기
            /*try {
                ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("bundle.bundle", loc, new UTF8Control());
                RESOURCE_BUNDLE.getString("success");
            } catch (Exception e) {
                for (int a = 0; a < result.get("country_code").asString().split(",").length; a++) {
                    try {
                        lc = result.get("country_code").asString().split(",")[a];
                        if (lc.split("-").length == 2) {
                            String[] array = lc.split("-");
                            loc = new Locale(array[0], array[1]);
                        }
                        ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("bundle.bundle", loc, new UTF8Control());
                        RESOURCE_BUNDLE.getString("success");
                        return loc;
                    } catch (Exception ignored) {
                    }
                }
            }*/
        }
        return loc;
    }

    public String getHostIP() {
        BufferedReader in = null;
        try {
            URL whatismyip = new URL("http://checkip.amazonaws.com");
            in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
            return in.readLine();
        } catch (Exception e) {
            return "127.0.0.1";
        } finally {
            if (in != null) try {
                in.close();
            } catch (IOException ignored) {
            }
        }
    }

    public String getMotd(Locale loc) {
        if (root.child("motd/" + loc.toString() + ".txt").exists()) {
            return root.child("motd/" + loc.toString() + ".txt").readString();
        } else {
            Fi file = root.child("motd/" + config.locale.toString() + ".txt");
            return file.exists() ? file.readString() : "Welcome to the server!";
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
            Log.player("system.password.match.regex", player.name);
            return false;
        } else if (matcher2.find()) {
            // 비밀번호에 ID에 사용된 같은 문자가 4개 이상일경우
            player.sendMessage("[green][Essentials] [sky]Passwords should not be similar to nicknames!\n" +
                    "[green][Essentials] [sky]비밀번호는 닉네임과 비슷하면 안됩니다!");
            Log.player("system.password.match.name", player.name);
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
            Log.player("system.password.match.blank", player.name);
            return false;
        } else if (password.matches("<(.*?)>")) {
            // 비밀번호 형식이 "<비밀번호>" 일경우
            player.sendMessage("[green][Essentials] [green]<[sky]password[green]>[sky] format isn't allowed!\n" +
                    "[green][Essentials] [sky]Use /register password\n" +
                    "[green][Essentials] [green]<[sky]비밀번호[green]>[sky] 형식은 허용되지 않습니다!\n" +
                    "[green][Essentials] [sky]/register password 형식으로 사용하세요.");
            Log.player("system.password.match.invalid", player.name);
            return false;
        }
        return true;
    }

    public void setTileText(Tile tile, Block block, String text) {
        ObjectMap<String, int[]> letters = new ObjectMap<>();
        letters.put("A", new int[]{0, 1, 1, 1, 1, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 1, 1});
        letters.put("B", new int[]{1, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0});
        letters.put("C", new int[]{0, 1, 1, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1});
        letters.put("D", new int[]{1, 1, 1, 1, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 1, 1, 1, 0});
        letters.put("E", new int[]{1, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1});
        letters.put("F", new int[]{1, 1, 1, 1, 1, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0});
        letters.put("G", new int[]{0, 1, 1, 1, 0, 1, 0, 0, 0, 1, 1, 0, 1, 0, 1, 0, 0, 1, 1, 1});
        letters.put("H", new int[]{1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1});
        letters.put("I", new int[]{1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1});
        letters.put("J", new int[]{1, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 1, 1, 1, 0, 1, 0, 0, 0, 0});
        letters.put("K", new int[]{1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1});
        letters.put("L", new int[]{1, 1, 1, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1});
        letters.put("M", new int[]{1, 1, 1, 1, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 1, 1, 1, 1});
        letters.put("N", new int[]{1, 1, 1, 1, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1});
        letters.put("O", new int[]{0, 1, 1, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 1, 1, 1, 0});
        letters.put("P", new int[]{1, 1, 1, 1, 1, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0});
        letters.put("Q", new int[]{0, 1, 1, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 1, 1, 1, 0, 0, 0, 0, 0, 1});
        letters.put("R", new int[]{1, 1, 1, 1, 1, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 1});
        letters.put("S", new int[]{1, 1, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 1, 1, 1});
        letters.put("T", new int[]{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0});
        letters.put("U", new int[]{1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0});
        letters.put("V", new int[]{1, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 1, 1, 0, 0});
        letters.put("W", new int[]{1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0});
        letters.put("X", new int[]{1, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1});
        letters.put("Y", new int[]{1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0});
        letters.put("Z", new int[]{1, 0, 0, 1, 1, 1, 0, 1, 0, 1, 1, 1, 0, 0, 1});

        letters.put("0", new int[]{1, 1, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1});
        letters.put("1", new int[]{0, 1, 0, 1, 1, 0, 0, 1, 0, 0, 1, 0, 1, 1, 1});
        letters.put("2", new int[]{1, 1, 1, 0, 0, 1, 1, 1, 1, 1, 0, 0, 1, 1, 1});
        letters.put("3", new int[]{1, 1, 1, 0, 0, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1});
        letters.put("4", new int[]{1, 0, 1, 1, 0, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1});
        letters.put("5", new int[]{1, 1, 1, 1, 0, 0, 1, 1, 1, 0, 0, 1, 1, 1, 1});
        letters.put("6", new int[]{1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 0, 1, 1, 1, 1});
        letters.put("7", new int[]{1, 1, 1, 1, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1});
        letters.put("8", new int[]{1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1});
        letters.put("9", new int[]{1, 1, 1, 1, 0, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1});

        letters.put("!", new int[]{1, 1, 1, 1, 0, 1});
        letters.put("?", new int[]{0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 0, 0, 0});

        letters.put(" ", new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0});

        String[] texts = text.split("");

        for (String txt : texts) {
            Array<int[]> pos = new Array<>();
            int[] target = letters.get(txt.toUpperCase());
            int xv = 0;
            int yv = 0;
            switch (target.length) {
                case 20:
                    xv = 5;
                    yv = 4;
                    break;
                case 15:
                    xv = 5;
                    yv = 3;
                    break;
                case 18:
                    xv = 6;
                    yv = 3;
                    break;
                case 25:
                    xv = 5;
                    yv = 5;
                    break;
                case 6:
                    xv = 6;
                    yv = 1;
                    break;
                case 10:
                    xv = 2;
                    yv = 5;
                    break;
            }

            for (int y = 0; y < yv; y++) {
                for (int x = 0; x < xv; x++) {
                    pos.add(new int[]{y, -x});
                }
            }

            for (int a = 0; a < pos.size; a++) {
                Tile target_tile = world.tile(tile.x + pos.get(a)[0], tile.y + pos.get(a)[1]);
                if (target[a] == 1) {
                    Call.onConstructFinish(target_tile, block, 100, (byte) 0, Team.sharded, false);
                } else if (target_tile.getTeam() != null) {
                    Call.onDeconstructFinish(target_tile, Blocks.air, 100);
                }
            }

            tile = world.tile(tile.x + (xv + 1), tile.y);
        }
    }

    public Player findPlayer(String name) {
        return playerGroup.find(p -> p.name.equals(name));
    }

    public Team getTeamByName(String name) {
        for (Team t : Team.all()) {
            if (t.name.equals(name)) {
                return t;
            }
        }
        return null;
    }

    public UnitType getUnitByName(String name) {
        return content.units().find(unitType -> unitType.name.equals(name));
    }

    public String getWebContent(String url) {
        try {
            Scanner sc = new Scanner(new URL(url).openStream());
            StringBuilder sb = new StringBuilder();
            while (sc.hasNext()) {
                sb.append(sc.next());
            }
            return sb.toString();
        } catch (IOException e) {
            new CrashReport(e);
        }
        return null;
    }
}
