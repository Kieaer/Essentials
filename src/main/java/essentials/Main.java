package essentials;

import essentials.net.Client;
import essentials.net.Server;
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
import io.anuke.mindustry.content.Bullets;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static essentials.EssentialConfig.*;
import static essentials.EssentialPlayer.*;
import static essentials.Global.getTeamNoCore;
import static io.anuke.arc.util.Log.err;
import static io.anuke.mindustry.Vars.*;

public class Main extends Plugin{
    private ArrayList<String> vote = new ArrayList<>();
	private boolean voteactive;
    static ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	//state.rules.bannedBlocks;

	public Main() {
		// Start config file
		EssentialConfig.main();

		// Make player DB
		createNewDataFile();

		// SQLite multi-thread
		openconnect();

		// Reset all connected status
		writeData("UPDATE players SET connected = 0");

	    // Start log
		if(logging){
			EssentialLog.main();
		}

		//EssentialAI.main();

		// Update check
		if(update){
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
		}

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

		// Essentials EPG Features
        EssentialEPG.main();

		// TODO Make PvP winner count
		/*
		Events.on(EventType.GameOverEvent.class, e -> {

		});
		*/

		Events.on(EventType.WorldLoadEvent.class, e -> EssentialTimer.playtime = "00:00.00");

        // Set if thorium rector explode
        Events.on(EventType.Trigger.thoriumReactorOverheat, () -> {
            if(detectreactor){
                Call.sendMessage("[scarlet]= WARNING WARNING WARNING =");
                Call.sendMessage("[scarlet]Thorium Reactor Exploded");
                Global.log("Thorium Reactor explode detected!!");
            }
        });

		// Set if player join event
		Events.on(EventType.PlayerConnect.class, e -> {
            JSONObject db = getData(e.player.uuid);
            if(db.has("uuid")){
                if(db.getString("uuid").equals(e.player.uuid)){
                    JSONObject db2 = getData(e.player.uuid);
                    if(db2.get("language").equals("KR")){
                        e.player.sendMessage(EssentialBundle.load(true, "autologin"));
                    } else {
                        e.player.sendMessage(EssentialBundle.load(false, "autologin"));
                    }
                }
            } else {
                // Login require
                String message = "You will need to login with [accent]/login <username> <password>[] to get access to the server.\n" +
                        "If you don't have an account, use the command [accent]/register <username> <password> <password repeat>[].\n\n" +
                        "서버를 플레이 할려면 [accent]/login <사용자 이름> <비밀번호>[] 를 입력해야 합니다.\n" +
                        "만약 계정이 없다면 [accent]/register <사용자 이름> <비밀번호> <비밀번호 재입력>[]를 입력해야 합니다.";
                Team no_core = getTeamNoCore(e.player);
                e.player.setTeam(no_core);
                Call.onPlayerDeath(e.player);
                Call.onInfoMessage(e.player.con, message);
            }

			// Database read/write
			Thread playerthread = new Thread(() -> {
				Thread.currentThread().setName("PlayerJoin Thread");

				// Check if blacklisted nickname
				String blacklist = Core.settings.getDataDirectory().child("plugins/Essentials/blacklist.json").readString();
				JSONTokener parser = new JSONTokener(blacklist);
				JSONArray array = new JSONArray(parser);

				for (int i = 0; i < array.length(); i++){
					if (array.getString(i).equals(e.player.name)){
						e.player.con.kick("Server doesn't allow blacklisted nickname.");
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
			});
			playerthread.start();
		});

        Events.on(PlayerJoin.class, e -> {
			// PvP placetime (WorldLoadEvent isn't work.)
			if(enableantirush && Vars.state.rules.pvp) {
				state.rules.playerDamageMultiplier = 0f;
				state.rules.playerHealthMultiplier = 0.001f;
			}
		});

		Events.on(EventType.PlayerLeave.class, e -> {
			if(!Vars.state.teams.get(e.player.getTeam()).cores.isEmpty()){
				JSONObject db = getData(e.player.uuid);
				e.player.name = db.getString("name");
				writeData("UPDATE players SET connected = '0' WHERE uuid = '"+e.player.uuid+"'");
			}
		});

		// Set if player chat event
		Events.on(EventType.PlayerChatEvent.class, e -> {
			String check = String.valueOf(e.message.charAt(0));
			//check if command
			if(!Vars.state.teams.get(e.player.getTeam()).cores.isEmpty()){
				if(!check.equals("/")) {
					//boolean valid = e.message.matches("\\w+");
					JSONObject db = getData(e.player.uuid);
					boolean crosschat = (boolean) db.get("crosschat");

					EssentialTR.main(e.player, e.message);

					if (clientenable) {
						if (crosschat) {
							Thread chatclient = new Thread(() -> {
								String message = e.player.name.replaceAll("\\[(.*?)]", "") + ": " + e.message;
								Client.main("chat", message, e.player);
							});
							chatclient.start();
						}
					} else if (crosschat) {
						e.player.sendMessage("Currently server isn't enable cross-server client!");
					}
				}
			}
		});

		// Set if player build block event
		Events.on(EventType.BlockBuildEndEvent.class, e -> {
			if (!e.breaking && e.player != null && e.player.buildRequest() != null) {
				JSONObject db = getData(e.player.uuid);
				String name = e.tile.block().name;
				try{
					int data = db.getInt("placecount");
					int exp = db.getInt("exp");

					Yaml yaml = new Yaml();
					Map<String, Object> obj = yaml.load(String.valueOf(Core.settings.getDataDirectory().child("plugins/Essentials/Exp.txt").readString()));
					int blockexp;
					if(obj.get(name) != null) {
						blockexp = (int) obj.get(name);
					} else {
						blockexp = 0;
					}
					int newexp = exp+blockexp;
					data++;

					writeData("UPDATE players SET placecount = '"+data+"', exp = '"+newexp+"' WHERE uuid = '"+e.player.uuid+"'");
				} catch (Exception ex){
					ex.printStackTrace();
				}
			}
		});

		Events.on(EventType.UnitDestroyEvent.class, e -> {
			if(playerGroup != null && playerGroup.size() > 0) {
                for (int i = 0; i < playerGroup.size(); i++) {
                    Player player = playerGroup.all().get(i);
                    JSONObject db = getData(player.uuid);
                    int killcount;
                    if (db.has("killcount")) {
                        killcount = db.getInt("killcount");
                    } else {
                        return;
                    }
                    killcount++;
                    writeData("UPDATE players SET killcount = '" + killcount + "' WHERE uuid = '" + player.uuid + "'");
                }
            }
		});

		Events.on(EventType.DepositEvent.class, e -> {
			for (Player p : playerGroup.all()) {
				if(p.item().item.flammability > 1){
					JSONObject db = getData(p.uuid);
					if(db.getString("language").equals("KR")){
						player.sendMessage(EssentialBundle.load(true, "flammable-disabled"));
					} else {
						player.sendMessage(EssentialBundle.load(false, "flammable-disabled"));
					}
				}
			}
		});

		EssentialTimer job = new EssentialTimer();
		Timer timer = new Timer(true);
		timer.scheduleAtFixedRate(job, 1000, 1000);

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

				closeconnect();

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

	}

	@Override
	public void registerServerCommands(CommandHandler handler){
		handler.register("gameover", "a test", arg -> {
			Global.log("The destruction of the world has begun!");
			Thread t1 = new Thread(() -> {
				for (int x = 0; x < world.width() * 8; x += 16) {
					for (int y = 0; y < world.height() * 8; y += 8) {
						int finalX = x;
						int finalY = y;
						// FULL SPEED!
						Runnable t = () -> {
							Call.createBullet(Bullets.meltdownLaser, finalX, finalY, 0);
                            Call.createBullet(Bullets.fireball, finalX, finalY, 0);
							try {
								Thread.sleep(5);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						};
						pool.execute(t);
					}
				}
			});
			t1.start();
		});

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

		handler.register("allinfo", "[name]", "Show player information", (arg) -> {
			Player other = Vars.playerGroup.find(p->p.name.equalsIgnoreCase(arg[0]));
			if(other != null) {
				JSONObject db = getData(other.uuid);
				String datatext = "\nPlayer Information\n" +
						"========================================\n" +
						"Name: "+other.name+"\n" +
						"UUID: "+other.uuid+"\n" +
						"Mobile: "+other.isMobile+"\n" +
						"Country: "+db.get("country")+"\n" +
						"Block place: "+db.get("placecount")+"\n" +
						"Block break: "+db.get("breakcount")+"\n" +
						"Kill units: "+db.get("killcount")+"\n" +
						"Death count: "+db.get("deathcount")+"\n" +
						"Join count: "+db.get("joincount")+"\n" +
						"Kick count: "+db.get("kickcount")+"\n" +
						"Level: "+db.get("level")+"\n" +
						"XP: "+db.get("reqtotalexp")+"\n" +
						"First join: "+db.get("firstdate")+"\n" +
						"Last join: "+db.get("lastdate")+"\n" +
						"Playtime: "+db.get("playtime")+"\n" +
						"Attack clear: "+db.get("attackclear")+"\n" +
						"PvP Win: "+db.get("pvpwincount")+"\n" +
						"PvP Lose: "+db.get("pvplosecount")+"\n" +
						"PvP Surrender: "+db.get("pvpbreakout");
				Log.info(datatext);
			} else {
				Global.log("Player not found!");
			}
		});

		handler.register("bansync", "Ban list synchronization from master server", (arg) -> {
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

		handler.register("team","[name]", "Change target player team", (arg) -> {
			Player other = playerGroup.find(p -> p.name.equalsIgnoreCase(arg[0]));
			if(other != null){
				int i = other.getTeam().ordinal()+1;
				while(i != other.getTeam().ordinal()){
					if (i >= Team.all.length) i = 0;
					if(!Vars.state.teams.get(Team.all[i]).cores.isEmpty()){
						other.setTeam(Team.all[i]);
						break;
					}
					i++;
				}
				other.kill();
			} else {
				Global.log("Player not found!");
			}
		});

		handler.register("nick", "<name> <newname...>", "Show player information", (arg) -> {
			//writeData("UPDATE players SET name='"+arg[1]+"', WHERE name = '"+arg[0]+"'");
			Global.log("This command isn't supported now!");
		});
	}

	@Override
	public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("login", "<id> <password>", "Access your account", (arg, player) -> {
            if (!Vars.state.teams.get(player.getTeam()).cores.isEmpty()){
                player.sendMessage("[green][Essentials] [orange]You are already logged in");
                return;
            }

            if(EssentialPlayer.login(player, arg[0], arg[1])){
                player.sendMessage("[green][Essentials] [orange]Login success!");
                EssentialPlayer.load(player, arg[0]);
            } else {
                player.sendMessage("[green][Essentials] [scarlet]Login failed!");
            }
        });
        handler.<Player>register("register", "<id> <password> <password_repeat>", "Register account", (arg, player)-> {
            if(EssentialPlayer.register(player, arg[0], arg[1], arg[2])){
				if (Vars.state.rules.pvp){
					int index = player.getTeam().ordinal()+1;
					while (index != player.getTeam().ordinal()){
						if (index >= Team.all.length){
							index = 0;
						}
						if (!Vars.state.teams.get(Team.all[index]).cores.isEmpty()){
							player.setTeam(Team.all[index]);
							break;
						}
						index++;
					}

					Call.onPlayerDeath(player);
				} else {
					player.setTeam(Vars.defaultTeam);
					Call.onPlayerDeath(player);
					player.sendMessage("[green][Essentials] [orange]Account register success!");
				}
            } else {
                player.sendMessage("[green][Essentials] [scarlet]Register failed!");
            }
        });

		handler.<Player>register("votekick", "Disabled.", (arg, player) -> {
			if(Vars.state.teams.get(player.getTeam()).cores.isEmpty()){
				player.sendMessage("[green][Essentials][scarlet] You aren't allowed to use the command until you log in.");
				return;
			}

			JSONObject db = getData(player.uuid);
			if(db.getString("language").equals("KR")){
				player.sendMessage(EssentialBundle.load(true, "votekick-disabled"));
			} else {
				player.sendMessage(EssentialBundle.load(false, "votekick-disabled"));
			}
		});

		handler.<Player>register("motd", "Show server motd.", (arg, player) -> {
			if(Vars.state.teams.get(player.getTeam()).cores.isEmpty()){
				player.sendMessage("[green][Essentials][scarlet] You aren't allowed to use the command until you log in.");
				return;
			}

			JSONObject db = getData(player.uuid);
			String motd;
			if(db.getString("language").equals("KR")){
				motd = Core.settings.getDataDirectory().child("plugins/Essentials/motd_ko.txt").readString();
			} else {
				motd = Core.settings.getDataDirectory().child("plugins/Essentials/motd.txt").readString();
			}
			int count = motd.split("\r\n|\r|\n").length;
			if (count > 10) {
				Call.onInfoMessage(player.con, motd);
			} else {
				player.sendMessage(motd);
			}
		});

		handler.<Player>register("getpos", "Get your current position info", (arg, player) -> {
			if(Vars.state.teams.get(player.getTeam()).cores.isEmpty()){
				player.sendMessage("[green][Essentials][scarlet] You aren't allowed to use the command until you log in.");
				return;
			}

			player.sendMessage("X: "+Math.round(player.x)+" Y: "+Math.round(player.y));
		});

		handler.<Player>register("info", "Show your information", (arg, player) -> {
			if(Vars.state.teams.get(player.getTeam()).cores.isEmpty()){
				player.sendMessage("[green][Essentials][scarlet] You aren't allowed to use the command until you log in.");
				return;
			}

			String ip = Vars.netServer.admins.getInfo(player.uuid).lastIP;
			JSONObject db = getData(player.uuid);
			String datatext;
			if(db.getString("language").equals("KR")){
				datatext = "[#DEA82A]"+EssentialBundle.nload(true, "player-info")+"[]\n" +
						"[#2B60DE]========================================[]\n" +
						"[green]"+EssentialBundle.nload(true, "player-name")+"[] : "+player.name+"[white]\n" +
						"[green]"+EssentialBundle.nload(true, "player-uuid")+"[] : "+player.uuid+"\n" +
						"[green]"+EssentialBundle.nload(true, "player-isMobile")+"[] : "+player.isMobile+"\n" +
						"[green]"+EssentialBundle.nload(true, "player-ip")+"[] : "+ip+"\n" +
						"[green]"+EssentialBundle.nload(true, "player-country")+"[] : "+db.get("country")+"\n" +
						"[green]"+EssentialBundle.nload(true, "player-placecount")+"[] : "+db.get("placecount")+"\n" +
						"[green]"+EssentialBundle.nload(true, "player-breakcount")+"[] : "+db.get("breakcount")+"\n" +
						"[green]"+EssentialBundle.nload(true, "player-killcount")+"[] : "+db.get("killcount")+"\n" +
						"[green]"+EssentialBundle.nload(true, "player-deathcount")+"[] : "+db.get("deathcount")+"\n" +
						"[green]"+EssentialBundle.nload(true, "player-joincount")+"[] : "+db.get("joincount")+"\n" +
						"[green]"+EssentialBundle.nload(true, "player-kickcount")+"[] : "+db.get("kickcount")+"\n" +
						"[green]"+EssentialBundle.nload(true, "player-level")+"[] : "+db.get("level")+"\n" +
						"[green]"+EssentialBundle.nload(true, "player-reqtotalexp")+"[] : "+db.get("reqtotalexp")+"\n" +
						"[green]"+EssentialBundle.nload(true, "player-firstdate")+"[] : "+db.get("firstdate")+"\n" +
						"[green]"+EssentialBundle.nload(true, "player-lastdate")+"[] : "+db.get("lastdate")+"\n" +
						"[green]"+EssentialBundle.nload(true, "player-playtime")+"[] : "+db.get("playtime")+"\n" +
						"[green]"+EssentialBundle.nload(true, "player-attackclear")+"[] : "+db.get("attackclear")+"\n" +
						"[green]"+EssentialBundle.nload(true, "player-pvpwincount")+"[] : "+db.get("pvpwincount")+"\n" +
						"[green]"+EssentialBundle.nload(true, "player-pvplosecount")+"[] : "+db.get("pvplosecount")+"\n" +
						"[green]"+EssentialBundle.nload(true, "player-pvpbreakout")+"[] : "+db.get("pvpbreakout");
			} else {
				datatext = "[#DEA82A]"+EssentialBundle.nload(false, "player-info")+"[]\n" +
						"[#2B60DE]========================================[]\n" +
						"[green]"+EssentialBundle.nload(false, "player-name")+"[] : "+player.name+"[white]\n" +
						"[green]"+EssentialBundle.nload(false, "player-uuid")+"[] : "+player.uuid+"\n" +
						"[green]"+EssentialBundle.nload(false, "player-isMobile")+"[] : "+player.isMobile+"\n" +
						"[green]"+EssentialBundle.nload(false, "player-ip")+"[] : "+ip+"\n" +
						"[green]"+EssentialBundle.nload(false, "player-country")+"[] : "+db.get("country")+"\n" +
						"[green]"+EssentialBundle.nload(false, "player-placecount")+"[] : "+db.get("placecount")+"\n" +
						"[green]"+EssentialBundle.nload(false, "player-breakcount")+"[] : "+db.get("breakcount")+"\n" +
						"[green]"+EssentialBundle.nload(false, "player-killcount")+"[] : "+db.get("killcount")+"\n" +
						"[green]"+EssentialBundle.nload(false, "player-deathcount")+"[] : "+db.get("deathcount")+"\n" +
						"[green]"+EssentialBundle.nload(false, "player-joincount")+"[] : "+db.get("joincount")+"\n" +
						"[green]"+EssentialBundle.nload(false, "player-kickcount")+"[] : "+db.get("kickcount")+"\n" +
						"[green]"+EssentialBundle.nload(false, "player-level")+"[] : "+db.get("level")+"\n" +
						"[green]"+EssentialBundle.nload(false, "player-reqtotalexp")+"[] : "+db.get("reqtotalexp")+"\n" +
						"[green]"+EssentialBundle.nload(false, "player-firstdate")+"[] : "+db.get("firstdate")+"\n" +
						"[green]"+EssentialBundle.nload(false, "player-lastdate")+"[] : "+db.get("lastdate")+"\n" +
						"[green]"+EssentialBundle.nload(false, "player-playtime")+"[] : "+db.get("playtime")+"\n" +
						"[green]"+EssentialBundle.nload(false, "player-attackclear")+"[] : "+db.get("attackclear")+"\n" +
						"[green]"+EssentialBundle.nload(false, "player-pvpwincount")+"[] : "+db.get("pvpwincount")+"\n" +
						"[green]"+EssentialBundle.nload(false, "player-pvplosecount")+"[] : "+db.get("pvplosecount")+"\n" +
						"[green]"+EssentialBundle.nload(false, "player-pvpbreakout")+"[] : "+db.get("pvpbreakout");
			}
			Call.onInfoMessage(player.con, datatext);
		});

		handler.<Player>register("status", "Show server status", (arg, player) -> {
            if(Vars.state.teams.get(player.getTeam()).cores.isEmpty()){
                player.sendMessage("[green][Essentials][scarlet] You aren't allowed to use the command until you log in.");
                return;
            }

			player.sendMessage("[#DEA82A]Server status[]");
			player.sendMessage("[#2B60DE]========================================[]");
			float fps = Math.round((int) 60f / Time.delta());
			float memory = Core.app.getJavaHeap() / 1024 / 1024;
			player.sendMessage(fps+"TPS "+memory+"MB");
			player.sendMessage(Vars.playerGroup.size()+" players online.");
			int idb = 0;
			int ipb = 0;

			Array<PlayerInfo> bans = Vars.netServer.admins.getBanned();
			for (PlayerInfo ignored : bans) {
				idb++;
			}

			Array<String> ipbans = Vars.netServer.admins.getBannedIPs();
			for (String ignored : ipbans) {
				ipb++;
			}
			int bancount = idb+ipb;
			player.sendMessage("Total [scarlet]"+bancount+"[]("+idb+"/"+ipb+") players banned.");
		});

		handler.<Player>register("tpp", "<player> <player>", "Teleport to other players", (arg, player) -> {
            if(Vars.state.teams.get(player.getTeam()).cores.isEmpty()){
                player.sendMessage("[green][Essentials][scarlet] You aren't allowed to use the command until you log in.");
                return;
            }

			JSONObject db = getData(player.uuid);
			Player other1 = null;
			Player other2 = null;
			for (Player p : playerGroup.all()) {
				boolean result1 = p.name.contains(arg[0]);
				if (result1) {
					other1 = p;
				}
				boolean result2 = p.name.contains(arg[1]);
				if (result2) {
					other2 = p;
				}
			}
			if(!player.isAdmin){
				if(db.getString("language").equals("KR")){
					player.sendMessage(EssentialBundle.load(true, "notadmin"));
				} else {
					player.sendMessage(EssentialBundle.load(false, "notadmin"));
				}
			} else {
				if (other1 == null || other2 == null) {
					if(db.getString("language").equals("KR")){
						player.sendMessage(EssentialBundle.load(true, "player-not-found"));
					} else {
						player.sendMessage(EssentialBundle.load(false, "player-not-found"));
					}
					return;
				}
				if (!other1.isMobile || !other2.isMobile) {
					other1.setNet(other2.x, other2.y);
				} else {
					if (db.getString("language").equals("KR")) {
						player.sendMessage(EssentialBundle.load(true, "tp-ismobile"));
					} else {
						player.sendMessage(EssentialBundle.load(false, "tp-ismobile"));
					}
				}
			}
		});

		handler.<Player>register("tp", "<player>", "Teleport to other players", (arg, player) -> {
            if(Vars.state.teams.get(player.getTeam()).cores.isEmpty()){
                player.sendMessage("[green][Essentials][scarlet] You aren't allowed to use the command until you log in.");
                return;
            }

			JSONObject db = getData(player.uuid);
			Player other = null;
			for (Player p : playerGroup.all()) {
				boolean result = p.name.contains(arg[0]);
				if (result) {
					other = p;
				}
			}
			if (other == null) {
				if (db.getString("language").equals("KR")) {
					player.sendMessage(EssentialBundle.load(true, "player-not-found"));
				} else {
					player.sendMessage(EssentialBundle.load(false, "player-not-found"));
				}
				return;
			}
			player.setNet(other.x, other.y);
		});

		handler.<Player>register("kickall", "Kick all players", (arg, player) -> {
            if(Vars.state.teams.get(player.getTeam()).cores.isEmpty()){
                player.sendMessage("[green][Essentials][scarlet] You aren't allowed to use the command until you log in.");
                return;
            }

			if(!player.isAdmin){
				JSONObject db = getData(player.uuid);
				if(db.getString("language").equals("KR")){
					player.sendMessage(EssentialBundle.load(true, "notadmin"));
				} else {
					player.sendMessage(EssentialBundle.load(false, "notadmin"));
				}
			} else {
				Vars.netServer.kickAll(KickReason.gameover);
			}
		});

		handler.<Player>register("tempban", "<player> <time>", "Temporarily ban player. time unit: 1 hours", (arg, player) -> {
            if(Vars.state.teams.get(player.getTeam()).cores.isEmpty()){
                player.sendMessage("[green][Essentials][scarlet] You aren't allowed to use the command until you log in.");
                return;
            }

			JSONObject db = getData(player.uuid);
			if(!player.isAdmin){
				if(db.getString("language").equals("KR")){
					player.sendMessage(EssentialBundle.load(true, "notadmin"));
				} else {
					player.sendMessage(EssentialBundle.load(false, "notadmin"));
				}
			} else {
				Player other = null;
				for (Player p : playerGroup.all()) {
					boolean result = p.name.contains(arg[0]);
					if (result) {
						other = p;
					}
				}
				if (other != null) {
					int bantimeset = Integer.parseInt(arg[1]);
					EssentialPlayer.addtimeban(other.name, other.uuid, bantimeset);
					Call.sendMessage("Player"+other.name+" was killed (ban) by player "+player.name+"!");
				} else {
					if (db.getString("language").equals("KR")) {
						player.sendMessage(EssentialBundle.load(true, "player-not-found"));
					} else {
						player.sendMessage(EssentialBundle.load(false, "player-not-found"));
					}
				}
			}
		});

		handler.<Player>register("me", "[text...]", "broadcast * message", (arg, player) -> {
            if(Vars.state.teams.get(player.getTeam()).cores.isEmpty()){
                player.sendMessage("[green][Essentials][scarlet] You aren't allowed to use the command until you log in.");
                return;
            }

			Call.sendMessage("[orange]*[] "+player.name+"[white] : "+arg[0]);
		});

		handler.<Player>register("difficulty", "<difficulty>", "Set server difficulty", (arg, player) -> {
			if(Vars.state.teams.get(player.getTeam()).cores.isEmpty()){
                player.sendMessage("[green][Essentials][scarlet] You aren't allowed to use the command until you log in.");
                return;
            }

			if(!player.isAdmin){
				JSONObject db = getData(player.uuid);
				if(db.getString("language").equals("KR")){
					player.sendMessage(EssentialBundle.load(true, "notadmin"));
				} else {
					player.sendMessage(EssentialBundle.load(false, "notadmin"));
				}
			} else {
				try {
					Difficulty.valueOf(arg[0]);
					player.sendMessage("Difficulty set to '"+arg[0]+"'.");
				} catch (IllegalArgumentException e) {
					player.sendMessage("No difficulty with name '"+arg[0]+"' found.");
				}
			}
		});

		handler.<Player>register("vote", "<gameover/skipwave/kick/y> [playername...]", "Vote surrender or skip wave, Long-time kick", (arg, player) -> {
            if(Vars.state.teams.get(player.getTeam()).cores.isEmpty()){
                player.sendMessage("[green][Essentials][scarlet] You aren't allowed to use the command until you log in.");
                return;
            }

			JSONObject db = getData(player.uuid);
			switch (arg[0]) {
				case "gameover":
					if (!this.voteactive) {
						this.voteactive = true;
						vote.add(player.name);
						int current = vote.size();
						int require = (int) Math.ceil(0.5 * Vars.playerGroup.size());
						for (int i = 0; i < playerGroup.size(); i++) {
							Player others = playerGroup.all().get(i);
							JSONObject db1 = getData(others.uuid);
							if (db1.get("language") == "KR") {
								others.sendMessage(EssentialBundle.load(true, "vote-gameover"));
							} else {
								others.sendMessage(EssentialBundle.load(false, "vote-gameover"));
							}
						}
						Call.sendMessage("[green][Essentials] Require [scarlet]"+require+"[green] players.");

						Thread t = new Thread(() -> {
							try {
								if (playerGroup != null && playerGroup.size() > 0) {
									for (int i = 0; i < playerGroup.size(); i++) {
										Player others = playerGroup.all().get(i);
										JSONObject db1 = getData(others.uuid);
										if (db1.get("language") == "KR") {
											Thread playeralarm1 = new Thread(() -> {
												try {
													Thread.sleep(10000);
													others.sendMessage(EssentialBundle.load(true, "vote-50sec"));
													Thread.sleep(10000);
													others.sendMessage(EssentialBundle.load(true, "vote-40sec"));
													Thread.sleep(10000);
													others.sendMessage(EssentialBundle.load(true, "vote-30sec"));
													Thread.sleep(10000);
													others.sendMessage(EssentialBundle.load(true, "vote-20sec"));
													Thread.sleep(10000);
													others.sendMessage(EssentialBundle.load(true, "vote-10sec"));
													Thread.sleep(10000);
												} catch (Exception e) {
													e.printStackTrace();
												}
											});
											playeralarm1.start();
										} else {
											Thread playeralarm2 = new Thread(() -> {
												try {
													Thread.sleep(10000);
													others.sendMessage(EssentialBundle.load(false, "vote-50sec"));
													Thread.sleep(10000);
													others.sendMessage(EssentialBundle.load(false, "vote-40sec"));
													Thread.sleep(10000);
													others.sendMessage(EssentialBundle.load(false, "vote-30sec"));
													Thread.sleep(10000);
													others.sendMessage(EssentialBundle.load(false, "vote-20sec"));
													Thread.sleep(10000);
													others.sendMessage(EssentialBundle.load(false, "vote-10sec"));
													Thread.sleep(10000);
												} catch (Exception e) {
													e.printStackTrace();
												}
											});
											playeralarm2.start();
										}
									}
								}
								Thread.sleep(60000);
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
						if (db.getString("language").equals("KR")) {
							player.sendMessage(EssentialBundle.load(true, "vote-in-processing"));
						} else {
							player.sendMessage(EssentialBundle.load(false, "vote-in-processing"));
						}
					}
					break;
				case "skipwave":
					if (!this.voteactive) {
						this.voteactive = true;
						vote.add(player.name);
						int current = vote.size();
						int require = (int) Math.ceil(0.5 * Vars.playerGroup.size());
						for (int i = 0; i < playerGroup.size(); i++) {
							Player others = playerGroup.all().get(i);
							JSONObject db1 = getData(others.uuid);
							if (db1.get("language") == "KR") {
								others.sendMessage(EssentialBundle.load(true, "vote-skipwave"));
							} else {
								others.sendMessage(EssentialBundle.load(false, "vote-skipwave"));
							}
						}
						Call.sendMessage("[green][Essentials] Require [scarlet]"+require+"[green] players.");

						Thread t = new Thread(() -> {
							try {
								if (playerGroup != null && playerGroup.size() > 0) {
									for (int i = 0; i < playerGroup.size(); i++) {
										Player others = playerGroup.all().get(i);
										JSONObject db1 = getData(others.uuid);
										if (db1.get("language") == "KR") {
											Thread playeralarm1 = new Thread(() -> {
												try {
													Thread.sleep(10000);
													others.sendMessage(EssentialBundle.load(true, "vote-50sec"));
													Thread.sleep(10000);
													others.sendMessage(EssentialBundle.load(true, "vote-40sec"));
													Thread.sleep(10000);
													others.sendMessage(EssentialBundle.load(true, "vote-30sec"));
													Thread.sleep(10000);
													others.sendMessage(EssentialBundle.load(true, "vote-20sec"));
													Thread.sleep(10000);
													others.sendMessage(EssentialBundle.load(true, "vote-10sec"));
													Thread.sleep(10000);
												} catch (Exception e) {
													e.printStackTrace();
												}
											});
											playeralarm1.start();
										} else {
											Thread playeralarm2 = new Thread(() -> {
												try {
													Thread.sleep(10000);
													others.sendMessage(EssentialBundle.load(false, "vote-50sec"));
													Thread.sleep(10000);
													others.sendMessage(EssentialBundle.load(false, "vote-40sec"));
													Thread.sleep(10000);
													others.sendMessage(EssentialBundle.load(false, "vote-30sec"));
													Thread.sleep(10000);
													others.sendMessage(EssentialBundle.load(false, "vote-20sec"));
													Thread.sleep(10000);
													others.sendMessage(EssentialBundle.load(false, "vote-10sec"));
													Thread.sleep(10000);
												} catch (Exception e) {
													e.printStackTrace();
												}
											});
											playeralarm2.start();
										}
									}
								}
								Thread.sleep(60000);
								if (current >= require) {
									assert playerGroup != null;
									for (int i = 0; i < playerGroup.size(); i++) {
										Player others = playerGroup.all().get(i);
										JSONObject db1 = getData(others.uuid);
										if (db1.get("language") == "KR") {
											others.sendMessage(EssentialBundle.load(true, "vote-in-processing"));
										} else {
											others.sendMessage(EssentialBundle.load(false, "vote-in-processing"));
										}
									}
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
						if (db.getString("language").equals("KR")) {
							player.sendMessage(EssentialBundle.load(true, "vote-in-processing"));
						} else {
							player.sendMessage(EssentialBundle.load(false, "vote-in-processing"));
						}
					}
					break;
				case "kick":
					if (!this.voteactive) {
						this.voteactive = true;
						vote.add(player.name);
						int current = vote.size();
						int require = (int) Math.ceil(0.5 * Vars.playerGroup.size());
						Player target = playerGroup.find(p -> p.name.equals(arg[1]));
						if (target != null) {
							target.con.kick("You have been kicked by voting.");
						} else {
							if (db.getString("language").equals("KR")) {
								player.sendMessage(EssentialBundle.load(true, "player-not-found"));
							} else {
								player.sendMessage(EssentialBundle.load(false, "player-not-found"));
							}
							this.voteactive = false;
							return;
						}

						for (int i = 0; i < playerGroup.size(); i++) {
							Player others = playerGroup.all().get(i);
							JSONObject db1 = getData(others.uuid);
							if (db1.get("language") == "KR") {
								others.sendMessage(EssentialBundle.load(true, "vote-kick"));
							} else {
								others.sendMessage(EssentialBundle.load(false, "vote-kick"));
							}
						}
						Call.sendMessage("[green][Essentials] Require [white]"+require+"[green] players.");

						Thread t = new Thread(() -> {
							try {
								if (playerGroup != null && playerGroup.size() > 0) {
									for (int i = 0; i < playerGroup.size(); i++) {
										Player others = playerGroup.all().get(i);
										JSONObject db1 = getData(others.uuid);
										if (db1.get("language") == "KR") {
											Thread playeralarm1 = new Thread(() -> {
												try {
													Thread.sleep(10000);
													others.sendMessage(EssentialBundle.load(true, "vote-50sec"));
													Thread.sleep(10000);
													others.sendMessage(EssentialBundle.load(true, "vote-40sec"));
													Thread.sleep(10000);
													others.sendMessage(EssentialBundle.load(true, "vote-30sec"));
													Thread.sleep(10000);
													others.sendMessage(EssentialBundle.load(true, "vote-20sec"));
													Thread.sleep(10000);
													others.sendMessage(EssentialBundle.load(true, "vote-10sec"));
													Thread.sleep(10000);
												} catch (Exception e) {
													e.printStackTrace();
												}
											});
											playeralarm1.start();
										} else {
											Thread playeralarm2 = new Thread(() -> {
												try {
													Thread.sleep(10000);
													others.sendMessage(EssentialBundle.load(false, "vote-50sec"));
													Thread.sleep(10000);
													others.sendMessage(EssentialBundle.load(false, "vote-40sec"));
													Thread.sleep(10000);
													others.sendMessage(EssentialBundle.load(false, "vote-30sec"));
													Thread.sleep(10000);
													others.sendMessage(EssentialBundle.load(false, "vote-20sec"));
													Thread.sleep(10000);
													others.sendMessage(EssentialBundle.load(false, "vote-10sec"));
													Thread.sleep(10000);
												} catch (Exception e) {
													e.printStackTrace();
												}
											});
											playeralarm2.start();
										}
									}
								}
								Thread.sleep(60000);
								if (current >= require) {
									Call.sendMessage("[green][Essentials] Player kick vote success!");
									EssentialPlayer.addtimeban(target.name, target.uuid, 4);
									Global.log(target.name+" / "+target.uuid+" Player has banned due to voting. "+current+"/"+require);

									Path path = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("plugins/Essentials/Logs/Player.log")));
									Path total = Paths.get(String.valueOf(Core.settings.getDataDirectory().child("plugins/Essentials/Logs/Total.log")));
									try {
										JSONObject other = getData(target.uuid);
										String text = other.get("name")+" / "+target.uuid+" Player has banned due to voting. "+current+"/"+require+"\n";
										byte[] result = text.getBytes();
										Files.write(path, result, StandardOpenOption.APPEND);
										Files.write(total, result, StandardOpenOption.APPEND);
									} catch (IOException error) {
										error.printStackTrace();
									}

									netServer.admins.banPlayer(target.uuid);
									Call.onKick(target.con, "You're banned.");
								} else {
									assert playerGroup != null;
									for (int i = 0; i < playerGroup.size(); i++) {
										Player others = playerGroup.all().get(i);
										JSONObject db1 = getData(others.uuid);
										if (db1.get("language") == "KR") {
											player.sendMessage(EssentialBundle.load(true, "vote-failed"));
										} else {
											player.sendMessage(EssentialBundle.load(false, "vote-failed"));
										}
									}
								}
								vote.clear();
								this.voteactive = false;
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						});
						t.start();
					} else {
						if (db.getString("language").equals("KR")) {
							player.sendMessage(EssentialBundle.load(true, "vote-in-processing"));
						} else {
							player.sendMessage(EssentialBundle.load(false, "vote-in-processing"));
						}
					}
					break;
				case "y":
					if (this.voteactive) {
						if (vote.contains(player.name)) {
							player.sendMessage("[green][Essentials][scarlet] You're already voted!");
						} else {
							vote.add(player.name);
							int current = vote.size();
							int require = (int) Math.ceil(0.5 * Vars.playerGroup.size()) - current;
							Call.sendMessage("[green][Essentials] "+current+" players voted. need "+require+" more players.");
						}
					} else {
						if (db.getString("language").equals("KR")) {
							player.sendMessage(EssentialBundle.load(true, "vote-not-processing"));
						} else {
							player.sendMessage(EssentialBundle.load(false, "vote-not-processing"));
						}
					}
					break;
				default:
					this.voteactive = false;
					if (db.getString("language").equals("KR")) {
						player.sendMessage(EssentialBundle.load(true, "vote-invalid"));
					} else {
						player.sendMessage(EssentialBundle.load(false, "vote-invalid"));
					}
					break;
			}
		});

		handler.<Player>register("suicide", "Kill yourself.", (arg, player) -> {
            if(Vars.state.teams.get(player.getTeam()).cores.isEmpty()){
                player.sendMessage("[green][Essentials][scarlet] You aren't allowed to use the command until you log in.");
                return;
            }

			Player.onPlayerDeath(player);
			if (playerGroup != null && playerGroup.size() > 0) {
				for (int i = 0; i < playerGroup.size(); i++) {
					Player others = playerGroup.all().get(i);
					JSONObject db = getData(others.uuid);
					if (db.getString("language").equals("KR")) {
						others.sendMessage("[green][Essentials][] "+player.name+EssentialBundle.nload(true, "suicide"));
					} else {
						others.sendMessage("[green][Essentials][] "+player.name+EssentialBundle.nload(false, "suicide"));
					}
				}
			}
		});

		handler.<Player>register("kill", "<player>", "Kill player.", (arg, player) -> {
            if(Vars.state.teams.get(player.getTeam()).cores.isEmpty()){
                player.sendMessage("[green][Essentials][scarlet] You aren't allowed to use the command until you log in.");
                return;
            }

			JSONObject db = getData(player.uuid);
			if(player.isAdmin){
				Player other = Vars.playerGroup.find(p -> p.name.equalsIgnoreCase(arg[0]));
				if (other == null) {
					if(db.getString("language").equals("KR")){
						player.sendMessage(EssentialBundle.load(true, "player-not-found"));
					} else {
						player.sendMessage(EssentialBundle.load(false, "player-not-found"));
					}
					return;
				}
				Player.onPlayerDeath(other);
			} else {
				if(db.getString("language").equals("KR")){
					player.sendMessage(EssentialBundle.load(true, "notadmin"));
				} else {
					player.sendMessage(EssentialBundle.load(false, "notadmin"));
				}
			}
		});

		handler.<Player>register("save", "Map save", (arg, player) -> {
            if(Vars.state.teams.get(player.getTeam()).cores.isEmpty()){
                player.sendMessage("[green][Essentials][scarlet] You aren't allowed to use the command until you log in.");
                return;
            }

			JSONObject db = getData(player.uuid);
			if(player.isAdmin){
				Core.app.post(() -> {
					SaveIO.saveToSlot(1);
					if(db.getString("language").equals("KR")){
						player.sendMessage(EssentialBundle.load(true, "mapsaved"));
					} else {
						player.sendMessage(EssentialBundle.load(false, "mapsaved"));
					}
				});
			} else {
				if(db.getString("language").equals("KR")){
					player.sendMessage(EssentialBundle.load(true, "notadmin"));
				} else {
					player.sendMessage(EssentialBundle.load(false, "notadmin"));
				}
			}
		});

		handler.<Player>register("time", "Show server time", (arg, player) -> {
            if(Vars.state.teams.get(player.getTeam()).cores.isEmpty()){
                player.sendMessage("[green][Essentials][scarlet] You aren't allowed to use the command until you log in.");
                return;
            }

			JSONObject db = getData(player.uuid);
			LocalDateTime now = LocalDateTime.now();
			DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd a hh:mm.ss");
			String nowString = now.format(dateTimeFormatter);
			if(db.getString("language").equals("KR")){
				player.sendMessage(EssentialBundle.load(true, "servertime")+" "+nowString);
			} else {
				player.sendMessage(EssentialBundle.load(false, "servertime")+" "+nowString);
			}
		});

		handler.<Player>register("tr", "Enable/disable Translate all chat", (arg, player) -> {
            if(Vars.state.teams.get(player.getTeam()).cores.isEmpty()){
                player.sendMessage("[green][Essentials][scarlet] You aren't allowed to use the command until you log in.");
                return;
            }

			JSONObject db = getData(player.uuid);
			boolean value = (boolean) db.get("translate");
			int set;
			if (!value) {
				set = 1;
				if(db.getString("language").equals("KR")){
					player.sendMessage(EssentialBundle.load(true, "translate1"));
					player.sendMessage(EssentialBundle.load(true, "translate2"));
					player.sendMessage(EssentialBundle.load(true, "translate3"));
				} else {
					player.sendMessage(EssentialBundle.load(false, "translate1"));
					player.sendMessage(EssentialBundle.load(false, "translate2"));
					player.sendMessage(EssentialBundle.load(false, "translate3"));
				}
			} else {
				set = 0;
				if(db.getString("language").equals("KR")){
					player.sendMessage(EssentialBundle.load(true, "translate-disable"));
				} else {
					player.sendMessage(EssentialBundle.load(false, "translate-disable"));
				}
			}

			writeData("UPDATE players SET translate = '"+set+"' WHERE uuid = '"+player.uuid+"'");
		});

		handler.<Player>register("ch", "Send chat to another server.", (arg, player) -> {
            if(Vars.state.teams.get(player.getTeam()).cores.isEmpty()){
                player.sendMessage("[green][Essentials][scarlet] You aren't allowed to use the command until you log in.");
                return;
            }

			JSONObject db = getData(player.uuid);
			boolean value = (boolean) db.get("crosschat");
			int set;
			if (!value) {
				set = 1;
				if(db.getString("language").equals("KR")){
					player.sendMessage(EssentialBundle.load(true, "crosschat1"));
					player.sendMessage(EssentialBundle.load(true, "crosschat2"));
				} else {
					player.sendMessage(EssentialBundle.load(false, "crosschat1"));
					player.sendMessage(EssentialBundle.load(false, "crosschat2"));
				}
			} else {
				set = 0;
				if(db.getString("language").equals("KR")){
					player.sendMessage(EssentialBundle.load(true, "crosschat-disable"));
				} else {
					player.sendMessage(EssentialBundle.load(false, "crosschat-disable"));
				}
			}

			writeData("UPDATE players SET crosschat = '"+set+"' WHERE uuid = '"+player.uuid+"'");
		});

		handler.<Player>register("color", "Enable color nickname", (arg, player) -> {
            if(Vars.state.teams.get(player.getTeam()).cores.isEmpty()){
                player.sendMessage("[green][Essentials][scarlet] You aren't allowed to use the command until you log in.");
                return;
            }

			if(!player.isAdmin){
				JSONObject db = getData(player.uuid);
				if(db.getString("language").equals("KR")){
					player.sendMessage(EssentialBundle.load(true, "notadmin"));
				} else {
					player.sendMessage(EssentialBundle.load(false, "notadmin"));
				}
			} else {
				JSONObject db = getData(player.uuid);
				boolean value = (boolean) db.get("colornick");
				int set;
				if (!value) {
					set = 1;
					if(db.getString("language").equals("KR")){
						player.sendMessage(EssentialBundle.load(true, "colornick1"));
						player.sendMessage(EssentialBundle.load(true, "colornick2"));
						player.sendMessage(EssentialBundle.load(true, "colornick3"));
					} else {
						player.sendMessage(EssentialBundle.load(false, "colornick1"));
						player.sendMessage(EssentialBundle.load(false, "colornick2"));
						player.sendMessage(EssentialBundle.load(false, "colornick3"));
					}
				} else {
					set = 0;
					if(db.getString("language").equals("KR")){
						player.sendMessage(EssentialBundle.load(true, "colornick-disable"));
					} else {
						player.sendMessage(EssentialBundle.load(false, "colornick-disable"));
					}
				}
				writeData("UPDATE players SET colornick = '"+set+"' WHERE uuid = '"+player.uuid+"'");
			}
		});

		handler.<Player>register("team", "Change team (PvP only)", (arg, player) -> {
            if(Vars.state.teams.get(player.getTeam()).cores.isEmpty()){
                player.sendMessage("[green][Essentials][scarlet] You aren't allowed to use the command until you log in.");
                return;
            }

			if(Vars.state.rules.pvp){
				if(player.isAdmin){
					int i = player.getTeam().ordinal()+1;
					while (i != player.getTeam().ordinal()) {
						if (i >= Team.all.length) i = 0;
						if(!Vars.state.teams.get(Team.all[i]).cores.isEmpty()){
							player.setTeam(Team.all[i]);
							break;
						}
						i++;
					}
					player.kill();
				} else {
					JSONObject db = getData(player.uuid);
					if(db.getString("language").equals("KR")){
						player.sendMessage(EssentialBundle.load(true, "notadmin"));
					} else {
						player.sendMessage(EssentialBundle.load(false, "notadmin"));
					}
				}
			} else {
				player.sendMessage("This command can use only PvP mode!");
			}
		});

		/*
        handler.<Player>register("powerstat", "messageblock", (arg, player) -> {
            Blocks.message = new MessageBlock("test");
        });
		 */
	}
}