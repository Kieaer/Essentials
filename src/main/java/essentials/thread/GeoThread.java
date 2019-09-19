package essentials.thread;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeoThread implements Runnable{
    private String ip;
    private boolean isLocal;
    private static String geo;
    private static String geocode;
    private static String lang;

    public GeoThread(String ip, boolean isLocal) {
        this.ip = ip;
        this.isLocal = isLocal;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Get geolocation thread");
        Pattern p = Pattern.compile(
                "(^127\\.)|\n" +
                "(^10\\.)|\n" +
                "(^172\\.1[6-9]\\.)|(^172\\.2[0-9]\\.)|(^172\\.3[0-1]\\.)|\n" +
                "(^192\\.168\\.)");
        Matcher m = p.matcher(ip);

        if(m.find()){
            isLocal = true;
        }
        if(isLocal) {
            geo = "Local IP";
            geocode = "LC";
            lang = "en";
        } else {
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
    }
    public static String getgeo(){
        return geo;
    }
    public static String getgeocode(){ return geocode; }
    public static String getlang(){ return lang; }
}