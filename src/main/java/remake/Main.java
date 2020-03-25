package remake;

import arc.Core;
import arc.files.Fi;
import arc.util.CommandHandler;
import essentials.special.StringUtils;
import mindustry.core.Version;
import mindustry.entities.type.Player;
import mindustry.plugin.Plugin;
import org.hjson.JsonObject;
import remake.core.plugin.Config;
import remake.feature.Permission;
import remake.internal.CrashReport;
import remake.internal.Log;
import remake.internal.thread.Threads;
import remake.internal.thread.TickTrigger;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static mindustry.Vars.netServer;
import static mindustry.Vars.playerGroup;
import static remake.Vars.build_version;

public class Main extends Plugin {
    public static Fi root = Core.settings.getDataDirectory().child("mods/Essentials/");
    public static Locale locale = new Locale(System.getProperty("user.language"), System.getProperty("user.country"));
    public static Config config;
    public static Timer mainThread = new Timer(true);

    public Main() throws Exception {
        // 서버 버전 확인
        if (Version.build != build_version) {
            InputStream reader = getClass().getResourceAsStream("/plugin.json");
            BufferedReader br = new BufferedReader(new InputStreamReader(reader));
            throw new Exception("Essentials " + JsonObject.readJSON(br).asObject().get("version").asString() + " plugin only works with mindustry build 104.");
        }

        // 파일 압축해제
        if (!root.exists()) {
            try {
                final String path = "configs";
                final JarFile jar = new JarFile(new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()));
                final Enumeration<JarEntry> entries = jar.entries();

                while (entries.hasMoreElements()) {
                    final String name = entries.nextElement().getName();
                    if (name.startsWith(path + "/")) {
                        InputStream reader = getClass().getResourceAsStream("/" + name);
                        root.child(name).write(reader, false);
                    }
                }
                jar.close();
            } catch (Exception e) {
                new CrashReport(e);
            }
        }

        // 설정 불러오기
        config = new Config();

        // 스레드 시작
        new TickTrigger();
        mainThread.scheduleAtFixedRate(new Threads(), 1000, 1000);
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.register("gendocs", "Generate Essentials README.md", (arg) -> {
            List<String> servercommands = new ArrayList<>(Arrays.asList(
                    "help", "version", "exit", "stop", "host", "maps", "reloadmaps", "status",
                    "mods", "mod", "js", "say", "difficulty", "rules", "fillitems", "playerlimit",
                    "config", "subnet-ban", "whitelisted", "whitelist-add", "whitelist-remove",
                    "shuffle", "nextmap", "kick", "ban", "bans", "unban", "admin", "unadmin",
                    "admins", "runwave", "load", "save", "saves", "gameover", "info", "search", "gc"
            ));
            List<String> clientcommands = new ArrayList<>(Arrays.asList(
                    "help", "t", "sync"
            ));
            String serverdoc = "## Server commands\n\n| Command | Parameter | Description |\n|:---|:---|:--- |\n";
            String clientdoc = "## Client commands\n\n| Command | Parameter | Description |\n|:---|:---|:--- |\n";
            String gentime = "\nREADME.md Generated time: " + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());

            Log.info("readme-generating");

            String header = "# Essentials\n" +
                    "Add more commands to the server.\n\n" +
                    "I'm getting a lot of suggestions.<br>\n" +
                    "Please submit your idea to this repository issues or Mindustry official discord!\n\n" +
                    "## Requirements for running this plugin\n" +
                    "This plugin does a lot of disk read/write operations depending on the features usage.\n\n" +
                    "### Minimum\n" +
                    "CPU: Athlon 200GE or Intel i5 2300<br>\n" +
                    "RAM: 20MB<br>\n" +
                    "Disk: HDD capable of more than 2MB/s random read/write.\n\n" +
                    "### Recommand\n" +
                    "CPU: Ryzen 3 2200G or Intel i3 8100<br>\n" +
                    "RAM: 50MB<br>\n" +
                    "Disk: HDD capable of more than 5MB/s random read/write.\n\n" +
                    "## Installation\n\n" +
                    "Put this plugin in the ``<server folder location>/config/mods`` folder.\n\n" +
                    "## Essentials 9.0 Plans\n" +
                    "- [ ] Server\n" +
                    "  - [ ] Control server using Web server\n" +
                    "- [x] PlayerDB\n" +
                    "  - [x] DB Server without MySQL/MariaDB..\n" +
                    "- [x] Internal\n" +
                    "  - [x] Fix server can't shutdown\n" +
                    "  - [x] Make ban reason\n" +
                    "- [ ] Anti-grief\n" +
                    "  - [ ] Make anti-filter art\n" +
                    "  - [ ] Make anti-fast build/break destroy\n" +
                    "  - [ ] Make detect custom client\n" +
                    "- [ ] Soruce code rebuild\n" +
                    "  - [ ] core\n" +
                    "    - [ ] Discord\n" +
                    "    - [ ] PlayerDB\n" +
                    "  - [ ] net\n" +
                    "    - [ ] Client\n" +
                    "    - [ ] Server\n" +
                    "  - [ ] special\n" +
                    "    - [ ] DataMigration\n" +
                    "    - [ ] DriverLoader\n" +
                    "  - [ ] utils\n" +
                    "    - [ ] Bundle\n" +
                    "    - [ ] Config\n" +
                    "  - [ ] Global\n" +
                    "  - [ ] Main\n" +
                    "  - [ ] Threads\n" +
                    "    - [ ] login\n" +
                    "    - [ ] changename\n" +
                    "    - [ ] AutoRollback\n" +
                    "    - [ ] eventserver\n" +
                    "    - [ ] ColorNick\n" +
                    "    - [ ] monitorresource\n" +
                    "    - [ ] Vote\n" +
                    "    - [ ] jumpdata\n" +
                    "    - [ ] visualjump\n\n";

            StringBuilder tempbuild = new StringBuilder();
            for (CommandHandler.Command command : netServer.clientCommands.getCommandList()) {
                if (!clientcommands.contains(command.text)) {
                    String temp = "| " + command.text + " | " + StringUtils.encodeHtml(command.paramText) + " | " + command.description + " |\n";
                    tempbuild.append(temp);
                }
            }

            String tmp = header + clientdoc + tempbuild.toString() + "\n";
            tempbuild = new StringBuilder();

            for (CommandHandler.Command command : handler.getCommandList()) {
                if (!servercommands.contains(command.text)) {
                    String temp = "| " + command.text + " | " + StringUtils.encodeHtml(command.paramText) + " | " + command.description + " |\n";
                    tempbuild.append(temp);
                }
            }

            root.child("README.md").writeString(tmp + serverdoc + tempbuild.toString() + gentime);

            Log.info("success");
        });
        handler.register("admin", "<name>", "Set admin status to player.", (arg) -> {
            if (arg.length != 0) {
                Permission perm = new Permission();
                Player player = playerGroup.find(p -> p.name.equals(arg[0]));

                if (player == null) {
                    Log.warn("player-not-found");
                } else {
                    for (JsonObject.Member data : perm.permission) {
                        if (data.getName().equals("new_admin")) {
                            //PlayerDB.PlayerData p = PlayerData(player.uuid);
                            //p.permission = "new_admin";
                            //PlayerDataSave(p);
                            Log.info("success");
                            break;
                        }
                    }
                    Log.warn("use-setperm");
                }
            } else {
                Log.warn("no-parameter");
            }
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {

    }
}
