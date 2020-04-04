package remake.feature;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import remake.internal.Log;

import javax.security.auth.login.LoginException;

import static remake.Main.config;

public class Discord {
    public JDA jda = null;

    public void start() {
        try {
            jda = new JDABuilder(config.discordtoken).build();
            jda.awaitReady();
            jda.addEventListener(new Discord());
            Guild guild = jda.getGuildById(config.discordguild);
            if (guild != null) {
                TextChannel channel = guild.getTextChannelsByName(config.discordroom, true).get(0);
                Log.info("discord-enabled");
            } else {
                Log.err("discord-error");
            }
        } catch (LoginException e) {
            Log.err("Discord login failed!");
        } catch (InterruptedException ignored) {
        }
    }

    public void shutdownNow(){
        if (jda != null) jda.shutdownNow();
    }
}
