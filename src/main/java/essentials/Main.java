package essentials;

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
import io.anuke.mindustry.net.Packets.KickReason;
import io.anuke.mindustry.plugin.Plugin;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

import static essentials.EssentialPlayer.createNewDatabase;
import static essentials.EssentialPlayer.getData;

public class Main extends Plugin{
	public Main(){
		if(!Core.settings.getDataDirectory().child("plugins/Essentials/motd.txt").exists()){
			String msg = "To edit this message, modify the [green]motd.txt[] file in the [green]config/plugins/Essentials/[] folder.";
			Core.settings.getDataDirectory().child("plugins/Essentials/motd.txt").writeString(msg);
			Log.info("[Essentials] motd file created.");
		}

		/*
		Runnable timeban = new TimeThread();
		Thread t2 = new Thread(timeban);
		t2.start();
		try {
			t2.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		t2.start();
		*/

        Events.on(PlayerJoin.class, e -> {
        	Player player = new Player();
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd a hh:mm.ss");
            String nowString = now.format(dateTimeFormatter);
			if (Core.settings.getDataDirectory().child("plugins/Essentials/players/"+player.uuid+".json").exists()) {
				String ip = Vars.netServer.admins.getInfo(player.uuid).lastIP;
				Runnable r1 = new thread1(ip);
				Thread t1 = new Thread(r1);
				t1.start();
				try {
					t1.join();
				} catch (InterruptedException f) {
					f.printStackTrace();
				}
				String geo = ((thread1) r1).getValue();

				createNewDatabase(e.player.name, e.player.uuid, e.player.isAdmin, e.player.isLocal, geo, 0,0, 0, 0, 0, 0, "F", nowString, nowString, "none", "none", 0, "none", 0, 0, 0, 0, 0, "none", 0);
				Log.info("[Essentials] "+e.player.name+"/"+e.player.uuid+" Database file created.");
			} else {
				getData(e.player.uuid);
				Log.info("[Essentials] "+e.player.name+" Database file loaded.");
			}
        });
		/*
		Events.on(EventType.BlockBuildEndEvent.class, e -> {
			Player player = new Player();
			if(e.team == player.getTeam()){
				if(e.breaking){
					JSONObject db = getData(player.uuid);
					int count = (int) db.get("breakcount");
					count++;
					db.put("breakcount", count);
				}else{
					JSONObject db = getData(player.uuid);
					int count = (int) db.get("placecount");
					count++;
					db.put("placecount", count);
				}
			}
		});

		Events.on(EventType.UnitDestroyEvent.class, e -> {
			Player player = new Player();
			if(e.unit.getTeam() != player.getTeam()){
				JSONObject db = getData(player.uuid);
				int count = (int) db.get("killcount");
				count++;
				db.put("killcount", count);
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
					//"[green]Country[]		: "+geo+"\n" +
					"[green]Block place[]	: "+(int)db.get("placecount")+"\n" +
					"[green]Block break[]	: "+(int)db.get("breakcount")+"\n" +
					"[green]Kill units[]	: "+(int)db.get("killcount")+"\n" +
					"[green]Death count[]	: "+(int)db.get("deathcount")+"\n" +
					"[green]Join count[]	: "+(int)db.get("joincount")+"\n" +
					"[green]Kick count[]	: "+(int)db.get("kickcount")+"\n" +
					"[green]Rank[]			: "+db.get("rank")+"\n" +
					"[green]First joindate[]: "+db.get("firstdate")+"\n" +
					"[green]Playtime[]		: "+(int)db.get("playtime")+"\n" +
					"[green]Attack clear[]	: " +(int)db.get("attackclear")+"\n" +
					"[green]PvP Win[]		: "+(int)db.get("pvpwincount")+"\n" +
					"[green]PvP Lose[]		: "+(int)db.get("pvplosecount")+"\n" +
					"[green]PvP Surrender[]	: "+(int)db.get("pvpbreakout");
			// Call.onInfoMessage(player.con, datatext);
			player.sendMessage(datatext);
			/*
			//player.sendMessage("[green]lastdate[]: "+db.get("lastdate"));
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
			player.sendMessage("Not avaliable now!");
		});

		handler.<Player>register("gamerule", "<gamerule>", "Set gamerule", (args, player) -> {
			player.sendMessage("Not avaliable now!");
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
			player.sendMessage("Not avaliable now!");
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
			player.sendMessage("Not avaliable now!");
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

	thread1(String ip) {
		this.ip = ip;
	}

	@Override
	public void run() {
		Player player = new Player();
		Thread.currentThread().setName("Get geolocation thread");
		//String ip = Vars.netServer.admins.getInfo(player.uuid).lastIP;
		// Get IP Geolocation
		String connUrl = "http://ipapi.co/" + ip + "/country_name";
		Document doc = null;
		try {
			doc = Jsoup.connect(connUrl).get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		geo = doc.text();
		return;
	}

	public String getValue(){
		return geo;
	}
}
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