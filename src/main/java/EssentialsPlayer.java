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

import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.Vars;

public class EssentialsPlayer extends Plugin{
	
	public String chat;
	public String ip;
	public String name;
	public String uuid;
	public boolean isAdmin;
	public boolean isLocal;;
	public int placecount;
	public int breakcount;
	public int killcount;
	public int deathcount;
	public int joincount;
	public int kickcount;
	//public String rank;
	
	public static String chat(Player player){
		String chat = player.lastText;
		return chat;
	}

	public static String ip(Player player){
		String ip = Vars.netServer.admins.getInfo(player.uuid).lastIP;
		return ip;
	}

	public static String name(Player player){
		String name = player.name;
		return name;
	}

	public static String uuid(Player player){
		String uuid = player.uuid;
		return uuid;
	}

	public static boolean isAdmin(Player player){
		boolean isAdmin = player.isAdmin;
		return isAdmin;
	}

	public static boolean isLocal(Player player){
		boolean isLocal = player.isLocal;
		return isLocal;
	}
	
	public static int placecount(){
		int placecount = playerinfo().result[0];
		return placecount;
	}

	public static int breakcount(Player player){
		int breakcount = playerinfo().result[1];
		return breakcount;
	}

	public static int killcount(){
		int killcount = playerinfo().result[2];
		return killcount;
	}

	public static int deathcount(){
		int deathcount = playerinfo().result[3];
		return deathcount;
	}
	
	public static int joincount(Player player){
		int joincount = Vars.netServer.admins.getInfo(player.uuid).timesJoined;
		return joincount;
	}

	public static int kickcount(Player player){
		int joincount = Vars.netServer.admins.getInfo(player.uuid).timesKicked;
		return joincount;
	}

	//public static String rank(Player player){
	//	//source
	//}
	
	public static String playerinfo(Player player){
		//EventType event = new EventType();
		if(!Core.settings.getDataDirectory().child("plugins/Essentials/players/"+player.name+".txt").exists()){
			String name = player.name;
			String uuid = player.uuid;
			String lastIP = Vars.netServer.admins.getInfo(player.uuid).lastIP;
			String lastText = player.lastText;
			boolean isAdmin = player.isAdmin;
			boolean isLocal = player.isLocal;
			int joincount = Vars.netServer.admins.getInfo(player.uuid).timesJoined;
			int kickcount = Vars.netServer.admins.getInfo(player.uuid).timesKicked;
			int pce = 0;
			int bce = 0;
			int kce = 0;
			int dce = 0;
			String text = "Name: "+name+"\nUUID: "+uuid+"\nIP: "+lastIP+"\nlastText: "+lastText+"\nAdmin: "+isAdmin+"\nLocal: "+isLocal+"\njoincount: "+joincount+"\nkickcount: "+kickcount+"\nplacecount: "+pce+"\nbreakcount: "+bce+"\nkillcount: "+kce+"\ndeathcount: "+dce;
			Core.settings.getDataDirectory().child("plugins/Essentials/player/"+name+".txt").writeString(text);
		}
		String info = Core.settings.getDataDirectory().child("plugins/Essentials/player/"+player.name+".txt").readString();

		String wordsArray[] = info.split("\\r?\\n");

		String pc = "placecount: ";
		String bc = "breakcount: ";
		String kc = "killcount: ";
		String dc = "deathcount: ";

		String[] result = new String[4];

		for(String word : wordsArray) {
		    if(word.indexOf(pc) != -1) {
		        result[0] = word.replace(pc,"");
		    }
		    if(word.indexOf(kc) != -1) {
		    	result[1] = word.replace(bc,"");
		    }
		    if(word.indexOf(bc) != -1) {
		    	result[2] = word.replace(kc,"");
		    }
		    if(word.indexOf(dc) != -1) {
		    	result[3] = word.replace(dc,"");
		    }
		}
	}
}