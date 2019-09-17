package essentials;

import io.anuke.arc.ApplicationListener;
import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.collection.Array;
import io.anuke.arc.util.CommandHandler;
import io.anuke.arc.util.Log;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.NetClient;
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
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

import static essentials.EssentialConfig.detectreactor;
import static essentials.EssentialConfig.realname;
import static essentials.EssentialPlayer.getData;
import static essentials.thread.Detectlang.detectlang;
import static io.anuke.arc.util.Log.err;
import static io.anuke.mindustry.Vars.netServer;
import static io.anuke.mindustry.Vars.playerGroup;

public class Main extends Plugin{
	public Main(){
	    // Start config file
	    EssentialConfig.main();

	    // Start chat server
		Runnable chatserver = new EssentialChatServer();
		Thread chat2 = new Thread(chatserver);
		chat2.start();

        // Set if thorium rector explode
        Events.on(EventType.Trigger.thoriumReactorOverheat, () -> {
            if(detectreactor){
                Call.sendMessage("[scarlet]WARNING WARNING WARNING");
                Call.sendMessage("[scarlet]Thorium Reactor Exploded");
                Log.info("Thorium Reactor explode detected!!");
            }
        });

		// Set if player join event
        Events.on(PlayerJoin.class, e -> {
			// Show motd
			String motd = Core.settings.getDataDirectory().child("plugins/Essentials/motd.txt").readString();
			e.player.sendMessage(motd);

			// Database read/write
			EssentialPlayer.main(e.player);

			// Give join exp
            Thread expthread = new Thread(new Runnable() {
                @Override
                public synchronized void run() {
                    EssentialExp.joinexp(e.player.uuid);
                }
            });
            expthread.start();
		});

		// Set if player chat event
		Events.on(EventType.PlayerChatEvent.class, e -> {
			String check = String.valueOf(e.message.charAt(0));
			//check if command
			if(!check.equals("/")) {
				//boolean valid = e.message.matches("\\w+");
				JSONObject db = getData(e.player.uuid);
				boolean translate = Boolean.parseBoolean(String.valueOf(db.get("translate")));
				boolean crosschat = Boolean.parseBoolean(String.valueOf(db.get("crosschat")));

				detectlang(translate, e.player, e.message);
				if (crosschat) {
					Thread chatclient = new Thread(new Runnable() {
						@Override
						public synchronized void run() {
							String message = NetClient.colorizeName(e.player.id, e.player.name)+" [white]: "+e.message;
							EssentialChatClient.main(message, e.player);
						}
					});
					chatclient.start();
				}
			}
		});

		// Set if player build block event
		Events.on(EventType.BlockBuildEndEvent.class, event -> {
			if (!event.breaking && event.player != null && event.player.buildRequest() != null) {
				JSONObject db = getData(event.player.uuid);
				int data = db.getInt("placecount");
				data++;
				db.put("placecount", data);
				Core.settings.getDataDirectory().child("plugins/Essentials/players/" + event.player.uuid + ".json").writeString(String.valueOf(db));
			}
		});

		// todo make block break count
		/*Events.on(EventType.BlockBuildEndEvent.class, event -> {})
			if(event.breaking && event.builder != null && event.builder.buildRequest() != null && event.builder instanceof Player) {
				JSONObject db = getData(((Player) event.builder).uuid);
				int data = db.getInt("breakcount");
				data++;
				db.put("breakcount", data);
				Core.settings.getDataDirectory().child("plugins/Essentials/players/"+((Player) event.builder).uuid+".json").writeString(String.valueOf(db));
			}
		});


		// Count unit destory
		Events.on(EventType.UnitDestroyEvent.class, event -> {
			if(playerGroup != null && playerGroup.size() > 0){
				for(int i=0;i<playerGroup.size();i++){
					Player player = playerGroup.all().get(i);
					JSONObject db = getData(player.uuid);
					int data = db.getInt("killcount");
					data++;
					db.put("killcount", data);
					Core.settings.getDataDirectory().child("plugins/Essentials/players/"+player.uuid+".json").writeString(String.valueOf(db));
				}
			}
		});
		*/

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
					Log.info("[Essentials] Play/bantime counting thread disabled.");
				} catch (Exception e){
					Log.err("[Essentials] Failure to disable Playtime counting thread!");
					e.printStackTrace();
				}

				// Kill Chat server thread
				try {
					EssentialChatServer.active = false;
					EssentialChatServer.serverSocket.close();
					Log.info("[EssentialsChat] Chat server thread disabled.");
				} catch (Exception e){
					Log.err("[Essentials] Failure to disable Playtime counting thread!");
					e.printStackTrace();
				}
			}
		});

        // Alert Realname event
        if(realname){
            Log.info("[Essentials] Realname enabled.");
        }

        // Alert thorium reactor explode detect event
        if(detectreactor){
            Log.info("[Essentials] Thorium reactor overheat detect enabled.");
        }

        timer.scheduleAtFixedRate(playtime, 0, 1000);
        Log.info("[Essentials] Play/bantime counting thread started.");
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
						Log.info("[Essentials] banned the "+other.name+" player for "+arg[1]+" hour.");
					} else {
						err("No matches found.");
					}
					break;
				case "ip":
					netServer.admins.banPlayerIP(arg[1]);
					Log.info("[Essentials] banned the "+other.name+" player for "+arg[1]+" hour.");
					break;
				default:
					err("Invalid type.");
					break;
			}
		});
	}

	@Override
	public void registerClientCommands(CommandHandler handler){
		String url = "jdbc:sqlite:"+Core.settings.getDataDirectory().child("plugins/Essentials/player.sqlite3");

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
					"[green]Name[]			: "+player.name+"\n" +
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

		handler.<Player>register("vote", "<gameover/map>", "Vote surrender or maps.", (args, player) -> {
			double per = 0.75;
			HashSet<Player> votes = new HashSet<>();
			votes.add(player);

			switch(args[0]){
				case "gameover":
					int v1 = votes.size();
					int v2 = (int) Math.ceil(per * Vars.playerGroup.size());
					Call.sendMessage("Game over vote [orange]"+v1+"[]/[green]"+v2+"[] required");
					if (v1<v2){return;}
					votes.clear();
					Events.fire(new EventType.GameOverEvent(Team.crux));;
					break;
				case "map":
					player.sendMessage("Not available map vote features now!");
					// todo make map votes
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
					break;
				default:
					player.sendMessage("Invalid option!");
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
			String value = String.valueOf(db.get("translate"));
			String set;
			String sql = "UPDATE players SET translate = ? WHERE uuid = ?";
			if(value.equals("false")){
				set = "true";
				player.sendMessage("[green][INFO] [] translate enabled.");
				player.sendMessage("Note: Translated letters are marked with [#F5FF6B]this[white] color.");
			} else {
				set = "false";
				player.sendMessage("[green][INFO] [] translate disabled.");
			}

			try{
				Class.forName("org.sqlite.JDBC");
				Connection conn = DriverManager.getConnection(url);

				PreparedStatement pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, set);
				pstmt.setString(2, player.uuid);
				pstmt.executeUpdate();
				pstmt.close();
				conn.close();
			} catch (Exception e){
				e.printStackTrace();
			}
		});

		handler.<Player>register("ch", "Send chat to another server.", (args, player) -> {
			JSONObject db = getData(player.uuid);
			String value = String.valueOf(db.get("crosschat"));
			String set;
			String sql = "UPDATE players SET crosschat = ? WHERE uuid = ?";
			if(value.equals("false")){
				set = "true";
				player.sendMessage("[green][INFO] [] Crosschat enabled.");
				player.sendMessage("[yellow]Note[]: [#357EC7][SC][] prefix is 'Send Chat', [#C77E36][RC][] prefix is 'Received Chat'.");
			} else {
				set = "false";
				player.sendMessage("[green][INFO] [] Crosschat disabled.");
			}

			try{
				Class.forName("org.sqlite.JDBC");
				Connection conn = DriverManager.getConnection(url);

				PreparedStatement pstmt = conn.prepareStatement(sql);
				pstmt.setString(1, set);
				pstmt.setString(2, player.uuid);
				pstmt.executeUpdate();
				pstmt.close();
				conn.close();
			} catch (Exception e){
				e.printStackTrace();
			}
		});
	}
}