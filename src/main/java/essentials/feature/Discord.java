package essentials.feature;

import arc.struct.ObjectMap;
import essentials.core.player.PlayerData;
import essentials.internal.Bundle;
import essentials.internal.CrashReport;
import essentials.internal.Log;
import mindustry.entities.type.Player;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.mindrot.jbcrypt.BCrypt;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static essentials.Main.*;
import static mindustry.Vars.playerGroup;

public class Discord extends ListenerAdapter {
    static ObjectMap<String, Integer> pins = new ObjectMap<>();
    public JDA jda;
    private MessageReceivedEvent event;

    public void start() {
        // TODO discord 방식 변경
        try {
            jda = new JDABuilder(config.discordtoken()).build();
            jda.awaitReady();
            jda.addEventListener(new Discord());
            Log.info("system.discord.enabled");
        } catch (LoginException e) {
            Log.err("system.discord.error");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void queue(Player player) {
        PlayerData playerData = playerDB.get(player.uuid);
        Bundle bundle = new Bundle(playerData.error() ? locale : playerData.locale());
        int pin = new Random().nextInt(9999);
        pins.put(player.name, pin);
        player.sendMessage(bundle.prefix(true, "discord-pin-queue", pin));
    }

    @Override
    public void onGuildMemberJoin(@Nonnull GuildMemberJoinEvent e) {
        send("Use the ``!signup <PIN>`` command to register the server.");
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent e) {
        event = e;

        if (e.isFromType(ChannelType.PRIVATE) && !e.getAuthor().isBot()) {
            String msg = e.getMessage().getContentRaw();
            String[] arr = msg.split(" ");
            switch (arr[0]) {
                case "!signup":
                    send("Array length: " + arr.length);
                    if (arr.length == 3) {
                        send("length match true");
                        send("pins length: " + pins.size);
                        for (ObjectMap.Entry<String, Integer> data : pins.entries()) {
                            String name = data.key;
                            send("name: " + name);
                            if (data.value == Integer.parseInt(arr[1])) {
                                String pw = arr[2];
                                send("PW: " + pw);
                                if (checkpw(e.getAuthor().getName(), name, pw)) {
                                    send("check pw success");
                                    PreparedStatement pstmt = null;
                                    ResultSet rs = null;
                                    try {
                                        pw = BCrypt.gensalt(12, SecureRandom.getInstanceStrong());
                                        pstmt = database.conn.prepareStatement("SELECT * FROM players WHERE accountid=?");
                                        pstmt.setString(1, name);
                                        rs = pstmt.executeQuery();
                                        if (!rs.next()) {
                                            Player player = playerGroup.find(p -> p.name.equalsIgnoreCase(name));
                                            if (player != null) {
                                                Locale lc = tool.getGeo(player);
                                                boolean register = playerDB.register(player.name, player.uuid, lc.getDisplayCountry(), lc.toString(), lc.getDisplayLanguage(), true, vars.serverIP(), "default", e.getAuthor().getIdLong(), name, pw);
                                                if (register) {
                                                    PlayerData playerData = playerDB.load(player.uuid);
                                                    player.sendMessage(new Bundle(playerData.locale()).prefix("register-success"));
                                                    send(new Bundle(playerData.locale()).get("register-success"));
                                                    break;
                                                }
                                            } else {
                                                send("Player not found in server!");
                                            }
                                        } else {
                                            send("You're already have account!");
                                        }
                                    } catch (Exception ex) {
                                        new CrashReport(ex);
                                    } finally {
                                        if (pstmt != null) try {
                                            pstmt.close();
                                        } catch (SQLException ignored) {
                                        }
                                        if (rs != null) try {
                                            rs.close();
                                        } catch (SQLException ignored) {
                                        }
                                    }
                                } else {
                                    send("Check password failed.");
                                }
                            } else {
                                send("PIN number don't match!");
                            }
                        }
                    } else {
                        send("Use ``!signup <PIN> <New password>``.");
                    }
                    break;
                case "!changepw":
                    break;
                default:
                    String message = ">>> Command list\n" +
                            "``!signup <PIN> <New password>`` Account register to server. Nickname and password can't use blank.";
                    send(message);
                    break;
            }
        }
    }

    private boolean checkpw(String username, String id, String pw) {
        // 영문(소문자), 숫자, 7~20자리
        String pwPattern = "^(?=.*\\d)(?=.*[a-z]).{7,20}$";
        Matcher matcher = Pattern.compile(pwPattern).matcher(pw);

        // 같은 문자 4개이상 사용 불가
        pwPattern = "(.)\\1\\1\\1";
        Matcher matcher2 = Pattern.compile(pwPattern).matcher(pw);

        if (!matcher.matches()) {
            // 정규식에 맞지 않을경우
            send("The password should be 7 ~ 20 letters long and contain alphanumeric characters and special characters!");
            Log.player("system.password.match.regex", username);
            return false;
        } else if (matcher2.find()) {
            // 비밀번호에 ID에 사용된 같은 문자가 4개 이상일경우
            send("Passwords should not be similar to nicknames!");
            Log.player("system.password.match.name", username);
            return false;
        } else if (pw.contains(id)) {
            // 비밀번호와 ID가 완전히 같은경우
            send("Password shouldn't be the same as your nickname.");
            return false;
        } else if (pw.contains(" ")) {
            // 비밀번호에 공백이 있을경우
            send("Password must not contain spaces!");
            Log.player("system.password.match.blank", username);
            return false;
        } else if (id.contains(" ")) {
            send("Username must not contain spaces!");
            Log.player("username-match-blank", username);
            return false;
        } else if (pw.matches("<(.*?)>")) {
            // 비밀번호 형식이 "<비밀번호>" 일경우
            send("<password> format isn't allowed! Use /register password");
            Log.player("system.password.match.invalid", username);
            return false;
        }
        return true;
    }

    public void send(String message) {
        event.getPrivateChannel().sendMessage(message).queue();
    }

    public void shutdownNow() {
        if (jda != null) jda.shutdown();
    }
}
