package essentials;

import essentials.net.Client;
import essentials.net.Server;
import essentials.special.ColorNick;
import essentials.thread.Update;
import essentials.vpn.VPNDetection;
import io.anuke.arc.ApplicationListener;
import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.collection.Array;
import io.anuke.arc.util.CommandHandler;
import io.anuke.arc.util.Log;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.game.Difficulty;
import io.anuke.mindustry.game.EventType;
import io.anuke.mindustry.game.EventType.PlayerJoin;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.mindustry.net.Administration.PlayerInfo;
import io.anuke.mindustry.net.Packets.KickReason;
import io.anuke.mindustry.plugin.Plugin;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static essentials.EssentialConfig.*;
import static essentials.EssentialPlayer.getData;
import static essentials.EssentialPlayer.writeData;
import static essentials.thread.Detectlang.detectlang;
import static io.anuke.arc.util.Log.err;
import static io.anuke.mindustry.Vars.*;

public class Main extends Plugin{
	private ArrayList<String> vote = new ArrayList<>();
	private boolean voteactive;

	// Note
	//Blocks.message = new MessageBlock(){ /**override methods **/};
	//Vars.plugins.getPlugin(getClass()).zipRoot.child("bundle.properties")
	//mods.getMod(getClass()).<stuff>
	//Connection#kick(String reason)

	public Main() {
		// Start config file
	    EssentialConfig.main();

	    // Start log
		EssentialLog.main();

		// Update check
		Thread update = new Thread(() -> {
			try {
				Global.log("Update checking...");
				Thread.sleep(1500);
				Update.main();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
		update.start();


		// Start discord bot
		EssentialDiscord.main();

		// DB Upgrade check
		EssentialPlayer.Upgrade();

		// Start ban/chat server
		if(banshare){
			Runnable server = new Server();
			Thread t = new Thread(server);
			t.start();
		}

		if(antivpn){
			Global.log("Anti-VPN enabled.");
		}

		// TODO Make PvP winner count
		Events.on(EventType.GameOverEvent.class, e -> {
			//e.winner.name();
		});

		Events.on(EventType.WorldLoadEvent.class, () -> EssentialTimer.playtime = "00:00.00");

        // Set if thorium rector explode
        Events.on(EventType.Trigger.thoriumReactorOverheat, () -> {
            if(detectreactor){
                Call.sendMessage("[scarlet]= WARNING WARNING WARNING =");
                Call.sendMessage("[scarlet]Thorium Reactor Exploded");
                Global.log("Thorium Reactor explode detected!!");
            }
        });

		// Set if player join event
        Events.on(PlayerJoin.class, e -> {
			// Database read/write
			Thread playerthread = new Thread(() -> {
				Thread.currentThread().setName("PlayerJoin Thread");
				EssentialPlayer.main(e.player);
				JSONObject db = getData(e.player.uuid);

				// Write player connected
				writeData("UPDATE players SET connected = '1' WHERE uuid = '"+e.player.uuid+"'");

				// Check if realname enabled
				if(realname){
					e.player.name = db.getString("name");
				}

				// Check if blacklisted nickname
				String blacklist = Core.settings.getDataDirectory().child("plugins/Essentials/blacklist.json").readString();
				JSONTokener parser = new JSONTokener(blacklist);
				JSONArray array = new JSONArray(parser);

				for (int i = 0; i < array.length(); i++){
					if (array.getString(i).equals(e.player.name)){
						e.player.con.kick("Server isn't allow blacklisted nickname.");
						Global.log(e.player.name+" nickname is blacklisted.");
					}
				}

				// Check VPN
				if(antivpn){
					String ipToLookup = netServer.admins.getInfo(e.player.uuid).lastIP;
					try {
						boolean isHostingorVPN = new VPNDetection().getResponse(ipToLookup).hostip;
						if(isHostingorVPN){
							e.player.con.kick("Server isn't allow VPN connection.");
						}
					} catch (IOException error) {
						error.printStackTrace();
					}
				}

				// Show motd
				String motd = Core.settings.getDataDirectory().child("plugins/Essentials/motd.txt").readString();
				e.player.sendMessage(motd);

				// Give join exp
				Thread expthread = new Thread(() -> EssentialExp.joinexp(e.player.uuid));
				expthread.start();

				// Color nickname
				int colornick = Integer.parseInt(db.getString("colornick"));
				if(realname && colornick == 1){
					ColorNick.main(e.player);
				} else if(!realname && colornick == 0){
					Global.logw("Color nickname must be enabled before 'realname' can be enabled.");

					writeData("UPDATE players SET colornick = '0' WHERE uuid = '"+e.player.uuid+"'");
				}
			});
			playerthread.start();
		});

		Events.on(EventType.PlayerLeave.class, e -> {
			JSONObject db = getData(e.player.uuid);
			e.player.name = db.getString("name");

			writeData("UPDATE players SET connected = '0' WHERE uuid = '"+e.player.uuid+"'");
		});

		// Set if player chat event
		Events.on(EventType.PlayerChatEvent.class, e -> {
			String check = String.valueOf(e.message.charAt(0));
			//check if command
			if(!check.equals("/")) {
				//boolean valid = e.message.matches("\\w+");
				JSONObject db = getData(e.player.uuid);
				int translate = Integer.parseInt(db.getString("translate"));
				int crosschat = Integer.parseInt(db.getString("crosschat"));

				detectlang(translate, e.player, e.message);
				if (clientenable) {
					if(crosschat == 1) {
						Thread chatclient = new Thread(() -> {
							String message = e.player.name.replaceAll("\\[(.*?)]", "") + ": " + e.message;
							Client.main("chat", message, e.player);
						});
						chatclient.start();
					}
				} else if(crosschat == 1){
					e.player.sendMessage("Currently server isn't enable cross-server client!");
				}
			}
		});

		// Set if player build block event
		Events.on(EventType.BlockBuildEndEvent.class, e -> {
			if (!e.breaking && e.player != null && e.player.buildRequest() != null) {
				JSONObject db = getData(e.player.uuid);
				try{
					int data = db.getInt("placecount");
					int exp = db.getInt("exp");

					Yaml yaml = new Yaml();
					Map<String, Object> obj = yaml.load(String.valueOf(Core.settings.getDataDirectory().child("plugins/Essentials/Exp.txt").readString()));
					int blockexp;
					if(String.valueOf(obj.get(e.tile.block().name)) != null) {
						blockexp = Integer.parseInt(String.valueOf(obj.get(e.tile.block().name)));
					} else {
						blockexp = 5;
					}
					int newexp = exp + blockexp;
					data++;

					writeData("UPDATE players SET placecount = '"+data+"', exp = '"+newexp+"' WHERE uuid = '"+e.player.uuid+"'");
				} catch (Exception ex){
					ex.printStackTrace();
				}
			}
		});

		Events.on(EventType.UnitDestroyEvent.class, event -> {
			if(playerGroup != null && playerGroup.size() > 0){
				for(int i=0;i<playerGroup.size();i++){
					Player player = playerGroup.all().get(i);
					JSONObject db = getData(player.uuid);
					int killcount;
					if(db.has("killcount")) {
						killcount = db.getInt("killcount");
					} else {
						return;
					}
					killcount++;

					writeData("UPDATE players SET killcount = '"+killcount+"' WHERE uuid = '"+player.uuid+"'");
				}
			}
		});

		Timer timer = new Timer();
		TimerTask playtime = new TimerTask(){
			@Override
			public void run() {
			    EssentialTimer.main();
			}
		};

		// Set if shutdown
		Core.app.addListener(new ApplicationListener(){
			public void dispose(){
				// Kill timer thread
				try{
					timer.cancel();
					Global.log("Play/bantime counting thread disabled.");
				} catch (Exception e){
					err("[Essentials] Failure to disable Playtime counting thread!");
					e.printStackTrace();
				}

				// Kill Ban/chat server thread
				if(serverenable){
					try {
						Server.active = false;
						Server.serverSocket.close();
						Global.log("Chat/Ban server thread disabled.");
					} catch (Exception ignored){
						err("[Essentials] Failure to disable Chat server thread!");
					}
				}
			}
		});

        // Alert Realname event
        if(realname){
			Global.log("Realname enabled.");
        }

        // Alert thorium reactor explode detect event
        if(detectreactor){
			Global.log("Thorium reactor overheat detect enabled.");
        }

        timer.scheduleAtFixedRate(playtime, 0, 1000);
		Global.log("Play/bantime counting thread started.");
	}

	@Override
	public void registerServerCommands(CommandHandler handler){
		handler.register("tempban", "<type-id/name/ip> <username/IP/ID> <time...>", "Temporarily ban player. time unit: 1 hours", arg -> {
			int bantimeset = Integer.parseInt(arg[1]);
			Player other = playerGroup.find(p -> p.name.equalsIgnoreCase(arg[0]));
			EssentialPlayer.addtimeban(other.name, other.uuid, bantimeset);
			switch (arg[0]) {
				case "id":
					netServer.admins.banPlayerID(arg[1]);
					break;
				case "name":
					Player target = playerGroup.find(p -> p.name.equalsIgnoreCase(arg[0]));
					if (target != null) {
						netServer.admins.banPlayer(target.uuid);
						Global.log("banned the "+other.name+" player for "+arg[1]+" hour.");
					} else {
						err("No matches found.");
					}
					break;
				case "ip":
					netServer.admins.banPlayerIP(arg[1]);
					Global.log("banned the "+other.name+" player for "+arg[1]+" hour.");
					break;
				default:
					err("Invalid type.");
					break;
			}
		});

        handler.register("blacklist", "<nickname>", "Block special nickname.", arg -> {
        	// TODO add remove option
            String db = Core.settings.getDataDirectory().child("plugins/Essentials/blacklist.json").readString();
            JSONTokener parser = new JSONTokener(db);
            JSONArray object = new JSONArray(parser);
            object.put(arg[0]);
			Core.settings.getDataDirectory().child("plugins/Essentials/blacklist.json").writeString(String.valueOf(object));
			Global.log(""+arg[0]+" nickname is registered in blacklist.");
        });

		handler.register("allinfo", "<name>", "Show player information", (args) -> {
			Player other = Vars.playerGroup.find(p->p.name.equalsIgnoreCase(args[0]));
			if(other != null) {
				JSONObject db = getData(other.uuid);
				String datatext = "\nPlayer Information\n" +
						"========================================\n" +
						"Name: " + other.name + "\n" +
						"UUID: " + other.uuid + "\n" +
						"Mobile: " + other.isMobile + "\n" +
						"Country: " + db.get("country") + "\n" +
						"Block place: " + db.get("placecount") + "\n" +
						"Block break: " + db.get("breakcount") + "\n" +
						"Kill units: " + db.get("killcount") + "\n" +
						"Death count: " + db.get("deathcount") + "\n" +
						"Join count: " + db.get("joincount") + "\n" +
						"Kick count: " + db.get("kickcount") + "\n" +
						"Level: " + db.get("level") + "\n" +
						"XP: " + db.get("reqtotalexp") + "\n" +
						"First join: " + db.get("firstdate") + "\n" +
						"Last join: " + db.get("lastdate") + "\n" +
						"Playtime: " + db.get("playtime") + "\n" +
						"Attack clear: " + db.get("attackclear") + "\n" +
						"PvP Win: " + db.get("pvpwincount") + "\n" +
						"PvP Lose: " + db.get("pvplosecount") + "\n" +
						"PvP Surrender: " + db.get("pvpbreakout");
				Log.info(datatext);
			} else {
				Global.log("Player not found!");
			}
		});

		handler.register("bansync", "Ban list synchronization from master server", (args) -> {
			if(banshare){
				String db = Core.settings.getDataDirectory().child("plugins/Essentials/data.json").readString();
				JSONTokener parser = new JSONTokener(db);
				JSONObject object = new JSONObject(parser);
				object.put("banall", "true");
				Core.settings.getDataDirectory().child("plugins/Essentials/data.json").writeString(String.valueOf(object));
				Thread banthread = new Thread(() -> Client.main("ban", "", null));
				banthread.start();
			}
		});

		handler.register("pvp", "<anticoal/timer> [time...]", "Set gamerule with PvP mode.", arg -> {
			/*
			if(Vars.state.rules.pvp){
				switch(arg[0]){
					case "anticoal":
						break;
					case "timer":
						break;
					default:
						break;
				}
			}
			*/
			Global.log("Currently not supported!");
		});
	}

	@Override
	public void registerClientCommands(CommandHandler handler){
		handler.<Player>register("motd", "Show server motd.", (args, player) -> {
			String motd = Core.settings.getDataDirectory().child("plugins/Essentials/motd.txt").readString();
			player.sendMessage(motd);
		});

		handler.<Player>register("getpos", "Get your current position info", (args, player) -> player.sendMessage("X: "+Math.round(player.x)+" Y: "+Math.round(player.y)));

		handler.<Player>register("info","Show your information", (args, player) -> {
			String ip = Vars.netServer.admins.getInfo(player.uuid).lastIP;
			JSONObject db = getData(player.uuid);
			String datatext =
					"[#DEA82A]Player Information[]\n" +
					"[#2B60DE]========================================[]\n" +
					"[green]Name[]			: "+player.name+"[white]\n" +
					"[green]UUID[]			: "+player.uuid+"\n" +
					"[green]Mobile[]		: "+player.isMobile+"\n" +
					"[green]IP[]			: "+ip+"\n" +
					"[green]Country[]		: "+db.get("country")+"\n" +
					"[green]Block place[]	: "+db.get("placecount")+"\n" +
					"[green]Block break[]	: "+db.get("breakcount")+"\n" +
					"[green]Kill units[]	: "+db.get("killcount")+"\n" +
					"[green]Death count[]	: "+db.get("deathcount")+"\n" +
					"[green]Join count[]	: "+db.get("joincount")+"\n" +
					"[green]Kick count[]	: "+db.get("kickcount")+"\n" +
					"[green]Level[]			: "+db.get("level")+"\n" +
					"[green]XP[]			: "+db.get("reqtotalexp")+"\n" +
					"[green]First join[]	: "+db.get("firstdate")+"\n" +
					"[green]Last join[]		: "+db.get("lastdate")+"\n" +
					"[green]Playtime[]		: "+db.get("playtime")+"\n" +
					"[green]Attack clear[]	: "+db.get("attackclear")+"\n" +
					"[green]PvP Win[]		: "+db.get("pvpwincount")+"\n" +
					"[green]PvP Lose[]		: "+db.get("pvplosecount")+"\n" +
					"[green]PvP Surrender[]	: "+db.get("pvpbreakout");
			Call.onInfoMessage(player.con, datatext);
		});

		handler.<Player>register("status", "Show server status", (args, player) -> {
			player.sendMessage("[#DEA82A]Server status[]");
			player.sendMessage("[#2B60DE]========================================[]");
			float fps = Math.round((int)60f / Time.delta());
			float memory = Core.app.getJavaHeap() / 1024 / 1024;
			player.sendMessage(fps+"TPS "+memory+"MB");
			player.sendMessage(Vars.playerGroup.size()+" players online.");
			int idb = 0;
			int ipb = 0;

			Array<PlayerInfo> bans = Vars.netServer.admins.getBanned();
			for(PlayerInfo ignored : bans){
				idb++;
			}

			Array<String> ipbans = Vars.netServer.admins.getBannedIPs();
            for(String ignored : ipbans){
				ipb++;
            }
            int bancount = idb + ipb;
            player.sendMessage("Total [scarlet]"+bancount+"[]("+idb+"/"+ipb+") players banned.");
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

		// Teleport source from https://github.com/J-VdS/locationplugin
		handler.<Player>register("tp", "<player>", "Teleport to other players", (args, player) -> {
			Player other = Vars.playerGroup.find(p->p.name.equalsIgnoreCase(args[0]));
			if(other == null){
				player.sendMessage("[scarlet]No player by that name found!");
				return;
			}
			player.setNet(other.x, other.y);
		});

		handler.<Player>register("kickall", "Kick all players", (args, player) -> {
			if(!player.isAdmin){
				player.sendMessage("[green]Notice: [] You're not admin!");
			} else {
				Vars.netServer.kickAll(KickReason.gameover);
			}
		});

		handler.<Player>register("tempban", "<player> <time>", "Temporarily ban player. time unit: 1 hours", (args, player) -> {
			if(!player.isAdmin){
				player.sendMessage("[green]Notice: [] You're not admin!");
			} else {
				Player other = Vars.playerGroup.find(p -> p.name.equalsIgnoreCase(args[0]));
				if(other != null){
					int bantimeset = Integer.parseInt(args[1]);
					EssentialPlayer.addtimeban(other.name, other.uuid, bantimeset);
					Call.sendMessage("Player"+other.name+" was killed (ban) by player "+player.name+"!");
				} else {
					player.sendMessage("No match player found!");
				}
			}
		});

		handler.<Player>register("me", "<text>", "broadcast * message", (args, player) -> Call.sendMessage("[orange]*[] "+player.name+"[white] : "+args[0]));

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

		handler.<Player>register("vote", "<gameover/skipwave/kick/y> [playername...]", "Vote surrender or skip wave, Long-time kick", (args, player) -> {
			switch(args[0]) {
				case "gameover":
					if(!this.voteactive) {
						this.voteactive = true;
						vote.add(player.name);
						int current = vote.size();
						int require = (int) Math.ceil(0.6 * Vars.playerGroup.size());
						Call.sendMessage("[green][Essentials] Gameover vote started! Use '/vote y' to agree.");
						Call.sendMessage("[green][Essentials] Require [scarlet]" + require + "[green] players.");

						Thread t = new Thread(() -> {
							try {
								Thread.sleep(10000);
								Call.sendMessage("[green][Essentials] 20 seconds remaining");
								Thread.sleep(10000);
								Call.sendMessage("[green][Essentials] 10 seconds remaining");
								Thread.sleep(10000);
								if (current >= require) {
									Call.sendMessage("[green][Essentials] Gameover vote passed!");
									Events.fire(new EventType.GameOverEvent(Team.sharded));
									Thread.sleep(15000);
								} else {
									Call.sendMessage("[green][Essentials] [red]Gameover vote failed.");
								}
								vote.clear();
								this.voteactive = false;
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						});
						t.start();
					} else {
						player.sendMessage("[green][Essentials] Vote in processing!");
					}
					break;
				case "skipwave":
					if(!this.voteactive){
						this.voteactive = true;
						vote.add(player.name);
						int current = vote.size();
						int require = (int) Math.ceil(0.6 * Vars.playerGroup.size());
						Call.sendMessage("[green][Essentials] skipwave vote started! Use '/vote y' to agree.");
						Call.sendMessage("[green][Essentials] Require [scarlet]"+require+"[green] players.");

						Thread t = new Thread(() -> {
							try {
								Thread.sleep(10000);
								Call.sendMessage("[green][Essentials] 20 seconds remaining");
								Thread.sleep(10000);
								Call.sendMessage("[green][Essentials] 10 seconds remaining");
								Thread.sleep(10000);
								if (current >= require) {
									Call.sendMessage("[green][Essentials] Skip 10 wave vote passed!");
									for (int i = 0; i < 10; i++) {
										logic.runWave();
									}
								} else {
									Call.sendMessage("[green][Essentials] [red]Skip 10 wave vote failed.");
								}
								vote.clear();
								this.voteactive = false;
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						});
						t.start();
					} else {
						player.sendMessage("[green][Essentials] Vote in processing!");
					}
					break;
				case "ban":
					if(!this.voteactive){
						this.voteactive = true;
						vote.add(player.name);
						int current = vote.size();
						int require = (int) Math.ceil(0.6 * Vars.playerGroup.size());
						Player target = playerGroup.find(p -> p.name.equals(args[1]));
						if (target != null) {
							target.con.kick("You have been kicked by voting.");
						} else {
							player.sendMessage("[scarlet]Player not found!");
							this.voteactive = false;
							return;
						}

						Call.sendMessage("[green][Essentials] ban vote started! Use '/vote y' to agree.");
						Call.sendMessage("[green][Essentials] Require [white]" + require + "[green] players.");

						Thread t = new Thread(() -> {
							try {
								Thread.sleep(10000);
								Call.sendMessage("[green][Essentials] 20 seconds remaining");
								Thread.sleep(10000);
								Call.sendMessage("[green][Essentials] 10 seconds remaining");
								Thread.sleep(10000);
								if (current >= require) {
									Call.sendMessage("[green][Essentials] Player ban vote success!");
									EssentialPlayer.addtimeban(target.name, target.uuid, 4);
									Global.log(target.name + " / " + target.uuid + " Player has banned due to voting. "+current+"/"+require);

									Path path = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("plugins/Essentials/Logs/Player.log")));
									Path total = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("plugins/Essentials/Logs/Total.log")));
									try {
										JSONObject db = getData(target.uuid);
										String text = db.get("name") + " / " + target.uuid + " Player has banned due to voting. "+current+"/"+require+"\n";
										byte[] result = text.getBytes();
										Files.write(path, result, StandardOpenOption.APPEND);
										Files.write(total, result, StandardOpenOption.APPEND);
									} catch (IOException error) {
										error.printStackTrace();
									}

									netServer.admins.banPlayer(target.uuid);
								} else {
									Call.sendMessage("[green][Essentials][red] Player ban vote failed.");
								}
								vote.clear();
								this.voteactive = false;
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						});
						t.start();
					} else {
						player.sendMessage("[green][Essentials] Vote in processing!");
					}
					break;
				case "y":
					if(this.voteactive) {
						if (vote.contains(player.name)) {
							player.sendMessage("[green][Essentials][scarlet] You're already voted!");
						} else {
							vote.add(player.name);
							int current = vote.size();
							int require = (int) Math.ceil(0.6 * Vars.playerGroup.size()) - current;
							Call.sendMessage("[green][Essentials] " + current + " players voted. need " + require + " more players.");
						}
					} else {
						player.sendMessage("[green][Essentials] Vote not processing!");
					}
					break;
				default:
					player.sendMessage("[Essentials] Invalid option!");
					break;
			}
		});

		handler.<Player>register("suicide", "Kill yourself.", (args, player) -> {
			Player.onPlayerDeath(player);
			Call.sendMessage(player.name+"[] used [green]suicide[] command.");
		});

		handler.<Player>register("kill", "<player>", "Kill player.", (args, player) -> {
			if(player.isAdmin){
				Player other = Vars.playerGroup.find(p->p.name.equalsIgnoreCase(args[0]));
				if(other == null){
					player.sendMessage("[scarlet]No player by that name found!");
					return;
				}
				Player.onPlayerDeath(other);
			} else {
				player.sendMessage("You're not admin!");
			}
		});

		handler.<Player>register("save", "Map save", (args, player) -> {
			if(player.isAdmin) {
				Core.app.post(() -> {
					SaveIO.saveToSlot(1);
					player.sendMessage("Map saved.");
				});
			} else {
				player.sendMessage("You're not admin!");
			}
		});

		handler.<Player>register("time", "Show server time", (args, player) -> {
			LocalDateTime now = LocalDateTime.now();
			DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd a hh:mm.ss");
			String nowString = now.format(dateTimeFormatter);
			player.sendMessage("[green]Server time[white]: "+nowString);
		});

		handler.<Player>register("tr", "Enable/disable Translate all chat", (args, player) -> {
			JSONObject db = getData(player.uuid);
			int value = Integer.parseInt(db.getString("translate"));
			int set;
			if(value == 0){
				set = 1;
				player.sendMessage("[green][INFO] [] translate enabled.");
				player.sendMessage("This translation uses the papago API, some languages may not be supported. (Google is paid)");
				player.sendMessage("Note: Translated letters are marked with [#F5FF6B]this[white] color.");
			} else {
				set = 0;
				player.sendMessage("[green][INFO] [] translate disabled.");
			}

			writeData("UPDATE players SET translate = '"+set+"' WHERE uuid = '"+player.uuid+"'");
		});

		handler.<Player>register("ch", "Send chat to another server.", (args, player) -> {
			JSONObject db = getData(player.uuid);
			int value = Integer.parseInt(db.getString("crosschat"));
			int set;
			if(value == 0){
				set = 1;
				player.sendMessage("[green][INFO] [] Crosschat enabled.");
				player.sendMessage("[yellow]Note[]: [#357EC7][SC][] prefix is 'Send Chat', [#C77E36][RC][] prefix is 'Received Chat'.");
			} else {
				set = 0;
				player.sendMessage("[green][INFO] [] Crosschat disabled.");
			}

			writeData("UPDATE players SET crosschat = '"+set+"' WHERE uuid = '"+player.uuid+"'");
		});

		handler.<Player>register("color", "Enable color nickname", (args, player) -> {
			if(!player.isAdmin){
				player.sendMessage("[green]Notice:[] You're not admin!");
			} else {
				JSONObject db = getData(player.uuid);
				int value = Integer.parseInt(db.getString("colornick"));
				int set;
				if(value == 0){
					set = 1;
					player.sendMessage("[green][INFO] [] colornick enabled.");
					player.sendMessage("[yellow]Note[]: This's a test function and can be forced to change the nickname.");
					player.sendMessage("[yellow]Note[]: Reconnect to apply color nickname effect.");
				} else {
					set = 0;
					player.sendMessage("[green][INFO] [] colornick disabled.");
				}

				writeData("UPDATE players SET colornick = '"+set+"' WHERE uuid = '"+player.uuid+"'");

			}
		});
	}
}