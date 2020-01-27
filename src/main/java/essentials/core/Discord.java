package essentials.core;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.mindrot.jbcrypt.BCrypt;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static essentials.Global.*;
import static essentials.core.PlayerDB.writeData;

public class Discord extends ListenerAdapter {
    static Guild guild;
    static TextChannel channel;
    static JDA jda;

    static MessageReceivedEvent event;

    public void main(){
        try {
            jda = new JDABuilder(config.getDiscordToken()).build();
            jda.awaitReady();
            jda.addEventListener(new Discord());
            guild = jda.getGuildById(config.getDiscordGuild());
            if(guild != null){
                channel = guild.getTextChannelsByName(config.getDiscordRoom(),true).get(0);
                log("log","discord-enabled");
            } else {
                log("err","discord-error");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent e) {
        event = e;
        if(!e.getAuthor().isBot()) e.getMessage().delete().queue();
        if(e.getChannel().getName().equals(config.getDiscordRoom())) {
            if (e.getMessage().getContentRaw().equals(config.getDiscordPrefix()+"help")) {
                String message = ">>> Command list\n" +
                        "**!help** Show discord bot commands\n" +
                        "**!signup <new account id> <new password> <password repeat>** Account register to kr server. Nickname and password can't use blank.\n" +
                        "**!changepw <old password> <new password> <new password repeat>** Change password";
                send(message);
                message = ">>> 명령어 목록\n" +
                        "**!help** Discord 봇 명령어를 표시합니다.\n" +
                        "**!signup <새 계정명> <새 비밀번호> <비밀번호 재입력>** 계정을 서버에 등록합니다. 닉네임과 비밀번호에는 공백을 포함할 수 없습니다.\n" +
                        "**!changepw <기존 비밀번호> <새 비밀번호> <새 비밀번호 재입력>** 비밀번호를 변경합니다.";
                send(message);
            }

            if (e.getMessage().getContentRaw().matches(config.getDiscordPrefix()+"signup.*")) {
                String message = e.getMessage().getContentRaw().replace("!signup ", "");
                String[] data = message.split(" ");
                if(data.length != 3){
                    message = "Use !signup <new account id> <new password> <password repeat>\n" +
                            "!signup <새 계정명> <새 비밀번호> <비밀번호 재입력> 형식으로 입력하세요.";
                    send(message);
                    return;
                }
                String id = data[0];
                String pw = data[1];
                String pw2 = data[2];
                if (checkpw(id, pw, pw2)) {
                    pw = BCrypt.hashpw(pw, BCrypt.gensalt(11));
                    if(isduplicateid(id) || isduplicatename(e.getAuthor().getName())) {
                        message = "This account is already in use or attempted to register an account with an invalid nickname!\n" +
                                "이 계정은 이미 사용중이거나 잘못된 닉네임으로 계정 등록을 시도했습니다!";
                        send(message);
                        return;
                    }
                    if(e.getMember() != null) {
                    if (PlayerDB.createNewDatabase(e.getAuthor().getName(), "InactiveAAA=", "invalid", "invalid", "invalid", false, 0, 0, getTime(), getTime(), false, e.getMember().getIdLong(), id, pw, null)) {
                            Role role = guild.getRolesByName(config.getDiscordRole(),false).get(0);
                            guild.addRoleToMember(e.getMember(), role).queue();
                        } else {
                            send("Invalid user! Discord set role failed.");
                        }
                        message = "Register successful! Now, join server and use /login command.\n" +
                                "계정 등록에 성공했습니다! 이제 서버에 가서 /login 명령어를 사용하세요.";
                    } else {
                        message = "Register failed.\n" +
                                "계정 등록 실패.";
                    }
                    send(message);
                }
            }

            if (e.getMessage().getContentRaw().matches(config.getDiscordPrefix()+"changepw.*")) {
                String message = e.getMessage().getContentRaw().replace("!changepw ", "");
                String[] data = message.split(" ");
                if(data.length != 3){
                    message = "Use !changepw <account id> <new password> <password repeat>\n" +
                            "!changepw <계정 ID> <새 비밀번호> <비밀번호 재입력> 형식으로 입력하세요.";
                    send(message);
                    return;
                }
                String id = data[0];
                String pw = data[1];
                String pw2 = data[2];

                if(checkpw(id,pw,pw2)){
                    try{
                        Class.forName("org.mindrot.jbcrypt.BCrypt");
                        String hashed = BCrypt.hashpw(pw, BCrypt.gensalt(11));
                        writeData("UPDATE players SET accountpw = ? WHERE accountid = ?",hashed,id);
                        send("Successful!\n성공!");
                    } catch (ClassNotFoundException ignored) {}
                }
            }
        }
    }
    
    public boolean checkpw(String id, String pw, String pw2){
        // 영문(소문자), 숫자, 7~20자리
        String pwPattern = "^(?=.*\\d)(?=.*[a-z]).{7,20}$";
        Matcher matcher = Pattern.compile(pwPattern).matcher(pw);

        // 같은 문자 4개이상 사용 불가
        pwPattern = "(.)\\1\\1\\1";
        Matcher matcher2 = Pattern.compile(pwPattern).matcher(pw);

        if (!pw.equals(pw2)) {
            // 비밀번호가 비밀번호 재확인 문자열과 똑같지 않을경우
            String message = "The password isn't the same.\n" +
                    "비밀번호가 똑같지 않습니다.";
            send(message);
            return false;
        } else
        if (!matcher.matches()) {
            // 정규식에 맞지 않을경우
            String message = "The password should be 7 ~ 20 letters long and contain alphanumeric characters and special characters!\n" +
                    "비밀번호는 7~20자 내외로 설정해야 하며, 영문과 숫자를 포함해야 합니다!";
            send(message);
            return false;
        } else if (matcher2.find()) {
            // 비밀번호에 ID에 사용된 같은 문자가 4개 이상일경우
            String message = "Passwords should not be similar to nicknames!\n" +
                    "비밀번호는 닉네임과 비슷하면 안됩니다!";
            send(message);
            return false;
        } else if (pw.contains(id)) {
            // 비밀번호와 ID가 완전히 같은경우
            String message = "Password shouldn't be the same as your nickname.\n" +
                    "비밀번호는 ID와 비슷하게 설정할 수 없습니다!";
            send(message);
            return false;
        } else if (pw.contains(" ")) {
            // 비밀번호에 공백이 있을경우
            String message = "Password must not contain spaces!\n" +
                    "비밀번호에는 공백이 있으면 안됩니다!";
            send(message);
            return false;
        } else if (pw.matches("<(.*?)>")) {
            // 비밀번호 형식이 "<비밀번호>" 일경우
            String message = "<password> format isn't allowed!\n" +
                    "Use '!command id password password_repeat' format.\n" +
                    "<비밀번호> 형식은 허용되지 않습니다!\n" +
                    "'!명령어 아이디 비밀번호 비밀번호_재입력' 형식으로 사용하세요.";
            send(message);
            return false;
        }
        return true;
    }
    
    public void send(String message){
        event.getChannel().sendMessage(message).queue(msg -> msg.delete().queueAfter(20, TimeUnit.SECONDS));
    }
}
