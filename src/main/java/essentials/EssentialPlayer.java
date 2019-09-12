package essentials;

import io.anuke.arc.Core;
import org.json.JSONObject;
import org.json.JSONTokener;

class EssentialPlayer{
	public static void createNewDatabase(String name, String uuid, boolean isAdmin, boolean isLocal, String country, String country_code, int placecount, int breakcount, int killcount, int deathcount, int joincount, int kickcount, String rank, String firstdate, String lastdate, String lastplacename, String lastbreakname, String playtime, String lastchat, int attackclear, int pvpwincount, int pvplosecount, int pvpbreakout, int reactorcount, String bantimeset, int bantime, boolean translate) {
		JSONObject data = new JSONObject();
		data.put("name", name);
		data.put("uuid", uuid);
		data.put("isAdmin", isAdmin);
		data.put("isLocal", isLocal);
		data.put("country", country);
		data.put("country_code", country_code);
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
		data.put("translate", translate);
		String json = data.toString();
		Core.settings.getDataDirectory().child("plugins/Essentials/players/"+uuid+".json").writeString(json);
	}

	public static JSONObject getData(String uuid){
		String db = Core.settings.getDataDirectory().child("plugins/Essentials/players/"+uuid+".json").readString();
		JSONTokener parser = new JSONTokener(db);
		JSONObject object = new JSONObject(parser);
		return object;
	}
/*
	public static JSONObject writeData(String uuid, String data){
		String db = Core.settings.getDataDirectory().child("plugins/Essentials/players/"+uuid+".json").readString();
		JSONTokener parser = new JSONTokener(db);
		JSONObject object = new JSONObject(parser);
		JSONObject response = (JSONObject) object.get("data");
		//JSONObject write = object.put(data);
		return response;
	}

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