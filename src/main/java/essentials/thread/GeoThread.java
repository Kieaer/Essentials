package essentials.thread;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

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

    public static boolean ifip(InetAddress addr) {
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress())
            return true;
        try {
            return NetworkInterface.getByInetAddress(addr) != null;
        } catch (SocketException e) {
            return false;
        }
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Get geolocation thread");
        boolean isLocal = false;
        try {
            isLocal = ifip(InetAddress.getByName(ip));
        }
        catch(Exception e){
            e.printStackTrace();
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