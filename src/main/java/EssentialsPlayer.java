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

import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.Vars;

public class EssentialsPlayer{
	/*
	public String chat;
	public String ip;
	public String name = "name";
	public String uuid;
	public boolean isAdmin;
	public boolean isLocal = false;
	public int placecount;
	public int breakcount;
	public int killcount;
	public int deathcount;
	public int joincount;
	public int kickcount;
	public String rank;
	*/
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
	/*
	public static int placecount(Player player){
		if place blocks{
			placecount++;
		}
	}

	public static int breakcount(Player player){
		if break blocks{
			breakcount++;
		}
	}

	public static int killcount(Player player){
		if kill enemies{
			killcount++;
		}
	}

	public static int deathcount(Player player){
		if death{
			deathcount++;
		}
	}

	public static int joincount(Player player){
		if place blocks{
			joincount++;
		}
	}

	public static int kickcount(Player player){
		if place blocks{
			kickcount++;
		}
	}

	public static String rank(Player player){
		//source
	}
	*/
}