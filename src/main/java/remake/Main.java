package remake;

import arc.Core;
import arc.files.Fi;
import arc.util.CommandHandler;
import essentials.Global;
import essentials.special.StringUtils;
import mindustry.plugin.Plugin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static essentials.Global.log;
import static mindustry.Vars.netServer;

public class Main extends Plugin {
    public static Fi root = Core.settings.getDataDirectory().child("mods/Essentials/");

    public Main(){

    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.register("gendocs", "Generate Essentials README.md", (arg) -> {
            List<String> servercommands = new ArrayList<>(Arrays.asList(
                    "help","version","exit","stop","host","maps","reloadmaps","status",
                    "mods","mod","js","say","difficulty","rules","fillitems","playerlimit",
                    "config","subnet-ban","whitelisted","whitelist-add","whitelist-remove",
                    "shuffle","nextmap","kick","ban","bans","unban","admin","unadmin",
                    "admins","runwave","load","save","saves","gameover","info","search", "gc"
            ));

            List<String> clientcommands = new ArrayList<>(Arrays.asList(
                    "help","t","sync"
            ));

            String serverdoc = "## Server commands\n\n| Command | Parameter | Description |\n|:---|:---|:--- |\n";
            String clientdoc = "## Client commands\n\n| Command | Parameter | Description |\n|:---|:---|:--- |\n";

            String gentime = "\nREADME.md Generated time: "+ DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());

            log(Global.LogType.log,"readme-generating");
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
            for(CommandHandler.Command command : netServer.clientCommands.getCommandList()){
                if(!clientcommands.contains(command.text)){
                    String temp = "| "+command.text+" | "+ StringUtils.encodeHtml(command.paramText)+" | "+command.description+" |\n";
                    tempbuild.append(temp);
                }
            }

            String tmp = header+clientdoc+tempbuild.toString()+"\n";
            tempbuild = new StringBuilder();

            for(CommandHandler.Command command : handler.getCommandList()){
                if(!servercommands.contains(command.text)){
                    String temp = "| "+command.text+" | "+ StringUtils.encodeHtml(command.paramText)+" | "+command.description+" |\n";
                    tempbuild.append(temp);
                }
            }

            root.child("README.md").writeString(tmp+serverdoc+tempbuild.toString()+gentime);
            log(Global.LogType.log,"success");
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {

    }
}
