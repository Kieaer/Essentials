package essentials.thread;

import io.anuke.arc.util.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;

public class GeoThread implements Runnable{
    private String ip;
    private static String geo;
    private static String geocode;

    public GeoThread(String ip) {
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
            geo = "invalid";
            geocode = "invalid";
        }
    }
    public static String getgeo(){
        return geo;
    }
    public static String getgeocode(){
        return geocode;
    }
}