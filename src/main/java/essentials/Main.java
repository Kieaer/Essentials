package essentials;

import essentials.thread.GeoThread;
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
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static essentials.EssentialConfig.detectreactor;
import static essentials.EssentialConfig.realname;
import static essentials.EssentialPlayer.createNewDatabase;
import static essentials.EssentialPlayer.getData;
import static io.anuke.arc.util.Log.err;
import static io.anuke.mindustry.Vars.netServer;
import static io.anuke.mindustry.Vars.playerGroup;

public class Main extends Plugin{
	public Main(){
		try {
			EssentialConfig.main();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Runnable chatserver = new EssentialChatServer();
		Thread chat2 = new Thread(chatserver);
		chat2.start();

		// Startup
		if(!Core.settings.getDataDirectory().child("plugins/Essentials/banned.json").exists()){
			JSONObject data = new JSONObject();
			String json = data.toString();
			Core.settings.getDataDirectory().child("plugins/Essentials/banned.json").writeString(json);
		}

		if(!Core.settings.getDataDirectory().child("plugins/Essentials/motd.txt").exists()){
			String msg = "To edit this message, modify the [green]motd.txt[] file in the [green]config/plugins/Essentials/[] folder.";
			Core.settings.getDataDirectory().child("plugins/Essentials/motd.txt").writeString(msg);
			Log.info("[Essentials] motd file created.");
		}

		if(realname){
			Log.info("[Essentials] Realname enabled.");
		}

		if(detectreactor){
			Log.info("[Essentials] Thorium reactor overheat detect enabled.");
		}

        Events.on(PlayerJoin.class, e -> {
			JSONObject db = null;
			try{
				db = getData(e.player.uuid);
			} catch (Exception error){
				Log.info(e.player.name+" data not found!");
			}

			// Show motd
			String motd = Core.settings.getDataDirectory().child("plugins/Essentials/motd.txt").readString();
			e.player.sendMessage(motd);

			// Database read/write
			LocalDateTime now = LocalDateTime.now();
			DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm.ss", Locale.ENGLISH);
			String nowString = now.format(dateTimeFormatter);
			String ip = Vars.netServer.admins.getInfo(e.player.uuid).lastIP;

			if (Core.settings.getDataDirectory().child("plugins/Essentials/players/" + e.player.uuid + ".json").exists()) {
				db.put("lastdate", nowString);
				String checkgeo = (String) db.get("country");
				if(checkgeo.equals("invalid")){
					Runnable georun = new GeoThread(ip);
					Thread geothread = new Thread(georun);
					try {
						geothread.start();
						geothread.join();
					} catch (InterruptedException ex) {
						ex.printStackTrace();
					}

					String geo = GeoThread.getgeo();
					String geocode = GeoThread.getgeocode();
					db.put("country", geo);
					db.put("country_code", geocode);
				}
				Core.settings.getDataDirectory().child("plugins/Essentials/players/"+e.player.uuid+".json").writeString(String.valueOf(db));

				// Set realname
				if(realname){
					e.player.name = (String) db.get("name");
				}

				Log.info("[Essentials] " + e.player.name + " Database file loaded.");
			} else {
				e.player.sendMessage("[green]Database creating... Please wait");

				Runnable georun = new GeoThread(ip);
				Thread geothread = new Thread(georun);
				try {
					geothread.start();
					geothread.join();
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				}

				String geo = GeoThread.getgeo();
				String geocode = GeoThread.getgeocode();
				int timesjoined = Vars.netServer.admins.getInfo(e.player.uuid).timesJoined;
				int timeskicked = Vars.netServer.admins.getInfo(e.player.uuid).timesKicked;
				createNewDatabase(e.player.name, e.player.uuid, e.player.isAdmin, e.player.isLocal, geo, geocode,
						0, 0, 0, 0, timesjoined,
						timeskicked, 1, 0, 500, "0(500) / 500", nowString, nowString, "none",
						"none", "00:00.00", "none", 0, 0, 0,
						0, 0, "none", 0, false);
				Log.info("[Essentials] " + e.player.name + "/" + e.player.uuid + " Database file created.");
				e.player.sendMessage("[green]Database created!");
			}

			// Give join exp
			EssentialExp.joinexp(e.player.uuid);
		});

		Events.on(EventType.PlayerChatEvent.class, e -> {
			String check = String.valueOf(e.message.charAt(0));
			//check if command
			if(!check.equals("/")) {
				boolean valid = e.message.matches("\\w+");
				JSONObject db = getData(e.player.uuid);
				boolean translate = (boolean) db.get("translate");
				// check if enable translate
				if (!valid && translate) {
					String clientId = "RNOXzFalw7FMFjBe2mbq";
					String clientSecret = "6k0TWLFmPN";
					try {
						String text = URLEncoder.encode(e.message, "UTF-8");
						String apiURL = "https://openapi.naver.com/v1/papago/n2mt";
						URL url = new URL(apiURL);
						HttpURLConnection con = (HttpURLConnection) url.openConnection();
						con.setRequestMethod("POST");
						con.setRequestProperty("X-Naver-Client-Id", clientId);
						con.setRequestProperty("X-Naver-Client-Secret", clientSecret);

						String postParams = "source=ko&target=en&text=" + text;
						con.setDoOutput(true);
						DataOutputStream wr = new DataOutputStream(con.getOutputStream());
						wr.writeBytes(postParams);
						wr.flush();
						wr.close();
						int responseCode = con.getResponseCode();
						BufferedReader br;
						if (responseCode == 200) {
							br = new BufferedReader(new InputStreamReader(con.getInputStream()));
						} else {
							br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
						}
						String inputLine;
						StringBuilder response = new StringBuilder();
						while ((inputLine = br.readLine()) != null) {
							response.append(inputLine);
						}
						br.close();
						JSONTokener parser = new JSONTokener(response.toString());
						JSONObject object = new JSONObject(parser);
						JSONObject v1 = (JSONObject) object.get("message");
						JSONObject v2 = (JSONObject) v1.get("result");
						String v3 = String.valueOf(v2.get("translatedText"));
						e.player.sendMessage("["+NetClient.colorizeName(e.player.id, e.player.name)+"[white]: [#F5FF6B]" + v3);
					} catch (Exception f) {
						f.getStackTrace();
					}
				}
			}
		});

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
		*/

		// Count unit destory (Temporary disabled)
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

		Timer timer = new Timer();
		TimerTask playtime = new TimerTask(){
			@Override
			public synchronized void run() {
				// Player playtime counting
				if(playerGroup.size() > 0){
					for(int i = 0; i < playerGroup.size(); i++){
						Player p = playerGroup.all().get(i);
						JSONObject db = getData(p.uuid);
						String data = db.getString("playtime");
						SimpleDateFormat format = new SimpleDateFormat("HH:mm.ss");
						Date d1;
						Calendar cal;
						String newTime = null;
						try {
							d1 = format.parse(data);
							cal = Calendar.getInstance();
							cal.setTime(d1);
							cal.add(Calendar.SECOND, 1);
							newTime = format.format(cal.getTime());
						} catch (ParseException e1) {
							e1.printStackTrace();
						}
						db.put("playtime", newTime);

						// Exp caculating
						int exp = (int) db.get("exp");
						db.put("exp", exp+(int)(Math.random()*5)+(int)db.get("level"));

						Core.settings.getDataDirectory().child("plugins/Essentials/players/" + p.uuid + ".json").writeString(String.valueOf(db));

						EssentialExp.exp(p.name, p.uuid);
					}
				}

				// Temporarily ban players time counting
				String db = Core.settings.getDataDirectory().child("plugins/Essentials/banned.json").readString();
				JSONTokener parser = new JSONTokener(db);
				JSONObject object = new JSONObject(parser);

				LocalDateTime now = LocalDateTime.now();
				DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd a hh:mm.ss", Locale.ENGLISH);
				String myTime = now.format(dateTimeFormatter);

				for (int i = 0; i < object.length(); i++) {
					JSONObject value1 = (JSONObject) object.get(String.valueOf(i));
					String date = (String) value1.get("date");
					String uuid = (String) value1.get("uuid");
					String name = (String) value1.get("name");

					if (date.equals(myTime)) {
						Log.info(myTime);
						object.remove(String.valueOf(i));
						Core.settings.getDataDirectory().child("plugins/Essentials/banned.json").writeString(String.valueOf(object));
						Log.info("[Essentials] " + name + "/" + uuid + " player unbanned!");
					}
				}

				// todo make dynamp for mindustry (extreme hard work)
				//Dynmap.takeMapScreenshot();
			}
		};

		// Alarm if thorium reactor explode
        Events.on(EventType.Trigger.thoriumReactorOverheat, () -> {
            if(detectreactor){
                Call.sendMessage("[scarlet]WARNING WARNING WARNING");
                Call.sendMessage("[scarlet]Thorium Reactor Exploded");
                Log.info("Thorium Reactor explode detected!!");
            }
        });

        Events.on(EventType.Trigger.impactPower, () -> {
            Call.sendMessage("[scarlet]power!");
            Log.info("[scarlet]power!");
        });

		timer.scheduleAtFixedRate(playtime, 0, 1000);
		Log.info("[Essentials] Play/bantime counting thread started.");

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
	}

	@Override
	public void registerServerCommands(CommandHandler handler){
		//
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

		handler.<Player>register("spawnmob", "Spawn mob", (args, player) -> {
			if(!player.isAdmin){
				player.sendMessage("[green]Notice: [] You're not admin!");
			} else {
				player.sendMessage("source here");
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

		handler.<Player>register("effect", "make effect", (args, player) -> {
			// todo make effect on in-game
			//Time.run(20f, () -> Effects.effect(Fx.spawnShockwave, player.x, player.y, 10));
			// Failed lol
			player.sendMessage("Not avaliable now!");
		});

		handler.<Player>register("gamerule", "<gamerule>", "Set gamerule", (args, player) -> player.sendMessage("Not avaliable now!"));

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
			boolean value = (boolean) db.get("translate");
			if(!value){
				db.put("translate", true);
				Core.settings.getDataDirectory().child("plugins/Essentials/players/" + player.uuid + ".json").writeString((String.valueOf(db)));
				player.sendMessage("[green][INFO] [] Auto-translate enabled.");
				player.sendMessage("Note: Translated letters are marked with [#F5FF6B]this[white] color.");
			} else {
				db.put("translate", false);
				Core.settings.getDataDirectory().child("plugins/Essentials/players/" + player.uuid + ".json").writeString((String.valueOf(db)));
				player.sendMessage("[green][INFO] [] Auto-translate disabled.");
			}
		});

		handler.<Player>register("ch", "<chat>", "Send chat to another server.", (args, player) -> {
			Thread chatclient = new Thread(new Runnable() {
				@Override
				public synchronized void run() {
					String message = "["+NetClient.colorizeName(player.id, player.name)+"[white]: "+"] "+args[0];
					EssentialChatClient.main(message);
					Call.sendMessage("sented!");
				}
			});
			chatclient.start();
		});
	}
}