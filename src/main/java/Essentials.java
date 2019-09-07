package essential;

import io.anuke.arc.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.core.*;
import io.anuke.mindustry.core.GameState.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.plugin.Plugin;

import io.anuke.mindustry.net.Packets.KickReason;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.maps.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import essential.EssentialsPlayer;

public class Essentials extends Plugin{
	public Essentials(){
		if(!Core.settings.getDataDirectory().child("plugins/Essentials/motd.txt").exists()){
			String msg = "To edit this message, modify the [green]motd.txt[] file in the [green]config/plugins/Essentials/[] folder.";
			Core.settings.getDataDirectory().child("plugins/Essentials/motd.txt").writeString(msg);
		}

		Events.on(PlayerJoin.class, e -> {
			String motd = Core.settings.getDataDirectory().child("plugins/Essentials/motd.txt").readString();
			e.player.sendMessage(motd);
		});
		
		// Block destroy/buildit count source (under development)
		/*
		Events.on(BlockBuildEndEvent.class, e -> {
			if(e.team == e.player.getTeam()){
				if(e.breaking){
					state.stats.buildingsDeconstructed++;
				}else{
					state.stats.buildingsBuilt++;
				}
			}
		});

		Events.on(BlockDestroyEvent.class, e -> {
			if(e.tile.getTeam() == e.player.getTeam()){
				state.stats.buildingsDestroyed++;
			}
		});

		Events.on(UnitDestroyEvent.class, e -> {
			if(e.unit.getTeam() != e.player.getTeam()){
				state.stats.enemyUnitsDestroyed++;
			}
		});
		*/
	}

	@Override
	public void registerServerCommands(CommandHandler handler){
		
	}

	@Override
	public void registerClientCommands(CommandHandler handler){
		/*
		handler.<Player>register("test", "test command", (args, player) -> {
			player.sendMessage(EssentialsPlayer.chat(player));
			player.sendMessage(EssentialsPlayer.ip(player));
			player.sendMessage(EssentialsPlayer.name(player));
			player.sendMessage(EssentialsPlayer.uuid(player));
		});
		*/

		handler.<Player>register("motd", "Show server motd.", (args, player) -> {
			String motd = Core.settings.getDataDirectory().child("plugins/Essentials/motd.txt").readString();
			player.sendMessage(motd);
		});

		handler.<Player>register("getpos", "Get your current position info", (args, player) -> {
			player.sendMessage("X: "+Math.round(player.x)+" Y: "+Math.round(player.y));
		});

		handler.<Player>register("info", "Show your information", (args, player) -> {
			
			/*
			Runnable r1 = new thread1();
            Thread t1 = new Thread(r1);
            t1.start();
            try{
                t1.join();
            } catch (Exception e){
                Log.info(e);
            }
            */
            Thread t = new Thread(new Runnable() {
            	public void run(){
					try{
						String ip = Vars.netServer.admins.getInfo(player.uuid).lastIP;
						player.sendMessage("[green][INFO][] Getting information...");
						String connUrl = "http://ipapi.co/"+ip+"/country_name";
						Document doc = Jsoup.connect(connUrl).get();
						String geo = doc.text();
						player.sendMessage("[green]Name[]: "+player.name);
						player.sendMessage("[green]UUID[]: "+player.uuid);
						player.sendMessage("[green]Mobile[]: "+player.isMobile);
						player.sendMessage("[green]IP[]: "+ip);
						player.sendMessage("[green]Country[]: "+geo);
					} catch (Exception e){
						player.sendMessage("Load failed!");
					}
				}
            });
            t.start();
            //String geo = r1.result();
		});

		// Teleport source from https://github.com/J-VdS/locationplugin
		handler.<Player>register("tp", "<player...>", "Teleport to other players", (args, player) -> {
			Player other = Vars.playerGroup.find(p->p.name.equalsIgnoreCase(args[0]));
			if(player.isAdmin == false){
				player.sendMessage("[green]Notice:[] You're not admin!");
			} else {
				if(other == null){
					player.sendMessage("[scarlet]No player by that name found!");
					return;
				}
				// strict on
				player.setNet(other.x, other.y);
				player.set(other.x, other.y);
				// strict off
				Call.onPositionSet(player.con.id, other.x, other.y);
			}
		});
		

		handler.<Player>register("kickall", "Kick all players", (args, player) -> {
			if(player.isAdmin == false){
				player.sendMessage("[green]Notice: [] You're not admin!");
			} else {
				// Call.onKick(other, KickReason.kick);
			}
		});

		handler.<Player>register("spawnmob", "Spawn mob", (args, player) -> {
			if(player.isAdmin == false){
				player.sendMessage("[green]Notice: [] You're not admin!");
			} else {
				player.sendMessage("source here");
			}
		});

		handler.<Player>register("tempban", "timer ban", (args, player) -> {
			if(player.isAdmin == false){
				player.sendMessage("[green]Notice: [] You're not admin!");
			} else {
				/*
				netServer.admins.banPlayerIP(other.con.address);
				netServer.kick(other.con.id, KickReason.banned);
				Core.settings.getDataDirectory().child("bans.txt").writeString(other.name+"/"+other.ip+"/"+time);
				String motd = Core.settings.getDataDirectory().child("motd.txt").readString();
				*/
				// copied from ServerControl.java
			}
		});

		handler.<Player>register("me", "<text...>", "broadcast * message", (args, player) -> {
			Call.sendMessage("[orange]*[] "+player.name+"[orange][] : "+args[0]);
		});

		handler.<Player>register("difficulty", "<difficulty...>", "Set server difficulty", (args, player) -> {
			/*
			if(player.isAdmin == false){
				player.sendMessage("[green]Notice: [] You're not admin!");
			} else {
				try{
					GameState.rules.waveSpacing = Difficulty.valueOf(args[0]).waveTime * 60 * 60 * 2;
					player.sendMessage("Difficulty set to '"+args[0]+"'.");
				}catch(IllegalArgumentException e){
					player.sendMessage("No difficulty with name '"+args[0]+"' found.");
				}
			}
			*/
			// error: non-static method all() cannot be referenced from a static context
		});

		handler.<Player>register("effect", "<effect...>", "make effect", (args, player) -> {
			//source
		});

		handler.<Player>register("gamerule", "<gamerule...>", "Set gamerule", (args, player) -> {
			//source
		});

		handler.<Player>register("vote", "<vote...>", "Votemap", (args, player) -> {
			/*
			if(!Maps.all().isEmpty()){
				player.sendMessage("Maps:");
				for(Map m : Maps.all()){
					player.sendMessage(m.name()+"/"+m.width+"x"+m.height);
				}
			}else{
				player.sendMessage("No maps found.");
			}
			*/
			// error: non-static method all() cannot be referenced from a static context
		});

		handler.<Player>register("suicide", "Kill yourself.", (args, player) -> {
			if(player.isAdmin == false){
				player.onPlayerDeath(player);
				Call.sendMessage(player.name+"[] used [green]suicide[] command.");
			}
		});

		handler.<Player>register("kill", "<player...>", "Kill player.", (args, player) -> {
			if(player.isAdmin == true){
				Player other = Vars.playerGroup.find(p->p.name.equalsIgnoreCase(args[0]));
				if(other == null){
					player.sendMessage("[scarlet]No player by that name found!");
					return;
				}
				other.onPlayerDeath(other);
			} else {
				player.sendMessage("You're not admin!");
			}
		});

		handler.<Player>register("save", "Map save", (args, player) -> {
			/*
			Core.app.post(() -> {
				int slot = Strings.parseInt(arg[0]);
				SaveIO.saveToSlot(slot);
				info("Saved to slot {0}.", slot);
			});
			*/
			// copied from ServerControl.java
		});

		handler.<Player>register("time", "Show server time", (args, player) -> {
			LocalDateTime now = LocalDateTime.now();
			DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-M-d a h:m.ss");
			String nowString = now.format(dateTimeFormatter);
			player.sendMessage("[green]Server time[white]: "+nowString);
		});

		handler.<Player>register("banlist", "Show banlist", (args, player) -> {
			/*
			for(PlayerInfo info : bans){
				info(" &ly {0} / Last known name: '{1}'", info.id, info.lastName);
			}
			for(String string : ipbans){
				PlayerInfo info = netServer.admins.findByIP(string);
				if(info != null){
					info(" &lm '{0}' / Last known name: '{1}' / ID: '{2}'", string, info.lastName, info.id);
				}else{
					info(" &lm '{0}' (No known name or info)", string);
				}
			}
			*/
			//copied from ServerControl.java
		});
	}
}

/*
class thread1 implements Runnable{

    public void run(){
		try{
			String connUrl = "http://ipapi.co/"+ip+"/country_name";
			Document doc = Jsoup.connect(connUrl).get();
			String geo = doc.text();
		} catch (Exception e){}
	}

	public String result(){
		return geo;
	}
}
*/
// cross server chat
// Tau 힐링 버프