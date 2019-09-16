package essentials.thread;

import io.anuke.arc.Core;
import io.anuke.arc.util.Log;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;

public class GeoThread implements Runnable{
    private String ip;
    private static String geo;
    private static String geocode;
    private static String lang;

    public GeoThread(String ip) {
        this.ip = ip;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Get geolocation thread");
        try {
            String json = Jsoup.connect("http://ipapi.co/" + ip + "/json").ignoreContentType(true).execute().body();
            JSONTokener parser = new JSONTokener(json);
            JSONObject result = new JSONObject(parser);
            geo = (String) result.get("country_name");
            geocode = (String) result.get("country");
            lang = ((String) result.get("languages")).substring(0, 1);
        } catch (IOException e) {
            geo = "invalid";
            geocode = "invalid";
            lang = "en";
        }
    }
    public static String getgeo(){
        return geo;
    }
    public static String getgeocode(){ return geocode; }
    public static String getlang(){ return lang; }
}