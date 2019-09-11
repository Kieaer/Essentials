package essentials;

import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.collection.Array;
import io.anuke.arc.util.CommandHandler;
import io.anuke.arc.util.Log;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.game.Difficulty;
import io.anuke.mindustry.game.EventType;
import io.anuke.mindustry.game.EventType.PlayerJoin;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.mindustry.net.Administration.PlayerInfo;
import io.anuke.mindustry.net.Packets.KickReason;
import io.anuke.mindustry.plugin.Plugin;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static essentials.EssentialPlayer.createNewDatabase;
import static essentials.EssentialPlayer.getData;
import static io.anuke.mindustry.Vars.playerGroup;

public class Main extends Plugin{
	public Main(){
		if(!Core.settings.getDataDirectory().child("plugins/Essentials/motd.txt").exists()){
			String msg = "To edit this message, modify the [green]motd.txt[] file in the [green]config/plugins/Essentials/[] folder.";
			Core.settings.getDataDirectory().child("plugins/Essentials/motd.txt").writeString(msg);
			Log.info("[Essentials] motd file created.");
		}

        Events.on(PlayerJoin.class, e -> {
			// Show motd
			String motd = Core.settings.getDataDirectory().child("plugins/Essentials/motd.txt").readString();
			e.player.sendMessage(motd);

			// Database read/write
			LocalDateTime now = LocalDateTime.now();
			DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm.ss", Locale.ENGLISH);
			String nowString = now.format(dateTimeFormatter);
			String ip = Vars.netServer.admins.getInfo(e.player.uuid).lastIP;

			if (Core.settings.getDataDirectory().child("plugins/Essentials/players/" + e.player.uuid + ".json").exists()) {
				JSONObject db = getData(e.player.uuid);
				db.put("lastdate", nowString);
				Core.settings.getDataDirectory().child("plugins/Essentials/players/"+e.player.uuid+".json").writeString(String.valueOf(db));
				Log.info("[Essentials] " + e.player.name + " Database file loaded.");
			} else {
				e.player.sendMessage("[green]Database creating... Please wait");
				thread1 r1 = new thread1(ip);
				Thread t1 = new Thread(r1);
				t1.start();
				try {
					t1.join();
				} catch (InterruptedException f) {
					f.printStackTrace();
				}
				String geo = r1.getgeo();
				String geocode = r1.getgeocode();
				createNewDatabase(e.player.name, e.player.uuid, e.player.isAdmin, e.player.isLocal, geo, geocode, 0, 0, 0, 0, Vars.netServer.admins.getInfo(e.player.uuid).timesJoined, Vars.netServer.admins.getInfo(e.player.uuid).timesKicked, "F", nowString, nowString, "none", "none", "00:00.00", "none", 0, 0, 0, 0, 0, "none", 0);
				Log.info("[Essentials] " + e.player.name + "/" + e.player.uuid + " Database file created.");
				e.player.sendMessage("[green]Database created!");
			}

			Runnable playtime = new Runnable(){
				@Override
				public void run() {
					if(playerGroup.size() > 0){
						for(Player p : playerGroup.all()) {
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
							Core.settings.getDataDirectory().child("plugins/Essentials/players/" + p.uuid + ".json").writeString(String.valueOf(db));
						}
					}
				}
			};

			ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
			service.scheduleWithFixedDelay(playtime, 0, 1, TimeUnit.SECONDS);

			// Check previous nickname
			//String test = (String) db.get("uuid");


		});

        //copied from ExamplePlugin
		Events.on(EventType.BuildSelectEvent.class, event -> {
			if (!event.breaking && event.builder != null && event.builder.buildRequest() != null && event.builder.buildRequest().block == Blocks.thoriumReactor && event.builder instanceof Player) {
				Call.sendMessage("[scarlet][NOTICE][] " + ((Player) event.builder).name + "[white] has begun building a [green]Thorium reactor[]!");
			}

			if(!event.breaking && event.builder != null && event.builder.buildRequest() != null && event.builder instanceof Player) {
				JSONObject db = getData(((Player) event.builder).uuid);
				int data = db.getInt("placecount");
				data++;
				db.put("placecount", data);
				Core.settings.getDataDirectory().child("plugins/Essentials/players/"+((Player) event.builder).uuid+".json").writeString(String.valueOf(db));
			}

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
			if(playerGroup.size() > 0){
				for(Player p : playerGroup.all()){
					JSONObject db = getData(p.uuid);
					int data = db.getInt("killcount");
					data++;
					db.put("killcount", data);
					Core.settings.getDataDirectory().child("plugins/Essentials/players/"+p.uuid+".json").writeString(String.valueOf(db));
				}
			}
		});
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

		handler.<Player>register("getpos", "Get your current position info", (args, player) -> player.sendMessage("X: "+Math.round(player.x)+" Y: "+Math.round(player.y)));

		handler.<Player>register("info","Show your information", (args, player) -> {
			// Geolocation thread
			player.sendMessage("[green][INFO][] Getting information...");
			String ip = Vars.netServer.admins.getInfo(player.uuid).lastIP;
			/*
			Runnable r1 = new thread1(ip);
			Thread t1 = new Thread(r1);
			t1.start();
			try {
				t1.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			String geo = ((thread1) r1).getValue();
			*/

			// Get Player information from local storage
			JSONObject db = getData(player.uuid);

			String datatext =
					"Player Information[]\n" +
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
					"[green]Rank[]			: "+db.get("rank")+"\n" +
					"[green]First join[]	: "+db.get("firstdate")+"\n" +
					"[green]Last join[]		: "+db.get("lastdate")+"\n" +
					"[green]Playtime[]		: "+db.get("playtime")+"\n" +
					"[green]Attack clear[]	: "+db.get("attackclear")+"\n" +
					"[green]PvP Win[]		: "+db.get("pvpwincount")+"\n" +
					"[green]PvP Lose[]		: "+db.get("pvplosecount")+"\n" +
					"[green]PvP Surrender[]	: "+db.get("pvpbreakout");
			// Call.onInfoMessage(player.con, datatext);
			player.sendMessage(datatext);
			/*
			//
			//player.sendMessage("[green]lastplacename[]: "+db.get("lastplacename"));
			//player.sendMessage("[green]lastbreakname[]:" +db.get("lastbreakname"));
			//player.sendMessage("[green]lastchat[]: "+db.get("lastchat"));
			//player.sendMessage("[green]reactorcount[]: "+(int)db.get("reactorcount"));
			*/
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

		handler.<Player>register("tempban", "<player> <time>", "timer ban", (args, player) -> {
			/*
			if(!player.isAdmin){

				player.sendMessage("[green]Notice: [] You're not admin!");
			} else {
				JSONObject db = getData(player.uuid);
				Player other = Vars.playerGroup.find(p->p.name.equalsIgnoreCase(args[0]));
				Vars.netServer.admins.banPlayer(other.uuid);

				LocalDateTime now = LocalDateTime.now();
				DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd a hh:mm.ss");
				String myTime = now.format(dateTimeFormatter);
				db.put("bantime", myTime);

				SimpleDateFormat df = new SimpleDateFormat("yy-MM-dd a hh:mm.ss");
				Date d = null;
				try {
					d = df.parse(myTime);
				} catch (ParseException e) {
					e.printStackTrace();
				}
				Calendar cal = Calendar.getInstance();
				cal.setTime(d);
				cal.add(Calendar.DATE, Integer.parseInt(args[1]));
				String newTime = df.format(cal.getTime());

				db.put("bantimeset", newTime);

				String list = Core.settings.getDataDirectory().child("plugins/Essentials/players/banned.json").readString();
				JSONTokener parser = new JSONTokener(list);
				JSONObject object = new JSONObject(parser);
				JSONObject response = (JSONObject) object.get("data");

				JSONObject data = new JSONObject();
				data.put("uuid", other.uuid);
				JSONObject response = new JSONObject();
				response.put("ban", data);
				String json = response.toString();
				Core.settings.getDataDirectory().child("plugins/Essentials/players/banned.json").writeString(json);
			}
			*/
			player.sendMessage("Not avaliable now!");
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
			//Time.run(20f, () -> Effects.effect(Fx.spawnShockwave, player.x, player.y, 10));
			// Failed lol
			player.sendMessage("Not avaliable now!");
		});

		handler.<Player>register("gamerule", "<gamerule>", "Set gamerule", (args, player) -> player.sendMessage("Not avaliable now!"));

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
			player.sendMessage("Not avaliable now!");
		});

		handler.<Player>register("suicide", "Kill yourself.", (args, player) -> {
			player.onPlayerDeath(player);
			Call.sendMessage(player.name+"[] used [green]suicide[] command.");
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
	}
}

class thread1 implements Runnable{
	private String ip;
	private String geo;
	private String geocode;

	thread1(String ip) {
		this.ip = ip;
	}

	@Override
	public void run() {
		Thread.currentThread().setName("Get geolocation thread");
		try {
			String connUrl = "http://ipapi.co/" + ip + "/country_name";
			Element web = Jsoup.connect(connUrl).get().body();
			Document doc = Jsoup.parse(String.valueOf(web));
			geo = doc.text();
			connUrl = "http://ipapi.co/" + ip + "/country";
			web = Jsoup.connect(connUrl).get().body();
			doc = Jsoup.parse(String.valueOf(web));
			geocode = doc.text();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Log.info(geo+"/"+geocode);
	}
	String getgeo(){
		return geo;
	}
	String getgeocode(){
		return geocode;
	}
}
/*
class APIExamTranslateNMT {
	public static void main(String[] args) {
		String clientId = "RNOXzFalw7FMFjBe2mbq";
		String clientSecret = "6k0TWLFmPN";
		try {
			String text = URLEncoder.encode(args[0], "UTF-8");
			String apiURL = "https://openapi.naver.com/v1/papago/n2mt";
			URL url = new URL(apiURL);
			HttpURLConnection con = (HttpURLConnection)url.openConnection();
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
			if(responseCode==200) {
				br = new BufferedReader(new InputStreamReader(con.getInputStream()));
			} else {
				br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
			}
			String inputLine;
			StringBuffer response = new StringBuffer();
			while ((inputLine = br.readLine()) != null) {
				response.append(inputLine);
			}
			br.close();
			Call.sendMessage(response.toString());
		} catch (Exception e) {
			Log.info(e);
		}
	}
}
*/

/*
class TimeThread implements Runnable {
	LocalDateTime now = LocalDateTime.now();
	DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd a hh:mm.ss");
	String myTime = now.format(dateTimeFormatter);

	String uuid = "test";
	JSONObject db = EssentialPlayer.getData(uuid);

	public void run() {
		SimpleDateFormat df = new SimpleDateFormat("yy-MM-dd a hh:mm.ss");
	}
}
*/