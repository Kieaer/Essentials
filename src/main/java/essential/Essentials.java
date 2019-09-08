package essential;

import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.collection.Array;
import io.anuke.arc.util.CommandHandler;
import io.anuke.arc.util.Log;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.game.Difficulty;
import io.anuke.mindustry.game.EventType.PlayerJoin;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.net.Administration.PlayerInfo;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetConnection;
import io.anuke.mindustry.net.Packets.KickReason;
import io.anuke.mindustry.plugin.Plugin;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static essential.EssentialsPlayer.createNewDatabase;
import static io.anuke.mindustry.Vars.player;


public class Essentials extends Plugin{
	public Essentials(){
		if(!Core.settings.getDataDirectory().child("plugins/Essentials/motd.txt").exists()){
			String msg = "To edit this message, modify the [green]motd.txt[] file in the [green]config/plugins/Essentials/[] folder.";
			Core.settings.getDataDirectory().child("plugins/Essentials/motd.txt").writeString(msg);
			Log.info("[Essentials] motd file created.");
		}

        if (!Core.settings.getDataDirectory().child("plugins/Essentials/database.db").exists()) {
            createNewDatabase();
            Log.info("[Essentials] Database file created.");
        }
        Events.on(PlayerJoin.class, e -> {
        	EssentialsPlayer essentialsPlayer = new EssentialsPlayer();
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd a hh:mm.ss");
            String nowString = now.format(dateTimeFormatter);
            essentialsPlayer.insert(e.player.name, e.player.uuid, e.player.isAdmin, e.player.isLocal, 0, 0, 0, 0, 0, 0, "F", nowString, nowString, "none", "none", 0, "none", 0, 0, 0, 0, 0);
			//EssentialsPlayer.insert(e.player.name, e.player.uuid, e.player.isAdmin, e.player.isLocal, 0,0,0,0, Vars.netServer.admins.getInfo(player.uuid).timesJoined, Vars.netServer.admins.getInfo(player.uuid).timesKicked, "F", nowString, nowString, "none","none",0,"none",0,0,0,0,0);
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
			// This command must be executed on a different thread.
			// Otherwise, the server will lag when this command is run.
            Thread thread1 = new Thread(new Runnable() {
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
            thread1.start();
		});

		handler.<Player>register("status", "Show server status", (args, player) -> {
			float fps = Math.round((int)60f / Time.delta());
			float memory = Core.app.getJavaHeap() / 1024 / 1024;
			player.sendMessage(fps+"TPS "+memory+"MB");
			player.sendMessage(Vars.playerGroup.size()+" players online.");
			int idb = 0;
			int ipb = 0;

			Array<PlayerInfo> bans = Vars.netServer.admins.getBanned();
			for(PlayerInfo info : bans){
				idb++;
			}

			Array<String> ipbans = Vars.netServer.admins.getBannedIPs();
            for(String string : ipbans){
				ipb++;
            }
            int bancount = idb + ipb;
            player.sendMessage(bancount+" players banned.");
		});

		// Teleport source from https://github.com/J-VdS/locationplugin
		handler.<Player>register("tp", "<player>", "Teleport to other players", (args, player) -> {
			Player other = Vars.playerGroup.find(p->p.name.equalsIgnoreCase(args[0]));
			if(!player.isAdmin){
				player.sendMessage("[green]Notice:[] You're not admin!");
			} else {
				if(other == null){
					player.sendMessage("[scarlet]No player by that name found!");
					return;
				}
				player.setNet(other.x, other.y);
			}
		});

		handler.<Player>register("tpp", "<player> <player>", "Teleport to other players", (args, player) -> {
			Player other1 = Vars.playerGroup.find(p->p.name.equalsIgnoreCase(args[0]));
			Player other2 = Vars.playerGroup.find(p->p.name.equalsIgnoreCase(args[1]));
			if(!player.isAdmin){
				player.sendMessage("[green]Notice:[] You're not admin!");
			} else {
				if(other1 == null || other2 == null){
					player.sendMessage("[scarlet]No player by that name found!");
					return;
				}
				other1.setNet(other2.x, other2.y);
			}
		});
		
		/*
		handler.<Player>register("tpmouse", "<player>", "Teleport to other players", (args, player) -> {
			Player other = Vars.playerGroup.find(p->p.name.equalsIgnoreCase(args[0]));
			if(player.isAdmin == false){
				player.sendMessage("[green]Notice:[] You're not admin!");
			} else {
				if(other == null){
					player.sendMessage("[scarlet]No player by that name found!");
					return;
				}
				boolean status = false;
				if(status = false){
					status = true;
				} else {
					status = false;
				}
				Thread thread2 = new Thread(new Runnable() {
            		public void run(){
						try{
							while(true){
								other.setNet(player.pointerX, player.pointerY);
								other.set(player.pointerX, player.pointerY);
								//Call.onPositionSet(other.con.id, player.pointerX, player.pointerY);
								if(other == null){
									break;
								}
							}
						} catch (Exception e){}
					}
            	});
            	thread2.start();
			}
		});
		*/

		handler.<Player>register("kickall", "Kick all players", (args, player) -> {
			if(!player.isAdmin){
				player.sendMessage("[green]Notice: [] You're not admin!");
			} else {
				for(NetConnection con : Net.getConnections()){
        		    Call.onKick(con.id, KickReason.kick);
        		}
			}
		});

		handler.<Player>register("spawnmob", "Spawn mob", (args, player) -> {
			if(!player.isAdmin){
				player.sendMessage("[green]Notice: [] You're not admin!");
			} else {
				player.sendMessage("source here");
			}
		});

		handler.<Player>register("tempban", "timer ban", (args, player) -> {
			if(!player.isAdmin){
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

		handler.<Player>register("me", "<text>", "broadcast * message", (args, player) -> {
			Call.sendMessage("[orange]*[] "+player.name+"[orange][white] : "+args[0]);
		});

		handler.<Player>register("difficulty", "<difficulty>", "Set server difficulty", (args, player) -> {
			
			if(!player.isAdmin){
				player.sendMessage("[green]Notice: [] You're not admin!");
			} else {
				try{
					Difficulty.valueOf(args[0]);
					player.sendMessage("Difficulty set to '"+args[0]+"'.");
				}catch(IllegalArgumentException e){
					player.sendMessage("No difficulty with name '"+args[0]+"' found.");
				}
			}
		});

		handler.<Player>register("effect", "<effect>", "make effect", (args, player) -> {
			//source
		});

		handler.<Player>register("gamerule", "<gamerule>", "Set gamerule", (args, player) -> {
			//source
		});

		handler.<Player>register("vote", "<vote>", "Votemap", (args, player) -> {
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
			if(!player.isAdmin){
				player.onPlayerDeath(player);
				Call.sendMessage(player.name+"[] used [green]suicide[] command.");
			}
		});

		handler.<Player>register("kill", "<player>", "Kill player.", (args, player) -> {
			if(player.isAdmin){
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
			DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd a hh:mm.ss");
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