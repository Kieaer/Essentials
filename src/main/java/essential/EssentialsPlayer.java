package essential;

import io.anuke.arc.Core;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.util.Log;
import io.anuke.mindustry.entities.type.Player;

import java.sql.*;

public class EssentialsPlayer{
	public static void createNewDatabase(String filename){
		FileHandle url = Core.settings.getDataDirectory().child("plugins/Essentials/database.db");
		try(Connection conn = DriverManager.getConnection(String.valueOf(url))){
			if(conn != null){
				DatabaseMetaData meta = conn.getMetaData();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void createNewTable(){
		String url = "jdbc:sqlite:"+Core.settings.getDataDirectory().child("plugins/Essentials/")+"database.db";
		String sql = "CREATE TABLE `players`.`players` ( " +
				"`id` INT NOT NULL," +
				"`name` TINYTEXT NOT NULL," +
				"`uuid` TINYTEXT NOT NULL," +
				"`isAdmin` BOOLEAN NOT NULL," +
				"`isLocal` BOOLEAN NOT NULL," +
				"`placecount` MEDIUMINT NOT NULL," +
				"`breakcount` MEDIUMINT NOT NULL," +
				"`killcount` MEDIUMINT NOT NULL," +
				"`deathcount` MEDIUMINT NOT NULL," +
				"`joincount` MEDIUMINT NOT NULL," +
				"`kickcount` MEDIUMINT NOT NULL," +
				"`rank` TINYTEXT NOT NULL," +
				"`firstdate` DATE NOT NULL," +
				"`lastdate` DATE NOT NULL," +
				"`lastblockplace` TINYTEXT NOT NULL," +
				"`lastblockbreak` TINYTEXT NOT NULL," +
				"`playtime` MEDIUMINT NOT NULL," +
				"`lastchat` TEXT NULL DEFAULT NULL," +
				"`attackclear` SMALLINT NOT NULL," +
				"`pvpwincount` SMALLINT NOT NULL," +
				"`pvplosecount` SMALLINT NOT NULL," +
				"`pvpbreakout` SMALLINT NOT NULL," +
				"`reactorcount` SMALLINT NOT NULL," +
				"PRIMARY KEY (`id`)) ENGINE = InnoDB CHARSET=utf8 COLLATE utf8_general_ci;";

		try (Connection conn = DriverManager.getConnection(url);
			 Statement stmt = conn.createStatement()) {
			// create a new table
			stmt.execute(sql);
		} catch (SQLException e) {
			Log.info("SQL ERROR! "+e);
		}
	}

	private Connection connect(){
		String url = "jdbc:sqlite:"+Core.settings.getDataDirectory().child("plugins/Essentials/")+"database.db";
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(url);
		} catch (SQLException e){
			Log.info("SQL ERROR! "+e);
		}
		return conn;
	}

	public void insert(String name, String uuid, boolean isAdmin, boolean isLocal, int placecount, int breakcount, int killcount, int deathcount, int joincount, int kickcount, String rank, String firstdate, String lastdate, String lastblockplace, String lastblockbreak, int playtime, String lastchat, int attackclear, int pvpwincount, int pvplosecount, int pvpbreakout, int reactorcount){
		String sql = "INSERT INTO players(name, uuid, isAdmin, isLocal, placecount, breakcount, killcount, deathcount, joincount, kickcount, rank, firstdate, lastdate, lastblockplace, lastblockbreak, playtime, lastchat, attackclear, pvpwincount, pvplosecount, pvpbreakout, reactorcount) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		try(Connection conn = this.connect();
			PreparedStatement pstmt = conn.prepareStatement(sql)){
			pstmt.setString(1, name);
			pstmt.setString(2, uuid);
			pstmt.setBoolean(3, isAdmin);
			pstmt.setBoolean(4, isLocal);
			pstmt.setInt(5, placecount);
			pstmt.setInt(6, breakcount);
			pstmt.setInt(7, killcount);
			pstmt.setInt(8, deathcount);
			pstmt.setInt(9, joincount);
			pstmt.setInt(10, kickcount);
			pstmt.setString(11, rank);
			pstmt.setString(12, firstdate);
			pstmt.setString(13, lastdate);
			pstmt.setString(14, lastblockplace);
			pstmt.setString(15, lastblockbreak);
			pstmt.setInt(16, playtime);
			pstmt.setString(17, lastchat);
			pstmt.setInt(18, attackclear);
			pstmt.setInt(19, pvpwincount);
			pstmt.setInt(20, pvplosecount);
			pstmt.setInt(21, pvpbreakout);
			pstmt.setInt(22, reactorcount);
			pstmt.executeUpdate();
		} catch (SQLException e){
			Log.info("SQL ERROR! "+e);
		}
	}

	public void selectAll(){
		String sql = "SELECT name, uuid, isAdmin, isLocal, placecount, breakcount, killcount, deathcount, joincount, kickcount, rank, firstdate, lastdate, lastblockplace, lastblockbreak, playtime, lastchat, attackclear, pvpwincount, pvplosecount, pvpbreakout, reactorcount FROM players";

		try (Connection conn = this.connect();
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql)){
			while(rs.next()){
				Log.info(rs.getInt("name")+" / "+
						rs.getString("uuid"));
			}
		} catch (SQLException e){
			Log.info("SQL ERROR! "+e);
		}
	}

	public static void main(String[] args) {

	}
/*
	private static Player player;
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
	
	public static String placecount(){
		String placecount = playerinfo().result[0];
		return placecount;
	}

	public static String breakcount(){
		String breakcount = playerinfo().result[1];
		return breakcount;
	}

	public static String killcount(){
		String killcount = playerinfo().result[2];
		return killcount;
	}

	public static String deathcount(){
		player = new Player();
		String deathcount = playerinfo().result[3];
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
*/
	//public static String rank(Player player){
	//	//source
	//}

	public static void playerinfo(Player player){
		return;
		/*
		//EventType event = new EventType();
		if(!Core.settings.getDataDirectory().child("plugins/Essentials/players/"+player.name+".txt").exists()){
			try{
				String ip = Vars.netServer.admins.getInfo(player.uuid).lastIP;
				player.sendMessage("[green][INFO][] Getting information...");
				String connUrl = "http://ipapi.co/"+ip+"/country_name";
				Document doc = Jsoup.connect(connUrl).get();
				String geo = doc.text();
				return geo;
			} catch (Exception e){
				
			}
			String name = player.name;
			String uuid = player.uuid;
			String lastIP = Vars.netServer.admins.getInfo(player.uuid).lastIP;
			String lastText = player.lastText;
			boolean isAdmin = player.isAdmin;
			boolean isLocal = player.isLocal;
			String geo = "KR";
			int joincount = Vars.netServer.admins.getInfo(player.uuid).timesJoined;
			int kickcount = Vars.netServer.admins.getInfo(player.uuid).timesKicked;
			int pce = 0;
			int bce = 0;
			int kce = 0;
			int dce = 0;
			String text = "Name: "+name+"\nUUID: "+uuid+"\nIP: "+lastIP+"\nlastText: "+lastText+"\ncountry: "+geo+"\nAdmin: "+isAdmin+"\nLocal: "+isLocal+"\njoincount: "+joincount+"\nkickcount: "+kickcount+"\nplacecount: "+pce+"\nbreakcount: "+bce+"\nkillcount: "+kce+"\ndeathcount: "+dce;
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
		*/
	}
}