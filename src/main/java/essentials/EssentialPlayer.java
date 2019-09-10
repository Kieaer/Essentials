package essentials;

import io.anuke.arc.Core;
import io.anuke.arc.files.FileHandle;
import jdk.nashorn.internal.parser.JSONParser;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class EssentialPlayer{
	public static void createNewDatabase(String name, String uuid, boolean isAdmin, boolean isLocal, String country, int placecount, int breakcount, int killcount, int deathcount, int joincount, int kickcount, String rank, String firstdate, String lastdate, String lastplacename, String lastbreakname, int playtime, String lastchat, int attackclear, int pvpwincount, int pvplosecount, int pvpbreakout, int reactorcount, String bantimeset, int bantime) {
		JSONObject data = new JSONObject();
		data.put("name", name);
		data.put("uuid", uuid);
		data.put("isAdmin", isAdmin);
		data.put("isLocal", isLocal);
		data.put("Country", country);
		data.put("placecount", placecount);
		data.put("breakcount", breakcount);
		data.put("killcount", killcount);
		data.put("deathcount", deathcount);
		data.put("joincount", joincount);
		data.put("kickcount", kickcount);
		data.put("rank", rank);
		data.put("firstdate", firstdate);
		data.put("lastdate", lastdate);
		data.put("lastplacename", lastplacename);
		data.put("lastbreakname", lastbreakname);
		data.put("playtime", playtime);
		data.put("lastchat", lastchat);
		data.put("attackclear", attackclear);
		data.put("pvpwincount", pvpwincount);
		data.put("pvplosecount", pvplosecount);
		data.put("pvpbreakout", pvpbreakout);
		data.put("reactorcount", reactorcount);
		data.put("bantimeset", bantimeset);
		data.put("bantime", bantime);
		JSONObject response = new JSONObject();
		response.put("data", data);
		String json = response.toString();
		Core.settings.getDataDirectory().child("plugins/Essentials/players/"+uuid+".json").writeString(json);
	}

	public static JSONObject getData(String uuid){
		String db = Core.settings.getDataDirectory().child("plugins/Essentials/players/"+uuid+".json").readString();
		JSONTokener parser = new JSONTokener(db);
		JSONObject object = new JSONObject(parser);
		JSONObject response = (JSONObject) object.get("data");
		return response;
	}
/*
	public static JSONObject getAll() throws FileNotFoundException {
		File dir = new File("plugins/Essentials/players");
		for (File file : dir.listFiles()) {
			Scanner s = new Scanner(file);
			JSONTokener parser = new JSONTokener(String.valueOf(s));
			JSONObject object = new JSONObject(parser);
			JSONObject response = (JSONObject) object.get("data");
			s.close();
		}

		return response;
	}
 */
}